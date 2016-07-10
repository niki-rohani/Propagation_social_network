package thibault.simBandit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import mlp.CPUMatrix;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import thibault.OptimGeneralizedBandit;



public abstract class Policy {

	public int nbIt=1;
	public ArrayList<Arm> arms;
	public HashSet<Arm> lastSelected;
	public HashSet<Arm> observedArms;
	public int nbObserved;
	public RealVector betaStar;
	

	public Policy(){
		this.arms=new ArrayList<Arm>();
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
	}

	public abstract void updateScore();
	public abstract void updateArmParameter();
	public abstract void select(int nb);
	public abstract void reinitPolicy();
	public abstract String toString();
}

class RandomPolicy extends Policy{

	public RandomPolicy() {
		super();
	}

	@Override
	public void updateArmParameter() {
		for (Arm a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.numberPlayed++;
		}
		nbIt ++;
	}

	public void updateScore() {

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
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		for (Arm a:this.arms){
			a.reinitArm();
		}
	}



	@Override
	public String toString() {
		return "RandomPolicy";
	}
}
class PlayAll extends Policy{

	public PlayAll() {
		super();
	}

	@Override
	public void updateArmParameter() {
		for (Arm a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.numberPlayed++;
		}
		nbIt ++;
	}

	public void updateScore() {

	}

	public class scoreComparatorPlayAll implements Comparator<Arm>
	{	
		public int compare(Arm arm1,Arm arm2){

			double r1=arm1.sumRewards;
			double r2=arm2.sumRewards;

			if(r1>r2){
				return -1;
			}
			if(r1<r2){
				return 1;
			}		

			return 0;
		}
	}

	@Override
	public void select(int nbToSelect) {
		int nbMax=arms.size();
		lastSelected=new HashSet<Arm>();
		Collections.sort(arms,new scoreComparatorPlayAll());
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}



	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		for (Arm a:this.arms){
			a.reinitArm();
		}
	}

	@Override
	public String toString() {
		return "PlayAll";
	}

}


class PlayBest extends Policy{

	public PlayBest() {
		super();
	}

	@Override
	public void updateArmParameter() {
		for (Arm a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.numberPlayed++;
		}
		nbIt ++;
	}

	public void updateScore() {

	}

	public class scoreComparatorBest implements Comparator<Arm>
	{	
		public int compare(Arm arm1,Arm arm2){

			double r1=arm1.potRwd;
			double r2=arm2.potRwd;

			if(r1>r2){
				return -1;
			}
			if(r1<r2){
				return 1;
			}		

			return 0;
		}
	}

	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		lastSelected=new HashSet<Arm>();
		Collections.sort(arms,new scoreComparatorBest());
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}



	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		for (Arm a:this.arms){
			a.reinitArm();
		}
	}

	@Override
	public String toString() {
		return "PlayBest";
	}

}


class CUCBPolicy extends Policy{

	public CUCBPolicy() {
		super();
	}

	@Override
	public void updateArmParameter() {
		for (Arm a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.numberPlayed++;
		}
		nbIt ++;
	}

	@Override
	public void updateScore() {
		for (Arm a: arms){
			double empAverage = a.sumRewards/(1.0*a.numberPlayed);
			a.score=empAverage+Math.sqrt(2*Math.log(nbIt)/(1.0*a.numberPlayed)); 
		}
	}


	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorCUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	public class scoreComparatorCUCB implements Comparator<Arm>
	{
		public int compare(Arm a1,Arm a2){

			if (a1.numberPlayed==0 && a2.numberPlayed==0){
				return 0;
			}
			else if(a1.numberPlayed==0 && a2.numberPlayed!=0){
				return -1;
			}
			else if(a1.numberPlayed!=0 && a2.numberPlayed==0){
				return 1;
			}
			else{
				if(a1.score>a2.score){
					return -1;
				}
				if(a1.score<a2.score){
					return 1;
				}	
			}
			return 0;
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		for (Arm a:this.arms){
			a.reinitArm();
		}
	}

	@Override
	public String toString() {
		return "CUCBPolicy";
	}
}

class CUCBVPolicy extends Policy{

	public CUCBVPolicy() {
		super();
	}

	@Override
	public void updateArmParameter() {
		for (Arm a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.numberPlayed++;
		}
		nbIt ++;
	}

	@Override
	public void updateScore() {
		for (Arm a: arms){
			double empAverage = a.sumRewards/(1.0*a.numberPlayed);
			double empVariance=a.sumSqrtRewards/(1.0*a.numberPlayed)-empAverage*empAverage;
			a.score=empAverage+Math.sqrt(2*empVariance*Math.log(nbIt)/(1.0*a.numberPlayed))+3*Math.log(nbIt)/(a.numberPlayed); 
		}
	}


	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorCUCBV());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	public class scoreComparatorCUCBV implements Comparator<Arm>
	{
		public int compare(Arm a1,Arm a2){

			if (a1.numberPlayed==0 && a2.numberPlayed==0){
				return 0;
			}
			else if(a1.numberPlayed==0 && a2.numberPlayed!=0){
				return -1;
			}
			else if(a1.numberPlayed!=0 && a2.numberPlayed==0){
				return 1;
			}
			else{
				if(a1.score>a2.score){
					return -1;
				}
				if(a1.score<a2.score){
					return 1;
				}	
			}
			return 0;
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		for (Arm a:this.arms){
			a.reinitArm();
		}
	}

	@Override
	public String toString() {
		return "CUCBVPolicy";
	}
}

class CUCBTunedPolicy extends Policy{

	public CUCBTunedPolicy() {
		super();
	}

	@Override
	public void updateArmParameter() {
		for (Arm a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.numberPlayed++;
		}
		nbIt ++;
	}

	@Override
	public void updateScore() {
		for (Arm a: arms){
			double empAverage = a.sumRewards/(1.0*a.numberPlayed);
			double empVariance=a.sumSqrtRewards/(1.0*a.numberPlayed)-empAverage*empAverage;
			a.score=empAverage+Math.sqrt(
					Math.min(1/4, (empVariance+Math.sqrt(2.0*Math.log(nbIt)/(1.0*a.numberPlayed))/(1.0*a.numberPlayed))*Math.log(nbIt)/(1.0*a.numberPlayed))
					); 
		}
	}


	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorCUCBV());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	public class scoreComparatorCUCBV implements Comparator<Arm>
	{
		public int compare(Arm a1,Arm a2){

			if (a1.numberPlayed==0 && a2.numberPlayed==0){
				return 0;
			}
			else if(a1.numberPlayed==0 && a2.numberPlayed!=0){
				return -1;
			}
			else if(a1.numberPlayed!=0 && a2.numberPlayed==0){
				return 1;
			}
			else{
				if(a1.score>a2.score){
					return -1;
				}
				if(a1.score<a2.score){
					return 1;
				}	
			}
			return 0;
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		for (Arm a:this.arms){
			a.reinitArm();
		}
	}

	@Override
	public String toString() {
		return "CUCBTunedPolicy";
	}
}

class kLinUCB extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public double alpha=2.0;
	public int sizeFeatures;
	public double priorFactorBeta;
	

	
	public kLinUCB(int sizeFeatures,double priorFactorBeta, double alpha) {
		super();
		this.alpha=alpha;
		this.priorFactorBeta=priorFactorBeta;
		this.sizeFeatures=sizeFeatures;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.initMatrix();
	}
	public kLinUCB(int sizeFeatures,double priorFactorBetaa) {
		this(sizeFeatures,priorFactorBetaa,2.0);
	}
	
