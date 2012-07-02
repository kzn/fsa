package name.kazennikov.dafsa;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Hash-based trie for int-length labels. Uses smaller memory footprint than {@link CharFSA}
 * when constructing trie-based structures
 * @author Anton Kazennikov
 *
 */
public class IntTrie {
	int states = 1;
	TLongIntHashMap trans = new TLongIntHashMap();
	TIntObjectHashMap<TIntSet> finals = new TIntObjectHashMap<TIntSet>();
	
	public int size() {
		return states;
	}
	/**
	 * Get final features of a state
	 * @return null, if state doesn't have final features
	 */
	public TIntSet getFinals(int state) {
		return finals.get(state);
	}
	
	/**
	 * Add a final feature to the state
	 * @param state state number
	 * @param fin final feature
	 */
	public void addFinal(int state, int fin) {
		TIntSet stateFinals = finals.get(state);
		
		if(stateFinals == null) {
			stateFinals = new TIntHashSet();
			finals.put(state, stateFinals);
		}
		
		stateFinals.add(fin);
	}
	
	public long getKey(int srcState, int label) {
		long key = srcState;
		key <<= 32;
		key += label;
		return key;
	}
	
	/**
	 * Add transition deterministically to the trie
	 * @param srcState source state number
	 * @param label transition label
	 * @param nextState destination state number
	 */
	public void setNext(int srcState, int label, int nextState) {
		trans.put(getKey(srcState, label), nextState);
	}
	
	/**
	 * Get next state on current state and transition label
	 * @param srcState source state number
	 * @param label transition label
	 * @return state number, or 0, if there is no such transition
	 */
	public int getNext(int srcState, int label) {
		return trans.get(getKey(srcState, label));
	}
	
	/**
	 * Add suffix to the given state
	 * @param state source state
	 * @param seq sequence, containing the suffix
	 * @param startOffset suffix start offset
	 * @param fin final feature of the suffix
	 * @return end state number
	 */
	public int addSuffix(int state, TIntArrayList seq, int startOffset, int fin) {
		for(int i =  startOffset; i < seq.size(); i++) {
			int nextState = ++states;
			setNext(state, seq.get(i), nextState);
			state = nextState;
		}
		
		addFinal(state, fin);
		return states;
	}
	
	/**
	 * Add given sequence with given final feature to the trie
	 * @param seq sequence
	 * @param finals final feature
	 */
	public void add(TIntArrayList seq, int finals) {
		if(seq.isEmpty())
			return; 
		
		int state = 1;		
		int idx = 0;

		while(idx < seq.size()) {
			int nextState = getNext(state, seq.get(idx));
			if(nextState == 0)
				break;

			idx++;
			state = nextState;
		}
		
		if(idx == seq.size()) {
			addFinal(state, finals);
		} else {
			addSuffix(state, seq, idx, finals);
		}
	}
	
	
	/**
	 * Add given sequence (through iterator) with given final feature to the trie
	 * @param it sequence iterator
	 * @param finals final feature
	 */
	public void add(TIntIterator it, int finals) {
		if(!it.hasNext())
			return;
		
		int state = 1;
		
		int currentInput = 0;

		while(it.hasNext()) {
			currentInput = it.next();
			
			int nextState = getNext(state, currentInput);
			if(nextState == 0) {
				nextState = ++states;
				setNext(state, currentInput, nextState);
			}

			state = nextState;
		}
		
		addFinal(state, finals);
	}

	
	

}
