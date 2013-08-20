package name.kazennikov.dafsa;

import java.util.ArrayList;
import java.util.List;

public abstract class GenericDaciukAlgoObject<E, I> {
	
	/**
	 * Find matching outbound transition for given state
	 * @param state source state
	 * @param input label
	 * 
	 * @return dest state, or -1, if no such transition exists
	 */
	public abstract E getNext(E state, I input);
	
	/**
	 * Checks a state for confluence (if in has more that 1 inbound transitions)
	 * @param state state to check

	 * @return true if state has more than one inbound transitions
	 */
	public abstract boolean isConfluence(E state);
	
	/**
	 * Clones given state. It clones:
	 * <ul>
	 * <li> transitions
	 * <li> final features
	 * </ul>
	 * @param state
	 * @return
	 */
	public abstract E cloneState(E state);
	
	/**
	 * Adds a new state to the automaton
	 * 
	 * @return index of the new state
	 */
	public abstract E addState();
	
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
	public abstract boolean setNext(E src, I label, E next);
	
	/**
	 * Removes given state from the automaton
	 * @param state state index
	 */
	public abstract void removeState(E state);
		
	/**
	 * public set final feature for state
	 * 
	 * @param state 
	 * @return true, if state has changed, else false (this is possible then state is already final)
	 */
	public abstract boolean setFinal(E state);
	
	/**
	 * Checks if state is final for this final feature
	 * @param state
	 * @return
	 */
	public abstract boolean isFinal(E state);
	
	protected E startState;


	/**
	 * Add state to register
	 * 
	 * @param state
	 */
	public abstract void regAdd(E state);

	/**
	 * Get equivalent state from register
	 * 
	 * @param state reference state
	 * 
	 * @return number of registered state, or -1 (INVALID_STATE) if no such state exist
	 */
	public abstract E regGet(E state);
	
	/**
	 * Remove given state from register. Removes exact state not equivalent one.
	 * 
	 * @param state state to remove
	 */
	public abstract void regRemove(E state);
	
	/**
	 * Add suffix to given new state
	 * 
	 * @param startState base node
	 * @param seq sequence to add
	 * @param fin final state
	 */
	protected List<E> addSuffix(E startState, List<I> seq, int start, int end) {
		E current = startState;

		List<E> nodes = new ArrayList<E>();
		
		if(end > start) {
			regRemove(startState); // as we will change it by adding new states in the sequence
		}

		for(int i = start; i < end; i++) {
			I in = seq.get(i);
			E node = addState();
			nodes.add(node);
			setNext(current, in, node);
			current = node;
		}

		// this check is needed only when we set finalty on already existing state (not fresh created one)
		if(start == end) {
			if(!isFinal(current))
				regRemove(current);
		}
		
		setFinal(current);

		return nodes;
	}

	
	
	public List<E> commonPrefix(List<I> seq) {
		E current = startState;
		List<E> prefix = new ArrayList<E>();
		prefix.add(current);

		for(int i = 0; i != seq.size(); i++) {
			I in = seq.get(i);
			E next = getNext(current, in);

			if(next == null)
				break;

			current = next;
			prefix.add(current);
		}

		return prefix;
	}

	public int findConfluence(List<E> nodes) {
		for(int i = 0; i != nodes.size(); i++) {
			if(isConfluence(nodes.get(i)))
				return i;
		}

		return 0;
	}


	
	public void addMinWord(List<I> input) {
		/*
		 * 1. get common prefix
		 * 2. find first confluence state in the common prefix
		 * 3. if any, clone it and all states after it in common prefix
		 * 4. add suffix
		 * 5. minimize(replaceOrRegister from the last state toward the first)
		 */

		List<E> prefix = commonPrefix(input);

		int confIdx = findConfluence(prefix);
		/* index of stop for replaceOrRegister a pointer to the state before modifications
		 * caused by this word addition. 
		 * 
		 * The logic is: if the state isn't changed by replaceOrRegister we can safely bail out
		 * as all states before this won't change either
		*/
		int stopIdx = confIdx == 0? prefix.size() : confIdx; 

		if(confIdx > 0) {	
			int idx = confIdx;
			regRemove(prefix.get(idx - 1)); // as we will clone confluence state and change previous to link the cloned

			while(idx < prefix.size()) {
				E prev = prefix.get(idx - 1);
				E cloned = cloneState(prefix.get(idx));
				prefix.set(idx, cloned);
				setNext(prev, input.get(confIdx - 1), cloned);
				idx++;
				confIdx++;
			}
		}



		List<E> nodeList = new ArrayList<E>();

		nodeList.addAll(addSuffix(prefix.get(prefix.size() - 1), input, prefix.size() - 1, input.size()));

		replaceOrRegister(input, nodeList, stopIdx);



	}


	private void replaceOrRegister(List<I> input, List<E> nodeList, int stop) {
		if(nodeList.size() < 2)
			return;

		int idx = nodeList.size() - 1;
		int inIdx = input.size() - 1;

		while(idx > 0) {
			E n = nodeList.get(idx);
			E regNode = regGet(n);

			// stop
			if(regNode == n) {
				if(idx < stop)
					return;
			} else if(regNode == null) {
				regAdd(n);
			} else {
				I in = input.get(inIdx);
				regRemove(nodeList.get(idx - 1));
				setNext(nodeList.get(idx - 1), in, regNode);
				nodeList.set(idx, regNode);
				removeState(n);
			}
			inIdx--;
			idx--;
		}

	}
	
	public void add(List<I> seq) {
		E current = startState;

		int idx = 0;

		while(idx < seq.size()) {
			E n = getNext(current, seq.get(idx));
			if(n == null)
				break;

			idx++;
			current = n;
		}

		addSuffix(current, seq, idx, seq.size());
	}
}
