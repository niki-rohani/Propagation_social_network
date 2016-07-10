package thibault.simulationBandit.testBis;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;




public class UCB extends Policy{

	public double rho;
	int nbPlayed=1;
	
	public UCB( ) {
		super();
		this.rho=1;
	}
	
	public UCB(double rho ) {
		super();
		this.rho=rho;
	}

	public void reinitPolicy(){
		super.reinitPolicy();
		nbPlayed=1;
	}

	@Override
	public void updateRewards(){
		for(Arm arm:lastSelected){
			arm.sumRewards+=arm.lastReward;
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
			double r1=(arm1.sumRewards/(1.0*arm1.numberPlayed))+(Math.sqrt(rho*Math.log(nbPlayed)/(1.0*arm1.numberPlayed)));
			double r2=(arm2.sumRewards/(1.0*arm2.numberPlayed))+(Math.sqrt(rho*Math.log(nbPlayed)/(1.0*arm2.numberPlayed)));
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
		return "UCB";
	}
}




