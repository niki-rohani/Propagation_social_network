package propagationModels;

import java.io.BufferedReader;
import java.util.Collections;
import java.util.TreeMap;

import cascades.ArtificialCascade;
import actionsBD.MongoDB;
import cascades.Cascade;
import core.*;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.TreeSet;

import statistics.Distributions;
import utils.Keyboard;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Link;
import core.Post;
import core.Structure;
import core.User;
//import jMEF.*;
public class ArtificialModel implements PropagationModel,ProbabilisticTransmissionModel {


	protected String modelFile="";
	protected boolean loaded=false;
	protected int dim=1;
	protected HashMap<String,HashMap<String,Double>> links=new HashMap<String,HashMap<String,Double>>();
	protected HashMap<String,HashMap<String,Long>> minDelays=new HashMap<String,HashMap<String,Long>>();
	protected HashMap<String,HashMap<String,Double>> varDelays=new HashMap<String,HashMap<String,Double>>();
	protected long maxT=100;
	//protected int nMods=1;
	//protected boolean cosineMode=true;
	
	protected HashMap<String,MultiInterestModel> userModels=new HashMap<String,MultiInterestModel>();
	//private HashMap<Link,MixtureModel> models; 
	
	public String getModelFile(){
		return modelFile;
	}
	
	public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(this.links.keySet());
	}
	public int getContentNbDims(){
		if(!loaded){
			load();
		}
		return dim;
	}
	
	public void setModelFile(String file){
		this.modelFile=file; 
	}
	public String getName(){
		String sm=modelFile.replaceAll("/", "__");
		return sm;
	}
	public String toString(){
		return this.getName();
	}
	public void load(){
		String filename=modelFile;
		links=new HashMap<String,HashMap<String,Double>>();
		minDelays=new HashMap<String,HashMap<String,Long>>();
		varDelays=new HashMap<String,HashMap<String,Double>>();
		
		userModels=new HashMap<String,MultiInterestModel>();
        //User.reinitAllLinks();
        BufferedReader r;
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          boolean modeLinks=false;
          boolean modeUsers=false;
          boolean modeMeanDelays=false;
          boolean modeVarDelays=false;
          
          while((line=r.readLine()) != null) {
        	
        	if(line.startsWith("maxT")){
        			String[] sline=line.split("=");
                    maxT=Long.valueOf(sline[1]);
                    continue;
            } 
        	
        	if(line.contains("<UserModels>")){
        		modeUsers=true;
        		continue;
        	}
        	if(line.contains("</UserModels>")){
        		modeUsers=false;
        		continue;
        	}
        	if(line.contains("<Links>")){
        		modeLinks=true;
        		continue;
        	}
        	if(line.contains("</Links>")){
        		modeLinks=false;
                continue;
        	}
        	if(line.contains("<MeanDelays>")){
        		modeMeanDelays=true;
        		continue;
        	}
        	if(line.contains("</MeanDelays>")){
        		modeMeanDelays=false;
                continue;
        	}
        	if(line.contains("<VarDelays>")){
        		modeVarDelays=true;
        		continue;
        	}
        	if(line.contains("</VarDelays>")){
        		modeVarDelays=false;
                continue;
        	}
        	
        	//System.out.println(line);
        	String[] tokens = line.split("\t") ;
        	if(tokens.length<2){
        		//System.out.println("Line wrongly formatted : "+line);
        		throw new RuntimeException("Line wrongly formatted : "+line);
        	}
        	String u1=tokens[0];
        	if(modeLinks){
        		HashMap<String,Double> h=links.get(u1);
        		if(h==null){
        			h=new HashMap<String,Double>();
        			links.put(u1, h);
        		}
        		String u2=tokens[1];
        		Double v=Double.valueOf(tokens[2]);
        		h.put(u2, v);
        		continue;
        	}
        	if(modeMeanDelays){
        		HashMap<String,Long> h=minDelays.get(u1);
        		if(h==null){
        			h=new HashMap<String,Long>();
        			minDelays.put(u1, h);
        		}
        		String u2=tokens[1];
        		Long v=Long.valueOf(tokens[2]);
        		h.put(u2, v);
        		continue;
        	}
        	if(modeVarDelays){
        		HashMap<String,Double> h=varDelays.get(u1);
        		if(h==null){
        			h=new HashMap<String,Double>();
        			varDelays.put(u1, h);
        		}
        		String u2=tokens[1];
        		Double v=Double.valueOf(tokens[2]);
        		h.put(u2, v);
        		continue;
        	}
        	if(modeUsers){
        		System.out.println(line);
        		MultiInterestModel m=new MultiInterestModel();
        		String[] sp=tokens[1].split(";");
        		for(int i=0;i<sp.length;i++){
        			boolean cosineMode=false;
        			if(sp[i].startsWith("Cosine")){
        				cosineMode=true;
        			}
        			String st=sp[i];
        			tokens=st.split("\\{");
        			tokens=tokens[1].split("}");
        			HashMap<Integer,Double> vals=new HashMap<Integer,Double>();
        			String v=tokens[0];
        			//v=v.substring(1);
        			if(v.length()>0){
	        			String[] els=v.split(",");
	        			for(int j=0;j<els.length;j++){
	        				String el=els[j];
	        				if(el.startsWith(" ")){
	        					el=el.substring(1);
	        				}
	        				String[] x=el.split("=");
	        				vals.put(Integer.parseInt(x[0]), Double.valueOf(x[1]));
	        			}
        			}
        			
        			String[] els=tokens[1].split(":");
        			InterestModel mod=null;
        			if(!cosineMode){
        				String af=els[1];
        				String sdim=els[2];
        				dim=Integer.parseInt(sdim);
        				af=af.substring(0, af.length()-4);
        				mod=new ExponentialInterestModel(vals,Double.valueOf(af),dim);
        			}
        			else{
        				String sdim=els[1];
        				dim=Integer.parseInt(sdim);
        				mod=new CosineInterestModel(vals,dim);
        			}
        			
        			//System.out.println("mod => "+mod);
        			m.models.add(mod);
        		}
        		userModels.put(u1,m);
        		//System.out.println(u1+" : "+m);
        	}
            
          }
          r.close();
          System.out.println("Artificial Model loaded");
          
        }
        catch(IOException e){
        	System.out.println("Probleme lecture modele "+filename);
        }
        loaded=true;
        
        //System.out.println(probas);
    }

	public void loadLinkWeihtsFromUsers(){
		links=new HashMap<String,HashMap<String,Double>>();
		minDelays=new HashMap<String,HashMap<String,Long>>();
		varDelays=new HashMap<String,HashMap<String,Double>>();
		userModels=new HashMap<String,MultiInterestModel>();
		for(User uS : User.users.values()) {
            HashMap<String,Link> succs=uS.getSuccesseurs();
            HashMap<String,Double> h=new HashMap<String,Double>();
            links.put(uS.getName(),h);
            for(Link lsuc: succs.values()) {
                h.put(lsuc.getNode2().getName(), lsuc.getVal());
            }
         }
		 loaded=true;
	}
	
	public void save() {
		String filename=modelFile;
		try{
			PrintStream p = new PrintStream(filename) ;
			p.println("maxT="+maxT);
			p.println("<UserModels>");
			for(String i : userModels.keySet()){
				MultiInterestModel m=userModels.get(i);
				p.println(i+"\t"+m);
			}
			p.println("</UserModels>");
			p.println("<Links>");
			for(String s:links.keySet()){
				HashMap<String,Double> h=links.get(s);
				for(String s2:h.keySet()){
					p.println(s+"\t"+s2+"\t"+h.get(s2));
				}
			}
			p.println("</Links>");
			p.println("<MeanDelays>");
			for(String s:minDelays.keySet()){
				HashMap<String,Long> h=minDelays.get(s);
				for(String s2:h.keySet()){
					p.println(s+"\t"+s2+"\t"+h.get(s2));
				}
			}
			p.println("</MeanDelays>");
			p.println("<VarDelays>");
			for(String s:varDelays.keySet()){
				HashMap<String,Double> h=varDelays.get(s);
				for(String s2:h.keySet()){
					p.println(s+"\t"+s2+"\t"+h.get(s2));
				}
			}
			p.println("</VarDelays>");
			p.close();
		}
    	catch(IOException e){
    		System.out.println("Probleme sauvegarde modele "+filename);
    	}
		System.out.println("Artificial Model saved");
	}
	
	
	public String createUsersGraph(String db,int nbUsers,double minOutDegree,double maxOutDegree){
		String collection=MongoDB.mongoDB.createCollection(db,"users","artificial users nbUsers="+nbUsers+" minOutDegree = "+minOutDegree+" maxOutDegree = "+maxOutDegree);
		ArrayList<User> users=new ArrayList<User>(); 
		for(int i=0;i<nbUsers;i++){
			users.add(new User(""+i));
		}
		for(int i=0;i<nbUsers;i++){
			User u=User.getUser(""+i);
			double out=(Math.random()*(maxOutDegree-minOutDegree))+minOutDegree;
			int nbOut=(int)(Math.round(out*(nbUsers-1)));
			Collections.shuffle(users);
			for(int j=0;j<nbOut;j++){
				User v=users.get(j);
				if(v==u) continue;
				u.addLink(new Link(u,v,1.0), true);
			}
		}
		for(int i=0;i<nbUsers;i++){
			users.get(i).indexInto(db, collection);
		}
		
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db, collection);
		col.ensureIndex(new BasicDBObject("name", 1));
		return collection;
	}
	
	
	public void setDelays(double meanMinDelay, double meanVarDelay, long maxT){
		this.maxT=maxT;
		minDelays=new HashMap<String,HashMap<String,Long>>();
		varDelays=new HashMap<String,HashMap<String,Double>>();
		
		Distributions distribs=new Distributions();
		for(String u:links.keySet()){
			HashMap<String,Double> hl=links.get(u);
			HashMap<String,Long> h=minDelays.get(u);
			if(h==null){
				h=new HashMap<String,Long>();
				minDelays.put(u, h);
			}
			for(String u2:hl.keySet()){
				Long v=0l;
				if(meanMinDelay>0){
					double e=distribs.nextExp(meanMinDelay); //,minMinDelay);
					v=(long)Math.round(e);
				}
				
				h.put(u2, v+1); //(1.0+Math.log(sv*Math.random())));
			}
		}
		for(String u:links.keySet()){
			HashMap<String,Double> hl=links.get(u);
			HashMap<String,Double> h=varDelays.get(u);
			if(h==null){
				h=new HashMap<String,Double>();
				varDelays.put(u, h);
			}
			for(String u2:hl.keySet()){
				/*double e=0.0;
				if(meanVarDelay>0){
					e=distribs.nextExp(meanVarDelay); //+minVarDelay;
				}
				else if(meanVarDelay==-1){
					e=-1.0;
				}*/
				h.put(u2,meanVarDelay); //(1.0+Math.log(sv*Math.random())));
			}
		}
		if ((modelFile.length()!=0) && (modelFile.contains("meanMinDelay"))){
			String s=modelFile.substring(0,modelFile.indexOf("_meanMinDelay"));
			s+="_meanMinDelay-"+meanMinDelay+"_meanVarDelay-"+meanVarDelay+"_maxT-"+maxT;
			modelFile=s;
		}
		
	}
	
	public void createLinksFromGraph(String db, String usersCollection,double userLinkThreshold,HashSet<User> users){
		Distributions distribs=new Distributions();
		int nb=0; 
		int nbTotalLiens = 0 ;
		for(User user:users){
			if(nb%100==0){
				System.out.println(nb+" liens d'utilisateurs charges");
			}
			user.loadLinksFrom(db, usersCollection, userLinkThreshold); 
			nbTotalLiens+=user.getPredecesseurs().size() ;
			nb++;
		}
		links=new HashMap<String,HashMap<String,Double>>();
		System.out.println(nbTotalLiens+" liens charges");
		for(User user:users){
			HashMap<String,Double> ls=links.get(user.getName());
			if(ls==null){
				ls=new HashMap<String,Double>();
				links.put(user.getName(), ls);
			}
			HashMap<String,Link> succs=user.getSuccesseurs();
			
			for(Link l:succs.values()){
				if(Math.random()<1.0){
				User u2=(User)l.getNode2();
				if(users.contains(u2)){
					
					//double val=(Math.random()); //+0.5;
					double val=distribs.nextExp(0.01);
					if(val>1.0){
						val=1.0;
					}
					//Double v=sumv.get(u2.getName());
					//v=(v==null)?0.0:v;
					//v+=val;
					//sumv.put(u2.getName(), v);
					//System.out.println(u2.getPredecesseurs().size()+" val "+val);
					//val/=u2.getPredecesseurs().size();
					ls.put(u2.getName(),val);
				}
				}
			}
		}
	}
	
	 // @param cosineMode if true, interest models used are instances of CosineInterestModel. Else instances of ExponentialInterestModel are used.
	 public void learn(String db, String usersCollection,double userLinkThreshold,int nbUsers,int dim,int nMods,boolean cosineMode, double meanMinDelay, double meanVarDelay, long maxT){ //, double minVarDelay) {
			this.maxT=maxT;
			User.loadUsersFrom(db, usersCollection, nbUsers);
			HashSet<User> users=new HashSet<User>(User.users.values());
			Distributions distribs=new Distributions();
			int nb=0;
			
			
			//HashSet<Link> allLinks=new HashSet<Link>();
			userModels=new HashMap<String,MultiInterestModel>();
			minDelays=new HashMap<String,HashMap<String,Long>>();
			varDelays=new HashMap<String,HashMap<String,Double>>();
			createLinksFromGraph(db,usersCollection,userLinkThreshold,users);
			this.dim=dim;
			//HashMap<String,Double> sumv=new HashMap<String,Double>();
			for(User user:users){
				MultiInterestModel m=MultiInterestModel.genereModel(nMods,dim,cosineMode);		
				userModels.put(user.getName(), m);
				
			}
			
			for(String u:links.keySet()){
				HashMap<String,Double> h=links.get(u);
				for(String u2:h.keySet()){
					Double v=h.get(u2);
					//Double sv=sumv.get(u2);
					h.put(u2, v); //sv); //(1.0+Math.log(sv*Math.random())));
				}
			}
				
			
			
			setDelays(meanMinDelay,meanVarDelay,maxT);
			
			
			loaded=true;
			        
			if (modelFile.length()==0){
			    		modelFile="propagationModels/ArtificialModel_dim"+dim+"_nMods"+nMods+"_db-"+db+"_users-"+usersCollection+"_nbUsers"+nbUsers+"_linkThreshold"+userLinkThreshold+(((cosineMode)?"_cosineMode":"")+"_meanMinDelay-"+meanMinDelay+"_meanVarDelay-"+meanVarDelay+"_maxT-"+maxT);
			}
		}

	
	
	 // @param cosineMode if true, interest models used are instances of CosineInterestModel. Else instances of ExponentialInterestModel are used.
	/*public void learn(String db, String usersCollection,double userLinkThreshold,int nbUsers,int dim,int nMods,boolean cosineMode, int nbMeanDelays, long meanDelayMin, long meanDelayMax, long varDelayMin, long varDelayMax, long maxT){ //, ExponentialFamily ef) {
		this.maxT=maxT;
		User.loadUsersFrom(db, usersCollection, nbUsers);
		HashSet<User> users=new HashSet<User>(User.users.values());
		
		int nb=0;
		
		int nbTotalLiens = 0 ;
		for(User user:users){
			if(nb%100==0){
				System.out.println(nb+" liens d'utilisateurs charges");
			}
			user.loadLinksFrom(db, usersCollection, userLinkThreshold); 
			nbTotalLiens+=user.getPredecesseurs().size() ;
			nb++;
		}
		System.out.println(nbTotalLiens+" liens charges");
		//HashSet<Link> allLinks=new HashSet<Link>();
		userModels=new HashMap<String,MultiInterestModel>();
		links=new HashMap<String,HashMap<String,Double>>();
		minDelays=new HashMap<String,HashMap<String,Long>>();
		varDelays=new HashMap<String,HashMap<String,Long>>();
		
		this.dim=dim;
		HashMap<String,Double> sumv=new HashMap<String,Double>();
		for(User user:users){
			MultiInterestModel m=MultiInterestModel.genereModel(nMods,dim,cosineMode);		
			userModels.put(user.getName(), m);
			HashMap<String,Link> succs=user.getSuccesseurs();
			HashMap<String,Double> ls=links.get(user.getName());
			if(ls==null){
				ls=new HashMap<String,Double>();
				links.put(user.getName(), ls);
			}
			
			for(Link l:succs.values()){
				if(Math.random()<1.0){
				User u2=(User)l.getNode2();
				if(users.contains(u2)){
					
					double val=(Math.random()); //+0.5;
					Double v=sumv.get(u2.getName());
					v=(v==null)?0.0:v;
					v+=val;
					sumv.put(u2.getName(), v);
					//System.out.println(u2.getPredecesseurs().size()+" val "+val);
					//val/=u2.getPredecesseurs().size();
					ls.put(u2.getName(),val);
				}
				}
			}
		}
		
		for(String u:links.keySet()){
			HashMap<String,Double> h=links.get(u);
			for(String u2:h.keySet()){
				Double v=h.get(u2);
				Double sv=sumv.get(u2);
				h.put(u2, v/sv); //(1.0+Math.log(sv*Math.random())));
			}
		}
			
		
		ArrayList<Long> delays=new ArrayList<Long>();
		long difm=meanDelayMax-meanDelayMin;
		long difv=varDelayMax-varDelayMin;
		for(int i=0;i<nbMeanDelays;i++){
			long d=(long)(Math.round(Math.random()*difm))+meanDelayMin;
			if(meanDelayMax<=0){
				d=-1;
			}
			delays.add(d);
		}
		
		for(String u:links.keySet()){
			HashMap<String,Double> hl=links.get(u);
			HashMap<String,Long> h=minDelays.get(u);
			if(h==null){
				h=new HashMap<String,Long>();
				minDelays.put(u, h);
			}
			for(String u2:hl.keySet()){
				Long v=delays.get((int)(Math.random()*delays.size()));
				h.put(u2, v); //(1.0+Math.log(sv*Math.random())));
			}
		}
		for(String u:links.keySet()){
			HashMap<String,Double> hl=links.get(u);
			HashMap<String,Long> h=varDelays.get(u);
			if(h==null){
				h=new HashMap<String,Long>();
				varDelays.put(u, h);
			}
			for(String u2:hl.keySet()){
				Long v=0l;
				if(varDelayMax>0){
					v=(long)(Math.round(Math.random()*difv))+varDelayMin;
					//v=(long)Math.round(Math.random()*varDelayMax);
				}
				h.put(u2, v); //(1.0+Math.log(sv*Math.random())));
			}
		}
		
		loaded=true;
		        
		if (modelFile.length()==0){
		    		modelFile="propagationModels/ArtificialModel_dim"+dim+"_nMods"+nMods+"_db-"+db+"_users-"+usersCollection+"_nbUsers"+nbUsers+"_linkThreshold"+userLinkThreshold+(((cosineMode)?"_cosineMode":"")+"_nbMeanDelays-"+nbMeanDelays+"_meanDelayMin-"+meanDelayMin+"_meanDelayMax-"+meanDelayMax+"_varDelayMin-"+varDelayMin+"_varDelayMax-"+varDelayMax+"_maxT-"+maxT);
		}
	}*/
	
	
	 public int infer(Structure struct) {
		 return inferSimulation(struct);
	 }
	 public int inferSimulation(Structure struct) {
        if (!loaded){
        	load();
        }
        Distributions distribs=new Distributions();
		
    	PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashMap<String,Long> contagious = new HashMap<String,Long>();
        HashMap<String,Long> times=new HashMap<String,Long>();
        HashSet<String> infectedBefore = new HashSet<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        
        TreeMap<Integer,Double> content=pstruct.getDiffusion();
        System.out.println("Content diffusion = "+content);
        
        
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
        	
        	
        	//currentT++;
        	for(String contagiousU : contagious.keySet()) {
        		Long time=contagious.get(contagiousU);
                HashMap<String,Double> succs=links.get(contagiousU);
                if(succs==null){
                	continue;
                }
                HashMap<String,Long> mdel=minDelays.get(contagiousU);
                HashMap<String,Double> vdel=varDelays.get(contagiousU);
                if(mdel==null){
                	mdel=new HashMap<String,Long>();
                }
                if(vdel==null){
                	vdel=new HashMap<String,Double>();
                }
                
                for(String succ : succs.keySet()){ //get(contagiousUser.getID()).keySet()) {
                    if(infectedBefore.contains(succ))
                        continue ;
                    //System.out.println(contagiousU+" => "+neighbour.getName()+" = "+lsuc.getVal());
                    
                    double v=getProba(contagiousU,succ,content);
                    if(Math.random()<v) {
                    	Long md=mdel.get(succ);
                    	Double vd=vdel.get(succ);
                    	md=(md==null)?1l:md;
                    	vd=(vd==null)?0l:vd;
                    	long ti=time;
                    	ti+=md;
                    	if(vd>0){
                    		double e=distribs.nextExp(vd);
    						ti+=(long)Math.round(e);
                    	}
                    	else if(vd==-1){
                    		double x=(long)Math.round(Math.random()*(maxT-time-md));
                    		ti+=x;
                    	}
                    	/*if(md>0){
                    		long vl=md-vd;
                    		if (vl<1){
                    			vl=1;
                    		}
                    		if(md+vd>maxT){
                    			vd=maxT-md;
                    		}
                    		long nv=vd+md-vl;
                    	
                    		long vi=(long)Math.round(Math.random()*nv);
                    	
                    		ti+=vl+vi;
                    	}
                    	else{
                    		ti+=(long)(Math.random()*(maxT-time)+1);
                    	}*/
                    	//System.out.println(neighbour.getName()+"=>"+ti);
                        if(ti>maxT){continue;}
                        Long oldT=times.get(succ);
                        if((oldT!=null) && (ti<oldT)){
                        	//if(this.inferMode!=2){
                        		HashMap<String,Double> infectedStep=infections.get(oldT);
                        		infectedStep.remove(succ);
                        		if(infectedStep.size()==0){
                        			infections.remove(oldT);
                        			cTimes.remove(oldT);
                        		}
                        	//}
                        		
                        		
                        }
                        
                        if((oldT==null) || (ti<oldT)){
                        	System.out.println(time+" : "+succ+" infecte par "+contagiousU+" a ti="+ti+" v = "+v+" var="+vd+" mean="+md);
                            
                        	//if(this.inferMode!=2){
                        		HashMap<String,Double> infectedStep=infections.get(ti);
                        		if(infectedStep==null){
                        			infectedStep=new HashMap<String,Double>();
                        			infections.put(ti, infectedStep);
                        		}
                        		infectedStep.put(succ, 1.0);
                        		
                        	//}
                        	cTimes.add(ti);
                    		times.put(succ,ti);
                    		
                        }
                    }	
                }
            }
        	if(cTimes.size()==0){
        		//System.out.println("maxT="+it);
        		break;
        	}
        	Long time=cTimes.first();
        	
        	//System.out.println("time of new contagious = "+time);
        	//System.out.println(times);
        	HashMap<String,Double> infectedStep=infections.get(time);
        	cTimes.remove(time);
        	contagious = new HashMap<String,Long>();
        	for(String user:infectedStep.keySet()){
        		contagious.put(user, time);
        		infectedBefore.add(user);
        	}
        	System.out.println("Cascade time "+time+" : "+infectedBefore.size()+" utilisateurs contamines before");
    		
        	
            if(contagious.isEmpty())
                break ; 
            
            if(time>=maxT){
            	break;
            }
           
            it=time;
        	
        }
        //System.out.println(times);
        System.out.println("Cascade ok :"+infectedBefore.size()+" utilisateurs contamines");
        pstruct.setInfections(infections) ;
        return 0;
    }
	
	 public Double getProba(String from,String to){
		 return getProba(from,to,new TreeMap<Integer,Double>());
	 }
	
	public Double getProba(String u1,String u2,TreeMap<Integer,Double> content){
		if (!loaded){
        	load();
        }
		HashMap<String,Double> succs=links.get(u1);
		if(succs==null){
			return null;
		}
		
		Double v=succs.get(u2);
		v=(v==null)?0.0:v;
        //double probaI=userModels.get(u2).getProba(content);
        double probaI=0.5;
        if(userModels.size()==0){
        	probaI=1.0;
        }
        if(content.size()>0){
        	MultiInterestModel mod=userModels.get(u2);
            
        	if(mod!=null){
        		probaI=mod.getProba(content);
        	}
        }
        else{
        	probaI=1.0;
        }
        double proba=probaI*v;
		return proba;
		
		
        
	}
	
	
	
	public static void main(String[] args){
		ArtificialModel mod=new ArtificialModel();
		if(args[0].compareTo("learn")==0){
			mod.learn(args[1], args[2], Double.valueOf(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]),Boolean.valueOf(args[7]),Double.valueOf(args[8]),Double.valueOf(args[9]),Long.valueOf(args[10]));
			System.out.println(mod.modelFile);
			mod.save();
		}
		else if(args[0].compareTo("genere")==0){
			//mod.setModelFile("propagationModels/ArtificialModel_dim10_nMods3_usersusers_1_nbUsers100_linkThreshold2.0_cosineMode") ;
			mod.setModelFile(args[2]) ;
		
			mod.load();
			//mod.genereArtificialCascades("us_elections5000", "OctaveOLSTest", 100000, 20,true);
			ArtificialCascade.genereArtificialCascades(mod,args[1], "", Integer.parseInt(args[3]), Integer.parseInt(args[4]), Boolean.valueOf(args[5]));
		}
		else if(args[0].compareTo("graph")==0){
			mod.createUsersGraph(args[1], Integer.parseInt(args[2]), Double.valueOf(args[3]), Double.valueOf(args[4]));
		}
		else if(args[0].compareTo("delays")==0){
			mod.setModelFile(args[1]) ;
			mod.load();
			mod.setDelays(Double.valueOf(args[2]), Double.valueOf(args[3]), Long.valueOf(args[4]));
			mod.save();
		}
		//testModel();
		
		/*mod.load();
		mod.modelFile+="bis";
		mod.save();*/
	}
	public static void testModel(){
		int nbMods=3;
		int dim=10;
		MultiInterestModel mod=MultiInterestModel.genereModel(nbMods,dim, true);
		//ExponentialModel mod=new ExponentialModel(dim);
		System.out.println(mod);
		TreeMap<Integer,Double> vals=new TreeMap<Integer,Double>();
    	for(int i=1;i<=dim;i++){
    		double x=Math.random();
    		if(x<0.5){
    			continue;
    		}
    		vals.put(i, 1.0); //Math.random());
    	}
    	System.out.println("point = "+vals);
		System.out.println(mod.getProba(vals));
		
		for(int i=1;i<=dim;i++){
			double x=Math.random();
    		if(x<0.5){
    			continue;
    		}
    		vals.put(i, 1.0); //Math.random());
    	}
    	System.out.println("point = "+vals);
		System.out.println(mod.getProba(vals));
		
		for(int i=1;i<=dim;i++){
			double x=Math.random();
    		if(x<0.5){
    			continue;
    		}
    		vals.put(i, 1.0); //Math.random());
    	}
    	System.out.println("point = "+vals);
		System.out.println(mod.getProba(vals));
	}
	
	
	
}

