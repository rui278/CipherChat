package com.ist.cipherchat.networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import android.util.Log;

import com.ist.cipherchat.gui.ChatActivity;

public class ChatActivityRunnable implements Runnable {

	public String thisUsername;
	private String otherUsername;
	private CyclicBarrier barrier;

	/** Unsecured socket for 'other' (B in the Needham-Shroeder protocol) */
	public static OutputSocketHandler thisSocket;

	public ChatActivity activity;

	public ChatActivityRunnable(CyclicBarrier barrier, String thisUsername, String otherUsername, ChatActivity activity) {
		this.thisUsername = thisUsername;
		this.otherUsername = otherUsername;
		this.activity = activity;
		this.barrier = barrier;
	}

	@Override
	public void run() {

		thisSocket = new OutputSocketHandler(thisUsername, otherUsername);

		// Ask KDC for IP for the other user
		if (!thisSocket.askOtherIp()) {
			// TODO GO BACK TO PREV ACTIVITY
			Log.d("register", ":( NO IP!!!");
			this.activity.finish();

		}

		// Open socket to the IP address gotten from the KDC
		if (!thisSocket.openSocket()) {
			// TODO GO BACK TO PREV ACTIVITY
			Log.d("register", "SOCK NOT OPENED!");
			this.activity.finish();
		}

		Log.d("register", "Socket opened with user " + otherUsername);

		// From here on, the Needham-Shroeder protocol in run

		// 1 and 2
		if (!thisSocket.sendStartMessage()) {
			this.activity.finish();

		}

		// 3 and 4
		if (!thisSocket.sessionKeyEnquiry()) {

			this.activity.finish();
		}

		// 5, 6, 7 and 8
		if (!thisSocket.connectToOther()) {

			this.activity.finish();
		}

		Log.d("register", "User A finished NS.");


		try {
			barrier.await();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		
		Thread t = new Thread(new ChatInRunnable());
		t.start();
	}
}
