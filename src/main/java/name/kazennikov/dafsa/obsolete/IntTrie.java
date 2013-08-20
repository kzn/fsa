package name.kazennikov.dafsa.obsolete;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Hash-based trie for int-length labels. Uses smaller memory footprint than {@link CharFSA}
 * when constructing trie-based structures
 * 
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
	
	public void clear() {
		states = 1;
		trans.clear();
		finals.clear();
	}
	
	public static interface Builder<T> extends IntFSA.Events {
		public T build();
	}
	
	
	public static class SimpleBuilder<E extends IntTrie> implements Builder<E> {
		E fsa;
		int current = 0;
		
		public SimpleBuilder(E fsa) {
			this.fsa = fsa;
		}

		@Override
		public void states(int states) {
			fsa.states = states;
		}

		@Override
		public void state(int state) {
			current = state;
			
		}

		@Override
		public void finals(int n) {
		}

		@Override
		public void stateFinal(int fin) {
			fsa.addFinal(current, fin);
			
		}

		@Override
		public void transitions(int n) {
		}

		@Override
		public void transition(int input, int dest) {
			fsa.setNext(current, input, dest);
		}

		@Override
		public E build() {
			return fsa;
		}

		@Override
		public void startState() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endState() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startFinals() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endFinals() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startTransitions() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endTransitions() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startStates() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endStates() {
			// TODO Auto-generated method stub
			
		}
		
	}
	public static class Reader {
		public static void read(DataInputStream s, Builder<?> builder, int labelSize) throws IOException, FSAException {
			int states = s.readInt();
			
			builder.states(states);
			TLongArrayList trans = new TLongArrayList();
			for(int i = 0; i < states; i++) {
				int stateNum = s.readInt();
				builder.state(stateNum);
				// read finals
				int finCount = s.readInt();
				builder.finals(finCount);
				for(int j = 0; j < finCount; j++) {
					builder.stateFinal(s.readInt());
				}
				
				int transCount = s.readInt();
				builder.transitions(transCount);
				trans.clear();
				for(int j = 0; j < transCount; j++) {
					int label = s.readInt();
					int dest = s.readInt();
					trans.add( (((long) label) << 32) + dest);
				}
				
				trans.sort();
				for(int j = 0; j < transCount; j++) {
					builder.transition((int)(trans.get(j) >> 32), (int)(trans.get(j) & 0xFFFFFFFFL));
				}
				
			}
		}
		
	}
	
	protected int countTransitions(long[] keys, int start) {
		int current = (int)(keys[start] >>> 32);
		int count = 0;
		for(int i = start; i < keys.length; i++) {
			int state = (int)(keys[i] >>> 32);
			if(state != current)
				return count;
			count++;
		}
		
		return count;
			
	}
	
	public void write(final IntFSA.Events writer) throws IOException, FSAException {
		writer.startStates();
		writer.states(states);
		
		long[] keys = trans.keys();
		Arrays.sort(keys);
		int lastState = 0;
		for(int i = 0; i != keys.length; i++) {
			long curr = keys[i];
			int state = (int) (curr >> 32);
			// for diff state write its header 
			if(state != lastState) {

				if(lastState != 0) {
					writer.endTransitions();
					writer.endState();
				}

				writer.startState();
				writer.state(state);

				writer.startFinals();
				TIntSet fin = getFinals(state);
				if(fin != null) {
					writer.finals(fin.size());
					TIntIterator it = fin.iterator();
					while(it.hasNext()) {
						writer.stateFinal(it.next());
					}
				} else {
					writer.finals(0);
				}
				writer.endFinals();
				writer.startTransitions();
				writer.transitions(countTransitions(keys, i));
			}

			int input = (int) (curr & 0xFFFFFFFFL);
			int dest = trans.get(curr);
			writer.transition(input, dest);
		}
		
		writer.endTransitions();
		writer.endState();
		writer.endStates();
	}
}
