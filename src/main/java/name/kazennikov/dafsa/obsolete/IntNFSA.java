package name.kazennikov.dafsa.obsolete;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;



/**
 * Int-based finite state transducer (FST)
 * It is a FSA based on pairs (in, out)
 * 
 * @author Anton Kazennikov
 *
 */
public class IntNFSA {
	TIntObjectHashMap<TIntSet> finals = new TIntObjectHashMap<TIntSet>();
	
	// map (current, inChar) -> (index, length)
	final TLongLongHashMap stateHash = new TLongLongHashMap();
	
	// using 2 arrays instead of array of pairs (outChar, nextState)
	final TIntArrayList outChars = new TIntArrayList();
	final TIntArrayList nextStates = new TIntArrayList();
	

	public void addTransition(int srcState, char input, char output, int destState) {
		long key = key(srcState, input);
		stateHash.adjustOrPutValue(key, 1, ((long)outChars.size() << 32) + 1);
		outChars.add(output);
		nextStates.add(destState);
	}
	
	long key(int state, int input){
		return ((long)input << 32) + state;
	}
	

	
	/**
	 * Get final features of a state
	 * @return null, if state doesn't have final features
	 */
	public TIntSet getFinals(int state) {
		return finals.get(state);
	}
	
	/**
	 * Add a final feature to the state
	 * @param state state number
	 * @param fin final feature
	 */
	public void addFinal(int state, int fin) {
		TIntSet stateFinals = finals.get(state);
		
		if(stateFinals == null) {
			stateFinals = new TIntHashSet();
			finals.put(state, stateFinals);
		}
		
		stateFinals.add(fin);
	}
	
	public int getState(long key) {
		return (int)(key >>> 32);
	}
	
	public long getTransitionsInfo(int state, int in) {
		return stateHash.get(key(state, in));
	}
	
	public int getTransitionsStart(long key) {
		return (int)(key >>> 32);
	}
	
	public int getTransitionsLength(long key) {
		return (int)(key & 0xFFFFFFFFL);
	}
	
	public int getTransitionOut(int index) {
		return outChars.get(index);
	}
	
	public int getTransitionNext(int index) {
		return nextStates.get(index);
	}



	
	
	
	
	
	public static class Builder implements IntTrie.Builder<IntNFSA> {
		IntNFSA nfsa;
		int state = 0;
		

		@Override
		public void states(int states) {
			nfsa = new IntNFSA();
		}

		@Override
		public void state(int state) {
			this.state = state;
		}

		@Override
		public void finals(int n) {
		}

		@Override
		public void stateFinal(int fin) {
			nfsa.addFinal(state, fin);
		}

		@Override
		public void transitions(int n) {
		}

		@Override
		public void transition(int input, int dest) {
			char in = (char)( input >> 16);
			char out = (char) (input & 0xffff);
			nfsa.addTransition(state, in, out, dest);
		}

		@Override
		public IntNFSA build() {
			return nfsa;
		}

		@Override
		public void startState() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endState() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startFinals() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endFinals() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startTransitions() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endTransitions() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startStates() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endStates() {
			// TODO Auto-generated method stub
			
		}
		
	}


	/**
	 * FST walker. There is 2 modes for walking this FST:<ul>
	 * <li> exact case
	 * <li> ignore case - assumes that given string is already lowercased
	 * </ul>
	 * @author Anton Kazennikov
	 *
	 */
	public class Walker implements CharFSTWalker {
		boolean ignoreCase;
		
		public Walker(boolean ignoreCase) {
			this.ignoreCase = ignoreCase;
		}

		
		
		protected void walkIterativeInternal(CharSequence s, StringBuilder sb, int startIndex, int endIndex, int currentIndex, 
				int state, char ch, Processor parseProcessor) {
			long value = getTransitionsInfo(state, ch);
			int start = getTransitionsStart(value);
			int end = start + getTransitionsLength(value);
			
			while(start < end) {
				
				char outCh = (char) getTransitionOut(start);
				int nextState = getTransitionNext(start);
				if(outCh != 0)
					sb.append(outCh);
				int nextIndex = currentIndex;
				// do not jump to next char on null char walk
				if(ch != 0) {
					nextIndex = currentIndex != endIndex? currentIndex + 1: endIndex;
				}
				walkIterative(s, sb, startIndex, endIndex, nextIndex, nextState, parseProcessor);
				
				
				if(outCh != 0)
					sb.deleteCharAt(sb.length() - 1);

				start++;
			}
		}
		
		public void walkIterative(CharSequence s, StringBuilder sb, int startIndex, int endIndex, int currentIndex, 
				int state, Processor parseProcessor) {
			TIntSet fin = getFinals(state);

				if(fin != null && !fin.isEmpty()) {
					parseProcessor.parse(s, sb, startIndex, currentIndex, fin);
				}
				
				char ch = currentIndex < endIndex? s.charAt(currentIndex) : 0;
								
				walkIterativeInternal(s, sb, startIndex, endIndex, currentIndex, state, ch, parseProcessor);
				
				if(ignoreCase) {
					char toUpper = Character.toUpperCase(ch);
					if(toUpper != ch) {
						walkIterativeInternal(s, sb, startIndex, endIndex, currentIndex, state, toUpper, parseProcessor);
					}
				}
				
				// for single-pass morphan on fst only
				walkIterativeInternal(s, sb, startIndex, endIndex, currentIndex, state, (char)0, parseProcessor);
		}


		@Override
		public void walk(CharSequence src, int start, int end, Processor proc) {
			walkIterative(src, new StringBuilder(), start, end, start, 1, proc);
		}
		
	}
	
	public CharFSTWalker makeFSTWalker(boolean ignoreCase) {
		return new Walker(ignoreCase);
	}
}
