package thomas.featuresProduction;

import thomas.features.CoveredTerms;
import thomas.features.FrequencyComputer;
import thomas.features.IDF;
import thomas.features.LMIRabs;
import thomas.features.LMIRdir;
import thomas.features.LMIRjm;
import thomas.features.Okapi;
import thomas.features.SimilariteCosinus;
import thomas.features.StreamLength;
import thomas.features.TF;
import thomas.features.TFIDF;
import thomas.features.WordCount;

public class RelevanceFeatures extends FeatureList {

	public RelevanceFeatures(String db, String stems) {
		super(db, stems);
		FrequencyComputer fcomputer = new FrequencyComputer(db, stems);
		addFeaturer(new WordCount(fcomputer));
		addFeaturer(new StreamLength(fcomputer));
		addFeaturer(new CoveredTerms(fcomputer));
		addFeaturer(new TF(fcomputer));
		addFeaturer(new IDF(fcomputer));
		addFeaturer(new TFIDF(fcomputer));
		addFeaturer(new Okapi(fcomputer));
		addFeaturer(new LMIRabs(fcomputer));
		addFeaturer(new LMIRdir(fcomputer));
		addFeaturer(new LMIRjm(fcomputer));
		addFeaturer(new SimilariteCosinus(fcomputer));
	}
}
