/**
 * 
 */
package com.heroicrobot.dropbit.discovery;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author mattstone
 * 
 */
public class DeviceHeaderTest {

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * 00 02 f7 f0 c0 99 0a 49 26 a6 02 01 02 00 01 00
   * 02 00 03 00 00 e1 f5 05 08 01 32 00 ff ff ff ff
   * 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
   * 00 00 00 00
   */
  @Test
  public void test() {
    byte[] headerPacket = { (byte) 0x00, (byte) 0x02, (byte) 0xf7, (byte) 0xf0,
        (byte) 0xc0, (byte) 0x99, (byte) 0x0a, (byte) 0x49, (byte) 0x26,
        (byte) 0xa6, (byte) 0x02, (byte) 0x01, (byte) 0x02, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x03,
        (byte) 0x00, (byte) 0x00, (byte) 0xe1, (byte) 0xf5, (byte) 0x05,
        (byte) 0x08, (byte) 0x01, (byte) 0x32, (byte) 0x00, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff };
    DeviceHeader deviceHeader = new DeviceHeader(Arrays.copyOfRange(
        headerPacket, 0, 24));
    assertEquals("PIXELPUSHER: MAC(00:02:f7:f0:c0:99), IP(/10.73.38.166), "
        + "Protocol Ver(1), Vendor ID(2), Product ID(1), "
        + "HW Rev(2), SW Rev(3), Link Spd(100000000), ",
        deviceHeader.toString());
  }

}
