package name.kazennikov.fsa;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Objects;

public class FSAState<E> {
	int number;
	List<FSATransition<E>> transitions = new ArrayList<FSATransition<E>>();
	E finals;
	
	public void setFinals(E finals) {
		this.finals = finals;
	}

	public FSATransition<E> addTransition(FSAState<E> to, int label) {
		FSATransition<E> t = new FSATransition<E>(this, label, to);
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
	public FSAState<E> next(int input) {
		for(int i = 0; i < transitions.size(); i++) {
			FSATransition<E> t = transitions.get(i);
			
			if(t.label == input)
				return t.dest;
		}
		
		return null;
	}


	public void toDot(PrintWriter pw, Set<FSAState<E>> visited) {
		if(visited.contains(this))
			return;

		visited.add(this);

		for(FSATransition<E> t : transitions) {
			pw.printf("%d -> %d [label=\"%d\"];%n", number, t.dest.number, t.label);
		}

		for(FSATransition<E> t : transitions) {
			t.dest.toDot(pw, visited);
		}



	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("number", number)
				.toString();
	}
	
	public List<FSATransition<E>> getTransitions() {
		return transitions;
	}
	
	public E getFinals() {
		return finals;
	}
	
	public int getNumber() {
		return number;
	}
}