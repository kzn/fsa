package name.kazennikov.fsa.walk;

import gnu.trove.list.array.TIntArrayList;
import name.kazennikov.fsa.Constants;

public class BaseWalkUnlabeledFSA {
	TIntArrayList dest = new TIntArrayList();
	TIntArrayList data = new TIntArrayList();
	
	TIntArrayList stateStart = new TIntArrayList();
	
	
	public int next(int src, int input) {
		for(int i = stateStart(src); i < stateEnd(src); i++) {
			int dest = this.dest.get(i);
			if(data.get(dest) == input)
				return dest;
		}
		
		return Constants.INVALID_STATE;
	}	
	
	public int dest(int transitionIndex) {
		return dest.get(transitionIndex);
	}
	
	public int stateStart(int state) {
		return stateStart.get(state);
	}
	
	public int data(int state) {
		return this.data.get(state);
	}
	
	public int stateEnd(int state) {
		state++;
		return state == stateStart.size()? dest.size() : stateStart.get(state);
	}
		
	


}
