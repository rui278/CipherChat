package com.ist.cipherchat.networking;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.concurrent.CyclicBarrier;

import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import android.content.Context;
import android.util.Log;

import com.desperate.common.messages.LoginMessage;
import com.desperate.common.replies.LoginReplyMessage;
import com.desperate.common.Message;
import com.desperate.common.messages.RegisterMessage;
import com.desperate.common.replies.RegisterReplyMessage;
import com.desperate.common.messages.UserListMessage;
import com.desperate.common.replies.UserListReplyMessage;
import com.desperate.common.Utilities;
import com.ist.cipherchat.R;
import com.ist.cipherchat.gui.Contacts;

public class Core implements Runnable {

	// Server IP and port, with default debug values
	String serverIP = "193.136.131.248";
	int portNumber = 1337;
	String[] user;
	
	String username;
	String password;
	String option;
	SecretKeySpec keyEncryptingKey;
	
	private SSLSocket sslSocket;

	CyclicBarrier barrier;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	
	Context androidContext;
	
	public Core(int portNumber, String serverIP, String username, String password, String option, Context context){

		this.username = username;
		this.password = password;
		this.serverIP = serverIP;
		this.portNumber = portNumber;
		this.androidContext = context;
		this.option = option;
	}
	
	
	@Override
	public void run() {

		Boolean Reglog = false;

		// Setup truststore
		loadSll();
				
		//Login Or register, according to user selected option
		if(option.equals("Log-In")){
			
			Reglog = serverlogIn(username, password);
			
		}else if(option.equals("Register")){
			
			Reglog = serverRegister(username, password);
		}
		
		
		//if register or login (hence reglog = reg+log) is successful, requests userList
		if(Reglog){
			requestUsers();
		}else{
			Contacts.users = new String[0];
		}
		
		//Has tried to register. 
		//if successful register has tried logeIn.
		//if successful login has tried to request user list.
		//if successful yay!(also, contacts can now generate activity.
		
		try {
			Contacts.barrier.await();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	private boolean serverRegister(String username2, String password2) {
		
		boolean registerBool = false;
		
		RegisterMessage message = new RegisterMessage(username2, password2);
		
		message.timestamp=System.currentTimeMillis();
		
		message.remoteHMAC = message.computeHMAC(password2);
		
		message.send(out);
		
		RegisterReplyMessage reply;
		
		try {
			
			
			reply = (RegisterReplyMessage) in.readObject();
			
			if(reply.remoteHMAC.equals(reply.computeHMAC(password2)) && reply.verifyTimestamp(Utilities.tolerance) && reply.accepted){
				registerBool = serverlogIn(username2, password2);
				Log.d("register", "Register successful!");
			}else{
				registerBool = false;
				Contacts.threadComm.error = true;
				Contacts.threadComm.registerFail = true;
				Log.d("register", "Register unsuccessful!");

			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return registerBool;
	}

	private boolean serverlogIn(String username2, String password2) {
		
		boolean loginBool = false;
		
		
		LoginMessage message = new LoginMessage(username2, password2);
		
		message.timestamp=System.currentTimeMillis();

		message.remoteHMAC = message.computeHMAC(password2);
		
		message.send(out);
		
		LoginReplyMessage reply;
		
		try {
			
			
			reply = (LoginReplyMessage) in.readObject();
			
			if(reply.remoteHMAC.equals(reply.computeHMAC(password2)) && reply.verifyTimestamp(Utilities.tolerance) && reply.accepted){
				loginBool = true;
				this.keyEncryptingKey = reply.keyEncryptionKey;
				Log.d("register", "Login successful!");

			}else{
				loginBool = false;
				Contacts.threadComm.error = true;
				Contacts.threadComm.loginFail = true;
				Log.d("register", "Login not successful!");
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return loginBool;
	}
	
	private boolean requestUsers() {

		boolean uRequestUsers = false;
		
		UserListMessage uMessage = new UserListMessage();
		
		uMessage.timestamp=System.currentTimeMillis();

		
		uMessage.remoteHMAC = uMessage.computeHMAC(getStrKek());
		
		uMessage.send(out);
		
		UserListReplyMessage reply;
		
		try {
						
			reply = (UserListReplyMessage) in.readObject();
			
			if(reply.remoteHMAC.equals(reply.computeHMAC(getStrKek())) && reply.verifyTimestamp(Utilities.tolerance) && reply.accepted){
				uRequestUsers = true;

				if(reply.usernames.length == 0){
					Contacts.threadComm.error = true;
					Contacts.threadComm.noUsers = true;
				}
				
				Contacts.users = reply.usernames;
			}else{
				uRequestUsers = false;
				Contacts.threadComm.error = true;
				Contacts.threadComm.contactsFail = true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return uRequestUsers;
	}
	
	public void sendMessage(Message message) {
		message.send(out);
	}
	
	public Message readMessage() {
		try {
			return (Message) in.readObject();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	public String getStrKek() {		
		try {
			return new String(keyEncryptingKey.getEncoded(), Utilities.charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public SecretKeySpec getKek(){
		return keyEncryptingKey;
	}


	private void loadSll() {

		try{	
			KeyStore trustStore = KeyStore.getInstance("BKS");
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			InputStream trustStoreStream = androidContext.getResources().openRawResource(R.raw.contribot_bks);
			trustStore.load(trustStoreStream, "123456".toCharArray());
			trustManagerFactory.init(trustStore);
	
			Log.d("socket creation", "Loaded truststore.");
	
			// Setup keystore, same as truststore
			KeyStore keyStore = KeyStore.getInstance("BKS");
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			InputStream keyStoreStream = androidContext.getResources().openRawResource(R.raw.contribot_bks);
			keyStore.load(keyStoreStream, "123456".toCharArray());
			keyManagerFactory.init(keyStore, "123456".toCharArray());
	
			Log.d("socket creation", "Loaded keystore.");
	
			// Setup the SSL context to use the truststore and keystore
			SSLContext ssl_ctx = SSLContext.getInstance("TLS");
			ssl_ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
	
			Log.d("socket creation", "Setup SSL context");
	
			// Create socket factory
			SSLSocketFactory socketFactory = (SSLSocketFactory) ssl_ctx.getSocketFactory();
	
			Log.d("socket creation", "Got SSL socket factory");
	
			// Finally create socket, which is in Chat class
			sslSocket = (SSLSocket) socketFactory.createSocket(serverIP, portNumber);
			((SSLSocket) sslSocket).startHandshake();
	
			Log.d("socket creation", "Connected SSL socket");

			Thread.sleep(100);
			out = new ObjectOutputStream(sslSocket.getOutputStream());
			Log.d("socket creation", "out created");
			in = new ObjectInputStream(sslSocket.getInputStream());

		}		
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}