package name.kazennikov.dafsa;

import gnu.trove.list.array.TIntArrayList;


public class CharTrie extends IntTrie {
	
	public void add(String s, int f) {
		TIntArrayList l = new TIntArrayList(s.length());
		for(int i = 0; i != s.length() ; i++)
			l.add(s.charAt(i));
		
		add(l, f);
		
	}
}
