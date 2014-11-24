package name.kazennikov.dafsa;

import gnu.trove.list.array.TIntArrayList;
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
public abstract class AbstractIntDAFSAUnlabeled extends IntDaciukAlgoUnlabeled {
	/**
	 * Basic FSA state. Doesn't store the final value.
	 * The value is stored in the DAFSA itself
	 * @author Anton Kazennikov
	 *
	 */
	public class State {
		TIntArrayList next = new TIntArrayList();

		int inbound;
		int outbound;
		int number;
		int data;
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
		
		public void setData(int data) {
			this.data = data;
		}
		
		public int getData() {
			return data;
		}
		
		int findIndex(int input) {
			
			for(int i = 0; i != next.size(); i++) {
				if(next.get(i) == Constants.INVALID_STATE)
					continue;
				
				State dest = states.get(next.get(i));
				if(dest.getData() == input)
					return i;
			}

			return -1;
		}
		
		public int getNext(int input) {
			int index = findIndex(input);
			
			if(index == -1)
				return Constants.INVALID_STATE;
			
			return next.get(index);
		}
		
		public void removeNext(int input) {
			int index = findIndex(input);
			
			if(index != -1) {				
				State n = states.get(this.next.get(index));
				next.set(index, -1);
				n.removeInbound(this);
				validHashCode = false;
			}
		}
		
		public int outboundCount() {
			int count = 0;
			for(int i = 0; i < next.size(); i++) {
				if(next.get(i) != Constants.INVALID_STATE)
					count++;
			}
			
			return count;
		}
		
		

		public void setNext(int next) {
			if(next != Constants.INVALID_STATE) {
				int emptyIndex = this.next.indexOf(-1);
				
				if(emptyIndex == -1) {
					this.next.add(next);
				} else {
					this.next.set(emptyIndex, next);
					
				}
				
				State s = states.get(next);
				s.addInbound(this);
			} 
			
			validHashCode = false;
		}
		
		public int outbound() {
			return outboundCount();//next.size();
		}
		

		public int inbound() {
			return inbound;
		}


		public void removeInbound(State node) {
			inbound--;
			
		}

		public void addInbound(State node) {
			inbound++;
		}


		int hc() {
			int result = finalHash(number);
			result += data;
			
			for(int i = 0; i != next.size(); i++) {
				if(next.get(i) != -1)
					result += next.get(i);
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
			
			// TODO: generic data compare
			if(data != other.data)
				return false;
			
			if(!finalEquals(number, other.number))
				return false;

			
			if(outboundCount() != other.outboundCount())
				return false;

			for(int i = 0; i != next.size(); i++) {
				if(next.get(i) == -1)
					continue;
				
				int otherIndex = other.next.indexOf(next.get(i));
				if(otherIndex == -1)
					return false;
			}

			
			return true;
		}

		public void reset() {
			finalReset(number);
			data = -2;
			
			for(int i = 0; i != outbound(); i++) {
				int input = next.get(i);
				State next = states.get(this.next.get(i));
				next.removeInbound(this);
			}
			
			next.resetQuick();
		}

		public State assign(final State node) {
			finalAssign(node.getNumber(), number);
			node.data = data;
			for(int i = 0; i != next.size(); i++) {
				if(next.get(i) != -1) {
					State dest = states.get(next.get(i));
					dest.addInbound(this);
					node.next.add(next.get(i));
				}
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
	
	public AbstractIntDAFSAUnlabeled() {
		initFinals();
		startState = addState(-2);
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
		int clonedState = addState(src.data);
		src.assign(states.get(clonedState));
		states.get(clonedState).hashCode = src.hashCode;
		states.get(clonedState).validHashCode = src.validHashCode;
		return clonedState;
	}
	
	@Override
	public int addState(int data) {
		if(!free.isEmpty()) {

			State s = free.poll();
			s.data = data;
			return s.getNumber();
		}
		
		State s = new State();
		s.data = data;
		s.number = states.size();
		states.add(s);
		newFinal(s.number);
		
		return s.getNumber();
	}
	
	@Override
	public void removeNext(int src, int label) {
		State s = states.get(src);
		s.removeNext(label);
	}

	@Override
	public boolean setNext(int src, int dest) {
		State s = states.get(src);
		s.setNext(dest);
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
			
			for(int i = 0; i < n.next.size(); i++) {
				if(n.next.get(i) != -1)
					pw.printf("%d -> %d;%n", n.number, n.next.get(i));
			}

			if(isFinalState(n.getNumber())) {
				pw.printf("%d [shape=doublecircle,label = \"%d [%s]\"];%n", n.number, n.number, "" + (char) n.data);
			} else {
				if(n.data != -2)
					pw.printf("%d [label = \"%d [%s]\"];%n", n.number, n.number, "" + (char) n.data);
			}
		}
		
		pw.println("}");
	}
	
	public void toDot(String fileName) throws IOException {
		PrintWriter pw = new PrintWriter(fileName);
		toDot(pw);
		pw.close();
	}
}
