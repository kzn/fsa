package name.kazennikov.dafsa;

import java.util.HashMap;

public class GenericRegister<E> {
	HashMap<E, E> m = new HashMap<E, E>();

	public boolean contains(E node) {
		return m.containsKey(node);
	}

	public E get(E node) {
		return m.get(node);
	}

	public void add(E node) {
		m.put(node, node);
	}

	public void remove(E node) {
		E regNode = m.get(node);

		if(regNode == null)
			return;

		if(node == regNode)
			m.remove(node);
	}
}
