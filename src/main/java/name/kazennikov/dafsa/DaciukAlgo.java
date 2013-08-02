package name.kazennikov.dafsa;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

/**
 * Generic algorighm for constructing minimal AFSA (acyclic finite state automata) or minimial
 * tries.
 * 
 * @author Anton Kazennikov
 *
 */
public abstract class DaciukAlgo {
	public static final int INVALID_STATE = -1;
	
	/**
	 * Find matching outbound transition for given state
	 * @param state source state
	 * @param input label
	 * 
	 * @return dest state, or -1, if no such transition exists
	 */
	public abstract int getNext(int state, int input);
	
	/**
	 * Checks a state for confluence (if in has more that 1 inbound transitions)
	 * @param state state to check

	 * @return true if state has more than one inbound transitions
	 */
	public abstract boolean isConfluence(int state);
	
	/**
	 * Clones given state. It clones:
	 * <ul>
	 * <li> transitions
	 * <li> final features
	 * </ul>
	 * @param state
	 * @return
	 */
	public abstract int cloneState(int state);
	
	/**
	 * Adds a new state to the automaton
	 * 
	 * @return index of the new state
	 */
	public abstract int addState();
	
	/**
	 * Adds a transition from src to dest on given label, if
	 * a transition from src with given label already exists, then change it
	 * to new destination state.
	 * 
	 * @param src source state
	 * @param label transition label 
	 * @param dest destination state
	 */
	public abstract void setNext(int src, int label, int dest);
	
	/**
	 * Add final feature to given state
	 * @param state
	 * @param finalId
	 */
	public abstract void addFinal(int state, int finalId);

	/**
	 * Removes given state from the automaton
	 * @param state state index
	 */
	public abstract void removeState(int state);
	/**
	 * Computes hash code of the state
	 * @param state
	 * @return
	 */
	public abstract int hash(int state);
	/**
	 * Checks for state equality. Checks
	 * <ul>
	 * <li> transitions
	 * <li> final features
	 * </ul>
	 * @param state1
	 * @param state2
	 * @return
	 */
	public abstract boolean equals(int state1, int state2);
	
	/**
	 * Get equivalent state from the register
	 * @param state
	 * @return
	 */
	public abstract int registerGet(int state);

	/**
	 * Adds given state to register
	 * @param state
	 */
	public abstract void registerAdd(int state);
	
	/**
	 * Removes given state from register
	 * @param state
	 */
	public abstract void registerRemove(int state); 
	
	protected int startState;
	
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
		int current = startState;
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
			int regNode = registerGet(n);

			// stop
			if(regNode == n) {
				if(idx < stop)
					return;
			} else if(regNode == INVALID_STATE) {
				registerAdd(n);
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
