package com.heroicrobot.dropbit.discovery;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import com.heroicrobot.dropbit.common.ByteUtils;

public class DeviceHeader {
  /**
   * Device Header format:
   * uint8_t mac_address[6];
   * uint8_t ip_address[4];
   * uint8_t device_type;
   * uint8_t protocol_version; // for the device, not the discovery
   * uint16_t vendor_id;
   * uint16_t product_id;
   * uint16_t hw_revision;
   * uint16_t sw_revision;
   * uint32_t link_speed; // in bits per second
   */
  public byte[] MacAddress;
  public InetAddress IpAddress;
  public DeviceType DeviceType;
  public int ProtocolVersion;
  public int VendorId;
  public int ProductId;
  public int HardwareRevision;
  public int SoftwareRevision;
  public long LinkSpeed;
  public byte[] PacketRemainder;

  private static final int headerLength = 24;

  @Override
  public String toString() {
    StringBuffer outBuf = new StringBuffer();
    outBuf.append(this.DeviceType.name());
    outBuf.append(": MAC(" + this.GetMacAddressString() + "), ");
    outBuf.append("IP(" + this.IpAddress.toString() + "), ");
    outBuf.append("Protocol Ver(" + this.ProtocolVersion + "), ");
    outBuf.append("Vendor ID(" + this.VendorId + "), ");
    outBuf.append("Product ID(" + this.ProductId + "), ");
    outBuf.append("HW Rev(" + this.HardwareRevision + "), ");
    outBuf.append("SW Rev(" + this.SoftwareRevision + "), ");
    outBuf.append("Link Spd(" + this.LinkSpeed + "), ");
    return outBuf.toString();
  }

  public String GetMacAddressString() {
    StringBuffer buffer = new StringBuffer();
    Formatter formatter = new Formatter(buffer, Locale.US);
    formatter.format("%02x:%02x:%02x:%02x:%02x:%02x", this.MacAddress[0],
        this.MacAddress[1], this.MacAddress[2], this.MacAddress[3],
        this.MacAddress[4], this.MacAddress[5]);
    String macAddrString = formatter.toString();
    formatter.close();
    return macAddrString;
  }

  public DeviceHeader(byte[] packet) {
    if (packet.length < headerLength) {
      throw new IllegalArgumentException();
    }
    byte[] headerPkt = Arrays.copyOfRange(packet, 0, headerLength);

    this.MacAddress = Arrays.copyOfRange(headerPkt, 0, 6);
    try {
      this.IpAddress = InetAddress.getByAddress(Arrays.copyOfRange(headerPkt,
          6, 10));
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException();
    }
    this.DeviceType = com.heroicrobot.dropbit.discovery.DeviceType
        .fromInteger(ByteUtils.unsignedCharToInt(new byte[] { headerPkt[10] }));
    this.ProtocolVersion = ByteUtils
        .unsignedCharToInt(new byte[] { headerPkt[11] });
    this.VendorId = ByteUtils.unsignedShortToInt(Arrays.copyOfRange(headerPkt,
        12, 14));
    this.ProductId = ByteUtils.unsignedShortToInt(Arrays.copyOfRange(headerPkt,
        14, 16));
    this.HardwareRevision = ByteUtils.unsignedShortToInt(Arrays.copyOfRange(
        headerPkt, 16, 18));
    this.SoftwareRevision = ByteUtils.unsignedShortToInt(Arrays.copyOfRange(
        headerPkt, 18, 20));
    this.LinkSpeed = ByteUtils.unsignedIntToLong(Arrays.copyOfRange(headerPkt,
        20, 24));
    this.PacketRemainder = Arrays.copyOfRange(packet, headerLength,
        packet.length);
  }
}
