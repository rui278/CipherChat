package com.ist.cipherchat.networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;

import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

import com.desperate.common.Message;
import com.desperate.common.SessionKeyRequestInfo;
import com.desperate.common.Utilities;
import com.desperate.common.messages.IPMessage;
import com.desperate.common.messages.SessionKeyRequestMessage;
import com.desperate.common.messages.StartChatMessage;
import com.desperate.common.replies.CheckSessionMessage;
import com.desperate.common.replies.IPReplyMessage;
import com.desperate.common.replies.NeedhamSchroederSuccessReply;
import com.desperate.common.replies.ReplyMessage;
import com.desperate.common.replies.SessionKeyReplyMessage;
import com.desperate.common.replies.StartChatReply;
import com.ist.cipherchat.gui.Contacts;

public class OutputSocketHandler {

	public static final int PORT = 7331;
	public static Socket socket;
	private InetAddress otherIP;

	private String thisUsername;
	private String otherUsername;

	private ObjectInputStream in;

	private ObjectOutputStream out;

	/** Eb(A, Rb) sent by the other user on step 2, and then received by KDC with the SessionKey inside. It will be re-sent to Bob on step 5. */
	private byte[] otherUserFreshnessPacket;
	private SessionKeyRequestInfo sessionKeyInfo;

	private SecretKeySpec sessionKey;

	private SecureRandom secRand = new SecureRandom();

	public OutputSocketHandler(String thisUsername, String otherUsername) {
		this.thisUsername = thisUsername;
		this.otherUsername = otherUsername;
		Globals.setOtherUsername(otherUsername);
	}

