package name.kazennikov.examples;

import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;

import name.kazennikov.dafsa.IntDAFSAInt;
import name.kazennikov.dafsa.TroveUtils;

/**
 * Trie example. Example from words:
 * "fox"
 * "box"
 * "boxes"
 * "foxes"
 * @author Anton Kazennikov
 *
 */
public class ExampleTrie {
	public static void main(String[] args) throws IOException {
		IntDAFSAInt fsa = new IntDAFSAInt();
		fsa.setFinalValue(1);
		TIntArrayList a = new TIntArrayList();
		TroveUtils.expand(a, "fox");
		fsa.add(a);
		
		TroveUtils.expand(a, "box");
		fsa.add(a);
		
		fsa.toDot("fox_box.dot");
		
		TroveUtils.expand(a, "boxes");
		fsa.add(a);
		
		fsa.toDot("fox_box_boxes.dot");
		
		
		TroveUtils.expand(a, "foxes");
		fsa.add(a);
		
		fsa.toDot("fox_box_boxes_foxes.dot");

	}

}
