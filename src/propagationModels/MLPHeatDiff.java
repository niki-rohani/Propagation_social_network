package propagationModels;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import core.Structure;
import core.User;
import experiments.EvalPropagationModel;
import mlp.CPUAddVals;
import mlp.CPUAddVecs;
import mlp.CPUAverageRows;
import mlp.CPUErf;
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
import mlp.EmbeddingsModel;
import mlp.Matrix;
import mlp.Module;
import mlp.Optimizer;
import mlp.Parameter;
import mlp.Parameters;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.Tensor;

public class MLPHeatDiff extends MLP implements EmbeddingsModel {
	
	private int nbDims;
	//private boolean withDiag=false;
	private boolean dualPoint=false;
	private double threshold=0.1;
	protected HashMap<String,CPUParams> receivers;
	protected HashMap<String,CPUParams> senders;
	protected HashMap<String,CPUParams> alphas;
	protected HashMap<String,CPUParams> betas;
	
	private long maxT=-1;
	//private transient CPUParams diag;
	//private transient SequentialModule seqDiag;
	private transient TableModule zi;
	private transient TableModule zj;
	private TableModule alpha;
	private TableModule beta;
	private HashMap<String,HashMap<String,Double>> attracts;
	private HashMap<String,HashMap<String,Integer>> nattracts;
	private double sumLoss=0.0f;
	private int nbForwards=0;
	private int nbSum=0;
	private double sumYes=0.0;
	private double sumNot=0.0;
	private int nbYes=0;
	private int nbNot=0;
	private int nbEstimations=1000000;
	private int nbAffiche=1000000;
	private int freqSave=1000000;
	private Tensor currentInput;
	private double lastLoss=0.0f;
	private double sumLossTot=0;
	private HashMap<String,Module> lastUsed;
	double best=Double.NaN;
	SequentialModule seqSims;
	private Heat simP;
	private int mode=0; //0:CumulHeatLT, 1:ProbaCumulHeatLT, >1 ProbaInstantHeatLT
	
	double lastLine=0;
	double parSup=1.0f;
	double parInf=-1.0f;
	double diagSup=10.0f;
	double diagInf=0.0001f;
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MLPHeatDiff(int nbDims,boolean dualPoint){
		this(nbDims,dualPoint,0);
	}
	
	public MLPHeatDiff(int nbDims,boolean dualPoint, int mode){
		this("");
		this.mode=mode;
		//System.out.println("here");
		//this.maxT=maxT;
		this.nbDims=nbDims;
		//this.maxIdStem=maxIdStem;
		//this.withDiag=withDiag;
		//this.withDiagContent=withDiagContent;
		this.dualPoint=dualPoint;
		if(mode>1){
			simP=new HeatSim(nbDims);
		}
		else{
			simP=new CumulHeatSim(nbDims);
		}
		params.allocateNewParamsFor(simP,1.0);
		
	}
	
	public MLPHeatDiff(String model_file, long maxT){
		super(model_file);
		this.nbDims=1;
		senders=new HashMap<String,CPUParams>();
		receivers=new HashMap<String,CPUParams>();
		
		this.maxT=maxT;
	}
    public MLPHeatDiff(String model_file){
		this(model_file,0);
		
	}
    
