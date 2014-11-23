package name.kazennikov.dafsa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.kazennikov.fsa.IntFSAObjectEventHandler;

import com.google.common.base.Objects;

public class IntDAFSAObject<E> extends AbstractIntDAFSA {
	protected List<Set<E>> finals;

	@Override
	public void initFinals() {
		finals = new ArrayList<>();
	}

	@Override
	public int finalHash(int state) {
		Set<E> f = finals.get(state);
		return f.hashCode();
	}

	@Override
	public boolean finalEquals(int state1, int state2) {
		return Objects.equal(finals.get(state1), finals.get(state2));
	}

	@Override
	public void finalReset(int state) {
		finals.get(state).clear();
	}


	@Override
	public void newFinal(int state) {
		finals.set(state, new HashSet<E>());		
	}


	@Override
	public boolean isFinalState(int state) {
		return !finals.get(state).isEmpty();
	}
	
	E finalValue;
	
	

	public E getFinalValue() {
		return finalValue;
	}

	public void setFinalValue(E finalValue) {
		this.finalValue = finalValue;
	}

	@Override
	public boolean setFinal(int state) {
		return finals.get(state).add(finalValue);
	}

	@Override
	public boolean hasFinal(int state) {
		return finals.get(state).contains(finalValue);
	}

	@Override
	public void finalAssign(int destState, int srcState) {
		finals.get(destState).clear();
		finals.get(destState).addAll(finals.get(srcState));
		
	}
	
	public void emit(IntFSAObjectEventHandler<Set<E>> events) {
		for(int i = 0; i < states.size(); i++) {
			State s = states.get(i);
			events.startState(i);
			
			events.setFinalValue(finals.get(i));
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
