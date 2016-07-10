package thibault.dynamicCollect;

import core.Node;













import java.awt.List;
import java.io.IOException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.la4j.inversion.GaussJordanInverter;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.Vectors;
import org.la4j.vector.dense.BasicVector;

import thibault.simulationBandit.Distributions;

public abstract class PolicyContextual {
	ArrayList<Arm> arms;
	ArrayList<Arm> lastSelected; //pourcentage de seens parmi le nombre de bras selectionne a chaque instant (essayer de faire en sosrte que nbTot*percent/100 soit un entier
	ArrayList<Arm> lastSelectedPrev;
	int nbPlayed=1;
	int sizeFeatures;
	boolean seeAll;
	
	public PolicyContextual(){
		this.arms=new ArrayList<Arm>();
		this.lastSelected=new ArrayList<Arm>();
		this.lastSelectedPrev=new ArrayList<Arm>();
	}

	
	public void addArm(Arm a){
		arms.add(a);
	}
	
	public void removeArm(Arm a){
		arms.remove(a);
		this.lastSelected=new ArrayList<Arm>();
	}
	
	public void reinitPolicy(){
		this.arms=new ArrayList<Arm>();
		this.nbPlayed=1;
	}
	
	public abstract void updateRewards();
	public abstract void updateScore();
	public abstract HashSet<Arm> select(int nb);
	public abstract void updateMatrix();

	
}



class LinUCB extends PolicyContextual{
	
	
	Matrix A0;
	Matrix invA0;
	Vector b0;
	Vector beta;
	double alpha=1;
	

	public LinUCB(int sizeFeatures,double alpha) {
		super();
		this.sizeFeatures=sizeFeatures;
		this.alpha=alpha;
		this.A0= new Basic2DMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new BasicVector(new double[sizeFeatures]);
		this.b0=new BasicVector(new double[sizeFeatures]);
		this.initMatrix();
		}
	

	
	public void reinitPolicy(){
		this.arms=new ArrayList<Arm>();
		this.nbPlayed=1;
		this.initMatrix();
	}
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.set(i, 0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.set(i, j, 1);}
				else{A0.set(i, j, 0);}
			}
		}	
	}
	
	@Override
	public void updateScore(){
		if(lastSelected.size()>0){
			for(Arm a:arms){
				a.score=
						a.features.innerProduct(beta)+
						alpha*Math.sqrt(
								a.features.innerProduct(invA0.multiply(a.features))
								);
				a.nbItPolicy=nbPlayed+1;		
			}
			Collections.sort(arms,new scoreComparatorLinUCB());
			nbPlayed++;
			
		}
	}
	

	@Override
	public void updateRewards() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void updateMatrix(){
		for(Arm a:lastSelected)	{
			A0=A0.add(a.features.outerProduct(a.features));
			b0=b0.add(a.features.multiply(a.lastReward));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
		}
		MatrixInverter inverter= new GaussJordanInverter(A0) ;
		invA0=inverter.inverse();
		beta=invA0.multiply(b0);

	}
	
	
	@Override
	public HashSet<Arm> select(int nb){
		
		
		int nbMax=nb;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		lastSelected = new ArrayList<Arm>();

		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
		HashSet<Arm> ret= new HashSet<Arm>(lastSelected);
		return ret;
		
	}
	
	
	public class scoreComparatorLinUCB implements Comparator<Arm>
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
	
	
	
	public String toString(){
		return "LinUCB__nbDim="+sizeFeatures+"_lRate="+alpha;
	}




}



class HybridLinUCB extends PolicyContextual{
	
	
	Matrix A0;
	Matrix invA0;
	Matrix Id;
	Vector b0;
	Vector Null;
	Vector beta;
	double alpha=1;
	int caseNumber;
	
	

