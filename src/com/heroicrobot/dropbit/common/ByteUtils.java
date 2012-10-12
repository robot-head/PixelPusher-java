package com.heroicrobot.dropbit.common;

public class ByteUtils {
	public static final long unsignedIntToLong(byte[] b) {
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
		int i = 0;
		i |= b[1] & 0xFF;
		i <<= 8;
		i |= b[0] & 0xFF;
		return i;
	}

	public static final int unsignedCharToInt(byte[] b) {
		int i = 0;
		i |= b[0] & 0xFF;
		return i;
	}
}
