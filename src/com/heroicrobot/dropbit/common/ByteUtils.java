package com.heroicrobot.dropbit.common;

public class ByteUtils {
  public static final long unsignedIntToLong(byte[] b) {
    if (b.length != 4) {
      throw new IllegalArgumentException();
    }
    long l = 0;
    l |= b[3] & 0xFF;
    l <<= 8;
    l |= b[2] & 0xFF;
    l <<= 8;
    l |= b[1] & 0xFF;
    l <<= 8;
    l |= b[0] & 0xFF;
    return l;
  }

  public static final int unsignedShortToInt(byte[] b) {
    if (b.length != 2) {
      throw new IllegalArgumentException();
    }
    int i = 0;
    i |= b[1] & 0xFF;
    i <<= 8;
    i |= b[0] & 0xFF;
    return i;
  }

  public static final int unsignedCharToInt(byte[] b) {
    if (b.length != 1) {
      throw new IllegalArgumentException();
    }
    int i = 0;
    i |= b[0] & 0xFF;
    return i;
  }

  public static final long byteArrayToLong(byte[] b, boolean bigEndian) {
    if (b.length > 8) {
      throw new IllegalArgumentException(
          "The largest byte array that can fit in a long is 8");
    }
    long value = 0;
    if (bigEndian) {
      for (int i = 0; i < b.length; i++) {
        value = (value << 8) + (b[i] & 0xFF);
      }
    } else {
      for (int i = 0; i < b.length; i++) {
        value += ((long) b[i] & 0xFFL) << (8 * i);
      }
    }
    return value;
  }
}
