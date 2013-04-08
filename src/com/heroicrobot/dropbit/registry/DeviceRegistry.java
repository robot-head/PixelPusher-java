package com.heroicrobot.dropbit.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.heroicrobot.dropbit.devices.pixelpusher.Pixel;
import com.heroicrobot.dropbit.devices.pixelpusher.PixelPusher;
import com.heroicrobot.dropbit.devices.pixelpusher.PusherGroup;
import com.heroicrobot.dropbit.devices.pixelpusher.SceneThread;
import com.heroicrobot.dropbit.devices.pixelpusher.Strip;
import com.heroicrobot.dropbit.discovery.DeviceHeader;
import com.heroicrobot.dropbit.discovery.DeviceType;

import hypermedia.net.UDP;

public class DeviceRegistry extends Observable {

  private final static Logger LOGGER = Logger.getLogger(DeviceRegistry.class
      .getName());

  private UDP udp;
  private static int DISCOVERY_PORT = 7331;
  private static int MAX_DISCONNECT_SECONDS = 10;
  private static long EXPIRY_TIMER_MSEC = 1000L;
  
  private static long totalPower = 0;
  private static long totalPowerLimit = -1;
  private static double powerScale = 1.0;
  private static boolean autoThrottle = false;
  
  private Map<String, PixelPusher> pusherMap;
  private Map<String, DateTime> pusherLastSeenMap;

  private Timer expiryTimer;

  private SceneThread sceneThread;

  private TreeMap<Integer, PusherGroup> groupMap;

  private TreeSet<PixelPusher> sortedPushers;

  public Map<String, PixelPusher> getPusherMap() {
    return pusherMap;
  }

  public void setExtraDelay(int msec) {
    sceneThread.setExtraDelay(msec);
  }
  
  public void setAutoThrottle(boolean autothrottle) {
    autoThrottle = autothrottle;
    sceneThread.setAutoThrottle(autothrottle);
  }

  public long getTotalBandwidth() {
    return sceneThread.getTotalBandwidth();
  }

  public long getTotalPower() {
    return totalPower;
  }
  
  public void setTotalPowerLimit(long powerLimit) {
    totalPowerLimit = powerLimit;
  }
  
  public long getTotalPowerLimit() {
    return totalPowerLimit;
  }
  
  public double getPowerScale() {
    return powerScale;
  }
  
  public List<Strip> getStrips() {
    List<Strip> strips = new ArrayList<Strip>();
    for (PixelPusher p : this.sortedPushers) {
      strips.addAll(p.getStrips());
    }
    return strips;
  }

  public List<Strip> getStrips(int groupNumber) {
    if (this.groupMap.containsKey(groupNumber)) {
      return this.groupMap.get(groupNumber).getStrips();
    } else {
      List<Strip> emptyList = new ArrayList<Strip>();
      return emptyList;
    }
  }

  class DeviceExpiryTask extends TimerTask {

    private DeviceRegistry registry;

    DeviceExpiryTask(DeviceRegistry registry) {
      this.registry = registry;
    }

    @Override
    public void run() {
      LOGGER.fine("Expiry and preening task running");
      
      // A little sleight of hand here.  We can't call registry.expireDevice()
      // directly from inside the loop, for the loop is an implicit iterator and
      // registry.expireDevice modifies the pusherMap.
      // Instead we create a list of the MAC addresses to kill, then loop over
      // them outside the iterator.  - jls
      List<String> toKill = new ArrayList<String>();
      for (String deviceMac : pusherMap.keySet()) {
        Seconds lastSeenSeconds = Seconds.secondsBetween(
            pusherLastSeenMap.get(deviceMac), DateTime.now());
        if (lastSeenSeconds.getSeconds() > MAX_DISCONNECT_SECONDS) {
          toKill.add(deviceMac);
        }
      }
      for (String doomedIndividual : toKill) {
        registry.expireDevice(doomedIndividual);
      }
    }
  }

  private class DefaultPusherComparator implements Comparator<PixelPusher> {

    @Override
    public int compare(PixelPusher arg0, PixelPusher arg1) {
      int group0 = arg0.getGroupOrdinal();
      int group1 = arg1.getGroupOrdinal();
      if (group0 != group1) {
        if (group0 < group1)
          return -1;
        return 1;
      }

      int ord0 = arg0.getControllerOrdinal();
      int ord1 = arg1.getControllerOrdinal();
      if (ord0 != ord1) {
        if (ord0 < ord1)
          return -1;
        return 1;
      }

      return arg0.getMacAddress().compareTo(arg1.getMacAddress());
    }

  }

