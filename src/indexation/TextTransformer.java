package indexation;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;

import actionsBD.MongoDB;
public abstract class TextTransformer {
	public abstract void learn();
	public abstract HashMap<Integer,Double> transform(HashMap<Integer,Double> text);
	public static void main(String[] args){
		IDFPruner pr=new IDFPruner(2000,"tweet09","stems_1",1);
		pr.learn();
	}
	public String toString(){
		return this.getClass().toString();
	}
	public static NoTransform getNoTransform(){
		return new NoTransform();
	}
}

class NoTransform extends TextTransformer{
	public void learn(){}
	public HashMap<Integer,Double> transform(HashMap<Integer,Double> text){
		return text;
	}
}
class IDFPruner extends TextTransformer {
	private int vocSize;
	private String db;
	private String collection;
	private int sens=1; //-1 highest idf, 1 lowest ones
	
	public IDFPruner(String db,String collection){
		this(2000,db,collection,1);
	}
	
	public IDFPruner(int vocSize,String db,String collection){
		this(vocSize,db,collection,1);
	}
	public IDFPruner(int vocSize,String db,String collection,int sens){
		this.vocSize=vocSize;
		this.db=db;
		this.collection=collection;
		this.sens=sens;
	}
	public void learn(){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		String tocollection=MongoDB.mongoDB.createCollection(db,"stems"," stems pruned to size "+vocSize+" from "+collection);
		DBCollection col2=MongoDB.mongoDB.getCollectionFromDB(db,tocollection);
		col.ensureIndex(new BasicDBObject("idf", sens));
		DBCursor cursor = col.find();
		cursor.limit(vocSize);
		cursor.sort(new BasicDBObject("idf", sens));
		int i=1;
		try {
			while(cursor.hasNext()) {
				DBObject res=cursor.next();
				/*int id=Integer.parseInt(res.get("id").toString());
				String stem=res.get("stem").toString();
				double idf=Double.valueOf(res.get("idf").toString());*/
				int id_old=Integer.parseInt(res.get("id").toString());
				res.put("id_old", id_old);
				res.put("id", i);
				col2.insert(res);
				i++;
			}
		} finally {
			cursor.close();
		}
		col2.ensureIndex(new BasicDBObject("stem", 1));
		col2.ensureIndex(new BasicDBObject("id_old", 1));
		col2.ensureIndex(new BasicDBObject("id", 1));
		/*ArrayList<String> freqs=new ArrayList<String>(stems.keySet());
		if(vocSize>0){
			FreqComp comp=new FreqComp(stems);
			Collections.sort(freqs, comp);
			freqs=new ArrayList<String>(freqs.subList(0, vocSize));
		}*/
		collection=tocollection;
		
	}
	/*private class FreqComp implements Comparator<String>
	{
		HashMap<String,Integer> freqs;
		public FreqComp(HashMap<String,Integer> freqs){
			this.freqs=freqs;
		}
		public int compare(String un,String deux){
			int x=freqs.get(un);
			int y=freqs.get(deux);
			if(x>y) return -1;
			if(x<y) return 1;
			return 0;
		}
	}*/
	
	public HashMap<Integer,Double> transform(HashMap<Integer,Double> text)
	{
		HashMap<Integer,Double> ret=new HashMap<Integer,Double>();
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		for(Integer id:text.keySet()){
			DBCursor cursor = col.find(new BasicDBObject("id_old",id));
			try {
				if(cursor.hasNext()) {
					DBObject res=cursor.next();
					int nid=Integer.parseInt(res.get("id").toString());
					ret.put(nid,text.get(id));
				}
			} finally {
				cursor.close();
			}
		}
		return ret;
	}
	
	public String toString(){
		return this.getClass().toString()+"_db="+db+"_col="+collection; //+"_vocSize="+vocSize+"_sens="+sens;
	}
	
}