	public kLinUCB(int sizeFeatures) {
		this(sizeFeatures,1.0);
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


	@Override
	public void updateArmParameter() {
		for(Arm a:lastSelected)	{
			A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext));
			b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar);
		}
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		nbIt ++;
	}

	@Override
	public void updateScore() {
		for(Arm a:arms){
			a.score=
					a.CurrentContext.dotProduct(beta)+
					alpha*Math.sqrt(
							a.CurrentContext.dotProduct(invA0.operate(a.CurrentContext))
							);		
		}
	}

	public class scoreComparatorkLinUCB implements Comparator<Arm>
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
	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorkLinUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "kLinUCB"+alpha;
	}
}


class ThompsonHybridkLin extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public boolean optimistic; 
	public double priorFactorBeta;
	public int sizeFeatures;

	public ThompsonHybridkLin(int sizeFeatures,double priorFactorBeta,boolean optimistic) {
		super();
		this.optimistic=optimistic;
		this.priorFactorBeta= priorFactorBeta;
		this.sizeFeatures=sizeFeatures;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.initMatrix();
		//System.out.println("gamma "+gamma);
	}

	
	public ThompsonHybridkLin(int sizeFeatures) {
		this(sizeFeatures, 1.0, true);
	}
	
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {
		for(Arm a:lastSelected)	{
			A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext));
			b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar)+a.thetaStar;
		}
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		nbIt ++;
	}

	@Override
	public void updateScore() {
		MultivariateNormalDistribution sBeta = new MultivariateNormalDistribution(beta.toArray(),invA0.getData());
		RealVector sampleBeta = new ArrayRealVector(sBeta.sample());
		for(Arm a:arms){
			NormalDistribution n = new NormalDistribution(a.sumRewards/(a.numberPlayed+1),1.0/(a.numberPlayed+1));
			if(this.optimistic){
				a.score=
						Math.max(a.CurrentContext.dotProduct(sampleBeta)+n.sample(),a.CurrentContext.dotProduct(beta)+a.sumRewards/(a.numberPlayed+1));
			}
			else{
				a.score=
						a.CurrentContext.dotProduct(sampleBeta)+n.sample();
			}

		}
	}

	public class scoreComparatorThompsonkLin implements Comparator<Arm>
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

	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorThompsonkLin());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "ThompsonHybridkLin";
	}
}

class ThompsonkLin extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public boolean optimistic; 
	public double priorFactorBeta;
	public int sizeFeatures;

	public ThompsonkLin(int sizeFeatures,double priorFactorBeta,boolean optimistic) {
		super();
		this.priorFactorBeta= priorFactorBeta;
		this.optimistic=optimistic;
		this.sizeFeatures=sizeFeatures;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.initMatrix();
		//System.out.println("gamma "+gamma);
	}

	
	public ThompsonkLin(int sizeFeatures) {
		this(sizeFeatures, 1.0,true);
	}
	
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {
		for(Arm a:lastSelected)	{
			A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext));
			b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar);
		}
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		nbIt ++;
	}

	@Override
	public void updateScore() {
		MultivariateNormalDistribution sBeta = new MultivariateNormalDistribution(beta.toArray(),invA0.getData());
		RealVector sampleBeta = new ArrayRealVector(sBeta.sample());
		for(Arm a:arms){
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

	public class scoreComparatorThompsonkLin implements Comparator<Arm>
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

	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorThompsonkLin());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "ThompsonkLin"+optimistic;
	}
}

class kHybridLinUCB extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	double alpha=2.0;
	public int sizeFeatures;
	public double priorFactorBeta;
	
	public kHybridLinUCB(int sizeFeatures,double priorFactorBeta) {
		super();
		this.priorFactorBeta=priorFactorBeta;
		this.sizeFeatures=sizeFeatures;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.initMatrix();
	}
	
	public kHybridLinUCB(int sizeFeatures) {
		this(sizeFeatures,1.0);
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


	@Override
	public void updateArmParameter() {
		for(Arm a:lastSelected)	{
			A0=A0.add(a.SumContext.outerProduct(a.SumContext).scalarMultiply(1.0/(a.numberPlayed+1)));
			b0=b0.add(a.SumContext.mapMultiply(a.sumRewards/(a.numberPlayed+1)));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar)+a.thetaStar;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.SumContext=a.SumContext.add(a.CurrentContext);
			A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).subtract(a.SumContext.outerProduct(a.SumContext).scalarMultiply(1.0/(a.numberPlayed+1))));
			b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward).subtract(a.SumContext.mapMultiply(a.sumRewards/(a.numberPlayed+1))));

		}


		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);

		for(Arm a:arms)	{
			a.theta=(a.sumRewards-a.SumContext.dotProduct(beta))/(a.numberPlayed+1);
		}


		nbIt ++;
	}

	@Override
	public void updateScore() {
		for(Arm a:arms){
			a.score=
					a.CurrentContext.dotProduct(beta)+a.theta+
					alpha*Math.sqrt(
							1.0/(a.numberPlayed+1)+a.CurrentContext.dotProduct(invA0.operate(a.CurrentContext))
							-2*a.CurrentContext.dotProduct(invA0.operate(a.SumContext))/(a.numberPlayed+1)
							+a.SumContext.dotProduct(invA0.operate(a.SumContext))/((a.numberPlayed+1)*(a.numberPlayed+1))
							);		
		}
	}

	public class scoreComparatorkHybridLinUCB implements Comparator<Arm>
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
	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorkHybridLinUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "kHybridLinUCB";
	}
}


class OptContextual extends Policy{


	public int sizeFeatures;

	public OptContextual(int sizeFeatures) {
		super();
		this.sizeFeatures=sizeFeatures;
	}

	public class scoreComparatorOptCtxt implements Comparator<Arm>
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
	
	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorOptCtxt());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
	}

	@Override
	public String toString() {
		return "OptContextual";
	}

	@Override
	public void updateScore() {
		for(Arm a: arms){
			a.score=a.CurrentContext.dotProduct(betaStar)+a.thetaStar;
		}
		
	}

	@Override
	public void updateArmParameter() {
		for (Arm a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar)+a.thetaStar;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.numberPlayed++;
		}
		nbIt ++;
		
	}
}





