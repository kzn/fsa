package name.kazennikov.trie;

import gnu.trove.map.hash.TIntObjectHashMap;
import name.kazennikov.fsm.Constants;

public class IntTrieInt extends IntBaseTrie {
	
	TIntObjectHashMap<int[]> finals = new TIntObjectHashMap<>();
	public static final int[] EMPTY = new int[0];
	
	public IntTrieInt(IntTrieBuilderInt builder) {
		super(builder);
		
		for(BaseIntTrieBuilder.BaseNode n : builder.nodes) {
			IntTrieBuilderInt.Node node = (IntTrieBuilderInt.Node) n;
			
			if(node.fin != null && !node.fin.isEmpty()) {
				finals.put(node.getNumber(), node.fin.toArray());
			}
		}
	}
	
	public int[] getFinals(int state) {
		int[] fin = finals.get(state);
		return fin != null? fin : EMPTY;
	}
	
	public int[] walk(String s) {
		int state = 0;
		for(int i = 0; i < s.length(); i++) {
			state = next(state, s.charAt(i));
			if(state == Constants.INVALID_STATE)
				return EMPTY;
		}
		
		return getFinals(state);
	}
}
