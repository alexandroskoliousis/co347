/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

import java.io.*;
import java.net.*;
import java.util.*;

class FaultInjector {
	
	public static void main (String [] args) {
		
		Socket s = null;
		
		InputStreamReader input;
		BufferedReader b;
		PrintWriter p;
		
		String result = null;

		InputStreamReader stdin;
		BufferedReader  console;
		
		String   command = null;
		boolean interact = true;
		
		/* Parse command line arguments */
		int i, j;
		for (i = 0; i < args.length; ) {
			if ((j = i + 1) == args.length) {
				System.err.println("usage: java FaultInjector [-m message]");
				System.exit(1);
			}
			if (args[i].equals("-m")) {
				command = args[j];
				interact = false;
			} else {
				System.err.println(
				String.format("Error: unknown flag %s %s.", args[i], args[j])
				);
			}
			i = j + 1;
		}
		
		String line = null;
		try {
			s = new Socket(Utils.REGISTRAR_ADDR, Utils.FAULTMNGR_PORT);
			s.setKeepAlive(true);
			s.setSoTimeout(0);
		} catch (Exception e) {
			System.err.println("Error: connection to FaultManager failed.");
			System.err.println(e.getMessage());
			System.exit(1);
		}
		try {
			input = new InputStreamReader(s.getInputStream());
			b = new BufferedReader(input);	
			p = new PrintWriter(s.getOutputStream());
			if (interact) {
				/* This is a user-interactive program. */
				stdin = new InputStreamReader(System.in);
				console = new BufferedReader(stdin);
				System.out.print("> ");
				while ((line = console.readLine()) != null) {
					/* Let the server-side parse for errors. */
					p.println(line);
					p.flush();
					result = b.readLine();
					System.out.println(String.format("< %s", result));
					System.out.print("> ");
				}
			} else {
				/* Send command directly. */
				System.out.println(String.format("> %s", command));
				p.println(command);
				p.flush();
				result = b.readLine();
				System.out.println(String.format("< %s", result));
			}
		} catch (IOException e) {
			System.err.println("Error: FaultInjector failed to read/write.");
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
}

