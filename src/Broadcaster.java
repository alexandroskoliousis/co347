/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

import java.lang.Math;

class Broadcaster extends Process {
	
	private long times, count = 0;
	
	private long _t_recv, _t_send;
	private long delay;
	private double delaysquared;

	public Broadcaster (String name, int pid, int n) {
		
		super(name, pid, n);
		
		/* Initialise this object's members */
		count = 0;
		times = 0;
		
		_t_recv = 0;
		_t_send = 0;
		/* Init delay */
		delay = 0;
		delaysquared = 0.0;
	}
	
	private void begin () {
		long t__send, dt;
		double rate;
		_t_send = System.currentTimeMillis();
		while (++times <= Utils.MAX) {
			broadcast("TEST", String.format("%d", System.currentTimeMillis()));
			if (times % Utils.STEP == 0) {
				t__send = System.currentTimeMillis();
				dt = t__send - _t_send;
				if (dt > 0)
					rate = (double) (Utils.STEP * 1000) / (double) dt;
				else
					rate = 0.0;
				Utils.out(pid, String.format("[SEND] %06d\trate %10.1f m/s", times, rate));
				_t_send = t__send;
			}
		}
	}
	
	public synchronized void receive (Message m) {
		long t__recv, dt;
		double rate;
		long d;
		double average, deviation;
		count += 1;
		if (count == 1)
			_t_recv = System.currentTimeMillis();
		d = (System.currentTimeMillis() - Long.parseLong(m.getPayload()));
		/* Accumulate */
		delay += d;
		delaysquared += (double) (d * d);
		if (count % Utils.STEP == 0) {
			t__recv = System.currentTimeMillis();
			dt = t__recv - _t_recv;
			if (dt > 0)
				rate = (double) (Utils.STEP * 1000) / (double) dt;
			else
				rate = 0.0;
			/* Calculate average delay */
			average = (double) delay / (double) Utils.STEP;
			/* Calculate std deviation */
			deviation = Math.sqrt(
				(delaysquared - (double) (delay * delay) / (double) Utils.STEP) / (double) (Utils.STEP - 1)
			);
			Utils.out(pid, 
				String.format(
				"[RECV] %06d\trate %10.1f m/s\tavg. delay %10.1f\tstddev %10.1f", 
				count, rate, average, deviation
				)
			);
			_t_recv = t__recv;
			/* Reset delay */
			delay = 0;
			delaysquared = 0.0;
		}
	}
	
	public static void main(String [] args) {
		String name = args[0];
		int pid = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		Broadcaster P = new Broadcaster(name, pid, n);
		/* Don't forget to register! */
		P.registeR ();
		P.begin ();
	}
}
