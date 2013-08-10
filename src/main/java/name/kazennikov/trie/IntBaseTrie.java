package name.kazennikov.trie;

import gnu.trove.list.array.TIntArrayList;

public class IntBaseTrie {
	TIntArrayList src;
	TIntArrayList dest;
	TIntArrayList labels;
	
	TIntArrayList stateStart;
	TIntArrayList stateEnd;
	
	int lastState = 0;
	
	public IntBaseTrie(BaseIntTrieBuilder builder) {
		for(BaseIntTrieBuilder.BaseNode n : builder.nodes) {
			stateStart.set(n.getNumber(), src.size());
			for(int i = 0; i < n.next.size(); i++) {
				int src = n.getNumber();
				int dest = n.dest(n.next.get(i));
				int label = n.label(n.next.get(i));
				addTransition(src, label, dest);
			}
			stateEnd.set(n.getNumber(), src.size());
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
	
	

}
