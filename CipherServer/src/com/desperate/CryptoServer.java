package com.desperate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

public class CryptoServer {

	static UserDatabase db;
	private static volatile boolean running = true;
	private static ServerSocket serverSocket;

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

		System.out.println("DEBUG VERSION");
		
		int portNumber = 1337;

		printLocalAddress();

		// Get the user database from disk
		db = UserDatabase.load();

		// Launch the admin console, which will run in background forever
		Thread adminThread = new Thread(new AdminConsole());
		adminThread.start();

		// Get the password for the keystore
		// TODO take this out of here for final version
		char[] keystorePass;
		// if (System.console() != null)

		// We know this is a major security flaw, but we hard-coded the password here so this would be easy to run for the teachers
		// System.out.println("We know this is a major security flaw, but we hard-coded the password here so this would be easy to run for the teachers");
		keystorePass = "123456".toCharArray();
		// keystorePass = System.console().readPassword();

		// Open socket where clients will first connect to
		serverSocket = startServerSocket(portNumber, keystorePass);
		System.out.println("SSL Socket listening on port " + portNumber);

		while (running) {
			try {

				// Accept a client
				SSLSocket clientSocket = (SSLSocket) serverSocket.accept();

				// Filter cipher suites, enable only ones using Diffie-Hellman and SHA
				String[] cipherSuites = clientSocket.getSupportedCipherSuites();
				ArrayList<String> desiredSuites = new ArrayList<String>(cipherSuites.length);

				for (int i = 0; i < cipherSuites.length; i++) {

					if (cipherSuites[i].contains("DH") && cipherSuites[i].contains("SHA")) {
						desiredSuites.add(cipherSuites[i]);
						System.out.println(cipherSuites[i]);
					}
				}

				String[] trimmed = (String[]) desiredSuites.toArray(new String[desiredSuites.size()]);
				clientSocket.setEnabledCipherSuites(trimmed);
				
				System.out.println("Selected cipher suite: " + clientSocket.getSession().getCipherSuite());

				// Create a handler thread, and let it run
				Thread t = new Thread(new ClientHandler(clientSocket));
				t.start();
			} catch (SocketException e) {
				System.out.println("Server socket stopped.");
			}
		}

		serverSocket.close();

		UserDatabase.save(db);
		System.out.println("Server closed.");

	}

	/**
	 * Starts the server SSL socket.
	 * 
	 * @param portNumber
	 *            Port where the socket will listen
	 * @param password
	 *            Password for the keystore. Will be cleared immediately after it's used.
	 * @return
	 */
	private static SSLServerSocket startServerSocket(int portNumber, char[] password) {

		try {
			// Setup truststore
			KeyStore trustStore = KeyStore.getInstance("JKS");
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			InputStream trustStoreStream = new FileInputStream("contribot.jks");
			trustStore.load(trustStoreStream, password);
			trustManagerFactory.init(trustStore);

			// Setup keystore, same as truststore
			KeyStore keyStore = KeyStore.getInstance("JKS");
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			InputStream keyStoreStream = new FileInputStream("contribot.jks");
			keyStore.load(keyStoreStream, password);
			keyManagerFactory.init(keyStore, password);

			// Clear password from memory
			Arrays.fill(password, ' ');

			// Setup the SSL context to use the truststore and keystore
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

			// Get the socket factory
			SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

			// And finally, get the socket
			SSLServerSocket serverSocket = (SSLServerSocket) socketFactory.createServerSocket(portNumber);
			return serverSocket;

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static void printLocalAddress() {

		System.out.println("Available interfaces:");

		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

			while (interfaces.hasMoreElements()) {
				NetworkInterface current = interfaces.nextElement();

				if (!current.isUp() || current.isLoopback() || current.isVirtual())
					continue;

				Enumeration<InetAddress> addresses = current.getInetAddresses();

				while (addresses.hasMoreElements()) {
					InetAddress current_addr = addresses.nextElement();

					if (current_addr.isLoopbackAddress())
						continue;

					System.out.println("\t" + current_addr.getHostAddress() + " on " + current.getDisplayName());
				}
			}

			System.out.println();

		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Attempts to stop the server gracefully.
	 */
	public static void stop() {
		running = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
