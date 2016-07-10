package thibault.simulationBandit.testBis;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;




public class LowerBond extends Policy{


	int nbPlayed=1;
	
	public LowerBond( ) {
		super();

	}

	public void reinitPolicy(){
		super.reinitPolicy();
		nbPlayed=1;
	}

	@Override
	public void updateRewards(){

	}

	
	@Override
	public HashSet<Arm> select(int nb){
		return null;
	}
	
	public String toString(){
		return "UpperBond";
	}
}




