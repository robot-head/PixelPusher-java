package com.heroicrobot.dropbit.devices;

import java.util.Arrays;

import com.heroicrobot.dropbit.common.ByteUtils;
import com.heroicrobot.dropbit.discovery.DeviceHeader;

public class PixelPusher extends DeviceImpl {
  /**
   * uint8_t strips_attached;
   * uint8_t max_strips_per_packet;
   * uint16_t pixels_per_strip; // uint16_t used to make alignment work
   * uint32_t update_period; // in microseconds
   * uint32_t power_total; // in PWM units
   */

  public int StripsAttached;
  public int MaxStripsPerPacket;
  public int PixelsPerStrip;
  public long UpdatePeriod;
  public long PowerTotal;

  public PixelPusher(byte[] packet, DeviceHeader header) {
    super(header);
    if (packet.length < 12) {
      throw new IllegalArgumentException();
    }
    StripsAttached = ByteUtils.unsignedCharToInt(Arrays.copyOfRange(packet, 0,
        1));
    MaxStripsPerPacket = ByteUtils.unsignedCharToInt(Arrays.copyOfRange(packet,
        1, 2));
    PixelsPerStrip = ByteUtils.unsignedShortToInt(Arrays.copyOfRange(packet, 2,
        4));
    UpdatePeriod = ByteUtils
        .unsignedIntToLong(Arrays.copyOfRange(packet, 4, 8));
    PowerTotal = ByteUtils.unsignedIntToLong(Arrays.copyOfRange(packet, 8, 12));
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
        + ") Max Strips Per Packet(" + MaxStripsPerPacket
        + ") PixelsPerStrip (" + PixelsPerStrip + ") Update Period ("
        + UpdatePeriod + ") Power Total (" + PowerTotal + ")";
  }
}
