package optimization;

import java.io.IOException;
import java.io.Serializable;

import statistics.Distributions;

import core.Model;
import core.Utils;

import java.util.HashMap;
import java.util.ArrayList;
public class ParametrizedModel implements Model,Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Fonction fonction; // inference function
	private Parameters params;
	private String modelFile;
	
	public ParametrizedModel(Fonction fonction){
		this(fonction,"");
	}
	
	public ParametrizedModel(Fonction fonction, String modelFile){
		this(fonction,new Parameters(), modelFile);
	}
	public ParametrizedModel(Fonction fonction,Parameters params, String modelFile){
		this.fonction=fonction;
		this.params=params;
		fonction.setParams(params);
		this.modelFile=modelFile;
	}
	
	public void setParams(HashMap<Integer,Double> params){
		this.params.setParams(params);
	}
	
	public Parameters getParams(){
		return(params);
	}
	public void learn(ArrayList<HashMap<Integer,Double>> samples,ArrayList<Double> labels,Fonction loss,Optimizer optim){
		learn(samples,labels,loss,optim,0.00000001);
	}
	public void learn(ArrayList<HashMap<Integer,Double>> samples,ArrayList<Double> labels,Fonction loss,Optimizer optim, double epsilon){
		learn(samples,labels,loss,optim,epsilon,1000);
	}
	public void learn(ArrayList<HashMap<Integer,Double>> samples,ArrayList<Double> labels,Fonction loss,Optimizer optim, double epsilon,int maxit){
		learn(samples,labels,loss,optim,epsilon,maxit,200);
	}
	public void learn(ArrayList<HashMap<Integer,Double>> samples,ArrayList<Double> labels,Fonction loss,Optimizer optim, double epsilon,int maxit,int minit){
		learn(samples,labels,loss,optim,epsilon,maxit,minit,-1);
	}
	
	public void learn(ArrayList<HashMap<Integer,Double>> samples,ArrayList<Double> labels,Fonction loss,Optimizer optim, double epsilon, int maxit, int minit, double maxloss){
		Fonction f=loss.copy();
		fonction.setSamples(samples);
		f.setSubFunction(fonction);
		f.setLabels(labels);
		f.setParams(params);
		optim.optimize(f,epsilon,maxit,minit,maxloss);
		
		fonction.clearListeners();
		/*try{
			save();
		}
		catch(IOException e){
			System.out.println(e);
		}*/
		
	}
	public double getLoss(ArrayList<HashMap<Integer,Double>> samples,ArrayList<Double> labels,Fonction loss){
		Fonction f=loss.copy();
		fonction.setSamples(samples);
		f.setSubFunction(fonction);
		f.setLabels(labels);
		f.setParams(params);
		double ret=f.getValue();
		//System.out.println(f);
		//System.out.println(f.getDerivativeFonction());
		fonction.clearListeners();
		return(ret);
	}
	public ArrayList<Double> infer(ArrayList<HashMap<Integer,Double>> samples){
		ArrayList<Double> ret=new ArrayList<Double>();
		fonction.setSamples(samples);
		ret=fonction.getValues(0);
		return(ret);
	}
	
	public void load() throws IOException{
		String filename=modelFile;
		ParametrizedModel mod=(ParametrizedModel)Utils.deserialize(filename);
		params=mod.params;
		fonction=mod.fonction;
		fonction.setParams(params);
	}
	public void save() throws IOException{
		String filename=modelFile;
		Utils.serializeThis(this, filename);
	}
	
	public static void test1(){
		int nbdims=2;
		int nbs=50;
		
		Distributions d=new Distributions();
		ArrayList<HashMap<Integer,Double>> samples=new ArrayList<HashMap<Integer,Double>>();
		ArrayList<Double> labels=new ArrayList<Double>();
		
		for(int j=0;j<nbs;j++){
			HashMap<Integer,Double> s1=new HashMap<Integer,Double>();
			HashMap<Integer,Double> s2=new HashMap<Integer,Double>();
			for(int i=1;i<=nbdims;i++){
				s1.put(i, d.nextGaussian(1, 0.5));
				s2.put(i, d.nextGaussian(-1, 0.5));
			}
			samples.add(s1);
			labels.add(1.0);
			samples.add(s2);
			labels.add(-1.0);
		}
		Fonction f=(new InferFonctionFactory(3)).buildFonction();
		ParametrizedModel m=new ParametrizedModel(f,"model_test1.txt");
		Optimizer opt=(new OptimizerFactory(1)).buildOptimizer();
		Fonction loss=(new LossFonctionFactory(1)).buildFonction();
		m.learn(samples,labels,loss,opt,0.001);
		System.out.println(m.getLoss(samples, labels, loss));
		ArrayList<Double> ret=m.infer(samples);
		System.out.println(ret);
		
		
		samples=new ArrayList<HashMap<Integer,Double>>();
		labels=new ArrayList<Double>();
		
		for(int j=0;j<nbs;j++){
			HashMap<Integer,Double> s1=new HashMap<Integer,Double>();
			HashMap<Integer,Double> s2=new HashMap<Integer,Double>();
			for(int i=1;i<=nbdims;i++){
				s1.put(i, d.nextGaussian(1, 0.5));
				s2.put(i, d.nextGaussian(-1, 0.5));
			}
			samples.add(s1);
			labels.add(1.0);
			samples.add(s2);
			labels.add(-1.0);
		}
		System.out.println(m.getLoss(samples, labels, loss));
		ret=m.infer(samples);
		System.out.println(ret);
		
		try{
			m.save();
		
			ParametrizedModel m2=new ParametrizedModel(f,"modelP.txt");
			m2.load();
			
			System.out.println(m2.getLoss(samples, labels, loss));
			ret=m2.infer(samples);
			System.out.println(ret);
		}
		catch(Exception e){
			System.out.println(e);
		}
	}
	
	public static void test2(){
		ArrayList<HashMap<Integer,Double>> samples=new ArrayList<HashMap<Integer,Double>>();
		ArrayList<Double> labels=new ArrayList<Double>();
		double sum=0;
		for(int i=20;i<30;i++){
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			h.put(1, i*1.0);
			samples.add(h);
			labels.add(sum);
			if((i==23) || (i==24)){
				sum+=0.5;
			}
		}
		Fonction f=(new InferFonctionFactory(3)).buildFonction();
		ParametrizedModel m=new ParametrizedModel(f,"model_test2.txt");
		Optimizer opt=(new OptimizerFactory(2)).buildOptimizer();
		Fonction loss=(new LossFonctionFactory(1)).buildFonction();
		m.learn(samples,labels,loss,opt,0.000000001);
		System.out.println(m.getLoss(samples, labels, loss));
		ArrayList<Double> ret=m.infer(samples);
		System.out.println(ret);
		System.out.println(labels);
		/*Fonction f2=(new InferFonctionFactory(4)).buildFonction();
		Minus min=new Minus();
		min.setSubFunction(f2);
		f2=min;
		samples=new ArrayList<HashMap<Integer,Double>>();
		HashMap<Integer,Double> sample=new HashMap<Integer,Double>();
		sample.put(1, 4.216989375957893);
		samples.add(sample);
		Parameters par=new Parameters();
		HashMap<Integer,Double> pa=new HashMap<Integer,Double>();
		pa.put(1,23.0);
		par.setParams(pa);
		f2.setParams(par);
		f2.setSamples(samples);
		System.out.println(f2);
		ArrayList<HashMap<Integer,Double>> v=f2.getValues();
		System.out.println(v);
		System.out.println(f2.getDerivativeFonction());
		ArrayList<HashMap<Integer,Double>> d=f2.getDerivativeFonction().getValues();
		System.out.println(d);
		System.out.println(f2.getDerivativeFonction().getDerivativeFonction());
		ArrayList<HashMap<Integer,Double>> d2=f2.getDerivativeFonction().getDerivativeFonction().getValues();
		System.out.println(d2);
		opt.optimize(f2.getDerivativeFonction().getDerivativeFonction(), 0.000001, 1000);*/
		opt=(new OptimizerFactory(1)).buildOptimizer();
		
		Fonction f2=f.getReverseParamsSamplesFonction();
		Fonction f3=f2.getDerivativeFonction().getDerivativeFonction();
		Fonction min=new Minus();
		//Power pow=new Power(2);
		//pow.setSubFunction(f3);
		min.setSubFunction(f3);
		Parameters par=new Parameters();
		HashMap<Integer,Double> pa=new HashMap<Integer,Double>();
		pa.put(1,0.0);
		par.setParams(pa);
		
		min.setParams(par);
		System.out.println(min);
		
		
		opt.optimize(min, 0.000001, 1000,500);
		
		/*pa.put(1,0.99999);
		par.setParams(pa);
		min.setParams(par);
		System.out.println(min.getValue());
		System.out.println(min.getGradient());
		pa.put(1,1.00000);
		par.setParams(pa);
		min.setParams(par);
		System.out.println(min.getValue());
		System.out.println(min.getGradient());
		pa.put(1,1.00001);
		par.setParams(pa);
		min.setParams(par);
		System.out.println(min.getValue());
		System.out.println(min.getGradient());
		System.out.println(min);
		
		min=f2.getDerivativeFonction();
		min.setParams(par);
		pa.put(1,-0.00001);
		par.setParams(pa);
		min.setParams(par);
		System.out.println(min.getValue());
		pa.put(1,0.00000);
		par.setParams(pa);
		min.setParams(par);
		System.out.println(min.getValue());
		System.out.println(min.getGradient());
		pa.put(1,0.00001);
		par.setParams(pa);
		min.setParams(par);
		System.out.println(min.getValue());
		System.out.println(min);*/
		
		/*HashMap<Integer,Double> pars=new HashMap<Integer,Double>();
		pars.put(0, -25.0);
		pars.put(1, 1.0);
		m.params.setParams(pars);
		ret=m.infer(samples);
		System.out.println(ret);
		System.out.println(m.getLoss(samples, labels, loss));*/
	}
	
	public static void test3(){
		ArrayList<HashMap<Integer,Double>> samples=new ArrayList<HashMap<Integer,Double>>();
		ArrayList<Double> labels=new ArrayList<Double>();
		HashMap<Integer,Double> h=new HashMap<Integer,Double>();
		h.put(1, 1.0);
		samples.add(h);
		Fonction f=(new InferFonctionFactory(2)).buildFonction();
		Parameters par=new Parameters();
		HashMap<Integer,Double> pa=new HashMap<Integer,Double>();
		pa.put(1,0.0);
		par.setParams(pa);
		f.setParams(par);
		f.setSamples(samples);
		
		pa.put(1,-100.0);
		par.setParams(pa);
		System.out.println(f.getValue());
		System.out.println(f.getGradient());
		System.out.println(f.getDerivativeFonction().getGradient());
		System.out.println(f.getDerivativeFonction().getDerivativeFonction().getGradient());
		pa.put(1,-100.000001);
		par.setParams(pa);
		System.out.println(f.getValue());
		System.out.println(f.getGradient());
		System.out.println(f.getDerivativeFonction().getGradient());
		System.out.println(f.getDerivativeFonction().getDerivativeFonction().getGradient());
		System.out.println(f.getDerivativeFonction().getDerivativeFonction().getDerivativeFonction());
	}
	
	public static void test4(){
		ArrayList<HashMap<Integer,Double>> samples=new ArrayList<HashMap<Integer,Double>>();
		Parameters par=new Parameters();
		HashMap<Integer,Double> pa=new HashMap<Integer,Double>();
		pa.put(1,0.0);
		pa.put(2,1.0);
		par.setParams(pa);
		
		Exp exp=new Exp();
		//Power pow=new Power(3);
		//exp.setSubFunction(pow);
		DotFonction dot=new DotFonction(true);
		
		exp.setSubFunction(dot);
		exp.setParams(par);
		exp.setSamples(samples);
		System.out.println(exp);
		System.out.println(exp.getDerivativeFonction());
		System.out.println(exp.getDerivativeFonction().getDerivativeFonction());
		
		
		
	}
	
	public static void main(String[] args){
		test4();
	}
}
