package com.desperate.common.messages;

import java.io.UnsupportedEncodingException;

import com.desperate.common.Message;
import com.desperate.common.Utilities;

/**
 * Implements the following messages of the Needham-Shroeder-revisited protocol:
 * <p>
 * 3. <b>A -> KDC:</b> A, B, Ra, Eb(A, Rb)
 * 
 * @author SIRS-RAR
 * 
 */
public class SessionKeyRequestMessage extends Message {

	private static final long serialVersionUID = -1991738735421123912L;

	public String usernameA;
	public String usernameB;
	public Long randomA;

	/** Object of class #{@link com.desperate.common.NoncePacket}. */
	public byte[] userBPacket;

	public SessionKeyRequestMessage(String unameA, String unameB, Long LarondA, byte[] userBPacket) {
		this.usernameA = unameA;
		this.usernameB = unameB;
		this.randomA = LarondA;
		this.userBPacket = userBPacket;
	}

	@Override
	public String getStringToHMAC() {

		try {
			return usernameA + usernameB + randomA.toString() + new String(userBPacket, Utilities.charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
