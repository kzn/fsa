package name.kazennikov.trie;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import name.kazennikov.fsm.Constants;

public abstract class BaseIntTrieBuilder extends IntDaciukAlgoIndexed {
	
	public abstract class BaseNode {
		TLongArrayList next = new TLongArrayList();

		int inbound;
		int number;
		int hashCode;
		boolean registered;
		boolean validHashCode = true;
		
		public BaseNode() {
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
				BaseNode n = nodes.get(dest(this.next.get(index)));
				n.removeInbound(input, this);
			}
			
			
			
			if(next != Constants.INVALID_STATE) {
				if(index == -1) {
					this.next.add(encode(input, next));
				} else {
					this.next.set(index, encode(input, next));
				}
				BaseNode n = nodes.get(next);
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


		public void removeInbound(int input, BaseNode node) {
			inbound--;
			
		}

		public void addInbound(int input, BaseNode node) {
			inbound++;
		}


		int hc() {
			final int prime = 31;
			int result = 1;
			
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
			if(!(obj instanceof BaseNode))
				return false;
			

			BaseNode other = (BaseNode) obj;

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
			
			for(int i = 0; i != outbound(); i++) {
				int input = label(next.get(i));
				BaseNode next = nodes.get(dest(this.next.get(i)));
				
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
		

		public BaseNode assign(final BaseNode node) {
						
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
	
	GenericRegister<BaseNode> r = new GenericRegister<BaseNode>();
	
	@Override
	public void regAdd(int state) {
		if(nodes.get(state).registered)
			return;
		nodes.get(state).registered = true;
		
		r.add(nodes.get(state));
		
	}

	@Override
	public int regGet(int state) {
		BaseNode n = r.get(nodes.get(state));
		
		if(n == null)
			return Constants.INVALID_STATE;

		return n.getNumber();
	}

	@Override
	public void regRemove(int state) {
		if(!nodes.get(state).registered)
			return;
		
		nodes.get(state).registered = false;
		
		r.remove(nodes.get(state));

	}
	
	List<BaseNode> nodes = new ArrayList<BaseNode>();
	Stack<BaseNode> free = new Stack<BaseNode>();
	int startState;
	
	public BaseIntTrieBuilder() {
		startState = addState();
	}

	@Override
	public int getNext(int state, int input) {
		BaseNode n = nodes.get(state);
		int next = n.getNext(input);
		
		return next;
	}

	@Override
	public boolean isConfluence(int state) {
		return nodes.get(state).inbound() > 1;
	}

	@Override
	public int cloneState(int state) {
		BaseNode src = nodes.get(state);
		int node = addState();
		src.assign(nodes.get(node));
		nodes.get(node).hashCode = src.hashCode;
		nodes.get(node).validHashCode = src.validHashCode;
		return node;
	}
	
	public abstract BaseNode newNode();

	@Override
	public int addState() {
		if(!free.isEmpty())
			return free.pop().getNumber();
		
		BaseNode n = newNode();
		n.number = nodes.size();
		nodes.add(n);
		
		return n.getNumber();
	}

	@Override
	public boolean setNext(int src, int label, int dest) {
		BaseNode n = nodes.get(src);
		n.setNext(label, dest);
		return false;
	}

	@Override
	public void removeState(int state) {
		BaseNode n = nodes.get(state);
		n.reset();
		free.push(n);
	}

	

	
	public static TIntArrayList str(CharSequence s) {
		TIntArrayList l = new TIntArrayList();
		
		for(int i = 0; i < s.length(); i++) {
			l.add(s.charAt(i));
		}
		
		return l;
	}
	
	public int size() {
		return nodes.size() - free.size();
	}
	


}
