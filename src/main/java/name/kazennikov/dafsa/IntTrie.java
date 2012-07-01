package name.kazennikov.dafsa;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class IntTrie {
	int states = 1;
	TLongIntHashMap trans = new TLongIntHashMap();
	TIntObjectHashMap<TIntSet> finals = new TIntObjectHashMap<TIntSet>();
	
	public int size() {
		return states;
	}
	
	public TIntSet getFinals(int state) {
		return finals.get(state);
	}
	
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
	
	public void setNext(int srcState, int label, int nextState) {
		trans.put(getKey(srcState, label), nextState);
	}
	
	public int getNext(int srcState, int label) {
		return trans.get(getKey(srcState, label));
	}
	
	public int addSuffix(int state, TIntArrayList seq, int startOffset, int fin) {
		for(int i =  startOffset; i < seq.size(); i++) {
			int nextState = ++states;
			setNext(state, seq.get(i), nextState);
			state = nextState;
		}
		
		addFinal(state, fin);
		return states;
	}
	
	public void add(TIntArrayList seq, int finals) {
		if(seq.isEmpty())
			return; 
		
		int state = 1;
		int offset = 0;
		
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
