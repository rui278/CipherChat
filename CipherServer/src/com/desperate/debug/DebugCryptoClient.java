package com.desperate.debug;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class DebugCryptoClient {
	final static String serverIP = "192.168.1.64";
	final static int portNumber = 1337;

	public static void main(String[] args) {
		try {
			// Setup truststore
			KeyStore trustStore = KeyStore.getInstance("JKS");
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			InputStream trustStoreStream = new FileInputStream("contribot.jks");
			trustStore.load(trustStoreStream, "123456".toCharArray());
			trustManagerFactory.init(trustStore);

			System.out.println("Loaded truststore.");

			// Setup keystore, same as truststore
			KeyStore keyStore = KeyStore.getInstance("JKS");
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			InputStream keyStoreStream = new FileInputStream("contribot.jks");
			keyStore.load(keyStoreStream, "123456".toCharArray());
			keyManagerFactory.init(keyStore, "123456".toCharArray());

			System.out.println("Loaded keystore.");

			// Setup the SSL context to use the truststore and keystore
			SSLContext ssl_ctx = SSLContext.getInstance("TLS");
			ssl_ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

			System.out.println("Setup SSL context");

			// Create socket factory
			SSLSocketFactory socketFactory = (SSLSocketFactory) ssl_ctx.getSocketFactory();

			System.out.println("Got SSL socket factory");

			// Finally create socket, which is in MainActivity class
			SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(serverIP, portNumber);
			// ((SSLSocket) sslSocket).startHandshake();

			System.out.println("Connected SSL socket");

			// This is just a prototype
			PrintWriter out = new PrintWriter(sslSocket.getOutputStream(), true);

			// Send through socket
			out.println("I'm pretending to be the Android App.");

			out.println("Another message from the Android App.");

			out.println("bye");

			sslSocket.close();

			System.out.println("Sent messages, Exited cleanly");
			System.exit(0);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
