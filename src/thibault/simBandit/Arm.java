package thibault.simBandit;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.dense.BasicVector;

import thibault.simulationBandit.Distributions;

public class Arm {

	//Attributs intrinseque
	public int Id;
	public int sizeFeatures;
	public double[] MeanContext;
	public RealVector SumContext;
	public RealMatrix SumProdContext;
	public RealVector SumContextOnObs;
	public double[][] CovMatrix;
	public RealVector CurrentContext;
	
	//Attributs qui bougent a chaque pas de temps et diférent pour chaque simulation
	public int numberPlayed=0;
	public int numberPlayedOnObs=0;
	public int numberObserved=0;
	public double sumRewards=0.0;
	public double sumRewardsOnObs=0.0;
	public double sumSqrtRewards=0.0;
	public double lastReward=0.0;
	public double score=0.0;
	public double potRwd=0.0;
	public double uBondCtxt =0.0;
	public double uBondRwd=0.0;
	public double sumRewardsStar=0.0; //reward pour calcul du regret def 1
	
	//Attributs spécifique aux algos qu on utilise
	public RealVector thetaVec;
	public RealVector thetaVecStar;
	public double theta;
	public double thetaStar=0.0;
	
	public Arm(int Id,int sizeFeatures){
		this.Id=Id;
		this.sizeFeatures=sizeFeatures;
		MeanContext=new double[sizeFeatures];
		CovMatrix = new double[sizeFeatures][sizeFeatures];
		SumContext=new ArrayRealVector(new double[sizeFeatures]);
		SumContextOnObs=new ArrayRealVector(new double[sizeFeatures]);
		SumProdContext = new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures]);
		CurrentContext=new ArrayRealVector(new double[sizeFeatures]);
		this.initMean();
		this.initCov();
		this.initSum();
	}
	
	public void initMean(){
		//Distributions simValue= new Distributions();
		for (int i = 0; i<sizeFeatures;i++){
			//MeanContext.set(i, simValue.nextDouble()*3);
			MeanContext[i]=0;
		}
	}
	
	public void initSum(){
		//Distributions simValue= new Distributions();
		for (int i = 0; i<sizeFeatures;i++){
			//MeanContext.set(i, simValue.nextDouble()*3);
			SumContext.setEntry(i, 0);
			SumContextOnObs.setEntry(i, 0);
		}
	}
	
	public void initCov(){
		//Distributions simValue= new Distributions();
		for (int i = 0; i<sizeFeatures;i++){
			for (int j = 0; j<sizeFeatures;j++){
				CovMatrix[i][j]=0;
				SumProdContext.setEntry(i, j, 0);
			}
		}
	}
	
	public void updateMean(double[] newMean){
		MeanContext=newMean;
	}
	
	public void updateCov(double[][] Cov){
		CovMatrix=Cov;
	}
	

	
	public void reinitArm(){
		numberPlayed=0;
		numberObserved=0;
		numberPlayedOnObs=0;
		sumRewards=0.0;
		sumRewardsOnObs=0.0;
		sumSqrtRewards=0.0;
		lastReward=0.0;
		score=0.0;
		potRwd=0.0;
		uBondCtxt=0.0;
		uBondRwd=0.0;
		sumRewardsStar=0.0;
		initMean();
		initCov();
		initSum();
	}
	
	public void getContext(String armLine){
		numberObserved++;
		//System.out.println(numberObserved);
		int j=1;
		if(Id>=10){j=2;}
		String st1[] = armLine.split("=");	
		String st2=st1[0];
		for(int k=1;k<=j;k++){
			st2 = st2.substring(1);
		}
		st2 = st2.replace(")", "").replace("(", "");
		//System.out.println(st2);
		String st3[] = st2.split(",");
		//System.out.println(st2);
		
		
		for(int i=0;i<st3.length;i++){
			//System.out.println(st3);
			CurrentContext.setEntry(i, Double.parseDouble(st3[i]));
		}
		//System.out.println(Id+" "+CurrentContext);
	}
	
	public void getReward(String armLine){
		String st1[] = armLine.split("=");
		lastReward=Double.parseDouble(st1[1]);
		//System.out.println(Id+" "+lastReward);
	}
	
	public void seeReward(String armLine){
		String st1[] = armLine.split("=");
		potRwd= Double.parseDouble(st1[1]);
		
	}
	
	
	
	/*public void sampleContext(){
		double[][] covArray = new double[sizeFeatures][sizeFeatures] ;
		double[] meanArray = new double[sizeFeatures];
		double[] simVectorArray = new double[sizeFeatures];
		for (int i = 0; i<sizeFeatures;i++){
			meanArray[i]=MeanContext.get(i);
			for (int j = 0; j<sizeFeatures;j++){
				covArray[i][j]=CovMatrix.get(i, j);
			}
		}
		MultivariateNormalDistribution simGauss = new MultivariateNormalDistribution(meanArray,covArray);
		simVectorArray=simGauss.sample();
		CurrentContext=new BasicVector(simVectorArray);
		Distributions simValue= new Distributions();
		for (int i = 0; i<sizeFeatures;i++){
			CurrentContext.set(i, simValue.nextDouble());
		}
	}
	
	public void computeReward(Vector betaStar) {
		Distributions simValue= new Distributions();
		simValue.nextGaussian(CurrentContext.innerProduct(betaStar), 2);
	}*/
	
}
