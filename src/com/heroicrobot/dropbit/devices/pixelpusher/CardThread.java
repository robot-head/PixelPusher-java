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
  private long threadSendTime = 0;
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

  CardThread(PixelPusher pusher, int pusherPort, DeviceRegistry dr) {
    this.pusher = pusher;
    this.pusherPort = pusherPort;
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
      bytesSent = sendPacketToPusher(pusher);
      long endTime = System.nanoTime();
      bandwidthEstimate = bytesSent / ((endTime - startTime) / 1000000);
    }
  }

  public void shutDown() {
    
  }
  
  public boolean cancel() {
    this.cancel = true;
    return true;
  }

  private int sendPacketToPusher(PixelPusher pusher) {
    int packetLength = 0;
    int totalLength = 0;
    boolean payload;
    double powerScale;
    
    powerScale = registry.getPowerScale();
    
    int stripPerPacket = pusher.getMaxStripsPerPacket();
    List<Strip> remainingStrips = new ArrayList<Strip>(pusher.getStrips());
    while (!remainingStrips.isEmpty()) {
      payload = false;
      if (pusher.getUpdatePeriod() > 100 && pusher.getUpdatePeriod() < 100000)
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
          long startTime = System.nanoTime();
          udpsocket.send(udppacket);
          long endTime = System.nanoTime();
          threadSendTime = (endTime - startTime) / 1000000;
        
        } catch (IOException ioe) {
          System.err.println("IOException: " + ioe.getMessage());
        }
       
        totalLength += packetLength;
      }
      try {
        Thread.sleep(threadSleepMsec + threadExtraDelayMsec + threadSendTime + pusher.getExtraDelay() );
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      packetLength = 0;
    }
    return totalLength;
  }
}
