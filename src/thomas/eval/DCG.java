package thomas.eval;

import java.util.ArrayList;

import com.mongodb.DBObject;

public class DCG extends ObjectifEvaluateur {
	
	
	public DCG(String db){
		this.db  = db;
	}
	
	@Override
	public double computeScore(Jugement j, ArrayList<Integer> ids, DBObject query) throws Exception {
		double res = 0.0;
		int idq  = (Integer)query.get("id");
		int presence;
		
		for(int i = 0;  i< ids.size(); i++){
			if (j.isRelevant(ids.get(i), idq)){
				presence=1;
			}else{
				presence = 0;
			}
				res+= (Math.pow(2, presence)-1)/(Math.log(2+i));
			}
		
		return res;
	}

}
