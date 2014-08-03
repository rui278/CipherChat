package com.desperate.common.replies;

import java.io.UnsupportedEncodingException;
import javax.crypto.spec.SecretKeySpec;

import com.desperate.common.Utilities;

public class LoginReplyMessage extends ReplyMessage {

	private static final long serialVersionUID = 6323719818125116841L;

	public SecretKeySpec keyEncryptionKey;

	@Override
	public String getStringToHMAC() {

		try {
			return timestamp + accepted.toString() + new String(keyEncryptionKey.getEncoded(), Utilities.charset);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