class kPartialHybridLinUCBsameScores extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public RealMatrix GammaBeta;//conf int noncentered chi2
	double confidenceInterval; 
	double confidenceInterval2;
	double alpha; //conf int normal
	double gamma; //conf int centered chi2
	public int sizeFeatures;
	public int choiceId;
	public double priorFactorBeta;

	public kPartialHybridLinUCBsameScores(int sizeFeatures,  double priorFactorBeta,double confidenceInterval,double confidenceInterval2, int choiceId) {
		super();
		this.choiceId=choiceId;
		this.priorFactorBeta=priorFactorBeta;
		this.sizeFeatures=sizeFeatures;
		this.confidenceInterval=confidenceInterval;
		this.confidenceInterval2=confidenceInterval2;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.GammaBeta=new Array2DRowRealMatrix(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.alpha=invertNormalG(confidenceInterval);
		this.gamma=invertCenteredChi2(confidenceInterval2);
		this.initMatrix();
	}
	
	public kPartialHybridLinUCBsameScores(int sizeFeatures,  double confidenceInterval, double confidenceInterval2,int choiceId) {
		this(sizeFeatures, 1.0, confidenceInterval, confidenceInterval2,choiceId);
	}
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {

		for(Arm a:observedArms)	{
			//a.numberObserved++;
			a.SumContext=a.SumContext.add(a.CurrentContext);
			a.SumProdContext=a.SumProdContext.add(a.CurrentContext.outerProduct(a.CurrentContext));
			for(int i=0;i<sizeFeatures;i++){
				a.MeanContext[i]=a.SumContext.getEntry(i)/(a.numberObserved);
				for(int j=0;j<sizeFeatures;j++){
					if(i==j){
						a.CovMatrix[i][j]=0.01+a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
					else{
						a.CovMatrix[i][j]=a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
				}
			}

		}

		for(Arm a:lastSelected)	{
			a.numberPlayed++;
			a.sumRewards+=a.lastReward;
			a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar)+a.thetaStar;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
		}

		for(Arm a:lastSelected)	{
			if(observedArms.contains(a)){
				A0=A0.add(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1)));
				b0=b0.add(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1)));
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs+=a.lastReward;
				a.SumContextOnObs=a.SumContextOnObs.add(a.CurrentContext);
				A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).subtract(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1))));
				b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward).subtract(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1))));			}
		}



		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);

		/*for(Arm a:arms)	{
			a.theta=(a.sumRewards-a.SumContext.dotProduct(beta))/(a.numberPlayed+1);
		}*/


		nbIt ++;
	}

	@Override
	public void updateScore() {
		double uBondBeta = getUbondBeta();
		for(Arm a:arms){
			if(observedArms.contains(a)){
				a.uBondCtxt =getUbondCtxt(a);
			}

			if(a.numberObserved<=2){
				a.score=2000000.0;
			}

			else {
				
				switch (choiceId) {
				case 0:  		
					a.uBondRwd= getUbondRwd(a);
					if(observedArms.contains(a)){
						
						a.score=
								a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
								a.uBondRwd+
								Math.sqrt(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1))))*uBondBeta;
						/*System.out.println("InLast: "+a.Id+" "+a.numberPlayed+" Exploit: "+a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+a.sumRewards/(a.numberPlayed+1)+" Explo: "+alpha*Math.sqrt(
								1.0/(a.numberPlayedOnObs+1)+
								a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
								));*/
						//System.out.println("InLast: "+a.Id+" "+a.numberPlayed+" score "+a.score);
					}
					else{
						a.score=
								a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
								a.uBondRwd+
								a.uBondCtxt*uBondBeta;
						//System.out.println("NotInLast: "a.Id+" "+a.numberPlayed+" Exploit: "+a.sumRewards/(a.numberPlayed+1)+" Explo: "+a.uBondRwd+a.uBondCtxt*uBondBeta);
						//System.out.println("NotInLast: "+a.Id+" "+a.numberPlayed+" score "+a.score);
					}
					
					break;
				case 1:  
					if(observedArms.contains(a)){
						a.score=
								a.sumRewards/(a.numberPlayed+1)+
								a.uBondRwd+
								Math.sqrt(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1))))*uBondBeta;
					}
					else{
						a.uBondRwd= getUbondRwd(a);
						a.score=a.sumRewards/(a.numberPlayed+1)+a.uBondRwd+a.uBondCtxt*uBondBeta;
					}
					break;

				default: break;

				}

			}
		}
		//System.out.println();

	}

	public class scoreComparatorPartialkHybridLinUCB implements Comparator<Arm>
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

	public static double invertNormalG (double confInt ){
		NormalDistribution n = new NormalDistribution();	
		return n.inverseCumulativeProbability((confInt+1)/(2.0));
	}

	public static double invertCenteredChi2 (double confInt ){
		NormalDistribution n = new NormalDistribution();
		double val =  n.inverseCumulativeProbability(confInt);
		return Math.pow(Math.sqrt(2.0/9.0)*val+1.0-2.0/9.0, 3.0);
	}

	public static double invertNonCenteredChi2 (double lambda, double confInt){
		NormalDistribution n = new NormalDistribution();
		double h = 1.0-2.0/3.0*(1.0+lambda)*(1.0+3.0*lambda)/((1.0+2.0*lambda)*(1.0+2.0*lambda));
		double p = (1.0+2.0*lambda)/((1.0+lambda)*(1.0+lambda));
		double m = (h-1.0)*(1.0-3.0*h);
		double val = n.inverseCumulativeProbability(confInt);
		return Math.pow((h*Math.sqrt(2*p)*(1+0.5*m*p))*val+(1+h*p*(h-1-0.5*(2-h)*m*p)), 1.0/h)*(lambda+1);
	}

	public double getUbondRwd(Arm a){
		switch (choiceId) {
		case 0:  				
			return alpha* Math.sqrt(1.0/(1+a.numberPlayedOnObs));

		case 1:  
			return alpha* Math.sqrt(1.0/(1+a.numberPlayed));
		}
		return 0.0;

	}

	public double getUbondCtxt(Arm a){
		/*Matrix M = new Array2DRowRealMatrix(a.CovMatrix);
		EigenDecompositor eig = new EigenDecompositor(M);
		Matrix D = eig.decompose()[1];*/

		RealMatrix Mat = new Array2DRowRealMatrix(a.CovMatrix);
		EigenDecomposition eigMat = new EigenDecomposition(Mat);
		RealMatrix dMat = eigMat.getD();
		return Math.sqrt(dMat.getTrace()*gamma);
	}

	public double getUbondBeta(){

		/*EigenDecompositor eig = new EigenDecompositor(invA0);
		Matrix[] dec =  eig.decompose();
		Matrix D = dec[1];
		Matrix P = dec[0];
		Vector Z=P.multiply(beta);*/


		EigenDecomposition eigMat1 = new EigenDecomposition(invA0);
		RealMatrix dMat1 = eigMat1.getD();
		RealMatrix pMat1 = eigMat1.getV();
		RealVector zMat1 = pMat1.operate(beta);

		double lambda;
		double sum=0.0;
		for(int i = 0;i<sizeFeatures;i++){
			lambda=Math.pow(zMat1.getEntry(i),2.0)/dMat1.getEntry(i,i);
			sum+=dMat1.getEntry(i,i)*invertNonCenteredChi2(lambda, confidenceInterval2);
		}
		return Math.sqrt(sum);
	}



	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorPartialkHybridLinUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "kPartialHybridLinUCBsameScores_ChoiceId_"+this.choiceId+"_confint_"+this.confidenceInterval+"_confint2_"+this.confidenceInterval2;
	}
}




