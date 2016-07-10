package thomas.deprecated;
/*
import thomas.featurers.CoveredTerms;
import thomas.featurers.FrequencyComputer;
import thomas.featurers.IDF;
import thomas.featurers.LMIRabs;
import thomas.featurers.LMIRdir;
import thomas.featurers.LMIRjm;
import thomas.featurers.Okapi;
import thomas.featurers.SimilariteCosinus;
import thomas.featurers.StreamLength;
import thomas.featurers.TF;
import thomas.featurers.TFIDF;
import thomas.featurers.WordCount;
import com.mongodb.MongoException;
*/
public class WholeFeatureProductionTest{
/*
	private static void finefoodComputeRelevanceFeatures() {
		String db="finefoods";
		String reviews = "foodReviews_1";
		String queries = "queries_1";
		String stems = "stems_1";
		FrequencyComputer fcomputer = new FrequencyComputer(db, stems);
		Featurer producer = new CoupleFeaturer(db, reviews, queries, stems);
		producer.addFeaturer(new WordCount(fcomputer));
		producer.addFeaturer(new StreamLength(fcomputer));
		producer.addFeaturer(new CoveredTerms(fcomputer));
		producer.addFeaturer(new TF(fcomputer));
		producer.addFeaturer(new IDF(fcomputer));
		producer.addFeaturer(new TFIDF(fcomputer));
		producer.addFeaturer(new Okapi(1, 1, fcomputer));
		producer.addFeaturer(new LMIRabs(1, fcomputer));
		producer.addFeaturer(new LMIRdir(1, fcomputer));
		producer.addFeaturer(new LMIRjm(1, fcomputer));
		producer.addFeaturer(new SimilariteCosinus(fcomputer));
		try {
			producer.computeFeatures("features");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	//TODO
	@SuppressWarnings("unused")
	private static void finefoodComputeSentimentfeatures() {
		String db="finefoods";
		String queries = "queries_1";
	}

	public static void main(String[] args){
		try{
			finefoodComputeRelevanceFeatures();
			//finefoodComputeSentimentfeatures();
		}catch(MongoException e){
			e.printStackTrace();
		}
	}
*/
}
