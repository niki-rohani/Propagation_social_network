package thomas.utils;

import actionsBD.MongoDB;
import thomas.featuresProduction.FeatureProduction;
import thomas.indexation.WholeIndexation;
import thomas.queryProduction.WholeQueryProduction;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoException;


public class Clean {

	private static void cleanFineFoods() {
		DB finefoods = MongoDB.mongoDB.getDB("finefoods");
		finefoods.cleanCursors(true);
		try{
			finefoods.dropDatabase();
		}catch(MongoException e){
			e.printStackTrace();
		}
		System.out.println("Finefood database cleaned.");
	}

	@SuppressWarnings("unused")
	private static void cleanCollection(String collection) {
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB("finefoods", collection);
		col.dropIndexes();
		try{
			col.drop();
		}catch(MongoException e){
			e.printStackTrace();
		}
		System.out.println(collection + " collection cleaned.");
	}		

	
	public static void main(String[] args){
		cleanFineFoods();
		WholeIndexation.main(args);
		WholeQueryProduction.main(args);
		FeatureProduction.main(args);
		//cleanCollection("foodReviews_1");
		//cleanCollection("queries_1");
		//cleanCollection("features_1");


	}
}

