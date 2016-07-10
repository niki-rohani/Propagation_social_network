package propagationModels;
import utils.ArgsParser;

import java.io.BufferedReader;
import java.util.LinkedHashMap;

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
import mlp.CPUTimesVals;
import mlp.CPUSparseMatrix;
public class MLPdiffusion extends MLP{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected int nbDims;
	protected HashMap<String,CPUParams> user_modules;
	protected HashMap<String,CPUParams> translations;
	protected HashMap<String,CPUParams> diagSenders;
	
	
	private transient TableModule diagSender;
	private transient TableModule zi;
	private transient TableModule zj;
	private transient TableModule zk;
	private transient Module zi2;
	private transient CPUSparseLinear diagContent;
	private transient CPUParams diag;
	private transient SequentialModule seqDiag;
	private transient CPUSparseLinear transContent;
	//private transient SequentialModule seqDiag;

	private transient CPUParams allPars;
	//private boolean diagContentLocked=true;
	private double sumLoss=0.0f;
	private int nbForwards=0;
	private int nbSum=0;
	private int nbEstimations=50000;
	private int freqSave=50000;
	private Tensor currentInput;
	private double lastLoss=0.0f;
	private int maxIdStem=2000;
	private boolean boolContent=false;
	private boolean logisticDiag=false;
	//private int nbInitSteps=1;
	private double sumLossTot=0;
	private HashMap<String,Module> lastUsed;
	//private boolean iInInit=true;
	private boolean withDiag=true;
	private boolean withDiagContent=false;
	private boolean withDiagSenders=false;
	private boolean transSend=true;
	private boolean transSendContent=false;
	boolean contentLocked=false;
	int nbContentLocked=0; //100000;
	int nbContentUnLocked=10000;
	int nbFromChangeLock=0;
	double best=Double.NaN;
	int nbTripletsInLoss;
	private CPUTimesVals tv;
	boolean unbiased=true;
	double parSup=1.0f;
	double parInf=-1.0f;
	double diagSup=10.0f;
	double diagInf=0.0001f;
	//boolean averageSender=false; //true;
	//private HashSet<Integer> stemsVus=new HashSet<Integer>();
	
	/**
	 * Constructor for learning.
	 * 
	 * @param nbDims  nombre de dimensions de l'espace de projection
	 * @param maxIdStem  numero max de stem a utiliser si on prend en compte le contenu (mets toujours 2000 dans un premier temps)
	 * @param boolContent  vrai si on veut travailler avec du contenu booleen plutot qu'avec des poids reels (mets toujours faux, il faut virer ca, ca ne sert a rien)
	 * @param logisticDiag  vrai si on veut appliquer une fonction logistique sur la diagonale (si on veut etre non lineaire par rapport au contenu)
	 * @param withDiag  vrai si on veut multiplier les projections par une diagonale dont les parametres sont libres
	 * @param withDiagContent  vrai si on veut multiplier les projections par une diagonale dont les parametres sont appris en fonction du contenu
	 * @param transSend  vrai si on veut appliquer une translation a la projection du sender (permet de separer senders et receivers)
	 * @param transSendContent  vrai si on veut ajouter une translation dependant du contenu a la projection du sender
	 * @param withDiagSender  vrai si on veut multiplier les projections des senders par une diagonale dont les parametres sont libres (autre moyen de separer senders et receivers)
	 * Si plusieurs translations sont definies, elles s'ajoutent les unes aux autres. Idem pour les diagonales.
	 */
	public MLPdiffusion(int nbDims, int maxIdStem, boolean boolContent, boolean logisticDiag, boolean withDiag, boolean withDiagContent, boolean transSend, boolean transSendContent, boolean withDiagSender){
		this("");
		this.nbDims=nbDims;
		this.maxIdStem=maxIdStem;
		this.withDiag=withDiag;
		this.withDiagContent=withDiagContent;
		this.boolContent=boolContent;
		this.logisticDiag=logisticDiag;
		this.transSend=transSend;
		this.transSendContent=transSendContent;
		this.withDiagSenders=withDiagSender;
		
		
	}
	/*public MLPDiagContent(){
		this("");
	}*/
	
