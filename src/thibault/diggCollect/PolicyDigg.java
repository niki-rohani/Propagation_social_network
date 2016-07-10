package thibault.diggCollect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;



public abstract class PolicyDigg {
	
	public int nbIt=1;
	public double totalReward;
	public double currentReward;
	public ArrayList<ArmContextDigg> arms;
	public ArmContextDigg lastSelected;
	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public int sizeFeatures;
	public double priorFactorBeta;
	public double exploFactor;
	public boolean optimistic;
	
	
	public PolicyDigg(int sizeFeatures, double priorFactorBeta, double exploFactor, boolean optimistic){
		this.totalReward=0.0;
		this.currentReward=0.0;
		this.optimistic=optimistic;
		this.sizeFeatures=sizeFeatures;
		this.priorFactorBeta=priorFactorBeta;
		this.exploFactor=exploFactor;
		this.arms= new ArrayList<ArmContextDigg>();
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.initMatrix();
	}
	
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i, 0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}
	
	public void reinitPolicy(){
		this.arms=new ArrayList<ArmContextDigg>();
		this.initMatrix();
		this.nbIt=1;
		totalReward=0.0;
		currentReward=0.0;
	};


	public void select(){
		Collections.sort(arms,new scoreComparator());
		lastSelected = arms.get(0);

	}

	public class scoreComparator implements Comparator<ArmContextDigg>{	
		public int compare(ArmContextDigg arm1,ArmContextDigg arm2){
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
	

	public abstract void updateScore();
	public abstract void updateParameters();
	public abstract String toString();
	
}
class Random extends PolicyDigg{

	public Random(int sizeFeatures, double priorFactorBeta, double exploFactor, boolean optimistic) {
		super(sizeFeatures, priorFactorBeta, exploFactor, optimistic);
	}
	
	public Random(){
		this(1,0.0,0.0,false);
	}

	@Override
	public void updateScore() {
	}

	@Override
	public void updateParameters() {
		
		ArmContextDigg a=lastSelected;
		currentReward=a.lastReward;
		totalReward+=a.lastReward;
		nbIt ++;
	}
	@Override
	public void select(){
		Collections.shuffle(arms);
		lastSelected = arms.get(0);
	}

	@Override
	public String toString() {
		return "Random";
	}
	
}
class Optimal extends PolicyDigg{

	public Optimal(int sizeFeatures, double priorFactorBeta, double exploFactor, boolean optimistic) {
		super(sizeFeatures, priorFactorBeta, exploFactor, optimistic);
	}
	
	public Optimal(){
		this(1,0.0,0.0,false);
	}

	@Override
	public void updateScore() {
		for (ArmContextDigg a: arms){
			a.score=a.potentialReward;
		}
	}

	@Override
	public void updateParameters() {
		ArmContextDigg a=lastSelected;
		totalReward+=a.lastReward;
		currentReward=a.lastReward;
		nbIt ++;
	}


	@Override
	public String toString() {
		return "Optimal";
	}
	
}

class ThompsonLinCtxt extends PolicyDigg{

	public ThompsonLinCtxt(int sizeFeatures, double priorFactorBeta, double exploFactor, boolean optimistic) {
		super(sizeFeatures, priorFactorBeta, exploFactor,optimistic);
	}

	@Override
	public void updateScore() {
		//System.out.println(nbIt);
		MultivariateNormalDistribution sBeta = new MultivariateNormalDistribution(beta.toArray(),invA0.getData());
		RealVector sampleBeta = new ArrayRealVector(sBeta.sample());
		for (ArmContextDigg a: arms){
			if(this.optimistic){
				a.score=
						Math.max(a.CurrentContext.dotProduct(sampleBeta),a.CurrentContext.dotProduct(beta));
			}
			else{
				a.score=
						a.CurrentContext.dotProduct(sampleBeta);
			}
			
		}
	}

	@Override
	public void updateParameters() {
		
		ArmContextDigg a=lastSelected;
		A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext));
		b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward));
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		totalReward+=a.lastReward;
		currentReward=a.lastReward;
		nbIt ++;
	}

	@Override
	public String toString() {
		return "ThompsonCtxt_Optimistic="+optimistic;
	}
	
}


