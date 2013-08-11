package com.heroicrobot.dropbit.devices.pixelpusher;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.List;

import com.heroicrobot.dropbit.common.ByteUtils;
import com.heroicrobot.dropbit.registry.DeviceRegistry;

public class CardThread extends Thread {

  private long threadSleepMsec = 4;
  private long threadExtraDelayMsec = 0;
  private long bandwidthEstimate = 0;
  private PixelPusher pusher;
  private byte[] packet;
  private DatagramPacket udppacket;
  private DatagramSocket udpsocket;
  private boolean cancel;
  private int pusherPort;
  private InetAddress cardAddress;
  private long packetNumber;
  private DeviceRegistry registry;
  private boolean useAntiLog;
  private boolean fileIsOpen;
  FileOutputStream recordFile;

  CardThread(PixelPusher pusher, DeviceRegistry dr) {
    this.pusher = pusher;
    this.pusherPort = pusher.getPort();

    this.registry = dr;
    try {
      this.udpsocket = new DatagramSocket();
    } catch (SocketException se) {
      System.err.println("SocketException: " + se.getMessage());
    }
    this.packet = new byte[1460];
    this.cardAddress = pusher.getIp();
    this.packetNumber = 0;
    this.cancel = false;
    this.fileIsOpen = false;
    if (pusher.getUpdatePeriod() > 100 && pusher.getUpdatePeriod() < 1000000)
      this.threadSleepMsec = (pusher.getUpdatePeriod() / 1000) + 1;
  }

  public void setExtraDelay(long msec) {
    threadExtraDelayMsec = msec;
  }
  public boolean controls(PixelPusher test) {
    return test.equals(this.pusher);
  }

  public int getBandwidthEstimate() {
    return (int) bandwidthEstimate;
  }

  @Override
  public void run() {
    while (!cancel) {
      int bytesSent;
      long startTime = System.nanoTime();
      // check to see if we're supposed to be recording.
      if (pusher.isAmRecording()) {
        if (!fileIsOpen) {
          try {
            recordFile = new FileOutputStream(new File(pusher.getFilename()));
            fileIsOpen = true;
          } catch (Exception e) {
            System.err.println("Failed to open recording file "+pusher.getFilename());
            pusher.setAmRecording(false);
          }
        }
      }
      bytesSent = sendPacketToPusher(pusher);
      long endTime = System.nanoTime();
      long duration = ((endTime - startTime) / 1000000);
      if (duration > 0)
        bandwidthEstimate = bytesSent / duration;
    }
  }

  public void shutDown() {
    if (fileIsOpen)
      try {
        pusher.setAmRecording(false);
        fileIsOpen = false;
        recordFile.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
  }
  
  public boolean cancel() {
    this.cancel = true;
    return true;
  }
  
  private int sendPacketToPusher(PixelPusher pusher) {
    int packetLength = 0;
    int totalLength = 0;
    long totalDelay = threadSleepMsec + threadExtraDelayMsec + pusher.getExtraDelay();
    boolean payload;
    double powerScale;
    
    powerScale = registry.getPowerScale();
    
    List<Strip> remainingStrips = new ArrayList<Strip>(pusher.getStrips());
    final int requestedStripsPerPacket = pusher.getMaxStripsPerPacket();
    final int supportedStripsPerPacket
        = (this.packet.length - 4) / (1 + 3 * pusher.getPixelsPerStrip());
    final int stripPerPacket = Math.min(requestedStripsPerPacket,
                                        supportedStripsPerPacket);

    while (!remainingStrips.isEmpty()) {
      payload = false;
      if (pusher.getUpdatePeriod() > 100)
        this.threadSleepMsec = (pusher.getUpdatePeriod() / 1000) + 1;
      byte[] packetNumberArray = ByteUtils.unsignedIntToByteArray(packetNumber, true);
      for(int i = 0; i < packetNumberArray.length; i++) {
        this.packet[packetLength++] = packetNumberArray[i];
      }
      for (int i = 0; i < stripPerPacket; i++) {
        if (remainingStrips.isEmpty()) {
          break;
        }
        Strip strip = remainingStrips.remove(0);
        if (strip.isTouched() ) {
          strip.setPowerScale(powerScale);
          byte[] stripPacket = strip.serialize();
          this.packet[packetLength++] = (byte) strip.getStripNumber();
          if (fileIsOpen) {
            try {
              // we need to make the pusher wait on playback the same length of time between strips as we wait between packets
              // this number is in microseconds, whereas we work with milliseconds.
              recordFile.write(ByteUtils.unsignedIntToByteArray((int)(1000 * ((threadExtraDelayMsec + pusher.getExtraDelay()) / stripPerPacket)), true));
              recordFile.write(this.packet, packetLength-1, 1);
              recordFile.write(stripPacket);
            } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
          for (int j = 0; j < stripPacket.length; j++) {
            this.packet[packetLength + j] = stripPacket[j];
          }
          packetLength += stripPacket.length;
          payload = true;
        }
      }
      if (payload) {
        packetNumber++;
        /* System.err.println(" Packet number array = length "+ packetLength + 
         *      " seq "+ packetNumber +" data " + String.format("%02x, %02x, %02x, %02x", 
         *          packetNumberArray[0], packetNumberArray[1], packetNumberArray[2], packetNumberArray[3]));
         */
        udppacket = new DatagramPacket(packet, packetLength, cardAddress,
            pusherPort);
        try {
          udpsocket.send(udppacket);    
        } catch (IOException ioe) {
          System.err.println("IOException: " + ioe.getMessage());
        }
       
        totalLength += packetLength;
      }
      try {
        Thread.sleep(totalDelay);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      packetLength = 0;
    }
    return totalLength;
  }

  public boolean hasTouchedStrips() {
    List<Strip> allStrips = new ArrayList<Strip>(pusher.getStrips());
    for (Strip strip: allStrips)
        if (strip.isTouched())
          return true;
    return false;
  }

  public void setAntiLog(boolean antiLog) {
    useAntiLog = antiLog;
    for (Strip strip: pusher.getStrips())
       strip.useAntiLog(useAntiLog);
  }
}
