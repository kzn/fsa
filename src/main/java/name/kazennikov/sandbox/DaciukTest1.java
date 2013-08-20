package name.kazennikov.sandbox;

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
import java.util.List;
import java.util.Stack;

import name.kazennikov.dafsa.GenericRegister;
import name.kazennikov.dafsa.IntDaciukAlgoObject;

public class DaciukTest1 extends IntDaciukAlgoObject<DaciukTest1.Node> {
	
	GenericRegister<Node> r = new GenericRegister<Node>();
	
	@Override
	public void regAdd(Node state) {
		r.add(state);
		
	}

	@Override
	public Node regGet(Node state) {
		Node n = r.get(state);

		return n;
	}

	@Override
	public void regRemove(Node state) {
		r.remove(state);
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
		
		public Node getNext(int input) {
			
			for(int i = 0; i != next.size(); i++) {
				if(label(next.get(i)) == input)
					return nodes.get(dest(next.get(i)));
			}
			return null;
		}
		
		

		public void setNext(int input, Node next) {
			int index = findIndex(input);
			
			if(index != -1) {
				Node n = nodes.get(dest(this.next.get(index)));
				n.removeInbound(input, this);
			}
			
			
			
			if(next != null) {
				if(index == -1) {
					this.next.add(encode(input, next.getNumber()));
				} else {
					this.next.set(index, encode(input, next.getNumber()));
				}
				Node n = next;
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

		public Node makeNode() {
			return new Node();
		}
		

		public Node cloneNode() {
			final Node node = makeNode();
			
			node.fin.addAll(this.fin);
			
			for(int i = 0; i != outbound(); i++) {
				node.next.add(next.get(i));
			}
			
			return node;
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
		
/*		public boolean equiv(Node node) {
			if(!node.getFinal().equals(fin))
				return false;
			
			
			if(outbound() != node.outbound())
				return false;
			
			for(int i = 0; i != outbound(); i++) {
				int input = outChars.get(i);
				Node next = outNodes.get(i);
				Node otherNext = node.getNext(input);
				if(otherNext == null)
					return false;
				
				if(!next.equiv(otherNext))
					return false;
				
			}
			
			return true;
			
		}*/

		public Node assign(final Node node) {
			
			this.fin.forEach(new TIntProcedure() {
				
				@Override
				public boolean execute(int value) {
					node.addFinal(value);
					return true;
				}
			});
			
			
			for(int i = 0; i != next.size(); i++) {
				node.setNext(label(next.get(i)), nodes.get(dest(next.get(i))));
			}

			return node;
		}
		
		@Override
		public String toString() {
			return String.format("state=%d", number);
		}


	}
	
	List<Node> nodes = new ArrayList<Node>();
	Stack<Node> free = new Stack<Node>();
		
	public DaciukTest1() {
		startState = addState();
	}

	@Override
	public Node getNext(Node n, int input) {
		Node next = n.getNext(input);		
		return next;
	}

	@Override
	public boolean isConfluence(Node state) {
		return state.inbound() > 1;
	}

	@Override
	public Node cloneState(Node state) {
		Node src = state;
		Node node = addState();
		src.assign(node);
		node.hashCode = src.hashCode;
		node.validHashCode = src.validHashCode;
		
		return node;
	}

	@Override
	public Node addState() {
		if(!free.isEmpty())
			return free.pop();
		
		Node n = new Node();
		n.number = nodes.size();
		nodes.add(n);
		
		return n;
	}

	@Override
	public boolean setNext(Node src, int label, Node dest) {
		Node n = src;
		n.setNext(label, dest);
		return false;
	}

	@Override
	public void removeState(Node state) {
		Node n = state;
		n.reset();
		free.push(n);
	}

	
	int finalValue;
	
	public void setFinalValue(int finalValue) {
		this.finalValue = finalValue;
	}

	@Override
	public boolean setFinal(Node state) {
		return state.addFinal(finalValue);
	}

	@Override
	public boolean isFinal(Node state) {
		return state.fin.contains(finalValue);
	}
	
	public void toDot(String fileName) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(fileName);
		
		pw.println("digraph finite_state_machine {");
		pw.println("rankdir=LR;");
		pw.println("node [shape=circle,style=filled, fillcolor=white]");
		
		for(Node n : nodes) {
			if(n == startState) {
				pw.printf("%d [fillColor=\"gray\"];%n", n.getNumber());
			}
			
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
	
	

	
	public static void main(String... args) throws IOException {
		DaciukTest1 fsa = new DaciukTest1();
		
		fsa.addMinWord(str("fox"));
		fsa.toDot("001.dot");
		fsa.addMinWord(str("box"));
		fsa.toDot("002.dot");
		
	}
}
