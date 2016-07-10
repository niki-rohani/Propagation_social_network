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
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;






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

import java.util.TreeMap;

public class NetRate extends MLP {
	private static final long serialVersionUID = 1L;
	private HashMap<String,HashMap<String,CPUParams>> alphas;
	private HashMap<String,HashMap<String,SequentialModule>> logSurvivals;
	private HashMap<String,HashMap<String,SequentialModule>> hazards;
	//private HashMap<String,HashMap<String,Double>> probas;
	private HashMap<String,CPUParams> usedModules;
	SequentialModule term1;
	SequentialModule term2;
	//SequentialModule term3;
	private double sumLoss=0.0;
	private double lastLoss=0.0;
	private double sumLossTot=0.0;
	private int nbForwards=0;
	private int nbEstimations=10000;
	private int nbAffiche=1000;
	private int freqSave=10000;
	private int nbF=0;
	private double best=Double.NaN;
	private int nbSum=0;
	private Tensor currentInput;
	private long maxT=-2;
	private int nbSimul=1;
	private long deltaMin=1;
	private int nbSamples=-1;
	private long nbInferSteps=10;
	/**
	 * Law used in the original paper.
	 * 1 => exponential
	 * 2 => power
	 * 3 => raigley
	 * 4 => discrete uniform (doesn't exist in the paper) 
	 */
	private int law=1; 
	private int inferMode=1;
	public NetRate(){
		super("");
		alphas=new  HashMap<String,HashMap<String,CPUParams>>();
		logSurvivals=new  HashMap<String,HashMap<String,SequentialModule>>();
		hazards=new HashMap<String,HashMap<String,SequentialModule>>();
	}
	
	public NetRate(int law){
		this();
		this.law=law;
		
	}
	
	public NetRate(String model_file, long nbInferSteps, int inferMode, int nbSimul){
		super(model_file);
		
		this.nbSimul=nbSimul;
		this.nbInferSteps=nbInferSteps;
		this.inferMode=inferMode;
	}
    public NetRate(String model_file){
		this(model_file,10,1,1);
		
	}
	
