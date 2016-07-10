package propagationModels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Map.Entry;

import utils.ArgsParser;
import utils.CopyFiles;
import core.Structure;
import core.User;
import experiments.EvalPropagationModel;
import mlp.CPUAddVals;
import mlp.CPUAddVecs;
import mlp.CPUAverageCols;
import mlp.CPUAverageRows;
import mlp.CPULog;
import mlp.CPULogistic;
import mlp.CPUMatrix;
import mlp.CPUParams;
import mlp.CPUPower;
import mlp.CPUSparseLinear;
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

public class MLPembedProba  extends MLP {
	private static final long serialVersionUID = 1L;
	protected int nbDims;
	protected HashMap<String,CPUParams> embeddings;
	protected HashMap<String,CPUParams> sender_modules;
	private MLPsimFromPoints simP;
	private TableModule zi;
	private TableModule zj;
	private CPUMatrix refs;
	
	protected int sim;
	protected boolean ignoreCouplesNotInTrain=false;
	protected int inferMode=0; 
	protected long maxT;
	protected int nbSimul;
	protected double minProbaInfer;
	protected ProbabilisticTransmissionModel ref;
	protected double parInf=-Double.MAX_VALUE;
	protected double parSup=Double.MAX_VALUE;
	
	private int nbForwards=0;
	private double loss=0.0;
	private double globalLoss=0.0;
	private int nbSums=0;
	private double best=Double.NaN;
	private double lastLoss=0.0;
	private int nbAffiche=1000;
	private int nbEstimations=1000;
	private int nbSave=1000;
	private int nbSamples=1000;
	private HashMap<String,Module> lastUsed;
	private Tensor currentInput;
	private double lastLine=0.0;
	//private double sumSim=0.0;
	
	public MLPembedProba(ProbabilisticTransmissionModel ref,int nbDims, int sim){
		this("");
		
		this.nbDims=nbDims;
		this.sim=sim;
		setSim(sim);
		this.ref=ref;
	}
	
	public MLPembedProba(String model_file, long maxT, int nbSimul,int inferMode,double minProbaInfer){
		this(model_file,maxT,nbSimul,inferMode,minProbaInfer,false);
	}
	
	public MLPembedProba(String model_file){
			this(model_file,0,1,0,0);
			
	}
	public MLPembedProba(String model_file, long maxT, int nbSimul,int inferMode,double minProbaInfer,boolean ignoreCouplesNotInTrain){
		super(model_file);
		this.ignoreCouplesNotInTrain=ignoreCouplesNotInTrain;
		this.inferMode=inferMode;
		this.nbDims=1;
		
		sender_modules=new HashMap<String,CPUParams>();
		embeddings=new HashMap<String,CPUParams>();
		
		
		this.maxT=maxT;
		this.nbSimul=nbSimul;
		this.minProbaInfer=minProbaInfer;
	}
	
