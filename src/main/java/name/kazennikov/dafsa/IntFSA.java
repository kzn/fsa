package name.kazennikov.dafsa;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import name.kazennikov.dafsa.CharFSA.Events;

public interface IntFSA {

	public void add(TIntList seq, int fin);
	public void addMinWord(TIntList seq, int fin);
	int size();
	void write(Events events) throws IOException;

	

	public interface Node {
		public Node getNext(int input);
		public void setNext(int input, Node next);
		public void removeInbound(int input, Node next);
		public void addInbound(int input, Node next);

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

		public TIntObjectIterator<Node> next();

		public boolean equiv(Node node);

		public void setNumber(int num);
		public int getNumber();

	}
	
	/**
	 * Event producer for for IntFSA
	 * @author Anton Kazennikov
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
		public void transition(int input, int dest) throws IOException;
	}

	
	public static class Simple implements IntFSA {

		IntFSA.Node start;
		List<IntFSA.Node> nodes = new ArrayList<IntFSA.Node>();

		Stack<IntFSA.Node> free = new Stack<IntFSA.Node>();

		Register reg = new Register();

		class Register {
			HashMap<IntFSA.Node, IntFSA.Node> m = new HashMap<IntFSA.Node, IntFSA.Node>();

			public boolean contains(IntFSA.Node node) {
				return m.containsKey(node);
			}

			public IntFSA.Node get(IntFSA.Node node) {
				return m.get(node);
			}

			public void add(IntFSA.Node node) {
				m.put(node, node);
			}

			public void remove(IntFSA.Node node) {
				IntFSA.Node regNode = m.get(node);

				if(regNode == null)
					return;

				if(node == regNode)
					m.remove(node);
			}

		}

		public Simple(IntFSA.Node start) {
			this.start = start;
			nodes.add(start);
			start.setNumber(nodes.size());
		}


		/**
		 * Add sequence seq with final fin to trie
		 * @param seq sequence to add
		 * @param fin final state
		 */
		public void add(TIntList seq, int fin) {
			IntFSA.Node current = start;

			int idx = 0;

			while(idx < seq.size()) {
				IntFSA.Node n = current.getNext(seq.get(idx));
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
		public IntFSA.Node getNextOrAdd(IntFSA.Node node, int in) {
			IntFSA.Node next = node.getNext(in);

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
			IntFSA.Node n = start;

			for(int i = 0; i != seq.length(); i++) {
				int in = seq.charAt(i);

				IntFSA.Node next = n.getNext(in);

				if(next == null)
					return null;

				n = next;
			}

			return n.getFinal();
		}

		/*public FC getAll(List<In> seq) {
		//Set<Final> finals = new HashSet<Final>();
		Trie.IntFSA.Node n = start;

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
		protected List<IntFSA.Node> addSuffix(IntFSA.Node n, TIntList seq, int start, int end, int fin) {
			IntFSA.Node current = n;

			List<IntFSA.Node> nodes = new ArrayList<IntFSA.Node>();

			for(int i = start; i < end; i++) {

				int in = seq.get(i);
				IntFSA.Node node = makeNode();
				nodes.add(node);
				setNext(current, in, node);
				current = node;
			}

			addFinal(current, fin);

			return nodes;
		}

		public void addFinal(IntFSA.Node node, int fin) {
			//if(node.getFinal().contains(fin))
			//	return;

			reg.remove(node);
			node.addFinal(fin);
		}

		public void setNext(IntFSA.Node src, int in, IntFSA.Node dest) {
			reg.remove(src);
			src.setNext(in, dest);
		}

		/**
		 * Get start node
		 * @return
		 */
		public IntFSA.Node getStart() {
			return start;
		}


		/**
		 * Make new node
		 * @return
		 */
		protected IntFSA.Node makeNode() {
			if(!free.empty())
				return free.pop();

			IntFSA.Node node = start.makeNode();//new Trie.SimpleNode<In, Final>();
			nodes.add(node);
			node.setNumber(nodes.size());
			return node;
		}

		protected IntFSA.Node cloneNode(IntFSA.Node src) {
			IntFSA.Node node = makeNode();
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

		public boolean isConfluence(IntFSA.Node node) {
			return node.inbound() > 1;
		}

		List<IntFSA.Node> commonPrefix(TIntList seq) {
			IntFSA.Node current = start;
			List<IntFSA.Node> prefix = new ArrayList<IntFSA.Node>();
			prefix.add(current);

			for(int i = 0; i != seq.size(); i++) {
				int in = seq.get(i);
				IntFSA.Node next = current.getNext(in);

				if(next == null)
					break;

				current = next;
				prefix.add(current);
			}

			return prefix;
		}

		int findConfluence(List<IntFSA.Node> nodes) {
			for(int i = 0; i != nodes.size(); i++)
				if(isConfluence(nodes.get(i)))
					return i;

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

			List<IntFSA.Node> prefix = commonPrefix(input);

			int confIdx = findConfluence(prefix);
			int stopIdx = confIdx == 0? prefix.size() : confIdx;

			if(confIdx > 0) {	
				int idx = confIdx;

				while(idx < prefix.size()) {
					IntFSA.Node prev = prefix.get(idx - 1);
					IntFSA.Node cloned = cloneNode(prefix.get(idx));
					prefix.set(idx, cloned);
					setNext(prev, input.get(confIdx - 1), cloned);
					idx++;
					confIdx++;
				}
			}



			List<IntFSA.Node> nodeList = new ArrayList<IntFSA.Node>(prefix);

			nodeList.addAll(addSuffix(prefix.get(prefix.size() - 1), input, prefix.size() - 1, input.size(), fin));

			replaceOrRegister(input, nodeList, stopIdx);



		}


		private void replaceOrRegister(TIntList input, List<IntFSA.Node> nodeList, int stop) {
			if(nodeList.size() < 2)
				return;

			int idx = nodeList.size() - 1;
			int inIdx = input.size() - 1;

			while(idx > 0) {
				IntFSA.Node n = nodeList.get(idx);
				IntFSA.Node regNode = reg.get(n);

				//if(n.equiv(regNode))

				// stop
				if(regNode == n) {
					if(idx < stop)
						return;
				} else if(regNode == null) {
					reg.add(n);
				} else {
					int in = input.get(inIdx);
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

		public static List<Character> string2CharList(String s) {
			List<Character> list = new ArrayList<Character>();

			for(int i = 0; i != s.length(); i++)
				list.add(s.charAt(i));

			return list;
		}
		
		public static class FSADotFormatter implements Events {
			PrintWriter pw;
			int currentState = 0;
			TIntArrayList finals = new TIntArrayList(10);
			
			public FSADotFormatter(PrintWriter pw) {
				this.pw = pw;
			}

			@Override
			public void states(int states) throws IOException {
			}

			@Override
			public void state(int state) throws IOException {
				currentState = state;
			}

			@Override
			public void finals(int n) throws IOException {
				finals.clear();
			}

			@Override
			public void stateFinal(int fin) throws IOException {
				finals.add(fin);
			}

			@Override
			public void transitions(int n) throws IOException {
			}

			@Override
			public void transition(int input, int dest) throws IOException {
				pw.printf("%d -> %d [label=\"%s\"];%n", currentState, dest, input);
			}

			@Override
			public void startState() {
			}

			@Override
			public void endState() {
			}

			@Override
			public void startFinals() {
			}

			@Override
			public void endFinals() {
				if(!finals.isEmpty()) {
					pw.printf("%d [shape=doublecircle, label=\"%d %s\"];%n", currentState, currentState, finals);
				} else {
					pw.printf("%d [label=\"%d\"];%n", currentState, currentState);
				}
			}

			@Override
			public void startTransitions() {
			}

			@Override
			public void endTransitions() {
			}

			@Override
			public void startStates() {
				pw.println("digraph finite_state_machine {");
				pw.println("rankdir=LR;");
				pw.println("node [shape=circle]");
			}

			@Override
			public void endStates() {
				pw.printf("}");
			}
		}
		
		public static class FSTDotFormatter extends FSADotFormatter {

			public FSTDotFormatter(PrintWriter pw) {
				super(pw);
			}
			
			@Override
			public void transition(int input, int dest) throws IOException {
				char in = (char)( input >> 16);
				char out = (char) (input & 0xFFFF);
				
				if(in == 0) {
					in = '_';
				}
				
				if(out == 0) {
					out = '_';
				}
					
				pw.printf("%d -> %d [label=\"%s:%s\"];%n", currentState, dest, in, out);
			}

			
		}


		public IntFSA.Node getNode(int index) {
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

		public void write(final IntFSA.Events writer) throws IOException {
			writer.startStates();
			writer.states(nodes.size());

			for(IntFSA.Node node : nodes) {
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
				
				TIntObjectIterator<IntFSA.Node> it = node.next();
				
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
		
		public static class FileWriter implements IntFSA.Events { 
			DataOutputStream s;

			public FileWriter(DataOutputStream s) {
				this.s = s;
			}

			@Override
			public void states(int states) throws IOException {
				s.writeInt(states);
			}

			@Override
			public void state(int state) throws IOException {
				s.writeInt(state);
			}

			@Override
			public void finals(int n) throws IOException {
				s.writeInt(n);
			}

			@Override
			public void stateFinal(int fin) throws IOException {
				s.writeInt(fin);
			}

			@Override
			public void transitions(int n) throws IOException {
				s.writeInt(n);
				
			}

			@Override
			public void transition(int input, int dest) throws IOException {
				s.writeInt(input);
				s.writeInt(dest);
			}

			@Override
			public void startState() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void endState() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void startFinals() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void endFinals() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void startTransitions() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void endTransitions() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void startStates() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void endStates() {
				// TODO Auto-generated method stub
				
			}
		}
	}

}
