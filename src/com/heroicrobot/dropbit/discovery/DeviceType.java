package com.heroicrobot.dropbit.discovery;

public enum DeviceType {
	PIXELPUSHER, ETHERDREAM, LUMIABRIDGE;

	public static DeviceType fromInteger(int x) {
		switch (x) {
		case 0:
			return PIXELPUSHER;
		case 1:
			return ETHERDREAM;
		case 2:
			return LUMIABRIDGE;
		}
		return null;
	}
}