class NaivekPartialHybridLinUCBMean extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public RealVector GammaBeta;//conf int noncentered chi2
	double confidenceInterval; 
	double alpha; //conf int normal
	public int sizeFeatures;
	public int choiceId;
	public double priorFactorBeta;

	public NaivekPartialHybridLinUCBMean(int sizeFeatures, double priorFactorBeta, double confidenceInterval, int choiceId) {
		super();
		this.sizeFeatures=sizeFeatures;
		this.priorFactorBeta=priorFactorBeta;
		this.confidenceInterval=confidenceInterval;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.GammaBeta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.alpha=invertNormalG(confidenceInterval);
		this.choiceId=choiceId;
		this.initMatrix();
		//System.out.println("gamma "+gamma);
	}
	
	public NaivekPartialHybridLinUCBMean(int sizeFeatures,  double confidenceInterval,int choiceId) {
		this(sizeFeatures, 1.0, confidenceInterval,choiceId);
	}
	
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {

		for(Arm a:observedArms)	{
			//a.numberObserved++;
			a.SumContext=a.SumContext.add(a.CurrentContext);
			a.SumProdContext=a.SumProdContext.add(a.CurrentContext.outerProduct(a.CurrentContext));
			for(int i=0;i<sizeFeatures;i++){
				a.MeanContext[i]=a.SumContext.getEntry(i)/(a.numberObserved);
				for(int j=0;j<sizeFeatures;j++){
					if(i==j){
						a.CovMatrix[i][j]=0.01+a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
					else{
						a.CovMatrix[i][j]=a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
				}
			}

		}

		for(Arm a:lastSelected)	{
			a.numberPlayed++;
			a.sumRewards+=a.lastReward;
			//a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar)+a.thetaStar;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
		}

		for(Arm a:lastSelected)	{
			if(observedArms.contains(a)){
				A0=A0.add(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1)));
				b0=b0.add(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1)));
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs+=a.lastReward;
				a.SumContextOnObs=a.SumContextOnObs.add(a.CurrentContext);
				A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).subtract(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1))));
				b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward).subtract(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1))));			}
		}


		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		/*for(Arm a:arms)	{
			a.theta=(a.sumRewards-a.SumContext.dotProduct(beta))/(a.numberPlayed+1);
		}*/


		nbIt ++;
	}

	@Override
	public void updateScore() {
		for(Arm a:arms){
			
			if(a.numberObserved<=2){
				a.score=2000000.0;
			}

			else {
				switch (choiceId) {
				case 0:  				
					if(observedArms.contains(a)){
						a.score=
								a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+
								a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayedOnObs+1)+
										a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
										);
					}
					else{
						//System.out.println("hussSample");
						a.score=
								a.SumContext.mapDivide(1.0*a.numberObserved).subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+
								a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayedOnObs+1)+
										a.SumContext.mapDivide(1.0*a.numberObserved).subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.SumContext.mapDivide(1.0*a.numberObserved).subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
										);	
					}
					
					break;
				case 1:  
					if(lastSelected.contains(a)){
						a.score=
								a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+
								a.sumRewards/(a.numberPlayed+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayed+1)+
										a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
										);	
					}
					else{
						a.score=
								a.SumContext.mapDivide(1.0*a.numberObserved).subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+
								a.sumRewards/(a.numberPlayed+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayed+1)+
										a.SumContext.mapDivide(1.0*a.numberObserved).subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.SumContext.mapDivide(1.0*a.numberObserved).subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
										);	
					}
					break;

				default: break;

				}
			}
		}
	}

	public class scoreComparatorPartialkHybridLinUCB implements Comparator<Arm>
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

	public static double invertNormalG (double confInt ){
		NormalDistribution n = new NormalDistribution();	
		return n.inverseCumulativeProbability((confInt+1)/(2.0));
	}




	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		Collections.sort(arms,new scoreComparatorPartialkHybridLinUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "NaivekPartialHybridLinUCBMean_ChoiceId_"+this.choiceId+"_confint_"+this.confidenceInterval;
	}
}


class kPartialHybridLinUCBdiffScores extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public RealMatrix GammaBeta;//conf int noncentered chi2
	double confidenceInterval; 
	double confidenceInterval2; 
	double alpha; //conf int normal
	double gamma; //conf int centered chi2
	public int sizeFeatures;
	public int choiceId;
	public double priorFactorBeta;

	public kPartialHybridLinUCBdiffScores(int sizeFeatures, double priorFactorBeta, double confidenceInterval, double confidenceInterval2,int choiceId) {
		super();
		this.priorFactorBeta=priorFactorBeta;
		this.choiceId=choiceId;
		this.sizeFeatures=sizeFeatures;
		this.confidenceInterval=confidenceInterval;
		this.confidenceInterval2=confidenceInterval2;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.GammaBeta=new Array2DRowRealMatrix(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.alpha=invertNormalG(confidenceInterval);
		this.gamma=invertCenteredChi2(confidenceInterval);
		this.initMatrix();
	}
	
	public kPartialHybridLinUCBdiffScores(int sizeFeatures,  double confidenceInterval, double confidenceInterval2,int choiceId) {
		this(sizeFeatures, 1.0, confidenceInterval, confidenceInterval2,choiceId);
	}
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {

		for(Arm a:observedArms)	{
			//a.numberObserved++;
			a.SumContext=a.SumContext.add(a.CurrentContext);
			a.SumProdContext=a.SumProdContext.add(a.CurrentContext.outerProduct(a.CurrentContext));
			for(int i=0;i<sizeFeatures;i++){
				a.MeanContext[i]=a.SumContext.getEntry(i)/(a.numberObserved);
				for(int j=0;j<sizeFeatures;j++){
					if(i==j){
						a.CovMatrix[i][j]=0.01+a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
					else{
						a.CovMatrix[i][j]=a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
				}
			}

		}

		for(Arm a:lastSelected)	{
			a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar)+a.thetaStar;
			a.numberPlayed++;
			a.sumRewards+=a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
		}

		for(Arm a:lastSelected)	{
			if(observedArms.contains(a)){
				A0=A0.add(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1)));
				b0=b0.add(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1)));
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs+=a.lastReward;
				a.SumContextOnObs=a.SumContextOnObs.add(a.CurrentContext);
				A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).subtract(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1))));
				b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward).subtract(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1))));			}
		}


		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);

		/*for(Arm a:arms)	{
			a.theta=(a.sumRewards-a.SumContext.dotProduct(beta))/(a.numberPlayed+1);
		}*/


		nbIt ++;
	}

	@Override
	public void updateScore() {
		double uBondBeta = getUbondBeta();
		for(Arm a:arms){
			if(observedArms.contains(a)){
				a.uBondCtxt =getUbondCtxt(a);
			}

			if(a.numberObserved<=10){
				a.score=2000000.0;
			}

			else {
				
				switch (choiceId) {
				case 0:  				
					if(observedArms.contains(a)){
						a.score=//essayer en majorant le prod sca pour etre egale aavec le cas ou on voit aps
								a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+
								a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayedOnObs+1)+
										a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
										);	
						/*System.out.println("InLast: "+a.Id+" "+a.numberPlayed+" Exploit: "+a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+a.sumRewards/(a.numberPlayed+1)+" Explo: "+alpha*Math.sqrt(
								1.0/(a.numberPlayedOnObs+1)+
								a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
								));*/
						//System.out.println("InLast: "+a.Id+" "+a.numberPlayed+" score "+a.score);
					}
					else{
						a.uBondRwd= getUbondRwd(a);
						a.score=a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+a.uBondRwd+a.uBondCtxt*uBondBeta;
						//System.out.println("NotInLast: "a.Id+" "+a.numberPlayed+" Exploit: "+a.sumRewards/(a.numberPlayed+1)+" Explo: "+a.uBondRwd+a.uBondCtxt*uBondBeta);
						//System.out.println("NotInLast: "+a.Id+" "+a.numberPlayed+" score "+a.score);
					}
					
					break;
				case 1:  
					if(observedArms.contains(a)){
						a.score=
								a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+
								a.sumRewards/(a.numberPlayed+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayed+1)+
										a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
										);		
					}
					else{
						a.uBondRwd= getUbondRwd(a);
						a.score=a.sumRewards/(a.numberPlayed+1)+a.uBondRwd+a.uBondCtxt*uBondBeta;
					}
					break;

				default: break;

				}

			}
		}
		//System.out.println();

	}

	public class scoreComparatorPartialkHybridLinUCB implements Comparator<Arm>
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

	public static double invertNormalG (double confInt ){
		NormalDistribution n = new NormalDistribution();	
		return n.inverseCumulativeProbability((confInt+1)/(2.0));
	}

	public static double invertCenteredChi2 (double confInt ){
		NormalDistribution n = new NormalDistribution();
		double val =  n.inverseCumulativeProbability(confInt);
		return Math.pow(Math.sqrt(2.0/9.0)*val+1.0-2.0/9.0, 3.0);
	}

	public static double invertNonCenteredChi2 (double lambda, double confInt){
		NormalDistribution n = new NormalDistribution();
		double h = 1.0-2.0/3.0*(1.0+lambda)*(1.0+3.0*lambda)/((1.0+2.0*lambda)*(1.0+2.0*lambda));
		double p = (1.0+2.0*lambda)/((1.0+lambda)*(1.0+lambda));
		double m = (h-1.0)*(1.0-3.0*h);
		double val = n.inverseCumulativeProbability(confInt);
		return Math.pow((h*Math.sqrt(2*p)*(1+0.5*m*p))*val+(1+h*p*(h-1-0.5*(2-h)*m*p)), 1.0/h)*(lambda+1);
	}

	public double getUbondRwd(Arm a){
		switch (choiceId) {
		case 0:  				
			return alpha* Math.sqrt(1.0/(1+a.numberPlayedOnObs));

		case 1:  
			return alpha* Math.sqrt(1.0/(1+a.numberPlayed));
		}
		return 0.0;

	}

	public double getUbondCtxt(Arm a){
		/*Matrix M = new Array2DRowRealMatrix(a.CovMatrix);
		EigenDecompositor eig = new EigenDecompositor(M);
		Matrix D = eig.decompose()[1];*/

		RealMatrix Mat = new Array2DRowRealMatrix(a.CovMatrix);
		EigenDecomposition eigMat = new EigenDecomposition(Mat);
		RealMatrix dMat = eigMat.getD();
		return Math.sqrt(dMat.getTrace()*gamma);
	}

	public double getUbondBeta(){

		/*EigenDecompositor eig = new EigenDecompositor(invA0);
		Matrix[] dec =  eig.decompose();
		Matrix D = dec[1];
		Matrix P = dec[0];
		Vector Z=P.multiply(beta);*/


		EigenDecomposition eigMat1 = new EigenDecomposition(invA0);
		RealMatrix dMat1 = eigMat1.getD();
		RealMatrix pMat1 = eigMat1.getV();
		RealVector zMat1 = pMat1.operate(beta);

		double lambda;
		double sum=0.0;
		for(int i = 0;i<sizeFeatures;i++){
			lambda=Math.pow(zMat1.getEntry(i),2.0)/dMat1.getEntry(i,i);
			sum+=dMat1.getEntry(i,i)*invertNonCenteredChi2(lambda, confidenceInterval2);
		}
		return Math.sqrt(sum);
	}



	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorPartialkHybridLinUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "kPartialHybridLinUCBdiffScores_ChoiceId_"+this.choiceId+"_confint_"+this.confidenceInterval+"_confint2_"+this.confidenceInterval2;
	}
}