    public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(this.receivers.keySet());
	}
	public int getContentNbDims(){
		return 0;
	}
	
    
    public void majParBounds(){
		for(CPUParams mod:receivers.values()){
			for(Parameter p:mod.getParamList().getParams()){
				p.setLowerBound(parInf);
				p.setUpperBound(parSup);
			}
		}
		if(dualPoint){
			for(CPUParams mod:this.senders.values()){
				for(Parameter p:mod.getParamList().getParams()){
					p.setLowerBound(parInf);
					p.setUpperBound(parSup);
				}
			}
		}
		/*if(withDiag){
			CPUParams mod=diag;
			if(mod!=null){
				for(Parameter p:mod.getParamList().getParams()){
					p.setLowerBound(diagInf);
					p.setUpperBound(diagSup);
				}
			}
		}*/
		
	}
	
    public void buildSim(){
		int nbd=nbDims;
        
		/*if(withDiag){
			if((!loaded) || (diag==null)){
				diag=new CPUParams(1,nbd);
				params.allocateNewParamsFor(diag,1.0f,diagInf,diagSup);
			}
		}*/
		
		/*seqDiag=new SequentialModule();
		TableModule tabDiag=new TableModule();
		seqDiag.addModule(tabDiag);
			
		if(withDiag){
			
			tabDiag.addModule(diag);
		}*/
		
		
		
		ArrayList<Double> weights=new ArrayList<Double>();
		weights.add(1.0);
		weights.add(-1.0);
		
		
		zi=new TableModule();
		zi.setName("zi");
			
		zi.addModule(new CPUParams(1,nbDims));   
		
		
		zj=new TableModule();
		zj.addModule(new CPUParams(1,nbDims));   
		
		
		
		
		Module sender=zi;
		
		Module receiver=zj;
		
		
		sender.setName("Sender");
		receiver.setName("Receiver");
		simP.setPoint1(sender);
		
		simP.setPoint2(receiver);
	}
    
    public String toString(){
		String sm=model_file.replaceAll("/", "_");
		return("MLPHeatDiff_maxT="+maxT);
	}
    
    
    @Override
    public int inferSimulation(Structure struct){
    	//TODO
    	return 0;
    }
    
    @Override
    public int infer(Structure struct){
		
		
		if(!loaded){
			System.out.println("Load Model...");
			load();
		}
		System.out.println("Inference...");
		
		PropagationStruct pstruct = (PropagationStruct)struct ;
        		
		Tensor tensor=new Tensor(0);
	    this.currentInput=tensor;
		
		TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        
       
        //pstruct.setInfections(infections) ;
        
		
        return 0;
	}
	
    
