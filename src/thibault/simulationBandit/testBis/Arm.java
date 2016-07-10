package thibault.simulationBandit.testBis;
import java.util.ArrayList;

import thibault.simulationBandit.Distributions;


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
	
	ArrayList<Double> rewards;
	
	public Arm(int Id, double mean,double var){
		this.Id=Id;
		this.mean=mean;
		this.var=var;
		this.rewards= new ArrayList<Double>();
	}

	public void computeLastReward(int j) {
		lastReward=rewards.get(j);	
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
