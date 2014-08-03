package com.desperate;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSocket;

import com.desperate.common.Message;
import com.desperate.common.NoncePacket;
import com.desperate.common.SessionKeyRequestInfo;
import com.desperate.common.Utilities;
import com.desperate.common.messages.IPMessage;
import com.desperate.common.messages.LoginMessage;
import com.desperate.common.messages.LogoutMessage;
import com.desperate.common.messages.RegisterMessage;
import com.desperate.common.messages.SessionKeyRequestMessage;
import com.desperate.common.messages.UserListMessage;
import com.desperate.common.replies.IPReplyMessage;
import com.desperate.common.replies.LoginReplyMessage;
import com.desperate.common.replies.LogoutReplyMessage;
import com.desperate.common.replies.RegisterReplyMessage;
import com.desperate.common.replies.ReplyMessage;
import com.desperate.common.replies.SessionKeyReplyMessage;
import com.desperate.common.replies.UserListReplyMessage;

public class ClientHandler implements Runnable {

	private SSLSocket clientSocket;
	private ObjectOutputStream outStream;
	private ObjectInputStream inStream;

	/** True while client is active with server */
	private transient boolean isActive;

	private User user;

	public ClientHandler(SSLSocket clientSocket) {

		this.clientSocket = clientSocket;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		try {
			System.out.println("Accepted" + clientSocket.getRemoteSocketAddress().toString());

			outStream = new ObjectOutputStream(clientSocket.getOutputStream());
			inStream = new ObjectInputStream(clientSocket.getInputStream());

			// Receive messages, follow protocol
			Message requestMessage;

			isActive = true;
			boolean isLoggedIn = false;

			// Attempt to login or register before handling any other messages
			try {
				while (isLoggedIn == false && (requestMessage = (Message) inStream.readObject()) != null) {

					System.out.println(requestMessage.toString());
					
					if (requestMessage instanceof RegisterMessage) {
						handleRegister((RegisterMessage) requestMessage);
					}

					else if (requestMessage instanceof LoginMessage) {
						handleLogin((LoginMessage) requestMessage);
						isLoggedIn = true;
					}
				}
			} catch (Exception e) {

				if (user != null)
					user.setOnlineStatus(false);
				System.out.println("Client " + clientSocket.getRemoteSocketAddress().toString() + " could not register or log in.");
				clientSocket.close();
				return;
			}

			// Handle other messages
			try {
				while (isActive && (requestMessage = (Message) inStream.readObject()) != null) {

					// if (requestMessage instanceof RegisterMessage) {
					// handleRegister((RegisterMessage) requestMessage);
					// }
					//
					// else if (requestMessage instanceof LoginMessage) {
					// handleLogin((LoginMessage) requestMessage);
					// }
					
					System.out.println(requestMessage.toString());

					if (requestMessage instanceof LogoutMessage) {
						handleLogout((LogoutMessage) requestMessage);
						break;
					}

					else if (requestMessage instanceof UserListMessage) {
						handleUserListRequest((UserListMessage) requestMessage);
					}

					else if (requestMessage instanceof IPMessage) {
						handleIPRequest((IPMessage) requestMessage);

					} else if (requestMessage instanceof SessionKeyRequestMessage) {
						handleSessionKeyRequest((SessionKeyRequestMessage) requestMessage);
					}
				}
			} catch (RuntimeException e) {

				e.printStackTrace();
				if (user != null)
					user.setOnlineStatus(false);
			}

			// Out of listening loop

			System.out.println("Closing socket.");
			System.out.println("User " + user != null ? user.userName : clientSocket.getRemoteSocketAddress().toString() + " disconnected.");
			if (user != null) {
				user.setOnlineStatus(false);
			}

			clientSocket.close();
		} catch (IOException e) {

			System.out.println("IOException probably because client " + (user != null ? user.userName : clientSocket.getRemoteSocketAddress())
					+ " committed sudoku. Socket closed.");

			if (user != null)
				user.setOnlineStatus(false);

		} catch (ClassNotFoundException e) {
			System.err.println("Client sent unknown message.");
			e.printStackTrace();
		}
	}

