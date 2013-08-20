package name.kazennikov.dafsa.obsolete;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import name.kazennikov.dafsa.obsolete.GenericTrie.Node;


/**
 * Simple trie class with some basic functionality

 * @author Anton Kazennikov
 *
 * @param <In> label type
 * @param <FC> type for collection of final types
 * @param <Final>  final type
 */
public class GenericFSA<In, FC extends Collection<Final>, Final> {
	GenericTrie.Node<In, FC, Final> start;
	List<GenericTrie.Node<In, FC, Final>> nodes = new ArrayList<GenericTrie.Node<In, FC, Final>>();
	
	Stack<GenericTrie.Node<In, FC, Final>> free = new Stack<GenericTrie.Node<In, FC, Final>>();
	
	Register reg = new Register();
	
	class Register {
		HashMap<Node<In, FC, Final>, Node<In, FC, Final>> m = new HashMap<GenericTrie.Node<In,FC, Final>, GenericTrie.Node<In,FC, Final>>();
		
		public boolean contains(Node<In, FC, Final> node) {
			return m.containsKey(node);
		}
		
		public Node<In, FC, Final> get(Node<In, FC, Final> node) {
			return m.get(node);
		}
		
		public void add(Node<In, FC, Final> node) {
			m.put(node, node);
		}
		
		public void remove(Node<In, FC, Final> node) {
			Node<In, FC, Final> regNode = m.get(node);
			
			if(regNode == null)
				return;
			
			if(node == regNode)
				m.remove(node);
		}
		
	}
	
