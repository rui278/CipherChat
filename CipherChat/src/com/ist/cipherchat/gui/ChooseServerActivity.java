package com.ist.cipherchat.gui;

import com.ist.cipherchat.R;
import com.ist.cipherchat.networking.Globals;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;


public class ChooseServerActivity extends Activity
{
	 public final static String EXTRA_SERVER_IP = "com.ist.cryptochat.SERVER_IP";
	 public final static String EXTRA_SERVER_PORT = "com.ist.cryptochat.SERVER_PORT";
	 public static final String EXTRA_USERNAME = "com.ist.cryptochat.USERNAME";
	 public static final String EXTRA_PASSWORD = "com.ist.cryptochat.PASSWORD";
	 public static final String EXTRA_OPTION = "com.ist.cryptochat.OPTION"; 
	 
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_server);
				
		EditText serverIPText = (EditText) findViewById(R.id.ServerIpText);
		serverIPText.setText("192.168.186.51");
	    EditText serverPortText = (EditText) findViewById(R.id.serverPortEdit);
	    serverPortText.setText("1337");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.choose_server, menu);
		return true;
	}

	public void connectToServer(View view)
	{
		// Create intent to start the main window
	    Intent intent = new Intent(this, Contacts.class);
	    
	    // Get server IP
	    EditText serverIPText = (EditText) findViewById(R.id.ServerIpText);
	    String serverIP = serverIPText.getText().toString();
	    intent.putExtra(EXTRA_SERVER_IP, serverIP);
	    
	    // Get server port
	    EditText serverPortText = (EditText) findViewById(R.id.serverPortEdit);
	    String serverPort = serverPortText.getText().toString();
	    intent.putExtra(EXTRA_SERVER_PORT, serverPort);
	    
	    //Get user name and password
	    EditText usernameText = (EditText) findViewById(R.id.UsernameTextField);
	    String username = usernameText.getText().toString();
	    intent.putExtra(EXTRA_USERNAME, username);
	    
	    EditText passwordText = (EditText) findViewById(R.id.PasswordTextField);
	    String password = passwordText.getText().toString();
	    intent.putExtra(EXTRA_PASSWORD, password);
	    
	    // get radio buttons
	    RadioGroup radio = (RadioGroup) findViewById(R.id.radioGroup);
	    int radioId = radio.getCheckedRadioButtonId();
	    
	    RadioButton radioButton = (RadioButton) findViewById(radioId);
	    	    
	    intent.putExtra(EXTRA_OPTION, radioButton.getText().toString());
	    
	    // Send IP and port to main window, so it can connect to server
	    startActivityForResult(intent, 666);
	   

	}
}
