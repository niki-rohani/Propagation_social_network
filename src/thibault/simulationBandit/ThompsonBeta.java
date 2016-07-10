package thibault.simulationBandit;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;



public class ThompsonBeta extends Policy{

	public ThompsonBeta() {
		super();
	}
	


	@Override
	public void updateRewards() {
			for(Arm arm:lastSelected){
				arm.sumRewards+=arm.lastReward;
				arm.numberPlayed++;
			}
			samplePrior();
			if(lastSelected.size()>0){
				Collections.sort(arms,new scorePriorComparator());
			}
	}
	
	
	public class scorePriorComparator implements Comparator<Arm>
	{
		public int compare(Arm arm1,Arm arm2){
			double r1=arm1.thompsonPrior;
			double r2=arm2.thompsonPrior;
			if(r1>r2){
				return -1;
			}
			if(r1<r2){
				return 1;
			}
			return 0;
		}
	}
	
	public void samplePrior(){
		for(Arm arm:arms){
			Distributions simValue= new Distributions();
			arm.thompsonPrior=simValue.nextBeta(arm.S+1, arm.F+1);
		}
	}
	

	
	@Override
	public HashSet<Arm> select(int nb){
		
		HashSet<Arm> ret=new HashSet<Arm>();
		
		for(int i=0;i<nb;i++){
		ret.add(arms.get(i));
		}
		lastSelected=ret;
		return ret;
	}


	
	
	public String toString(){
		return "ThompsonBeta";
	}

}
