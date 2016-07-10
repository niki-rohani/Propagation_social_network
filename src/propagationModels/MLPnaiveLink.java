package propagationModels;

import java.io.BufferedReader;
import java.util.LinkedHashMap;

import mlp.CPUPower;
import mlp.CPUMatrix;
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

import mlp.CPUAddVals;
//import trash.ArtificialCascadesLoader;
import utils.CopyFiles;
import utils.Keyboard;
import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import mlp.CPUTimesVals;
import mlp.CPUExp;
import mlp.CPULog;
import core.Link;
import core.Post;
import core.Structure;
import core.User;
import cascades.Cascade ;
import cascades.CascadesProducer;
import cascades.IteratorDBCascade;

import java.util.TreeMap;

import mlp.DescentDirection;
import mlp.Env;
import mlp.LineSearch;
import mlp.Matrix;
import mlp.CPUAddVecs;
import mlp.CPUAverageRows;
import mlp.CPUHingeLoss;
import mlp.CPUL2Norm;
import mlp.CPULogistic;
import mlp.CPUParams;
import mlp.CPUSparseLinear;
import mlp.CPUSparseMatrix;
import mlp.CPUSum;
import mlp.CPUTermByTerm;
import mlp.Module;
import mlp.Optimizer;
import mlp.Parameters;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.Tensor;
public class MLPnaiveLink extends MLP {
	private static final long serialVersionUID = 1L;
	
	protected HashMap<String,HashMap<String,CPUParams>> probas;
	

	private transient CPUParams allPars;
	//private boolean diagContentLocked=true;
	private double sumLoss=0.0f;
	private int nbForwards=0;
	private int nbSum=0;
	private int nbEstimations=500000;
	private int freqSave=500000;
	private Tensor currentInput;
	private double lastLoss=0.0f;
	private CPUAverageRows av;
	//private int nbInitSteps=1;
	private double sumLossTot=0;
	private HashMap<String,Module> lastUsed;
	HashMap<String,HashSet<Integer>> senders;
	ArrayList<String> senderList;
	double best=0.0f;
	int nbYes=0;
	int nbNot=0;
	double sumNot=0.0;
	double sumYes=0.0;
	int nbCouplesInLoss=0;
	//private Random r;
	private long maxT;

	private int nbMaxSamples=-1;
	//private boolean loaded=false;
	//private boolean inferProbas=true;
	
	public MLPnaiveLink(){
		this("");
		
	}
	
	public MLPnaiveLink(String model_file){
		super(model_file);	
	}
	
