package name.kazennikov.dafsa;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.iterator.TCharObjectIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TCharArrayList;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class Nodes {
	private Nodes() {}
	
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
		
		public void setNumber(int num) {
			this.number = num;
		}
		
		public int getNumber() {
			return number;
		}
		
		int findIndex(char input) {
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


}
