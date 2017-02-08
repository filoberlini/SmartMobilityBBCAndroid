package bbc.unibo.it.smartmoblitybbc.model.interfaces;

/**
 * Interface that models an observer for a GPS source
 * @author BBC
 *
 */
public interface IGPSObserver {
	
	/**
	 * coordinates are passed to the observer
	 * @param coordinates
	 */
	void notifyGps(ICoordinates coordinates);
}
