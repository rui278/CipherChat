package com.desperate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AdminConsole implements Runnable {

	@Override
	public void run() {

		BufferedReader commandLine = new BufferedReader(new InputStreamReader(System.in));
		String command;

		boolean running = true;

		while (running) {
			try {
				command = commandLine.readLine();

				if ("users".equals(command)) {
					CryptoServer.db.printUserList();
				} else if ("save".equals(command)) {
					CryptoServer.db.save();
				} else if ("help".equals(command)) {
					printHelp();
				} else if ("stop".equals(command)) {
					CryptoServer.stop();
					running = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Admin console closed.");
	}

	public void printHelp() {
		System.out.println("CryptoServer - server for CryptoChat app");

		System.out.printf("%-8s\t%s\n", "users", "Prints information of all the registered users.");
		System.out.printf("%-8s\t%s\n", "save", "Saves the current state of the registered user database to disk.");
	}
}
