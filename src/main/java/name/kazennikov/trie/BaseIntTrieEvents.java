package name.kazennikov.trie;

/**
 * Events interface
 * Announces the transition of the fsm
 * @author Anton Kazennikov
 *
 */
public interface BaseIntTrieEvents {
	public void transition(int src, int label, int dest);
	public void setFinal(int state);
	
}
