package thibault.SNCollect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;


public abstract class PolicyContextHidden extends PolicyBase{
	
	static int maxScore=2000000;
	public int nbIt;
	public ArrayList<ArmContextFull> arms;
	public HashSet<ArmContextFull> lastSelected;
	public HashSet<ArmContextFull> observedArms;
	
	public int sizeFeaturesCom;
	public int sizeFeaturesInd;
	public double regParam;
	public RealVector betaStar;
	public RealVector beta;
	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	
	public PolicyContextHidden(int sizeFeaturesCom, int sizeFeaturesInd, double regParam){
		this.arms=new ArrayList<ArmContextFull>();
		this.lastSelected=new HashSet<ArmContextFull>();
		this.observedArms=new HashSet<ArmContextFull>();
		this.nbIt=1;
		this.sizeFeaturesCom=sizeFeaturesCom;
		this.sizeFeaturesInd=sizeFeaturesInd;
		this.regParam=regParam;
		if(sizeFeaturesCom>0){
			this.A0= new Array2DRowRealMatrix(new double[sizeFeaturesCom][sizeFeaturesCom] );
			this.invA0= new Array2DRowRealMatrix(new double[sizeFeaturesCom][sizeFeaturesCom] );
			this.beta=new ArrayRealVector(new double[sizeFeaturesCom]);
			this.b0=new ArrayRealVector(new double[sizeFeaturesCom]);
		}
		this.initMatrix();
	}
	
	public void  initMatrix(){
		for (int i = 0; i<sizeFeaturesCom;i++){
			b0.setEntry(i, 0);
			beta.setEntry(i, 0);
			for (int j = 0; j<sizeFeaturesCom;j++){
				if(i==j){
					A0.setEntry(i, j, regParam);
					invA0.setEntry(i, j, regParam);
				}
				else{
					A0.setEntry(i, j, 0);
					invA0.setEntry(i, j, 0);
				}
			}
			
		}
	}
	
	public void reinitPolicy(){
		this.arms=new ArrayList<ArmContextFull>();
		this.lastSelected=new HashSet<ArmContextFull>();
		this.observedArms=new HashSet<ArmContextFull>();
		this.nbIt=1;
		this.initMatrix();
	};
	
	//public abstract void updateScores();
	//public abstract void updateParameters();
	//public abstract String toString();
	

	
	public void select(int nbToSelect){
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		Collections.sort(arms,new scoreComparator());
		lastSelected=new HashSet<ArmContextFull>();
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




class HiddenCLinUCBHybrid extends PolicyContextHidden{

	public double exploFactor;
	public int nbSamples=30;
	
	public HiddenCLinUCBHybrid(int sizeFeaturesCom, int sizeFeaturesInd, double regParam, double exploFactor) {
		super(sizeFeaturesCom, sizeFeaturesInd, regParam);
		this.exploFactor=exploFactor;
	}

	@Override
	public void updateScores() {
		
		
		for (ArmContextFull a: arms){
			
			if(a.numberPlayed<=2){
				a.score=maxScore;
			}
			else{
				if(observedArms.contains(a)){
					a.score=
							a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
							exploFactor*Math.sqrt(
									1.0/(a.numberPlayedOnObs+1)+
									a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)))))
									);
				}
				else{
					a.featuresCom=a.sumFeaturesCom.mapDivide(a.numberObserved*1.0);
					a.score=
							a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
							exploFactor*Math.sqrt(
									1.0/(a.numberPlayedOnObs+1)+
									a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)))))
									);
				}
				/*else{
					MultivariateNormalDistribution simFeat = new MultivariateNormalDistribution(a.sumFeaturesCom.mapDivide(a.numberObserved*1.0).toArray(),a.sumProdFeaturesCom.scalarMultiply(1.0/a.numberObserved).subtract(a.sumFeaturesCom.mapDivide(a.numberObserved*1.0).outerProduct(a.sumFeaturesCom.mapDivide(a.numberObserved*1.0))).getData());
					double exploit=0.0;
					double explore =0.0;
					for(int i=0;i<nbSamples;i++){
						RealVector sampleFeat= new ArrayRealVector(simFeat.sample());
						exploit+=sampleFeat.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+a.sumRewardsOnObs/(a.numberPlayedOnObs+1);
						explore+=1.0/(a.numberPlayedOnObs+1)+
								sampleFeat.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(sampleFeat.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)))));
					}
					a.score=(exploit+exploFactor*Math.sqrt(explore))/nbSamples;
				}*/

			}

			
		}
	}
	
	@Override
	public void updateParameters() {
		for(ArmContextFull a:lastSelected)	{
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumProdRewards+=a.lastReward*a.lastReward;
			if(observedArms.contains(a)){
				A0=A0.add(a.BArm.transpose().multiply(a.AArmInverse).multiply(a.BArm));
				b0=b0.add(a.BArm.transpose().multiply(a.AArmInverse).operate(a.bArm));
				a.AArm=a.AArm.add(a.featuresInd.outerProduct(a.featuresInd));
				a.BArm=a.BArm.add(a.featuresInd.outerProduct(a.featuresCom));
				a.bArm=a.bArm.add(a.featuresInd.mapMultiply(a.lastReward));
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs=a.sumRewardsOnObs+a.lastReward;
				a.AArmInverse= new LUDecomposition(a.AArm).getSolver().getInverse();
				A0=A0.add(a.featuresCom.outerProduct(a.featuresCom)).subtract(a.BArm.transpose().multiply(a.AArmInverse).multiply(a.BArm));
				b0=b0.add(a.featuresCom.mapMultiply(a.lastReward)).subtract(a.BArm.transpose().multiply(a.AArmInverse).operate(a.bArm));
			}

		}
		
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		for(ArmContextFull a:lastSelected)	{
			if(observedArms.contains(a)){
				a.thetaArm=a.AArmInverse.operate(a.bArm.subtract(a.BArm.operate(beta)));
			}
		}
		nbIt ++;
	}

	@Override
	public String toString() {
		return "HybridCLinUCBHybrid";
	}
}



