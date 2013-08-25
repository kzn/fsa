package name.kazennikov.fsa;

/**
 * Events interface to IntFSA with list of integers as final value
 * 
 * The interpretation of this list is left for target FSA
 * @author Anton Kazennikov
 *
 */
public interface IntFSAObjectEvents<E> extends IntFSAEvents {
	public void setFinalValue(E object);

}
