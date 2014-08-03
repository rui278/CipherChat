package com.desperate.common.messages;

import com.desperate.common.Message;

public class UserListMessage extends Message {

	private static final long serialVersionUID = 7902979286457860859L;
	
	@Override
	public String getStringToHMAC() {

		return timestamp.toString();
	}


}
