package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.Arrays;
import java.util.List;

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

  private List<Strip> strips;
  boolean stripsCreated = false;
  long extraDelayMsec = 0;
  boolean autothrottle = false;
  Semaphore stripLock;
  
  final int SFLAG_RGBOW = 1;

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

  synchronized void doDeferredStripCreation() {
    stripLock.acquireUninterruptibly();
    this.strips = new ArrayList<Strip>();
    for (int stripNo = 0; stripNo < stripsAttached; stripNo++) {
      this.strips.add(new Strip(this, stripNo, pixelsPerStrip));
    }
    for (Strip strip: this.strips) {
      strip.useAntiLog(useAntiLog);
      strip.setRGBOW((stripFlags[strip.getStripNumber()] & SFLAG_RGBOW) == 1);
    }
    stripLock.release();
    stripsCreated = true;
    touchedStrips = false;
  }

  /**
   * @return the stripsAttached
   */
  public int getNumberOfStrips() {
    if (stripsCreated)
      return strips.size();
    else {
      doDeferredStripCreation();
      return strips.size();
    }
  }

  public List<Strip> getStrips() {
    if (stripsCreated)
      return this.strips;
    else {
      doDeferredStripCreation();
      return this.strips;
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
    if (stripsCreated)
      return this.strips.get(stripNumber);
    else {
      doDeferredStripCreation();
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

  public void setStripValues(int stripNumber, Pixel[] pixels) {
    if (stripsCreated)
      this.strips.get(stripNumber).setPixels(pixels);
    else {
      doDeferredStripCreation();
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
    stripLock = new Semaphore(1);
    
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

    if (packet.length > 28) {
      my_port = (int) ByteUtils.unsignedShortToInt(Arrays.copyOfRange(packet, 28, 30));
    } else {
      my_port = 9798;
    }
    if (packet.length > 30) {
      stripFlags = Arrays.copyOfRange(packet, 30, 38);
    } else {
      stripFlags = new byte[8];
      for (int i=0; i<8; i++)
        stripFlags[i]=0;
    }
    this.stripsCreated = false;
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
    if (stripsCreated)
      for (Strip strip: this.strips)
        if (strip.getRGBOW())
          return true;

    return false;
  }

  private String formattedStripFlags() {
    return "["+stripFlags[0]+"]["+stripFlags[1]+"]["+stripFlags[2]+"]["+stripFlags[3]+"]["
        +stripFlags[4]+"]["+stripFlags[5]+"]["+stripFlags[6]+"]["+stripFlags[7]+"]";
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

    // if the number of strips we have doesn't match,
    // we'll need to make a fresh set.
    if (this.stripsAttached != device.stripsAttached) {
      this.stripsCreated = false;
      this.stripsAttached = device.stripsAttached;
    }
    // likewise, if the length of each strip differs,
    // we will need to make a new set.
    if (this.pixelsPerStrip != device.pixelsPerStrip) {
      this.pixelsPerStrip = device.pixelsPerStrip;
      this.stripsCreated = false;
    }

    this.powerTotal = device.powerTotal;
    this.updatePeriod = device.updatePeriod;
    this.artnet_channel = device.artnet_channel;
    this.artnet_universe = device.artnet_universe;
    this.my_port = device.my_port;
    this.filename = device.filename;
    this.amRecording = device.amRecording;

    // if it already has strips, just use those
    if (device.stripsCreated) {
      this.makeBusy();
      this.strips = device.strips;
      this.stripsCreated = device.stripsCreated;
      this.clearBusy();
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
    if (stripsCreated) {
      for (Strip strip: this.strips)
        strip.useAntiLog(useAntiLog);
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
    if (!stripsCreated)
      doDeferredStripCreation();

    List<Strip>touchedStrips = new ArrayList<Strip>(strips);
    for (Strip strip: strips)
      if (!strip.isTouched())
        touchedStrips.remove(strip);

    return touchedStrips;
  }

}
