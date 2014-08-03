package com.ist.cipherchat.gui;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ist.cipherchat.R;
import com.ist.cipherchat.networking.ChatActivityRunnable;
import com.ist.cipherchat.networking.ChatOutHandler;
import com.ist.cipherchat.networking.Globals;
import com.ist.cipherchat.networking.OutputSocketHandler;

public class ChatActivity extends Activity implements View.OnClickListener {

	static CyclicBarrier ChatBarrier;

	static ChatActivityRunnable chatActivityRunnable;
	Context androidContext;

	public static Origin origin;
	ChatOutHandler chatOut;

	public String thisUsername;

	/** Handles messages coming from ChatInRunnable thread. Texts come already formatted. */
	public static Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				String text = (String) msg.obj;
				TextView messageReceived = (TextView) findViewById(R.id.receivedMsg);
				text = text + "\n";
				messageReceived.append(text);
			}
		};

		Log.d("register", "Activity has been set!");
		String thisUsername;
		String otherUsername;

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		androidContext = this.getApplicationContext();

		TextView text = (TextView) findViewById(R.id.receivedMsg);
		text.setMovementMethod(new ScrollingMovementMethod());

		Button button = (Button) findViewById(R.id.sendToServerButton);
		button.setOnClickListener(this);

		Intent intent = getIntent();

		thisUsername = intent.getStringExtra(Contacts.EXTRA_THIS_USERNAME);
		otherUsername = intent.getStringExtra(Contacts.EXTRA_OTHER_USERNAME);

		SecretKeySpec sessionKey;
		ObjectOutputStream out;

		if (origin == Origin.CONTACTS) {

			CyclicBarrier barrier = new CyclicBarrier(2);

			chatActivityRunnable = new ChatActivityRunnable(barrier, thisUsername, otherUsername, this);

			Thread t = new Thread(chatActivityRunnable);
			t.start();

			try {
				barrier.await();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (BrokenBarrierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		chatOut = new ChatOutHandler();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {

		// Get message from GUI
		EditText messageEdit = (EditText) findViewById(R.id.message);
		String message = messageEdit.getText().toString();

		if(message.equals("")){
			return;
		}
		chatOut.sendMessage(message);
		
		messageEdit.setText("");

		Date dNow = new Date();
		SimpleDateFormat ft = new SimpleDateFormat("hh:mm");

		// Message is valid: format it to be pretty
		String formatted = String.format("[%s] %s: %s\n", ft.format(dNow), Globals.getThisUsername(), message);

		TextView messageReceived = (TextView) findViewById(R.id.receivedMsg);
		messageReceived.append(formatted);
	}
}