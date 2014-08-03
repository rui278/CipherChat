package com.desperate.common.messages;

import com.desperate.common.Message;

public class IPMessage extends Message {

	private static final long serialVersionUID = 3923056733462088710L;

	public String username;

	public IPMessage(String username) {
		this.username = username;
	}

	@Override
	public String getStringToHMAC() {
		return timestamp + username;
	}

}
