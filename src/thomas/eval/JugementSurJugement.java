package thomas.eval;

import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class JugementSurJugement extends Jugement{
	public Jugement j1;
	public Jugement j2;

	public JugementSurJugement(String db, String documents, Jugement j1, Jugement j2){
		//super(db, documents, j1.title+"SUR"+j2.title);
		super();
		this.db=db;
		this.documents = documents;
		this.title = j1.title+"SUR"+j2.title;
		this.j1=j1;
		this.j2=j2;
		writeNRelevant();
	}

	@Override
	public boolean isRelevant(int id, int idq) {
		return (j1.isRelevant(id, idq) && j2.isRelevant(id, idq));
	}

	@Override
	public void writeNRelevant() {
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, "queries_1");
		DBCursor cursor = col.find();
		DBObject query;
		while (cursor.hasNext()){
			query = cursor.next();
			DBCollection documents = MongoDB.mongoDB.getCollectionFromDB(db, "documents_1");
			DBCursor docCursor = documents.find();
			DBObject doc;
			int id, count = 0;
			int idq = (Integer)query.get("id");
			while (cursor.hasNext()){
				doc = docCursor.next();
				id = (Integer)doc.get("id");
				if(j1.isRelevant(id, idq))count++;
			}
			MongoDB.mongoDB.insertInformationAbout(db, "documents_1", title+"NRelevantFor"+idq, count);
		}
	}		

	public int getNRelevant(DBObject query) {
		MongoDB.mongoDB.getInformationAbout(db, "documents_1", title+"NRelevantFor"+(Integer)query.get("id"));
		return 0;
	}
}