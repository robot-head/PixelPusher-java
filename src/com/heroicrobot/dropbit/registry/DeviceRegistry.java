package com.heroicrobot.dropbit.registry;

import java.io.IOException;
import java.util.concurrent.*;

import java.net.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
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

public class DeviceRegistry extends Observable {

  private final static Logger LOGGER = Logger.getLogger(DeviceRegistry.class
      .getName());

  
  private static Semaphore updateLock;
  private static int DISCOVERY_PORT = 7331;
  private static int MAX_DISCONNECT_SECONDS = 10;
  private static long EXPIRY_TIMER_MSEC = 1000L;
  private DiscoveryListenerThread _dlt;
  
  private static long totalPower = 0;
  private static long totalPowerLimit = -1;
  private static double powerScale = 1.0;
  private static boolean autoThrottle = false;
  private static boolean AntiLog = false;
  private static boolean logEnabled = true;
  private static int frameLimit = 85;
  private boolean expiryEnabled = true;
  
  private Map<String, PixelPusher> pusherMap;
  private Map<String, DateTime> pusherLastSeenMap;

  private Timer expiryTimer;

  private SceneThread sceneThread;

  private TreeMap<Integer, PusherGroup> groupMap;

  private TreeSet<PixelPusher> sortedPushers;

  public void setLogging(boolean b) {
    logEnabled = b;
  }
  
  public Set<Long> getPowerDomains() {
    return sceneThread.getPowerDomains();
  }
  
  public void enableExpiry() {
    expiryEnabled = true;
  }
  
  public void disableExpiry() {
   expiryEnabled = false; 
  }
  
  public void setFrameLimit(int fl) {
    frameLimit = fl;
  }
  
  public int getFrameLimit() {
    return frameLimit;
  }
  
  public boolean getLogging() {
    return logEnabled;
  }
  
  public void startDatRecording(String filename, int group, int pusher) {
    List<PixelPusher> pushers = getPushers(group);
    (pushers.get(pusher)).startRecording(filename);
  }
  
  public Map<String, PixelPusher> getPusherMap() {
    return pusherMap;
  }

  public void setAntiLog(boolean useAntiLog) {
    AntiLog = useAntiLog;
    sceneThread.useAntiLog(AntiLog);
  }
  
  public void setExtraDelay(int msec) {
    sceneThread.setExtraDelay(msec);
  }
  
  public void stopFrameCallback() {
    sceneThread.stopFrameCallback();
  }
  
  public void setFrameCallback(Object caller, String method) {
    sceneThread.setFrameCallback(caller, method);
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
    updateLock.acquireUninterruptibly();
    for (PixelPusher p : this.sortedPushers) {
      strips.addAll(p.getStrips());
    }
    updateLock.release();
    return strips;
  }
  
  public List<PixelPusher> getPushers() {
    List<PixelPusher> pushers = new ArrayList<PixelPusher>();
    for (PixelPusher p : this.sortedPushers)
        pushers.add(p);
    
    return pushers;
  }
  
