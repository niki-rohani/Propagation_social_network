package propagationModels;

import java.util.LinkedHashSet;

import utils.ValInHashMapComparator;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import actionsBD.MongoDB;

import cascades.Cascade;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;
import core.Text;
import core.User;
import propagationModels.PropagationStruct;
import java.util.ArrayList;
public class PropagationStructLoader {
  
	 private String db;
     private String collection;
     protected HashMap<Integer,PropagationStruct> cascades;
 	 protected HashMap<String,HashMap<Integer,Double>> users_profiles;
 	 protected HashMap<String,HashMap<Integer,Long>> users_cascades;
 	 protected HashMap<String,ArrayList<Integer>> userAsFirst;
 	 //protected boolean emptyIgnored;
     protected long step;
     protected double ratioInits;
     protected int nbMaxInits;
     protected boolean loaded=false;
     protected int start;
     protected int nbMax;
     
     public boolean computeFirsts=false;
     
     /**
      * 
      * @param db  Name of database to use
      * @param collection   Cascade collection
      * @param step   Length of a step
      */
     /*public PropagationStructLoader(String db,String collection,long step){
    	 this(db,collection,step,-1,-1);
     }*/
     
     public PropagationStructLoader(String db,String collection,long step,double ratioInits,int nbMaxInits){
    	 this(db,collection,step,ratioInits,nbMaxInits,0,-1);
     }
     
     public PropagationStructLoader(String db,String collection,long step,double ratioInits,int nbMaxInits,int start, int nbMax){
    	 this.db=db;
    	 this.collection=collection;
    	 this.step=step;
    	 this.ratioInits=ratioInits;
    	 this.nbMaxInits=nbMaxInits;
    	 this.start=start;
    	 this.nbMax=nbMax;
     }
     public int load(){
    	 return load(0);
     }
     
     public int load(int minID){
    	 DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
         DBCursor cursor = col.find();
         users_profiles=new HashMap<String,HashMap<Integer,Double>>();
         users_cascades=new HashMap<String,HashMap<Integer,Long>>();
         userAsFirst=new HashMap<String,ArrayList<Integer>>();
         HashMap<Integer,PropagationStruct> props=new HashMap<Integer,PropagationStruct>();
         Post p=null;
         int nb=0;
         int nbs=0;
         /*User.reinitAllPosts();
         User.reinitAllLinks();
         User.reinitUsers();*/
         int maxID=minID;
         try {
             while(cursor.hasNext()) {
                if((nbMax>0) && (nbs>nbMax)){
             	   break;
                }
            	User.reinitAllPosts(); // Pour alleger, on supprime les textes des posts dont on ne se sert plus
                Post.reinitPosts();
            	DBObject res=cursor.next();
            	Cascade c=Cascade.getCascadeFrom(res);
            	
            	c.addID(minID);
            	int ic=c.getID();
            	if(ic>maxID){
            		maxID=ic;
            	}
                PropagationStruct ps=new PropagationStruct(c,step,ratioInits,nbMaxInits);
                TreeMap<Long,HashMap<String,Double>> inits=ps.getInitContaminated();
                TreeMap<Long,HashMap<String,Double>> steps=ps.getInfections();
                TreeMap<Integer,Double> diff=ps.getDiffusion();
                //System.out.println(c.getContentWeigths(1, 1));
                System.out.println("nb posts = "+Post.posts.size()+" nb initSteps = "+inits.size()+" nb steps = "+steps.size()+" nbWeights "+diff.size());
                
                /*if(steps.size()<=inits.size()){
             	   continue;
                }*/
                nb++;
                if((start>0) && (nb<start)){
                	continue;
                }
                nbs++;
                ps.getDiffusion();
                //System.out.println("diffusion ok");
                HashMap<String,Long> times=ps.getInfectionTimes();
                
                System.out.println(times.size()+" utilisateurs");
                props.put(ic,ps);
                HashSet<String> infected=new HashSet<String>((PropagationStruct.getPBeforeT(steps)).keySet()) ;
                //System.out.println(infected.size()+" utilisateurs dans infected");
                for(String us:infected){
             	   HashMap<Integer,Long> usc=users_cascades.get(us);
             	   if(usc==null){
             		   usc=new HashMap<Integer,Long>();
             		   users_cascades.put(us,usc);
             	   }
             	   usc.put(ic, times.get(us));
             	   HashMap<Integer,Double> w=users_profiles.get(us);
             	   if(w==null){
             		   w=new HashMap<Integer,Double>(); 
             		  users_profiles.put(us, w);
             	   }
             	   User user=User.getUser(us);
             	   HashMap<Integer,Double> wc=user.getWeights();
             	   for(Integer s:wc.keySet()){
             		   double v=wc.get(s);
             		   Double o=w.get(s);
             		   o=(o==null)?0.0:o;
             		   w.put(s, o+v);
             	   }
                }
                
                if(computeFirsts){
                
	                HashMap<String,Double> firsts=steps.firstEntry().getValue();
	                for(String u:firsts.keySet()){
	                	ArrayList<Integer> a=userAsFirst.get(u);
	                	if(a==null){
	                		a=new ArrayList<Integer>();
	                		userAsFirst.put(u,a);
	                	}
	                	a.add(ic);
	                }
	            }
                
                 
                System.out.println("Cascade "+ic+" chargee");
             }
         } finally {
             cursor.close();
         }
         
         User.reinitAllPosts(); // Pour alleger, on supprime les textes des posts dont on ne se sert plus
         Post.reinitPosts();
         /*for(String user:users_profiles.keySet()){
         	HashMap<Integer,Double> w=users_profiles.get(user);
         	Text.normalize(w);
         }*/
         LinkedHashSet<String> possibles=new LinkedHashSet<String>(users_profiles.keySet());
         for(PropagationStruct prop:props.values()){
        	 prop.setPossibleUsers(possibles);
         }
         
         
         
         cascades=props;
    	 loaded=true;
    	 return maxID;
     }
     
     
     
     
     
    
     