    public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(this.alphas.keySet());
	}
	public int getContentNbDims(){
		if(!loaded){
			load();
		}
		return 0;
	}
	
	public HashMap<String,HashMap<String,CPUParams>> getAlphas(){
		return alphas;
	}
	
	
	// returns a sequentialModule coding for log(survival j=>i)
	public SequentialModule getLogSurvival_ji(String uj,String ui,long tj,long ti){
		HashMap<String,SequentialModule> mj=logSurvivals.get(uj);
		
		if(mj==null){
			//System.out.println("logSurvivals null de "+uj+" "+alphas.get(uj).size());
			return null;
		}
		/*else{
			System.out.println("logSurvivals de "+uj+" "+mj.size());
		}*/
		SequentialModule ret=mj.get(ui);
		//ret=(SequentialModule)ret.forwardSharedModule();
		if(ret==null){
			return null;
		}
		CPUMatrix mat=new CPUMatrix(1,1);
		double v=0.0;
		
		if(law==1){
			if(ti>tj){
				v=tj-ti;
			}
		}
		else if (law==2){
			if(ti>(tj+deltaMin)){
				v=-Math.log((ti-tj)/deltaMin);
			}
		}
		else if (law==3){
			if(ti>tj){
				v=(ti-tj)/2.0;
				v=-v*v;
			}
		}
		else if (law==4){
			if(ti>(tj+1)){
				v=-((ti-tj-1)/(maxT-tj));
			}
		}
		mat.setValue(0, 0, v);
		((CPUTimesVals)ret.getModule(1)).setVals(mat);
		
		
		return ret;
	}
	
	public SequentialModule getHazard_ji(String uj,String ui,long tj,long ti){
		HashMap<String,SequentialModule> mj=hazards.get(uj);
		if(mj==null){
			return null;
		}
		SequentialModule ret=mj.get(ui);
		if(ret==null){
			return ret;
		}
		CPUMatrix mat=new CPUMatrix(1,1);
		double v=0.0;
		
		if(law==1){
			if(ti>tj){
				v=1.0;
			}
		}
		else if (law==2){
			if(ti>(tj+deltaMin)){
				v=1.0/(ti-tj);
			}
		}
		else if (law==3){
			if(ti>tj){
				v=(ti-tj);
			}
		}
		else if (law==4){
			if(ti>tj){
				v=1.0/(maxT-tj);
			}
		}
		mat.setValue(0, 0, v);
		((CPUTimesVals)ret.getModule(1)).setVals(mat);
		
		
		return ret;
	}
	
	
	public String toString(){
		String sm=model_file.replaceAll("/", "_");
		return("NetRate_inferMode="+inferMode+"_maxT="+maxT+"_nbSimul="+nbSimul+"_"+sm);
	}
	
	public int infer(Structure struct) {
		
		return(inferProbas(struct));
		
	}
	
	public int inferSimulation(Structure struct){
		inferMode=0;
		return(inferProbas(struct));
	}
	
	public int inferProbas(Structure struct){
		
		
		if(!loaded){
			System.out.println("Load Model...");
			load();
			makeStructs();
		}
		
		if(this.nbInferSteps>maxT){
			this.nbInferSteps=maxT;
		}
		
		
		System.out.println("Inference...");
		//System.out.println("bias = "+bias);
		//System.out.println("directed = "+sender_receiver);
		//System.out.println("logistic = "+logisticDiag);
		PropagationStruct pstruct = (PropagationStruct)struct ;
        		
		
		Tensor tensor=new Tensor(0);
	    
	    this.currentInput=tensor;
		
		TreeMap<Long,HashMap<String,Double>>  moyContaminated=new TreeMap<Long,HashMap<String,Double>> ();
		for(int i=0;i<nbSimul;i++){
			System.out.print(i+" ");
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
		System.out.println(" ");
		
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
        	makeStructs();
        }
    	PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashSet<String> infectedBefore = new HashSet<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        HashSet<String> usersH=new HashSet<String>(train_users.keySet());
        HashMap<String,Double> contagious=new HashMap<String,Double>();
        HashSet<String> inits=new HashSet<String>();
        HashMap<String,Long> times=new HashMap<String,Long>();
        
        long maxt=0;
        //TreeSet<Long> cTimes=new TreeSet<Long>();
	    for(long t:contaminated.keySet()){
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	
	    	HashMap<String,Double> infectedstep=new HashMap<String,Double>();
	    	infections.put(t, infectedstep);
	    	for(String user:inf.keySet()){
	    		contagious.put(user, 1.000001);
	    		infectedBefore.add(user);
	    		infectedstep.put(user, 1.000001);
	    		inits.add(user);
	    		usersH.remove(user);
	    		times.put(user,t);
	    	}
	    	maxt=t;
	    	
	    	//cTimes.add(t);
	    }
	    
	    //HashMap<String,Double> lastcontagious=(HashMap<String,Double>)contagious.clone();
        HashMap<String,Double> infectedstep=new HashMap<String,Double>();
        User currentUser ;
        //int it=tt;
        
        //long it=1;
        long oldTi=maxt;
        boolean ok=true;
        long maxIter=maxT;
        if(maxIter<0){
        	maxIter=100;
        }
        
        long stepSize=Math.round(maxIter/this.nbInferSteps);
        
        long ti=oldTi;
        long firstNewT=ti+stepSize;
       
        while(ti<=maxIter){
        	oldTi=ti;
        	ti+=stepSize;
        	
        	HashMap<String,Double> infectedStep=new HashMap<String,Double>();
    		//infections.put(ti, infectedStep);
    		
        	for(String contagiousU : contagious.keySet()) {
        		Long tj=times.get(contagiousU);
        		HashMap<String,CPUParams> au=alphas.get(contagiousU);
        		if(au==null){
        			continue;
        		}
                for(String user:au.keySet()){ //get(contagiousUser.getID()).keySet()) {
                    if(infectedBefore.contains(user)){
                    	continue;
                    }
                	Double p=getProba(contagiousU,user,tj,oldTi,ti);
                    if(p==null){
                    	continue;
                    }
                    /*if(p<0.1){
                    	continue;
                    }*/
                    if(Math.random()<p) {
                        	infectedStep.put(user, 1.0);
                        	infectedBefore.add(user);
                        	times.put(user,ti);
                        	System.out.println(contagiousU+"=>"+user+"="+p);
                        	
                    }	
                }
            }
        	
        	if(inferMode<2){
        		infections.put(ti,infectedStep);
        	}
        	contagious=infectedStep;
        	
            if(contagious.isEmpty())
                break ; 
        	
        }
        /*if(ti>firstNewT){
        	System.out.println("nb conta = "+infectedBefore.size());
        }*/
        if(this.inferMode>=1){
        	long nt=ti;
        	if(inferMode==2){
        		nt=firstNewT;
        	}
        	//System.out.println("ti="+ti);
	        HashMap<String,Double> notYet=new HashMap<String,Double>();
	       
	        for(String user : infectedBefore) {
	        	//System.out.println(user + "infectedBefore");
	        	Long tu=times.get(user);
	        	HashMap<String,CPUParams> au=alphas.get(user);
	        	if(au==null){
        			continue;
        		}
	            for(String v : au.keySet()){ //get(contagiousUser.getID()).keySet()) {
	                if(((inferMode!=2) && (infectedBefore.contains(v))) || ((inferMode==2) && ((v.equals((user)) || (inits.contains(v))))))
	                    continue ;
	                Double p=notYet.get(v);
	                p=(p==null)?1.0:p;
	                Double pp=getProba(user,v,tu,oldTi,ti);
	                pp=(pp==null)?0.0:pp;
	                p*=(1.0-pp);
	                if(pp>1.0){
	                	System.out.println(v + " => "+pp);
	                }
	                notYet.put(v,p);
	                //
	                
	            }
	        }
	        infectedstep=new HashMap<String,Double>();
	        for(String user:notYet.keySet()){
	        	double p=1.0-notYet.get(user);
	        	p*=0.99999;
	        	
	        	infectedstep.put(user,p);
	        	if(p>1.0){
	        		System.out.println(user+" => "+p);
	        	}
	        	//System.out.println("fin "+ user + " => "+(1.0-notYet.get(user)));
	        	//System.out.println(user + " : "+(1.0-notYet.get(user)));
	        }
	        
	       
	        infections.put(nt,infectedstep);
        }
        //infections.add(infectedstep);
       
        pstruct.setInfections(infections) ;
        return 0;
    }
	
	public Double getProba(String from, String to, long tfrom, long inf, long sup){
		
        SequentialModule favt=this.getLogSurvival_ji(from, to, tfrom, inf);
        /*if(favt==null){
        	//System.out.println("null avt from "+from+"("+tfrom+") to "+to+"("+inf+")");
        }
        else{
        	System.out.println("avt ok from "+from+"("+tfrom+") to "+to+"("+inf+")");
        }*/
        Double x=0.0;
        if(favt!=null){
        	favt.forward(new Tensor(0));
        	x=favt.getOutput().getMatrix(0).getValue(0, 0);
        }
        SequentialModule fnow=this.getLogSurvival_ji(from, to, tfrom, sup);
        /*if(fnow==null){
        	//System.out.println("null now from "+from+"("+tfrom+") to "+to+"("+sup+")");
        }
        else{
        	System.out.println("now ok from "+from+"("+tfrom+") to "+to+"("+sup+")");
        }*/
        Double y=0.0;
        if(fnow!=null){
        	fnow.forward(new Tensor(0));
        	y=fnow.getOutput().getMatrix(0).getValue(0, 0);
        }
        x=(x==null)?0:x;
        y=(y==null)?0:y;
        x=Math.exp(x);
        y=Math.exp(y);
        double ret=((1.0-y)-(1.0-x));
        if(ret<0){
        	System.out.println("ret negatif :"+ret+" x="+x+" y="+y+" from="+from+"("+tfrom+") to "+to+"("+inf+","+sup+")");
        }
        return ret;
	}
	
	 
	
	
   
	
	
    public void load(){
    	
    	String filename=model_file;
        System.out.println("Load "+model_file);
    	User.reinitAllLinks();
        BufferedReader r;
        alphas=new  HashMap<String,HashMap<String,CPUParams>>();
		logSurvivals=new  HashMap<String,HashMap<String,SequentialModule>>();
		hazards=new HashMap<String,HashMap<String,SequentialModule>>();
        train_users=new HashMap<String,HashMap<Integer,Double>>();
        params=new Parameters();
        
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          boolean params_mode=false;
          boolean profiles=false;
          String[] sline;//=line.split("=");
          
          while((line=r.readLine()) != null) {
        	if((line.length()==0) || (line.compareTo(" ")==0)){
        		continue;
        	}
        	
        	if(line.contains("<Params>")){
        		 params_mode=true;
                  continue;
          	}
          	if(line.contains("</Params>")){
                  params_mode=false;
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
        	if(params_mode){  
        		
        		String[] tokens = line.split("\t") ;
	            String user1=tokens[0];
	            String user2=tokens[1];
	            String val=tokens[2];
        		
	            
	            CPUParams cpuPars=new CPUParams(1,1);
	            params.allocateNewParamsFor(cpuPars, Double.valueOf(val),0.0,1.0);
	            HashMap<String,CPUParams> au=alphas.get(user1);
	            if(au==null){
	            	au=new HashMap<String,CPUParams>();
	            	alphas.put(user1,au);
	            }
	            au.put(user2, cpuPars);
	            cpuPars.paramsChanged();
	            //System.out.println(user+" : "+cpuPars.getParamList());
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
        	
        	/*if(line.startsWith("iInInit")){
        		sline=line.split("=");
                iInInit=Boolean.valueOf(sline[1]);
                continue;
        	}*/
        	
        	if(line.startsWith("law")){
            	  sline=line.split("=");
            	  law=Integer.valueOf(sline[1]);
            	  continue;
            }
        	if(line.startsWith("deltaMin")){
          	  sline=line.split("=");
          	  deltaMin=Long.valueOf(sline[1]);
          	  continue;
            }
        	if(line.startsWith("maxT")){
            	if(this.maxT==-2){
            		sline=line.split("=");
            		maxT=Long.valueOf(sline[1]);
            	}
                continue;
            }
        	
          }
          
          r.close();
          
          //System.out.println("Sim="+sim);
          //System.out.println("Sim="+sim);
          
          loaded=true;
          
        }
        catch(IOException e){
        	throw new RuntimeException("Load model => Probleme lecture modele "+filename+"\n "+e);
        }

    }

    @Override
	public void save(){ 
    	File f=new File(model_name);
		File dir=f.getParentFile();
		File fileOut=new File(dir.getAbsolutePath()+"/last");
    	
		model_file=fileOut.getAbsolutePath(); //model_name+"_"+last_save;
		PrintStream p = null;
		double loss=(sumLoss*1.0)/(nbSum*1.0);
        try{
        	File file=new File(model_file);
        	dir=file.getParentFile();
        	if(dir!=null){
        		dir.mkdirs();
        	}
        	
          p = new PrintStream(fileOut) ;
          p.println("Loss="+(sumLoss*1.0)/(nbSum*1.0));
          //p.println("iInInit="+iInInit);
          p.println("law="+law);
          p.println("deltaMin="+this.deltaMin);
          p.println("maxT="+maxT);
          p.println("<Params>");
          StringBuilder sb;
          Parameters pars;
          for(String user:alphas.keySet()){
        	  sb=new StringBuilder();
        	  HashMap<String,CPUParams> h=alphas.get(user);
        	  for(String v:h.keySet()){
        		  CPUParams mod=h.get(v);
        		  pars=mod.getParamList();
        		  sb.append(user);
        		  sb.append("\t");
        		  sb.append(v);
        	      sb.append("\t");
        	      sb.append(pars.get(0).getVal());
        	      sb.append("\n");
        	  }
        	  p.println(sb.toString());
  		  }
          p.println("</Params>");
          
          
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

	private void makeStructs(){
		logSurvivals=new  HashMap<String,HashMap<String,SequentialModule>>();
		hazards=new HashMap<String,HashMap<String,SequentialModule>>();
		for(String uj:alphas.keySet()){
			HashMap<String,CPUParams> au=alphas.get(uj);
			HashMap<String,SequentialModule> su=logSurvivals.get(uj);
			if(su==null){
				su=new HashMap<String,SequentialModule>();
				logSurvivals.put(uj, su);
			}
			HashMap<String,SequentialModule> hu=this.hazards.get(uj);
			if(hu==null){
				hu=new HashMap<String,SequentialModule>();
				hazards.put(uj, hu);
			}
			
			for(String ui:au.keySet()){
				CPUParams a=au.get(ui);
				SequentialModule s=su.get(ui);
				if(s==null){
					s=new SequentialModule();
					s.addModule(a);
					s.addModule(new CPUTimesVals(1,0.0));
					if(law==4){
						s.addModule(new CPUAddVals(1,1.0));
						s.addModule(new CPULog(1));
					}
					su.put(ui, s);
				}
				SequentialModule h=hu.get(ui);
				if(h==null){
					h=new SequentialModule();
					h.addModule(a.parametersSharedModule());
					h.addModule(new CPUTimesVals(1,0.0));
					hu.put(ui, h);
				}
			}
		}
		System.out.println("nb logSurvivals : "+logSurvivals.size());
	}
    
	public void learn(PropagationStructLoader ploader,Optimizer optim){
		
		if(model_file.length()!=0){
			if(!loaded){load();}
		}
		
		//this.iInInit=iInInit;
		String format = "dd.MM.yyyy_H.mm.ss";
		
		//contentLocked=false;
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		if(model_name.length()==0){
			model_name="propagationModels/NetRate_step-"+ploader.getStep()+"_ratioInits-"+ploader.getRatioInits()+"_nbMaxInits-"+ploader.getNbMaxInits()+"_db-"+ploader.getDb()+"_cascadesCol-"+ploader.getCollection()+"_law-"+law+"/NetRate_"+formater.format(date);
		}
		System.out.println("learn : "+model_name);
		
		prepareLearning(ploader);
		
		for(Integer casc: train_cascades.keySet()){
			PropagationStruct ps=(PropagationStruct)train_cascades.get(casc);
			TreeMap<Long,HashMap<String,Double>> inf=ps.getInfections();
			HashSet<String> prev=new HashSet<String>();
			for(Long t:inf.keySet()){
				if(t>maxT){
					maxT=t;
				}
				HashMap<String,Double> vs=inf.get(t);
				for(String u:prev){
					
					HashMap<String,CPUParams> au=this.alphas.get(u);
					if(au==null){
						au=new HashMap<String,CPUParams>();
						alphas.put(u, au);
					}
					for(String v:vs.keySet()){
						CPUParams a=au.get(v);
						if(a==null){
							a=new CPUParams(1,1);
							this.params.allocateNewParamsFor(a,0.000001,0.999999);
							au.put(v, a);
						}
						
					}
				}
				prev.addAll(vs.keySet());
			}
		}

		makeStructs();
		
		currentInput=new Tensor(0);
		
		TableModule table=new TableModule();
		term1=new SequentialModule(); // term1 corresponds to term1 and 2 in the paper
		term2=new SequentialModule(); // term2 corresponds to term3 in the paper
		table.addModule(term1);
		table.addModule(term2);
		
		term1.addModule(null); // on y mettra les logs de survivals
		term1.addModule(null); // on y mettra un CPUSum
		
		
		term2.addModule(null); // on y mettra les logs de sommes de hazards
		term2.addModule(null); // on y mettra un CPUSum

		global=new SequentialModule();
		global.addModule(table);  
		global.addModule(new CPUSum(1,2));
		global.addModule(new CPUTimesVals(1,-1.0f));
		
		params.setMaxMove(0.1);
		
		
		optim.optimize(this);
		
		loaded=true;
	
   
	}
	
	public void forward(){
		
			forward_cascade();
		
	}
	public void forward_cascade() {
		nbF++;
		// choose cascade
		int x=(int)(Math.random()*this.cascades_ids.size()); 
		int c=cascades_ids.get(x);
		
		usedModules=new HashMap<String,CPUParams>();
				
		PropagationStruct pstruct=this.train_cascades.get(c);
		LinkedHashSet<String> users=pstruct.getPossibleUsers();
		
		TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();   
	    TreeMap<Long,ArrayList<String>> cumul=PropagationStruct.getListBeforeT(infections);
	    HashMap<String,Long> times=pstruct.getInfectionTimes();     
	    
	    TableModule tab1=getTableLogSurvivals(times,users);
	    int nbM=tab1.getModules().size();
	    if(nbM>0){
	    	term1.setModule(0, tab1);
	    	term1.setModule(1,new CPUSum(1,nbM));
	    }
	    else{
	    	term1.setModule(0, new CPUParams(1,1,1.0));
	    	term1.setModule(1,new CPUSum(1,1));
	    }
	    TableModule tab2=getTableLogsSumHazards(times,cumul);
	    nbM=tab2.getModules().size();
	    if(nbM>0){
	    	term2.setModule(0, tab2);
	    	term2.setModule(1,new CPUSum(1,nbM));
	    }
	    else{
	    	term2.setModule(0, new CPUParams(1,1,1.0));
	    	term2.setModule(1,new CPUSum(1,1));
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
		
		if(nbForwards%nbAffiche==0){ 
			System.out.println(this.getName()+" Average Loss = "+String.format("%.5f", sumLossTot/(1.0*nbForwards))+", "+String.format("%.5f", sumLoss/(1.0*nbSum))+", "+String.format("%.5f", lastLoss)+" nbSums="+nbSum+" nbForwards="+nbForwards);
			
		}
		
		if(nbForwards%(freqSave)==0){
				save();
		}
		if(nbForwards%nbEstimations==0){
			//System.out.println("Average Loss = "+sumLoss/(1.0*nbSum));
			sumLoss=0;
			nbSum=0;
			System.out.println("reinit sum");
		}
	    
	    
	}
	
	private TableModule getTableLogsSumHazards(HashMap<String,Long> times, TreeMap<Long,ArrayList<String>> cumul){
		TableModule ret=new TableModule();
		for(String ui:times.keySet()){
			Long ti=times.get(ui);
			ArrayList<String> at_ti=cumul.get(ti);
			TableModule tab=new TableModule();
			for(String uj:at_ti){
				Long tj=times.get(uj);
				if(tj<ti){
					tab.addModule(getHazard_ji(uj,ui,tj,ti));
				}
			}
			int nbM=tab.getModules().size();
			if(nbM>0){
				SequentialModule seq=new SequentialModule();
				seq.addModule(tab);
				seq.addModule(new CPUSum(1,nbM));
				seq.addModule(new CPULog(1));
				ret.addModule(seq);
				
			}
		}
		return ret;
	}
	
	private TableModule getTableLogSurvivals(HashMap<String,Long> times, HashSet<String> users){
		TableModule tab=new TableModule();
		int i=0;
		for(String uj:times.keySet()){
			HashMap<String,CPUParams> h=this.alphas.get(uj);
			if(h==null){
				continue;
			}
			Long tj=times.get(uj);
			for(String ui:h.keySet()){
				if(!users.contains(ui)){
					continue;
				}
				Long ti=times.get(ui);
				if((ti==null) || (tj<ti)){
					if(ti==null){
						ti=this.maxT;
					}
					usedModules.put(uj+"=>"+ui,h.get(ui));
					SequentialModule s=null;
					if(ti!=null){
						s=getLogSurvival_ji(uj,ui,tj,ti);
					}
					else{
						s=getLogSurvival_ji(uj,ui,tj,this.maxT);
					}
					tab.addModule(s);
					i++;
				}
			}
		}
		
		return tab;
	}
	
 	
	
	
	public String getName(){
		return "NetRate_law-"+law;
		
	}
    
    
    public void backward()
	{
		global.backward_updateGradient(this.currentInput);
	}
    public void updateParams(double line){
    	for(Module mod:this.usedModules.values()){
    		mod.updateParameters(line);
    	}
    	global.paramsChanged();
	}
	
	
	
    @Override
	public Parameters getUsedParams(){
		Parameters p=new Parameters();
		for(Module mod:this.usedModules.values()){
				p.addSubParamList(mod.getParamList());
		}
		
		
		return p;
	}

   
   
   
   
   
   
    public static void main(String[] args){
    	Env.setVerbose(0);
		NetRate mod=new NetRate(1);
		Optimizer opt=Optimizer.getDescent(DescentDirection.getGradientDirection(), LineSearch.getFactorLine(Double.valueOf(args[2]),0.99999));
		/*if(args.length>3){
			mod.model_file=args[3];
			//mod.load();
		}*/
		PropagationStructLoader ploader=new PropagationStructLoader(args[0],args[1],(long)1,1.0,-1,1,Integer.parseInt(args[3]));
		
		mod.learn(ploader, opt);
		
    }
    
    

}
