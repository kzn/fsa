package name.kazennikov.dafsa;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.TLongList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TLongObjectProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;


public class LongFSA {

	public interface Node {
		public Node getNext(long input);
		public void setNext(long input, Node next);
		public void removeInbound(long input, Node next);
		public void addInbound(long input, Node next);

		public TIntIterator getFinal();
		public int finalCount();
		/**
		 * Add final feature to node
		 * @param fin final feature
		 * @return true, if feature was added to the finals collection
		 */
		public boolean addFinal(int f);

		/**
		 * Remove final feature from the node
		 * @param fin final feature
		 * @return true, if feature was removed from the finals collection
		 */
		public boolean removeFinal(int f);

		public boolean isFinal();
		public int outbound();
		public int inbound();



		public Node makeNode();
		public Node cloneNode();
		public Node assign(Node dest);

		public void reset();

		public TLongObjectIterator<Node> next();

		public boolean equiv(Node node);

		public void setNumber(int num);
		public int getNumber();

	}
	
	public static class SimpleNode implements Node {
		TIntHashSet fin = new TIntHashSet();
		TLongObjectHashMap<Node> out = new TLongObjectHashMap<LongFSA.Node>();

		int inbound;
		int number;
		int hashCode;
		boolean validHashCode = true;
		
		public SimpleNode() {
			inbound = 0;
			hashCode = 1;
		}
		
		public void setNumber(int num) {
			this.number = num;
		}
		
		public int getNumber() {
			return number;
		}
		
		@Override
		public Node getNext(long input) {
			return out.get(input);
		}
		@Override
		public void setNext(long input, Node next) {
			if(out.containsKey(input)) {
				out.get(input).removeInbound(input, this);
			}
			
			if(next != null) {
				out.put(input, next);
				next.addInbound(input, this);
			} else {
				out.remove(input);
			}
			
			validHashCode = false;
		}
		
		@Override
		public TIntIterator getFinal() {
			return fin.iterator();//Collections.unmodifiableSet(fin);
		}
		
		@Override
		public int finalCount() {
			return fin.size();
		}
		
		
		
		
		@Override
		public boolean isFinal() {
			return fin != null && fin.size() > 0;
		}
		@Override
		public int outbound() {
			return out.size();
		}
		@Override
		public int inbound() {
			return inbound;
		}

		@Override
		public void removeInbound(long input, Node base) {
			inbound--;
			
		}

		@Override
		public void addInbound(long input, Node base) {
			inbound++;
		}

		@Override
		public boolean addFinal(int fin) {
			validHashCode = !this.fin.add(fin);
			return !validHashCode;
		}

		@Override
		public boolean removeFinal(int fin) {
			validHashCode = !this.fin.remove(fin);
			return !validHashCode;
		}
		
