package thomas.deprecated;
/*
import thomas.actionsBD.MongoDB;
import thomas.featurers.QueryFeaturer;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

*/

public class SingletonFeaturer/* extends Featurer*/ {

/*
	public SingletonFeaturer(String db, String documents, String stems){
		super(db, documents, stems);
		this.isSingleFeaturer=true;
	}

	public void computeFeatures(String repName) throws Exception {
		String featuresCollection = MongoDB.mongoDB.createCollection(db,repName,"queriesCollection");
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, featuresCollection);
		DBCollection queriesCol = MongoDB.mongoDB.getCollectionFromDB(db, documents);
		System.out.println("Creation de la collection  : "+ repName);
		DBCursor documentCursor = queriesCol.find();
		DBObject query;
		int idr;
		System.out.println("Calcul des features");
		if(!documentCursor.hasNext())throw new Exception("Base de documents vide ou inexistante");

		while (documentCursor.hasNext()){
			query = documentCursor.next();
			idr = (Integer)query.get("id");
			BasicDBObject obj = new BasicDBObject();
			for(QueryFeaturer featurer:featurers){
				obj.put("idr", idr);
				obj.put("idq", 0);
				obj.put(featurer.toString(), featurer.getFeature(query, null));
			}
			col.insert(obj);
			obj.clear();
		}
		col.ensureIndex(new BasicDBObject("idr",1));
	}

*/
}