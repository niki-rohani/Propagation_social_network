package cascades;

import java.util.ArrayList;


public abstract class CascadeFeatureProducer {
	public abstract ArrayList<Double> getFeatures(Cascade cascade);
	public int getNbFeatures(){
		return(1);
	}
	public abstract String toString();
}
