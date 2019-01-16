/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

import java.io.*;
import java.util.*;

public class Utils {

	public static final int INFINITY = -1;
	
	public static final String REGISTRAR_ADDR = "localhost";
	public static final int    REGISTRAR_PORT = 6667;
	public static final int    FAULTMNGR_PORT = 6665;

	public static final String SEPARATOR = "<|>";
	
	public static final boolean DEBUG = true;
	
	public static final boolean SELFMSGENABLED = false;
	
	public static final int MSG_QUEUE_SIZE = 100;
	
	public static enum Accuracy {
		DEFAULT, /* STRONG or EVENTUALLY_STRONG; difference determined by GAUSSIAN */
		WEAK,
		EVENTUALLY_WEAK
	};
	
	/* `open` and `closed` events for testing routing algorithms 
	 * without failure detectors */
	public static final String OPENED =   "open";
	public static final String CLOSED = "closed";
	
	/* Interacting with the routing oracle */
	public static final String CHECK_COST = "CSP"; /* Check shortest paths */
	public static final String CHECK_NEXT = "CRT"; /* Check next best hops */
	
	/* Configuration parameters */
	public static final Accuracy accuracy = Accuracy.DEFAULT;
	public static final boolean GAUSSIAN = false;
	/* Link delay */
	public static final int DELAY = 10; /* msec; 1sec = 1000msec */
	public static final double STDEV = ((double) DELAY) /2.0;
	
	/* Periodicity of heartbeat messages */
	public static final int Delta = 1000; /* msec; 1sec = 1000msec */
	
	/* For internal measurement purposes */
	public static final long STEP =  100;
	public static final long MAX  = 2000;
	
	/* For external measurement purposes
	 *
	 * If true, message counters (as defined in hash-map `P` below), will
	 * be polled every 100ms and written into file "./measurements.log".
	 */
	public static final boolean COLLECTSTATS = true;
	
	/* Message priorities
	 * 
	 * Periodic `heartbeat` messages should have higher priority
	 * Otherwise, the system may cause route oscillations.
	 */
	public static final HashMap<String,Integer> P = new HashMap<String,Integer>();
	static {
		P.put("heartbeat" ,1);
		P.put("mydist"    ,2);
		P.put(OPENED      ,3);
		P.put(CLOSED      ,4);
    }
	
	public static int getPriority(String type) {
		Integer priority = null;
		priority = P.get(type);
		if (priority == null) return 0xff; /* Maximum # message types is 255 */
		return priority;
	}
	
	public static synchronized void out (String s) {
		
		if (DEBUG) {
			String x = String.format("[DBG] %s%n", s);
			System.out.print(x);
			System.out.flush();
		}
	}

	public static synchronized void out (int id, String s) {
		
		/* Printf assumes there are no more than 999 processes */
		if (DEBUG) {
			String x = String.format("[%03d] %s%n", id, s);
			System.out.print(x);
			System.out.flush();
		}
	}
	
	/* For measurement purposes */
	public static String getPayload (int size) {
		char [] payload = new char[size];
		for (int i = 0; i < size; i++)
			payload[i] = 'x';
		return new String(payload);
	}
	
	public static void main (String [] args) {
		Utils.out(1, "test");
		Utils.out("test");
	}
}
