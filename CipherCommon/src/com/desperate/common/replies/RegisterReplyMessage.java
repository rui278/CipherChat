package com.desperate.common.replies;

public class RegisterReplyMessage extends ReplyMessage {

	/**  */
	private static final long serialVersionUID = 1614980055763741215L;

	public RegisterReplyMessage(boolean accepted, Long timestamp) {

		this.accepted = accepted;
		this.timestamp = timestamp;
	}

	@Override
	public String getStringToHMAC() {
		return accepted.toString() + timestamp;
	}

}
