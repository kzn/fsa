package name.kazennikov.dafsa;

import gnu.trove.list.array.TByteArrayList;
import name.kazennikov.fsa.IntFSABooleanEventHandler;

public class IntDAFSABoolean extends AbstractIntDAFSA {
	TByteArrayList finals;
	
	@Override
	public int finalHash(int state) {
		return finals.get(state);
	}

	@Override
	public boolean finalEquals(int state1, int state2) {
		return finals.get(state1) ==  finals.get(state2);
	}

	@Override
	public void finalReset(int state) {
		finals.set(state, (byte)0);
		states.get(state).validHashCode = false;
		
	}

	@Override
	public void finalAssign(int destState, int srcState) {
		finals.set(destState, finals.get(srcState));
	}
	
	@Override
	public void initFinals() {
		finals = new TByteArrayList();
	}

	@Override
	public void newFinal(int state) {
		finals.add((byte)0);
	}
	
	boolean finalValue = true;
	
	public void setFinalValue(boolean finalValue) {
		this.finalValue = finalValue;
	}

	@Override
	public boolean setFinal(int state) {
		finals.set(state, finalValue? (byte)1 : (byte)0);
		states.get(state).validHashCode = false;
		return true;
	}

	@Override
	public boolean hasFinal(int state) {
		return (finals.get(state) == 1) == finalValue;
	}
	
	@Override
	public boolean isFinalState(int state) {
		return finals.get(state) != 0;
	}
	
	public void emit(IntFSABooleanEventHandler events) {
		for(int i = 0; i < states.size(); i++) {
			State s = states.get(i);
			events.startState(i);
			
			events.setFinalValue(finals.get(i) == 1);
			events.setFinal();

			
			for(int j = 0; j < s.next.size(); j++) {
				int input = decodeLabel(s.next.get(j));
				int dest = decodeDest(s.next.get(j));
				events.addTransition(input, dest);
			}
			
			events.endState();
		}
	}



}