	public void setMinProbaInfer(double p){
    	this.minProbaInfer=p;
    }
    public double getMinProbaInfer(){
    	return this.minProbaInfer;
    }
    public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(this.embeddings.keySet());
	}
    
   
	
	public void majParBounds(){
		for(CPUParams mod:embeddings.values()){
			for(Parameter p:mod.getParamList().getParams()){
				p.setLowerBound(parInf);
				p.setUpperBound(parSup);
			}
		}
		
		for(CPUParams mod:sender_modules.values()){
			for(Parameter p:mod.getParamList().getParams()){
				p.setLowerBound(parInf);
				p.setUpperBound(parSup);
			}
		}
		
		
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
		/*if(sim==3){
			simP=new P3Double(nbDims);
		}
		if(sim==4){
			simP=new P4Double(nbDims);
			params.allocateNewParamsFor(simP,1.0);
			//simP.p
		}*/
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
	}
	
	public void buildSim(){
		
		zi=new TableModule();
		zi.setName("zi");
		zi.addModule(new CPUParams(1,nbDims));   
		
		zj=new TableModule();
		zj.setName("zj");
		zj.addModule(new CPUParams(1,nbDims));   
		
		simP.setPoint1(zi);
		simP.setPoint2(zj);
	}
	
	public void prepareLearning(){
    	
    	
		if(model_file.length()!=0){
			if(!loaded){load();}
		}
		else{
			buildSim();
		}
		
		
		model_name="propagationModels/MLPembedProbas_Dims-"+nbDims+"_sim-"+sim+"/"+ref.getModelFile();
		
		System.out.println("learn : "+model_name);
			
		users=new ArrayList<String>(ref.getUsers());
		
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
					params.allocateNewParamsFor(mod,parInf,parSup);
				}
				embeddings.put(user,mod);
			}
			if(!sender_modules.containsKey(user)){
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
						params.allocateNewParamsFor(mod,0.5,1.5,parInf,parSup); 
					}
					else{
						params.allocateNewParamsFor(mod,(1.0/nbDims),parInf,parSup); 
					}
					sender_modules.put(user,mod);
			}
			
			
		}
		
		refs=new CPUMatrix(nbSamples,1);
		CPUAddVals comp=new CPUAddVals(1,refs);
		global=new SequentialModule();
		global.addModule(simP);
		global.addModule(comp);
		global.addModule(new CPUPower(1,2));
		global.addModule(new CPUAverageRows(1));
		this.currentInput=new Tensor(0);
	}
	
	public void learn(Optimizer optim){
		prepareLearning();
		
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
		
		if((nbForwards%nbAffiche==0) && (nbForwards!=0)){ // && (listei.size()>0)){
			System.out.println(this.getName()+" Average Loss = "+String.format("%.5f", globalLoss/(1.0*nbForwards))+", "+String.format("%.5f", loss/(1.0*nbSums))+", "+String.format("%.5f", lastLoss)+" nbSums="+nbSums+" nbForwards="+nbForwards);
			
		}
		
		
		if((nbForwards%nbSave==0)&& (nbForwards!=0) ){
				System.out.println("save "+nbForwards);
				save();
				
		}
		
		
		
		if(nbForwards%nbEstimations==0){
			loss=0;
			nbSums=0;
			System.out.println("lastLine="+lastLine);
			//System.gc();
			System.out.println("reinit sum");
		}
		
		lastUsed=new HashMap<String,Module>();
		CPUParams parsi=((CPUParams)zi.getModule(0)); 
		parsi.setNbVecs(nbSamples);
		CPUParams parsj=((CPUParams)zj.getModule(0));
		parsj.setNbVecs(nbSamples);
		for(int i=0;i<nbSamples;i++){
			int x=(int)(Math.random()*users.size());
			int y=x;
			while(y==x){
				y=(int)(Math.random()*users.size());
			}
			String ux=users.get(x);
			CPUParams modx=sender_modules.get(ux);
			parsi.addParametersFrom(modx);
			String uy=users.get(y);
			CPUParams mody=sender_modules.get(uy);
			parsj.addParametersFrom(mody);
			
			lastUsed.put("s_"+ux, modx);
			lastUsed.put("r_"+uy, mody);
			
			Double v=ref.getProba(ux, uy);
			v=(v==null)?0.0:v;
			//System.out.println(ux+","+uy+"="+v);
			refs.setValue(i, 0, -1.0*v);
		}	
		global.forward(this.currentInput);
		
		nbForwards++;
		lastLoss=getLossValue();
		loss+=lastLoss;
		globalLoss+=lastLoss;
		nbSums++;
		
		if(nbSums%nbAffiche==0){
			Matrix out=simP.getOutput().getMatrix(0);
			double s=0.0;
			for(int q=0;q<out.getNumberOfRows();q++){
				double o=out.getValue(q, 0);
				s+=o;	
			}
			System.out.println((s*1.0)/nbSamples);
		}
	}
	 
	public void backward()
	{
			global.backward_updateGradient(this.currentInput);
	}
	
	
	public void updateParams(double line){
    	lastLine=line;
    	for(String mod:lastUsed.keySet()){
    		Module m=lastUsed.get(mod);
    		m.updateParameters(line);
    		m.paramsChanged();
    		
    	}
		simP.updateParameters(line);
    }
	
	@Override
	public Parameters getUsedParams(){
		Parameters p=new Parameters();
		for(Module mod:lastUsed.values()){
			if(!mod.isLocked()){
				p.addSubParamList(mod.getParamList());
			}
		}
		
		Parameters ps=simP.getParamList();
		if(ps.getParams().size()>0){
			p.addSubParamList(ps);
		}
		
		return p;
    }
	
	public void load(){
    	String filename=model_file;
    	System.out.println("Load "+model_file);
        User.reinitAllLinks();
        BufferedReader r;
        
        embeddings=new HashMap<String,CPUParams>();
        train_users=new HashMap<String,HashMap<Integer,Double>>();
        
        params=new Parameters();
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          boolean modules_mode=false;
          boolean senders_mode=false;
          //boolean couples_mode=false;
          String[] sline;
          
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
          		senders_mode=true;
                 continue;
         	}
         	if(line.contains("</Sender_Modules>")){
                 senders_mode=false;
         		  continue;
         	}
          	
          	/*if(line.contains("<Couples>")){
          		couples_mode=true;
          		continue;
          	}
          	if(line.contains("</Couples>")){
          		couples_mode=false;
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
        	if(senders_mode){
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
        	
        	/*if(couples_mode){
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
        	}*/
        	
        	if(line.startsWith("nbDims")){
        		sline=line.split("=");
                nbDims=Integer.parseInt(sline[1]);
                continue;
        	}
        	
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
          }
          majParBounds();
          r.close();
          
          loaded=true;
          System.out.println("size embeddings loaded ="+embeddings.size());
          //System.out.println(this.couplesInTrain);
          buildSim();
          System.out.println("Sim built");
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
		double loss=(this.loss*1.0)/(nbSums*1.0);
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
          Parameters pars=this.simP.getParamList();
    	  StringBuilder sb=new StringBuilder();
    	  for(int i=0;i<pars.size();i++){
    		  sb.append("\t"+pars.get(i).getVal());
    	  }
          p.println("sim="+sim+sb.toString());
          p.println("parInf="+parInf);
          p.println("parSup="+parSup);
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
          
          
          /*p.println("<Couples>");
          for(String user:couplesInTrain.keySet()){
        	  sb=new StringBuilder();
        	  HashSet<String> w=couplesInTrain.get(user);
        	  for(String st:w){
        		  sb.append(user+";"+st+"\n");
        	  }
        	  p.print(sb.toString());
          }
          p.println("</Couples>");
          */
          
          
          
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
    
    @Override
	public void save(){ // throws IOException {
		
		File dir=new File(model_name);
		File fileOut=new File(dir.getAbsolutePath()+"/last");
    	save(dir.getAbsolutePath(),"last");
		
		//PrintStream p = null;
		double loss=(this.loss*1.0)/(nbSums*1.0);
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

	@Override
	public int infer(Structure struct) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int inferSimulation(Structure struct) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getContentNbDims() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public String getName(){
		return "MLPembedProba "+nbDims;
	}
	
	public static void main(String[] args){
		HashMap<String,String> hargs=ArgsParser.parseArgs(args);
		int nbDims=(hargs.containsKey("nD"))?Integer.parseInt(hargs.get("nD")):2;
		double line=(hargs.containsKey("l"))?Double.valueOf(hargs.get("l")):0.1;
		double decFactor=(hargs.containsKey("dF"))?Double.valueOf(hargs.get("dF")):0.9999999;
		int sim=(hargs.containsKey("s"))?Integer.parseInt(hargs.get("s")):5;
		
		IC mod=new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/usersusers_3_linkThreshold1.0_contaMaxDelay-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",100,2);
		
		MLPembedProba mlp=new MLPembedProba(mod,nbDims,sim);
		
		mlp.nbSave=(hargs.containsKey("nS"))?Integer.valueOf(hargs.get("nS")):10000;
		mlp.nbEstimations=(hargs.containsKey("nE"))?Integer.valueOf(hargs.get("nE")):1000;
		mlp.nbAffiche=(hargs.containsKey("nA"))?Integer.valueOf(hargs.get("nA")):100;
		
		Env.setVerbose(0);
		
		Optimizer opt=Optimizer.getDescent(DescentDirection.getGradientDirection(), LineSearch.getFactorLine(line,decFactor));
		mlp.learn(opt);
	}
	
	
	
}
