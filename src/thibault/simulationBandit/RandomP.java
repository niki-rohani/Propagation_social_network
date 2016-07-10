package thibault.simulationBandit;
import java.util.Collections;
import java.util.HashSet;


public class RandomP extends Policy{


	@Override
	public void updateRewards(){
		for(Arm arm:lastSelected){
			arm.sumRewards+=arm.lastReward;
			arm.numberPlayed++;
		}
	}
	

	@Override
	public HashSet<Arm> select(int nb){
		Collections.shuffle(arms);
		HashSet<Arm> ret=new HashSet<Arm>();

		for(int i=0;i<nb;i++){
			ret.add(arms.get(i));
		}
		lastSelected=ret;
		return ret;
	}
	
	public String toString(){
		return "RandomPolicy";
	}

}
