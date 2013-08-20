package name.kazennikov.fsa.walk;

import gnu.trove.set.hash.TIntHashSet;

public class WalkFSABolean extends BaseWalkFSA {
	TIntHashSet finals = new TIntHashSet();
		
	public boolean isFinalState(int state) {
		return finals.contains(state);
	}

	
	
}
