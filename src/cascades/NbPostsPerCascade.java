package cascades;

import java.util.ArrayList;
import java.util.HashSet;

public class NbPostsPerCascade extends CascadeSetFeatureProducer {

	@Override
	public ArrayList<Double> getFeatures(HashSet<Cascade> cascades) {
		ArrayList<Double> ret=new ArrayList<Double>();
		int nb=0;
		for(Cascade c:cascades){
			nb+=c.posts.size();
		}
		double av=(nb*1.0)/(cascades.size()*1.0);
		ret.add(av);
		return ret;
	}

	@Override
	public String toString() {
		return "NbPostsPerCascade";
	}

}
