package name.kazennikov.fsa;

public class FSATransition<E> {
	FSAState<E> src;
	int label;
	FSAState<E> dest;
	
	public FSATransition(FSAState<E> src, int label, FSAState<E> dest) {
		this.src = src;
		this.label = label;
		this.dest = dest;
	}
	
	
	public FSAState<E> getSrc() {
		return src;
	}
	
	public FSAState<E> getDest() {
		return dest;
	}
	
	public int getLabel() {
		return label;
	}
	
	public boolean isEpsilon() {
		return label == Constants.EPSILON;
	}
	
	@Override
	public String toString() {
		return String.format("{src=%d, label=%d, dest=%d}", src.getNumber(), label, dest.getNumber());
	}
	

}