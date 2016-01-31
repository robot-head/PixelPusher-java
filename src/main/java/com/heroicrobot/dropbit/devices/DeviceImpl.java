package com.heroicrobot.dropbit.devices;

import java.net.InetAddress;

import com.heroicrobot.dropbit.discovery.DeviceHeader;
import com.heroicrobot.dropbit.discovery.DeviceType;

public abstract class DeviceImpl implements Device {

  private DeviceHeader header;

  public DeviceImpl(DeviceHeader header) {
    this.header = header;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.heroicrobot.dropbit.devices.Device#getMacAddress()
   */
  @Override
  public String getMacAddress() {
    // TODO Auto-generated method stub
    return header.GetMacAddressString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.heroicrobot.dropbit.devices.Device#getIp()
   */
  @Override
  public InetAddress getIp() {
    // TODO Auto-generated method stub
    return header.IpAddress;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.heroicrobot.dropbit.devices.Device#getDeviceType()
   */
  @Override
  public DeviceType getDeviceType() {
    // TODO Auto-generated method stub
    return header.DeviceType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.heroicrobot.dropbit.devices.Device#getProtocolVersion()
   */
  @Override
  public int getProtocolVersion() {
    // TODO Auto-generated method stub
    return header.ProtocolVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.heroicrobot.dropbit.devices.Device#getVendorId()
   */
  @Override
  public int getVendorId() {
    // TODO Auto-generated method stub
    return header.VendorId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.heroicrobot.dropbit.devices.Device#getProductId()
   */
  @Override
  public int getProductId() {
    // TODO Auto-generated method stub
    return header.ProductId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.heroicrobot.dropbit.devices.Device#getHardwareRevision()
   */
  @Override
  public int getHardwareRevision() {
    // TODO Auto-generated method stub
    return header.HardwareRevision;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.heroicrobot.dropbit.devices.Device#getSoftwareRevision()
   */
  @Override
  public int getSoftwareRevision() {
    // TODO Auto-generated method stub
    return header.SoftwareRevision;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.heroicrobot.dropbit.devices.Device#getLinkSpeed()
   */
  @Override
  public long getLinkSpeed() {
    // TODO Auto-generated method stub
    return header.LinkSpeed;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Mac: " + header.GetMacAddressString() + ", IP: "
        + header.IpAddress.getHostAddress() + " Firmware revision: "+getSoftwareRevision();
  }

}