package com.heroicrobot.dropbit.discovery;

import java.util.HashMap;
import java.util.Map;

import com.heroicrobot.dropbit.devices.Device;
import com.heroicrobot.dropbit.devices.PixelPusher;

import hypermedia.net.UDP;

public class DeviceRegistry {

  private UDP udp;
  private static final int DISCOVERY_PORT = 7331;
  
  private Map<String, Device> deviceMap;
  
  public Map<String, Device> getDeviceMap() {
    return deviceMap;
  }


  public DeviceRegistry() {
    udp = new UDP(this, DISCOVERY_PORT);
    deviceMap = new HashMap<String, Device>();
    udp.setReceiveHandler("receive");
    udp.log(true);
    udp.listen(true);
  }

  public void receive(byte[] data) {
    System.out.println("Got data");
    DeviceHeader header = new DeviceHeader(data);
    String macAddr = header.GetMacAddressString();
    Device device = null;
    if (header.DeviceType == DeviceType.PIXELPUSHER) {
      device = new PixelPusher(header.PacketRemainder);
    }
    deviceMap.put(macAddr, device);
  }
  
}