class NaivekPartialHybridLinUCBSample extends Policy{
	
	
	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public RealVector GammaBeta;//conf int noncentered chi2
	double confidenceInterval; 
	double alpha; //conf int normal
	int choiceId;
	public double priorFactorBeta;
	public int nbSample=10;


	public int sizeFeatures;

	public NaivekPartialHybridLinUCBSample(int sizeFeatures, double priorFactorBeta, double confidenceInterval, int nbObserved,int choiceId) {
		super();
		this.nbObserved=nbObserved;
		this.choiceId=choiceId;
		this.priorFactorBeta=priorFactorBeta;
		this.sizeFeatures=sizeFeatures;
		this.confidenceInterval=confidenceInterval;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.GammaBeta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.alpha=invertNormalG(confidenceInterval);
		this.initMatrix();
		//System.out.println("gamma "+gamma);
	}
	
	public NaivekPartialHybridLinUCBSample(int sizeFeatures,  double confidenceInterval,int nbObserved,int choiceId) {
		this(sizeFeatures, 1.0, confidenceInterval,  nbObserved,choiceId);
	}
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {

		for(Arm a:observedArms)	{
			//a.numberObserved++;
			a.SumContext=a.SumContext.add(a.CurrentContext);
			a.SumProdContext=a.SumProdContext.add(a.CurrentContext.outerProduct(a.CurrentContext));
			for(int i=0;i<sizeFeatures;i++){
				a.MeanContext[i]=a.SumContext.getEntry(i)/(a.numberObserved);
				for(int j=0;j<sizeFeatures;j++){
					if(i==j){
						a.CovMatrix[i][j]=0.01+a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
					else{
						a.CovMatrix[i][j]=a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
				}
			}

		}

		for(Arm a:lastSelected)	{
			a.numberPlayed++;
			a.sumRewards+=a.lastReward;
			//a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar)+a.thetaStar;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
		}

		for(Arm a:lastSelected)	{
			if(observedArms.contains(a)){
				A0=A0.add(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1)));
				b0=b0.add(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1)));
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs+=a.lastReward;
				a.SumContextOnObs=a.SumContextOnObs.add(a.CurrentContext);
				A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).subtract(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1))));
				b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward).subtract(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1))));			}
		}


		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);

		/*for(Arm a:arms)	{
			a.theta=(a.sumRewards-a.SumContext.dotProduct(beta))/(a.numberPlayed+1);
		}*/


		nbIt ++;
	}

	@Override
	public void updateScore() {
		for(Arm a:arms){
			if(a.numberObserved<=2){
				a.score=2000000.0;
			}

			else {
				switch (choiceId) {
				case 0:  				
					if(observedArms.contains(a)){
						a.score=
								a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+
								a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayedOnObs+1)+
										a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
										);		
					}
					else{
						//System.out.println("hussSample");
						MultivariateNormalDistribution dist = new MultivariateNormalDistribution(a.MeanContext,a.CovMatrix);
						a.score=0.0;
						double exploit=0.0;
						double explore =0.0;
						for(int i=0;i<nbSample;i++){
							RealVector sampleContext = new ArrayRealVector(dist.sample());
							exploit+=sampleContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+a.sumRewardsOnObs/(a.numberPlayedOnObs+1);
							explore+=1.0/(a.numberPlayedOnObs+1)+
									sampleContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(sampleContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))));
							a.score=(exploit+alpha*Math.sqrt(explore))/nbSample;
							/*a.score+=
									(sampleContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+
									a.sumRewardsOnObs/(a.numberPlayedOnObs+1)+
									alpha*Math.sqrt(
											1.0/(a.numberPlayedOnObs+1)+
											sampleContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(sampleContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
											))/nbSample;*/
						}
						
						
					}
					break;
				case 1:  
					if(observedArms.contains(a)){
						a.score=
								a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+
								a.sumRewards/(a.numberPlayed+1)+
								alpha*Math.sqrt(
										1.0/(a.numberPlayed+1)+
										a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
										);	
					}
					else{
						MultivariateNormalDistribution dist = new MultivariateNormalDistribution(a.MeanContext,a.CovMatrix);
						a.score=0.0;
						for(int i=0;i<nbSample;i++){
							RealVector sampleContext = new ArrayRealVector(dist.sample());
							a.score+=
									(sampleContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(beta)+
									a.sumRewards/(a.numberPlayed+1)+
									alpha*Math.sqrt(
											1.0/(a.numberPlayedOnObs+1)+
											sampleContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct((invA0.operate(sampleContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)))))
											))/nbSample;
						}
					}
					break;

				default: break;

				}

			}
		}

	}

	public class scoreComparatorPartialkHybridLinUCB implements Comparator<Arm>
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

	public static double invertNormalG (double confInt ){
		NormalDistribution n = new NormalDistribution();	
		return n.inverseCumulativeProbability((confInt+1)/(2.0));
	}






	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorPartialkHybridLinUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "NaivekPartialHybridLinUCBSample_"+this.choiceId+"_confint_"+this.confidenceInterval+"_"+this.nbObserved;
	}
}


