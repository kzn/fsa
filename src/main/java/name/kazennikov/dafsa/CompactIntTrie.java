package name.kazennikov.dafsa;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.queue.TIntQueue;
import name.kazennikov.trove.TIntDeque;

import java.lang.reflect.Array;

/**
 * Created on 8/30/15.
 *
 * @author Anton Kazennikov
 */
public class CompactIntTrie {

	public static final int RESERVED = Integer.MIN_VALUE;


	/*
		2i - label
		2i+1 - next state
	 */
	TrieMemory m;
	int nextState = 0;
	int start;
	int stateCount;



	public static class Memory {
		int data[];
		TIntArrayList[] blocks = new TIntArrayList[16];
		int unallocPtr;

		public static int blockSize(int size) {
			int log = 32 - Integer.numberOfLeadingZeros(size);

			// power of two
			if(Integer.numberOfTrailingZeros(size) == log - 1)
				return log;

			return log + 1;
		}

		public Memory(int size) {
			data = new int[1 << blockSize(size)];
			unallocPtr = 0;

		}


		public TIntArrayList getBlocksFor(int size) {
			if(blocks.length < size) {
				int oldSize = blocks.length;
				blocks = realloc(blocks, TIntArrayList.class, size + 1);

				for(int i = oldSize; i < blocks.length; i++) {
					blocks[i] = new TIntArrayList(16);
				}
			}

			return blocks[size];
		}

		public int getBlock(int size) {
			int blockSize = blockSize(size);
			return getBlockInternal(blockSize);
		}

		public int getBlockInternal(int blockSize) {
			TIntArrayList l = getBlocksFor(blockSize);
			int intSize = 1 << blockSize;

			// if allocating largest block
			if(l.isEmpty() && blockSize + 1 == blocks.length) {


				// allocate from free space
				if(data.length - unallocPtr > intSize) {
					int newLength = data.length;
					while(newLength - unallocPtr < intSize) {
						newLength = newLength + (newLength >> 2);
					}

					data = realloc(data, newLength);
				}

				l.add(unallocPtr);
				int block = unallocPtr;
				unallocPtr += intSize;
				return block;
			} else if(l.isEmpty()) {

				int block = getBlockInternal(blockSize + 1);
				l.add(block + blockSize); // as power of two

				// split it
				return block;
			}

			int block = l.get(l.size() - 1);
			l.removeAt(l.size() - 1);
			return block;
		}


	}



	public static class TrieMemory {


		int[] data;
		int unallocPtr;

		TIntDeque[] q = new TIntDeque[0];

		public TrieMemory(int size) {
			data = new int[size];
		}

		public TIntDeque queueFor(int blockSize) {

			if(blockSize >= q.length) {
				int oldSize = q.length;
				q = realloc(q, TIntDeque.class, blockSize + 1);

				for(int i = oldSize; i < q.length; i++) {
					q[i] = new TIntDeque(16);
				}
			}

			return q[blockSize];
		}

		public void ensureFree(int dataSize) {
			if(data.length - unallocPtr > dataSize)
				return;

			int newSize = data.length;

			while(newSize - unallocPtr < dataSize) {
				newSize = newSize + (newSize >> 2);
			}

			data = realloc(data, newSize);
		}

		public int alloc(int blockSize) {
			TIntDeque q = queueFor(blockSize);

			if(q.isEmpty()) {
				int tr = 1 << blockSize;
				int dataSize = 1 + tr*2;
				ensureFree(dataSize);

				int ptr = unallocPtr;
				unallocPtr += dataSize;
				return ptr;
			} else {
				return q.pollFirst();
			}
		}

		public void free(int ptr, int blockSize) {
			TIntDeque q = queueFor(blockSize);
			q.addLast(ptr);
		}



		public static int blockSize(int size) {
			if(size == 0)
				return 0;

			int log = 32 - Integer.numberOfLeadingZeros(size);

			// power of two
			if(Integer.numberOfTrailingZeros(size) == log - 1)
				return log;

			return log + 1;
		}

		public int getUnallocPtr() {
			return unallocPtr;
		}





	}

	//List<Chunk> blocks = new ArrayList<>();

	public CompactIntTrie() {
		m = new TrieMemory(256);
		start = addState(0);
	}

	public static int[] realloc(int[] a, int newLength) {
		assert newLength >= a.length;

		int[] newArray = new int[newLength]; // factor 1.25
		System.arraycopy(a, 0, newArray, 0, a.length);
		return newArray;
	}

	public static <E> E realloc(E array, Class<?> c, int newLength) {
		int len = Array.getLength(array);

		assert newLength >= len;

		Object o = Array.newInstance(c, newLength);
		System.arraycopy(array, 0, o, 0, len);
		return (E) o;
	}




