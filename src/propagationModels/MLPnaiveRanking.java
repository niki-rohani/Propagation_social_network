package propagationModels;

import java.io.BufferedReader;

import mlp.CPUMatrix;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeMap;

import cascades.Cascade;
import utils.Keyboard;
import utils.CopyFiles;
import mlp.CPULogistic;
import mlp.CPUAverageRows;
import mlp.CPUHingeLoss;
import mlp.CPUL2Norm;
import mlp.CPUParams;
//import mlp.CPUPosNegSeparator;
import mlp.CPUSum;
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
import mlp.CPUAddVecs;
import core.Structure;
import core.User;
//import mlp.CPUDiag;
import mlp.CPUTermByTerm;
import mlp.CPUSparseLinear;
import mlp.CPUSparseMatrix;
public class MLPnaiveRanking extends MLP{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected HashMap<String,HashMap<String,CPUParams>> probas;
	private transient CPUParams allPars;
	private TableModule diff;
	
	//private boolean diagContentLocked=true;
	private double sumLoss=0.0f;
	private int nbForwards=0;
	private int nbSum=0;
	private int nbEstimations=50000;
	private int freqSave=1000000;
	private Tensor currentInput;
	private double lastLoss=0.0f;
	//private int nbInitSteps=1;
	private double sumLossTot=0;
	private HashMap<String,Module> lastUsed;
	//private boolean iInInit=true;
	double best=0.0f;
	
	//boolean averageSender=false; //true;
	//private HashSet<Integer> stemsVus=new HashSet<Integer>();
	
	
	public MLPnaiveRanking(){
		this("");
	}
	public MLPnaiveRanking(String model_file){
		super(model_file);
		
	}
	