  public DeviceRegistry() {
    udp = new UDP(this, DISCOVERY_PORT);
    pusherMap = new TreeMap<String, PixelPusher>();
    groupMap = new TreeMap<Integer, PusherGroup>();
    sortedPushers = new TreeSet<PixelPusher>(new DefaultPusherComparator());
    pusherLastSeenMap = new HashMap<String, DateTime>();
    udp.setReceiveHandler("receive");
    udp.log(false);
    udp.listen(true);
    this.expiryTimer = new Timer();
    this.expiryTimer.scheduleAtFixedRate(new DeviceExpiryTask(this), 0L,
        EXPIRY_TIMER_MSEC);
    this.sceneThread = new SceneThread();
    this.addObserver(this.sceneThread);
  }

  public void expireDevice(String macAddr) {
    LOGGER.info("Device gone: " + macAddr);
    PixelPusher pusher = pusherMap.remove(macAddr);
    pusherLastSeenMap.remove(macAddr);
    sortedPushers.remove(pusher);
    this.groupMap.get(pusher.getGroupOrdinal()).removePusher(pusher);
    if (sceneThread.isRunning())
      sceneThread.removePusherThread(pusher);
    this.setChanged();
    this.notifyObservers(); 
  }

  public void setStripValues(String macAddress, int stripNumber, Pixel[] pixels) {
    this.pusherMap.get(macAddress).setStripValues(stripNumber, pixels);

  }

  public void startPushing() {
    if (!sceneThread.isRunning()) {
      sceneThread.run();
    }
  }

  public void stopPushing() {
    if (sceneThread.isRunning()) {
      sceneThread.cancel();
    }
  }

  public void receive(byte[] data) {
    // This is for the UDP callback, this should not be called directly
    DeviceHeader header = new DeviceHeader(data);
    String macAddr = header.GetMacAddressString();
    if (header.DeviceType != DeviceType.PIXELPUSHER) {
      LOGGER.fine("Ignoring non-PixelPusher discovery packet from "
          + header.toString());
      return;
    }
    PixelPusher device = new PixelPusher(header.PacketRemainder, header);
    // Set the timestamp for the last time this device checked in
    pusherLastSeenMap.put(macAddr, DateTime.now());
    if (!pusherMap.containsKey(macAddr)) {
      // We haven't seen this device before
      addNewPusher(macAddr, device);
    } else {
      if (!pusherMap.get(macAddr).equals(device)) { // we already saw it
        updatePusher(macAddr, device);
      } else {
        // The device is identical, nothing has changed
        LOGGER.fine("Device still present: " + macAddr);
        // if we dropped more than occasional packets, slow down a little
        if (device.getDeltaSequence() > 3)
            pusherMap.get(macAddr).increaseExtraDelay(5);
        if (device.getDeltaSequence() < 1)
            pusherMap.get(macAddr).decreaseExtraDelay(1);
        System.out.println(device.toString());
      }
    }
    
    // update the power limit variables
    if (totalPowerLimit > 0) {
      totalPower = 0;
      for (Iterator<PixelPusher> iterator = sortedPushers.iterator(); iterator
          .hasNext();) {
        PixelPusher pusher = iterator.next();
        totalPower += pusher.getPowerTotal();
      }
      if (totalPower > totalPowerLimit) {
        powerScale = totalPowerLimit / totalPower;
      } else {
        powerScale = 1.0;
      }
    }
  }

  private void updatePusher(String macAddr, PixelPusher device) {
    // We already knew about this device at the given MAC, but its details
    // have changed
    LOGGER.info("Device changed: " + macAddr);
    pusherMap.get(macAddr).copyHeader(device);
    
    this.setChanged();
    this.notifyObservers(device);
  }

  private void addNewPusher(String macAddr, PixelPusher pusher) {
    LOGGER.info("New device: " + macAddr +" has group ordinal "+ pusher.getGroupOrdinal());
    pusherMap.put(macAddr, pusher);
    LOGGER.info("Adding to sorted list");
    sortedPushers.add(pusher);
    LOGGER.info("Adding to group map");
    if (groupMap.get(pusher.getGroupOrdinal()) != null) {
      LOGGER.info("Adding pusher to group "+pusher.getGroupOrdinal());
      groupMap.get(pusher.getGroupOrdinal()).addPusher(pusher);
    } else {
      // we need to create a PusherGroup since it doesn't exist yet.
      PusherGroup pg = new PusherGroup();
      LOGGER.info("Creating group and adding pusher to group "+pusher.getGroupOrdinal());
      pg.addPusher(pusher);
      groupMap.put(pusher.getGroupOrdinal(), pg); 
    }
    pusher.setAutoThrottle(autoThrottle);
    LOGGER.info("Notifying observers");
    this.setChanged();
    this.notifyObservers(pusher);
  }

}
