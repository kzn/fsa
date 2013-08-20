package name.kazennikov.dafsa.obsolete;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;

/**
 * Character based trie. Based on int-trie (so consumes 4 bytes per label).
 * 
 * @author Anton Kazennikov
 *
 */
public class CharTrie extends IntTrie {
	
	public void add(String s, int f) {
		TIntArrayList l = new TIntArrayList(s.length());
		for(int i = 0; i != s.length() ; i++)
			l.add(s.charAt(i));
		
		add(l, f);
		
	}
	
	public class Walker implements CharFSAWalker {

		@Override
		public void walk(CharSequence src, int start, int end, Processor proc) {
			int state = 1;
			
			for(int i = start; i < end; i++) {
				TIntSet finals = getFinals(state);
				if(finals != null && !finals.isEmpty())
					proc.parse(src, start, i, finals);
				
				int nextState = getNext(state, src.charAt(i));
				
				if(nextState == 0)
					return;
				
				state = nextState;
			}
		}
		
	}
	
	public CharFSAWalker makeCharWalker() {
		return new Walker();
	}
}
