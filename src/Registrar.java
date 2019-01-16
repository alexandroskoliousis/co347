/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import java.util.*;
import java.net.*;
import java.io.*;

public class Registrar {
	
	public static final int pid = 0; /* P0 is reserved for this server */
	
	/* The size of the system */
	public int n;
	
	/* The "switch" table */
	private ConcurrentHashMap<String, Record> registry;
	
	/* The one process that is never suspected */
	private int coordinator;
	/* List of failed processes */
	List<Integer> F;
	Random random;
	
	/* A lock as a barrier to synchronise all clients */
	private Object lock;
	
	public RoutingOracle oracle;
	
	/* Message statistics */
	private final ConcurrentMap<String,AtomicLong> stats =
		new ConcurrentHashMap<String,AtomicLong>();
	
	public Registrar(int n, String filename) {
		
		this.n = n;
		registry = new ConcurrentHashMap<String, Record>(n, 0.9f, n);
		
		/* Synchronize worker threads and, consequently, Process.registeR() */
		lock = new Object();
		
		/* Fault management */
		(new Thread(new FaultManager(this))).start();
		
		/* Periodic measurements */
		if (Utils.COLLECTSTATS)
			(new Thread(new PeriodicPrinter(this))).start();
		
		/* Consensus with strong failure detectors */
		coordinator = -1;
		F = new ArrayList<Integer>();
		random = new Random();

		if (Utils.accuracy == Utils.Accuracy.WEAK) {
			coordinator = random.nextInt(n) + 1; /* nextInt belongs in [0,n) */
			Utils.out(pid, String.format("The rotating coordinator is %d.", coordinator));
		} else
		if (Utils.accuracy == Utils.Accuracy.EVENTUALLY_WEAK) {
			int max = (int) Math.floor((double) n / 3.);
			if (max > 1) {
			do {
				int p = random.nextInt(n) + 1;
				if (! F.contains(p))
					F.add(p);
			} while (F.size() < max);
			Utils.out(pid, String.format("Set F contains: %s.", F.toString()));
			} else {
				System.out.println("Warning: cannot simulate eventually strong failure detectors.");
			}
		}
		
		/* Initialise oracle */
		oracle = new RoutingOracle(n, filename);
		if (! oracle.reset()) 
			System.out.println("Warning: approximate oracle.");
		Utils.out(pid, String.format("|V| = %d, |E| = %d", oracle.getV(), oracle.getE()));
	}
	
	public int getCoordinator() { return coordinator; }
	public boolean isResilient(int id) { return (! F.contains(id)); }
	
	public void incStats (String key) { /* Increments message `key` count */
		AtomicLong value;
		AtomicLong count = stats.get(key);
		if (count == null) {
			count = new AtomicLong(0);
			value = stats.putIfAbsent(key, count);
			if (value != null)
				count = value;
		}
		count.incrementAndGet();
		return ;
	}
	
	public long getStats (String key) { /* Returns message `key` count */
		AtomicLong count;
		count = stats.get(key);
		return (count == null) ? 0 : count.get();
	}
	
	public Object getLock () { return lock; }
	
	private void tryNotify () {
		synchronized (lock) {
			if (areRegistered()) /* If all processes have registered, notify them */
				lock.notifyAll();
		}
	}
	
	public boolean areRegistered () { /* The condition for synchronisation */
		return (registry.size() == n);
	}
	
	/* Process registration */
	public boolean registeR (Record record) {
		String key = record.getName();
		Record result = registry.put(key, record);
		/* Synchronize */
		tryNotify ();
		return (result == null) ? true : false; 
	}
	
	public void update (Record record) {
		String key = record.getName();
		
		String P = key.substring(1); /* Skip `P` */
		int p = Integer.parseInt(P);
		
		registry.put(key, record);
		
		/* Notify the oracle. */
		if (record.isFaulty()) {
			oracle.remove(p -1);
		} else {
			oracle.repair(p -1);
		}
	}
	
	public Record find (int pid) {
		String key;
		key = String.format("P%d", pid);
		return find(key);
	}

	public Record find (String key) {
		Record record;
		record = registry.get(key);
		return record;
	}
	
	public boolean contains(String key) {
		return registry.containsKey(key);
	}
	
	private void handle (Socket client) {
		new Thread(new Worker(client, this)).start();
	}
	
	public static void main(String[] args) {
		
		if (args.length != 2) {
			System.err.println("usage: java Registrar [integer] [filename]");
			System.exit(1);
		}
		
		int n = Integer.parseInt(args[0]);
		String filename = args[1];
		
		if (! (new File(filename).isFile())) {
			System.err.println(String.format("Error: %s does not exist.", 
				filename));
			System.exit(1);
		}
		
		Registrar server = new Registrar(n, filename);
		Utils.out(server.pid, String.format("Registrar started; n = %d.",n));
		
		ServerSocket serversocket = null;
		boolean done = false;
		try {
			serversocket = new ServerSocket(Utils.REGISTRAR_PORT, n);
		
		} catch (IOException e) {
			System.err.println("Error: failure to launch registrar.");
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		while (! done) {	
			Socket clientsocket = null;
			try {
				clientsocket = serversocket.accept();
			
			} catch (IOException e) {
				System.err.println("Error: failure to accept connection at Registrar.");
				System.err.println(e.getMessage());
				System.exit(1);
			}
			/* And so it begins... */
			server.handle(clientsocket);
		}
	}
}
