package name.kazennikov.dafsa;

import gnu.trove.list.array.TLongArrayList;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import name.kazennikov.fsa.Constants;

/**
 * Abstract DAFSA builder. It almost complete implementation, where only
 * finality state data is missing. Main difference from previous implementations
 * is that final value is stored in the DAFSA instead of the state.
 * 
 * So one doesn't need to subclass the node state, instead one can implement
 * it's own handling strategy for final values using abstract functions.
 * 
 * Also, the finals are indexed by state number
 * 
 * @author Anton Kazennikov
 *
 */
public abstract class AbstractIntDAFSA extends IntDaciukAlgoIndexed {
	
	/**
	 * Decode label from the transition table value
	 * @param val transition table value
	 * @return
	 */
	public static int decodeLabel(long val) {
		return (int)(val >>> 32);
	}
	
	/**
	 * Decode destination from the transition table value
	 * @param val transition table value
	 * @return
	 */
	public static int decodeDest(long val) {
		return (int) (val & 0xFFFFFFFFL);
	}
	
	/**
	 * Encode transition to table value
	 * @param input input symbol
	 * @param next next state number
	 * @return
	 */
	public static long encodeTransition(int input, int next) {
		long k = input;
		k <<= 32;
		k += next;
		return k;
	}

	

	/**
	 * Basic FSA state. Doesn't store the final value.
	 * The value is stored in the DAFSA itself
	 * @author Anton Kazennikov
	 *
	 */
	public class State {
		TLongArrayList next = new TLongArrayList();

		int inbound;
		int number;
		int hashCode;
		boolean registered;
		boolean validHashCode = true;
		
		public State() {
			inbound = 0;
			hashCode = 1;
		}
		
		public void setNumber(int num) {
			this.number = num;
		}
		
		public int getNumber() {
			return number;
		}
		
		int findIndex(int input) {
			for(int i = 0; i != next.size(); i++) {
				if(decodeLabel(next.get(i)) == input)
					return i;
			}

			return -1;
		}
		
		public int getNext(int input) {
			int index = findIndex(input);
			if(index == -1)
				return Constants.INVALID_STATE;
			
			return decodeDest(next.get(index));
		}
		
		

		public void setNext(int input, int next) {
			int index = findIndex(input);
			
			if(index != -1) {
				State n = states.get(decodeDest(this.next.get(index)));
				n.removeInbound(input, this);
			}
			
			if(next != Constants.INVALID_STATE) {
				if(index == -1) {
					this.next.add(encodeTransition(input, next));
				} else {
					this.next.set(index, encodeTransition(input, next));
				}
				State s = states.get(next);
				s.addInbound(input, this);
			} else if(index != -1) {
				this.next.removeAt(index);
			}
			
			validHashCode = false;
		}
		
		public int outbound() {
			return next.size();
		}
		

		public int inbound() {
			return inbound;
		}


		public void removeInbound(int input, State node) {
			inbound--;
			
		}

		public void addInbound(int input, State node) {
			inbound++;
		}


		int hc() {
			int result = finalHash(number);
			
			for(int i = 0; i != next.size(); i++) {
				result += decodeLabel(next.get(i));
				result += decodeDest(next.get(i));
			}

			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			if(!validHashCode) {
				hashCode = hc();
				validHashCode = true;
			}
			
			return hashCode;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(!(obj instanceof State))
				return false;
			

			State other = (State) obj;
			
			if(!finalEquals(number, other.number))
				return false;

			if(next.size() != other.next.size())
				return false;

			for(int i = 0; i != outbound(); i++) {
				int otherIndex = other.findIndex(decodeLabel(next.get(i)));
				if(otherIndex == -1)
					return false;

				if(decodeDest(next.get(i)) != decodeDest(other.next.get(otherIndex)))
					return false;
			}

			
			return true;
		}

		public void reset() {
			finalReset(number);
			
			for(int i = 0; i != outbound(); i++) {
				int input = decodeLabel(next.get(i));
				State next = states.get(decodeDest(this.next.get(i)));
				next.removeInbound(input, this);
			}
			
			next.clear();
		}

		public State assign(final State node) {
			finalAssign(node.getNumber(), number);

			for(int i = 0; i != next.size(); i++) {
				node.setNext(decodeLabel(next.get(i)), decodeDest(next.get(i)));
			}

			return node;
		}
		
		@Override
		public String toString() {
			return String.format("state=%d", number);
		}
	}
	
	GenericRegister<State> register = new GenericRegister<State>();
	