	public HybridLinUCB(int sizeFeatures,double alpha, int caseNumber) {
		super();
		this.caseNumber=caseNumber;
		this.sizeFeatures=sizeFeatures;
		this.alpha=alpha;
		this.A0= new Basic2DMatrix(new double[sizeFeatures][sizeFeatures] );
		this.Id= new Basic2DMatrix(new double[sizeFeatures][sizeFeatures] );
		this.Null=new BasicVector(new double[sizeFeatures]);
		this.beta=new BasicVector(new double[sizeFeatures]);
		this.b0=new BasicVector(new double[sizeFeatures]);
		this.initMatrix();
		}
	
	public void reinitPolicy(){
		this.arms=new ArrayList<Arm>();
		this.nbPlayed=1;
		this.initMatrix();
	}
	
	
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.set(i, 0);
			Null.set(i, 0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){
					A0.set(i, j, 1);
					Id.set(i, j, 1);
				}
				else{
					A0.set(i, j, 0);
					Id.set(i, j, 0);
					}
			}
		}	
	}
	
	@Override
	public void updateScore(){
		if(arms.size()>0){
			
			
			int NTHREADS = Runtime.getRuntime().availableProcessors();
			//System.out.println(NTHREADS);
			ExecutorService exec = Executors.newFixedThreadPool(NTHREADS-1);
			
			final int segmentLen = arms.size() / NTHREADS;
			int offset = 0;
			
			for (int i = 0; i < NTHREADS - 1; i++) {
				final int from = offset;
				final int to = offset + segmentLen;
				exec.execute(new Runnable() {
				@Override
				public void run() {
					updateScoreValues(from, to);
				}});
				offset += segmentLen;
			}
			updateScoreValues(arms.size() - segmentLen, arms.size());
			exec.shutdown();
			try {exec.awaitTermination(10, TimeUnit.SECONDS);} 
			catch (InterruptedException ignore) {}
			
			/*for(Arm a:arms){
				if(a.numberPlayed!=0){
						a.score=
								//a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(beta)+
								//a.sumRewards/(a.numberPlayed+1)+
								//alpha*Math.sqrt(
								//		1/(a.numberPlayed+1)+
								//		a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(invA0.multiply(a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1))))
								//		);
								a.features.subtract(a.sumFeatures.divide(a.numberObs+1)).innerProduct(beta)+
								a.sumRewards/(a.numberPlayed+1)+
								alpha*Math.sqrt(
										1/(a.numberPlayed+1)+
										a.features.subtract(a.sumFeatures.divide(a.numberObs+1)).innerProduct(invA0.multiply(a.features.subtract(a.sumFeatures.divide(a.numberObs+1))))
										);
					}

			}*/
			
			lastSelectedPrev = new ArrayList(lastSelected);
			Collections.sort(arms,new scoreComparatorHybridLinUCBVTest());
			nbPlayed++;

		}
	}
	
	public void updateScoreValues(int from, int to) {
		
		switch (caseNumber)
		{
		  case 0: //on met a jour tout ce que l on peut
				for (int j = from; j < to; j++) {
					Arm a =arms.get(j);
					if(a.numberPlayed!=0){
						a.score=
								a.features.subtract(a.sumFeatures.divide(a.numberObs+1)).innerProduct(beta)+
								a.sumRewards/(a.numberPlayed+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayed+1)+
										a.features.subtract(a.sumFeatures.divide(a.numberObs+1)).innerProduct(invA0.multiply(a.features.subtract(a.sumFeatures.divide(a.numberObs+1))))
										);
								}
					}
			    break;  
		  case 1: //on met a jour que quand on voit
				for (int j = from; j < to; j++) {
					Arm a =arms.get(j);
					if(a.numberPlayed!=0){
						a.score=
								a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(beta)+
								a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayedOnObs+1)+
										a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(invA0.multiply(a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1))))
										);
								}
					}
			    break; 	
		  case 2: //on met a jour tout partiellement juste pour theta
				for (int j = from; j < to; j++) {
					Arm a =arms.get(j);
					if(a.numberPlayed!=0){
						a.score=
								a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(beta)+
								a.sumRewards/(a.numberPlayed+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayed+1)+
										a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(invA0.multiply(a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1))))
										);
								}
					}
			    break; 
		  case 3: //on met a jour tout partiellement juste pour beta
				for (int j = from; j < to; j++) {
					Arm a =arms.get(j);
					if(a.numberPlayed!=0){
						a.score=
								a.features.subtract(a.sumFeatures.divide(a.numberObs+1)).innerProduct(beta)+
								a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayed+1)+
										a.features.subtract(a.sumFeatures.divide(a.numberObs+1)).innerProduct(invA0.multiply(a.features.subtract(a.sumFeatures.divide(a.numberObs+1))))
										);
								}
					}
		 
		  default:            
		}
		
		
		

	}

	
	
	@Override
	public void updateRewards() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void updateMatrix(){
		A0=Id;
		b0=Null;
		
		//Mise a jour pour tous les bras pour la moyenne des reward
		for(Arm a:lastSelected)	{
			a.numberPlayed++;
			a.sumRewards+=a.lastReward;
		}
		
		//mise a jour contexte comme et outerproduct pour calcul des covairiance dans A0 et pour tous les bras observe
		for(Arm a:lastSelectedPrev)	{
			a.sumFeatures=a.sumFeatures.add(a.features);
			a.sumOuterProdFeatures=a.sumOuterProdFeatures.add(a.features.outerProduct(a.features));
			
		}
		
		//Mise a jour uniquement pour les bras qu on a vu deux fois d affile. En plus de la some des contexte et du outer product on il y a aussi une somme de 
		//rewrd juste pour les fois ou on avait le contexte et un terme pour le 
		//calcul de la cov entre reward et contexte
		for(Arm a:lastSelected)	{
			if(lastSelectedPrev.contains(a)){
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs+=a.lastReward;
				a.sumFeaturesOnObs=a.sumFeaturesOnObs.add(a.features);
				a.sumOuterProdFeaturesOnObs=a.sumOuterProdFeaturesOnObs.add(a.features.outerProduct(a.features));
				a.sumProdFeaturesRwd=a.sumProdFeaturesRwd.add(a.features.multiply(a.lastReward));
			}
		}
		
		

		switch (caseNumber)
		{
		  case 0: //on met tout a jour
			  for(Arm a:arms)	{
				  	A0=A0.add(a.sumOuterProdFeatures.subtract(a.sumFeatures.outerProduct(a.sumFeatures).divide(a.numberObs+1)));
				  	b0=b0.add(a.sumProdFeaturesRwd).subtract(a.sumFeatures.multiply(a.sumRewards/(a.numberPlayed+1)));
				}
			    break; 
		  case 1: // on met a jour que quand on a tout
			  for(Arm a:arms)	{
					A0=A0.add(a.sumOuterProdFeaturesOnObs.subtract(a.sumFeaturesOnObs.outerProduct(a.sumFeaturesOnObs).divide(a.numberPlayedOnObs+1)));
					b0=b0.add(a.sumProdFeaturesRwd).subtract(a.sumFeaturesOnObs.multiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1)));
				}  
			    break;	
		  case 2: //on met a jour tout partiellement juste pour theta
			  for(Arm a:arms)	{
				  A0=A0.add(a.sumOuterProdFeaturesOnObs.subtract(a.sumFeaturesOnObs.outerProduct(a.sumFeaturesOnObs).divide(a.numberPlayedOnObs+1)));
				  b0=b0.add(a.sumProdFeaturesRwd).subtract(a.sumFeaturesOnObs.multiply(a.sumRewards/(a.numberPlayed+1)));
				}
				break;
		  case 3: //on met a jour tout partiellement juste pour beta
			  for(Arm a:arms)	{
				  	A0=A0.add(a.sumOuterProdFeatures.subtract(a.sumFeatures.outerProduct(a.sumFeatures).divide(a.numberObs+1)));
				  	b0=b0.add(a.sumProdFeaturesRwd).subtract(a.sumFeatures.multiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1)));
				} 
				break;

		  default: 
		}
	
	
		MatrixInverter inverter= new GaussJordanInverter(A0);
		invA0=inverter.inverse();
		beta=invA0.multiply(b0);
		if(nbPlayed%100==0){
			System.out.println(A0);
			System.out.println(b0);
			System.out.println(beta);
		}
	}
	
	
	@Override
	public HashSet<Arm> select(int nb){
		
		int nbMax=nb;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		lastSelected = new ArrayList<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
		HashSet<Arm> ret= new HashSet<Arm>(lastSelected);
		return ret;
		
	}
	
	
	public class scoreComparatorHybridLinUCBVTest implements Comparator<Arm>
	{	
		/*public int compare(Arm arm1,Arm arm2){
				double r1=arm1.score;
				double r2=arm2.score;
				if(r1>r2){
				return -1;
				}
				if(r1<r2){
				return 1;
				}				
			return 0;
		}*/
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
				double r1=arm1.score;
				double r2=arm2.score;
			
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
	
	public String toString(){
		return "HybridLinUCB_nbDim="+sizeFeatures+"_lRate="+alpha;
	}

}


