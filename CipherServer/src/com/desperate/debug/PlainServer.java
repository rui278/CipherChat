package com.desperate.debug;

import java.net.*;
import java.io.*;

public class PlainServer
{
	public static void main(String[] args) throws IOException
	{
		int portNumber = 1337;

		try
		{
			ServerSocket serverSocket = new ServerSocket(portNumber);
			
			System.out.println("Server listening on port " + portNumber);
			
			Socket clientSocket = serverSocket.accept();
			
			PrintWriter out = new PrintWriter(
					clientSocket.getOutputStream(), true);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			
			System.out.println("Accepted " + clientSocket.getRemoteSocketAddress().toString());
			
			out.println("Server says: coisas");

			String inputLine, outputLine;

			while ((inputLine = in.readLine()) != null)
			{
				System.out.println(inputLine);
				outputLine = "gotten";
				out.println(outputLine);
				
				if ("end".equals(inputLine))
					break;
			}
			
			clientSocket.close();
			serverSocket.close();
			
			System.out.println("Server closed. Bye!");
		}
		catch (IOException e)
		{
			System.out
					.println("Exception caught when trying to listen on port "
							+ portNumber + " or listening for a connection");
			System.out.println(e.getMessage());
		}
	}
}