class ThompsonPoissonCtxt extends PolicyDigg{

	public ThompsonPoissonCtxt(int sizeFeatures, double priorFactorBeta, double exploFactor, boolean optimistic) {
		super(sizeFeatures, priorFactorBeta, exploFactor,optimistic);
	}

	@Override
	public void updateScore() {
		//System.out.println(nbIt);
		MultivariateNormalDistribution sBeta = new MultivariateNormalDistribution(beta.toArray(),invA0.getData());
		RealVector sampleBeta = new ArrayRealVector(sBeta.sample());
		for (ArmContextDigg a: arms){
			if(this.optimistic){
				a.score=
						Math.max(Math.exp(a.CurrentContext.dotProduct(sampleBeta)),Math.exp(a.CurrentContext.dotProduct(beta)));
			}
			else{
				a.score=
						Math.exp(a.CurrentContext.dotProduct(sampleBeta));
			}
			
		}
	}

	@Override
	public void updateParameters() {
		
		ArmContextDigg a=lastSelected;
		
		A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).scalarMultiply(a.lastReward));;
		if(a.lastReward!=0){
			b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward*Math.log(a.lastReward)));
		}
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		totalReward+=a.lastReward;
		currentReward=a.lastReward;
		nbIt ++;
	}

	@Override
	public String toString() {
		return "ThompsonPoissonCtxt_Optimistic="+optimistic;
	}
	
}


class LinUCB extends PolicyDigg{

	public LinUCB(int sizeFeatures, double priorFactorBeta, double exploFactor, boolean optimistic) {
		super(sizeFeatures, priorFactorBeta, exploFactor,optimistic);
	}

	@Override
	public void updateScore() {
		//System.out.println(nbIt);
		for (ArmContextDigg a: arms){
				a.score=a.CurrentContext.dotProduct(beta)+exploFactor*Math.sqrt(a.CurrentContext.dotProduct(invA0.operate(a.CurrentContext)));
			}
		}
	

	@Override
	public void updateParameters() {
		
		ArmContextDigg a=lastSelected;
		A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext));
		b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward));
		//fastComputeinvA(a);
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		totalReward+=a.lastReward;
		currentReward=a.lastReward;
		nbIt ++;
	}

	@Override
	public String toString() {
		return "LinUCB_explo="+exploFactor;
	}
	
	
	public void fastComputeinvA(ArmContextDigg a){
		RealMatrix BinvA = a.CurrentContext.outerProduct(a.CurrentContext).multiply(invA0);
		invA0=invA0.subtract(invA0.multiply(BinvA).scalarMultiply(1/(1+BinvA.getTrace())));
	}
	
	
}

class PoissonUCB extends PolicyDigg{

	public PoissonUCB(int sizeFeatures, double priorFactorBeta, double exploFactor, boolean optimistic) {
		super(sizeFeatures, priorFactorBeta, exploFactor,optimistic);
	}

	@Override
	public void updateScore() {
		//System.out.println(nbIt);
		for (ArmContextDigg a: arms){
			double mu= a.CurrentContext.dotProduct(beta);
			double sigma2= a.CurrentContext.dotProduct(invA0.operate(a.CurrentContext));
				a.score=Math.exp(mu+sigma2/2)+exploFactor*Math.sqrt((Math.exp(sigma2)-1)*Math.exp(2*mu*sigma2));
			}
		}
	

	@Override
	public void updateParameters() {
		
		ArmContextDigg a=lastSelected;
		A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).scalarMultiply(a.lastReward));
		if(a.lastReward!=0){
			b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward*Math.log(a.lastReward)));
		}
		//fastComputeinvA(a);
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		totalReward+=a.lastReward;
		currentReward=a.lastReward;
		nbIt ++;
	}

	@Override
	public String toString() {
		return "PoissonUCB_explo="+exploFactor;
	}
	
	public void fastComputeinvA(ArmContextDigg a){
		RealMatrix BinvA = a.CurrentContext.outerProduct(a.CurrentContext).scalarMultiply(a.lastReward).multiply(invA0);
		invA0=invA0.subtract(invA0.multiply(BinvA).scalarMultiply(1/(1+BinvA.getTrace())));
	}
	
}