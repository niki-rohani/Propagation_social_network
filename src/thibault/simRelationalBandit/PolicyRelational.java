package thibault.simRelationalBandit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealVector;




public abstract class PolicyRelational {

	public int nbIt=1;
	public ArrayList<ArmRelational> arms;
	public HashSet<ArmRelational> lastSelected;


	public PolicyRelational(){
		this.arms=new ArrayList<ArmRelational>();
		this.lastSelected=new HashSet<ArmRelational>();
	}

	public abstract void updateScore();
	public abstract void updateArmParameters();
	public abstract void select(int nb);
	public abstract void reinitPolicy();
	public abstract String toString();
}


class RandomPolicy extends PolicyRelational{

	public RandomPolicy() {
		super();
	}

	@Override
	public void updateArmParameters() {
		for (ArmRelational a: lastSelected){
			a.sumRewards+=a.lastReward;
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
		lastSelected=new HashSet<ArmRelational>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}



	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<ArmRelational>();
		for (ArmRelational a:this.arms){
			a.reinit();
		}
	}

	@Override
	public String toString() {
		return "RandomPolicy";
	}
}

class UCB extends PolicyRelational{

	public double rho=1.0;

	public UCB() {
		super();
	}

	@Override
	public void updateArmParameters() {
		for (ArmRelational a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.numberPlayed++;
		}
		nbIt ++;
	}

	public void updateScore() {
		for(ArmRelational a:arms){
			if(a.numberPlayed>0){
			a.score=a.sumRewards/a.numberPlayed+rho*Math.sqrt(2*Math.log(nbIt)/a.numberPlayed);
			}
			else{
				a.score=200000000;
			}
		}
	}

	public class scoreComparatorkLinUCB implements Comparator<ArmRelational>
	{	
		public int compare(ArmRelational arm1,ArmRelational arm2){
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
		lastSelected=new HashSet<ArmRelational>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}



	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<ArmRelational>();
		for (ArmRelational a:this.arms){
			a.reinit();
		}
	}

	@Override
	public String toString() {
		return "UCB";
	}
}

class LinUCB extends PolicyRelational{

	public double alpha=2.0;
	public int sizeFeatures;

	public LinUCB() {
		super();
	}

	@Override
	public void updateArmParameters() {
		for (ArmRelational a: lastSelected){
			a.A=a.A.add(a.CurrentContext.outerProduct(a.CurrentContext));
			a.b=a.b.add(a.CurrentContext.mapMultiply(a.lastReward));
			a.sumRewards+=a.lastReward;
			a.numberPlayed++;
			a.AInv=new LUDecomposition(a.A).getSolver().getInverse();
			a.thetaVec=a.AInv.operate(a.b);
		}
		nbIt ++;
	}

	public void updateScore() {
		for(ArmRelational a:arms){
			a.score=a.thetaVec.dotProduct(a.CurrentContext)+alpha*Math.sqrt(a.CurrentContext.dotProduct(a.AInv.operate(a.CurrentContext)));
		}
	}

	public class scoreComparatorkLinUCB implements Comparator<ArmRelational>
	{	
		public int compare(ArmRelational arm1,ArmRelational arm2){
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
		lastSelected=new HashSet<ArmRelational>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}



	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<ArmRelational>();
		for (ArmRelational a:this.arms){
			a.reinit();
		}
	}

	@Override
	public String toString() {
		return "LinUCB";
	}
}


class LinUCBGrad extends PolicyRelational{

	public double alpha=2.0;
	public int sizeFeatures;
	public double gradStep=0.1; 
	int nbStepDescent=1000;
	
	public LinUCBGrad(int sizeFeatures,int nbStepDescent) {
		super();
		this.sizeFeatures=sizeFeatures;
		this.nbStepDescent=nbStepDescent;
	}

	@Override
	public void updateArmParameters() {

		
		for (ArmRelational a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.numberPlayed++;
			a.rewardsList.add(a.lastReward);
			a.contextList.add(a.CurrentContext);
		}

		//gradient descent pour les nouvelles coordonnees
		for(int i=0;i<nbStepDescent;i++){
			for (ArmRelational a: arms){
				computeGradThetaVec(a);
			}
			for (ArmRelational a: arms){
				a.thetaVec=a.thetaVec.subtract(a.gradThetaVec.mapMultiply(gradStep));
			}
			gradStep=1.0/(i+1);
			if(i%100==0){
				System.out.println(nbIt+" "+evaluateF());
			}
			
		}

		nbIt ++;
		
	}
	
