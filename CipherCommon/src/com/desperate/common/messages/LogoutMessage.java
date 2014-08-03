package com.desperate.common.messages;

import com.desperate.common.Message;

public class LogoutMessage extends Message{

	
	/**  */
	private static final long serialVersionUID = 8474235947479435638L;

	@Override
	public String getStringToHMAC() {

		return timestamp.toString();
	}
}
