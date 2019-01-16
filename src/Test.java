import java.util.*;

class Test extends Process {
	
	/* This array stores the minimum hop-count 
	 * distance to a destination `v` in [1,n] 
	 */
	private int [] D;
	
	/* Increments whenever a `mydist` message is received */
	private int mydistCount;
	
	public Test (String name, int pid, int n) {
		super(name, pid, n);
		
		D = new int [n+1]; /* Don't forget that id range from 1 to n */
		mydistCount = 0;
	}
	
	public void begin () {
		return ;
	}
	
	public void checkRoutingDistances () {
		
		/* It creates a message of the form:
		 * this.pid<|>0<|>Utils.CHECK_COST<|>1:D[1];2:D[2]...n:D[n]
		 */
		String payload = "";
		String f;
		for (int v = 1; v < n + 1; v++) {
			f = "%d:%d";
			if (v != n) f += ";";
			payload += String.format(f, v, D[v]);
		}
		Message m = new Message(pid, 0, Utils.CHECK_COST, payload);
		if (unicast (m))
			Utils.out(pid, "OK");
		else
			Utils.out(pid, "Error");
		return ;
	}
	
	public int getMydistCount() {
		return mydistCount;
	}
	
	public synchronized void receive (Message m) { /* Dummy implementation */
		Utils.out(pid, m.toString());
		return ;
	}
	
	public static void main (String [] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int  n = Integer.parseInt(args[2]);
		Test p = new Test(name, id, n);
		p.registeR ();
		p.begin ();
		
		/* Check periodically for convergence. */
		int current, previous = 0;
		int count = 0;
		
		while (true) { /* Sleep, poll, check. */
			try {
				/* Follow the periodicity of heartbeat messages */
				Thread.sleep(Utils.Delta);
				/* Get the current `mydist` message count */
				current = p.getMydistCount();
				
				/* For debugging purposes */
				Utils.out(id, 
				String.format("previous = %d, current = %d, count = %d", 
				previous, current, count));
				
				if (previous == current) count ++;
				else count = 0;
				
				/* 10 x Delta should be enough. */
				if (count == 10) {
					/* Check computed routing distances */
					p.checkRoutingDistances();
					
					/*
					 * At this point, you should print your routing table
					 * (not distances) to a file named `name`-rt-1.txt.
					 *
					 * The format should be along the lines of:
					 * String.format("%d:%s")
					 * where:
					 * %d is the destination id, and
					 * %s is the string representation of the next best hop.
					 * This can be "local", "undefined", or a number.
					 */
					
					/* Reset counters */
					previous = 0;
				} else {
					previous = current;
				}
			
			} catch (Exception e) { e.printStackTrace(); }
		}
	}
}

