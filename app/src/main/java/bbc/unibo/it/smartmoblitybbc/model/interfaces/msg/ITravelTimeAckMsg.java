package bbc.unibo.it.smartmoblitybbc.model.interfaces.msg;

import bbc.unibo.it.smartmoblitybbc.model.interfaces.IInfrastructureNode;

/**
 * Interface that models the message of the travel time between two nodes
 * @author Matteo
 *
 */
public interface ITravelTimeAckMsg extends IUserMobilityMsg {
	/**
	 * gets the first of the two nodes
	 * @return first node
	 */

	IInfrastructureNode getFirstNode();
	
	/**
	 * gets the second of the two nodes
	 * @return second node
	 */
	IInfrastructureNode getSecondNode();
	
	/**
	 * gets (in seconds) the travel time
	 * @return travel time
	 */
	int getTravelTime();
}