	/**
	 * Constructor from a parameters file.  
	 * @param model_file  file containing the learned model 
	 */
	public MLPdiffusion(String model_file){
		super(model_file);
		this.nbDims=1;
		
		translations=new HashMap<String,CPUParams>();
		user_modules=new HashMap<String,CPUParams>();
		diagSenders=new HashMap<String,CPUParams>();
	}
	public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(this.user_modules.keySet());
	}
	public int getContentNbDims(){
		return getMaxIdStem();
	}
	
	public int getMaxIdStem(){
		if(!loaded){
			load();
		}
		return maxIdStem;
		
	}
	
	public void majParBounds(){
		for(CPUParams mod:user_modules.values()){
			for(Parameter p:mod.getParamList().getParams()){
				p.setLowerBound(parInf);
				p.setUpperBound(parSup);
			}
		}
		if(transSend){
			for(CPUParams mod:translations.values()){
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
	}
	
	
	
	@Override
	public void load(){ // throws IOException {
		String filename=model_file;
        User.reinitAllLinks();
        BufferedReader r;
        translations=new HashMap<String,CPUParams>();
        user_modules=new HashMap<String,CPUParams>();
        diagSenders=new HashMap<String,CPUParams>();
        train_users=new HashMap<String,HashMap<Integer,Double>>();
        params=new Parameters();
        transSend=false;
        transSendContent=false;
        withDiag=false;
        withDiagContent=false;
        withDiagSenders=false;
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          boolean modules_mode=false;
          boolean trans_mode=false;
          boolean transContent_mode=false;
          boolean profiles=false;
          boolean diagContent_mode=false;
          boolean diagSender_mode=false;
          boolean diag_mode=false;
          String[] sline;
          
          nbDims=-1;
          while((line=r.readLine()) != null) {
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
      	      	if(line.startsWith("unbiased")){
            			  sline=line.split("=");
            			  unbiased=Boolean.valueOf(sline[1]);
                	   	  continue;
      	      	}
	      	     if(line.startsWith("parInf")){
	        	  	  sline=line.split("=");
	        	  	  parInf=Float.valueOf(sline[1]);
	        	  	  continue;
	        	}
	        	if(line.startsWith("parSup")){
	      	  	  sline=line.split("=");
	      	  	  parSup=Float.valueOf(sline[1]);
	      	  	  continue;
	        	}
	        	if(line.startsWith("diagInf")){
	      	  	  sline=line.split("=");
	      	  	  diagInf=Float.valueOf(sline[1]);
	      	  	  continue;
	        	}
	        	if(line.startsWith("diagSup")){
	    	  	  sline=line.split("=");
	    	  	  diagSup=Float.valueOf(sline[1]);
	    	  	  continue;
	        	}
	        	if(line.contains("<User_Modules>")){
	        		 modules_mode=true;
	                  continue;
	          	}
	          	if(line.contains("</User_Modules>")){
	                  modules_mode=false;
	          		  continue;
	          	}
	          	if(line.contains("<Trans_Modules>")){
	      		 	 trans_mode=true;
	                continue;
	        	}
	         	if(line.contains("</Trans_Modules>")){
	               trans_mode=false;
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
	        	if(modules_mode){  
	        		String[] tokens = line.split("\t") ;
		            String user=tokens[0];
		            
	        		double[] vals=new double[tokens.length-1];
		            for(int i=1;i<tokens.length;i++){
		            	vals[i-1]=Float.valueOf(tokens[i]);
		            }
		            if(nbDims<0){
		            	nbDims=vals.length;
		            }
		            CPUParams cpuPars=new CPUParams(1,nbDims);
		            params.allocateNewParamsFor(cpuPars, vals, parInf, parSup);
		            user_modules.put(user, cpuPars);
		            cpuPars.paramsChanged();
		            
	        	}
	        	if(trans_mode){  
	        		transSend=true;
	        		String[] tokens = line.split("\t") ;
		            String user=tokens[0];
		            
	        		double[] vals=new double[tokens.length-1];
		            for(int i=1;i<tokens.length;i++){
		            	vals[i-1]=Float.valueOf(tokens[i]);
		            }
		            if(nbDims<0){
		            	nbDims=vals.length;
		            }
		            CPUParams cpuPars=new CPUParams(1,nbDims);
		            params.allocateNewParamsFor(cpuPars, vals, parInf, parSup);
		            translations.put(user, cpuPars);
		            
	        	}
	        	if(transContent_mode){ 
	        		transSendContent=true;
	        		String[] tokens = line.split("\t") ;
		            double[] vals=new double[tokens.length-1];
		            for(int i=1;i<tokens.length;i++){
		            	//System.out.println(tokens[i]);
		            	vals[i-1]=Float.valueOf(tokens[i]);
		            	
		            }
		            int nbd=nbDims;
		            //if(directed){nbd*=2;}
		            transContent=new CPUSparseLinear(vals.length/nbd,nbd);
		            params.allocateNewParamsFor(transContent, vals);
		            transContent.paramsChanged();
		            
		            
	        	}
	        	if(diagContent_mode){ 
	        		withDiagContent=true;
	        		String[] tokens = line.split("\t") ;
		            double[] vals=new double[tokens.length-1];
		            for(int i=1;i<tokens.length;i++){
		            	//System.out.println(tokens[i]);
		            	vals[i-1]=Float.valueOf(tokens[i]);
		            	
		            }
		            int nbd=nbDims;
		            //if(directed){nbd*=2;}
		            diagContent=new CPUSparseLinear(vals.length/nbd,nbd);
		            params.allocateNewParamsFor(diagContent, vals);
		            diagContent.paramsChanged();
		            
		            
	        	}
	        	if(diagSender_mode){ 
	        		withDiagSenders=true;
	        		String[] tokens = line.split("\t") ;
		            String user=tokens[0];
		            
	        		double[] vals=new double[tokens.length-1];
		            for(int i=1;i<tokens.length;i++){
		            	vals[i-1]=Float.valueOf(tokens[i]);
		            }
		            
		            CPUParams cpuPars=new CPUParams(1,nbDims);
		            params.allocateNewParamsFor(cpuPars, vals, diagInf, diagSup);
		            diagSenders.put(user, cpuPars);
		            
	        	}
	        	if(diag_mode){ 
	        		withDiag=true;
	        		String[] tokens = line.split("\t") ;
		            double[] vals=new double[tokens.length-1];
		            for(int i=1;i<tokens.length;i++){
		            	//System.out.println(tokens[i]);
		            	vals[i-1]=Float.valueOf(tokens[i]);
		            	
		            }
		            int nbd=nbDims;
		            //if(directed){nbd*=2;}
		            diag=new CPUParams(1,nbd);
		            params.allocateNewParamsFor(diag, vals, diagInf, diagSup);
		            diag.paramsChanged();
		            
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
	        	}
          }
          
          r.close();
          loaded=true;
        }
        catch(IOException e){
        	throw new RuntimeException("Load model => Probleme lecture modele "+filename);
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
          p.println("nbDims="+nbDims);
          p.println("maxIdStem="+maxIdStem);
          //p.println("iInInit="+iInInit);
          p.println("boolContent="+boolContent);
          p.println("logisticDiag="+logisticDiag);
          p.println("unbiased="+unbiased);
          p.println("parInf="+parInf);
          p.println("parSup="+parSup);
          p.println("diagInf="+diagInf);
          p.println("diagSup="+diagSup);
          p.println("<User_Modules>");
          for(String user:user_modules.keySet()){
        	  StringBuilder sb=new StringBuilder();
        	  CPUParams mod=user_modules.get(user);
        	  Parameters pars=mod.getParamList();
        	  sb.append(user);
        	  for(int i=0;i<pars.size();i++){
        		  sb.append("\t"+pars.get(i).getVal());
        	  }
        	  p.println(sb.toString());
  		  }
          p.println("</User_Modules>");
          if(transSend){
        	  p.println("<Trans_Modules>");
              for(String user:translations.keySet()){
            	  StringBuilder sb=new StringBuilder();
            	  CPUParams mod=translations.get(user);
            	  Parameters pars=mod.getParamList();
            	  sb.append(user);
            	  for(int i=0;i<pars.size();i++){
            		  sb.append("\t"+pars.get(i).getVal());
            	  }
            	  p.println(sb.toString());
      		  }
              p.println("</Trans_Modules>");
          }
          if(withDiagSenders){
        	  p.println("<DiagSenders>");
              for(String user:diagSenders.keySet()){
            	  StringBuilder sb=new StringBuilder();
            	  CPUParams mod=diagSenders.get(user);
            	  Parameters pars=mod.getParamList();
            	  sb.append(user);
            	  for(int i=0;i<pars.size();i++){
            		  sb.append("\t"+pars.get(i).getVal());
            	  }
            	  p.println(sb.toString());
      		  }
              p.println("</DiagSenders>");
          }
          if(transSendContent){
        	  p.println("<TransContent>");
        	  Parameters pars=this.transContent.getParamList();
        	  StringBuilder sb=new StringBuilder();
        	  for(int i=0;i<pars.size();i++){
        		  sb.append("\t"+pars.get(i).getVal());
        	  }
        	  p.println(sb.toString());
  		
        	  p.println("</TransContent>");
          }
          if(withDiagContent){
        	  p.println("<DiagContent>");
        	  Parameters pars=this.diagContent.getParamList();
        	  StringBuilder sb=new StringBuilder();
        	  for(int i=0;i<pars.size();i++){
        		  sb.append("\t"+pars.get(i).getVal());
        	  }
        	  p.println(sb.toString());
  		
        	  p.println("</DiagContent>");
          }
          if(withDiag){
        	  p.println("<Diag>");
        	  Parameters pars=this.diag.getParamList();
        	  StringBuilder sb=new StringBuilder();
        	  for(int i=0;i<pars.size();i++){
        		  sb.append("\t"+pars.get(i).getVal());
        	  }
              p.println(sb.toString());
        	  p.println("</Diag>");
          }
          p.println("<User_Profiles>");
          for(String user:train_users.keySet()){
        	  StringBuilder sb=new StringBuilder();
        	  sb.append(user);
        	  HashMap<Integer,Double> w=train_users.get(user);
        	  for(Integer st:w.keySet()){
        		  sb.append("\t"+st+"="+w.get(st));
        	  }
        	  p.println(sb.toString());
          }
          p.println("<User_Profiles>");
          
          
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
          
      	  /*if(nbForwards>this.freqSave){
      		  if(loss<best){
      			dir=f.getParentFile();
            	File fOut=new File(dir.getAbsolutePath()+"/best");
            	CopyFiles.copyFile(fileOut,fOut);
            	best=loss;   
      		  }
      	  }
      	  else{
      		  best=loss;
      	  }*/
          
        }
        catch(IOException e){
        	throw new RuntimeException("Probleme sauvegarde modele "+fileOut.getAbsolutePath());
        	
        }
        finally{
        	if(p!=null){
        		p.close();
        	}
        }
		

	}

	public void test(){
		long step=1l;
		int nbInitSteps=1;
		users=new ArrayList<String>();
		users.add("a"); users.add("b"); users.add("c"); users.add("d"); users.add("e");
		//this.train_users=new HashMap<String,HashMap<Integer,Double>>();
		
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
		diffusion.put(0, 1.0);
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
		h.put("d", 1.0);
		infections.put(2l,h);
		h=new HashMap<String,Double>();
		h.put("e", 1.0);
		infections.put(3l,h);
		diffusion.put(1, 1.0);
		struct=new PropagationStruct(null,step,nbInitSteps,init,infections,diffusion);
		train_cascades.put(2, struct);
		
		init=new TreeMap<Long,HashMap<String,Double>>();
		infections=new TreeMap<Long,HashMap<String,Double>>();
		diffusion=new TreeMap<Integer,Double>();
		h=new HashMap<String,Double>();
		h.put("b",1.0);
		init.put(1l, h);
		infections.put(1l, h);
		h=new HashMap<String,Double>();
		h.put("d", 1.0);
		infections.put(2l,h);
		h=new HashMap<String,Double>();
		h.put("e", 1.0);
		infections.put(3l,h);
		diffusion.put(1, 1.0);
		struct=new PropagationStruct(null,step,nbInitSteps,init,infections,diffusion);
		train_cascades.put(3, struct);
		
		
		cascades_ids=new ArrayList<Integer>(train_cascades.keySet());
	}
	
	
	/**
	 * Learning function
	 * 
	 * @param db  database where to find the cascades
	 * @param cascadesCollection   collection where to find the cascades
	 * @param step  step size (set 1 to use original timestamps)
	 * @param optim 	optimizer to be used
	 * @param lambda	not used for the moment (to set a weight for a regularization term )
	 * @param unbiased  if true, forwards correct sampling biases (true recommended) 
	 */
	public void learn(PropagationStructLoader ploader,Optimizer optim, double lambda, boolean unbiased){
		
		if(model_file.length()!=0){
			if(!loaded){load();}
		}
		//this.iInInit=iInInit;
		this.unbiased=unbiased;
		String format = "dd.MM.yyyy_H.mm.ss";
		
		//contentLocked=false;
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		if(model_name.length()==0){
			//model_file="propagationModels/MLPdiffusion_Dims-"+nbDims+"_step-"+step+"_nbInit-"+nbInitSteps+"_db-"+db+"_cascadesCol-"+cascadesCollection+"_lambda-"+lambda+"_iInInit-"+iInInit+"_transSend-"+transSend+"_transSendContent-"+transSendContent+"_diag-"+withDiag+"_withDiagContent-"+withDiagContent+"/last";
			model_name="propagationModels/MLPdiffusion_Dims-"+nbDims+"_step-"+step+"_ratioInits-"+ploader.getRatioInits()+"_nbMaxInits-"+ploader.getNbMaxInits()+"_db-"+ploader.getDb()+"_cascadesCol-"+ploader.getCollection()+"_lambda-"+lambda+"_transSend-"+transSend+"_transSendContent-"+transSendContent+"_diag-"+withDiag+"_withDiagContent-"+withDiagContent+"_withDiagSenders-"+withDiagSenders+"_unbiased"+unbiased+"/MLPdiffusion_"+formater.format(date);
		}
		System.out.println("learn : "+model_name);
		
		if(ploader.getDb().compareTo("test")==0){
			test();	
		}
		else{
			prepareLearning(ploader);
		}
		System.out.println("learn : "+model_name);
		//System.out.println("Directed = "+transSend);
		//System.out.println("ici");
		/*HashSet<String> initUsers=new HashSet<String>();
		if(transSend){
				
				for(PropagationStruct pstruct:this.train_cascades.values()){
					ArrayList<String> cont=pstruct.getArrayInit();
					initUsers.addAll(cont);
				}
		}*/
		
		currentInput=new Tensor(0);
		if(withDiagContent || transSendContent){
			if(withDiagContent && transSendContent){
				currentInput=new Tensor(4);
			}
			else{
				currentInput=new Tensor(2);
			}
		}
		
		int nbStems=maxIdStem; //+((bias)?1:0);
		int nbd=nbDims;
		int nu=0;
		for(String user:users){
			nu++;
			
			if(!user_modules.containsKey(user)){
				CPUParams mod=new CPUParams(1,nbDims);
				mod.setName(user);
				params.allocateNewParamsFor(mod, parInf, parSup); //,(1.0f*nu)/(1.0f*users.size()));
				user_modules.put(user,mod);
			}
			if(transSend){
				if(!translations.containsKey(user)){
					CPUParams mod=new CPUParams(1,nbDims);
					mod.setName(user+"_translation");
					params.allocateNewParamsFor(mod,0.0f, parInf, parSup);
					translations.put(user,mod);
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
		}
		
		
		
		
		if(withDiagContent){
			//if(withDiag){
				if((!loaded) || (diagContent==null)){
					diagContent=new CPUSparseLinear(nbStems,nbd);
					params.allocateNewParamsFor(diagContent,0.0f);
					((CPUSparseLinear)diagContent).majParams();
				}
			/*}
			else{
				if(!loaded){
					diagContent=new CPUSparseLinear(nbStems,nbd);
					params.allocateNewParamsFor(diagContent,1.0f);
					HashSet<Integer> stemsTrain=new HashSet<Integer>();
					for(PropagationStruct pstruct:this.train_cascades.values()){
						TreeMap<Integer,Float> cont=pstruct.getDiffusion();
						stemsTrain.addAll(cont.keySet());
					}
				
					Parameters pars=this.diagContent.getParamList();
					for(int i=0;i<maxIdStem;i++){
						if(!stemsTrain.contains(i)){
							for(int j=0;j<nbd;j++){
								pars.get(Matrix.IDX2C(i, j, nbStems)).setVal(0.0f);
							}
						}
					}
				}
			}*/
			/*if(!loaded){
				((CPUSparseLinear)diagContent).majParams();
			}*/
		}
		
		
		
		if(transSendContent){
			if((!loaded) || (transContent==null)){
				transContent=new CPUSparseLinear(nbStems,nbDims);
				params.allocateNewParamsFor(transContent,0.0f);
				((CPUSparseLinear)transContent).majParams();
			}
		}
        
		//System.out.println("diag "+diag.getParameters());
		seqDiag=new SequentialModule();
		TableModule tabDiag=new TableModule();
		seqDiag.addModule(tabDiag);
		
		boolean diags=false;
		
		if(withDiagContent){
			diags=true;
			tabDiag.addModule(diagContent);
		}
		
		
		if(withDiag){
			diags=true;
			if((!loaded) || (diag==null)){
				diag=new CPUParams(1,nbd);
				params.allocateNewParamsFor(diag,1.0f, diagInf, diagSup);
			}
			tabDiag.addModule(diag);
			if(withDiagContent){
					seqDiag.addModule(new CPUAddVecs(nbd));
				
			}
		}
		
		if(withDiagSenders){
			SequentialModule seqD=new SequentialModule();
			diagSender=new TableModule();
			diagSender.addModule(new CPUParams(1,nbDims)); // On y mettra les diagSenders
			seqD.addModule(diagSender);
			if(diags){
				diagSender.addModule(seqDiag);
				seqD.addModule(new CPUAddVecs(nbd));
			}
			seqDiag=seqD;
		}
		
		
		if(logisticDiag){
			seqDiag.addModule(new CPULogistic(nbd));
		}
		
		
		ArrayList<Double> weights=new ArrayList<Double>();
		weights.add(1.0);
		weights.add(-1.0);
		
		
		zi=new TableModule();
		zi.addModule(new CPUParams(1,nbDims));   
		if(transSend){
			zi.addModule(new CPUParams(1,nbDims));
		}
		
		zj=new TableModule();
		zj.addModule(new CPUParams(1,nbDims));   
		
		zk=new TableModule();
		zk.addModule(new CPUParams(1,nbDims));   
		
		SequentialModule transSendSeq=new SequentialModule();
		transSendSeq.addModule(zi);
		if(transSend){
			ArrayList<Double> w=new ArrayList<Double>();
			w.add(1.0);
			w.add(1.0);
			transSendSeq.addModule(new CPUSum(nbDims,2,w));
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
		
		TableModule  zi_zk=new TableModule();
		zi_zk.setName("zi_zk");
		zi_zk.addModule(transSendContentSeq);
		zi_zk.addModule(zk);
		TableModule zi_zj=new TableModule();
		zi_zj.setName("zi_zj");
		zi2=transSendContentSeq.forwardSharedModule();
		
		zi_zj.addModule(zi2);
		zi_zj.addModule(zj);
		
		
		CPUSum diffik=new CPUSum(nbDims,2,weights);
		SequentialModule sdiffik=new SequentialModule();
		sdiffik.addModule(zi_zk);
		sdiffik.addModule(diffik);
		
		TableModule diagik=new TableModule();
		diagik.addModule(sdiffik);
		if(withDiag || withDiagContent){
			diagik.addModule(seqDiag);
		}
		SequentialModule dik=new SequentialModule();
		dik.addModule(diagik);
		if(withDiag || withDiagContent){
			dik.addModule(new CPUTermByTerm(nbd));
		}
		dik.addModule(new CPUL2Norm(nbd));
		
		CPUSum diffij=new CPUSum(nbDims,2,weights);
		SequentialModule sdiffij=new SequentialModule();
		sdiffij.addModule(zi_zj);
		sdiffij.addModule(diffij);
		
		TableModule diagij=new TableModule();
		diagij.addModule(sdiffij);
		if(withDiag || withDiagContent){
			//diagij.addModule(seqDiag.parametersSharedModule()); 
			diagij.addModule(seqDiag.forwardSharedModule());
		}
		SequentialModule dij=new SequentialModule();
		dij.addModule(diagij);
		if(withDiag || withDiagContent){
			dij.addModule(new CPUTermByTerm(nbd));
		}
		dij.addModule(new CPUL2Norm(nbd));
		
		
		
		TableModule tdiffs=new TableModule();
		tdiffs.addModule(dik);
		tdiffs.addModule(dij);
		CPUSum diffs=new CPUSum(1,2,weights);
		CPUHingeLoss hinge=new CPUHingeLoss();
		
		SequentialModule fonc=new SequentialModule();
		fonc.addModule(tdiffs);
		fonc.addModule(diffs);
		fonc.addModule(hinge);
		tv=new CPUTimesVals(1);
		
		if(unbiased){
			fonc.addModule(tv);
		}
		fonc.addModule(new CPUAverageRows(1));
		global=fonc;
		
		
		
		/*
		if(lambda>0){
			TableModule table=new TableModule();
			table.addModule(global);
			allPars=new CPUParams();
			params.giveAllParamsTo(allPars);
			Module reg=new CPUL2Norm(allPars.getNbParams());
			SequentialModule seqreg=new SequentialModule();
			seqreg.addModule(allPars);
			seqreg.addModule(reg);
			table.addModule(seqreg);
			ArrayList<Float> weights2=new ArrayList<Float>();
			weights2.add(1.0f);
			weights2.add(lambda);
			CPUSum sum=new CPUSum(1,2,weights2);
			global=new SequentialModule();
			global.addModule(table);
			global.addModule(sum);
			
		}*/
		nbFromChangeLock=0;
		if(this.contentLocked){
			if(withDiagContent && (nbContentLocked>0)){
				diagContent.lockParams();
			}
			if(transSendContent && (nbContentLocked>0)){
				transContent.lockParams();
			}
		}
		else{
			if(this.nbContentLocked>0){
				diag.lockParams();
				for(Module mod:user_modules.values()){
					mod.lockParams();
				}
				if(transSend){
					for(Module mod:translations.values()){
						mod.lockParams();
					}
				}
			}
		}
		
		
		nbTripletsInLoss=0;
		
		
		HashSet<String> couplesVus=new HashSet<String>();
		for(int ic:cascades_ids){
			PropagationStruct pstruct=this.train_cascades.get(ic);
			TreeMap<Long,HashMap<String,Double>> set=pstruct.getInitContaminated();
			TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
			TreeMap<Long,LinkedHashMap<String,Double>> cumul=PropagationStruct.getPCumulBeforeT(infections);
			HashMap<String,Long> times=pstruct.getInfectionTimes();
			
			//HashSet<String> vus=new HashSet<String>();
			//HashSet<String> notvus=new HashSet<String>(pstruct.getPossibleUsers());
			HashSet<String> notvusInfected=new HashSet<String>(pstruct.getArrayContamined());
			int nbi=0;
			int nbc=0;
			for(Long t:set.keySet()){
				for(Long t2:infections.keySet()){
					if(t2<=t){
						continue;
					}
					nbTripletsInLoss+=pstruct.getPossibleUsers().size()-cumul.get(t2).size();
				}
			}
		}
		System.out.println(nbTripletsInLoss);
		//System.out.println(zi_zj);
		//System.out.println("la");
		//params=new Parameters();
		
		/*this.diagLocked=true;
		seqDiag.lockParams();
		seqDiag2.lockParams();
		seqDiag3.lockParams();
		seqDiag4.lockParams();*/
		/*diag.lockParams();
		for(Module mod:user_modules.values()){
			mod.lockParams();
		}*/
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
		ArrayList<String> users=new ArrayList<String>(pstruct.getPossibleUsers());
		TreeMap<Long,HashMap<String,Double>> initInfected=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
        ArrayList<String> contaminated = new ArrayList<String>((PropagationStruct.getPBeforeT(infections)).keySet()) ;
        ArrayList<String> initContaminated = new ArrayList<String>((PropagationStruct.getPBeforeT(initInfected)).keySet()) ;
        
        /*while(contaminated.size()<=initContaminated.size()){
        	x=(int)(Math.random()*this.cascades_ids.size()); 
    		c=cascades_ids.get(x);
    		pstruct=this.train_cascades.get(c);
    		initInfected=pstruct.getInitContaminated();
            infections=pstruct.getInfections();
            contaminated = new ArrayList<String>((PropagationStruct.getPBeforeT(infections)).keySet()) ;
            initContaminated = new ArrayList<String>((PropagationStruct.getPBeforeT(initInfected)).keySet()) ;
        }*/
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
		CPUMatrix probas=new CPUMatrix(nbMaxSamples,1);
		while(nbSamples<nbMaxSamples){
			double p=1.0*initContaminated.size();
			String ui="";
			/*if(iInInit){
				x=(int)(Math.random()*initContaminated.size());
				ui=initContaminated.get(x);
			}
			else{
			x=(int)(Math.random()*contaminated.size());
			ui=contaminated.get(x);
			//}*/
			x=(int)(Math.random()*initContaminated.size());
			ui=initContaminated.get(x);
			
	        Long ti=times.get(ui);
	        
	        x=(int)(Math.random()*contaminated.size());
	        String uj=contaminated.get(x);
	        Long tj=times.get(uj);
	        /*if((tj==ti) && (ui.compareTo(uj)!=0)){
       		 continue;
       	 	}*/
	        if(ti==tj){
	        	continue;
	        }
	        p*=(contaminated.size()-infections.get(ti).size());
	        
	        if(tj<ti){
	        	String tmp=ui;
	        	ui=uj;
	        	uj=tmp;
	        	Long ttmp=ti;
	        	ti=tj;
	        	tj=ttmp;
	        }
	        
	        x=(int)(Math.random()*users.size());
	        String uk=users.get(x);
	        if(uk.compareTo(ui)==0){
	        	continue;
	        }
	        if(uk.compareTo(uj)==0){
	        	continue;
	        }
	        p*=(users.size()-infections.get(ti).size()-infections.get(tj).size());
       	 
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
			
			
			// TODO : revoir probas
			probas.setValue(nbSamples,0,((double) (((cascades_ids.size()*1.0)/nbTripletsInLoss)*p)));
	        nbSamples++;
		}
		CPUParams parsi=new CPUParams(listei.size(),nbDims);
		CPUParams parsi_sender=new CPUParams(listei.size(),nbDims);
		CPUParams parsi_diag=new CPUParams(listei.size(),nbDims);
		
		CPUParams parsj=new CPUParams(listej.size(),nbDims);
		CPUParams parsk=new CPUParams(listek.size(),nbDims);
		for(int i=0;i<listei.size();i++){
			String ui=listei.get(i);
			String uj=listej.get(i);
			String uk=listek.get(i);
			//System.out.println("Chosen ui = "+ui);
			//System.out.println("Chosen uj = "+uj);
			//System.out.println("Chosen uk = "+uk);
			
			CPUParams mi=user_modules.get(ui);
			parsi.addParametersFrom(mi);
			lastUsed.put(ui, mi);
			if(transSend){
				mi=translations.get(ui);
				parsi_sender.addParametersFrom(mi);
				lastUsed.put(ui+"_sender", mi);
			}
			if(withDiagSenders){
				mi=diagSenders.get(ui);
				parsi_diag.addParametersFrom(mi);
				lastUsed.put(ui+"_diag", mi);
			}
			CPUParams mj=user_modules.get(uj);
			parsj.addParametersFrom(mj);
			lastUsed.put(uj, mj);
			CPUParams mk=user_modules.get(uk);
			parsk.addParametersFrom(mk);
			lastUsed.put(uk, mk);
		}
		
		zi.setModule(0,parsi);
		if(transSend){
			zi.setModule(1,parsi_sender);
		}
		if(withDiagSenders){
			diagSender.setModule(0, parsi_diag);
			//parsi_diag.forward(new Tensor(0));
			//System.out.println(parsi_diag.getOutput().getMatrix(0));
		}
		zj.setModule(0,parsj);
		zk.setModule(0,parsk);
		
		if(withDiagContent || transSendContent){
			int nbStems=maxIdStem; //+((bias)?1:0);
			CPUSparseMatrix content=new CPUSparseMatrix(1,nbStems);
        
			TreeMap<Integer,Double> cont=pstruct.getDiffusion();
			if(boolContent){
				TreeMap<Integer,Double> ncont=new TreeMap<Integer,Double>();
				for(Integer i:cont.keySet()){
					ncont.put(i, 1.0);
				}
				cont=ncont;
			}
			//stemsVus.addAll(cont.keySet());
			content.setValues(cont);
			/*if(bias){
        	content.setValue(0, nbStems-1, 1.0f);
        	}*/
			//System.out.println(content);
			this.currentInput.setMatrix(0, content);
			this.currentInput.setMatrix(1, content);
			if(withDiagContent && transSendContent){
				this.currentInput.setMatrix(2, content);
				this.currentInput.setMatrix(3, content);
			}
		} 
        
        //System.out.println("content = "+content);
		tv.setVals(probas);
        global.forward(this.currentInput);
       
		nbForwards++;
		nbFromChangeLock++;
		lastLoss=getLossValue();
		sumLoss+=lastLoss;
		sumLossTot+=lastLoss;
		nbSum++;
		if(nbForwards%100==1){

			System.out.println(this.getName()+" Average Loss = "+String.format("%.5f", sumLossTot/(1.0*nbForwards))+", "+String.format("%.5f", sumLoss/(1.0*nbSum))+", "+String.format("%.5f", lastLoss)+" nbSums="+nbSum+" nbForwards="+nbForwards);
			
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
		if((nbContentLocked<=0) || (contentLocked)){
			
			for(Module mod:lastUsed.values()){
				//System.out.println(mod);
				//System.out.println(mod.getParamList());
				
				mod.updateParameters(line);
				mod.paramsChanged();
				nb++;
			}
			
			if(withDiag){
				//System.out.println("Diag");
				//System.out.println(diag.getParamList());
				diag.updateParameters(line);
				
			}
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
		
		/*if(allPars!=null){
			allPars.paramsChanged();
		}*/
		//global.paramsChanged();
		if(nbForwards%1000==0){
			/*if(!locked){
				diag.lockParams();
				for(Module mod:user_modules.values()){
					mod.lockParams();
				}
				diagContent.unlockParams();
				System.out.println("unlock diagContent");
				locked=true;
			}
			else if(locked){
				diag.unlockParams();
				for(Module mod:user_modules.values()){
					mod.unlockParams();
				}
				diagContent.lockParams();
				locked=false;
			}*/
			//if(withDiag){ //Content){
			/*Tensor tensor=new Tensor(1);
			int nbStems=maxIdStem; //+((bias)?1:0);
	        CPUSparseMatrix content=new CPUSparseMatrix(1,nbStems);
	        content.setValue(0, 0, 1.0f);*/
	        /*content.setValue(0, 1, 0.1f);
	        content.setValue(0, 2, 0.1f);
	        content.setValue(0, 3, 0.1f);
	        content.setValue(0, 4, 0.1f);
	        content.setValue(0, 5, 0.1f);
	        content.setValue(0, 6, 0.1f);
	        content.setValue(0, 7, 0.1f);
	        content.setValue(0, 8, 0.1f);
	        content.setValue(0, 9, 0.1f);*/
	        
	        //tensor.setMatrix(0, content);
	        //diagContent.forward(tensor);
	        //System.out.println("content = "+content);
	        //System.out.println("diag out = "+diagContent.getOutput());
	        /*content=new CPUSparseMatrix(1,nbStems);
	        content.setValue(0, 1, 1.0f);
	        tensor.setMatrix(0, content);
	        diagContent.forward(tensor);
	        System.out.println("diag out = "+diagContent.getOutput());*/
	        
			//}
			if(withDiagContent){
				System.out.println("diag out = "+diagContent.getOutput());
			}
	        if(withDiag){
	        	
	        	System.out.println("diag = "+(diag.getOutput()));
	        }
		}
		//if(nbForwards%10000==0){
		if((nbContentLocked>0) && (contentLocked)){
			if(nbFromChangeLock%nbContentLocked==0){
				diag.lockParams();
				for(Module mod:user_modules.values()){
					mod.lockParams();
				}
				if(transSend){
					for(Module mod:translations.values()){
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
				for(Module mod:user_modules.values()){
					mod.unlockParams();
				}
				if(transSend){
					for(Module mod:translations.values()){
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
			
	}
	
	
	
	@Override
	public Parameters getUsedParams(){
		Parameters p=new Parameters();
		if((nbContentLocked<=0) || (contentLocked)){
			
			for(Module mod:lastUsed.values()){
				p.addSubParamList(mod.getParamList());
			}
			if(withDiag){
				p.addSubParamList(diag.getParamList());
			}
		}
		if((!contentLocked)){
			
			if(withDiagContent){
				p.addSubParamList(diagContent.getParamList());
			}
			if(transSendContent){
				p.addSubParamList(transContent.getParamList());
			}
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
        ArrayList<String> users=new ArrayList<String>(user_modules.keySet());
        int tt=1;
        System.out.println("users="+user_modules.keySet().size());
	    
	    for(long t:contaminated.keySet()){
	    	//System.out.println(t);
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	infections.put((long)tt, (HashMap<String,Double>) inf.clone());
	    	//System.out.println(t+"ok");
	    	tt++;
	    }
	    int firstNewT=tt;
	    
	    
	    ArrayList<String> sources=new ArrayList<String>();
	    //ArrayList<String> sourcesSend=new ArrayList<String>();
	    ArrayList<String> receivers=new ArrayList<String>();
	    
	    int nbLignes=0;
	    System.out.println("init="+infected.size());
	    for(String init:infected){
	    	if(user_modules.containsKey(init)){
	    		for(String user:users){
	    			sources.add(init);
	    			//sourcesSend.add(init);
	    			receivers.add(user);
	    			nbLignes++;
	    		}
	    	}
	    }
	    System.out.println("Modules created");
	    if(nbLignes>0){
	    	CPUParams modUs=new CPUParams(1,nbDims);
		    CPUParams modInit=new CPUParams(1,nbDims);
		    CPUParams modInit_send=new CPUParams(1,nbDims);
	    	TableModule tabTransInit=new TableModule();
			tabTransInit.addModule(modInit);
			if(transSend){
				tabTransInit.addModule(modInit_send);
			}
			
			ArrayList<Double> w=new ArrayList<Double>();
			w.add(1.0);
			w.add(1.0);
			SequentialModule sTransInit=new SequentialModule();
			sTransInit.addModule(tabTransInit);
			if(transSend){
				sTransInit.addModule(new CPUSum(nbDims,2,w));
			}
			
			if(transSendContent){
				TableModule tabi=new TableModule();
				tabi.addModule(sTransInit);
				tabi.addModule(transContent);
				SequentialModule seqi=new SequentialModule();
				seqi.addModule(tabi);
				seqi.addModule(new CPUAddVecs(nbDims));
				sTransInit=seqi;
			}
			TableModule zi_zj=new TableModule();
			zi_zj.addModule(sTransInit);
		    zi_zj.addModule(modUs);
	    	
	    	ArrayList<Double> weights=new ArrayList<Double>();
			weights.add(1.0);
			weights.add(-1.0);
			int nbd=nbDims;
			
			
			SequentialModule seqDiag=new SequentialModule();
			TableModule tabDiag=new TableModule();
			if(withDiagContent){
				tabDiag.addModule(diagContent);
			}
			seqDiag.addModule(tabDiag);
			if(withDiag){
				tabDiag.addModule(diag);
				if(withDiagContent){
					seqDiag.addModule(new CPUAddVecs(nbd));
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
			
			CPUSum diffij=new CPUSum(nbDims,2,weights);
			SequentialModule sdiffij=new SequentialModule();
			sdiffij.addModule(zi_zj);
			sdiffij.addModule(diffij);
			
			TableModule diagij=new TableModule();
			diagij.addModule(sdiffij);
			if(withDiag || withDiagContent){
				diagij.addModule(seqDiag);
			}
			SequentialModule dij=new SequentialModule();
			dij.addModule(diagij);
			if(withDiag || withDiagContent){
				dij.addModule(new CPUTermByTerm(nbd));
			}
			dij.addModule(new CPUL2Norm(nbd));
			
			
		    
		    
			
			
		    Tensor tensor=new Tensor(0);
		    if(withDiagContent || transSendContent){
		    	int nbStems=maxIdStem; //+((bias)?1:0);
		        CPUSparseMatrix content=new CPUSparseMatrix(1,nbStems);
		         TreeMap<Integer,Double> cont=pstruct.getDiffusion();
		        if(boolContent){
		        	TreeMap<Integer,Double> ncont=new TreeMap<Integer,Double>();
		        	for(Integer i:cont.keySet()){
		        		ncont.put(i, 1.0);
		        	}
		        	cont=ncont;
		        }
		        content.setValues(cont);
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
		    	tabTransInit.setModule(0, user_modules.get(sources.get(i)));
		    	if(transSend){
		    		tabTransInit.setModule(1, translations.get(sources.get(i)));
		    	}
		    	if(withDiagSenders){
		    		diagSender.setModule(0, diagSenders.get(sources.get(i)));
		    	}
		    	zi_zj.setModule(1, user_modules.get(receivers.get(i)));
		    	dij.forward(tensor);
		    	Tensor res=dij.getOutput();
			    Matrix m=res.getMatrix(0);
			    results.setValue(i, 0, m.getValue(0, 0));
		    }
		       
	       
	        
		    
		    
		    System.out.println("Diffusion done");
		    //System.out.println(res);
		    double max=-1;
		    HashMap<String,Double> minDist=new HashMap<String,Double>();
		    int r=0;
		    TreeMap<Long,HashMap<String,Double>> ref=pstruct.getInfections();
		    HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		    //for(String init:infected){
		    for(int i=0;i<nbLignes;i++){
		    	//if(user_modules.containsKey(init)){
			    	String  receiv=receivers.get(i);
			    	Double v=minDist.get(receiv);
			    	double dist=results.getValue(i, 0);
			    	if((v==null) || (dist<v)){
			    		minDist.put(receiv, dist);
			    			
			    	}
		    }
		    
		    /*for(String user:minDist.keySet()){
		    	System.out.println(user+" => "+(minDist.get(user))+" ref?"+((href.containsKey(user))?"1":"0"));
		    	if(href.containsKey(user)){
		    		Clavier.saisirLigne("");
		    	}
		    }*/
		    
		    for(Double f:minDist.values()){
		    	if(max<f){
		    		max=f;
		    	}
		    }
		    //System.out.println(minDist);
		    if(max<=0){
		    	max=1.0f;
		    }
		    HashMap<String,Double> infstep=new HashMap<String,Double>();
		    	
		    for(String user:minDist.keySet()){
		    	if(!infected.contains(user)){
		    		infstep.put(user, (double)(-minDist.get(user)+max)/max);
		    	}
		    }
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
		    
		    Clavier.saisirLigne("");*/
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
		return "MLPdiffusion_"+nbDims+"  "+((withDiag)?"diag ":" ")+((withDiagContent)?"diagContent ":" ")+((withDiagSenders)?"diagSenders ":" ")+((transSend)?"transSend ":" ")+((transSendContent)?"transSendContent ":" ")+((unbiased)?"unbiased ":" ");
		
	}
	public String toString(){
		return "MLPdiffusion_model_file="+model_file;
	}

	
	
	
	public static void main(String[] args){
		HashMap<String,String> hargs=ArgsParser.parseArgs(args);
		String db=(hargs.containsKey("db"))?hargs.get("db"):"test";
		String cascades=(hargs.containsKey("cascades"))?hargs.get("cascades"):"test";
		cascades=(hargs.containsKey("c"))?hargs.get("c"):"test";
		int nbDims=(hargs.containsKey("nbDims"))?Integer.parseInt(hargs.get("nbDims")):2;
		nbDims=(hargs.containsKey("nD"))?Integer.parseInt(hargs.get("nD")):2;
		boolean withDiag=(hargs.containsKey("withDiag"))?Boolean.valueOf(hargs.get("withDiag")):false;
		withDiag=(hargs.containsKey("wD"))?Boolean.valueOf(hargs.get("wD")):false;
		boolean transSend=(hargs.containsKey("transSend"))?Boolean.valueOf(hargs.get("transSend")):false;
		transSend=(hargs.containsKey("tS"))?Boolean.valueOf(hargs.get("tS")):false;
		boolean withDiagContent=(hargs.containsKey("withDiagContent"))?Boolean.valueOf(hargs.get("withDiagContent")):false;
		withDiagContent=(hargs.containsKey("wDC"))?Boolean.valueOf(hargs.get("wDC")):false;
		boolean transSendContent=(hargs.containsKey("transSendContent"))?Boolean.valueOf(hargs.get("transSendContent")):false;
		transSendContent=(hargs.containsKey("tSC"))?Boolean.valueOf(hargs.get("tSC")):false;
		boolean withDiagSenders=(hargs.containsKey("withDiagSenders"))?Boolean.valueOf(hargs.get("withDiagSenders")):false;
		withDiagSenders=(hargs.containsKey("wDS"))?Boolean.valueOf(hargs.get("wDS")):false;
		boolean unbiased=(hargs.containsKey("unbiased"))?Boolean.valueOf(hargs.get("unbiased")):true;
		unbiased=(hargs.containsKey("u"))?Boolean.valueOf(hargs.get("u")):true;
		boolean logisticDiag=(hargs.containsKey("logisticDiag"))?Boolean.valueOf(hargs.get("logisticDiag")):false;
		logisticDiag=(hargs.containsKey("lD"))?Boolean.valueOf(hargs.get("lD")):false;
		boolean boolContent=(hargs.containsKey("boolContent"))?Boolean.valueOf(hargs.get("boolContent")):false;
		boolContent=(hargs.containsKey("bC"))?Boolean.valueOf(hargs.get("bC")):false;
		double line=(hargs.containsKey("line"))?Double.valueOf(hargs.get("line")):0.02;
		line=(hargs.containsKey("l"))?Double.valueOf(hargs.get("l")):0.02;
		double decFactor=(hargs.containsKey("decFactor"))?Double.valueOf(hargs.get("decFactor")):0.999999;
		decFactor=(hargs.containsKey("dF"))?Double.valueOf(hargs.get("dF")):0.999999;
		String modelFile=(hargs.containsKey("modelFile"))?hargs.get("modelFile"):"";
		modelFile=(hargs.containsKey("mF"))?hargs.get("mF"):"";
		double parInf=(hargs.containsKey("parInf"))?Float.valueOf(hargs.get("parInf")):-1.0f;
		parInf=(hargs.containsKey("pI"))?Float.valueOf(hargs.get("pI")):-1.0f;
		double parSup=(hargs.containsKey("parSup"))?Float.valueOf(hargs.get("parSup")):1.0f;
		parSup=(hargs.containsKey("pS"))?Float.valueOf(hargs.get("pS")):1.0f;
		double diagInf=(hargs.containsKey("diagInf"))?Float.valueOf(hargs.get("diagInf")):0.0001f;
		diagInf=(hargs.containsKey("dI"))?Float.valueOf(hargs.get("dI")):0.0001f;
		double diagSup=(hargs.containsKey("diagSup"))?Float.valueOf(hargs.get("diagSup")):10.0f;
		diagSup=(hargs.containsKey("dS"))?Float.valueOf(hargs.get("dS")):10.0f;
		double ratioInits=(hargs.containsKey("rI"))?Double.valueOf(hargs.get("rI")):1.0;
		int nbMaxInits=(hargs.containsKey("nbI"))?Integer.valueOf(hargs.get("nbI")):-1;
		
		MLPdiffusion mlp;
		if(modelFile.length()==0){
			mlp=new MLPdiffusion(nbDims,2000,boolContent,logisticDiag,withDiag,withDiagContent,transSend,transSendContent,withDiagSenders); 
		}
		else{
			mlp=new MLPdiffusion(modelFile); 
			mlp.load();
			mlp.withDiag=withDiag;
			mlp.withDiagContent=withDiagContent;
			mlp.transSend=transSend;
			mlp.transSendContent=transSendContent;
			mlp.withDiagSenders=withDiagSenders;
		}
		mlp.parInf=parInf;
		mlp.parSup=parSup;
		mlp.diagInf=diagInf;
		mlp.diagSup=diagSup;
		mlp.majParBounds();
		
		Env.setVerbose(0);
		//MLPdiffusion mlp=new MLPdiffusion("psauv/MLPDiagContent2_Dims-100_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0_iInInit-true_senderReceiver-false/last");
		/*
		*/
		Optimizer opt=Optimizer.getDescent(DescentDirection.getAverageGradientDirection(), LineSearch.getFactorLine(line,decFactor));
		//opt.optimize(mlp);
		PropagationStructLoader ploader=new PropagationStructLoader(db,cascades,(long)1,ratioInits,nbMaxInits);
		
		//mlp.load();
		mlp.learn(ploader, opt, 0.00f,unbiased); //,"propagationModels/MLPDiagContent_Dims-200_step-1_nbInit-1_db-diggPruned_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0/MLPDiagContent_18.09.2013_14.19.12_20.09.2013_8.28.55");
		
		mlp.save();
	}
	
	
	/*
		
		try{
			if(args.length==0){
				args=new String[10];
				args[0]="test";
				args[1]="test";
				args[2]="2";
				args[3]="true";
				args[4]="true";
				args[5]="false";
				args[6]="false";
				args[7]="false";
				args[8]="false";
				args[9]="false";
				args[10]="0.02";
				args[11]="0.999999";
				
			}
			MLPdiffusion mlp;
			try{
				int nbDims=Integer.parseInt(args[2]);
				mlp=new MLPdiffusion(nbDims,2000,false,false,Boolean.valueOf(args[3]),Boolean.valueOf(args[4]),Boolean.valueOf(args[5]),Boolean.valueOf(args[6]),Boolean.valueOf(args[7]),Boolean.valueOf(args[8])); 
					
			}
			catch(Exception e){
				mlp=new MLPdiffusion(args[2]); 
				mlp.load();
				if(args.length>3){
					mlp.withDiag=Boolean.valueOf(args[3]);
					mlp.withDiagContent=Boolean.valueOf(args[4]);
					mlp.transSend=Boolean.valueOf(args[5]);
					mlp.transSendContent=Boolean.valueOf(args[6]);
					mlp.withDiagSenders=Boolean.valueOf(args[7]);
					mlp.unbiased=Boolean.valueOf(args[8]);
				}
				//mlp.model_file="";
			}
			Env.setVerbose(0);
			//MLPdiffusion mlp=new MLPdiffusion("psauv/MLPDiagContent2_Dims-100_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0_iInInit-true_senderReceiver-false/last");
			
			Optimizer opt=Optimizer.getDescent(DescentDirection.getAverageGradientDirection(), LineSearch.getFactorLine(Float.valueOf(args[10]),Float.valueOf(args[11])));
			//opt.optimize(mlp);
			
			//mlp.load();
			mlp.learn(args[0], args[1], (long)1, 1, opt, 0.00f,Boolean.valueOf(args[9])); //,"propagationModels/MLPDiagContent_Dims-200_step-1_nbInit-1_db-diggPruned_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0/MLPDiagContent_18.09.2013_14.19.12_20.09.2013_8.28.55");
			
			mlp.save();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}*/
}
