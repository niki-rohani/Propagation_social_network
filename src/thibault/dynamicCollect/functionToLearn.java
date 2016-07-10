package thibault.dynamicCollect;

import java.util.ArrayList;
import java.util.Random;

import org.la4j.vector.Vector;
import org.la4j.vector.dense.BasicVector;

public abstract class functionToLearn {

	public int nbParam;
	public Vector weights;
	
	public functionToLearn(int nbParam){
		this.nbParam=nbParam;
		this.initWeightstoZeros();
	}
	
	
	public abstract double value(Vector entries);
	public abstract Vector gradient(Vector entries);
	
	public void reinit(){
		initWeightstoZeros();
		//initWeightsRandomly();
	}
	
	public void initWeightsRandomly(){
		for (int i=0;i<nbParam;i++){
			Random rand = new Random();
			this.weights.set(i,rand.nextDouble());
		}
	}
	
	public void initWeightstoZeros(){
		this.weights=new BasicVector(new double[nbParam]);
		for (int i=0;i<nbParam;i++){
			this.weights.set(i,0.0);
		}
	}


	protected int getNbParam() {
		return nbParam;
	}

	protected void setNbParam(int nbParam) {
		this.nbParam = nbParam;
	}

	protected Vector getWeights() {
		return weights;
	}

	protected void setWeights(Vector weights) {
		this.weights = weights;
	}

	
}

class scalarProduct extends functionToLearn {

	public scalarProduct(int nbParam){
		super(nbParam);
	}

	@Override
	public double value(Vector entries) {
		return entries.innerProduct(weights);
	}

	@Override
	public Vector gradient(Vector entries) {
		return entries;
	}
	
	public String toString(){
		return "scalarProduct";
	}


}