  public List<PixelPusher> getPushers(int groupNumber) {
    updateLock.acquireUninterruptibly();
    List<PixelPusher> pushers = new ArrayList<PixelPusher>();
    for (PixelPusher p : this.sortedPushers)
        if (p.getGroupOrdinal() == groupNumber)
          pushers.add(p);
    updateLock.release();
    return pushers;
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
      updateLock.acquireUninterruptibly();
      if (logEnabled) 
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
          if (expiryEnabled)
            toKill.add(deviceMac);
          else
            System.out.println("Would expire "+deviceMac+" but expiry is disabled.");
        }
      }
      for (String doomedIndividual : toKill) {
        registry.expireDevice(doomedIndividual);
      }
      updateLock.release();
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
  
  private class DiscoveryListenerThread extends Thread {
    private DatagramSocket discovery_socket;
    private DatagramPacket discovery_buffer;
    private DeviceRegistry _dr;
   
    DiscoveryListenerThread(int discovery_port, DeviceRegistry dr) {
     super("PixelPusher Discovery Listener");
     try {

       
       this.discovery_socket = new DatagramSocket(null);
       
       this.discovery_socket.setReuseAddress(true);
       this.discovery_socket.setBroadcast(true);
       
       this.discovery_socket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), discovery_port));
       if (logEnabled)
         System.out.println("Listening for PixelPusher announcements on " + this.discovery_socket.getLocalAddress() + " port "
           + this.discovery_socket.getLocalPort() + ", broadcast=" + this.discovery_socket.getBroadcast());
       
       } catch (SocketException e) {
         e.printStackTrace();
       } catch (UnknownHostException e) {
         System.err.println("For some reason, could not resolve 0.0.0.0.");
         e.printStackTrace();
      }
      byte[] buf = new byte[1536];
      this.discovery_buffer = new DatagramPacket(buf, buf.length);
      this._dr = dr;
    }
    
    public void run() {
      while (true) {
        try {
          discovery_socket.receive(discovery_buffer);
        } catch (IOException e) {
          e.printStackTrace();
        }
        _dr.receive(discovery_buffer.getData());
      }
    }
  }

  public DeviceRegistry() {
    updateLock = new Semaphore(1);
    pusherMap = new TreeMap<String, PixelPusher>();
    groupMap = new TreeMap<Integer, PusherGroup>();
    sortedPushers = new TreeSet<PixelPusher>(new DefaultPusherComparator());
    pusherLastSeenMap = new HashMap<String, DateTime>();

    this._dlt = new DiscoveryListenerThread(DISCOVERY_PORT, this);
    this.expiryTimer = new Timer();
    this.expiryTimer.scheduleAtFixedRate(new DeviceExpiryTask(this), 0L,
        EXPIRY_TIMER_MSEC);
    this.sceneThread = new SceneThread();
    this.addObserver(this.sceneThread);
    this._dlt.start();
  }

  public void expireDevice(String macAddr) {
    if (logEnabled)
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
      sceneThread.start();
    }
  }

  public void stopPushing() {
    if (sceneThread.isRunning()) {
      sceneThread.cancel();
    }
  }

  synchronized public void receive(byte[] data) {
    // This is for the UDP callback, this should not be called directly
    updateLock.acquireUninterruptibly();
    DeviceHeader header = new DeviceHeader(data);
    String macAddr = header.GetMacAddressString();
    if (header.DeviceType != DeviceType.PIXELPUSHER) {
      if (logEnabled)
        LOGGER.fine("Ignoring non-PixelPusher discovery packet from "
          + header.toString());
      return;
    }
    PixelPusher device = new PixelPusher(header.PacketRemainder, header);
    device.setAntiLog(AntiLog);
    
    // Set the timestamp for the last time this device checked in
    pusherLastSeenMap.put(macAddr, DateTime.now());
    if (!pusherMap.containsKey(macAddr)) {
      // We haven't seen this device before
      addNewPusher(macAddr, device);
    } else {
      if (!pusherMap.get(macAddr).equals(device)) { // we already saw it but it's changed.
        updatePusher(macAddr, device);
      } else {
        // The device is identical, nothing important has changed
        if (logEnabled) {
          LOGGER.fine("Device still present: " + macAddr);
          System.out.println("Updating pusher from bcast.");
        }
        pusherMap.get(macAddr).updateVariables(device);
        // if we dropped more than occasional packets, slow down a little
        if (device.getDeltaSequence() > 3)
            pusherMap.get(macAddr).increaseExtraDelay(5);
        if (device.getDeltaSequence() < 1)
            pusherMap.get(macAddr).decreaseExtraDelay(1);
        if (logEnabled)
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
    updateLock.release();
  }

  private void updatePusher(String macAddr, PixelPusher device) {
    // We already knew about this device at the given MAC, but its details
    // have changed
    if (logEnabled)
      LOGGER.info("Device changed: " + macAddr);
    pusherMap.get(macAddr).copyHeader(device);
    
    this.setChanged();
    this.notifyObservers(device);
  }

  private void addNewPusher(String macAddr, PixelPusher pusher) {
    if (logEnabled)
      LOGGER.info("New device: " + macAddr +" has group ordinal "+ pusher.getGroupOrdinal());
    pusherMap.put(macAddr, pusher);
    if (logEnabled)
      LOGGER.info("Adding to sorted list");
    sortedPushers.add(pusher);
    if (logEnabled)
      LOGGER.info("Adding to group map");
    if (groupMap.get(pusher.getGroupOrdinal()) != null) {
      if (logEnabled)
        LOGGER.info("Adding pusher to group "+pusher.getGroupOrdinal());
      groupMap.get(pusher.getGroupOrdinal()).addPusher(pusher);
    } else {
      // we need to create a PusherGroup since it doesn't exist yet.
      PusherGroup pg = new PusherGroup();
      if (logEnabled)
        LOGGER.info("Creating group and adding pusher to group "+pusher.getGroupOrdinal());
      pg.addPusher(pusher);
      groupMap.put(pusher.getGroupOrdinal(), pg); 
    }
    pusher.setAutoThrottle(autoThrottle);
    if (logEnabled)
      LOGGER.info("Notifying observers");
    this.setChanged();
    this.notifyObservers(pusher);
  }
}
