package name.kazennikov.trie;


public class IntTrieBuilderBoolean extends BaseIntTrieBuilder {
	
	public class Node extends BaseIntTrieBuilder.BaseNode {
		boolean fin;

		@Override
		int hc() {
			int hash = fin? 1 : 0 << 31;
			return hash + super.hc();
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Node))
				return false;
			Node other = (Node) obj;
			
			if(other.fin != this.fin)
				return false;
			

			return super.equals(obj);
		}

		@Override
		public void reset() {
			fin = false;
			super.reset();
		}

		@Override
		public BaseNode assign(BaseNode node) {
			if(node instanceof Node) {
				((Node) node).fin = fin;
			}
			
			return super.assign(node);
		}
		
		public boolean addFinal(boolean fin) {
			validHashCode = this.fin == fin;
			this.fin = fin;
			return !validHashCode;
		}


		public boolean removeFinal(boolean fin) {
			validHashCode = this.fin != fin;
			return !validHashCode;
		}

	}

	@Override
	public BaseNode newNode() {
		return new Node();
	}
	
	boolean finalValue;
	

	public void setFinalValue(boolean finalValue) {
		this.finalValue = finalValue;
	}

	@Override
	public boolean setFinal(int state) {
		return ((Node)nodes.get(state)).addFinal(finalValue);
	}

	@Override
	public boolean isFinal(int state) {
		return ((Node)nodes.get(state)).fin == finalValue;
	}



}
