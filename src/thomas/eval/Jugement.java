package thomas.eval;

import actionsBD.MongoDB;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public abstract class Jugement {
	public String db, documents, queries;
	public String title;

	public Jugement(String db, String documents, String queries, String  title) {
		this.db = db;
		this.documents = documents;
		this.queries = queries;
		this.title = title;
		writeNRelevant();
	}
	
	public Jugement() {
		this.db= null;
		this.documents=null;
		this.title = null;
	}

	public abstract boolean isRelevant(int id, int idq);

	protected int countNRelevant(DBObject query) {
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, documents);
		DBCursor cursor = col.find();
		DBObject obj;
		int id, idq, count = 0;
		if(query!=null){
			idq = (Integer)query.get("id");
		}else{
			idq = -1;
		}
		while (cursor.hasNext()){
			obj = cursor.next();
			id = (Integer)obj.get("id");
			if(isRelevant(id, idq))count++;
		}
		return count;
	}

	public void writeNRelevant() {
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, queries);
		DBCursor cursor = col.find();
		DBObject obj;
		int idq, count;

		//On verifie que ca na pas ete deja fait
		obj = cursor.next();
		BasicDBObject verif = new BasicDBObject();
		verif.put("key", title+"NRelevantFor"+(obj.get("id")));
		if (MongoDB.mongoDB.getCollectionFromDB(db, documents+"_info").find(verif).hasNext())return;
		idq = (Integer)obj.get("id");
		count = countNRelevant(obj);
		MongoDB.mongoDB.insertInformationAbout(db, documents, title+"NRelevantFor"+idq, count);

		while (cursor.hasNext()){
			obj = cursor.next();
			count = countNRelevant(obj);
			idq = (Integer)obj.get("id");
			MongoDB.mongoDB.insertInformationAbout(db, documents, title+"NRelevantFor"+idq, count);
		}
	}
	
	public int getNRelevant(DBObject query) {
		return (Integer) MongoDB.mongoDB.getInformationAbout(db, documents, title+"NRelevantFor"+(Integer)query.get("id"));
	}
}
