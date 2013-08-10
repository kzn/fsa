package name.kazennikov.trie;

import java.util.HashSet;
import java.util.Set;

public class IntTrieBuilderObject<E> extends BaseIntTrieBuilder {
	
	public class Node extends BaseIntTrieBuilder.BaseNode {
		Set<E> fin = new HashSet<>();

		@Override
		int hc() {
			int hash = fin.hashCode();
			return hash + super.hc();
		}

		@Override
		public boolean equals(Object obj) {
			if(!this.getClass().isAssignableFrom(obj.getClass()))
				return false;
			@SuppressWarnings("unchecked")
			Node other = (Node) obj;
			
			if((other.fin == null && fin != null) || (fin == null && other.fin != null))
				return false;
			
			if(!other.fin.equals(fin))
				return false;

			return super.equals(obj);
		}

		@Override
		public void reset() {
			if(fin != null)
				fin.clear();
			
			super.reset();
		}

		@SuppressWarnings("unchecked")
		@Override
		public BaseNode assign(BaseNode node) {
			if(this.getClass().isAssignableFrom(node.getClass())) {
				((Node) node).fin.clear();
				((Node) node).fin.addAll(fin);
			}
			
			return super.assign(node);
		}
		
		public boolean addFinal(E fin) {
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
	
	E finalValue;
	
	public void setFinalValue(E finalValue) {
		this.finalValue = finalValue;
	}

	@Override
	public boolean setFinal(int state) {
		return ((Node)nodes.get(state)).addFinal(finalValue);
	}

	@Override
	public boolean isFinal(int state) {
		return ((Node)nodes.get(state)).fin.contains(finalValue);
	}

}
