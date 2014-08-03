package com.desperate.common.replies;

import java.io.UnsupportedEncodingException;

import javax.crypto.spec.SecretKeySpec;

import com.desperate.common.NoncePacket;
import com.desperate.common.Utilities;

/**
 * Implements the following messages of the Needham-Shroeder-revisited protocol:
 * <p>
 * 2. <b>B -> A:</b> Eb( A, Rb ) (Ks is null)<br>
 * 5. <b>A -> B:</b> Eb( A, Rb , Ks)
 * 
 * @author SIRS-RAR
 * 
 */
public class StartChatReply extends ReplyMessage {

	private static final long serialVersionUID = 709775960818919208L;

	/**
	 * Creates a message with a ciphered nonce packet inside. To be used by B on step 2 of Needham-Shroeder revisited.
	 * 
	 * @param packet
	 *            Packet with freshness information and the name of A
	 * @param key
	 *            Secret key of B
	 * @throws Exception
	 *             if the ciphering failed for any reason
	 */
	public StartChatReply(NoncePacket packet, SecretKeySpec key) throws Exception {

		this.cipheredNoncePacket = Utilities.cipherObject(packet, key);
	}

	/**
	 * Creates a message with an already ciphered nonce packet inside. To be used by A on step 5 of Needham-Shroeder revisited.
	 * 
	 * @param cipheredNoncePacket
	 *            ciphered freshness packet sent by B on step 2
	 */
	public StartChatReply(byte[] cipheredNoncePacket) {
		this.cipheredNoncePacket = cipheredNoncePacket;
	}

	/** Is actually a {@linkplain com.desperate.common.NoncePacket NoncePacket}. */
	public byte[] cipheredNoncePacket;

	@Override
	public String getStringToHMAC() {

		try {
			return timestamp + new String(cipheredNoncePacket, Utilities.charset);
		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
