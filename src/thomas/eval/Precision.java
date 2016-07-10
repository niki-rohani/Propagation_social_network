package thomas.eval;

import java.util.ArrayList;
import com.mongodb.DBObject;

public class Precision extends ObjectifEvaluateur {

	public Precision(String db){
		this.db  = db;
	}
	
	public double computeScore(Jugement j, ArrayList<Integer> ids, DBObject query) throws Exception {
		double good=0.0;
		int idq = (Integer) query.get("id");
		for(int id:ids){
			if (j.isRelevant(id, idq))good++;
		}
		return good/ids.size();
	}
}
