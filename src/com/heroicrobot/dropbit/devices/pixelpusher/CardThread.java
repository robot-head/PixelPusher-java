package com.heroicrobot.dropbit.devices.pixelpusher;

import hypermedia.net.UDP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CardThread extends Thread {

  private static final long THREAD_SLEEP_MS = 4;
  private PixelPusher pusher;
  private byte[] packet;
  private int packetLength;
  private UDP udp;
  private boolean cancel;
  private int pusherPort;

  CardThread(PixelPusher pusher, int pusherPort) {
    this.pusher = pusher;
    this.pusherPort = pusherPort;
    this.packet = new byte[1460];
    this.packetLength = 0;
    this.udp = new UDP(this);
  }

  @Override
  public void run() {
    while (!cancel) {
      sendPacketToPusher(pusher);
      try {
        this.wait(THREAD_SLEEP_MS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }

  public boolean cancel() {
    this.cancel = true;
    return true;
  }

  private void sendPacketToPusher(PixelPusher pusher) {
    int stripPerPacket = pusher.getMaxStripsPerPacket();
    List<Strip> remainingStrips = new ArrayList<Strip>(pusher.getStrips());
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
        this.packetLength += stripPacket.length;
      }
      this.udp.setBuffer(this.packetLength);
      byte[] slicedPacket = Arrays.copyOfRange(packet, 0, packetLength);
      this.udp
          .send(slicedPacket, pusher.getIp().getHostAddress(), pusherPort);
      // System.out.println(Arrays.toString(this.packet));
      this.packetLength = 0;
    }
  }
}