public void prepareLearning(PropagationStructLoader ploader){
    	
    	
		if(model_file.length()!=0){
			if(!loaded){load();}
		}
		else{
			buildSim();
		}
		
		
		String format = "dd.MM.yyyy_H.mm.ss";
		
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		//if(model_name.length()==0){
			model_name="propagationModels/MLPProj_Dims-"+nbDims+"_step-"+ploader.getStep()+"_ratioInits-"+ploader.getRatioInits()+"_nbMaxInits-"+ploader.getNbMaxInits()+"_db-"+ploader.getDb()+"_cascadesCol-"+ploader.getCollection()+"_start"+ploader.getStart()+"_nbC"+ploader.getNbC()+"/dP-"+dualPoint+"/"+formater.format(date);
		//}
		
		System.out.println("learn : "+model_name);
			
		super.prepareLearning(ploader);
		
		System.out.println("learn : "+model_name);
		
		
		attracts=new HashMap<String,HashMap<String,Double>>();
		nattracts=new HashMap<String,HashMap<String,Integer>>();
		//discards=new HashMap<String,Integer>();
		currentInput=new Tensor(0);
		
		
		for(String user:users){
			if(!receivers.containsKey(user)){
				//System.out.println("Creation Module for user "+user);
				CPUParams mod=new CPUParams(1,nbDims);
				mod.setName(user);
				params.allocateNewParamsFor(mod,-1.0,1.0,parInf,parSup);
				receivers.put(user,mod);
			}
			if(dualPoint){
				if(!senders.containsKey(user)){
					//System.out.println("Creation Module for user "+user);
					CPUParams mod=new CPUParams(1,nbDims);
					mod.setName(user+"_sender");
					params.allocateNewParamsFor(mod,-1.0,1.0,parInf,parSup); 
					senders.put(user,mod);
				}
			}
			if(!alphas.containsKey(user)){
				//System.out.println("Creation Module for user "+user);
				CPUParams mod=new CPUParams(1,1);
				mod.setName(user+"_alpha");
				params.allocateNewParamsFor(mod,0.0,1.0,0.0000000001,diagSup);
				alphas.put(user,mod);
			}
			if(!betas.containsKey(user)){
				//System.out.println("Creation Module for user "+user);
				CPUParams mod=new CPUParams(1,1);
				mod.setName(user+"_beta");
				if(mode>1){
					params.allocateNewParamsFor(mod,-1.0,1.0,parInf,parSup);
				}
				else{
					params.allocateNewParamsFor(mod,0.0,1.0,0.0,parSup);
				}
				betas.put(user,mod);
			}
		}
		
		
		seqSims=new SequentialModule();
		seqSims.addModule(simP);
		//CPUTimesVals tv=new CPUTimesVals(1,new CPUMatrix(1,1));
		seqSims.addModule(new CPUAverageRows(1,2));
		alpha=new TableModule();
		beta=new TableModule();
		if(mode>1){
			alpha.addModule(seqSims);
			alpha.addModule(new CPUParams(1,1));
			
			
			SequentialModule seq=new SequentialModule();
			seq.addModule(alpha);
			seq.addModule(new CPUTermByTerm(1));
			
			beta.addModule(seq);
			beta.addModule(new CPUParams(1,1));
			
			
			global=new SequentialModule();
			global.addModule(beta);
			global.addModule(new CPUSum(1,2));
			global.addModule(new CPULogistic(1));
			global.addModule(new CPUTimesVals(1,0.99999999));
			global.addModule(new CPUAddVals(1,0.00000001));
			global.addModule(new CPULog(1));
			global.addModule(new CPUTimesVals(1,-1.0f));
		}
		else{
			if(mode==0){
				alpha.addModule(seqSims);
				alpha.addModule(new CPUParams(1,1));
				
				ArrayList<Double> w=new ArrayList<Double>();
				w.add(1.0); w.add(-1.0);
				global=new SequentialModule();
				global.addModule(alpha);
				global.addModule(new CPUSum(1,2,w));
				global.addModule(new CPUTimesVals(1,1.0));
				global.addModule(new CPUHingeLoss(1,1.0));
				
			}
			else{
				alpha.addModule(seqSims);
				alpha.addModule(new CPUParams(1,1));
				
				
				SequentialModule aseq=new SequentialModule();
				aseq.addModule(alpha);
				ArrayList<Double> w=new ArrayList<Double>();
				w.add(1.0); w.add(-1.0);
				aseq.addModule(new CPUSum(1,2,w));
				beta.addModule(new CPUParams(1,1));
				SequentialModule bseq=new SequentialModule();
				bseq.addModule(beta);
				bseq.addModule(new CPUPower(1,-1));
				bseq.addModule(new CPUTimesVals(1,1.0/Math.sqrt(2.0)));
				TableModule tab=new TableModule();
				tab.addModule(aseq);
				tab.addModule(bseq);
				global=new SequentialModule();
				global.addModule(tab);
				global.addModule(new CPUTermByTerm(1));
				global.addModule(new CPUErf(1));
				global.addModule(new CPUTimesVals(1,-0.5)); // 0.5 if not infected
				global.addModule(new CPUAddVals(1,-0.5));
				
				
			}
			
		}
		
		
	}
    
    
    public void configHeatModuleForUser(HashMap<String,Long> contaminated, String user, long t){
    	CPUParams parsi=((CPUParams)zi.getModule(0)); //
		parsi.setNbVecs(contaminated.size());
		CPUParams parsj=((CPUParams)zj.getModule(0)); 
		parsj.setNbVecs(contaminated.size());
		CPUMatrix mat=new CPUMatrix(contaminated.size(),1);
		CPUMatrix mat2=new CPUMatrix(contaminated.size(),1);
		
		int i=0;
		CPUParams mj=receivers.get(user);
		lastUsed.put(user, mj);
		for(String ui:contaminated.keySet()){
			mat.setValue(i, 0, 4.0*(t-contaminated.get(ui)));
			mat2.setValue(i, 0, 1.0/(Math.pow(Math.sqrt(4*Math.PI*(t-contaminated.get(ui))),nbDims)));
			parsj.addParametersFrom(mj);
			if(this.dualPoint){
				CPUParams mi=senders.get(ui);
				parsi.addParametersFrom(mi);
				lastUsed.put(ui+"_sender", mi);
			}
			else{
				CPUParams mi=receivers.get(ui);
				parsi.addParametersFrom(mi);
				lastUsed.put(ui, mi);
			}
		}
		simP.setXVals(mat,mat2);
		
    }
    
    public void configCumulHeatModuleForUser(HashMap<String,Long> contaminated, String user, long t){
    	//TODO
    	CPUParams parsi=((CPUParams)zi.getModule(0)); //
		parsi.setNbVecs(contaminated.size());
		CPUParams parsj=((CPUParams)zj.getModule(0)); 
		parsj.setNbVecs(contaminated.size());
		CPUMatrix mat=new CPUMatrix(contaminated.size(),1);
		CPUMatrix mat2=new CPUMatrix(contaminated.size(),1);
		
		int i=0;
		CPUParams mj=receivers.get(user);
		lastUsed.put(user, mj);
		for(String ui:contaminated.keySet()){
			mat.setValue(i, 0, 4.0*(t-contaminated.get(ui)));
			mat2.setValue(i, 0, 1.0/(Math.pow(Math.sqrt(4*Math.PI*(t-contaminated.get(ui))),nbDims)));
			parsj.addParametersFrom(mj);
			if(this.dualPoint){
				CPUParams mi=senders.get(ui);
				parsi.addParametersFrom(mi);
				lastUsed.put(ui+"_sender", mi);
			}
			else{
				CPUParams mi=receivers.get(ui);
				parsi.addParametersFrom(mi);
				lastUsed.put(ui, mi);
			}
		}
		simP.setXVals(mat,mat2);
		
    }
    
	@Override
	public void backward() {
		// TODO Auto-generated method stub

	}

	@Override
	public void forward() {
	

		if((nbForwards%nbAffiche==0) && (nbForwards!=0)){ // && (global!=regulTerm)){ // && (listei.size()>0)){
			//double rYes=(1.0*sumYes/nbYes);
			//double rNot=(1.0*sumNot/nbNot);
			System.out.println(this.getName()+" Average Loss = "+String.format("%.5f", sumLossTot/(1.0*nbForwards))+", "+String.format("%.5f", sumLoss/(1.0*nbSum))+", "+String.format("%.5f", lastLoss)+" nbSums="+nbSum+" nbForwards="+nbForwards);//+" "+String.format("%.5f",rYes)+" "+String.format("%.5f",rNot)+" "+(String.format("%.5f",rYes/rNot)));
			
		}
		
		
		if((nbForwards%freqSave==0)&& (nbForwards!=0)){ // && (global!=regulTerm)){
				System.out.println("save "+nbForwards);
				save();
				sumYes=0.0;
				sumNot=0.0;
				nbYes=0;
				nbNot=0;
		}
		
		
		
		if(nbForwards%nbEstimations==0){
			sumLoss=0;
			nbSum=0;
			//System.gc();
			System.out.println("reinit sum");
		}
		
		lastUsed=new HashMap<String,Module>();
    	
		forward_sample();
		
	}
	
	public void forward_sampleLT() {
		//System.out.println("emitters.size="+emitters.size());
		//System.out.println(zi_zj);
		// choose cascade
		int x=(int)(Math.random()*this.cascades_ids.size()); 
		int c=cascades_ids.get(x);
		//System.out.println("Cascade "+c);
		long t=(long)(Math.random()*maxT)+2;
		PropagationStruct pstruct=this.train_cascades.get(c);
		LinkedHashSet<String> users=pstruct.getPossibleUsers();
		TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
	        
        TreeMap<Long,ArrayList<String>> cumul=PropagationStruct.getListBeforeT(infections);
        
        HashSet<String> nc=new HashSet<String>(users);
        for(String u:cumul.get(1l)){
            nc.remove(u);
        }
        
        if(nc.size()==0){
        	System.out.println("Wrong Cascade ");
        	forward_sampleLT();
        	return;
        }
        ArrayList<String> candidats=new ArrayList<String>(nc);
        Collections.shuffle(candidats);
        x=(int)(Math.random()*candidats.size()); 
        String v=candidats.get(x); 
        HashMap<String,Long> times=pstruct.getInfectionTimes();
        Long tv=times.get(v);
        tv=(tv==null)?(maxT+2):tv;
        boolean infect=false;
        long maxTi=0l;
        if(t>=tv) infect=true;
        maxTi=t-1;
        
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
        
        
        
        
       
		ArrayList<String> before;
		before=cumul.get(maxTi);
		HashMap<String,Long> conta=new HashMap<String,Long>();
		for(String u:before){
			conta.put(u, times.get(u));
		}
		configCumulHeatModuleForUser(conta, v, t);
		
		double vi=((infect)?1.0:-1.0);
		
		if(mode==0){
			alpha.setModule(1, alphas.get(v));
			CPUTimesVals prod=(CPUTimesVals)global.getModule(2);
			prod.setVals(vi);
		}
		else{
			alpha.setModule(1, alphas.get(v));
			beta.setModule(0, betas.get(v));
			CPUTimesVals prod=(CPUTimesVals)global.getModule(3);
			prod.setVals(vi*(-0.5));
		}
		
		
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
		
		
		/*Matrix out=simP.getOutput().getMatrix(0);
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
		}*/
		
		
			
		/*if(nbForwards%longDiscard==0){
			discards=new HashSet<String>();
			ArrayList<String> vusers=new ArrayList<String>(users);
			Collections.shuffle(vusers);
			for(int j=0;j<nbDiscard;j++){
				discards.add(vusers.get(j));
			}
		}*/
		
		
	}	
	
	public void forward_sample() {
		//System.out.println("emitters.size="+emitters.size());
		//System.out.println(zi_zj);
		// choose cascade
		int x=(int)(Math.random()*this.cascades_ids.size()); 
		int c=cascades_ids.get(x);
		//System.out.println("Cascade "+c);
		long t=(long)(Math.random()*maxT)+2;
		PropagationStruct pstruct=this.train_cascades.get(c);
		LinkedHashSet<String> users=pstruct.getPossibleUsers();
		//TreeMap<Long,HashMap<String,Double>> initInfected=pstruct.getInitContaminated();
		//HashMap<String,Long> times=pstruct.getInfectionTimes();
		
        TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
        
        TreeMap<Long,ArrayList<String>> cumul=PropagationStruct.getListBeforeT(infections);
        
        HashSet<String> nc=new HashSet<String>(users);
        for(String u:cumul.get(1l)){
            nc.remove(u);
        }
        
        if(nc.size()==0){
        	System.out.println("Wrong Cascade ");
        	forward_sample();
        	return;
        }
        ArrayList<String> candidats=new ArrayList<String>(nc);
        Collections.shuffle(candidats);
        x=(int)(Math.random()*candidats.size()); 
        String v=candidats.get(x); 
        HashMap<String,Long> times=pstruct.getInfectionTimes();
        Long tv=times.get(v);
        tv=(tv==null)?(maxT+2):tv;
        boolean infect=false;
        long maxTi=0l;
        if(t>=tv){
        	infect=true;
        	maxTi=tv-1;
        	t=tv;
 	        if(maxTi>pstruct.getNbInitSteps()){
         		maxTi=(long)pstruct.getNbInitSteps();
 	        }
        }
        else{
        	infect=false;
        	
        	maxTi=t-1;
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
        
        
        
        
       
		ArrayList<String> before;
		before=cumul.get(maxTi);
		HashMap<String,Long> conta=new HashMap<String,Long>();
		for(String u:before){
			conta.put(u, times.get(u));
		}
		configHeatModuleForUser(conta, v, t);
		
		alpha.setModule(1, alphas.get(v));
		beta.setModule(1, betas.get(v));
		if(infect){
			CPUTimesVals prod=(CPUTimesVals)global.getModule(3);
			CPUAddVals add=(CPUAddVals)global.getModule(4);
			prod.setVals(0.99999999);
			add.setVals(0.00000001);
		}
		else{
			CPUTimesVals prod=(CPUTimesVals)global.getModule(3);
			CPUAddVals add=(CPUAddVals)global.getModule(4);
			prod.setVals(-0.99999999);
			add.setVals(0.99999999);
		}
		
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
		
		
		/*Matrix out=simP.getOutput().getMatrix(0);
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
		}*/
		
		
			
		/*if(nbForwards%longDiscard==0){
			discards=new HashSet<String>();
			ArrayList<String> vusers=new ArrayList<String>(users);
			Collections.shuffle(vusers);
			for(int j=0;j<nbDiscard;j++){
				discards.add(vusers.get(j));
			}
		}*/
		
		
	}

	@Override
	public void load() {
		// TODO Auto-generated method stub

	}

	@Override
	public void save(){ // throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public HashMap<String, Double> getAttractivities(String referer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<String, Integer> getDiscards() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<String, double[]> getEmbeddings() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getGlobalLoss() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getLoss() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public HashMap<String, Double> getSims(String referer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void optimizeNext(Optimizer optimizer, int nb) {
		// TODO Auto-generated method stub

	}
	
	public double getLastLine(){
    	return lastLine;
    }

	@Override
	public void reinitLoss() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDiscard(String name, int nb) {
		// TODO Auto-generated method stub

	}

	

}

