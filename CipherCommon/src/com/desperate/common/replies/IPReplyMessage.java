package com.desperate.common.replies;

import java.net.InetAddress;

public class IPReplyMessage extends ReplyMessage {



	private static final long serialVersionUID = -6416881186994491806L;

	public InetAddress IPaddress;
	public String username;

	@Override
	public String getStringToHMAC() {

		return timestamp + IPaddress.toString() + username;
	}

}
