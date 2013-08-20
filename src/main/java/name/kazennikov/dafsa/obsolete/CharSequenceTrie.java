package name.kazennikov.dafsa.obsolete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.kazennikov.dafsa.obsolete.GenericTrie.SimpleNode;

/**
 * A Trie built on strings
 * 
 * @author Anton Kazennikov
 *
 */
public class CharSequenceTrie<Final> {
	GenericFSA<Character, Set<Final>, Final> trie;
	
	class Node extends GenericTrie.SimpleNode<Character, Set<Final>, Final> {

		public Node(Set<Final> fin) {
			super(fin);
		}

		@Override
		public SimpleNode<Character, Set<Final>, Final> makeNode() {
			return new Node(new HashSet<Final>());
		}
	}
	
	public CharSequenceTrie() {
		trie = new GenericFSA<Character, Set<Final>, Final>(new Node(new HashSet<Final>()));
	}
	
	/**
	 * Add string to the trie
	 * 
	 * @param s char sequence to walk
	 * @param fin final state payload to add
	 */
	public void addString(CharSequence s, Final fin) {
		GenericTrie.Node<Character, Set<Final>, Final> n = trie.getStart();

		for(int i = 0; i < s.length(); i++) {
			n = trie.getNextOrAdd(n, s.charAt(i));
		}
			
		n.addFinal(fin);
	}
	
	/**
	 * Walk trie 
	 * @param s char sequence to walk
	 * @return list of finals encountered of the path from trie start to final state
	 */
	public List<Final> walk(CharSequence s) {
		List<Final> finals = new ArrayList<Final>();
		
		GenericTrie.Node<Character, Set<Final>, Final> n = trie.getStart();
		int i = s.length();
		
		while(n != null && i > 0) {
			finals.addAll(n.getFinal());
			n = n.getNext(s.charAt(--i));
		}
		Collections.reverse(finals);
		
		return finals;
	}
	
	public int size() {
		return trie.size();
	}
	

}