class MultiInterestModel{
	ArrayList<InterestModel> models;
	public MultiInterestModel(){
		models=new ArrayList<InterestModel>(); 
	}
	public static MultiInterestModel genereModel(int nb,int dim){
		return(genereModel(nb,dim,false));
	}
	public static MultiInterestModel genereModel(int nb,int dim, boolean cosineMode){
		MultiInterestModel m=new MultiInterestModel();
		for(int i=0;i<nb;i++){
			if(cosineMode){
				m.models.add(new CosineInterestModel(dim));
			}
			else{
				m.models.add(new ExponentialInterestModel(dim));
			}
		}
		return m;
	}
	public double getProba(TreeMap<Integer,Double> point){
		double p=1.0;
		int nb=0;
		for(InterestModel mod:models){
			double v=mod.getVal(point);
			
			p*=(1.0-v);
			//p+=mod.getVal(point);
			
			nb++;
		}
		//p/=nb;
		return(1.0-p);
	}
	public String toString(){
		String s="";
		for(InterestModel model:models){
			s+=model.toString()+";";
		}
		return s;
	}
	
}

abstract class InterestModel{
    protected HashMap<Integer,Double> vals;
    protected int dim;
    public abstract double  getVal(TreeMap<Integer,Double> point); 
    
}
class CosineInterestModel extends InterestModel{
	double norm;
	public CosineInterestModel(HashMap<Integer,Double> vals, int dim){
    	this.vals=vals; //new HashMap<Integer,Double>();
    	this.dim=dim;
    	norm=0;
    	for(Integer i:vals.keySet()){
    		norm+=vals.get(i)*vals.get(i);
    	}
    	norm=Math.sqrt(norm);
    }
	public CosineInterestModel(int dim){
    	this.dim=dim;
    	
    	this.vals=new HashMap<Integer,Double>();
    	norm=0.0;
    	for(int i=0;i<dim;i++){
    		double x=Math.random();
    		if(x<0.5){
    			continue;
    		}
    	
    		double v=1.0; //Math.random();
    		vals.put(i, v);
    		norm+=v*v;
    	}
    	norm=Math.sqrt(norm);
    }
	 public double getVal(TreeMap<Integer,Double> point){
	    	HashSet<Integer> vus=new HashSet<Integer>();
	    	double sum=0.0;
	    	double normp=0.0;
	    	for(Integer i:point.keySet()){
	    		Double v=vals.get(i);
	    		v=(v==null)?0.0:v;
	    		double p=point.get(i);
	    		sum+=p*v;
	    		normp+=p*p;
	    		
	    	}
	    	
	    	normp=Math.sqrt(normp);
	    	//System.out.println(sum+", "+norm+", "+normp);
	    	//System.out.println(point);
	    	sum=sum/(norm*normp);
	    	if(Double.isNaN(sum)){
	    		sum=0.0;
	    	}
	    	return(sum);
	    	/*System.out.println(vals);
	    	System.out.println(sum);
	    	return(sum);*/
	    }
	 	public String toString(){
	    	return("CosineInterestModel_"+vals+"_dim:"+dim);
	    }
}
class ExponentialInterestModel extends InterestModel{
    
