package com.heroicrobot.dropbit.registry;

import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.heroicrobot.dropbit.devices.pixelpusher.Pixel;
import com.heroicrobot.dropbit.devices.pixelpusher.Strip;


public class DeviceRegistryTest {

  private final static Logger LOGGER = Logger
      .getLogger(DeviceRegistryTest.class.getName());

  private Random random = new Random();
  
  public Pixel generateRandomPixel() {
    byte[] pixelBytes = new byte[3];
    random.nextBytes(pixelBytes);
    return new Pixel(pixelBytes[0], pixelBytes[1], pixelBytes[2]);
  }
  
  @Test
  public void testDiscovery() throws InterruptedException {
    LOGGER.setLevel(Level.FINEST);
    LOGGER.info("Beginning discovery");
    DeviceRegistry registry = new DeviceRegistry();
    class TestObserver implements Observer {

      public boolean hasStrips = false;
      @Override
      public void update(Observable registry, Object updatedDevice) {
        LOGGER.info("Registry changed!");
        if (updatedDevice != null) {
          LOGGER.info("Device change: " + updatedDevice);
        }
        this.hasStrips = true;
      }

    }
    TestObserver testObserver = new TestObserver();
    registry.addObserver(testObserver);
    while (true) {
      Thread.yield();
      if (testObserver.hasStrips) {
        registry.startPushing();
        for(Strip strip : registry.getStrips()) {
          for (int i = 0; i < strip.getLength(); i++)
            strip.setPixel(generateRandomPixel(), i);
        }
      }
    }
  }

}