class ThompsonkPartialHybridLin extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	int choiceId;
	public double priorFactorBeta;
	public int sizeFeatures;

	public ThompsonkPartialHybridLin(int sizeFeatures,double priorFactorBeta, int choiceId) {
		super();
		this.priorFactorBeta= priorFactorBeta;
		this.choiceId=choiceId;
		this.sizeFeatures=sizeFeatures;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.initMatrix();
		//System.out.println("gamma "+gamma);
	}

	
	public ThompsonkPartialHybridLin(int sizeFeatures,int choiceId) {
		this(sizeFeatures, 1.0,choiceId);
	}
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {

		for(Arm a:observedArms)	{
			//a.numberObserved++;
			a.SumContext=a.SumContext.add(a.CurrentContext);
			a.SumProdContext=a.SumProdContext.add(a.CurrentContext.outerProduct(a.CurrentContext));
			for(int i=0;i<sizeFeatures;i++){
				a.MeanContext[i]=a.SumContext.getEntry(i)/(a.numberObserved);
				for(int j=0;j<sizeFeatures;j++){
					if(i==j){
						a.CovMatrix[i][j]=0.01+a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
					else{
						a.CovMatrix[i][j]=a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
				}
			}

		}

		for(Arm a:lastSelected)	{
			a.numberPlayed++;
			a.sumRewards+=a.lastReward;
			//a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar)+a.thetaStar;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
		}

		for(Arm a:lastSelected)	{
			if(observedArms.contains(a)){
				A0=A0.add(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1)));
				b0=b0.add(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1)));
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs+=a.lastReward;
				a.SumContextOnObs=a.SumContextOnObs.add(a.CurrentContext);
				A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).subtract(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1))));
				b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward).subtract(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1))));			}
		}


		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);

		/*for(Arm a:arms)	{
			a.theta=(a.sumRewards-a.SumContext.dotProduct(beta))/(a.numberPlayed+1);
		}*/


		nbIt ++;
	}

	@Override
	public void updateScore() {
		MultivariateNormalDistribution sBeta = new MultivariateNormalDistribution(beta.toArray(),invA0.getData());
		RealVector sampleBeta = new ArrayRealVector(sBeta.sample());
		for(Arm a:arms){
			if(a.numberObserved<=2){
				a.score=2000000.0;
			}

			else {			
				switch (choiceId) {
				case 0:  	
					NormalDistribution n = new NormalDistribution(a.sumRewardsOnObs/(a.numberPlayedOnObs+1),1.0/(a.numberPlayedOnObs+1));
					if(observedArms.contains(a)){
						a.score=
								a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(sampleBeta)+
								n.sample();	
					}
					else{
						MultivariateNormalDistribution dist = new MultivariateNormalDistribution(a.MeanContext,a.CovMatrix);
						RealVector sampleContext = new ArrayRealVector(dist.sample());
						a.score=
								sampleContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(sampleBeta)+
								n.sample();
					}
					break;
				case 1:  
					NormalDistribution n1 = new NormalDistribution(a.sumRewards/(a.numberPlayed+1),1.0/(a.numberPlayed+1));
					if(observedArms.contains(a)){
						a.score=
								a.CurrentContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(sampleBeta)+
								n1.sample();	
					}
					else{
						MultivariateNormalDistribution dist = new MultivariateNormalDistribution(a.MeanContext,a.CovMatrix);
						RealVector sampleContext = new ArrayRealVector(dist.sample());
						a.score=
								sampleContext.subtract(a.SumContextOnObs.mapDivide(1.0*a.numberPlayedOnObs+1)).dotProduct(sampleBeta)+
								n1.sample();
					}
					break;

				default: break;

				}

			}
		}

	}

	public class scoreComparatorPartialkHybridLinUCB implements Comparator<Arm>
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








	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorPartialkHybridLinUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "ThompsonkPartialHybridLin_ChoiceId_"+this.choiceId;
	}
}


class ThompsonkPartialLin extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	int choiceId;
	public double priorFactorBeta;
	public int sizeFeatures;

	public ThompsonkPartialLin(int sizeFeatures,double priorFactorBeta) {
		super();
		this.priorFactorBeta= priorFactorBeta;

		this.sizeFeatures=sizeFeatures;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.initMatrix();
		//System.out.println("gamma "+gamma);
	}

	
	public ThompsonkPartialLin(int sizeFeatures) {
		this(sizeFeatures, 1.0);
	}
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {

		for(Arm a:observedArms)	{
			//a.numberObserved++;
			a.SumContext=a.SumContext.add(a.CurrentContext);
			a.SumProdContext=a.SumProdContext.add(a.CurrentContext.outerProduct(a.CurrentContext));
			for(int i=0;i<sizeFeatures;i++){
				a.MeanContext[i]=a.SumContext.getEntry(i)/(a.numberObserved);
				for(int j=0;j<sizeFeatures;j++){
					if(i==j){
						a.CovMatrix[i][j]=0.01+a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
					else{
						a.CovMatrix[i][j]=a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
				}
			}

		}

		for(Arm a:lastSelected)	{
			a.numberPlayed++;
			a.sumRewards+=a.lastReward;
			a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar);
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
		}

		for(Arm a:lastSelected)	{
			if(observedArms.contains(a)){
				A0=A0.add(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1)));
				b0=b0.add(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1)));
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs+=a.lastReward;
				a.SumContextOnObs=a.SumContextOnObs.add(a.CurrentContext);
				A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).subtract(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1))));
				b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward).subtract(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1))));			}
		}


		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);

		/*for(Arm a:arms)	{
			a.theta=(a.sumRewards-a.SumContext.dotProduct(beta))/(a.numberPlayed+1);
		}*/


		nbIt ++;
	}

	@Override
	public void updateScore() {
		MultivariateNormalDistribution sBeta = new MultivariateNormalDistribution(beta.toArray(),invA0.getData());
		RealVector sampleBeta = new ArrayRealVector(sBeta.sample());
		for(Arm a:arms){
			if(a.numberObserved<=2){
				a.score=2000000.0;
			}

			else {			  	
					if(observedArms.contains(a)){
						a.score=
								a.CurrentContext.dotProduct(sampleBeta);
					}
					else{
						MultivariateNormalDistribution dist = new MultivariateNormalDistribution(a.MeanContext,a.CovMatrix);
						RealVector sampleContext = new ArrayRealVector(dist.sample());
						a.score=
								sampleContext.dotProduct(sampleBeta);
					}

			}
		}

	}

	public class scoreComparatorPartialkHybridLinUCB implements Comparator<Arm>
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








	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorPartialkHybridLinUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "ThompsonkPartialLin";
	}
}