		int hc() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fin == null)? 0 : fin.hashCode());

			if(out == null) {
				result = prime * result;
			} else {
				//result = prime * result + out.size();
				TLongObjectIterator<Node> it = out.iterator();
				
				while(it.hasNext()) {
					it.advance();
					result += it.key();
					result += System.identityHashCode(it.value());

					
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
			if(!(obj instanceof SimpleNode))
				return false;
			

			SimpleNode other = (SimpleNode) obj;
			if(fin == null) {
				if(other.fin != null)
					return false;
			} else if(!fin.equals(other.fin))
				return false;
			if(out == null) {
				if(other.out != null)
					return false;
			} else {

				if(out.size() != other.out.size())
					return false;
				
				TLongObjectIterator<Node> 
					it1 = out.iterator();
				
				while(it1.hasNext()) {
					it1.advance();
					if(other.getNext(it1.key()) != it1.value())
						return false;
				}
			}
			
			return true;
		}

		@Override
		public SimpleNode makeNode() {
			return new SimpleNode();
		}
		
		/*@Override
		public SimpleNode<In, FC, Final> makeNode() {
			return new SimpleNode<In, Final>();
		}*/

		@Override
		public SimpleNode cloneNode() {
			final SimpleNode node = makeNode();
			
			node.fin.addAll(this.fin);
			
			out.forEachEntry(new TLongObjectProcedure<Node>() {

				@Override
				public boolean execute(long key, Node value) {
					node.setNext(key, value);
					return true;
				}
			});
			
			return node;
		}

		@Override
		public void reset() {
			fin.clear();
			
			for(long ch : out.keys()) {
				setNext(ch, null);
			}
		}

		@Override
		public TLongObjectIterator<Node> next() {
			return out.iterator();
		}
		
		@Override
		public boolean equiv(Node node) {
			if(!node.getFinal().equals(fin))
				return false;
			
			TLongObjectIterator<Node> it = out.iterator();
			while(it.hasNext()) {
				it.advance();
				
				Node n = node.getNext(it.key());
				if(n == null)
					return false;
				
				if(!it.value().equiv(n))
					return false;
			}
			
			return true;
			
		}

		@Override
		public Node assign(final Node node) {
			this.fin.forEach(new TIntProcedure() {
				
				@Override
				public boolean execute(int value) {
					node.addFinal(value);
					return true;
				}
			});
			
			this.out.forEachEntry(new TLongObjectProcedure<Node>() {

				@Override
				public boolean execute(long a, Node b) {
					node.setNext(a, b);
					return true;
				}
			});

			return node;
		}
		
		@Override
		public String toString() {
			return String.format("state=%d", number);
		}
	}

	/**
	 * Events producer for LongFSA
	 * @author ant
	 *
	 */
	public interface Events {
		
		public void startStates();
		public void endStates();
		
		public void startState();
		public void endState();
		
		public void startFinals();
		public void endFinals();
		
		public void startTransitions();
		public void endTransitions();
		
		/**
		 * Announce number of states in the trie
		 * @param states
		 */
		public void states(int states) throws IOException;

		/**
		 * Announce current state for the writer
		 * @param state number of the current state
		 */
		public void state(int state) throws IOException;

		/**
		 * Announce number of final features of the current state
		 * @param n number of final features
		 */
		public void finals(int n) throws IOException;

		/**
		 * Announce final feature of the current state
		 * @param fin  final feature
		 */
		public void stateFinal(int fin) throws IOException;

		/**
		 * Announce number of transitions of the current state
		 * @param n number of transitions
		 */
		public void transitions(int n) throws IOException;

		/**
		 * Announce transition of the current state
		 * @param input input label
		 * @param dest number of the destination state
		 */
		public void transition(long input, int dest) throws IOException;
	}


		LongFSA.Node start;
		List<LongFSA.Node> nodes = new ArrayList<LongFSA.Node>();

		Stack<LongFSA.Node> free = new Stack<LongFSA.Node>();

		Register reg = new Register();

		class Register {
			HashMap<LongFSA.Node, LongFSA.Node> m = new HashMap<LongFSA.Node, LongFSA.Node>();

			public boolean contains(LongFSA.Node node) {
				return m.containsKey(node);
			}

			public LongFSA.Node get(LongFSA.Node node) {
				return m.get(node);
			}

			public void add(LongFSA.Node node) {
				m.put(node, node);
			}

			public void remove(LongFSA.Node node) {
				LongFSA.Node regNode = m.get(node);

				if(regNode == null)
					return;

				if(node == regNode)
					m.remove(node);
			}

		}

		public LongFSA(LongFSA.Node start) {
			this.start = start;
			nodes.add(start);
			start.setNumber(nodes.size());
		}


		/**
		 * Add sequence seq with final fin to trie
		 * @param seq sequence to add
		 * @param fin final state
		 */
		public void add(TLongList seq, int fin) {
			LongFSA.Node current = start;

			int idx = 0;

			while(idx < seq.size()) {
				LongFSA.Node n = current.getNext(seq.get(idx));
				if(n == null)
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
		 * Get next state to the given with input in, get exisiting state or add new
		 * @param node base trie node
		 * @param in input
		 * @return next node
		 */
		public LongFSA.Node getNextOrAdd(LongFSA.Node node, long in) {
			LongFSA.Node next = node.getNext(in);

			if(next != null)
				return next;

			next = makeNode();
			setNext(node, in, next);

			return next;

		}

		/**
		 * Get set of finals encountered while walking this trie with sequence seq
		 * @param seq input sequence
		 * @return set of encountered finals
		 */
		public TIntIterator get(CharSequence seq) {
			LongFSA.Node n = start;

			for(int i = 0; i != seq.length(); i++) {
				long in = seq.charAt(i);

				LongFSA.Node next = n.getNext(in);

				if(next == null)
					return null;

				n = next;
			}

			return n.getFinal();
		}

		/*public FC getAll(List<In> seq) {
		//Set<Final> finals = new HashSet<Final>();
		Trie.LongFSA.Node n = start;

		for(In in : seq) {
			finals.addAll(n.getFinal());
			Trie.Node<In, Final> next = n.getNext(in);

			if(next == null)
				break;



			n = next;
		}

		return finals;
	}*/



		/**
		 * Add suffix to given new state
		 * @param n base node
		 * @param seq sequence to add
		 * @param fin final state
		 */
		protected List<LongFSA.Node> addSuffix(LongFSA.Node n, TLongList seq, int start, int end, int fin) {
			LongFSA.Node current = n;

			List<LongFSA.Node> nodes = new ArrayList<LongFSA.Node>();

			for(int i = start; i < end; i++) {

				long in = seq.get(i);
				LongFSA.Node node = makeNode();
				nodes.add(node);
				setNext(current, in, node);
				current = node;
			}

			addFinal(current, fin);

			return nodes;
		}

		public void addFinal(LongFSA.Node node, int fin) {
			//if(node.getFinal().contains(fin))
			//	return;

			reg.remove(node);
			node.addFinal(fin);
		}

		public void setNext(LongFSA.Node src, long in, LongFSA.Node dest) {
			reg.remove(src);
			src.setNext(in, dest);
		}

		/**
		 * Get start node
		 * @return
		 */
		public LongFSA.Node getStart() {
			return start;
		}


		/**
		 * Make new node
		 * @return
		 */
		protected LongFSA.Node makeNode() {
			if(!free.empty())
				return free.pop();

			LongFSA.Node node = start.makeNode();//new Trie.SimpleNode<In, Final>();
			nodes.add(node);
			node.setNumber(nodes.size());
			return node;
		}

		protected LongFSA.Node cloneNode(LongFSA.Node src) {
			LongFSA.Node node = makeNode();
			src.assign(node);
			return node;
		}


		/**
		 * Return size of the trie as number of nodes
		 * @return
		 */
		public int size() {
			return nodes.size() - free.size();
		}

		public boolean isConfluence(LongFSA.Node node) {
			return node.inbound() > 1;
		}

		List<LongFSA.Node> commonPrefix(TLongList seq) {
			LongFSA.Node current = start;
			List<LongFSA.Node> prefix = new ArrayList<LongFSA.Node>();
			prefix.add(current);

			for(int i = 0; i != seq.size(); i++) {
				long in = seq.get(i);
				LongFSA.Node next = current.getNext(in);

				if(next == null)
					break;

				current = next;
				prefix.add(current);
			}

			return prefix;
		}

		int findConfluence(List<LongFSA.Node> nodes) {
			for(int i = 0; i != nodes.size(); i++)
				if(isConfluence(nodes.get(i)))
					return i;

			return 0;
		}

		public void addMinWord(TLongList input, int fin) {
			/*
			 * 1. get common prefix
			 * 2. find first confluence state in the common prefix
			 * 3. if any, clone it and all states after it in common prefix
			 * 4. add suffix
			 * 5. minimize(replaceOrRegister from the last state toward the first)
			 */

			List<LongFSA.Node> prefix = commonPrefix(input);

			int confIdx = findConfluence(prefix);
			int stopIdx = confIdx == 0? prefix.size() : confIdx;

			if(confIdx > 0) {	
				int idx = confIdx;

				while(idx < prefix.size()) {
					LongFSA.Node prev = prefix.get(idx - 1);
					LongFSA.Node cloned = cloneNode(prefix.get(idx));
					prefix.set(idx, cloned);
					setNext(prev, input.get(confIdx - 1), cloned);
					idx++;
					confIdx++;
				}
			}



			List<LongFSA.Node> nodeList = new ArrayList<LongFSA.Node>(prefix);

			nodeList.addAll(addSuffix(prefix.get(prefix.size() - 1), input, prefix.size() - 1, input.size(), fin));

			replaceOrRegister(input, nodeList, stopIdx);



		}


		private void replaceOrRegister(TLongList input, List<LongFSA.Node> nodeList, int stop) {
			if(nodeList.size() < 2)
				return;

			int idx = nodeList.size() - 1;
			int inIdx = input.size() - 1;

			while(idx > 0) {
				LongFSA.Node n = nodeList.get(idx);
				LongFSA.Node regNode = reg.get(n);

				//if(n.equiv(regNode))

				// stop
				if(regNode == n) {
					if(idx < stop)
						return;
				} else if(regNode == null) {
					reg.add(n);
				} else {
					long in = input.get(inIdx);
					setNext(nodeList.get(idx - 1), in, regNode);
					nodeList.set(idx, regNode);
					n.reset();
					free.push(n);

					//nodes.remove(n);
				}
				inIdx--;
				idx--;
			}

		}

		public LongFSA.Node getNode(int index) {
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

		public void write(final LongFSA.Events writer) throws IOException {
			writer.startStates();
			writer.states(nodes.size());

			for(LongFSA.Node node : nodes) {
				writer.startState();
				writer.state(node.getNumber());

				writer.startFinals();

				writer.finals(node.finalCount());
				TIntIterator fit = node.getFinal();
				
				while(fit.hasNext()) {
					writer.stateFinal(fit.next());
				}
				writer.endFinals();

				writer.startTransitions();
				writer.transitions(node.outbound());
				
				TLongObjectIterator<LongFSA.Node> it = node.next();
				
				while(it.hasNext()) {
					it.advance();
					int dest = it.value().getNumber();
					writer.transition(it.key(), dest);
				}
				
				writer.endTransitions();
				writer.endState();
			}
			
			writer.endStates();
		}
}
