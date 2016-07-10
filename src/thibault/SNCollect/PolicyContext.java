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


public abstract class PolicyContext extends PolicyBase{
	
	static int maxScore=2000000;
	public int nbIt;
	public ArrayList<ArmContext> arms;
	public HashSet<ArmContext> lastSelected;
	
	public int sizeFeaturesCom;
	public int sizeFeaturesInd;
	public double regParam;
	public RealVector betaStar;
	public RealVector beta;
	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	
	public PolicyContext(int sizeFeaturesCom, int sizeFeaturesInd, double regParam){
		this.arms=new ArrayList<ArmContext>();
		this.lastSelected=new HashSet<ArmContext>();
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
		this.arms=new ArrayList<ArmContext>();
		this.lastSelected=new HashSet<ArmContext>();
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
		lastSelected=new HashSet<ArmContext>();
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


class CLinUCBCom extends PolicyContext{

	public double exploFactor;
	
	public CLinUCBCom(int sizeFeaturesCom, int sizeFeaturesInd, double regParam, double exploFactor) {
		super(sizeFeaturesCom, sizeFeaturesInd, regParam);
		this.exploFactor=exploFactor;
	}

	@Override
	public void updateScores() {
		for (ArmContext a: arms){
				a.score=
						a.featuresCom.dotProduct(beta)+
						exploFactor*Math.sqrt(
								a.featuresCom.dotProduct(invA0.operate(a.featuresCom))
								);
			
		}
	}
	
	@Override
	public void updateParameters() {
		for(ArmContext a:lastSelected)	{
			A0=A0.add(a.featuresCom.outerProduct(a.featuresCom));
			b0=b0.add(a.featuresCom.mapMultiply(a.lastReward));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumProdRewards+=a.lastReward*a.lastReward;
		}
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		nbIt ++;
	}

	@Override
	public String toString() {
		return "CLinUCBCom";
	}
}


class CLinUCBInd extends PolicyContext{

	public double exploFactor;
	
	public CLinUCBInd(int sizeFeaturesCom, int sizeFeaturesInd, double regParam, double exploFactor) {
		super(sizeFeaturesCom, sizeFeaturesInd, regParam);
		this.exploFactor=exploFactor;
	}

	@Override
	public void updateScores() {
		for (ArmContext a: arms){
				a.score=
						a.featuresInd.dotProduct(a.thetaArm)+
						exploFactor*Math.sqrt(
								a.featuresInd.dotProduct(a.AArmInverse.operate(a.featuresInd))
								);
			
		}
	}
	
	@Override
	public void updateParameters() {
		for(ArmContext a:lastSelected)	{
			a.AArm=a.AArm.add(a.featuresInd.outerProduct(a.featuresInd));
			a.bArm=a.bArm.add(a.featuresInd.mapMultiply(a.lastReward));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumProdRewards+=a.lastReward*a.lastReward;
			a.AArmInverse= new LUDecomposition(a.AArm).getSolver().getInverse();
			a.thetaArm=a.AArmInverse.operate(a.bArm);
		}
		nbIt ++;
	}

	@Override
	public String toString() {
		return "CLinUCBInd";
	}
}


class CLinUCBHybrid extends PolicyContext{

	public double exploFactor;
	
	public CLinUCBHybrid(int sizeFeaturesCom, int sizeFeaturesInd, double regParam, double exploFactor) {
		super(sizeFeaturesCom, sizeFeaturesInd, regParam);
		this.exploFactor=exploFactor;
	}

	@Override
	public void updateScores() {
		for (ArmContext a: arms){
				a.score=
						a.featuresCom.dotProduct(beta)+
						a.featuresInd.dotProduct(a.thetaArm)+
						exploFactor*Math.sqrt(
								a.featuresInd.dotProduct(a.AArmInverse.operate(a.featuresInd))+
								a.featuresCom.subtract(a.BArm.transpose().multiply(a.AArmInverse).operate(a.featuresInd)).dotProduct(invA0.operate(a.featuresCom.subtract(a.BArm.transpose().multiply(a.AArmInverse).operate(a.featuresInd))))
								);
				//System.out.println(a.score);
		}
	}
	
	@Override
	public void updateParameters() {
		//System.out.println(lastSelected.size());
		for(ArmContext a:lastSelected)	{
			A0=A0.add(a.BArm.transpose().multiply(a.AArmInverse).multiply(a.BArm));
			b0=b0.add(a.BArm.transpose().multiply(a.AArmInverse).operate(a.bArm));
			a.AArm=a.AArm.add(a.featuresInd.outerProduct(a.featuresInd));
			a.BArm=a.BArm.add(a.featuresInd.outerProduct(a.featuresCom));
			a.bArm=a.bArm.add(a.featuresInd.mapMultiply(a.lastReward));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumProdRewards+=a.lastReward*a.lastReward;
			a.AArmInverse= new LUDecomposition(a.AArm).getSolver().getInverse();
			A0=A0.add(a.featuresCom.outerProduct(a.featuresCom)).subtract(a.BArm.transpose().multiply(a.AArmInverse).multiply(a.BArm));
			b0=b0.add(a.featuresCom.mapMultiply(a.lastReward)).subtract(a.BArm.transpose().multiply(a.AArmInverse).operate(a.bArm));
		}
		
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		for(ArmContext a:arms)	{
			a.thetaArm=a.AArmInverse.operate(a.bArm.subtract(a.BArm.operate(beta)));
			//System.out.println(a.thetaArm);
		}
		nbIt ++;
	}

	@Override
	public String toString() {
		return "CLinUCBHybrid";
	}
}

class CLinThompsonCom extends PolicyContext{

	public boolean optimistic;
	
	public CLinThompsonCom(int sizeFeaturesCom, int sizeFeaturesInd, double regParam, boolean optimistic) {
		super(sizeFeaturesCom, sizeFeaturesInd, regParam);
		this.optimistic=optimistic;
	}
	
	public CLinThompsonCom(int sizeFeaturesCom, int sizeFeaturesInd, double regParam) {
		this(sizeFeaturesCom, sizeFeaturesInd, regParam,true);
	}
	

	@Override
	public void updateScores() {
		MultivariateNormalDistribution sBeta = new MultivariateNormalDistribution(beta.toArray(),invA0.getData());
		RealVector sampleBeta = new ArrayRealVector(sBeta.sample());
		for (ArmContext a: arms){
			if(optimistic){
				a.score=Math.max(a.featuresCom.dotProduct(sampleBeta), a.featuresCom.dotProduct(beta));
			}
			else{
				a.score=a.featuresCom.dotProduct(sampleBeta);
			}
		}
	}
	
	@Override
	public void updateParameters() {
		for(ArmContext a:lastSelected)	{
			A0=A0.add(a.featuresCom.outerProduct(a.featuresCom));
			b0=b0.add(a.featuresCom.mapMultiply(a.lastReward));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumProdRewards+=a.lastReward*a.lastReward;
		}
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		nbIt ++;
	}

	@Override
	public String toString() {
		return "CLinThompsonCom";
	}
}


class CLinThompsonInd extends PolicyContext{

	public boolean optimistic;
	
	public CLinThompsonInd(int sizeFeaturesCom, int sizeFeaturesInd, double regParam, boolean optimistic) {
		super(sizeFeaturesCom, sizeFeaturesInd, regParam);
		this.optimistic= optimistic;
	}

	public CLinThompsonInd(int sizeFeaturesCom, int sizeFeaturesInd, double regParam) {
		this(sizeFeaturesCom, sizeFeaturesInd, regParam, true);

	}
	
	@Override
	public void updateScores() {
		for (ArmContext a: arms){
			MultivariateNormalDistribution sTheta = new MultivariateNormalDistribution(a.thetaArm.toArray(),a.AArmInverse.getData());
			RealVector sampleTheta = new ArrayRealVector(sTheta.sample());
			if(optimistic){
				a.score=Math.max(a.featuresInd.dotProduct(sampleTheta), a.featuresInd.dotProduct(a.thetaArm));
			}
			else{
				a.score=a.featuresInd.dotProduct(sampleTheta);
			}
		}
	}
	
	@Override
	public void updateParameters() {
		for(ArmContext a:lastSelected)	{
			a.AArm=a.AArm.add(a.featuresInd.outerProduct(a.featuresInd));
			a.bArm=a.bArm.add(a.featuresInd.mapMultiply(a.lastReward));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumProdRewards+=a.lastReward*a.lastReward;
			a.AArmInverse= new LUDecomposition(a.AArm).getSolver().getInverse();
			a.thetaArm=a.AArmInverse.operate(a.bArm);
		}
		nbIt ++;
	}

	@Override
	public String toString() {
		return "CLinThompsonInd";
	}
}


class CPoissonThompsonCom extends PolicyContext{

	public boolean optimistic;
	
	public CPoissonThompsonCom(int sizeFeaturesCom, int sizeFeaturesInd, double regParam, boolean optimistic) {
		super(sizeFeaturesCom, sizeFeaturesInd, regParam);
		this.optimistic=optimistic;
	}
	
	public CPoissonThompsonCom(int sizeFeaturesCom, int sizeFeaturesInd, double regParam) {
		this(sizeFeaturesCom, sizeFeaturesInd, regParam,true);
	}
	

	@Override
	public void updateScores() {
		MultivariateNormalDistribution sBeta = new MultivariateNormalDistribution(beta.toArray(),invA0.getData());
		RealVector sampleBeta = new ArrayRealVector(sBeta.sample());
		for (ArmContext a: arms){
			if(optimistic){
				a.score=Math.max(Math.exp(a.featuresCom.dotProduct(sampleBeta)), Math.exp(a.featuresCom.dotProduct(beta)));
			}
			else{
				a.score=Math.exp(a.featuresCom.dotProduct(sampleBeta));
			}
		}
	}
	
	@Override
	public void updateParameters() {
		for(ArmContext a:lastSelected)	{
			A0=A0.add(a.featuresCom.outerProduct(a.featuresCom).scalarMultiply(a.lastReward));;
			if(a.lastReward!=0){
				b0=b0.add(a.featuresCom.mapMultiply(a.lastReward*Math.log(a.lastReward)));
			}
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumProdRewards+=a.lastReward*a.lastReward;
		}
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		nbIt ++;
	}

	@Override
	public String toString() {
		return "CPoissonThompsonCom";
	}
}


class CBinaryThompsonCom extends PolicyContext{

	public boolean optimistic;
	
	public CBinaryThompsonCom(int sizeFeaturesCom, int sizeFeaturesInd, double regParam, boolean optimistic) {
		super(sizeFeaturesCom, sizeFeaturesInd, regParam);
		this.optimistic=optimistic;
	}
	
	public CBinaryThompsonCom(int sizeFeaturesCom, int sizeFeaturesInd, double regParam) {
		this(sizeFeaturesCom, sizeFeaturesInd, regParam,true);
	}
	

	@Override
	public void updateScores() {
		MultivariateNormalDistribution sBeta = new MultivariateNormalDistribution(beta.toArray(),invA0.getData());
		RealVector sampleBeta = new ArrayRealVector(sBeta.sample());
		for (ArmContext a: arms){
			if(optimistic){
				a.score=Math.max(Math.exp(a.featuresCom.dotProduct(sampleBeta)), Math.exp(a.featuresCom.dotProduct(beta)));
			}
			else{
				a.score=Math.exp(a.featuresCom.dotProduct(sampleBeta));
			}
		}
	}
	
	@Override
	public void updateParameters() {
		for(ArmContext a:lastSelected)	{
			A0=A0.add(a.featuresCom.outerProduct(a.featuresCom).scalarMultiply(a.lastReward));;
			if(a.lastReward!=0){
				b0=b0.add(a.featuresCom.mapMultiply(a.lastReward*Math.log(a.lastReward)));
			}
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumProdRewards+=a.lastReward*a.lastReward;
		}
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		nbIt ++;
	}

	@Override
	public String toString() {
		return "CBinaryThompsonCom";
	}
}

class CPoissonThompsonInd extends PolicyContext{

	public boolean optimistic;
	
	public CPoissonThompsonInd(int sizeFeaturesCom, int sizeFeaturesInd, double regParam, boolean optimistic) {
		super(sizeFeaturesCom, sizeFeaturesInd, regParam);
		this.optimistic= optimistic;
	}

	public CPoissonThompsonInd(int sizeFeaturesCom, int sizeFeaturesInd, double regParam) {
		this(sizeFeaturesCom, sizeFeaturesInd, regParam, true);

	}
	
	@Override
	public void updateScores() {
		for (ArmContext a: arms){
			MultivariateNormalDistribution sTheta = new MultivariateNormalDistribution(a.thetaArm.toArray(),a.AArmInverse.getData());
			RealVector sampleTheta = new ArrayRealVector(sTheta.sample());
			if(optimistic){
				a.score=Math.max(Math.exp(a.featuresInd.dotProduct(sampleTheta)),Math.exp( a.featuresInd.dotProduct(a.thetaArm)));
			}
			else{
				a.score=Math.exp(a.featuresInd.dotProduct(sampleTheta));
			}
		}
	}
	
	@Override
	public void updateParameters() {
		for(ArmContext a:lastSelected)	{
			a.AArm=a.AArm.add(a.featuresInd.outerProduct(a.featuresInd).scalarMultiply(a.lastReward));
			if(a.lastReward!=0){
				a.bArm=a.bArm.add(a.featuresInd.mapMultiply(a.lastReward*Math.log(a.lastReward)));
			}
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumProdRewards+=a.lastReward*a.lastReward;
			a.AArmInverse= new LUDecomposition(a.AArm).getSolver().getInverse();
			a.thetaArm=a.AArmInverse.operate(a.bArm);
		}
		nbIt ++;
	}

	@Override
	public String toString() {
		return "CPoissonThompsonInd";
	}
}