	public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(this.senders.keySet());
	}
	public int getContentNbDims(){
		return 0;
	}
	
	public String toString(){
		String sm=model_file.replaceAll("/", "_");
		return("MLPnaiveLink_"+sm);
	}
	
	public void load(){
		String filename=model_file;
		probas = new HashMap<String,HashMap<String,CPUParams>>();
        //User.reinitAllLinks();
        BufferedReader r;
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          String[] sline;
          boolean probas_mode=false;
          int nb=0;
          while((line=r.readLine()) != null) {
        	/*if(line.startsWith("iInInit")){
          		sline=line.split("=");
                  iInInit=Boolean.valueOf(sline[1]);
                  continue;
          	}*/
        	if(line.contains("<Infections_Probas>")){
       		 	 probas_mode=true;
                 continue;
         	}
         	if(line.contains("</Infections_Probas>")){
                  probas_mode=false;
         		  continue;
         	}
         	if(probas_mode){
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
		    	pi.put(j,new CPUParams(1,1,(double)d,0.00001f, 0.99999f));
		    	nb++;
         	}
          }
          r.close();
          loaded=true;
          System.out.println(nb+" probas chargees");
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
		double loss=(double)(sumLoss*1.0f)/(nbSum*1.0f);
        try{
        	File file=new File(model_file);
        	dir=file.getParentFile();
        	if(dir!=null){
        		dir.mkdirs();
        	}
        	
        	p = new PrintStream(fileOut) ;
        	p.println("Loss="+(sumLoss*1.0)/(nbSum*1.0));
        	
        	//p.println("iInInit="+iInInit);
        	p.println("<Infections_Probas>");
			for(String i : probas.keySet()){
				HashMap<String,CPUParams> pi=probas.get(i);
				for(String j:pi.keySet()){
					p.println(i+"\t"+j+"\t"+pi.get(j).getParamList().get(0).getVal()) ;
				}
			}
			p.println("</Infections_Probas>");
			
				
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
		String format = "dd.MM.yyyy_H.mm.ss";
		
		//contentLocked=false;
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		if(model_name.length()==0){
			model_name="propagationModels/MLPnaiveLink_step-"+step+"_ratioInits-"+ploader.getRatioInits()+"_nbMaxInits-"+ploader.getNbMaxInits()+"_db-"+ploader.getDb()+"_cascadesCol-"+ploader.getCollection()+"/MLPnaiveLink_"+formater.format(date);
		}
		System.out.println("learn : "+model_name);
		
		
		prepareLearning(ploader);
		if(!loaded){
			probas=new HashMap<String,HashMap<String,CPUParams>>();
		}
		for(Integer c:cascades_ids){
			PropagationStruct pstruct=this.train_cascades.get(c);
			TreeMap<Long,HashMap<String,Double>> initInfected=pstruct.getInitContaminated();
        	TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
        	ArrayList<String> contaminated = new ArrayList<String>((PropagationStruct.getPBeforeT(infections)).keySet()) ;
        	ArrayList<String> initContaminated = new ArrayList<String>((PropagationStruct.getPBeforeT(initInfected)).keySet()) ;
			HashMap<String,Long> times=pstruct.getInfectionTimes();
			ArrayList<String> inits=initContaminated;
			
			for(String ui:inits){
				HashMap<String,CPUParams> pars=probas.get(ui);
				if(pars==null){
						pars=new HashMap<String,CPUParams>();
						probas.put(ui, pars);
				}				
				
				long ti=times.get(ui);
				for(String uk:contaminated){
					long tk=times.get(uk);
					if(tk<=ti){continue;}
					CPUParams mk=pars.get(uk);
					if(mk==null){
						mk=new CPUParams(1,1);
						params.allocateNewParamsFor(mk, 0.00001f, 0.99999f);
						pars.put(uk, mk);
					}	
				}
			}
			
		}
		currentInput=new Tensor(0);
		
		av=new CPUAverageRows(1);
		global=new SequentialModule();
		global.addModule(null);  // on y mettra les probas dans le forward
		/*global.addModule(somme);
		global.addModule(sum);*/
		global.addModule(av);
		global.addModule(new CPUTimesVals(1,-1.0f));
		senders=new HashMap<String,HashSet<Integer>>();
		
		
		
		HashSet<String> couplesVus=new HashSet<String>();
		for(int ic:cascades_ids){
			PropagationStruct pstruct=this.train_cascades.get(ic);
			TreeMap<Long,HashMap<String,Double>> set=pstruct.getInitContaminated();
			HashMap<String,Long> times=pstruct.getInfectionTimes();
			
			HashSet<String> vus=new HashSet<String>();
			HashSet<String> notvus=new HashSet<String>(pstruct.getPossibleUsers());
			HashSet<String> notvusInfected=new HashSet<String>(pstruct.getArrayContamined());
			int nbi=0;
			int nbc=0;
			for(Long t:set.keySet()){
	        	HashMap<String,Double> ust=set.get(t);
	        	
	        	notvus.removeAll(ust.keySet());
	        	notvusInfected.removeAll(ust.keySet());
	        	int nvs=notvus.size();
	        	int nvsi=notvusInfected.size();
	        	int vs=vus.size();
	        	for(String us:ust.keySet()){
	        		HashSet<Integer> h=senders.get(us);
	        		if(h==null){
	        			h=new HashSet<Integer>();
	        			senders.put(us,h);
	        		}
	        		h.add(ic);
	        		
	        		nbCouplesInLoss+=nvs; //+vs;
	        		
	        	}
	        	vus.addAll(ust.keySet());
	        	
	        }
		}

		senderList=new ArrayList<String>(senders.keySet());
		
		optim.optimize(this);
		
		loaded=true;
	
   
	}
 	
    
	public void forward() {
		forward_couple();
	}
	
	public void forward_user() {
	/*	//System.out.println(zi_zj);
		
		// choose a user
		int x=(int)(Math.random()*this.users.size());
		String ui=users.get(x);
		//int nbused=nbUsedInLoss.get(ui);
		//int nbusedAsInfected=nbUsedInLossAsInfected.get(ui);
		
		// choose a cascade
		//x=(int)(Math.random());
		//if(x>0){
			ArrayList<Integer> casc=new ArrayList<Integer>(this.users_cascades.get(ui).keySet()); 
			x=(int)(Math.random()*casc.size());
		
		int c=casc.get(x);
		PropagationStruct pstruct=this.train_cascades.get(c);
		TreeMap<Long,HashMap<String,Double>> initInfected=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
        TreeMap<Long,ArrayList<String>> cumul=PropagationStruct.getListBeforeT(infections);
        ArrayList<String> contaminated = new ArrayList<String>(cumul.lastEntry().getValue());
        HashMap<String,Long> times=pstruct.getInfectionTimes();
        Long ti=times.get(ui);
        HashSet<String> set=null;
        //System.out.println(c+" select "+ui+" "+ti);
        //Long maxtj=cumul.lastEntry().getKey();
        
        if(ti!=null){
        	if((iInInit) && (ti>nbInitSteps)){
        		set=new HashSet<String>(cumul.get((long)nbInitSteps));
        	}
        	else{
        		set=new HashSet<String>(users);
        		set.removeAll(infections.get(ti).keySet());
        	}
        }
        else{
        	if(iInInit){
        		set=new HashSet<String>(cumul.get((long)nbInitSteps));
        	}
        	else{
        		set=new HashSet<String>(contaminated);
        	}
        }
        double probaCouple=1.0f;
        int nbCouplesC=set.size();
        //probaCouple=(nbCouplesC*1.0f/nbusedAsInfected)*(nbused*1.0f/nbCouplesInLoss)*cascades_ids.size();
        if(unbiased){
        	probaCouple=(nbCouplesC*1.0f/nbCouplesInLoss)*10000;
        }
        else{
        	probaCouple=(double)(1.0/(nbCascadesPerUser*users.size()))*10000;
        }
        HashMap<String,HashMap<String,Double>> hashConta=new HashMap<String,HashMap<String,Double>>();
        HashMap<String,HashMap<String,Double>> hashNotConta=new HashMap<String,HashMap<String,Double>>();
        
		int nbSamples=0;
		ArrayList<String> usersj=new ArrayList<String>(set);
		lastUsed=new HashMap<String,Module>();
		int nbs=0;
		while(nbSamples<nbMaxSamples){
			x=(int)(Math.random()*usersj.size());
			String uj=usersj.get(x);
			Long tj=times.get(uj);
			if((tj==null) || ((ti!=null) && (ti<tj))){
				
				HashMap<String,Double> h=null;
				if(tj==null){
					h=hashNotConta.get(ui);
					if(h==null){
						h=new HashMap<String,Double>();
						hashNotConta.put(ui,h);
					}
				}
				else{
					h=hashConta.get(ui);
					if(h==null){
						h=new HashMap<String,Double>();
						hashConta.put(ui,h);
					}
				}
				Double a=h.get(uj);
				if(a==null){
					nbs++;
				}
				a=(a==null)?0.0f:a;
				h.put(uj,a+1.0f);
			}
			if((ti==null) || ((tj!=null) && (tj<ti))){
				
				HashMap<String,Double> h=null;
				if(ti==null){
					h=hashNotConta.get(uj);
					if(h==null){
						h=new HashMap<String,Double>();
						hashNotConta.put(uj,h);
					}
				}
				else{
					h=hashConta.get(uj);
					if(h==null){
						h=new HashMap<String,Double>();
						hashConta.put(uj,h);
					}
				}
				Double a=h.get(ui);
				if(a==null){
					nbs++;
				}
				a=(a==null)?0.0f:a;
				h.put(ui,a+1.0f);
			}
			nbSamples++;
		}
		nbSamples=nbs;
		
		CPUParams parsv=new CPUParams(nbSamples,1);
		for(String u:hashConta.keySet()){
			HashMap<String,Double> h=hashConta.get(u);
			HashMap<String,CPUParams> pars=probas.get(u);
			if(pars==null){
				pars=new HashMap<String,CPUParams>();
				probas.put(u,pars);
			}
			for(String v:h.keySet()){
				CPUParams mk=pars.get(v);
			}
		}*/
	}
	
	public void forward_couple(){
		//System.out.println(zi_zj);
		
		
		boolean ok=false;
		ArrayList<Integer> casc=null;
		int x=0;
		String ui="";
		String uj="";
		
		/*while(!ok){
			int nbok=0;
			int nbnon=0;
			// choose a user ui as sender
			x=(int)(Math.random()*this.senderList.size());
			ui=senderList.get(x);
			
			// choose a user uj as receiver
			x=(int)(Math.random()*this.users.size());
			uj=users.get(x);
			
			HashSet<Integer> casci=senders.get(ui);
			System.out.println(casci.size());
			casc=new ArrayList<Integer>();
			HashMap<Integer,Long> timesi=users_cascades.get(ui);
			HashMap<Integer,Long> timesj=users_cascades.get(uj);
			for(Integer c:casci){
				Long ti=timesi.get(c);
				Long tj=timesj.get(c);
				if((tj==null) || (tj>ti)){
					casc.add(c);
					if(tj==null){
						nbnon++;
					}
					else{
						nbok++;
					}
				}
			}
			HashMap<String,CPUParams> pars=probas.get(ui);
			if(pars==null){
				pars=new HashMap<String,CPUParams>();
			}
			CPUParams mk=pars.get(uj);
			if(mk!=null){
				ok=true;
				System.out.println(ui+" "+uj+" "+nbok+" "+nbnon+" "+mk.getParamList().get(0).getVal());
			}
			
		}*/
		
		// choose a user ui as sender
		x=(int)(Math.random()*this.senderList.size());
		ui=senderList.get(x);
		
		HashMap<String,CPUParams> pars=probas.get(ui);
		ArrayList<String> receivers=new ArrayList<String>(pars.keySet());
		
		// choose a user uj as receiver
		x=(int)(Math.random()*receivers.size());
		uj=receivers.get(x);
		//System.out.println(ui+" "+uj);
		HashSet<Integer> casci=senders.get(ui);
		//System.out.println(casci.size());
		casc=new ArrayList<Integer>();
		HashMap<Integer,Long> timesi=users_cascades.get(ui);
		HashMap<Integer,Long> timesj=users_cascades.get(uj);
		for(Integer c:casci){
			Long ti=timesi.get(c);
			Long tj=timesj.get(c);
			if((tj==null) || (tj>ti)){
				if(!train_cascades.get(c).getPossibleUsers().contains(uj)){
					continue;
				}
				casc.add(c);
				/*if(tj==null){
					nbnon++;
				}
				else{
					nbok++;
				}*/
			}
		}
		
		CPUParams mk=pars.get(uj);
		
		
		lastUsed=new HashMap<String,Module>();
		int nbm=nbMaxSamples;
		if(nbm<0){nbm=casc.size();}
		CPUParams parsv=new CPUParams(nbm,1);
		
		if(mk==null){
			throw new RuntimeException("Should not be here!!");
		}
		else{				
			lastUsed.put(ui+"_"+uj, mk);
		}
		CPUMatrix mat=new CPUMatrix(nbm,1);
		CPUMatrix mat2=new CPUMatrix(nbm,1);
        //ArrayList<Integer> cplus=new ArrayList<Integer>(); 
        //ArrayList<Integer> cmoins=new ArrayList<Integer>();
        double proba=((1.0f*casc.size())/nbCouplesInLoss)*senders.size()*receivers.size();
        int nbSamples=0;
        
        while(nbSamples<nbm){
        	if(nbMaxSamples<0){
        		x=nbSamples;
        	}
        	else{
        		x=(int)(Math.random()*casc.size());
        	}
        	int c=casc.get(x);
    		PropagationStruct pstruct=this.train_cascades.get(c);
    		HashMap<String,Long> times=pstruct.getInfectionTimes();
            Long tj=times.get(uj);
            if(tj==null){
            	mat.setValue(nbSamples,0,-1.0f);
				mat2.setValue(nbSamples,0,-1.0f);
            	//cmoins.add(c);
            }
            else{
            	mat.setValue(nbSamples,0,0.0f);
				mat2.setValue(nbSamples,0,1.0f);
            	//cplus.add(c);
            }
            
            parsv.addParametersFrom(mk);

            
        	nbSamples++;
        }
        
        SequentialModule somme=new SequentialModule();
		//System.out.println("nb Input = "+simP.getNbInputMatrix());
		somme.addModule(parsv);
		
		CPUAddVals adv=new CPUAddVals(1,mat);
		somme.addModule(adv);
		CPUTimesVals tv=new CPUTimesVals(1,mat2);
		somme.addModule(tv);
		somme.addModule(new CPULog(1));
		//CPUTimesVals tv2=new CPUTimesVals(1,mat3);
		//somme.addModule(tv2);
        //System.out.println("content = "+content);
		global.setModule(0, somme);
		//av.setWeights(w, 1);
		//this.currentInput=new Tensor(0);
		global.setModule(2, new CPUTimesVals(1,-1.0f*proba));
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
		Matrix out=parsv.getOutput().getMatrix(0);
		//System.out.println(out);
		for(int q=0;q<out.getNumberOfRows();q++){
			if(mat2.getValue(q, 0)>0){
				nbYes++;
				sumYes+=out.getValue(q, 0);
			}
			else{
				nbNot++;
				sumNot+=out.getValue(q, 0);
			}
			
		}
		//System.out.println("sim = "+out.getValue(0, 0));
		if(nbForwards%1000==0){ // && (listei.size()>0)){
			//System.out.println(listei.size()+" contamines");
			double rYes=(1.0*sumYes/nbYes);
			double rNot=(1.0*sumNot/nbNot);
			System.out.println(this.getName()+" Average Loss = "+sumLossTot/(1.0*nbForwards)+", "+sumLoss/(1.0*nbSum)+", "+lastLoss+" nbSums="+nbSum+" nbForwards="+nbForwards+" "+rYes+" "+rNot+" "+(rYes/rNot));
			//System.out.println(pInfect.getOutput());
			//System.out.println(somme.getOutput());
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
	
	public void forward3() {
		//System.out.println(zi_zj);
		
		
		// choose cascade
		int x=(int)(Math.random()*this.cascades_ids.size()); 
		int c=cascades_ids.get(x);
		//System.out.println("Cascade "+c);
		
		PropagationStruct pstruct=this.train_cascades.get(c);
		TreeMap<Long,HashMap<String,Double>> initInfected=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
        LinkedHashSet<String> users=pstruct.getPossibleUsers();
		
        while(infections.size()<=initInfected.size()){
        	x=(int)(Math.random()*this.cascades_ids.size()); 
    		c=cascades_ids.get(x);
    		pstruct=this.train_cascades.get(c);
    		initInfected=pstruct.getInitContaminated();
            infections=pstruct.getInfections();
       
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
        //System.out.println("Chosen cascade = "+c);
        //System.out.println(infections);
        //System.out.println(contaminated.size()+" init "+(PropagationStruct.getPBeforeT(infections)).keySet().size()+" contamines");
        /*ArrayList<String> listei=new ArrayList<String>();
        ArrayList<String> listek=new ArrayList<String>();
        ArrayList<String> listeiNotConta=new ArrayList<String>();
        ArrayList<String> listekNotConta=new ArrayList<String>();
        */
        HashMap<CPUParams,Double> hashConta=new HashMap<CPUParams,Double>();
        HashMap<CPUParams,Double> hashNotConta=new HashMap<CPUParams,Double>();
        
        //HashMap<String,HashMap<String,Double>> hash=new HashMap<String,HashMap<String,Double>>(); 
        
        HashMap<String,Long> times=pstruct.getInfectionTimes();
       
        lastUsed=new HashMap<String,Module>();
        ArrayList<Long> infTimes=new ArrayList<Long>(infections.keySet());
		//HashSet<String> vus=new HashSet<String>();
		boolean stop=false;
		int nbSamples=0;
		int nfirst=initInfected.get(1l).keySet().size();
		double rconta=(1.0f*contaminated.size())/(users.size()-nfirst);
		double rnconta=(1.0f*(notContaminated.size()))/(users.size()-nfirst);
		CPUParams vide=new CPUParams(1,1,Double.MIN_VALUE,Double.MIN_VALUE, 1.0f-Double.MIN_VALUE);
		while(nbSamples<nbMaxSamples){
			x=(int)(Math.random()*contaminated.size());
	        String uk=contaminated.get(x);
	        Long tk=times.get(uk);
	        if(tk==1){
	        	throw new RuntimeException("Should not have time equal to 1");
	        }
	        Long ti=tk;
	        
	        Long maxTi=tk-1;
	        if(maxTi>pstruct.getNbInitSteps()){
	        		maxTi=(long)pstruct.getNbInitSteps();
	        }
	        ArrayList<String> before=cumul.get(maxTi);
	        x=(int)(Math.random()*before.size());
	        String ui=before.get(x);
	        HashMap<String,CPUParams> pars=probas.get(ui);
			if(pars==null){
				pars=new HashMap<String,CPUParams>();
			}
			
			CPUParams mk=pars.get(uk);
			if(mk==null){
				throw new RuntimeException("Should not be here!!");
			}
			else{				
				lastUsed.put(uk, mk);
			}
			Double nh=hashConta.get(mk);
			nh=(nh==null)?0.0f:nh;
			hashConta.put(mk,nh+rconta);
			
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
		
			pars=probas.get(ui);
			if(pars==null){
				pars=new HashMap<String,CPUParams>();
			}
			
			mk=pars.get(uk);
			if(mk==null){
				mk=vide;
			}
			else{				
				lastUsed.put(uk, mk);
			}
			nh=hashNotConta.get(mk);
			nh=(nh==null)?0.0f:nh;
			hashNotConta.put(mk,nh+rnconta);
			//System.out.println(ui+"=>"+uk+"("+times.get(ui)+")"+":"+(nh+rnconta));
			
			nbSamples++;
		}
		nbSamples=hashConta.size()+hashNotConta.size();
		CPUParams parsi=new CPUParams(nbSamples,1);
		CPUMatrix mat=new CPUMatrix(nbSamples,1);
		CPUMatrix mat2=new CPUMatrix(nbSamples,1);
		//CPUMatrix mat3=new CPUMatrix(nbSamples,1);
		ArrayList<Double> w=new ArrayList<Double>();
		int i=0;
		for(CPUParams mk:hashConta.keySet()){
			parsi.addParametersFrom(mk);
			mat.setValue(i,0,0.0f);
			mat2.setValue(i,0,1.0f);
			//mat3.setValue(i,0,hashConta.get(mk));
			w.add(hashConta.get(mk));
			i++;
		}
		for(CPUParams mk:hashNotConta.keySet()){
			parsi.addParametersFrom(mk);
			mat.setValue(i,0,-1.0f);
			mat2.setValue(i,0,-1.0f);
			//mat3.setValue(i,0,hashNotConta.get(mk));
			w.add(hashNotConta.get(mk));
			i++;
		}
		
		
        
		SequentialModule somme=new SequentialModule();
		//System.out.println("nb Input = "+simP.getNbInputMatrix());
		somme.addModule(parsi);
		
		CPUAddVals adv=new CPUAddVals(1,mat);
		somme.addModule(adv);
		CPUTimesVals tv=new CPUTimesVals(1,mat2);
		somme.addModule(tv);
		somme.addModule(new CPULog(1));
		//CPUTimesVals tv2=new CPUTimesVals(1,mat3);
		//somme.addModule(tv2);
        //System.out.println("content = "+content);
		global.setModule(0, somme);
		av.setWeights(w, 1);
		//this.currentInput=new Tensor(0);
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
		if(nbForwards%100==0){ // && (listei.size()>0)){
			//System.out.println(listei.size()+" contamines");
			System.out.println(this.getName()+" Average Loss = "+sumLossTot/(1.0*nbForwards)+", "+sumLoss/(1.0*nbSum)+", "+lastLoss+" nbSums="+nbSum+" nbForwards="+nbForwards);
			//System.out.println(pInfect.getOutput());
			//System.out.println(somme.getOutput());
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
	public void forward2() {
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
        ArrayList<String> listek=new ArrayList<String>();
        ArrayList<String> listeiNotConta=new ArrayList<String>();
        ArrayList<String> listekNotConta=new ArrayList<String>();
         
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
	        
	        
	        
	        x=(int)(Math.random()*users.size());
	        String uk=lusers.get(x);
	        if(uk.compareTo(ui)==0){
	        	continue;
	        }
	        
	        if(initContaminated.contains(uk)){	
	        		continue;
	        }
	        
	        
	        Long tk=times.get(uk);
	        if ((tk!=null) && (tk<=ti)){
	        	 if(tk==ti){
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
	 	        
	        }
	        
			if(tk!=null){
				listei.add(ui);
				listek.add(uk);
			}
			else{
				listeiNotConta.add(ui);
				listekNotConta.add(uk);
			}
			nbSamples++;
		}
		CPUParams parsi=new CPUParams(nbSamples,1);
		
		for(int i=0;i<nbSamples;i++){
			String ui="";
			String uk="";
			if(i<listek.size()){
				uk=listek.get(i);
				ui=listei.get(i);
			}
			else {
				uk=listekNotConta.get(i-listek.size());
				ui=listeiNotConta.get(i-listek.size());
			}
			HashMap<String,CPUParams> pars=probas.get(ui);
			if(pars==null){
				pars=new HashMap<String,CPUParams>();
				//probas.put(ui, pars);
			}
			//System.out.println("Chosen ui = "+ui);
			//System.out.println("Chosen uk = "+uk);
			
			CPUParams mk=pars.get(uk);
			if(mk==null){
				if(contaminated.contains(uk)){
					throw new RuntimeException("Problem : should not be here");
				}
				//	mk=new CPUParams(1,1);
				//	params.allocateNewParamsFor(mk, 0.0001f, 0.9999f);
				//	pars.put(uk, mk);
				//	lastUsed.put(uk, mk);
				//}
				//else{*/
				mk=new CPUParams(1,1,Double.MIN_VALUE,Double.MIN_VALUE, 1.0f-Double.MIN_VALUE);
				//}
			}
			else{
				//System.out.println(uk+"="+mk.getParamList());				
				lastUsed.put(uk, mk);
			}
			parsi.addParametersFrom(mk);
		}
		
		
        
		SequentialModule somme=new SequentialModule();
		//System.out.println("nb Input = "+simP.getNbInputMatrix());
		somme.addModule(parsi);
		CPUMatrix mat=new CPUMatrix(nbSamples,1);
		CPUMatrix mat2=new CPUMatrix(nbSamples,1);
		for(int i=0;i<listek.size();i++){
			mat.setValue(i,0,0.0f);
			mat2.setValue(i,0,1.0f);
		}
		for(int i=0;i<listekNotConta.size();i++){
			mat.setValue(i+listek.size(),0,-1.0f);
			mat2.setValue(i+listek.size(),0,-1.0f);
		}
		CPUAddVals adv=new CPUAddVals(1,mat);
		somme.addModule(adv);
		CPUTimesVals tv=new CPUTimesVals(1,mat2);
		somme.addModule(tv);
		somme.addModule(new CPULog(1));
		
		
        //System.out.println("content = "+content);
		global.setModule(0, somme);
		//this.currentInput=new Tensor(0);
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
		if(nbForwards%100==0){ // && (listei.size()>0)){
			System.out.println(listei.size()+" contamines");
			System.out.println(this.getName()+" Average Loss = "+sumLossTot/(1.0*nbForwards)+", "+sumLoss/(1.0*nbSum)+", "+lastLoss+" nbSums="+nbSum+" nbForwards="+nbForwards);
			//System.out.println(pInfect.getOutput());
			//System.out.println(somme.getOutput());
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
	public String getName(){
		return "MLPnaiveLink";
		
	}
    
       
    
    
    public void backward()
	{
		global.backward_updateGradient(this.currentInput);
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
	public int inferSimulation(Structure struct) {
		infer(struct);
		PropagationStruct pstruct = (PropagationStruct)struct ;
		TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
		TreeMap<Long,HashMap<String,Double>> ninfections=new TreeMap<Long,HashMap<String,Double>>();
		TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
		int tt=1;
	     for(long t:contaminated.keySet()){
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	infections.put((long)tt, (HashMap<String,Double>) inf.clone());
	    	
	    	tt++;
	     }
	     int firstNewT=tt;
	     
	     HashMap<String,Double> infectedstep = new HashMap<String,Double>() ;
	     ninfections.put((long)firstNewT,infectedstep);
	     
		HashMap<String,Double> probas=infections.get(firstNewT);
		for(String u : probas.keySet()) {
			double v=Math.random();
			if(v<probas.get(u)){
				infectedstep.put(u,1.0);
			}
		}
		return 0;
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
	
   
   
   
   
    /**
     * @param args
     */
    public static void main(String[] args) {
    	try{
			
			MLPnaiveLink mlp;
			if(args.length==6){
				mlp=new MLPnaiveLink(args[5]); 
				mlp.load();
			}
			else{
				mlp=new MLPnaiveLink();
			}
			
			double ratioInits=Double.valueOf(args[2]);
			Env.setVerbose(0);
			//MLPdiffusion mlp=new MLPdiffusion("psauv/MLPDiagContent2_Dims-100_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0_iInInit-true_senderReceiver-false/last");
			/*
			*/
			Optimizer opt=Optimizer.getDescent(DescentDirection.getAverageGradientDirection(), LineSearch.getFactorLine(Double.valueOf(args[3]),Double.valueOf(args[4])));
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
