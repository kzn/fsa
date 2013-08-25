package name.kazennikov.dafsa.obsolete;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * FSA with int labels on state transitions
 * 
 * @author Anton Kazennikov
 *
 */
public interface IntFSA {

	/**
	 * Add a sequence with optional final type to the FSA
	 * 
	 * @param seq char sequence to add
	 * @param fin sequence final type
	 */
	public void add(TIntList seq, int fin);
	
	/**
	 * Add a a sequence with optional final type to the FSA using 
	 * incremental minimization algorithm
	 * 
	 * @param seq char sequence to add
	 * @param fin sequence final type
	 */
	public void addMinWord(TIntList seq, int fin);

	/**
	 * Number of states in the FSA
	 * 
	 * @return
	 */
	int size();
	
	/**
	 * Write FSA using Events interface
	 * 
	 * @param events
	 * @throws FSAException
	 */
	void write(Events events) throws FSAException;

	

	/**
	 * Node of a int FSA
	 * 
	 * @author Anton Kazennikov
	 *
	 */
	public interface Node {
		
		/**
		 * Get next state on given input
		 * 
		 * @param input input char
		 * @return next node or null, if there is no such transition
		 */
		public Node getNext(int input);
		
		/**
		 * Set transition (current, input) -> next
		 * 
		 * @param input input char
		 * @param next next state
		 */
		public void setNext(int input, Node next);
		
		/**
		 * Callback on transition removal
		 * 
		 * @param input input char
		 * @param next next state
		 */		
		public void removeInbound(int input, Node next);
		
		/**
		 * Callback on transition addition
		 * 
		 * @param input input char
		 * @param next next state
		 */
		public void addInbound(int input, Node next);

		/**
		 * Final features iterator
		 * 
		 * @return
		 */
		public TIntIterator getFinal();
		
		/**
		 * Get final features count
		 * 
		 * @return
		 */
		public int finalCount();

		/**
		 * Add final feature to node
		 * 
		 * @param fin final feature
		 * @return true, if feature was added to the finals collection
		 */
		public boolean addFinal(int f);

		/**
		 * Remove final feature from the node
		 * 
		 * @param fin final feature
		 * @return true, if feature was removed from the finals collection
		 */
		public boolean removeFinal(int f);

		/**
		 * Is this state final?
		 */
		public boolean isFinal();
		
		/**
		 * Get number of inbound transitions
		 * 
		 * @return
		 */
		public int outbound();
		
		/**
		 * Get number of outbound transitions
		 * 
		 * @return
		 */
		public int inbound();


		/**
		 * Make a fresh node
		 */
		public Node makeNode();
		
		/**
		 * Clone current node
		 */
		public Node cloneNode();
		
		/**
		 * Assign data from this node to given
		 * 
		 * @param dest destination node
		 * @return
		 */
		public Node assign(Node dest);

		/**
		 * Reset node - remove all transitions
		 */
		public void reset();

		/**
		 * Get transitions table
		 * 
		 * @return
		 */
		public TIntObjectIterator<Node> next();

		/**
		 * Checks node equivalence
		 * 
		 * @return
		 */
		public boolean equiv(Node node);

		/**
		 * Set node number
		 */
		public void setNumber(int num);
		
		/**
		 * Get node number
		 * 
		 * @return
		 */
		public int getNumber();

	}
	
	/**
	 * Event producer/consumer for for IntFSA
	 * 
	 * @author Anton Kazennikov
	 */
	public interface Events {
		
		/**
		 * Announce start of states
		 */
		public void startStates() throws FSAException;
		
		/**
		 * Announce end of states
		 */
		public void endStates() throws FSAException;
		
		/**
		 * Announce start of state
		 */
		public void startState() throws FSAException;
		
		/**
		 * Announce end of state
		 */
		public void endState() throws FSAException;
		
		/**
		 * Announce start of final features list
		 */
		public void startFinals() throws FSAException;
		
		/**
		 * Announce end of final features list
		 */
		public void endFinals() throws FSAException;
		
		/**
		 * Announce start of transition table
		 */
		public void startTransitions() throws FSAException;

		/**
		 * Announce end of transition table
		 */
		public void endTransitions() throws FSAException;

		/**
		 * Announce number of states in the trie
		 * 
		 * @param states
		 */
		public void states(int states) throws FSAException;

		/**
		 * Announce current state for the writer
		 * 
		 * @param state number of the current state
		 */
		public void state(int state) throws FSAException;

		/**
		 * Announce number of final features of the current state
		 * 
		 * @param n number of final features
		 */
		public void finals(int n) throws FSAException;

		/**
		 * Announce final feature of the current state
		 * 
		 * @param fin final feature
		 */
		public void stateFinal(int fin) throws FSAException;

		/**
		 * Announce number of transitions of the current state
		 * 
		 * @param n number of transitions
		 */
		public void transitions(int n) throws FSAException;

