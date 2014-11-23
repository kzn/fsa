package name.kazennikov.fsa.walk;

import gnu.trove.set.hash.TIntHashSet;
import name.kazennikov.fsa.IntFSABooleanEventHandler;

public class WalkFSABoolean extends BaseWalkFSA {
	TIntHashSet finals = new TIntHashSet();
		
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
			fsa.stateEnd.add(fsa.labels.size());
		}

		@Override
		public void setFinalValue(boolean value) {
			isFinal = value;
		}
		
		public WalkFSABoolean build() {
			return fsa;
		}
	}


	
	
}
