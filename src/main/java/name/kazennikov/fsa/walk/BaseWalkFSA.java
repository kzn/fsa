package name.kazennikov.fsa.walk;

import gnu.trove.list.array.TIntArrayList;
import name.kazennikov.fsa.Constants;

public class BaseWalkFSA {
	TIntArrayList dest = new TIntArrayList();
	TIntArrayList labels = new TIntArrayList();
	
	TIntArrayList stateStart = new TIntArrayList();
	TIntArrayList stateEnd = new TIntArrayList();
	
	
	public int next(int src, int input) {
		for(int i = stateStart.get(src); i < stateEnd.get(src); i++) {
			if(labels.get(i) == input)
				return dest.get(i);
		}
		
		return Constants.INVALID_STATE;
	}
	
	public int label(int transitionIndex) {
		return labels.get(transitionIndex);
	}
	
	public int dest(int transitionIndex) {
		return dest.get(transitionIndex);
	}
	
	public int stateStart(int state) {
		return stateStart.get(state);
	}
	
	public int stateEnd(int state) {
		return stateEnd.get(state);
	}
		
	

}
