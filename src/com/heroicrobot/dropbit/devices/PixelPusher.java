package com.heroicrobot.dropbit.devices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.heroicrobot.dropbit.common.ByteUtils;
import com.heroicrobot.dropbit.discovery.DeviceHeader;
import com.heroicrobot.dropbit.pixelpusher.Strip;

public class PixelPusher extends DeviceImpl {
  /**
   * uint8_t strips_attached;
   * uint8_t max_strips_per_packet;
   * uint16_t pixels_per_strip; // uint16_t used to make alignment work
   * uint32_t update_period; // in microseconds
   * uint32_t power_total; // in PWM units
   */

  private List<Strip> strips;

  /**
   * @return the stripsAttached
   */
  public int getNumberOfStrips() {
    return strips.size();
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

  private int maxStripsPerPacket;
  private long updatePeriod;
  private long powerTotal;

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
    this.strips = new ArrayList<Strip>();
    for (int stripNo = 0; stripNo < stripsAttached; stripNo++) {
      this.strips.add(new Strip(pixelsPerStrip));
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
    result = prime * result + PixelsPerStrip;
    result = prime * result + StripsAttached;
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
    if (PixelsPerStrip != other.PixelsPerStrip)
      return false;
    if (StripsAttached != other.StripsAttached)
      return false;
    return true;
  }

  public String toString() {
    return super.toString() + " # Strips(" + StripsAttached
        + ") Max Strips Per Packet(" + maxStripsPerPacket
        + ") PixelsPerStrip (" + PixelsPerStrip + ") Update Period ("
        + updatePeriod + ") Power Total (" + powerTotal + ")";
  }
}
