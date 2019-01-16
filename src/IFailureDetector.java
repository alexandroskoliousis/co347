/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

interface IFailureDetector {
	
	/* Initiates communication tasks, e.g. sending heartbeats periodically */
	void begin ();
	
	/* Handles in-coming (heartbeat) messages */
	void receive(Message m);
	
	/* Returns true if `process` is suspected */
	boolean isSuspect(Integer process);
	
	/* Notifies a blocking thread that ‘process’ has been suspected. */
	void isSuspected(Integer process);
}