abstract class Heat extends MLPsimFromPoints{
	public Heat(Integer nbd){
		super(nbd);
	}
	public abstract void setXVals(CPUMatrix v,CPUMatrix w);
}

/**
 * sim(z1,z2)=xbis * exp(-||z1-z2||^2 / x)
 * 
 */
class HeatSim extends Heat{
	
	CPUTimesVals x;
	CPUTimesVals xbis;
	public HeatSim(Integer nbd){
		super(nbd);
		ArrayList<Double> w=new ArrayList<Double>();
		w.add(1.0); w.add(-1.0); 
		addModule(new CPUSum(nbd,2,w));
		addModule(new CPUL2Norm(nbd));
		x=new CPUTimesVals(1);
		addModule(x);
		addModule(new CPUTimesVals(1,-1.0));
		addModule(new CPUExp(1));
		xbis=new CPUTimesVals(1);
		addModule(xbis);
		
	}
	
	public void destroy(){
		super.destroy();
		
		
	}
	
	
	
	public void setXVals(CPUMatrix v,CPUMatrix w){
		if(sharedForward){
			throw new RuntimeException("Please do not call setXVals on a shared forward module");
		}
		x.setVals(v);
		xbis.setVals(w);
		for(Module mod:getListeners()){
			((HeatSim) mod).x.setVals(v);
			((HeatSim) mod).xbis.setVals(w);
		}
	}
	public void clearListeners(){
		super.clearListeners();
		
		
	}
	public int getNbParams(){
		return 0;
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		
		
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		HeatSim ret=new HeatSim(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.modules.clear();
		
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		ret.x=(CPUTimesVals)ret.getModule(3);
		ret.xbis=(CPUTimesVals)ret.getModule(6);
		return(ret);
	}
	
}


class CumulHeatSim extends Heat{
	