class ThompsonSamplingLin extends PolicyContextual{
	
	
	Matrix B;
	Matrix invB;
	Vector f;
	Vector muHat;
	double varPriorOmega;
	double varPriorModel=1;
	boolean optimistic;
	
	

	public ThompsonSamplingLin(int sizeFeatures,double varPriorOmega) {
		super();
		this.optimistic=true;
		this.sizeFeatures=sizeFeatures;
		this.varPriorOmega=varPriorOmega;
		this.B= new Basic2DMatrix(new double[sizeFeatures][sizeFeatures] );
		this.muHat=new BasicVector(new double[sizeFeatures]);
		this.f=new BasicVector(new double[sizeFeatures]);
		this.initMatrix();
		}
	
	public ThompsonSamplingLin(int sizeFeatures,double varPriorOmega,boolean opt) {
		this(sizeFeatures,varPriorOmega);
		this.optimistic=opt;
		}

	
	
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			f.set(i, 0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){B.set(i, j, 1.0/varPriorOmega);}
				else{B.set(i, j, 0);}
			}
		}	
	}
	
	@Override
	public void updateScore(){
		if(lastSelected.size()>0){
			System.out.println(nbPlayed);
			//Ici on genere un vecteur random de moyenne beta et de matrice de cov invA0*varLik
			MultivariateNormalDistribution simGauss;
			double[][] covArray = new double[sizeFeatures][sizeFeatures] ;
			double[] meanArray = new double[sizeFeatures];
			double[] simVectorArray = new double[sizeFeatures];
			BasicVector simVector;
			for (int i = 0; i<sizeFeatures;i++){
				meanArray[i]=muHat.get(i);
				for (int j = 0; j<sizeFeatures;j++){
					covArray[i][j]=invB.get(i, j);
				}
			}
			
			simGauss = new MultivariateNormalDistribution(meanArray,covArray);
			simVectorArray=simGauss.sample();
			simVector=new BasicVector(simVectorArray);
			if(optimistic){
				for(Arm a:arms){
					a.score=Math.max(a.features.innerProduct(simVector),a.features.innerProduct(muHat));
				}
			}
			else{
				for(Arm a:arms){
					a.score=a.features.innerProduct(simVector);
				}
			}

			Collections.sort(arms,new scoreComparatorThompson());
			nbPlayed++;
		}
	}
	

	@Override
	public void updateRewards() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void updateMatrix(){
		for(Arm a:lastSelected)	{
			B=B.add(a.features.outerProduct(a.features)).divide(this.varPriorModel);
			f=f.add(a.features.multiply(a.lastReward));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
		}
		//System.out.println(B);
		MatrixInverter inverter= new GaussJordanInverter(B) ;
		invB=inverter.inverse();
		//System.out.println(invB);
		muHat=invB.multiply(f).divide(this.varPriorModel);
	}
	
	
	@Override
	public HashSet<Arm> select(int nb){
		int nbMax=nb;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		lastSelected = new ArrayList<Arm>();

		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
		HashSet<Arm> ret= new HashSet<Arm>(lastSelected);
		return ret;
		
		
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
	
	public String toString(){
		return "thompsonSamplingLin_nbDim="+sizeFeatures+"varPriorOmega"+varPriorOmega;
	}

}
		