	private void handleLogin(LoginMessage message) {

		LoginReplyMessage reply = new LoginReplyMessage();

		String mRemote = message.remoteHMAC;
		String mServer = message.computeHMAC(message.password);

		// Check HMAC
		if (!mRemote.equals(mServer)) {
			// Login rejected
			sendErrorMessage(reply, "Login failed: wrong HMAC");
			return;
		}

		// Check freshness
		if (!message.verifyTimestamp(Utilities.tolerance)) {

			// Login rejected due to time-out
			sendErrorMessage(reply, "Login timed out");
		}

		// Try to find user
		User user;
		try {

			user = CryptoServer.db.getUser(message.username);

		} catch (Exception e) {

			sendErrorMessage(reply, "User does not exist, please register.");
			return;
		}

		// Attempt to authenticate with given password
		boolean loginStatus;
		try {
			loginStatus = user.authenticate(message.password);
			user.setOnlineStatus(true);

		} catch (NoSuchAlgorithmException e) {

			sendErrorMessage(reply, "Log-in failed: Could not authenticate");
			return;
		}

		if (loginStatus == false) {

			sendErrorMessage(reply, "Log-in failed: Wrong password.");

			return;
		}

		// Login successful - reply to client with KEK
		this.user = user;
		user.ip = clientSocket.getInetAddress();

		reply = new LoginReplyMessage();

		reply.timestamp = System.currentTimeMillis();
		reply.accepted = true;
		reply.keyEncryptionKey = user.getKeyEncryptionKey();

		reply.remoteHMAC = reply.computeHMAC(message.password);

		reply.send(outStream);

		return;

	}

	private void handleLogout(LogoutMessage message) {

		LogoutReplyMessage reply = new LogoutReplyMessage();

		String mRemote = message.remoteHMAC;
		String mServer = message.computeHMAC(user.getStrKeyEncryptionKey());

		// Check HMAC
		if (!mRemote.equals(mServer)) {
			// Logout rejected
			sendErrorMessage(reply, "Invalid Log-Out Message.");

			return;
		}

		if (!message.verifyTimestamp(Utilities.tolerance)) {
			sendErrorMessage(reply, "Log-Out Message Time-out.");

			return;
		}

		// Finish Thread on user log-out
		isActive = false;

		user.setOnlineStatus(false);
		System.out.print("User" + user.userName + "has logged out.");

		return;
	}

	private void handleRegister(RegisterMessage requestMessage) {

		RegisterReplyMessage reply = new RegisterReplyMessage(false, System.currentTimeMillis());

		// Don't let already-logged-in user register again
		if (user != null) {
			sendErrorMessage(reply, "You are already logged in.");
			return;
		}

		if (!requestMessage.verifyTimestamp(Utilities.tolerance)) {
			sendErrorMessage(reply, "Message expired.");
			return;
		}

		User newUser;
		try {
			newUser = new User(requestMessage.username, requestMessage.password, clientSocket.getInetAddress(), clientSocket);

		} catch (NoSuchAlgorithmException e) {

			sendErrorMessage(reply, "Unable to create user.");
			e.printStackTrace();
			return;
		}

		if (CryptoServer.db.addUser(newUser)) {
			// User was accepted
			this.user = newUser;

			reply = new RegisterReplyMessage(true, System.currentTimeMillis());
			reply.remoteHMAC = reply.computeHMAC(requestMessage.password);
			reply.send(outStream);
			return;

		} else {
			sendErrorMessage(reply, "Registration failed: user already exists.");
			return;
		}
	}

	private void handleIPRequest(IPMessage message) {

		IPReplyMessage reply = new IPReplyMessage();

		String mRemote = message.remoteHMAC;
		String mServer = message.computeHMAC(message.username);

		// Check HMAC
		if (!mRemote.equals(mServer)) {
			// IPRequest rejected
			sendErrorMessage(reply, "Invalid IPRequest Message: wrong HMAC");

			return;
		}

		// Check timestamp
		if (!message.verifyTimestamp(Utilities.tolerance)) {
			sendErrorMessage(reply, "User IP request message time-out.");

			return;
		}

		reply.IPaddress = CryptoServer.db.getIP(message.username);

		if (reply.IPaddress == null) {
			sendErrorMessage(reply, "User " + message.username + " is offline.");
			return;
		}

		reply.username = message.username;
		reply.timestamp = System.currentTimeMillis();

		reply.remoteHMAC = reply.computeHMAC(message.username);

		reply.send(outStream);

		return;
	}

