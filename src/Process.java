/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

import java.io.*;
import java.net.*;
import java.util.*;

class Process {
	
	private String name; /* A triptych that identifies a process p */
	private String host;
	private int    port;
	
	/* A unique identifier; and the total number of process */
	public int pid, n;
	
	/* Socket `socket` connects to the Registrar */
	private Socket socket = null;
	private InputStreamReader input;
	private BufferedReader b;
	private PrintWriter p;	
	
	/* A random number generator */
	Random random;
	
	public Process(String name, int pid, int n) {
		
		this.name = name;
		this.port = Utils.REGISTRAR_PORT + pid;
		
		this.host = "UNKNOWN";
		try {
			this.host = (InetAddress.getLocalHost()).getHostName();
		
		} catch (UnknownHostException e) {
			String msg =
				String.format("Error: getHostName() failed at %s.",
			this.getInfo());
			System.err.println(msg);
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		this.pid = pid;
		this.n = n;
		
		random = new Random();
		
		/* Accepts connections from one of Registrar's worker threads */
		new Thread (new Listener(this)).start();
		/* Connect to registrar */
		socket = connect ();
		init ();
		Utils.out(pid, "Connected.");
	}
	
	private Socket connect () {
		Socket socket = null;
		int attempts = 0;
		do {
			attempts ++;
			try {
				socket = new Socket(Utils.REGISTRAR_ADDR, Utils.REGISTRAR_PORT);
				socket.setKeepAlive(true);
				socket.setSoTimeout(0);
			} catch (Exception e) {
				String msg =
					String.format("Warning: connection attempt %d failed at %s.",
				attempts, this.getInfo());
				System.err.println(msg);
				System.err.println(e.getMessage());
				try {
					Thread.sleep(random.nextInt(100) + 1); /* [0..100] + 1 > 0. */
				} catch (InterruptedException ignored) {}
			}
		} while (socket == null);
		return socket;
	}
	
	private void init () { /* Configure I/O */
		
		if (socket != null) {
			try {
				input = new InputStreamReader(socket.getInputStream());
				b = new BufferedReader(input);
				p = new PrintWriter(socket.getOutputStream());

			} catch (IOException e) { 
				/* Ignore for now. */
			}
		}
		return ;
	}
	
	public boolean registeR () {
		String payload;
		Message m;
		boolean result;
		payload = String.format("%s:%s:%d", name, host, port);
		m = new Message(pid, 0, "NULL", payload);
		result = await(m.pack());
		if (result)
			Utils.out(pid, "Registered.");
		return result;
	}
	
	private boolean await (String message) {
		write(message);
		return
			read();
	}
	
	private boolean read () {
		String reply = null;
		if (socket != null) {
			try {
				/* Await reply */
				reply = b.readLine();
			} catch (IOException ignored) {}
		}
		if (reply == null)
			return false;
		return ((reply.equals("OK")) ? true : false);
	}
	
	private boolean write (String message) {
		if (socket != null) {
			p.println(message);
			p.flush();
		}
		return true;
	}
	
	public int    getPort () { return port; }
	public String getName () { return name; }
	public String getHost () { return host; }
	
	public int getPid () { return pid; }
	public int getNo  () { return   n; }
	
	public String getInfo() {
		String s = null;
		s = String.format("%s at %s:%d", name, host, port);
		return s;
	}
	
	public synchronized void receive (Message m) {

		Utils.out(pid, m.toString()); /* The default action. */
	}
	
	public boolean unicast (Message m) {
		boolean drop = false;
		drop = (m.getDestination() == pid && ! Utils.SELFMSGENABLED);
		if (! drop)
			return await(m.pack()); /* write(m.pack()); */
		return false;
	}
	
	public void broadcast (String type, String payload) {
		Message m = new Message();
		m.setSource(pid);
		m.setDestination(-1); /* See Worker.deliver() */
		m.setType(type);
		m.setPayload(payload);
		/* for (int i = 1; i <= n; i++) {
			m.setDestination(i);
			unicast (m);
		} */
		unicast(m);
	}
}
