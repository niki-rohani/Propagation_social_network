package thomas.deprecated;
/*
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;



import thomas.actionsBD.MongoDB;
import thomas.core.IdComparatorForScore;
import thomas.featurers.QueryFeaturer;
import thomas.featuresProduction.Featurer;
import thomas.scoring.ModeleScoring;
import thomas.scoring.WeightSum;
*/
public abstract class Objectif/* implements Jugement*/{/*
	String title;
	protected String repBase;
	public Featurer featurer;
	public ModeleScoring fonction;
	public String db = "finefoods";

	public Objectif(String title, Featurer featurer){
		this.title=title;
		this.repBase="features_"+title;
		this.featurer=featurer;
		setModele(new WeightSum(featurer.getFeatureLength()));
		writeNRelevant();
	}

	public void writeNRelevant() {
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, "queries_1");
		DBCursor cursor = col.find();
		DBObject obj;

		//On verifie que ca na pas ete deja fait
		obj = cursor.next();
		BasicDBObject verif = new BasicDBObject();
		verif.put("key", title+"NRelevantFor"+(obj.get("id")));
		if (MongoDB.mongoDB.getCollectionFromDB(db, "documents_1_informations_1").find(verif).hasNext())return;

		countNRelevant(obj);

		while (cursor.hasNext()){
			obj = cursor.next();
			countNRelevant(obj);
		}
	}

	private void countNRelevant(DBObject query) {
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, "documents_1");
		DBCursor cursor = col.find();
		DBObject obj;
		int id, count = 0;
		int idq = (Integer)query.get("id");
		while (cursor.hasNext()){
			obj = cursor.next();
			id = (Integer)obj.get("id");
			if(isRelevant(id, idq))count++;
		}
		MongoDB.mongoDB.insertInformationAbout(db, "documents_1", title+"NRelevantFor"+idq, count);
	}


	public void setModele(WeightSum weightSum) {
		this.fonction=weightSum;		
	}

	public double computeScore(ArrayList<Double> scores) throws Exception{
		return fonction.computeScore(scores);
	}

	public void generateFeatures() throws Exception{
		featurer.computeFeatures(repBase);
	}

	public String toString(){
		return title;
	}


	public ArrayList<Integer> searchIds(String db, DBObject query, int nbResults){
		if (featurer.isSingleFeaturer()){
			return searchIdsForSingle(db, nbResults);
		}else{
			return searchIdsForCouple(db, query, nbResults);
		}
	}

	private ArrayList<Integer> searchIdsForSingle(String db, int nbResults) {
		DBCollection featuresCol = MongoDB.mongoDB.getCollectionFromDB(db, repBase+"_1");
		HashMap<Integer, Double> idScore = new HashMap<Integer, Double>(); 
		//System.out.println("Ordonnancement de l'objectif " + this.toString());
		DBCursor cursor = featuresCol.find();
		DBObject obj;
		int idr;
		ArrayList<Double> feature = new ArrayList<Double>();
		while (cursor.hasNext()){
			obj = cursor.next();
			idr = (Integer)obj.get("idr");
			for(QueryFeaturer f:featurer.featurers){
				feature.add((Double) obj.get(f.toString()));
			}
			try {
				idScore.put(idr, fonction.computeScore(feature));
				feature.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//Ici on a une Hashmap qui contient tous les id et les scores associes
		//tri

		ArrayList<Integer> idTrie = new ArrayList<Integer>(idScore.keySet());
		Collections.sort(idTrie, new IdComparatorForScore(idScore));
		if(nbResults>idTrie.size())return idTrie;
		ArrayList<Integer> sousListe = new ArrayList<Integer>();
		int i=0;
		while( i< nbResults){
			sousListe.add(idTrie.get(i));
			i++;
		}
		return sousListe;
		//return (ArrayList<Integer>) idTrie.subList(0, nbResults);	
	}



	private ArrayList<Integer> searchIdsForCouple(String db, DBObject query, int nbResults){

		DBCollection featuresCol = MongoDB.mongoDB.getCollectionFromDB(db, repBase+"_1");
		HashMap<Integer, Double> idScore = new HashMap<Integer, Double>(); 
		int idr,idq = (Integer) query.get("id");
		//System.out.println("Ordonnancement de l'objectif " + this.toString() + " pour la requete " + idq);
		DBObject match = new BasicDBObject("$match", new BasicDBObject("idq", idq));
		AggregationOutput output = featuresCol.aggregate(match);

		ArrayList<Double> feature = new ArrayList<Double>();
		for(DBObject obj : output.results()){
			idr = (Integer)obj.get("idr");
			for(QueryFeaturer f:featurer.featurers){
				feature.add((Double) obj.get(f.toString()));
			}
			try {
				idScore.put(idr, fonction.computeScore(feature));
				feature.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//Ici on a une Hashmap qui contient tous les id et les scores associes
		//tri
		ArrayList<Integer> idTrie = new ArrayList<Integer>(idScore.keySet());
		Collections.sort(idTrie, new IdComparatorForScore(idScore));
		if(nbResults>idTrie.size())return idTrie;
		ArrayList<Integer> sousListe = new ArrayList<Integer>();
		int i=0;
		while( i< nbResults){
			sousListe.add(idTrie.get(i));
			i++;
		}
		return sousListe;
		//return (ArrayList<Integer>) idTrie.subList(0, nbResults);
	}

	public int getNRelevant(DBObject query) {
		return (Integer) MongoDB.mongoDB.getInformationAbout(db, "documents_1", title+"NRelevantFor"+(Integer)query.get("id"));

	}*/
}
