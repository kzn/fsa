package name.kazennikov.trie;

import gnu.trove.list.array.TIntArrayList;
import name.kazennikov.fsm.Constants;

public class IntBaseTrie {
	TIntArrayList src = new TIntArrayList();
	TIntArrayList dest = new TIntArrayList();
	TIntArrayList labels = new TIntArrayList();
	
	TIntArrayList stateStart = new TIntArrayList();
	TIntArrayList stateEnd = new TIntArrayList();
	
	int lastState = 0;
	/**
	 * This constructor is based on following assumptions:
	 * <ul>
	 * <li> the state indexes are continuous and start from 0
	 * <li> transitions sorted by start state (and may be by input symbol)
	 * <li> 
	 * </ul>
	 * @param builder
	 */
	public IntBaseTrie(BaseIntTrieBuilder builder) {
		for(BaseIntTrieBuilder.BaseNode n : builder.nodes) {
			stateStart.add(src.size());
			for(int i = 0; i < n.next.size(); i++) {
				int src = n.getNumber();
				int dest = n.dest(n.next.get(i));
				int label = n.label(n.next.get(i));
				addTransition(src, label, dest);
			}
			stateEnd.add(src.size());
		}
	}
	
	public void addTransition(int src, int label, int dest) {
		this.src.add(src);
		this.dest.add(dest);
		this.labels.add(label);
		lastState = Math.max(lastState, Math.max(src, dest));
	}
	
	public int transitionCount() {
		return src.size();
	}
	
	public int stateCount() {
		return lastState + 1; // assuming that states are started from 0
	}
	
	public int next(int src, int input) {
		for(int i = stateStart.get(src); i < stateEnd.get(src); i++) {
			if(labels.get(i) == input)
				return dest.get(i);
		}
		
		return Constants.INVALID_STATE;
	}
	
	

}
