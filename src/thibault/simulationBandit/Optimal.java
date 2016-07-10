package thibault.simulationBandit;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;


public class Optimal extends Policy{

	int nbPlayed=1;
	
	@Override
	public void updateRewards(){
		if (nbPlayed==1){
			Collections.sort(arms,new meanComparator());
		}
		
		for(Arm arm:lastSelected){
			arm.sumRewards+=arm.lastReward;
			arm.numberPlayed++;
			
		}
		Collections.sort(arms,new meanComparator());
		nbPlayed++;
		
	}
	
	public class meanComparator implements Comparator<Arm>
	{
		public int compare(Arm arm1,Arm arm2){
			double r1=arm1.mean;
			double r2=arm2.mean;
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
		return "Optimal";
	}

}
