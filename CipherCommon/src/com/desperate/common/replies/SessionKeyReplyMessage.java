package com.desperate.common.replies;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.desperate.common.SessionKeyRequestInfo;
import com.desperate.common.Utilities;

/**
 * In the "Needham-Shroeder revisited" method lectured, represents the reply that KDC sends to A to get a session key to talk with B. Comes ciphered
 * with A's Key Encryption Key.
 * 
 * @author SIRS-RAR
 * 
 */
public class SessionKeyReplyMessage extends ReplyMessage {

	// Ea( Ra, B, Ks, Eb(A, Rb, Ks) )

	/**  */
	private static final long serialVersionUID = 7773927354068004172L;

	/**
	 * Contains a #{@link com.desperate.common.SessionKeyRequestInfo SessionKeyRequestInfo} ciphered with the requester's KEK.
	 */
	public byte[] cipheredPacket;

	public SessionKeyRequestInfo getInfo(SecretKeySpec key) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, ClassNotFoundException, IOException {

		return (SessionKeyRequestInfo) Utilities.decipherObject(cipheredPacket, key);
	}

	@Override
	public String getStringToHMAC() {

		try {
			return timestamp + new String(cipheredPacket, Utilities.charset);

		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}