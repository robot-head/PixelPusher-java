package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.heroicrobot.dropbit.common.ByteUtils;
import com.heroicrobot.dropbit.devices.DeviceImpl;
import com.heroicrobot.dropbit.discovery.DeviceHeader;

public class PixelPusher extends DeviceImpl
  implements java.lang.Comparable<PixelPusher> {
  private static final int ACCEPTABLE_LOWEST_SW_REV = 121;
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

  /**
   * All access (including iteration) and mutation must be performed
   * while holding stripLock
   */
  private List<Strip> strips;
  private final Object stripLock = new Object();
  long extraDelayMsec = 0;
  boolean autothrottle = false;
  
  boolean multicast = false;
  boolean multicastPrimary = false;
  
  /**
   * Queue for commands using the new majik strip protocol.
   */
  
  ArrayBlockingQueue<PusherCommand> commandQueue;
  
  final int SFLAG_RGBOW = 1;
  final int SFLAG_WIDEPIXELS = (1<<1);
  final int SFLAG_LOGARITHMIC = (1<<2);
  final int SFLAG_MOTION = (1<<3);
  final int SFLAG_NOTIDEMPOTENT = (1<<4);
  final int SFLAG_BRIGHTNESS = (1<<5);
  final int SFLAG_MONOCHROME = (1<<6);
  
  final int PFLAG_PROTECTED = (1<<0);
  final int PFLAG_FIXEDSIZE = (1<<1);
  final int PFLAG_GLOBALBRIGHTNESS = (1<<2);
  final int PFLAG_STRIPBRIGHTNESS = (1<<3);
  final int PFLAG_MONOCHROME_NOT_PACKED = (1<<4);

  int artnet_universe = 0;
  int artnet_channel = 0;
  int my_port = 9798;
  int stripsAttached = 0;
  int pixelsPerStrip = 0;

  /**
   * @return the my_port
   */
  public int getPort() {
    if (my_port > 0)
      return my_port;

    return 9897;
  }

  /**
   * @param my_port the my_port to set
   */
  public void setPort(int my_port) {
    this.my_port = my_port;
  }

  public void sendCommand(PusherCommand pc) {
    commandQueue.add(pc);
  }
  
  synchronized void doDeferredStripCreation() {
    synchronized (stripLock) {
      this.strips = new CopyOnWriteArrayList<Strip>();
      for (int stripNo = 0; stripNo < stripsAttached; stripNo++) {
        this.strips.add(new Strip(this, stripNo, pixelsPerStrip));
      }
      for (Strip strip: this.strips) {
        if ((stripFlags[strip.getStripNumber()] & SFLAG_LOGARITHMIC) != 0) {
          strip.useAntiLog(false);
        } else {
          strip.useAntiLog(useAntiLog);
        }
        if ((stripFlags[strip.getStripNumber()] & SFLAG_MOTION) != 0) {
          strip.setMotion(true);
        } else {
          strip.setMotion(false);
        }
        if ((stripFlags[strip.getStripNumber()] & SFLAG_NOTIDEMPOTENT) != 0) {
          strip.setNotIdempotent(true);
        } else {
          strip.setNotIdempotent(false);
        }
        if ((stripFlags[strip.getStripNumber()] & SFLAG_BRIGHTNESS) != 0) {
          strip.setHasBrightness(true);
        } else {
          strip.setHasBrightness(false);
        }
        if ((stripFlags[strip.getStripNumber()] & SFLAG_MONOCHROME) != 0) {
          strip.setIsMonochrome(true);
        } else {
          strip.setIsMonochrome(false);
        }     
        strip.setRGBOW((stripFlags[strip.getStripNumber()] & SFLAG_RGBOW) == 1);
      }
      touchedStrips = false;
    }
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
    // Devices that are members of a multicast group,
    // but which are not the primary member of that group,
    // do not return strips.
    if (multicast) {
      if (!multicastPrimary) {
        return new ArrayList<Strip>();
      }
    }
    synchronized (stripLock) {
      if (strips == null) {
        doDeferredStripCreation();
      }
      // Ensure callers can't modify the returned list
      return Collections.unmodifiableList(strips);
    }
  }

  public int getArtnetUniverse() {
    return artnet_universe;
  }

  public int getArtnetChannel() {
    return artnet_channel;
  }

  public Strip getStrip(int stripNumber) {
    if (stripNumber > stripsAttached)
       return null;
    synchronized (stripLock) {
      if (strips == null) {
        doDeferredStripCreation();
      }
      return this.strips.get(stripNumber);
    }
  }

  public void setAutoThrottle(boolean state) {
    autothrottle = state;
   // System.err.println("Setting autothrottle on card "+controllerOrdinal+" in group "+groupOrdinal+" to "+
   //     (autothrottle?"ON":"OFF"));
  }

  /**
   * @return the maxStripsPerPacket
   */
  public int getMaxStripsPerPacket() {
    return maxStripsPerPacket;
  }

  /**
   * @return the pixelsPerStrip
   */
  public int getPixelsPerStrip() {
    return pixelsPerStrip;
  }

  public long getPusherFlags() {
    return pusherFlags;
  }

  public void setPusherFlags(long pusherFlags) {
    this.pusherFlags = pusherFlags;
  }

  /**
   * @return the updatePeriod
   */
  public long getUpdatePeriod() {
    return updatePeriod;
  }

  /**
   * @return the powerTotal
   */
  public long getPowerTotal() {
    return powerTotal;
  }

  public long getDeltaSequence() {
    return deltaSequence;
  }
  public void increaseExtraDelay(long i) {
    if (autothrottle) {
      extraDelayMsec += i;
      System.err.println("Group "+groupOrdinal+" card "+controllerOrdinal+" extra delay now "+extraDelayMsec);
    } else {
      System.err.println("Group "+groupOrdinal+" card "+controllerOrdinal+" would increase delay, but autothrottle is disabled.");
    }
  }

  public void decreaseExtraDelay(long i) {
    extraDelayMsec -= i;
    if (extraDelayMsec < 0)
       extraDelayMsec = 0;
  }
  public long getExtraDelay() {
    if (autothrottle)
      return extraDelayMsec;
    else
      return 0;
  }
  public void setExtraDelay(long i) {
    extraDelayMsec = i;
  }
  public int getControllerOrdinal() {
      return controllerOrdinal;
  }

  public int getGroupOrdinal() {
    return groupOrdinal;
  }

  private boolean touchedStrips;
  private int maxStripsPerPacket;
  private long updatePeriod;
  private long powerTotal;
  private long deltaSequence;
  private int controllerOrdinal;
  private int groupOrdinal;
  private boolean useAntiLog;
  private String filename;
  private boolean amRecording;
  private boolean isBusy;
  private byte[] stripFlags;
  private long pusherFlags;
  private long segments;
  private long powerDomain;
  private int lastUniverse;
  

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
    
    commandQueue = new ArrayBlockingQueue<PusherCommand>(3);
    
    if (super.getSoftwareRevision() < ACCEPTABLE_LOWEST_SW_REV) {
       System.err.println("WARNING!  This PixelPusher Library requires firmware revision "+ACCEPTABLE_LOWEST_SW_REV/100.0);
       System.err.println("WARNING!  This PixelPusher is using "+super.getSoftwareRevision()/100.0);
       System.err.println("WARNING!  This is not expected to work.  Please update your PixelPusher.");
    }
    if (packet.length < 28) {
      throw new IllegalArgumentException();
    }
    setPusherFlags(0);
    segments=0;
    powerDomain=0;
    
    stripsAttached = ByteUtils.unsignedCharToInt(Arrays.copyOfRange(packet, 0, 1));
    pixelsPerStrip = ByteUtils.unsignedShortToInt(Arrays.copyOfRange(packet, 2, 4));
    maxStripsPerPacket = ByteUtils.unsignedCharToInt(Arrays.copyOfRange(packet, 1, 2));

    updatePeriod = ByteUtils
        .unsignedIntToLong(Arrays.copyOfRange(packet, 4, 8));
    powerTotal = ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 8, 12));
    deltaSequence = ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 12, 16));
    controllerOrdinal = (int) ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 16, 20));
    groupOrdinal = (int) ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 20, 24));

    artnet_universe = (int) ByteUtils.unsignedShortToInt(Arrays.copyOfRange(packet, 24, 26));
    artnet_channel = (int) ByteUtils.unsignedShortToInt(Arrays.copyOfRange(packet, 26, 28));
    amRecording = false;
    setPusherFlags(0);

    if (packet.length > 28 && super.getSoftwareRevision() > 100) {
      my_port = (int) ByteUtils.unsignedShortToInt(Arrays.copyOfRange(packet, 28, 30));
    } else {
      my_port = 9798;
    }
    // A minor complication here.  The PixelPusher firmware generates announce packets from
    // a static structure, so the size of stripFlags is always 8;  even if there are fewer
    // strips configured.  So we have a wart. - jls.
    
    int stripFlagSize = 8;
    if (stripsAttached>8)
        stripFlagSize = stripsAttached;
    
    if (packet.length > 30 && super.getSoftwareRevision() > 108) {
      stripFlags = Arrays.copyOfRange(packet, 32, 32+stripFlagSize);
    } else {
      stripFlags = new byte[stripFlagSize];
      for (int i=0; i<stripFlagSize; i++)
        stripFlags[i]=0;
    }
    
    
    /*
     * We have some entries that come after the per-strip flag array.
     * We represent these as longs so that the entire range of a uint may be preserved;
     * why on earth Java doesn't have unsigned ints I have no idea. - jls
     * 
     * uint32_t pusher_flags;      // flags for the whole pusher
     * uint32_t segments;          // number of segments in each strip
     * uint32_t power_domain;      // power domain of this pusher
     */
    
    if (packet.length > 32+stripFlagSize && super.getSoftwareRevision() > 116) {
      setPusherFlags(ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 34+stripFlagSize, 38+stripFlagSize)));
      segments = ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 38+stripFlagSize, 42+stripFlagSize));
      powerDomain = ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 42+stripFlagSize, 46+stripFlagSize));
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

    // we should update every time the power total changes significantly
    if (Math.abs(this.powerTotal - other.powerTotal) > 10000)
      return false;

    // handle the case where our power domain changed
    if (this.powerDomain != other.powerDomain)
      return false;
    
    // ditto for number of segments and pusherFlags
    if (this.segments != other.segments)
      return false;   
    
    if (this.getPusherFlags() != other.getPusherFlags())
      return false;
    
    // if all those other things are the same, then we call it good.
    return true;
  }

  private boolean hasRGBOW() {
    synchronized (stripLock) {
      if (strips != null) {
        for (Strip strip: this.strips)
          if (strip.getRGBOW())
            return true;        
      }
    }
    return false;
  }

  private String formattedStripFlags() {
    StringBuffer s = new StringBuffer();
    
    for (int i = 0; i<stripsAttached; i++)
      s.append("["+stripFlags[i]+"]");
    return new String(s);
  }
  
  public String toString() {
    return super.toString() + " # Strips(" + getNumberOfStrips()
        + ") Max Strips Per Packet(" + maxStripsPerPacket
        + ") PixelsPerStrip (" + getPixelsPerStrip() +") Update Period ("
        + updatePeriod + ") Power Total (" + powerTotal + ") Delta Sequence ( "
        + deltaSequence + ") Group (" +groupOrdinal +") Controller ("
        + controllerOrdinal + " ) + Port ("+my_port+") Art-Net Universe ("
        +artnet_universe+") Art-Net Channel ("+artnet_channel+")" 
        + " Strip flags "+formattedStripFlags()+" Pusher Flags ("+ getPusherFlags()
        +") Segments (" + segments +") Power Domain ("+ powerDomain + ")" 
        + (multicast?" Multicast ":" Unicast ") + (multicastPrimary?"Primary":"Stooge");
  }

  public void updateVariables(PixelPusher device) {
    this.deltaSequence = device.deltaSequence;
    this.maxStripsPerPacket = device.maxStripsPerPacket;
    this.powerTotal = device.powerTotal;
    this.updatePeriod = device.updatePeriod;
  }
  
  public void copyHeader(PixelPusher device) {
    this.controllerOrdinal = device.controllerOrdinal;
    this.deltaSequence = device.deltaSequence;
    this.groupOrdinal = device.groupOrdinal;
    this.maxStripsPerPacket = device.maxStripsPerPacket;

    this.powerTotal = device.powerTotal;
    this.updatePeriod = device.updatePeriod;
    this.artnet_channel = device.artnet_channel;
    this.artnet_universe = device.artnet_universe;
    this.my_port = device.my_port;
    this.filename = device.filename;
    this.amRecording = device.amRecording;
    
    this.setPusherFlags(device.getPusherFlags());
    this.powerDomain = device.powerDomain;

    synchronized (stripLock) {
      // if the number of strips we have doesn't match,
      // we'll need to make a fresh set.
      if (this.stripsAttached != device.stripsAttached) {
        this.strips = null;
        this.stripsAttached = device.stripsAttached;
      }
      // likewise, if the length of each strip differs,
      // we will need to make a new set.
      if (this.pixelsPerStrip != device.pixelsPerStrip) {
        this.pixelsPerStrip = device.pixelsPerStrip;
        this.strips = null;
      }
      // and it's the same for segments
      if (this.segments != device.segments) {
        this.segments = device.segments;
        this.strips = null;
      }
      if (this.strips != null)
        for (Strip s: this.strips)
          s.setPusher(this);   
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
    useAntiLog = antiLog;
    synchronized (stripLock) {
      if (strips == null) {
        doDeferredStripCreation();
        for (Strip strip: this.strips)
          strip.useAntiLog(useAntiLog);
      }
    }
  }

  public void startRecording(String filename) {
        amRecording = true;
        setFilename(filename);
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public boolean isAmRecording() {
    return amRecording;
  }

  public void setAmRecording(boolean amRecording) {
    this.amRecording = amRecording;
  }

  public synchronized void makeBusy() {
    isBusy = true;
  }

  public synchronized void clearBusy() {
    isBusy = false;
  }

  public synchronized boolean isBusy() {
    return isBusy;
  }

  public boolean hasTouchedStrips() {
    if (touchedStrips)
      return true;

    touchedStrips = false;
    return false;
  }
  
  public void markUntouched() {
    touchedStrips = false;
  }
  
  public void markTouched() {
    touchedStrips = true;
  }
  
  public List<Strip> getTouchedStrips() {
    synchronized (stripLock) {
      if (strips == null) {
        doDeferredStripCreation();
      }
      List<Strip>touchedStrips = new CopyOnWriteArrayList<Strip>(strips);
      for (Strip strip: strips)
        if (!strip.isTouched())
          touchedStrips.remove(strip);

      return touchedStrips;
    }
  }

  public long getPowerDomain() {
    return powerDomain;
  }

  public void shutDown() {
    synchronized (stripLock) {
      clearBusy();
    }
  }

  public boolean isMulticast() {
    return multicast;
  }

  public boolean isMulticastPrimary() {
    return multicastPrimary;
  }

  public void setMulticastPrimary(boolean b) {
    multicastPrimary = b;
  }

  public void setMulticast(boolean b) {
    multicast = b;
  }

  public void setLastUniverse(int universe) {
    this.lastUniverse = universe; 
  }

  public int getLastUniverse() {
    return this.lastUniverse;
  }
}
