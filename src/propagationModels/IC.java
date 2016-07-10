package propagationModels;

import java.io.BufferedReader;

import cascades.CascadesLoader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;












//import trash.ArtificialCascadesLoader;
import trash.ICold;
import utils.ArgsParser;
import utils.Keyboard;
import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import statistics.ErrorFunction;
import core.Link;
import core.Post;
import core.Structure;
import core.User;
import cascades.Cascade ;
import cascades.CascadesProducer;
import cascades.IteratorDBCascade;

import java.util.TreeMap;
public class IC implements PropagationModel, ProbabilisticTransmissionModel {
   
	private long maxIter=-2;  // -2 => can be modified by loading
	private String modelFile="";
	private boolean loaded=false;
	private int inferMode=0;
	private int contaMaxDelay=-1;
	double smoothing=0.00000001;
	int nbGibbsIt=10;
	int nbBurnOut=1;
    int nbPos=25;
    private HashMap<String,HashMap<String,Double>> pij;
    private HashSet<String> users;
    public static boolean first = true;

	public IC(int contaMaxDelay){
		this(contaMaxDelay,1);
	}
	public IC(int contaMaxDelay,int inferMode){
		this.contaMaxDelay=contaMaxDelay;
		this.inferMode=inferMode;
	}
	
/*	public IC(String modelFile, long maxIter){
		this(modelFile,1,maxIter);
	}*/
	
	public IC(String modelFile, int inferMode){
		this.modelFile=modelFile;
		this.inferMode=inferMode;
		
	}
	
	
	public IC(String modelFile,  long maxIter, int inferMode){
		this.modelFile=modelFile;
		this.inferMode=inferMode;
		this.maxIter=maxIter;
	}
	
	public HashMap<String,HashMap<String,Double>> getProbas(){
		return pij;
	}
	
