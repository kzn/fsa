package name.kazennikov.fsa.walk;

import gnu.trove.set.hash.TIntHashSet;
import name.kazennikov.fsa.IntFSABooleanEventHandler;
import name.kazennikov.fsa.UnlabeledIntBooleanFSAEventHandler;

public class WalkFSABooleanUnlabeled extends BaseWalkUnlabeledFSA {
	TIntHashSet finals = new TIntHashSet();
	
	public boolean isFinalState(int state) {
		return finals.contains(state);
	}
	
	
	public static class Builder implements UnlabeledIntBooleanFSAEventHandler {
		WalkFSABooleanUnlabeled fsa = new WalkFSABooleanUnlabeled();
		boolean isFinal;
		int state;
		
		@Override
		public void startState(int state, int data) {
			fsa.stateStart.add(fsa.data.size());
			fsa.data.add(data);
			this.state = state;
		}

		@Override
		public void setFinal() {
			if(isFinal)
				fsa.finals.add(state);
		}

		@Override
		public void addTransition(int destState) {
			fsa.dest.add(destState);
		}

		@Override
		public void endState() {			
		}

		@Override
		public void setFinalValue(boolean value) {
			isFinal = value;
		}
		
		public WalkFSABooleanUnlabeled build() {
			fsa.dest.trimToSize();
			fsa.finals.trimToSize();
			fsa.stateStart.trimToSize();
			return fsa;
		}
	}


	


}
