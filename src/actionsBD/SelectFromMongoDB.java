package actionsBD;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.BasicDBObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import core.Post;

public class SelectFromMongoDB {

	
	public static HashMap<String, HashSet<String>> converts(HashMap<String, ArrayList<DBObject>> whereValsIn){
		HashMap<String,HashSet<String>> v=new HashMap<String,HashSet<String>>();
		for(String k:whereValsIn.keySet()){
			HashSet<String> vals=new HashSet<String>();
			v.put(k, vals);
			for(DBObject obj:whereValsIn.get(k)){
				String val=obj.get(k).toString();
				vals.add(val);
			}
			
		}
		return v;
	}
	public static ArrayList<DBObject> selectFrom(String db, String collection,DBObject where){
		return selectFrom(db,collection,where,new HashMap<String, HashSet<String>>());
	}
	public static ArrayList<DBObject> selectFrom(String db, String collection,DBObject where,HashMap<String, HashSet<String>> whereValsIn){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		
		DBCursor cursor = col.find(where);
		ArrayList<DBObject> ret=new ArrayList<DBObject>();
		try {
			while(cursor.hasNext()) {
				DBObject res=cursor.next();
				for(String k:whereValsIn.keySet()){
					HashSet<String> h=whereValsIn.get(k);
					/*BSONObject o=res.get(k);
					if()*/
					String v=res.get(k).toString();
					if(!h.contains(v)){
						continue;
					}
				}
				ret.add(res);
			}
		} finally {
			cursor.close();
		}
		return ret;
	}
	
	public static ArrayList<String> project(String onWhat, ArrayList<DBObject> objs){
		ArrayList<String> ret=new ArrayList<String>();
		for(DBObject res:objs){
			ret.add(res.get(onWhat).toString());
		}
		return ret;
	}
	
	public static void main(String[] args){
		DBObject query=new BasicDBObject();
		query.put("owner","HenryRoy5");
		ArrayList<DBObject> posts=selectFrom("usElections5000_hashtag","posts_3",query);
		
	}
	
}
