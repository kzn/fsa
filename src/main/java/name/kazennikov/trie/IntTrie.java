package name.kazennikov.trie;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;






public class IntTrie {
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
	
	
	public static final int INVALID_STATE = -1;
	Node start;
	List<Node> nodes = new ArrayList<Node>();

	TIntStack free = new TIntArrayStack();

	Register reg = new Register();

	class Register {
		
		HashMap<Node, Node> m = new HashMap<Node, Node>();

		public int get(int node) {
			Node n = m.get(nodes.get(node));
			return n == null? INVALID_STATE : n.getNumber();
		}

		public void add(int n) {
			Node node = nodes.get(n);
			m.put(node, node);
		}

		public void remove(int n) {
			Node node = nodes.get(n);
			Node regNode = m.get(node);

			if(regNode == null)
				return;

			if(node == regNode)
				m.remove(node);
		}

	}

	public IntTrie() {
		this.start = new Node();
		nodes.add(start);
		start.setNumber(nodes.size());
	}


	/**
	 * Add sequence seq with final fin to trie
	 * 
	 * @param seq sequence to add
	 * @param fin final state
	 */
	public void add(TIntList seq, int fin) {
		int current = start.getNumber();

		int idx = 0;

		while(idx < seq.size()) {
			int n = getNext(current, seq.get(idx));
			if(n == INVALID_STATE)
				break;

			idx++;
			current = n;
		}

		if(idx == seq.size()) {
			addFinal(current, fin);
		} else {
			addSuffix(current, seq, idx, seq.size(), fin);
		}
	}



	/**
	 * Get set of finals encountered while walking this trie with sequence seq
	 * 
	 * @param seq input sequence
	 * @return set of encountered finals
	 */
	public TIntIterator get(CharSequence seq) {
		Node n = start;

		for(int i = 0; i != seq.length(); i++) {
			int in = seq.charAt(i);

			Node next = n.getNext(in);

			if(next == null)
				return null;

			n = next;
		}

		return n.getFinal();
	}

	/**
	 * Add suffix to given new state
	 * 
	 * @param n base node
	 * @param seq sequence to add
	 * @param fin final state
	 */
	protected TIntList addSuffix(int n, TIntList seq, int start, int end, int fin) {
		int current = n;

		TIntList nodes = new TIntArrayList();

		for(int i = start; i < end; i++) {

			int in = seq.get(i);
			int node = makeNode();
			nodes.add(node);
			setNext(current, in, node);
			current = node;
		}

		addFinal(current, fin);

		return nodes;
	}

	public void addFinal(int node, int fin) {
		reg.remove(node);
		nodes.get(node).addFinal(fin);
	}

	public void setNext(int srcIndex, int in, int destIndex) {
		Node src = nodes.get(srcIndex);
		Node dest = nodes.get(destIndex);
		reg.remove(src.getNumber());
		src.setNext(in, dest);
	}

	/**
	 * Get start node
	 * @return
	 */
	public Node getStart() {
		return start;
	}


	/**
	 * Make new node
	 * @return
	 */
	protected int makeNode() {
		if(free.size() != 0)
			return free.pop();

		Node node = start.makeNode();//new Trie.SimpleNode<In, Final>();
		nodes.add(node);
		node.setNumber(nodes.size());
		return node.getNumber();
	}

	protected int cloneNode(int srcNode) {
		Node src = nodes.get(srcNode);
		int node = makeNode();
		src.assign(nodes.get(node));
		return node;
	}


	/**
	 * Return size of the trie as number of nodes
	 * 
	 * @return
	 */
	public int size() {
		return nodes.size() - free.size();
	}

	public boolean isConfluence(int node) {
		return nodes.get(node).inbound() > 1;
	}
	
	public int getNext(int state, int input) {
		Node n = nodes.get(state);
		Node next = n.getNext(input);
		return next == null? INVALID_STATE : next.getNumber();
	}

	TIntList commonPrefix(TIntList seq) {
		int current = start.getNumber();
		TIntArrayList prefix = new TIntArrayList();
		prefix.add(current);

		for(int i = 0; i != seq.size(); i++) {
			int in = seq.get(i);
			int next = getNext(current, in);

			if(next == INVALID_STATE)
				break;

			current = next;
			prefix.add(current);
		}

		return prefix;
	}

	int findConfluence(TIntList nodes) {
		for(int i = 0; i != nodes.size(); i++) {
			if(isConfluence(nodes.get(i)))
				return i;
		}

		return 0;
	}

	public void addMinWord(TIntList input, int fin) {
		/*
		 * 1. get common prefix
		 * 2. find first confluence state in the common prefix
		 * 3. if any, clone it and all states after it in common prefix
		 * 4. add suffix
		 * 5. minimize(replaceOrRegister from the last state toward the first)
		 */

		TIntList prefix = commonPrefix(input);

		int confIdx = findConfluence(prefix);
		int stopIdx = confIdx == 0? prefix.size() : confIdx;

		if(confIdx > 0) {	
			int idx = confIdx;

			while(idx < prefix.size()) {
				int prev = prefix.get(idx - 1);
				int cloned = cloneNode(prefix.get(idx));
				prefix.set(idx, cloned);
				setNext(prev, input.get(confIdx - 1), cloned);
				idx++;
				confIdx++;
			}
		}



		TIntList nodeList = new TIntArrayList(prefix);

		nodeList.addAll(addSuffix(prefix.get(prefix.size() - 1), input, prefix.size() - 1, input.size(), fin));

		replaceOrRegister(input, nodeList, stopIdx);



	}


	private void replaceOrRegister(TIntList input, TIntList nodeList, int stop) {
		if(nodeList.size() < 2)
			return;

		int idx = nodeList.size() - 1;
		int inIdx = input.size() - 1;

		while(idx > 0) {
			int n = nodeList.get(idx);
			int regNode = reg.get(n);

			// stop
			if(regNode == n) {
				if(idx < stop)
					return;
			} else if(regNode == INVALID_STATE) {
				reg.add(n);
			} else {
				int in = input.get(inIdx);
				setNext(nodeList.get(idx - 1), in, regNode);
				nodeList.set(idx, regNode);
				reset(n);
				free.push(n);
			}
			inIdx--;
			idx--;
		}

	}
	
	public void reset(int n) {
		nodes.get(n).reset();
	}

	
	public Node getNode(int index) {
		return nodes.get(index);
	}

	public static <T> int getId(TObjectIntHashMap<T> map, T object) {
		int id = map.get(object);

		if(id == 0) {
			id = map.size() + 1;
			map.put(object, id);
		}

		return id;
	}


}
