package name.kazennikov.dafsa.obsolete;

import gnu.trove.list.array.TCharArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;

public class IntNFSAv2 {
	final ArrayList<TIntSet> finals = new ArrayList<TIntSet>();

	int[] stateStart;
	int[] stateEnd;

	final TCharArrayList in = new TCharArrayList();
	final TCharArrayList out = new TCharArrayList();
	final TIntArrayList next = new TIntArrayList();


	public void states(int states) {
		stateStart = new int[states + 1];
		stateEnd = new int[states + 1];
	}



	public void addFinal(int state, int fin) {
		TIntSet f = finals.get(state);
		if(f == null) {
			f = new TIntHashSet();
			finals.set(state, f);
		}
		f.add(fin);
	}


	public void addTransition(int srcState, char input, char output, int destState) {
		if(stateEnd[srcState] == 0) {
			stateStart[srcState] = next.size();
			stateEnd[srcState] = next.size();
		}

		stateEnd[srcState]++;

		in.add(input);
		out.add(output);
		next.add(destState);
	}

	long key(int state, char input){
		return ((long)input << 32) + state;
	}

	public static class IntNFSABuilder implements IntTrie.Builder<IntNFSAv2> {
		IntNFSAv2 nfsa;
		int state = 0;


		@Override
		public void states(int states) {
			nfsa = new IntNFSAv2();
			nfsa.stateStart = new int[states];
			nfsa.stateEnd = new int[states];
		}

		@Override
		public void state(int state) {
			this.state = state;
			nfsa.finals.add(null);
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
		public IntNFSAv2 build() {
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
	
	public Walker makeWalker() {
		return new Walker();
	}


	public class Walker implements CharFSTWalker {
		final StringBuilder sb = new StringBuilder();

		public void walkIterative(CharSequence s, int startIndex, int endIndex,
				int currentIndex, int state, CharFSTWalker.Processor morphProc) {
			TIntSet fin = finals.get(state);

			if(fin != null && fin.size() > 0) {
				morphProc.parse(s, sb, startIndex, currentIndex, fin);
			}

			char ch = currentIndex < endIndex? s.charAt(currentIndex) : 0;
			char upperCh = Character.toUpperCase(ch);
			int start = stateStart[state];
			int end = stateEnd[state];

			while(start < end) {
				char inCh = in.get(start);

				if(ch == inCh || upperCh == inCh || inCh == 0) {

					char outCh = out.get(start);
					int nextState = next.get(start);
					if(outCh != 0)
						sb.append(outCh);

					boolean nextIndex = (currentIndex < endIndex) && inCh != 0;
					walkIterative(s, startIndex, endIndex, nextIndex? currentIndex + 1: currentIndex,
							nextState, morphProc);
					if(outCh != 0)
						sb.deleteCharAt(sb.length() -1);
				}

				start++;
			}

			if(ch != 0) {
				ch = 0;

				start = stateStart[state];
				end = stateEnd[state];

				while(start < end) {
					char inCh = in.get(start);

					if(ch == inCh) {

						char outCh = out.get(start);
						int nextState = next.get(start);
						if(outCh != 0)
							sb.append(outCh);

						boolean nextIndex = (currentIndex < endIndex);
						walkIterative(s, startIndex, endIndex, currentIndex,
								nextState, morphProc);
						if(outCh != 0)
							sb.deleteCharAt(sb.length() -1);
					}

					start++;
				}
			}

		}

		@Override
		public void walk(CharSequence s, int startIndex, int endIndex, CharFSTWalker.Processor morphProc){
			walkIterative(s, startIndex, endIndex, startIndex, 0, morphProc);
		}

		//        @Override
		//        public void walk(TExtCharIterator it, Processor proc) {
			//            walkIterative(it, 0, new StringBuilder(), proc);
			//        }

		//        private void walkIterative(TExtCharIterator it, int state, StringBuilder inSB, Processor proc) {
			//            int[] fin = finals.get(state);
			//
			//
			//            if(fin != null && fin.length > 0) {
				//                proc.parse(inSB, sb, 0, inSB.length(), fin);
				//            }
			//
			//            int maxChar = 0;
			//
			//            int pos = it.getPos();
		//            char ch = it.hasNext()? it.next() : 0;
		//            if(ch != 0) {
		//                inSB.append(ch);
		//                maxChar = ch;
		//            }
		//            char inputChar = ch;
		//            int start = stateStart[state];
		//            int end = stateEnd[state];
		//            char upperCh = Character.toUpperCase(ch);
		//            maxChar = upperCh > maxChar? upperCh : maxChar;
		//
		//            while(start < end) {
		//                char inCh = (char) in.get(start);
		//
		//                if(inCh > maxChar)
		//                    break;
		//
		//                if(inCh == ch || inCh == upperCh) {
		//                char outCh = (char) out.get(start);
		//
		//                int nextState = next.get(start);
		//                if(outCh != 0)
		//                    sb.append(outCh);
		//                walkIterative(it, nextState, inSB, proc);
		//                if(outCh != 0)
		//                    sb.deleteCharAt(sb.length() -1);
		//                }
		//
		//                start++;
		//            }
		//
		//            if(inputChar != 0) {
		//                inSB.setLength(inSB.length() - 1);
		//                it.setPos(pos);
		//            }
		//
		//
		//            // for single-pass morphan on fst only
		//            if(inputChar != 0) {
		//                ch = 0;
		//                start = stateStart[state];
		//                end = stateEnd[state];
		//
		//                while(start < end) {
		//                    char inCh = in.get(start);
		//                    if(inCh != 0)
		//                        break;
		//
		//                    if(inCh == ch) {
		//
		//                    char outCh = (char) out.get(start);
		//                    int nextState = next.get(start);
		//                    if(outCh != 0)
		//                        sb.append(outCh);
		//                    walkIterative(it, nextState, inSB, proc);
		//                    if(outCh != 0)
		//                        sb.deleteCharAt(sb.length() -1);
		//
		//                    }
		//                    start++;
		//                }
		//            }
		//
		//
		//
		//            it.setPos(pos);
		//        }
		//    }

	}


}
