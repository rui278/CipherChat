package com.ist.cipherchat.networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import android.annotation.SuppressLint;
import android.os.Message;
import android.util.Log;
import com.desperate.common.Utilities;
import com.desperate.common.messages.ChatMessage;
import com.ist.cipherchat.gui.ChatActivity;

/**
 * Receives messages from another chatter. Verifies message freshness and integrity with timestamp and HMAC. Refuses messages that aren't valid, and
 * doesn't print them to the final user.
 * 
 * @author SIRS-RAR
 * 
 */
public class ChatInRunnable implements Runnable {

	private ObjectInputStream in;
	private String senderName;
	private SecretKeySpec sessionKey;

	private boolean running = true;

	/**
	 * Creates a new thread to listen to messages coming from another chatter.
	 * 
	 */
	public ChatInRunnable() {
		this.in = Globals.getOis();
		this.senderName = Globals.getOtherUsername();
		this.sessionKey = Globals.getSessionKey();

	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public void run() {

		ChatMessage message;

		while (running) {

			try {
				message = (ChatMessage) in.readObject();

				// Check HMAC - refuse message if fails
				String localHMAC = message.computeHMAC(Utilities.keyToString(sessionKey));
				if (!message.remoteHMAC.equals(localHMAC)) {
					continue;
				}

				// Check timestamp - refuse message if fails
				if (!message.verifyTimestamp(Utilities.tolerance)) {
					continue;
				}

				String text = (String) Utilities.decipherObject(message.cipheredText, sessionKey);

				Date dNow = new Date();
				SimpleDateFormat ft = new SimpleDateFormat("hh:mm");

				// Message is valid: format it to be pretty
				String formatted = String.format("[%s] %s: %s", ft.format(dNow), senderName, text);

				Message msg = new Message();
				msg.obj = formatted;

				ChatActivity.handler.sendMessage(msg);

			} catch (IOException e) {
				Log.d("register", "Chat ended: socket probably closed.");
				running = false;
				e.printStackTrace();
			} catch (Exception e) {
				running = false;
				e.printStackTrace();

			}
		}
	}
}