	public GenericFSA(GenericTrie.Node<In, FC, Final> start) {
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
	public void add(List<In> seq, Final fin) {
		GenericTrie.Node<In, FC, Final> current = start;
		
		int idx = 0;
		
		while(idx < seq.size()) {
			GenericTrie.Node<In, FC, Final> n = current.getNext(seq.get(idx));
			if(n == null)
				break;
			
			idx++;
			current = n;
		}
		
		if(idx == seq.size()) {
			addFinal(current, fin);
		} else {
			addSuffix(current, seq.subList(idx, seq.size()), fin);
		}
	}
	
	
	/**
	 * Get next state to the given with input in, get existing state or add new
	 * 
	 * @param node base trie node
	 * @param in input
	 * @return next node
	 */
	public GenericTrie.Node<In, FC, Final> getNextOrAdd(GenericTrie.Node<In, FC, Final> node, In in) {
		GenericTrie.Node<In, FC, Final> next = node.getNext(in);
		
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
	public FC get(List<In> seq) {
		GenericTrie.Node<In, FC, Final> n = start;

		for(In in : seq) {
			GenericTrie.Node<In, FC, Final> next = n.getNext(in);
			
			if(next == null)
				return null;
			
			n = next;
		}
		
		return n.getFinal();
	}
	
	/*public FC getAll(List<In> seq) {
		//Set<Final> finals = new HashSet<Final>();
		Trie.Node<In, FC, Final> n = start;

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
	 * 
	 * @param n base node
	 * @param seq sequence to add
	 * @param fin final state
	 */
	protected List<Node<In, FC, Final>> addSuffix(GenericTrie.Node<In, FC, Final> n, List<In> seq, Final fin) {
		GenericTrie.Node<In, FC, Final> current = n;
		
		List<Node<In, FC, Final>> nodes = new ArrayList<GenericTrie.Node<In, FC, Final>>();
		
		for(In in : seq) {
			GenericTrie.Node<In, FC, Final> node = makeNode();
			nodes.add(node);
			setNext(current, in, node);
			current = node;
		}
		
		addFinal(current, fin);
		
		return nodes;
	}
	
	public void addFinal(GenericTrie.Node<In, FC, Final> node, Final fin) {
		reg.remove(node);
		node.addFinal(fin);
	}
	
	public void setNext(GenericTrie.Node<In, FC, Final> src, In in, Node<In, FC, Final> dest) {
		reg.remove(src);
		src.setNext(in, dest);
	}
	
	/**
	 * Get start node
	 * @return
	 */
	public GenericTrie.Node<In, FC, Final> getStart() {
		return start;
	}
	
	
	/**
	 * Make new node
	 * @return
	 */
	protected GenericTrie.Node<In, FC, Final> makeNode() {
		if(!free.empty())
			return free.pop();
		
		GenericTrie.Node<In, FC, Final> node = start.makeNode();//new Trie.SimpleNode<In, Final>();
		nodes.add(node);
		node.setNumber(nodes.size());
		return node;
	}
	
	protected GenericTrie.Node<In, FC, Final> cloneNode(GenericTrie.Node<In, FC, Final> src) {
		GenericTrie.Node<In, FC, Final> node = makeNode();
		src.assign(node);
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
	
	public boolean isConfluence(GenericTrie.Node<In, FC, Final> node) {
		return node.inbound() > 1;
	}
	
	List<Node<In, FC, Final>> commonPrefix(List<In> input) {
		Node<In, FC, Final> current = start;
		List<Node<In, FC, Final>> prefix = new ArrayList<GenericTrie.Node<In, FC, Final>>();
		prefix.add(current);
		
		for(In in : input) {
			Node<In, FC, Final> next = current.getNext(in);
			
			if(next == null)
				break;
			
			current = next;
			prefix.add(current);
		}
		
		return prefix;
	}
	
	int findConfluence(List<Node<In, FC, Final>> nodes) {
		for(int i = 0; i != nodes.size(); i++)
			if(isConfluence(nodes.get(i)))
				return i;
		
		return 0;
	}
	
	public void addMinWord(List<In> input, Final fin) {
		/*
		 * 1. get common prefix
		 * 2. find first confluence state in the common prefix
		 * 3. if any, clone it and all states after it in common prefix
		 * 4. add suffix
		 * 5. minimize(replaceOrRegister from the last state toward the first)
		 */
		
		List<Node<In, FC, Final>> prefix = commonPrefix(input);
		
		int confIdx = findConfluence(prefix);
		int stopIdx = confIdx == 0? prefix.size() : confIdx;
				
		if(confIdx > 0) {	
			int idx = confIdx;
			
			while(idx < prefix.size()) {
				Node<In, FC, Final> prev = prefix.get(idx - 1);
				Node<In, FC, Final> cloned = cloneNode(prefix.get(idx));
				prefix.set(idx, cloned);
				setNext(prev, input.get(confIdx - 1), cloned);
				idx++;
				confIdx++;
			}
		}
		

		
		List<Node<In, FC, Final>> nodeList = new ArrayList<GenericTrie.Node<In,FC, Final>>(prefix);
		
		nodeList.addAll(addSuffix(prefix.get(prefix.size() - 1), input.subList(prefix.size() - 1, input.size()), fin));
		
		replaceOrRegister(input, nodeList, stopIdx);
		
		
		
	}


	private void replaceOrRegister(List<In> input, List<Node<In, FC, Final>> nodeList, int stop) {
		if(nodeList.size() < 2)
			return;
		
		int idx = nodeList.size() - 1;
		int inIdx = input.size() - 1;
		
		while(idx > 0) {
			Node<In, FC, Final> n = nodeList.get(idx);
			Node<In, FC, Final> regNode = reg.get(n);
			
			//if(n.equiv(regNode))
			
			// stop
			if(regNode == n) {
				if(idx < stop)
					return;
			} else if(regNode == null) {
				reg.add(n);
			} else {
				In in = input.get(inIdx);
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
	
	public void toDot(String fileName) throws IOException {
		PrintWriter pw = new PrintWriter(fileName);
		
		pw.println("digraph finite_state_machine {");
		pw.println("rankdir=LR;");
		pw.println("node [shape=circle]");
		
		for(Node<In, FC, Final> n : nodes) {
			int src = n.getNumber();

			if(n.isFinal()) {
				pw.printf("%d [shape=doublecircle, label=\"%d %s\"];%n", src, src, n.getFinal());
			}
			
			for(Map.Entry<In, Node<In, FC, Final>> next : n.next().entrySet()) {
				int dest = next.getValue().getNumber();

				pw.printf("%d -> %d [label=\"%s\"];%n", src, dest, next.getKey());
			}
		}
		
		pw.println("}");
		pw.close();
	}
	
	public Node<In, FC, Final> getNode(int index) {
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
	
	public void write(GenericTrie.Writer<In, Final> writer) throws IOException {
		writer.states(nodes.size());
		
		for(Node<In, FC, Final> node : nodes) {
			writer.state(node.getNumber());
			
			writer.finals(node.getFinal().size());
			for(Final f : node.getFinal()) {
				writer.stateFinal(f);
			}
			
			writer.transitions(node.next().size());
			for(Map.Entry<In, Node<In,FC, Final>> e : node.next().entrySet()) {
				writer.transition(e.getKey(), e.getValue().getNumber());
			}
		}
	}
}
