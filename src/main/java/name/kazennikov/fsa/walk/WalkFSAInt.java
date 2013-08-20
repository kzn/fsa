package name.kazennikov.fsa.walk;

import java.util.ArrayList;
import java.util.List;

import name.kazennikov.fsa.Constants;

public class WalkFSAInt extends BaseWalkFSA {
	
	List<int[]> finals = new ArrayList<>();
	public static final int[] EMPTY = new int[0];
	
	
	public int[] getFinals(int state) {
		int[] fin = finals.get(state);
		return fin != null? fin : EMPTY;
	}
	
	public int[] walk(String s) {
		int state = 0;
		for(int i = 0; i < s.length(); i++) {
			state = next(state, s.charAt(i));
			if(state == Constants.INVALID_STATE)
				return EMPTY;
		}
		
		return getFinals(state);
	}
}
