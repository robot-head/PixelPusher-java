package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TimerTask;

import com.heroicrobot.dropbit.registry.DeviceRegistry;

public class PusherTask extends TimerTask implements Observer {

  private Map<String, PixelPusher> pusherMap;

  public PusherTask() {
    this.pusherMap = new HashMap<String, PixelPusher>();
  }
  
  @Override
  public void update(Observable observable, Object update) {
    DeviceRegistry registry = (DeviceRegistry) observable;
    this.pusherMap = registry.getPusherMap();

  }

  @Override
  public void run() {
    for (PixelPusher pusher : pusherMap.values()) {
      int stripPerPacket = pusher.getMaxStripsPerPacket();
      List<Strip> remainingStrips = pusher.getStrips();
      List<Byte> packet;
      while(!remainingStrips.isEmpty()) {
        packet = new ArrayList<Byte>();
        for(int i = 0; i < stripPerPacket; i++) {
          if (remainingStrips.isEmpty()) {
            break;
          }
          Strip strip = remainingStrips.remove(0);
          List<Byte> stripBytes = new ArrayList<Byte>(Arrays.asList(strip.serialize()));
        }
      }
    }

  }

}
