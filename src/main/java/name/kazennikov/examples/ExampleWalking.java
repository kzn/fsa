package name.kazennikov.examples;

import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

import name.kazennikov.dafsa.IntDAFSAInt;
import name.kazennikov.dafsa.TroveUtils;
import name.kazennikov.fsa.walk.WalkFSAInt;

public class ExampleWalking {
	
	public static void walk(WalkFSAInt fsa, String word) {
		int state = 0;
		
		for(int index = 0; index < word.length(); index++) {
			state = fsa.next(state, word.charAt(index));
			
			if(state == -1)
				return;
		}
		
		System.out.printf("Walked '%s' successfully, state=%d, final=%s%n", word, state, Arrays.toString(fsa.getFinals(state)));
	}
	
	public static void main(String[] args) {
		IntDAFSAInt fsa = new IntDAFSAInt();
		fsa.setFinalValue(1);
		TIntArrayList a = new TIntArrayList();
		TroveUtils.expand(a, "fox");
		fsa.addMinWord(a);
		TroveUtils.expand(a, "box");
		fsa.addMinWord(a);
		TroveUtils.expand(a, "boxes");
		fsa.addMinWord(a);
		TroveUtils.expand(a, "foxes");
		fsa.addMinWord(a);
		
		WalkFSAInt.Builder builder = new WalkFSAInt.Builder();
		fsa.emit(builder);
		WalkFSAInt walkFSA = builder.build();
		
		walk(walkFSA, "foxes");

		
	}

}
