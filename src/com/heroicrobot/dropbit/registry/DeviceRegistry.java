package com.heroicrobot.dropbit.registry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
// import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import com.heroicrobot.dropbit.devices.pixelpusher.Pixel;
import com.heroicrobot.dropbit.devices.pixelpusher.PixelPusher;
import com.heroicrobot.dropbit.devices.pixelpusher.PusherGroup;
import com.heroicrobot.dropbit.devices.pixelpusher.SceneThread;
import com.heroicrobot.dropbit.devices.pixelpusher.Strip;
import com.heroicrobot.dropbit.discovery.DeviceHeader;
import com.heroicrobot.dropbit.discovery.DeviceType;

public final class DeviceRegistry extends Observable {

  private final static Logger LOGGER = Logger.getLogger(DeviceRegistry.class
      .getName());

  
  private static Semaphore updateLock;
  private static int DISCOVERY_PORT = 7331;
  private static int MAX_DISCONNECT_SECONDS = 10;
  private static long EXPIRY_TIMER_MSEC = 1000L;
  private DiscoveryListenerThread _dlt;
  
  private static double overallBrightnessScale = 1.0;
  
  /**
   * Boolean flag to use or not the overall brightness.
   * @see setOverallBrightnessScale
   * @see getOverallBrightnessScale
   */
  public static boolean useOverallBrightnessScale = false;
  
  private static long totalPower = 0;
  private static long totalPowerLimit = -1;
  private static double powerScale = 1.0;
  private static boolean autoThrottle = false;
  private static boolean AntiLog = false;
  private static boolean logEnabled = true;
  private static int frameLimit = 85;
  private static Boolean hasDiscoveryListener = false;
  private static Boolean alreadyExist = false;
  private boolean expiryEnabled = true;
  
  private Map<String, PixelPusher> pusherMap;
  private Map<String, Long> pusherLastSeenMap;

  private Timer expiryTimer;

  private SceneThread sceneThread;

  private TreeMap<Integer, PusherGroup> groupMap;

  private TreeSet<PixelPusher> sortedPushers;


  public Boolean hasDeviceExpiryTask=false;

  /**
   * Enable / disable logging
   * @param b
   */
  public void setLogging(boolean b) {
    logEnabled = b;
  }
  
 /* public Set<Long> getPowerDomains() {
    return sceneThread.getPowerDomains();
  } */
  
  /**
   * Enable expiry.
   * <p>
   * The system will automatically forget devices that are not seen for a while. 
   */
  public void enableExpiry() {
    expiryEnabled = true;
  }
  
  /**
   * Disable expiry.
   * <p>
   * @see enableExpiry
   * This may help with some unstable network but this is intended for debugging only.
   */
  public void disableExpiry() {
   expiryEnabled = false; 
  }
  
  /**
   * Set the maximal number of frames per second in Hz.
   * @param fl Framerate limit in Hz.
   */
  public void setFrameLimit(int fl) {
    frameLimit = fl;
  }
  
  /**
   * Get the current max frame frequency in Hz.
   * @see setFrameLimit
   * @return Current frame frequency limit in Hz.
   */
  public int getFrameLimit() {
    return frameLimit;
  }
  
  /**
   * Get the logging status
   * @return
   */
  public boolean getLogging() {
    return logEnabled;
  }
  
  /**
   * Start recording data.
   * <p>
   * The first parameter is the group number of the pusher you want to record. 
   * The second parameter is the pusher's position within that group (they start from zero).
   * This will put a file named canned.dat somewhere on your computer.
   * The place it goes will depend upon what machine you're using. 
   * On mine, it shows up next to the Processing application. 
   * 
   * The generated file will be specific to the number of pixels configured on that pusher and also to the pixel format.
   * It won't play back properly on pushers with different length strips.
   * 
   * If you want to record several different pushers at once, you can add as many calls to startDatRecording() with different filenames as you like.
   * Note, though, that they may not stay synchronized over a long period of time since they won't talk to each other. 
   * You need to rename the files to canned.dat when you copy them to the USB stick, of course, otherwise the PixelPusher won't know to look for them.
   * 
   * Add note that this code records Format 2 canned.dat files, and requires a recent PixelPusher firmware.
   * 
   * @param filename
   * @param group
   * @param pusher
   */
  public void startDatRecording(String filename, int group, int pusher) {
    List<PixelPusher> pushers = getPushers(group);
    (pushers.get(pusher)).startRecording(filename);
  }
  
