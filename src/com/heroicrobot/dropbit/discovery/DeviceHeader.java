package com.heroicrobot.dropbit.discovery;

import com.heroicrobot.dropbit.discovery.DeviceType;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DeviceHeader {
	/**
	 * Device Header format: uint8_t mac_address[6]; uint8_t ip_address[4];
	 * uint8_t device_type; uint8_t protocol_version; // for the device, not the
	 * discovery uint16_t vendor_id; uint16_t product_id; uint16_t hw_revision;
	 * uint16_t sw_revision; uint32_t link_speed; // in bits per second
	 */
	public byte[] MacAddress;
	public InetAddress IpAddress;
	public DeviceType DeviceType;
	public int ProtocolVersion;
	public int VendorId;
	public int HardwareRevision;
	public int SoftwareRevision;
	public long LinkSpeed;

	public String toString() {
		return "Foo";
	}

	public String GetMacAddressString() {
		return "Bar";
	}

	public DeviceHeader(byte[] HeaderPacket) {
		this.MacAddress = new byte[] { HeaderPacket[0], HeaderPacket[1],
		        HeaderPacket[2], HeaderPacket[3], HeaderPacket[4],
		        HeaderPacket[5] };
		try {
			this.IpAddress = InetAddress.getByAddress(new byte[] {
			        HeaderPacket[6], HeaderPacket[7], HeaderPacket[8],
			        HeaderPacket[9] });
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.DeviceType = com.heroicrobot.dropbit.discovery.DeviceType
		        .fromInteger((int) HeaderPacket[10] & 0xff);
		this.ProtocolVersion = (int) HeaderPacket[11] & 0xff;
		this.VendorId = (int) HeaderPacket[12] & 0xff;
		this.HardwareRevision = (int) HeaderPacket[13] & 0xff;
		this.SoftwareRevision = (int) HeaderPacket[14] & 0xff;
	}
}