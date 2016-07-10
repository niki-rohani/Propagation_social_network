package elie.dataGenerator.Representations;

import java.util.ArrayList;

import elie.dataGenerator.tools.*;


public class Item {
	private int id;
	private int repSize;
	private ArrayList<Integer> successors;
	private Representation rep;
	private ArrayList<Integer> effect;
	
	public Item(ArrayList<Integer> successors, Representation rep, int id, ArrayList<Integer> effect) {
		this.successors = successors;
		this.rep = rep;
		if (rep!=null){
		this.repSize=rep.size();
		}else{
			this.repSize=0;
		}
		this.effect = effect;
		
	}
	public int getRepSize() {
		return repSize;
	}
	public ArrayList<Integer> getSuccessors() {
		return successors;
	}
	public void setSuccessors(ArrayList<Integer> successors) {
		this.successors = successors;
	}
	public Representation getRep() {
		return rep;
	}
	public void setRep(Representation rep) {
		this.rep = rep;
		this.repSize = rep.size();
	}
	
	
	public ArrayList<Integer> getEffect() {
		return effect;
	}
	public void setEffect(ArrayList<Integer> effect) {
		this.effect = effect;
	}
	
	
	public int getId(){
		return this.id;
	}

	
}
