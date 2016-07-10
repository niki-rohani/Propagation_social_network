package simon.propagationModels;

import mlp.Optimizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import mlp.CPUMatrix ;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import mlp.Env ;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import cascades.Cascade;
import mlp.CPUAddVals;
import mlp.CPUAverageRows;
import mlp.CPUHingeLoss;
import mlp.CPUMatrixProduct;
import mlp.CPUParams;
import mlp.DescentDirection;
import mlp.LineSearch;
import mlp.Optimizer;
import mlp.Parameter;
import mlp.Parameters;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.Tensor;
import core.Structure;
import experiments.EvalMeasure;
import experiments.EvalMeasureList;
import experiments.EvalPropagationModel;
import experiments.EvalPropagationModelConfig;
import experiments.Hyp;
import experiments.MAP;
import experiments.Result;
import propagationModels.MLP ;
import propagationModels.MLPdiffusion;
import propagationModels.PropagationModel;
import propagationModels.PropagationStruct;
import simon.mlp.CPUHeatDist;
import simon.mlp.CPUL2Dist2;
import simon.mlp.MLPCommunities;

public class MLPDiffChainLTstyle extends MLPDiffChain {

	private double init_t_inf = 0;
	private  double final_t_inf = 0;
	private double step_t_inf = 0;
	private double threshold_infer;

	public MLPDiffChainLTstyle(String model_file, int nbDims, double threshold, boolean dualPoint, boolean useDiag, double parinf2, double parsup2, int forwardmode, int batchsize) {
		super(model_file, nbDims, threshold, dualPoint, useDiag, parinf2, parsup2, forwardmode,batchsize);
		// TODO Auto-generated constructor stub
		this.threshold_infer=this.threshold ;
	}
	
	public void learn(String db, String cascadesCollection, long step, int nbInitSteps,Optimizer optim, int minibatchsize, int maxiters) {
		super.learn(db, cascadesCollection, step, nbInitSteps, optim, minibatchsize, maxiters);
		PropagationStruct ps;
		this.init_t_inf=0 ;
		this.final_t_inf= 0;
		for(int cascade_id : this.cascades_ids) {
			Object cascades_id;
			PropagationStruct ps1 = this.train_cascades.get(cascade_id) ;
			HashMap<String, Long> inf = ps1.getInfectionTimes() ;
			for(String u : inf.keySet()) {
				this.final_t_inf = this.final_t_inf > inf.get(u) ? this.final_t_inf : inf.get(u) ;
			}
		}
		this.step_t_inf = (final_t_inf - init_t_inf) / 30 ;
		
		
		
	}
	
