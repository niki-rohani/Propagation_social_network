package thibault.dynamicCollect;

import java.util.ArrayList;

import org.la4j.vector.Vector;

public abstract class lossFunction {
	
	public functionToLearn f;
	
	public lossFunction(functionToLearn f){
		this.f=f;
	}
	
	
	public void reinit(){
		this.f.reinit();
	}
	
	public abstract double valueF(Vector entries1, Vector entries2);
	public abstract ArrayList<Double> gradientF(Vector entries1, Vector entries2);
	

	
}

class hingeLoss extends lossFunction{

	public hingeLoss(functionToLearn f) {
		super(f);
	}

	public double value(double e1,double e2) {
		return Math.max(0, 1+e2-e1);
	}

	public double derivative(double e1,double e2) {
		if(1+e2-e1<0){
			return 0;
		}
		else{
			return 1;
		}
	}
	
	@Override
	public double valueF(Vector entries1, Vector entries2) {
		return this.value(f.value(entries1),f.value(entries2));
	}

	
	public ArrayList<Double> gradientF(Vector entries1, Vector entries2) {
		double value1 = f.value(entries1);
		double value2 = f.value(entries2);
		Vector gradient1 = f.gradient(entries1);
		Vector gradient2 = f.gradient(entries2);
		ArrayList<Double> gradientF = new ArrayList<Double>();
		for (int i=0;i<f.nbParam;i++){
			gradientF.add(i,derivative(value1,value2)*(gradient2.get(i)-gradient1.get(i)));
		}
		return gradientF;
	}
	
	public String toString(){
		return "hingeLoss_"+f.toString();
	}

	

	
}