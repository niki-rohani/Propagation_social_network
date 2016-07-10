package thomas.features;

import java.util.ArrayList;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;


public class FrequencyComputer {
	private DBCollection stems;
	private DBCollection stems_informations;

	public FrequencyComputer(String db, String stems){
		this.stems = MongoDB.mongoDB.getCollectionFromDB(db, stems);
		this.stems_informations = MongoDB.mongoDB.getCollectionFromDB(db, stems+"_info");
	}

	@SuppressWarnings("unchecked")
	public int getLength(DBObject requete){
		int length=0;
		String key;
		ArrayList<DBObject> stems = (ArrayList<DBObject>) requete.get("weights");
		for(DBObject stem : stems){
			key = stem.keySet().toArray()[0].toString();
			length+=getFrequencyFromTfidf((Double)stem.get(key), key);
		}
		return length;
	}

	@SuppressWarnings("unchecked")
	public int getDistinctLength(DBObject requete){
		ArrayList<DBObject> stems = (ArrayList<DBObject>) requete.get("weights");
		return stems.size();
	}

	public double getIdf(String id){
		BasicDBObject query = new BasicDBObject();
		query.put("id", Integer.parseInt(id));
		DBCursor cursor = stems.find(query);
		double idf;
		try {
			if(cursor.hasNext()) {
				DBObject res=cursor.next();
				idf=Double.valueOf(res.get("idf").toString());
			}else{
				System.out.println(id+"  ignore => pas dans table stems");
				throw new MongoException("Mauvaise Indexation");
			}
		} finally {
			cursor.close();
		}
		return idf;
	}


	public double getFrequencyFromTfidf(Double tfidf, String id){
		double idf = getIdf(id);
		try{
			return Math.round(Math.pow(Math.E, ((tfidf/idf)-1)));
		}catch(ArithmeticException e){
			e.printStackTrace();
			return 0;
		}
	}
	//si tf  = 1 cest que le mot apparait une fois dans le doc car on calcule tf = 1 +log(1)
	public double getTfFromTfidf(Double tfidf, String id) {
		double idf = getIdf(id);
		try{
			return (tfidf/idf);
		}catch(ArithmeticException e){
			e.printStackTrace();
			return 0;
		}		
	}

	//nombre de mots de la base
	public int getNWords() {
		BasicDBObject query = new BasicDBObject();
		query.put("key", "nWords");
		DBCursor cursor = stems_informations.find(query);
		try {
			if(cursor.hasNext()) {
				DBObject res=cursor.next();
				return (Integer)res.get("val");
			}else{
				throw new MongoException("Information manquante");
			}
		} finally {
			cursor.close();
		}
	}
	
	//nombre de mots distincts de la base
		public int getNDistinctWords() {
			BasicDBObject query = new BasicDBObject();
			query.put("key", "nDistinctWords");
			DBCursor cursor = stems_informations.find(query);
			try {
				if(cursor.hasNext()) {
					DBObject res=cursor.next();
					return (Integer)res.get("val");
				}else{
					throw new MongoException("information manquante");
				}
			} finally {
				cursor.close();
			}
		}

		public DBObject getMostUsed() {
			BasicDBObject query = new BasicDBObject();
			query.put("key", "mostUsed");
			DBCursor cursor = stems_informations.find(query);
			try {
				if(cursor.hasNext()) {
					DBObject res=cursor.next();
					return (DBObject) res.get("val");
				}else{
					throw new MongoException("Information manquante");
				}
			} finally {
				cursor.close();
			}
		}
		
		public double getMeanNWords(){
			BasicDBObject query = new BasicDBObject();
			query.put("key", "meanNWords");
			DBCursor cursor = stems_informations.find(query);
			try {
				if(cursor.hasNext()) {
					DBObject res=cursor.next();
					return (Double)res.get("val");
				}else{
					throw new MongoException("Information manquante");
				}
			} finally {
				cursor.close();
			}
		}
		
		
		
	public int getNOcc(String id) {
		BasicDBObject query = new BasicDBObject();
		query.put("id",Integer.parseInt(id));
		DBCursor cursor = stems.find(query);
		try {
			if(cursor.hasNext()) {
				DBObject res=cursor.next();
				return (Integer)res.get("nOcc");
			}else{
				throw new MongoException("Mauvaise indexation stem ");
			}
		} finally {
			cursor.close();
		}	}


	public static void main(String[] args){
		//String db="finefoods";
		//String stems = "stems_1";
		//FrequencyComputer fcomputer = new FrequencyComputer(db, stems);
		//fcomputer.getFrequencyFromTfidf(tfidf, id)

		//test retrouver infos avec tfidf
		int n = 120;
		System.out.println("le stem apparait n = " + n + " fois dans le doc.");
		System.out.println("le tf vaut 1 + log (n) = 1 + " + Math.log(n) + ".");
		double tf = 1 + Math.log(n);
		System.out.println("le tf vaut 1 + log (n) = "+ (1 + Math.log(n)));
		double idf = Math.log(3000/200);
		System.out.println("l'idf vaut log (ndBase/ndBase) comprenant le stem = log (3000/200) = " + Math.log(3000/200));
		double tfidf = tf*idf;
		System.out.println("Le tf idf vaut donc tf*idf = " +(tf*idf));
		System.out.println("Resolution");
		System.out.println("On retrouve le tf par tfidf/idf = " + tfidf/idf);
		System.out.println("On retrouve n par = " + Math.round(Math.pow(Math.E, ((tfidf/idf)-1))));
	}

	


}