	CPUTimesVals time;
	CPUTimesVals moinsinv4time;
	//CPUMatrix time;
	public CumulHeatSim(Integer nbd){
		super(nbd);
		if((nbd<4) || (nbd%2!=0)){
			throw new RuntimeException("Nb Dims incorrect");
		}
		moinsinv4time=new CPUTimesVals(1);
		int m=(nbd-4)/2;
		ArrayList<Double> w=new ArrayList<Double>();
		w.add(1.0); w.add(-1.0); 
		SequentialModule dist=new SequentialModule();
		dist.addListener(points);
		dist.addModule(new CPUSum(nbd,2,w));
		dist.addModule(new CPUL2Norm(nbd));
		modules.clear();
		TableModule tab1=new TableModule();
		SequentialModule s1=new SequentialModule();
		s1.addModule(dist);
		s1.addModule(new CPUPower(1,-(m/2.0)));
		s1.addModule(new CPUTimesVals(1,(1.0/4.0)*Math.pow(Math.PI,-(nbd/2.0))));
		tab1.addModule(s1);
		SequentialModule s2=new SequentialModule();
		s1.addModule(dist.forwardSharedModule());
		
		//long f=factorial(m);
		CPUAddVals add=new CPUAddVals(1,1);
		TableModule tab=new TableModule();
		long x=1;
		long p=m;
		for(int i=1;i<=m;i++){
			x*=p;
			p--;
			SequentialModule seqi=new SequentialModule();
			tab.addModule(seqi);
			
		}
		
		
		/*x=new CPUTimesVals(1);
		addModule(x);
		addModule(new CPUTimesVals(1,-1.0));
		addModule(new CPUExp(1));
		xbis=new CPUTimesVals(1);
		addModule(xbis);
		*/
	}
	
	/*public static long factorial(int n) {
        long fact = 1; // this  will be the result
        for (int i = 1; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }*/
	
	public void destroy(){
		super.destroy();
		
		
	}
	
	
	
	public void setXVals(CPUMatrix v,CPUMatrix w){
		if(sharedForward){
			throw new RuntimeException("Please do not call setXVals on a shared forward module");
		}
		//x.setVals(v);
		//xbis.setVals(w);
		for(Module mod:getListeners()){
			((HeatSim) mod).x.setVals(v);
			((HeatSim) mod).xbis.setVals(w);
		}
	}
	public void clearListeners(){
		super.clearListeners();
		
		
	}
	public int getNbParams(){
		return 0;
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		
		
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		CumulHeatSim ret=new CumulHeatSim(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}
