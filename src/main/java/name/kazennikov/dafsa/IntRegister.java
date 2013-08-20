package name.kazennikov.dafsa;

import gnu.trove.set.hash.TIntHashSet;

public class IntRegister extends TIntHashSet {
	
	
	public IntRegister() {
		no_entry_value = -1;
	}
	
	
	public int hash(int state) {
		return state;
	}
	
	public boolean equals(int state1, int state2) {
		return state1 == state2;
	}
	
	
	
    /**
     * Locates the index of <tt>val</tt>.
     *
     * @param val an <code>int</code> value
     * @return the index of <tt>val</tt> or -1 if it isn't in the set.
     */
	@Override
    protected int index(int val) {
        int hash, probe, index, length;

        final byte[] states = _states;
        final int[] set = _set;
        length = states.length;
        hash = hash(val) & 0x7fffffff;
        index = hash % length;
        byte state = states[index];

        if (state == FREE)
            return -1;

        if (state == FULL && hash(val) == hash(set[index]) && equals(set[index],val))
            return index;

        return indexRehashed(val, index, hash, state);
    }

    int indexRehashed(int key, int index, int hash, byte state) {
        // see Knuth, p. 529
        int length = _set.length;
        int probe = 1 + (hash % (length - 2));
        final int loopIndex = index;

        do {
            index -= probe;
            if (index < 0) {
                index += length;
            }
            state = _states[index];
            //
            if (state == FREE)
                return -1;

            //
            if (state != REMOVED && hash(key) == hash(_set[index]) && equals(key,_set[index]))
                return index;
        } while (index != loopIndex);

        return -1;
    }

    /**
     * Locates the index at which <tt>val</tt> can be inserted.  if
     * there is already a value equal()ing <tt>val</tt> in the set,
     * returns that value as a negative integer.
     *
     * @param val an <code>int</code> value
     * @return an <code>int</code> value
     */
    @Override
	protected int insertKey( int val ) {
        int hash, index;

        hash = hash(val) & 0x7fffffff;
        index = hash % _states.length;
        byte state = _states[index];

        consumeFreeSlot = false;

        if (state == FREE) {
            consumeFreeSlot = true;
            insertKeyAt(index, val);

            return index;       // empty, all done
        }

        if (state == FULL && hash(val) == hash(_set[index]) && equals(_set[index], val)) {
            return -index - 1;   // already stored
        }

        // already FULL or REMOVED, must probe
        return insertKeyRehash(val, index, hash, state);
    }

    int insertKeyRehash(int val, int index, int hash, byte state) {
        // compute the double hash
        final int length = _set.length;
        int probe = 1 + (hash % (length - 2));
        final int loopIndex = index;
        int firstRemoved = -1;

        /**
         * Look until FREE slot or we start to loop
         */
        do {
            // Identify first removed slot
            if (state == REMOVED && firstRemoved == -1)
                firstRemoved = index;

            index -= probe;
            if (index < 0) {
                index += length;
            }
            state = _states[index];

            // A FREE slot stops the search
            if (state == FREE) {
                if (firstRemoved != -1) {
                    insertKeyAt(firstRemoved, val);
                    return firstRemoved;
                } else {
                    consumeFreeSlot = true;
                    insertKeyAt(index, val);
                    return index;
                }
            }

            if (state == FULL && hash(val) == hash(_set[index]) && equals(_set[index],val)) {
                return -index - 1;
            }

            // Detect loop
        } while (index != loopIndex);

        // We inspected all reachable slots and did not find a FREE one
        // If we found a REMOVED slot we return the first one found
        if (firstRemoved != -1) {
            insertKeyAt(firstRemoved, val);
            return firstRemoved;
        }

        // Can a resizing strategy be found that resizes the set?
        throw new IllegalStateException("No free or removed slots available. Key set full?!!");
    }
    
    void insertKeyAt(int index, int val) {
        _set[index] = val;  // insert value
        _states[index] = FULL;
    }
    
    public int get(int key) {
    	int index = index(key);
    	
    	if(index >= 0)
    		return _set[index];
    	
    	return no_entry_value;
    	
    }
    
    @Override
	public boolean remove( int val ) {
        int index = index(val);
        if ( index >= 0 && _set[index] == val) {
            removeAt( index );
            return true;
        }
        return false;
    }




}
