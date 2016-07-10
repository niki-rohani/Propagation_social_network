package thibault.simulationBandit;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;




public class UCBV extends Policy{

int nbPlayed=1;
double b=1.0;

	public UCBV( ) {
		super();
	}
	
	

	public void reinitPolicy(){
		super.reinitPolicy();
		nbPlayed=1;
	}

	@Override
	public void updateRewards(){
		for(Arm arm:lastSelected){
			arm.sumRewards+=arm.lastReward;
			arm.sumSqrtRewards+=arm.lastReward*arm.lastReward;
			arm.numberPlayed++;
		}
	
		if(lastSelected.size()>0){
			nbPlayed++;
			Collections.sort(arms,new scoreComparator());
		}
	}
	
	public class scoreComparator implements Comparator<Arm>
	{
		public int compare(Arm arm1,Arm arm2){
			double empAverage=arm1.sumRewards/(1.0*arm1.numberPlayed);
			double empVariance=arm1.sumSqrtRewards/(1.0*arm1.numberPlayed)-empAverage*empAverage;
			double r1=empAverage+Math.sqrt(2*empVariance*Math.log(nbPlayed)/(1.0*arm1.numberPlayed))+b*Math.log(nbPlayed)/(2.0*arm1.numberPlayed);
			empAverage=arm2.sumRewards/(1.0*arm2.numberPlayed);
			empVariance=arm2.sumSqrtRewards/(1.0*arm2.numberPlayed)-empAverage*empAverage;
			double r2=empAverage+Math.sqrt(2*empVariance*Math.log(nbPlayed)/(1.0*arm2.numberPlayed))+b*Math.log(nbPlayed)/(2.0*arm2.numberPlayed);
			
			if(r1>r2){
				return -1;
			}
			if(r1<r2){
				return 1;
			}
			return 0;
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
		return "UCBV";
	}

}