class HiddenCLinUCBHybridBis extends PolicyContextHidden{

	public double exploFactor;
	public int nbSamples=30;
	
	public HiddenCLinUCBHybridBis(int sizeFeaturesCom, int sizeFeaturesInd, double regParam, double exploFactor) {
		super(sizeFeaturesCom, sizeFeaturesInd, regParam);
		this.exploFactor=exploFactor;
	}

	@Override
	public void updateScores() {
		
		
		for (ArmContextFull a: arms){
			
			if(a.numberPlayed<=2){
				a.score=maxScore;
			}
			else{
				if(observedArms.contains(a)){
					a.score=
							a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
							exploFactor*Math.sqrt(
									1.0/(a.numberPlayedOnObs+1)+
									a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)))))
									);
				}
				else{
					a.featuresCom=a.sumFeaturesCom.mapDivide(a.numberObserved*1.0);
					a.score=
							a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
							exploFactor*Math.sqrt(
									1.0/(a.numberPlayedOnObs+1)+
									a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.featuresCom.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)))))
									);
				}
				/*else{
					MultivariateNormalDistribution simFeat = new MultivariateNormalDistribution(a.sumFeaturesCom.mapDivide(a.numberObserved*1.0).toArray(),a.sumProdFeaturesCom.scalarMultiply(1.0/a.numberObserved).subtract(a.sumFeaturesCom.mapDivide(a.numberObserved*1.0).outerProduct(a.sumFeaturesCom.mapDivide(a.numberObserved*1.0))).getData());
					double exploit=0.0;
					double explore =0.0;
					for(int i=0;i<nbSamples;i++){
						RealVector sampleFeat= new ArrayRealVector(simFeat.sample());
						exploit+=sampleFeat.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+a.sumRewardsOnObs/(a.numberPlayedOnObs+1);
						explore+=1.0/(a.numberPlayedOnObs+1)+
								sampleFeat.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(sampleFeat.subtract(a.BArm.getRowVector(0).mapDivide(1.0*a.numberPlayedOnObs+1)))));
					}
					a.score=(exploit+exploFactor*Math.sqrt(explore))/nbSamples;
				}*/

			}

			
		}
	}
	
	@Override
	public void updateParameters() {
		for(ArmContextFull a:lastSelected)	{
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumProdRewards+=a.lastReward*a.lastReward;
			if(observedArms.contains(a)){
				A0=A0.add(a.BArm.transpose().multiply(a.AArmInverse).multiply(a.BArm));
				b0=b0.add(a.BArm.transpose().multiply(a.AArmInverse).operate(a.bArm));
				a.AArm=a.AArm.add(a.featuresInd.outerProduct(a.featuresInd));
				a.BArm=a.BArm.add(a.featuresInd.outerProduct(a.featuresCom));
				a.bArm=a.bArm.add(a.featuresInd.mapMultiply(a.lastReward));
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs=a.sumRewardsOnObs+a.lastReward;
				a.AArmInverse= new LUDecomposition(a.AArm).getSolver().getInverse();
				A0=A0.add(a.featuresCom.outerProduct(a.featuresCom)).subtract(a.BArm.transpose().multiply(a.AArmInverse).multiply(a.BArm));
				b0=b0.add(a.featuresCom.mapMultiply(a.lastReward)).subtract(a.BArm.transpose().multiply(a.AArmInverse).operate(a.bArm));
			}

		}
		
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		for(ArmContextFull a:arms)	{
				a.thetaArm=a.AArmInverse.operate(a.bArm.subtract(a.BArm.operate(beta)));
		}
		nbIt ++;
	}

	@Override
	public String toString() {
		return "HybridCLinUCBHybridBis";
	}
}