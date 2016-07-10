package propagationModels;

import java.io.BufferedReader;
import java.util.TreeMap;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import cascades.Cascade;
import cascades.IteratorDBCascade;
import core.Post;
import core.Structure;
import core.User;

import java.util.ArrayList;
public class NaiveNode implements PropagationModel {

	private Random r;
	private String modelFile;
	private boolean loaded=false;
	private HashMap<String,Double> probas=null;	
	
	public NaiveNode(){
		this("");
	}
	public NaiveNode(String modelFile){
		this.modelFile=modelFile;
		r = new Random() ;
	}
	
	public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(this.probas.keySet());
	}
	public int getContentNbDims(){
		if(!loaded){
			load();
		}
		return 0;
	}
	
	public String toString(){
		String sm=modelFile.replaceAll("/", "_");
		return("NaiveNodeModel_"+sm);
	}
	public String getName(){
		String sm=modelFile.replaceAll("/", "__");
		return sm;
	}
	public void load(){
		String filename=modelFile; 
		probas = new HashMap<String, Double>() ;
		 try{
			BufferedReader r = new BufferedReader(new FileReader(filename)) ;
			String line ;
			while((line=r.readLine()) != null) {
				String[] tokens = line.split("\t") ;
				
				double d = Double.parseDouble(tokens[1]) ; 
				if(d==0)
					continue ;
				
				if(! probas.containsKey(tokens[0]))
					probas.put(tokens[0],d) ;
				
				
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
			for(String u : probas.keySet())
				p.println(u+"\t"+probas.get(u)) ;
			p.close();
		}
    	catch(IOException e){
    		System.out.println("Probleme sauvegarde modele "+filename);
    	}
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
	     for(String u : probas.keySet()) {
			if(!infected.contains(u)){ //&& r.nextFloat()<=probas.get(u)) {
				infectedstep.put(u,probas.get(u));
			}
	     }
	     pstruct.setInfections(infections);
		 return 0 ;
		
	}
	
	public void learn(String db, String cascadesCollection){
		
		probas = new HashMap<String, Double>() ;
		HashMap<User,Integer> nbs=new HashMap<User,Integer>() ;
		
		Cascade c ;
		int nbTotal = 0 ;
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,cascadesCollection);
        DBCursor cursor = col.find();
        
        try {
            while(cursor.hasNext()) {
            	Post.reinitPosts();
            	User.reinitAllPosts();
				//User.reinitUsers();
				//System.gc();
                DBObject res=cursor.next();
                c=Cascade.getCascadeFrom(res);
                nbTotal++ ;
                System.out.println(nbTotal);
                for(Post p : c.getPosts()) {
                	User u = p.getOwner() ;
                	int nb=0;
                	Integer nbu=nbs.get(u);
                	nb=(nbu==null)?0:(int)nbu;
                	nbs.put(u, nb+1);
            	}
            }
        } finally {
            cursor.close();
        }
        Post.reinitPosts();
		User.reinitUsers();
        probas = new HashMap<String, Double>() ;
        if (nbTotal>0){
        	for(User u : nbs.keySet())
        		probas.put(u.getName(), (1.0*nbs.get(u))/nbTotal);
        }
        
        if (modelFile.length()==0){
    		modelFile="propagationModels/naiveNodeModel_"+db+"_"+cascadesCollection;
    	}
        loaded=true;
	}
	
	public static void main(String[] args) {
	       
        NaiveNode myModel = new NaiveNode(); //"propagationModels/naiveNodeModel_cascades1.txt");
        myModel.learn(args[0], args[1]);
        myModel.save();
    }

}



