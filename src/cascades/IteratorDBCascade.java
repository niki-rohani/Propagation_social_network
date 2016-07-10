package cascades;

import java.awt.Cursor;
import java.util.Iterator;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class IteratorDBCascade implements Iterator<Cascade> {

	DBCollection col ;
	DBCursor cursor ;
	
	String db ;
	String collection;
	
	public IteratorDBCascade(String db,String collection) {
		this.db=db ;
		this.collection=collection;
		col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		cursor = col.find(new BasicDBObject());
	}
	
	
	@Override
	public boolean hasNext() {
		return cursor.hasNext() ;
	}

	@Override
	public Cascade next() {
		DBObject res=cursor.next();
		return Cascade.getCascadeFrom(res);
	}

	@Override
	public void remove() {
		cursor.remove() ;

	}
	
	public void reset() {
		col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		cursor = col.find(new BasicDBObject()); 
	}

}
