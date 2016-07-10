package cascades;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class CascadeSetFeatureProducer {
	public abstract ArrayList<Double> getFeatures(HashSet<Cascade> cascades);
	public int getNbFeatures(){
		return(1);
	}
	public abstract String toString();
}
