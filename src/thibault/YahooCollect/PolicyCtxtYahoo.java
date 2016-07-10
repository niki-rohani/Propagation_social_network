package thibault.YahooCollect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import thibault.OptimGeneralizedBandit;
import thibault.SNCollect.Arm;
import thibault.SNCollect.PolicyBase;


public abstract class PolicyCtxtYahoo extends PolicyBase{
	public int nbIt;
	public ArrayList<ArmContextYahoo> arms;
	public HashSet<String> armsNames;
	public ArrayList<ArmContextYahoo> possibleArms;
	public HashSet<String> possibleArmsNames;
	public ArmContextYahoo lastSelected;
	public int sizeFeaturesInd;
	public ArrayRealVector featuresVect;

	public PolicyCtxtYahoo(int sizeFeaturesInd, double regParam){
		this.arms=new ArrayList<ArmContextYahoo>();
		this.armsNames = new  HashSet<String>();
		this.possibleArms=new ArrayList<ArmContextYahoo>();
		this.possibleArmsNames = new  HashSet<String>();
		this.nbIt=1;
		this.featuresVect=new ArrayRealVector(new double[sizeFeaturesInd]);
		this.sizeFeaturesInd=sizeFeaturesInd;
	}

	public void reinitPolicy(){
		this.arms=new ArrayList<ArmContextYahoo>();
		this.nbIt=1;
	};


	public void select(int nbToSelect){

		/*Collections.sort(arms,new scoreComparator());
		lastSelected=new HashSet<ArmContextYahoo>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}*/

		Collections.sort(possibleArms,new scoreComparator());
		lastSelected=possibleArms.get(0);
	}

	public class scoreComparator implements Comparator<Arm>{	
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


class PoissonThompsonInd extends PolicyCtxtYahoo{

	public boolean optimistic;

	public PoissonThompsonInd(int sizeFeaturesInd, double regParam, boolean optimistic) {
		super(sizeFeaturesInd, regParam);
		this.optimistic= optimistic;
	}

	public PoissonThompsonInd(int sizeFeaturesInd, double regParam) {
		this(sizeFeaturesInd, regParam, true);

	}

	@Override
	public void updateScores() {

		for (ArmContextYahoo a: arms){
			if(possibleArmsNames.contains(a.getName())){
				possibleArms.add(a);	
				MultivariateNormalDistribution sTheta = new MultivariateNormalDistribution(a.thetaArm.toArray(),a.AArmInverse.getData());
				RealVector sampleTheta = new ArrayRealVector(sTheta.sample());
				if(optimistic){
					a.score=Math.max(Math.exp(featuresVect.dotProduct(sampleTheta)),Math.exp(featuresVect.dotProduct(a.thetaArm)));
				}
				else{
					a.score=Math.exp(featuresVect.dotProduct(sampleTheta));
				}
			}
		}}

	@Override
	public void updateParameters() {
		ArmContextYahoo a = lastSelected;
		a.AArm=a.AArm.add(featuresVect.outerProduct(featuresVect).scalarMultiply(a.lastReward));
		if(a.lastReward!=0){
			a.bArm=a.bArm.add(featuresVect.mapMultiply(a.lastReward*Math.log(a.lastReward)));
		}
		a.numberPlayed++;
		a.sumRewards=a.sumRewards+a.lastReward;
		a.sumProdRewards+=a.lastReward*a.lastReward;
		a.AArmInverse= new LUDecomposition(a.AArm).getSolver().getInverse();
		//fastComputeinvA(a);
		a.thetaArm=a.AArmInverse.operate(a.bArm);
		nbIt ++;
	}

	@Override
	public String toString() {
		return "PoissonThompsonInd";
	}

	public void fastComputeinvA(ArmContextYahoo a){
		RealMatrix BinvA = featuresVect.outerProduct(featuresVect).scalarMultiply(a.lastReward).multiply(a.AArmInverse);
		a.AArmInverse=a.AArmInverse.subtract(a.AArmInverse.multiply(BinvA).scalarMultiply(1/(1+BinvA.getTrace())));
	}
}

class LinUCB extends PolicyCtxtYahoo{

	public double exploParam;

	public LinUCB(int sizeFeaturesInd, double regParam, double exploParam) {
		super(sizeFeaturesInd, exploParam);
		this.exploParam= exploParam;
	}

	public LinUCB(int sizeFeaturesInd, double regParam) {
		this(sizeFeaturesInd, regParam, 2.0);

	}

	@Override
	public void updateScores() {

		for (ArmContextYahoo a: arms){
			if(possibleArmsNames.contains(a.getName())){		
				possibleArms.add(a);	
				a.score=a.thetaArm.dotProduct(featuresVect)+exploParam*Math.sqrt(featuresVect.dotProduct(a.AArmInverse.operate(featuresVect)));
			//System.out.println(a.score);
			}
		}
		}

	@Override
	public void updateParameters() {
		ArmContextYahoo a = lastSelected;
		a.AArm=a.AArm.add(featuresVect.outerProduct(featuresVect));
		a.bArm=a.bArm.add(featuresVect.mapMultiply(a.lastReward));
		a.numberPlayed++;
		a.sumRewards=a.sumRewards+a.lastReward;
		a.sumProdRewards+=a.lastReward*a.lastReward;
		a.AArmInverse= new LUDecomposition(a.AArm).getSolver().getInverse();
		a.thetaArm=a.AArmInverse.operate(a.bArm);
		nbIt ++;
	}

	@Override
	public String toString() {
		return "LinUCB";
	}
}

class RandomYahoo extends PolicyCtxtYahoo{

	public boolean optimistic;

	public RandomYahoo(int sizeFeaturesInd, double regParam, boolean optimistic) {
		super(sizeFeaturesInd, regParam);
		this.optimistic= optimistic;
	}

	public RandomYahoo(int sizeFeaturesInd, double regParam) {
		this(sizeFeaturesInd, regParam, true);

	}

	@Override
	public void updateScores() {
		for (ArmContextYahoo a: arms){
			if(possibleArmsNames.contains(a.getName())){

				possibleArms.add(a);	
			}}

	}

	@Override
	public void updateParameters() {

	}
	@Override
	public void select(int nbToSelect){

		/*Collections.sort(arms,new scoreComparator());
		lastSelected=new HashSet<ArmContextYahoo>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}*/

		Collections.shuffle(possibleArms);
		lastSelected=possibleArms.get(0);
	}

	@Override
	public String toString() {
		return "Random";
	}
}

