package propagationModels;

import java.io.BufferedReader;
import java.util.LinkedHashMap;

import mlp.CPUAddVals;
import mlp.CPUAddVecs;
import mlp.CPUAverageRows;
import mlp.CPUExp;
import mlp.CPUHingeLoss;
import mlp.CPUL2Norm;
import mlp.CPULog;
import mlp.CPULogistic;
import mlp.CPUMatrix;
import mlp.CPUParams;
import mlp.CPUPower;
import mlp.CPUSparseLinear;
import mlp.CPUSparseMatrix;
import mlp.CPUSum;
import mlp.CPUTermByTerm;
import mlp.CPUTimesVals;
import mlp.DescentDirection;
import mlp.Env;
import mlp.LineSearch;
import mlp.Matrix;
import mlp.Module;
import mlp.Optimizer;
import mlp.Parameter;
import mlp.Parameters;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.Tensor;
import cascades.CascadesLoader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedHashSet;

import experiments.EvalMeasure;
import experiments.EvalMeasureList;
import experiments.EvalPropagationModelConfig;
import experiments.EvalPropagationModel;


import experiments.FMeasure;
import experiments.LogLikelihood;
import experiments.MAP;
import experiments.MeanRank;
import experiments.NbContaminated;
//import trash.ArtificialCascadesLoader;
import utils.ArgsParser;
import utils.CopyFiles;
import utils.Keyboard;
import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Link;
import core.Post;
import core.Structure;
import core.User;
import cascades.Cascade ;
import cascades.CascadesProducer;
import cascades.IteratorDBCascade;
import mlp.EmbeddingsModel;

import java.util.TreeMap;

import mlp.CPUAverageCols;
public class MLPproj extends MLP implements EmbeddingsModel{
	private static final long serialVersionUID = 1L;
	
	protected int nbDims;
	protected HashMap<String,CPUParams> embeddings;
	protected HashMap<String,CPUParams> sender_modules;
	protected HashMap<String,CPUParams> transSender_modules;
	protected HashMap<String,CPUParams> transReceiver_modules;
	protected HashMap<String,CPUParams> diagSenders;
	protected HashMap<String,CPUParams> diagReceivers;
	
	protected CPUTimesVals tv1;
	protected CPUTimesVals tv2;
	protected SequentialModule pInfect;
	protected CPUSum sum;
	SequentialModule seqTer;
	SequentialModule seqBis;
	private double sumReg=0.0;
	private transient TableModule zi;
	private transient TableModule zj;
	private transient TableModule zk;
	private transient Module zi2;
	private transient CPUSparseLinear diagContent;
	private transient CPUParams diag;
	private transient SequentialModule seqDiag;
	private transient CPUSparseLinear transContent;
	private transient CPUAverageRows av;
	//private transient SequentialModule seqDiag;
	private boolean simPLocked=false;
	private transient TableModule diagSenderReceiver;
	private HashMap<String,HashMap<String,Double>> attracts;
	private HashMap<String,HashMap<String,Integer>> nattracts;
	private transient CPUParams allPars;
	//private boolean diagContentLocked=true;
	private double sumLoss=0.0f;
	private int nbForwards=0;
	private int nbSum=0;
	private int nbEstimations=1000000;
	private int nbAffiche=1000000;
	private int freqSave=1000000;
	private Tensor currentInput;
	private double lastLoss=0.0f;
	private int maxIdStem=2000;
	private boolean boolContent=false;
	private boolean logisticDiag=false;
	//private int nbInitSteps=1;
	private double sumLossTot=0;
	private HashMap<String,Module> lastUsed;
	private boolean withDiag=false;
	public boolean withDiagSenders=false;
	public boolean withDiagReceivers=false;
	private boolean withDiagContent=false;
	private boolean transSend=false;
	private boolean transReceive=false;
	private boolean transSendContent=false;
	private EvalPropagationModelConfigx testConfig=null;
	private int freqTest=1000000;
	public HashMap<String,HashSet<String>> couplesInTrain;
	
	boolean diagLocked=false;
	int nbDiagLocked=0;//100000;
	int nbDiagUnLocked=0;//100000;
	int nbFromChangeLock=0;
	double best=Double.NaN;
	CPUL2Norm dist2;
	//private Random r;
	private long maxT;
	double sumNot=0.0;
	int nbNot=1;
	double sumYes=0.0;
	int nbYes=1;
	HashMap<String,Integer> nbUsedInLoss;
	HashMap<String,Integer> nbUsedInLossAsInfected;
	HashMap<String,HashSet<Integer>> senders;
	ArrayList<String> senderList;
	int nbCouplesInLoss;
	//private boolean iInInit=true;
	private boolean computeAttracts=false;
	private boolean unbiased=true;
	HashMap<Integer,Integer> nbInfectedCouplesInCascade;
	HashMap<Integer,Integer> nbCouplesInCascade;
	HashMap<String,HashMap<String,Integer>> nbUsedCouple;
	int nbUniqueCouplesInLoss;
	static double relSim=0.0;
	static double nrelSim=0.0;
	static int relNb=0;
	static int nrelNb=0;
	boolean ecrireCgt=false;
	double parSup=1.0f;
	double parInf=-1.0f;
	double diagSup=10.0f;
	double diagInf=0.0001f;
	//private boolean loaded=false;
	//private boolean inferProbas=true;
	private int sim;
	private int nbDiscard=0;
	private int longDiscard=0; //1000000;
	private HashMap<String,Integer> discards;
	int nbProbas=0;
	private MLPsimFromPoints simP;
	private boolean multiSource=false;
	double pcConta=1.0;
	private int nbMaxSamples=50;
	//private long maxIter=0; 
	private int nbSimul=1;
	private HashMap<String,HashMap<String,Double>> probas;
	int inferMode=2;
	int nbF=0;
	SequentialModule seqSims;
	boolean iterativeOptimization=false;
	boolean prepared=false;
	SequentialModule mainTerm;
	SequentialModule regulTerm;
	double regul=0.0;
	boolean dualPoints;
	boolean senderLocked=false;
	int modeRegul=0; //0 => l1 sur probas, sinon l1 sur poids 
	double minProbaInfer=0.0;
	boolean ignoreCouplesNotInTrain=false;
	double lastLine=0;
	int rescale_mode=0;
	double zStat=1.960;
	//HashSet<String> emitters;
	
	public MLPproj(int nbDims, int maxIdStem, boolean boolContent, boolean logisticDiag, boolean dualPoints, boolean withDiag, boolean withDiagContent, boolean transSend, boolean transReceive, boolean transSendContent,boolean diagSender, boolean withDiagReceivers, int sim,boolean multiSource,double regul){
		this("");
		//System.out.println("here");
		//this.maxT=maxT;
		this.nbDims=nbDims;
		this.maxIdStem=maxIdStem;
		this.withDiag=withDiag;
		this.withDiagContent=withDiagContent;
		this.boolContent=boolContent;
		this.logisticDiag=logisticDiag;
		this.transSend=transSend;
		this.transSendContent=transSendContent;
		this.withDiagSenders=diagSender;
		this.sim=sim;
		setSim(sim);
		this.multiSource=multiSource;
		//this.iInInit=iII;
		this.regul=regul;
		this.transReceive=transReceive;
		this.withDiagReceivers=withDiagReceivers;
		this.dualPoints=dualPoints;
		
		System.out.println("regul="+regul);
		
	}
	
	public MLPproj(String model_file, long maxT, int nbSimul){
		this(model_file,maxT,nbSimul,0);
	}
	
	public MLPproj(String model_file, long maxT, int nbSimul,int inferMode,double minProbaInfer){
		this(model_file,maxT,nbSimul,inferMode,minProbaInfer,false);
	}
	
	public MLPproj(String model_file, long maxT, int nbSimul,int inferMode,double minProbaInfer,boolean ignoreCouplesNotInTrain){
		super(model_file);
		this.ignoreCouplesNotInTrain=ignoreCouplesNotInTrain;
		this.inferMode=inferMode;
		this.nbDims=1;
		transSender_modules=new HashMap<String,CPUParams>();
		transReceiver_modules=new HashMap<String,CPUParams>();
		sender_modules=new HashMap<String,CPUParams>();
		embeddings=new HashMap<String,CPUParams>();
		diagSenders=new HashMap<String,CPUParams>();
		diagReceivers=new HashMap<String,CPUParams>();
		
		this.maxT=maxT;
		this.nbSimul=nbSimul;
		this.minProbaInfer=minProbaInfer;
	}
	public MLPproj(String model_file, long maxT, int nbSimul,int inferMode){
		this(model_file,maxT,nbSimul,inferMode,0.0);
	}
		
    public MLPproj(String model_file){
		this(model_file,0,1,0);
		
	}
    

    
    public void setMinProbaInfer(double p){
    	this.minProbaInfer=p;
    }
    public double getMinProbaInfer(){
    	return this.minProbaInfer;
    }
    
    public void rescaleDiag(double factor){
    	if(!loaded){
    		load();
    	}
    	if(!withDiag){
    		withDiag=true;
    		buildSim();
    	}
    	this.diagInf=this.diagInf*factor;
    	this.diagSup=this.diagSup*factor;
    	if(this.diagSup>Double.MAX_VALUE){
    		this.diagSup=Double.MAX_VALUE;
    	}
    	if(this.diagInf<-Double.MAX_VALUE){
    		this.diagInf=-Double.MAX_VALUE;
    	}
    	this.majParBounds();
    	for(Parameter p:diag.getParamList().getParams()){
    			p.setVal(p.getVal()*factor);
    	}
    	
    }
    
