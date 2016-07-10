package propagationModels;

import java.io.BufferedReader;

import statistics.Distributions;
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

//import mlpDouble.CPUParams;
public class CTIC implements PropagationModel,ProbabilisticTransmissionModel {
   
	private long maxIter;
	private String modelFile;
	private boolean loaded=false;
	//private boolean inferProbas=false;
	private HashMap<String,HashMap<String,Double>> influences;
	private HashMap<String,HashMap<String,Double>> delais;
	private Distributions distrib;
	//boolean inferFinalProbas=false;
	private int inferMode=1;
	
	public CTIC(){
		this("",1);
		
	}
	
	
	
	public CTIC(String modelFile, int inferMode){
		this.modelFile=modelFile;
		//this.maxIter=maxIter;
		influences=new HashMap<String,HashMap<String,Double>>();
		delais=new HashMap<String,HashMap<String,Double>>();
		//this.maxIter=maxIter;
		distrib=new Distributions();
		this.inferMode=inferMode;
	}
	
	public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(influences.keySet());
	}
	public int getContentNbDims(){
		return 0;
	}
	public Double getProba(String from,String to){
		if(!loaded){
			load();
		}
		HashMap<String,Double> h=influences.get(from);
		if(h==null) return null;
		
		return h.get(to); 
	}
	
	
	public String toString(){
		String sm=modelFile.replaceAll("/", "__");
		return("CTIC_inferMode-"+inferMode+"_"+sm);
	}
	public String getName(){
		String sm=modelFile.replaceAll("/", "__");
		return sm;
	}
	public int infer(Structure struct) {
		//System.out.println("infer!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		/*if(inferProbas){
			return(inferProbas(struct));
		}
		else{*/
			return(inferSimulationProbas(struct));
		//}
	}
	
	
	public int inferSimulation(Structure struct) {
		inferMode=0;
		return(inferSimulationProbas(struct));
	}
	private long sampleTime(double v){
		double x=distrib.nextExp(1.0/v);
	    return ((long)x+1);
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
        
        boolean ok=true;
        long currentT=firstNewT-1;
        //boolean noMax=true;
        while(ok){
        	if(it>=maxIter){
        		break;
        	}
        	//currentT++;
        	for(String contagiousU : contagious.keySet()) {
        		User contagiousUser=User.getUser(contagiousU);
                Long time=contagious.get(contagiousU);
                HashMap<String,Double> r=delais.get(contagiousU);
                HashMap<String,Double> k=influences.get(contagiousU);
                if((r==null) || (k==null)){
                	continue;
                }
		//System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
                                
                for(String v:r.keySet()){ //get(contagiousUser.getID()).keySet()) {
                    if(infectedBefore.contains(v))
                        continue ;
                    
                    Double ruv=r.get(v);
                    Double kuv=k.get(v);
		   // System.out.println("ruv="+ruv+" kuv="+kuv);
                    if((ruv==null) || (kuv==null)){
                    	continue;
                    }
                    if(ruv==0){
                    	continue;
                    }
		    //System.out.println("kuv="+kuv);
                    if(Math.random()<kuv) {
                    	long ti=sampleTime(ruv)+time;
			//System.out.println(v+" => "+ti+" ("+time+")");
                    	if(ti<firstNewT){
                    		continue;
                    	}
                    	if(ti>maxIter){
                    		continue;
                    	}
                    	
                        Long oldT=times.get(v);
                        if((oldT!=null) && (ti<oldT)){
                        	//if(this.inferMode!=2){
                        		HashMap<String,Double> infectedStep=infections.get(oldT);
                        		infectedStep.remove(v);
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
                        		infectedStep.put(v, 0.999);
                        		
                        	//}
                        	cTimes.add(ti);
                    		times.put(v,ti);
                    		
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
        if(this.inferMode>=1){
        	if(this.inferMode==2){
        		infectedBefore=new HashSet<String>();
        		cTimes=new TreeSet<Long>(infections.keySet());
        		for(Long t:cTimes){
        			if(t>=firstNewT){
        				infections.remove(t);
        			}
        			else{
        				infectedBefore.addAll(infections.get(t).keySet());
        			}
        		}
        	}
	        HashMap<String,Double> notYet=new HashMap<String,Double>();
	        
	        for(String user:User.users.keySet()){
	        	if(((this.inferMode==2) && (!infectedBefore.contains(user)))|| (!times.containsKey(user))){
	        		notYet.put(user,1.0);
	        	}
	        }
	         
	        for(String user : times.keySet()) {
	            Long time=times.get(user);
	            HashMap<String,Double> k=influences.get(user);
                if((k==null)){
                	continue;
                }
                //System.out.println(user+" => nb succs = "+k.size());
                                
                for(String v:k.keySet()){ //get(contagiousUser.getID()).keySet()) {
                    if(!notYet.containsKey(v))
	                    continue ;
	                if(v.equals(user)){
	                	continue;
	                }
                    Double kuv=k.get(v);
		   // System.out.println("ruv="+ruv+" kuv="+kuv);
                    if((kuv==null)){
                    	continue;
                    }
                    
	                
	                Double p=notYet.get(v);
	                p*=(1.0-kuv);
	                notYet.put(v,p);
	            }
	        }
	        for(String user:notYet.keySet()){
	        	double p=1.0-notYet.get(user);
	        	infectedstep.put(user,p*0.998+0.001);
	        	//System.out.println(user + " : "+(1.0-notYet.get(user)));
	        }
	        
	       
	        infections.put(it,infectedstep);
        }
        //infections.add(infectedstep);
        //System.out.println(PropagationStruct.getPBeforeT(infections));
        pstruct.setInfections(infections) ;
        return 0;
    }
	
	
	 public int inferSimulationProbas_old(Structure struct) {
		//System.out.println("inferSimulationProbas");
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
        
        boolean ok=true;
        long currentT=firstNewT-1;
        //boolean noMax=true;
        while(ok){
        	//System.out.println(contagious);
        	for(String contagiousU : contagious.keySet()) {
                User contagiousUser=User.getUser(contagiousU);
                Long time=contagious.get(contagiousU);
               // HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
                HashMap<String,Double> r=delais.get(contagiousU);
                HashMap<String,Double> k=influences.get(contagiousU);
                if((r==null) || (k==null)){
                	continue;
                }
		//System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
                                
                for(String v:r.keySet()){ //get(contagiousUser.getID()).keySet()) {
                    if(infectedBefore.contains(v))
                        continue ;
                    
                    Double ruv=r.get(v);
                    Double kuv=k.get(v);
		   // System.out.println("ruv="+ruv+" kuv="+kuv);
                    if((ruv==null) || (kuv==null)){
                    	continue;
                    }
                    if(ruv==0){
                    	continue;
                    }
		    //System.out.println("kuv="+kuv);
                    if(Math.random()<kuv) {
                    	long ti=sampleTime(ruv)+time;
			//System.out.println(v+" => "+ti+" ("+time+")");
                    	if(ti<firstNewT){
                    		continue;
                    	}
                    	if(ti>maxIter){
                    		continue;
                    	}
                        Long oldT=times.get(v);
                        if((oldT!=null) && (ti<oldT)){
                        		HashMap<String,Double> infectedStep=infections.get(oldT);
                        		infectedStep.remove(v);
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
                    		infectedStep.put(v, 1.0);
                    		cTimes.add(ti);
                    		times.put(v,ti);
                    		
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
        if(inferMode==1){
        //System.out.println(times);
        it=maxIter+1;
        HashMap<String,Double> notYet=new HashMap<String,Double>();
        
        /*for(String user:User.users.keySet()){
        	if(!times.containsKey(user)){
        		notYet.put(user,1.0);
        	}
        }*/
         
        for(String user : times.keySet()) {
            Long time=times.get(user);
            
            //User contagiousUser=User.getUser(user); 
    		//HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
            //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
    		HashMap<String,Double> r=delais.get(user);
            HashMap<String,Double> k=influences.get(user);
            if((r==null) || (k==null)){
            	continue;
            }
            for(String v:k.keySet()){ //get(contagiousUser.getID()).keySet()) {
                if(times.containsKey(v))
                    continue ;
                Double ruv=r.get(v);
                Double kuv=k.get(v);
                if((ruv==null) || (kuv==null)){
                	continue;
                }
                if(ruv==0){
                	continue;
                }
                Double p=notYet.get(v);
                if(p==null){
                	p=1.0;
                }
                double psup=kuv*Math.exp(-ruv*(it-time));
                p*=(1.0-psup);
                notYet.put(v,p);
            }
        }
        for(String user:notYet.keySet()){
        	infectedstep.put(user,1.0-notYet.get(user));
        	//System.out.println(user + " : "+(1.0-notYet.get(user)));
        }
        
       
        infections.put(it,infectedstep);
        }
        //System.out.println(infections);
        //infections.add(infectedstep);
        pstruct.setInfections(infections) ;
        
        return 0;
    }
	
	
	// iterative process
    public int inferSimulation_old(Structure struct) {
        if (!loaded){
        	load();
        }
    	PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashSet<String> infected = new HashSet<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        
        int tt=1;
	    HashMap<String,Long> times=new HashMap<String,Long>();
        for(long t:contaminated.keySet()){
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	infections.put((long)tt, (HashMap<String,Double>) inf.clone());
	    	for(String s:inf.keySet()){
	    		times.put(s, t);
	    	}
	    	tt++;
	    }
	    int firstNewT=tt;
        HashSet<String> contagious=new HashSet<String>();
        contagious.addAll(infected);
        HashMap<String,Double> infectedstep=new HashMap<String,Double>();
        User currentUser ;
        
        for(int iteration =tt ; iteration <= maxIter ; iteration++) {
        	
        	for(String contagiousU : contagious) {
                HashMap<String,Double> ri=delais.get(contagiousU);
        		HashMap<String,Double> ki=influences.get(contagiousU);
                if(ri==null || ki==null){
                	continue;
                }
                Long ti=times.get(contagiousU);
                Long t=(long)iteration;
                for(String v:ki.keySet()){ //get(contagiousUser.getID()).keySet()) {
                    if(infected.contains(v))
                        continue ;
                    
                    Double rij=ri.get(v);
                    Double kij=ki.get(v);
                    if(rij==null || kij==null){
                    	continue;
                    }
                    double p=kij*(Math.exp(-rij*(t-1-ti))-Math.exp(-rij*(t-ti)));
                    double x=Math.random();
                    if(x<=p){
                    	infectedstep.put(v,1.0) ;
                        infected.add(v); 
                        times.put(v, t);
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
		String filename=modelFile;
		delais = new HashMap<String,HashMap<String,Double>>();
		influences = new HashMap<String,HashMap<String,Double>>();
        
		//User.reinitAllLinks();
        BufferedReader r;
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          String[] sline;
          boolean probas_mode=false;
          boolean delais_mode=true;
          int nb=0;
          while((line=r.readLine()) != null) {
        	if(line.startsWith("maxIter")){
              	sline=line.split("=");
                  maxIter=Long.valueOf(sline[1]);
                  continue;
            }
        	if(line.contains("<Infections_Probas>")){
       		 	 probas_mode=true;
                 continue;
         	}
         	if(line.contains("</Infections_Probas>")){
                  probas_mode=false;
         		  continue;
         	}
         	if(line.contains("<Delais>")){
      		 	delais_mode=true;
                continue;
        	}
        	if(line.contains("</Delais>")){
                  delais_mode=false;
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
	            HashMap<String,Double> pi=influences.get(i);
		    	if (pi==null){
		    		 pi=new HashMap<String,Double>();
		    		 influences.put(i, pi);
		    	}
		    	pi.put(j,d);
		    	nb++;
         	}
         	if(delais_mode){
	        	String[] tokens = line.split("\t") ;
	            if(tokens[2].startsWith("NaN"))
	                tokens[2]="0.0" ;
	           
	            double d = Double.parseDouble(tokens[2]) ;
	            if(d==0)
	                continue ;
	            
	            String i=tokens[0];
	            String j=tokens[1];
	            HashMap<String,Double> pi=delais.get(i);
		    	if (pi==null){
		    		 pi=new HashMap<String,Double>();
		    		 delais.put(i, pi);
		    	}
		    	pi.put(j,d);
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
		PrintStream p = null;
		try{
        	File file=new File(modelFile);
        	File dir=file.getParentFile();
        	if(dir!=null){
        		dir.mkdirs();
        	}
        	p = new PrintStream(file) ;
        	
        	p.println("maxIter="+maxIter);
        	p.println("<Infections_Probas>");
			for(String i : influences.keySet()){
				HashMap<String,Double> pi=influences.get(i);
				for(String j:pi.keySet()){
					p.println(i+"\t"+j+"\t"+pi.get(j)) ;
				}
			}
			p.println("</Infections_Probas>");
			p.println("<Delais>");
			for(String i : delais.keySet()){
				HashMap<String,Double> pi=delais.get(i);
				for(String j:pi.keySet()){
					p.println(i+"\t"+j+"\t"+pi.get(j)) ;
				}
			}
			p.println("</Delais>");
			
				
			p.close();
			
		}
    	catch(IOException e){
    		System.out.println("Probleme sauvegarde modele "+modelFile);
    	}
	}
	
	public String getModelFile(){
		return modelFile;
	}
	
	public void learn(PropagationStructLoader ploader, String db, String usersCollection, double userLinkThreshold, double min_init, double max_init, int nbIter) {
        
        // Pour chaque cascade, liste id users avec leur temps de contamination
        //HashMap<Integer,HashMap<User,Long>> userTimeContamination=loader.getTimeSteps(true);
		HashMap<Integer,PropagationStruct> cascades=ploader.getCascades();
        
		
		
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
            HashMap<String,Double> ki=new HashMap<String,Double>();
            influences.put(u.getName(),ki);
            HashMap<String,Double> ri=new HashMap<String,Double>();
            delais.put(u.getName(),ri);	
            
            for(Link l:succs.values()){
            	User u2=(User)l.getNode2();
            	if(!User.users.values().contains(u2)){
            		throw new RuntimeException("Blem Users");
            	}
                double v=(Math.random()*dif)+min_init;
                ki.put(u2.getName(), v);
                v=(Math.random()*100); //dif)+min_init;
                ri.put(u2.getName(), v);
                /*if(u.getName().equals("iddaaaliyiz")){
                	System.out.println(u+","+l.getNode2()+"=>"+v);
                }*/
                //System.out.println("Link "+l);
                allLinks.add(l);
                HashMap<String,Link> preds=u2.getPredecesseurs();
                if((!preds.containsKey(u.getName())) || (preds.get(u.getName())!=l)){
                	throw new RuntimeException("Blem Links "+u+","+u2);
                	
                	//System.out.println("Blem Links "+u+","+u2);
                	//Keyboard.saisirLigne("Blem Links "+u+","+u2);
                }
            }
            HashMap<String,Link> preds=u.getPredecesseurs();
            for(Link l:preds.values()){
            	User u2=(User)l.getNode1();
            	HashMap<String,Link> succs2=u2.getSuccesseurs();
            	if(!User.users.values().contains(u2)){
            		throw new RuntimeException("Blem Users");
            	}
            	if((!succs2.containsKey(u.getName())) || (succs2.get(u.getName())!=l)){
                	throw new RuntimeException("Blem Links");
                }
            }
        }
       
        System.out.println(allLinks.size());
        
       
        HashMap<Link,HashSet<Integer>> spos=new HashMap<Link,HashSet<Integer>>();
       
        //HashMap<Link<User>,HashSet<Integer>> sneg=new HashMap<Link<User>,HashSet<Integer>>();
        HashMap<Link,Integer> tneg=new HashMap<Link,Integer>();
        HashMap<Link,Integer> tpos=new HashMap<Link,Integer>();
        
          
        System.out.println("Compute sneg et spos");
        maxIter=1;
        // compute sneg et spos
       	for(Integer c:cascades.keySet()) {
       			PropagationStruct pstruct=cascades.get(c);
       			HashMap<String, Long> hc = pstruct.getInfectionTimes();
       			HashSet<String> pus=pstruct.getPossibleUsers();
       			System.out.println("Cascade c = "+c);
        		for(String w:hc.keySet()){
        			User uw=User.getUser(w);
        			 if(influences.get(w)==null){
                     	throw new RuntimeException("No influence table for user "+w);
                     }
        			//User user=w;
        			Long l=hc.get(w);
        			if(l>maxIter){
        				maxIter=l;
        			}
        			if(l>cascades.get(c).getNbInitSteps()){
        				continue;
        			}
        			//System.out.println("User w = "+w+" => "+l);
        			HashMap<String,Link> succs=uw.getSuccesseurs();
        			/*if(w.getName().equals("iddaaaliyiz")){
                    	System.out.println(w+","+succs.size()+" succs");
                    }*/
                    for(String v:succs.keySet()){
                    	if(!pus.contains(v)){
                    		continue;
                    	}
                        User uv=User.getUser(v);
                    	Long hl2=hc.get(v);
                        long l2=(hl2!=null)?hl2:-2;
                        if (l2>l){
                        	Link lvw=succs.get(v);
                        	HashSet<Integer> cpos=spos.get(lvw);
                            if (cpos==null){
                                cpos=new HashSet<Integer>();
                                spos.put(lvw, cpos);
                            }
                            cpos.add(c);
                            Integer npos=tpos.get(lvw);
                    		npos=(npos!=null)?npos:0;
                    		npos++;
                    		
                    		tpos.put(lvw, npos);
                        }
                        else{
                        	if (l2==-2){
                        		Link lvw=succs.get(v);
                        		Integer nneg=tneg.get(lvw);
                        		nneg=(nneg!=null)?nneg:0;
                        		nneg++;
                        		
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
            influences.get(v.getName()).remove(w.getName());
            delais.get(v.getName()).remove(w.getName());
        }
        
        HashMap<Integer,HashMap<String,HashMap<String,Double>>> A; // Table <cascade, successeur, predecesseur, val>
        HashMap<Integer,HashMap<String,HashMap<String,Double>>> B; // Table <cascade, successeur, predecesseur, val>
        HashMap<Integer,HashMap<String,HashMap<String,Double>>> a; // Table <cascade, successeur, predecesseur, val>
        HashMap<Integer,HashMap<String,HashMap<String,Double>>> b; // Table <cascade, successeur, predecesseur, val>
        //HashMap<Integer,HashMap<User,Double>> sumA;
        
        for(int iteration = 0 ; iteration<nbIter ; iteration++) {
           
            System.out.println("iteration : "+iteration);
           
            double sump=0;
            double sumnp=0;
            
            
            
            // Estimate A et B
            A=new HashMap<Integer,HashMap<String,HashMap<String,Double>>>();
            B=new HashMap<Integer,HashMap<String,HashMap<String,Double>>>();
            a=new HashMap<Integer,HashMap<String,HashMap<String,Double>>>();
            b=new HashMap<Integer,HashMap<String,HashMap<String,Double>>>();
            //sumA=new HashMap<Integer,HashMap<User,Double>>();
            System.out.println("A et B... pour "+cascades.size());
            int iii =0;
            double like=0.0;
            
            for(Integer c:cascades.keySet()) {
                    if(iii%((int)(cascades.size()/10))==0){
                    	System.out.print((iii++) + ", ");
                    }
                    PropagationStruct cas=cascades.get(c);
           			HashSet<String> pus=cas.getPossibleUsers();
           			
            	   
                    HashMap<String,HashMap<String,Double>> Ac=new HashMap<String,HashMap<String,Double>>();
                    A.put(c, Ac);
                    HashMap<String,HashMap<String,Double>> Bc=new HashMap<String,HashMap<String,Double>>();
                    B.put(c, Bc);
                    HashMap<String,HashMap<String,Double>> ac=new HashMap<String,HashMap<String,Double>>();
                    a.put(c, ac);
                    HashMap<String,HashMap<String,Double>> bc=new HashMap<String,HashMap<String,Double>>();
                    b.put(c, bc);
                    //HashMap<User,Double> sAc=new HashMap<User,Double>();
                    //sumA.put(c, sAc);
                    
                    HashMap<String, Long> hc = cas.getInfectionTimes();
                    double sumLogH=0.0;
                    double sumLogG=0.0;
                    for(String v:hc.keySet()){
                    	User uv=User.getUser(v);
                    	long l=hc.get(v);
                        HashMap<String,Double> Acv=new HashMap<String,Double>();
                    	Ac.put(v, Acv);
                    	HashMap<String,Double> Bcv=new HashMap<String,Double>();
                    	Bc.put(v, Bcv);
                    	HashMap<String,Double> acv=new HashMap<String,Double>();
                     	ac.put(v, acv);
                     	HashMap<String,Double> bcv=new HashMap<String,Double>();
                     	bc.put(v, bcv);
                        double p=1.0;
                         int npd=0;
                        double sumAcv=0.0;
                        double sumBcv=0.0;
                        HashMap<String,Link> preds=uv.getPredecesseurs();
                        int nbp=0;
                        for(String pred:preds.keySet()){
                            User u=User.getUser(pred);
                        	Long l2=hc.get(pred);
                        	
                        	if((l2==null) || (l2>cas.getNbInitSteps())){
                				//System.out.println("> nbInits => "+l2);
                				continue;
                			}
                            //long l2=(hl2!=null)?hl2:-2;
                            if((l2!=null) && (l2<l)){
                            	HashMap<String,Double> k=influences.get(pred);
                            	Double ki=k.get(v);
                            	//ki=(ki==null)?0.0:ki;
                            	//if(ki>0){
                            		double ri=delais.get(pred).get(v);
                            		if(ri==0.0){
                            			ri=Double.MIN_VALUE;
                            		}
                            		if(ki==0.0){
                            			ki=Double.MIN_VALUE;
                            		}
                            		double vA=Math.log(ki)+Math.log(ri)-ri*l+ri*l2;
                            		if (Double.isInfinite(vA)){
                            			System.out.println("va infinite "+ri+" "+ki+" "+l+" "+l2);
                            		}
                            		Acv.put(pred, vA);
                            		double vB=Math.log(ki*Math.exp(-ri*(l-l2))+(1.0-ki));
                            		
                            		if (Double.isInfinite(vB)){
                            			System.out.println("vb infinite "+ri+" "+ki+" "+l+" "+l2);
                            		}
                            		Bcv.put(pred, vB);
                            		//double x=Math.log(ki*Math.exp(-ri*(l-l2)))-vB;
                            		double x=Math.log(ki)-(ri*(l-l2))-vB;
                            		if (Double.isInfinite(x)){
                                    	System.out.println("x infinite "+ri+" "+ki+" "+l+" "+l2+" "+vA+" "+vB);
                                    }
                            		bcv.put(pred, x);
                            		x=Math.exp(vA-vB);
                            		if (Double.isInfinite(x)){
                                    	System.out.println("x2 infinite "+ri+" "+ki+" "+l+" "+l2+" "+vA+" "+vB);
                                    }
                            		sumAcv+=x;
                            		sumBcv+=vB;
                            	//}
                            	nbp++;
                            }
                        }
                        //System.out.println(sumAcv+" : "+nbp);
                        if(sumAcv==0){
                        	sumAcv=Double.MIN_VALUE;
                        }
                        sumAcv=Math.log(sumAcv);
                        if (Double.isInfinite(sumAcv)){
                        	System.out.println("sumAcv infinite ");
                        }
                        //sAc.put(v, sumAcv);
                        if((nbp>0) && (l>1)){
                        	sumLogH+=sumAcv+sumBcv;
                        	//System.out.println(sumAcv+" "+sumBcv);
                        }
                        
                        for(String u:Acv.keySet()){
                        	acv.put(u, Acv.get(u)-sumAcv);
                        	
                        }
                        HashMap<String,Double> k=influences.get(v);
                        /*if(influences.get(v)==null){
                         	throw new RuntimeException("No influence table for user "+v);
                        }*/
                        if ((l>=1) && (l<=cas.getNbInitSteps())){
	                        HashMap<String,Link> succs=uv.getSuccesseurs();
	                        for(String succ:succs.keySet()){
	                        	if(!pus.contains(succ)){
	                        		continue;
	                        	}
	                            User w=User.getUser(succ);
	                        	Long l2=hc.get(succ);
	                        	if(l2==null){
	                        		
	                        		/*if(k==null){
	                        			continue;
	                        		}*/
	                        		Double val=k.get(succ);
	                        		if(val!=null){
	                        			sumLogG+=Math.log(1.0-val);
	                        		}
	                        	}
	                        }
                        }
                    }
                    like+=sumLogH+sumLogG;
                    iii++;
                    
            }
            
            
            System.out.println("\n Likelihood = "+like);
            
            
            
            int nbChanges=0;
            double sum=0;
            int nb1=0;
            // Estimate weights ;
            System.out.println("w... pour "+spos.size()+" liens");
            for(Link l : allLinks) {
                User uu=(User)l.getNode1();
                User uv=(User)l.getNode2();
                String su=uu.getName();
                String sv=uv.getName();
                HashSet<Integer> cpos=spos.get(l);
                if (cpos==null){
                	throw new RuntimeException("Should not be here");
                }
                double nom=0.0;
                double den=0.0;
                double kuv=0.0;
                for(Integer c:cpos){
                	PropagationStruct cas=cascades.get(c);  
                    HashMap<String, Long> hc = cas.getInfectionTimes();
                	HashMap<String,Double> acv=a.get(c).get(sv);
                	HashMap<String,Double> bcv=b.get(c).get(sv);
                	if(acv==null){
                		continue;
                	}
                	Double acuv=acv.get(su);
                	if(acuv==null){
                		continue;
                	}
                	acuv=Math.exp(acuv);
                	Double bcuv=bcv.get(su);
                	nom+=acuv;
                	double x=(acuv+(1.0-acuv)*Math.exp(bcuv));
                	den+=(hc.get(sv)-hc.get(su))*x;
                	kuv+=(acuv+(1.0-acuv)*Math.exp(bcuv));
                }
                
                
                Integer nneg=tneg.get(l);
                nneg=(nneg!=null)?nneg:0;
                Integer npos=tpos.get(l);
                npos=(npos!=null)?npos:0;
                kuv/=(nneg+npos);
                if(den==0){
                	den=Double.MIN_VALUE;
                }
                double ruv=nom/den;
                //System.out.println(nom+","+den+","+kuv);
                if(kuv==0){
                	kuv=Double.MIN_VALUE;
                }
                if(ruv==0){
                	ruv=Double.MIN_VALUE;
                }
                influences.get(su).put(sv, kuv);
                delais.get(su).put(sv, ruv);
            }
            
        } 
        
        loaded=true;
        
        if (modelFile.length()==0){
    		modelFile="propagationModels/CTIC_step-"+ploader.getStep()+"_ratioInits-"+ploader.getRatioInits()+"_nbMaxInits-"+ploader.getNbMaxInits()+"_db-"+ploader.getDb()+"_cascadesCol-"+ploader.getCollection()+"_users"+usersCollection+"_linkThreshold"+userLinkThreshold;
    	}
        
    }
	
	 /**
     * @param args
     */
    public static void main(String[] args) {
    	String db=args[0];
    	
    	String cascadesCol=args[1];
    	String users=args[2];
    	PropagationStructLoader ploader=new PropagationStructLoader(db, cascadesCol,(long)1,1.0,-1,1,Integer.parseInt(args[4]));
		
        //CascadesLoader loader=new CascadesLoader(db, cascadesCol,Integer.parseInt(args[3]),false);
        CTIC myModel = new CTIC(); // "propagationModels/ICmodel2_3600_1_cascades2_users1s.txt",50) ;
        myModel.learn(ploader,db, users, 1, 0.1, 0.3,Integer.parseInt(args[3]));
        myModel.save();
    	
    	//test();
    	
    	//System.out.println(Math.exp(Math.log(0)));
    }
	
}
