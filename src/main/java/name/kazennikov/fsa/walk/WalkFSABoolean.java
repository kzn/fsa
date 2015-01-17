package name.kazennikov.fsa.walk;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import name.kazennikov.fsa.Constants;
import name.kazennikov.fsa.IntFSABooleanEventHandler;

import java.util.ArrayList;
import java.util.List;

public class WalkFSABoolean extends BaseWalkFSA {

	TIntHashSet finals = new TIntHashSet();
    private final long serialVersionUUID = 1L;


		
	public boolean isFinalState(int state) {
		return finals.contains(state);
	}
	
	
	public static class Builder implements IntFSABooleanEventHandler {
		WalkFSABoolean fsa = new WalkFSABoolean();
		boolean isFinal;
		int state;
		@Override
		public void startState(int state) {
			fsa.stateStart.add(fsa.labels.size());
			this.state = state;
		}

		@Override
		public void setFinal() {
			if(isFinal)
				fsa.finals.add(state);
		}

		@Override
		public void addTransition(int label, int destState) {
			fsa.labels.add(label);
			fsa.dest.add(destState);
		}

		@Override
		public void endState() {			
		}

		@Override
		public void setFinalValue(boolean value) {
			isFinal = value;
		}
		
		public WalkFSABoolean build() {
			fsa.dest.trimToSize();
			fsa.finals.trimToSize();
			fsa.labels.trimToSize();
			fsa.stateStart.trimToSize();
			return fsa;
		}
	}

    public boolean hasAnnotStart(int state) {
        return next(state, 0) != Constants.INVALID_STATE;
    }

    /**
     * Collect all annotations starting at the state
     * @param state
     */
    public TIntArrayList collectAnnotationsSimple(int state, int sepValue) {
        //List<int[]> annots = new ArrayList<int[]>();
        TIntArrayList annots = new TIntArrayList();

        if(!hasAnnotStart(state))
            return annots;

        TIntArrayList data = new TIntArrayList();
        collectAnnotations(next(state, 0), data, true, annots, sepValue);
        return annots;
    }



    /**
     * Collect all annotations starting at this (or descendents) state
     * @param state start state
     * @param annots output list for annotations
     */
    public void collectAnnotations(int state, TIntArrayList annots, int sepValue) {
        TIntArrayList data = new TIntArrayList();
        collectAnnotations(state, data, false, annots, sepValue);
    }



    /**
     * Recursively collects annotation from the FSA
     * @param state state
     * @param data current annotation data
     * @param passedAnnotChar true if the walker passed the annotation char
     * @param annots output list for the annotations
     */
    public void collectAnnotations(int state, TIntArrayList data, boolean passedAnnotChar, TIntArrayList annots, int sepValue) {

        if(finals.contains(state)) {
            for(int i = 0; i < data.size(); i++) {
                annots.add(data.get(i));
            }

            annots.add(sepValue);
        }

        int start = stateStart.get(state);
        int end = state < stateStart.size() - 1? stateStart.get(state + 1) : stateStart.size();

        while(start < end) {
            int input = labels.get(start);
            if(passedAnnotChar)
                data.add(labels.get(start));
            collectAnnotations(dest.get(start), data, input == 0? true : passedAnnotChar, annots, sepValue);
            if(passedAnnotChar)
                data.removeAt((data.size() - 1));
            start++;
        }
    }

}
