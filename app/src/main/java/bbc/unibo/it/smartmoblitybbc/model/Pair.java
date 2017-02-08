package bbc.unibo.it.smartmoblitybbc.model;

import bbc.unibo.it.smartmoblitybbc.model.interfaces.IPair;

public class Pair<X, Y> implements IPair<X, Y> {

	private X first;
	private Y second;
	
	public Pair(X first, Y second){
		this.first = first;
		this.second = second;
	}
	
	@Override
	public X getFirst() {
		return this.first;
	}

	@Override
	public Y getSecond() {
		return this.second;
	}

}
