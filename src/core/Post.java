package core;

import java.util.HashMap;
import java.util.HashSet;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.bson.BSONObject;
public class Post extends Text {
	public static final long serialVersionUID=1;
	public static HashMap<Integer,Post> posts=new HashMap<Integer,Post>(); 
	//private HashMap<Post,Double> ref=null; // if not null => references other similar posts (eg., if post is a digg of a given post, this contains the post in concern, if it is a text with hashtags, it references other posts with this tag, if we classified posts in topics, it may contains other posts of the same topic, etc... it also may contains link values between posts such as content similarities)
	protected HashSet<Link> ref=null;
	protected User owner;
	protected long timestamp; 
	protected HashMap<String,String> tags;
	protected DBObject other;  // contains every other info 
	
	
	public Post(int id, User owner, long timestamp){
		this(id,"",owner,timestamp,null,null);
	}
	public Post(int id,String texte, User owner, long timestamp, HashMap<Integer,Double> poids){
		this(id,texte,owner,timestamp,poids,new BasicDBObject());
	}
	
	public Post(int id,String texte, User owner, long timestamp, HashMap<Integer,Double> poids, DBObject other){	
		super(texte,poids);
		this.other=other; 
		if (id>=0){
			if (posts.containsKey(id)){
				System.out.println("Post "+id+" existe deja => id du nouveau post = "+this.id);
			}
			else{
				this.id=id;
			}
		}
		while(posts.containsKey(getID())){
			System.out.println("Probleme duplicate post, id changed...");
			this.id++;
		}
		name="Post_"+id;
		this.owner=owner;
		this.timestamp=timestamp;
		if(owner!=null){
			owner.addPost(this);	
		}
		
		tags=new HashMap<String,String>();
		posts.put(getID(), this);
	}
	
	public Post(int id, String titre, HashMap<Integer, Double> poids) {
		this(id, titre, null, 1, poids);
	}
	
	// text of the post, timestamp, and weights
	public Post(String texte, User owner, long timestamp, HashMap<Integer,Double> poids){
		this(-1,texte,owner,timestamp,poids);
	}
	
	public Post(String name, User owner, long timestamp, HashMap<Integer,Double> poids,DBObject obj){
			this(-1,name,owner,timestamp,poids,obj);
	}
	/*Post(String texte, long timestamp){
		this(texte, timestamp, null);
	}*/
	
	public static Post getPost(int id){
		return(posts.get(id));
	}
	
	public static void reinitPosts(){
		posts=new HashMap<Integer,Post>();
		
	}
	

	public User getOwner(){
		return(owner);
	}
	
	public void addTag(String tag){
		addTag(tag,1.0);
	}
	
	public void addTag(String tag,String val){
		tags.put(tag, val);
	}
	public void addTag(String tag,double val){
		tags.put(tag, ""+val);
	}
	
	public void videTags(){
		tags=new HashMap<String,String>();
	}
	
	public HashMap<String,String> getTags(){
		return(tags);
	}
	
	/*public void addReference(Post p) throws Exception{
		addReference(new Link<Post>(" ",this,p));
	}
	public void addReference(Post p, double val) throws Exception{
		addReference(new Link<Post>(" ",this,p,val));
	}
	public void addReference(String type,Post p, double val) throws Exception{
		addReference(new Link<Post>(type,this,p,val));
	}
	public void addReference(Link<Post> link) throws Exception{
		Post p=link.getNode2();
		if (p==this){
			p=link.getNode1();
		}
		if (p==null){
			throw new Exception("AddReference Exception : le post a ajouter en reference de "+this+" n existe pas...");
		}
		if (ref==null){
			ref=new HashSet<Link<Post>>(); //Map<Post,Double>();
		}
		if (p.ref==null){
			p.ref=new HashSet<Link<Post>>(); //Map<Post,Double>();
		}
		
		ref.add(link); //(p, val);
		p.ref.add(link); //put(this,val);
		
	}*/
	public int hashCode(){
		return(("Post"+id).hashCode());
	}
	public String getTexte(){
		return(titre);
	}
	public void setTexte(String texte){
		titre=texte;
	}
	public long getTimeStamp(){
		return timestamp;
	}
	
	public void setTimeStamp(long timestamp){
		this.timestamp=timestamp;
	}
	
	public DBObject getOther(){
		return other;
	}
	
	public String toString(){
		String s="Post "+id+" (at "+getTimeStamp()+") "+getTexte();
		/*if ((ref!=null) && (ref.size()>0)){
			s+="\n\t Relations = ";
			for(Link<Post> link:ref){
				Post r=link.getNode2();
				if (r==this){
					r=link.getNode1();
				}
				s+=r.getID()+" (at "+r.getTimeStamp()+") type "+link.getTitre()+" = "+link.getVal()+";";
			}
		}*/
		return s;
	}
	