	public void computeGradThetaVec(ArmRelational a){
		RealVector grad=new ArrayRealVector(new double[sizeFeatures]);
		for(int i=0;i<a.rewardsList.size();i++){
			//System.out.println(grad);
			grad=grad.add(a.contextList.get(i).mapMultiply(a.thetaVec.dotProduct(a.contextList.get(i))-a.rewardsList.get(i)));
		}
	}
	public double evaluateF(){
		double val=0.0;
		for (ArmRelational a: arms){
			for(int i=0;i<a.rewardsList.size();i++){
				val+=Math.pow(a.thetaVec.dotProduct(a.contextList.get(i))-a.rewardsList.get(i), 2);
			}
		}
		return val;
	}

	public void updateScore() {
		for(ArmRelational a:arms){
			//a.score=a.thetaVec.dotProduct(a.CurrentContext)+alpha*Math.sqrt(a.CurrentContext.dotProduct(a.AInv.operate(a.CurrentContext)));
			a.score=Math.random();
		}
	}

	public class scoreComparatorkLinUCB implements Comparator<ArmRelational>
	{	
		public int compare(ArmRelational arm1,ArmRelational arm2){
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
		lastSelected=new HashSet<ArmRelational>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}



	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<ArmRelational>();
		for (ArmRelational a:this.arms){
			a.reinit();
		}
	}

	@Override
	public String toString() {
		return "LinUCBGrad";
	}
}


class LinUCBStochGrad extends PolicyRelational{

	public double alpha=2.0;
	public int sizeFeatures;
	public double gradStep=0.1; 
	int nbStepDescent=1000;
	
	public LinUCBStochGrad(int sizeFeatures,int nbStepDescent) {
		super();
		this.sizeFeatures=sizeFeatures;
		this.nbStepDescent=nbStepDescent;
	}

	@Override
	public void updateArmParameters() {

		
		for (ArmRelational a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.numberPlayed++;
			a.rewardsList.add(a.lastReward);
			a.contextList.add(a.CurrentContext);
		}

		//gradient descent pour les nouvelles coordonnees
		for(int i=0;i<nbStepDescent;i++){
			for (ArmRelational a: arms){
				computeGradThetaVec(a);
			}
			for (ArmRelational a: arms){
				a.thetaVec=a.thetaVec.subtract(a.gradThetaVec.mapMultiply(gradStep));
			}
			gradStep=1.0/(i+1);
			if(i%100==0){
				System.out.println(nbIt+" "+evaluateF());
			}
			
		}

		nbIt ++;
		
	}
	
	public void computeGradThetaVec(ArmRelational a){
		RealVector grad=new ArrayRealVector(new double[sizeFeatures]);
		for(int i=0;i<a.rewardsList.size();i++){
			//System.out.println(grad);
			grad=grad.add(a.contextList.get(i).mapMultiply(a.thetaVec.dotProduct(a.contextList.get(i))-a.rewardsList.get(i)));
		}
	}
	public double evaluateF(){
		double val=0.0;
		for (ArmRelational a: arms){
			for(int i=0;i<a.rewardsList.size();i++){
				val+=Math.pow(a.thetaVec.dotProduct(a.contextList.get(i))-a.rewardsList.get(i), 2);
			}
		}
		return val;
	}

	public void updateScore() {
		for(ArmRelational a:arms){
			//a.score=a.thetaVec.dotProduct(a.CurrentContext)+alpha*Math.sqrt(a.CurrentContext.dotProduct(a.AInv.operate(a.CurrentContext)));
			a.score=Math.random();
		}
	}

	public class scoreComparatorkLinUCB implements Comparator<ArmRelational>
	{	
		public int compare(ArmRelational arm1,ArmRelational arm2){
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
		lastSelected=new HashSet<ArmRelational>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}



	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<ArmRelational>();
		for (ArmRelational a:this.arms){
			a.reinit();
		}
	}

	@Override
	public String toString() {
		return "LinUCBStochGrad";
	}
}

class EpsilonGreedyEmbeddings extends PolicyRelational{

	public double alpha=2.0;
	public int sizeFeatures;
	public int sizeSpace;
	public double gradStep=0.001; //pas de gradient
	int nbStepDescent=1000;

