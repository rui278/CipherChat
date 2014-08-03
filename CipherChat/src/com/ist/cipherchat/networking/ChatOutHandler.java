package com.ist.cipherchat.networking;

import java.io.ObjectOutputStream;

import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

import com.desperate.common.Utilities;
import com.desperate.common.messages.ChatMessage;

public class ChatOutHandler {

	private ObjectOutputStream out;
	private SecretKeySpec sessionKey;

	public ChatOutHandler() {
		this.out = Globals.getOos();
		this.sessionKey = Globals.getSessionKey();
	}

	public void sendMessage(String text) {
		
		ChatMessage message = new ChatMessage();

		try {
			message.cipheredText = Utilities.cipherObject(text, sessionKey);
		} catch (Exception e) {
			Log.d("register", "Could not cipher message to be sent. Message not sent.");
			e.printStackTrace();
			return;
		}

		message.timestamp = System.currentTimeMillis();
		message.remoteHMAC = message.computeHMAC(Utilities.keyToString(sessionKey));

		message.send(out);
	}
}
