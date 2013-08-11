package name.kazennikov.trie;

import gnu.trove.set.hash.TIntHashSet;

public class IntTrieBoolean extends IntBaseTrie {
	TIntHashSet finals = new TIntHashSet();
	
	public IntTrieBoolean(IntTrieBuilderBoolean builder) {
		super(builder);
		
		for(BaseIntTrieBuilder.BaseNode n : builder.nodes) {
			IntTrieBuilderBoolean.Node node = (IntTrieBuilderBoolean.Node) n;
			
			if(node.fin)
				finals.add(node.getNumber());
		}
	}
	
	public boolean isFinal(int state) {
		return finals.contains(state);
	}

	
	
}
