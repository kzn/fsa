package name.kazennikov.dafsa.obsolete;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.TLongList;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;


/**
 * FSA with long labels on state transitions
 * @author Anton Kazennikov
 *
 */
public interface LongFSA {
	
	/**
	 * Add a sequence with optional final type to the FSA
	 * @param seq long sequence to add
	 * @param fin sequence final type
	 */
	public void add(TLongList seq, int fin);
	
	/**
	 * Add a a sequence with optional final type to the FSA using 
	 * incremental minimization algorithm
	 * @param seq long sequence to add
	 * @param fin sequence final type
	 */
	public void addMinWord(TLongList seq, int fin);
	
	/**
	 * Number of states in the FSA
	 * @return
	 */
	int size();
	
	/**
	 * Write FSA using Events interface
	 * @param events
	 * @throws FSAException
	 */
	void write(Events events) throws FSAException;


	/**
	 * Node of a long FSA
	 * @author Anton Kazennikov
	 *
	 */
	public interface Node {
		
		/**
		 * Get next state on given input
		 * @param input input char
		 * @return next node or null, if there is no such transition
		 */
		public Node getNext(long input);
		
		/**
		 * Set transition (current, input) -> next
		 * @param input input char
		 * @param next next state
		 */
		public void setNext(long input, Node next);
		
		/**
		 * Callback on transition removal
		 * @param input input char
		 * @param next next state
		 */
		public void removeInbound(long input, Node next);
		
		/**
		 * Callback on transition addition
		 * @param input input char
		 * @param next next state
		 */
		public void addInbound(long input, Node next);

		/**
		 * Final ids iterator
		 * @return
		 */
		public TIntIterator getFinal();
		
		/**
		 * Get final features count
		 * @return
		 */
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

		/**
		 * Is this state final?
		 */
		public boolean isFinal();
		
		/**
		 * Get number of inbound transitions
		 * @return
		 */
		public int outbound();
		
		/**
		 * Get number of outbound transitions
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
		 * @return
		 */
		public TLongObjectIterator<Node> next();

		/**
		 * Checks node equivalence
		 * @return
		 */
		public boolean equiv(Node node);

		/**
		 * Set node number
		 */
		public void setNumber(int num);
		
		/**
		 * Get node number
		 * @return
		 */
		public int getNumber();

	}
	

	/**
	 * Events producer for LongFSA
	 * @author ant
	 *
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
		 * @param states
		 */
		public void states(int states) throws FSAException;

		/**
		 * Announce current state for the writer
		 * @param state number of the current state
		 */
		public void state(int state) throws FSAException;

		/**
		 * Announce number of final features of the current state
		 * @param n number of final features
		 */
		public void finals(int n) throws FSAException;

		/**
		 * Announce final feature of the current state
		 * @param fin  final feature
		 */
		public void stateFinal(int fin) throws FSAException;

		/**
		 * Announce number of transitions of the current state
		 * @param n number of transitions
		 */
		public void transitions(int n) throws FSAException;

		/**
		 * Announce transition of the current state
		 * @param input input label
		 * @param dest number of the destination state
		 */
		public void transition(long input, int dest) throws FSAException;
	}

	public static class Simple implements LongFSA {
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

		public Simple(LongFSA.Node start) {
			this.start = start;
			nodes.add(start);
			start.setNumber(nodes.size());
		}


		/**
		 * Add sequence seq with final fin to trie
		 * @param seq sequence to add
		 * @param fin final state
		 */
		@Override
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

		@Override
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

		@Override
		public void write(final LongFSA.Events writer) throws FSAException {
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
}
