/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class FaultManager extends Thread {
	
	private Registrar r;
	
	private String key;
	private boolean value;
	private String ticket;
	
	public FaultManager (Registrar r) {
		this.r = r;
		
		/* See this.parse(msg) */
		key = null;
		value = false;
	}
	
	/* Parses `command` from a FaultInjector instance */
	private boolean parse(String command) {
		StringTokenizer tokens = new StringTokenizer(command, Utils.SEPARATOR);
		if (tokens.countTokens() != 2)
			return false;
		String process = tokens.nextToken();
		if (! r.contains(process))
			return false;
		String action = tokens.nextToken();
		if (! (action.equals("ON") || action.equals("OFF")))
			return false;
		key = process;
		value = ((action.equals("ON")) ? false : true);
		return true;
	}
	
	public void run() {
		
		ServerSocket serversocket = null;
		Socket s;

		InputStreamReader input;
		BufferedReader b;
		PrintWriter p;
		
		String message = null;
		boolean done = false;

		try {
			serversocket = new ServerSocket(Utils.FAULTMNGR_PORT, 1); /* backlog is 1 */
			serversocket.setSoTimeout(0);
		} catch (IOException e) {
			System.err.println("Error: failed to launch FaultManager.");
			System.err.println(e.getMessage());
			System.exit(1);
		}
		while (! done) {
			s = null;
			try {
				s = serversocket.accept();
			
			} catch (IOException e) {
				System.err.println("Error: failed to accept connection from FaultInjector.");
				System.err.println(e.getMessage());
				System.exit(1);
			}
						
			try {
				input = new InputStreamReader(s.getInputStream());
				b = new BufferedReader(input);
				p = new PrintWriter(s.getOutputStream());
				while ((message = b.readLine()) != null) {
					/* Handle FaultInjector's command */
					if (! parse(message)) {
						p.println("E.g. usage: P1<|>ON; P2<|>OFF; etc.");
						p.flush();
						continue;			
					}
					/* Search registry */
					Record record = r.find(key);
					/* Update process status */
					record.beFaulty(value);
					r.update(record);
					p.println("OK");
					p.flush();
				}
				/* Null message. */
				p.close();
				b.close();
				s.close();
			} catch (IOException e) {
				System.err.println("Error: failed to read/write at FaultManager.");
				System.err.println(e.getMessage());
				System.exit(1);
			}
		}
	}
}

