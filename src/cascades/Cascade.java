package cascades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import actionsBD.MongoDB;
import core.Post;
import core.User;

import java.util.HashSet;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Structure;

public class Cascade implements Structure {
	protected String name;
	protected HashSet<Post> posts;
	protected int id;
	protected String fromDb; // db from which this cascade has been produced
	protected String fromCol; // collection from which this cascade has been produced 
	protected static int nbCascades=0;
	protected ArrayList<Double> features=null;
	
	//private long start_time;
	//private long end_time;
	//private long timestep; // 
	//private int nb_steps_in_init;
	
	
	public Cascade(String name,String db,String from){
		this(name,db,from,new HashSet<Post>());
	}
	public Cascade(int id,String name,String db,String from){
		this(id,name,db,from,new HashSet<Post>());
	}
	
	public Cascade(String name,String db,String from,HashSet<Post> posts){ // ,long start_time,long end_time){
		this(-1,name,db,from,posts);
	}
	public Cascade(int id,String name,HashSet<Post> posts){
		this(id,name,"","",posts);
	}
	public Cascade(int id,String name,String db,String from,HashSet<Post> posts){ // ,long start_time,long end_time){
		/*name=name.replaceAll("\r\n", "");
		name=name.replaceAll("\n", "");
		name=name.replaceAll("\r", "");*/
		this.name=name;
		
		this.posts=posts;
		this.fromDb=db;
		this.fromCol=from;
		if (id>=0){
			this.id=id;
			if (nbCascades<=this.id){
				nbCascades=id+1;
			}
		}
		else{
			nbCascades++;
			this.id=nbCascades;
		}
		//this.start_time=start_time;
		//this.end_time=end_time;
		//this.timestep=timestep;
		//this.nb_steps_in_init=nb_steps_in_init;
	}
	
	public void addPost(Post post){
		posts.add(post);
	}
	public void setPosts(HashSet<Post> posts){
		this.posts=posts;
	}
	public HashSet<Post> getPosts(){
		return(posts);
	}
	public int getID(){
		return id;
	}
	public void setID(int id){
		this.id=id;
	}
	public void addID(int id){
		this.id+=id;
	}
	public String toString(){
		String s="Cascade "+name; // +" \n";
		/*s+="\t Posts = \n ";
		for(Post p:posts){
			s+="\t"+p+"\n";
		}*/
		return(s);
	}
	
	public void setFeatures(ArrayList<Double> features){
		this.features=features;
	}
	
	public void indexInto(String db,String collection){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		BasicDBObject obj = new BasicDBObject();
		obj.put("id", id);
		obj.put("fromDb", fromDb);
		obj.put("fromCol", fromCol);
		obj.put("name", name);
		ArrayList<Integer> postsListe=new ArrayList<Integer>();
		for(Post p:posts){
			postsListe.add(p.getID());
		}
		obj.put("posts",postsListe);
		if(features!=null){
			obj.put("features", features);
		}
		
		col.insert(obj);
		System.out.println("Cascade inseree : "+obj);
	}
	public static Cascade getCascadeFrom(DBObject res){
		int id=Integer.parseInt(res.get("id").toString());
		String name=res.get("name").toString();
		String fromCol=res.get("fromCol").toString();
		if (fromCol.equals("artificial")){
			return ArtificialCascade.getCascadeFrom(res);
		}
		String fromDb=res.get("fromDb").toString();
		DBCollection postsCol=MongoDB.mongoDB.getCollectionFromDB(fromDb,fromCol);
		Cascade c=new Cascade(id,name,fromDb,fromCol);
		ArrayList<Integer> list=(ArrayList<Integer>)res.get("posts");
		for(Integer obj:list){
			BasicDBObject o=new BasicDBObject();
			o.put("id", obj);
			Post p=Post.getPostFromDB(postsCol, o);
			if (p!=null){
				c.addPost(p);
			}
		}
		if (res.containsField("features")){
			c.setFeatures((ArrayList<Double>)res.get("features"));
			
		}
			
		return(c);
	}
	
	
	
	public static Cascade getCascadeFromDB(String db,String collection,BasicDBObject query){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		Cascade c=null;
		DBCursor cursor = col.find(query);
		try {
			if(cursor.hasNext()) {
				DBObject res=cursor.next();
				c=getCascadeFrom(res);
			}
				
		} finally {
			cursor.close();
		}
		return(c);
	}
	
	public static Cascade getCascadeFromDB(String db,String collection,int id){
		BasicDBObject obj=(new BasicDBObject());
		obj.put("id",id);
		return getCascadeFromDB(db,collection,obj);
	}
	
	public static HashSet<Cascade> getCascadesFromDB(String db,String collection){
		return(getCascadesFromDB(db,collection,new BasicDBObject()));
	}
	
	
	
	public static HashSet<Cascade> getCascadesFromDB(String db,String collection,BasicDBObject query){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		HashSet<Cascade> cascades=new HashSet<Cascade>();
		DBCursor cursor = col.find(query);
		int nbc=0;
		try {
			//HashMap<Integer,ArrayList<DBObject>> relations=new HashMap<Integer,ArrayList<DBObject>>();
			while(cursor.hasNext()) {
				DBObject res=cursor.next();
				Cascade c=getCascadeFrom(res);
				if (c!=null){
					cascades.add(c);
					nbc++;
//					System.out.println(nbc+" Cascades chargees");
				}
				
			}
				
		} finally {
			cursor.close();
		}
		return(cascades);
	}
	
