package name.kazennikov.fsa;

/**
 * Event-based interface to FSA. Can be used to write FSA to other format.
 * This interface needs to be extended with specific final state data type.
 * 
 * Current interface assumes that there is some other method that will set internal final value
 * before setFinal() is called
 * @author Anton Kazennikov
 *
 */
public interface UnlabeledIntFSAEventHandler {

	/**
	 * Announce that that current state changed to given state number.
	 * This also resets the final value stored by events processor
	 * @param state state number
	 */
	public void startState(int state, int date);

	/**
	 * Set final value of the current state
	 */
	public void setFinal();

	/**
	 * Add transition to current state
	 * 
	 * @param destState destination state number
	 */
	public void addTransition(int destState);
	
	/**
	 * Announce end of current state - at this point current state is fully built and
	 * will be not changed by the writing algorithm
	 */
	public void endState();


}
