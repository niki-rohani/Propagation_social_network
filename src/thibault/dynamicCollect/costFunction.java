package thibault.dynamicCollect;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class costFunction {
	
	public lossFunction l;
	public double learningRate;
	public int timeStep=0;
	public double valueTot=0;
	public costFunction(lossFunction l){
		this.l=l;
		this.learningRate=1;
	}
	
	public costFunction(lossFunction l,double learningRate){
		this.l=l;
		this.learningRate=learningRate;
	}
	
	

	public abstract double value(HashSet<Arm> arms);
	public abstract ArrayList<Double> gradient(HashSet<Arm> arms);
	
	public void updateWeights(HashSet<Arm> arms){
		ArrayList<Double> gradient = this.gradient(arms);
		//learningRate=1/(1+timeStep);
		for (int i=0;i<this.l.f.nbParam;i++){
			this.l.f.weights.set(i,this.l.f.weights.get(i)-this.learningRate*gradient.get(i));
		}
	}
	
	public void reinit(){
		this.l.reinit();
		this.learningRate=1;
		this.timeStep=0;
		this.valueTot=0;
	}
	
}


class basicCost extends costFunction {

	public basicCost(lossFunction l) {
		super(l);
	}
	
	public basicCost(lossFunction l,double learningRate) {
		super(l,learningRate);
	}

	@Override
	public double value(HashSet<Arm> arms) {
			double value=0;
			for (Arm a1:arms){
				for (Arm a2:arms){
					if(a1.lastReward>a2.lastReward){
						value=value+this.l.valueF(a1.features,a2.features);
					}
				}
			}
			return value;
		}
	

	@Override
	public ArrayList<Double> gradient(HashSet<Arm> arms) {
		ArrayList<Double> gradient=new ArrayList<Double>();
		for(int i=0;i<this.l.f.nbParam;i++){
			gradient.add(i, 0.0);
		}
		
		for (Arm a1:arms){
			for (Arm a2:arms){
				if(a1.lastReward>a2.lastReward){
					ArrayList<Double> gradTemp = this.l.gradientF(a1.features,a2.features);
					for(int i=0;i<gradient.size();i++){
						gradient.set(i, gradient.get(i)+gradTemp.get(i));
					}
				}
			}
		}

		return gradient;
	}

	public String toString(){
		return "SimpleCost_"+l.toString();
	}
	
	
	
}
