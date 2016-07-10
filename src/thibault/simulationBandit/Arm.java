package thibault.simulationBandit;
import java.util.ArrayList;


public class Arm {

	public int Id;
	public double mean;
	public double var;
	
	int numberPlayed=1;
	double sumRewards=0.0;
	double sumSqrtRewards=0.0;
	double lastReward=0.0;
	Distributions simValue= new Distributions();
	double thompsonPrior;
	double S=0;
	double F=0;
	double muHat=0;;

	
	
	public Arm(int Id, double mean,double var){
		this.Id=Id;
		this.mean=mean;
		this.var=var;

	}

	public void computeLastReward() {
		boolean val=simValue.nextBoolean(mean);
		lastReward= val ? 1.0:0.0;		
	}
	
	public void updateFactorsBeta(){
			if(lastReward==1.0){S=S+1;}
			else{F=F+1;}
	}
	
	public void updateFactorsBetaGeneral(){
		boolean val=simValue.nextBoolean(lastReward);
		double r= val ? 1.0:0.0;	
		if(r==1.0){S=S+1;}
		else{F=F+1;}
}
	
	public void updateFactorsGaussian(){
		muHat=(muHat*numberPlayed-1+lastReward)/(numberPlayed+1);
}
	
	public void reinit(){
		numberPlayed=1;
		sumRewards=0.0;
		sumSqrtRewards=0.0;
		lastReward=0.0;
		S=0;
		F=0;
		
	}
	
	
}
