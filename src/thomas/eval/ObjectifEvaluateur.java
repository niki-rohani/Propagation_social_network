package thomas.eval;

import java.util.ArrayList;
import com.mongodb.DBObject;

public abstract class ObjectifEvaluateur {
	protected String db;
	/*
	public double getScore(Objectif o) throws Exception{
		return computeScore(o ,o.repBase+"_1");
	}
	/*
	public double getScore(Objectif o, DBObject query) throws Exception{
		int idq = (Integer)query.get("idq");
		if (idq==0){
		return computeScore(o ,o.repBase+"_1");
		}else{
			DBObject match = new BasicDBObject("$match", new BasicDBObject("idq", idq));
			DBCollection featuresCol = MongoDB.mongoDB.getCollectionFromDB(db, o.repBase+"_1");
			AggregationOutput output = featuresCol.aggregate(match);
			String queryFeature = MongoDB.mongoDB.createCollection(db, "queryFeature", "tempCol");
			DBCollection queryCol = MongoDB.mongoDB.getCollectionFromDB(db, queryFeature);

			for(DBObject obj : output.results()){
				queryCol.insert(obj);
			}
			double res =  computeScore(o, queryFeature, query);
			queryCol.drop();
			return res;
		}
		
	}
	*/
	//protected abstract double computeScore(Objectif o, String  collection) throws Exception;       //calcule un score selon un objectif sur toute la base
	public abstract double computeScore(Jugement j, ArrayList<Integer> ids, DBObject query) throws Exception;		//calcule un score sur une liste de resultats
}
