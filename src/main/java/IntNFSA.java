import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.DataInputStream;
import java.io.IOException;

import name.kazennikov.dafsa.IntFSA;
import name.kazennikov.dafsa.IntTrie.Builder;


public class IntNFSA {
	int states = 1;
	TLongObjectHashMap<TLongList> trans = new TLongObjectHashMap<TLongList>();
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
	
	public char getInputLabel(long key) {
		return (char)(key & 0xffffL);
	}
	
	public char getOutputLabel(long key) {
		return (char)( (key & 0xffffffffL) >>> 16);
	}

	
	public int getState(long key) {
		return (int)(key >>> 32);
	}

	
	
	
	/**
	 * Add transition deterministically to the trie
	 * @param srcState source state number
	 * @param label transition label
	 * @param nextState destination state number
	 */
	public void setNext(int srcState, char inLabel, char outLabel, int nextState) {
		long key = getKey(srcState, inLabel);
		TLongList trans = this.trans.get(key);
		
		if(trans == null) {
			trans = new TLongArrayList();
			this.trans.put(key, trans);
		}
		
		//trans.put(getKey(srcState, label), nextState);
	}
	
	/**
	 * Get next state on current state and transition label
	 * @param srcState source state number
	 * @param label transition label
	 * @return state number, or 0, if there is no such transition
	 */
	public TLongList getNext(int srcState, int label) {
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
	
	public void clear() {
		states = 1;
		trans.clear();
		finals.clear();
	}
	
	public static interface Builder<T> extends IntFSA.Events {
		public T build();
	}
	
	public static class Reader {
		public static void read(DataInputStream s, Builder<?> builder, int labelSize) throws IOException {
			int states = s.readInt();
			
			builder.state(states);
			
			for(int i = 0; i < states; i++) {
				builder.state(i + 1);
				// read finals
				int finCount = s.readInt();
				builder.finals(finCount);
				for(int j = 0; j < finCount; j++) {
					builder.stateFinal(s.readInt());
				}
				
				int transCount = s.readInt();
				builder.transitions(transCount);
				for(int j = 0; j < transCount; j++) {
					int label = labelSize == 2? s.readChar() : s.readInt();
					int dest = s.readInt();
					builder.transition(label, dest);
				}
				
			}
		}
		
	}

	

}