	/**
	 * Add new empty state
	 * @param reserve number of reserved transitions
	 * @return state index
	 */
	public int addState(int reserve) {
		stateCount++;
		int pos = m.alloc(m.blockSize(reserve)); // header

		m.data[pos] = 0;
//		for(int i = 0; i < reserve; i++) {
//			m.data[pos + 1 + 2*i] = RESERVED; // set label to reserve
//		}

		return pos;
	}

	public int stateSize(int state) {
		return m.data[state] * 2 + 1; // transitions (label, next) + head
	}

	/**
	 *
	 * @param parentState parent state
	 * @param state target state
	 * @param label label
	 * @param next dest state
	 * @return source state (possibly reallocated)
	 */
	public int addTransition(int parentState, int state, int label, int next) {

		int trCount = m.data[state];
		int oldBlockSize = m.blockSize(trCount);
		int newBlockSize = m.blockSize(trCount + 1);
		// sameBlock
		if(oldBlockSize == newBlockSize) {
			m.data[state]++;
			m.data[state + 1 + trCount *2] = label;
			m.data[state + 1 + trCount *2 + 1] = next;
			return state;
		}
		// failed to find reserved transitions, relocating the state
		int newState =  m.alloc(newBlockSize);
		m.data[newState] = trCount + 1; // inc transition count
		System.arraycopy(m.data, state + 1, m.data, newState + 1, trCount*2); // relocateState
		m.data[newState + trCount*2 + 1] = label;
		m.data[newState + trCount*2 + 1 + 1] = next;
		m.free(state, oldBlockSize);

		// change start if necessary
		if(state == start) {
			start = newState;
		}

		// change parent transition
		if(parentState != -1) {

			int count = m.data[parentState];

			// change transition from parentState to state using new state address
			for(int i = 0; i < count; i++) {
				int ptr = parentState + 1 + i * 2; // transitions start + i-th transition offset
				if(m.data[ptr + 1] == state) {
					m.data[ptr + 1] = newState;
					break;
				}
			}
		}

		return newState;
	}

	public int findTransition(int state, int label) {
		int count = m.data[state];

		for(int i = 0; i < count; i++) {
			int ptr = state + 1 + i * 2; // transitions start + i-th transition offset
			if(m.data[ptr] == label)
				return m.data[ptr + 1];
		}

		return -1;
	}

	/**
	 *
	 * Add word to the trie. It is the caller responsibility to establish such protocol that guarantee
	 * that the final state couldn't be relocated (i.e. that the given word couldn't be a prefix of some other
	 * word)
	 *
	 * @param l word to add
	 * @return last state id, or -1 if the word is a prefix of another word
	 *
	 */
	public int add(TIntList l) {
		int state = start;
		int prevState = -1;
		int pos = 0;

		while(pos < l.size()) {
			int next = findTransition(state, l.get(pos));
			if(next == -1)
				break;
			prevState = state;
			state = next;
			pos++;
		}

		if(pos == l.size())
			return -1; // l is a prefix of another word

		return addSuffix(l, pos, prevState, state);
	}

	protected int addSuffix(TIntList l, int pos, int prevState,int state) {
		while(pos < l.size()) {
			int next = addState(pos != l.size() - 1? 1 : 0);
			state = addTransition(prevState, state, l.get(pos), next); // reduce relocations by reserving transitions for non-last states
			prevState = state;
			state = next;
			pos++;
		}

		return state;
	}

	public boolean contains(TIntList l) {
		int pos = 0;
		int state = start;

		while(pos < l.size()) {
			state = findTransition(state, l.get(pos));
			if(state == -1)
				return false;
			pos++;
		}

		return findTransition(state, 0) != -1;
	}

	public int size() {
		return stateCount;
	}

	public int dataSize() {
		return m.data.length;
	}


	public static void main(String[] args) {
		CompactIntTrie trie = new CompactIntTrie();
		TIntArrayList l = new TIntArrayList();
		TroveUtils.expand(l, "foo");
		l.add(0);
		trie.add(l);
		TroveUtils.expand(l, "foo");
		System.out.println(trie.contains(l));

		TroveUtils.expand(l, "foobar");
		l.add(0);
		trie.add(l);
		TroveUtils.expand(l, "foo");
		System.out.println(trie.contains(l));
		TroveUtils.expand(l, "foobar");
		System.out.println(trie.contains(l));
		System.out.println(trie.stateCount);

		/*TroveUtils.expand(l, "foobar");
		System.out.println(trie.contains(l));
		TroveUtils.expand(l, "fo");
		System.out.println(trie.contains(l));*/

	}

}
