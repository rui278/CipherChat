package com.desperate.common.replies;

public class UserListReplyMessage extends ReplyMessage {

	private static final long serialVersionUID = 7884010982761333606L;

	public String[] usernames;

	@Override
	public String getStringToHMAC() {

		String total = "";
		for (String u : usernames)
			total += u;

		return timestamp + total;
	}
}
