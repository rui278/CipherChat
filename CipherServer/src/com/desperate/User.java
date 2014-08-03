package com.desperate;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSocket;

import com.desperate.common.Utilities;

public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int SALT_SIZE = 32;

	static private SecureRandom random = new SecureRandom();

	String userName;
	byte[] passHash;
	byte[] salt;

	/** All users are offline when they are created/loaded from disk */
	private transient boolean isOnline = false;

	/** Medium-term secret shared with the KDC for establishment of session keys. 256 bits (32 bytes) */
	private transient SecretKeySpec keyEncryptionKey;

	transient InetAddress ip;

	public transient SSLSocket commSocket;

	/**
	 * Creates a new User.
	 * 
	 * @param name
	 *            user name
	 * @param password
	 *            plaintext password
	 * @throws NoSuchAlgorithmException
	 */
	public User(String name, String password, InetAddress ip, SSLSocket commSocket) throws NoSuchAlgorithmException {

		// TODO store password as char[] to avoid disassemble/memory dump attacks (Strings are immutable)
		this.userName = name;

		this.salt = new byte[SALT_SIZE];
		random.nextBytes(salt);

		// Hash the salt and the password
		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		digest.update(password.getBytes());
		digest.update(salt);

		this.passHash = digest.digest();

		// Save session-dependant
		this.ip = ip;

		this.commSocket = commSocket;
	}

	/**
	 * Sets the current state of the user: online or offline. If set to offline, the KEK is emptied.
	 * 
	 * @param newStatus
	 *            New status
	 */
	public synchronized void setOnlineStatus(boolean newStatus) {

		// DEBUG
		if (this.isOnline == newStatus) {
			String status = newStatus ? "online" : "offline";
			System.out.println("Client " + userName + " went " + status + " while already being " + status);

		}

		this.isOnline = newStatus;

		if (newStatus == false) {
			keyEncryptionKey = null;
			ip = null;
			commSocket = null;
		}
	}

	@Override
	public synchronized boolean equals(Object obj) {

		if (obj == null) {
			return false;
		} else {
			return ((User) obj).userName.equals(this.userName);
		}
	}

	public synchronized boolean getOnlineStatus() {

		return isOnline;
	}

	/**
	 * Logs the user in. Checks if the password is correct. Generates a new KeyEncryptionKey for the user. The key can be gotten through
	 * {@link #getKeyEncryptionKey()}
	 * 
	 * @param password
	 *            Password the person tried to use
	 * @return True if the password is correct
	 * @throws NoSuchAlgorithmException
	 *             if SHA-256 or AES are not available
	 */
	public synchronized boolean authenticate(String password) throws NoSuchAlgorithmException {

		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		digest.update(password.getBytes());
		digest.update(salt);

		byte[] potencialHash = digest.digest();

		// If lengths don't match, hashes might've been somehow tampered with
		if (potencialHash.length != passHash.length)
			return false;

		// Compare every byte
		for (int i = 0; i < potencialHash.length; i++) {
			if (potencialHash[i] != passHash[i])
				return false;
		}

		// Create the key encryption key
		KeyGenerator keyGen = KeyGenerator.getInstance(Utilities.cipherAlgorithm);
		keyGen.init(Utilities.keySize);
		keyEncryptionKey = (SecretKeySpec) keyGen.generateKey();

		// If didn't return, hashes were equal
		return true;
	}

	/**
	 * Gets the KEK used between this user and the server when starting a new chat. Is attributed on login.
	 * 
	 * @return
	 */
	public synchronized SecretKeySpec getKeyEncryptionKey() {
		return keyEncryptionKey;
	}

	public synchronized String getStrKeyEncryptionKey() {

		try {
			return new String(keyEncryptionKey.getEncoded(), Utilities.charset);

		} catch (UnsupportedEncodingException e) {
			System.out.print("Error: Invalid conversion of key");
			e.printStackTrace();

			return null;
		} catch (NullPointerException e) {
			e.printStackTrace();
			throw e;
		}

	}
}
