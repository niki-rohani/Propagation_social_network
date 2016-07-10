package propagationModels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;

import mlp.CPUParams;
import utils.ArgsParser;
import utils.CopyFiles;
import actionsBD.MongoDB;
import cascades.Cascade;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Link;
import core.Post;
import core.Structure;
import core.User;

public class NaiveLink implements PropagationModel {
	private Random r;
	private String modelFile;
	private boolean timeOriented;
	private boolean loaded=false;
	private HashMap<String,HashMap<String,Double>> probas = null ;
	
	public NaiveLink(){
		this("");
	}
	public NaiveLink(String modelFile){
		this.modelFile=modelFile;
		r = new Random() ;
	}
	public HashMap<String,HashMap<String,Double>> getProbas(){
		return probas;
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
	public String toString(){
		String sm=modelFile.replaceAll("/", "_");
		return("NaiveLinkModel_"+sm);
	}
	public String getName(){
		String sm=modelFile.replaceAll("/", "__");
		return sm;
	}
	
	public void load(){
		String filename=modelFile;
		probas = new HashMap<String, HashMap<String,Double>>() ;
        User.reinitAllLinks();
        BufferedReader r;
        try{
        	r = new BufferedReader(new FileReader(filename)) ;
            String line ;
            String[] sline;
            boolean probas_mode=false;
            int nb=0;
            while((line=r.readLine()) != null) {
            	if(line.startsWith("TimeOriented")){
            		sline=line.split("=");
                    this.timeOriented=Boolean.valueOf(sline[1]);
                    continue;
            	}
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
	            	HashMap<String,Double> pi=probas.get(i);
	            	if (pi==null){
	            		pi=new HashMap<String,Double>();
	            		probas.put(i, pi);
	            	}
	            	pi.put(j,d);
	           	}
	    	
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
		String filename=modelFile;
		try{
			PrintStream p = new PrintStream(filename) ;
			p.println("TimeOriented="+this.timeOriented);
			//p.println("iInInit="+this.iInInit);
			p.println("<Infections_Probas>");
			for(String i : probas.keySet()){
				HashMap<String,Double> pi=probas.get(i);
				for(String j:pi.keySet()){
					p.println(i+"\t"+j+"\t"+pi.get(j)) ;
				}
			}
			p.println("</Infections_Probas>");
			
				
			p.close();
		}
    	catch(IOException e){
    		System.out.println("Probleme sauvegarde modele "+filename);
    	}
	}

	
	

	
	
	public int infer2(Structure struct) {
		 if (probas==null){
			 load();
		 }
		 //System.out.println(probas);
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
	     
	     HashMap<String,Double> infectedstep = new HashMap<String,Double>() ;
	     infections.put((long)firstNewT,infectedstep);
	        
	     //long tstart=pstruct.getNbInitSteps();
	     for(String i : infected) {
	    	HashMap<String,Double> pi=probas.get(i);
	    	//System.out.println(pi);
	    	if (pi==null){
	    		continue;
	    	}
	    	for(String j:pi.keySet()){
	    		if(!infected.contains(j) && r.nextFloat()<=pi.get(j)) {
	    			infectedstep.put(j,1.0);
	    		}
	    	}
	     }
	     pstruct.setInfections(infections);
		 return 0 ;
		
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
	
	public int infer(Structure struct) {
		 if (probas==null){
			 load();
		 }
		 //System.out.println(probas);
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
	     
	     HashMap<String,Double> infectedstep = new HashMap<String,Double>() ;
	     infections.put((long)firstNewT,infectedstep);
	        
	     //long tstart=pstruct.getNbInitSteps();
	     HashMap<String,HashMap<String,Double>> reverse=new HashMap<String,HashMap<String,Double>>();
	     for(String i : infected) {
	    	HashMap<String,Double> pi=probas.get(i);
	    	//System.out.println(pi);
	    	if (pi==null){
	    		continue;
	    	}
	    	
	    	for(String j:pi.keySet()){
	    		HashMap<String,Double> r=reverse.get(j);
	    		if(r==null){
	    			r=new HashMap<String,Double>();
	    			reverse.put(j, r);
	    		}
	    		r.put(i,pi.get(j));
	    	}
	     }
	     for(String i:reverse.keySet()){
	    	 double p=1.0;
	    	 HashMap<String,Double> pb=reverse.get(i);
	    	 for(String j:pb.keySet()){
	    		p*=1.0-pb.get(j);
	    	 }
	    	 p=1.0-p;
	    	 infectedstep.put(i,p);
	     }
	    		
	     pstruct.setInfections(infections);
		 return 0 ;
		
	}
	
	 // retourne une table cascade_id,user_id => time contamination
 	public static HashMap<Integer,HashMap<User,Long>> getTimeSteps(String db, String collection,long step){
 		 HashMap<Integer,HashMap<User,Long>> userTimeContamination=new HashMap<Integer,HashMap<User,Long>>();
         DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
         DBCursor cursor = col.find();
         Post p=null;
         try {
             while(cursor.hasNext()) {
                 DBObject res=cursor.next();
                 Cascade c=Cascade.getCascadeFrom(res);
                 int ic=c.getID();
                 //HashMap<String,Long> ihc=new HashMap<String,Long>();
     			 HashMap<User,Long> hc=c.getUserContaminationsSteps(-1,step);
                 /*
                 for(User u:hc.keySet()){
                     Long t=hc.get(u);
                     ihc.put(u.getName(), t);
                 }
                 */
                 userTimeContamination.put(c.getID(),hc);
                 
                 User.reinitAllPosts(); // Pour alleger, on supprime les textes des posts dont on ne se sert pas ici
                 Post.reinitPosts();
                 System.out.println("Cascade "+ic+" chargee");
             }
         } finally {
             cursor.close();
         }
         return(userTimeContamination);
        
     }
	
 	public void learn(PropagationStructLoader ploader, String usersDb, String usersCollection, double userLinkThreshold, boolean timeOriented){
 		this.timeOriented=timeOriented;
		//this.iInInit=iInInit;
		
		probas = new HashMap<String, HashMap<String,Double>>() ;
		HashMap<String, HashMap<String,Integer>> nbc = new HashMap<String, HashMap<String,Integer>>() ;
		 
		 HashMap<Integer,PropagationStruct> cascades=ploader.getCascades();
	        
		// Pour chaque cascade, liste des users avec leur temps de contamination
		 //HashMap<Integer,HashMap<User,Long>> contamination=getTimeSteps(db,cascadesCollection,step);
		
		 if (cascades.size()==0){
        	 return;
         }
		 
		 HashSet<User> users=new HashSet<User>(User.users.values());
		 System.out.println("Nb users = "+users.size());
		 System.out.println("Nb cascades = "+cascades.size());
	     // Pour tous les utilisateurs on recupere leurs liens
	     for(User u:users){
	            u.loadLinksFrom(usersDb, usersCollection, userLinkThreshold);
	            System.out.println("Liens user "+u.getName()+" charge");
	     }
	     
	     for(User i:users){
        	System.out.println("Links selon user "+i.getName()); 
        	HashMap<String,Link> rels=i.getSuccesseurs();
            if (rels==null){continue;}
            //int nbci=0;
            HashMap<String,Double> pi=probas.get(i.getName());
	    	if (pi==null){
	    		 pi=new HashMap<String,Double>();
	    		 probas.put(i.getName(), pi);
	    	}
	    	HashMap<String,Integer> nbci=nbc.get(i.getName());
	    	if (nbci==null){
	    		 nbci=new HashMap<String,Integer>();
	    		 nbc.put(i.getName(), nbci);
	    	}
	    	//HashSet<String> vus=new HashSet<String>();
            for(Integer c:cascades.keySet()){
            	PropagationStruct pstruct=cascades.get(c);
            	LinkedHashSet<String> pus=pstruct.getPossibleUsers();
        		
            	HashMap<String,Long> hc=pstruct.getInfectionTimes();
				Long ti=hc.get(i.getName());
				if (ti==null){
					continue;
				}
				if(ti>pstruct.getNbInitSteps()){
					continue;
				}
				
				
				for(Link l:rels.values()){
					
            		User j = (User)l.getNode2();
					if (j.getName().equals(i.getName())){
						j=(User)l.getNode1();
					}
					if(!pus.contains(j.getName())){
						continue;
					}
					//System.out.println(i+","+j);
            		Long tj=hc.get(j.getName());
            		
            		if((tj!=null) && (tj<=ti)){
            			if (timeOriented){
            				continue;
            			}
            		}
            		
            		Integer nbcj=nbci.get(j.getName());
					nbci.put(j.getName(), 1+((nbcj==null)?0:nbcj));
					
					if (tj==null){
						continue;
					}
					/*System.out.println("t=>"+ti+","+tj);
					if(vus.contains(j.getName())){
						throw new RuntimeException("ici!!");
					}
					vus.add(j.getName());*/
					Double nbj=pi.get(j.getName());
					
					pi.put(j.getName(),1+((nbj==null)?0:nbj));
					
				}
            }
         }
	     
         User.reinitAllLinks();
         for(Entry<String,HashMap<String,Double>> e:probas.entrySet()){
	    	 String i=e.getKey();
	    	 HashMap<String,Double> h=e.getValue();
	    	 HashMap<String,Integer> nbci=nbc.get(i);
	    	 //int nbci=cascades.get(i);
	    	 for(String j:h.keySet()){
	    		 Integer nbcj=nbci.get(j);
	    		 double d=h.get(j);
	    		 h.put(j, (d*1.0)/nbcj);
	    		 System.out.println(j+" : "+d);
	    	 }
	     }
         if (modelFile.length()==0){
     		modelFile="propagationModels/naiveLinkModel_step-"+ploader.getStep()+"_ratioInits-"+ploader.getRatioInits()+"_nbMaxInits-"+ploader.getNbMaxInits()+"_db-"+ploader.getDb()+"_cascadesCol-"+ploader.getCollection()+"_users-"+usersCollection+"_linkThreshold-"+userLinkThreshold+"_timeOriented-"+timeOriented+"_start"+ploader.getStart()+"_nbC"+ploader.getNbC();
     	}
         loaded=true;
 	}
	
	
	public static void main(String[] args) {
		//System.out.println("Usage : NaiveLink <db> <cascadesCol> <users_db> <users_col> <ratio_inits> <nbMax_inits>"); 
		HashMap<String,String> hargs=ArgsParser.parseArgs(args);
    	String db=(hargs.containsKey("db"))?hargs.get("db"):"digg";
		String cascadesCol=(hargs.containsKey("c"))?hargs.get("c"):"cascades_1";
		String dbusers=(hargs.containsKey("dbus"))?hargs.get("dbus"):db;
		String userscol=(hargs.containsKey("us"))?hargs.get("us"):"users_1";
		String ratioInits=(hargs.containsKey("rI"))?hargs.get("rI"):"1.0";
        String nbInitsMax=(hargs.containsKey("nI"))?hargs.get("nI"):"-1";
        String nbC=(hargs.containsKey("nbC"))?hargs.get("nbC"):"-1";
		String start=(hargs.containsKey("start"))?hargs.get("start"):"1";
		
		
		//NaiveLinkModel myModel = new NaiveLinkModel("propagationModels/naiveLinkModel_cascades1_users1.txt");
        NaiveLink myModel = new NaiveLink();
        PropagationStructLoader ploader=new MultiSetsPropagationStructLoader(db,cascadesCol,(long)1,ratioInits,nbInitsMax,start,nbC);
		
        myModel.learn(ploader, dbusers, userscol,1.0,true);
        System.out.println("save...");
        myModel.save();
    }
}
