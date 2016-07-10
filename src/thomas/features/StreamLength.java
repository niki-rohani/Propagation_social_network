package thomas.features;

import java.util.ArrayList;

import actionsBD.MongoDB;


import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class StreamLength extends Feature{
	private FrequencyComputer fComputer;

	public StreamLength(FrequencyComputer fcomputer) {
		this.fComputer = fcomputer;
	}

	@Override
	public ArrayList<Double> getFeatureList(DBObject requete, DBObject document) {
		ArrayList<Double> feature= new ArrayList<Double>();
		double length = 0.0;
		length+=fComputer.getLength(requete);
		length+=fComputer.getLength(document);
		feature.add(length);
		return feature;
	}

	public String toString(){
		return "StreamLength";
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
		new StreamLength(fcomputer).getFeature(queriesCursor.next(), reviewsCursor.next());
	}
}

