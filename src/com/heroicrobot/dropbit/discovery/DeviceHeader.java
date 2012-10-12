package com.heroicrobot.dropbit.discovery;

import com.heroicrobot.dropbit.common.ByteUtils;
import com.heroicrobot.dropbit.discovery.DeviceType;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

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
		StringBuffer outBuf = new StringBuffer();
		outBuf.append(this.DeviceType.name());
		outBuf.append(": Mac(" + this.GetMacAddressString() + "), ");
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
		formatter.format("%X:%X:%X:%X:%X:%X", this.MacAddress[0],
				this.MacAddress[1], this.MacAddress[2], this.MacAddress[3],
				this.MacAddress[4], this.MacAddress[5]);
		String macAddrString = formatter.toString();
		formatter.close();
		return macAddrString;
	}

	public DeviceHeader(byte[] HeaderPacket) {
		if (HeaderPacket.length != 24) {
			throw new IllegalArgumentException();
		}
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
