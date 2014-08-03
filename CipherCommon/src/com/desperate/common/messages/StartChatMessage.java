package com.desperate.common.messages;

import com.desperate.common.Message;

/**
 * Implements the following message of the Needham-Shroeder-revisited algorithm:
 * <p>
 * 1. <b>A -> B:</b> A
 * 
 * @author SIRS-RAR
 * 
 */
public class StartChatMessage extends Message {

	private static final long serialVersionUID = -6406198061595412305L;

	/** User name of A */
	public String requesterUserName;

	public StartChatMessage(String requesterUserName) {
		this.requesterUserName = requesterUserName;
	}

	@Override
	public String getStringToHMAC() {
		return null;
	}

}