    public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(this.embeddings.keySet());
	}
	public int getContentNbDims(){
		if(!loaded){
			load();
		}
		return maxIdStem;
	}
	
	public void majParBounds(){
		for(CPUParams mod:embeddings.values()){
			for(Parameter p:mod.getParamList().getParams()){
				p.setLowerBound(parInf);
				p.setUpperBound(parSup);
			}
		}
		if(transSend){
			for(CPUParams mod:this.transSender_modules.values()){
				for(Parameter p:mod.getParamList().getParams()){
					p.setLowerBound(parInf);
					p.setUpperBound(parSup);
				}
			}
		}
		if(dualPoints){
			for(CPUParams mod:sender_modules.values()){
				for(Parameter p:mod.getParamList().getParams()){
					p.setLowerBound(parInf);
					p.setUpperBound(parSup);
				}
			}
		}
		if(transReceive){
			for(CPUParams mod:this.transReceiver_modules.values()){
				for(Parameter p:mod.getParamList().getParams()){
					p.setLowerBound(parInf);
					p.setUpperBound(parSup);
				}
			}
		}
		if(withDiag){
			CPUParams mod=diag;
			if(mod!=null){
				for(Parameter p:mod.getParamList().getParams()){
					p.setLowerBound(diagInf);
					p.setUpperBound(diagSup);
				}
			}
		}
		if(withDiagSenders){
			for(CPUParams mod:diagSenders.values()){
				for(Parameter p:mod.getParamList().getParams()){
					p.setLowerBound(diagInf);
					p.setUpperBound(diagSup);
				}
			}
		}
		if(withDiagReceivers){
			for(CPUParams mod:diagReceivers.values()){
				for(Parameter p:mod.getParamList().getParams()){
					p.setLowerBound(diagInf);
					p.setUpperBound(diagSup);
				}
			}
		}
		/*Module mod=simP;
		for(Parameter p:mod.getParamList().getParams()){
				p.setLowerBound(diagInf);
				p.setUpperBound(diagSup);
		}*/
	}
	
	public void setSim(int sim){
		System.out.println("setSim "+sim);
		this.sim=sim;
		if(sim==1){
			simP=new P1Double(nbDims);
		}
		if(sim==2){
			simP=new P2Double(nbDims);
			params.allocateNewParamsFor(simP, 1.0);
			//simP.p
		}
		if(sim==3){
			simP=new P3Double(nbDims);
		}
		if(sim==4){
			simP=new P4Double(nbDims);
			params.allocateNewParamsFor(simP,1.0,0.0001,1000);
			//simP.p
		}
		if(sim==5){
			simP=new P5Double(nbDims);
			params.allocateNewParamsFor(simP,1.0);
			//simP.p
		}
		if(sim==6){
			simP=new P6Double(nbDims);
			params.allocateNewParamsFor(simP,1.0);
			
		}
		if(sim==7){
			simP=new P7Double(nbDims,6.0,-1.0);
			
		}
		if(sim==8){
			simP=new P8Double(nbDims);
			params.allocateNewParamsFor(simP,1.0);
			
		}
		if(sim==9){
			simP=new P9(nbDims,3.0,-1.0,-1.0);
			
		}
		if(sim==10){
			simP=new P10Double(nbDims,6.0,-1.0);
			
		}
		if(sim==11){
			simP=new P11Double(nbDims,0.0,1.0);
			
		}
		if(sim==12){
			simP=new P10Double(nbDims,0.0,1.0);
			
		}
		if(sim==13){
			simP=new P13Double(nbDims);
			
		}
		if(sim==14){
			simP=new P14Double(nbDims);
			
		}
		if(sim==15){
			simP=new P15Double(nbDims);
			
		}
	}
	
	
	
	
	public void buildSim(){
		int nbStems=maxIdStem; 
		int nbd=nbDims;
		
		if(withDiagContent){
				if((!loaded) || (diagContent==null)){
					diagContent=new CPUSparseLinear(nbStems,nbd);
					params.allocateNewParamsFor(diagContent,0.0f);
					((CPUSparseLinear)diagContent).majParams();
				}
		}
		
		
		
		if(transSendContent){
			if((!loaded) || (transContent==null)){
				transContent=new CPUSparseLinear(nbStems,nbDims);
				params.allocateNewParamsFor(transContent,0.0f);
				((CPUSparseLinear)transContent).majParams();
			}
		}
        
		if(withDiag){
			if((!loaded) || (diag==null)){
				diag=new CPUParams(1,nbd);
				params.allocateNewParamsFor(diag,1.0f,diagInf,diagSup);
			}
		}
		
		//System.out.println("diag "+diag.getParameters());
		seqDiag=new SequentialModule();
		TableModule tabDiag=new TableModule();
		seqDiag.addModule(tabDiag);
		
		if(withDiagContent){
			tabDiag.addModule(diagContent);
		}
		
		if(withDiag){
			
			tabDiag.addModule(diag);
			if(withDiagContent){
				seqDiag.addModule(new CPUAddVecs(nbd));
			}
		}
		if(logisticDiag){
			seqDiag.addModule(new CPULogistic(nbd));
		}
		
		if((withDiagSenders) || (withDiagReceivers)) {
			TableModule tab;
			diagSenderReceiver=new TableModule();
			diagSenderReceiver.setName("DiagSenderReceiver");
			
			if(withDiagReceivers){
				diagSenderReceiver.addModule(new CPUParams(1,nbDims)); // On y mettra les diagReceivers
			}
			if(withDiagSenders){
				diagSenderReceiver.addModule(new CPUParams(1,nbDims)); // On y mettra les diagSenders
			}
			if((withDiagSenders) && (withDiagReceivers)){
				SequentialModule seqD=new SequentialModule();
				seqD.addModule(diagSenderReceiver);
				seqD.addModule(new CPUTermByTerm(nbd));
				tab=new TableModule();
				tab.addModule(seqD);
			}
			else{
				tab=diagSenderReceiver;
			}
			
			SequentialModule seqD=new SequentialModule();
			seqD.addModule(tab);
			
			if((withDiag) || (withDiagContent)){
				tab.addModule(seqDiag);
				seqD.addModule(new CPUAddVecs(nbd));
			}
			seqDiag=seqD;
		}
		
		
		ArrayList<Double> weights=new ArrayList<Double>();
		weights.add(1.0);
		weights.add(-1.0);
		
		
		zi=new TableModule();
		zi.setName("zi");
			
		zi.addModule(new CPUParams(1,nbDims));   
		if((transSend) || (transReceive)){
			if((transSend) && (transReceive)){
				System.out.println("BuildSim => tranSend and transReceive");
				SequentialModule seq=new SequentialModule();
				TableModule tab=new TableModule();
				tab.addModule(new CPUParams(1,nbDims));
				tab.addModule(new CPUParams(1,nbDims));
				seq.addModule(tab);
				seq.addModule(new CPUAddVecs(nbDims));   
				zi.addModule(seq);
				
			}
			else{
				zi.addModule(new CPUParams(1,nbDims));
			}
		}
		
		zk=new TableModule();
		zk.addModule(new CPUParams(1,nbDims));   
		
		
		
		SequentialModule transSendSeq=new SequentialModule();
		transSendSeq.addModule(zi);
		if((transSend) || (transReceive)){
			transSendSeq.addModule(new CPUAddVecs(nbDims));   
		}
		
		
		SequentialModule transSendContentSeq;
		if(transSendContent){
			TableModule transSendContentTable=new TableModule();
			transSendContentTable.addModule(transSendSeq);
			transSendContentTable.addModule(transContent);
			transSendContentSeq=new SequentialModule();
			transSendContentSeq.addModule(transSendContentTable);
			transSendContentSeq.addModule(new CPUAddVecs(nbd));
		}
		else{
			transSendContentSeq=transSendSeq;
		}
		Module sender=transSendContentSeq;
		
		Module receiver=zk;
		
		if((withDiag) || (withDiagContent) || (withDiagSenders) || (withDiagReceivers)){
			TableModule dz=new TableModule();
			dz.addModule(transSendContentSeq);
			dz.addModule(seqDiag);
			SequentialModule sm=new SequentialModule();
			sm.addModule(dz);
			CPUTermByTerm tbt=new CPUTermByTerm(nbd);
			tbt.setName("Diag Multiplier ");
			sm.addModule(tbt);
			sender=sm;
			dz=new TableModule();
			dz.addModule(zk);
			dz.addModule(seqDiag.forwardSharedModule());
			sm=new SequentialModule();
			sm.addModule(dz);
			sm.addModule(new CPUTermByTerm(nbd));
			receiver=sm;
		}
		sender.setName("Sender");
		receiver.setName("Receiver");
		simP.setPoint1(sender);
		
		simP.setPoint2(receiver);
		
		
	}
	
	
	public String toString(){
		String sm=model_file.replaceAll("/", "_");
		return("MLPnaiveProj_inferMode="+inferMode+"_minProbaInfer="+minProbaInfer+"_ignoreCoupleNotInTrain="+this.ignoreCouplesNotInTrain+"_maxT="+maxT+"_nbSimul="+nbSimul+"_"+sm);
	}
	
	public int infer(Structure struct) {
		/*if(inferProbas){
			return(inferProbas(struct));
		}
		else{*/
			//return(inferSimulation(struct));
		//}
		/*inferProbas_old(struct);
		PropagationStruct pstruct = (PropagationStruct)struct ;
		
		System.out.println(pstruct.getInfections());
		
		inferProbas(struct);
		pstruct = (PropagationStruct)struct ;
		
		System.out.println(pstruct.getInfections());
		throw new RuntimeException("aa");
		//return 0;*/
		return inferProbas(struct);
		
	}
	
	public int inferSimulation(Structure struct) {
		inferMode=0;
		nbSimul=1;
		return inferProbas(struct);
	}
	
	public int inferProbas(Structure struct){
		
		
		if(!loaded){
			System.out.println("Load Model...");
			load();
		}
		System.out.println("Inference...");
		//System.out.println("bias = "+bias);
		//System.out.println("directed = "+sender_receiver);
		//System.out.println("logistic = "+logisticDiag);
		probas=new HashMap<String,HashMap<String,Double>>();
		PropagationStruct pstruct = (PropagationStruct)struct ;
        		
		
		Tensor tensor=new Tensor(0);
	    if(withDiagContent || transSendContent){
	    	int nbStems=maxIdStem; //+((bias)?1:0);
	        CPUSparseMatrix content=new CPUSparseMatrix(1,nbStems);
	         TreeMap<Integer,Double> cont=pstruct.getDiffusion();
	         TreeMap<Integer,Double> ncont=new TreeMap<Integer,Double>();
	        if(boolContent){
	        	
	        	for(Integer i:cont.keySet()){
	        		ncont.put(i, 1.0);
	        	}
	        	
	        }
	        else{
	        	for(Integer i:cont.keySet()){
	        		ncont.put(i, (double)cont.get(i));
	        	}
	        }
	        content.setValues(ncont);
	    	tensor=new Tensor(1);
	    	tensor.setMatrix(0, content);
	    	if(transSendContent && withDiagContent){
	    		tensor=new Tensor(2);	
	    		tensor.setMatrix(0, content);
	    		tensor.setMatrix(1, content);
	    	}
	    	
	    	
	    }
	    this.currentInput=tensor;
		
		TreeMap<Long,HashMap<String,Double>>  moyContaminated=new TreeMap<Long,HashMap<String,Double>> ();
		for(int i=0;i<nbSimul;i++){
        	inferSimulationProbas(pstruct);
        	TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
			for(Long t:infections.keySet()){
				HashMap<String,Double> us=infections.get(t);
				HashMap<String,Double> h=moyContaminated.get(t);
				if(h==null){
					h=new HashMap<String,Double>();
					moyContaminated.put(t, h);
				}
				for(String u:us.keySet()){
					Double n=h.get(u);
					double v=us.get(u)/nbSimul;
					h.put(u,(n==null)?v:(n+v));
				}
			}
		}
		
        pstruct.setInfections(moyContaminated);
        
        //System.out.println(moyContaminated);
        return 0;
	}
	
	/**
	 * On last step, we set probas of infection for non infected nodes at this step rather than a binary information from simulation. 
	 * @param struct
	 * @return
	 */
	 public int inferSimulationProbas(Structure struct) {
        if (!loaded){
        	load();
        }
        //System.out.println(this.couplesInTrain);
    	PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashSet<String> infectedBefore = new HashSet<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        HashSet<String> users=new HashSet<String>(embeddings.keySet());
        HashMap<String,Double> contagious=new HashMap<String,Double>();
        HashSet<String> inits=new HashSet<String>();
        HashMap<String,Long> times=new HashMap<String,Long>();
        long maxt=0;
        //TreeSet<Long> cTimes=new TreeSet<Long>();
        long f=-1;
	    for(long t:contaminated.keySet()){
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	if(f<0){
	    		f=t;
	    	}
	    	HashMap<String,Double> infectedstep=new HashMap<String,Double>();
	    	if((inferMode!=3) || (f==t)){
	    		infections.put(t, infectedstep);
	    	}
	    	for(String user:inf.keySet()){
	    		contagious.put(user, 1.0);
	    		times.put(user, t);
	    		infectedBefore.add(user);
	    		infectedstep.put(user, 1.0);
	    		if((inferMode!=3) || (f==t)){
	    			inits.add(user);
	    			users.remove(user);
	    		}
	    		
	    	}
	    	maxt=t;
	    	
	    	//cTimes.add(t);
	    }
	    long firstNewT=maxt+1;
	    //System.out.println("firstNewT="+firstNewT+" inferMode="+inferMode);
	    //HashMap<String,Double> lastcontagious=(HashMap<String,Double>)contagious.clone();
        HashMap<String,Double> infectedstep=new HashMap<String,Double>();
        User currentUser ;
        //int it=tt;
        
        //long it=1;
        
        boolean ok=true;
        long ti=firstNewT;
        long maxIter=maxT;
        if(maxIter<0){
        	maxIter=100;
        }
        while(ti<(maxIter+firstNewT)){
        	System.out.println("ti "+contagious.size());
        	HashMap<String,Double> infectedStep=new HashMap<String,Double>();
    		//infections.put(ti, infectedStep);
    		
        	for(String contagiousU : contagious.keySet()) {
        		/*if(!emitters.contains(contagiousU)){
        			continue;
        		}*/
        		HashMap<String,Double> pu=probas.get(contagiousU);
        		if(pu==null){
        			pu=getProbas(contagiousU);
        			probas.put(contagiousU,pu);
        		}
        		HashSet<String> succs=null;
        		if(ignoreCouplesNotInTrain){
        			succs=couplesInTrain.get(contagiousU);
        			if(succs==null){
        				succs=new HashSet<String>();
        			}
        		}
                for(String user:users){ //get(contagiousUser.getID()).keySet()) {
                    if((ignoreCouplesNotInTrain) && (!succs.contains(user))){
                    	continue;
                    }
                    
                	Double p=pu.get(user);
                    if(p==null){
                    	continue;
                    }
                    if(p<minProbaInfer){
                    	continue;
                    }
                   
                    if(Math.random()<p) {
                        	infectedStep.put(user, 1.0);
                        	times.put(user, ti);
                        	//System.out.println(contagiousU+"=>"+user+"="+p);
                        	
                    }	
                }
            }
        	
        	if(inferMode<2){
        		infections.put(ti,infectedStep);
        	}
        	contagious=infectedStep;
        	for(String user:infectedStep.keySet()){
        		users.remove(user);
        		infectedBefore.add(user);
        	}
        	
        	ti++;
        	
            if(contagious.isEmpty())
                break ; 
            
            
            /*if((maxIter>0) && (ti>maxIter)){
            	break;
            }*/
        	
        }
        if(ti>firstNewT){
        	System.out.println("nb conta = "+infectedBefore.size());
        }
        //ti=maxIter+1;
        if(this.inferMode>=1){
        	if(inferMode>=2){
        		ti=firstNewT+maxIter;
        	}
        	//System.out.println("ti="+ti);
	        HashMap<String,Double> notYet=new HashMap<String,Double>();
	       
	        for(String user : infectedBefore) {
	        	//System.out.println(user + "infectedBefore");
	        	/*if(!emitters.contains(user)){
        			continue;
        		}*/
	        	HashMap<String,Double> pu=probas.get(user);
        		if(pu==null){
        			pu=getProbas(user);
        			probas.put(user,pu);
        		}
	        	//System.out.println(pu);
        		HashSet<String> succs=null;
        		if(ignoreCouplesNotInTrain){
        			succs=couplesInTrain.get(user);
        			if(succs==null){
        				succs=new HashSet<String>();
        			}
        		}
                long tu=times.get(user);
	            for(String v : pu.keySet()){ //get(contagiousUser.getID()).keySet()) {
	            	if((ignoreCouplesNotInTrain) && (!succs.contains(v))){
	                    	continue;
	                }
	                    
	            	if(((inferMode<2) && (infectedBefore.contains(v))) || ((inferMode>=2) && (v.equals(user) || (inits.contains(v)))))
	                    continue ;
	            	
	            	if((inferMode==3) && (times.containsKey(v)) && (times.get(v)<=tu)){
	            		continue;
	            	}
	                Double p=notYet.get(v);
	                p=(p==null)?1.0:p;
	                Double pp=pu.get(v);
	                pp=(pp==null)?0.0:pp;
	                p*=(1.0-pp);
	                if((pp>1.0) || (pp<0.0)){
	                	throw new RuntimeException(v + " => "+pp);
	                }
	                notYet.put(v,p);
	                //
	                
	            }
	        }
	        infectedstep=new HashMap<String,Double>();
	        for(String user:notYet.keySet()){
	        	double p=1.0-notYet.get(user);
	        	if((p>1.0) || (p<0.0)){
	        		throw new RuntimeException(user+" => "+p);
	        	}
	        	p*=0.99999;
	        	
	        	infectedstep.put(user,p);
	        	
	        	//System.out.println("fin "+ user + " => "+(1.0-notYet.get(user)));
	        	//System.out.println(user + " : "+(1.0-notYet.get(user)));
	        }
	        
	       
	        infections.put(ti,infectedstep);
        }
        //infections.add(infectedstep);
       
        pstruct.setInfections(infections) ;
        //System.out.println(infections);
        return 0;
    }
	
	 /**
	 * Returns the similarities of points with the referer point whose name in given as parameter.
	 * @param referer point
	 * @return sims
	 */
	public HashMap<String,Double> getSims(String referer){
		currentInput=new Tensor(0);
		HashMap<String,Double> pr=getProbas(referer);
		
		return(pr);
	}
	 
	public HashMap<String,Double> getProbas(String init){
		//PropagationStruct pstruct = (PropagationStruct)struct ;
		ArrayList<String> users=new ArrayList<String>(embeddings.keySet());
        //System.out.println("users="+user_modules.keySet().size());
	    
        HashMap<String,Double> ret=new HashMap<String,Double>();
	    
	    ArrayList<String> receivers=new ArrayList<String>();
	    
	    int nbLignes=0;
	    //System.out.print(init+" ");
	    if(embeddings.containsKey(init)){
	    	for(String user:users){
	    		receivers.add(user);
	    		nbLignes++;
	    	}
	    
	    	//System.out.println();
	    }
	    int nbd=nbDims;
	    if(nbLignes>0){
	    	CPUParams modUs=new CPUParams(nbLignes,nbDims);
		    CPUParams modInit=new CPUParams(nbLignes,nbDims);
		    CPUParams modInit_send=new CPUParams(nbLignes,nbDims);
		    CPUParams modInit_receive=new CPUParams(nbLignes,nbDims);
		    CPUParams modDiag_receive=new CPUParams(nbLignes,nbDims);
		    
		    CPUParams mi=null;
		    if(withDiagSenders){
		    	int x=0;
		    	if(withDiagReceivers){
		    		x=1;
		    	}
		    	mi=diagSenders.get(init);
		    	if(mi!=null){
	    			diagSenderReceiver.setModule(x,mi);
	    		}
	    		else{
	    			diagSenderReceiver.setModule(x,new CPUParams(1,nbDims,1.0f));
	    		}
		    	
	    	}
		    if(dualPoints){
		    	mi=sender_modules.get(init);
		    }
		    else{
		    	mi=embeddings.get(init);
		    }
		    CPUParams msi=null;
		    if(transSend){
		    	msi=this.transSender_modules.get(init);
		    	if(msi==null){
		    		msi=new CPUParams(1,nbDims,0.0);
		    	}
		    }
		    
		    //Matrix results=new CPUMatrix(nbLignes,1);
		    for(int i=0;i<nbLignes;i++){
		    	modInit.addParametersFrom(mi);
		    	String uk=receivers.get(i);
		    	modUs.addParametersFrom(embeddings.get(uk));
		    	if(transSend){
		    		modInit_send.addParametersFrom(msi);
		    	}
		    	if(transReceive){
		    		modInit_receive.addParametersFrom(transReceiver_modules.get(uk));
		    	}
		    	if(withDiagReceivers){
		    		modDiag_receive.addParametersFrom(this.diagReceivers.get(uk));
		    	}
		    	
		    }
		    if(withDiagReceivers){
		    	diagSenderReceiver.setModule(0,modDiag_receive);
		    }
		    zi.setModule(0, modInit);
		    zk.setModule(0, modUs);
		    if((transSend) || (transReceive)){
		    	if((transSend) && (transReceive)){
		    		SequentialModule seq=(SequentialModule)zi.getModule(1); 
		    		TableModule tab=(TableModule)seq.getModule(0);
		    		tab.setModule(0, modInit_send);
		    		tab.setModule(1, modInit_receive);
		    	}
		    	else{
		    		if(transSend){
		    			zi.setModule(1, modInit_send);
		    		}
		    		if(transReceive){
		    			zi.setModule(1, modInit_receive);
		    		}
		    	}
		    }
		    simP.forward(this.currentInput);
		    Tensor res=simP.getOutput();
			Matrix m=res.getMatrix(0);
			//System.out.println(sources.get(i)+" => "+user_modules.get(sources.get(i)).getParameters());
			//System.out.println(sourcesSend.get(i)+" => "+translations.get(sourcesSend.get(i)).getParameters());
			//System.out.println(receivers.get(i)+" => "+user_modules.get(receivers.get(i)).getParameters());
			  
			//System.out.println(m);
			for(int i=0;i<nbLignes;i++){
				ret.put(receivers.get(i), m.getValue(i, 0));
				
			}
			
	    }
	    
	    return ret;
	}
	
	 
	/*public int inferProbas_old(Structure struct){
		
		
		if(!loaded){
			System.out.println("Load Model...");
			load();
		}
		System.out.println("Inference...");
		//System.out.println("bias = "+bias);
		//System.out.println("directed = "+sender_receiver);
		//System.out.println("logistic = "+logisticDiag);
		
		
		PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
    	
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        int tt=1;
        for(Long t:contaminated.keySet()){
        	HashMap<String,Double> infstep=contaminated.get(t);
        	infections.put((long)tt, (HashMap<String,Double>) infstep.clone());
        	tt++;
	    }
        
	    int firstNewT=tt+1;
        
        pstruct.setInfections(infections);
        return inferProbas(pstruct,firstNewT);
	}
	
	
	
	public int inferProbas(Structure struct, int firstNewT){
		
		if(!loaded){load();}
		PropagationStruct pstruct = (PropagationStruct)struct ;
    	//TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInfections();
        TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
        ArrayList<String> infected = new ArrayList<String>((PropagationStruct.getPBeforeT(infections)).keySet()) ;
        ArrayList<String> users=new ArrayList<String>(embeddings.keySet());
        System.out.println("users="+embeddings.keySet().size());
	    
	    
	    
	    
	    ArrayList<String> sources=new ArrayList<String>();
	    ArrayList<String> receivers=new ArrayList<String>();
	    
	    int nbLignes=0;
	    System.out.println("init="+infected.size());
	    for(String init:infected){
	    	System.out.print(init+" ");
	    	if(embeddings.containsKey(init)){
	    		System.out.print("ok");
	    		for(String user:users){
	    			sources.add(init);
	    			receivers.add(user);
	    			nbLignes++;
	    		}
	    	}
	    	System.out.println();
	    }
	    int nbd=nbDims;
	    System.out.println("Modules created");
	    if(nbLignes>0){
	    	CPUParams modUs=new CPUParams(1,nbDims);
		    CPUParams modInit=new CPUParams(1,nbDims);
		    CPUParams modInit_send=new CPUParams(1,nbDims);
		    seqDiag=new SequentialModule();
			TableModule tabDiag=new TableModule();
			seqDiag.addModule(tabDiag);
			
			if(withDiagContent){
				tabDiag.addModule(diagContent);
			}
			
			if(withDiag){
				
				tabDiag.addModule(diag);
				if(withDiagContent){
					seqDiag.addModule(new CPUSum(nbd,2));
				}
			}
			if(logisticDiag){
				seqDiag.addModule(new CPULogistic(nbd));
			}
			
			if(withDiagSenders){
				SequentialModule seqD=new SequentialModule();
				diagSender=new TableModule();
				diagSender.addModule(new CPUParams(1,nbDims)); // On y mettra les diagSenders
				seqD.addModule(diagSender);
				if(withDiag || withDiagContent){
					diagSender.addModule(seqDiag);
					seqD.addModule(new CPUAddVecs(nbd));
				}
				seqDiag=seqD;
			}
			
			ArrayList<Double> weights=new ArrayList<Double>();
			weights.add(1.0);
			weights.add(-1.0);
			
			
			zi=new TableModule();
			zi.setName("zi");
				
			zi.addModule(modInit);   
			if(transSend){
				zi.addModule(modInit_send);   
				
			}
			
			
			zk=new TableModule();
			zk.addModule(modUs);   
			
			SequentialModule transSendSeq=new SequentialModule();
			transSendSeq.addModule(zi);
			if(transSend){
				transSendSeq.addModule(new CPUAddVecs(nbd));
			}
			
			SequentialModule transSendContentSeq;
			if(transSendContent){
				TableModule transSendContentTable=new TableModule();
				transSendContentTable.addModule(transSendSeq);
				transSendContentTable.addModule(transContent);
				transSendContentSeq=new SequentialModule();
				transSendContentSeq.addModule(transSendContentTable);
				transSendContentSeq.addModule(new CPUAddVecs(nbd));
			}
			else{
				transSendContentSeq=transSendSeq;
			}
			Module sender=transSendContentSeq;
			
			Module receiver=zk;
			
			if((withDiag) || (withDiagContent)  || (withDiagSenders)){
				TableModule dz=new TableModule();
				dz.addModule(transSendContentSeq);
				dz.addModule(seqDiag);
				SequentialModule sm=new SequentialModule();
				sm.addModule(dz);
				sm.addModule(new CPUTermByTerm(nbd));
				sender=sm;
				dz=new TableModule();
				dz.addModule(zk);
				dz.addModule(seqDiag.forwardSharedModule());
				sm=new SequentialModule();
				sm.addModule(dz);
				sm.addModule(new CPUTermByTerm(nbd));
				receiver=sm;
			}
			sender.setName("Sender");
			receiver.setName("Receiver");
			simP.setPoint1(sender);
			
			simP.setPoint2(receiver);
		    

		    Tensor tensor=new Tensor(0);
		    if(withDiagContent || transSendContent){
		    	int nbStems=maxIdStem; //+((bias)?1:0);
		        CPUSparseMatrix content=new CPUSparseMatrix(1,nbStems);
		         TreeMap<Integer,Double> cont=pstruct.getDiffusion();
		         TreeMap<Integer,Double> ncont=new TreeMap<Integer,Double>();
		        if(boolContent){
		        	
		        	for(Integer i:cont.keySet()){
		        		ncont.put(i, 1.0);
		        	}
		        	
		        }
		        else{
		        	for(Integer i:cont.keySet()){
		        		ncont.put(i, (double)cont.get(i));
		        	}
		        }
		        content.setValues(ncont);
		    	tensor=new Tensor(1);
		    	tensor.setMatrix(0, content);
		    	if(transSendContent && withDiagContent){
		    		tensor=new Tensor(2);	
		    		tensor.setMatrix(0, content);
		    		tensor.setMatrix(1, content);
		    	}
		    	
		    	
		    }
		    this.currentInput=tensor;
		    Matrix results=new CPUMatrix(nbLignes,1);
		    for(int i=0;i<nbLignes;i++){
		    	CPUParams mi=embeddings.get(sources.get(i));
		    	zi.setModule(0, mi);
		    	CPUParams msi=transSender_modules.get(sources.get(i));
		    	if(transSend){
		    		if(msi!=null){
		    			zi.setModule(1, msi);
		    		}
		    		else{
		    			zi.setModule(1, new CPUParams(1,nbDims,0.0));
		    		}
		    	}
		    	if(withDiagSenders){
		    		mi=diagSenders.get(sources.get(i));
		    		if(mi!=null){
		    			diagSender.setModule(0,mi);
		    		}
		    		else{
		    			diagSender.setModule(0,new CPUParams(1,nbDims,1.0f));
		    		}
		    	}
		    	zk.setModule(0, embeddings.get(receivers.get(i)));
		    	simP.forward(currentInput);
		    	Tensor res=simP.getOutput();
			    Matrix m=res.getMatrix(0);
			    //System.out.println(sources.get(i)+" => "+user_modules.get(sources.get(i)).getParameters());
			    //System.out.println(sourcesSend.get(i)+" => "+translations.get(sourcesSend.get(i)).getParameters());
			    //System.out.println(receivers.get(i)+" => "+user_modules.get(receivers.get(i)).getParameters());
			    
			    //System.out.println(m);
			    results.setValue(i, 0, m.getValue(0, 0));
			    //System.out.println(i+"=>"+m.getValue(0, 0));
		    }
		       
	       
	        
		    
		    
		    System.out.println("Diffusion done");
		    //System.out.println(res);
		    //double max=-1;
		    HashMap<String,Double> maxP=new HashMap<String,Double>();
		    int r=0;
		    TreeMap<Long,HashMap<String,Double>> ref=pstruct.getInfections();
		    HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		    for(int i=0;i<nbLignes;i++){
		    		    String user=receivers.get(i);
			    		Double v=maxP.get(user);
			    		if(v==null){
			    			v=1.0;
			    		}
			    		double pr=results.getValue(i, 0);
			    		//if((v==null) || (pr>v)){
			    		maxP.put(user, v*(1.0-pr));
			    			
			    		//}
			    			
		    }
		    
		    
		    HashMap<String,Double> infstep=new HashMap<String,Double>();
		    	
		    int relNb=0;
			int nrelNb=0;
			double relSim=0.0;
			double nrelSim=0.0;
		    for(String user:maxP.keySet()){
		    	if(!infected.contains(user)){
		    		double m=(double)(1.0-maxP.get(user));
		    		infstep.put(user, m);
		    		if(href.containsKey(user)){
		    			relSim+=m;
		    			relNb++;
		    		}
		    		else{
		    			nrelSim+=m;
		    			nrelNb++;
		    		}
		    		//System.out.println(user+" : "+(double)maxP.get(user)+ "ref "+href.get(user));
		    	}
		    	
		    }
		    if(relNb==0){
		    	relNb=1;
		    }
		    System.out.println("relSim : "+(relSim/relNb)+ " nrelSim : "+(nrelSim/nrelNb)+" r "+((relSim/relNb)/(nrelSim/nrelNb)));
		    infections.put((long)firstNewT, infstep);
		    /*System.out.println("init :"+contaminated);
		    System.out.println("href :"+href);
		    HashMap<String,Double> hinf=PropagationStruct.getPBeforeT(infections);
		    ArrayList<String> ainf=new ArrayList<String>(hinf.keySet());
		    ContaminatedComparator ccomp=new ContaminatedComparator(hinf);
		    Collections.sort(ainf,ccomp);
		    
		    for(String user:href.keySet()){
		    	System.out.println(user +" : "+ ainf.indexOf(user) +" = "+hinf.get(user)+" minDist="+minDist.get(user)+" ref?"+((href.containsKey(user))?"1":"0"));
		    }
		    
		    Clavier.saisirLigne("");
		    
		    
		    simP.clearListeners();
		    //sender.destroy();
		    //receiver.destroy();
		    
	    }
	    
	    pstruct.setInfections(infections);
		return 0;
	}
	*/
	
	
	
   
    public void load(){
    	String filename=model_file;
    	System.out.println("Load "+model_file);
        User.reinitAllLinks();
        BufferedReader r;
        transSender_modules=new HashMap<String,CPUParams>();
        embeddings=new HashMap<String,CPUParams>();
        diagSenders=new HashMap<String,CPUParams>();
        train_users=new HashMap<String,HashMap<Integer,Double>>();
        couplesInTrain=new HashMap<String,HashSet<String>>();
        //emitters=new HashSet<String>();
        params=new Parameters();
        transSend=false;
        transReceive=false;
        transSendContent=false;
        withDiag=false;
        withDiagContent=false;
        withDiagSenders=false;
        withDiagReceivers=false;
        dualPoints=false;
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          boolean modules_mode=false;
          boolean trans_mode=false;
          boolean transr_mode=false;
          boolean transContent_mode=false;
          boolean profiles=false;
          boolean diagContent_mode=false;
          boolean diagSender_mode=false;
          boolean diagReceiver_mode=false;
          boolean diag_mode=false;
          boolean dual_mode=false;
          boolean couples_mode=false;
          //boolean emitters_mode=false;
          String[] sline;//=line.split("=");
          
          nbDims=-1;
          while((line=r.readLine()) != null) {
        	
        	
        	if(line.contains("<User_Modules>")){
        		 modules_mode=true;
                  continue;
          	}
          	if(line.contains("</User_Modules>")){
                  modules_mode=false;
          		  continue;
          	}
          	if(line.contains("<Sender_Modules>")){
          		dual_mode=true;
                 continue;
         	}
         	if(line.contains("</Sender_Modules>")){
                 dual_mode=false;
         		  continue;
         	}
          	if(line.contains("<TransSender>")){
      		 	 trans_mode=true;
                continue;
        	}
         	if(line.contains("</TransSender>")){
               trans_mode=false;
       		  continue;
         	}
         	if(line.contains("<TransReceiver>")){
     		 	 transr_mode=true;
     		 	 continue;
         	}
        	if(line.contains("</TransReceiver>")){
              transr_mode=false;
      		  continue;
        	}
         	if(line.contains("<TransContent>")){
     		 	 transContent_mode=true;
               continue;
         	}	
        	if(line.contains("</TransContent>")){
              transContent_mode=false;
      		  continue;
        	}
          	if(line.contains("<DiagContent>")){
                  diagContent_mode=true;
        		  continue;
        	}
          	if(line.contains("</DiagContent>")){
          		  diagContent_mode=false;
        		  continue;
        	}
        	if(line.contains("<DiagSenders>")){
                diagSender_mode=true;
    		  	continue;
        	}
        	if(line.contains("</DiagSenders>")){
      		  	diagSender_mode=false;
    		 	continue;
        	}
        	if(line.contains("<DiagReceivers>")){
                diagReceiver_mode=true;
    		  	continue;
        	}
        	if(line.contains("</DiagReceivers>")){
      		  	diagReceiver_mode=false;
    		 	continue;
        	}
          	if(line.contains("<Diag>")){
                diag_mode=true;
      		  	continue;
          	}
        	if(line.contains("</Diag>")){
        		  diag_mode=false;
        		  continue;
        	}
          	if(line.contains("<User_Profiles>")){
          		profiles=true;
          		continue;
          	}
          	if(line.contains("</User_Profiles>")){
          		profiles=false;
          		continue;
          	}
          	if(line.contains("<Couples>")){
          		System.out.println("couples_mode_on");
          		couples_mode=true;
          		continue;
          	}
          	if(line.contains("</Couples>")){
          		couples_mode=false;
          		continue;
          	}
          	/*if(line.contains("<Emitters>")){
          		emitters_mode=true;
          		continue;
          	}
          	if(line.contains("</Emitters>")){
          		emitters_mode=false;
          		continue;
          	}*/
        	if(modules_mode){  
        		String[] tokens = line.split("\t") ;
	            String user=tokens[0];
	            double[] vals;
	            int nbd=tokens.length-1;
	           
	            vals=new double[nbd];
	            for(int i=1;i<tokens.length;i++){
	            	vals[i-1]=Double.valueOf(tokens[i]);
	            }
	            if(nbDims<0){
	            	nbDims=vals.length;
	            }
	            CPUParams cpuPars=new CPUParams(1,nbDims);
	            params.allocateNewParamsFor(cpuPars, vals,parInf,parSup);
	            embeddings.put(user, cpuPars);
	            cpuPars.paramsChanged();
	            //System.out.println(user+" : "+cpuPars.getParamList());
	            continue;
        	}
        	if(dual_mode){
        		dualPoints=true;
        		String[] tokens = line.split("\t") ;
	            String user=tokens[0];
	            
        		double[] vals=new double[tokens.length-1];
	            for(int i=1;i<tokens.length;i++){
	            	vals[i-1]=Double.valueOf(tokens[i]);
	            }
	            if(nbDims<0){
	            	nbDims=vals.length;
	            }
	            CPUParams cpuPars=new CPUParams(1,nbDims);
	            params.allocateNewParamsFor(cpuPars, vals,parInf,parSup);
	            sender_modules.put(user, cpuPars);
	            cpuPars.paramsChanged();
	            //System.out.println(user+" : "+cpuPars.getParamList());
	            continue;
        	}
        	if(trans_mode){  
        		transSend=true;
        		String[] tokens = line.split("\t") ;
	            String user=tokens[0];
	            
        		double[] vals=new double[tokens.length-1];
	            for(int i=1;i<tokens.length;i++){
	            	vals[i-1]=Double.valueOf(tokens[i]);
	            }
	            if(nbDims<0){
	            	nbDims=vals.length;
	            }
	            CPUParams cpuPars=new CPUParams(1,nbDims);
	            params.allocateNewParamsFor(cpuPars, vals,parInf,parSup);
	            transSender_modules.put(user, cpuPars);
	            continue;
        	}
        	if(transr_mode){  
        		transReceive=true;
        		String[] tokens = line.split("\t") ;
	            String user=tokens[0];
	            
        		double[] vals=new double[tokens.length-1];
	            for(int i=1;i<tokens.length;i++){
	            	vals[i-1]=Double.valueOf(tokens[i]);
	            }
	            if(nbDims<0){
	            	nbDims=vals.length;
	            }
	            CPUParams cpuPars=new CPUParams(1,nbDims);
	            params.allocateNewParamsFor(cpuPars, vals,parInf,parSup);
	            transReceiver_modules.put(user, cpuPars);
	            continue;
        	}
        	if(transContent_mode){ 
        		transSendContent=true;
        		String[] tokens = line.split("\t") ;
	            double[] vals=new double[tokens.length-1];
	            for(int i=1;i<tokens.length;i++){
	            	//System.out.println(tokens[i]);
	            	vals[i-1]=Double.valueOf(tokens[i]);
	            	
	            }
	            int nbd=nbDims;
	            //if(directed){nbd*=2;}
	            transContent=new CPUSparseLinear(vals.length/nbd,nbd);
	            params.allocateNewParamsFor(transContent, vals);
	            transContent.paramsChanged();
	            continue;
	            
        	}
        	if(diagContent_mode){ 
        		withDiagContent=true;
        		String[] tokens = line.split("\t") ;
	            double[] vals=new double[tokens.length-1];
	            for(int i=1;i<tokens.length;i++){
	            	//System.out.println(tokens[i]);
	            	vals[i-1]=Double.valueOf(tokens[i]);
	            	
	            }
	            int nbd=nbDims;
	            //if(directed){nbd*=2;}
	            diagContent=new CPUSparseLinear(vals.length/nbd,nbd);
	            params.allocateNewParamsFor(diagContent, vals);
	            diagContent.paramsChanged();
	            continue;
	            
        	}
        	if(diag_mode){ 
        		withDiag=true;
        		String[] tokens = line.split("\t") ;
	            double[] vals=new double[tokens.length-1];
	            for(int i=1;i<tokens.length;i++){
	            	//System.out.println(tokens[i]);
	            	vals[i-1]=Double.valueOf(tokens[i]);
	            	
	            }
	            int nbd=nbDims;
	            //if(directed){nbd*=2;}
	            diag=new CPUParams(1,nbd);
	            params.allocateNewParamsFor(diag, vals,diagInf,diagSup);
	            diag.paramsChanged();
	            continue;
        	}
        	if(diagSender_mode){ 
        		withDiagSenders=true;
        		String[] tokens = line.split("\t") ;
	            String user=tokens[0];
	            
        		double[] vals=new double[tokens.length-1];
	            for(int i=1;i<tokens.length;i++){
	            	vals[i-1]=Double.valueOf(tokens[i]);
	            }
	            
	            CPUParams cpuPars=new CPUParams(1,nbDims);
	            params.allocateNewParamsFor(cpuPars, vals, diagInf, diagSup);
	            diagSenders.put(user, cpuPars);
	            continue;
	            
        	}
        	if(diagReceiver_mode){ 
        		withDiagReceivers=true;
        		String[] tokens = line.split("\t") ;
	            String user=tokens[0];
	            
        		double[] vals=new double[tokens.length-1];
	            for(int i=1;i<tokens.length;i++){
	            	vals[i-1]=Double.valueOf(tokens[i]);
	            }
	            
	            CPUParams cpuPars=new CPUParams(1,nbDims);
	            params.allocateNewParamsFor(cpuPars, vals, diagInf, diagSup);
	            diagReceivers.put(user, cpuPars);
	            continue;
	            
        	}
        	/*if(emitters_mode){
        		String[] tokens = line.split("\t") ;
	            String user=tokens[0];
	            emitters.add(user);
        	}*/
        	if(couples_mode){
        		String[] tokens = line.split(";") ;
	            String user=tokens[0];
	            String user2="";
	            if(tokens.length>1){
	            	user2=tokens[1];
	            }
	            //System.out.println(user+";"+user2);
	            HashSet<String> succs=couplesInTrain.get(user);
        		if(succs==null){
        			succs=new HashSet<String>();
        			couplesInTrain.put(user, succs);
        		}
        		succs.add(user2);
	            continue;
        	}
        	if(profiles){
        		String[] tokens = line.split("\t") ;
	            String user=tokens[0];
	            HashMap<Integer,Double> weights=new HashMap<Integer,Double>();
	            for(int i=1;i<tokens.length;i++){
	            	String[] els=tokens[i].split("=");
	            	int st=Integer.parseInt(els[0]);
	            	double val=Double.valueOf(els[1]);
	            	weights.put(st, val);
	            }
	            train_users.put(user, weights);
	            continue;
        	}
        	
        	if(line.startsWith("maxIdStem")){
        		sline=line.split("=");
                maxIdStem=Integer.parseInt(sline[1]);
                continue;
        	}
        	if(line.startsWith("nbDims")){
        		sline=line.split("=");
                nbDims=Integer.parseInt(sline[1]);
                continue;
        	}
        	/*if(line.startsWith("iInInit")){
        		sline=line.split("=");
                iInInit=Boolean.valueOf(sline[1]);
                continue;
        	}*/
        	if(line.startsWith("sim")){
          	  sline=line.split("=");
          	  sline=sline[1].split("\t");
              sim=Integer.valueOf(sline[0]);
              setSim(sim);
              double[] vals=new double[sline.length-1];
	          for(int i=1;i<sline.length;i++){
	            	//System.out.println(tokens[i]);
	            	vals[i-1]=Double.valueOf(sline[i]);
	           }
	           params.allocateNewParamsFor(simP, vals);
	           simP.paramsChanged();
	           continue;
            }
        	if(line.startsWith("multiSource")){
            	  sline=line.split("=");
            	  multiSource=Boolean.valueOf(sline[1]);
            	  continue;
            }
        	if(line.startsWith("boolContent")){
            	  sline=line.split("=");
            	  boolContent=Boolean.valueOf(sline[1]);
            	  continue;
            }
        	if(line.startsWith("logisticDiag")){
          	  	  sline=line.split("=");
          	  	  logisticDiag=Boolean.valueOf(sline[1]);
          	  	  continue;
            }
        	if(line.startsWith("regul")){
      	  	  sline=line.split("=");
      	  	  regul=Double.valueOf(sline[1]);
      	  	  continue;
        	}
        	if(line.startsWith("parInf")){
        	  	  sline=line.split("=");
        	  	  parInf=Double.valueOf(sline[1]);
        	  	  continue;
        	}
        	if(line.startsWith("parSup")){
      	  	  sline=line.split("=");
      	  	  parSup=Double.valueOf(sline[1]);
      	  	  continue;
        	}
        	if(line.startsWith("diagInf")){
      	  	  sline=line.split("=");
      	  	  diagInf=Double.valueOf(sline[1]);
      	  	  continue;
        	}
        	if(line.startsWith("diagSup")){
    	  	  sline=line.split("=");
    	  	  diagSup=Double.valueOf(sline[1]);
    	  	  continue;
        	}
        	if(line.startsWith("unbiased")){
            	  sline=line.split("=");
            	  if(sline[1].contains("1")){
            		  unbiased=true;
            	  }
            	  else{
            		  if(sline[1].contains("0")){
            			  unbiased=false;
            		  }
            		  else{
            			  unbiased=Boolean.valueOf(sline[1]);
            		  }
            	  }
            	 
            	  continue;
            }
          }
          /*if(emitters.size()==0){
        	  emitters.addAll(train_users.keySet());
          }*/
          
          majParBounds();
          r.close();
          
          //System.out.println("Sim="+sim);
          //System.out.println("Sim="+sim);
          
          loaded=true;
          System.out.println("size embeddings loaded ="+embeddings.size());
          //System.out.println(this.couplesInTrain);
          buildSim();
          System.out.println("Sim built");
          model_name=model_file;
          //saveProbas();
        }
        catch(IOException e){
        	throw new RuntimeException("Load model => Probleme lecture modele "+filename+"\n "+e);
        }
       
    }

    public void save(String rep){
    	save(rep,"model");
    }
    
    public void save(String rep,String fic){
    	model_file=rep+"/"+fic;
    	PrintStream p = null;
		double loss=(sumLoss*1.0)/(nbSum*1.0);
		System.out.println("Saved Loss = "+loss+" "+model_file);
        try{
        	File file=new File(model_file);
        	File dir=file.getParentFile();
        	if(dir!=null){
        		dir.mkdirs();
        	}
        	
          p = new PrintStream(file) ;
          p.println("Loss="+loss);
          p.println("nbDims="+nbDims);
          p.println("maxIdStem="+maxIdStem);
          //p.println("iInInit="+iInInit);
          Parameters pars=this.simP.getParamList();
    	  StringBuilder sb=new StringBuilder();
    	  for(int i=0;i<pars.size();i++){
    		  sb.append("\t"+pars.get(i).getVal());
    	  }
          p.println("sim="+sim+sb.toString());
          p.println("multiSource="+this.multiSource);
          //p.println("maxT="+maxT);
          p.println("boolContent="+boolContent);
          p.println("logisticDiag="+logisticDiag);
          p.println("unbiased="+unbiased);
          p.println("regul="+regul);
          p.println("parInf="+parInf);
          p.println("parSup="+parSup);
          p.println("diagInf="+diagInf);
          p.println("diagSup="+diagSup);
          p.println("<User_Modules>");
          for(String user:embeddings.keySet()){
        	  sb=new StringBuilder();
        	  CPUParams mod=embeddings.get(user);
        	  pars=mod.getParamList();
        	  sb.append(user);
        	  for(int i=0;i<pars.size();i++){
        		  sb.append("\t"+pars.get(i).getVal());
        	  }
        	  p.println(sb.toString());
  		  }
          p.println("</User_Modules>");
          if(dualPoints){
        	  p.println("<Sender_Modules>");
              for(String user:sender_modules.keySet()){
            	  sb=new StringBuilder();
            	  CPUParams mod=sender_modules.get(user);
            	  pars=mod.getParamList();
            	  sb.append(user);
            	  for(int i=0;i<pars.size();i++){
            		  sb.append("\t"+pars.get(i).getVal());
            	  }
            	  p.println(sb.toString());
      		  }
              p.println("</Sender_Modules>");
          }
          if(transSend){
        	  p.println("<TransSender>");
              for(String user:transSender_modules.keySet()){
            	  sb=new StringBuilder();
            	  CPUParams mod=transSender_modules.get(user);
            	  pars=mod.getParamList();
            	  sb.append(user);
            	  for(int i=0;i<pars.size();i++){
            		  sb.append("\t"+pars.get(i).getVal());
            	  }
            	  p.println(sb.toString());
      		  }
              p.println("</TransSender>");
          }
          if(transReceive){
        	  p.println("<TransReceiver>");
              for(String user:transReceiver_modules.keySet()){
            	  sb=new StringBuilder();
            	  CPUParams mod=transReceiver_modules.get(user);
            	  pars=mod.getParamList();
            	  sb.append(user);
            	  for(int i=0;i<pars.size();i++){
            		  sb.append("\t"+pars.get(i).getVal());
            	  }
            	  p.println(sb.toString());
      		  }
              p.println("</TransReceiver>");
          }
          if(transSendContent){
        	  p.println("<TransContent>");
        	  pars=this.transContent.getParamList();
        	  sb=new StringBuilder();
        	  for(int i=0;i<pars.size();i++){
        		  sb.append("\t"+pars.get(i).getVal());
        	  }
        	  p.println(sb.toString());
  		
        	  p.println("</TransContent>");
          }
          if(withDiagContent){
        	  p.println("<DiagContent>");
        	  pars=this.diagContent.getParamList();
        	  sb=new StringBuilder();
        	  for(int i=0;i<pars.size();i++){
        		  sb.append("\t"+pars.get(i).getVal());
        	  }
        	  p.println(sb.toString());
  		
        	  p.println("</DiagContent>");
          }
          if(withDiag){
        	  p.println("<Diag>");
        	  pars=this.diag.getParamList();
        	  sb=new StringBuilder();
        	  for(int i=0;i<pars.size();i++){
        		  sb.append("\t"+pars.get(i).getVal());
        	  }
              p.println(sb.toString());
        	  p.println("</Diag>");
          }
          if(withDiagSenders){
        	  p.println("<DiagSenders>");
              for(String user:diagSenders.keySet()){
            	  sb=new StringBuilder();
            	  CPUParams mod=diagSenders.get(user);
            	  pars=mod.getParamList();
            	  sb.append(user);
            	  for(int i=0;i<pars.size();i++){
            		  sb.append("\t"+pars.get(i).getVal());
            	  }
            	  p.println(sb.toString());
      		  }
              p.println("</DiagSenders>");
          }
          if(withDiagReceivers){
        	  p.println("<DiagReceivers>");
              for(String user:diagReceivers.keySet()){
            	  sb=new StringBuilder();
            	  CPUParams mod=diagReceivers.get(user);
            	  pars=mod.getParamList();
            	  sb.append(user);
            	  for(int i=0;i<pars.size();i++){
            		  sb.append("\t"+pars.get(i).getVal());
            	  }
            	  p.println(sb.toString());
      		  }
              p.println("</DiagReceivers>");
          }
          /*p.println("<Emitters>");
          for(String user:emitters){
        	  p.println(user);
          }
          p.println("</Emitters>");
          */
          p.println("<User_Profiles>");
          for(String user:train_users.keySet()){
        	  sb=new StringBuilder();
        	  sb.append(user);
        	  HashMap<Integer,Double> w=train_users.get(user);
        	  for(Integer st:w.keySet()){
        		  sb.append("\t"+st+"="+w.get(st));
        	  }
        	  p.println(sb.toString());
          }
          p.println("</User_Profiles>");
          p.println("<Couples>");
          for(String user:couplesInTrain.keySet()){
        	  sb=new StringBuilder();
        	  HashSet<String> w=couplesInTrain.get(user);
        	  for(String st:w){
        		  sb.append(user+";"+st+"\n");
        	  }
        	  p.print(sb.toString());
          }
          p.println("</Couples>");
          
          
          
          
        }
        catch(IOException e){
        	e.printStackTrace();
        	throw new RuntimeException("Probleme sauvegarde modele "+model_file+"\n "+e);
        	
        }
        finally{
        	if(p!=null){
        		p.close();
        	}
        }
    }
    
    
    
	public void saveProbas(){
		File f=new File(model_name);
		File dir=f.getParentFile();
		String filename=dir.getAbsolutePath()+"/lastProbas";
		
		
		
        try{
        	
          PrintStream p = new PrintStream(filename) ;
          p.println("maxIter="+maxT);
          p.println("contaMaxDelay=-1");
         
          System.out.println("Save conta for "+embeddings.size()+" users");
          int nb=0;
          for(String uS:embeddings.keySet()){
        	  //if(this.couplesInTrain)
        	  HashMap<String,Double> hi=getProbas(uS);
        	  for(String w:hi.keySet()){
        		  double v=hi.get(w);
        		  if(v>=0.01){
        			  System.out.println(uS+"\t"+w+"\t"+v);
        			  p.println(uS+"\t"+w+"\t"+v);
        		  }
        		 
        	  }
        	  nb++;
        	  if((nb%100)==0){
        		  System.out.println(nb+" probas sauvees");
        	  }
          }
          
        }
        catch(IOException e){
        	System.out.println("Probleme sauvegarde modele "+filename);
        	
        }
	}
	
    @Override
	public void save(){ // throws IOException {
		/*String format = "dd.MM.yyyy_H.mm.ss";
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		last_save=formater.format(date);*/
		//String filename=model_name+"_"+last_save;
		File f=new File(model_name);
		File dir=f.getParentFile();
		File fileOut=new File(dir.getAbsolutePath()+"/last");
    	save(dir.getAbsolutePath(),"last");
		
		//PrintStream p = null;
		double loss=(sumLoss*1.0)/(nbSum*1.0);
		//System.out.println("Saved Loss = "+loss);
        try{  
          if(Double.isNaN(best)){
  			File fOut=new File(dir.getAbsolutePath()+"/best");
  			
  			if(fOut.exists()){
  				BufferedReader r;
  				r = new BufferedReader(new FileReader(fOut)) ;
  				String line ;
  				
  				String[] sline;
            
  				while((line=r.readLine()) != null) {
  					if(line.startsWith("Loss")){
  						sline=line.split("=");
  			          	best=Double.valueOf(sline[1]);
  			          	break;
  					}
  				}
  				r.close();
  			}
  		  }
          if((Double.isNaN(best)) || (loss<best)){
    		dir=f.getParentFile();
          	File fOut=new File(dir.getAbsolutePath()+"/best");
          	CopyFiles.copyFile(fileOut,fOut);
          	best=loss;   
    	  }
          
          
        }
        catch(IOException e){
        	e.printStackTrace();
        	throw new RuntimeException("Probleme sauvegarde modele "+fileOut.getAbsolutePath()+"\n "+e);
        	
        }
       
		

	}

	/*public void test(){
		long step=1l;
		int nbInitSteps=1;
		users=new ArrayList<String>();
		users.add("a"); users.add("b"); users.add("c"); users.add("d"); users.add("e");
		train_cascades=new HashMap<Integer,PropagationStruct>();
		
		TreeMap<Long,HashMap<String,Double>> init=new TreeMap<Long,HashMap<String,Double>>();
		TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
		TreeMap<Integer,Double> diffusion=new TreeMap<Integer,Double>();
		HashMap<String,Double> h=new HashMap<String,Double>();
		h.put("a",1.0);
		init.put(1l, h);
		infections.put(1l, h);
		h=new HashMap<String,Double>();
		h.put("b", 1.0);
		infections.put(2l,h);
		h=new HashMap<String,Double>();
		h.put("c", 1.0);
		infections.put(3l,h);
		diffusion.put(0, 1.0f);
		PropagationStruct struct=new PropagationStruct(null,step,nbInitSteps,init,infections,diffusion);
		train_cascades.put(1, struct);
		
		
		init=new TreeMap<Long,HashMap<String,Double>>();
		infections=new TreeMap<Long,HashMap<String,Double>>();
		diffusion=new TreeMap<Integer,Double>();
		h=new HashMap<String,Double>();
		h.put("a",1.0);
		init.put(1l, h);
		infections.put(1l, h);
		h=new HashMap<String,Double>();
		h.put("c", 1.0);
		infections.put(2l,h);
		//h=new HashMap<String,Double>();
		//h.put("e", 1.0);
		//infections.put(3l,h);
		diffusion.put(1, 1.0f);
		struct=new PropagationStruct(null,step,nbInitSteps,init,infections,diffusion);
		train_cascades.put(2, struct);
		
		init=new TreeMap<Long,HashMap<String,Double>>();
		infections=new TreeMap<Long,HashMap<String,Double>>();
		diffusion=new TreeMap<Integer,Double>();
		h=new HashMap<String,Double>();
		h.put("d",1.0);
		init.put(1l, h);
		infections.put(1l, h);
		h=new HashMap<String,Double>();
		h.put("b", 1.0);
		infections.put(2l,h);
		h=new HashMap<String,Double>();
		h.put("e", 1.0);
		infections.put(3l,h);
		diffusion.put(1, 1.0f);
		struct=new PropagationStruct(null,step,nbInitSteps,init,infections,diffusion);
		train_cascades.put(3, struct);
		
		
		cascades_ids=new ArrayList<Integer>(train_cascades.keySet());
	}*/
   
    public void specialInit(){
    	if(this.transSend){
    		for(CPUParams mod:transSender_modules.values()){
    			Parameters pars=mod.getParamList();
    			ArrayList<Parameter> pp=pars.getParams();
    			int x1=(int)(Math.random()*pp.size());
    			int x2=x1;
    			while(x1==x2){
    				x2=(int)(Math.random()*pp.size());
    			}
    			int i=0;
    			for(Parameter p:pp){
    				if((i==x1) || (i==x2)){
    					p.setVal(1.0);
    				}
    				else{
    					p.setVal(0.0);
    				}
    			}
    			//mod.lockParams();
    		}
    	}
    }
    
    
    public void changeNbDims(int nbd){
    	if(nbd==nbDims){
    		return;
    	}
    	this.nbDims=nbd;
    	for(String user:embeddings.keySet()){
			
			//System.out.println("Creation Module for user "+user);
    		
			CPUParams mod=embeddings.get(user);
			Parameters pars=mod.getParamList();
			pars.adjustParameters(nbd, parInf, parSup);
			CPUParams nmod=new CPUParams(1,nbd);
			nmod.setName(user);
			nmod.setParameters(pars);
			embeddings.put(user,nmod);
			
			if(dualPoints){
				mod=sender_modules.get(user);
				pars=mod.getParamList();
				pars.adjustParameters(nbd, parInf, parSup);
				nmod=new CPUParams(1,nbd);
				nmod.setName(user);
				nmod.setParameters(pars);
				sender_modules.put(user,nmod);
			}
			//System.out.println(user+" = "+user_modules.get(user).getParamList());
			if(transSend){
				mod=this.transSender_modules.get(user);
				pars=mod.getParamList();
				pars.adjustParameters(nbd, parInf, parSup);
				nmod=new CPUParams(1,nbd);
				nmod.setName(user);
				nmod.setParameters(pars);
				transSender_modules.put(user,nmod);
			}
			if(transReceive){
				mod=this.transReceiver_modules.get(user);
				pars=mod.getParamList();
				pars.adjustParameters(nbd, parInf, parSup);
				nmod=new CPUParams(1,nbd);
				nmod.setName(user);
				nmod.setParameters(pars);
				transReceiver_modules.put(user,nmod);
			}
			if(withDiagSenders){
				mod=this.diagSenders.get(user);
				pars=mod.getParamList();
				pars.adjustParameters(nbd, parInf, parSup);
				nmod=new CPUParams(1,nbd);
				nmod.setName(user);
				nmod.setParameters(pars);
				diagSenders.put(user,nmod);
				
			}
			if(withDiagReceivers){
				mod=this.diagReceivers.get(user);
				pars=mod.getParamList();
				pars.adjustParameters(nbd, parInf, parSup);
				nmod=new CPUParams(1,nbd);
				nmod.setName(user);
				nmod.setParameters(pars);
				diagReceivers.put(user,nmod);
			}
		}
    	if(withDiag){
    		CPUParams mod=diag;
    		Parameters pars=mod.getParamList();
    		pars.adjustParameters(nbd, parInf, parSup);
    		CPUParams nmod=new CPUParams(1,nbd);
    		nmod.setName("diag");
    		nmod.setParameters(pars);
    		diag=nmod;
    	}
    }
    
    public void prepareLearning(PropagationStructLoader ploader){
    	
    	
		if(model_file.length()!=0){
			if(!loaded){load();}
		}
		else{
			buildSim();
		}
		
		//emitters=new HashSet<String>();
		
		String format = "dd.MM.yyyy_H.mm.ss";
		
		//contentLocked=false;
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		//if(model_name.length()==0){
			model_name="propagationModels/MLPProj_Dims-"+nbDims+"_step-"+ploader.getStep()+"_ratioInits-"+ploader.getRatioInits()+"_nbMaxInits-"+ploader.getNbMaxInits()+"_db-"+ploader.getDb()+"_cascadesCol-"+ploader.getCollection()+"_start"+ploader.getStart()+"_nbC"+ploader.getNbC()+"/dP-"+dualPoints+"_tS-"+transSend+"_tR-"+transReceive+"_tSC-"+transSendContent+"_diag-"+withDiag+"_wDC-"+withDiagContent+"_wDS-"+withDiagSenders+"_wDR-"+withDiagReceivers+"_sim-"+sim+"_unbiased-"+unbiased+"_regul-"+regul+"_multiSource-"+multiSource+((this.rescale_mode>0)?("-rescale_mode"+rescale_mode+((this.rescale_mode>=1)?("-zStat"+zStat):"")):"")+"/"+formater.format(date);
		//}
		
		System.out.println("learn : "+model_name);
			
		super.prepareLearning(ploader);
		
		System.out.println("learn : "+model_name);
		
		
		attracts=new HashMap<String,HashMap<String,Double>>();
		nattracts=new HashMap<String,HashMap<String,Integer>>();
		discards=new HashMap<String,Integer>();
		currentInput=new Tensor(0);
		if(withDiagContent || transSendContent){
			if(withDiagContent && transSendContent){
				currentInput=new Tensor(2);
			}
			else{
				currentInput=new Tensor(1);
			}
		}
		
		int nu=0;
		for(String user:users){
			nu++;
			
			if(!embeddings.containsKey(user)){
				//System.out.println("Creation Module for user "+user);
				CPUParams mod=new CPUParams(1,nbDims);
				mod.setName(user);
				if(sim>=13){
					/*double[] vals=new double[nbDims];
					//int k=(int)(Math.random()*nbDims);
					vals[0]=1.0;
					for(int i=1;i<nbDims;i++){
						vals[i]=Math.sqrt(Math.random());
						int k=(int)(Math.random()*2);
						if(k==1){
							vals[i]=1.0;
						}
						else{
							vals[i]=0.0;
						}
					}*/
					params.allocateNewParamsFor(mod,0.5,1.5,parInf,parSup);
					//params.allocateNewParamsFor(mod,200.0/nbDims,parInf,parSup);
				}
				else{
					if(sim==6){
						params.allocateNewParamsFor(mod,0,1.0/nbDims,parInf,parSup);
					}
					else{
						if(sim==3){
							double[] vals=new double[nbDims];
							vals[0]=(nbDims-1.0);
							for(int i=1;i<nbDims;i++){
								vals[i]=Math.random()*2.0;
							}
							params.allocateNewParamsFor(mod,vals,parInf,parSup);
							
						}
						else{
							params.allocateNewParamsFor(mod,parInf,parSup);
						}
					}
				}
				embeddings.put(user,mod);
			}
			if(dualPoints){
				if(!sender_modules.containsKey(user)){
					//System.out.println("Creation Module for user "+user);
					CPUParams mod=new CPUParams(1,nbDims);
					mod.setName(user);
					if(sim>=13){
						/*double[] vals=new double[nbDims];
						//int k=(int)(Math.random()*nbDims);
						vals[0]=Math.sqrt((nbDims-1.0)/2.0);
						for(int i=1;i<nbDims;i++){
							vals[i]=1.0;
							int k=(int)(Math.random()*2);
							if(k==1){
								vals[i]=1.0;
							}
							else{
								vals[i]=0.0;
							}
						}*/
						if(sim==13){
							params.allocateNewParamsFor(mod,0.5,1.5,parInf,parSup); 
						}
						if(sim==14){
							params.allocateNewParamsFor(mod,1.0/nbDims,parInf,parSup); 
						}
						if(sim==15){
							params.allocateNewParamsFor(mod,0.5/nbDims,parInf,parSup); 
						}
					}
					else{
						if(sim==6){
							params.allocateNewParamsFor(mod,0,1.0/nbDims,parInf,parSup);
						}
						else{
							if(sim==3){
								params.allocateNewParamsFor(mod,0.0,2.0,parInf,parSup);
							}
							else{
								params.allocateNewParamsFor(mod,(1.0/nbDims),parInf,parSup); 
							}
						}
					}
					sender_modules.put(user,mod);
				}
			}
			//System.out.println(user+" = "+user_modules.get(user).getParamList());
			if(transSend){
				if(!transSender_modules.containsKey(user)){
					CPUParams mod=new CPUParams(1,nbDims);
					mod.setName(user+"_translation");
					params.allocateNewParamsFor(mod,0.0,parInf,parSup);
					transSender_modules.put(user,mod);
				}
			}
			if(transReceive){
				if(!transReceiver_modules.containsKey(user)){
					CPUParams mod=new CPUParams(1,nbDims);
					mod.setName(user+"_translationReceiver");
					params.allocateNewParamsFor(mod,0.0,parInf,parSup);
					transReceiver_modules.put(user,mod);
				}
			}
			if(withDiagSenders){
				if(!diagSenders.containsKey(user)){
					CPUParams mod=new CPUParams(1,nbDims);
					mod.setName(user+"_diagSender");
					params.allocateNewParamsFor(mod,1.0f, diagInf, diagSup);
					diagSenders.put(user,mod);
				}
			}
			if(withDiagReceivers){
				if(!diagReceivers.containsKey(user)){
					CPUParams mod=new CPUParams(1,nbDims);
					mod.setName(user+"_diagReceiver");
					params.allocateNewParamsFor(mod,1.0f, diagInf, diagSup);
					diagReceivers.put(user,mod);
				}
			}
		}
		//specialInit();
		
		
		
		seqSims=new SequentialModule();
		seqSims.addModule(simP);
		CPUTimesVals tv=new CPUTimesVals(1,new CPUMatrix(1,1));
		System.out.println(tv.toString());
		seqSims.addModule(tv);
		CPUAddVals adv=new CPUAddVals(1,new CPUMatrix(1,1));
		seqSims.addModule(adv);
		seqBis=(SequentialModule)seqSims.forwardSharedModule();
		seqTer=new SequentialModule();
		seqTer.addModule(simP.forwardSharedModule());
		CPUMatrix m1=new CPUMatrix(1,1);
		m1.setValue(0, 0, -0.999999998);
		CPUMatrix m2=new CPUMatrix(1,1);
		m2.setValue(0, 0, 0.999999999);
		CPUTimesVals tv2=new CPUTimesVals(1,m1);
		seqTer.addModule(tv2);
		CPUAddVals adv2=new CPUAddVals(1,m2);
		seqTer.addModule(adv2);
		
		
		
		av=new CPUAverageRows(1);
		mainTerm=new SequentialModule();
		mainTerm.addModule(seqBis);  
		/*global.addModule(somme);
		global.addModule(sum);*/
		mainTerm.addModule(new CPULog(1));
		mainTerm.addModule(av);
		mainTerm.addModule(new CPUTimesVals(1,-1.0f));
		global=mainTerm;
		
		if(regul>0){
			regulTerm=new SequentialModule();
			if(modeRegul==0){
				regulTerm.addModule(seqBis);  
				regulTerm.addModule(new CPUAverageRows(1));
				regulTerm.addModule(new CPUTimesVals(1,1.0f));
			}
			else{
				regulTerm.addModule(new CPUParams(nbMaxSamples,nbDims));
				regulTerm.addModule(new CPUL2Norm(nbDims));
				regulTerm.addModule(new CPUAverageRows(1));
				regulTerm.addModule(new CPUTimesVals(1,1.0f));
				
			}
		}
		senders=new HashMap<String,HashSet<Integer>>();
		for(int ic:cascades_ids){
			PropagationStruct pstruct=this.train_cascades.get(ic);
			TreeMap<Long,HashMap<String,Double>> set=pstruct.getInitContaminated();
			HashMap<String,Long> times=pstruct.getInfectionTimes();
				
			for(Long t:set.keySet()){
	        	HashMap<String,Double> ust=set.get(t);
	        	for(String us:ust.keySet()){
	        		HashSet<Integer> x=senders.get(us);
	        		if(x==null){
	        			x=new HashSet<Integer>();
	        			senders.put(us,x);
	        		}
	        		x.add(ic);
	        	}
			}
		}
		
		nbUsedInLoss=new HashMap<String,Integer>();
		nbUsedInLossAsInfected=new HashMap<String,Integer>();
		nbCouplesInLoss=0;
		nbInfectedCouplesInCascade=new HashMap<Integer,Integer>();
		nbCouplesInCascade=new HashMap<Integer,Integer>();
		nbUsedCouple=new HashMap<String,HashMap<String,Integer>>();
		
		if(couplesInTrain==null){
			couplesInTrain=new HashMap<String,HashSet<String>>();
		}
		for(int ic:cascades_ids){
			PropagationStruct pstruct=this.train_cascades.get(ic);
			TreeMap<Long,HashMap<String,Double>> set=pstruct.getInitContaminated();
			HashMap<String,Long> times=pstruct.getInfectionTimes();
			
			HashSet<String> vus=new HashSet<String>();
			HashSet<String> notvus=new HashSet<String>(users);
			HashSet<String> notvusInfected=new HashSet<String>(pstruct.getArrayContamined());
			int nbi=0;
			int nbc=0;
			int nbcc=0;
			for(Long t:set.keySet()){
	        	HashMap<String,Double> ust=set.get(t);
	        	
	        	notvus.removeAll(ust.keySet());
	        	notvusInfected.removeAll(ust.keySet());
	        	int nvs=notvus.size();
	        	int nvsi=notvusInfected.size();
	        	int vs=vus.size();
	        	for(String us:ust.keySet()){
	        		HashSet<String> succs=couplesInTrain.get(us);
	        		if(succs==null){
	        			succs=new HashSet<String>();
	        			couplesInTrain.put(us, succs);
	        		}
	        		long time=times.get(us);
	        		for(Entry<String,Long> en:times.entrySet()){
	        			long ti=en.getValue();
	        			if(ti>time){
	        				String x=en.getKey();
	        				if(!x.equals(us)){
	        					succs.add(x);
	        				}
	        			}
	        		}
	        		
	        		HashSet<Integer> h=senders.get(us);
	        		if(h==null){
	        			h=new HashSet<Integer>();
	        			senders.put(us,h);
	        		}
	        		h.add(ic);
	        		Integer x=nbUsedInLoss.get(us);
	        		x=(x==null)?0:x;
	        		x+=nvs+vs;
	        		nbCouplesInLoss+=nvs; //+vs;
	        		nbcc+=nvs;
	        		nbc+=nvs;
	        		nbi+=nvsi;
	        		nbUsedInLoss.put(us,x);
	        		x=nbUsedInLossAsInfected.get(us);
	        		x=(x==null)?0:x;
	        		x+=nvs+vs;
	        		nbUsedInLossAsInfected.put(us,x);
	        		HashMap<String,Integer> hu=nbUsedCouple.get(us);
	        		if(hu==null){
	        			hu=new HashMap<String,Integer>();
	        			nbUsedCouple.put(us, hu);
	        		}
	        		for(String v:notvus){
	        			Integer nv=hu.get(v);
	        			hu.put(v,((nv==null)?1:(nv+1)));
	        		}
	        	}
	        	vus.addAll(ust.keySet());
	        	
	        }
			int vs=vus.size();
			for(String us:notvus){
				Integer x=nbUsedInLoss.get(us);
        		x=(x==null)?0:x;
        		x+=vs;
        		//nbCouplesInLoss+=vs;
        		nbUsedInLoss.put(us,x);
        		if(times.containsKey(us)){
        			x=nbUsedInLossAsInfected.get(us);
        			x=(x==null)?0:x;
        			x+=vs;
        			nbUsedInLossAsInfected.put(us,x);
        		}
			}
			nbCouplesInCascade.put(ic, nbcc);
		}
		senderList=new ArrayList<String>(senders.keySet());
		nbUniqueCouplesInLoss=0;
		for(String ui:nbUsedCouple.keySet()){
			HashMap<String,Integer> h=nbUsedCouple.get(ui);
			nbUniqueCouplesInLoss+=h.size();
		}
		
		nbProbas=0;
		for(int ic:cascades_ids){
			PropagationStruct pstruct=this.train_cascades.get(ic);
			TreeMap<Long,HashMap<String,Double>> set=pstruct.getInfections();
			HashSet<String> pos=pstruct.getPossibleUsers();
			nbProbas+=pos.size()-set.get(1l).keySet().size();
		}
		
		prepared=true;
	
   
	}
	
    public void learn(PropagationStructLoader ploader, Optimizer optim){
    		prepareLearning(ploader);
    		if(senderLocked){
    			zi.lockParams(true);
    		}
    		if(this.simPLocked){
    			if(sim==2){
    				((P2Double)simP).p.lockParams(true);
    			}
    		}
    		//diagLocked=true;
    		//this.lastUsed=new HashMap<String,Module>();
    		//this.updateParams(0.0);
    		System.out.println(global.toString());
    		global.setName("global");
    		System.out.println(global.toString());
    		global.showStructure();
    		Parameters pars=this.simP.getParamList();
       	  	StringBuilder sb=new StringBuilder();
       	  	for(int i=0;i<pars.size();i++){
       		  sb.append("\t"+pars.get(i).getVal());
       	  	}
            System.out.println("sim="+sim+sb.toString());
    		save();
    	    optim.optimize(this);
    	    loaded=true;
    }
    
	public void forward(){
		
		if((nbForwards%nbAffiche==0) && (nbForwards!=0) && (global!=regulTerm)){ // && (listei.size()>0)){
			double rYes=(1.0*sumYes/nbYes);
			double rNot=(1.0*sumNot/nbNot);
			System.out.println(this.getName()+" Average Loss = "+String.format("%.5f", sumLossTot/(1.0*nbForwards))+", "+String.format("%.5f", sumLoss/(1.0*nbSum))+", "+String.format("%.5f", lastLoss)+" nbSums="+nbSum+" nbForwards="+nbForwards+" "+String.format("%.5f",rYes)+" "+String.format("%.5f",rNot)+" "+(String.format("%.5f",rYes/rNot)));
			
		}
		
		
		if((nbForwards%freqSave==0)&& (nbForwards!=0) && (global!=regulTerm)){
				System.out.println("save "+nbForwards);
				save();
				sumYes=0.0;
				sumNot=0.0;
				nbYes=0;
				nbNot=0;
		}
		
		if((nbForwards%freqTest==0) && (testConfig!=null) && (nbForwards>0) && (global!=regulTerm)){
			System.out.println("save before test "+nbForwards);
			save();
			String format = "dd.MM.yyyy_H.mm.ss";
			java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
			Date date = new Date(); 
			
			File f=new File(model_name);
			File dir=f.getParentFile();
			
			File fileOut=new File(dir.getAbsolutePath()+"/Results/"+formater.format(date));
			String rep=fileOut.getAbsolutePath();
			MLPproj testeur=new MLPproj(this.model_file,maxT,nbSimul,inferMode,this.minProbaInfer);
			testConfig.reinitModels();
			testConfig.addModel(testeur, 1);
			//fileOut.mkdirs();
			EvalPropagationModel.run(testConfig, rep);
			save(rep);
			//buildSim();
		}
		
		if((global!=regulTerm) && ((nbForwards%nbEstimations==0)  || (iterativeOptimization))){
			sumLoss=0;
			nbSum=0;
			//System.gc();
			System.out.println("reinit sum");
		}
		
		
		if(regul>0.0){
			if(global==mainTerm){
				global=regulTerm;
				if(modeRegul==0){
					forward_L1();
				}
				else{
					forward_L1_poids();
				}
				return;
			}
			else{
				global=mainTerm;
			}
		}
		if(multiSource){
			forward_cascadeMultiSource();
		}
		else{
			forward_cascade();
		}
	}
	
	
 	
	public void forward_cascade() {
		//System.out.println(zi_zj);
		nbF++;
		
		// choose cascade
		int x=(int)(Math.random()*this.cascades_ids.size()); 
		int c=cascades_ids.get(x);
		//System.out.println("Cascade "+c);
		
		PropagationStruct pstruct=this.train_cascades.get(c);
		HashMap<String,Double> initInfected=pstruct.getHashInit();
		TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
		LinkedHashSet<String> users=pstruct.getPossibleUsers();
		
        if(discards.size()>0){
        	TreeMap<Long,HashMap<String,Double>> infectionsWithoutDiscards=new TreeMap<Long,HashMap<String,Double>>();
        	for(Long t:infections.keySet()){
        		HashMap<String,Double> us=infections.get(t);
        		HashMap<String,Double> nus=new HashMap<String,Double>();
        		for(String u:us.keySet()){
        			if(!discards.containsKey(u)){
        	        	
        				nus.put(u,us.get(u));
        			}
        		}
        		if(nus.size()>0){
        			infectionsWithoutDiscards.put(t, nus);
        		}
        	}
        	infections=infectionsWithoutDiscards;
        	Set<String> s=discards.keySet();
        	for(String u:s){
	        	Integer d=discards.get(u);
	        	d-=1;
	        	if(d<=0){
	        		discards.remove(u);
	        	}
	        	else{
	        		discards.put(u, d);
	        	}
	        }
        	if(infections.get(1l).size()==0){
        		forward_cascade();
        		return;
        	}
        }
        
        TreeMap<Long,ArrayList<String>> cumul=PropagationStruct.getListBeforeT(infections);
        
        
        
        ArrayList<String> contaminated = new ArrayList<String>(cumul.lastEntry().getValue());
        //ArrayList<String> initContaminated = new ArrayList<String>(cumul.get(this.nbInitSteps).keySet()) ;
        
        HashSet<String> nc=new HashSet<String>(users);
        for(String u:contaminated){
        	nc.remove(u);
        	
        }
        ArrayList<String> notContaminated = new ArrayList<String>(nc);
        for(String u:cumul.get(1l)){
            contaminated.remove(u);
        }
        if((contaminated.size()==0) || (notContaminated.size()==0)){
        	forward_cascade();
        	return;
		}
            
        
        //System.out.println("Chosen cascade = "+c);
        //System.out.println(infections);
        //System.out.println(contaminated.size()+" init "+(PropagationStruct.getPBeforeT(infections)).keySet().size()+" contamines");
        /*ArrayList<String> listei=new ArrayList<String>();
        ArrayList<String> listek=new ArrayList<String>();
        ArrayList<String> listeiNotConta=new ArrayList<String>();
        ArrayList<String> listekNotConta=new ArrayList<String>();
        */
        HashMap<String,HashMap<String,Double>> hashConta=new HashMap<String,HashMap<String,Double>>();
        HashMap<String,HashMap<String,Double>> hashNotConta=new HashMap<String,HashMap<String,Double>>();
        
        //HashMap<String,HashMap<String,Double>> hash=new HashMap<String,HashMap<String,Double>>(); 
        
        HashMap<String,Long> times=pstruct.getInfectionTimes();
       
        lastUsed=new HashMap<String,Module>();
        //ArrayList<Long> infTimes=new ArrayList<Long>(infections.keySet());
		//HashSet<String> vus=new HashSet<String>();
		boolean stop=false;
		int nbSamples=0;
		int nfirst=infections.get(1l).keySet().size();
		double rconta=1.0;//(1.0f*contaminated.size())/(users.size()-nfirst);
		double rnconta=1.0;//(1.0f*(notContaminated.size()))/(users.size()-nfirst);
		double proba=1.0;
		if(unbiased){
			proba=(1.0f*nbCouplesInCascade.get(c)/nbCouplesInLoss)*cascades_ids.size(); 
			//System.out.println("proba="+proba);
		}
		double pc=(1.0f*contaminated.size())/(users.size()-nfirst);
		//double rf=1.0/(1.0+0.00001*nbForwards);
		//double rf=1.0-(1.0/(1.0+Math.exp(1-0.0000001*nbForwards)));
		double rf=1.0-(1.0/(1.0+Math.exp(10-0.00001*nbF)));
		//double pconta=pc+(1.0-pc)*rf; //pcConta;
		double plage=1.0+((users.size()-nfirst)*rf);
		double den=plage*contaminated.size()+notContaminated.size();
		double pconta=(plage*contaminated.size())/den;
		pcConta*=0.99999;
		int nn=0;
		while(nbSamples<nbMaxSamples){
			double a=Math.random();
			String uk="";
			ArrayList<String> before;
			Long maxTi=0l;
			HashMap<String,Double> pars;
			String ui="";
			Double  nh;
			if(a<pconta){
				x=(int)(Math.random()*contaminated.size());
		        uk=contaminated.get(x);
		        
		        Long tk=times.get(uk);
		        if(tk==1){
		        	throw new RuntimeException("Should not have time equal to 1");
		        }
		        if(tk==null){
		        	throw new RuntimeException("Should not have null time");
		        }
		        Long ti=tk;
		        
		        maxTi=tk-1;
		        if(maxTi>pstruct.getNbInitSteps()){
		        		maxTi=(long)pstruct.getNbInitSteps();
		        }
		        if(!cumul.containsKey(maxTi)){
					long max=0;
					for(Long tt:cumul.keySet()){
						if(tt>maxTi){
							break;
						}
						max=tt;
					}
					maxTi=max;
				}
		        //System.out.println("maxTi = "+maxTi);
		        before=cumul.get(maxTi);
		        x=(int)(Math.random()*before.size());
		        ui=before.get(x);
		       
		        
		        pars=hashConta.get(uk);
				if(pars==null){
					pars=new HashMap<String,Double>();
					hashConta.put(uk,pars);
				}
				nh=pars.get(ui);
				if(nh==null){
					nh=0.0;
					nn++;
				}
				pars.put(ui,nh+rconta);
				//emitters.add(ui);
				
			}
			else{
				//System.out.println(ui+"=>"+uk+"("+times.get(ui)+","+tk+")"+":"+(nh+rconta));
				
				x=(int)(Math.random()*notContaminated.size());
		        uk=notContaminated.get(x);
		        maxTi=cumul.lastKey();
		        if(maxTi>pstruct.getNbInitSteps()){
	        		maxTi=(long)pstruct.getNbInitSteps();
		        }
		        before=cumul.get(maxTi);
		        x=(int)(Math.random()*before.size());
		        ui=before.get(x);
		        pars=hashNotConta.get(uk);
				if(pars==null){
					pars=new HashMap<String,Double>();
					hashNotConta.put(uk,pars);
				}
				nh=pars.get(ui);
				if(nh==null){
					nh=0.0;
					nn++;
				}
				pars.put(ui,nh+rnconta);
				//emitters.add(ui);
				
				//System.out.println(ui+"=>"+uk+"("+times.get(ui)+")"+":"+(nh+rnconta));
			}	
			
			nbSamples++;
		}
		nbSamples=nn; //hashConta.size()+hashNotConta.size();
		CPUParams parsi=((CPUParams)zi.getModule(0)); //
		parsi.setNbVecs(nbSamples);
		//CPUParams parsi=new CPUParams(nbSamples,nbDims);
		CPUParams diagSenderi=null;
		if(withDiagSenders){
			int xi=0;
			if(withDiagReceivers){
				xi=1;
			}
			diagSenderi=((CPUParams)diagSenderReceiver.getModule(xi)); //new CPUParams(nbSamples,nbDims);
			diagSenderi.setNbVecs(nbSamples);
			
		}
		CPUParams diagReceiveri=null;
		if(withDiagReceivers){
			diagReceiveri=((CPUParams)diagSenderReceiver.getModule(0)); //new CPUParams(nbSamples,nbDims);
			diagReceiveri.setNbVecs(nbSamples);
		}
		CPUParams parsi_sender=null;
		CPUParams parsi_receiver=null;
		
		
		if((this.transSend) && (this.transReceive)){
			SequentialModule seq=(SequentialModule)zi.getModule(1); 
    		TableModule tab=(TableModule)seq.getModule(0);
    		parsi_sender=((CPUParams)tab.getModule(0)); //new CPUParams(nbSamples,nbDims);
			parsi_sender.setNbVecs(nbSamples);
			parsi_receiver=((CPUParams)tab.getModule(1)); //new CPUParams(nbSamples,nbDims);
			parsi_receiver.setNbVecs(nbSamples);
		}
		else{
			if(this.transSend){
				parsi_sender=((CPUParams)zi.getModule(1)); //new CPUParams(nbSamples,nbDims);
				parsi_sender.setNbVecs(nbSamples);
			}
			if(this.transReceive){
				parsi_receiver=((CPUParams)zi.getModule(1)); //new CPUParams(nbSamples,nbDims);
				parsi_receiver.setNbVecs(nbSamples);
			}
		}
		
		CPUParams parsk=((CPUParams)zk.getModule(0)); //new CPUParams(nbSamples,nbDims);
		parsk.setNbVecs(nbSamples);
		//CPUParams parsk=new CPUParams(nbSamples,nbDims);
		
		CPUMatrix mat=new CPUMatrix(nbSamples,1);
		CPUMatrix mat2=new CPUMatrix(nbSamples,1);
		//CPUMatrix mat3=new CPUMatrix(nbSamples,1);
		ArrayList<Double> w=new ArrayList<Double>();
		int i=0;
		CPUParams mi;
		
		for(String uk:hashConta.keySet()){
			HashMap<String,Double> pars=hashConta.get(uk);
			CPUParams mk=embeddings.get(uk);
			lastUsed.put(uk, mk);
			HashMap<String,Double> att=attracts.get(uk);
			HashMap<String,Integer> natt=nattracts.get(uk);
			
			if(att==null){
				att=new HashMap<String,Double>();
				natt=new HashMap<String,Integer>();
				attracts.put(uk, att);
				nattracts.put(uk,natt);
			}
			for(String ui:pars.keySet()){
				parsk.addParametersFrom(mk);
				if(dualPoints){
					mi=sender_modules.get(ui);
					lastUsed.put(ui+"_sender", mi);
				}
				else{
					mi=embeddings.get(ui);
					lastUsed.put(ui, mi);
				}
				parsi.addParametersFrom(mi);
				
				
				if(this.transSend){
					mi=transSender_modules.get(ui);
					parsi_sender.addParametersFrom(mi);
					lastUsed.put(ui+"_transSend", mi);
				}
				if(this.transReceive){
					mi=transReceiver_modules.get(uk);
					parsi_receiver.addParametersFrom(mi);
					lastUsed.put(uk+"_transReceiver", mi);
				}
				
				if(this.withDiagSenders){
					mi=diagSenders.get(ui);
					diagSenderi.addParametersFrom(mi);
					lastUsed.put(ui+"_diagsender", mi);
				}
				
				if(this.withDiagReceivers){
					mi=diagReceivers.get(uk);
					diagReceiveri.addParametersFrom(mi);
					lastUsed.put(uk+"_diagreceiver", mi);
				}
				mat.setValue(i,0,0.999999998);
				mat2.setValue(i,0,0.000000001);
				//mat3.setValue(i,0,hashConta.get(mk));
				w.add(pars.get(ui));
				i++;
				
				
				Double atti=att.get(ui);
				Integer natti=natt.get(ui);
				atti=(atti==null)?0:atti;
				natti=(natti==null)?0:natti;
				atti+=1.0;
				natti++;
				att.put(ui,atti);
				natt.put(ui,natti);
				
				HashMap<String,Double> att2=attracts.get(ui);
				HashMap<String,Integer> natt2=nattracts.get(ui);
				
				if(att2==null){
					att2=new HashMap<String,Double>();
					natt2=new HashMap<String,Integer>();
					attracts.put(ui, att2);
					nattracts.put(ui,natt2);
				}
				Double attk=att2.get(uk);
				Integer nattk=natt2.get(uk);
				attk=(attk==null)?0:attk;
				nattk=(nattk==null)?0:nattk;
				attk+=1.0;
				nattk++;
				att2.put(uk,attk);
				natt2.put(uk,nattk);
				
			}
			
		}
		for(String uk:hashNotConta.keySet()){
			HashMap<String,Double> pars=hashNotConta.get(uk);
			CPUParams mk=embeddings.get(uk);
			lastUsed.put(uk, mk);
			HashMap<String,Double> att=attracts.get(uk);
			HashMap<String,Integer> natt=nattracts.get(uk);
			
			if(att==null){
				att=new HashMap<String,Double>();
				natt=new HashMap<String,Integer>();
				attracts.put(uk, att);
				nattracts.put(uk,natt);
			}
			
			for(String ui:pars.keySet()){
				parsk.addParametersFrom(mk);
				if(dualPoints){
					mi=sender_modules.get(ui);
					lastUsed.put(ui+"_sender", mi);
				}
				else{
					mi=embeddings.get(ui);
					lastUsed.put(ui, mi);
				}
				parsi.addParametersFrom(mi);
			
				
				if(this.transSend){
					mi=transSender_modules.get(ui);
					parsi_sender.addParametersFrom(mi);
					lastUsed.put(ui+"_transSender", mi);
				}
				if(this.transReceive){
					mi=transReceiver_modules.get(uk);
					parsi_receiver.addParametersFrom(mi);
					lastUsed.put(uk+"_transReceiver", mi);
				}
				if(this.withDiagSenders){
					mi=diagSenders.get(ui);
					diagSenderi.addParametersFrom(mi);
					lastUsed.put(ui+"_diagsender", mi);
				}
				if(this.withDiagReceivers){
					mi=diagReceivers.get(uk);
					diagReceiveri.addParametersFrom(mi);
					lastUsed.put(uk+"_diagreceiver", mi);
				}
				mat.setValue(i,0,-0.999999998);
				mat2.setValue(i,0,0.999999999);
				
				//mat3.setValue(i,0,hashConta.get(mk));
				w.add(pars.get(ui));
				i++;
				Double atti=att.get(ui);
				Integer natti=natt.get(ui);
				atti=(atti==null)?0:atti;
				natti=(natti==null)?0:natti;
				atti-=1.0;
				natti++;
				att.put(ui,atti);
				natt.put(ui,natti);
				
				HashMap<String,Double> att2=attracts.get(ui);
				HashMap<String,Integer> natt2=nattracts.get(ui);
				
				if(att2==null){
					att2=new HashMap<String,Double>();
					natt2=new HashMap<String,Integer>();
					attracts.put(ui, att2);
					nattracts.put(ui,natt2);
				}
				Double attk=att2.get(uk);
				Integer nattk=natt2.get(uk);
				attk=(attk==null)?0:attk;
				nattk=(nattk==null)?0:nattk;
				attk-=1.0;
				nattk++;
				att2.put(uk,attk);
				natt2.put(uk,nattk);
			}
			
		}
		
		//System.out.println("pars "+parsi.getParamList());
		
		/*zi.setModule(0,parsi);
		if(this.transSend){
			zi.setModule(1,parsi_sender);
		}
		
		zk.setModule(0,parsk);
		if(withDiagSenders){
			diagSender.setModule(0, diagi);
		}*/
		if(withDiagContent || transSendContent){
			int nbStems=maxIdStem; //+((bias)?1:0);
			CPUSparseMatrix content=new CPUSparseMatrix(1,nbStems);
        
			TreeMap<Integer,Double> cont=pstruct.getDiffusion();
	         TreeMap<Integer,Double> ncont=new TreeMap<Integer,Double>();
	        if(boolContent){
	        	
	        	for(Integer k:cont.keySet()){
	        		ncont.put(k, 1.0);
	        	}
	        	
	        }
	        else{
	        	for(Integer k:cont.keySet()){
	        		ncont.put(k, (double)cont.get(k));
	        	}
	        }
	        content.setValues(ncont);
			/*if(bias){
        	content.setValue(0, nbStems-1, 1.0f);
        	}*/
			//System.out.println(content);
			this.currentInput.setMatrix(0, content);
			if(withDiagContent && transSendContent){
				this.currentInput.setMatrix(1, content);
				
			}
		} 
        
		//SequentialModule somme=new SequentialModule();
		//System.out.println("nb Input = "+simP.getNbInputMatrix());
		
		
		
		((CPUTimesVals)seqSims.getModule(1)).setVals(mat);
		((CPUAddVals)seqSims.getModule(2)).setVals(mat2);
		//}
		//somme.addModule(seqSims);
		
		/*somme.addModule(simP);
		CPUTimesVals tv=new CPUTimesVals(1,mat);
		somme.addModule(tv);
		CPUAddVals adv=new CPUAddVals(1,mat2);
		somme.addModule(adv);*/
		//somme.addModule(new CPULog(1));
		
        //System.out.println("content = "+content);
		//global.setModule(0, seqSims);
		//System.out.println("forwardSims");
		seqSims.forward(this.currentInput);
		//System.out.println("forwardSims ok");
		
		//System.out.println("seqSims : \n"+seqSims.getHierarchy(0));
		//System.out.println("seqSims : \n"+global.getModule(0).getHierarchy(0));
		
		av.setWeights(w, 1);
		//this.currentInput=new Tensor(0);
		global.setModule(3, new CPUTimesVals(1,-1.0f*proba));
		
        global.forward(this.currentInput);
       
		nbForwards++;
		//nbFromChangeLock++;
		lastLoss=getLossValue();
		/*if(lastLoss>1000000){
			throw new RuntimeException("stop");
		}*/
		sumLoss+=lastLoss;
		sumLossTot+=lastLoss;
		nbSum++;
		
		ecrireCgt=false;
		Matrix out=simP.getOutput().getMatrix(0);
		for(int q=0;q<out.getNumberOfRows();q++){
			double o=out.getValue(q, 0);
			if(mat.getValue(q, 0)>0){
				nbYes++;
				sumYes+=o;
				/*if(o<0.00000001){
					System.out.println(ui+","+uj+" => cascade "+listeCascades.get(q)+" "+o+" (Yes)");
					ecrireCgt=true;
				}*/
			}
			else{
				nbNot++;
				sumNot+=o;
				/*if(o>0.99999999){
					System.out.println(ui+","+uj+" => cascade "+listeCascades.get(q)+" "+o+" (No)");
					ecrireCgt=true;
				}*/
			}
			if(Double.isInfinite(getLossValue())){
				throw new RuntimeException(o+" infinite loss");
			}
			
		}
		
		
			
		/*if(nbForwards%longDiscard==0){
			discards=new HashSet<String>();
			ArrayList<String> vusers=new ArrayList<String>(users);
			Collections.shuffle(vusers);
			for(i=0;i<nbDiscard;i++){
				discards.add(vusers.get(i));
			}
		}*/
		
		
	}
	
	
	public void forward_L1_poids(){
		int nb=0;
		CPUParams parsi=((CPUParams)global.getModule(0)); 
		int nbv=nbMaxSamples;
		if(dualPoints){nbv*=2;}
		parsi.setNbVecs(nbv);
		lastUsed=new HashMap<String,Module>();
		
		while(nb<nbMaxSamples){
		
			int i=(int)(Math.random()*this.users.size());
			String ui=users.get(i);
			
			CPUParams mi=null;
			if(dualPoints){
				mi=sender_modules.get(ui);
				lastUsed.put(ui+"_sender", mi);
				parsi.addParametersFrom(mi);
			}
			
			mi=embeddings.get(ui);
			lastUsed.put(ui, mi);
			parsi.addParametersFrom(mi);
			
			nb++;
		}
		
		global.setModule(3, new CPUTimesVals(1,regul*1.0/nbMaxSamples));
		this.currentInput=new Tensor(0);
        global.forward(this.currentInput);
	}
	
	public void forward_L1(){
		int x=(int)(Math.random()*this.cascades_ids.size()); 
		int c=cascades_ids.get(x);
		//System.out.println("Cascade "+c);
		PropagationStruct pstruct=this.train_cascades.get(c);
		LinkedHashSet<String> users=pstruct.getPossibleUsers();
		ArrayList<String> lusers=new ArrayList<String>(users);
		int nbSamples=10;
		int nb=0;
		CPUParams parsi=((CPUParams)zi.getModule(0)); 
		parsi.setNbVecs(nbSamples);
		//CPUParams parsi=new CPUParams(nbSamples,nbDims);
		CPUParams diagSenderi=null;
		 lastUsed=new HashMap<String,Module>();
		if(withDiagSenders){
			int xi=0;
			if(withDiagReceivers){
				xi=1;
			}
			diagSenderi=((CPUParams)diagSenderReceiver.getModule(xi)); //new CPUParams(nbSamples,nbDims);
			diagSenderi.setNbVecs(nbSamples);
			
		}
		CPUParams diagReceiveri=null;
		
		if(withDiagReceivers){
				
			diagReceiveri=((CPUParams)diagSenderReceiver.getModule(0)); //new CPUParams(nbSamples,nbDims);
			diagReceiveri.setNbVecs(nbSamples);
		
		}
		CPUParams parsi_sender=null;
		CPUParams parsi_receiver=null;
		
		
		if((this.transSend) && (this.transReceive)){
			SequentialModule seq=(SequentialModule)zi.getModule(1); 
    		TableModule tab=(TableModule)seq.getModule(0);
    		parsi_sender=((CPUParams)tab.getModule(0)); //new CPUParams(nbSamples,nbDims);
			parsi_sender.setNbVecs(nbSamples);
			parsi_receiver=((CPUParams)tab.getModule(1)); //new CPUParams(nbSamples,nbDims);
			parsi_receiver.setNbVecs(nbSamples);
		}
		else{
			if(this.transSend){
				parsi_sender=((CPUParams)zi.getModule(1)); //new CPUParams(nbSamples,nbDims);
				parsi_sender.setNbVecs(nbSamples);
			}
			if(this.transReceive){
				parsi_receiver=((CPUParams)zi.getModule(1)); //new CPUParams(nbSamples,nbDims);
				parsi_receiver.setNbVecs(nbSamples);
			}
		}
		CPUParams parsk=((CPUParams)zk.getModule(0)); //new CPUParams(nbSamples,nbDims);
		parsk.setNbVecs(nbSamples);
		CPUMatrix matMult=new CPUMatrix(nbSamples,1);
		CPUMatrix matAdd=new CPUMatrix(nbSamples,1);
		while(nb<nbSamples){
		
			int i=(int)(Math.random()*users.size());
			String ui=lusers.get(i);
			int k=i; 
			while(k==i){
				k=(int)(Math.random()*users.size());
			}
			String uk=lusers.get(k);
			
			CPUParams mk=embeddings.get(uk);
			parsk.addParametersFrom(mk);
			lastUsed.put(uk, mk);
			CPUParams mi=null;
			if(dualPoints){
				mi=sender_modules.get(ui);
				lastUsed.put(ui+"_sender", mi);
			}
			else{
				mi=embeddings.get(ui);
				lastUsed.put(ui, mi);
			}
			parsi.addParametersFrom(mi);
				
			if(this.transSend){
					mi=transSender_modules.get(ui);
					parsi_sender.addParametersFrom(mi);
					lastUsed.put(ui+"_transSender", mi);
			}
			if(this.transReceive){
				mi=transReceiver_modules.get(uk);
				parsi_receiver.addParametersFrom(mi);
				lastUsed.put(uk+"_transReceiver", mi);
			}
			if(this.withDiagSenders){
					mi=diagSenders.get(ui);
					diagSenderi.addParametersFrom(mi);
					lastUsed.put(ui+"_diagsender", mi);
			}
			if(this.withDiagReceivers){
				mi=diagReceivers.get(uk);
				diagReceiveri.addParametersFrom(mi);
				lastUsed.put(uk+"_diagreceiver", mi);
			}
			matMult.setValue(nb,0,0.999999998);
			matAdd.setValue(nb,0,0.000000001);
			nb++;
		}
		double proba=1.0/nbSamples;
		if(withDiagContent || transSendContent){
			int nbStems=maxIdStem; //+((bias)?1:0);
			CPUSparseMatrix content=new CPUSparseMatrix(1,nbStems);
        
			TreeMap<Integer,Double> cont=pstruct.getDiffusion();
	         TreeMap<Integer,Double> ncont=new TreeMap<Integer,Double>();
	        if(boolContent){
	        	
	        	for(Integer k:cont.keySet()){
	        		ncont.put(k, 1.0);
	        	}
	        	
	        }
	        else{
	        	for(Integer k:cont.keySet()){
	        		ncont.put(k, (double)cont.get(k));
	        	}
	        }
	        content.setValues(ncont);
			/*if(bias){
        	content.setValue(0, nbStems-1, 1.0f);
        	}*/
			//System.out.println(content);
			this.currentInput.setMatrix(0, content);
			if(withDiagContent && transSendContent){
				this.currentInput.setMatrix(1, content);
				
			}
			//proba*=cascades_ids.size()*;
		}
		
		
		((CPUTimesVals)seqSims.getModule(1)).setVals(matMult);
		((CPUAddVals)seqSims.getModule(2)).setVals(matAdd);
		
		seqSims.forward(this.currentInput);
		
		av.setWeights(1);
		global.setModule(2, new CPUTimesVals(1,regul*proba));
		
        global.forward(this.currentInput);
        
        Matrix out=global.getOutput().getMatrix(0);
        sumReg+=out.getValue(0, 0);
        if(nbForwards%nbAffiche==0){ // && (listei.size()>0)){
			System.out.println(this.getName()+" Average Sim = "+String.format("%.10f", sumReg/(1.0*regul*nbForwards)));
		}
        
	}
	
	public void forward_cascadeMultiSource() {
		//System.out.println("emitters.size="+emitters.size());
		//System.out.println(zi_zj);
		nbF++;
		
		// choose cascade
		int x=(int)(Math.random()*this.cascades_ids.size()); 
		int c=cascades_ids.get(x);
		//System.out.println("Cascade "+c);
		
		PropagationStruct pstruct=this.train_cascades.get(c);
		LinkedHashSet<String> users=pstruct.getPossibleUsers();
		//TreeMap<Long,HashMap<String,Double>> initInfected=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
        if(discards.size()>0){
        	TreeMap<Long,HashMap<String,Double>> infectionsWithoutDiscards=new TreeMap<Long,HashMap<String,Double>>();
        	for(Long t:infections.keySet()){
        		HashMap<String,Double> us=infections.get(t);
        		HashMap<String,Double> nus=new HashMap<String,Double>();
        		for(String u:us.keySet()){
        			if(!discards.containsKey(u)){
        	        	
        				nus.put(u,us.get(u));
        			}
        		}
        		if(nus.size()>0){
        			infectionsWithoutDiscards.put(t, nus);
        		}
        	}
        	infections=infectionsWithoutDiscards;
        	Set<String> s=discards.keySet();
        	for(String u:s){
	        	Integer d=discards.get(u);
	        	d-=1;
	        	if(d<=0){
	        		discards.remove(u);
	        	}
	        	else{
	        		discards.put(u, d);
	        	}
	        }
        	if(infections.get(1l).size()==0){
        		forward_cascadeMultiSource();
        		return;
        	}
        }
        TreeMap<Long,ArrayList<String>> cumul=PropagationStruct.getListBeforeT(infections);
        ArrayList<String> contaminated = new ArrayList<String>(cumul.lastEntry().getValue());
        //ArrayList<String> initContaminated = new ArrayList<String>(cumul.get(this.nbInitSteps).keySet()) ;
        
        HashSet<String> nc=new HashSet<String>(users);
        for(String u:contaminated){
        	nc.remove(u);
        }
        ArrayList<String> notContaminated = new ArrayList<String>(nc);
        for(String u:cumul.get(1l)){
            contaminated.remove(u);
        }
        
        //if((contaminated.size()==0) || (notContaminated.size()==0)){
        //	forward_cascadeMultiSource();
        //	return;
		//}
        int nfirst=infections.get(1l).keySet().size();
        if((users.size()-nfirst)==0){
        	System.out.println("Wrong Cascade ");
        	forward_cascadeMultiSource();
        	return;
        }
        
        //HashMap<String,HashMap<String,Double>> hashConta=new HashMap<String,HashMap<String,Double>>();
        
        //HashMap<String,HashMap<String,Double>> hash=new HashMap<String,HashMap<String,Double>>(); 
        
        HashMap<String,Long> times=pstruct.getInfectionTimes();
       
        lastUsed=new HashMap<String,Module>();
        //ArrayList<Long> infTimes=new ArrayList<Long>(infections.keySet());
		//HashSet<String> vus=new HashSet<String>();
		//boolean stop=false;
		int nbSamples=0;
		
		double proba=1.0;
		
		//System.out.println(nfirst);
		double pc=(1.0f*contaminated.size())/(users.size()-nfirst);
		
		double rf=1.0/(1.0+0.001*nbF);
		nbF++;
		rf=0.0;
		//double rf=1.0-(1.0/(1.0+Math.exp(1-0.0000001*nbForwards)));
		//double rf=1.0-(1.0/(1.0+Math.exp(10-0.00001*nbF)));
		double pconta=pc+(1.0-pc)*rf; //pcConta;
		//double plage=1.0+((users.size()-nfirst)*rf);
		//double den=plage*contaminated.size()+notContaminated.size();
		//double pconta=(plage*contaminated.size())/den;
		double a=Math.random();
		String uk="";
		ArrayList<String> before;
		Long maxTi=0l;
		//HashMap<String,Double> pars;
		boolean infect=false;
		//Double  nh;
		if(a<pconta){
			infect=true;
			x=(int)(Math.random()*contaminated.size());
	        uk=contaminated.get(x);
	        Long tk=times.get(uk);
	        if(tk==1){
	        	throw new RuntimeException("Should not have time equal to 1");
	        }
	        if(tk==null){
	        	throw new RuntimeException("Should not have null time");
	        }
	        //Long ti=tk;
	        
	        maxTi=tk-1;
	        if(maxTi>pstruct.getNbInitSteps()){
        		maxTi=(long)pstruct.getNbInitSteps();
	        }
	        
		}
		else{
			x=(int)(Math.random()*notContaminated.size());
	        uk=notContaminated.get(x);
	        maxTi=cumul.lastKey();
	        if(maxTi>pstruct.getNbInitSteps()){
        		maxTi=(long)pstruct.getNbInitSteps();
	        }
		}
		if(!cumul.containsKey(maxTi)){
			long max=0;
			for(Long tt:cumul.keySet()){
				if(tt>maxTi){
					break;
				}
				max=tt;
			}
			maxTi=max;
		}
		//System.out.println(maxTi);
          
	    before=cumul.get(maxTi);
	    //int beforeSize=before.size();
	    nbSamples=before.size();
	    /*if(before.size()>this.nbMaxSamples){
	    	nbSamples=nbMaxSamples;
	    	Collections.shuffle(before);
	    	before=new ArrayList<String>(before.subList(0, nbMaxSamples));
	    }*/
		//nbSamples=hashConta.size()+hashNotConta.size();
	    //System.out.println(before.size());
	    CPUParams parsi=((CPUParams)zi.getModule(0)); //
		parsi.setNbVecs(nbSamples);
		//CPUParams parsi=new CPUParams(nbSamples,nbDims);
		CPUParams diagSenderi=null;
		if(withDiagSenders){
			int xi=0;
			if(withDiagReceivers){
				xi=1;
			}
			diagSenderi=((CPUParams)diagSenderReceiver.getModule(xi)); //new CPUParams(nbSamples,nbDims);
			diagSenderi.setNbVecs(nbSamples);
			
		}
		CPUParams diagReceiveri=null;
		
		if(withDiagReceivers){
				
			diagReceiveri=((CPUParams)diagSenderReceiver.getModule(0)); //new CPUParams(nbSamples,nbDims);
			diagReceiveri.setNbVecs(nbSamples);
		
		}
		CPUParams parsi_sender=null;
		CPUParams parsi_receiver=null;
		
		
		if((this.transSend) && (this.transReceive)){
			SequentialModule seq=(SequentialModule)zi.getModule(1); 
    		TableModule tab=(TableModule)seq.getModule(0);
    		parsi_sender=((CPUParams)tab.getModule(0)); //new CPUParams(nbSamples,nbDims);
			parsi_sender.setNbVecs(nbSamples);
			parsi_receiver=((CPUParams)tab.getModule(1)); //new CPUParams(nbSamples,nbDims);
			parsi_receiver.setNbVecs(nbSamples);
		}
		else{
			if(this.transSend){
				parsi_sender=((CPUParams)zi.getModule(1)); //new CPUParams(nbSamples,nbDims);
				parsi_sender.setNbVecs(nbSamples);
			}
			if(this.transReceive){
				parsi_receiver=((CPUParams)zi.getModule(1)); //new CPUParams(nbSamples,nbDims);
				parsi_receiver.setNbVecs(nbSamples);
			}
		}
		CPUParams parsk=((CPUParams)zk.getModule(0)); //new CPUParams(nbSamples,nbDims);
		parsk.setNbVecs(nbSamples);
		CPUMatrix matMult=new CPUMatrix(before.size(),1);
		CPUMatrix matAdd=new CPUMatrix(before.size(),1);
		int nui=0;
		
		for(String ui:before){
			 //emitters.add(ui);
			 CPUParams mk=embeddings.get(uk);
			 parsk.addParametersFrom(mk);
			 lastUsed.put(uk, mk);
			 CPUParams mi=null;
			 if(dualPoints){
					mi=sender_modules.get(ui);
					lastUsed.put(ui+"_sender", mi);
			 }
			 else{
					mi=embeddings.get(ui);
					lastUsed.put(ui, mi);
			 }
			 parsi.addParametersFrom(mi);
			 	
			  if(this.transSend){
					mi=transSender_modules.get(ui);
					parsi_sender.addParametersFrom(mi);
					lastUsed.put(ui+"_transSender", mi);
			  }
			  if(this.transReceive){
					mi=transReceiver_modules.get(uk);
					parsi_receiver.addParametersFrom(mi);
					lastUsed.put(uk+"_transReceiver", mi);
			  }
			  if(this.withDiagSenders){
					mi=diagSenders.get(ui);
					diagSenderi.addParametersFrom(mi);
					lastUsed.put(ui+"_diagsender", mi);
			  }
			  if(this.withDiagReceivers){
					mi=diagReceivers.get(uk);
					diagReceiveri.addParametersFrom(mi);
					lastUsed.put(uk+"_diagreceiver", mi);
			  }
			  if(infect){
				  matMult.setValue(nui,0,0.999999998);
				  matAdd.setValue(nui,0,0.000000001);
			  }
			  else{
				  matMult.setValue(nui,0,-0.999999998);
				  matAdd.setValue(nui,0,0.999999999);
			  }
			  
			  if(computeAttracts){
				  HashMap<String,Double> att=attracts.get(uk);
				  HashMap<String,Integer> natt=nattracts.get(uk);
				
				  if(att==null){
					att=new HashMap<String,Double>();
					natt=new HashMap<String,Integer>();
					attracts.put(uk, att);
					nattracts.put(uk,natt);
				  }
				  Double atti=att.get(ui);
				  Integer natti=natt.get(ui);
				  atti=(atti==null)?0:atti;
				  natti=(natti==null)?0:natti;
				  if(infect){
					  atti+=1.0;
				  }
				  else{
					  atti-=1.0;
				  }
				  natti++;
				  att.put(ui,atti);
				  natt.put(ui,natti);
				  
				  
				  
				  HashMap<String,Double> att2=attracts.get(ui);
				  HashMap<String,Integer> natt2=nattracts.get(ui);
					
				  if(att2==null){
						att2=new HashMap<String,Double>();
						natt2=new HashMap<String,Integer>();
						attracts.put(ui, att2);
						nattracts.put(ui,natt2);
				  }
				  Double attk=att2.get(uk);
				  Integer nattk=natt2.get(uk);
			      attk=(attk==null)?0:attk;
				  nattk=(nattk==null)?0:nattk;
				  if(infect){
					  attk+=1.0;
				  }
				  else{
					  attk-=1.0;
				  }
				  nattk++;
				  att2.put(uk,attk);
				  natt2.put(uk,nattk);
			  }  
			  
			  nui++;
	    }
		//System.out.println(before.size());
		
		/*zi.setModule(0,parsi);
		if(transSend){
			zi.setModule(1,parsi);
			
		}
		zk.setModule(0,parsk);
		if(withDiagSenders){
			diagSender.setModule(0, diagi);
		}*/
		//System.out.println("forwardSims");
		if(withDiagContent || transSendContent){
			int nbStems=maxIdStem; //+((bias)?1:0);
			CPUSparseMatrix content=new CPUSparseMatrix(1,nbStems);
        
			TreeMap<Integer,Double> cont=pstruct.getDiffusion();
	         TreeMap<Integer,Double> ncont=new TreeMap<Integer,Double>();
	        if(boolContent){
	        	
	        	for(Integer k:cont.keySet()){
	        		ncont.put(k, 1.0);
	        	}
	        	
	        }
	        else{
	        	for(Integer k:cont.keySet()){
	        		ncont.put(k, (double)cont.get(k));
	        	}
	        }
	        content.setValues(ncont);
			/*if(bias){
        	content.setValue(0, nbStems-1, 1.0f);
        	}*/
			//System.out.println(content);
			this.currentInput.setMatrix(0, content);
			if(withDiagContent && transSendContent){
				this.currentInput.setMatrix(1, content);
				
			}
		}
		
		
		/*if(seqSims==null){
			seqSims=new SequentialModule();
			seqSims.addModule(simP);
			CPUTimesVals tv=new CPUTimesVals(1,matMult);
			seqSims.addModule(tv);
			CPUAddVals adv=new CPUAddVals(1,matAdd);
			seqSims.addModule(adv);
		}
		else{*/
			((CPUTimesVals)seqSims.getModule(1)).setVals(matMult);
			((CPUAddVals)seqSims.getModule(2)).setVals(matAdd);
		//}
		
		seqSims.forward(currentInput);
		/*System.out.println("forwardSims Ok");
		Matrix outx=simP.getOutput().getMatrix(0);
		for(int q=0;q<outx.getNumberOfRows();q++){
			System.out.println(outx.getValue(q, 0));	
		}*/
		double z2=zStat*zStat;
		CPUMatrix mat=new CPUMatrix(before.size(),1);
		CPUMatrix mat2=new CPUMatrix(before.size(),1);
		if(infect){
			Matrix sims=seqSims.getOutput().getMatrix(0);
			
			
			double pk=1.0;
			double[] vals=new double[before.size()];
			for(int i=0;i<before.size();i++){
				//System.out.println("sim="+sims.getValue(i, 0));
				
				double p=sims.getValue(i, 0);
				if(rescale_mode==1){
					// Wilson Method for taking the center of a given confidence interval
                	// zStat = 2.576 => 99% confidence interval for kvw
                	// zStat = 1.960 => 95% confidence interval for kvw
                	// zStat = 1.645 => 90% confidence interval for kvw
					int n=nbUsedCouple.get(before.get(i)).get(uk);
					
					double den=(2.0*(n+z2));
					double num=((2.0*n*p)+z2);
					//double num2=(-1.0-Math.sqrt(z2*(z2-2.0-(1.0/n)+4.0*p*(n*(1.0-p)+1.0))));
					double num2=0.0; //-Math.sqrt(z2*(z2+(4.0*n*p*(1.0-p))));
					p=(num+num2)/den;
				}
				
				vals[i]=p;
            	pk*=1.0-p;
			}
			pk=1.0-pk;
			if(pk<0.000000001){
				pk=0.000000001;
			}
			for(int i=0;i<before.size();i++){
				double v=vals[i]/pk;
				mat.setValue(i, 0, v);
				mat2.setValue(i, 0, 1.0-v);
			}
			
			
			
		}
		
		TableModule table=new TableModule();
		
		
		if(infect){
			SequentialModule seq1=new SequentialModule();
			table.addModule(seq1);
			seq1.addModule(seqBis);
			seq1.addModule(new CPULog(1));
			seq1.addModule(new CPUTimesVals(1,mat));
			SequentialModule seq2=new SequentialModule();
			table.addModule(seq2);
			seq2.addModule(seqTer);
			seq2.addModule(new CPULog(1));
			seq2.addModule(new CPUTimesVals(1,mat2));
			global.setModule(0, table);
			global.setModule(1, new CPUSum(1,2));
		}
		else{
			global.setModule(0,seqBis);
			global.setModule(1,new CPULog(1));
		}
		av.setWeights(2);
		//this.currentInput=new Tensor(0);
		proba=1.0;
		if(unbiased){
			proba=((1.0f*(users.size()-nfirst))/nbProbas)*cascades_ids.size();
			/*if(beforeSize>nbSamples){
					proba*=(beforeSize*1.0/nbSamples);
			}*/
			//proba=(1.0*(users.size()-nfirst)*nbSamples*cascades_ids.size())/(nbCouplesInLoss*1.0); 
			//System.out.println("proba="+proba);
		}
		
		if(rescale_mode==2){
			ArrayList<Double> weights=new ArrayList<Double>();
			for(int i=0;i<nbSamples;i++){
				double n=Math.pow(nbUsedCouple.get(before.get(i)).get(uk),zStat);
				//System.out.println(n);
				weights.add(n*1.0);
			}
			av.setWeights(weights,2);
		}
		
		if(rescale_mode==3){
			ArrayList<Double> weights=new ArrayList<Double>();
			for(int i=0;i<nbSamples;i++){
				double n=nbUsedCouple.get(before.get(i)).get(uk);
				n=Math.tanh(zStat*(n));
				weights.add(n*1.0);
			}
			av.setWeights(weights,2);
		}
		
		if(rescale_mode==4){
			ArrayList<Double> weights=new ArrayList<Double>();
			double w=1.0;
			if(infect){
				w=zStat;
			}
			for(int i=0;i<nbSamples;i++){
				weights.add(w);
			}
			av.setWeights(weights,2);
		}
		
		global.setModule(3, new CPUTimesVals(1,-1.0f*proba));
		
        global.forward(this.currentInput);
        //System.out.println("forward Ok");
		nbForwards++;
		//nbFromChangeLock++;
		lastLoss=getLossValue();
		/*if(lastLoss>1000000){
			throw new RuntimeException("stop");
		}*/
		sumLoss+=lastLoss;
		sumLossTot+=lastLoss;
		nbSum++;
		
		ecrireCgt=false;
		Matrix out=simP.getOutput().getMatrix(0);
		double po=1.0;
		for(int q=0;q<out.getNumberOfRows();q++){
			double o=out.getValue(q, 0);
			po*=(1.0-o);	
		}
		po=1.0-po;
		if(infect){
			nbYes++;
			sumYes+=po;
		}
		else{
			nbNot++;
			sumNot+=po;
		}
		
		
			
		/*if(nbForwards%longDiscard==0){
			discards=new HashSet<String>();
			ArrayList<String> vusers=new ArrayList<String>(users);
			Collections.shuffle(vusers);
			for(int j=0;j<nbDiscard;j++){
				discards.add(vusers.get(j));
			}
		}*/
		
		
	}
	
	
	public String getName(){
		return "MLPProj_sim-"+sim+"_unbiased"+unbiased+"_"+nbDims+((multiSource)?" multiSource ":"")+((dualPoints)?"dual ":"")+((withDiag)?"diag ":"")+((withDiagContent)?"diagContent ":"")+((transSend)?"transSend ":"")+((transReceive)?"transReceive ":"")+((transSendContent)?"transSendContent ":"")+((withDiagSenders)?"withDiagSenders ":"")+((withDiagReceivers)?"withDiagReceivers ":"")+((regul>0)?"REGUL="+regul:"")+((this.rescale_mode>0)?("-rescale_mode"+rescale_mode+((this.rescale_mode>=1)?("-zStat"+zStat):"")):"");
		
	}
    
       
    
    
    public void backward()
	{
		global.backward_updateGradient(this.currentInput);
	}
    
    public double getLastLine(){
    	return lastLine;
    }
    
    public void updateParams(double line){
    	lastLine=line;
    	/*for(Module mod:lastUsed.values()){
    		if(!mod.isLocked()){
    			mod.updateParameters(line);
    			mod.paramsChanged();
    		}
    	}*/
    	for(String mod:lastUsed.keySet()){
    		Module m=lastUsed.get(mod);
    		if(!m.isLocked()){
    			/*if(nbFromChangeLock%nbDiagUnLocked==1){
        			System.out.println("mod before: "+m.getParamList().toString());
    			}*/
    			//if((mod.contains("_sender")) && (m.getParamList().getParams().get(0).getDirection()!=0.0)){
    			//	System.out.println(mod+":"+m.getParamList().toString());
    			//}
        		m.updateParameters(line);
    			m.paramsChanged();
    			
    			/*if(nbFromChangeLock%nbDiagUnLocked==1){
        			System.out.println("mod after: "+m.getParamList());
    			}*/
    		}
    		else{
    			if(nbFromChangeLock%nbDiagUnLocked==1){
    				System.out.println(diagLocked+" "+mod+" locked!");
    			}
    		}
    	}
    	if(withDiag){
    		if(!diag.isLocked()){
        		diag.updateParameters(line);
        		diag.paramsChanged();
    		}
		}
    	
		simP.updateParameters(line);
	
		if(withDiagContent){
			//System.out.println("DiagContent");
			//System.out.println(diagContent.getParamList());
			if(!diagContent.isLocked()){	
				diagContent.updateParameters(line);
			}
			
		}
		
		if(transSendContent){
			if(!transContent.isLocked()){
				transContent.updateParameters(line);
			}
		}
		if((nbDiagLocked>0) && (diagLocked)){
			if(nbFromChangeLock%nbDiagUnLocked==0){
				System.out.println("lock embeddings! "+line);
				diagLocked=false;
				nbFromChangeLock=0;
				for(Module mod:this.embeddings.values()){
					mod.lockParams();
				}
				for(Module mod:this.sender_modules.values()){
					mod.unlockParams();
					//System.out.println(mod.getParamList().toString());
				}
				/*for(Module mod:this.transSender_modules.values()){
					mod.lockParams();
				}
				for(Module mod:this.transReceiver_modules.values()){
					mod.lockParams();
				}
				for(Module mod:this.diagSenders.values()){
					mod.unlockParams();
				}
				for(Module mod:this.diagReceivers.values()){
					mod.unlockParams();
				}*/
			}
			nbFromChangeLock++;
		}
		else if((nbDiagLocked>0) && (!diagLocked)){
			if(nbFromChangeLock%nbDiagUnLocked==0){
				System.out.println("unlock embeddings ! "+line);
				
				diagLocked=true;
				nbFromChangeLock=0;
				for(Module mod:this.embeddings.values()){
					mod.unlockParams();
				}
				/*for(Module mod:this.transSender_modules.values()){
					mod.lockParams();
				}*/
				for(Module mod:this.sender_modules.values()){
					mod.unlockParams();
				}
				/*
				for(Module mod:this.transReceiver_modules.values()){
					mod.unlockParams();
				}
				for(Module mod:this.diagSenders.values()){
					mod.lockParams();
				}
				for(Module mod:this.diagReceivers.values()){
					mod.lockParams();
				}*/
			}
			nbFromChangeLock++;
		}
    }
    
    /*public void updateParams(double line){
    	if(ecrireCgt){
    		simP.forward(new Tensor(0));
    		Matrix out=simP.getOutput().getMatrix(0);
    		double o=out.getValue(0, 0);
    		System.out.println("Avant modif => "+o);
    	}
    	int nb=0;
		if((nbContentLocked<=0) || (contentLocked)){
			
			for(Module mod:lastUsed.values()){
				//System.out.println(mod);
				//System.out.println(mod.getParamList());
				if(ecrireCgt){
					for(Parameter para:mod.getParamList().getParams()){
						System.out.println(para.getVal()+" => "+para.getGradient()+", "+para.getDirection());
					}
				}
				mod.updateParameters(line);
				mod.paramsChanged();
				nb++;
			}
			
			if(withDiag){
				//System.out.println("Diag");
				//System.out.println(diag.getParamList());
				diag.updateParameters(line);
				
			}
			simP.updateParameters(line);
		}
		//System.out.println(nb+" mods changed");
		if(!contentLocked){
				
			if(withDiagContent){
				//System.out.println("DiagContent");
				//System.out.println(diagContent.getParamList());
				diagContent.updateParameters(line);
				
			}
			
			if(transSendContent){
				transContent.updateParameters(line);
			}
		}
		
	
		if(nbForwards%1000==0){
			
			if(withDiagContent){
				System.out.println("diag out = "+diagContent.getOutput());
			}
	        if(withDiag){
	        	//System.out.println("diag = "+seqDiag.getOutput());
	        }
		}
		//if(nbForwards%10000==0){
		if((nbContentLocked>0) && (contentLocked)){
			if(nbFromChangeLock%nbContentLocked==0){
				diag.lockParams();
				for(Module mod:embeddings.values()){
					mod.lockParams();
				}
				if(dualPoints){
					for(Module mod:sender_modules.values()){
						mod.lockParams();
					}
				}
				if(transSend){
					for(Module mod:transSender_modules.values()){
						mod.lockParams();
					}
				}
				if(withDiagContent){
					diagContent.unlockParams();
				}
				if(transSendContent){
					transContent.unlockParams();
				}
				System.out.println("unlock content");
				nbFromChangeLock=0;
				contentLocked=false;
			}
		}
		else if((nbContentLocked>0) && (!contentLocked)){
			if(nbFromChangeLock%nbContentUnLocked==0){
				diag.unlockParams();
				for(Module mod:embeddings.values()){
					mod.unlockParams();
				}
				if(dualPoints){
					for(Module mod:sender_modules.values()){
						mod.unlockParams();
					}
				}
				if(transSend){
					for(Module mod:transSender_modules.values()){
						mod.unlockParams();
					}
				}
				if(transReceive){
					for(Module mod:transReceiver_modules.values()){
						mod.unlockParams();
					}
				}
				if(withDiagContent){
					diagContent.lockParams();
				}
				if(transSendContent){
					transContent.lockParams();
				}
				System.out.println("lock content");
				nbFromChangeLock=0;
				contentLocked=true;
			}
		}
		
		if(ecrireCgt){
			simP.forward(new Tensor(0));
			Matrix out2=simP.getOutput().getMatrix(0);
			double o2=out2.getValue(0, 0);
			System.out.println("Apres modif => "+o2);
		}
	}*/
	
	
    @Override
	public Parameters getUsedParams(){
		Parameters p=new Parameters();
		for(Module mod:lastUsed.values()){
			if(!mod.isLocked()){
				p.addSubParamList(mod.getParamList());
			}
		}
		if(withDiag){
			if(!diag.isLocked()){
				p.addSubParamList(diag.getParamList());
			}
		}
		Parameters ps=simP.getParamList();
		if(ps.getParams().size()>0){
			p.addSubParamList(ps);
		}
		if(this.withDiagContent){
			p.addSubParamList(diagContent.getParamList());
		}
		
		if(transSendContent){
			p.addSubParamList(transContent.getParamList());
		}
		return p;
    }
		
	/*@Override
	public Parameters getUsedParams(){
		Parameters p=new Parameters();
		if((nbDiagLocked<=0) || (diagLocked)){
			
			for(Module mod:lastUsed.values()){
				p.addSubParamList(mod.getParamList());
			}
			if(withDiag){
				p.addSubParamList(diag.getParamList());
			}
			Parameters ps=simP.getParamList();
			if(ps.getParams().size()>0){
				p.addSubParamList(ps);
			}
			
		}
		if((!diagLocked)){
			
			if(withDiagContent){
				p.addSubParamList(diagContent.getParamList());
			}
			if(transSendContent){
				p.addSubParamList(transContent.getParamList());
			}
		}
		
		return p;
	}*/

   
    /*public boolean function(Link link) {
       
        //System.out.println(weights.get(source).get(target));
        try {
        	//System.out.print(link);
            return Math.random() <= link.getVal() ;
        } catch (NullPointerException e) {
            return false ;
        }
       
    }*/
    
   
    /**
	 * Returns the loss estimated by averaging all losses computed from the begining of the optimization.
	 * 
	 */
	public double getGlobalLoss(){
		if(nbForwards==0){
			return 0.0;
		}
		return sumLossTot/nbForwards;
	}
	
	/**
	 * Returns the loss estimated by averaging all losses computed from the last call to the reinitLoss function (or the begining of the optimization if no reinit).
	 * 
	 */
	public double getLoss(){
		if(nbSum==0){
			return 0.0;
		}
		return sumLoss/nbSum;
	}
   
	/**
	 * Allows to reinit the estimated loss. 
	 */
	public void reinitLoss(){
		sumLoss=0.0;
		nbSum=0;
	}
   
	/**
	 * Returns a map containing the embeddings of the model (indexed by the name of the point).
	 *
	 */
	public HashMap<String,double[]> getEmbeddings(){
		HashMap<String,double[]> ret=new HashMap<String,double[]>();
		for(String p:embeddings.keySet()){
			ret.put(p, ((CPUMatrix)embeddings.get(p).getOutput().getMatrix(0)).getValues());
		}
		return ret;
	}
	
	/**
	 * Launch the optimization of the model for nb steps.
	 * @param nb number of optimization steps to perform.
	 */
	public void optimizeNext(Optimizer optimizer, int nb){
		if(!prepared){
			throw new RuntimeException("Model not prepared to be optimized");
		}
		optimizer.optimize(this,0.0000000001,nb,nb,false);
		loaded=true;
		
	}
	
	
	
	/**
	 * Returns the tendency of attractivity / repulsion effect points have on the referer point whose name in given as parameter.
	 * @param referer point
	 * @return map containing for each point its attractivity level on the referer : values between -1 (high level of repulsion) and 1 (high level of attractivity).
	 */
	public HashMap<String,Double> getAttractivities(String referer){
		computeAttracts=true;
		HashMap<String,Double> ret=new HashMap<String,Double>();
		HashMap<String,Double> att=attracts.get(referer);
		HashMap<String,Integer> natt=nattracts.get(referer);
		if(att!=null){
			for(String u:att.keySet()){
				ret.put(u,att.get(u)/natt.get(u));
			}
		}
		
		return ret;
	}
	
	/**
	 * Allows to ask to the optimizer to ignore the concerning point during a given number of learning steps.
	 * @param name
	 * @param nb
	 */
	public void setDiscard(String name, int nb){
		discards.put(name, nb);
	}
	
	/**
	 * Returns the table of discards points
	 */
	public HashMap<String,Integer> getDiscards(){
		return discards;
	}
   
    public static void main(String[] args){
		
		
		HashMap<String,String> hargs=ArgsParser.parseArgs(args);
		String modelFile=(hargs.containsKey("mF"))?hargs.get("mF"):"";
		
		
		
		String db=(hargs.containsKey("db"))?hargs.get("db"):"digg";
		String cascades=(hargs.containsKey("c"))?hargs.get("c"):"cascades_1";
		int nbDims=(hargs.containsKey("nD"))?Integer.parseInt(hargs.get("nD")):2;
		
		
		
		boolean withDiag=(hargs.containsKey("wD"))?Boolean.valueOf(hargs.get("wD")):false;
		boolean transSend=(hargs.containsKey("tS"))?Boolean.valueOf(hargs.get("tS")):false;
		boolean transReceive=(hargs.containsKey("tR"))?Boolean.valueOf(hargs.get("tR")):false;
		boolean withDiagContent=(hargs.containsKey("wDC"))?Boolean.valueOf(hargs.get("wDC")):false;
		boolean transSendContent=(hargs.containsKey("tSC"))?Boolean.valueOf(hargs.get("tSC")):false;
		boolean withDiagSenders=(hargs.containsKey("wDS"))?Boolean.valueOf(hargs.get("wDS")):false;
		boolean withDiagReceivers=(hargs.containsKey("wDR"))?Boolean.valueOf(hargs.get("wDR")):false;
		boolean dualPoints=(hargs.containsKey("dP"))?Boolean.valueOf(hargs.get("dP")):false;
		
		
		boolean unbiased=(hargs.containsKey("u"))?Boolean.valueOf(hargs.get("u")):true;
		boolean logisticDiag=(hargs.containsKey("lD"))?Boolean.valueOf(hargs.get("lD")):false;
		boolean boolContent=(hargs.containsKey("bC"))?Boolean.valueOf(hargs.get("bC")):false;
		boolean multiSource=(hargs.containsKey("mS"))?Boolean.valueOf(hargs.get("mS")):false;
		double line=(hargs.containsKey("l"))?Double.valueOf(hargs.get("l")):0.02;
		double decFactor=(hargs.containsKey("dF"))?Double.valueOf(hargs.get("dF")):0.9999999;
		int sim=(hargs.containsKey("s"))?Integer.parseInt(hargs.get("s")):5;
		double regul=(hargs.containsKey("r"))?Double.valueOf(hargs.get("r")):0.0;
		String ratioInits=(hargs.containsKey("rI"))?hargs.get("rI"):"1.0";
		String nbMaxInits=(hargs.containsKey("nbI"))?hargs.get("nbI"):"-1";
		int rescale_mode=(hargs.containsKey("rS"))?Integer.valueOf(hargs.get("rS")):0;
		double zStat=(hargs.containsKey("z"))?Double.valueOf(hargs.get("z")):1.64;
		
		double minProbaInfer=(hargs.containsKey("mPI"))?Double.valueOf(hargs.get("mPI")):0.0;
		double parInf=(hargs.containsKey("pI"))?Double.valueOf(hargs.get("pI")):-1.0f;
		double parSup=(hargs.containsKey("pS"))?Double.valueOf(hargs.get("pS")):1.0f;
		double diagInf=(hargs.containsKey("dI"))?Double.valueOf(hargs.get("dI")):0.0001f;
		double diagSup=(hargs.containsKey("dS"))?Double.valueOf(hargs.get("dS")):10.0f;
		int nbF=(hargs.containsKey("nF"))?Integer.valueOf(hargs.get("nF")):100000000;
		int nbE=(hargs.containsKey("nE"))?Integer.valueOf(hargs.get("nE")):1000000;
		int nbA=(hargs.containsKey("nA"))?Integer.valueOf(hargs.get("nA")):1000;
		String nbC=(hargs.containsKey("nbC"))?hargs.get("nbC"):"-1";
		String start=(hargs.containsKey("start"))?hargs.get("start"):"1";
		
		MLPproj mlp=null;
		
		if(modelFile.length()==0){
			mlp=new MLPproj(nbDims,2000,boolContent,logisticDiag,dualPoints,withDiag,withDiagContent,transSend,transReceive,transSendContent,withDiagSenders,withDiagReceivers,sim,multiSource,regul); 
		}
		else{
			mlp=new MLPproj(modelFile); 
			mlp.load();
			/*mlp.sender_modules=mlp.diagSenders;
			mlp.diagSenders=null;
			mlp.sim=10;
			mlp.withDiagSenders=false;
			mlp.dualPoints=true;
			mlp.prepareLearning(db, cascades, 1, 1);
			mlp.save();
			return;*/
		}
		//long maxT=(hargs.containsKey("maxT"))?Long.valueOf(hargs.get("maxT")):100;
		//maxT=(hargs.containsKey("mT"))?Long.valueOf(hargs.get("mT")):100;
		
		mlp.rescale_mode=rescale_mode;
		mlp.zStat=zStat;
		
		boolean changed=false;
		if((hargs.containsKey("sim")) || hargs.containsKey("s")){
			if(sim!=mlp.sim){
				mlp.setSim(sim);
				changed=true;
			}
		}
		if(hargs.containsKey("mPI")){
			mlp.minProbaInfer=minProbaInfer;
		}
		if((hargs.containsKey("dualPoints")) || hargs.containsKey("dP")){
			mlp.dualPoints=dualPoints;
			changed=true;
		}
		if((hargs.containsKey("r"))){
			mlp.regul=regul;
			changed=true;
		}
		if((hargs.containsKey("withDiag")) || hargs.containsKey("wD")){
			mlp.withDiag=withDiag;
			changed=true;
		}
		if((hargs.containsKey("withDiagContent")) || hargs.containsKey("wDC")){
			mlp.withDiagContent=withDiagContent;
			changed=true;
		}
		if((hargs.containsKey("transSend")) || hargs.containsKey("tS")){
			mlp.transSend=transSend;
			changed=true;
		}
		if((hargs.containsKey("transReceive")) || hargs.containsKey("tR")){
			mlp.transReceive=transReceive;
			changed=true;
		}
		if((hargs.containsKey("transSendContent")) || hargs.containsKey("tSC")){
			mlp.transSendContent=transSendContent;
			changed=true;
		}
		if((hargs.containsKey("withDiagSenders")) || hargs.containsKey("wDS")){
			mlp.withDiagSenders=withDiagSenders;
			changed=true;
		}
		if((hargs.containsKey("withDiagReceivers")) || hargs.containsKey("wDR")){
			mlp.withDiagReceivers=withDiagReceivers;
			changed=true;
		}
		if((hargs.containsKey("parInf")) || hargs.containsKey("pI")){
			mlp.parInf=parInf;
			
		}
		if((hargs.containsKey("parSup")) || hargs.containsKey("pS")){
			mlp.parSup=parSup;
			
		}
		if((hargs.containsKey("diagInf")) || hargs.containsKey("dI")){
			mlp.diagInf=diagInf;
			
		}
		if((hargs.containsKey("diagSup")) || hargs.containsKey("dS")){
			mlp.diagSup=diagSup;
			
		}
		if((hargs.containsKey("unbiased")) || hargs.containsKey("u")){
			mlp.unbiased=unbiased;
			
		}
		if((hargs.containsKey("multiSource")) || hargs.containsKey("mS")){
			mlp.multiSource=multiSource;
			
		}
		mlp.nbEstimations=nbE;
		mlp.freqSave=nbE;
		mlp.nbAffiche=nbA;
		
		
		if((hargs.containsKey("nbDims")) || hargs.containsKey("nD")){
			int nbd=mlp.nbDims;
			
			if(nbd!=nbDims){
				mlp.changeNbDims(nbDims);
				mlp.setSim(mlp.sim);
				changed=true;
			}
		}
		
		if(changed){
			mlp.buildSim();
		}
		System.out.println("parSup="+mlp.parSup);
		mlp.majParBounds();
		Env.setVerbose(0);
		//MLPdiffusion mlp=new MLPdiffusion("psauv/MLPDiagContent2_Dims-100_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0_iInInit-true_senderReceiver-false/last");
		/*
		*/
		Optimizer opt=Optimizer.getDescent(DescentDirection.getGradientDirection(), LineSearch.getFactorLine(line,decFactor));
		//opt.optimize(mlp);
		//System.out.println("unbiased="+unbiased);
		//mlpDouble.load();
		mlp.nbF=nbF;
		
	    //mlp.prepareLearning(db, cascades, 1, 1,1000);
		
		PropagationStructLoader ploader=new MultiSetsPropagationStructLoader(db,cascades,(long)1,ratioInits,nbMaxInits,start,nbC);
		
		String testDb=(hargs.containsKey("tdb"))?hargs.get("tdb"):"";
		String testCascades=(hargs.containsKey("tc"))?hargs.get("tc"):"";
		String testUsers=(hargs.containsKey("tu"))?hargs.get("tu"):"";
		
		if(testDb.length()>0){
			
			mlp.testConfig=new EvalPropagationModelConfigx(testDb,testCascades,testUsers);
			mlp.freqTest=(hargs.containsKey("nT"))?Integer.valueOf(hargs.get("nT")):1000000;
			mlp.testConfig.addModel(mlp,1); //,100,10,2,0.001),1);
			mlp.maxT=100;
			mlp.nbSimul=10;
			mlp.minProbaInfer=0.01;
			mlp.inferMode=2;
		}
		
		if(hargs.containsKey("lS")){
			boolean ls=Boolean.valueOf(hargs.get("lS"));
			if(ls){
				
				System.out.println("Lock Sim");
				mlp.simPLocked=true;
			}
			
		}
		
		mlp.learn(ploader, opt); //, mlp.iInInit); //,mlp.unbiased); //,"propagationModels/MLPDiagContent_Dims-200_step-1_nbInit-1_db-diggPruned_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0/MLPDiagContent_18.09.2013_14.19.12_20.09.2013_8.28.55");
		//mlp.optimizeNext(opt, 10);
		//System.out.println(mlp.getAttractivities(mlp.attracts.keySet().iterator().next()));
		
		mlp.save();
		 
		// sh run.sh propagationModels.MLPproj -db=digg -c=cascades_1 -nD=10 -nA=1000 -nE=100000 -r=0.0 -dP=true -l=0.1 -dF=0.99999999 -mS=true -nbC=1000 -s=13 -pI=0.01 -pS=1000
	}
    
    
    /**
     * @param args
     */
    /*public static void main(String[] args) {
    	try{
			if(args.length==0){
				args=new String[10];
				args[0]="test";
				args[1]="test";
				args[2]="10";
				args[3]="true";
				args[4]="false";
				args[5]="false";
				args[6]="true";
				args[7]="false";
				args[8]="false";
				args[9]="1";
				args[10]="true";
				
				args[11]="0.002";
				args[12]="0.999999";
				
			}
			MLPnaiveProj2 mlp;
			//
			try{
				//ystem.out.println(args[2]);
				int nbDims=Integer.parseInt(args[2]);
				//System.out.println(nbDims);
				mlp=new MLPnaiveProj2(100,nbDims,2000,false,false,Boolean.valueOf(args[3]),Boolean.valueOf(args[4]),Boolean.valueOf(args[5]),Boolean.valueOf(args[6]),Boolean.valueOf(args[7]),Boolean.valueOf(args[8]),Integer.valueOf(args[9]),Boolean.valueOf(args[10])); 
				//System.out.println(nbDims);	
			}
			catch(NumberFormatException e){
				mlp=new MLPnaiveProj2(args[2]); 
				mlpDouble.load();
				if(args.length>3){
					mlpDouble.withDiag=Boolean.valueOf(args[3]);
					mlpDouble.withDiagContent=Boolean.valueOf(args[4]);
					mlpDouble.sender_receiver=Boolean.valueOf(args[5]);
					mlpDouble.transSendContent=Boolean.valueOf(args[6]);
					mlpDouble.withDiagSenders=Boolean.valueOf(args[7]);
					mlpDouble.unbiased=Boolean.valueOf(args[9]);
				}
				//mlpDouble.model_file="";
			}
			Env.setVerbose(0);
			//MLPdiffusion mlp=new MLPdiffusion("psauv/MLPDiagContent2_Dims-100_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0_iInInit-true_senderReceiver-false/last");
			
			Optimizer opt=Optimizer.getDescent(DescentDirection.getAverageGradientDirection(), LineSearch.getFactorLine(Double.valueOf(args[11]),Double.valueOf(args[12])));
			//opt.optimize(mlp);
			
			//mlpDouble.load();
			mlpDouble.learn(args[0], args[1], (long)1, 1, opt); //,"propagationModels/MLPDiagContent_Dims-200_step-1_nbInit-1_db-diggPruned_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0/MLPDiagContent_18.09.2013_14.19.12_20.09.2013_8.28.55");
			
			mlpDouble.save();
		}
		catch(Exception e){
			e.printStackTrace();
		}
    }*/
        
   
    

}
class EvalPropagationModelConfigx extends EvalPropagationModelConfig{
	private String model; //cascadesTrain;
	private int nbDims;
	LinkedHashMap<PropagationModel,Integer> mods=new LinkedHashMap<PropagationModel,Integer>();
	public EvalPropagationModelConfigx(String db, String cascades,String users){
		super();
		pars.put("db",db);
		pars.put("cascadesCol", cascades);
		pars.put("allUsers", users);
		pars.put("nbMaxInits", "1");
		pars.put("ratioInits", "0.3");
		pars.put("nbCascades", "100");
	}
	
	public void addModel(PropagationModel mod,Integer nb){
		mods.put(mod,nb);
	}
	public void reinitModels(){
		mods=new LinkedHashMap<PropagationModel,Integer>();
	}
	
	public LinkedHashMap<PropagationModel,Integer> getModels(){
		return mods;
	}
	public EvalMeasureList getMeasures(){
		ArrayList<EvalMeasure> ev=new ArrayList<EvalMeasure>();
		ev.add(new NbContaminated());
		ev.add(new FMeasure(true,1));
		if (allUsers==null){
			loadAllUsers();
		}
		ev.add(new MAP(allUsers,true));
		ev.add(new MeanRank(allUsers,true));
		ev.add(new LogLikelihood(allUsers));
		EvalMeasureList mes=new EvalMeasureList(ev);
		return(mes);
	}
}
