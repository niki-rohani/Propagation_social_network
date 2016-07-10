package thibault.indexBertin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import wordsTreatment.Stemmer;
import wordsTreatment.WeightComputer;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;






import actionsBD.MongoDB;
import actionsBD.MySQLConnection;

public class TF_WeighterThib extends WeightComputer {

	
	public TF_WeighterThib(){
		this("propagation","stems");
	}
	public TF_WeighterThib(String dbName,String colName){
		super(dbName,colName);
	}
	

	//@Override 
	public HashMap<String,Double> getWeightsForStems(MongoDB m,String st){
		Stemmer stemmer=new Stemmer();
		HashMap<String,Integer> w=stemmer.porterStemmerHash(st);
		w.remove(" * ");
		HashMap<String,Double> poids=new HashMap<String,Double>();
		//MongoDB m = new MongoDB("localhost");
		DBCollection col=m.getCollectionFromDB(dbName,colName);
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
					System.out.println(stem+"  ignore => pas dans table stems");
				}
			} finally {
				cursor.close();
			}
		}
		// m.mongo.close();
		return(poids);
	}
	
	//@Override 
	public HashMap<Integer,Double> getWeightsForIds(MongoDB m,String st){
		Stemmer stemmer=new Stemmer();
		HashMap<String,Integer> w=stemmer.porterStemmerHash(st);
		w.remove(" * ");
		HashMap<Integer,Double> poids=new HashMap<Integer,Double>();
		//MongoDB m = new MongoDB("localhost");
		DBCollection col=m.getCollectionFromDB(dbName,colName);
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
		// m.mongo.close();
		return(poids);
	}
	
	@Override	
	public String toString(){
		return("TF_"+dbName+"."+colName);
	}
	@Override
	public HashMap<String, Double> getWeightsForStems(String st) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public HashMap<Integer, Double> getWeightsForIds(String st) {
		// TODO Auto-generated method stub
		return null;
	}
	


}
