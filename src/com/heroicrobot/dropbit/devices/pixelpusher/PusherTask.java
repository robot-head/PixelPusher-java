package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import hypermedia.net.UDP;
import com.heroicrobot.dropbit.registry.DeviceRegistry;

public class PusherTask extends TimerTask implements Observer {

  private static int PUSHER_PORT = 9897;
  
  private Map<String, PixelPusher> pusherMap;
  byte[] packet;
  int packetLength;
  private Semaphore semaphore;
  private UDP udp;

  public PusherTask() {
    this.pusherMap = new HashMap<String, PixelPusher>();
    this.packet = new byte[1460];
    this.packetLength = 0;
    this.semaphore = new Semaphore(1);
    this.udp = new UDP(this);
  }

  @Override
  public void update(Observable observable, Object update) {
    this.semaphore.acquireUninterruptibly();
    this.pusherMap = ((DeviceRegistry) observable).getPusherMap();
    this.semaphore.release();

  }

  @Override
  public void run() {
    this.semaphore.acquireUninterruptibly();
    if (pusherMap.isEmpty())
      return;
    for (PixelPusher pusher : pusherMap.values()) {
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
        this.udp.send(this.packet, pusher.getIp().getHostAddress(), PUSHER_PORT);
        //System.out.println(Arrays.toString(this.packet));
        this.packetLength = 0;
      }
    }
    this.semaphore.release();

  }

}