	/**
	 * Handles the following messages
	 * <p>
	 * <b>A -> KDC:</b> A, B, Ra, Eb(A, Rb) <b><br>
	 * KDC -> A:</b> Ea(Ra, B, Ks, Eb(A, Rb, Ks)))
	 * 
	 * @param message
	 */
	private void handleSessionKeyRequest(SessionKeyRequestMessage message) {

		SessionKeyReplyMessage reply = new SessionKeyReplyMessage();

		String localHMAC = message.computeHMAC(user.getStrKeyEncryptionKey());

		// Check HMAC validity
		if (!message.remoteHMAC.equals(localHMAC)) {
			sendErrorMessage(reply, "Session key request rejected: wrong HMAC.");
			return;
		}

		if (!message.verifyTimestamp(Utilities.tolerance)) {
			sendErrorMessage(reply, "Chat buddy resquest time-out.");
			return;
		}

		// Verify if users exist
		User userA;
		User userB;
		try {
			userA = CryptoServer.db.getUser(message.usernameA);
			userB = CryptoServer.db.getUser(message.usernameB);
		} catch (Exception e) {
			sendErrorMessage(reply, "Invalid users.");
			e.printStackTrace();
			return;
		}

		// Get keys from users
		SecretKeySpec keyA = userA.getKeyEncryptionKey();
		SecretKeySpec keyB = userB.getKeyEncryptionKey();

		NoncePacket bInfo;

		// Decrypt B's encrypted packet containing its freshness nonce, to append the session key inside
		try {

			bInfo = (NoncePacket) Utilities.decipherObject(message.userBPacket, keyB);

		} catch (Exception e) {

			sendErrorMessage(reply, "Could not verify other user's permission to start chat.");
			e.printStackTrace();
			return;
		}

		// confirm if requester is who he says it is
		if (!message.usernameA.equals(bInfo.requesterUserName)) {
			sendErrorMessage(reply, "Wrong request for chat session.");
			return;
		}

		// Generate a session key for A and B to use in communication
		SecretKeySpec sessionKey;
		try {

			KeyGenerator keyGen = KeyGenerator.getInstance(Utilities.cipherAlgorithm);
			keyGen.init(Utilities.keySize);
			sessionKey = (SecretKeySpec) keyGen.generateKey();

		} catch (NoSuchAlgorithmException e) {

			sendErrorMessage(reply, "Could not create session key for secure communication.");
			e.printStackTrace();
			return;
		}

		// Append session key
		bInfo.sessionKey = sessionKey;

		// Re-cipher nonce packet
		byte[] cipheredNonce;
		try {

			cipheredNonce = Utilities.cipherObject(bInfo, keyB);

		} catch (Exception e) {

			sendErrorMessage(reply, "Could not create session key for secure communication.");
			e.printStackTrace();
			return;
		}

		SessionKeyRequestInfo replyInfo = new SessionKeyRequestInfo(userB.userName, message.randomA, sessionKey, cipheredNonce);

		// Cipher the whole message contents with A's key
		byte[] cipheredReplyInfo;
		try {

			cipheredReplyInfo = Utilities.cipherObject(replyInfo, keyA);
		} catch (Exception e) {

			sendErrorMessage(reply, "Could not cipher KDC response");
			e.printStackTrace();
			return;
		}

		reply.cipheredPacket = cipheredReplyInfo;

		// Assemble rest of reply
		reply.accepted = true;
		reply.timestamp = System.currentTimeMillis();
		reply.remoteHMAC = reply.computeHMAC(user.getStrKeyEncryptionKey());

		// Send reply
		reply.send(outStream);

		return;
	}

	/**
	 * Sends the list of all online users to the client, except the client himself.
	 * 
	 * @return true if the request and response were successfully formed
	 */
	private void handleUserListRequest(UserListMessage message) {

		UserListReplyMessage userList = new UserListReplyMessage();

		String mRemote = message.remoteHMAC;
		String mServer = message.computeHMAC(user.getStrKeyEncryptionKey());

		// Check HMAC
		if (!mRemote.equals(mServer)) {
			// Request rejected
			sendErrorMessage(userList, "Invalid Log-Out Message: wrong HMAC");
			return;
		}

		if (!message.verifyTimestamp(Utilities.tolerance)) {
			sendErrorMessage(userList, "User List request message time-out.");
			return;
		}

		userList.usernames = CryptoServer.db.getOnlineUserList(user);
		userList.timestamp = System.currentTimeMillis();
		userList.remoteHMAC = userList.computeHMAC(user.getStrKeyEncryptionKey());
		userList.send(outStream);

		return;
	}

	private void sendErrorMessage(ReplyMessage message, String error) {
		message.accepted = false;
		message.errorMessage = error;
		message.timestamp = System.currentTimeMillis();
		message.send(outStream);

		System.out.println("error for client " + ((user != null && user.userName != null) ? user.userName : "???") + ": "
				+ message.getClass().getSimpleName());
	}
}
