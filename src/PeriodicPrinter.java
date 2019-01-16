/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

import java.io.*;
import java.util.*;

public class PeriodicPrinter extends Thread {
	
	private Registrar  r;

	private boolean poll;
	private long   count;

	private LinkedList<String> messages;
	private PrintWriter writer;
	
	private long period;
	
	public PeriodicPrinter (Registrar r) {
		this.r = r;
		
		poll  = true;
		count = 0;
		writer = null;
		messages = new LinkedList<String>();
		period = 100; /* Poll every 100 ms */
		if (Utils.P.isEmpty()) /* Nothing to poll. */
			poll = false;
		else {
			try { writer = new PrintWriter("measurements.log");
			} catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	public void run  () {
		long answer = 0;
		String line = null;
		while (poll) {
			/* Poll values periodically. */
			try { Thread.sleep(period);
			} catch (Exception e) { e.printStackTrace(); }
			count += 1;
			line = String.format("%4d\t", count);
			for (String key: Utils.P.keySet()) {
				answer = r.getStats(key);
				line += String.format("%10s\t%6d\t", key, answer);
			}
			writer.println(line);
			writer.flush();
		}
		return ;
	}
}


