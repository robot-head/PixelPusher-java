package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import com.heroicrobot.dropbit.registry.DeviceRegistry;

public class SceneThread extends Thread implements Observer {

  private static int PUSHER_PORT = 9897;

  private Map<String, PixelPusher> pusherMap;
  private Map<String, CardThread> cardThreadMap;
  byte[] packet;
  int packetLength;

  private boolean drain;

  private boolean running;

  public SceneThread() {
    this.pusherMap = new HashMap<String, PixelPusher>();
    this.cardThreadMap = new HashMap<String, CardThread>();
    this.drain = false;
    this.running = false;

  }

  public long getTotalBandwidth() {
    long totalBandwidth=0;
    for (CardThread thread : cardThreadMap.values()) {
      totalBandwidth += thread.getBandwidthEstimate();
    }
    return totalBandwidth;
  }
  
  public void setExtraDelay(int msec) {
    for (CardThread thread : cardThreadMap.values()) {
      thread.setExtraDelay(msec);
    }
  }
  
  @Override
  public void update(Observable observable, Object update) {
    if (!drain) {
      Map<String, PixelPusher> incomingPusherMap = ((DeviceRegistry) observable)
          .getPusherMap(); // all observed pushers
      Map<String, PixelPusher> newPusherMap = new HashMap<String, PixelPusher>(
          incomingPusherMap);
      Map<String, PixelPusher> deadPusherMap = new HashMap<String, PixelPusher>(
          pusherMap);

      for (String key : newPusherMap.keySet()) {
        if (pusherMap.containsKey(key)) { // if we already know about it
          newPusherMap.remove(key); // remove it from the new pusher map (is
                                    // old)
        }
      }
      for (String key : pusherMap.keySet()) {
        if (newPusherMap.containsKey(key)) { // if it's in the new pusher map
          deadPusherMap.remove(key); // it can't be dead
        }
      }

      for (String key : newPusherMap.keySet()) {
        CardThread newCardThread = new CardThread(newPusherMap.get(key),
            PUSHER_PORT);
        if (running) {
          newCardThread.start();
        }
        cardThreadMap.put(key, newCardThread);
      }
      for (String key : deadPusherMap.keySet()) {
        System.out.println("Killing old CardThread " + key);
        cardThreadMap.get(key).cancel();
        cardThreadMap.remove(key);
      }
    }
  }

  public boolean isRunning() {
    return this.running;
  }

  @Override
  public void run() {
    this.running = true;
    this.drain = false;
    for (CardThread thread : cardThreadMap.values()) {
      thread.start();
    }
  }

  public boolean cancel() {
    this.drain = true;
    for (String key : cardThreadMap.keySet()) {
      cardThreadMap.get(key).cancel();
      cardThreadMap.remove(key);
    }
    return true;
  }
}