	@Override
	public void regAdd(int state) {
		if(states.get(state).registered)
			return;
		
		states.get(state).registered = true;
		register.add(states.get(state));
		
	}

	@Override
	public int regGet(int state) {
		State s = register.get(states.get(state));
		
		if(s == null)
			return Constants.INVALID_STATE;

		return s.getNumber();
	}

	@Override
	public void regRemove(int state) {
		if(!states.get(state).registered)
			return;
		
		states.get(state).registered = false;
		register.remove(states.get(state));
	}
	
	List<State> states = new ArrayList<State>();
	PriorityQueue<State> free = new PriorityQueue<>(10, new Comparator<State>() {

		@Override
		public int compare(State s1, State s2) {
			return s1.number - s2.number;
		}
	});
	int startState;
	
	public AbstractIntDAFSA() {
		initFinals();
		startState = addState();
	}

	@Override
	public int getNext(int state, int input) {
		State s = states.get(state);
		int next = s.getNext(input);
		
		return next;
	}

	@Override
	public boolean isConfluence(int state) {
		return states.get(state).inbound() > 1;
	}

	@Override
	public int cloneState(int srcState) {
		State src = states.get(srcState);
		int clonedState = addState();
		src.assign(states.get(clonedState));
		states.get(clonedState).hashCode = src.hashCode;
		states.get(clonedState).validHashCode = src.validHashCode;
		return clonedState;
	}
	
	@Override
	public int addState() {
		if(!free.isEmpty())
			return free.poll().getNumber();
		
		State s = new State();
		s.number = states.size();
		states.add(s);
		newFinal(s.number);
		
		return s.getNumber();
	}

	@Override
	public boolean setNext(int src, int label, int dest) {
		State s = states.get(src);
		s.setNext(label, dest);
		return false;
	}

	@Override
	public void removeState(int state) {
		State s = states.get(state);
		s.reset();
		free.add(s);
	}
	
	public int size() {
		return states.size() - free.size();
	}
	
	public int getTransitionCount(int state) {
		return states.get(state).next.size();
	}
	
	public int getTransitionInput(int state, int transitionIndex) {
		return decodeLabel(states.get(state).next.get(transitionIndex));
	}
	
	public int getTransitionNext(int state, int transitionIndex) {
		return decodeDest(states.get(state).next.get(transitionIndex));
	}

	
	public int getStartState() {
		return startState;
	}
	
	/**
	 * Initialize finality values for this FSA
	 */
	public abstract void initFinals();

	
	/**
	 * Compute hash code for the final value of the state 
	 * 
	 * @param state state
	 * 
	 * @return hash code
	 */
	public abstract int finalHash(int state);
	
	/**
	 * Check if final values of two states are equal
	 * 
	 * @param state1 first state
	 * @param state2 second state
	 * 
	 * @return true, if they are equal
	 */
	public abstract boolean finalEquals(int state1, int state2);
	
	/**
	 * Reset the final value of the given state
	 * 
	 * @param state
	 */
	public abstract void finalReset(int state);
	
	/**
	 * Copy final value from source state to destination state
	 * 
	 * @param destState destination state
	 * @param srcState source state
	 */
	public abstract void finalAssign(int destState, int srcState);
	
	/**
	 * Initialize final value for given state
	 * 
	 * @param state
	 */
	public abstract void newFinal(int state);
	
	/**
	 * Checks if state is final
	 * @param state state number
	 */
	public abstract boolean isFinalState(int state);
	
	
	/**
	 * Output this DAFSA to dot format
	 * @param pw print writer
	 * @throws IOException
	 */
	public void toDot(PrintWriter pw) throws IOException {
		pw.println("digraph fsm {");
		pw.println("rankdir=LR;");
		pw.println("node [shape=circle,style=filled, fillcolor=white]");
		

		for(State n : states) {
			if(n.getNumber() == startState) {
				pw.printf("%d [fillcolor=\"gray\"];%n", n.getNumber());
			}
			
			for(int i = 0; i < n.outbound(); i++) {
				pw.printf("%d -> %d [label=\"%s\"];%n", n.number, decodeDest(n.next.get(i)), "" + ((char) decodeLabel(n.next.get(i))));
			}

			if(isFinalState(n.getNumber())) {
				pw.printf("%d [shape=doublecircle];%n", n.number);
			}
		}
		
		pw.println("}");
	}
	
	public void toDot(String fileName) throws IOException {
		PrintWriter pw = new PrintWriter(fileName);
		toDot(pw);
		pw.close();
	}
	

	public int transitionCount() {
		int count = 0;
		for(State s : states) {
			count += s.next.size();
		}
		
		return count;

	}


}
