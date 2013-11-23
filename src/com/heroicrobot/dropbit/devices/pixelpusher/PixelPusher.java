package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.heroicrobot.dropbit.common.ByteUtils;
import com.heroicrobot.dropbit.devices.DeviceImpl;
import com.heroicrobot.dropbit.discovery.DeviceHeader;

public class PixelPusher extends DeviceImpl
  implements java.lang.Comparable<PixelPusher> {
  private static final int ACCEPTABLE_LOWEST_SW_REV = 112;
  /**
   * uint8_t strips_attached;
   * uint8_t max_strips_per_packet;
   * uint16_t pixels_per_strip; // uint16_t used to make alignment work
   * uint32_t update_period; // in microseconds
   * uint32_t power_total; // in PWM units
   * uint32_t delta_sequence; // difference between received and expected
   * sequence numbers
   * int32_t controller_ordinal;  // configured order number for controller
   * int32_t group_ordinal;  // configured group number for this controller
   * int16_t artnet_universe;
   * int16_t artnet_channel;
   * int16_t my_port;
   */

  /** Guarded by stripLock, all access must synchronize on {@link #stripLock}. */
  private List<Strip> strips;
  private final Object stripLock = new Object();
  
  private final AtomicLong extraDelayMsec = new AtomicLong(0);
  private final AtomicBoolean autothrottle = new AtomicBoolean(false);
  
  private static final int SFLAG_RGBOW = 1;

  private final AtomicInteger artnet_universe = new AtomicInteger(0);
  private final AtomicInteger artnet_channel = new AtomicInteger(0);
  private final AtomicInteger my_port = new AtomicInteger(9798);
  final AtomicInteger stripsAttached = new AtomicInteger(0);
  private final AtomicInteger pixelsPerStrip = new AtomicInteger(0);

  /**
   * @return the my_port
   */
  public int getPort() {
    int port = my_port.get();
    if (port > 0)
      return port;

    return 9897;
  }

  /**
   * @param my_port the my_port to set
   */
  public void setPort(int my_port) {
    this.my_port.set(my_port);
  }

  void doDeferredStripCreation() {
    synchronized (stripLock) {
      this.strips = new ArrayList<Strip>();
      synchronized(stripFlags) {
        for (int stripNo = 0, n = stripsAttached.get(); stripNo < n; stripNo++) {
          Strip strip = new Strip(this, stripNo, pixelsPerStrip.get());
          strip.useAntiLog(useAntiLog.get());
          strip.setRGBOW((stripFlags[strip.getStripNumber()] & SFLAG_RGBOW) == 1);
          this.strips.add(strip);
        }
      }
    }
    touchedStrips.set(false);
  }

  /**
   * @return the stripsAttached
   */
  public int getNumberOfStrips() {
    synchronized (stripLock) {
      if (strips == null) {
        doDeferredStripCreation();
      }
      return strips.size();
    }
  }

  public List<Strip> getStrips() {
    synchronized (stripLock) {
      if (strips == null) {
        doDeferredStripCreation();
      }
      // Return a defensive copy so external code can't mutate the list
      // and doesn't need to iterate whilst locking.
      return new ArrayList<Strip>(strips);
    }
  }

  public int getArtnetUniverse() {
    return artnet_universe.get();
  }

  public int getArtnetChannel() {
    return artnet_channel.get();
  }

  public Strip getStrip(int stripNumber) {
    if (stripNumber > stripsAttached.get())
       return null;
    synchronized (stripLock) {
      if (strips == null) {
        doDeferredStripCreation();
      }
      return this.strips.get(stripNumber);
    }
  }

  public void setAutoThrottle(boolean state) {
    autothrottle.set(state);
   // System.err.println("Setting autothrottle on card "+controllerOrdinal+" in group "+groupOrdinal+" to "+
   //     (autothrottle?"ON":"OFF"));
  }

  /**
   * @return the maxStripsPerPacket
   */
  public int getMaxStripsPerPacket() {
    return maxStripsPerPacket.get();
  }

  /**
   * @return the pixelsPerStrip
   */
  public int getPixelsPerStrip() {
    return pixelsPerStrip.get();
  }

  /**
   * @return the updatePeriod
   */
  public long getUpdatePeriod() {
    return updatePeriod.get();
  }

  /**
   * @return the powerTotal
   */
  public long getPowerTotal() {
    return powerTotal.get();
  }

  public long getDeltaSequence() {
    return deltaSequence.get();
  }
  public void increaseExtraDelay(long i) {
    if (autothrottle.get()) {
      extraDelayMsec.incrementAndGet();
      System.err.println("Group "+groupOrdinal+" card "+controllerOrdinal+" extra delay now "+extraDelayMsec);
    } else {
      System.err.println("Group "+groupOrdinal+" card "+controllerOrdinal+" would increase delay, but autothrottle is disabled.");
    }
  }

  public void decreaseExtraDelay(long i) {
    // TODO this is unsafe in the face of concurrency. This is a
    // compare then act which means the value could have changed
    // between the read and the set. We really need a critical
    // section around the entire get and set, but AtomicLong 
    // only allows equal rather than general inequality
    if (extraDelayMsec.decrementAndGet() < 0)
      extraDelayMsec.set(0);
  }

  public long getExtraDelay() {
    if (autothrottle.get())
      return extraDelayMsec.get();
    else
      return 0;
  }
  
  public void setExtraDelay(long i) {
    extraDelayMsec.set(i);
  }
  
  public int getControllerOrdinal() {
      return controllerOrdinal.get();
  }

  public int getGroupOrdinal() {
    return groupOrdinal.get();
  }

  private final AtomicBoolean touchedStrips = new AtomicBoolean(false);
  private final AtomicInteger maxStripsPerPacket = new AtomicInteger(0);
  private final AtomicLong updatePeriod = new AtomicLong(0);
  private final AtomicLong powerTotal = new AtomicLong(0);
  private final AtomicLong deltaSequence = new AtomicLong(0);
  private final AtomicInteger controllerOrdinal = new AtomicInteger(0);
  private final AtomicInteger groupOrdinal = new AtomicInteger(0);
  private final AtomicBoolean useAntiLog = new AtomicBoolean(false);
  private final AtomicReference<String> filename = new AtomicReference<String>();
  private final AtomicBoolean amRecording = new AtomicBoolean(false);
  private final AtomicBoolean isBusy = new AtomicBoolean(false);
  
  /** Synchronize on stripFlags when iterating or mutating */
  private final byte[] stripFlags;

  public void setStripValues(int stripNumber, Pixel[] pixels) {
    synchronized (stripLock) {
      if (strips == null) {
        doDeferredStripCreation();
      }
      this.strips.get(stripNumber).setPixels(pixels);
    }
  }

  public PixelPusher(byte[] packet, DeviceHeader header) {
    super(header);
    if (super.getSoftwareRevision() < ACCEPTABLE_LOWEST_SW_REV) {
       System.err.println("WARNING!  This PixelPusher Library requires firmware revision "+ACCEPTABLE_LOWEST_SW_REV/100.0);
       System.err.println("WARNING!  This PixelPusher is using "+super.getSoftwareRevision()/100.0);
       System.err.println("WARNING!  This is not expected to work.  Please update your PixelPusher.");
    }
    if (packet.length < 28) {
      throw new IllegalArgumentException();
    }
    
    stripsAttached.set(ByteUtils.unsignedCharToInt(Arrays.copyOfRange(packet, 0, 1)));
    pixelsPerStrip.set(ByteUtils.unsignedShortToInt(Arrays.copyOfRange(packet, 2, 4)));
    maxStripsPerPacket.set(ByteUtils.unsignedCharToInt(Arrays.copyOfRange(packet, 1, 2)));

    updatePeriod.set(ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 4, 8)));
    powerTotal.set(ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 8, 12)));
    deltaSequence.set(ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 12, 16)));
    controllerOrdinal.set((int) ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 16, 20)));
    groupOrdinal.set((int) ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 20, 24)));

    artnet_universe.set((int) ByteUtils.unsignedShortToInt(Arrays.copyOfRange(packet, 24, 26)));
    artnet_channel.set((int) ByteUtils.unsignedShortToInt(Arrays.copyOfRange(packet, 26, 28)));
    amRecording.set(false);

    if (packet.length > 28) {
      my_port.set((int) ByteUtils.unsignedShortToInt(Arrays.copyOfRange(packet, 28, 30)));
    } else {
      my_port.set(9798);
    }
    
    // No need to synchronize as we are in the constructor
    // No other threads can bea ccess stripFlags right now
    if (packet.length > 30) {
      stripFlags = Arrays.copyOfRange(packet, 30, 38);
    } else {
      stripFlags = new byte[8];
      for (int i=0; i<8; i++)
        stripFlags[i]=0;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + getPixelsPerStrip();
    result = prime * result + getNumberOfStrips();
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {

    // quick checks first.

    // object handle identity
    if (this == obj)
      return true;

    // if it's null, it's not the same as anything
    // (and we can't compare its fields without a null pointer exception)
    if (obj == null)
      return false;

    // if it's some different class, well then something is bad.
    if (getClass() != obj.getClass())
      return false;

    // ok so it's the same class. in that case, let's make a reference...
    PixelPusher other = (PixelPusher) obj;

    // if it differs by less than half a msec, it has no effect on our timing
    if (Math.abs(getUpdatePeriod() - other.getUpdatePeriod()) > 500)
       return false;

    // some fudging to cope with the fact that pushers don't know they have RGBOW
    if (this.hasRGBOW() & !other.hasRGBOW()) {
      if (getPixelsPerStrip() != other.getPixelsPerStrip() / 3)
        return false;
    }
     if (!this.hasRGBOW() & other.hasRGBOW()) {
      if (getPixelsPerStrip() / 3 != other.getPixelsPerStrip())
        return false;
    }
    if (! (this.hasRGBOW() || other.hasRGBOW()))
    if (getPixelsPerStrip() != other.getPixelsPerStrip())
      return false;
    if (getNumberOfStrips() != other.getNumberOfStrips())
      return false;

    // handle the case where someone changed the config during library runtime
    if (this.artnet_channel != other.artnet_channel ||
        this.artnet_universe != other.artnet_universe)
       return false;

    // if the port's been changed, we need to update
    if (this.my_port != other.my_port)
      return false;

    // we should update every time the power total changes
    //if (this.powerTotal != other.powerTotal)
    //  return false;

    // if all those other things are the same, then we call it good.
    return true;
  }

  private boolean hasRGBOW() {
    synchronized (stripLock) {
      if (this.strips == null) {
        doDeferredStripCreation();
      }
      for (Strip strip : this.strips) {
        if (strip.getRGBOW()) {
          return true;
        }
      }
      return false;
    }
  }

  private String formattedStripFlags() {
    synchronized (stripFlags) {
      return "["+stripFlags[0]+"]["+stripFlags[1]+"]["+stripFlags[2]+"]["+stripFlags[3]+"]["
          +stripFlags[4]+"]["+stripFlags[5]+"]["+stripFlags[6]+"]["+stripFlags[7]+"]";
    }
  }
  
  public String toString() {
    return super.toString() + " # Strips(" + getNumberOfStrips()
        + ") Max Strips Per Packet(" + maxStripsPerPacket
        + ") PixelsPerStrip (" + getPixelsPerStrip() +") Update Period ("
        + updatePeriod + ") Power Total (" + powerTotal + ") Delta Sequence ( "
        + deltaSequence + ") Group (" +groupOrdinal +") Controller ("
        + controllerOrdinal + " ) + Port ("+my_port+") Art-Net Universe ("
        +artnet_universe+") Art-Net Channel ("+artnet_channel+")" 
        + " Strip flags "+formattedStripFlags();
  }

  public void updateVariables(PixelPusher device) {
    this.deltaSequence.set(device.deltaSequence.get());
    this.maxStripsPerPacket.set(device.maxStripsPerPacket.get());
    this.powerTotal.set(device.powerTotal.get());
    this.updatePeriod.set(device.updatePeriod.get());
  }
  
  public synchronized void copyHeader(PixelPusher device) {
    this.controllerOrdinal.set(device.controllerOrdinal.get());
    this.deltaSequence.set(device.deltaSequence.get());
    this.groupOrdinal.set(device.groupOrdinal.get());
    this.maxStripsPerPacket.set(device.maxStripsPerPacket.get());

    this.powerTotal.set(device.powerTotal.get());
    this.updatePeriod.set(device.updatePeriod.get());
    this.artnet_channel.set(device.artnet_channel.get());
    this.artnet_universe.set(device.artnet_universe.get());
    this.my_port.set(device.my_port.get());
    this.filename.set(device.filename.get());
    this.amRecording.set(device.amRecording.get());

    synchronized (stripLock) {
      
      // if the number of strips we have doesn't match,
      // we'll need to make a fresh set.
      if (this.stripsAttached.get() != device.stripsAttached.get()) {
        this.strips = null;
        this.stripsAttached.set(device.stripsAttached.get());
      }
      // likewise, if the length of each strip differs,
      // we will need to make a new set.
      if (this.pixelsPerStrip != device.pixelsPerStrip) {
        this.pixelsPerStrip.set(device.pixelsPerStrip.get());
        this.strips = null;
      }
  
  
      // if it already has strips, just copy those
      // but we need to synchronize on the other strip's lock
      synchronized (device.stripLock) {
        this.makeBusy();
        try {
          // Take a copy so the original device instance can't 
          // modify this instance's list
          this.strips = new ArrayList<Strip>(device.strips);
        } finally {
          this.clearBusy();
        }
      }  
    }
  }

  @Override
  public int compareTo(PixelPusher comp) {
    int group0 = this.getGroupOrdinal();
    int group1 = ((PixelPusher) comp).getGroupOrdinal();
    if (group0 != group1) {
      if (group0 < group1)
        return -1;
      return 1;
    }
    int ord0 = this.getControllerOrdinal();
    int ord1 = ((PixelPusher) comp).getControllerOrdinal();
    if (ord0 != ord1) {
      if (ord0 < ord1)
        return -1;
      return 1;
    }

    return this.getMacAddress().compareTo(((DeviceImpl) comp).getMacAddress());
  }

  public void setAntiLog(boolean antiLog) {
    synchronized (this.stripLock) {
      useAntiLog.set(antiLog);
      if (this.strips == null) {
        doDeferredStripCreation();
      }
      for (Strip strip : this.strips) {
        strip.useAntiLog(antiLog);
      }
    }
  }

  public void startRecording(String filename) {
        amRecording.set(true);
        setFilename(filename);
  }

  public String getFilename() {
    return filename.get();
  }

  public void setFilename(String filename) {
    this.filename.set(filename);
  }

  public boolean isAmRecording() {
    return amRecording.get();
  }

  public void setAmRecording(boolean amRecording) {
    this.amRecording.set(amRecording);
  }

  public synchronized void makeBusy() {
    isBusy.set(true);
  }

  public synchronized void clearBusy() {
    isBusy.set(false);
  }

  public synchronized boolean isBusy() {
    return isBusy.get();
  }

  public boolean hasTouchedStrips() {
    return touchedStrips.get();
  }
  
  public void markUntouched() {
    touchedStrips.set(false);
  }
  
  public void markTouched() {
    touchedStrips.set(true);
  }
  
  public List<Strip> getTouchedStrips() {
    synchronized (stripLock) {
      if (this.strips == null) {
        doDeferredStripCreation();
      }
      List<Strip> touchedStrips = new ArrayList<Strip>(strips.size());
      for (Strip strip : strips) {
        if (strip.isTouched()) {
          touchedStrips.add(strip);
        }
      }
      return touchedStrips;
    }
  }

}