    double affaiblissement;
    public ExponentialInterestModel(HashMap<Integer,Double> vals, double af, int dim){
    	this.vals=vals; //new HashMap<Integer,Double>();
    	affaiblissement=af;
    	this.dim=dim;
    }
    
    public ExponentialInterestModel(int dim){
    	this.dim=dim;
    	affaiblissement=5.0+Math.random()*5.0;
    	//affaiblissement=10.0; //Math.random()*5.0;
    	
    	this.vals=new HashMap<Integer,Double>();
    	
    	for(int i=0;i<dim;i++){
    		double v=Math.random();
    		vals.put(i, v);
    		
    	}
    	
    }
    
    public double getVal(TreeMap<Integer,Double> point){
    	HashSet<Integer> vus=new HashSet<Integer>();
    	double sum=0.0;
    	//double normp=0.0;
    	for(Integer i:point.keySet()){
    		Double v=vals.get(i);
    		v=(v==null)?0.0:v;
    		double p=point.get(i);
    		/*sum+=p*v;
    		normp+=p*p;*/
    		double d=(v-p);
    		sum+=d*d;
    		vus.add(i);
    	}
    	
    	for(Integer i:vals.keySet()){
	    	if(vus.contains(i)){
	    		continue;
	    	}
	    	double d=vals.get(i);
	    	sum+=d*d;
    	}
    	sum=Math.sqrt(sum)/Math.sqrt(dim);
    	/*normp=Math.sqrt(normp);
    	sum=sum/(norm*normp);*/
    	//sum=1.0-sum;
    	sum*=affaiblissement;
    	return(Math.exp(-sum));
    	/*System.out.println(vals);
    	System.out.println(sum);
    	return(sum);*/
    }
    public String toString(){
    	return("ExponentialInterestModel_"+vals+"_af:"+affaiblissement+"_dim:"+dim);
    }
}
