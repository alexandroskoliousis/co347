/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

import java.util.*;

public class Message {
	
	private int source;
	private int destination;
	private String type; /* Message type */
    private String payload;
	
	public Message (int source, int destination, String type, String payload) {
		this.source = source;
		this.destination = destination;
		this.type = type;
		this.payload = payload;
	}
	
	public Message () {
		source = Utils.INFINITY;
		destination = Utils.INFINITY;
		type = null;
		payload = null;
	}
	
	public Message (Message m) {
		this(m.getSource(), m.getDestination(), m.getType(), m.getPayload());
	}
	
	public int getSource () {
		return source;
	}
	public int getDestination () { 
		return destination;
	}

	public String getType () { 
		return type;
	}

	public String getPayload () {
		return payload;
	}
	
	public void setSource (int source) {
		this.source = source;
	}
 
	public void setDestination (int destination) {
		this.destination = destination;
	}

	public void setType (String type) {
		this.type = type;
	}

	public void setPayload (String payload) {
		this.payload = payload;
	}
	
	public static Message parse (String msg) {
		StringTokenizer tokens = new StringTokenizer(msg, Utils.SEPARATOR);
		int s = Integer.parseInt(tokens.nextToken());
		int d = Integer.parseInt(tokens.nextToken());
		String t = tokens.nextToken();
		String p = tokens.nextToken();
		return new Message(s, d, t, p);
    }

	public String pack () {
		String s = 
			String.format("%d",      source) + Utils.SEPARATOR + 
			String.format("%d", destination) + Utils.SEPARATOR +
			String.format("%s",        type) + Utils.SEPARATOR + 
			String.format("%s",     payload) + Utils.SEPARATOR ;
		return s;
    }
	
	public String toString () { /* Just prettier than pack() */
		String s = 
			String.format("%03d",      source) + Utils.SEPARATOR + 
			String.format("%03d", destination) + Utils.SEPARATOR +
			String.format("%s"  ,        type) + Utils.SEPARATOR + 
			String.format("%s"  ,     payload) + Utils.SEPARATOR ;
		return s;
    }
	
	public static void main (String [] args) {
		String s = "0<|>0<|><|><|>";
		Message m = Message.parse(s);
		System.out.println(m);
	}
}
