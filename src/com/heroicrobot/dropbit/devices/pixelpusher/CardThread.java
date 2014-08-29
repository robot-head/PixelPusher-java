package com.heroicrobot.dropbit.devices.pixelpusher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.heroicrobot.dropbit.common.ByteUtils;
import com.heroicrobot.dropbit.registry.DeviceRegistry;

public class CardThread extends Thread {

  private long threadSleepMsec = 4;
  private long threadExtraDelayMsec = 0;
  private long bandwidthEstimate = 0;
  private int maxPacketSize = 1460;
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
  private long lastWorkTime;
  private long firstSendTime;
  public boolean terminated=false;

  CardThread(PixelPusher pusher, DeviceRegistry dr) {
    super("CardThread for PixelPusher "+pusher.getMacAddress());
    this.pusher = pusher;
    this.pusherPort = pusher.getPort();
    this.lastWorkTime = System.nanoTime();

    this.registry = dr;
    try {
      this.udpsocket = new DatagramSocket();
    } catch (SocketException se) {
      System.err.println("SocketException: " + se.getMessage());
    }
    maxPacketSize = 4 +  ((1 + 3 * pusher.getPixelsPerStrip()) * pusher.getMaxStripsPerPacket());
    this.packet = new byte[maxPacketSize];
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
      if (pusher.isMulticast()) {
        if (!pusher.isMulticastPrimary()) {
          try {
           Thread.sleep(1000);
          } catch (InterruptedException ie) {
            // we don't care.
          }
          continue; // we just sleep until we're primary
        }
      }
      
      int bytesSent;
      long startTime = System.nanoTime();
      // check to see if we're supposed to be recording.
      if (pusher.isAmRecording()) {
        if (!fileIsOpen) {
          try {
            recordFile = new FileOutputStream(new File(pusher.getFilename()));
            fileIsOpen = true;
            firstSendTime = System.nanoTime();
          } catch (Exception e) {
            System.err.println("Failed to open recording file "+pusher.getFilename());
            pusher.setAmRecording(false);
          }
        }
      }
      bytesSent = sendPacketToPusher(pusher);
      
      int requestedStripsPerPacket = pusher.getMaxStripsPerPacket();
      int stripPerPacket = Math.min(requestedStripsPerPacket, pusher.stripsAttached);
      
      if (bytesSent == 0) {
        try {
          long estimatedSleep = (System.nanoTime() - lastWorkTime)/1000000;
          estimatedSleep = Math.min(estimatedSleep, ((1000/registry.getFrameLimit()) 
                                      / (pusher.stripsAttached / stripPerPacket)));
          
          Thread.sleep(estimatedSleep);
        } catch (InterruptedException e) {
          // Don't care if we get interrupted.
        }
      }
      else {
        lastWorkTime = System.nanoTime();
      }
      long endTime = System.nanoTime();
      long duration = ((endTime - startTime) / 1000000);
      if (duration > 0)
        bandwidthEstimate = bytesSent / duration;
    }
    terminated = true;
  }

  public void shutDown() {
    pusher.shutDown();
    if (fileIsOpen)
      try {
        pusher.setAmRecording(false);
        fileIsOpen = false;
        recordFile.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    this.cancel = true;
    while (!terminated) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        System.err.println("Interrupted terminating CardThread "+pusher.getMacAddress());
        e.printStackTrace();
      }
    }
  }

  private int sendPacketToPusher(PixelPusher pusher) {
    int packetLength;
    int totalLength = 0;
    long totalDelay;
    boolean payload;
    double powerScale;
    int stripsInDatagram;

    powerScale = registry.getPowerScale();

    List<Strip> remainingStrips;

    if (!pusher.hasTouchedStrips()) {
      //System.out.println("Yielding because no touched strips.");
      if (pusher.commandQueue.isEmpty())
        return 0;
    }

    if (pusher.isBusy()) {
      //System.out.println("Yielding because pusher is busy.");
      return 0;
    }

    pusher.makeBusy();
    //System.out.println("Making pusher busy.");

    remainingStrips = new CopyOnWriteArrayList<Strip>(pusher.getStrips());
    stripsInDatagram = 0;

    int requestedStripsPerPacket = pusher.getMaxStripsPerPacket();
    int stripPerPacket = Math.min(requestedStripsPerPacket, pusher.stripsAttached);

    while (!remainingStrips.isEmpty()) {
      packetLength = 0;
      payload = false;
      if (pusher.getUpdatePeriod() > 1000) {
        this.threadSleepMsec = (pusher.getUpdatePeriod() / 1000) + 1;
      } else {
        // Shoot for the framelimit.
        this.threadSleepMsec = ((1000/registry.getFrameLimit()) / (pusher.stripsAttached / stripPerPacket));
      }

      // Handle errant delay calculation in the firmware.
      if (pusher.getUpdatePeriod() > 100000)
        this.threadSleepMsec = (16 / (pusher.stripsAttached / stripPerPacket));

      totalDelay = threadSleepMsec + threadExtraDelayMsec + pusher.getExtraDelay();

      byte[] packetNumberArray = ByteUtils.unsignedIntToByteArray(packetNumber, true);
      for(int i = 0; i < packetNumberArray.length; i++) {
        this.packet[packetLength++] = packetNumberArray[i];
      }

      // first check to see if we have an outstanding command.

      boolean commandSent;

      if (!(pusher.commandQueue.isEmpty())) {
        commandSent = true;
        System.out.println("Pusher "+pusher.getMacAddress()+" has a PusherCommand outstanding.");
        PusherCommand pc = pusher.commandQueue.remove();
        byte[] commandBytes= pc.generateBytes();

        packetLength = 0;
        packetNumberArray = ByteUtils.unsignedIntToByteArray(packetNumber, true);
        for(int j = 0; j < packetNumberArray.length; j++) {
          this.packet[packetLength++] = packetNumberArray[j];
        }
        for(int j = 0; j < commandBytes.length; j++) {
          this.packet[packetLength++] = commandBytes[j];
        }
        // We need fixed size datagrams for the Photon, because the cc3000 sucks.
        if ((pusher.getPusherFlags() & pusher.PFLAG_FIXEDSIZE) != 0) {
           packetLength = 4 + ((1 + 3 * pusher.getPixelsPerStrip()) * stripPerPacket);
        }
        packetNumber++;
        udppacket = new DatagramPacket(packet, packetLength, cardAddress,
            pusherPort);
        try {
          udpsocket.send(udppacket);
        } catch (IOException ioe) {
          System.err.println("IOException: " + ioe.getMessage());
        }

        totalLength += packetLength;
        
      } else {
        commandSent = false;
      }

      if (!commandSent) {
        int i;
        // Now loop over remaining strips.
        for (i = 0; i < stripPerPacket; i++) {
          if (remainingStrips.isEmpty()) {
            break;
          }
          Strip strip = remainingStrips.remove(0);
          // Don't weed untouched strips if we are recording.
          if (!fileIsOpen) {
            if (!strip.isTouched() && ((pusher.getPusherFlags() & pusher.PFLAG_FIXEDSIZE) == 0))
              continue;
          }

          stripsInDatagram++;
          
          strip.setPowerScale(powerScale);
          byte[] stripPacket;
          
          if (!DeviceRegistry.useOverallBrightnessScale) {
            stripPacket = strip.serialize();
          } else {
            stripPacket = strip.serialize(DeviceRegistry.getOverallBrightnessScale());
          }
          strip.setPushedAt(System.nanoTime());
          strip.markClean();
          this.packet[packetLength++] = (byte) strip.getStripNumber();
          if (fileIsOpen) {
            try {
              // we need to make the pusher wait on playback the same length of time between strips as we wait between packets
              // this number is in microseconds.
              if (stripsInDatagram > 1) { // only write the delay for the first strip in a datagram.
                if ((System.nanoTime() - firstSendTime / 1000) < (25 * 60 * 1000000)) {
                  recordFile.write(ByteUtils.unsignedIntToByteArray((int)0, true));
                } else {
                  // write the timer reset magic - we do this into a sentence that would otherwise have no timing info
                  recordFile.write(ByteUtils.unsignedIntToByteArray((int)0xdeadb33f, true));
                  // and reset the timer
                  firstSendTime = System.nanoTime();
                }
              } else {
                if (firstSendTime != 0) { // this is not the first send to this pusher, we know how long it's been
                  recordFile.write(ByteUtils.unsignedIntToByteArray((int)((System.nanoTime() - firstSendTime) / 1000), true));
                } else { // fall back to the update period.
                  recordFile.write(ByteUtils.unsignedIntToByteArray((int)pusher.getUpdatePeriod(), true));
                }
              }
              recordFile.write((byte) strip.getStripNumber());
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
        if (payload) {
          //System.out.println("Got a payload to send to "+cardAddress);
          packetNumber++;
          /* System.err.println(" Packet number array = length "+ packetLength +
           *      " seq "+ packetNumber +" data " + String.format("%02x, %02x, %02x, %02x",
           *          packetNumberArray[0], packetNumberArray[1], packetNumberArray[2], packetNumberArray[3]));
           */
          udppacket = new DatagramPacket(packet, packetLength, cardAddress,
              pusherPort);
          try {
            udpsocket.send(udppacket);
            //System.out.println("Sent it.");
          } catch (IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
          }
        }
        totalLength += packetLength;
      }

      packetLength = 0;
      payload = false;
      try {
        Thread.sleep(totalDelay);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    //System.out.println("Clearing busy.");
    pusher.clearBusy();
    return totalLength;
  }

  public boolean hasTouchedStrips() {
    List<Strip> allStrips = new CopyOnWriteArrayList<Strip>(pusher.getStrips());
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
