package com.desperate;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * A thread-safe database that saves information of registered users.
 * <p>
 * The users are accessed through their user names, which are unique. It's possible to get a list of all the users that are online at the moment. The
 * whole database can be serialized to a file using the method {@link #save()}.
 * 
 * @author SIRS group 10 - First Semester 2013
 * 
 */
public class UserDatabase implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String databaseFileName = "users.udb";

	/**
	 * Stores user data. Key is the user's username.
	 */
	private HashMap<String, User> users;

	public UserDatabase() {

		this.users = new HashMap<String, User>();
	}

	/**
	 * Adds a new user to the database. If a user with the same name already exists, this function fails and returns false.
	 * 
	 * @param user
	 *            user to be added
	 * @return true if the user was added. false otherwise
	 */
	public synchronized boolean addUser(User user) {

		if (users.containsKey(user.userName)) {
			return false;
		} else {
			users.put(user.userName, user);
			return true;
		}
	}

	/**
	 * Gets the username data, given a username.
	 * 
	 * @param userName
	 *            Name the user used to register
	 * @return data of the user
	 * @throws Exception
	 */
	public synchronized User getUser(String userName) throws Exception {

		User user = users.get(userName);

		if (user == null) {
			throw new Exception("User does not exists.");
		}

		return user;

	}

	public static synchronized void save(UserDatabase database) {

		try {
			FileOutputStream fileOut = new FileOutputStream(databaseFileName);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(database);
			out.close();
			fileOut.close();
			System.out.println("Serialized data is saved in " + databaseFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads a database from disk. File name is {@value #databaseFileName}.
	 * 
	 * @return database loaded from file
	 */
	public static UserDatabase load() {

		UserDatabase database = new UserDatabase();

		try {
			FileInputStream fileIn = new FileInputStream(databaseFileName);
			ObjectInputStream in = new ObjectInputStream(fileIn);

			database = (UserDatabase) in.readObject();

			in.close();
			fileIn.close();
			System.out.println("Serialized data is loaded from " + databaseFileName);

		} catch (IOException e) {
			System.err.println("No database found. New database created");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.err.println("Database was created with an older version of server. New database created.");
			e.printStackTrace();
		}

		return database;
	}

	/**
	 * Gets the IP address of a user.
	 * 
	 * @param userName
	 * @return
	 */
	public synchronized InetAddress getIP(String userName) {

		User user = users.get(userName);

		if (user != null)
			return users.get(userName).ip;
		else
			return null;
	}

	/**
	 * Prints info of all the users to standard output.
	 */
	public synchronized void printUserList() {

		System.out.printf("%-10s\t%s\n", "      Name", "    Address", " State");

		for (User u : users.values()) {
			System.out.printf("%-16s    %9s    %18s\n", u.userName, u.ip, u.getOnlineStatus() ? "online" : "offline");
		}
	}

	public synchronized void save() {

		UserDatabase.save(this);
	}

	/**
	 * Gets an array of Strings containing the names of all online users, except the requesting client itself.
	 * 
	 * @param requester
	 *            User that issued the list request.
	 * @return usernames of all currenly online users, except the <b>requester</b>
	 */
	public synchronized String[] getOnlineUserList(User requester) {

		LinkedList<String> onlineUsers = new LinkedList<String>();

		for (User u : users.values()) {
			if (u.getOnlineStatus() == true && !u.equals(requester)) {
				onlineUsers.add(u.userName);
			}
		}

		String[] onlineUserNames = new String[onlineUsers.size()];

		for (int i = 0; i < onlineUserNames.length; i++) {
			onlineUserNames[i] = onlineUsers.get(i);
			System.out.println("Online user: " + onlineUserNames[i]);
		}

		return onlineUserNames;
	}
}
