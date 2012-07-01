package name.kazennikov.dafsa;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class IntTrie {
	int states;
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
			setNext(state, seq.get(i), ++states);
		}
		
		addFinal(states, fin);
		return states;
	}
	
	public void add(TIntArrayList seq, int finals) {
		if(seq.isEmpty())
			return; 
		
		
		int state = 1;
		int offset = 0;
		
		do {
			int nextState = getNext(state, seq.get(offset));
			if(nextState == 0)
				break;
			state = nextState;
			offset++;
		} while(offset < seq.size());
		
		if(offset == seq.size()) {
			addFinal(state, finals);
		} else {
			addSuffix(state, seq, offset, finals);
		}
	}
}
