package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import com.heroicrobot.dropbit.registry.DeviceRegistry;

public class PusherTask extends Thread implements Observer {

  private static int PUSHER_PORT = 9897;

  private Map<String, PixelPusher> pusherMap;
  private Map<String, CardThread> cardThreadMap;
  byte[] packet;
  int packetLength;

  private boolean drain;

  public PusherTask() {
    this.pusherMap = new HashMap<String, PixelPusher>();
    this.cardThreadMap = new HashMap<String, CardThread>();

  }

  @Override
  public void update(Observable observable, Object update) {
    if (!drain) {
      Map<String, PixelPusher> incomingPusherMap = ((DeviceRegistry) observable)
          .getPusherMap();
      Map<String, PixelPusher> newPusherMap = new HashMap<String, PixelPusher>(
          incomingPusherMap);
      Map<String, PixelPusher> deadPusherMap = new HashMap<String, PixelPusher>(
          incomingPusherMap);

      for (String key : newPusherMap.keySet()) {
        if (pusherMap.containsKey(key)) {
          newPusherMap.remove(key);
        }
      }
      for (String key : pusherMap.keySet()) {
        if (newPusherMap.containsKey(key)) {
          deadPusherMap.remove(key);
        }
      }

      for (String key : newPusherMap.keySet()) {
        CardThread newCardThread = new CardThread(pusherMap.get(key),
            PUSHER_PORT);
        newCardThread.run();
        cardThreadMap.put(key, newCardThread);
      }
      for (String key : deadPusherMap.keySet()) {
        cardThreadMap.get(key).cancel();
        cardThreadMap.remove(key);
      }
    }
  }

  @Override
  public void run() {
    this.drain = false;
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