	public void indexInto(String db,String collection){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		BasicDBObject obj = new BasicDBObject();
		obj.put("id", id);
		obj.put("text", getTexte());
		obj.put("timestamp", timestamp);
		obj.put("owner", owner.getName());
		HashMap<Integer,Double> w=getWeights();
		ArrayList<BasicDBObject> poids=new ArrayList<BasicDBObject>(); 
		for(Integer i:w.keySet()){
			BasicDBObject wi=new BasicDBObject();
			wi.put(i+"", w.get(i));
			poids.add(wi);
		}
		obj.put("weights",poids);
		ArrayList<BasicDBObject> tagListe=new ArrayList<BasicDBObject>();
		for(String tag:tags.keySet()){
			String val=tags.get(tag);
			BasicDBObject ti=new BasicDBObject();
			ti.put("tag",tag);
			
			ti.put("val", val);
			tagListe.add(ti);
		}
		obj.put("tags",tagListe);
		obj.put("other", other);
		
		/*ArrayList<BasicDBObject> rel=new ArrayList<BasicDBObject>();
		if (ref!=null){
			for(Link<Post> l:ref){
				Post n2=l.getNode2();
				if (n2==this){
					n2=l.getNode1();
				}
				BasicDBObject rl=new BasicDBObject();
				rl.put("type", l.getType());
				rl.put("post", n2.getID());
				rel.add(rl);
			}
		}*/
		//obj.put("rel", rel);
		//System.out.println("Insertion ");
		//System.out.println(obj);
		col.insert(obj);
	}
	
	public static Post getPostFrom(DBObject res){
		return(getPostFrom(res,true));
	}
	
	// if ignoreIfExists true and id of post already exists in posts, then this old post is returned. If false, a new post with a new id is created 
	public static Post getPostFrom(DBObject res, boolean ignoreIfExists){
		//System.out.println("getPost");
		int id=Integer.parseInt(res.get("id").toString());
		
		String texte=res.get("text").toString();
		String nown=res.get("owner").toString();
		User owner=User.getUser(nown);
		ArrayList<DBObject> list=(ArrayList<DBObject>)res.get("weights");
		HashMap<Integer,Double> w=new HashMap<Integer,Double>();
		for(DBObject obj:list){
			int st=Integer.parseInt(obj.keySet().iterator().next());
			double val=Double.valueOf(obj.get(st+"").toString());
			w.put(st,val);
		}
		Post old=null;
		Post p=null;
		if (ignoreIfExists){
			old=getPost(id);
		}
		if (old==null){
			p=new Post(id,texte,owner,Long.valueOf(res.get("timestamp").toString()),w);
		}
		else{
			p=old;
		}
		
		ArrayList<DBObject> ltags=(ArrayList<DBObject>)res.get("tags");
		//HashMap<Integer,Double> tags=new HashMap<Integer,Double>();
		for(DBObject obj:ltags){
			String tag=obj.get("tag").toString(); //keySet().iterator().next();
			//double val=Double.valueOf(obj.get("val").toString());
			String val=obj.get("val").toString();
			p.addTag(tag, val);
		}
		
		
		if(res.containsField("other")){
			DBObject other=(DBObject)res.get("other");
			p.other=other;
		}
		
		
		//ArrayList<DBObject> rel=(ArrayList<DBObject>)res.get("rel");
		//relations.put(id, rel);
	
		/*for(Integer ipost:relations.keySet()){	
			Post post=Post.getPost(ipost);
			ArrayList<DBObject> list=relations.get(ipost);
			HashSet<Link<Post>> ref=new HashSet<Link<Post>>();
			for(DBObject obj:list){
				String type=obj.get("type").toString();
				int p2=Integer.parseInt(obj.get("post").toString());
				Post post2=Post.getPost(p2);
				if (post2==null){
					System.out.println("Attention : le post "+p2+" n existe pas dans les resultats (lien avec "+ipost+" ignore)");
				}
				else{
					Link<Post> link=new Link<Post>(type,post,post2);
					post.ref.add(link);
				}
			}
		}*/
		return(p);
	}
	public static Post getPostFromDB(DBCollection col, BasicDBObject query){
		DBCursor cursor = col.find(query);
		Post p=null;
		try {
			if(cursor.hasNext()) {
				DBObject res=cursor.next();
				p=getPostFrom(res);
			}
		} finally {
			cursor.close();
		}
		return(p);
	}
	public static Post getPostFromDB(String db,String collection,BasicDBObject query){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		
		return getPostFromDB(col,query);
	}
	
	public static HashSet<Post> getPostsFromDB(String db,String collection,BasicDBObject query){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		HashSet<Post> posts=new HashSet<Post>();
		DBCursor cursor = col.find(query);
		try {
			//HashMap<Integer,ArrayList<DBObject>> relations=new HashMap<Integer,ArrayList<DBObject>>();
			while(cursor.hasNext()) {
				DBObject res=cursor.next();
				Post p=getPostFrom(res);
				if (p!=null){
					posts.add(p);
				}
			}
				
		} finally {
			cursor.close();
		}
		return(posts);
	}
	
	public static void main(String args[]){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB("aa","a");
		BasicDBObject bb=new BasicDBObject("jij.ei",1);
		col.insert(bb);
	}
}
