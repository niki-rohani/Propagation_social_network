package thomas.core;

import java.util.Comparator;
import java.util.HashMap;

public class IdComparatorForScore implements Comparator<Integer> {
	private HashMap<Integer,Double> idScore;

	public IdComparatorForScore(HashMap<Integer, Double> idScore) {
		this.idScore=idScore;
	}
	
	@Override
	public int compare(Integer arg0, Integer arg1) {
		double s0 = idScore.get(arg0);
		double s1 = idScore.get(arg1);
		return (int) (s1-s0);
	}
	
}
