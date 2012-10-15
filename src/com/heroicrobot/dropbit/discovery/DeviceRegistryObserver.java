package com.heroicrobot.dropbit.discovery;

import java.util.List;
import java.util.Observable;

import com.heroicrobot.dropbit.devices.Device;

public class DeviceRegistryObserver extends Observable {

  List<Device> devices;
}
