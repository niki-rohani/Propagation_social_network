package thomas.featuresProduction;

import java.util.ArrayList;

import thomas.features.Feature;

public abstract class FeatureList {

	public ArrayList<Feature> featurers;
	public String db;
	public String stems;

	public FeatureList(String db, String stems) {
		this.db = db;
		this.stems = stems;
		this.featurers = new ArrayList<Feature>();
	}

	public void addFeaturer(Feature qf) {
		featurers.add(qf);
	}

	public int getFeatureLength() {
		return featurers.size();
	}
}
