package com.desperate.common.messages;

import java.io.UnsupportedEncodingException;

import com.desperate.common.Message;
import com.desperate.common.Utilities;

/**
 * Contains a message ciphered with a session key. To be used after the Needham-Schroeder protocol was completed.
 * 
 * @author SIRS-RAR
 * 
 */
public class ChatMessage extends Message {

	private static final long serialVersionUID = 2281847078152149714L;

	/** Is actually a String, ciphered with a session key. */
	public byte[] cipheredText;

	@Override
	public String getStringToHMAC() {
		try {
			return timestamp + new String(cipheredText, Utilities.charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
