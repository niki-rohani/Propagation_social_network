package thomas.features;

import java.util.ArrayList;

import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class WordCount extends Feature {

	private FrequencyComputer fComputer;

	public WordCount(FrequencyComputer fComputer){
		this.fComputer = fComputer;
	}

	public double getFeature(DBObject requete, DBObject document) {
		ArrayList<Double> feature = getFeatureList(requete, document);
		return sumFeature(feature);
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Double> getFeatureList(DBObject requete, DBObject document) {

		ArrayList<Double> feature = new ArrayList<Double>();
		ArrayList<DBObject> stemsRequete = (ArrayList<DBObject>) requete.get("weights");
		ArrayList<DBObject> stemsDocument = (ArrayList<DBObject>) document.get("weights");
		boolean flag = false;
		String keyR, keyD;
		
		for(DBObject stemR : stemsRequete){
			keyR = stemR.keySet().toArray()[0].toString();
			for(DBObject stemD : stemsDocument){
				keyD = stemD.keySet().toArray()[0].toString();
				if(keyD.equals(keyR)){	
					feature.add(getfComputer().getFrequencyFromTfidf((Double)stemD.get(keyD), keyD));
					flag = true;
					if(flag){;break;}
				}
				if(!flag){
					feature.add(0.0);
				}else{
					flag = false;
				}
			}
		}
		return feature;
	}

	public FrequencyComputer getfComputer() {
		return fComputer;
	}

	public String toString(){
		return "WordCount";
	}



	public static void main(String[] args){
		String db="finefoods";
		String reviews = "foodReviews_1";
		String queries = "queries_1";
		String stems = "stems_1";

		DBCollection queriesCol = MongoDB.mongoDB.getCollectionFromDB(db, queries);
		DBCollection reviewsCol = MongoDB.mongoDB.getCollectionFromDB(db, reviews);
		DBCursor queriesCursor = queriesCol.find();
		DBCursor reviewsCursor = reviewsCol.find();

		FrequencyComputer fcomputer = new FrequencyComputer(db, stems);
		new WordCount(fcomputer).getFeature(queriesCursor.next(), reviewsCursor.next());
	}

}
