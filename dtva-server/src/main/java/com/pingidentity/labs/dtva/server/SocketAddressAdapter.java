/* Copyright 2017 Ping Identity Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. */
package com.pingidentity.labs.dtva.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.pingidentity.labs.rapport.StateSerializer;

// Supports addresses in the form of:
// - name:port
// - 11.22.33.44:port
// - [::FF]:port
// underlying JDK does not appear to support output as short form ipv6.

public class SocketAddressAdapter extends XmlAdapter<String, InetSocketAddress> implements StateSerializer<InetSocketAddress> {

	private static Pattern IPV6_ADDRESS_PATTERN;

	static {
		IPV6_ADDRESS_PATTERN = Pattern.compile("\\[([^\\]]+)\\]:([0-9]+)");
	}
	@Override
	public InetSocketAddress unmarshal(String v) throws Exception {
		return unmarshalSocket(v);
	}

	static InetSocketAddress unmarshalIpv6(String v) {
		Matcher m = IPV6_ADDRESS_PATTERN.matcher(v);
		if (!m.matches()) {
			throw new IllegalArgumentException("Unable to parse as ipv6 address");
		}
		return new InetSocketAddress(m.group(1), Integer.parseInt(m.group(2)));
	}

	public static String marshalSocket(InetSocketAddress v) throws Exception {
		String host = v.getHostString();
		if (host.contains(":")) {
			host = "[" + host + "]";
		}
		return host + ":" + v.getPort();
	}

	@Override
	public String marshal(InetSocketAddress v) throws Exception {
		return SocketAddressAdapter.marshalSocket(v);
	}
	
	public static InetSocketAddress unmarshalSocket(String v) {
		if (v.startsWith("[")) {
			return unmarshalIpv6(v);
		}
		
		String[] parts = v.split(":");
		if (parts.length != 2) {
			throw new IllegalArgumentException("no port detected on address");
		}

		InetAddress addr;
		int port;
		
		try {
			addr = InetAddress.getByName(parts[0]);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse host/ip");
		}
		try {
			port = Integer.parseInt(parts[1]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("unable to parse port");
		}
		return new InetSocketAddress(addr, port);
	}

	@Override
	public InetSocketAddress deserializeState(DataInput in) throws IOException {
		int addressLength = in.readByte();
		byte[] address = new byte[addressLength];
		in.readFully(address);
		int port = in.readInt();
		return new InetSocketAddress(InetAddress.getByAddress(address), port);
	}

	@Override
	public void serializeState(InetSocketAddress socketAddress, DataOutput out) throws IOException {
		byte[] address = socketAddress.getAddress().getAddress();
		out.writeByte(address.length);
		out.write(address);
		out.writeInt(socketAddress.getPort());
	}
}