	public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(this.probas.keySet());
	}
	public int getContentNbDims(){
		return 0;
	}
	
	public void load(){
		String filename=model_file;
		probas = new HashMap<String,HashMap<String,CPUParams>>();
        //User.reinitAllLinks();
        BufferedReader r;
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          line=r.readLine();
          line=r.readLine();
          String[] sline=line.split("=");
          //iInInit=Boolean.valueOf(sline[1]);
          while((line=r.readLine()) != null) {
        	//System.out.println(line);
        	String[] tokens = line.split("\t") ;
            if(tokens[2].startsWith("NaN"))
                tokens[2]="0.0" ;
           
            double d = Double.parseDouble(tokens[2]) ;
            if(d==0)
                continue ;
            
            String i=tokens[0];
            String j=tokens[1];
            HashMap<String,CPUParams> pi=probas.get(i);
	    	if (pi==null){
	    		 pi=new HashMap<String,CPUParams>();
	    		 probas.put(i, pi);
	    	}
	    	pi.put(j,new CPUParams(1,1,(double)d,0.0f,Double.MAX_VALUE));
	    	
          }
          r.close();
          loaded=true;
        }
        catch(IOException e){
        	System.out.println("Probleme lecture modele "+filename);
        }
        //System.out.println(probas);
    }

	public void save() {
		File f=new File(model_name);
		File dir=f.getParentFile();
		File fileOut=new File(dir.getAbsolutePath()+"/last");
    	
		model_file=fileOut.getAbsolutePath(); //model_name+"_"+last_save;
		PrintStream p = null;
		double loss=(sumLoss*1.0f)/(nbSum*1.0f);
        try{
        	File file=new File(model_file);
        	dir=file.getParentFile();
        	if(dir!=null){
        		dir.mkdirs();
        	}
        	
        	p = new PrintStream(fileOut) ;
        	p.println("Loss="+(sumLoss*1.0)/(nbSum*1.0));
        	
        	//p.println("iInInit="+iInInit);
			for(String i : probas.keySet()){
				HashMap<String,CPUParams> pi=probas.get(i);
				for(String j:pi.keySet()){
					p.println(i+"\t"+j+"\t"+pi.get(j).getParamList().get(0).getVal()) ;
				}
			}
				
			p.close();
			if(nbForwards>this.freqSave){
	      		  if(loss<best){
	      			dir=f.getParentFile();
	            	File fOut=new File(dir.getAbsolutePath()+"/best");
	            	CopyFiles.copyFile(fileOut,fOut);
	            	best=loss;   
	      		  }
	      	 }
	      	 else{
	      		  best=loss;
	      	 }
		}
    	catch(IOException e){
    		System.out.println("Probleme sauvegarde modele "+fileOut.getAbsolutePath());
    	}
	}

	
	
	
	
	public void learn(PropagationStructLoader ploader,Optimizer optim){
		
		if(model_file.length()!=0){
			if(!loaded){load();}
		}
		//this.iInInit=iInInit;
		String format = "dd.MM.yyyy_H.mm.ss";
		probas=new HashMap<String,HashMap<String,CPUParams>>();
		//contentLocked=false;
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		if(model_name.length()==0){
			//model_file="propagationModels/MLPdiffusion_Dims-"+nbDims+"_step-"+step+"_nbInit-"+nbInitSteps+"_db-"+db+"_cascadesCol-"+cascadesCollection+"_lambda-"+lambda+"_iInInit-"+iInInit+"_transSend-"+transSend+"_transSendContent-"+transSendContent+"_diag-"+withDiag+"_withDiagContent-"+withDiagContent+"/last";
			model_name="propagationModels/MLPnaiveRanking_step-"+step+"_ratioInits-"+ploader.getRatioInits()+"_nbMaxInits-"+ploader.getNbMaxInits()+"_db-"+ploader.getDb()+"_cascadesCol-"+ploader.getCollection()+"/MLPdiffusion_"+formater.format(date);
		}
		System.out.println("learn : "+model_name);
		/*NaiveLinkModel nm=new NaiveLinkModel();
		nm.learn(db, cascadesCollection, "users_1", 1, step, nbInitSteps,iInInit,true);
		HashMap<String,HashMap<String,Double>> pr=nm.getProbas();
		*/
		prepareLearning(ploader);
		
		/*for(String v:pr.keySet()){
			HashMap<String,Double> h=pr.get(v);
			HashMap<String,CPUParams> nh=probas.get(v);
			if(nh==null){
				nh=new HashMap<String,CPUParams>();
				probas.put(v,nh);
			}
			for(String w:users){
				Double val=h.get(w);
				double va=(val==null)?0.0:val;
				CPUParams pars=new CPUParams(1,1,(double)va,0.0f,Double.MAX_VALUE);
				nh.put(w, pars);
			}
		}*/
		/*for(String v:users){
			HashMap<String,CPUParams> nh=new HashMap<String,CPUParams>();
			probas.put(v,nh);
			for(String w:users){
				CPUParams pars=new CPUParams(1,1);
				params.allocateNewParamsFor(pars, 0.0f, 0.0f, Double.MAX_VALUE);
				nh.put(w, pars);
			}
		}*/
		
		
		System.out.println("learn : "+model_name);
		
		currentInput=new Tensor(0);
		
		ArrayList<Double> weights=new ArrayList<Double>();
		weights.add(1.0);
		weights.add(-1.0);
		
		
		diff=new TableModule();
		diff.setName("diff");
		diff.addModule(null);
		diff.addModule(null);
		
		CPUSum diffik=new CPUSum(1,2,weights);
		global=new SequentialModule();
		global.addModule(diff);
		global.addModule(diffik);
		global.addModule(new CPUHingeLoss());
		global.addModule(new CPUAverageRows(1));
		optim.optimize(this);
		
		loaded=true;
	}
	
	
	
	public void forward(){
		
			forward_minibatch();
		
	}
	
	
	
	
	
	public void forward_minibatch() {
		//System.out.println(zi_zj);
		
		
		// choose cascade
		int x=(int)(Math.random()*this.cascades_ids.size()); 
		int c=cascades_ids.get(x);
		//System.out.println("Cascade "+c);
		
		PropagationStruct pstruct=this.train_cascades.get(c);
		LinkedHashSet<String> users=pstruct.getPossibleUsers();
		ArrayList<String> lusers=new ArrayList<String>(users);
		
		TreeMap<Long,HashMap<String,Double>> initInfected=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
        ArrayList<String> contaminated = new ArrayList<String>((PropagationStruct.getPBeforeT(infections)).keySet()) ;
        ArrayList<String> initContaminated = new ArrayList<String>((PropagationStruct.getPBeforeT(initInfected)).keySet()) ;
        
        while(contaminated.size()<=initContaminated.size()){
        	x=(int)(Math.random()*this.cascades_ids.size()); 
    		c=cascades_ids.get(x);
    		pstruct=this.train_cascades.get(c);
    		initInfected=pstruct.getInitContaminated();
            infections=pstruct.getInfections();
            contaminated = new ArrayList<String>((PropagationStruct.getPBeforeT(infections)).keySet()) ;
            initContaminated = new ArrayList<String>((PropagationStruct.getPBeforeT(initInfected)).keySet()) ;
        }
        //System.out.println("Chosen cascade = "+c);
        //System.out.println(infections);
        //System.out.println(contaminated.size()+" init "+(PropagationStruct.getPBeforeT(infections)).keySet().size()+" contamines");
        ArrayList<String> listei=new ArrayList<String>();
        ArrayList<String> listej=new ArrayList<String>();
        ArrayList<String> listek=new ArrayList<String>();
        
       int nbMaxSamples=100;
       HashMap<String,Long> times=pstruct.getInfectionTimes();
       
       lastUsed=new HashMap<String,Module>();
        ArrayList<Long> infTimes=new ArrayList<Long>(infections.keySet());
		//HashSet<String> vus=new HashSet<String>();
		boolean stop=false;
		int nbSamples=0;
		
		while(nbSamples<nbMaxSamples){
			String ui="";
			x=(int)(Math.random()*initContaminated.size());
			ui=initContaminated.get(x);
			
			
	        Long ti=times.get(ui);
	        
	        x=(int)(Math.random()*contaminated.size());
	        String uj=contaminated.get(x);
	        Long tj=times.get(uj);
	        if((tj==ti) && (ui.compareTo(uj)!=0)){
       		 continue;
       	 	}
	        
	        
	        if(tj<ti){
	        	String tmp=ui;
	        	ui=uj;
	        	uj=tmp;
	        	Long ttmp=ti;
	        	ti=tj;
	        	tj=ttmp;
	        }
	        
	        x=(int)(Math.random()*users.size());
	        String uk=lusers.get(x);
	        if(uk.compareTo(ui)==0){
	        	continue;
	        }
	        if(uk.compareTo(uj)==0){
	        	continue;
	        }
	        Long tk=times.get(uk);
	        if ((contaminated.contains(uk)) && (tk<=tj)){
	        	 if(tk==ti){
	        		 continue;
	        	 }
	        	 if(tk==tj){
	        		 continue;
	        	 }
	        	
	        	 if(tk<ti){
	        		 String tmp=ui;
		 	         ui=uk;
		 	         uk=tmp;
		 	         Long ttmp=ti;
		 	         ti=tk;
		 	         tk=ttmp;
	 	         }
	        	 
	        	 String tmp=uj;
	 	         uj=uk;
	 	         uk=tmp;
	 	         Long ttmp=tj;
	 	         tj=tk;
	 	         tk=ttmp;
	 	        
	        }
	        listei.add(ui);
			listej.add(uj);
			listek.add(uk);
			
	        nbSamples++;
		}
		CPUParams parsj=new CPUParams(listej.size(),1);
		CPUParams parsk=new CPUParams(listek.size(),1);
		for(int i=0;i<listei.size();i++){
			String ui=listei.get(i);
			String uj=listej.get(i);
			String uk=listek.get(i);
			HashMap<String,CPUParams> pars=probas.get(ui);
			if(pars==null){
				pars=new HashMap<String,CPUParams>();
				probas.put(ui, pars);
			}
			//System.out.println("Chosen ui = "+ui);
			//System.out.println("Chosen uj = "+uj);
			//System.out.println("Chosen uk = "+uk);
			
			
			CPUParams mj=pars.get(uj);
			if(mj==null){
				mj=new CPUParams(1,1);
				params.allocateNewParamsFor(mj,1.0f, 0.0f, Double.MAX_VALUE);
				pars.put(uj, mj);
			}
			parsj.addParametersFrom(mj);
			lastUsed.put(uj, mj);
			CPUParams mk=pars.get(uk);
			if(mk==null){
				if(contaminated.contains(uk)){
					mk=new CPUParams(1,1);
					params.allocateNewParamsFor(mk,0.0f, 0.0f, Double.MAX_VALUE);
					pars.put(uk, mk);
					lastUsed.put(uk, mk);
				}
				else{
					mk=new CPUParams(1,1,0.0f, 0.0f, Double.MAX_VALUE);
				}
			}
			else{
				lastUsed.put(uk, mk);
			}
			parsk.addParametersFrom(mk);
			
		}
		
		diff.setModule(0,parsj);
		diff.setModule(1,parsk);
		
		
        
        //System.out.println("content = "+content);
		
        global.forward(this.currentInput);
       
		nbForwards++;
		lastLoss=getLossValue();
		sumLoss+=lastLoss;
		sumLossTot+=lastLoss;
		nbSum++;
		if(nbForwards%1==0){
			System.out.println(this.getName()+" Average Loss = "+sumLossTot/(1.0*nbForwards)+", "+sumLoss/(1.0*nbSum)+", "+lastLoss+" nbSums="+nbSum+" nbForwards="+nbForwards);
			//System.out.println(params.getParams().get(0).getVal());
		}
		//zi.lockParams();
		//zj.lockParams();
		//zk.lockParams();
		//diag.lockParams();
		//System.out.println("diag = "+this.seqDiag.getOutput());
		if(nbForwards%10000==0){
			//System.out.println(this.currentInput.getMatrix(0));
		}
		if(nbForwards%freqSave==0){
			//try{
				save();
				
			/*}
			catch(Exception e){
				
			}*/
		}
		if(nbForwards%nbEstimations==0){
			//System.out.println("Average Loss = "+sumLoss/(1.0*nbSum));
			sumLoss=0;
			nbSum=0;
			System.out.println("reinit sum");
		}
			
		
		
	}
	
	
	
	
	public void updateParams(double line){
		int nb=0;
		for(Module mod:lastUsed.values()){
				//System.out.println(mod);
				//System.out.println(mod.getParamList());
				
				mod.updateParameters(line);
				mod.paramsChanged();
				nb++;
		}
		
		
	}
	
	
	
	@Override
	public Parameters getUsedParams(){
		Parameters p=new Parameters();
		for(Module mod:lastUsed.values()){
				p.addSubParamList(mod.getParamList());
		}
				
		return p;
	}

	
	

	@Override
	public void backward() {
		//if(lastLoss>0.0f){
			global.backward_updateGradient(this.currentInput);
		//}
	}

	
	public int infer(Structure struct){
		
		if(!loaded){load();}
		System.out.println("Inference...");
		//System.out.println("bias = "+bias);
		//System.out.println("directed = "+sender_receiver);
		//System.out.println("logistic = "+logisticDiag);
		
		
		PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        ArrayList<String> infected = new ArrayList<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        //ArrayList<String> users=new ArrayList<String>(probas.keySet());
        int tt=1;
        System.out.println("users="+probas.keySet().size());
	    
	    for(long t:contaminated.keySet()){
	    	//System.out.println(t);
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	infections.put((long)tt, (HashMap<String,Double>) inf.clone());
	    	//System.out.println(t+"ok");
	    	tt++;
	    }
	    int firstNewT=tt;
	    
	    
	    ArrayList<String> sources=new ArrayList<String>();
	    ArrayList<String> receivers=new ArrayList<String>();
	    
	    int nbLignes=0;
	    System.out.println("init="+infected.size());
	    for(String init:infected){
	    	if(probas.containsKey(init)){
	    		HashMap<String,CPUParams> h=probas.get(init);
	    		for(String user:h.keySet()){
	    			if(!infected.contains(user)){
	    				sources.add(init);
	    				receivers.add(user);
	    				nbLignes++;
	    			}
	    		}
	    	}
	    }
	    System.out.println("Modules created");
	    if(nbLignes>0){
	    	
			
		    Tensor tensor=new Tensor(0);
		    this.currentInput=tensor;
		    Matrix results=new CPUMatrix(nbLignes,1);
		    for(int i=0;i<nbLignes;i++){
		    	HashMap<String,CPUParams> h=probas.get(sources.get(i));
		    	CPUParams mod=h.get(receivers.get(i));
		    	mod.forward(tensor);
		    	Tensor res=mod.getOutput();
			    Matrix m=res.getMatrix(0);
			    results.setValue(i, 0, m.getValue(0, 0));
		    }
		    
		    System.out.println("Diffusion done");
		    //System.out.println(res);
		    double max=1.0f;
		    HashMap<String,Double> maxP=new HashMap<String,Double>();
		    int r=0;
		    TreeMap<Long,HashMap<String,Double>> ref=pstruct.getInfections();
		    HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		    for(int i=0;i<nbLignes;i++){
			    String w=receivers.get(i);
			    Double v=maxP.get(w);
			    double dist=results.getValue(i, 0);
			    if((v==null) || (dist>v)){
			    			maxP.put(w, dist);
			    			if(dist>max){
			    				max=dist;
			    			}
			    }
		    }
		    
		    
		    
		    HashMap<String,Double> infstep=new HashMap<String,Double>();
		    	
		    for(String user:maxP.keySet()){
		    	if(!infected.contains(user)){
		    		infstep.put(user, (double)(maxP.get(user)/max));
		    	}
		    }
		    infections.put((long)firstNewT, infstep);
	    }
	    
	    pstruct.setInfections(infections);
		return 0;
	}
	
	
	
	public int inferSimulation(Structure struct){
		throw new RuntimeException("No simulation possible with model MLPdiffusion");
	}
	
	private class ContaminatedComparator implements Comparator<String>{
		 HashMap<String,Double> nbc;
		 public ContaminatedComparator(HashMap<String,Double> nbc){
			 this.nbc=nbc;
		 }
		 public int compare(String x, String y) {
	         Double nx=nbc.get(x);   
	         Double ny=nbc.get(y);
	         double vx=(nx==null)?0.0:nx;
	         double vy=(ny==null)?0.0:ny;
	         if(vy>vx){
	        	 return(1);
	         }
	         else{
	        	 if(vx>vy){
		        	 return(-1);
		         }
	        	 else{
	        		 return(0);
	        	 }
	         }
	     }
	}
	
	
	
	public String getName(){
		return "MLPnaiveRanking";
		
	}
	public String toString(){
		return "MLPnaiveRanking_model_file="+model_file;
	}

	public static void main(String[] args){
		try{
			
			MLPnaiveRanking mlp;
			if(args.length==6){
				mlp=new MLPnaiveRanking(args[5]); 
			}
			else{
				mlp=new MLPnaiveRanking();
			}
			double ratioInits=Double.valueOf(args[2]);
			
			Env.setVerbose(0);
			//MLPdiffusion mlp=new MLPdiffusion("psauv/MLPDiagContent2_Dims-100_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0_iInInit-true_senderReceiver-false/last");
			/*
			*/
			Optimizer opt=Optimizer.getDescent(DescentDirection.getGradientDirection(), LineSearch.getFactorLine(Double.valueOf(args[3]),Double.valueOf(args[4])));
			//opt.optimize(mlp);
			
			//mlp.load();
			PropagationStructLoader ploader=new PropagationStructLoader(args[0],args[1],(long)1,ratioInits,-1);
			
			mlp.learn(ploader, opt); //,"propagationModels/MLPDiagContent_Dims-200_step-1_nbInit-1_db-diggPruned_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0/MLPDiagContent_18.09.2013_14.19.12_20.09.2013_8.28.55");
			
			mlp.save();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