class NaivekPartialLinUCBMean extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public RealVector GammaBeta;//conf int noncentered chi2
	double confidenceInterval; 
	double alpha; //conf int normal
	public int sizeFeatures;
	public double priorFactorBeta;

	public NaivekPartialLinUCBMean(int sizeFeatures, double priorFactorBeta, double confidenceInterval) {
		super();
		this.sizeFeatures=sizeFeatures;
		this.priorFactorBeta=priorFactorBeta;
		this.confidenceInterval=confidenceInterval;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.GammaBeta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.alpha=invertNormalG(confidenceInterval);

		this.initMatrix();
		//System.out.println("gamma "+gamma);
	}
	
	public NaivekPartialLinUCBMean(int sizeFeatures,  double confidenceInterval) {
		this(sizeFeatures, 1.0, confidenceInterval);
	}
	
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {

		for(Arm a:observedArms)	{
			//a.numberObserved++;
			a.SumContext=a.SumContext.add(a.CurrentContext);
			a.SumProdContext=a.SumProdContext.add(a.CurrentContext.outerProduct(a.CurrentContext));
			for(int i=0;i<sizeFeatures;i++){
				a.MeanContext[i]=a.SumContext.getEntry(i)/(a.numberObserved);
				for(int j=0;j<sizeFeatures;j++){
					if(i==j){
						a.CovMatrix[i][j]=0.01+a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
					else{
						a.CovMatrix[i][j]=a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
				}
			}

		}

		for(Arm a:lastSelected)	{
			a.numberPlayed++;
			a.sumRewards+=a.lastReward;
			a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar);
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
		}

		for(Arm a:lastSelected)	{
			if(observedArms.contains(a)){
				A0=A0.add(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1)));
				b0=b0.add(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1)));
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs+=a.lastReward;
				a.SumContextOnObs=a.SumContextOnObs.add(a.CurrentContext);
				A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).subtract(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1))));
				b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward).subtract(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1))));			}
		}


		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		/*for(Arm a:arms)	{
			a.theta=(a.sumRewards-a.SumContext.dotProduct(beta))/(a.numberPlayed+1);
		}*/


		nbIt ++;
	}

	@Override
	public void updateScore() {
		for(Arm a:arms){
			
			if(a.numberObserved<=2){
				a.score=2000000.0;
			}

			else {
					if(lastSelected.contains(a)){
						a.score=
								a.CurrentContext.dotProduct(beta)+
								alpha*Math.sqrt(
										a.CurrentContext.dotProduct((invA0.operate(a.CurrentContext)))
										);
					}
					else{
						a.score=
								a.SumContext.mapDivide(1.0*a.numberObserved).dotProduct(beta)+
								alpha*Math.sqrt(
										a.SumContext.mapDivide(1.0*a.numberObserved).dotProduct((invA0.operate(a.SumContext.mapDivide(1.0*a.numberObserved))))
										);	
					}
					
			}
		}
	}

	public class scoreComparatorPartialkHybridLinUCB implements Comparator<Arm>
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

	public static double invertNormalG (double confInt ){
		NormalDistribution n = new NormalDistribution();	
		return n.inverseCumulativeProbability((confInt+1)/(2.0));
	}




	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		Collections.sort(arms,new scoreComparatorPartialkHybridLinUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "NaivekPartialLinUCBMean_confint_"+this.confidenceInterval;
	}
}

class NaivekPartialLinUCBSample extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public RealVector GammaBeta;//conf int noncentered chi2
	double confidenceInterval; 
	double alpha; //conf int normal
	public double priorFactorBeta;
	public int nbSample=100;

	public int sizeFeatures;

	public NaivekPartialLinUCBSample(int sizeFeatures, double priorFactorBeta, double confidenceInterval) {
		super();
		this.priorFactorBeta=priorFactorBeta;
		this.sizeFeatures=sizeFeatures;
		this.confidenceInterval=confidenceInterval;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.GammaBeta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.alpha=invertNormalG(confidenceInterval);
		this.initMatrix();
		//System.out.println("gamma "+gamma);
	}
	
	public NaivekPartialLinUCBSample(int sizeFeatures,  double confidenceInterval) {
		this(sizeFeatures, 1.0, confidenceInterval);
	}
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {

		for(Arm a:observedArms)	{
			//a.numberObserved++;
			a.SumContext=a.SumContext.add(a.CurrentContext);
			a.SumProdContext=a.SumProdContext.add(a.CurrentContext.outerProduct(a.CurrentContext));
			for(int i=0;i<sizeFeatures;i++){
				a.MeanContext[i]=a.SumContext.getEntry(i)/(a.numberObserved);
				for(int j=0;j<sizeFeatures;j++){
					if(i==j){
						a.CovMatrix[i][j]=0.01+a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
					else{
						a.CovMatrix[i][j]=a.SumProdContext.getEntry(i, j)/(1.0*a.numberObserved)-a.SumContext.outerProduct(a.SumContext).getEntry(i, j)/((1.0*a.numberObserved)*(1.0*a.numberObserved));
					}
				}
			}

		}

		for(Arm a:lastSelected)	{
			a.numberPlayed++;
			a.sumRewards+=a.lastReward;
			a.sumRewardsStar+=a.CurrentContext.dotProduct(betaStar)+a.thetaStar;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
		}

		for(Arm a:lastSelected)	{
			if(observedArms.contains(a)){
				A0=A0.add(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1)));
				b0=b0.add(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1)));
				a.numberPlayedOnObs++;
				a.sumRewardsOnObs+=a.lastReward;
				a.SumContextOnObs=a.SumContextOnObs.add(a.CurrentContext);
				A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).subtract(a.SumContextOnObs.outerProduct(a.SumContextOnObs).scalarMultiply(1.0/(a.numberPlayedOnObs+1))));
				b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward).subtract(a.SumContextOnObs.mapMultiply(a.sumRewardsOnObs/(a.numberPlayedOnObs+1))));			}
		}


		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);

		/*for(Arm a:arms)	{
			a.theta=(a.sumRewards-a.SumContext.dotProduct(beta))/(a.numberPlayed+1);
		}*/


		nbIt ++;
	}

	@Override
	public void updateScore() {
		for(Arm a:arms){
			if(a.numberObserved<=2){
				a.score=2000000.0;
			}

			else {			
					if(observedArms.contains(a)){
						a.score=
								a.CurrentContext.dotProduct(beta)+
								alpha*Math.sqrt(
										a.CurrentContext.dotProduct((invA0.operate(a.CurrentContext)))
										);		
					}
					else{
						MultivariateNormalDistribution dist = new MultivariateNormalDistribution(a.MeanContext,a.CovMatrix);
						a.score=0.0;
						for(int i=0;i<nbSample;i++){
							RealVector sampleContext = new ArrayRealVector(dist.sample());
							a.score+=(sampleContext.dotProduct(beta)+
									alpha*Math.sqrt(
											sampleContext.dotProduct((invA0.operate(sampleContext)))
											))/nbSample;
									
						}
					}
			}
		}

	}

	public class scoreComparatorPartialkHybridLinUCB implements Comparator<Arm>
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

	public static double invertNormalG (double confInt ){
		NormalDistribution n = new NormalDistribution();	
		return n.inverseCumulativeProbability((confInt+1)/(2.0));
	}






	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorPartialkHybridLinUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "NaivekPartialLinUCBSample_confint_"+this.confidenceInterval;
	}
}


class OptPoisson extends Policy{


	public int sizeFeatures;

	public OptPoisson(int sizeFeatures) {
		super();
		this.sizeFeatures=sizeFeatures;
	}

	public class scoreComparatorOptPoisson implements Comparator<Arm>
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
	
	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorOptPoisson());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
	}

	@Override
	public String toString() {
		return "OptPoisson";
	}

	@Override
	public void updateScore() {
		for(Arm a: arms){
			a.score=Math.exp(a.CurrentContext.dotProduct(betaStar));
		}
		
	}

	@Override
	public void updateArmParameter() {
		for (Arm a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.numberPlayed++;
			a.sumRewardsStar+=Math.exp(a.CurrentContext.dotProduct(betaStar));
		}
		nbIt ++;
		
	}
}


class ThompsonkPoisson extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public boolean optimistic; 
	public double priorFactorBeta;
	public int sizeFeatures;

	public ThompsonkPoisson(int sizeFeatures,double priorFactorBeta,boolean optimistic) {
		super();
		this.priorFactorBeta= priorFactorBeta;
		this.optimistic=optimistic;
		this.sizeFeatures=sizeFeatures;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.initMatrix();
		//System.out.println("gamma "+gamma);
	}

	
	public ThompsonkPoisson(int sizeFeatures) {
		this(sizeFeatures, 1.0,true);
	}
	
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {
		for(Arm a:lastSelected)	{
			A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).scalarMultiply(a.lastReward));;
			if(a.lastReward!=0){
				b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward*Math.log(a.lastReward)));
			}
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.sumRewardsStar+=Math.exp(a.CurrentContext.dotProduct(betaStar));
		}
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		nbIt ++;
	}

	@Override
	public void updateScore() {
		MultivariateNormalDistribution sBeta = new MultivariateNormalDistribution(beta.toArray(),invA0.getData());
		RealVector sampleBeta = new ArrayRealVector(sBeta.sample());
		
		for(Arm a:arms){
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

	public class scoreComparatorThompsonkLin implements Comparator<Arm>
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

	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorThompsonkLin());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "ThompsonkPoisson"+optimistic;
	}
}