	public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return users;
	}
	
	public Double getProba(String from,String to){
		if(!loaded){
			load();
		}
		HashMap<String,Double> h=pij.get(from);
		if(h==null) return null;
		
		return h.get(to); 
	}
	
	public String getModelFile(){
		return modelFile;
	}
	
	public int getContentNbDims(){
		return 0;
	}
	
	
	public String toString(){
		String sm=modelFile.replaceAll("/", "/");
		return("ICmodel_"+contaMaxDelay+"_inferMode-"+inferMode+"_maxIter-"+maxIter+"_"+sm);
	}
	public String getName(){
		String sm=modelFile.replaceAll("/", "__");
		return sm;
	}
	public int infer(Structure struct) {
		//if(inferMode==0) return(inferSimulation(struct));
		//if(inferMode<=1) 
		if(inferMode<=3){
			return(inferSimulationProbas(struct));
		}
		//System.out.println("IC infer struct");
		//else{return(inferOneStepFromRef(struct));}
		//if(inferMode==2) return(inferProbasGibbs(struct));
		return -1;
	}
	
	
	public int inferSimulation(Structure struct) {
		inferMode=0;
		return inferSimulationProbas(struct);
	}
	
	
	
	/*public void computeProbas(Structure struct){
		 if (!loaded){
	        	load();
	        }
	        //System.out.println("IC inference");
	    	PropagationStruct pstruct = (PropagationStruct)struct ;
	    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
	        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
	        HashMap<String, HashMap<String,Double>> from = new HashMap<String, HashMap<String,Double>>();
	        HashMap<String,Double> proba=new HashMap<String,Double>();
	       long maxt=0;
	        TreeSet<Long> cTimes=new TreeSet<Long>();
		    for(long t:contaminated.keySet()){
		    	HashMap<String,Double> inf=contaminated.get(t);
		    	infections.put(t, (HashMap<String,Double>) inf.clone());
		    	for(String user:inf.keySet()){
		    		HashMap<String,Double> pr=new HashMap<String,Double>();
		    		from.put(user, pr);
		    		times.put(user, t);
		    	}
		    	maxt=t;
		    	
		    	//cTimes.add(t);
		    }
		    long firstNewT=maxt+1;
	}*/
	
	public int inferSimulationProbas(Structure struct) {
        if (!loaded){
        	load();
        }
        //System.out.println(this.couplesInTrain);
    	PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashSet<String> infectedBefore = new HashSet<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        HashMap<String,Double> contagious=new HashMap<String,Double>();
        HashSet<String> inits=new HashSet<String>();
        HashMap<String,Long> times=new HashMap<String,Long>();
        HashSet<String> users=new HashSet<String>(this.users);
        long maxt=0;
        //TreeSet<Long> cTimes=new TreeSet<Long>();
        long f=-1;
	    for(long t:contaminated.keySet()){
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	if(f<0){
	    		f=t;
	    	}
	    	HashMap<String,Double> infectedstep=new HashMap<String,Double>();
	    	if((inferMode<3) || (f==t)){
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
        //long maxIter=maxT;
        if(maxIter<0){
        	maxIter=100;
        }
        while(ti<(maxIter+firstNewT)){
        	//System.out.println("ti "+contagious.size());
        	HashMap<String,Double> infectedStep=new HashMap<String,Double>();
    		//infections.put(ti, infectedStep);
    		
        	for(String contagiousU : contagious.keySet()) {
        		/*if(!emitters.contains(contagiousU)){
        			continue;
        		}*/
        		 HashMap<String,Double> succs=pij.get(contagiousU); //contagiousUser.getSuccesseurs();
                 if(succs==null){
                 	continue;
                 }
                 
                 for(String neighbour : succs.keySet()){ //get(contagiousUser.getID()).keySet()) {
                     if(infectedBefore.contains(neighbour))
                         continue ;
                     //System.out.println(contagiousU+" => "+neighbour.getName()+" = "+lsuc.getVal());
                     double val=succs.get(neighbour);
 		 
                     if(Math.random()<val) {
                     	
                        	infectedStep.put(neighbour, 1.0);
                        	times.put(neighbour, ti);
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
	        	HashMap<String,Double> succs=pij.get(user); //contagiousUser.getSuccesseurs();
                if(succs==null){
                	continue;
                }
                long tu=times.get(user);
	            for(String v : succs.keySet()){ //get(contagiousUser.getID()).keySet()) {
	        
	                    
	            	if(((inferMode<2) && (infectedBefore.contains(v))) || ((inferMode>=2) && (v.equals(user) || (inits.contains(v)))))
	                    continue ;
	            	
	            	if((inferMode==3) && (times.containsKey(v)) && (times.get(v)<=tu)){
	            		continue;
	            	}
	                Double p=notYet.get(v);
	                p=(p==null)?1.0:p;
	                Double pp=succs.get(v);
	                pp=(pp==null)?0.0:pp;
	                p*=(1.0-pp);
	                /*if(pp>1.0){
	                	System.out.println(v + " => "+pp);
	                }*/
	                notYet.put(v,p);
	                //
	                
	            }
	        }
	        infectedstep=new HashMap<String,Double>();
	        for(String user:notYet.keySet()){
	        	double p=1.0-notYet.get(user);
	        	p*=0.99999;
	        	
	        	infectedstep.put(user,p);
	        	/*if(p>1.0){
	        		System.out.println(user+" => "+p);
	        	}*/
	        	//System.out.println("fin "+ user + " => "+(1.0-notYet.get(user)));
	        	//System.out.println(user + " : "+(1.0-notYet.get(user)));
	        }
	        
	       
	        infections.put(ti,infectedstep);
        }
        //infections.add(infectedstep);
       
        pstruct.setInfections(infections) ;
        return 0;
    }
	
	
	
	/**
	 * On last step, we set probas of infection for non infected nodes at this step rather than a binary information from simulation. 
	 * @param struct
	 * @return
	 */
	/* public int inferSimulationProbas(Structure struct) {
        if (!loaded){
        	load();
        }
        //System.out.println("conta "+this.contaMaxDelay);
        //System.out.println("IC inference");
    	PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashMap<String,Long> contagious = new HashMap<String,Long>();
        HashMap<String,Long> times=new HashMap<String,Long>();
        HashSet<String> infectedBefore = new HashSet<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        
        long maxt=0;
        TreeSet<Long> cTimes=new TreeSet<Long>();
        long f=-1;
	    for(long t:contaminated.keySet()){
	    	if(f<0){
	    		f=t;
	    	}
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	infections.put(t, (HashMap<String,Double>) inf.clone());
	    	for(String user:inf.keySet()){
	    		contagious.put(user, t);
	    		times.put(user, t);
	    	}
	    	maxt=t;
	    	
	    	//cTimes.add(t);
	    }
	    long firstNewT=maxt+1;
        
        HashMap<String,Double> lastcontagious=(HashMap<String,Double>)contagious.clone();
        HashMap<String,Double> infectedstep=new HashMap<String,Double>();
        //int it=tt;
        long it=firstNewT;
        long horizon=this.contaMaxDelay;
        if(horizon<0){
        	horizon=this.maxIter;
        }
        if(horizon<=0){
        	horizon=1;
        }
        boolean ok=true;
        long currentT=firstNewT-1;
        //boolean noMax=true;
        while(ok){
        	if(it>=(maxIter+firstNewT)){
        		break;
        	}
        	//System.out.println(it);
        	//currentT++;
        	for(String contagiousU : contagious.keySet()) {
        		Long time=contagious.get(contagiousU);
                HashMap<String,Double> succs=pij.get(contagiousU); //contagiousUser.getSuccesseurs();
                if(succs==null){
                	continue;
                }
                //System.out.println(contagiousU+" : "+succs.size());
                
                //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
                
                long tpos=horizon;
                if((time+horizon)>maxIter){
                	tpos=maxIter-time;
                }
                long minT=time+1;
                if(time<(firstNewT-1)){
                	tpos-=(firstNewT-time+1);
                	minT=firstNewT;
                }
                
                if(tpos<0){
                	continue;
                }
                for(String neighbour : succs.keySet()){ //get(contagiousUser.getID()).keySet()) {
                    if(infectedBefore.contains(neighbour))
                        continue ;
                    //System.out.println(contagiousU+" => "+neighbour.getName()+" = "+lsuc.getVal());
                    double val=succs.get(neighbour);
		 
                    if(Math.random()<val) {
                    	long ti=((long)(Math.random()*tpos))+minT;
                    	//System.out.println(contagiousU+" => "+neighbour+"="+ti);
                        
                        Long oldT=times.get(neighbour);
                        if((oldT!=null) && (ti<oldT)){
                        	//if(this.inferMode!=2){
                        		HashMap<String,Double> infectedStep=infections.get(oldT);
                        		infectedStep.remove(neighbour);
                        		if(infectedStep.size()==0){
                        			infections.remove(oldT);
                        			cTimes.remove(oldT);
                        		}
                        	//}
                        		
                        		
                        }
                        
                        if((oldT==null) || (ti<oldT)){
                        	//if(this.inferMode!=2){
                        		HashMap<String,Double> infectedStep=infections.get(ti);
                        		if(infectedStep==null){
                        			infectedStep=new HashMap<String,Double>();
                        			infections.put(ti, infectedStep);
                        		}
                        		infectedStep.put(neighbour, 1.0);
                        		
                        	//}
                        	cTimes.add(ti);
                    		times.put(neighbour,ti);
                    		
                        }
                    }	
                }
            }
        	if(cTimes.size()==0){
        		//System.out.println("maxT="+it);
        		break;
        	}
        	Long time=cTimes.first();
        	if(time>=maxIter){
        		it=time;
        		break;
        	}
        	//System.out.println("time of new contagious = "+time);
        	//System.out.println(times);
        	HashMap<String,Double> infectedStep=infections.get(time);
        	cTimes.remove(time);
        	contagious = new HashMap<String,Long>();
        	for(String user:infectedStep.keySet()){
        		contagious.put(user, time);
        		infectedBefore.add(user);
        	}
        	
    		
        	
            if(contagious.isEmpty())
                break ; 
            
           
            it=time;
        	
        }
        //System.out.println(times);
        if(it>firstNewT){
        	System.out.println("nb conta = "+infectedBefore.size());
        }
        it=firstNewT+maxIter;
        if(inferMode>=3){
        	it=f+1;
        }
        if(this.inferMode>=1){
        	if(this.inferMode>=2){
        		infectedBefore=new HashSet<String>();
        		cTimes=new TreeSet<Long>(infections.keySet());
        		for(Long t:cTimes){
        			long x=firstNewT;
        			if(inferMode>=3){
        				x=f+1;
        			}
        			if(t>=x){
        				infections.remove(t);
        			}
        			else{
        				infectedBefore.addAll(infections.get(t).keySet());
        			}
        		}
        	}
	        HashMap<String,Double> notYet=new HashMap<String,Double>();
	        
	        for(String user:users){
	        	if(((this.inferMode>=2) && (!infectedBefore.contains(user)))|| (!times.containsKey(user))){
	        		notYet.put(user,1.0);
	        	}
	        }
	         
	        for(String user : times.keySet()) {
	            Long time=times.get(user);
	            if((time+horizon)<it){
	            	//System.out.println("continue");
	            	continue;
	            }
	             
	    		HashMap<String,Double> succs=pij.get(user);
	    		if(succs==null){
	    			continue;
	    		}
	            //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
	    		long tu=times.get(user);
	            for(String v : succs.keySet()){ //get(contagiousUser.getID()).keySet()) {
	                if(!notYet.containsKey(v))
	                    continue ;
	                if(v.equals(user)){
	                	continue;
	                }
	                if((inferMode>=3) && (times.containsKey(v)) && (times.get(v)<=tu)){
	            		continue;
	            	}
	                Double p=notYet.get(v);
	                double val=succs.get(v);
	              
	                p*=(1.0-val);
	                notYet.put(v,p);
	            }
	        }
	        for(String user:notYet.keySet()){
	        	double p=1.0-notYet.get(user);
	        	infectedstep.put(user,p); //*0.998+0.001);
	        	//System.out.println(user + " : "+(1.0-notYet.get(user)));
	        }
	        
	       
	        infections.put(it,infectedstep);
        }
        //infections.add(infectedstep);
        //System.out.println(PropagationStruct.getPBeforeT(infections));
        pstruct.setInfections(infections) ;
        return 0;
    }
	 */   
	
	
	
	
   
    public void load(){
		String filename=modelFile;
        User.reinitAllLinks();
        BufferedReader r;
        pij=new HashMap<String,HashMap<String,Double>>();
        users=new HashSet<String>();
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          String[] sline ;
          while((line=r.readLine()) != null) {
        	if(line.startsWith("maxIter")){
            	if(this.maxIter==-2){
            		sline=line.split("=");
            		maxIter=Long.valueOf(sline[1]);
            	}
                continue;
            }
        	if(line.startsWith("contaMaxDelay")){
            	sline=line.split("=");
            	contaMaxDelay=Integer.parseInt(sline[1]);
                continue;
            }
        	String[] tokens = line.split("\t") ;
            if(tokens[2].startsWith("NaN"))
                tokens[2]="0.0" ;
           
            double d = Double.parseDouble(tokens[2]) ;
            if(d<=Double.MIN_VALUE)
                continue ;
            HashMap<String,Double> hi=pij.get(tokens[0]);
            if(hi==null){
            	hi=new HashMap<String,Double>();
            	pij.put(tokens[0], hi);
            }
            hi.put(tokens[1],d);
            users.add(tokens[0]);
            users.add(tokens[1]);
            //System.out.println("new link "+l);
          }
          r.close();
          loaded=true;
          System.out.println("Loaded" );
        }
        catch(IOException e){
        	System.out.println("Probleme lecture modele "+filename+" "+e);
        }
        /*for(User user:User.users.values()){
		System.out.println("nb succs "+user+" => "+user.getSuccesseurs().size());
        }*/
        
    }

    public void save() {
    	
		String filename=modelFile;
        try{
        	File file=new File(modelFile);
        	File dir=file.getParentFile();
        	if(dir!=null){
        		dir.mkdirs();
        	}
          PrintStream p = new PrintStream(filename) ;
          p.println("maxIter="+maxIter);
          p.println("contaMaxDelay="+contaMaxDelay);
         
          System.out.println("Save conta for "+pij.size()+" users");
          for(String uS:pij.keySet()){
        	  HashMap<String,Double> hi=pij.get(uS);
        	  for(String w:hi.keySet()){
        		  p.println(uS+"\t"+w+"\t"+hi.get(w));
        	  }
          }
          
        }
        catch(IOException e){
        	System.out.println("Probleme sauvegarde modele "+filename);
        	
        }
    }
   
    
    private double unbiasVal(double p,double n, double z){
    	// Wilson Method for taking the center of a given confidence interval
    	// unbiased = 2.576 => 99% confidence interval for kvw
    	// unbiased = 1.960 => 95% confidence interval for kvw
    	// unbiased = 1.645 => 90% confidence interval for kvw
    	double den=(2.0*(n+z*z));
    	double num=((2.0*n*p)+z*z);
    	//double num2=(-1.0-Math.sqrt(z2*(z2-2.0-(1.0/n)+4.0*p*(n*(1.0-p)+1.0))));
    	double num2=-z*Math.sqrt(z*z+(4.0*n*p*(1.0-p)));
    	double ret=(num+num2)/den;
    	/*if(((num+num2)<0) || (Double.isNaN(kvw))){
    		throw new RuntimeException("p="+p+" z2="+z2+" n="+n+" num="+num+" num2="+num2+" den="+den+" kvw="+kvw);
    	}*/
    	if(ret<0){ret=0.0;}
    	return ret;
    }
 
   
 	/*private class Link{
 		String from;
 		String to;
 		Double val;
 		public Link(String from,String to, Double val){
 			this.from=from;
 		}
 	}*/
 	
    public void learn(PropagationStructLoader ploader, String usersDb, String usersCollection, int nbIter, double userLinkThreshold,double lambdaReg){
    	learn(ploader,usersDb,usersCollection,nbIter,userLinkThreshold,0.1,0.3,0.0,0.0,1,lambdaReg,0.0,0.0);
    }
    
    // "Prediction of Information Diffusion Probabilities for Independent Cascade Model"
    // contaMaxDelay = 1 => propagation can only be done between two users contamined in contiguous steps (as defined in the saito paper). contaMaxDelay=n => propagation can be between steps separated one from each other from at max n steps. contaMaxDelay=-1 => a user can have contamined every user contamined after him  
    public void learn(PropagationStructLoader ploader, String usersDb, String usersCollection, int nbIter, double userLinkThreshold, double min_init, double max_init, double addNeg, double unbiased,int l1reg,double lambdaReg,double globalExtern,double individualExtern) {
    	System.out.println("lambdaReg="+lambdaReg);
    	System.out.println("l1Reg="+l1reg);
    	//boolean regul=true;
        
    	boolean displayLikelihood=true;
        HashMap<Integer,PropagationStruct> cascades=ploader.getCascades();
        // Pour chaque cascade, liste id users avec leur temps de contamination
        /*HashMap<Integer,HashMap<User,Long>> userTimeContamination=new HashMap<Integer,HashMap<User,Long>>(); //loader.getTimeSteps(true);
        HashMap<Integer,TreeMap<Long,HashSet<User>>> ctimes=new HashMap<Integer,TreeMap<Long,HashSet<User>>>();
        for(Integer c:cascades.keySet()){
        	HashMap<User,Long> hc=new HashMap<User,Long>();
        	TreeMap<Long,HashSet<User>> tc=new TreeMap<Long,HashSet<User>>();
        	userTimeContamination.put(c, hc);
        	HashMap<String,Long> h=cascades.get(c).getInfectionTimes();
        	for(String s:h.keySet()){
        		hc.put(User.getUser(s),h.get(s));
        		
        	}
        }*/
        
        //HashMap<Integer,TreeMap<Long,HashSet<String>>> ctimes=new HashMap<Integer,TreeMap<Long,HashSet<String>>>();
        //
        
       
        HashSet<User> users=new HashSet<User>(User.users.values());
        System.out.println("Chargement util");
        int i = 0;
        // Pour tous les utilisateurs on recupere leurs liens

        if (first)
        for(User u:users){
        	if (i % 100 == 0)
        		System.out.println (i + " User link");
            u.loadLinksFrom(usersDb, usersCollection, userLinkThreshold);
            // System.out.println("Liens user "+u.getName()+" charge");
            //System.out.println(u.getSuccesseurs().values());
            i = i + 1;
        }
        first = false;
       
        pij=new HashMap<String,HashMap<String,Double>>();
        //HashMap<String,HashSet<String>> preds=new HashMap<String,HashSet<String>>();
        
        System.out.println("Initialisation valeurs liens");
        HashSet<Link> allLinks=new HashSet<Link>();
        // Tous les liens obtiennent une valeur arbitraire tiree entre min_init et max_init
        double dif=max_init-min_init;
        User userExt=null;
        if(globalExtern>0){
        	userExt=User.getUser("extern");
        	//hc.put(userc.getName(), 0l);
        }
        for(User u:User.users.values()){
            HashMap<String,Link> succs=u.getSuccesseurs();
            
            System.out.println("User "+u);
            
            for(Link l:succs.values()){
            	
                double v=(Math.random()*dif)+min_init;
                l.setVal(v);
                
                //System.out.println("Link "+l);
                allLinks.add(l);
                /*User u2=(User)l.getNode2();
            	if(!User.users.values().contains(u2)){
            		throw new RuntimeException("Blem Users");
            	}
            	HashMap<String,Link> preds=u2.getPredecesseurs();
                if((!preds.containsKey(u.getName())) || (preds.get(u.getName())!=l)){
                	throw new RuntimeException("Blem Links");
                }*/
            }
            /*HashMap<String,Link> preds=u.getPredecesseurs();
            for(Link l:preds.values()){
            	User u2=(User)l.getNode1();
            	HashMap<String,Link> succs2=u2.getSuccesseurs();
            	if(!User.users.values().contains(u2)){
            		throw new RuntimeException("Blem Users");
            	}
            	if((!succs2.containsKey(u.getName())) || (succs2.get(u.getName())!=l)){
                	throw new RuntimeException("Blem Links");
                }
            }*/
        }
       
        
        // Table de Probabilite de contamination pour chaque user, indexee par cascade id
        HashMap<Integer,HashMap<String,Double>> P;
       
        // Table des cascades pour chaque lien <v,w> ou time de v == time de w - 1
        HashMap<Link,HashSet<Integer>> spos=new HashMap<Link,HashSet<Integer>>();
       
        // Table des cascades pour chaque lien <v,w> ou time de v == time de w - 1
        //HashMap<Link,HashSet<Integer>> sneg=new HashMap<Link,HashSet<Integer>>();
        HashMap<Link,Double> sneg=new HashMap<Link,Double>();
          
        System.out.println("Compute sneg et spos");
        // compute sneg et spos
        maxIter=1;
        /*int initNeg=0;
        if(addNeg){
        	initNeg=1;
        }*/
        double sumParticip=0.0;
        HashMap<User,Double> nbParticip=new HashMap<User,Double>();
       	double moyParticip=0.0;
        for(Integer c:cascades.keySet()) {
       			//ctimes.put(c, ctimesc);
        		PropagationStruct pstruct=cascades.get(c);
       			HashMap<String, Long> hc = pstruct.getInfectionTimes();
       			HashSet<String> pus=pstruct.getPossibleUsers();
       			
        		User userc=null;
        		if(individualExtern>0){
        			userc=User.getUser("cascade_"+c);
        			hc.put(userc.getName(), 0l);
        		}
        		moyParticip+=hc.size();
        		for(String w:hc.keySet()){
        			User uw=User.getUser(w);
        			HashSet<Integer> cpos=null;
        			if(individualExtern>0){
        				Link lc=new Link("individualExtern",userc,uw,0.5);
            			userc.addLink(lc,true);
            			allLinks.add(lc);
        				cpos=new HashSet<Integer>();
        				spos.put(lc, cpos);
                        cpos.add(c);
                        
        			}
                    
        			sumParticip+=1.0;
        			Double nbp=nbParticip.get(uw);
        			nbp=(nbp==null)?0.0:nbp;
        			nbParticip.put(uw,nbp+1.0);
        			//User user=w;
        			Long l=hc.get(w);
        			if(l>maxIter){
        				maxIter=l;
        			}
        			
        			if(l>cascades.get(c).getNbInitSteps()){
        				continue;
        			}
        			
        			
        			HashMap<String,Link> succs=uw.getSuccesseurs();
        			if(c==778){
        				continue;
        				//System.out.println("User w = "+w+" => "+l+" "+succs.size());
            			
        			}
        			for(String v:succs.keySet()){
                        //User uv=User.getUser(v);
                        if(!pus.contains(v)){
                        	continue;
                        }
                    	Long hl2=hc.get(v);
                    	
                    	/*if((uv.getName().equals("NJConservative9")) && (w.getName().equals("KeshaRam"))){
                        	
                        }*/
                        long l2=(hl2!=null)?hl2:-2;
                        if (((contaMaxDelay>=0) && (l2>l) && (l2<=(l+contaMaxDelay))) || ((contaMaxDelay<0) && (l2>l))){
                        	Link lvw=succs.get(v);
                        	
                        	cpos=spos.get(lvw);
                            if (cpos==null){
                                cpos=new HashSet<Integer>();
                                spos.put(lvw, cpos);
                            }
                            cpos.add(c);
                            /*if((uv.getName().equals("NJConservative9")) && (w.getName().equals("KeshaRam"))){
                            	System.out.println("NJConservative9 in "+c+" after "+w+" ("+l2+","+l+") "+cpos);
                            	
                            }
                            if((uv.getName().equals("KeshaRam")) && (w.getName().equals("NJConservative9"))){
                            	System.out.println("KeshaRam in "+c+" after "+w+" ("+l2+","+l+") "+cpos);
                            	
                            }*/
                            	
                        }
                        else{
                        	if ((l2==-2) || ((contaMaxDelay>=0) && (l2>(l+contaMaxDelay)))){
                        		Link lvw=succs.get(v);
                        		/*HashSet<Integer> cneg=sneg.get(lvw);
                        		if (cneg==null){
                                    cneg=new HashSet<Integer>();
                                    sneg.put(lvw, cneg);
                                }
                                cneg.add(c);*/
                        		Double x=sneg.get(lvw);
                        		x=(x==null)?0.0:x;
                        		sneg.put(lvw, x+1.0);
                        	}
                        }
                    }
        		}
        		System.out.println("ok");
        }
        if(cascades.size()>0){
        	System.out.println("Cascade average size = "+ moyParticip/cascades.size());
        }
       	System.out.println(allLinks.size()+" liens "+spos.size()+" actifs");
       	System.out.println(User.users.size()+" users");
       	HashSet<Link> asup=new HashSet<Link>();
       	double sum=0.0;
       	
        for(Link l : allLinks) {
        	HashSet<Integer> cpos=spos.get(l);
        	
        	//HashSet<Integer> cneg=sneg.get(l);
        	User u1=(User)l.getNode1();
        	User u2=(User)l.getNode2();
        	/*if((u1.getName().equals("KeshaRam")) && (u2.getName().equals("NJConservative9"))){
                throw new RuntimeException("cpos = "+cpos+ " \n cneg = "+sneg.get(l));
        	}*/
        	
        	
        	if (cpos==null){
                l.setVal(0.0);
                asup.add(l);
                continue;
            }
            else{
            	/*if(cpos.size()>1){
            		System.out.println("Lien > 1 pos :"+cpos.size());
            		System.out.println("Lien > 1 neg :"+sneg.get(l));
            	}*/
            	Double nneg=sneg.get(l);
            	nneg=(nneg==null)?0.0:nneg;
            	/*if(cneg!=null){
            		nneg=cneg.size();
            	}*/
            		
            	nneg+=addNeg;
            	Integer npos=cpos.size();
            	//double v=(1.0*npos)/(nneg+npos);
            	double v=0.5; //(Math.random()*dif)+min_init;	
	        	/*if(unbiased>=0){
            		l.setVal(unbiasVal(v,(nneg+npos),unbiased));
            	}
            	else{*/
            		l.setVal(v);
            	//}
	        	sum+=l.getVal();
	        	HashMap<Integer,Double> weights=new HashMap<Integer,Double>();
	        	weights.put(1,npos*1.0);
	        	weights.put(-1,nneg*1.0);
	        	l.setWeights(weights);
	        	
            }
        }
        for(Link l : asup) {
        	allLinks.remove(l);
        	User v=(User)l.getNode1();
            User w=(User)l.getNode2();
            v.removeSuccesseur(w);
            w.removePredecesseur(v);
            if(spos.get(l)!=null){
            	throw new RuntimeException("cpos non null alors que l a sup");
            }
        }
        
        double z2=unbiased*unbiased;
        
        
        double sumR=-1.0;
        if (modelFile.length()==0){
    		//modelFile="propagationModels/ICmodel_step"+ploader.getStep()+"_ratioInits-"+ploader.getRatioInits()+"_nbMaxInits-"+ploader.getNbMaxInits()+"_db-"+ploader.getDb()+"_cascadesCol-"+ploader.getCollection()+"_users"+usersCollection+"_linkThreshold"+userLinkThreshold+"_asPos"+contaMaxDelay+"_addNeg-"+addNeg+"_unbiased-"+unbiased+"_l1reg-"+l1reg+"_lambdaReg-"+lambdaReg+"_globalExtern-"+globalExtern+"_individualExtern-"+individualExtern;
    		modelFile="propagationModels/ICmodel_step"+ploader.getStep()+"_ratioInits-"+ploader.getRatioInits()+"_nbMaxInits-"+ploader.getNbMaxInits()+"_db-"+ploader.getDb()+"_cascadesCol-"+ploader.getCollection()+"_start"+ploader.getStart()+"_nbC"+ploader.getNbC()+"/users"+usersCollection+"_linkThreshold"+userLinkThreshold+"_contaMaxDelay"+contaMaxDelay+"_addNeg-"+addNeg+"_unbiased-"+unbiased+"_l1reg-"+l1reg+"_lambdaReg-"+lambdaReg+"_globalExtern-"+globalExtern+"_individualExtern-"+individualExtern;
    	}
        for(int iteration = 0 ; iteration<nbIter ; iteration++) {
        	
        	save();
            System.out.println("iteration : "+iteration);
           
            double sump=0;
            double sumnp=0;
            
            
            long tmoy=0;
            int tnb=0;
            long told=0;
            // Estimate P
            P=new HashMap<Integer,HashMap<String,Double>>();
            System.out.println("P... pour "+cascades.size());
            for(Integer c:cascades.keySet()) {
            		//TreeMap<Long,HashSet<User>> ctimesc=ctimes.get(c);
       			
//                    System.out.print((iii++) + ", ");
            		PropagationStruct cas=cascades.get(c);
            		HashSet<String> pus=cas.getPossibleUsers();
       			
                    HashMap<String,Double> pc=new HashMap<String,Double>();
                    P.put(c, pc);
                    HashMap<String, Long> hc = cas.getInfectionTimes();
                   /* User te=User.getUser("NJConservative9");
                    if(c==304){
                    	if(!hc.containsKey(te)){
                    		//System.out.println()
                    		throw new RuntimeException(hc+"\n 304 Ne contient pas NJConservative9");
                    	}
                    }*/
                    TreeMap<Long,HashMap<String,Double>> ctimesc=cas.getInfections();
                    told=0;
                    for(Long tc:ctimesc.keySet()){
                    	HashMap<String,Double> tusers=ctimesc.get(tc);
                    	//System.out.println(tc);
                    	for(String w:tusers.keySet()){
                    		double p=1.0-globalExtern;
                    		Long l=tc;
                    		/*if(l<=1){
                    			p=0.0;
                        	}*/
                    		//System.out.println(w+" "+l);
                    		User uw=User.getUser(w);
                    		HashMap<String,Link> preds=uw.getPredecesseurs();
                    		int npd=0;
                    		for(String v:preds.keySet()){
                    			User uv=User.getUser(v);
                    			Long hl2=hc.get(v);
                    			long l2=(hl2!=null)?hl2:-2;
                    			//System.out.println(v+" "+l2);
                        		
                    			if(l2>cas.getNbInitSteps()){
                    				//System.out.println("> nbInits => "+l2);
                    				continue;
                    			}
                    			//System.out.println("l1 = "+l+" l2="+l2);
                    			if (((contaMaxDelay>=0) && (l2>=(l-contaMaxDelay)) && (l2<l) && (l2>=0)) || ((contaMaxDelay<0) && (hl2!=null) && (l2<l))){
                    				Link lvw=preds.get(v);
                    				double kvw=lvw.getVal();
                    				//Double pv=pc.get(uv);
                    				//pv=(pv==null)?0.0:pv;
                    				
                    				/*if(pv==null){
                    					throw new RuntimeException("Pas vu "+v+" alors que time actuel = "+l+" et time de v = "+l2);
                    				}*/
                    				/*if(pv<0.000001){
                    					pv=0.000001;
                    				}*/
                    				double x=1.0; //(safety*pv)+(1.0-safety);
                    				p*=(1.0-x*kvw);
                    				npd++;
                    			
                    			}
                    		}
                    		if (npd>0){
                        	   p=1.0-p;
                    		}
                        	/*if(p<0.000001){
                        		//p=Double.MIN_VALUE;
                        		p=0.000001;
                        	}*/
                        
                        	pc.put(w, p);
                        	if (p<0){
                        		System.out.println("Blem user "+w+" pour cascade "+c+" => p = "+p);
                        	}
                        	if (displayLikelihood){
                        	
                        		if (l>1){
                        			if (p>0){
                        				sump+=Math.log(p);
                        			}
                        			else{
                        				System.out.println("p="+p);
                        				sump-=1000000.0;
                        			}
                        		}
                        	
                        		if ((l>=1) && (l<=cas.getNbInitSteps())){
                        			HashMap<String,Link> succs=uw.getSuccesseurs();
                        			for(String v:succs.keySet()){
                        				if(!pus.contains(v)){
                        					continue;
                        				}
                        				//User uv=User.getUser(v);
                        				Long hl2=hc.get(v);
                        				if((hl2==null) || ((contaMaxDelay>=0) && (hl2>(l+contaMaxDelay)))){
                        					//Double pv=pc.get(v);
                            				//pv=(pv==null)?0.0:pv;
                        					//double pv=p;
                        					/*if(pv<0.000001){
                            					pv=0.000001;
                            				}*/
                        					double x=1.0; //(safety*pv)+(1.0-safety);
                        					double kvw=succs.get(v).getVal();
                        					kvw=1.0-x*kvw;
                        					if(kvw<=0){
                        						sumnp-=1000000.0;
                        						System.out.println("kvw="+kvw+" x="+x+" val="+succs.get(v).getVal());
                        					}
                        					else{
                        						sumnp+=Math.log(kvw);
                        					}
                        				}
                        			}
                                    
                        		}
                            
                        	}
                    	}
                    	if(told!=0){
                    		tmoy+=tc-told;
                    		
                    		tnb++;
                    	}
                    	told=tc;
                    }
            }
            
          //  System.out.println("Avg delay = "+tmoy/tnb);
            
            if (displayLikelihood){
            	//if(sumR<0){
            		System.out.println("Likelihood = "+(sump+sumnp)+"\t"+sump+"\t"+sumnp);
            	//}
            	/*else{
            		System.out.println("Likelihood = "+(sump+sumnp)+" \t "+sumR+" \t "+(sump+sumnp-sumR));
            	}*/
            }
           
            
            int nbChanges=0;
            sum=0.0;
            sumR=0.0;
            int nb1=0;
            // Estimate weights ;
            System.out.println("w... pour "+spos.size()+" liens");
            double sc=0.0;
            for(Link l : allLinks) {
                User uv=(User)l.getNode1();
                User uw=(User)l.getNode2();
                double kvw=l.getVal();
                double okvw=kvw;
                
                if (okvw==1){
                	nb1++;
                }
                HashSet<Integer> cpos=spos.get(l);
                if (cpos==null){
                    l.setVal(0.0);
                    //asup.add(l);
                    continue;
                }
               
                //HashSet<Integer> cneg=sneg.get(l);
                
                double npos=cpos.size();
                Double nneg=sneg.get(l);
                nneg=(nneg==null)?0.0:nneg;
                nneg+=addNeg;
                
                
                double sp=0.0;
                for(Integer c:cpos){
                    HashMap<String,Double> pc=P.get(c);
                    double x=1.0;
                    /*if(safety>0){
                    	Double pv=pc.get(v);
                    	//pv=(pv==null)?0.0:pv;
                    	x=(safety*pv)+(1.0-safety);
                    }*/
                    //npos+=x;
                    Double pu=pc.get(uw.getName());
                    if (pu!=null){
                        sp+=(x/pu);
                    }
                    else{
                    	throw new RuntimeException(c+"=> pu null ");
                    }
                }
                /*if(npos<=0.00001){
                	npos=0.00001;
                }*/
               /* if((safety>0) && (cneg!=null)){
                	for(Integer c:cneg){
                		HashMap<User,Double> pc=P.get(c);
                		double x=1.0;
                		if(safety>0){
                			Double pv=pc.get(v);
                			//pv=(pv==null)?0.0:pv;
                			x=(safety*pv)+(1.0-safety);
                		}
                		nneg+=x;
                	}	
                	
                }
                else{
                	if(cneg!=null){
                		nneg=cneg.size();
                	}
                	
                //}*/
                //nneg+=addNeg;
                
                
                kvw*=sp;
                double n=(1.0*npos+1.0*nneg);
                double lambda=lambdaReg;
                int regType=l1reg;
                if(l.getName().equals("individualExtern")){
                	lambda=individualExtern;
                	regType=1;
                }
                
            	if(iteration==(nbIter-1)){
            		if(regType>1){
            			regType=0;
            			//lambda=1.0;
            		}
            	}
                if(regType<=0){
                	kvw*=(1.0/n);
                	
                }
                else{
                	//double f=Math.exp(0.1*(-1.0*npos-1.0*nneg));
                	double reg=lambda; //
                	if(regType==2){
                		reg=reg/(nbParticip.get(uv)); ///(sumParticip/nbParticip.size())); //1.0+Math.log(1.0*npos));
                	}
                	if(regType==3){
                		reg=reg/(npos+1); ///(sumParticip/nbParticip.size())); //1.0+Math.log(1.0*npos));
                	}
                	if(regType==4){
                		reg=reg*(Math.exp(-1.0*npos)); ///(sumParticip/nbParticip.size())); //1.0+Math.log(1.0*npos));
                		//System.out.println(reg);
                	}
                	if(regType==5){
                		reg=reg/(nbParticip.get(uv)-nneg+1); ///(sumParticip/nbParticip.size())); //1.0+Math.log(1.0*npos));
                	}
                	if(reg<0.00000001){
                		reg=0.00000001;
                	}
                	sc=((1.0*npos)+(1.0*nneg)+reg);
                	double x=Math.pow(sc,2.0)-(4.0*reg*kvw);
                	if(x<-0.01){
                		throw new RuntimeException(kvw+" : sc="+sc+" sp="+sp+" okvw="+okvw+" x="+x+" reg="+reg);
                	}
                	if(x<0){x=0.0;}
                	kvw=(sc-Math.sqrt(x))/(2.0*reg);
                	if((Double.isNaN(kvw)) || Double.isInfinite(kvw)){
                		throw new RuntimeException(kvw+" => sc="+sc+" sp="+sp+" okvw="+okvw+" x="+x+" reg="+reg);
                	}
                	sumR+=reg*kvw;
                }
                if (kvw>=1){
                	/*System.out.println("v "+v.getName()+" w "+w.getName()+" = "+kvw+" "+okvw+" "+npos+" "+nneg);
                	for(Integer c:cpos){
                        HashMap<User,Double> pc=P.get(c);
                        Double pu=pc.get(w);
                        //System.out.println(c + "=> "+userTimeContamination.get(c));
                        if (pu!=null){
                        	System.out.println(c+"=> pu = "+pu);
                        }
                        else{
                        	throw new RuntimeException(c+"=> pu null ");
                        }
                    }*/
                	kvw=1.0;
                	if(iteration<(nbIter-1)){
                		kvw-=0.00001;
                	}
                }
                if((unbiased>0) && (iteration<(nbIter-1))){
                	
                	double p=kvw;
                	// Wilson Method for taking the center of a given confidence interval
                	// unbiased = 2.576 => 99% confidence interval for kvw
                	// unbiased = 1.960 => 95% confidence interval for kvw
                	// unbiased = 1.645 => 90% confidence interval for kvw
                	double den=(2.0*(n+z2));
                	double num=((2.0*n*p)+z2);
                	//double num2=(-1.0-Math.sqrt(z2*(z2-2.0-(1.0/n)+4.0*p*(n*(1.0-p)+1.0))));
                	double num2=0; //-Math.sqrt(z2*(z2+(4.0*n*p*(1.0-p))));
                	kvw=(num+num2)/den;
                	//double erf=ErrorFunction.erf(unbiased/(Math.sqrt((2.0*kvw*(1.0-kvw))/n)));
                	//kvw=erf*kvw; //+(1.0-erf)*0.5;
                	//if(((num+num2)<0) || (Double.isNaN(kvw))){
                	//	throw new RuntimeException("p="+p+" z2="+z2+" n="+n+" num="+num+" num2="+num2+" den="+den+" kvw="+kvw);
                	//} 
                	/*double den=Math.log(1+userTimeContamination.size());
                	if(unbiased>1){

                		kvw=(Math.log(1.0+n)/Math.log(unbiased))*kvw/den;
                	}
                	else{
                		kvw=(Math.log(1.0+n))*kvw/den;
                	}*/
                	
                }
                if (kvw>=1){
                	kvw=1.0;
                }
                if(kvw<=0){
                	kvw=0.0;
                }
                if((kvw>=(1-0.000001)) && (iteration<(nbIter-1))){
                		kvw=1.0-0.000001;
                	
                }
            	if((kvw<=(0.000001)) && (iteration<(nbIter-1))){
            		kvw=0.000001;
            	}
        	
                sum+=kvw;
                l.setVal(kvw);
                if (kvw!=okvw){
                	nbChanges++;
                }
                
                HashMap<String,Double> h=pij.get(uv.getName());
                if(h==null){
                	h=new HashMap<String,Double>();
                	pij.put(uv.getName(), h);
                }
                h.put(uw.getName(), kvw);
                //System.out.println("lien "+v.getID()+","+w.getID()+" => "+kvw);
            }
            
            
            
            /*if (nbChanges==0){
            	break;
            }*/
           
            
            
           
        } 
        
        loaded=true;
        
        if (modelFile.length()==0){
    		modelFile="propagationModels/ICmodel_step"+ploader.getStep()+"_ratioInits-"+ploader.getRatioInits()+"_nbMaxInits-"+ploader.getNbMaxInits()+"_db-"+ploader.getDb()+"_cascadesCol-"+ploader.getCollection()+"_start"+ploader.getStart()+"_nbC"+ploader.getNbC()+"/users"+usersCollection+"_linkThreshold"+userLinkThreshold+"_contaMaxDelay"+contaMaxDelay+"_addNeg-"+addNeg+"_unbiased-"+unbiased+"_l1reg-"+l1reg+"_lambdaReg-"+lambdaReg+"_globalExtern-"+globalExtern+"_individualExtern-"+individualExtern;
    	}
        
    }
   
   
    
   
    public double smooth(double val){
	   double v=smoothing;
	   return val*(1.0-v*2)+v;
   }
   
   
   
   
    /**
     * @param args
     */
    public static void main(String[] args) {
    	
    	//System.out.println("Usage : IC <db> <cascadesCol> <users_db> <users_col> <maxDelay> <ratio_inits> <nb_initMax> <nb_iter> <addNeg> <unbiased> <l1reg> <lambdaReg> <globalExtern> <individualExtern>");
    	HashMap<String,String> hargs=ArgsParser.parseArgs(args);
    	String db=(hargs.containsKey("db"))?hargs.get("db"):"digg";
		String cascadesCol=(hargs.containsKey("c"))?hargs.get("c"):"cascades_1";
		String dbusers=(hargs.containsKey("dbus"))?hargs.get("dbus"):db;
		String userscol=(hargs.containsKey("us"))?hargs.get("us"):"users_1";
		Integer maxDelay=(hargs.containsKey("mD"))?Integer.parseInt(hargs.get("mD")):-1;
        String ratioInits=(hargs.containsKey("rI"))?hargs.get("rI"):"1.0";
        String nbInitsMax=(hargs.containsKey("nI"))?hargs.get("nI"):"-1";
        Integer nbIter=(hargs.containsKey("iter"))?Integer.parseInt(hargs.get("iter")):100;
        Double lambdaReg=(hargs.containsKey("lR"))?Double.valueOf(hargs.get("lR")):0.0;
        Integer regType=(hargs.containsKey("rT"))?Integer.parseInt(hargs.get("rT")):1;
        String nbC=(hargs.containsKey("nbC"))?hargs.get("nbC"):"-1";
		String start=(hargs.containsKey("start"))?hargs.get("start"):"1";
        
    	PropagationStructLoader ploader=new MultiSetsPropagationStructLoader(db, cascadesCol,(long)1,ratioInits,nbInitsMax,start,nbC);
		IC myModel = new IC(maxDelay); // "propagationModels/ICmodel2_3600_1_cascades2_users1s.txt",50) ;
        myModel.learn(ploader,dbusers, userscol, nbIter, 1.0, 0.1, 0.3,0.0,0.0,regType,lambdaReg,0.0,0.0); //0.00001); //Double.valueOf(args[5]));
        myModel.save();
    	
    	
    	//System.out.println(Math.sqrt(0.002154));
    	//test();
    }
        
    

    


}
