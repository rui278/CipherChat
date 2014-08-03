package com.ist.cipherchat.networking;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.crypto.spec.SecretKeySpec;

public class Globals {

	private static Socket ClientSocket;
	private static Socket ServerSocket;

	private static String thisUsername;
	private static String otherUsername;

	private static SecretKeySpec sessionKey;

	private static ObjectInputStream ois;
	private static ObjectOutputStream oos;

	private Globals() {

	}

	public static synchronized Socket getClientSocket() {
		return ClientSocket;
	}

	public static synchronized void setClientSocket(Socket clientSocket) {
		ClientSocket = clientSocket;
	}

	public static synchronized Socket getServerSocket() {
		return ServerSocket;
	}

	public static synchronized void setServerSocket(Socket serverSocket) {
		ServerSocket = serverSocket;
	}

	public static synchronized String getThisUsername() {
		return thisUsername;
	}

	public static synchronized void setThisUsername(String thisUsername) {
		Globals.thisUsername = thisUsername;
	}

	public static synchronized String getOtherUsername() {
		return otherUsername;
	}

	public static synchronized void setOtherUsername(String otherUsername) {
		Globals.otherUsername = otherUsername;
	}

	public static synchronized SecretKeySpec getSessionKey() {
		return sessionKey;
	}

	public static synchronized void setSessionKey(SecretKeySpec sessionKey) {
		Globals.sessionKey = sessionKey;
	}

	public static synchronized ObjectInputStream getOis() {
		System.out.println("OIS gotten here");
		return ois;
	}

	public static synchronized void setOis(ObjectInputStream ois) {
		Globals.ois = ois;
		System.out.println("OIS set here");
	}

	public static synchronized ObjectOutputStream getOos() {
		System.out.println("OOS gotten here");
		return oos;

	}

	public static synchronized void setOos(ObjectOutputStream oos) {
		Globals.oos = oos;
		System.out.println("OOS set here");
	}
}