  /**
   * Get ordered Map of pushers
   * @return Map<String, PixelPusher> pusheMap
   */
  public Map<String, PixelPusher> getPusherMap() {
    return pusherMap;
  }
  
  /**
   * Set to true to use the luminance anti-log curve. This is useful to improve video color rendering.
   * @param useAntiLog
   */

  public void setAntiLog(boolean useAntiLog) {
    AntiLog = useAntiLog;
    sceneThread.useAntiLog(AntiLog);
  }
  
  /**
   * Adds an extra delay to every packet that is sent by the registry’s worker threads. 
   * <p>
   * Normally, the registry keeps track of how many packets are being lost
   * (they get dropped if there isn’t enough bandwidth or if you’re using a wireless network and there’s noise) 
   * and how long each PixelPusher is saying it takes to push out a complete packet to the strips, 
   * and adjusts the rate at which packets are sent automatically.  
   * However, if you’re on a very slow network, like a very long wireless link or a cellular modem, 
   * you might want to add an extra slowdown.  That’s what the registry.setExtraDelay() method is for.
   * <p>
   * This adds an extra delay to every packet that is sent by the registry’s worker threads.  
   * If you set it to zero, only the standard rate limiting applies.  
   * With full-length Heroic Robotics strips and the default update rate, 
   * each PixelPusher consumes between 5 and 10 megabits per second of bandwidth. 
   * In the special turbo mode, a PixelPusher with two strips running at maximum speed will consume about 52 megabits per second of bandwidth.
   * @param msec
   */
  
  public void setExtraDelay(int msec) {
    sceneThread.setExtraDelay(msec);
  }
  
  /**
   * Disable the frame callback set with @see setFrameCallback
   */
  public void stopFrameCallback() {
    sceneThread.stopFrameCallback();
  }
  
  /**
   * Register an object's method to be called each time the strips are clean.
   * 
   * This is useful to sync data generation with framerate.
   * @param caller
   * @param method
   */
  public void setFrameCallback(Object caller, String method) {
    sceneThread.setFrameCallback(caller, method);
  }
  
  /**
   * Set auto-throttling ON or OFF.
   * 
   * If you’re on a network with a high error rate, like some wireless networks, or poorly installed ethernet, 
   * you might find that you get persistently high error rates and the update frequency drops uncontrollably.  
   * In this case, you may want to disable the autothrottling entirely.
   * @param autothrottle
   */
  public void setAutoThrottle(boolean autothrottle) {
    autoThrottle = autothrottle;
    sceneThread.setAutoThrottle(autothrottle);
  }

  /**
   * Return the total of bandwidth used
   * @return Total bandwidth in bytes per millisecond 
   */
  public long getTotalBandwidth() {
    return sceneThread.getTotalBandwidth();
  }

  /**
   * Return the total amount of power used.
   * 
   * To convert the power in luminance unit to electric power in Watt, you need to know how much power each of your LED consumes.
   * For example, some RGB strips consumes 0.3W per LED, assuming each colour consumes the same amount of electricity at the same luminance level (which is not true),
   * we can approximate the electric power with this formula : 
   * Electric Power = Luminance Power/255 * 0.3W /3 = Luminance Power / 2550
   * @return Total power in luminance unit
   */
  public long getTotalPower() {
    return totalPower;
  }
  
  /**
   * Set the max power. 
   * 
   * The limit is specified in luminance units, which is to say, 255 means one colour of a single pixel turned fully on. 
   * Since the LEDs on the strips we ship are 20 mA per segment, the units are therefore steps of 1/255th of 20 mA 
   * which approximately equals 80 microamps.
   * 
   * @param powerLimit
   */
  public void setTotalPowerLimit(long powerLimit) {
    totalPowerLimit = powerLimit;
  }
  
  /**
   * Get the max power. 
   * 
   * @see setTotalPowerLimit
   * @return Power limit in luminance unit
   */
  public long getTotalPowerLimit() {
    return totalPowerLimit;
  }
  
  /**
   * Get the actual power scale.
   * 
   * The power scale is adjust to avoid total power exceed the power limit set with @see setTotalPowerLimit.
   * It scale the whole display brightness.
   * 
   * @return Power scale
   */
  public double getPowerScale() {
    return powerScale;
  }
  
  /**
   * Get list of all the strips known to the registry.
   * @return List of all strips
   */
  public List<Strip> getStrips() {
    List<Strip> strips = new CopyOnWriteArrayList<Strip>();
    updateLock.acquireUninterruptibly();
    for (PixelPusher p : this.sortedPushers) {
      strips.addAll(p.getStrips());
    }
    updateLock.release();
    return strips;
  }
  
