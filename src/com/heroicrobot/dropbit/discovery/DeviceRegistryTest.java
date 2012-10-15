package com.heroicrobot.dropbit.discovery;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.heroicrobot.dropbit.devices.Device;

public class DeviceRegistryTest {

  @Test
  public void testDiscovery() throws InterruptedException {
    DeviceRegistry registry = new DeviceRegistry();
    while (registry.getDeviceMap().isEmpty()) {
      System.out.println("Waiting for discovery packet");
      Thread.sleep(1000);
    }
    Map<String, Device> deviceMap = registry.getDeviceMap();
    
  }

}