class ThompsonkPoissonSimple extends Policy{

	
	public boolean optimistic; 

	public ThompsonkPoissonSimple(boolean optimistic) {
		super();
		this.optimistic=optimistic;
	}

	
	public ThompsonkPoissonSimple() {
		this(true);
		
	}
	



	@Override
	public void updateArmParameter() {
		for(Arm a:lastSelected)	{
			a.sumRewards+=a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.numberPlayed++;
		}

		nbIt ++;
	}

	@Override
	public void updateScore() {

		for(Arm a:arms){
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
	

	public class scoreComparatorThompsonkPoissonSimple implements Comparator<Arm>
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

	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorThompsonkPoissonSimple());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
	}

	@Override
	public String toString() {
		return "ThompsonkPoissonSimple"+optimistic;
	}
}

class ThompsonSimple extends Policy{

	
	public boolean optimistic; 

	public ThompsonSimple(boolean optimistic) {
		super();
		this.optimistic=optimistic;
	}

	
	public ThompsonSimple() {
		this(true);
		
	}
	



	@Override
	public void updateArmParameter() {
		for(Arm a:lastSelected)	{
			a.sumRewards+=a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.numberPlayed++;
		}

		nbIt ++;
	}

	@Override
	public void updateScore() {

		for(Arm a:arms){
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
	

	public class scoreComparatorThompsonkPoissonSimple implements Comparator<Arm>
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

	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorThompsonkPoissonSimple());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
			a.numberPlayed=1;
			a.sumRewards=1.0;
		}
	}

	@Override
	public String toString() {
		return "ThompsonSimple";
	}
}


class kGLMPoissonUCB extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public double alpha=2.0;
	public int sizeFeatures;
	public double priorFactorBeta;
	public OptimGeneralizedBandit opt;
	public ArrayList<RealVector> vecInputs;
	public ArrayList<Double> vecOutputs;
	

	
	public kGLMPoissonUCB(int sizeFeatures,double priorFactorBeta, double alpha) {
		super();
		this.alpha=alpha;
		this.priorFactorBeta=priorFactorBeta;
		this.sizeFeatures=sizeFeatures;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.opt=new OptimGeneralizedBandit(sizeFeatures);
		this.vecInputs = new ArrayList<RealVector>();
		this.vecOutputs = new ArrayList<Double>();
		this.initMatrix();
	}
	
	public kGLMPoissonUCB(int sizeFeatures) {
		this(sizeFeatures,1.0,20.0);
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


	@Override
	public void updateArmParameter() {
		for(Arm a:lastSelected)	{
			A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext));
			b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward));
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.sumRewardsStar+=Math.exp(a.CurrentContext.dotProduct(betaStar));
			vecInputs.add(a.CurrentContext);
			vecOutputs.add(a.lastReward);
		}
		CPUMatrix inputs=new CPUMatrix(vecOutputs.size(),sizeFeatures);
		CPUMatrix labels=new CPUMatrix(vecOutputs.size(),1);
		
		for(int i=0 ; i<vecOutputs.size();i++){
			inputs.setValue(i,0,vecOutputs.get(i));
			for(int j=0 ; j<sizeFeatures;j++){
				inputs.setValue(i, j, vecInputs.get(i).getEntry(j));
			}
		}
		
		
		
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		opt.optimize(inputs,labels,-1,1000,0.0,0.1,0.9999999);
		beta = new ArrayRealVector(new double[sizeFeatures]);
		
		for(int i=0;i< sizeFeatures;i++){
			beta.setEntry(i, opt.getParams().get(i).getVal());
		}
		
		//beta = new ArrayRealVector(opt.getParams());
		//beta=invA0.operate(b0);
		nbIt ++;
	}

	@Override
	public void updateScore() {
		for(Arm a:arms){
			a.score=
					Math.exp(a.CurrentContext.dotProduct(beta))+
					alpha*Math.sqrt(
							a.CurrentContext.dotProduct(invA0.operate(a.CurrentContext))
							);		
		}
	}

	public class scoreComparatorkLinUCB implements Comparator<Arm>
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
	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorkLinUCB());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "kGLMPoissonUCB"+this.alpha;
	}
}



class kPoissonUCB extends Policy{

	public RealMatrix A0;
	public RealMatrix invA0;
	public RealVector b0;
	public RealVector beta;
	public double priorFactorBeta;
	public double alpha=2.0;
	public int sizeFeatures;

	public kPoissonUCB(int sizeFeatures,double priorFactorBeta, double alpha) {
		super();
		this.priorFactorBeta= priorFactorBeta;
		this.alpha=alpha;
		this.sizeFeatures=sizeFeatures;
		this.A0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.invA0= new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures] );
		this.beta=new ArrayRealVector(new double[sizeFeatures]);
		this.b0=new ArrayRealVector(new double[sizeFeatures]);
		this.initMatrix();
		//System.out.println("gamma "+gamma);
	}

	
	public kPoissonUCB(int sizeFeatures,double priorFactorBetaa) {
		this(sizeFeatures,priorFactorBetaa,2.0);
	}
	
	public kPoissonUCB(int sizeFeatures) {
		this(sizeFeatures,1.0);
	}
	
	public void initMatrix(){
		for (int i = 0; i<sizeFeatures;i++){
			b0.setEntry(i,0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){A0.setEntry(i, j, 1.0*priorFactorBeta);invA0.setEntry(i, j, 1);}
				else{A0.setEntry(i, j, 0);invA0.setEntry(i, j, 0);}
			}
		}	
	}


	@Override
	public void updateArmParameter() {
		for(Arm a:lastSelected)	{
			A0=A0.add(a.CurrentContext.outerProduct(a.CurrentContext).scalarMultiply(a.lastReward));;
			if(a.lastReward!=0){
				b0=b0.add(a.CurrentContext.mapMultiply(a.lastReward*Math.log(a.lastReward)));
			}
			a.numberPlayed++;
			a.sumRewards=a.sumRewards+a.lastReward;
			a.sumSqrtRewards+=a.lastReward*a.lastReward;
			a.sumRewardsStar+=Math.exp(a.CurrentContext.dotProduct(betaStar));
		}
		invA0= new LUDecomposition(A0).getSolver().getInverse();
		beta=invA0.operate(b0);
		nbIt ++;
	}

	@Override
	public void updateScore() {
		for(Arm a:arms){
			double mu= a.CurrentContext.dotProduct(beta);
			double sigma2= a.CurrentContext.dotProduct(invA0.operate(a.CurrentContext));
				a.score=Math.exp(mu+sigma2/2)+alpha*Math.sqrt((Math.exp(sigma2)-1)*Math.exp(2*mu*sigma2));
		}
	}

	public class scoreComparatorThompsonkLin implements Comparator<Arm>
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

	@Override
	public void select(int nbToSelect) {
		int nbMax=nbToSelect;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}

		Collections.sort(arms,new scoreComparatorThompsonkLin());
		lastSelected=new HashSet<Arm>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}

	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<Arm>();
		this.observedArms=new HashSet<Arm>();
		for (Arm a:arms){
			a.reinitArm();
		}
		initMatrix();
	}

	@Override
	public String toString() {
		return "kPoissonUCB"+alpha;
	}
}