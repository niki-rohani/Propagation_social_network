package thibault;

import java.io.IOException;

import mlp.CPUTimesVals;

import java.util.ArrayList;

import mlp.CPUAverageRows;
import mlp.DescentDirection;
import mlp.Env;
import mlp.LineSearch;
import mlp.MLPModel;
import mlp.Optimizer;
import mlp.CPUMatrix;
import mlp.Parameter;
import mlp.Parameters;
import mlp.CPULinear;
import mlp.Module;
import mlp.CPUExp;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.CPUSum;
import mlp.Tensor;
public class OptimGeneralizedBandit extends MLPModel {
	CPULinear mod;
	Module mod2;
	int nbPars=0;
	CPUMatrix inputs;
	CPUMatrix labels;
	int nbSamples=-1;
	CPUTimesVals timesLabels;
	Tensor currentInput;
	double lastLoss;
	int nbForwards=0;
	int nbSum=0;
	double sumLoss=0.0;
	double sumLossTot=0.0;
	int nbAffiche=10;
	int nbEstimations=100;
	
	public OptimGeneralizedBandit(int nbPars){
		
		this.nbPars=nbPars; 
		mod=new CPULinear(nbPars,1);
		params.allocateNewParamsFor(mod);
		mod2=mod.forwardSharedModule();
		timesLabels=new CPUTimesVals(1,1.0);
		CPUExp exp=new CPUExp(1);
		ArrayList<Double> weights=new ArrayList<Double>();
		weights.add(1.0);weights.add(-1.0);
		CPUSum sum=new CPUSum(1,2,weights);
		TableModule tab=new TableModule();
		SequentialModule seq1=new SequentialModule();
		seq1.addModule(mod);
		seq1.addModule(timesLabels);
		SequentialModule seq2=new SequentialModule();
		seq2.addModule(mod2);
		seq2.addModule(exp);
		tab.addModule(seq1);
		tab.addModule(seq2);
		global.addModule(tab);
		global.addModule(sum);
		global.addModule(new CPUAverageRows(1,0));
		global.addModule(new CPUTimesVals(1,-1.0));
		this.currentInput=new Tensor(2);
	}
	
	
	
	@Override
	public void load() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void save() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void forward() {
		CPUMatrix in;
		CPUMatrix labs;
		
		if(nbSamples<0){
			in=inputs;
			labs=labels;
		}
		else{
			int nbr=inputs.getNumberOfRows();
			in=new CPUMatrix(nbSamples,nbPars);
			labs=new CPUMatrix(nbSamples,1);
			for(int i=0;i<nbSamples;i++){
				int x=(int)(Math.random()*nbr);
				for(int j=0;j<nbPars;j++){
					in.setValue(i,j,inputs.getValue(x,j));
				}
				labs.setValue(i,0,labels.getValue(x,0));
			}
		}	
		this.currentInput.setMatrix(0,in);
		this.currentInput.setMatrix(1,in);
		timesLabels.setVals(labs);
		
		global.forward(currentInput);
		nbForwards++;
		//nbFromChangeLock++;
		lastLoss=getLossValue();
		/*if(lastLoss>1000000){
			throw new RuntimeException("stop");
		}*/
		sumLoss+=lastLoss;
		sumLossTot+=lastLoss;
		nbSum++;
		if((nbForwards%nbAffiche==0) && (nbForwards!=0)){
			//System.out.println(" Average Loss = "+String.format("%.5f", sumLossTot/(1.0*nbForwards))+", "+String.format("%.5f", sumLoss/(1.0*nbSum))+", "+String.format("%.5f", lastLoss)+" nbSums="+nbSum+" nbForwards="+nbForwards);
		}
		if(nbForwards%nbEstimations==0){
			sumLoss=0;
			nbSum=0;
			//System.gc();
			//System.out.println("reinit sum");
		}
		
	}

	@Override
	public void backward() {
		global.backward(currentInput);

	}
	
	public double[] getPars(){
		double[] ret=new double[params.length()];
		ArrayList<Parameter> pars=params.getParams(); 
		for(int i=0;i<pars.size();i++){
			ret[i]=pars.get(i).getVal();
		}
		return ret;
	}
	
	
	public void setPars(double[] pars){
		params=new Parameters();
		params.allocateNewParamsFor(mod, pars);
		
	}
	
	
	public void optimize(CPUMatrix inputs,CPUMatrix labels, int nbSamples, int nbMaxIt, double eps, double line, double decFactor){
		Env.setVerbose(0);
		if(inputs.getNumberOfColumns()!=nbPars){
			throw new RuntimeException("input de taille incompatible avec les parametres ("+inputs.getNumberOfColumns()+","+nbPars+")");
		}
		if(labels.getNumberOfColumns()!=1){
			throw new RuntimeException("labels doit comporter exactement 1 colonne");
		}
		this.nbSamples=nbSamples;
		this.inputs=inputs;
		this.labels=labels;
		Optimizer opt=Optimizer.getDescent(DescentDirection.getGradientDirection(), LineSearch.getFactorLine(line,decFactor));
		
		opt.optimize(this,eps,nbMaxIt,false);
		
	}
	
	
	public static void main(String[] args){
		CPUMatrix inputs=new CPUMatrix(30,3);
		CPUMatrix labels=new CPUMatrix(30,1);
		for(int j=0;j<10;j++){
			inputs.setValue(j,0,Math.random()+0.1);
			inputs.setValue(j,1,Math.random()-0.5);
			inputs.setValue(j,2,Math.random()+1.0);
			labels.setValue(j,0,1.0);
		}
		for(int j=0;j<10;j++){
			inputs.setValue(j+10,0,Math.random()-0.3);
			inputs.setValue(j+10,1,Math.random()+0.5);
			inputs.setValue(j+10,2,Math.random()-0.3);
			
			labels.setValue(j+10,0,2.0);
		}
		for(int j=0;j<10;j++){
			inputs.setValue(j+20,0,Math.random()-0.3);
			inputs.setValue(j+20,1,Math.random()+0.5);
			inputs.setValue(j+20,2,Math.random()-0.3);
			labels.setValue(j+20,0,3.0);
		}
		OptimGeneralizedBandit opt=new OptimGeneralizedBandit(3);
		opt.optimize(inputs,labels,-1,1000,0.0,0.1,0.9999999);
		//opt.getPars();
		System.out.println(opt.getParams());
		System.out.println(opt.getParams().get(0).getVal());
		System.out.println(opt.getParams().get(1).getVal());
	}

}