     public String getCollection(){
    	 return collection;
     }
     
     public String getDb(){
    	 return db;
     }
     
     public String getStep(){
    	 return step+"";
     }
     
     
     
    public String getRatioInits() {
		return ratioInits+"";
	}


	/*public void setRatioInits(double ratioInits) {
		this.ratioInits = ratioInits;
	}*/


	public String getNbMaxInits() {
		return nbMaxInits+"";
	}

	public String getStart(){
		return start+"";
	}
	
	public String getNbC(){
		return nbMax+"";
	}

	/*public void setNbMaxInits(int nbMaxInits) {
		this.nbMaxInits = nbMaxInits;
	}*/

	
	public HashMap<String, ArrayList<Integer>> getUserAsFirst() {
		if(!loaded){
			load();
		}
		return userAsFirst;
	}
	
	public HashMap<Integer, PropagationStruct> getCascades() {
		if(!loaded){
			load();
		}
		return cascades;
	}

	public HashMap<String, HashMap<Integer, Double>> getUsers_profiles() {
		if(!loaded){
			load();
		}
		return users_profiles;
	}

	public HashMap<String, HashMap<Integer, Long>> getUsers_cascades() {
		if(!loaded){
			load();
		}
		return users_cascades;
	}

	public static void main(String[] args){
		PropagationStructLoader loader=new PropagationStructLoader(args[0],args[1],1,1.0,-1,1, -1);
		loader.computeFirsts=true;
		loader.load(); 
		if(loader.computeFirsts){
       	 HashMap<String,Integer> nbFirsts=new HashMap<String,Integer>(); 
       	 for(String u:loader.userAsFirst.keySet()){
       		 nbFirsts.put(u, loader.userAsFirst.get(u).size());
       		 
       	 }
       	 ArrayList<String> lu=new ArrayList<String>(nbFirsts.keySet());
       	 Collections.sort(lu,new ValInHashMapComparator<String,Integer>(nbFirsts,true));
       	 for(String u:lu){
       		 System.out.println(u+" : "+loader.userAsFirst.get(u).size()+" cascades in First Step");
       	 }
        }
	}
	
	
}
