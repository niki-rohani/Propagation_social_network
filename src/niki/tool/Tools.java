package niki.tool;

import java.util.HashMap;

public class Tools {

	
	public static double dotProduct (HashMap<Long, Double> h1, HashMap<Long, Double> h2) {
		double nb = 0;
		for (Long i: h1.keySet()) {
			if (h2.containsKey(i))
				nb = nb + (h1.get(i) * h2.get(i));
		}
		return nb;
	}
	
	public static double norm (HashMap <Long, Double> h1) {
		double nb = 0;
		for (Long i: h1.keySet()){
			nb = nb + Math.pow(h1.get(i), 2);
		}
		nb = Math.sqrt(nb);
		return nb;
	}
}
