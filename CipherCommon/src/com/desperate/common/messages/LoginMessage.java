package com.desperate.common.messages;

import com.desperate.common.Message;

public class LoginMessage extends Message {

	private static final long serialVersionUID = -3624779800555467623L;
	
	public String username;
	public String password;

	public LoginMessage(){
		
	}
	
	public LoginMessage(String username, String password){
		this.username = username;
		this.password = password;
	}
	
	@Override
	public String getStringToHMAC() {

		return username + password + timestamp;
	}

}
