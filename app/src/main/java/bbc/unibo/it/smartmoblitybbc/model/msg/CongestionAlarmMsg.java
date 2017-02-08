package bbc.unibo.it.smartmoblitybbc.model.msg;

import bbc.unibo.it.smartmoblitybbc.model.interfaces.IInfrastructureNode;
import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.ICongestionAlarmMsg;

public class CongestionAlarmMsg implements ICongestionAlarmMsg {
	
	private String msgId;
	private IInfrastructureNode firstNode;
	private IInfrastructureNode secondNode;
	
	public CongestionAlarmMsg(String msgId, IInfrastructureNode firstNode, IInfrastructureNode secondNode){
		this.msgId = msgId;
		this.firstNode = firstNode;
		this.secondNode = secondNode;
	}

	@Override
	public String getMsgID() {
		return this.msgId;
	}

	@Override
	public IInfrastructureNode getFirstNode() {
		return this.firstNode;
	}

	@Override
	public IInfrastructureNode getSecondNode() {
		return this.secondNode;
	}

}