		/**
		 * Announce transition of the current state
		 * 
		 * @param input input label
		 * @param dest number of the destination state
		 */
		public void transition(int input, int dest) throws FSAException;
	}

	
	public static class Simple implements IntFSA {

		public static final int INVALID_STATE = -1;
		IntFSA.Node start;
		List<IntFSA.Node> nodes = new ArrayList<IntFSA.Node>();

		TIntStack free = new TIntArrayStack();

		Register reg = new Register();

		class Register {
			HashMap<IntFSA.Node, IntFSA.Node> m = new HashMap<IntFSA.Node, IntFSA.Node>();

			public boolean contains(IntFSA.Node node) {
				return m.containsKey(node);
			}

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
				IntFSA.Node regNode = m.get(node);

				if(regNode == null)
					return;

				if(node == regNode)
					m.remove(node);
			}

		}

		public Simple(IntFSA.Node start) {
			this.start = start;
			start.setNumber(nodes.size());
			nodes.add(start);

		}


		/**
		 * Add sequence seq with final fin to trie
		 * 
		 * @param seq sequence to add
		 * @param fin final state
		 */
		@Override
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
		 * Get next state to the given with input in, get existing state or add new
		 * 
		 * @param node base trie node
		 * @param in input
		 * @return next node
		 */
//		public IntFSA.Node getNextOrAdd(IntFSA.Node node, int in) {
//			IntFSA.Node next = node.getNext(in);
//
//			if(next != null)
//				return next;
//
//			next = nodes.get(makeNode());
//			setNext(node.getNumber(), in, next.getNumber());
//
//			return next;
//
//		}

		/**
		 * Get set of finals encountered while walking this trie with sequence seq
		 * 
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
			IntFSA.Node src = nodes.get(srcIndex);
			IntFSA.Node dest = nodes.get(destIndex);
			reg.remove(src.getNumber());
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
		protected int makeNode() {
			if(free.size() != 0)
				return free.pop();

			IntFSA.Node node = start.makeNode();//new Trie.SimpleNode<In, Final>();
			node.setNumber(nodes.size());
			nodes.add(node);

			return node.getNumber();
		}

		protected int cloneNode(int srcNode) {
			IntFSA.Node src = nodes.get(srcNode);
			int node = makeNode();
			src.assign(nodes.get(node));
			return node;
		}


		/**
		 * Return size of the trie as number of nodes
		 * 
		 * @return
		 */
		@Override
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

		@Override
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

		@Override
		public void write(final IntFSA.Events writer) throws FSAException {
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
		
	}
	
	public static class FileWriter implements IntFSA.Events { 
		DataOutputStream s;

		public FileWriter(DataOutputStream s) {
			this.s = s;
		}

		@Override
		public void states(int states) throws FSAException {
			try {
				s.writeInt(states);
			} catch (IOException e) {
				throw new FSAException(e);
			}
		}

		@Override
		public void state(int state) throws FSAException {
			try {
				s.writeInt(state);
			} catch (IOException e) {
				throw new FSAException(e);
			}
			
		}

		@Override
		public void finals(int n) throws FSAException {
			try {
				s.writeInt(n);
			} catch (IOException e) {
				throw new FSAException(e);
			}
		}

		@Override
		public void stateFinal(int fin) throws FSAException {
			try {
				s.writeInt(fin);
			} catch (IOException e) {
				throw new FSAException(e);
			}
		}

		@Override
		public void transitions(int n) throws FSAException {
			try {
				s.writeInt(n);
			} catch (IOException e) {
				throw new FSAException(e);
			}
			
		}

		@Override
		public void transition(int input, int dest) throws FSAException {
			try {
				s.writeInt(input);
				s.writeInt(dest);
			} catch (IOException e) {
				throw new FSAException(e);
			}
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
		}

		@Override
		public void startTransitions() {
		}

		@Override
		public void endTransitions() {
		}

		@Override
		public void startStates() {
		}

		@Override
		public void endStates() {
		}
	}

	

	public static class FSADotFormatter implements Events {
		PrintWriter pw;
		int currentState = 0;
		TIntArrayList finals = new TIntArrayList(10);
		
		public FSADotFormatter(PrintWriter pw) {
			this.pw = pw;
		}

		@Override
		public void states(int states) {
		}

		@Override
		public void state(int state) {
			currentState = state;
		}

		@Override
		public void finals(int n) {
			finals.clear();
		}

		@Override
		public void stateFinal(int fin) {
			finals.add(fin);
		}

		@Override
		public void transitions(int n) {
		}

		@Override
		public void transition(int input, int dest) {
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
				pw.printf("%d [shape=doublecircle, label=\"%d\"];%n", currentState, currentState);
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
		public void transition(int input, int dest) {
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




}