	// 3 e 4
	boolean sessionKeyEnquiry() {

		long randomVal = secRand.nextLong();

		SessionKeyRequestMessage message = new SessionKeyRequestMessage(thisUsername, otherUsername, randomVal, otherUserFreshnessPacket);

		message.timestamp = System.currentTimeMillis();

		message.remoteHMAC = message.computeHMAC(Contacts.sslSocketHandler.getStrKek());

		Contacts.sslSocketHandler.sendMessage(message);

		SessionKeyReplyMessage reply;

		try {
			reply = (SessionKeyReplyMessage) Contacts.sslSocketHandler.readMessage();

			// Check HMAC and timestamp for integrity and freshness
			if (!reply.remoteHMAC.equals(reply.computeHMAC(Contacts.sslSocketHandler.getStrKek()))) {
				return false;
			}
			if (!reply.verifyTimestamp(Utilities.tolerance)) {
				return false;
			}

			sessionKeyInfo = reply.getInfo(Contacts.sslSocketHandler.getKek());

			sessionKey = sessionKeyInfo.sessionKey;
			Globals.setSessionKey(sessionKey);

			otherUserFreshnessPacket = sessionKeyInfo.cipheredBPacket;

			if (sessionKeyInfo.nonceA != randomVal) {
				StartChatReply errToBMessage = new StartChatReply(new byte[0]);

				sendErrorMessage(errToBMessage, "Wrong Nonce. Blame KDC", "");
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * Executes steps 5, 6 and 7 from Alice side
	 * 
	 * @return
	 */
	boolean connectToOther() {
		// StartChatReply
		// CheckSessionMessage

		StartChatReply sessionKeyMessage = new StartChatReply(otherUserFreshnessPacket);
		sessionKeyMessage.timestamp = System.currentTimeMillis();
		sessionKeyMessage.accepted = true;
		sessionKeyMessage.remoteHMAC = sessionKeyMessage.computeHMAC(Utilities.keyToString(sessionKey));
		// Bob's reply must come with HMAC with session key as secret

		// Send 5
		sessionKeyMessage.send(out);

		// Receive Nonce (6)
		CheckSessionMessage sessionNonce = (CheckSessionMessage) readMessage();

		if (!sessionNonce.accepted) {
			// received error
			Log.d("NEEDHAM_SCHOEDER", "Client who I connected to died.");
			return false;
		}

		String localHMAC = sessionNonce.computeHMAC(Utilities.keyToString(sessionKey));

		if (!sessionNonce.remoteHMAC.equals(localHMAC)) {
			sendErrorMessage(sessionKeyMessage, "Wrong verification hash while connecting.", Utilities.keyToString(sessionKey));
			return false;
		}

		if (!sessionNonce.verifyTimestamp(Utilities.tolerance)) {
			sendErrorMessage(sessionKeyMessage, "Your freshness failed  while connecting.", Utilities.keyToString(sessionKey));
			return false;
		}

		Long nonce = sessionNonce.getNonce(sessionKey);

		nonce--;

		CheckSessionMessage replySessionNonce = new CheckSessionMessage();

		replySessionNonce.setNonce(nonce, sessionKey);
		replySessionNonce.timestamp = System.currentTimeMillis();
		replySessionNonce.remoteHMAC = replySessionNonce.computeHMAC(Utilities.keyToString(sessionKey));

		// Send 7
		replySessionNonce.send(out);

		// Receive 8
		NeedhamSchroederSuccessReply nsFinished = (NeedhamSchroederSuccessReply) readMessage();

		// TODO verify correctness of nsFinished (hmac, etc)

		// Keep in and out for future chat
		Globals.setOis(in);
		Globals.setOos(out);

		return true;

	}

	boolean openSocket() {
		try {

			OutputSocketHandler.socket = new Socket(otherIP, PORT);

			Globals.setClientSocket(socket);

			this.out = new ObjectOutputStream(socket.getOutputStream());
			this.in = new ObjectInputStream(socket.getInputStream());

			return true;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	boolean askOtherIp() {

		boolean getIpBoolean = true;

		IPMessage ipmessage = new IPMessage(otherUsername);

		ipmessage.timestamp = System.currentTimeMillis();

		ipmessage.remoteHMAC = ipmessage.computeHMAC(otherUsername);

		Contacts.sslSocketHandler.sendMessage(ipmessage);

		IPReplyMessage ipreply;

		try {

			ipreply = (IPReplyMessage) Contacts.sslSocketHandler.readMessage();

			if (ipreply.remoteHMAC.equals(ipreply.computeHMAC(otherUsername)) && ipreply.verifyTimestamp(Utilities.tolerance) && ipreply.accepted
					&& ipreply != null) {

				otherIP = ipreply.IPaddress;

				getIpBoolean = true;

				Log.d("register", this.otherIP.toString());

			} else {
				getIpBoolean = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return getIpBoolean;
	}

	boolean sendStartMessage() {

		boolean startChatBool = true;

		StartChatMessage startChatMessage = new StartChatMessage(this.thisUsername);

		startChatMessage.timestamp = System.currentTimeMillis();

		sendMessage(startChatMessage);

		StartChatReply reply = (StartChatReply) readMessage();

		if (reply.accepted && reply != null) {
			startChatBool = true;

			this.otherUserFreshnessPacket = reply.cipheredNoncePacket;

			try {
				Log.d("register", new String(otherUserFreshnessPacket, Utilities.charset));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

		} else {
			startChatBool = false;
		}

		return startChatBool;
	}

	/**
	 * read from thisAndroid to otherAndroid (the B entity)
	 * 
	 * @return
	 */
	public Message readMessage() {
		try {

			return (Message) in.readObject();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * send to otherAndroid (the B entity)
	 * 
	 * @param message
	 */
	public void sendMessage(Message message) {
		message.send(out);
	}

	private void sendErrorMessage(ReplyMessage message, String error, String hmacKey) {
		message.accepted = false;
		message.errorMessage = error;
		message.timestamp = System.currentTimeMillis();
		message.remoteHMAC = message.computeHMAC(hmacKey);
		message.send(out);
	}

	/**
	 * @return Gets the sessionKey
	 */
	public SecretKeySpec getSessionKey() {
		return sessionKey;
	}

	/**
	 * @return Gets the in
	 */
	public ObjectInputStream getSocketInputStream() {
		return in;
	}
}
