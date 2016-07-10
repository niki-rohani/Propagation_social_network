package thomas.test;

import java.util.ArrayList;

import actionsBD.MongoDB;
import thomas.courbes.Courbe;
import thomas.courbes.CourbePertinence;
import thomas.courbes.RappelPrecision;
import thomas.eval.Modele;
import thomas.featuresProduction.FeatureList;
import thomas.featuresProduction.RelevanceFeatures;
import thomas.featuresProduction.SentimentFeatures;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MainCourbe {

	@SuppressWarnings("unused")
	public static void main(String[] args){
		
		String db = "finefoods";
		String reviews = "documents_1";
		String queries = "queries_1";
		String stems = "stems_1";
		String features = "features_1";
		
		
		BasicDBObject obj = new BasicDBObject();
		obj.put("id", 534);
		obj = (BasicDBObject) MongoDB.mongoDB.getCollectionFromDB(db, queries).find(obj).next();
		
		ArrayList<DBObject> requetes = new ArrayList<DBObject>();
		//requetes.add(obj);
		DBCursor cursor= MongoDB.mongoDB.getCollectionFromDB(db, queries).find();
		while (cursor.hasNext()){
			requetes.add(cursor.next());
		}
		
		FeatureList fr = new RelevanceFeatures(db, stems);
		FeatureList fs = new SentimentFeatures(db, stems);
		ArrayList<FeatureList> featurers1 = new ArrayList<FeatureList>();

		CourbePertinence c = new CourbePertinence(db, reviews, queries, features, requetes, 100);
		
		featurers1.add(fr);
		Modele m1 = (new Modele(featurers1));
		m1.fonctions.get(fr).setWeightsQuicklyForRelevance(fr);
		c.addModele(m1);

		
		ArrayList<FeatureList> featurers2 = new ArrayList<FeatureList>();
		featurers2.add(fs);
		c.addModele(new Modele(featurers2));
		
		featurers1 = new ArrayList<FeatureList>();
		featurers1.add(fr);
		featurers1.add(fs);
		Modele m = new Modele(featurers1);
		c.addModele(m);
		
		c.draw();
		
		System.out.println(obj.get("id"));
		Courbe c2 = new RappelPrecision(db, reviews, queries, features, m, obj, 500);
		//c2.draw();
	}
}
