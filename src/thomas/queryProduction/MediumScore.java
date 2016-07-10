package thomas.queryProduction;

import actionsBD.MongoDB;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MediumScore extends PostSelector {

	public MediumScore(String db, String collection) {
		super(db, collection);
	}


	public String select(int nbPosts) {
		String requestables = MongoDB.mongoDB.createCollection(collection.getDB().getName(), "requestables"," requests from "+ collection.getFullName());
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(collection.getDB().getName(), requestables);
		String requests = MongoDB.mongoDB.createCollection(collection.getDB().getName(), "queries"," queries from "+ collection.getFullName());
		DBCollection requestsDB = MongoDB.mongoDB.getCollectionFromDB(collection.getDB().getName(), requests);
		System.out.println("Creation de la base de requetes reussie");
		System.out.println("Selection de " + nbPosts + " requetes dans la base : " + collection.getFullName() + "...");

		DBObject match = new BasicDBObject("$match", new BasicDBObject("score", 3));
		AggregationOutput output = collection.aggregate(match);

		for(DBObject obj : output.results()){
			col.insert(obj);
		}

		long count = col.count();
		if(nbPosts>count){
			nbPosts = (int)count;
			DBCursor cursor = col.find();
			while (cursor.hasNext()) {
				requestsDB.insert(cursor.next());
			}
		}else{
			long skip = Math.round(nbPosts);
			DBCursor cursor = col.find();
			while (requestsDB.count() < nbPosts) {
				int offset = (int) ((skip * requestsDB.count() + (int) ((Math.random() * skip) % count)) % count);
				DBObject next = cursor.skip(offset).next();
				requestsDB.insert(next);
				cursor.close();
				cursor = col.find();
			}
		}
		col.drop();
		System.out.println("Selection de " + nbPosts + " requetes terminee");
		requestsDB.ensureIndex(new BasicDBObject("id",1));
		return requests;
	}

	public static void main(String[] args){
		MediumScore selector = new MediumScore("finefoods", "foodReviews_1");
		@SuppressWarnings("unused")
		String requests = selector.select(3);
	}

}
