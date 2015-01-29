package name.kazennikov.fsa.walk;

import cern.colt.GenericSorting;
import cern.colt.Swapper;
import cern.colt.function.IntComparator;
import gnu.trove.list.array.TIntArrayList;
import name.kazennikov.dafsa.TroveUtils;
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

    public void sortTransitions() {
        for(int i = 0; i < stateStart.size(); i++) {
            int start = stateStart(i);
            int end = stateEnd(i);

            GenericSorting.quickSort(start, end, new IntComparator() {
                @Override
                public int compare(int i, int i2) {
                    return data.get(i) - data.get(i2);
                }
            }, new Swapper() {
                @Override
                public void swap(int i, int i2) {
                    TroveUtils.swap(dest, i, i2);
                }
            });
        }
    }
		
	


}
