package thomas.eval;

import java.util.ArrayList;

import com.mongodb.DBObject;

public class F1Mesure extends ObjectifEvaluateur {
	Precision P;
	Rappel R;
	
	public F1Mesure(String db){
		this.db  = db;
		this.P = new Precision(db);
		this.R = new Rappel(db);
	}

	public double computeScore(Jugement j, ArrayList<Integer> ids, DBObject query) throws Exception {
		double p = P.computeScore(j, ids, query);
		double r = R.computeScore(j, ids, query);
		if(p+r==0)return 0;
		return (2*p*r)/(p+r);
	}
}
