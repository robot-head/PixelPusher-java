package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.heroicrobot.dropbit.common.ByteUtils;
import com.heroicrobot.dropbit.devices.DeviceImpl;
import com.heroicrobot.dropbit.discovery.DeviceHeader;

public class PixelPusher extends DeviceImpl 
  implements java.lang.Comparable<PixelPusher> {
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
   */

  private List<Strip> strips;
  long extraDelayMsec = 0;
  boolean autothrottle = false;

  /**
   * @return the stripsAttached
   */
  public int getNumberOfStrips() {
    return strips.size();
  }

  public List<Strip> getStrips() {
    return this.strips;
  }

  public Strip getStrip(int stripNumber) {
    return this.strips.get(stripNumber);
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
    if (this.strips.isEmpty()) {
      return 0;
    }
    return this.strips.get(0).getLength();
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

  private int maxStripsPerPacket;
  private long updatePeriod;
  private long powerTotal;
  private long deltaSequence;
  private int controllerOrdinal;
  private int groupOrdinal;

  public void setStripValues(int stripNumber, Pixel[] pixels) {
    this.strips.get(stripNumber).setPixels(pixels);
  }

  public PixelPusher(byte[] packet, DeviceHeader header) {
    super(header);
    if (packet.length < 12) {
      throw new IllegalArgumentException();
    }
    int stripsAttached = ByteUtils.unsignedCharToInt(Arrays.copyOfRange(packet,
        0, 1));
    int pixelsPerStrip = ByteUtils.unsignedShortToInt(Arrays.copyOfRange(
        packet, 2, 4));
    maxStripsPerPacket = ByteUtils.unsignedCharToInt(Arrays.copyOfRange(packet,
        1, 2));

    updatePeriod = ByteUtils
        .unsignedIntToLong(Arrays.copyOfRange(packet, 4, 8));
    powerTotal = ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 8, 12));
    deltaSequence = ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 12,
        16));
    controllerOrdinal = (int) ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 16, 20));
    groupOrdinal = (int) ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 20, 24));
    this.strips = new ArrayList<Strip>();
    for (int stripNo = 0; stripNo < stripsAttached; stripNo++) {
      this.strips.add(new Strip(this, stripNo, pixelsPerStrip));
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
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    PixelPusher other = (PixelPusher) obj;
    if (getPixelsPerStrip() != other.getPixelsPerStrip())
      return false;
    if (getNumberOfStrips() != other.getNumberOfStrips())
      return false;
    return true;
  }

  public String toString() {
    return super.toString() + " # Strips(" + getNumberOfStrips()
        + ") Max Strips Per Packet(" + maxStripsPerPacket
        + ") PixelsPerStrip (" + getPixelsPerStrip() + ") Update Period ("
        + updatePeriod + ") Power Total (" + powerTotal + ") Delta Sequence ( "
        + deltaSequence + ") Group (" +groupOrdinal +") Controller ("
        + controllerOrdinal + " )";
  }

  public void copyHeader(PixelPusher device) {
    this.controllerOrdinal = device.controllerOrdinal;
    this.deltaSequence = device.deltaSequence;
    this.groupOrdinal = device.groupOrdinal;
    this.maxStripsPerPacket = device.maxStripsPerPacket;
    this.powerTotal = device.powerTotal;
    this.updatePeriod = device.updatePeriod;

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
}
