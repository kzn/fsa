package name.kazennikov.dafsa;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import name.kazennikov.trove.TIntDeque;

import java.lang.reflect.Array;

/**
 * Created on 8/30/15.
 *
 * @author Anton Kazennikov
 */
public class CompactIntTrie {

	public static final int RESERVED = Integer.MIN_VALUE;


    /**
     * Chunked memory allocator.
     * Splits given memory area into chunks nd tracks
     * allocation, so the chunks could be freed and reused.
     *
     *
     */
	public static class ChunkAllocator {

        // actual data
		int[] data;
		// pointer to unallocated area
		int unallocPtr;

		// buckets of free chunks
		TIntDeque[] q = new TIntDeque[0];



		public ChunkAllocator(int size) {
			data = new int[size];
		}

        /**
         * Get queue of free chunks of bucket
         * @param bucketIndex index of the bucket
         * @return
         */
		public TIntDeque queueFor(int bucketIndex) {

			if(bucketIndex >= q.length) {
				int oldSize = q.length;
				q = realloc(q, TIntDeque.class, bucketIndex + 1);

				for(int i = oldSize; i < q.length; i++) {
					q[i] = new TIntDeque(16);
				}
			}

			return q[bucketIndex];
		}

        /**
         * Ensure of that allocation area has enough free space for a new block
         *
         * @param dataSize size of the requested block
         */
		public void ensureFree(int dataSize) {
			if(data.length - unallocPtr > dataSize)
				return;

			int newSize = data.length;

			while(newSize - unallocPtr < dataSize) {
				newSize = newSize + (newSize >> 2);
			}

			data = realloc(data, newSize);
		}


        /**
         * Allocate a chunk
         * @param bucketIndex requested bucketIndex
         * @return block pointer
         */
        public int alloc(int bucketIndex) {
            TIntDeque q = queueFor(bucketIndex);

            if(!q.isEmpty()) {
                return q.pollFirst();
            }

            int dataSize = bucketSize(bucketIndex);


            ensureFree(dataSize);

            int ptr = unallocPtr;
            unallocPtr += dataSize;

            return ptr;
        }

        /**
         * Free (deallocate) a chunk
         * @param ptr chunk pointer
         * @param bucketIndex bucket index
         */
		public void free(int ptr, int bucketIndex) {
			TIntDeque q = queueFor(bucketIndex);
			q.addLast(ptr);
		}


		public int getUnallocPtr() {
			return unallocPtr;
		}


        /**
         * Bucket size in ints
         * @param bucketIndex bucket index
         * @return
         */
		public int bucketSize(int bucketIndex) {
            int trCount = 1 << (bucketIndex - 1);
            return 1 + trCount*2;
        }

        /**
         * Bucket index by data size
         * @param dataSize
         * @return
         */
        public static int bucket(int dataSize) {
            if(dataSize == 0)
                return 0;

            int log = 32 - Integer.numberOfLeadingZeros(dataSize);

            // power of two
            if(Integer.numberOfTrailingZeros(dataSize) == log - 1)
                return log;

            return log + 1;
        }






    }


    /*
        2i - label
        2i+1 - next state
     */
    ChunkAllocator m;

    int start;
    int stateCount;


    public CompactIntTrie() {
		m = new ChunkAllocator(256);
		start = addState(true);
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
     * @param reserve should we reserve memory for a transition? true on known non-leaf states
	 * @return state index
	 */
	public int addState(boolean reserve) {
		stateCount++;
		int bucket = ChunkAllocator.bucket(reserve? 1 : 0);
		int pos = m.alloc(bucket); // header

		m.data[pos] = reserve? RESERVED : 0;

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
		int oldBucket = ChunkAllocator.bucket(Math.abs(trCount));
		int newBucket = ChunkAllocator.bucket(Math.abs(trCount) + 1);

		// same bucket
		if(oldBucket == newBucket || trCount == RESERVED) {

			if(trCount == RESERVED) {
				m.data[state] = 0;
				trCount = 0;
			}

			m.data[state]++;
			m.data[state + 1 + trCount *2] = label;
			m.data[state + 1 + trCount *2 + 1] = next;
			return state;
		}


		// failed to find reserved transitions, relocating the state
		int newState =  m.alloc(newBucket);
		m.data[newState] = trCount + 1; // inc transition count
		System.arraycopy(m.data, state + 1, m.data, newState + 1, trCount*2); // relocateState
		m.data[newState + trCount*2 + 1] = label;
		m.data[newState + trCount*2 + 1 + 1] = next;
		m.free(state, oldBucket);

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
			int next = addState(pos != l.size() - 1);
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

	public int usedSize() {
	    return m.data.length - m.unallocPtr;
    }

}
