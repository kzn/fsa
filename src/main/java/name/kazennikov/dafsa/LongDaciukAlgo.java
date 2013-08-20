package name.kazennikov.dafsa;

import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import name.kazennikov.fsa.Constants;

public abstract class LongDaciukAlgo {
	/**
	 * Find matching outbound transition for given state
	 * @param state source state
	 * @param input label
	 * 
	 * @return dest state, or -1, if no such transition exists
	 */
	public abstract int getNext(int state, long input);
	
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
	 * 
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
	 * 
	 * @return true, if state has changed
	 */
	public abstract boolean setNext(int src, long next, int dest);
	
	/**
	 * Removes given state from the automaton
	 * @param state state index
	 */
	public abstract void removeState(int state);
		
	/**
	 * public set final feature for state
	 * 
	 * @param state 
	 * @return true, if state has changed, else false (this is possible then state is already final)
	 */
	public abstract boolean setFinal(int state);
	
	/**
	 * Checks if state is final for this final feature
	 * @param state
	 * @return
	 */
	public abstract boolean isFinal(int state);
	
	protected int startState;


	/**
	 * Add state to register
	 * 
	 * @param state
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
	 * @param n base state
	 * @param seq sequence to add
	 * @param fin final state
	 */
	protected TIntList addSuffix(TIntList states, int n, TLongList seq, int start, int end) {
		int current = n;
		
		if(end > start) {
			regRemove(n); // as we will change it by adding new states in the sequence
		}

		for(int i = start; i < end; i++) {
			long in = seq.get(i);
			int state = addState();
			
			if(states != null)
				states.add(state);
			
			setNext(current, in, state);
			current = state;
		}

		// this check is needed only when we set finalty on already existing state (not fresh created one)
		if(start == end) {
			if(!isFinal(current))
				regRemove(current);
		}
		
		setFinal(current);

		return states;
	}

	
	
	TIntList commonPrefix(TLongList seq) {
		int current = startState;
		TIntArrayList prefix = new TIntArrayList();
		prefix.add(current);

		for(int i = 0; i != seq.size(); i++) {
			long in = seq.get(i);
			int next = getNext(current, in);

			if(next == Constants.INVALID_STATE)
				break;

			current = next;
			prefix.add(current);
		}

		return prefix;
	}

	int findConfluence(TIntList states) {
		for(int i = 0; i != states.size(); i++) {
			if(isConfluence(states.get(i)))
				return i;
		}

		return -1;
	}


	
	public void addMinWord(TLongList input) {
		/*
		 * 1. get common prefix
		 * 2. find first confluence state in the common prefix
		 * 3. if any, clone it and all states after it in common prefix
		 * 4. add suffix
		 * 5. minimize(replaceOrRegister from the last state toward the first)
		 */

		TIntList stateList = commonPrefix(input);

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
				setNext(prev, input.get(confIdx - 1), cloned);
				idx++;
				confIdx++;
			}
		}

		addSuffix(stateList, stateList.get(stateList.size() - 1), input, stateList.size() - 1, input.size());
		replaceOrRegister(input, stateList, stopIdx);
	}


	protected void replaceOrRegister(TLongList input, TIntList stateList, int stop) {
		if(stateList.size() < 2)
			return;

		int stateIdx = stateList.size() - 1;
		int inputIdx = input.size() - 1;

		while(stateIdx > 0) {
			int n = stateList.get(stateIdx);
			int regState = regGet(n);

			// stop
			if(regState == n) {
				if(stateIdx < stop)
					return;
			} else if(regState == Constants.INVALID_STATE) {
				regAdd(n);
			} else {
				long in = input.get(inputIdx);
				regRemove(stateList.get(stateIdx - 1));
				setNext(stateList.get(stateIdx - 1), in, regState);
				stateList.set(stateIdx, regState);
				removeState(n);
			}
			inputIdx--;
			stateIdx--;
		}

	}
	
	public void add(TLongList seq) {
		int current = startState;

		int idx = 0;

		while(idx < seq.size()) {
			int n = getNext(current, seq.get(idx));
			if(n == Constants.INVALID_STATE)
				break;

			idx++;
			current = n;
		}

		addSuffix(null, current, seq, idx, seq.size());
	}

	



}
