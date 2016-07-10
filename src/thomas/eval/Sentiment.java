package thomas.eval;

import actionsBD.MongoDB;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class Sentiment extends Jugement{

	public Sentiment(String db, String documents, String queries) {
		super(db, documents, queries, "polarite");
	}

	public boolean isRelevant(int id, int idq) {
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, documents);
		DBObject query = new BasicDBObject();
		query.put("id", id);
		DBCursor cursor = col.find(query);
		DBObject obj = cursor.next();
		return ((Integer)obj.get("score")>3);
	}

	public boolean isCorrectlyAttribued(int idr, double score) {
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, documents);
		DBObject query = new BasicDBObject("id", idr);
		DBCursor cursor = col.find(query);
		DBObject obj = cursor.next();
		return (((Integer)obj.get("score")>3) && (score>0));
	}

	@Override
	public void writeNRelevant() {
		//On verifie que ca na pas ete deja fait
		BasicDBObject verif = new BasicDBObject();
		verif.put("key", title+"NRelevant");
		if (MongoDB.mongoDB.getCollectionFromDB(db, documents+"_info").find(verif).hasNext())return;
		MongoDB.mongoDB.insertInformationAbout(db, documents, title+"NRelevant",countNRelevant(null));
	}

	@Override
	public int getNRelevant(DBObject query) {
		return (Integer) MongoDB.mongoDB.getInformationAbout(db, documents, title+"NRelevant"+(Integer)query.get("id"));
	}
}
