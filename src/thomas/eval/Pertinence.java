package thomas.eval;

import actionsBD.MongoDB;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;


public class Pertinence extends Jugement {

	public Pertinence(String db, String documents, String queries) {
		super(db, documents, queries, "pertinence");
	}

	@Override
	public boolean isRelevant(int idr, int idq) {
		DBCollection docs = MongoDB.mongoDB.getCollectionFromDB(db, documents);
		DBObject review, query;
		DBCursor rCursor, qCursor;
		
		query = new BasicDBObject();
		review = new BasicDBObject();

		query.put("id", idq);
		review.put("id", idr);

		rCursor = docs.find(review);
		qCursor = docs.find(query);
		
		query =rCursor.next();
		review = qCursor.next();
		//System.out.println("idq = " + idq +", idr = " + idr +"  , " +(String)(query.get("productId")) + " == " + (String)review.get("productId") 
		//+ "==> " + ((String)(query.get("productId"))).equals((String)review.get("productId"))	);
		return ((String)(query.get("productId"))).equals((String)review.get("productId"));
	}

	/*
	public int countNRelevant(DBObject query) {
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, documents);
		String idProduct = (String)query.get("productId");
		DBObject match = new BasicDBObject("$match", new BasicDBObject("idProduct", idProduct));
		AggregationOutput output = col.aggregate(match);
		int n=0;
		for(@SuppressWarnings("unused") DBObject obj:output.results()){
			n++;
		}
		return n;
	}
*/
}