	public void removeEarlyPosts(long minStep,long stepLength){
		 HashSet<Post> nposts=new HashSet<Post>();
		 for(Post p:posts){
			 long t=p.getTimeStamp();
			 
			 long tt=Step.getIdStep(t, stepLength);
			 if (tt>=minStep){
				 nposts.add(p);
			 }
			 //System.out.println(t+" "+tt+" "+minStep);
		 }
		 posts=nposts;
	}
	
	/**
	 * Retourner juste une HashMap User->Timestep pour la cascade
	 */
	public HashMap<User,Long> getUserContaminationSteps() {
		return getUserContaminationsSteps(-1,1) ;
	}
	

	public HashMap<User,Long> getUserContaminationsSteps(long origin, long step) {
		HashMap<User,Long> retour = new HashMap<User,Long>() ;
		
		if(origin<0){
			origin = Long.MAX_VALUE ;
			for(Post p : this.posts) {
				if (p.getTimeStamp()<origin)
					origin = p.getTimeStamp() ;
			
			}
		}
		//System.out.println("origin = "+origin);
		//HashSet<Long> vus=new HashSet<Long>();
		for(Post p : this.posts) {
			Long t = p.getTimeStamp() ;
			//System.out.println(t+" => "+p.getOwner());
			t = ( t-origin ) / step ;
			Long tu=retour.get(p.getOwner());
			if ((tu==null) || (t<tu)){
				retour.put(p.getOwner(),t+1) ;
			}
			//if(vus.contains(t+1)){
			//	throw new RuntimeException("deux fois mme t");
			//}
			//vus.add(t+1);
			
		}
		//System.out.println(retour);
		return retour;
	}
	
	// meme fonction que getUserContaminationsSteps mais indexee par les noms des users
	public HashMap<String,Long> getContaminationsSteps(long origin, long step) {
			HashMap<String,Long> retour = new HashMap<String,Long>() ;
			
			if(origin<0){
				origin = Long.MAX_VALUE ;
				for(Post p : this.posts) {
					if (p.getTimeStamp()<origin)
						origin = p.getTimeStamp() ;
				
				}
			}
			for(Post p : this.posts) {
				Long t = p.getTimeStamp() ;
				t = ( t-origin ) / step ;
				Long tu=retour.get(p.getOwner());
				if ((tu==null) || (t<tu)){
					retour.put(p.getOwner().getName(),t+1) ;
				}
			}
			return retour;
	}
	
	public TreeMap<Long,HashSet<Post>> getStepsPosts(long step) {
		TreeMap<Long,HashSet<Post>> retour = new TreeMap<Long,HashSet<Post>>() ;
		
		long origin = Long.MAX_VALUE ;
		for(Post p : this.posts) {
			if (p.getTimeStamp()<origin)
				origin = p.getTimeStamp() ;
			
		}
		for(Post p : this.posts) {
			Long t = p.getTimeStamp() ;
			t = ( t-origin ) / step ;
			HashSet<Post> psts=retour.get(t+1);
			if (psts==null){
				psts=new HashSet<Post>();
				retour.put(t+1,psts) ;
			}
			psts.add(p);
		}
		return retour;
	}
	
	
	/**
	 * Computes weights of the diffused content in the cascade (computed from the first step of the cascade). 
	 * 
	 * @return A (normalized) vector of weights resulting from the aggregation of weights from posts of the first step of the  cascade. 
	 */
	public HashMap<Integer,Double> getContentWeigths(){
		return(getContentWeigths(1,1));
	}
	
	/**
	 * Computes weights of the diffused content in the cascade (computed from the first step of the cascade). 
	 * 
	 * @param step length of a step
	 * @return A (normalized) vector of weights resulting from the aggregation of weights from posts of the first step of the  cascade. 
	 */
	public HashMap<Integer,Double> getContentWeigths(long step){
		return(getContentWeigths(step,1));
	}
	
	/**
	 * Computes weights of the diffused content in the cascade (computed from the nbSteps first steps of the cascade). 
	 * 
	 * @param step length of a step
	 * @param nbSteps  nb steps to consider to compute the weights of the content
	 * @return A (normalized) vector of weights resulting from the aggregation of weights from posts belonging to the cascade and whose step is in [step,step+nbSteps[ 
	 */
	public HashMap<Integer,Double> getContentWeigths(long step, long nbSteps){
		//if (weights==null){
			TreeMap<Long,HashSet<Post>> stPosts=getStepsPosts(step);
			//System.out.println(stPosts.size()+" post steps");
			//System.out.println(nbSteps+" steps");
			HashMap<Integer,Double> weights=new HashMap<Integer,Double>();
			int nb=0;
			for(Long i:stPosts.keySet()){
				if(i>=nbSteps){
					break;
				}
				// System.out.println("i "+i);
				HashSet<Post> pos=stPosts.get((long)i);
				if(pos!=null){
					//System.out.println(pos.size()+" posts");
					for(Post p:pos){
						nb++;
						HashMap<Integer,Double> w=p.getWeights();
						for(Integer s:w.keySet()){
							Double v=weights.get(s);
							v=(v==null)?0:v;
							weights.put(s, v+w.get(s));
						}
					}
				}
				
			}
			if(nb>0){
				for(Integer s:weights.keySet()){
					double val=weights.get(s);
					weights.put(s, val/nb);
				}
			}
			
			/*double nbt=0.0;
			for(Integer s:weights.keySet()){
					double val=weights.get(s);
					nbt+=val*val;
			}
			double norm=0.0;
			if (nbt>0){
					norm=Math.sqrt(nbt);
			}
			if(norm>0){
				for(Integer s:weights.keySet()){
					double val=weights.get(s);
					weights.put(s, val/norm);
				}
			}*/
		//}
		
		return(weights);
	}
	
	public boolean isArtificial(){
		return false;
	}
	
}

