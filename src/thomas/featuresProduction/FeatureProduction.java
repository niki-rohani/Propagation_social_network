package thomas.featuresProduction;

import java.util.ArrayList;
import actionsBD.MongoDB;
import thomas.features.Feature;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class FeatureProduction{

	private static String createFeatureCollection(String db, String documents, String queries, String features) {
		System.out.println("Creation de la base de features");
		features = MongoDB.mongoDB.createCollection(db, features, "features from " + db);
		DBCollection featuresCol = MongoDB.mongoDB.getCollectionFromDB(db, features);
		DBCollection docs = MongoDB.mongoDB.getCollectionFromDB(db, documents);
		DBCollection queriesCol = MongoDB.mongoDB.getCollectionFromDB(db, queries);
		DBCursor qCursor, dCursor = docs.find();
		DBObject d, q;
		int idr, idq;

		while (dCursor.hasNext()){
			d = dCursor.next();
			idr = (Integer)d.get("id");
			qCursor = queriesCol.find();
			while(qCursor.hasNext()){
				q = qCursor.next();
				idq = (Integer)q.get("id");
				BasicDBObject obj = new BasicDBObject();
				obj.put("idr", idr);
				obj.put("idq", idq);
				featuresCol.insert(obj);
			}
		}
		
		BasicDBObject obj = new BasicDBObject();
		obj.put("idr", 1);
		obj.put("idq", 1);
		featuresCol.ensureIndex(obj);
		return features;
	}

	private static void computeFeatures(String db, String documents,
			String queries, String features, ArrayList<FeatureList> featurers) throws Exception {
		DBCollection featuresCol = MongoDB.mongoDB.getCollectionFromDB(db, features);
		DBCollection docs = MongoDB.mongoDB.getCollectionFromDB(db, documents);
		DBCollection queriesCol = MongoDB.mongoDB.getCollectionFromDB(db, queries);

		DBCursor reviewsCursor, queriesCursor;
		queriesCursor = queriesCol.find();
		DBObject query, review;
		int idq, idr;
		
		System.out.println("Calcul des features");

		if(!queriesCursor.hasNext())throw new Exception("Base de requete vide ou inexistante");
		
		while (queriesCursor.hasNext()){
			query = queriesCursor.next();
			idq = (Integer)query.get("id");
			reviewsCursor = docs.find();
			while(reviewsCursor.hasNext()){
				review = reviewsCursor.next();
				idr = (Integer)review.get("id");
				
				BasicDBObject obj = new BasicDBObject();
				obj.put("idr", idr);
				obj.put("idq", idq);
				DBCursor feature = featuresCol.find(obj);
				obj = (BasicDBObject) feature.next();
				for( FeatureList featurer : featurers){
					for(Feature qf:featurer.featurers){
						if (!obj.containsField(qf.toString())){
							featuresCol.remove(obj);
							obj.put(qf.toString(), qf.getFeature(query, review));
						}
						featuresCol.insert(obj);
					}
				}
			}	
		}
	}


	public static void main(String[] args){

		String db = "finefoods";
		String reviews = "documents_1";
		String queries = "queries_1";
		String stems = "stems_1";

		ArrayList<FeatureList> featurers = new ArrayList<FeatureList>();
		featurers.add(new RelevanceFeatures(db, stems));
		featurers.add(new SentimentFeatures(db, stems));
		createFeatureCollection(db, reviews, queries, "features");

		try {
		computeFeatures(db, reviews, queries, "features_1", featurers);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
