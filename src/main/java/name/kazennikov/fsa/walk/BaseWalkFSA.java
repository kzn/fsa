package name.kazennikov.fsa.walk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cern.colt.GenericSorting;
import cern.colt.Swapper;
import cern.colt.function.IntComparator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import name.kazennikov.dafsa.TroveUtils;
import name.kazennikov.fsa.Constants;

public class BaseWalkFSA implements Serializable {
	private static final long serialVersionUID = 1L;
	
	TIntArrayList dest = new TIntArrayList();
	TIntArrayList labels = new TIntArrayList();
	
	TIntArrayList stateStart = new TIntArrayList();
	
	
	public int next(int src, int input) {
        int start = stateStart(src);
        int end = stateEnd(src);
        int index = labels.binarySearch(input, start, end);

		/*for(int i = start; i < end; i++) {
			if(labels.get(i) == input)
				return dest.get(i);
		}*/
		
		//return Constants.INVALID_STATE;
        return index < 0? Constants.INVALID_STATE : dest.get(index);
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
                    return labels.get(i) - labels.get(i2);
                }
            }, new Swapper() {
                @Override
                public void swap(int i, int i2) {
                    TroveUtils.swap(labels, i, i2);
                    TroveUtils.swap(dest, i, i2);
                }
            });
        }
    }
    
    public int size() {
    	return stateStart.size();
    }

    public TIntArrayList walk(TIntArrayList res, int state, CharSequence s, int start, int end) {

        res.add(state);

        for (int i = start; i < end; i++) {
            int next = next(state, (int) s.charAt(i));

            if (next == -1)
                break;

            res.add(next);

            state = next;
        }

        return res;
    }

    public TIntArrayList walk(int state, CharSequence s, int start, int end) {
        return walk(new TIntArrayList(), state, s, start, end);
    }

    public TIntArrayList walk(int state, TIntList s, int start, int end) {
        TIntArrayList res = new TIntArrayList();

        res.add(state);

        for (int i = start; i < end; i++) {
            int next = next(state, (int) s.get(i));

            if (next == -1)
                break;

            res.add(next);

            state = next;
        }

        return res;
    }


}