	public int infer(Structure struct) {
		PropagationStruct ps = (PropagationStruct)struct ;
		ArrayList<String> initials  = ps.getArrayInit() ;
		
		HashMap<String,Double> infectionsTimes = new HashMap<String, Double>() ;
		for(String u : initials) {
			infectionsTimes.put(u,0.0) ;
		}
		
		
		boolean new_user_added = false ;
		double maxTinfection = 0.0 ;
		
		Set<String> newly_infected_users = infectionsTimes.keySet() ;
		//int debugiter =  0 ;
		
		// METTRE Là L'INFERENCE
		// COMPLETER INFECTIONTIMES
		for(double t = this.init_t_inf ; t<this.final_t_inf ; t=t+this.step_t_inf) {
			for(String target : this.users) {
				if(infectionsTimes.containsKey(target)) {
					continue ;
				}
				double currentTargetHeat = 0.0 ;
				for(String source : infectionsTimes.keySet()) {
					if(!this.users.contains(source)) {
						continue;
					}
					if(infectionsTimes.get(source)!=t) {
						CPUParams zsource = this.dualPoint ? user_modules_2.get(source) : user_modules_1.get(source) ;
						CPUParams ztarget = user_modules_1.get(target) ;
						currentTargetHeat += this.heat.heatkernel(this.nbDims, zsource, ztarget, t-infectionsTimes.get(source)) ;
					}
					if(currentTargetHeat > this.threshold_infer) {
						break ;
					}
				}
				if(currentTargetHeat > this.threshold_infer) {
					infectionsTimes.put(target, t) ;
				}
				
			}
		}
		
		
		
		TreeMap<Long, HashMap<String, Double>> inf = new TreeMap<Long, HashMap<String,Double>>();
		/*for(long t=0  ;t<maxTinfection ; t++) {
			inf.put(t, new HashMap<String,Double>()) ;
		}
		for(String u : infectionsTimes.keySet()) {
			double ti = infectionsTimes.get(u) ;
			for(long t=0 ; t<ti ;t++) {
				inf.get(t).put(u,1.0) ;
			}
		}*/
		inf.put(0l, new HashMap<String,Double>()) ;
		inf.put(1l, new HashMap<String,Double>()) ;
		for(String u : infectionsTimes.keySet()) {
				double s = 1/infectionsTimes.get(u) ;
				inf.get(1l).put(u,1.0) ;
		}
		
		
		//System.out.println(inf);
		ps.setInfections(inf);
		
		return 0;
	}
	
	
public static void main(String[] args) {
		
		int nbDims=2;
		boolean diag = false ;
		double thresh= 1.0 ;
		String modelfile = "" ;
		String resultsFile = "tempres" ;
		String db = "digg" ;
		String cascadetrain = "cascades_1" ;
		String cascadetest = "cascades_2" ;
		double parinf = -Double.MAX_VALUE ;
		double parsup = +Double.MAX_VALUE ;
		int forwardmode= 1 ;
		int maxiters=100000 ;
		double line = 0.01 ;
		double fact = 1.0 ;
		int batchsize = 30 ;
		boolean dualpoint = false ;
		
		
		
		if(args.length>0) {
			nbDims = Integer.parseInt(args[0]) ;
			diag = Boolean.parseBoolean(args[1]) ;
			thresh = Double.parseDouble(args[2]) ;
			modelfile = args[3] ;
			resultsFile = args[4] ;
			db = args[5].split(",")[0] ;
			cascadetrain = args[5].split(",")[1] ;
			cascadetest = args[5].split(",")[2] ;
			parinf = Double.parseDouble(args[6]) ;
			parsup = Double.parseDouble(args[7]) ;
			forwardmode= Integer.parseInt(args[8]) ;
			maxiters=Integer.parseInt(args[9]) ;
			line = Double.parseDouble(args[10]) ;
			fact = Double.parseDouble(args[11]) ;
			batchsize = Integer.parseInt(args[12]) ;
			dualpoint = Boolean.parseBoolean(args[13]) ; ;
		}
				
		///*
		
		//*/
		
		//MLPDiffChain m = new MLPDiffChain("", 10, 1.0, false,false) ;
		MLPDiffChainLTstyle m = new MLPDiffChainLTstyle(modelfile,nbDims, thresh, dualpoint,diag,parinf,parsup,forwardmode,batchsize) ;
		
		//m.prepareLearning("icwsm","cascades_1", 1, 1);
		// m.checkCascades();
		/*System.out.println("Check done");
		if(5>3){
			return ;
		}*/

		Env.setVerbose(1) ;
		LineSearch lsearch=LineSearch.getFactorLine(line,fact); 
 		DescentDirection dir=DescentDirection.getGradientDirection();
		m.learn(db, cascadetrain, 1, 1, Optimizer.getDescent(dir, lsearch),100,maxiters);
		//m.heat.lockParams();
		Date d = new Date() ;
		m.computeAllTranmissions(); 
		Date d2 = new Date() ;
		System.out.println("TEMPS = "+(d2.getTime()-d.getTime())/1000);
		/*if(5>3){
			return ;
		}*/
		try {
			m.save();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*System.out.println("MAP = "+m.evaltest(m.train_cascades)) ;
		for(Parameter p : m.getParams().getParams()){
			System.out.println(p.getVal());
		}*/
		/*for(String u : m.user_modules_src.keySet()) {
			m.user_modules_src.get(u).paramsChanged();//m.user_modules_src.get(u).forward(null);
			System.out.println(u +" : " +m.user_modules_src.get(u).getParameters().getMatrix(0));
		}*/
		EvalPropagationModel.run(new EvalMLPDiffChain(m,db, cascadetrain), resultsFile+".train") ;
		//m.threshold = 1.0 ;
		EvalPropagationModel.run(new EvalMLPDiffChain(m,db, cascadetest), resultsFile+".test") ;
		/*m.threshold = 1.05 ;
		m.computeAllTranmissions(); 
		EvalPropagationModel.run(new EvalMLPDiffChain(m,"digg", "cascades_2"), resultsFile+".test15") ;
		m.threshold = 1.1 ;
		m.computeAllTranmissions(); 
		EvalPropagationModel.run(new EvalMLPDiffChain(m,"digg", "cascades_2"), resultsFile+".test11") ;
		m.threshold = 1.2 ;
		m.computeAllTranmissions(); 
		EvalPropagationModel.run(new EvalMLPDiffChain(m,"digg", "cascades_2"), resultsFile+".test12") ;
		*/
	}	
	

}
