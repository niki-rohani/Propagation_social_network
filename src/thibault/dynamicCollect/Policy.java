package thibault.dynamicCollect;

import core.Node;

import java.awt.List;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.GammaDistribution;

import thibault.simulationBandit.Distributions;

public abstract class Policy {
	public int nbPlayed;
	ArrayList<Arm> arms;
	HashSet<Arm> lastSelected;
	public HashSet<String> NameOtpArmToPlay;//attribut util si on veut calculer une politique optimGlobal
	public Policy(){
		this.arms=new ArrayList<Arm>();
		this.lastSelected=new HashSet<Arm>();
		this.nbPlayed=1;
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
		this.NameOtpArmToPlay=new HashSet<String>();
	}
	
	public abstract void updateRewards();
	
}

class RandomPolicy extends Policy{
	
	public RandomPolicy(){
		super();
	}
	public void updateRewards(){
		for(Arm arm:lastSelected){
			arm.sumRewards+=arm.lastReward;
			arm.numberPlayed++;
		}
	}
	
	public HashSet<Arm> select(int nb){
		Collections.shuffle(arms);
		HashSet<Arm> ret=new HashSet<Arm>();
		int nbMax=nb;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		for(int i=0;i<nbMax;i++){
			ret.add(arms.get(i));
		}
		lastSelected=ret;
		return ret;
	}
	
	
	public String toString(){
		return "RandomPolicy";
	}
}

class OptimalPolicyLocal extends Policy{
	
	int nbToSelect=0;
	public OptimalPolicyLocal(){
		super();
	}
	
	public void updateRewards(){
		//ArrayList<Arm> listArms=new ArrayList<Arm>(lastSelected);
		Collections.sort(arms,new LastSelectedArmComparator());
		if(nbToSelect>arms.size()){
			nbToSelect=arms.size();
		}
		for(int i=0;i<nbToSelect;i++){
			Arm arm=arms.get(i);
			arm.sumRewards+=arm.lastReward;
			arm.numberPlayed++;
		}
	}
	
	public class LastSelectedArmComparator implements Comparator<Arm>
	{
		public int compare(Arm arm1,Arm arm2){
			if(arm1.lastReward>arm2.lastReward){
				return -1;
			}
			if(arm1.lastReward<arm2.lastReward){
				return 1;
			}
			return 0;
		}
	}
	
	public HashSet<Arm> select(int nb){
		HashSet<Arm> ret=new HashSet<Arm>(arms);
		lastSelected=ret;
		nbToSelect=nb;
		if(nb>arms.size()){
			nbToSelect=arms.size();
		}
		return ret;
	}
	
	public String toString(){
		return "OptimalPolicyLocal";
	}
}

//Polituque qui joue tous les bras chaque pas de temps et qui  
//un attribut qui va classer les bras selon leur reward moyen a la fin
//On utilise cet attribut pour la polituque "OptimalPolicyGlobal"
//Dans la pratique on ne fais tourner ce truc qu'un fois et apres on prends le nombre qui va bien dedans
//pour notre politique Global, on n'a as besoin d'enregistrer dans un fichier a n'a pas de sens, regarder dans la DynamicCollect comment on s'en sert

class BestGlobalArmFinder extends Policy{

	boolean firstRound=true;
	
	public BestGlobalArmFinder(){
		super();
	}
	
	public void updateRewards(){
		if(firstRound==false){
			for(int i=0;i<arms.size();i++){
				Arm arm=arms.get(i);
				arm.sumRewards+=arm.lastReward;
				arm.numberPlayed++;
			}
			Collections.sort(arms,new GlogalComparator());
		}
		else{
			firstRound=false;
		}
		
		
	}
	

	
	public class GlogalComparator implements Comparator<Arm>
	{
		public int compare(Arm arm1,Arm arm2){
			if(arm1.sumRewards>arm2.sumRewards){
				return -1;
			}
			if(arm1.sumRewards<arm2.sumRewards){
				return 1;
			}
			return 0;
		}
	}
	
	public HashSet<Arm> select(int nb){
		HashSet<Arm> ret=new HashSet<Arm>(arms);
		lastSelected=ret;
		return ret;
	}
	
	public String toString(){
		return "BestGlobalArmFinder";
	}

	
}



class OptimalPolicyGlobal extends Policy{
	int nbToSelect=0;
	
