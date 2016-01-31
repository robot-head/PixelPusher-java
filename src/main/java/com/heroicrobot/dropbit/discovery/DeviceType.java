package com.heroicrobot.dropbit.discovery;

public enum DeviceType {
  ETHERDREAM, LUMIABRIDGE, PIXELPUSHER;

  public static DeviceType fromInteger(int x) {
    switch (x) {
      case 0:
        return ETHERDREAM;
      case 1:
        return LUMIABRIDGE;
      case 2:
        return PIXELPUSHER;
    }
    return null;
  }
}
