package name.kazennikov.trie;

import gnu.trove.set.hash.TIntHashSet;

public class IntTrieBoolean extends IntBaseTrie {
	TIntHashSet finals = new TIntHashSet();
	
	public IntTrieBoolean(IntTrieBuilderBoolean builder) {
		super(builder);
		
		for(int i = 0; i < builder.size(); i++) {
			
			if(builder.isFinalState(i))
				finals.add(i);
		}
	}
	
	public boolean isFinalState(int state) {
		return finals.contains(state);
	}

	
	
}
