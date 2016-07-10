package thibault.simulationBandit;
import java.util.ArrayList;
import java.util.HashSet;



public abstract class Policy {
	ArrayList<Arm> arms;
	HashSet<Arm> lastSelected;
	
	public Policy(){
		this.arms=new ArrayList<Arm>();
		this.lastSelected=new HashSet<Arm>();
	}
	
	public abstract HashSet<Arm> select(int nb);
	
	public void addArm(Arm a){
		arms.add(a);
	}
	public void removeArm(Arm a){
		arms.remove(a);
		this.lastSelected=new HashSet<Arm>();
	}
	
	public void reinitPolicy(){
		this.arms=new ArrayList<Arm>();
	}
	
	public abstract void updateRewards();
	
}



