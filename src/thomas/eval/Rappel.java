package thomas.eval;

import java.util.ArrayList;
import com.mongodb.DBObject;

public class Rappel extends ObjectifEvaluateur {

	public Rappel(String db){
		this.db  = db;
	}

	public double computeScore(Jugement j, ArrayList<Integer> ids, DBObject query) throws Exception {
		double good=0.0;
		int npert =  j.getNRelevant(query);
		int idq = (Integer) query.get("id");
		for(int id:ids){
			if (j.isRelevant(id, idq))good++;
		}
		if(npert==0)return 0;
		return good/npert;
	}
}
