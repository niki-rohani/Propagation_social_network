package thibault.dynamicCollect;

import java.net.UnknownHostException;
import java.util.HashSet;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import core.Post;
import core.User;
public abstract class Streamer {
  public int verbose=0;
  public abstract HashSet<Post> streamWhileT(HashSet<String> what,long t);
  public abstract long getT();
  public abstract void reinitStreamer();
  public abstract HashSet<String> getNewArms();
  public abstract HashSet<String> getArmNames();
}

class DataBaseStreamer extends Streamer{
	long currentT;
	String db;
	String posts_col;
	long lastT;
	long initT;
	DBCollection collectionToStream;
	
	public DataBaseStreamer(String db,String posts_col){
		this.db=db;
		this.posts_col=posts_col;
		//DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,posts_col);
		
		///Test pour faire tourner sur mon ordi
		Mongo mongo = null;
		try {
			//mongo = new Mongo( "132.227.201.134" );
			mongo = new Mongo( "localhost" );
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DB dbs = mongo.getDB( db );
		DBCollection col = dbs.getCollection(posts_col);
		//Fin test
		this.collectionToStream=col;
		col.ensureIndex(new BasicDBObject("timestamp", 1));
		BasicDBObject sort=new BasicDBObject();
		sort.put("timestamp", 1);
		DBCursor cursor = col.find().sort(sort).limit(1);
		try {
			if(cursor.hasNext()) {
				DBObject res=cursor.next();
				currentT=Long.valueOf(res.get("timestamp").toString());
			}
		} finally {
			cursor.close();
		}
		initT=currentT;
		
		sort.put("timestamp", -1);
		cursor = col.find().sort(sort).limit(1);
		try {
			if(cursor.hasNext()) {
				DBObject res=cursor.next();
				lastT=Long.valueOf(res.get("timestamp").toString());
			}
		} finally {
			cursor.close();
		}
	}
	
	public String toString(){
		return "DataBaseStreamer_"+db+"_"+posts_col;
	}
	
	public long getT(){
		return currentT;
	}
	
	public void reinitStreamer(){
		currentT=initT;
	}
	
	/**
	 * Streams the database from currentT while t steps.
	 * The HashSet what is not used in this streamer. 
	 * @param what
	 * @param t
	 * @return
	 */
	public HashSet<Post> streamWhileT(HashSet<String> what,long t){
		
		BasicDBObject query=new BasicDBObject();
		BasicDBObject comp=new BasicDBObject();
		comp.put("$gte", currentT);
		comp.put("$lte", currentT+t);
		query.put("timestamp", comp);

		 HashSet<Post> set=new HashSet<Post>();
		 DBCursor cursor = this.collectionToStream.find(query);
		 try {
				while(cursor.hasNext()) {
					DBObject res=cursor.next();
					Post p=Post.getPostFrom(res);
					if (p!=null){
						set.add(p);
					}
				}
					
			} finally {
				cursor.close();
			}

		
		if(verbose>=1){
			System.out.println(set.size()+" collectes de "+currentT+" a "+(currentT+t));
		}
		
		/*for(Post p:set){
			if(userNames){
				// Select w.r.t. owner names
				if(what.contains(p.getOwner().getName())){
					ret.add(p);
				}
			}
			else{
				// Select w.r.t. words
				boolean ok=false;
				for(String w:what){
					if(p.getWeights().containsKey(w)){
						ok=true;
						break;
					}
				}
				if(ok){
					ret.add(p);
				}
			}
			
		}*/
		currentT+=t;
		if(currentT>lastT){
			return null;
		}
		return set;
	}

	@Override
	public HashSet<String> getNewArms() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashSet<String> getArmNames() {
		// TODO Auto-generated method stub
		return null;
	}


}