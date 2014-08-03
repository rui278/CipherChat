package com.desperate.common.replies;

import com.desperate.common.Message;

public abstract class ReplyMessage extends Message {

	private static final long serialVersionUID = 5064222132893477293L;

	public Boolean accepted = true;
	public String errorMessage = null;

}
