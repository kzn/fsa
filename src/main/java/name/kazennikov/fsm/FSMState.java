package name.kazennikov.fsm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Objects;

public class FSMState<E> {
	int number;
	List<FSMTransition<E>> transitions = new ArrayList<FSMTransition<E>>();
	E finals;
	
	public void setFinals(E finals) {
		this.finals = finals;
	}

	public FSMTransition<E> addTransition(FSMState<E> to, int label) {
		FSMTransition<E> t = new FSMTransition<E>(this, label, to);
		transitions.add(t);
		return t;
	}
	
	/**
	 * Get next state from this one on given input (threat the state transitions deterministically)
	 * This means that only the first matched input is returned
	 * 
	 * @param input input symbol
	 * 
	 * @return next state, or null if there is no such transition
	 */
	public FSMState<E> next(int input) {
		for(int i = 0; i < transitions.size(); i++) {
			FSMTransition<E> t = transitions.get(i);
			
			if(t.label == input)
				return t.dest;
		}
		
		return null;
	}


	public void toDot(PrintWriter pw, Set<FSMState<E>> visited) {
		if(visited.contains(this))
			return;

		visited.add(this);

		for(FSMTransition<E> t : transitions) {
			pw.printf("%d -> %d [label=\"%d\"];%n", number, t.dest.number, t.label);
		}

		for(FSMTransition<E> t : transitions) {
			t.dest.toDot(pw, visited);
		}



	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("number", number)
				.toString();
	}
	
	public List<FSMTransition<E>> getTransitions() {
		return transitions;
	}
	
	public E getFinals() {
		return finals;
	}
	
	public int getNumber() {
		return number;
	}
}