package name.kazennikov.trie;

import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;

import com.google.common.base.Objects;

public class IntTrieBuilderIntNG extends AbstractIntTrieBuilder {
	
	ArrayList<TIntHashSet> finals;
	
	public IntTrieBuilderIntNG() {
		super();
	}

	@Override
	public int finalHash(int state) {
		return finals.get(state).hashCode();
	}

	@Override
	public boolean finalEquals(int state1, int state2) {
		return Objects.equal(finals.get(state1), finals.get(state2));
	}

	@Override
	public void finalReset(int state) {
		finals.get(state).clear();
		states.get(state).validHashCode = false;
		
	}

	@Override
	public void finalAssign(int destState, int srcState) {
		TIntHashSet dest = finals.get(destState);
		TIntHashSet src = finals.get(srcState);
		dest.clear();
		dest.addAll(src);

	}
	
	@Override
	public void initFinals() {
		finals = new ArrayList<>();
	}

	@Override
	public void newFinal(int state) {
		finals.add(new TIntHashSet(3));
		
	}
	
	int finalValue;
	
	public void setFinalValue(int finalValue) {
		this.finalValue = finalValue;
	}

	@Override
	public boolean setFinal(int state) {
		boolean b = finals.get(state).add(finalValue);
		states.get(state).validHashCode = false;
		return b;
	}

	@Override
	public boolean isFinal(int state) {
		return finals.get(state).contains(finalValue);
	}
	
	public TIntHashSet getFinals(int state) {
		return finals.get(state);
	}

}
