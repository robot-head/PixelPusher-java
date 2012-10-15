package com.heroicrobot.dropbit.devices;

import java.util.Arrays;

import com.heroicrobot.dropbit.common.ByteUtils;

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

  public PixelPusher(byte[] packet) {
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
}
