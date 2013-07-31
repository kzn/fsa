package name.kazennikov.dafsa;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.HashMap;

import name.kazennikov.dafsa.IntFSA.Node;

public abstract class DaciukAlgo {
	public static final int INVALID_STATE = -1;
	
	class Register {
		TIntIntHashMap map;
		
		HashMap<IntFSA.Node, IntFSA.Node> m = new HashMap<IntFSA.Node, IntFSA.Node>();

		public boolean contains(IntFSA.Node node) {
			return m.containsKey(node);
		}

		public int get(int node) {
			Node n = m.get(nodes.get(node));
			return n == null? INVALID_STATE : n.getNumber();
		}

		public void add(int n) {
			Node node = nodes.get(n);
			m.put(node, node);
		}

		public void remove(int n) {
			Node node = nodes.get(n);
			IntFSA.Node regNode = m.get(node);

			if(regNode == null)
				return;

			if(node == regNode)
				m.remove(node);
		}

	}
	
	Register reg;

	
	
	public int getStartState() {
		return 0;
	}
	
	public abstract int getNext(int state, int input);
	
	public abstract boolean isConfluence(int state);
	
	public abstract int cloneState(int state);
	public abstract int addState();
	public abstract void setNext(int src, int label, int dest);
	public abstract void addFinal(int state, int finalId);
	public abstract void removeState(int State);
	
	/**
	 * Add suffix to given new state
	 * 
	 * @param n base node
	 * @param seq sequence to add
	 * @param fin final state
	 */
	protected TIntList addSuffix(int n, TIntList seq, int start, int end, int fin) {
		int current = n;

		TIntList nodes = new TIntArrayList();

		for(int i = start; i < end; i++) {

			int in = seq.get(i);
			int node = addState();
			nodes.add(node);
			setNext(current, in, node);
			current = node;
		}

		addFinal(current, fin);

		return nodes;
	}

	
	
	TIntList commonPrefix(TIntList seq) {
		int current = getStartState();
		TIntArrayList prefix = new TIntArrayList();
		prefix.add(current);

		for(int i = 0; i != seq.size(); i++) {
			int in = seq.get(i);
			int next = getNext(current, in);

			if(next == INVALID_STATE)
				break;

			current = next;
			prefix.add(current);
		}

		return prefix;
	}

	int findConfluence(TIntList nodes) {
		for(int i = 0; i != nodes.size(); i++) {
			if(isConfluence(nodes.get(i)))
				return i;
		}

		return 0;
	}


	
	public void addMinWord(TIntList input, int fin) {
		/*
		 * 1. get common prefix
		 * 2. find first confluence state in the common prefix
		 * 3. if any, clone it and all states after it in common prefix
		 * 4. add suffix
		 * 5. minimize(replaceOrRegister from the last state toward the first)
		 */

		TIntList prefix = commonPrefix(input);

		int confIdx = findConfluence(prefix);
		int stopIdx = confIdx == 0? prefix.size() : confIdx;

		if(confIdx > 0) {	
			int idx = confIdx;

			while(idx < prefix.size()) {
				int prev = prefix.get(idx - 1);
				int cloned = cloneState(prefix.get(idx));
				prefix.set(idx, cloned);
				setNext(prev, input.get(confIdx - 1), cloned);
				idx++;
				confIdx++;
			}
		}



		TIntList nodeList = new TIntArrayList(prefix);

		nodeList.addAll(addSuffix(prefix.get(prefix.size() - 1), input, prefix.size() - 1, input.size(), fin));

		replaceOrRegister(input, nodeList, stopIdx);



	}


	private void replaceOrRegister(TIntList input, TIntList nodeList, int stop) {
		if(nodeList.size() < 2)
			return;

		int idx = nodeList.size() - 1;
		int inIdx = input.size() - 1;

		while(idx > 0) {
			int n = nodeList.get(idx);
			int regNode = reg.get(n);

			// stop
			if(regNode == n) {
				if(idx < stop)
					return;
			} else if(regNode == INVALID_STATE) {
				reg.add(n);
			} else {
				int in = input.get(inIdx);
				setNext(nodeList.get(idx - 1), in, regNode);
				nodeList.set(idx, regNode);
				removeState(n);
			}
			inIdx--;
			idx--;
		}

	}


}
