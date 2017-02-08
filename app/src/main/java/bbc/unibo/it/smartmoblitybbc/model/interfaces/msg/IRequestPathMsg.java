package bbc.unibo.it.smartmoblitybbc.model.interfaces.msg;

import bbc.unibo.it.smartmoblitybbc.model.interfaces.IInfrastructureNode;

/**
 * Interface that models a request path msg from the user to the server
 * @author BBC
 *
 */
public interface IRequestPathMsg extends IMobilityMsg {
	
	/**
	 * gets the starting node of the path
	 * @return starting node of the path
	 */
	IInfrastructureNode getStartingNode();
	
	/**
	 * gets the ending node of the path
	 * @return ending node of the path
	 */
	IInfrastructureNode getEndingNode();
}
