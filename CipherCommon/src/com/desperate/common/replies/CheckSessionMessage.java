package com.desperate.common.replies;

import java.io.UnsupportedEncodingException;
import javax.crypto.spec.SecretKeySpec;

import com.desperate.common.Utilities;

/**
 * 
 * 6. Eks(Rc); <br>
 * 7. Eks(Rc - 1);<br>
 * 
 * 
 * @author SIRS-RAR
 * 
 */
public class CheckSessionMessage extends ReplyMessage {

	/**  */
	private static final long serialVersionUID = 1647421209753843361L;

	/** Is actually a {@link Long}. */
	public byte[] bobNonce;

	@Override
	public String getStringToHMAC() {
		try {
			return timestamp + new String(bobNonce, Utilities.charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Long getNonce(SecretKeySpec sessionKey) {

		try {
			return (Long) Utilities.decipherObject(bobNonce, sessionKey);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);

		}
	}

	public boolean setNonce(Long nonce, SecretKeySpec sessionKey) {
		try {
			this.bobNonce = Utilities.cipherObject(nonce, sessionKey);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);

		}
	}

}
