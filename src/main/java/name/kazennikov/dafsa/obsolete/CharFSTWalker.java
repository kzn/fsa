package name.kazennikov.dafsa.obsolete;

import gnu.trove.set.TIntSet;

/**
 * Walker interface for char based FST. Walks the FST on subsequence
 * and returns found feature sets
 * 
 * @author Anton Kazennikov
 *
 */
public interface CharFSTWalker {
	/**
	 * Successful match processor of the walker
	 * 
	 * @author Anton Kazennikov
	 *
	 */
	public static interface Processor {
		/**
		 * Process successful string match
		 * 
		 * @param src source char sequence
		 * @param out output char sequence
		 * @param start start of the matched part
		 * @param end end of the matcher part
		 * @param feats set of feature ids
		 */

		public void parse(CharSequence src, StringBuilder out, int start, int end, TIntSet feats);
	}
	
	
	/**
	 * Walk a char sequence
	 * 
	 * @param src source char sequence
	 * @param start start index of the walked subsequence
	 * @param end end index of the walked subsequence
	 * @param proc processor to process matched subsequence
	 */
	public void walk(CharSequence src, int start, int end, Processor proc);
}
