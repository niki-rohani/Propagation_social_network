package thibault.SNCollect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;



public abstract class PolicyBase {

	static int maxScore=2000000;
	public int nbIt;
	public ArrayList<Arm> arms;
	public HashSet<Arm> lastSelected;
	public HashSet<String> NameOtpArmToPlay;//attribut util si on veut calculer une politique optimGlobal

	public PolicyBase(){
		this.arms=new ArrayList<Arm>();
		this.lastSelected=new HashSet<Arm>();
		this.nbIt=1;
	}

	public abstract void updateScores();
	public abstract String toString();
	
	public void reinitPolicy(){
		this.arms=new ArrayList<Arm>();
		this.lastSelected=new HashSet<Arm>();
		this.nbIt=1;
	};

	public void updateParameters(){
		for(Arm a:lastSelected){
			a.sumRewards+=a.lastReward;
			a.sumProdRewards+=a.lastReward*a.lastReward;
			a.numberPlayed++;
		}
		nbIt++;
	};
	
	public void select(int nbToSelect){
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		Collections.sort(arms,new scoreComparator());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	public class scoreComparator implements Comparator<Arm>
	{	
		public int compare(Arm arm1,Arm arm2){
			double r1=arm1.score;
			double r2=arm2.score;
			if(r1>r2){
				return -1;
			}
			if(r1<r2){
				return 1;
			}		
			return 0;
		}
	}
}

class OptimalStationnaire extends PolicyBase{
	
	public OptimalStationnaire(){
		super();
		NameOtpArmToPlay = new HashSet<String>();
	}

	@Override
	public void updateScores() {	
	}

	@Override
	public void select(int nbToSelect) {
		lastSelected=new HashSet<Arm>();
		for(Arm arm: arms){
			if(NameOtpArmToPlay.contains(arm.getName())){
				lastSelected.add(arm);
			}
		}
	}
	
	@Override
	public String toString() {
		return "OptimalPolicyStationnaire";
	}
}

class BestArmFinder extends PolicyBase{
	
	public BestArmFinder(){
		super();
		NameOtpArmToPlay = new HashSet<String>();
	}

	@Override
	public void updateScores() {	
	}

	@Override
	public void select(int nbToSelect) {
		lastSelected=new HashSet<Arm>(arms);
	}
	
	@Override
	public String toString() {
		return "BestArmFinder";
	}
}

class Random extends PolicyBase{

	public Random() {
		super();
	}

	@Override
	public void updateScores() {
	}
	
	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		Collections.shuffle(arms);
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public String toString() {
		return "Random";
	}
}

class CUCB extends PolicyBase{

	public double rho;

	public CUCB() {
		super();
		this.rho=2;
	}

	public CUCB(double rho ) {
		super();
		this.rho=rho;
	}

	@Override
	public void updateScores() {
		for (Arm a: arms){
			if(a.numberPlayed==0){
				a.score=maxScore;
			}
			else{
				double empAverage = a.sumRewards/(1.0*a.numberPlayed);
				a.score=empAverage+Math.sqrt(rho*Math.log(nbIt)/(1.0*a.numberPlayed));
			}
		}
	}
	
	@Override
	public String toString(){
		return "CUCB";
	}
}

class CUCBV extends PolicyBase{

	
	public CUCBV() {
		super();
	}


	@Override
	public void updateScores() {
		for (Arm a: arms){
			if(a.numberPlayed==0){
				a.score=maxScore;
			}
			else{
				double empAverage = a.sumRewards/(1.0*a.numberPlayed);
				double empVariance=a.sumProdRewards/(1.0*a.numberPlayed)-empAverage*empAverage;
				a.score=empAverage+Math.sqrt(2*empVariance*Math.log(nbIt)/(1.0*a.numberPlayed))+Math.log(nbIt)/(2.0*a.numberPlayed); 
			}
		}
	}
	
	@Override
	public String toString(){
		return "CUCBV";
	}
}

class ThompsonBernouilli extends PolicyBase{

	public boolean optimistic; 
	
	public ThompsonBernouilli(boolean optimistic) {
		super();
		this.optimistic=optimistic;
	}
	public ThompsonBernouilli() {
		this(true);
	}


	@Override
	public void updateScores() {
		for (Arm a: arms){
			double Q=( new  BetaDistribution(a.sumRewards+1,a.numberPlayed*1.0-a.sumRewards-1)).sample();
			if(this.optimistic){
				a.score=
						Math.max((a.sumRewards+1)/(1.0*a.numberPlayed+2),Q);
			}
			else{
				a.score=Q;
			}
		}
	}
	
	@Override
	public String toString(){
		return "ThompsonBernouilli";
	}
}


class ThompsonPoisson extends PolicyBase{

	public boolean optimistic; 
	
	public ThompsonPoisson(boolean optimistic) {
		super();
		this.optimistic=optimistic;
	}
	public ThompsonPoisson() {
		this(true);
	}


	@Override
	public void updateScores() {
		for (Arm a: arms){
			double Q=( new  GammaDistribution(a.sumRewards+1.0,1/(a.numberPlayed*1.0+1.0))).sample();
			if(this.optimistic){
				a.score=
						Math.max((a.sumRewards+1.0)/(1.0*a.numberPlayed+1),Q);
			}
			else{
				a.score=Q;
			}
		}
	}
	
	@Override
	public String toString(){
		return "ThompsonPoisson";
	}
}

