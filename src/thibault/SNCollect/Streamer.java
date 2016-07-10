package thibault.SNCollect;

import java.net.UnknownHostException;
import java.util.HashSet;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import core.Post;


public abstract class Streamer {
	  public int verbose=0;
	  public abstract HashSet<Post> streamWhileT(HashSet<String> what,long t);
	  public abstract long getT();
	  public abstract void reinitStreamer();
	  public abstract HashSet<String> getNewArms();
	  public abstract HashSet<String> getArmNames();
}


class StreamerDataBase extends Streamer{
	long currentT;
	String db;
	String posts_col;
	long lastT;
	long initT;
	DBCollection collectionToStream;
	
	public StreamerDataBase(String db,String posts_col){
		this.db=db;
		this.posts_col=posts_col;
		Mongo mongo = null;
		try {
			mongo = new Mongo( "localhost" );
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		DB dbs = mongo.getDB(db);
		DBCollection col = dbs.getCollection(posts_col);
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
		return null;
	}

	@Override
	public HashSet<String> getArmNames() {
		return null;
	}


}