	public EpsilonGreedyEmbeddings(int sizeSpace,int nbStepDescent) {
		super();
		this.sizeSpace=sizeSpace;
		this.nbStepDescent=nbStepDescent;
	}

	@Override
	public void updateArmParameters() {

		
		for (ArmRelational a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.numberPlayed++;
			a.rewardsList.add(a.lastReward);
			a.contextList.add(a.CurrentContext);
		}

		//gradient descent pour les nouvelles coordonnees
		for(int i=0;i<nbStepDescent;i++){
			for (ArmRelational a: arms){
				computeThetaVec(a);
			}
			for (ArmRelational a: arms){
				computeGradCoords(a);
				computeGradTheta(a);
				//System.out.println(a.coords);
				a.coords=a.coords.subtract(a.gradCoords.mapMultiply(this.gradStep));
				//System.out.println(a.coords);
				//System.out.println(a.Id+" "+a.theta);
				a.theta=a.theta-this.gradStep*a.gradTheta;

			}

			for (ArmRelational a: arms){
				computeThetaVec(a);
			}
			//System.out.println(nbIt+" "+i+" "+evaluateF());
			gradStep=1.0/(i+1);
		}
		//System.out.println(nbIt+" "+evaluateF());
		//calcul du vecteur de features grace aux distances et au biais
		
		/*for (ArmRelational a: arms){
			System.out.println(a.Id+" "+a.theta);
			System.out.println(a.Id+" "+a.thetaVec);
		}*/
		nbIt ++;
		
	}

	public void updateScore() {
		for(ArmRelational a:arms){
			//a.score=a.thetaVec.dotProduct(a.CurrentContext)+alpha*Math.sqrt(a.CurrentContext.dotProduct(a.AInv.operate(a.CurrentContext)));
			a.score=Math.random();
		}
	}


	public void computeThetaVec(ArmRelational a){
			for (ArmRelational b: arms){
				if(a!=b){
					a.thetaVec.setEntry(b.Id, 1/Math.pow(a.coords.subtract(b.coords).getNorm(),2));
				}
				else{
					a.thetaVec.setEntry(a.Id, a.theta); //a modifier
				}
			}
	}

	public void computeGradCoords(ArmRelational a){
		RealVector grad=new ArrayRealVector(new double[sizeSpace]);
		for (ArmRelational b: arms){
			if(b!=a){
				RealVector Zij=b.coords.subtract(a.coords).mapDivide(Math.pow(b.coords.subtract(a.coords).getNorm(),4));
				double s1=0.0;
				for(int i=0;i<b.rewardsList.size();i++){
					s1+=(b.thetaVec.dotProduct(b.contextList.get(i))-b.rewardsList.get(i))*b.contextList.get(i).getEntry(a.Id);
				}
				double s2=0.0;
				for(int i=0;i<a.rewardsList.size();i++){
					s2+=(a.thetaVec.dotProduct(a.contextList.get(i))-a.rewardsList.get(i))*a.contextList.get(i).getEntry(b.Id);
				}
				grad=grad.add(Zij.mapMultiply(s1+s2));
			}
			a.gradCoords=grad;
		}
	}

	public void computeGradTheta(ArmRelational a){
		double grad=0.0;
		for(int i=0;i<a.rewardsList.size();i++){
			grad=grad+a.thetaVec.dotProduct(a.contextList.get(i))-a.rewardsList.get(i);
		}
		a.gradTheta=2*grad;
	}

	public double evaluateF(){
		double val=0.0;
		for (ArmRelational a: arms){
			for(int i=0;i<a.rewardsList.size();i++){
				val+=Math.pow(a.thetaVec.dotProduct(a.contextList.get(i))-a.rewardsList.get(i), 2);
			}
		}
		return val;
	}

	public class scoreComparatorkLinUCB implements Comparator<ArmRelational>
	{	
		public int compare(ArmRelational arm1,ArmRelational arm2){
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
		lastSelected=new HashSet<ArmRelational>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}



	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<ArmRelational>();
		for (ArmRelational a:this.arms){
			a.reinit();
		}
	}

	@Override
	public String toString() {
		return "EpsilonGreedyEmbeddings";
	}
}


class EpsilonGreedyEmbeddings2 extends PolicyRelational{

	public double alpha=2.0;
	public int sizeFeatures;
	public int sizeSpace;
	public double gradStep=0.001; //pas de gradient
	int nbStepDescent=10000;

