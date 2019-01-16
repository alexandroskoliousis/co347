/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class Listener extends Thread {
	
	/* The process this listener is belonging */
	private Process process; 

	public Listener (Process process) {
		this.process = process;
	}
	
	public void run() {
		ServerSocket serversocket = null;
		Socket s;

		InputStreamReader input;
		BufferedReader b;
		
		String message = null;
		boolean done = false;

		try {
			serversocket = new ServerSocket(process.getPort(), 1);
			serversocket.setSoTimeout(0);

		} catch (IOException e) {
			String msg =
				String.format("Error: failed to launch Listener at %s.", 
			process.getInfo());
			System.err.println(msg);
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		while (! done) {
			s = null;
			try {
				s = serversocket.accept();
			} catch (IOException e) {
				String msg =
					String.format("Error: failed to accept connection at %s.", 
				process.getInfo());
				System.err.println(msg);
				System.err.println(e.getMessage());
				System.exit(1);
			}
			/* Accepted connection from a MessageHandler. */
			try {
				input = new InputStreamReader(s.getInputStream());
				b = new BufferedReader(input);
				while((message = b.readLine()) != null) {
					Message m = Message.parse(message);
					/* Notify process */
					process.receive(m);
				}
				/* Close connection */
				b.close();
				s.close();
			} catch (IOException e) {
				String msg =
					String.format("Error: failed to read() at %s.", 
				process.getInfo());
				System.err.println(msg);
				System.err.println(e.getMessage());
				System.exit(1);
			}
		}
	}
}
