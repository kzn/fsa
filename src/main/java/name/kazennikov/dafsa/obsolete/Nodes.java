package name.kazennikov.dafsa.obsolete;

import gnu.trove.iterator.TCharObjectIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.array.TCharArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TCharObjectHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TCharObjectProcedure;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TLongObjectProcedure;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodes for various FSA 
 * @author Anton Kazennikov
 *
 */
public class Nodes {
	private Nodes() {}

	/**
	 * Char-type node with Trove structures for outbound table
	 * @author Anton Kazennikov
	 *
	 */
	public static class CharTroveNode implements CharFSA.Node {
		TIntHashSet fin = new TIntHashSet();
		TCharObjectHashMap<CharFSA.Node> out = new TCharObjectHashMap<CharFSA.Node>();

		int inbound;
		int number;
		int hashCode;
		boolean validHashCode = true;
		
		public CharTroveNode() {
			inbound = 0;
			hashCode = 1;
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
		public CharFSA.Node getNext(char input) {
			return out.get(input);
		}
		@Override
		public void setNext(char input, CharFSA.Node next) {
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
			return fin.iterator();
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
		public void removeInbound(char input, CharFSA.Node base) {
			inbound--;
			
		}

		@Override
		public void addInbound(char input, CharFSA.Node base) {
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
				TCharObjectIterator<CharFSA.Node> it = out.iterator();
				
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
			if(!(obj instanceof CharTroveNode))
				return false;
			

			CharTroveNode other = (CharTroveNode) obj;
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
				
				TCharObjectIterator<CharFSA.Node> 
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
		public CharTroveNode makeNode() {
			return new CharTroveNode();
		}
		
		/*@Override
		public SimpleNode<In, FC, Final> makeNode() {
			return new SimpleNode<In, Final>();
		}*/

		@Override
		public CharTroveNode cloneNode() {
			final CharTroveNode node = makeNode();
			
			node.fin.addAll(this.fin);
			
			out.forEachEntry(new TCharObjectProcedure<CharFSA.Node>() {

				@Override
				public boolean execute(char key, CharFSA.Node value) {
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
		public TCharObjectIterator<CharFSA.Node> next() {
			return out.iterator();
		}
		
		@Override
		public boolean equiv(CharFSA.Node node) {
			if(!node.getFinal().equals(fin))
				return false;
			
			TCharObjectIterator<CharFSA.Node> it = out.iterator();
			while(it.hasNext()) {
				it.advance();
				
				CharFSA.Node n = node.getNext(it.key());
				if(n == null)
					return false;
				
				if(!it.value().equiv(n))
					return false;
			}
			
			return true;
			
		}

		@Override
		public CharFSA.Node assign(final CharFSA.Node node) {
			this.fin.forEach(new TIntProcedure() {
				
				@Override
				public boolean execute(int value) {
					node.addFinal(value);
					return true;
				}
			});
			
			this.out.forEachEntry(new TCharObjectProcedure<CharFSA.Node>() {

				@Override
				public boolean execute(char a, CharFSA.Node b) {
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
	 * Simple char-type node, where outbound table is an array
	 * @author Anton Kazennikov
	 *
	 */
	public static class CharSimpleNode implements CharFSA.Node {
		TIntHashSet fin = new TIntHashSet();
		//TCharObjectHashMap<Node> out = new TCharObjectHashMap<CharFSA.Node>();
		TCharArrayList outChars = new TCharArrayList();
		List<CharFSA.Node> outNodes = new ArrayList<CharFSA.Node>();

		int inbound;
		int number;
		int hashCode;
		boolean validHashCode = true;
		
		public CharSimpleNode() {
			inbound = 0;
			hashCode = 1;
		}
		
		@Override
		public void setNumber(int num) {
			this.number = num;
		}
		
		@Override
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
		
		@Override
		public CharFSA.Node getNext(char input) {
			for(int i = 0; i != outChars.size(); i++) {
				if(outChars.get(i) == input)
					return outNodes.get(i);
			}
			return null;
		}
		
		
		@Override
		public void setNext(char input, CharFSA.Node next) {
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
		
		@Override
		public TIntIterator getFinal() {
			return fin.iterator();
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
			return outChars.size();
		}
		@Override
		public int inbound() {
			return inbound;
		}

		@Override
		public void removeInbound(char input, CharFSA.Node base) {
			inbound--;
			
		}

		@Override
		public void addInbound(char input, CharFSA.Node base) {
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
			if(!(obj instanceof CharSimpleNode))
				return false;
			

			CharSimpleNode other = (CharSimpleNode) obj;
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

		@Override
		public CharSimpleNode makeNode() {
			return new CharSimpleNode();
		}
		
		/*@Override
		public SimpleNode<In, FC, Final> makeNode() {
			return new SimpleNode<In, Final>();
		}*/

		@Override
		public CharSimpleNode cloneNode() {
			final CharSimpleNode node = makeNode();
			
			node.fin.addAll(this.fin);
			
			for(int i = 0; i != outbound(); i++) {
				node.outChars.add(outChars.get(i));
				node.outNodes.add(outNodes.get(i));
			}
			
			return node;
		}

		@Override
		public void reset() {
			fin.clear();
			
			for(int i = 0; i != outbound(); i++) {
				char input = outChars.get(i);
				CharFSA.Node next = outNodes.get(i);
				
				next.removeInbound(input, this);
			}
			
			outChars.reset();
			outNodes.clear();
		}

		@Override
		public TCharObjectIterator<CharFSA.Node> next() {
			return new TCharObjectIterator<CharFSA.Node>() {
				
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
				public CharFSA.Node value() {
					return outNodes.get(pos);
				}
				
				@Override
				public CharFSA.Node setValue(CharFSA.Node val) {
					return null;
				}
				
				@Override
				public char key() {
					return outChars.get(pos);
				}
			};
		}
		
		@Override
		public boolean equiv(CharFSA.Node node) {
			if(!node.getFinal().equals(fin))
				return false;
			
			
			if(outbound() != node.outbound())
				return false;
			
			for(int i = 0; i != outbound(); i++) {
				char input = outChars.get(i);
				CharFSA.Node next = outNodes.get(i);
				CharFSA.Node otherNext = node.getNext(input);
				if(otherNext == null)
					return false;
				
				if(!next.equiv(otherNext))
					return false;
				
			}
			
			return true;
			
		}

		@Override
		public CharFSA.Node assign(final CharFSA.Node node) {
			
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
	
	/**
	 * Int-type node where outbound table is based on trove data structures
	 * @author Anton Kazennikov
	 *
	 */
	public static class IntTroveNode implements IntFSA.Node {
		TIntHashSet fin = new TIntHashSet();
		TIntObjectHashMap<IntFSA.Node> out = new TIntObjectHashMap<IntFSA.Node>();

		int inbound;
		int number;
		int hashCode;
		boolean validHashCode = true;
		
		public IntTroveNode() {
			inbound = 0;
			hashCode = 1;
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
		public IntFSA.Node getNext(int input) {
			return out.get(input);
		}
		@Override
		public void setNext(int input, IntFSA.Node next) {
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
		public void removeInbound(int input, IntFSA.Node base) {
			inbound--;
			
		}

		@Override
		public void addInbound(int input, IntFSA.Node base) {
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
				TIntObjectIterator<IntFSA.Node> it = out.iterator();
				
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
			if(!(obj instanceof IntTroveNode))
				return false;
			

			IntTroveNode other = (IntTroveNode) obj;
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
				
				TIntObjectIterator<IntFSA.Node> 
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
		public IntTroveNode makeNode() {
			return new IntTroveNode();
		}
		
		/*@Override
		public SimpleNode<In, FC, Final> makeNode() {
			return new SimpleNode<In, Final>();
		}*/

		@Override
		public IntTroveNode cloneNode() {
			final IntTroveNode node = makeNode();
			
			node.fin.addAll(this.fin);
			
			out.forEachEntry(new TIntObjectProcedure<IntFSA.Node>() {

				@Override
				public boolean execute(int key, IntFSA.Node value) {
					node.setNext(key, value);
					return true;
				}
			});
			
			return node;
		}

		@Override
		public void reset() {
			fin.clear();
			
			for(int ch : out.keys()) {
				setNext(ch, null);
			}
		}

		@Override
		public TIntObjectIterator<IntFSA.Node> next() {
			return out.iterator();
		}
		
		@Override
		public boolean equiv(IntFSA.Node node) {
			if(!node.getFinal().equals(fin))
				return false;
			
			TIntObjectIterator<IntFSA.Node> it = out.iterator();
			while(it.hasNext()) {
				it.advance();
				
				IntFSA.Node n = node.getNext(it.key());
				if(n == null)
					return false;
				
				if(!it.value().equiv(n))
					return false;
			}
			
			return true;
			
		}

		@Override
		public IntFSA.Node assign(final IntFSA.Node node) {
			this.fin.forEach(new TIntProcedure() {
				
				@Override
				public boolean execute(int value) {
					node.addFinal(value);
					return true;
				}
			});
			
			this.out.forEachEntry(new TIntObjectProcedure<IntFSA.Node>() {

				@Override
				public boolean execute(int a, IntFSA.Node b) {
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
	 * Long-type node with trove structures
	 * @author Anton Kazennikov
	 *
	 */
	public static class LongTroveNode implements LongFSA.Node {
		TIntHashSet fin = new TIntHashSet();
		TLongObjectHashMap<LongFSA.Node> out = new TLongObjectHashMap<LongFSA.Node>();

		int inbound;
		int number;
		int hashCode;
		boolean validHashCode = true;
		
		public LongTroveNode() {
			inbound = 0;
			hashCode = 1;
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
		public LongFSA.Node getNext(long input) {
			return out.get(input);
		}
		@Override
		public void setNext(long input, LongFSA.Node next) {
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
		public void removeInbound(long input, LongFSA.Node base) {
			inbound--;
			
		}

		@Override
		public void addInbound(long input, LongFSA.Node base) {
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
				TLongObjectIterator<LongFSA.Node> it = out.iterator();
				
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
			if(!(obj instanceof LongTroveNode))
				return false;
			

			LongTroveNode other = (LongTroveNode) obj;
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
				
				TLongObjectIterator<LongFSA.Node> 
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
		public LongTroveNode makeNode() {
			return new LongTroveNode();
		}
		
		/*@Override
		public SimpleNode<In, FC, Final> makeNode() {
			return new SimpleNode<In, Final>();
		}*/

		@Override
		public LongTroveNode cloneNode() {
			final LongTroveNode node = makeNode();
			
			node.fin.addAll(this.fin);
			
			out.forEachEntry(new TLongObjectProcedure<LongFSA.Node>() {

				@Override
				public boolean execute(long key, LongFSA.Node value) {
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
		public TLongObjectIterator<LongFSA.Node> next() {
			return out.iterator();
		}
		
		@Override
		public boolean equiv(LongFSA.Node node) {
			if(!node.getFinal().equals(fin))
				return false;
			
			TLongObjectIterator<LongFSA.Node> it = out.iterator();
			while(it.hasNext()) {
				it.advance();
				
				LongFSA.Node n = node.getNext(it.key());
				if(n == null)
					return false;
				
				if(!it.value().equiv(n))
					return false;
			}
			
			return true;
			
		}

		@Override
		public LongFSA.Node assign(final LongFSA.Node node) {
			this.fin.forEach(new TIntProcedure() {
				
				@Override
				public boolean execute(int value) {
					node.addFinal(value);
					return true;
				}
			});
			
			this.out.forEachEntry(new TLongObjectProcedure<LongFSA.Node>() {

				@Override
				public boolean execute(long a, LongFSA.Node b) {
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
	 * Int-type node with trove structures as outbound table
	 * @author Anton Kazennikov
	 *
	 */
	public static class IntSimpleNode implements IntFSA.Node {
		TIntHashSet fin = new TIntHashSet();
		//TCharObjectHashMap<Node> out = new TCharObjectHashMap<CharFSA.Node>();
		TIntArrayList outChars = new TIntArrayList();
		List<IntFSA.Node> outNodes = new ArrayList<IntFSA.Node>();

		int inbound;
		int number;
		int hashCode;
		boolean validHashCode = true;
		
		public IntSimpleNode() {
			inbound = 0;
			hashCode = 1;
		}
		
		@Override
		public void setNumber(int num) {
			this.number = num;
		}
		
		@Override
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
		
		@Override
		public IntFSA.Node getNext(int input) {
			for(int i = 0; i != outChars.size(); i++) {
				if(outChars.get(i) == input)
					return outNodes.get(i);
			}
			return null;
		}
		
		
		@Override
		public void setNext(int input, IntFSA.Node next) {
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
		
		@Override
		public TIntIterator getFinal() {
			return fin.iterator();
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
			return outChars.size();
		}
		@Override
		public int inbound() {
			return inbound;
		}

		@Override
		public void removeInbound(int input, IntFSA.Node base) {
			inbound--;
			
		}

		@Override
		public void addInbound(int input, IntFSA.Node base) {
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
			if(!(obj instanceof IntSimpleNode))
				return false;
			

			IntSimpleNode other = (IntSimpleNode) obj;
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

		@Override
		public IntSimpleNode makeNode() {
			return new IntSimpleNode();
		}
		
		/*@Override
		public SimpleNode<In, FC, Final> makeNode() {
			return new SimpleNode<In, Final>();
		}*/

		@Override
		public IntSimpleNode cloneNode() {
			final IntSimpleNode node = makeNode();
			
			node.fin.addAll(this.fin);
			
			for(int i = 0; i != outbound(); i++) {
				node.outChars.add(outChars.get(i));
				node.outNodes.add(outNodes.get(i));
			}
			
			return node;
		}

		@Override
		public void reset() {
			fin.clear();
			
			for(int i = 0; i != outbound(); i++) {
				int input = outChars.get(i);
				IntFSA.Node next = outNodes.get(i);
				
				next.removeInbound(input, this);
			}
			
			outChars.reset();
			outNodes.clear();
		}

		@Override
		public TIntObjectIterator<IntFSA.Node> next() {
			return new TIntObjectIterator<IntFSA.Node>() {
				
				int pos = -1;
				
				@Override
				public void remove() {
				}
				
				@Override
				public boolean hasNext() {
					return pos + 1 < outChars.size();
				}
				
				@Override
				public void advance() {
					pos++;
				}
				
				@Override
				public IntFSA.Node value() {
					return outNodes.get(pos);
				}
				
				@Override
				public IntFSA.Node setValue(IntFSA.Node val) {
					return null;
				}
				
				@Override
				public int key() {
					return outChars.get(pos);
				}
			};
		}
		
		@Override
		public boolean equiv(IntFSA.Node node) {
			if(!node.getFinal().equals(fin))
				return false;
			
			
			if(outbound() != node.outbound())
				return false;
			
			for(int i = 0; i != outbound(); i++) {
				int input = outChars.get(i);
				IntFSA.Node next = outNodes.get(i);
				IntFSA.Node otherNext = node.getNext(input);
				if(otherNext == null)
					return false;
				
				if(!next.equiv(otherNext))
					return false;
				
			}
			
			return true;
			
		}

		@Override
		public IntFSA.Node assign(final IntFSA.Node node) {
			
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
}
