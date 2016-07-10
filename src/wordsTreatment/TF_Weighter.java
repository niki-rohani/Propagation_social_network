package wordsTreatment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;



import actionsBD.MongoDB;
import actionsBD.MySQLConnection;

public class TF_Weighter extends WeightComputer {

	
	public TF_Weighter(){
		this("propagation","stems");
	}
	public TF_Weighter(String dbName,String colName){
		super(dbName,colName);
	}
	
	@Override 
	public HashMap<String,Double> getWeightsForStems(String st){
		Stemmer stemmer=new Stemmer();
		HashMap<String,Integer> w=stemmer.porterStemmerHash(st);
		w.remove(" * ");
		HashMap<String,Double> poids=new HashMap<String,Double>();
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(dbName,colName);
		BasicDBObject query = new BasicDBObject();
		for(String stem:w.keySet()){
			query.put("stem", stem);
			DBCursor cursor = col.find(query);
			try {
				if(cursor.hasNext()) {
					DBObject res=cursor.next();
					double tf=(1+Math.log(w.get(stem)));
					poids.put(stem, tf);
				}
				else{
					//System.out.println(stem+"  ignore => pas dans table stems");
				}
			} finally {
				cursor.close();
			}
		}
		return(poids);
	}
	
	@Override 
	public HashMap<Integer,Double> getWeightsForIds(String st){
		Stemmer stemmer=new Stemmer();
		HashMap<String,Integer> w=stemmer.porterStemmerHash(st);
		w.remove(" * ");
		HashMap<Integer,Double> poids=new HashMap<Integer,Double>();
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(dbName,colName);
		BasicDBObject query = new BasicDBObject();
		for(String stem:w.keySet()){
			query.put("stem", stem);
			DBCursor cursor = col.find(query);
			try {
				if(cursor.hasNext()) {
					DBObject res=cursor.next();
					//double idf=Double.valueOf(res.get("idf").toString());
					int id=Integer.parseInt(res.get("id").toString());
					double tf=(1+Math.log(w.get(stem)));
					poids.put(id, tf);
				}
				else{
					//System.out.println(stem+"  ignore => pas dans table stems");
				}
			} finally {
				cursor.close();
			}
		}
		return(poids);
	}
	
	@Override	
	public String toString(){
		return("TF_"+dbName+"."+colName);
	}
	
	public static void main(String[] args){
		TF_Weighter wc=new  TF_Weighter();
		String s="information retrieval refers to the task of retrieving documents that are thematically related to a user's query";
		System.out.println(s);
		HashMap<Integer,Double> poids=wc.getWeightsForIds(s);
		System.out.println(poids);
		HashMap<String,Double> spoids=wc.getWeightsForStems(s);
		System.out.println(spoids);
	}

}
