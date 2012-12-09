package com.heroicrobot.dropbit.devices.pixelpusher;

import java.io.*;
import java.net.*;

import java.util.ArrayList;
import java.util.List;

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

  CardThread(PixelPusher pusher, int pusherPort) {
    this.pusher = pusher;
    this.pusherPort = pusherPort;
    try {
      this.udpsocket = new DatagramSocket();
    } catch (SocketException se) {
      System.err.println("SocketException: " + se.getMessage());
    }
    this.packet = new byte[1460];
    this.cardAddress = pusher.getIp();
    this.cancel = false;
    if (pusher.getUpdatePeriod() > 100 && pusher.getUpdatePeriod() < 1000000)
      this.threadSleepMsec = (pusher.getUpdatePeriod() / 1000) + 1;
  }

  public void setExtraDelay(long msec) {
    threadExtraDelayMsec = msec;
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

  public boolean cancel() {
    this.cancel = true;
    return true;
  }

  private int sendPacketToPusher(PixelPusher pusher) {
    int packetLength = 0;
    int totalLength = 0;
    int stripPerPacket = pusher.getMaxStripsPerPacket();
    List<Strip> remainingStrips = new ArrayList<Strip>(pusher.getStrips());
    if (pusher.getUpdatePeriod() > 100 && pusher.getUpdatePeriod() < 1000000)
      this.threadSleepMsec = (pusher.getUpdatePeriod() / 1000) + 1;
    while (!remainingStrips.isEmpty()) {
      for (int i = 0; i < stripPerPacket; i++) {
        if (remainingStrips.isEmpty()) {
          break;
        }
        Strip strip = remainingStrips.remove(0);
        byte[] stripPacket = strip.serialize();
        this.packet[packetLength++] = (byte) strip.getStripNumber();
        for (int j = 0; j < stripPacket.length; j++) {
          this.packet[packetLength + j] = stripPacket[j];
        }
        packetLength += stripPacket.length;
      }
      udppacket = new DatagramPacket(packet, packetLength, cardAddress,
          pusherPort);
      try {
        udpsocket.send(udppacket);
      } catch (IOException ioe) {
        System.err.println("IOException: " + ioe.getMessage());
      }
      totalLength += packetLength;

      try {
        Thread.sleep(threadSleepMsec + threadExtraDelayMsec);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      packetLength = 0;
    }
    return totalLength;
  }
}
