package mlp;

import java.util.HashSet;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

import core.Model;

public abstract class MLPModel implements Serializable {
	protected Parameters params;
	protected Parameters paramsCopy;
	protected transient SequentialModule global;
	protected boolean loaded;
	protected String model_file;
	public abstract void load() throws IOException;
	public abstract void save() throws IOException;
	public MLPModel(Parameters params){
		this(params,"");
	}
	public MLPModel(){
		this(new Parameters());
	}
	public MLPModel(String model_file){
		this(new Parameters(),model_file);
	}
	public MLPModel(Parameters params,String model_file){
		this.params=params;
		global=new SequentialModule();
		loaded=false;
		this.model_file=model_file;
	}
	public void copyParams(){
		paramsCopy=new Parameters(params);
	}
	public void loadCopy(){
		params.restoreParams(paramsCopy);
	}
	public abstract void forward();
	public abstract void backward();
	public Parameters getParams(){
		return(params);
	}
	
	/**
	 * Should be overriden for coordinate gradient, when all params are not used each iteration
	 */
	public void updateParams(double line){
		params.update(line);
		global.paramsChanged();
	}
	
	/**
	 * Should be overriden for coordinate  gradient, when all params are not used each iteration and reverts are performed by the optimizer
	 */
	public void revertLastMove(){
		params.revertLastMove();
	}
	
	public double getLossValue(){
		Tensor out=global.getOutput();
		int nbm=out.number_of_matrices;
		if(nbm>1){
			throw new RuntimeException("Model with several output matrix => redefine getAverageValue().");
		}
		Matrix m=out.getMatrix(0);
		if(m.getNumberOfColumns()>1){
			throw new RuntimeException("Model with several output values => redefine getAverageValue().");
		}
		CPUAverageRows av=new CPUAverageRows(1);
		av.forward(out);
		double[] vals=((CPUMatrix)(av.getOutput().getMatrix(0))).getValues();
		/*double sum=0.0f;
		for(int i=0;i<vals.length;i++){
			sum+=vals[i];
		}
		if(vals.length>0){
			sum/=vals.length;
		}*/
		return(vals[0]);
	}
	
	public Tensor getOutput() {
		return(global.getOutput());
	}
	
	/**
	 * Used by DescentDirection strategies, to compute the optimization direction. 
	 * Should be overriden for coordinate gradient, when all params are not used each iteration
	 * 
	 */
	public Parameters getUsedParams(){
		return params;
	}
	
	/*public void initParams()
	{
		params.update(0.0f);
	}*/
	
	
}