	public EpsilonGreedyEmbeddings2(int sizeSpace,int nbStepDescent) {
		super();
		this.sizeSpace=sizeSpace;
		this.nbStepDescent=nbStepDescent;
	}

	@Override
	public void updateArmParameters() {

		
		for (ArmRelational a: lastSelected){
			a.sumRewards+=a.lastReward;
			a.numberPlayed++;
			a.rewardsList.add(a.lastReward);
			a.contextList.add(a.CurrentContext);
		}

		//gradient descent pour les nouvelles coordonnees
		for(int i=0;i<nbStepDescent;i++){
			for (ArmRelational a: arms){
				computeThetaVec(a);
			}
			for (ArmRelational a: arms){
				computeGradCoords(a);
				computeGradTheta(a);
				//System.out.println(a.coords);
				a.coords=a.coords.subtract(a.gradCoords.mapMultiply(this.gradStep));
				//System.out.println(a.coords);
				//System.out.println(a.Id+" "+a.theta);
				a.theta=a.theta-this.gradStep*a.gradTheta;

			}

			for (ArmRelational a: arms){
				computeThetaVec(a);
			}
			//System.out.println(nbIt+" "+i+" "+evaluateF());
			gradStep=1.0/(i+1);
		}
		//System.out.println(nbIt+" "+evaluateF());
		//calcul du vecteur de features grace aux distances et au biais
		
		/*for (ArmRelational a: arms){
			System.out.println(a.Id+" "+a.theta);
			System.out.println(a.Id+" "+a.thetaVec);
		}*/
		nbIt ++;
		
	}

	public void updateScore() {
		for(ArmRelational a:arms){
			//a.score=a.thetaVec.dotProduct(a.CurrentContext)+alpha*Math.sqrt(a.CurrentContext.dotProduct(a.AInv.operate(a.CurrentContext)));
			a.score=Math.random();
		}
	}


	public void computeThetaVec(ArmRelational a){
			for (ArmRelational b: arms){
				if(a.Id!=b.Id){
					a.thetaVec.setEntry(b.Id, Math.exp(-Math.pow(a.coords.subtract(b.coords).getNorm(),2)));
				}
				else{
					a.thetaVec.setEntry(a.Id, a.theta); //a modifier
				}
			}
			
	}

	public void computeGradCoords(ArmRelational a){
		RealVector grad=new ArrayRealVector(new double[sizeSpace]);
		for (ArmRelational b: arms){
			if(b.Id!=a.Id){
				RealVector Zij=b.coords.subtract(a.coords).mapMultiply(Math.exp(-Math.pow(b.coords.subtract(a.coords).getNorm(),2)));
				double s1=0.0;
				for(int i=0;i<b.rewardsList.size();i++){
					s1+=(b.thetaVec.dotProduct(b.contextList.get(i))-b.rewardsList.get(i))*b.contextList.get(i).getEntry(a.Id);
				}
				double s2=0.0;
				for(int i=0;i<a.rewardsList.size();i++){
					s2+=(a.thetaVec.dotProduct(a.contextList.get(i))-a.rewardsList.get(i))*a.contextList.get(i).getEntry(b.Id);
				}
				grad=grad.add(Zij.mapMultiply(s1+s2));
			}
			a.gradCoords=grad.mapMultiply(4);
		}
	}

	public void computeGradTheta(ArmRelational a){
		double grad=0.0;
		for(int i=0;i<a.rewardsList.size();i++){
			grad=grad+a.thetaVec.dotProduct(a.contextList.get(i))-a.rewardsList.get(i);
		}
		a.gradTheta=2*grad;
	}

	public double evaluateF(){
		double val=0.0;
		for (ArmRelational a: arms){
			for(int i=0;i<a.rewardsList.size();i++){
				val+=Math.pow(a.thetaVec.dotProduct(a.contextList.get(i))-a.rewardsList.get(i), 2);
				//System.out.println(val);
			}
		}
		return val;
	}

	public class scoreComparatorkLinUCB implements Comparator<ArmRelational>
	{	
		public int compare(ArmRelational arm1,ArmRelational arm2){
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
		lastSelected=new HashSet<ArmRelational>();
		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
	}



	@Override
	public void reinitPolicy() {
		this.nbIt=1;
		this.lastSelected=new HashSet<ArmRelational>();
		for (ArmRelational a:this.arms){
			a.reinit();
		}
	}

	@Override
	public String toString() {
		return "EpsilonGreedyEmbeddings2";
	}
}