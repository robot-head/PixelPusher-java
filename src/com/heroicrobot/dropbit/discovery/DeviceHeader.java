package com.heroicrobot.dropbit.discovery;

import com.heroicrobot.dropbit.common.ByteUtils;
import com.heroicrobot.dropbit.discovery.DeviceType;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

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

	public String toString() {
		return "Foo";
	}

	public String GetMacAddressString() {
		return "Bar";
	}

	public DeviceHeader(byte[] HeaderPacket) {
		this.MacAddress = Arrays.copyOfRange(HeaderPacket, 0, 5);
		try {
			this.IpAddress = InetAddress.getByAddress(Arrays.copyOfRange(
					HeaderPacket, 6, 9));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.DeviceType = com.heroicrobot.dropbit.discovery.DeviceType
				.fromInteger(ByteUtils
						.unsignedCharToInt(new byte[] { HeaderPacket[10] }));
		this.ProtocolVersion = ByteUtils
				.unsignedCharToInt(new byte[] { HeaderPacket[11] });
		Arrays.copyOfRange(HeaderPacket, 12, 13);
		this.VendorId = ByteUtils.unsignedShortToInt(Arrays.copyOfRange(
				HeaderPacket, 12, 13));
		this.ProductId = ByteUtils.unsignedShortToInt(Arrays.copyOfRange(
				HeaderPacket, 13, 14));
		this.HardwareRevision = ByteUtils.unsignedShortToInt(Arrays
				.copyOfRange(HeaderPacket, 14, 15));
		this.SoftwareRevision = ByteUtils.unsignedShortToInt(Arrays
				.copyOfRange(HeaderPacket, 16, 17));
		this.LinkSpeed = ByteUtils.unsignedIntToLong(Arrays.copyOfRange(
				HeaderPacket, 18, 23));
	}
}
