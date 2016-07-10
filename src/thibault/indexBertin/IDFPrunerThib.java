package thibault.indexBertin;

import java.util.HashMap;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class IDFPrunerThib extends TextTransformerThib {
	private int vocSize;
	private String db;
	private String collection;
	private int sens=1; //-1 highest idf, 1 lowest ones
	
	public IDFPrunerThib(String db,String collection){
		this(2000,db,collection,1);
	}
	
	public IDFPrunerThib(int vocSize,String db,String collection){
		this(vocSize,db,collection,1);
	}
	public IDFPrunerThib(int vocSize,String db,String collection,int sens){
		this.vocSize=vocSize;
		this.db=db;
		this.collection=collection;
		this.sens=sens;
	}
	public void learn(){
		MongoDB m = new MongoDB("localhost");
		DBCollection col=m.getCollectionFromDB(db,collection);
		String tocollection=m.createCollection(db,"stems"," stems pruned to size "+vocSize+" from "+collection);
		DBCollection col2=m.getCollectionFromDB(db,tocollection);
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
	
	public HashMap<Integer,Double> transformBis(MongoDB m,HashMap<Integer,Double> text)
	{
		HashMap<Integer,Double> ret=new HashMap<Integer,Double>();
		//MongoDB m = new MongoDB("localhost");
		DBCollection col=m.getCollectionFromDB(db,collection);
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
		// m.mongo.close();
		return ret;
	}
	
	public String toString(){
		return this.getClass().toString()+"_db="+db+"_col="+collection; //+"_vocSize="+vocSize+"_sens="+sens;
	}

	@Override
	public HashMap<Integer, Double> transform(HashMap<Integer, Double> text) {
		// TODO Auto-generated method stub
		return null;
	}
	
}