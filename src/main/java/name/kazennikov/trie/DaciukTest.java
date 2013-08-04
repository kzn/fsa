package name.kazennikov.trie;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.hash.TIntHashSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import name.kazennikov.dafsa.DaciukAlgo;

public class DaciukTest extends DaciukAlgo {
	Register r = new Register();
	
	@Override
	public void regAdd(int state) {
		r.add(nodes.get(state));
		
	}

	@Override
	public int regGet(int state) {
		Node n = r.get(nodes.get(state));
		if(n == null)
			return INVALID_STATE;

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
	public static class Node {
		TIntHashSet fin = new TIntHashSet();
		TIntArrayList outChars = new TIntArrayList();
		List<Node> outNodes = new ArrayList<Node>();

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
		
		int findIndex(int input) {
			for(int i = 0; i != outChars.size(); i++) {
				if(outChars.get(i) == input)
					return i;
			}

			return -1;
		}
		
		public Node getNext(int input) {
			for(int i = 0; i != outChars.size(); i++) {
				if(outChars.get(i) == input)
					return outNodes.get(i);
			}
			return null;
		}
		
		

		public void setNext(int input, Node next) {
			int index = findIndex(input);
			
			if(index != -1) {
				outNodes.get(index).removeInbound(input, this);
			}
			
			
			
			if(next != null) {
				if(index == -1) {
					outChars.add(input);
					outNodes.add(next);
				} else {
					outNodes.set(index, next);
				}
				next.addInbound(input, this);
			} else if(index != -1) {
				outChars.removeAt(index);
				outNodes.remove(index);
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
			return outChars.size();
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

			if(outChars == null) {
				result = prime * result;
			} else {
				
				for(int i = 0; i != outChars.size(); i++) {
					if(outChars.get(i) == 0)
						continue;
					
					result += outChars.get(i);
					result += System.identityHashCode(outNodes.get(i));
				}
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

				if(outChars.size() != other.outChars.size())
					return false;
				
				for(int i = 0; i != outbound(); i++) {
					int otherIndex = other.findIndex(outChars.get(i));
					if(otherIndex == -1)
						return false;
					
					if(outNodes.get(i) !=other.outNodes.get(otherIndex))
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
				node.outChars.add(outChars.get(i));
				node.outNodes.add(outNodes.get(i));
			}
			
			return node;
		}

		public void reset() {
			fin.clear();
			
			for(int i = 0; i != outbound(); i++) {
				int input = outChars.get(i);
				Node next = outNodes.get(i);
				
				next.removeInbound(input, this);
			}
			
			outChars.reset();
			outNodes.clear();
		}

		public TIntObjectIterator<Node> next() {
			return new TIntObjectIterator<Node>() {
				
				int pos = -1;
				
				@Override
				public void remove() {
				}
				
				@Override
				public boolean hasNext() {
					return pos < outChars.size();
				}
				
				@Override
				public void advance() {
					pos++;
				}
				
				@Override
				public Node value() {
					return outNodes.get(pos);
				}
				
				@Override
				public Node setValue(Node val) {
					return null;
				}
				
				@Override
				public int key() {
					return outChars.get(pos);
				}
			};
		}
		
		public boolean equiv(Node node) {
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
			
		}

		public Node assign(final Node node) {
			
			this.fin.forEach(new TIntProcedure() {
				
				@Override
				public boolean execute(int value) {
					node.addFinal(value);
					return true;
				}
			});
			
			
			for(int i = 0; i != outChars.size(); i++) {
				node.setNext(outChars.get(i), outNodes.get(i));
			}

			return node;
		}
		
		@Override
		public String toString() {
			return String.format("state=%d", number);
		}


	}
	
	List<Node> nodes = new ArrayList<DaciukTest.Node>();
	Stack<Node> free = new Stack<DaciukTest.Node>();
		
	public DaciukTest() {
		startState = addState();
	}

	@Override
	public int getNext(int state, int input) {
		Node n = nodes.get(state);
		n = n.getNext(input);
		
		return n == null? INVALID_STATE : n.getNumber();
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
		n.setNext(label, nodes.get(dest));
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
				pw.printf("%d -> %d [label=\"%s\"];%n", n.number, n.outNodes.get(i).getNumber(), "" + ((char) n.outChars.get(i)));
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
		DaciukTest fsa = new DaciukTest();
		
		fsa.addMinWord(str("fox"));
		fsa.toDot("001.dot");
		fsa.setFinalValue(1);
		fsa.addMinWord(str("fox"));
		fsa.toDot("002.dot");
		
	}
	
	public int regSize() {
		return r.m.size();
	}


}
