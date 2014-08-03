package com.ist.cipherchat.networking;

public class ThreadComm {

	public boolean error;
	
	public boolean loginFail;
	public boolean registerFail;
	public boolean noUsers;
	public boolean contactsFail;
	
	public ThreadComm(){
		this.error = false;

		this.loginFail = false;
		this.registerFail = false;
		this.noUsers = false;
		this.contactsFail = false;
	}
	
	public void clean(){
		this.error = false;

		this.loginFail = false;
		this.registerFail = false;
		this.noUsers = false;
		this.contactsFail = false;
	}
}
