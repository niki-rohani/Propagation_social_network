package cascades;
import utils.ValInHashMapComparator;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;
import core.User;
import propagationModels.PropagationStruct;
import java.util.ArrayList;
public class CascadesLoader {
     private String db;
     private String collection;
     protected boolean emptyIgnored;
     protected long step;
     protected double ratioInits;
     protected int nbMaxInits;
     
     /**
      * 
      * @param db  Name of database to use
      * @param collection   Cascade collection
      * @param step   Length of a step
      * @param emptyIgnored  Indicates whether empty steps should be ignored
      */
     public CascadesLoader(String db,String collection,long step,boolean emptyIgnored){
    	 this(db,collection,step,emptyIgnored,-1,-1);
     }
     
     
     public CascadesLoader(String db,String collection,long step,boolean emptyIgnored,double ratioInits,int nbMaxInits){
    	 this.db=db;
    	 this.collection=collection;
    	 this.emptyIgnored=emptyIgnored;
    	 this.step=step;
    	 this.ratioInits=ratioInits;
    	 this.nbMaxInits=nbMaxInits;
     }
     
     
     public boolean getEmptyIgnored(){
    	 return emptyIgnored;
     }
     
     public String getCollection(){
    	 return collection;
     }
     
     public String getDb(){
    	 return db;
     }
     
     public long getStep(){
    	 return step;
     }
     
     
     
     public double getRatioInits() {
		return ratioInits;
	}


	public void setRatioInits(double ratioInits) {
		this.ratioInits = ratioInits;
	}


	public int getNbMaxInits() {
		return nbMaxInits;
	}


	public void setNbMaxInits(int nbMaxInits) {
		this.nbMaxInits = nbMaxInits;
	}

	
	
	//public PropagationStruct getPropagationStruct
	
	

	/**
      * Loads time of contamination of users for cascades of the cascadeCollection. 
      * @param reinitPosts indicates whether posts should be removed from structures (for memory purposes).
      * @return a table <cascade_id, user> => contamination time 
      */
     // retourne une table cascade_id,user_id => time contamination
     // zapVides indiques si on compte compte les timesteps sans posts dans une cascade donnee pour numeroter les temps de contamination (true=> on ne les compte pas) 
   	 public HashMap<Integer,HashMap<User,Long>> getTimeSteps(boolean reinitPosts){
           HashMap<Integer,HashMap<User,Long>> userTimeContamination=new HashMap<Integer,HashMap<User,Long>>();
           DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
           DBCursor cursor = col.find();
           Post p=null;
           int nb=0;
           try {
               while(cursor.hasNext()) {
              	 if(reinitPosts){
              	   User.reinitAllPosts(); // Pour alleger, on supprime les textes des posts dont on ne se sert pas ici
                   Post.reinitPosts();
              	 }
                   /*nb++;
                   if(nb>10){
                  	 break;
                   }*/
                   
              	 DBObject res=cursor.next();
              	 //int idc=Integer.parseInt(res.get("id").toString());
                   //if(idc>10){continue;}
                   
                   Cascade c=Cascade.getCascadeFrom(res);
                   int ic=c.getID();
                  System.out.println("Traitement cascades "+ic);
                  System.out.println("nb posts = "+Post.posts.size());
                   HashMap<User,Long> ihc=new HashMap<User,Long>();
                   userTimeContamination.put(c.getID(),ihc);
                   HashMap<User,Long> hc=c.getUserContaminationsSteps(-1,step);
                   System.out.println(hc);
                   if (!emptyIgnored){
                  	 for(User u:hc.keySet()){
                           Long t=hc.get(u);
                           ihc.put(u, t);
                       }
                   }
                   else{
                  	 /*long maxt=0;
                  	 HashMap<Long,HashSet<User>> ht=new HashMap<Long,HashSet<User>>();
                  	 for(User u:hc.keySet()){
                  		 Long t=hc.get(u);
                  		 if (t>maxt){
                  			 maxt=t;
                  		 }
                  		 User iu=u;
                  		 HashSet<User> tu=ht.get(t);
                  		 if (tu==null){
                  			 tu=new HashSet<User>();
                  			 ht.put(t, tu);
                  		 }
                  		 tu.add(iu);
                  	 }
                  	 //System.out.println("cascade "+ic+" max = "+maxt);
                  	 long ste=1;
                  	 for(long t=0;t<=maxt;t++){
                  		 HashSet<User> tu=ht.get(t);
                  		 if (tu!=null){
                  			 //System.out.println("time "+t+" nb "+tu.size());
                  			 for(User iu:tu){
                  				 ihc.put(iu, ste);
                  			 }
                  			 ste++;
                  		 }
                  	 }*/
                	 ValInHashMapComparator<User,Long> comp=new ValInHashMapComparator<User,Long>(hc);
                	 ArrayList<User> cont=new ArrayList<User>(hc.keySet());
                	 Collections.sort(cont,comp);
                	 long ste=0;
                	 long lastL=-1;
                	 
                	 for(User u:cont){
                		 Long l=hc.get(u);
                		 if(lastL<l){
                			 ste++;
                			 lastL=l;
                		 }
                		 ihc.put(u, ste);
                	 }
                   }
                  
                   
                   System.out.println("Cascade "+ic+" chargee");
               }
           } finally {
               cursor.close();
           }
           if(reinitPosts){
            	 User.reinitAllPosts(); // Pour alleger, on supprime les textes des posts dont on ne se sert pas ici
            	 Post.reinitPosts();
           }
           return(userTimeContamination);
          
       }
   	 
   	  /* public HashSet<PropagationStruct> getPropagationStructs(){
   		HashSet<PropagationStruct> structs=new HashSet<PropagationStruct>();
   		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
        DBCursor cursor = col.find();
        Post p=null;
        int nb=0;
        try {
            while(cursor.hasNext()) {
            	 DBObject res=cursor.next();
             	
                  Cascade c=Cascade.getCascadeFrom(res);
                  int ic=c.getID();
            }
        } finally {
            cursor.close();
        }
        
   		return(structs);
   	   }
   	   public PropagationStruct getPropagationStruct(){
   		   
   	   }*/
}
