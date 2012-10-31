package com.heroicrobot.dropbit.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.heroicrobot.dropbit.devices.pixelpusher.Pixel;
import com.heroicrobot.dropbit.devices.pixelpusher.PixelPusher;
import com.heroicrobot.dropbit.devices.pixelpusher.PusherTask;
import com.heroicrobot.dropbit.devices.pixelpusher.Strip;
import com.heroicrobot.dropbit.discovery.DeviceHeader;
import com.heroicrobot.dropbit.discovery.DeviceType;

import hypermedia.net.UDP;

public class DeviceRegistry extends Observable {

  private final static Logger LOGGER = Logger.getLogger(DeviceRegistry.class
      .getName());

  private static final long PUSHER_UPDATE_INTERVAL = 16;

  private UDP udp;
  private static int DISCOVERY_PORT = 7331;
  private static int MAX_DISCONNECT_SECONDS = 2;
  private static long EXPIRY_TIMER_MSEC = 1000L;

  private Map<String, PixelPusher> pusherMap;
  private Map<String, DateTime> pusherLastSeenMap;

  private Timer expiryTimer;

  private PusherTask pusherTask;

  private Timer pusherTaskTimer;

  public Map<String, PixelPusher> getPusherMap() {
    return pusherMap;
  }
  
  public List<Strip> getStrips() {
    List<Strip> strips = new ArrayList<Strip>();
    for (PixelPusher pusher : pusherMap.values()) {
      strips.addAll(pusher.getStrips());
    }
    return strips;
  }

  class DeviceExpiryTask extends TimerTask {

    private DeviceRegistry registry;

    DeviceExpiryTask(DeviceRegistry registry) {
      this.registry = registry;
    }

    @Override
    public void run() {
      LOGGER.fine("Expiry Task running");
      for (String deviceMac : pusherMap.keySet()) {
        Seconds lastSeenSeconds = Seconds.secondsBetween(
            pusherLastSeenMap.get(deviceMac), DateTime.now());
        if (lastSeenSeconds.getSeconds() > MAX_DISCONNECT_SECONDS) {
          registry.expireDevice(deviceMac);
        }
      }
    }

  }

  public DeviceRegistry() {
    udp = new UDP(this, DISCOVERY_PORT);
    pusherMap = new HashMap<String, PixelPusher>();
    pusherLastSeenMap = new HashMap<String, DateTime>();
    udp.setReceiveHandler("receive");
    udp.log(false);
    udp.listen(true);
    this.expiryTimer = new Timer();
    this.expiryTimer.scheduleAtFixedRate(new DeviceExpiryTask(this), 0L,
        EXPIRY_TIMER_MSEC);
    this.pusherTask = new PusherTask();
    this.addObserver(this.pusherTask);
    this.pusherTaskTimer = new Timer();
  }

  public void expireDevice(String macAddr) {
    LOGGER.info("Device gone: " + macAddr);
    pusherMap.remove(macAddr);
    pusherLastSeenMap.remove(macAddr);
    this.setChanged();
    this.notifyObservers();
  }

  public void setStripValues(String macAddress, int stripNumber, Pixel[] pixels) {
    this.pusherMap.get(macAddress).setStripValues(stripNumber, pixels);
  }
  
  public void startPushing() {
    this.pusherTaskTimer.schedule(this.pusherTask, 0, PUSHER_UPDATE_INTERVAL);
  }
  
  public void stopPushing() {
    this.pusherTaskTimer.cancel();
  }

  public void receive(byte[] data) {
    // This is for the UDP callback, this should not be called directly
    DeviceHeader header = new DeviceHeader(data);
    String macAddr = header.GetMacAddressString();
    if (header.DeviceType != DeviceType.PIXELPUSHER) {
      LOGGER.fine("Ignoring non-pixel pusher discovery packet from "
          + header.toString());
      return;
    }
    PixelPusher device = new PixelPusher(header.PacketRemainder, header);
    // Set the timestamp for the last time this device checked in
    pusherLastSeenMap.put(macAddr, DateTime.now());
    if (!pusherMap.containsKey(macAddr)) {
      // We haven't seen this device before
      LOGGER.info("New device: " + macAddr);
      pusherMap.put(macAddr, device);
      this.setChanged();
      this.notifyObservers(device);
    } else {
      if (!pusherMap.get(macAddr).equals(device)) {
        // We already knew about this device at the given MAC, but its details
        // have changed
        LOGGER.info("Device changed: " + macAddr);
        pusherMap.put(macAddr, device);
        this.setChanged();
        this.notifyObservers(device);
      } else {
        // The device is identical, nothing has changed
        LOGGER.fine("Device still present: " + macAddr);
      }
    }
  }

}
