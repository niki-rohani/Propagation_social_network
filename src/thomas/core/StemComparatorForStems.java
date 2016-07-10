package thomas.core;

import java.util.Comparator;
import java.util.HashMap;

public class StemComparatorForStems implements Comparator<String> {
	private HashMap<String,Integer> stems;

	public StemComparatorForStems(HashMap<String, Integer> stems) {
		this.stems=stems;
	}

	@Override
	public int compare(String stem1, String stem2) {
		int nocc1 = stems.get(stem1);
		int nocc2 = stems.get(stem2);
		return nocc2-nocc1;
	}


}
