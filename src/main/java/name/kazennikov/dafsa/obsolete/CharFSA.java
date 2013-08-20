package name.kazennikov.dafsa.obsolete;

import gnu.trove.iterator.TCharObjectIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * FSA with char labels on state transitions
 * @author Anton Kazennikov
 *
 */
public interface CharFSA {
	
	/**
	 * Add a sequence with optional final type to the FSA
	 * 
	 * @param seq char sequence to add
	 * @param fin sequence final type
	 */
	public void add(CharSequence seq, int fin);
	
	/**
	 * Add a a sequence with optional final type to the FSA using 
	 * incremental minimization algorithm
	 * 
	 * @param seq char sequence to add
	 * @param fin sequence final type
	 */
	public void addMinWord(CharSequence seq, int fin);

	/**
	 * Number of states in the FSA
	 * 
	 * @return
	 */
	int size();

	/**
	 * Write FSA using Events interface
	 * 
	 * @param events event listener object
	 * @throws FSAException
	 */
	void write(Events events) throws FSAException;


	/**
	 * Node of a FSA with char labels
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
		public Node getNext(char input);
		
		/**
		 * Set transition (current, input) -> next
		 * 
		 * @param input input char
		 * @param next next state
		 */
		public void setNext(char input, Node next);
		
		/**
		 * Callback on transition removal
		 * 
		 * @param input input char
		 * @param next next state
		 */
		public void removeInbound(char input, Node next);
		
		/**
		 * Callback on transition addition
		 * 
		 * @param input input char
		 * @param next next state
		 */
		public void addInbound(char input, Node next);

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
		public TCharObjectIterator<Node> next();

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
	 * Event producer/consumer for FSA
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
		public void transition(char input, int dest) throws FSAException;
	}


	public static class Simple implements CharFSA {
		CharFSA.Node start;
		List<CharFSA.Node> nodes = new ArrayList<CharFSA.Node>();

		Stack<CharFSA.Node> free = new Stack<CharFSA.Node>();

		Register reg = new Register();

		protected static class Register {
			HashMap<CharFSA.Node, CharFSA.Node> m = new HashMap<CharFSA.Node, CharFSA.Node>();
			int add;
			int remove;
			int get;

			public boolean contains(CharFSA.Node node) {
				return m.containsKey(node);
			}

			public CharFSA.Node get(CharFSA.Node node) {
				get++;
				return m.get(node);
			}

			public void add(CharFSA.Node node) {
				add++;
				m.put(node, node);
			}

			public void remove(CharFSA.Node node) {
				CharFSA.Node regNode = m.get(node);
				remove++;

				if(regNode == null)
					return;

				if(node == regNode)
					m.remove(node);
			}

		}

		public Simple(CharFSA.Node start) {
			this.start = start;
			nodes.add(start);
			start.setNumber(nodes.size());
		}


		/**
		 * Add sequence seq with final fin to trie
		 * 
		 * @param seq sequence to add
		 * @param fin final state
		 */
		@Override
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
		 * Get next state to the given with input in, get existing state or add new
		 * 
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
		 * 
		 * @param seq input sequence
		 * @return set of encountered finals
		 */
		public TIntIterator get(CharSequence seq) {
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

		/**
		 * Add suffix to given new state
		 * 
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
			reg.remove(node);
			node.addFinal(fin);
		}

		public void setNext(CharFSA.Node src, char in, CharFSA.Node dest) {
			reg.remove(src);
			src.setNext(in, dest);
		}

		/**
		 * Get start node
		 * 
		 * @return
		 */
		public CharFSA.Node getStart() {
			return start;
		}


		/**
		 * Make new node
		 * 
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
		 * 
		 * @return
		 */
		@Override
		public int size() {
			return nodes.size() - free.size();
		}

		/**
		 * Check is node is confluenced
		 * 
		 * @param node node to check
		 * @return
		 */
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

		/**
		 * Find index of the first confluenced node. If no such node is found 0 is return.
		 * It is valid because 0 is the start node and by definition isn't confluenced.
		 * 
		 * @param nodes node list
		 * @return index of first confluenced node, or 0, if there are no such node 
		 */
		int findConfluence(List<CharFSA.Node> nodes) {
			for(int i = 0; i != nodes.size(); i++)
				if(isConfluence(nodes.get(i)))
					return i;

			return 0;
		}

		@Override
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

				// stop criterion
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
				}
				inIdx--;
				idx--;
			}
		}
		

		public CharFSA.Node getNode(int index) {
			return nodes.get(index);
		}

		@Override
		public void write(final CharFSA.Events writer) throws FSAException {
			writer.startStates();
			writer.states(nodes.size());

			for(CharFSA.Node node : nodes) {
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
				
				TCharObjectIterator<CharFSA.Node> it = node.next();
				
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
	
	/**
	 * Event listener for writing CharFSA to a {@link DataOutputStream}
	 * 
	 * @author Anton Kazennikov
	 *
	 */
	public static class FileWriter implements CharFSA.Events { 
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
		public void transition(char input, int dest) throws FSAException {
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

	/**
	 * Event write of CharFSA to .dot (graphviz) file format
	 * 
	 * @author Anton Kazennikov
	 *
	 */
	public static class DotFormatter implements Events {
		PrintWriter pw;
		int currentState = 0;
		TIntArrayList finals = new TIntArrayList(10);
		
		public DotFormatter(PrintWriter pw) {
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
		public void transition(char input, int dest) {
			pw.printf("%d -> %d [label=\"%s\"];%n", currentState, dest, input);
		}

		@Override
		public void startState() {
			pw.println("digraph finite_state_machine {");
			pw.println("rankdir=LR;");
			pw.println("node [shape=circle]");
		}

		@Override
		public void endState() {
			pw.println("}");
		}

		@Override
		public void startFinals() {
		}

		@Override
		public void endFinals() {
			pw.printf("%d [shape=doublecircle, label=\"%d %s\"];%n", currentState, currentState, finals);
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



}
