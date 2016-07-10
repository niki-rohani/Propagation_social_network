package thomas.scoring;

import java.util.ArrayList;

public class WeightSum extends ModeleScoring {

	public WeightSum(int n) {
		super(n);
	}


	public WeightSum(int size, int init) {
		super(size, init);
	}


	@Override
	public double computeScore(ArrayList<Double> rep) throws Exception {
		if (rep.size()!=parametres.size()){
			throw new Exception("Dimensions differentes : param = "+parametres.size() + " et data = " + rep.size());
		}
		
		double res = 0.0;
		
		for(int i=0;i<rep.size();i++){
			res+=(parametres.get(i)*rep.get(i));
		}
		return res;
		
	}

}
