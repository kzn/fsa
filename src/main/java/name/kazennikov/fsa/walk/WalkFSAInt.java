package name.kazennikov.fsa.walk;

import java.util.ArrayList;
import java.util.List;

import name.kazennikov.fsa.Constants;
import name.kazennikov.fsa.IntFSAObjectEventHandler;

public class WalkFSAInt extends BaseWalkFSA {
	
	List<int[]> finals = new ArrayList<>();
	public static final int[] EMPTY = new int[0];
	
	
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
	
	public static class Builder implements IntFSAObjectEventHandler<int[]> {
		WalkFSAInt fsa = new WalkFSAInt();
		int[] values;
		@Override
		public void startState(int state) {
			fsa.stateStart.add(fsa.labels.size());
		}

		@Override
		public void setFinal() {
			fsa.finals.add(values);
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
		public void setFinalValue(int[] object) {
			this.values = object;
			if(object.length == 0)
				this.values = EMPTY;
			
		}
		
		public WalkFSAInt build() {
			return fsa;
		}
	}

}
