package name.kazennikov.trie;

import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import name.kazennikov.fsm.Constants;

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
public abstract class AbstractIntTrieBuilder extends IntDaciukAlgoIndexed {

	/**
	 * Basic FSA node. Doesn't store the final value.
	 * The value is stored in the DAFSA itself
	 * @author Anton Kazennikov
	 *
	 */
	public class Node {
		TLongArrayList next = new TLongArrayList();

		int inbound;
		int number;
		int hashCode;
		boolean registered;
		boolean validHashCode = true;
		
		public Node() {
			inbound = 0;
			hashCode = 1;
		}
		
		public void setNumber(int num) {
			this.number = num;
		}
		
		public int getNumber() {
			return number;
		}
		
		public long encode(int input, int next) {
			long k = input;
			k <<= 32;
			k += next;
			return k;
		}
		
		public int label(long val) {
			return (int)(val >>> 32);
		}
		
		public int dest(long val) {
			return (int) (val & 0xFFFFFFFFL);
		}
		
		int findIndex(int input) {
			for(int i = 0; i != next.size(); i++) {
				if(label(next.get(i)) == input)
					return i;
			}

			return -1;
		}
		
		public int getNext(int input) {
			int index = findIndex(input);
			if(index == -1)
				return Constants.INVALID_STATE;
			
			return dest(next.get(index));
		}
		
		

		public void setNext(int input, int next) {
			int index = findIndex(input);
			
			if(index != -1) {
				Node n = nodes.get(dest(this.next.get(index)));
				n.removeInbound(input, this);
			}
			
			
			
			if(next != Constants.INVALID_STATE) {
				if(index == -1) {
					this.next.add(encode(input, next));
				} else {
					this.next.set(index, encode(input, next));
				}
				Node n = nodes.get(next);
				n.addInbound(input, this);
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


		public void removeInbound(int input, Node node) {
			inbound--;
			
		}

		public void addInbound(int input, Node node) {
			inbound++;
		}


		int hc() {
			int result = finalHash(number);
			
			for(int i = 0; i != next.size(); i++) {
				result += label(next.get(i));
				result += dest(next.get(i));

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
			if(!(obj instanceof Node))
				return false;
			

			Node other = (Node) obj;
			
			if(!finalEquals(number, other.number))
				return false;

			if(next.size() != other.next.size())
				return false;

			for(int i = 0; i != outbound(); i++) {
				int otherIndex = other.findIndex(label(next.get(i)));
				if(otherIndex == -1)
					return false;

				if(dest(next.get(i)) != dest(other.next.get(otherIndex)))
					return false;
			}

			
			return true;
		}

		public void reset() {
			finalReset(number);
			for(int i = 0; i != outbound(); i++) {
				int input = label(next.get(i));
				Node next = nodes.get(dest(this.next.get(i)));
				
				next.removeInbound(input, this);
			}
			
			next.clear();
		}

/*		public TIntObjectIterator<BaseNode> next() {
			return new TIntObjectIterator<BaseNode>() {
				
				int pos = -1;
				
				@Override
				public void remove() {
				}
				
				@Override
				public boolean hasNext() {
					return pos < next.size();
				}
				
				@Override
				public void advance() {
					pos++;
				}
				
				@Override
				public BaseNode value() {
					return nodes.get(dest(next.get(pos)));
				}
				
				@Override
				public BaseNode setValue(BaseNode val) {
					return null;
				}
				
				@Override
				public int key() {
					return label(next.get(pos));
				}
			};
		}*/
		

		public Node assign(final Node node) {
			finalAssign(node.getNumber(), number);
			for(int i = 0; i != next.size(); i++) {
				node.setNext(label(next.get(i)), dest(next.get(i)));
			}

			return node;
		}
		
		@Override
		public String toString() {
			return String.format("state=%d", number);
		}
	}
	
	GenericRegister<Node> register = new GenericRegister<Node>();
	
	@Override
	public void regAdd(int state) {
		if(nodes.get(state).registered)
			return;
		nodes.get(state).registered = true;
		
		register.add(nodes.get(state));
		
	}

	@Override
	public int regGet(int state) {
		Node n = register.get(nodes.get(state));
		
		if(n == null)
			return Constants.INVALID_STATE;

		return n.getNumber();
	}

	@Override
	public void regRemove(int state) {
		if(!nodes.get(state).registered)
			return;
		
		nodes.get(state).registered = false;
		
		register.remove(nodes.get(state));

	}
	
	List<Node> nodes = new ArrayList<Node>();
	PriorityQueue<Node> free = new PriorityQueue<>(10, new Comparator<Node>() {

		@Override
		public int compare(Node o1, Node o2) {
			return o1.number - o2.number;
		}
	});
	int startState;
	
	public AbstractIntTrieBuilder() {
		initFinals();
		startState = addState();
	}

	@Override
	public int getNext(int state, int input) {
		Node n = nodes.get(state);
		int next = n.getNext(input);
		
		return next;
	}

	@Override
	public boolean isConfluence(int state) {
		return nodes.get(state).inbound() > 1;
	}

	@Override
	public int cloneState(int state) {
		Node src = nodes.get(state);
		int node = addState();
		src.assign(nodes.get(node));
		nodes.get(node).hashCode = src.hashCode;
		nodes.get(node).validHashCode = src.validHashCode;
		return node;
	}
	
	@Override
	public int addState() {
		if(!free.isEmpty())
			return free.poll().getNumber();
		
		Node n = new Node();
		n.number = nodes.size();
		nodes.add(n);
		newFinal(n.number);
		
		return n.getNumber();
	}

	@Override
	public boolean setNext(int src, int label, int dest) {
		Node n = nodes.get(src);
		n.setNext(label, dest);
		return false;
	}

	@Override
	public void removeState(int state) {
		Node n = nodes.get(state);
		n.reset();
		free.add(n);
	}
	
	public int size() {
		return nodes.size() - free.size();
	}
	
	/**
	 * Init finality values for this FSA
	 */
	public abstract void initFinals();

	
	/**
	 * Compute hash code for the final value of the state 
	 * @param state state
	 * @return hash code
	 */
	public abstract int finalHash(int state);
	
	/**
	 * Check if final values of two states are equal
	 * @param state1 first state
	 * @param state2 second state
	 * 
	 * @return true, if they are equal
	 */
	public abstract boolean finalEquals(int state1, int state2);
	
	/**
	 * Reset the final value of the given state
	 * @param state
	 */
	public abstract void finalReset(int state);
	
	/**
	 * Copy final value from source state to destination state
	 * @param destState destination state
	 * @param srcState source state
	 */
	public abstract void finalAssign(int destState, int srcState);
	
	/**
	 * Initialize final value for given state
	 * @param state
	 */
	public abstract void newFinal(int state);
}
