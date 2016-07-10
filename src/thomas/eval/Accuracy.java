package thomas.eval;

import java.util.ArrayList;

import actionsBD.MongoDB;

import com.mongodb.DBObject;

public class Accuracy extends ObjectifEvaluateur{

	public Accuracy(String db){
		this.db  = db;
	}

	
	//Aucun sens car accuracy est utile que pour biclasse
	@Override
	public double computeScore(Jugement j, ArrayList<Integer> ids, DBObject query) throws Exception {
		double good=0.0;
		int idq = (Integer) query.get("id");
		for(int id:ids){
			if (j.isRelevant(id, idq))good++;
		}
		return good/(MongoDB.mongoDB.getCollectionFromDB(db, j.documents).count()*1.0);
	}
}
