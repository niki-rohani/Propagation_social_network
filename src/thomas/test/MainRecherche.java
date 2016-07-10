package thomas.test;

import java.util.ArrayList;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import actionsBD.MongoDB;
import thomas.eval.Accuracy;
import thomas.eval.DCG;
import thomas.eval.F1Mesure;
import thomas.eval.Modele;
import thomas.eval.ObjectifEvaluateur;
import thomas.eval.Pertinence;
import thomas.eval.Precision;
import thomas.eval.Rappel;
import thomas.featuresProduction.FeatureList;
import thomas.featuresProduction.RelevanceFeatures;
import thomas.featuresProduction.SentimentFeatures;

public class MainRecherche {
	public static void main(String[] args){
		String db = "finefoods";
		String reviews = "documents_1";
		String queries = "queries_1";
		String stems = "stems_1";
		String features = "features_1";
		
		ArrayList<FeatureList> featurers = new ArrayList<FeatureList>();
		FeatureList f = new RelevanceFeatures(db, stems);
		featurers.add(f);
		Modele m1 = new Modele(featurers);
		m1.fonctions.get(f).setWeightsQuicklyForRelevance(f);

		
		featurers = new ArrayList<FeatureList>();
		featurers.add(f);
		featurers.add(new SentimentFeatures(db, stems));
		Modele modele = new Modele(featurers);
		modele.fonctions.get(f).setWeightsQuicklyForRelevance(f);

		
		//Selection d'une requete
		DBCollection queryCol = MongoDB.mongoDB.getCollectionFromDB(db, queries);
		DBCursor cursor = queryCol.find();
		DBObject query = cursor.next();
		
		
		//calcul des scores pour cette requete selon les objectifs
		System.out.println(m1.ordonnancement(db, features, query, m1.featurers));
		ArrayList<Integer> search = modele.ordonnancement(db, features, query, modele.featurers);
		System.out.println(search);
		
		ArrayList<ObjectifEvaluateur> mesures = new ArrayList<ObjectifEvaluateur>();
		mesures.add(new Precision(db));
		mesures.add(new Rappel(db));
		mesures.add(new F1Mesure(db));
		mesures.add(new Accuracy(db));
		mesures.add(new DCG(db));
		
		for(ObjectifEvaluateur ev : mesures){
			try {
				System.out.println(ev.computeScore(new Pertinence(db, reviews, queries), search, query));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}	
	}
}
