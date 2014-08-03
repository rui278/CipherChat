package com.desperate.common;

import java.io.Serializable;

import javax.crypto.spec.SecretKeySpec;

/**
 * Packet created by the B entity in the second message of the Needham-Shroeder-revisited protocol. In message 7, B will receive this again, with the
 * new Session Key inside, and verify the nonce it sent before is the same.
 * 
 * @author SIRS-RAR
 * 
 */
public class NoncePacket implements Serializable {

	private static final long serialVersionUID = 4643248173148359568L;

	/** Name of A */
	public String requesterUserName;

	/** Nonce created by B for freshness of the chat session request */
	public Long bNonce;

	/**
	 * Session key to be used in the session. Null when B created the packet. Contains the key when A returns the packet after getting the key from
	 * the KDC.
	 */
	public SecretKeySpec sessionKey = null;

	/**
	 * Creates the nonce to be used in message 2.
	 * 
	 * @param requestedUserName
	 *            Name of A
	 * @param bNonce
	 *            Random unique number to be used for freshnes
	 */
	public NoncePacket(String requestedUserName, Long bNonce) {
		this.requesterUserName = requestedUserName;
		this.bNonce = bNonce;
	}

}