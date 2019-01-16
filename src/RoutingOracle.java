/* Copyright (c) 2013-2014, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 *
 * Partially based on R. Sedgewick's and K. Wayne's implementation
 * Algorithms, 4th edition
 */

import java.io.*;
import java.util.*;

public class RoutingOracle {
	
	/* cost[u][v] is the length of the shortest path from v to w */
	private int [][] cost;
	
	/* Edges, read from file */
	private int [][] edge;
	
	private int [][] last;
	private int [][] next;
	
	private Hashtable<Integer, ArrayList<Integer>> nodes;
	
	/* G = (V, E) */
	private int _V_;
	private int _E_;
	private int INF; /* Infeasible path length */
	private int INV; /* Invalid next/last  hop */
	
	public RoutingOracle (int N, String filename) {

		_V_ = N;
		INF = _V_ + 1; /* No path should be greater that N + 1 */
		INV = _V_;
		_E_ = 0;
		
		cost = new int[_V_][_V_];
        edge = new int[_V_][_V_];
        last = new int[_V_][_V_];
        next = new int[_V_][_V_];
		
		nodes = new Hashtable<Integer, ArrayList<Integer>>();
		
		/* Initialise edges 
		 *
		 * This operation is unsafe. It assumes that the file
		 * is formatted correctly.
		 */
		load (filename);

		if (! isGraphUndirected())
			Utils.out("Warning: graph is directed\n");
	}
	
	public int getV() { return _V_;     }
	/* |E| = E / 2 because graph G is an undirected graph */
	public int getE() { return _E_ / 2; }
	
	public boolean reset() {

		initialise();
		computeShortestPaths(); /* Compute last [][] */
		computeRoutingTables(); /* Compute next [][] */
		
		return check();
	}
	
	public boolean remove (int w) {
		Integer key = new Integer(w);
		if (nodes.containsKey(key))
			return false;
		ArrayList<Integer> values = getNeighbours(w);
		nodes.put(key, values);
		for (Integer U: values) {
			int u = U.intValue();
			edge[u][w] = 0;
			edge[w][u] = 0;
		}
		return reset (); /* Hm. Too expensive, needs more work. */
	}
	
	public boolean repair (int w) {
		Integer key = new Integer(w);
		if (! nodes.containsKey(key))
			return false;
		ArrayList<Integer> values = nodes.remove(key);
		for (Integer U: values) {
			int u = U.intValue();
			edge[u][w] = 1;
			edge[w][u] = 1;
		}
		return reset ();
	}
	
	private void initialise() {
		/* Initialize costs based on connectivity */
		for (int u = 0; u < _V_; u++) {
			for (int v = 0; v < _V_; v++) {
				if (u == v) {
					cost[u][v] = 0;
					last[u][v] = u;
				} else
				if (edge[u][v] == 1) { /* They are neighbors */
					cost[u][v] = edge[u][v];
					last[u][v] = u;
				} else 
				{
					cost[u][v] = INF;
					last[u][v] = INV;
				}
			}
		}
		return ;
	}
	
	private void computeShortestPaths() {
		for (int i = 0; i < _V_; i++) { /* Pivot */
			for (int u = 0; u < _V_; u++) {
				if (u == i) /* Skip diagonal */
					continue;
				for (int v = 0; v < _V_; v++) {
					if (cost[u][v] > cost[u][i] + cost[i][v]) {
						cost[u][v] = cost[u][i] + cost[i][v];
						last[u][v] = last[i][v];
					}
				}
			}
		}
		return ;
	}
	
	private boolean isGraphUndirected() {
		for (int u = 0; u < _V_; u++) {
			for (int v = 0; v < _V_; v++) {
				if (edge[u][v] != edge[v][u])
					return false;
			}
		}
		return true;
	}
	
	private void load (String filename) {
		FileInputStream f;
		DataInputStream d;
		BufferedReader  b;
		
		String line;
		int u = 0, v;
		try {

			f = new FileInputStream(filename);
			d = new DataInputStream(f);
			b = new BufferedReader(new InputStreamReader(d));
			
			while ((line = b.readLine()) != null) {
				String [] link = line.split(",");
				/* Assumes link.length == V */
				for (v = 0; v < link.length; v++) {
					int value = (link[v].equals("1") && u != v ? 1 : 0);
					edge[u][v] = value;
					_E_ += value;
				}
				u ++;
			}
			if (u != _V_) {
				System.err.println(String.format("Error: %s is incorrect", filename));
				System.exit(1);
			}
		} catch (Exception e) {
			System.err.println("Error: cannot read file " + filename);
		}
		return ;
	}
	
	public boolean hasPath(int u, int v) { return (cost[u][v] < INF); }
	
	public int getPathLength(int u, int v) { return cost[u][v]; }
	
	public int getNextHop(int u, int v) { return next[u][v]; }
	
	public boolean areNeighbours(int u, int v) {
		boolean result = false;
		if (u >= 0 && u < _V_ && v >= 0 && v < _V_) /* Is within bounds? */
			result = (edge[u][v] == 1);
		return result;
	}
	
	public boolean isBestNextHop(int u, int v, int w) {
		
		if (! areNeighbours(u, w))
			return false;
		
		if (getPathLength(u, v) < (getPathLength(u, w) + getPathLength(w, v)))
			return false;
		
		return true;
	}
	
	public ArrayList<Integer> getNeighbours(int u) {
		ArrayList<Integer> neighbours = new ArrayList<Integer>();
		for (int v = 0; v < _V_; v++)
			if (edge[u][v] == 1 && u != v)
				neighbours.add(v);
		return neighbours;
	}
	
	private void dumpRoutingTable(int u) {
		Utils.out(String.format("%d's routing table", u));
		for (int w = 0; w < _V_; w++)
			Utils.out(String.format("%d: %d", w, next[u][w]));
	}
	
	public Stack<Integer> getPath(int u, int v) {
		if (! hasPath(u, v)) 
			return null;
		Stack<Integer> path = new Stack<Integer>();
		path.push(v);
		int w = last[u][v];
		while (w != u) {
			path.push(w);
			w = last[u][w];
		}
		return path;
	}
	
	private void computeRoutingTables() {
		for (int u = 0; u < _V_; u++) {
			for (int v = 0; v < _V_; v++) {
				Stack<Integer> s = getPath(u, v);
				if (s != null)
					next[u][v] = s.peek().intValue();
			}
		}
	}
	
	private boolean check () {
		for (int u = 0; u < _V_; u++) {
			for (int v = 0; v < _V_; v++) {
				ArrayList<Integer> neighbours = getNeighbours(u);
				for (Integer w: neighbours) {
					int z = w.intValue();
					if (cost[u][v] > cost[u][z] + cost[z][v])
						return false;
				}
			}
		}
        return true;
    }
	
	/* Unit tests */
	public static void main(String[] args) {
		
		int N = Integer.parseInt(args[0]);
		String filename = args[1];
		
		/* Floyd-Warshall algorithm */
		RoutingOracle o = new RoutingOracle(N, filename);
		
		/* Is it connected? */
		for (int u = 0; u < N; u++) {
			for (int v = 0; v < N; v++) {
				if (! o.hasPath(u, v)) {
					System.err.println(
					"Error: path from " + u + " to " + v + " does not exist"
					);
					return;
				}
			}
		}
		
		/* Print neighbors */
		for (int u = 0; u < N; u++) {
			System.out.println(String.format("%2d's neighbors are %s", u, o.getNeighbours(u)));
		}

		System.out.println(String.format("Path from %d to %d is %s", 0, 4, o.getPath(0, 4)));
	}
}
