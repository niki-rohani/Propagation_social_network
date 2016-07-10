package thomas.core;

import java.util.Comparator;
import java.util.HashMap;

public class StemComparatorForIds implements Comparator<Integer> {
	private HashMap<Integer,Integer> stems;

	public StemComparatorForIds(HashMap<Integer, Integer> stems) {
		this.stems=stems;
	}
	
	
	@Override
	public int compare(Integer arg0, Integer arg1) {
		int nocc0 = stems.get(arg0);
		int nocc1 = stems.get(arg1);
		return nocc1-nocc0;
	}

}
