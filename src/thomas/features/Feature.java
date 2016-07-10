package thomas.features;

import java.util.ArrayList;

import com.mongodb.DBObject;

public  abstract class Feature{
	
	public abstract ArrayList<Double> getFeatureList(DBObject requete, DBObject document);
	
	public abstract String toString();
	
	public double sumFeature(ArrayList<Double> feature){
		double res=0;
		for(Double elem:feature){
			res+=elem;
		}
		return res;
	}
	
	public double getFeature(DBObject requete, DBObject document) {
		ArrayList<Double> feature = getFeatureList(requete, document);
		return sumFeature(feature);
	}
}