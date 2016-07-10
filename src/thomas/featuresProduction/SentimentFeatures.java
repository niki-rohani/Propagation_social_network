package thomas.featuresProduction;

import thomas.features.ContainsMostUsed;
import thomas.features.FrequencyComputer;


public class SentimentFeatures extends FeatureList{
	
	public SentimentFeatures(String db, String stems) {
		super(db, stems);
		addFeaturer(new ContainsMostUsed(new FrequencyComputer(db, stems))); 
	}
}
