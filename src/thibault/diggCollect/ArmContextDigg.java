package thibault.diggCollect;

import java.util.TreeMap;

import jgibblda.Inferencer;
import jgibblda.Model;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;



public class ArmContextDigg {
	
	public int Id;
	public int sizeFeatures;
	public RealVector CurrentContext;
	public int numberPlayed=0;
	public double sumRewards=0.0;
	public double sumProdRewards=0.0;
	public double lastReward=0.0;
	public double potentialReward=0.0;
	public double score=0.0;
	
	public ArmContextDigg(int Id, int sizeFeatures) {
		this.Id=Id;
		this.sizeFeatures=sizeFeatures;
		CurrentContext=new ArrayRealVector(new double[sizeFeatures]);
	}

	public void reinitArm(){
		lastReward=0.0;
		score=0.0;
	}
	
	public void getReward(){
		lastReward=0.0;
		lastReward=potentialReward;
	}
	
	public void getPotentialReward(Integer r){
		potentialReward=r;
	}
	
	public void getContext(TreeMap<Integer, Double> treeMap,Inferencer inferencer, boolean useLDA){
		
		if(useLDA){
		String post[]=new String[1];
		post[0]="";
		for(int i : treeMap.keySet()){
			post[0]=i+" "+post[0];
		}
	
		Model newModel = inferencer.inference(post);
		CurrentContext=new ArrayRealVector(newModel.theta[0]);
		
		}
		
		else{
			for(int i =0;i<sizeFeatures;i++){
				CurrentContext.setEntry(i, 0.0);
			}
			for(int i : treeMap.keySet()){
				CurrentContext.setEntry(i, 1.0);
			}
		}
	}
}
