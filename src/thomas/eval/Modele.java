package thomas.eval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import actionsBD.MongoDB;
import thomas.core.IdComparatorForScore;
import thomas.features.Feature;
import thomas.featuresProduction.FeatureList;
import thomas.scoring.ModeleScoring;
import thomas.scoring.WeightSum;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class Modele {

	public ArrayList<FeatureList> featurers;
	public HashMap<FeatureList, ModeleScoring> fonctions;
	public ModeleScoring fonctionMultiObj;

	public Modele(ArrayList<FeatureList> featurers){
		this.featurers = featurers;
		this.fonctions = new HashMap<FeatureList, ModeleScoring>();
		for(FeatureList f : featurers){
			fonctions.put(f, new WeightSum(f.getFeatureLength(), 1));
		}
		this.fonctionMultiObj = new WeightSum(featurers.size(), 1);
	}

	public Double computeScore(ArrayList<Double> scores, FeatureList f) throws Exception {
		return fonctions.get(f).computeScore(scores);
	}

	public  ArrayList<Integer> ordonnancement(String db, String features, DBObject query, FeatureList featurer){

		DBCollection featuresCol = MongoDB.mongoDB.getCollectionFromDB(db, features);
		HashMap<Integer, Double> idScore = new HashMap<Integer, Double>(); 
		int idr,idq = (Integer) query.get("id");

		DBObject match = new BasicDBObject("$match", new BasicDBObject("idq", idq));
		AggregationOutput output = featuresCol.aggregate(match);
		ArrayList<Double> feature = new ArrayList<Double>();

		for(DBObject obj : output.results()){
			idr = (Integer)obj.get("idr");
			for(Feature f:featurer.featurers){
				feature.add((Double) obj.get(f.toString()));
			}
			try {
				idScore.put(idr, fonctions.get(featurers.indexOf(featurer)).computeScore(feature));
				feature.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//Ici on a une Hashmap qui contient tous les id et les scores associes
		//tri
		ArrayList<Integer> idTrie = new ArrayList<Integer>(idScore.keySet());
		Collections.sort(idTrie, new IdComparatorForScore(idScore));

		return idTrie;
	}

	public  ArrayList<Integer> ordonnancement(String db, String features, DBObject query, ArrayList<FeatureList> featurers){
		System.out.println("Recherche");
		DBCollection featuresCol = MongoDB.mongoDB.getCollectionFromDB(db, features);
		HashMap<Integer, Double> idScore = new HashMap<Integer, Double>(); 
		int idr ,idq = (Integer) query.get("id");

		ArrayList<Double> objScores = new ArrayList<Double>();
		ArrayList<Double> feature = new ArrayList<Double>();

		DBObject match = new BasicDBObject("$match", new BasicDBObject("idq", idq));
		AggregationOutput output = featuresCol.aggregate(match);
			
		for(DBObject obj : output.results()){
			idr = (Integer)obj.get("idr");
			for(FeatureList f:featurers){
				for(Feature qf:f.featurers){
					feature.add((Double) obj.get(qf.toString()));
				}
				try {
					objScores.add(computeScore(feature, f));
				} catch (Exception e) {
					e.printStackTrace();
				}
				feature.clear();
			}
			try {
				idScore.put(idr, fonctionMultiObj.computeScore(objScores));
				objScores.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//Ici on a une Hashmap qui contient tous les id et les scores associes
		//tri
		//System.out.println(idScore);


		ArrayList<Integer> idTrie = new ArrayList<Integer>(idScore.keySet());
		Collections.sort(idTrie, new IdComparatorForScore(idScore));

		return idTrie;
	}
}