class ThompsonSamplingHybridLin extends PolicyContextual{
	
	

	double varPriorOmega;
	boolean optimistic;
	Matrix A0;
	Matrix invA0;
	Matrix Id;
	Vector b0;
	Vector Null;
	Vector beta;
	


	

	public ThompsonSamplingHybridLin(int sizeFeatures,double varPriorOmega) {
		super();
		this.optimistic=true;
		this.sizeFeatures=sizeFeatures;
		this.varPriorOmega=varPriorOmega;
		this.A0= new Basic2DMatrix(new double[sizeFeatures][sizeFeatures] );
		this.Id= new Basic2DMatrix(new double[sizeFeatures][sizeFeatures] );
		this.Null=new BasicVector(new double[sizeFeatures]);
		this.beta=new BasicVector(new double[sizeFeatures]);
		this.b0=new BasicVector(new double[sizeFeatures]);
		this.initMatrix();
		}
	
	public ThompsonSamplingHybridLin(int sizeFeatures,double varPriorOmega,boolean opt) {
		this(sizeFeatures,varPriorOmega);
		this.optimistic=opt;
		}

	
	
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.set(i, 0);
			Null.set(i, 0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){
					A0.set(i, j, 1);
					Id.set(i, j, 1);
				}
				else{
					A0.set(i, j, 0);
					Id.set(i, j, 0);
					}
			}
		}	
	}
	
	@Override
	public void updateScore(){
		
		if(arms.size()>0){
			//Ici on genere un vecteur random de moyenne beta et de matrice de cov invA0*varLik
			MultivariateNormalDistribution simGauss;
			Distributions muHat = new Distributions();
			double valSim;
			double[][] covArray = new double[sizeFeatures][sizeFeatures] ;
			double[] meanArray = new double[sizeFeatures];
			double[] simVectorArray = new double[sizeFeatures];
			final BasicVector simVector;
			for (int i = 0; i<sizeFeatures;i++){
				meanArray[i]=beta.get(i);
				for (int j = 0; j<sizeFeatures;j++){
					covArray[i][j]=invA0.get(i, j);
				}
			}	
			simGauss = new MultivariateNormalDistribution(meanArray,covArray);
			simVectorArray=simGauss.sample();
			simVector=new BasicVector(simVectorArray);
			
			
			
			int NTHREADS = Runtime.getRuntime().availableProcessors();
			//System.out.println(NTHREADS);
			ExecutorService exec = Executors.newFixedThreadPool(NTHREADS-1);
			
			final int segmentLen = arms.size() / NTHREADS;
			int offset = 0;
			
			for (int i = 0; i < NTHREADS - 1; i++) {
				final int from = offset;
				final int to = offset + segmentLen;
				exec.execute(new Runnable() {
				@Override
				public void run() {
					updateScoreValues(from, to, simVector);
				}});
				offset += segmentLen;
			}
			
			updateScoreValues(arms.size() - segmentLen, arms.size(), simVector);
			exec.shutdown();
			try {exec.awaitTermination(10, TimeUnit.SECONDS);} 
			catch (InterruptedException ignore) {}
			

			/*if(optimistic){
				for(Arm a:arms){
					valSim=muHat.nextGaussian(a.sumRewards/(a.numberPlayed+1), 1/(a.numberPlayed+1));
					//a.score=Math.max(a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(simVector)+valSim,a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(beta)+a.sumRewards/(a.numberPlayed+1));
					a.score=Math.max(a.features.subtract(a.sumFeatures.divide(a.numberObs+1)).innerProduct(simVector)+valSim,a.features.subtract(a.sumFeatures.divide(a.numberObs+1)).innerProduct(beta)+a.sumRewards/(a.numberPlayed+1));
					
				}
			}
			else{
				for(Arm a:arms){
					valSim=muHat.nextGaussian(a.sumRewards/(a.numberPlayed+1), 1/(a.numberPlayed+1));
					a.score=a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(simVector)+valSim;
				}
			}*/
			
			lastSelectedPrev = new ArrayList(lastSelected);
			Collections.sort(arms,new scoreComparatorThompson());
			nbPlayed++;
		}
	}
	

	public void updateScoreValues(int from, int to, BasicVector simVector) {
		Distributions muHat = new Distributions();
		double valSim;
		for (int j = from; j < to; j++) {
			Arm a =arms.get(j);
			if(optimistic){
					valSim=muHat.nextGaussian(a.sumRewards/(a.numberPlayed+1), 1.0/(a.numberPlayed+1));
					//a.score=Math.max(a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(simVector)+valSim,a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(beta)+a.sumRewards/(a.numberPlayed+1));
					a.score=Math.max(a.features.subtract(a.sumFeatures.divide(a.numberObs+1)).innerProduct(simVector)+valSim,a.features.subtract(a.sumFeatures.divide(a.numberObs+1)).innerProduct(beta)+a.sumRewards/(a.numberPlayed+1));
					
				}
			else{
					a.score=a.features.subtract(a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1)).innerProduct(simVector);
				}
			}
		}
	
	
	@Override
	public void updateRewards() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void updateMatrix(){
		A0=Id;
		b0=Null;
		
		//Mise a jour pour tous les bras pour la moyenne des reward
		for(Arm a:lastSelected)	{
			a.numberPlayed++;
			a.sumRewards+=a.lastReward;
		}
		
		//mise a jour contexte comme et outerproduct pour calcul des covairiance dans A0 et pour tous les bras observe
		for(Arm a:lastSelectedPrev)	{
			a.sumFeatures=a.sumFeatures.add(a.features);
			a.sumOuterProdFeatures=a.sumOuterProdFeatures.add(a.features.outerProduct(a.features));
			
		}
		
		//Mise a jour uniquement pour les bras qu on a vu deux fois d affile. En plus de la some des contexte et du outer product on il y a aussi une somme de 
		//rewrd juste pour les fois ou on avait le contexte et un terme pour le 
		//calcul de la cov entre reward et contexte
		for(Arm a:lastSelected)	{
			if(lastSelectedPrev.contains(a)){
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs+=a.lastReward;
				a.sumFeaturesOnObs=a.sumFeaturesOnObs.add(a.features);
				a.sumOuterProdFeaturesOnObs=a.sumOuterProdFeaturesOnObs.add(a.features.outerProduct(a.features));
				a.sumProdFeaturesRwd=a.sumProdFeaturesRwd.add(a.features.multiply(a.lastReward));
			}
		}
		
		
		//calcul de la matrice A0 si on utilise tous les observe
			for(Arm a:arms)	{
				//calcul de la matrice A0 si on utilise tous les observe
				A0=A0.add(a.sumOuterProdFeatures.subtract(a.sumFeatures.outerProduct(a.sumFeatures).divide(a.numberObs+1)));
				
				//calcul de la matrice A0 si on utilise que les observe et joue deux fois a la suite
				//A0=A0.add(a.sumOuterProdFeaturesOnObs.subtract(a.sumFeaturesOnObs.outerProduct(a.sumFeaturesOnObs).divide(a.numberPlayedOnObs+1)));
			}

		
	
		for(Arm a:arms)	{
			b0=b0.add(a.sumProdFeaturesRwd).subtract(a.sumFeaturesOnObs.multiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1)));
		}
		

		
		MatrixInverter inverter= new GaussJordanInverter(A0);
		invA0=inverter.inverse();
		beta=invA0.multiply(b0);
		if(nbPlayed%100==0){
			System.out.println(A0);
			System.out.println(b0);
			System.out.println(beta);
		}
	}
	
	@Override
	public HashSet<Arm> select(int nb){
		int nbMax=nb;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		lastSelected = new ArrayList<Arm>();

		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
		HashSet<Arm> ret= new HashSet<Arm>(lastSelected);
		return ret;
		
		
	}
	
	
	public class scoreComparatorThompson implements Comparator<Arm>
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
				double r1=arm1.score;
				double r2=arm2.score;
			
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
	
	public String toString(){
		return "thompsonSamplingHybridLin_nbDim="+sizeFeatures+"varPriorOmega"+varPriorOmega;
	}

}