	public OptimalPolicyGlobal(){
		super();
		NameOtpArmToPlay = new HashSet<String>();
	}
	
	public void updateRewards(){

		if(nbToSelect>arms.size()){
			nbToSelect=arms.size();
		}
		for(Arm arm: arms){
			if(NameOtpArmToPlay.contains(arm.getName())){
				arm.sumRewards+=arm.lastReward;
				arm.numberPlayed++;
			}
		}
	}
	
	public HashSet<Arm> select(int nb){
		HashSet<Arm> ret=new HashSet<Arm>();
		for(Arm arm: arms){
			if(NameOtpArmToPlay.contains(arm.getName())){
				ret.add(arm);
			}
		}
		lastSelected=ret;
		nbToSelect=nb;
		if(nb>arms.size()){
			nbToSelect=arms.size();
		}
		return ret;
	}
	
	public String toString(){
		return "OptimalPolicyGlobal";
	}	
}
class Greedy extends Policy{
		ArrayList<Double> scores;
		int nbPlayed=1;
		private double epsilon;
		private double d=0.5;
		private double c=1;
		
		public Greedy(){
			super();
			scores=new ArrayList<Double>();
		}
		
		public Greedy(double epsilon){
			scores=new ArrayList<Double>();
			this.epsilon=epsilon;
		}

		
		public void reinitPolicy(){
			super.reinitPolicy();
			scores=new ArrayList<Double>();
			nbPlayed=1;
		}
		public void updateRewards(){
			for(Arm arm:lastSelected){
				arm.sumRewards+=arm.lastReward;
				arm.numberPlayed++;
				addArm(arm);
			}
			if(lastSelected.size()>0){
				nbPlayed++;
			}
			
		}
		
		
		
		public void removeArm(Arm a){
			int index=arms.indexOf(a);
			if(index>=0){
				arms.remove(index);
				scores.remove(index);
			}
		}
		
		public void addArm(Arm a){
			int index=arms.indexOf(a);
			if(index>=0){
				arms.remove(index);
				scores.remove(index);
			}
			int nbp=a.getNumberPlayed();
			if(nbp==0){
				 a.numberPlayed++;
				 nbp=1;
			}
			double r=(a.getSumRewards()/(1.0*nbp))+(Math.sqrt(2.0*Math.log(nbPlayed)/(1.0*nbp)));

			index=0;
			for(int i=scores.size()-1;i>=0;i--){
				 double s=scores.get(i);
				 if(r<=s){
					 index=i+1;
					 break;
				 }
			}
			scores.add(index,r);
			arms.add(index,a);
		}
		
		public HashSet<Arm> select(int nb){
			epsilon=Math.min(1,c*arms.size()/(d*d*nbPlayed));
			int nbMax=nb;
			if(nbMax>arms.size()){
				nbMax=arms.size();
			}
			HashSet<Arm> ret=new HashSet<Arm>();
			for(int i=0;i<nbMax;i++){
				boolean p;
				Distributions val = new Distributions();
				p=val.nextBoolean(1-epsilon);
				if (p==true){
					ret.add(arms.get(i));
				}
				else {
					Random r = new Random();
					ret.add(arms.get(r.nextInt(arms.size())));
				}				
			}
			lastSelected=ret;
			return ret;
		}
		
