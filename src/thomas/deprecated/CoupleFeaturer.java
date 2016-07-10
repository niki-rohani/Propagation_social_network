
package thomas.deprecated;
/*

import thomas.actionsBD.MongoDB;
import thomas.featurers.QueryFeaturer;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
*/

public class CoupleFeaturer /*extends Featurer*/{
	/*String queries;

	public CoupleFeaturer(String db, String reviewsCollection, String queries, String stems){
		super(db, reviewsCollection, stems);
		this.queries=queries;
		this.isSingleFeaturer=false;
	}

	public void computeFeatures(String repName) throws Exception{
		String featuresCollection = MongoDB.mongoDB.createCollection(db, repName," features from " + db + " : " + documents + " and " + queries);
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, featuresCollection);
		System.out.println("Creation de la collection : " + repName);
		DBCollection queriesCol = MongoDB.mongoDB.getCollectionFromDB(db, queries);
		DBCollection reviewsCol = MongoDB.mongoDB.getCollectionFromDB(db, documents);

		DBCursor reviewsCursor, queriesCursor;
		queriesCursor = queriesCol.find();
		DBObject query, review;
		int idq, idr;
		System.out.println("Calcul des features");
		if(!queriesCursor.hasNext())throw new Exception("Base de requete vide ou inexistante");
		while (queriesCursor.hasNext()){
			query = queriesCursor.next();
			idq = (Integer)query.get("id");
			reviewsCursor = reviewsCol.find();
			while(reviewsCursor.hasNext()){
				review = reviewsCursor.next();
				idr = (Integer)review.get("id");
				BasicDBObject obj = new BasicDBObject();
				for(QueryFeaturer featurer:featurers){
					obj.put("idq", idq);
					obj.put("idr", idr);
					obj.put(featurer.toString(), featurer.getFeature(query, review));
				}
				col.insert(obj);
				obj.clear();
			}
		}
		
		BasicDBObject obj = new BasicDBObject();
		obj.put("idq", 1);
		obj.put("idr", 1);
		col.ensureIndex(obj);
	}*/
}
