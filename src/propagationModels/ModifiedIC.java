package propagationModels;

import java.io.BufferedReader;

import cascades.CascadesLoader;

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
public class ModifiedIC implements PropagationModel {
   
	private Random r;
	private long maxIter=50;
	private String modelFile="";
	private boolean loaded=false;
	private boolean inferProbas=false;
	private boolean infiniteDelay=true;
	private int inferMode=0;
	//private long step=1;
	
	/*public ModifiedIC(){
		this("");
	}*/
	
	public ModifiedIC(String modelFile,int inferMode){
		this.modelFile=modelFile;
		this.inferMode=inferMode;
		//this.maxIter=maxIter;
		r = new Random() ;
	}
	
	
	public ModifiedIC(boolean infiniteDelay){
		r = new Random() ;
		this.infiniteDelay=infiniteDelay;
	}
	
	
	
	public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(User.users.keySet());
	}
	public int getContentNbDims(){
		return 0;
	}
	
	public String toString(){
		/*if(!loaded){
			if(modelFile.equals("")){throw new RuntimeException("No model to load");}
			load();
		}*/
		String sm=modelFile.replaceAll("/", "/");
		return("ModifiedIC_infiniteDelay-"+infiniteDelay+"_inferMode-"+inferMode+"_"+sm);
	}
	public String getName(){
		String sm=modelFile.replaceAll("/", "__");
		return sm;
	}
	public int infer(Structure struct) {
		/*if(inferProbas){
			return(inferProbas(struct));
		}
		else{*/
			return(inferSimulationProbas(struct));
		//}
	}
	
	
	private long sampleTime(double v, long tpos){
		long inf=1;
		long t=inf;
		long sup=tpos;
		while((sup-inf)>0){
			t=(sup-inf+1)/2;
			double tot=1.0-Math.pow(1.0-v, sup-inf+1);
			double pinf=1.0-Math.pow(1.0-v, t);
			double r=pinf;
			double x=Math.random()*tot;
			if(x<r){
				sup=inf+t-1;
			}
			else{
				inf=inf+t;
			}
		}
		if(sup<inf){
			throw new RuntimeException("Sup < Inf !");
		}

		return inf;
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
    	PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashMap<String,Long> contagious = new HashMap<String,Long>();
        HashMap<String,Long> times=new HashMap<String,Long>();
        HashSet<String> infectedBefore = new HashSet<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        
        long maxt=0;
        TreeSet<Long> cTimes=new TreeSet<Long>();
	    for(long t:contaminated.keySet()){
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
        User currentUser ;
        //int it=tt;
        long it=firstNewT;
        long horizon=1;
        if(this.infiniteDelay){
        	horizon=this.maxIter;
        }
        boolean ok=true;
        long currentT=firstNewT-1;
        //boolean noMax=true;
        while(ok){
        	
        	for(String contagiousU : contagious.keySet()) {
                User contagiousUser=User.getUser(contagiousU);
                Long time=contagious.get(contagiousU);
                HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
                //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
                long tpos=horizon;
                if((time+horizon)>maxIter){
                	tpos=maxIter-time;
                }
                long minT=time+1;
                if(time<(firstNewT-1)){
                	tpos-=(firstNewT-time-1);
                	minT=firstNewT;
                }
                if(tpos<0){
                	continue;
                }
                for(Link lsuc : succs.values()){ //get(contagiousUser.getID()).keySet()) {
                    User neighbour=(User)lsuc.getNode2();
                    if(infectedBefore.contains(neighbour.getName()))
                        continue ;
                    double v=lsuc.getVal();
                    double p=1.0-Math.pow(1.0-v, tpos);
                    if(Math.random()<v) {
                    	long ti=sampleTime(v,tpos)+minT-1;
                        Long oldT=contagious.get(neighbour.getName());
                        if((oldT!=null) && (ti<oldT)){
                        		HashMap<String,Double> infectedStep=infections.get(oldT);
                        		infectedStep.remove(neighbour.getName());
                        		if(infectedStep.size()==0){
                        			infections.remove(oldT);
                        			cTimes.remove(oldT);
                        		}
                        }
                        
                        if((oldT==null) || (ti<oldT)){
                        	HashMap<String,Double> infectedStep=infections.get(ti);
                    		if(infectedStep==null){
                    			infectedStep=new HashMap<String,Double>();
                    			infections.put(ti, infectedStep);
                    		}
                    		infectedStep.put(neighbour.getName(), 1.0);
                    		cTimes.add(ti);
                    		times.put(neighbour.getName(),ti);
                    		
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
        it=maxIter+1;
        if(inferMode==1){
	        HashMap<String,Double> notYet=new HashMap<String,Double>();
	        
	        for(String user:User.users.keySet()){
	        	if(!times.containsKey(user)){
	        		notYet.put(user,1.0);
	        	}
	        }
	         
	        for(String user : times.keySet()) {
	            Long time=times.get(user);
	            if((time+horizon)<it){
	            	continue;
	            }
	            User contagiousUser=User.getUser(user); 
	    		HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
	            //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
	           
	            for(Link lsuc : succs.values()){ //get(contagiousUser.getID()).keySet()) {
	                User neighbour=(User)lsuc.getNode2();
	                String v=neighbour.getName();
	                if(times.containsKey(v))
	                    continue ;
	                
	                Double p=notYet.get(v);
	                p*=(1.0-lsuc.getVal());
	                notYet.put(v,p);
	            }
	        }
	        for(String user:notYet.keySet()){
	        	infectedstep.put(user,1.0-notYet.get(user));
	        	//System.out.println(user + " : "+(1.0-notYet.get(user)));
	        }
	        
	       
	        infections.put(it,infectedstep);
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
	 public int inferSimulationProbas_old(Structure struct) {
        if (!loaded){
        	load();
        }
    	PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashSet<String> infected = new HashSet<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        
        
        int tt=1;
	    for(long t:contaminated.keySet()){
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	infections.put((long)tt, (HashMap<String,Double>) inf.clone());
	    	
	    	tt++;
	    }
	    int firstNewT=tt;
        HashSet<String> contagious=new HashSet<String>();
        contagious.addAll(infected);
        HashMap<String,Double> infectedstep=new HashMap<String,Double>();
        User currentUser ;
        
        int it=tt;
        for(int iteration =tt ; iteration < maxIter ; iteration++) {
        	
        	for(String contagiousU : contagious) {
                User contagiousUser=User.getUser(contagiousU);
        		HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
                //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
               
                for(Link lsuc : succs.values()){ //get(contagiousUser.getID()).keySet()) {
                    User neighbour=(User)lsuc.getNode2();
                    if(infected.contains(neighbour.getName()))
                        continue ;
                    if(function(lsuc) ) {
                    	//System.out.println(neighbour.getName()+" infecte !! ");
                        infectedstep.put(neighbour.getName(),1.0) ;
                        infected.add(neighbour.getName());   
                    }
                }
            }
           
            
        	contagious.addAll(infected);
            
            infections.put((long)iteration,infectedstep);
            infectedstep = new HashMap<String,Double>() ;
            it++;
           
        }
        HashMap<String,Double> notYet=new HashMap<String,Double>();
        for(String user:User.users.keySet()){
        	if(!infected.contains(user)){
        		notYet.put(user,1.0);
        	}
        }
        
        for(String contagiousU : contagious) {
            User contagiousUser=User.getUser(contagiousU);
    		HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
            //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
           
            for(Link lsuc : succs.values()){ //get(contagiousUser.getID()).keySet()) {
                User neighbour=(User)lsuc.getNode2();
                String v=neighbour.getName();
                if(infected.contains(v))
                    continue ;
                Double p=notYet.get(v);
                //System.out.println(p);
                p*=(1.0-lsuc.getVal());
                notYet.put(v,p);
            }
        }
        for(String user:notYet.keySet()){
        	infectedstep.put(user,1.0-notYet.get(user));
        }
       //System.out.println(infectedstep);
        infections.put((long)it,infectedstep);
        
        //infections.add(infectedstep);
        pstruct.setInfections(infections) ;
        return 0;
    }
	
	
	
    public int inferSimulation(Structure struct) {
        if (!loaded){
        	load();
        }
    	PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashSet<String> infected = new HashSet<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        
        int tt=1;
	    for(long t:contaminated.keySet()){
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	infections.put((long)tt, (HashMap<String,Double>) inf.clone());
	    	
	    	tt++;
	    }
	    int firstNewT=tt;
        HashSet<String> contagious=new HashSet<String>();
        contagious.addAll(infected);
        HashMap<String,Double> infectedstep=new HashMap<String,Double>();
        User currentUser ;
        for(int iteration =tt ; iteration <= maxIter ; iteration++) {
        	
        	for(String contagiousU : contagious) {
                User contagiousUser=User.getUser(contagiousU);
        		HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
                //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
               
                for(Link lsuc : succs.values()){ //get(contagiousUser.getID()).keySet()) {
                    User neighbour=(User)lsuc.getNode2();
                    if(infected.contains(neighbour.getName()))
                        continue ;
                    if(function(lsuc) ) {
                        infectedstep.put(neighbour.getName(),1.0) ;
                        infected.add(neighbour.getName());   
                    }
                }
            }
           
            
        	contagious.addAll(infected);
            
            infections.put((long)iteration,infectedstep);
            infectedstep = new HashMap<String,Double>() ;
           
        }
        //infections.add(infectedstep);
        pstruct.setInfections(infections) ;
        return 0;
    }
   
   
    public void load(){
		System.out.println(" Load "+modelFile);
    	String filename=modelFile;
        User.reinitAllLinks();
        BufferedReader r;
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          
          String[] sline;
          //maxIter=Integer.parseInt(sline[1]);
          while((line=r.readLine()) != null) {
        	  if(line.startsWith("maxIter")){
              	sline=line.split("=");
                  maxIter=Long.valueOf(sline[1]);
                  continue;
              }
        	 
          	if(line.startsWith("infiniteDelay")){
              	sline=line.split("=");
              	infiniteDelay=Boolean.valueOf(sline[1]);
                  continue;
             }
        	  String[] tokens = line.split("\t") ;
            if(tokens[2].startsWith("NaN"))
                tokens[2]="0.0" ;
           
            double d = Double.parseDouble(tokens[2]) ;
            if(d<=Double.MIN_VALUE)
                continue ;
            User source=User.getUser(tokens[0]);
            User target=User.getUser(tokens[1]);
            Link l=new Link(source,target,d);
            source.addLink(l);
            target.addLink(l);
            //System.out.println("new link "+l);
          }
          r.close();
          loaded=true;
        }
        catch(IOException e){
        	System.out.println("Probleme lecture modele "+filename);
        }
        
    }

    public void save() {
    	
		String filename=modelFile;
        try{
          PrintStream p = new PrintStream(filename) ;
          p.println("maxIter="+maxIter);
          
          p.println("infiniteDelay="+infiniteDelay);
          for(User uS : User.users.values()) {
            HashMap<String,Link> succs=uS.getSuccesseurs();
            for(Link lsuc: succs.values()) {
                p.println(uS.getName()+"\t"+lsuc.getNode2().getName()+"\t"+lsuc.getVal());
            }
          }
        }
        catch(IOException e){
        	System.out.println("Probleme sauvegarde modele "+filename);
        	
        }
    }
   
 
   
 	
 	
    // "Prediction of Information Diffusion Probabilities for Independent Cascade Model"
    // Modified such that taking into account multiple fails of transmission if infiniteDelay (if not, classical IC)
 	// contaMaxDelay = 1 => propagation can only be done between two users contamined in contiguous steps (as defined in the saito paper). contaMaxDelay=n => propagation can be between steps separated one from each other from at max n steps. contaMaxDelay=-1 => a user can have contamined every user contamined after him  
    public void learn(CascadesLoader loader, String db, String usersCollection, int nbIter, double userLinkThreshold, double min_init, double max_init) {
        
    	boolean displayLikelihood=true;
       
        // Pour chaque cascade, liste id users avec leur temps de contamination
        HashMap<Integer,HashMap<User,Long>> userTimeContamination=loader.getTimeSteps(true);
        Long maxTime=1l;
        for(Integer i:userTimeContamination.keySet()){
        	HashMap<User,Long> u=userTimeContamination.get(i);
        	for(Long t:u.values()){
        		if(t>maxTime){
        			maxTime=t;
        		}
        	}
        }
        this.maxIter=maxTime;
        
        HashSet<User> users=new HashSet<User>(User.users.values());
        // Pour tous les utilisateurs on recupere leurs liens
        for(User u:users){
            u.loadLinksFrom(db, usersCollection, userLinkThreshold);
            System.out.println("Liens user "+u.getName()+" charge");
            //System.out.println(u.getSuccesseurs().values());
        }
       
        System.out.println("Initialisation valeurs liens");
        HashSet<Link> allLinks=new HashSet<Link>();
        // Tous les liens obtiennent une valeur arbitraire tiree entre min_init et max_init
        double dif=max_init-min_init;
        for(User u:User.users.values()){
            HashMap<String,Link> succs=u.getSuccesseurs();
            System.out.println("User "+u);
            
            for(Link l:succs.values()){
            	
                double v=(Math.random()*dif)+min_init;
                l.setVal(v);
                /*if(u.getName().equals("iddaaaliyiz")){
                	System.out.println(u+","+l.getNode2()+"=>"+v);
                }*/
                //System.out.println("Link "+l);
                allLinks.add(l);
            }
        }
       
        System.out.println(allLinks.size());
        
        // Table de Probabilite de contamination pour chaque user, indexee par cascade id
        HashMap<Integer,HashMap<User,Double>> P;
       
        // Table des cascades pour chaque lien <v,w> ou time de v == time de w - 1
        HashMap<Link,HashSet<Integer>> spos=new HashMap<Link,HashSet<Integer>>();
       
        // Table des cascades pour chaque lien <v,w> ou time de v == time de w - 1
        //HashMap<Link<User>,HashSet<Integer>> sneg=new HashMap<Link<User>,HashSet<Integer>>();
        HashMap<Link,Long> tneg=new HashMap<Link,Long>();
        HashMap<Link,Long> tpos=new HashMap<Link,Long>();
        
          
        System.out.println("Compute sneg et spos");
        // compute sneg et spos
       	for(Integer c:userTimeContamination.keySet()) {
        		HashMap<User, Long> hc = userTimeContamination.get(c);
        		System.out.println("Cascade c = "+c);
        		for(User w:hc.keySet()){
        			//User user=w;
        			Long l=hc.get(w);
        			//System.out.println("User w = "+w+" => "+l);
        			HashMap<String,Link> succs=w.getSuccesseurs();
        			/*if(w.getName().equals("iddaaaliyiz")){
                    	System.out.println(w+","+succs.size()+" succs");
                    }*/
                    for(String v:succs.keySet()){
                        User uv=User.getUser(v);
                    	Long hl2=hc.get(uv);
                        long l2=(hl2!=null)?hl2:-2;
                        if (((!infiniteDelay) && (l2==l+1)) || ((infiniteDelay) && (l2>l))){
                        	Link lvw=succs.get(v);
                        	HashSet<Integer> cpos=spos.get(lvw);
                            if (cpos==null){
                                cpos=new HashSet<Integer>();
                                spos.put(lvw, cpos);
                            }
                            cpos.add(c);
                            Long npos=tpos.get(lvw);
                    		npos=(npos!=null)?npos:0;
                    		if(infiniteDelay){
                    			npos+=l2-l;
                    		}
                    		else{
                    			npos++;
                    		}
                    		tpos.put(lvw, npos);
                        }
                        else{
                        	if ((l2==-2) || ((!infiniteDelay) && (l2>(l+1)))){
                        		Link lvw=succs.get(v);
                        		Long nneg=tneg.get(lvw);
                        		nneg=(nneg!=null)?nneg:0;
                        		if(infiniteDelay){
                        			nneg+=maxTime-1-l;
                        		}
                        		else{
                        			nneg++;
                        		}
                        		tneg.put(lvw, nneg);
                        	}
                        }
                    }
        		}
        }
        
       	HashSet<Link> asup=new HashSet<Link>();
        for(Link l : allLinks) {
        	HashSet<Integer> cpos=spos.get(l);
            if (cpos==null){
                l.setVal(0.0);
                asup.add(l);
                continue;
            }
        }
        for(Link l : asup) {
        	allLinks.remove(l);
        	User v=(User)l.getNode1();
            User w=(User)l.getNode2();
            v.removeSuccesseur(w);
            w.removePredecesseur(v);
        }
        
        for(int iteration = 0 ; iteration<nbIter ; iteration++) {
           
            System.out.println("iteration : "+iteration);
           
            double sump=0;
            double sumnp=0;
            
            
            
            // Estimate P
            P=new HashMap<Integer,HashMap<User,Double>>();
            System.out.println("P... pour "+userTimeContamination.size());
            int iii =0;
            for(Integer c:userTimeContamination.keySet()) {
//                    System.out.print((iii++) + ", ");
                    HashMap<User,Double> pc=new HashMap<User,Double>();
                    P.put(c, pc);
                    HashMap<User, Long> hc = userTimeContamination.get(c);
                    for(User w:hc.keySet()){
                    	
                        double p=1.0;
                        Long l=hc.get(w);
                        User user=w;
                        HashMap<String,Link> preds=user.getPredecesseurs();
                        int npd=0;
                        for(String v:preds.keySet()){
                            User uv=User.getUser(v);
                        	Long hl2=hc.get(uv);
                            long l2=(hl2!=null)?hl2:-2;
                            //System.out.println("l1 = "+l+" l2="+l2);
                            if (((!infiniteDelay) && (l2==(l-1)) && (l2>=0)) || ((infiniteDelay) && (hl2!=null) && (l2<l))){
                                Link lvw=preds.get(v);
                                double kvw=lvw.getVal();
                               
                                p*=(1.0-kvw);
                                npd++;
                            }
                        }
                        if (npd>0){
                        	p=1.0-p;
                        }
                        
                        pc.put(w, p);
                        if (p<0){
                        	System.out.println("Blem user "+w.getName()+" pour cascade "+c+" => p = "+p);
                        }
                        if (displayLikelihood){
                        	
                        	if (l>1){
                        		if (p>0){
                                	sump+=Math.log(p);
                                }
                            	else{
                            		//System.out.println("p="+p);
                            		sump-=1000000.0;
                            	}
                        	}
                        	
                        	if (l>=1){
                        		HashMap<String,Link> succs=user.getSuccesseurs();
                        		for(String v:succs.keySet()){
                                    User uv=User.getUser(v);
                        			Long hl2=hc.get(uv);
                        			hl2=(hl2==null)?((infiniteDelay)?maxTime:(l+2)):hl2;
                        			
                                    if(hl2>l){
                                    	long difft=hl2-(l+1);
                                    	if(difft>0){
                                    		double kvw=succs.get(v).getVal();
                                        	kvw=1.0-kvw;
                                        	if(kvw<=0){
                                        		sumnp-=difft*1000000.0;
                                        		//System.out.println("kvw="+kvw);
                                        	}
                                        	else{
                                        		sumnp+=difft*Math.log(kvw);
                                        	}
                                    	}
                                    	
                                    }
                        		}
                                    
                        	}
                            
                        }
                    }
            }
            
            
            if (displayLikelihood){
            	System.out.println("Likelihood = "+(sump+sumnp));
            }
            
            
            int nbChanges=0;
            double sum=0;
            int nb1=0;
            // Estimate weights ;
            System.out.println("w... pour "+spos.size()+" liens");
            for(Link l : allLinks) {
                User v=(User)l.getNode1();
                User w=(User)l.getNode2();
                double kvw=l.getVal();
                double okvw=kvw;
                sum+=okvw;
                if (okvw==1){
                	nb1++;
                }
                HashSet<Integer> cpos=spos.get(l);
                if (cpos==null){
                    l.setVal(0.0);
                    //asup.add(l);
                    continue;
                }
                Long nneg=tneg.get(l);
                nneg=(nneg!=null)?nneg:0;
                Long npos=tpos.get(l);
                npos=(npos!=null)?npos:0;
                /*
                if (cneg!=null){
                    nneg=cneg.size();
                }*/
                kvw*=(1.0/(1.0*npos+1.0*nneg));
                double sp=0.0;
                for(Integer c:cpos){
                    HashMap<User,Double> pc=P.get(c);
                    Double pu=pc.get(w);
                    if (pu!=null){
                        sp+=(1.0/pu);
                    }
                }
                kvw*=sp;
                if (kvw>1){
                	kvw=1.0;
                }
                if(kvw<0){
                	throw new RuntimeException("kvw < 0 pour "+v.getName()+"=>"+w.getName());
                }
                l.setVal(kvw);
                if (kvw!=okvw){
                	nbChanges++;
                }
                //System.out.println("lien "+v.getID()+","+w.getID()+" => "+kvw);
            }
            
            
            
            System.out.println("nbChanges ="+nbChanges);
            System.out.println("Sum weights = "+sum);
            System.out.println("nb 1 = "+nb1);
            if (nbChanges==0){
            	break;
            }
            
           
        } 
        
        loaded=true;
        
        if (modelFile.length()==0){
    		modelFile="propagationModels/ModifiedIC_step"+loader.getStep()+"db_"+db+"_cascades"+loader.getCollection()+"_users"+usersCollection+"_linkThreshold"+userLinkThreshold+"_maxIter"+maxIter+((loader.getEmptyIgnored())?"_sansStepsVides":"")+"_infiniteDelay-"+infiniteDelay;
    	}
        
    }
   
   
    
    

   
    public boolean function(Link link) {
       
        //System.out.println(weights.get(source).get(target));
        try {
        	//System.out.print(link);
            return r.nextDouble() <= link.getVal() ;
        } catch (NullPointerException e) {
            return false ;
        }
       
    }
   
   
   
   
   
   
    /**
     * @param args
     */
    public static void main(String[] args) {
    	String db="digg";
    	if(args.length>0){
    		db=args[0];
    	}
    	String cascadesCol="cascades_1";
    	if(args.length>1){
    		cascadesCol=args[1];
    	}
    	CascadesLoader loader=new CascadesLoader(db, cascadesCol,Integer.parseInt(args[3]),false);
        ModifiedIC myModel = new ModifiedIC(Boolean.valueOf(args[2])); // "propagationModels/ICmodel2_3600_1_cascades2_users1s.txt",50) ;
        myModel.learn(loader,db, "users_1", Integer.parseInt(args[4]), 1, 0.1, 0.3) ;
        myModel.save();
    	//test();
    }
        
    public static void test(){
    	ModifiedIC myModel = new ModifiedIC(true);
        User u1=User.getUser("1");
        User u2=User.getUser("2");
        User u3=User.getUser("3");
        User u4=User.getUser("4");
        User u5=User.getUser("5");
        User u6=User.getUser("6");
        User u7=User.getUser("7");
        User u8=User.getUser("8");
        User u9=User.getUser("9");
        User u10=User.getUser("10");
        Link l=new Link(u1,u2,0.3);
        u1.addLink(l);
        u2.addLink(l);
        l=new Link(u1,u3,0.1);
        u1.addLink(l);
        u3.addLink(l);
        l=new Link(u2,u5,0.5);
        u2.addLink(l);
        u5.addLink(l);
        l=new Link(u3,u1,0.2);
        u3.addLink(l);
        u1.addLink(l);
        l=new Link(u3,u4,0.5);
        u3.addLink(l);
        u4.addLink(l);
        l=new Link(u4,u5,0.1);
        u4.addLink(l);
        u5.addLink(l);
        l=new Link(u5,u6,1.0);
        u5.addLink(l);
        u6.addLink(l);
        l=new Link(u6,u7,0.4);
        u6.addLink(l);
        u7.addLink(l);
        l=new Link(u7,u8,0.3);
        u7.addLink(l);
        u8.addLink(l);
        l=new Link(u7,u9,0.6);
        u7.addLink(l);
        u9.addLink(l);
        l=new Link(u7,u10,0.8);
        u7.addLink(l);
        u10.addLink(l);
        l=new Link(u10,u3,0.4);
        u10.addLink(l);
        u3.addLink(l);
        myModel.loaded=true;
        HashMap<String,Double> init=new HashMap<String,Double>();
        init.put(u1.getName(),1.0);
        TreeMap<Long,HashMap<String,Double>> tinit=new TreeMap<Long,HashMap<String,Double>> ();
		tinit.put((long)1.0, init);
        PropagationStruct p=new PropagationStruct(new Cascade(1,"x",new HashSet<Post>()),tinit, new TreeMap<Long,HashMap<String,Double>>());
        myModel.inferSimulation(p);
        
        /*HashMap<User,HashMap<Integer,Integer>> nbContamined=new HashMap<User,HashMap<Integer,Integer>>();
        for(int i=0;i<99000;i++){
        	myModel.inferSimulation(p);
	        
        	ArrayList<HashMap<String,Double>> inf=p.getInfections();
	        HashMap<String,Double> infect=inf.get(inf.size()-1);
	        for(String u:infect.keySet()){
	  		  User user=User.getUser(u);
	          HashMap<Integer,Integer> h=nbContamined.get(user);
	  		  if(h==null){
	  			  h=new HashMap<Integer,Integer>();
	  			  nbContamined.put(user,h);
	  		  }
	  		  Integer t=2; 
	  		  Integer nb=h.get(t);
	  		  nb=(nb==null)?1:(nb+1);
	  		  h.put(t,nb);
	  	  }
	    }
	    System.out.println(nbContamined);*/
	        
    }


}