package com.ist.cipherchat.networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;

import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.desperate.common.NoncePacket;
import com.desperate.common.Utilities;
import com.desperate.common.messages.StartChatMessage;
import com.desperate.common.replies.CheckSessionMessage;
import com.desperate.common.replies.NeedhamSchroederSuccessReply;
import com.desperate.common.replies.ReplyMessage;
import com.desperate.common.replies.StartChatReply;
import com.ist.cipherchat.gui.ChatActivity;
import com.ist.cipherchat.gui.Contacts;
import com.ist.cipherchat.gui.Origin;

public class PhoneServerSocketHandler implements Runnable {

	public static String EXTRA_ENUM = "com.ist.cryptochat.EXTRA_ENUM";
	public static int PHONE_SERVER_TO_CHAT = 555;

	public static ServerSocket serverSocket;
	static Socket clientSocket;

	public ObjectInputStream in;
	private ObjectOutputStream out;

	private Context context;
	private Activity callerActivity;

	private String otherUsername;

	private SecureRandom rand = new SecureRandom();
	private Long nonce;
	private SecretKeySpec sessionKey;

	public PhoneServerSocketHandler(Context context, Activity callerActivity) {
		this.context = context;
		this.callerActivity = callerActivity;

		try {
			PhoneServerSocketHandler.serverSocket = new ServerSocket(OutputSocketHandler.PORT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Log.d("register", "Listening on port" + OutputSocketHandler.PORT);

		if (serverSocket.isBound()) {
			Log.d("register", "bound on " + OutputSocketHandler.PORT);
		}

		try {
			PhoneServerSocketHandler.clientSocket = serverSocket.accept();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Globals.setServerSocket(clientSocket);
		Log.d("register", "accepted on port" + OutputSocketHandler.PORT + clientSocket.getInetAddress().toString());

		try {

			out = new ObjectOutputStream(PhoneServerSocketHandler.clientSocket.getOutputStream());

			in = new ObjectInputStream(PhoneServerSocketHandler.clientSocket.getInputStream());

			startChatMessage();

			recieveSessionKey();

			callActivity();

		} catch (Exception e) {
			e.printStackTrace();
		}

		Log.d("register", "User B finished NS.");

		// Keep in and out for future chat
		Globals.setOis(in);
		Globals.setOos(out);
		
		// ChatInRunnable chatIn = new ChatInRunnable(in, otherUsername, sessionKey);
		ChatInRunnable chatIn = new ChatInRunnable();

		Thread t = new Thread(chatIn);
		t.start();

	}

	/**
	 * Messages 5, 6 and 7, and nonce verification (8)
	 */
	private void recieveSessionKey() {

		NoncePacket noncePacket;

		// Message 6, has Rc
		CheckSessionMessage message = new CheckSessionMessage();

		// check message 5
		try {
			StartChatReply reply = (StartChatReply) in.readObject();

			noncePacket = (NoncePacket) Utilities.decipherObject(reply.cipheredNoncePacket, Contacts.sslSocketHandler.getKek());

			if (!reply.verifyTimestamp(Utilities.tolerance)) {
				sendErrorMessage(message, "Wrong time-stamp on NS (step 5)", Utilities.keyToString(noncePacket.sessionKey));
				return;
			}

			if (!reply.remoteHMAC.equals(reply.computeHMAC(Utilities.keyToString(noncePacket.sessionKey)))) {
				sendErrorMessage(message, "Wrong HMAC on NS (step 5)", Utilities.keyToString(noncePacket.sessionKey));
				return;
			}

			// Check Rb
			if (!noncePacket.bNonce.equals(nonce) || !noncePacket.requesterUserName.equals(this.otherUsername)) {
				message.accepted = false;

			} else {
				message.accepted = true;

				Globals.setOtherUsername(otherUsername);

				this.sessionKey = noncePacket.sessionKey;
				Globals.setSessionKey(sessionKey);

				// Generate a second nonce, to guarantee A knows the sessionKey
				nonce = rand.nextLong();
				message.setNonce(nonce, sessionKey);
			}

			message.timestamp = System.currentTimeMillis();

			message.remoteHMAC = message.computeHMAC(Utilities.keyToString(sessionKey));

			message.send(out);

			// Receive nonce, decremented
			CheckSessionMessage decrementedNonce = (CheckSessionMessage) in.readObject();
			NeedhamSchroederSuccessReply finalMessage = new NeedhamSchroederSuccessReply();

			// Check HMAC
			if (!decrementedNonce.remoteHMAC.equals(decrementedNonce.computeHMAC(Utilities.keyToString(sessionKey)))) {
				sendErrorMessage(message, "Wrong HMAC on NS (step 7)", Utilities.keyToString(noncePacket.sessionKey));
				return;
			}

			// Check timestamp
			if (!decrementedNonce.verifyTimestamp(Utilities.tolerance)) {
				sendErrorMessage(message, "Wrong TS on NS (step 7)", Utilities.keyToString(noncePacket.sessionKey));
				return;
			}

			Long decNonce = (Long) Utilities.decipherObject(decrementedNonce.bobNonce, sessionKey);
			if (!decNonce.equals(nonce - 1)) {
				sendErrorMessage(message, "Wrong nonce on NS (step 7)", Utilities.keyToString(noncePacket.sessionKey));
				return;
			}

			finalMessage.accepted = true;
			finalMessage.timestamp = System.currentTimeMillis();
			finalMessage.remoteHMAC = finalMessage.computeHMAC(Utilities.keyToString(sessionKey));

			// NS concluded, send message 8
			finalMessage.send(out);


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void startChatMessage() {

		byte[] cipheredNoncePacket;

		try {
			StartChatMessage chatMessage = (StartChatMessage) in.readObject();

			this.otherUsername = chatMessage.requesterUserName;

			nonce = rand.nextLong();

			NoncePacket packet = new NoncePacket(otherUsername, nonce);

			packet.sessionKey = null;

			cipheredNoncePacket = Utilities.cipherObject(packet, Contacts.sslSocketHandler.getKek());

			StartChatReply reply = new StartChatReply(cipheredNoncePacket);

			reply.send(out);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public SecretKeySpec getSessionKey() {
		return sessionKey;
	}

	public String getStrSessionKey() {
		return new String(sessionKey.getEncoded());
	}

	private void callActivity() {
		Intent chatIntent = new Intent(context, ChatActivity.class);

		Origin origin = Origin.PHONESERVERSOCKET;

		chatIntent.putExtra("EXTRA_ENUM", origin.toString());

		chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		callerActivity.startActivityForResult(chatIntent, PHONE_SERVER_TO_CHAT);
	}

	private void sendErrorMessage(ReplyMessage message, String error, String hmacKey) {
		message.accepted = false;
		message.errorMessage = error;
		message.timestamp = System.currentTimeMillis();
		message.remoteHMAC = message.computeHMAC(hmacKey);
		message.send(out);
	}

	public ObjectOutputStream getObjectOutputStream() {
		return out;
	}
}
