package elie.dataGenerator.Representations;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import elie.dataGenerator.tools.*;

public class user {
	private ArrayList<Integer> trace;
	private int repSize;
	private Representation rep;
	private int position;
	
	
	
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public ArrayList<Integer> getTrace() {
		return trace;
	}
	public void setTrace(ArrayList<Integer> trace) {
		this.trace = trace;
	}
	public int getRepSize() {
		return repSize;
	}
	public void setRepSize(int repSize) {
		this.repSize = repSize;
	}
	public ArrayList<Boolean> getRep() {
		return rep;
	}
	public void setRep(Representation rep) {
		this.rep = rep;
	}
	public user(ArrayList<Integer> trace, Representation rep) {
		super();
		this.trace = trace;
		this.rep = rep;
		this.repSize = rep.size();
	}
	
	public void move(int item, ArrayList<Integer> filtre){
		this.trace.add(item);
		this.position=item;
		this.rep= toolbox.updateRep(this.rep,filtre);
	}
	
	
}
