package name.kazennikov.trie;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.hash.TIntHashSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import name.kazennikov.dafsa.ng.GenericRegister;
import name.kazennikov.dafsa.ng.IntDaciukAlgoIndexed;
import name.kazennikov.fsm.Constants;

public class IntTrieBuilder extends IntDaciukAlgoIndexed {
	GenericRegister<Node> r = new GenericRegister<Node>();
	
	@Override
	public void regAdd(int state) {
		r.add(nodes.get(state));
		
	}

	@Override
	public int regGet(int state) {
		Node n = r.get(nodes.get(state));
		
		if(n == null)
			return Constants.INVALID_STATE;

		return n.getNumber();
	}

	@Override
	public void regRemove(int state) {
		r.remove(nodes.get(state));

	}
	
	/**
	 * Int-type node with trove structures as outbound table
	 * @author Anton Kazennikov
	 *
	 */
	public class Node {
		TIntHashSet fin = new TIntHashSet();
		TLongArrayList next = new TLongArrayList();

		int inbound;
		int number;
		int hashCode;
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
		
		private long encode(int input, int next) {
			long k = input;
			k <<= 32;
			k += next;
			return k;
		}
		
		private int label(long val) {
			return (int)(val >>> 32);
		}
		
		private int dest(long val) {
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
		

		public TIntIterator getFinal() {
			return fin.iterator();
		}
		

		public int finalCount() {
			return fin.size();
		}
		
		
		
		
		public boolean isFinal() {
			return fin != null && fin.size() > 0;
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


		public boolean addFinal(int fin) {
			validHashCode = !this.fin.add(fin);
			return !validHashCode;
		}


		public boolean removeFinal(int fin) {
			validHashCode = !this.fin.remove(fin);
			return !validHashCode;
		}
		
		int hc() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fin == null)? 0 : fin.hashCode());

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
			if(fin == null) {
				if(other.fin != null)
					return false;
			} else if(!fin.equals(other.fin))
				return false;
			else {

				if(next.size() != other.next.size())
					return false;
				
				for(int i = 0; i != outbound(); i++) {
					int otherIndex = other.findIndex(label(next.get(i)));
					if(otherIndex == -1)
						return false;
					
					if(dest(next.get(i)) != dest(other.next.get(otherIndex)))
						return false;
				}
			}
			
			return true;
		}

		public void reset() {
			fin.clear();
			
			for(int i = 0; i != outbound(); i++) {
				int input = label(next.get(i));
				Node next = nodes.get(dest(this.next.get(i)));
				
				next.removeInbound(input, this);
			}
			
			next.clear();
		}

		public TIntObjectIterator<Node> next() {
			return new TIntObjectIterator<Node>() {
				
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
				public Node value() {
					return nodes.get(dest(next.get(pos)));
				}
				
				@Override
				public Node setValue(Node val) {
					return null;
				}
				
				@Override
				public int key() {
					return label(next.get(pos));
				}
			};
		}
		

		public Node assign(final Node node) {
			
			this.fin.forEach(new TIntProcedure() {
				
				@Override
				public boolean execute(int value) {
					node.addFinal(value);
					return true;
				}
			});
			
			
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
	
	List<Node> nodes = new ArrayList<IntTrieBuilder.Node>();
	Stack<Node> free = new Stack<IntTrieBuilder.Node>();
		
	public IntTrieBuilder() {
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
			return free.pop().getNumber();
		
		Node n = new Node();
		n.number = nodes.size();
		nodes.add(n);
		
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
		free.push(n);
	}

	
	int finalValue;
	
	public void setFinalValue(int finalValue) {
		this.finalValue = finalValue;
	}

	@Override
	public boolean setFinal(int state) {
		return nodes.get(state).addFinal(finalValue);
	}

	@Override
	public boolean isFinal(int state) {
		return nodes.get(state).fin.contains(finalValue);
	}
	
	public void toDot(String fileName) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(fileName);
		
		pw.println("digraph finite_state_machine {");
		pw.println("rankdir=LR;");
		pw.println("node [shape=circle]");
		
		for(Node n : nodes) {
			for(int i = 0; i < n.outbound(); i++) {
				pw.printf("%d -> %d [label=\"%s\"];%n", n.number, n.dest(n.next.get(i)), "" + ((char) n.label(n.next.get(i))));
			}

			if(n.isFinal()) {
				pw.printf("%d [shape=doublecircle];%n", n.number);
			}

		}
		pw.println("}");


		
		
		pw.close();
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
	
	protected static class Register {
		HashMap<Node, Node> m = new HashMap<Node, Node>();
		int add;
		int remove;
		int get;

		public boolean contains(Node node) {
			return m.containsKey(node);
		}

		public Node get(Node node) {
			get++;
			return m.get(node);
		}

		public void add(Node node) {
			add++;
			m.put(node, node);
		}

		public void remove(Node node) {
			Node regNode = m.get(node);
			remove++;

			if(regNode == null)
				return;

			if(node == regNode)
				m.remove(node);
		}

	}
	
	

	
	public static void main(String... args) throws IOException {
		IntTrieBuilder fsa = new IntTrieBuilder();
		
		fsa.addMinWord(str("fox"));
		fsa.toDot("001.dot");
		//fsa.setFinalValue(1);
		fsa.addMinWord(str("box"));
		fsa.toDot("002.dot");
		
	}
	


}