		public String toString(){
			return "greedy";
		}
	}
		
	class playEveryArms extends Policy{

		boolean firstRound=true;
		
		public playEveryArms(){
			super();
		}
		
		public void updateRewards(){
			if(firstRound==false){
				for(int i=0;i<arms.size();i++){
					Arm arm=arms.get(i);
					arm.sumRewards+=arm.lastReward;
					arm.numberPlayed++;
				}
			}
			else{
				firstRound=false;
			}	
		}
		
		public HashSet<Arm> select(int nb){
			HashSet<Arm> ret=new HashSet<Arm>(arms);
			lastSelected=ret;
			return ret;
		}
		
		public String toString(){
			return "PlayEveryArm";
		}
	}
	
	
	
	

	
	 class Minimax extends Policy{
		int nbPlayed=1;
		public Minimax(){
			super();
		}
		
		public void reinitPolicy(){
			super.reinitPolicy();
			nbPlayed=1;
		}
		public void updateRewards(){
			for(Arm arm:lastSelected){
				arm.sumRewards+=arm.lastReward;
				arm.numberPlayed++;
				arm.sumSqrtRewards+=arm.lastReward*arm.lastReward;

			}
		
			if(lastSelected.size()>0){
				nbPlayed++;
				Collections.sort(arms,new scoreComparator());
			}
			
		}
			
		
		
		
		public void removeArm(Arm a){
			int index=arms.indexOf(a);
			if(index>=0){
				arms.remove(index);
			}
		}
		
		public void addArm(Arm a){
			int nbp=a.getNumberPlayed();
			if(nbp==0){
				 a.numberPlayed++;
				 nbp=1;
			}
			arms.add(a);
		}
		
		public class scoreComparator implements Comparator<Arm>
		{
			public int compare(Arm arm1,Arm arm2){

			double r1=arm1.getSumRewards()/(1.0*arm1.getNumberPlayed())+Math.sqrt(Math.max(0, Math.log(nbPlayed/(arm1.getNumberPlayed()*arms.size())))/(1.0*arm1.getNumberPlayed()));
			double r2=arm2.getSumRewards()/(1.0*arm2.getNumberPlayed())+Math.sqrt(Math.max(0, Math.log(nbPlayed/(arm2.getNumberPlayed()*arms.size())))/(1.0*arm2.getNumberPlayed()));
			
				if(r1>r2){
					return -1;
				}
				if(r1<r2){
					return 1;
				}
				return 0;
			}
		}
		
		
		public HashSet<Arm> select(int nb){
			
			int nbMax=nb;
			if(nbMax>arms.size()){
				nbMax=arms.size();
			}
			HashSet<Arm> ret=new HashSet<Arm>();

			
			for(int i=0;i<nbMax;i++){
			ret.add(arms.get(i));
			}
			lastSelected=ret;
			return ret;
		}
		
		public String toString(){
			return "Minimax";
		}
	}
	
	
	
	class adversial extends Policy{
	
	Distributions distribution;

	int nbPlayed=1;
	double nu;

	public void reinitPolicy(){
		super.reinitPolicy();
		nbPlayed=1;
	}
	
	public void updateRewards(){
		for(Arm arm:lastSelected){
			arm.sumRewards+=arm.lastReward;
			arm.numberPlayed++;
			arm.estimatedCumulativeGain+=1.0*arm.lastReward/arm.prob;
		}
		
		int K=arms.size();
		if(nbPlayed==1 && K!=0){
			for (Arm arm:arms){
				arm.prob=1.0/K;
			}
			
		}
		
		
		if(lastSelected.size()>0){
			nbPlayed++;
			updateProb();
			Collections.sort(arms,new probComparator());
		}

		
	}
	
	public void updateCumEstimatedGain(){
		
	}
	
	public void updateProb(){
		double sum=0.0;
		int K=arms.size();
		//nu=Math.min(1.0/K,0.8*Math.sqrt(Math.log(K)/(nbPlayed*K*1.0)));
		//nu=Math.sqrt(2.0*Math.log(K)/(nbPlayed*K*1.0));
		//nu=Math.sqrt(Math.log(K)/(nbPlayed*K*(Math.exp(1)-1)));
		nu=1.0/(2*K);
		System.out.println(1-K*nu);
		for (Arm arm:arms){
			sum+=Math.exp(nu*arm.estimatedCumulativeGain);
		}
		for (Arm arm:arms){
			arm.prob=(1-K*nu)*Math.exp(nu*arm.estimatedCumulativeGain)/sum+nu;
			//System.out.println(arm.prob+"__"+nu);
			//System.out.println(Math.exp(nu*arm.estimatedCumulativeGain)/sum+"__"+arm.estimatedCumulativeGain);
		}

	}
	
	public void removeArm(Arm a){
		int index=arms.indexOf(a);
		if(index>=0){
			arms.remove(index);
		}
	}
	
	public void addArm(Arm a){
		int nbp=a.getNumberPlayed();
		if(nbp==0){
			 a.numberPlayed++;
			 nbp=1;
		}
		arms.add(a);
	}
	

	
	public class probComparator implements Comparator<Arm>
	{
		public int compare(Arm arm1,Arm arm2){
			double p1=arm1.getProb();
			double p2=arm2.getProb();
	
			if(p1>p2){
				return -1;
			}
			if(p1<p2){
				return 1;
			}
			return 0;
		}
	}

	public HashSet<Arm> select(int nb) {

		//System.out.println(nbPlayed+"_"+arms.size());
		int nbMax=nb;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		HashSet<Arm> ret=new HashSet<Arm>();
		
		if(arms.size()!=0){
			
		ArrayList<Double> probaArray=new ArrayList<Double>();
		for (int i=0;i<arms.size();i++){
			probaArray.add(i,arms.get(i).getProb());
		}
			
			distribution = new Distributions();

			HashSet<Integer> indArmToPlay = new HashSet<Integer>();
			for(int i=0;i<nbMax;i++){
				int ind = distribution.discrete(probaArray);
				while(indArmToPlay.contains(ind)==true){
					ind = distribution.discrete(probaArray);
				}
				
				indArmToPlay.add(ind);
				ret.add(arms.get(ind));
			}
			//System.out.println(indArmToPlay);
		}
		
		lastSelected=ret;
		return ret;
	}

	public String toString(){
		return "Adversial";
	}
}
	
	
	class Thompson extends Policy{

		public Thompson(){
			super();
		}

		
		public void reinitPolicy(){
			super.reinitPolicy();
			nbPlayed=1;
		}
		
		public void updateRewards(){
			for(Arm arm:lastSelected){
				arm.sumRewards+=arm.lastReward;
				arm.numberPlayed++;
			}
			updateScores();
			if(lastSelected.size()>0){
				nbPlayed++;
				Collections.sort(arms,new scoreComparatorThompson());
			}

		}
		
		
			
		public void updateScores(){
			for (Arm arm:arms){
				Distributions simValue= new Distributions();
				arm.thompsonPrior=simValue.nextBeta(arm.S+1, arm.F+1);
			}
		}
		

		
		
		
		public class scoreComparatorThompson implements Comparator<Arm>
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
		
		
		public HashSet<Arm> select(int nb){
			
			int nbMax=nb;
			if(nbMax>arms.size()){
				nbMax=arms.size();
			}
			HashSet<Arm> ret=new HashSet<Arm>();

			
			for(int i=0;i<nbMax;i++){
			ret.add(arms.get(i));
			}
			lastSelected=ret;
			return ret;
		}
		
		public String toString(){
			return "Thompson";
		}
	}
	
	
	class ThompsonPoisson extends Policy{

		boolean optimistic;
		public ThompsonPoisson(boolean optimistic){
			super();
			this.optimistic=optimistic;
		}

		public ThompsonPoisson(){
			this(true);
		}
		
		public void reinitPolicy(){
			super.reinitPolicy();
			nbPlayed=1;
		}
		
		public void updateRewards(){
			for(Arm arm:lastSelected){
				arm.sumRewards+=arm.lastReward;
				arm.numberPlayed++;
			}
			updateScores();
			if(lastSelected.size()>0){
				nbPlayed++;
				Collections.sort(arms,new scoreComparatorThompson());
			}

		}
		
		
			
		public void updateScores(){
			for (Arm a:arms){
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
		

		
		
		
		public class scoreComparatorThompson implements Comparator<Arm>
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
		
		
		public HashSet<Arm> select(int nb){
			
			int nbMax=nb;
			if(nbMax>arms.size()){
				nbMax=arms.size();
			}
			HashSet<Arm> ret=new HashSet<Arm>();

			
			for(int i=0;i<nbMax;i++){
			ret.add(arms.get(i));
			}
			lastSelected=ret;
			return ret;
		}
		
		public String toString(){
			return "ThompsonPoisson";
		}
	}

	class UCBMod extends Policy{
		//int nbPlayed=1;
		double rho;
		public UCBMod(){
			super();
			this.rho=1;
		}
		public UCBMod(double rho){
			super();
			this.rho=rho;
		}
		
		public void reinitPolicy(){
			super.reinitPolicy();
			nbPlayed=1;
		}
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
			
		public void removeArm(Arm a){
			int index=arms.indexOf(a);
			if(index>=0){
				arms.remove(index);
			}
		}
		
		public void addArm(Arm a){
			//int nbp=a.getNumberPlayed();
			//if(nbp==0){
			//	 a.numberPlayed++;
			//	 nbp=1;
			//}
			arms.add(a);
		}
		
		public class scoreComparator implements Comparator<Arm>
		{
			public int compare(Arm arm1,Arm arm2){

				
				if (arm1.getNumberPlayed()==0 && arm2.getNumberPlayed()==0){
					return 0;
					}
					
				else if(arm1.getNumberPlayed()==0 && arm2.getNumberPlayed()!=0){
					return -1;
					}
					
				else if(arm1.getNumberPlayed()!=0 && arm2.getNumberPlayed()==0){
					return 1;
					}
				
				else{
					double r1=(arm1.getSumRewards()/(1.0*arm1.getNumberPlayed()))+(Math.sqrt(rho*Math.log(nbPlayed)/(1.0*arm1.getNumberPlayed())));
					double r2=(arm2.getSumRewards()/(1.0*arm2.getNumberPlayed()))+(Math.sqrt(rho*Math.log(nbPlayed)/(1.0*arm2.getNumberPlayed())));
		
					if(r1>r2){
					return -1;
					}	
					if(r1<r2){
					return 1;
					}
				}
				return 0;
			}
		}
		
		public HashSet<Arm> select(int nb){
			
			int nbMax=nb;
			if(nbMax>arms.size()){
				nbMax=arms.size();
			}
			HashSet<Arm> ret=new HashSet<Arm>();
	
			
			for(int i=0;i<nbMax;i++){
			ret.add(arms.get(i));
			}
			lastSelected=ret;
			return ret;
		}
		
		public String toString(){
			return "UCBMod"+rho;
		}
	}

	class UCBVMod extends Policy{
		double b=1.0;
		public UCBVMod(){
			super();
		}
		
		public UCBVMod(double b){
			super();
			this.b=b;
		}
		
		public void reinitPolicy(){
			super.reinitPolicy();
			nbPlayed=1;
		}
		public void updateRewards(){
			for(Arm arm:lastSelected){
				arm.sumRewards+=arm.lastReward;
				arm.numberPlayed++;
				arm.sumSqrtRewards+=arm.lastReward*arm.lastReward;
	
			}
		
			if(lastSelected.size()>0){
				nbPlayed++;
				Collections.sort(arms,new scoreComparator());
			}
			
		}
		
		public void removeArm(Arm a){
			int index=arms.indexOf(a);
			if(index>=0){
				arms.remove(index);
			}
		}
		
		public void addArm(Arm a){
			/*int nbp=a.getNumberPlayed();
			if(nbp==0){
				 a.numberPlayed++;
				 nbp=1;
			}*/
			arms.add(a);
		}
		
		public class scoreComparator implements Comparator<Arm>
		{
			public int compare(Arm arm1,Arm arm2){

				if (arm1.getNumberPlayed()==0 && arm2.getNumberPlayed()==0){
				return 0;
				}
				
				else if(arm1.getNumberPlayed()==0 && arm2.getNumberPlayed()!=0){
				return -1;
				}
				
				else if(arm1.getNumberPlayed()!=0 && arm2.getNumberPlayed()==0){
				return 1;
				}
				
				else{
				double empAverage=arm1.getSumRewards()/(1.0*arm1.getNumberPlayed());
				double empVariance=arm1.getSumSqrtRewards()/(1.0*arm1.getNumberPlayed())-empAverage*empAverage;
				double r1=empAverage+Math.sqrt(2*empVariance*Math.log(nbPlayed)/(1.0*arm1.getNumberPlayed()))+b*Math.log(nbPlayed)/(2.0*arm1.getNumberPlayed());
				empAverage=arm2.getSumRewards()/(1.0*arm2.getNumberPlayed());
				empVariance=arm2.getSumSqrtRewards()/(1.0*arm2.getNumberPlayed())-empAverage*empAverage;
				double r2=empAverage+Math.sqrt(2*empVariance*Math.log(nbPlayed)/(1.0*arm2.getNumberPlayed()))+b*Math.log(nbPlayed)/(2.0*arm2.getNumberPlayed());
				
					if(r1>r2){
					return -1;
					}
					if(r1<r2){
					return 1;
					}	
				}
				
				return 0;
			}
		}
		
		public HashSet<Arm> select(int nb){
			
			int nbMax=nb;
			if(nbMax>arms.size()){
				nbMax=arms.size();
			}
			HashSet<Arm> ret=new HashSet<Arm>();
	
			
			for(int i=0;i<nbMax;i++){
			ret.add(arms.get(i));
			}
			lastSelected=ret;
			return ret;
		}
		
		public String toString(){
			return "UCBVMod";
		}
	}

	
	
	