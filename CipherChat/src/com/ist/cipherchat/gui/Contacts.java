package com.ist.cipherchat.gui;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.ist.cipherchat.R;
import com.ist.cipherchat.networking.Core;
import com.ist.cipherchat.networking.Globals;
import com.ist.cipherchat.networking.PhoneServerSocketHandler;
import com.ist.cipherchat.networking.ThreadComm;

public class Contacts extends Activity implements OnItemClickListener {

	Context androidContext;
	public static String[] users = new String[0];

	String otherusername;
	String thisUsername;

	public final static String EXTRA_ENUM = "com.ist.cryptochat.ENUM";
	public final static String EXTRA_THIS_USERNAME = "com.ist.cryptochat.EXTRA_THIS_USERNAME";
	public final static String EXTRA_OTHER_USERNAME = "com.ist.cryptochat.EXTRA_OTHER_USERNAME";

	public static ThreadComm threadComm = new ThreadComm();

	public static Core sslSocketHandler;
	public static PhoneServerSocketHandler serverSocket;

	public static CyclicBarrier barrier = new CyclicBarrier(2);

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contacts);

		androidContext = this.getApplicationContext();

		Intent intent = getIntent();

		thisUsername = intent.getStringExtra(ChooseServerActivity.EXTRA_USERNAME);
		String password = intent.getStringExtra(ChooseServerActivity.EXTRA_PASSWORD);
		String option = intent.getStringExtra(ChooseServerActivity.EXTRA_OPTION);
		String serverIP = intent.getStringExtra(ChooseServerActivity.EXTRA_SERVER_IP);
		
		Globals.setThisUsername(thisUsername);

		int serverPort = Integer.parseInt(intent.getStringExtra(ChooseServerActivity.EXTRA_SERVER_PORT));

		if (serverPort < 0 || serverPort > 65535)
			System.exit(-1);

		// instantiates the ssl socket handler class (static) that connects to the server.
		Contacts.sslSocketHandler = new Core(serverPort, serverIP, thisUsername, password, option, androidContext);

		Thread t = new Thread(Contacts.sslSocketHandler);
		t.start();

		try {
			try {
				barrier.await();
			} catch (BrokenBarrierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final ListView listview = (ListView) findViewById(R.id.listView1);

		String[] usersLocal;

		if (threadComm.error) {

			String toastMessage = resolveString();

			Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();

			usersLocal = new String[0];

			if (threadComm.contactsFail || threadComm.registerFail) {
				threadComm.clean();
				finish();
				return;
			}

			threadComm.clean();
		} else {
			usersLocal = Contacts.users;
			threadComm.clean();
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, usersLocal);

		listview.setAdapter(adapter);

		listview.setOnItemClickListener(this);

		Contacts.serverSocket = new PhoneServerSocketHandler(androidContext, this);

		t = new Thread(Contacts.serverSocket);
		t.start();

		Log.d("register", "ServerSock open on:" + PhoneServerSocketHandler.serverSocket.getInetAddress().toString());
	}

	private String resolveString() {

		if (threadComm.loginFail)
			return "Failed to Login. Please try again.";
		if (threadComm.registerFail)
			return "Failed to register. Please try again.";
		if (threadComm.contactsFail)
			return "Failed to retrieve contacts. Please try again.";
		if (threadComm.noUsers)
			return "Sorry, there are no online contacts";

		return "Alguma coisa esta mal";
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		String name = (String) parent.getItemAtPosition(position);
		Toast.makeText(getApplicationContext(), name, Toast.LENGTH_LONG).show();

		Intent forwardIntent = new Intent(this, ChatActivity.class);

		Origin origin = Origin.CONTACTS;

		forwardIntent.putExtra(EXTRA_ENUM, origin.toString());

		forwardIntent.putExtra(EXTRA_THIS_USERNAME, thisUsername);
		forwardIntent.putExtra(EXTRA_OTHER_USERNAME, name);

		ChatActivity.origin = Origin.CONTACTS;
		startActivityForResult(forwardIntent, 777);
	}
}
