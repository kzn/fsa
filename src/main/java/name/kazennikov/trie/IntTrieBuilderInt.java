package name.kazennikov.trie;

import gnu.trove.set.hash.TIntHashSet;

public class IntTrieBuilderInt extends BaseIntTrieBuilder {
	
	public class Node extends BaseIntTrieBuilder.BaseNode {
		TIntHashSet fin = null;

		@Override
		int hc() {
			int hash = fin == null? 0 : fin.hashCode();
			return hash + super.hc();
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Node))
				return false;
			Node other = (Node) obj;
			
			if((other.fin == null && fin != null) || (fin == null && other.fin != null))
				return false;
			
			
			
			if(fin != null && !other.fin.equals(fin))
				return false;

			return super.equals(obj);
		}

		@Override
		public void reset() {
			fin = null;
			
			super.reset();
		}

		@Override
		public BaseNode assign(BaseNode node) {
			if(node instanceof Node) {
				if(fin == null) {
					((Node) node).fin = null;
				} else {
					if(((Node) node).fin == null) {
						((Node) node).fin = new TIntHashSet(fin);
					}
				}				
			}
			
			return super.assign(node);
		}
		
		public boolean addFinal(int fin) {
			if(this.fin == null)
				this.fin = new TIntHashSet(3);
			
			validHashCode = !this.fin.add(fin);
			return !validHashCode;
		}


		public boolean removeFinal(int fin) {
			validHashCode = !this.fin.remove(fin);
			return !validHashCode;
		}

	}

	@Override
	public BaseNode newNode() {
		return new Node();
	}
	
	int finalValue;
	
	public void setFinalValue(int finalValue) {
		this.finalValue = finalValue;
	}

	@Override
	public boolean setFinal(int state) {
		return ((Node)nodes.get(state)).addFinal(finalValue);
	}

	@Override
	public boolean isFinal(int state) {
		Node n = (Node) nodes.get(state);
		
		if(n.fin == null)
			return false;
		
		return n.fin.contains(finalValue);
	}

}
