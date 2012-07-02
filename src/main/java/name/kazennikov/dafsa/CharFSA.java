package name.kazennikov.dafsa;

import gnu.trove.iterator.TCharObjectIterator;
import gnu.trove.map.hash.TCharObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TCharObjectProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class CharFSA {

	public interface Node {
		public Node getNext(char input);
		public void setNext(char input, Node next);
		public void removeInbound(char input, Node next);
		public void addInbound(char input, Node next);

		public TIntSet getFinal();
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

		public TCharObjectHashMap<Node> next();

		public boolean equiv(Node node);

		public void setNumber(int num);
		public int getNumber();

	}
	
	public static class SimpleNode implements Node {
		TIntHashSet fin = new TIntHashSet();
		TCharObjectHashMap<Node> out = new TCharObjectHashMap<CharFSA.Node>();

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
		public Node getNext(char input) {
			return out.get(input);
		}
		@Override
		public void setNext(char input, Node next) {
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
		public TIntSet getFinal() {
			return fin;//Collections.unmodifiableSet(fin);
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
		public void removeInbound(char input, Node base) {
			inbound--;
			
		}

		@Override
		public void addInbound(char input, Node base) {
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
				TCharObjectIterator<Node> it = out.iterator();
				
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
				
				TCharObjectIterator<Node> 
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
			
			out.forEachEntry(new TCharObjectProcedure<Node>() {

				@Override
				public boolean execute(char key, Node value) {
					node.setNext(key, value);
					return true;
				}
			});
			
			return node;
		}

		@Override
		public void reset() {
			fin.clear();
			
			for(char ch : out.keys()) {
				setNext(ch, null);
			}
		}

		@Override
		public TCharObjectHashMap<Node> next() {
			return out;
		}
		
		@Override
		public boolean equiv(Node node) {
			if(!node.getFinal().equals(fin))
				return false;
			
			TCharObjectIterator<Node> it = out.iterator();
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
			
			this.out.forEachEntry(new TCharObjectProcedure<Node>() {

				@Override
				public boolean execute(char a, Node b) {
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
	 * Writer for trie
	 * @author ant
	 *
	 * @param <In> input label type
	 * @param <Final> final feature type
	 */
	public interface Writer {
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
		public void transition(char input, int dest) throws IOException;
	}


		CharFSA.Node start;
		List<CharFSA.Node> nodes = new ArrayList<CharFSA.Node>();

		Stack<CharFSA.Node> free = new Stack<CharFSA.Node>();

		Register reg = new Register();

		class Register {
			HashMap<CharFSA.Node, CharFSA.Node> m = new HashMap<CharFSA.Node, CharFSA.Node>();

			public boolean contains(CharFSA.Node node) {
				return m.containsKey(node);
			}

			public CharFSA.Node get(CharFSA.Node node) {
				return m.get(node);
			}

			public void add(CharFSA.Node node) {
				m.put(node, node);
			}

			public void remove(CharFSA.Node node) {
				CharFSA.Node regNode = m.get(node);

				if(regNode == null)
					return;

				if(node == regNode)
					m.remove(node);
			}

		}

		public CharFSA(CharFSA.Node start) {
			this.start = start;
			nodes.add(start);
			start.setNumber(nodes.size());
		}


		/**
		 * Add sequence seq with final fin to trie
		 * @param seq sequence to add
		 * @param fin final state
		 */
		public void add(CharSequence seq, int fin) {
			CharFSA.Node current = start;

			int idx = 0;

			while(idx < seq.length()) {
				CharFSA.Node n = current.getNext(seq.charAt(idx));
				if(n == null)
					break;

				idx++;
				current = n;
			}

			if(idx == seq.length()) {
				addFinal(current, fin);
			} else {
				addSuffix(current, seq, idx, seq.length(), fin);
			}
		}


		/**
		 * Get next state to the given with input in, get exisiting state or add new
		 * @param node base trie node
		 * @param in input
		 * @return next node
		 */
		public CharFSA.Node getNextOrAdd(CharFSA.Node node, char in) {
			CharFSA.Node next = node.getNext(in);

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
		public TIntSet get(CharSequence seq) {
			CharFSA.Node n = start;

			for(int i = 0; i != seq.length(); i++) {
				char in = seq.charAt(i);

				CharFSA.Node next = n.getNext(in);

				if(next == null)
					return null;

				n = next;
			}

			return n.getFinal();
		}

		/*public FC getAll(List<In> seq) {
		//Set<Final> finals = new HashSet<Final>();
		Trie.CharFSA.Node n = start;

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
		protected List<CharFSA.Node> addSuffix(CharFSA.Node n, CharSequence seq, int start, int end, int fin) {
			CharFSA.Node current = n;

			List<CharFSA.Node> nodes = new ArrayList<CharFSA.Node>();

			for(int i = start; i < end; i++) {

				char in = seq.charAt(i);
				CharFSA.Node node = makeNode();
				nodes.add(node);
				setNext(current, in, node);
				current = node;
			}

			addFinal(current, fin);

			return nodes;
		}

		public void addFinal(CharFSA.Node node, int fin) {
			//if(node.getFinal().contains(fin))
			//	return;

			reg.remove(node);
			node.addFinal(fin);
		}

		public void setNext(CharFSA.Node src, char in, CharFSA.Node dest) {
			reg.remove(src);
			src.setNext(in, dest);
		}

		/**
		 * Get start node
		 * @return
		 */
		public CharFSA.Node getStart() {
			return start;
		}


		/**
		 * Make new node
		 * @return
		 */
		protected CharFSA.Node makeNode() {
			if(!free.empty())
				return free.pop();

			CharFSA.Node node = start.makeNode();//new Trie.SimpleNode<In, Final>();
			nodes.add(node);
			node.setNumber(nodes.size());
			return node;
		}

		protected CharFSA.Node cloneNode(CharFSA.Node src) {
			CharFSA.Node node = makeNode();
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

		public boolean isConfluence(CharFSA.Node node) {
			return node.inbound() > 1;
		}

		List<CharFSA.Node> commonPrefix(CharSequence seq) {
			CharFSA.Node current = start;
			List<CharFSA.Node> prefix = new ArrayList<CharFSA.Node>();
			prefix.add(current);

			for(int i = 0; i != seq.length(); i++) {
				char in = seq.charAt(i);
				CharFSA.Node next = current.getNext(in);

				if(next == null)
					break;

				current = next;
				prefix.add(current);
			}

			return prefix;
		}

		int findConfluence(List<CharFSA.Node> nodes) {
			for(int i = 0; i != nodes.size(); i++)
				if(isConfluence(nodes.get(i)))
					return i;

			return 0;
		}

		public void addMinWord(CharSequence input, int fin) {
			/*
			 * 1. get common prefix
			 * 2. find first confluence state in the common prefix
			 * 3. if any, clone it and all states after it in common prefix
			 * 4. add suffix
			 * 5. minimize(replaceOrRegister from the last state toward the first)
			 */

			List<CharFSA.Node> prefix = commonPrefix(input);

			int confIdx = findConfluence(prefix);
			int stopIdx = confIdx == 0? prefix.size() : confIdx;

			if(confIdx > 0) {	
				int idx = confIdx;

				while(idx < prefix.size()) {
					CharFSA.Node prev = prefix.get(idx - 1);
					CharFSA.Node cloned = cloneNode(prefix.get(idx));
					prefix.set(idx, cloned);
					setNext(prev, input.charAt(confIdx - 1), cloned);
					idx++;
					confIdx++;
				}
			}



			List<CharFSA.Node> nodeList = new ArrayList<CharFSA.Node>(prefix);

			nodeList.addAll(addSuffix(prefix.get(prefix.size() - 1), input, prefix.size() - 1, input.length(), fin));

			replaceOrRegister(input, nodeList, stopIdx);



		}


		private void replaceOrRegister(CharSequence input, List<CharFSA.Node> nodeList, int stop) {
			if(nodeList.size() < 2)
				return;

			int idx = nodeList.size() - 1;
			int inIdx = input.length() - 1;

			while(idx > 0) {
				CharFSA.Node n = nodeList.get(idx);
				CharFSA.Node regNode = reg.get(n);

				//if(n.equiv(regNode))

				// stop
				if(regNode == n) {
					if(idx < stop)
						return;
				} else if(regNode == null) {
					reg.add(n);
				} else {
					char in = input.charAt(inIdx);
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

		public void toDot(String fileName) throws IOException {
			final PrintWriter pw = new PrintWriter(fileName);

			pw.println("digraph finite_state_machine {");
			pw.println("rankdir=LR;");
			pw.println("node [shape=circle]");

			for(CharFSA.Node n : nodes) {
				final int src = n.getNumber();

				if(n.isFinal()) {
					pw.printf("%d [shape=doublecircle, label=\"%d %s\"];%n", src, src, n.getFinal());
				}

				n.next().forEachEntry(new TCharObjectProcedure<CharFSA.Node>() {
					@Override
					public boolean execute(char input, CharFSA.Node next) {
						int dest = next.getNumber();
						pw.printf("%d -> %d [label=\"%s\"];%n", src, dest, input);

						return true;
					}
				});
			}

			pw.println("}");
			pw.close();
		}

		public CharFSA.Node getNode(int index) {
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

		public void write(final CharFSA.Writer writer) throws IOException {
			writer.states(nodes.size());

			for(CharFSA.Node node : nodes) {
				writer.state(node.getNumber());

				writer.finals(node.getFinal().size());
				for(int f : node.getFinal().toArray()) {
					writer.stateFinal(f);
				}

				writer.transitions(node.next().size());
				
				TCharObjectIterator<CharFSA.Node> it = node.next().iterator();
				
				while(it.hasNext()) {
					it.advance();
					int dest = it.value().getNumber();
					writer.transition(it.key(), dest);
				}
			}
		}
}
