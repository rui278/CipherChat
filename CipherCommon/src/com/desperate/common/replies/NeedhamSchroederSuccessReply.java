package com.desperate.common.replies;

public class NeedhamSchroederSuccessReply extends ReplyMessage {

	private static final long serialVersionUID = -5989137070648803525L;

	@Override
	public String getStringToHMAC() {
		return accepted.toString() + timestamp;
	}

}
