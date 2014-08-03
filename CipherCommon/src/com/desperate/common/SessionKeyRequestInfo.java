package com.desperate.common;

import java.io.Serializable;

import javax.crypto.spec.SecretKeySpec;

/**
 * Contains information for a session key, made by the KDC server. Used in message 4.
 * 
 * @author SIRS-2013
 * 
 */
public class SessionKeyRequestInfo implements Serializable {

	/**  */
	private static final long serialVersionUID = 7384857163198979138L;

	public String usernameB;
	public Long nonceA;
	public SecretKeySpec sessionKey;
	/** Type NoncePacket */
	public byte[] cipheredBPacket;

	public SessionKeyRequestInfo(String usernameB, Long nonceA, SecretKeySpec sessionKey, byte[] cipheredBPacket) {

		this.usernameB = usernameB;
		this.nonceA = nonceA;
		this.sessionKey = sessionKey;
		this.cipheredBPacket = cipheredBPacket;

	}

}
