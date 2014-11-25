package name.kazennikov.dafsa;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import name.kazennikov.fsa.Constants;

/**
 * Generic algorithm for constructing minimal DAFSA (deterministic acyclic finite state automata) 
 * or minimial tries.
 * 
 * This variant uses unlabeled DAFSA. Data (characters) is stored state themselves and transitions 
 * only link states together. This is somewhat more natural that data is stored in the states 
 * explicitly, not in some opaque ways in the transitions. 
 * 
 * @author Anton Kazennikov
 *
 */
public abstract class UnlabeledIntDaciukAlgo {
	/**
	 * Find next state of given state with given data
	 * 
	 * @param state source state
	 * @param data data value
	 * 
	 * @return dest state, or -1, if no such transition exists
	 */
	public abstract int getNext(int state, int data);
	
	/**
	 * Checks a state for confluence (if in has more that 1 inbound transitions)
	 * 
	 * @param state state to check
	 * 
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
	 * 
	 * @return
	 */
	public abstract int cloneState(int state);
	
	/**
	 * Adds a new state to the automaton
	 * 
	 * @data data of the state
	 * @return index of the new state
	 */
	public abstract int addState(int data);
	
	
	/**
	 * Remove transition from given state to next state with given data
	 * @param state source state
	 * @param data target state data
	 */
	public abstract void removeNext(int state, int data);
	
	
	/**
	 * Adds a transition from src to dest.
	 * 
	 * @param src source state 
	 * @param dest destination state
	 * 
	 * @return true, if state has changed
	 */
	public abstract boolean setNext(int src, int dest);
	
	/**
	 * Removes given state from the automaton
	 * 
	 * @param state state index
	 */
	public abstract void removeState(int state);
		
	/**
	 * public set final feature for state
	 * 
	 * @param state state number
	 * 
	 * @return true, if state has changed, else false (this is possible then state is already final)
	 */
	public abstract boolean setFinal(int state);
	
	/**
	 * Checks if state is final for this final feature
	 * 
	 * @param state state number
	 * 
	 * @return
	 */
	public abstract boolean hasFinal(int state);
	
	/**
	 * Start state number
	 */
	protected int startState;


	/**
	 * Add state to register
	 * 
	 * @param state state number
	 */
	public abstract void regAdd(int state);

	/**
	 * Get equivalent state from register
	 * 
	 * @param state reference state
	 * 
	 * @return number of registered state, or -1 (INVALID_STATE) if no such state exist
	 */
	public abstract int regGet(int state);
	
	/**
	 * Remove given state from register. Removes exact state not equivalent one.
	 * 
	 * @param state state to remove
	 */
	public abstract void regRemove(int state);
	
	/**
	 * Add suffix to given new state
	 * 
	 * @param states [out] state list
	 * @param s base state number
	 * @param seq sequence to add
	 * @param fin final state
	 */
	protected TIntList addSuffix(TIntList states, int s, TIntList seq, int start, int end) {
		int current = s;
		
		if(end > start) {
			regRemove(s); // as we will change it by adding new states in the sequence
		}

		for(int i = start; i < end; i++) {
			int in = seq.get(i);
			int state = addState(in);
			
			if(states != null)
				states.add(state);
			
			setNext(current, state);
			current = state;
		}

		// this check is needed only when we set finalty on already existing state (not fresh created one)
		if(start == end) {
			if(!hasFinal(current))
				regRemove(current);
		}
		
		setFinal(current);

		return states;
	}

	
	/**
	 * Compute common prefix for given input sequence
	 * @param seq input sequence
	 * 
	 * @return list of states in prefix
	 */
	TIntList commonPrefix(TIntList seq) {
		int current = startState;
		TIntArrayList prefix = new TIntArrayList(seq.size() + 1);
		prefix.add(current);

		for(int i = 0; i != seq.size(); i++) {
			int in = seq.get(i);
			int next = getNext(current, in);

			if(next == Constants.INVALID_STATE)
				break;

			current = next;
			prefix.add(current);
		}

		return prefix;
	}

	/**
	 * Find first confluence state index
	 * 
	 * @param states state list 
	 * 
	 * @return confluence index, or -1 if no confluence state found
	 */
	int findConfluence(TIntList states) {
		for(int i = 0; i != states.size(); i++) {
			if(isConfluence(states.get(i)))
				return i;
		}

		return -1;
	}

	/**
	 * Add sequence to the DAFSA
	 * @param seq sequence to add
	 */
	public void addMinWord(TIntList seq) {
		/*
		 * 1. get common prefix
		 * 2. find first confluence state in the common prefix
		 * 3. if any, clone it and all states after it in common prefix
		 * 4. add suffix
		 * 5. minimize(replaceOrRegister from the last state toward the first)
		 */
		TIntList stateList = commonPrefix(seq);
		int confIdx = findConfluence(stateList);
		
		/* index of stop for replaceOrRegister a pointer to the state before modifications
		 * caused by this word addition. 
		 * 
		 * The logic is: if the state isn't changed by replaceOrRegister we can safely bail out
		 * as all states before this won't change either
		*/
		int stopIdx = confIdx == -1? stateList.size() : confIdx; 

		if(confIdx > -1) {	
			int idx = confIdx;
			regRemove(stateList.get(idx - 1)); // as we will clone confluence state and change previous to link the cloned

			while(idx < stateList.size()) {
				int prev = stateList.get(idx - 1);
				int cloned = cloneState(stateList.get(idx));
				stateList.set(idx, cloned);
				removeNext(prev, seq.get(confIdx - 1));
				setNext(prev, cloned);
				idx++;
				confIdx++;
			}
		}

		addSuffix(stateList, stateList.get(stateList.size() - 1), seq, stateList.size() - 1, seq.size());
		replaceOrRegister(seq, stateList, stopIdx);
	}


	protected void replaceOrRegister(TIntList input, TIntList stateList, int stop) {
		if(stateList.size() < 2)
			return;

		int stateIdx = stateList.size() - 1;
		int inputIdx = input.size() - 1;

		while(stateIdx > 0) {
			int n = stateList.get(stateIdx);
			int regNode = regGet(n);

			// stop
			if(regNode == n) {
				if(stateIdx < stop)
					return;
			} else if(regNode == Constants.INVALID_STATE) {
				regAdd(n);
			} else {
				int in = input.get(inputIdx);
				regRemove(stateList.get(stateIdx - 1));
				removeNext(stateList.get(stateIdx - 1), in);
				setNext(stateList.get(stateIdx - 1), regNode);
				stateList.set(stateIdx, regNode);
				removeState(n);
			}
			inputIdx--;
			stateIdx--;
		}

	}
	
	/**
	 * Add sequence to trie
	 * @param seq sequence to add
	 */
	public void add(TIntList seq) {
		int current = startState;

		int idx = 0;

		while(idx < seq.size()) {
			int s = getNext(current, seq.get(idx));
			if(s == Constants.INVALID_STATE)
				break;

			idx++;
			current = s;
		}
	
		addSuffix(null, current, seq, idx, seq.size());
	}
}