  /**
   * Get the elapsed time since the PixelPusher @param p have been seen for the last time.
   * 
   * @param p
   * @return
   */
  public int lastSeen(PixelPusher p) {
    return (int)(System.nanoTime() - pusherLastSeenMap.get(p.getMacAddress()) ) / 1000000000;
  }
  
  /**
   * Get list of connected PixelPusher boards
   * @return List of PixelPusher
   */
  public List<PixelPusher> getPushers() {
    List<PixelPusher> pushers = new CopyOnWriteArrayList<PixelPusher>();
    for (PixelPusher p : this.sortedPushers)
        pushers.add(p);
    
    return pushers;
  }
  
  /**
   * Get list of connected PixelPusher boards that belongs to provided group
   * @param groupNumber
   * @return List of PixelPusher
   */
  public List<PixelPusher> getPushers(int groupNumber) {
    updateLock.acquireUninterruptibly();
    List<PixelPusher> pushers = new CopyOnWriteArrayList<PixelPusher>();
    for (PixelPusher p : this.sortedPushers)
        if (p.getGroupOrdinal() == groupNumber)
          pushers.add(p);
    updateLock.release();
    return pushers;
  }
  
  /**
   * Get all pushers that match the given IP address
   * @param addr
   * @return List of PixelPusher that match the given IP address
   */
  public List<PixelPusher> getPushers(InetAddress addr) {
    updateLock.acquireUninterruptibly();
    List<PixelPusher> pushers = new CopyOnWriteArrayList<PixelPusher>();
    for (PixelPusher p : this.sortedPushers)
        if (p.getIp().equals(addr))
          pushers.add(p);
    updateLock.release();
    return pushers;
  }
  
  /**
   * Get strips that belong to given group number
   * @param groupNumber
   * @return List of PixelPusher
   */
  public List<Strip> getStrips(int groupNumber) {
    if (this.groupMap.containsKey(groupNumber)) {
      return this.groupMap.get(groupNumber).getStrips();
    } else {
      List<Strip> emptyList = new CopyOnWriteArrayList<Strip>();
      return emptyList;
    }
  }

  class DeviceExpiryTask extends TimerTask {

    private DeviceRegistry registry;

    DeviceExpiryTask(DeviceRegistry registry) {
      if(registry.hasDeviceExpiryTask) {
        System.err.println("Already have a DeviceExpiryTask;  doppelganger terminating.");
        this.registry=null;
      } else {
        this.registry = registry;
        hasDeviceExpiryTask = true;
      }
    }

