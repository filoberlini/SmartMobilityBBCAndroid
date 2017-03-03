package bbc.unibo.it.smartmoblitybbc.model;

import java.util.List;

import bbc.unibo.it.smartmoblitybbc.model.interfaces.IInfrastructureNode;
import bbc.unibo.it.smartmoblitybbc.model.interfaces.INodePath;

public class NodePath implements INodePath {
	
	private List<IInfrastructureNode> pathNodes;
	
	public NodePath(List<IInfrastructureNode> pathNodes){
		this.pathNodes = pathNodes;
	}

	@Override
	public List<IInfrastructureNode> getPathNodes() {
		return this.pathNodes;
	}

	@Override
	public void removeFirstNode() {
		this.pathNodes.remove(0);
	}

	@Override
	public void setPath(List<IInfrastructureNode> path) {
		this.pathNodes = path;
	}

    @Override
    public void printPath(){
        String s = "";
		for(IInfrastructureNode node : this.pathNodes){
            s+=" "+node.getNodeID();
        }
		System.out.println("PATH "+s);
    }

	@Override
	public String getStringPath() {
		String s = "";
		for(IInfrastructureNode node : this.pathNodes){
			s+=" "+node.getNodeID();
		}
		return s;
	}

}
