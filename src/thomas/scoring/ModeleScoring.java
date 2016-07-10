package thomas.scoring;

import java.util.ArrayList;

import actionsBD.MongoDB;
import thomas.features.Feature;
import thomas.featuresProduction.FeatureList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;


public abstract class ModeleScoring {

	public ArrayList<Double> parametres;
	
	public ModeleScoring(int n){
		parametres = new ArrayList<Double>();
		for (int i = 0 ; i < n ; i++){
			parametres.add(1.0);
		}
	}
	public ModeleScoring(int size, double init) {
		parametres = new ArrayList<Double>();
		for (int i = 0 ; i < size ; i++){
			parametres.add(init);
		}
	}
	
	public abstract double computeScore(ArrayList<Double> rep) throws Exception;
	
	@SuppressWarnings("unchecked")
	public void setWeightsQuicklyForPolarity(String db) {
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, "stems_1");
		DBCollection infCol = MongoDB.mongoDB.getCollectionFromDB(db, "stems_1_informations_1");
		String key;
		int id;
		DBCursor cursor, infCursor;
		
		DBObject query = new BasicDBObject();
		query.put("key", "mostUsed");
		infCursor = infCol.find(query);
		ArrayList<DBObject> liste = (ArrayList<DBObject>) infCursor.next().get("val");
		
		
		DBObject good = new BasicDBObject();
		good.put("stem", "good");
		cursor = col.find(good);
		id = (Integer) cursor.next().get("id");
		
		for(DBObject stem : liste){
			key = stem.keySet().toArray()[0].toString();
			if ((Integer)stem.get(key)==id){
				parametres.set(Integer.parseInt(key), 1.0);
			}
		}
		
		DBObject bad = new BasicDBObject();
		good.put("stem", "bad");
		cursor = col.find(bad);
		id = (Integer) cursor.next().get("id");
		for(DBObject stem : liste){
			key = stem.keySet().toArray()[0].toString();
			if ((Integer)stem.get(key)==id){
				parametres.set(Integer.parseInt(key), -1.0);
			}
		}
	}
	public void setWeightsQuicklyForRelevance(FeatureList featurer) {
		int i = 0;
		for (Feature f : featurer.featurers){
			if(f.toString().equals("StreamLength")) parametres.set(i, 0.01);
			if(f.toString().equals("TF")) parametres.set(i, 0.1);
			if(f.toString().equals("SimilariteCosinus")) parametres.set(i, 100.0);
			i++;
		}
	}
	
}
