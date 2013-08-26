package name.kazennikov.dafsa;

import gnu.trove.list.array.TIntArrayList;

public class TroveUtils {
	public static void expand(TIntArrayList dest, CharSequence s) {
		dest.clear();
		
		for(int i = 0; i < s.length(); i++) {
			dest.add(s.charAt(i));
		}
	}
	
	public static void expand(TIntArrayList dest, CharSequence wordForm, CharSequence  lemma) {
		dest.clear();
		int maxLength = Math.max(wordForm.length(), lemma.length());
		
		for(int i = 0; i < maxLength; i++) {
			char wfChar = i < wordForm.length()? wordForm.charAt(i) : 0;
			char lemmaChar = i < lemma.length()? lemma.charAt(i) : 0;
			
			int destSymbol = (lemmaChar << 16) + wfChar;
			dest.add(destSymbol);
		}
	}


}
