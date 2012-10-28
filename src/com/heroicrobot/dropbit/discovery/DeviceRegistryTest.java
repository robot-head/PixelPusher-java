package com.heroicrobot.dropbit.discovery;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.heroicrobot.dropbit.devices.Device;

public class DeviceRegistryTest {

  private final static Logger LOGGER = Logger
      .getLogger(DeviceRegistryTest.class.getName());

  @Test
  public void testDiscovery() throws InterruptedException {
    LOGGER.setLevel(Level.FINE);
    LOGGER.info("Beginning discovery");
    DeviceRegistry registry = new DeviceRegistry();
    class TestObserver implements Observer {

      @Override
      public void update(Observable registry, Object updatedDevice) {
        System.out.println("Registry changed!");
        if (updatedDevice != null) {
          System.out.println("Device change: " + updatedDevice);
        }
      }

    }
    TestObserver testObserver = new TestObserver();
    registry.addObserver(testObserver);
    while (true) {
      Thread.sleep(1000);
    }
  }

}
