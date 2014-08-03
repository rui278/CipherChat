package com.desperate.common.messages;

import com.desperate.common.Message;

public class RegisterMessage extends Message {

	private static final long serialVersionUID = -2023768990593310990L;

	public String username;
	public String password;

	public RegisterMessage(String username, String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	public String getStringToHMAC() {

		return username + password + timestamp;
	}

}
