package thomas.deprecated;
/*
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import thomas.actionsBD.MongoDB;
import thomas.core.IdComparatorForScore;
import thomas.featurers.QueryFeaturer;
import thomas.scoring.ModeleScoring;
*/
public class RechercheMultiCritere {/*
	String db;
	public ArrayList<Objectif> criteres;
	private ModeleScoring fonction;
	private int nbResults;


	public RechercheMultiCritere(String db, ModeleScoring fonction, int nbResults){
		this.db=db;
		this.fonction=fonction;
		criteres = new ArrayList<Objectif>();
		this.nbResults=nbResults;
	}

	public void addCritere(Objectif o){
		criteres.add(o);
		fonction.parametres.add(1.0);
	}

	public int getCriteresLength(){
		return criteres.size();
	}

	public double computeScore(ArrayList<Double> scores) throws Exception{
		return fonction.computeScore(scores);
	}

	public void writeFeatures(){
		for(Objectif o:criteres){
			try {
				o.generateFeatures();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	//pas utile
	public ArrayList<ArrayList<Integer>> searchIDs(DBObject query){
		ArrayList<ArrayList<Integer>> ids = new ArrayList<ArrayList<Integer>>();
		for(Objectif o:criteres){
			ids.add(o.searchIds(db, query, nbResults));
		}
		return ids;
	}



	public ArrayList<Integer> multiSearch(DBObject query){
		//System.out.println("Ordonnancement multi-critere selon les objectifs " + criteres.toString());
		HashMap<Integer, Double> idScore = new HashMap<Integer, Double>(); 
		int i ,idq = (Integer) query.get("id");
		ArrayList<Double> objScores = new ArrayList<Double>();
		long n = MongoDB.mongoDB.getCollectionFromDB(db, "documents_1").count();
		BasicDBObject obj = new BasicDBObject();
		obj.put("idq", idq);
		DBCursor cursor;
		DBCollection featuresCol;
		DBObject res;

		for (i = 1 ; i<=n ;i++){
			obj.put("idr", i);
			for(Objectif o:criteres){
				featuresCol = MongoDB.mongoDB.getCollectionFromDB(db, "features_"+o.toString()+"_1");
				cursor = featuresCol.find(obj);
				try {
					if(cursor.hasNext()) {
						res = cursor.next();		//cas du couple requete/doc
					}else{
						cursor = featuresCol.find(new BasicDBObject("idr", i));				
						res = cursor.next();
					}

					ArrayList<Double> features = new ArrayList<Double>();
					for(QueryFeaturer f:o.featurer.featurers){
						features.add((Double) res.get(f.toString()));
					}
					objScores.add(o.fonction.computeScore(features));
					features.clear();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			try {
				idScore.put(i, fonction.computeScore(objScores));
				objScores.clear();
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
		i=0;
		while( i< nbResults){
			sousListe.add(idTrie.get(i));
			i++;
		}
		return sousListe;
		//return (ArrayList<Integer>) idTrie.subList(0, nbResults);	
	}
*/}