    @Override
    public void run() {
      if (this.registry == null)
          return;
      
      if (updateLock.tryAcquire()) {
      synchronized(registry.hasDeviceExpiryTask) {
          if (logEnabled) 
            LOGGER.fine("Expiry and preening task running");
          
          // A little sleight of hand here.  We can't call registry.expireDevice()
          // directly from inside the loop, for the loop is an implicit iterator and
          // registry.expireDevice modifies the pusherMap.
          // Instead we create a list of the MAC addresses to kill, then loop over
          // them outside the iterator.  - jls
          List<String> toKill = new ArrayList<String>();
          for (String deviceMac : pusherMap.keySet()) {
            double lastSeenSeconds = 
                 (System.nanoTime() - pusherLastSeenMap.get(deviceMac)) / 1000000000.0;
            if (lastSeenSeconds > MAX_DISCONNECT_SECONDS) {
              if (expiryEnabled) {
                toKill.add(deviceMac);
              } else {
                System.out.println("Would expire "+deviceMac+" but expiry is disabled.");
              }
            }
          }
          for (String doomedIndividual : toKill) {
            registry.expireDevice(doomedIndividual);
          }
          updateLock.release();
          hasDeviceExpiryTask = false;
        }
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
  
  static private class DiscoveryListenerThread extends Thread {
    private DatagramSocket discovery_socket;
    private DatagramPacket discovery_buffer;
    private DeviceRegistry _dr;
   
    @SuppressWarnings("deprecation")
    DiscoveryListenerThread(int discovery_port, DeviceRegistry dr) {
     super("PixelPusher Discovery Listener");
     synchronized(DeviceRegistry.hasDiscoveryListener) {
       if (DeviceRegistry.hasDiscoveryListener) {
         System.err.println("Already have a DiscoveryListener!  Not creating a fresh one.");
         System.err.println("This happens if you call size() anywhere other than the first line of your setup() method in Processing.");
         this.stop();
       }
       DeviceRegistry.hasDiscoveryListener = true;
       System.err.println("Starting a new instance of the discovery listener.");
       for(StackTraceElement ste: this.getStackTrace())
         System.err.println(ste.toString());
       
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
    synchronized(alreadyExist) {
      if (alreadyExist) {
        System.err.println("DeviceRegistry being instantiated for a second time.");
        return;
      }
      alreadyExist = true;
      
      updateLock = new Semaphore(1);
      pusherMap = new TreeMap<String, PixelPusher>();
      groupMap = new TreeMap<Integer, PusherGroup>();
      sortedPushers = new TreeSet<PixelPusher>(new DefaultPusherComparator());
      pusherLastSeenMap = new HashMap<String, Long>();
      System.err.println("Building a new DeviceRegistry.");
      
      this._dlt = new DiscoveryListenerThread(DISCOVERY_PORT, this);
      this.expiryTimer = new Timer();
      this.expiryTimer.scheduleAtFixedRate(new DeviceExpiryTask(this), 0L,
          EXPIRY_TIMER_MSEC);
      this.sceneThread = new SceneThread();
      this.addObserver(this.sceneThread);
      this._dlt.start();
    }
  }

  /**
   * Forget a given device.
   * @param macAddr
   */
  public void expireDevice(String macAddr) {
    if (logEnabled)
      LOGGER.info("Device gone: " + macAddr);
    PixelPusher pusher = pusherMap.remove(macAddr);
    
    // In the case where it is a multicast pusher,
    // and is the primary of its mcast group, we must
    // select a new primary.
    if (pusher.isMulticast()) {
      if (pusher.isMulticastPrimary()) {
        List<PixelPusher> candidates = getPushers(pusher.getIp());
        if (candidates.size() > 0)
           candidates.get(0).setMulticastPrimary(true);
      }
    }
    pusherLastSeenMap.remove(macAddr);
    sortedPushers.remove(pusher);
    this.groupMap.get(pusher.getGroupOrdinal()).removePusher(pusher);
    if (sceneThread.isRunning())
      sceneThread.removePusherThread(pusher);
    this.setChanged();
    this.notifyObservers(); 
  }

  /**
   * Set the value the given strip on the pusher with the given IP to the given values
   * @param macAddress
   * @param stripNumber
   * @param pixels : array of pixels values
   */
  public void setStripValues(String macAddress, int stripNumber, Pixel[] pixels) {
    this.pusherMap.get(macAddress).setStripValues(stripNumber, pixels);
  }

  /**
   * Start pushing pixel to registered boards
   * @see stopPushing
   */
  public void startPushing() {
    if (!sceneThread.isRunning()) {
      sceneThread.start();
    }
  }

  /**
   * Stop talking to PixelPusher boards
   * @see startPushing
   */
  public void stopPushing() {
    if (sceneThread == null) {
      return;
    }
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
    pusherLastSeenMap.put(macAddr, System.nanoTime());
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
    
    if (pusher.getIp().isMulticastAddress()) {
      pusher.setMulticast(true);
      List<PixelPusher> members = getPushers(pusher.getIp());
      boolean groupHasPrimary = false;
      for (PixelPusher p: members) {
        if (p.isMulticastPrimary())
          groupHasPrimary = true;
      }
      if (!groupHasPrimary) {
        pusher.setMulticastPrimary(true);
        if (logEnabled)
          LOGGER.info("Setting pusher "+macAddr +" to multicast primary.");
      }
    }
    
    if (logEnabled)
      LOGGER.info("Notifying observers");
    this.setChanged();
    this.notifyObservers(pusher);
  }

  /**
   * Get the current global brightness scale.
   * @see useOverallBrightnessScale
   * @see setOverallBrightnessScale
   * @return double scale
   */
  public static double getOverallBrightnessScale() {
    return DeviceRegistry.overallBrightnessScale;
  }

  /**
   * Set the global overall brightness.
   * 
   * This is a scale factor. 1. is the default factor, and brightness is full.
   * 0.5 is half the brightness, 2. is the double.
   * The brightness scale is applied after the anti-log correction, preserving low end resolution. 
   * @see useOverallBrightnessScale 
   * @see getOverallBrightnessScale
   * @param double bScale
   */
  public static void setOverallBrightnessScale(double bScale) {
    DeviceRegistry.overallBrightnessScale = bScale;
    DeviceRegistry.useOverallBrightnessScale = true;
  }
}
