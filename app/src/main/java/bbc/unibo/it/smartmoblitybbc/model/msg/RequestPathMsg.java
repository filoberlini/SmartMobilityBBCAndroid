package bbc.unibo.it.smartmoblitybbc.model.msg;

import bbc.unibo.it.smartmoblitybbc.model.interfaces.IInfrastructureNode;
import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.IRequestPathMsg;

public class RequestPathMsg implements IRequestPathMsg {
	
	private String msgId;
	private IInfrastructureNode startingNode;
	private IInfrastructureNode endingNode;
	private String userID;
	
	public RequestPathMsg(String msgId, IInfrastructureNode startingNode, IInfrastructureNode endingNode, String userID){
		this.msgId = msgId;
		this.startingNode = startingNode;
		this.endingNode = endingNode;
		this.userID = userID;
	}

	@Override
	public String getMsgID() {
		return this.msgId;
	}

	@Override
	public IInfrastructureNode getStartingNode() {
		return this.startingNode;
	}

	@Override
	public IInfrastructureNode getEndingNode() {
		return this.endingNode;
	}

	@Override
	public String getUserID() {
		return this.userID;
	}
}
