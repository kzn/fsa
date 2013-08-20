package name.kazennikov.dafsa.obsolete;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Trie based on generics
 * 
 * @author Anton Kazennikov
 *
 * @param <In> input label type
 * @param <FC> collection for storing finals
 * @param <Final> final type
 */
public class GenericTrie<In, FC extends Collection<Final>, Final> {

	public interface Node<In, FC, Final> {
		public Node<In, FC, Final> getNext(In input);
		public void setNext(In input, Node<In, FC, Final> next);
		public void removeInbound(Node<In, FC, Final> base, In input);
		public void addInbound(Node<In, FC, Final> base, In input);
		
		public FC getFinal();

		/**
		 * Add final feature to node
		 * 
		 * @param fin final feature
		 * @return true, if feature was added to the finals collection
		 */
		public boolean addFinal(Final fin);
		
		/**
		 * Remove final feature from the node
		 * 
		 * @param fin final feature
		 * @return true, if feature was removed from the finals collection
		 */
		public boolean removeFinal(Final fin);
		
		public boolean isFinal();
		public int outbound();
		public int inbound();
		
		public Node<In, FC, Final> makeNode();
		public Node<In, FC, Final> cloneNode();
		public Node<In, FC, Final> assign(Node<In, FC, Final> dest);
		
		public void reset();
		
		public Map<In, Node<In, FC, Final>> next();
		
		public boolean equiv(Node<In, FC, Final> node);

		public void setNumber(int num);
		public int getNumber();
		
	}
	
	
	public static abstract class SimpleNode<In, FC extends Collection<Final>, Final> implements Node<In, FC, Final> {
		FC fin;
		Map<In, Node<In, FC, Final>> out = new TreeMap<In, GenericTrie.Node<In, FC, Final>>();

		int inbound;
		int number;
		int hashCode;
		boolean validHashCode = true;
		
		public SimpleNode(FC fin) {
			inbound = 0;
			hashCode = 1;
			this.fin = fin;
		}
		
		@Override
		public void setNumber(int num) {
			this.number = num;
		}
		
		@Override
		public int getNumber() {
			return number;
		}
		
		@Override
		public Node<In, FC, Final> getNext(In input) {
			return out.get(input);
		}
		@Override
		public void setNext(In input, Node<In, FC, Final> next) {
			if(out.containsKey(input)) {
				out.get(input).removeInbound(this, input);
			}
			
			if(next != null) {
				out.put(input, next);
				next.addInbound(this, input);
			} else {
				out.remove(input);
			}
			
			validHashCode = false;
		}
		
		@Override
		public FC getFinal() {
			return fin;
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
		public void removeInbound(Node<In, FC, Final> base, In input) {
			inbound--;
			
		}

		@Override
		public void addInbound(Node<In, FC, Final> base, In input) {
			inbound++;
		}

		@Override
		public boolean addFinal(Final fin) {
			validHashCode = !this.fin.add(fin);
			return !validHashCode;
		}

		@Override
		public boolean removeFinal(Final fin) {
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
				for(Map.Entry<In, Node<In, FC, Final>> next : out.entrySet()) {
					result = prime * result + next.getKey().hashCode();
					result = prime * result + System.identityHashCode(next.getValue());
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
			
			@SuppressWarnings("unchecked")
			SimpleNode<In, FC, Final> other = (SimpleNode<In, FC, Final>) obj;
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
				
				Iterator<Map.Entry<In, Node<In, FC, Final>>> 
					it1 = out.entrySet().iterator(),
					it2 = other.out.entrySet().iterator();
				
				while(it1.hasNext()) {
					if(!it2.hasNext())
						return false;
					
					Map.Entry<In, Node<In, FC, Final>>  n1 = it1.next();
					Map.Entry<In, Node<In, FC, Final>>  n2 = it2.next();
					
					if(!n1.getKey().equals(n2.getKey()))
						return false;
					
					if(n2.getValue() != n1.getValue())
						return false;
				}
				
				
			}
			
			return true;
		}

		@Override
		public abstract SimpleNode<In, FC, Final> makeNode();
		
		/*@Override
		public SimpleNode<In, FC, Final> makeNode() {
			return new SimpleNode<In, Final>();
		}*/

		@Override
		public Node<In, FC, Final> cloneNode() {
			SimpleNode<In, FC, Final> node = makeNode();
			
			node.fin.addAll(this.fin);
			
			for(Map.Entry<In, Node<In, FC, Final>> p : out.entrySet()) {
				node.setNext(p.getKey(), p.getValue());
			}
			
			return node;
		}

		@Override
		public void reset() {
			fin.clear();
			
			for(In p : new ArrayList<In>(out.keySet())) {
				setNext(p, null);
			}
		}

		@Override
		public Map<In, Node<In, FC, Final>> next() {
			return Collections.unmodifiableMap(out);
		}
		
		@Override
		public boolean equiv(Node<In, FC, Final> node) {
			if(!node.getFinal().equals(fin))
				return false;
			
			for(Map.Entry<In, Node<In, FC, Final>> e : out.entrySet()) {
				Node<In, FC, Final> n = node.getNext(e.getKey());
				
				if(n == null)
					return false;
				
				if(!e.getValue().equiv(n))
					return false;
			}
			
			return true;
			
		}

		@Override
		public Node<In, FC, Final> assign(Node<In, FC, Final> node) {
			for(Final f : this.fin)
				node.addFinal(f);

			for(Map.Entry<In, Node<In, FC, Final>> p : out.entrySet()) {
				node.setNext(p.getKey(), p.getValue());
			}

			return node;
		}
	}
	

	/**
	 * Writer for trie
	 * 
	 * @author Anton Kazennikov
	 *
	 * @param <In> input label type
	 * @param <Final> final feature type
	 */
	public interface Writer<In, Final> {
		/**
		 * Announce number of states in the trie
		 * 
		 * @param states
		 */
		public void states(int states) throws IOException;

		/**
		 * Announce current state for the writer
		 * 
		 * @param state number of the current state
		 */
		public void state(int state) throws IOException;

		/**
		 * Announce number of final features of the current state
		 * 
		 * @param n number of final features
		 */
		public void finals(int n) throws IOException;

		/**
		 * Announce final feature of the current state
		 * 
		 * @param fin  final feature
		 */
		public void stateFinal(Final fin) throws IOException;
		
		/**
		 * Announce number of transitions of the current state
		 * 
		 * @param n number of transitions
		 */
		public void transitions(int n) throws IOException;

		/**
		 * Announce transition of the current state
		 * 
		 * @param input input label
		 * @param dest number of the destination state
		 */
		public void transition(In input, int dest) throws IOException;
	}
}
