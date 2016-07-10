package experiments;

import java.util.HashMap;
import java.util.TreeMap;

import cascades.Cascade;
import propagationModels.*;
import java.util.ArrayList;
import core.User;
public class Hyp {
	//private Cascade ref;
	private PropagationModel model;
	private PropagationStruct pstruct;
	private int nbIterations; // nb of runs of each inference process 
	private ArrayList<TreeMap<Long,HashMap<String,Double>>> contaminations;
	private TreeMap<Long,HashMap<String,Double>> ref;
	private TreeMap<Long,HashMap<String,Double>> init;
	public Hyp(PropagationStruct pstruct,PropagationModel model, int nbIterations){
		//ref=c.getCascade();
		this.model=model;
		this.pstruct=pstruct;
		ref=new TreeMap<Long,HashMap<String,Double>>(pstruct.getInfections());
		init=pstruct.getInitContaminated();
		contaminations=new ArrayList<TreeMap<Long,HashMap<String,Double>>>();
		int it=0;
		this.nbIterations=nbIterations;
		System.out.println("Propagation selon "+model);
		System.out.print("iteration = ");
		while(it<nbIterations){
			pstruct.setInfections(new TreeMap<Long,HashMap<String,Double>>()); 
			System.out.print(it+" ");
			//pstruct.setInfections(new TreeMap<Long,HashMap<String,Double>>());
			model.infer(pstruct);
			contaminations.add(pstruct.getInfections());
			pstruct.setInfections(ref);
			//System.out.println(pstruct.getContaminated());
			it++;
		}
		System.out.println();
	}
	public TreeMap<Long,HashMap<String,Double>> getRef(){
		return(ref);
	}
	public TreeMap<Long,HashMap<String,Double>> getInit(){
		return(init);
	}
	public PropagationModel getModel(){
		return(model);
	}
	public PropagationStruct getStruct(){
		return(pstruct);
	}
	public ArrayList<TreeMap<Long,HashMap<String,Double>>> getContaminations(){
		return(contaminations);
	}
}
