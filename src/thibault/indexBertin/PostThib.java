package thibault.indexBertin;

import java.util.HashMap;
import java.util.HashSet;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Link;
import core.Post;
import core.Text;
import core.User;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.bson.BSONObject;
public class PostThib extends Post {
	public static final long serialVersionUID=1;
	public static HashMap<Integer,PostThib> PostThibs=new HashMap<Integer,PostThib>(); 
	//private HashMap<PostThib,Double> ref=null; // if not null => references other similar PostThibs (eg., if PostThib is a digg of a given PostThib, this contains the PostThib in concern, if it is a text with hashtags, it references other PostThibs with this tag, if we classified PostThibs in topics, it may contains other PostThibs of the same topic, etc... it also may contains link values between PostThibs such as content similarities)
	private HashSet<Link> ref=null;
	private User owner;
	private long timestamp; 
	private HashMap<String,String> tags;


	private DBObject other;  // contains every other info 
	
	
	
	public PostThib(int id,String texte, User owner, long timestamp, HashMap<Integer,Double> poids){
		this(id,texte,owner,timestamp,poids,new BasicDBObject());
	}
	
	public PostThib(int id,String texte, User owner, long timestamp, HashMap<Integer,Double> poids, DBObject other){	
		super(id,texte,owner, timestamp,poids,other);
		this.other=other; 
		if (id>=0){
			if (PostThibs.containsKey(id)){
				System.out.println("PostThib "+id+" existe deja => id du nouveau PostThib = "+this.id);
			}
			else{
				this.id=id;
			}
		}
		while(PostThibs.containsKey(getID())){
			System.out.println("Probleme duplicate PostThib, id changed...");
			this.id++;
		}
		name="PostThib_"+id;
		this.owner=owner;
		this.timestamp=timestamp;
		owner.addPost(this);
		tags=new HashMap<String,String>();
		PostThibs.put(getID(), this);
	}
	
	// text of the PostThib, timestamp, and weights
	public PostThib(String texte, User owner, long timestamp, HashMap<Integer,Double> poids){
		this(-1,texte,owner,timestamp,poids);
	}
	
	public PostThib(String name, User owner, long timestamp, HashMap<Integer,Double> poids,DBObject obj){
			this(-1,name,owner,timestamp,poids,obj);
	}
	/*PostThib(String texte, long timestamp){
		this(texte, timestamp, null);
	}*/
	
	public static PostThib getPostThib(int id){
		return(PostThibs.get(id));
	}
	
	public static void reinitPostThibs(){
		PostThibs=new HashMap<Integer,PostThib>();
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
	
	/*public void addReference(PostThib p) throws Exception{
		addReference(new Link<PostThib>(" ",this,p));
	}
	public void addReference(PostThib p, double val) throws Exception{
		addReference(new Link<PostThib>(" ",this,p,val));
	}
	public void addReference(String type,PostThib p, double val) throws Exception{
		addReference(new Link<PostThib>(type,this,p,val));
	}
	public void addReference(Link<PostThib> link) throws Exception{
		PostThib p=link.getNode2();
		if (p==this){
			p=link.getNode1();
		}
		if (p==null){
			throw new Exception("AddReference Exception : le PostThib a ajouter en reference de "+this+" n existe pas...");
		}
		if (ref==null){
			ref=new HashSet<Link<PostThib>>(); //Map<PostThib,Double>();
		}
		if (p.ref==null){
			p.ref=new HashSet<Link<PostThib>>(); //Map<PostThib,Double>();
		}
		
		ref.add(link); //(p, val);
		p.ref.add(link); //put(this,val);
		
	}*/
	public int hashCode(){
		return(("PostThib"+id).hashCode());
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
	
	public DBObject getOther(){
		return other;
	}
	
	public String toString(){
		String s="PostThib "+id+" (at "+getTimeStamp()+") "+getTexte();
		/*if ((ref!=null) && (ref.size()>0)){
			s+="\n\t Relations = ";
			for(Link<PostThib> link:ref){
				PostThib r=link.getNode2();
				if (r==this){
					r=link.getNode1();
				}
				s+=r.getID()+" (at "+r.getTimeStamp()+") type "+link.getTitre()+" = "+link.getVal()+";";
			}
		}*/
		return s;
	}
	
	public void indexIntoBis(MongoDB m,String db,String collection){
		//MongoDB m = new MongoDB("localhost");
		DBCollection col=m.getCollectionFromDB(db,collection);
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
			for(Link<PostThib> l:ref){
				PostThib n2=l.getNode2();
				if (n2==this){
					n2=l.getNode1();
				}
				BasicDBObject rl=new BasicDBObject();
				rl.put("type", l.getType());
				rl.put("PostThib", n2.getID());
				rel.add(rl);
			}
		}*/
		//obj.put("rel", rel);
		//System.out.println("Insertion ");
		//System.out.println(obj);
		col.insert(obj);
				
	}
	
	public static PostThib getPostThibFrom(DBObject res){
		return(getPostThibFrom(res,true));
	}
	
	// if ignoreIfExists true and id of PostThib already exists in PostThibs, then this old PostThib is returned. If false, a new PostThib with a new id is created 
	public static PostThib getPostThibFrom(DBObject res, boolean ignoreIfExists){
		//System.out.println("getPostThib");
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
		PostThib old=null;
		PostThib p=null;
		if (ignoreIfExists){
			old=getPostThib(id);
		}
		if (old==null){
			p=new PostThib(id,texte,owner,Long.valueOf(res.get("timestamp").toString()),w);
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
	
		/*for(Integer iPostThib:relations.keySet()){	
			PostThib PostThib=PostThib.getPostThib(iPostThib);
			ArrayList<DBObject> list=relations.get(iPostThib);
			HashSet<Link<PostThib>> ref=new HashSet<Link<PostThib>>();
			for(DBObject obj:list){
				String type=obj.get("type").toString();
				int p2=Integer.parseInt(obj.get("PostThib").toString());
				PostThib PostThib2=PostThib.getPostThib(p2);
				if (PostThib2==null){
					System.out.println("Attention : le PostThib "+p2+" n existe pas dans les resultats (lien avec "+iPostThib+" ignore)");
				}
				else{
					Link<PostThib> link=new Link<PostThib>(type,PostThib,PostThib2);
					PostThib.ref.add(link);
				}
			}
		}*/
		return(p);
	}
	public static PostThib getPostThibFromDB(String db,String collection,BasicDBObject query){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		DBCursor cursor = col.find(query);
		PostThib p=null;
		try {
			if(cursor.hasNext()) {
				DBObject res=cursor.next();
				p=getPostThibFrom(res);
			}
		} finally {
			cursor.close();
		}
		return(p);
		
	}
	
	public static HashSet<PostThib> getPostThibsFromDB(String db,String collection,BasicDBObject query){
		MongoDB m = new MongoDB("localhost");
		DBCollection col=m.getCollectionFromDB(db,collection);
		HashSet<PostThib> PostThibs=new HashSet<PostThib>();
		DBCursor cursor = col.find(query);
		try {
			//HashMap<Integer,ArrayList<DBObject>> relations=new HashMap<Integer,ArrayList<DBObject>>();
			while(cursor.hasNext()) {
				DBObject res=cursor.next();
				PostThib p=getPostThibFrom(res);
				if (p!=null){
					PostThibs.add(p);
				}
			}
				
		} finally {
			cursor.close();
		}
		return(PostThibs);
	}
	
}