class ThompsonSamplingPoissonApproxGauss extends PolicyContextual{
	
	
	Matrix B;
	Matrix invB;
	Vector f;
	Vector muHat;
	double varPriorOmega;
	boolean optimistic;
	

	public ThompsonSamplingPoissonApproxGauss(int sizeFeatures,double varPriorOmega) {
		super();
		this.optimistic=true;
		this.sizeFeatures=sizeFeatures;
		this.varPriorOmega=varPriorOmega;
		this.B= new Basic2DMatrix(new double[sizeFeatures][sizeFeatures] );
		this.muHat=new BasicVector(new double[sizeFeatures]);
		this.f=new BasicVector(new double[sizeFeatures]);
		this.initMatrix();
		}
	

	public ThompsonSamplingPoissonApproxGauss(int sizeFeatures,double varPriorOmega, boolean opt) {
		this(sizeFeatures,varPriorOmega);
		this.optimistic=opt;
		}
	
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			f.set(i, 0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){B.set(i, j, 1.0/varPriorOmega);}
				//if(i==j){B.set(i, j, 1*vSquare);}
				else{B.set(i, j, 0);}
			}
		}	
	}
	
	@Override
	public void updateScore(){
		if(lastSelected.size()>0){
			//Ici on genere un vecteur random de moyenne beta et de matrice de cov invA0*varLik
			MultivariateNormalDistribution simGauss;
			double[][] covArray = new double[sizeFeatures][sizeFeatures] ;
			double[] meanArray = new double[sizeFeatures];
			double[] simVectorArray = new double[sizeFeatures];
			BasicVector simVector;
			for (int i = 0; i<sizeFeatures;i++){
				meanArray[i]=muHat.get(i);
				for (int j = 0; j<sizeFeatures;j++){
					covArray[i][j]=invB.get(i, j);
				}
			}	
			simGauss = new MultivariateNormalDistribution(meanArray,covArray);
			simVectorArray=simGauss.sample();
			simVector=new BasicVector(simVectorArray);
			if(optimistic){
				for(Arm a:arms){
					a.score=Math.max(Math.exp(a.features.innerProduct(simVector)),Math.exp(a.features.innerProduct(muHat)));
				}
			}
			else{
				for(Arm a:arms){
					a.score=Math.exp(a.features.innerProduct(simVector));
				}
			}

			Collections.sort(arms,new scoreComparatorThompson());
			nbPlayed++;
		}
	}
	

	@Override
	public void updateRewards() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void updateMatrix(){
		for(Arm a:lastSelected)	{
			B=B.add(a.features.outerProduct(a.features).multiply(a.lastReward));
			if(a.lastReward!=0){
				f=f.add(a.features.multiply(a.lastReward).multiply(Math.log(a.lastReward)));
			}
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
		}
	//	System.out.println(B);
		MatrixInverter inverter= new GaussJordanInverter(B) ;
		invB=inverter.inverse();
		muHat=invB.multiply(f);
		
	}
	
	
	@Override
	public HashSet<Arm> select(int nb){
		
		
		int nbMax=nb;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		lastSelected = new ArrayList<Arm>();

		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
		HashSet<Arm> ret= new HashSet<Arm>(lastSelected);
		return ret;
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
	
	public String toString(){
		return "thompsonSamplingPoisson_nbDim="+sizeFeatures+"varPriorOmega"+varPriorOmega;
	}

}




