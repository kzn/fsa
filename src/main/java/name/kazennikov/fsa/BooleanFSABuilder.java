package name.kazennikov.fsa;

import gnu.trove.list.TIntList;
import name.kazennikov.dafsa.IntDAFSABoolean;
import name.kazennikov.fsa.walk.WalkFSABoolean;

/**
 * Simple FSA builder helper class
 */
public class BooleanFSABuilder {
    IntDAFSABoolean fsa = new IntDAFSABoolean();

    public BooleanFSABuilder() {
        fsa.setFinalValue(true);
    }

    public void add(TIntList seq) {
        fsa.add(seq);
    }

    public void addMinWord(TIntList seq) {
        fsa.addMinWord(seq);
    }

    public int size() {
        return fsa.size();
    }

    public WalkFSABoolean build() {
        WalkFSABoolean.Builder builder = new WalkFSABoolean.Builder();
        fsa.emit(builder);
        return builder.build();
    }

	public IntDAFSABoolean fsa() {
		return fsa;
	}
}
