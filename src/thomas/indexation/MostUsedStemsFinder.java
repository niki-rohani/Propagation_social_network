package thomas.indexation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import actionsBD.MongoDB;
import thomas.core.StemComparatorForIds;
import thomas.core.StemComparatorForStems;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MostUsedStemsFinder {

	public static void findMostUsedStemsForStems(int nb, String db, String stemsCollection){
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, stemsCollection);
		DBCursor cursor = col.find();
		HashMap<String, Integer> stems= new HashMap<String, Integer>();
		DBObject query;
		String stem;
		int nocc;

		System.out.println("Recherche dans " + stemsCollection + " des " + nb + " stems les plus utilises");

		while (cursor.hasNext()){
			query = cursor.next();
			try{
				stem = (String)query.get("stem");
				nocc = (Integer)query.get("nOcc");
				stems.put(stem, nocc);
			}catch(Exception e){
				System.out.println("Ligne ne correspondant pas a un stem");
			}
		}
		ArrayList<String> cles = new ArrayList<String>(stems.keySet());
		Collections.sort(cles, new StemComparatorForStems(stems));

		int i = 1;
		ArrayList<BasicDBObject> presence=new ArrayList<BasicDBObject>(); 
		BasicDBObject obj=new BasicDBObject();

		while( (i<=nb) && (i<cles.size())){
			obj=new BasicDBObject();
			obj.put(i+"", cles.get(i));
			presence.add(obj);
			i++;
		}
		MongoDB.mongoDB.insertInformationAbout(db, stemsCollection, "mostUsed", presence);

		/*
		int i = 0;
		String collection = MongoDB.mongoDB.createCollection(db, nb+"mostUsed","");
		DBCollection dbCol = MongoDB.mongoDB.getCollectionFromDB(db, collection);
		DBObject obj= new BasicDBObject();

		while( (i<nb) && (i<cles.size())){
			obj.put("stem", cles.get(i));
			dbCol.insert(obj);
			i++;
		}
		 */

	}

	public static void findMostUsedStemsForIds(int nb, String db, String stemsCollection){
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, stemsCollection);
		DBCursor cursor = col.find();
		HashMap<Integer, Integer> stems= new HashMap<Integer, Integer>();
		DBObject query;
		Integer stem;
		int nocc;

		System.out.println("Recherche dans " + stemsCollection + " des " + nb + " stems les plus utilises");

		while (cursor.hasNext()){
			query = cursor.next();
			try{
				stem = (Integer)query.get("id");
				nocc = (Integer)query.get("nOcc");
				stems.put(stem, nocc);
			}catch(Exception e){
				System.out.println("Ligne ne correspondant pas a un stem");
			}
		}
		ArrayList<Integer> cles = new ArrayList<Integer>(stems.keySet());
		Collections.sort(cles, new StemComparatorForIds(stems));

		int i = 1;
		ArrayList<BasicDBObject> presence=new ArrayList<BasicDBObject>(); 
		BasicDBObject obj=new BasicDBObject();

		while( (i<=nb) && (i<cles.size())){
			obj=new BasicDBObject();
			obj.put(i+"", cles.get(i));
			presence.add(obj);
			i++;
		}
		MongoDB.mongoDB.insertInformationAbout(db, stemsCollection, "mostUsed", presence);

	}
}
