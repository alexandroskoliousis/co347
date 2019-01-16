/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Worker extends Thread {

	private Socket s;
	private Registrar r;
	private int myprocess;
	
	/* Random delay generator */
	Random random;
	private static final double mean = (double) Utils.DELAY;
	private static final double stdv = Utils.STDEV;
	
	/* Throughput measurements */
	private long count;
	private long _t_recv;
	
	public Worker(Socket s, Registrar r) {
		this.s = s;
		this.r = r;

		myprocess = Utils.INFINITY;
		
		random = new Random();
		
		count = 0;
	}
	
	private void unicast (Message m, int delay) {
		
		int src = m.getSource();
		int dst = m.getDestination();
		
		Record source = r.find (src);
		Record destination = r.find (dst);
		
		if ((source == null) || (destination == null)) {
			/* In this unlikely event. */
			String msg =
				String.format("Error: link <P%d, P%d> does not exist.", 
			src, dst);
			System.err.println(msg);
			System.exit(1);
		}
		
		/* Utils.out(r.pid, String.format("%s > %s", source, destination)); */
		if (! source.isFaulty() && ! destination.isFaulty()) {
			if (delay >= 0) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException ignored) {}
			}
			if (Utils.accuracy == Utils.Accuracy.WEAK) {
				if (src != r.getCoordinator())
					return ;
			} else
			if (Utils.accuracy == Utils.Accuracy.EVENTUALLY_WEAK) {
				if (! r.isResilient(src))
					return ; 
			}
			/* Message statistics */
			r.incStats(m.getType());
			MessageEntry entry = 
				new MessageEntry(m.pack(), Utils.getPriority(m.getType()), System.currentTimeMillis());
			destination.getQueue().put(entry);
		}
		return ;
	}
	
	/*
	 * A process p can ask the oracle whether its computed
	 * routing distances are optimal or not.
	 */
	private boolean areShortestPaths (String s) {
		boolean result = true;
		int U = r.n + 1; /* Upper and lower bounds */
		int L = 0;
		int answer;
		String [] t;
		int v, d;
		
		/* Parse message payload */
		String [] token = s.split(";");
		
		for (int i = 0; i < token.length; i++) {
			/* Routing table entry */
			t = token[i].split(":");

			v = Integer.parseInt(t[0]); /* destination */
			d = Integer.parseInt(t[1]); /* cost */
			
			if (v < (L + 1) || v > (U - 1)) {
				Utils.out(String.format("Error: c(%2d,%2d) is out of scope", myprocess, v));
				result = false;
				break;
			} else {
				/* Query oracle */
				answer = r.oracle.getPathLength(myprocess -1, v -1);
				if (answer != d) {
					/* What if INF values do not agree? */
					if (answer >= r.n && d >= r.n)
						continue;
					result = false;
					Utils.out(String.format("Error: c(%2d,%2d) != %d (oracle says %d)", myprocess, v, d, answer));
					break;
				}
			}
		}
		/* if (result) Utils.out("OK"); */
		return result;
	}
	
	/*
	 * A process p can ask the oracle whether its computed
	 * next hops are optimal or not.
	 */
	private boolean areBestNextHops (String s) {
		boolean result = true;
		int U = r.n + 1; /* Upper and lower bounds */
		int L = 0;
		boolean answer;
		String [] t;
		int v, w;
		/* Parse message payload */

		String [] token = s.split(";");
		
		for (int i = 0; i < token.length; i++) {
			/* Routing table entry */
			t = token[i].split(":");

			v = Integer.parseInt(t[0]); /* destination */
			w = Integer.parseInt(t[1]); /* next */
			if (
				(v < (L + 1) || v > (U - 1)) ||
				(w < (L + 1) || w > (U - 1))
			) {
				result = false;
				break;
			} else {
				/* Query oracle */
				answer = r.oracle.isBestNextHop(myprocess -1, v -1, w -1);
				if (! answer) {
					result = answer;
					break;
				}
			}
		}
		return result;
	}
	
	private int getDelay () {
		int d;
		double x;
		int y;
		if (Utils.GAUSSIAN) {
			x = (random.nextGaussian() * stdv) + mean;
			y = (int) Math.round(x);
			if (y < 0)
				d = 0;
			else
				d = y;
		} else
			d = Utils.DELAY;
		return d;
	}
	
	private void deliver (Message m) {
		int source = m.getSource();
		int destination = m.getDestination();
		if (destination != -1) {
			/* Check if source and destination are neighbours. */
			if (r.oracle.areNeighbours(source -1, destination -1))
				unicast(m, getDelay());
		} else {
			int delay;
			boolean drop = false;
			boolean first = true;
			/* Broadcast. */
			for (int i = 1; i <= r.n; i++) {
				drop = (source == i && Utils.SELFMSGENABLED == false);
				if (drop)
					continue;
				
				/* This is no longer a full-connected network. */
				if (! r.oracle.areNeighbours(source -1, i -1))
					continue;
				
				if (first) {
					delay = getDelay();
					first = false;
				} else delay = -1;
				
				m.setDestination(i);
				unicast(m, delay);
			}
		}
	}
	
	public void run() {
		
		InputStreamReader input;
		BufferedReader b;
		PrintWriter p;
		
		String message, reply;
		boolean result;
		
		long t__recv, dt;
		double rate;

		try {
			input = new InputStreamReader(s.getInputStream());
			b = new BufferedReader(input);
			p = new PrintWriter(s.getOutputStream());
			
			while ((message = b.readLine()) != null) {
				
				Message m = Message.parse(message);
				
				if (m.getDestination() == r.pid && m.getType().equals("NULL")) {
					
					myprocess = m.getSource();
					/* 
					 * Note that a process that registers blocks 
					 * until all other processes have registered.
					 */
					String payload = m.getPayload();
					/* Payload is of the form `name:host:port` */
					StringTokenizer tokens = new StringTokenizer(payload, ":");
					String name = tokens.nextToken();
					String host = tokens.nextToken();
					int port = Integer.parseInt(tokens.nextToken());
					Record record = new Record(name, host, port);
					new Thread(new MessageHandler(record)).start();
					result = r.registeR (record);
					/* Synchronise P(i), for all i. */
					synchronized (r.getLock()) {
						while (! r.areRegistered()) {
							try {
								r.getLock().wait();
							} catch (InterruptedException ignored) {}
						}
					}
					reply = (result ? "OK" : "ERR");
					p.println(reply);
					p.flush();
					
					/* Yield the processor; allow other threads to be notified. */
					Thread.yield();
				
				} else
				/* Added support for querying the oracle.
				 * 
				 * Currently, we are checking for the correctness
				 * of routing tables and shortest paths.
				 */
				if (m.getDestination() == r.pid && m.getType().equals(Utils.CHECK_COST)) {
					
					result = areShortestPaths(m.getPayload());
					reply = (result ? "OK" : "ERR");
					p.println(reply);
					p.flush();
				
				} else
				if (m.getDestination() == r.pid && m.getType().equals(Utils.CHECK_NEXT)) {
					
					result = areBestNextHops(m.getPayload());
					reply = (result ? "OK" : "ERR");
					p.println(reply);
					p.flush();
				
				} else { /* Relay message. */
					
					count += 1;
					if (count == 1)
						_t_recv = System.currentTimeMillis();
					if (count % Utils.STEP == 0) {
						t__recv = System.currentTimeMillis();
						dt = t__recv - _t_recv;
						rate = (double) (Utils.STEP * 1000) / (double) dt;
						Utils.out(r.pid, String.format("[W %03d][RECV] %06d\t%10.1f", 
							myprocess, count, rate));
						_t_recv = t__recv;
					}
					deliver (m);
					/* Assumes always correct. */
					p.println("OK");
					p.flush();
				}
			}
			/* Null message. */
			p.close();
			b.close();
			s.close();
		} catch (IOException e) {
			System.err.println(String.format("Error: P%d's worker has failed.", myprocess));
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
	
	class MessageHandler extends Thread { /* Again, once per process... */
		
		private Socket socket;
		
		private InputStreamReader input;
		private BufferedReader b;
		private PrintWriter p;
		
		private String name;
		private String host;
		private int    port;

		private PriorityBlockingQueue<MessageEntry> queue;
		
		public MessageHandler(Record record) {
			name = record.getName();
			host = record.getHost();
			port = record.getPort();
			queue = record.getQueue();
		}
		
		private String getInfo () {
			String s = null;
			s = String.format("[%s at %s:%d]", name, host, port);
			return s;	
		}
		
		private Socket connect () {
			Socket s = null;
			int attempts = 0;
			do {
				attempts ++;
				try {
					s = new Socket(host, port);
					s.setKeepAlive(true);
					s.setSoTimeout(0);

				} catch (Exception e) {
					String msg =
						String.format("Warning: connection attempt %d to %s failed.",
					attempts, getInfo());
					System.err.println(msg);
					System.err.println(e.getMessage());
					try {
						Thread.sleep(random.nextInt(100) + 1); /* [0..100] + 1 > 0.*/
					} catch (InterruptedException ignored) {}
				}
			} while (s == null);
			return s;
		}
		
		private void init () { /* Configure I/O */
			try {
				input = new InputStreamReader(socket.getInputStream());
				b = new BufferedReader(input);
				p = new PrintWriter(socket.getOutputStream());

			} catch (IOException e) { 
				/* Ignore for now. */
			}
			return ;
		}
		
		public void run () {
			socket = connect();
			init ();
			for (;;) {
				MessageEntry entry = null;
				try {
					entry = queue.take();
				} catch (InterruptedException ignored) {}
				write(entry.getMessage());
			}
		}
		
		private boolean write (String message) {
			p.println(message);
			p.flush();
			return true;
		}
	}
}
