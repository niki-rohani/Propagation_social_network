package thomas.features;

import java.util.ArrayList;

import thomas.scoring.ModeleScoring;
import thomas.scoring.WeightSum;

import com.mongodb.DBObject;


public class ContainsMostUsed extends Feature {
	private ModeleScoring fonction;
	private FrequencyComputer fcomputer;
	public ContainsMostUsed(FrequencyComputer fcomputer){
		this.fcomputer=fcomputer;
	}
	
	//	Feature pour caracteriser la requete seule
	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<Double> getFeatureList(DBObject requete, DBObject document) {
		ArrayList<Double> feature = new ArrayList<Double>();
		ArrayList<DBObject> stemsDocument = (ArrayList<DBObject>) document.get("weights");
		ArrayList<DBObject> mostUsed = (ArrayList<DBObject>) fcomputer.getMostUsed();
		boolean flag = false;
		String keyM,keyD;
		
		for(DBObject stemM : mostUsed){
			keyM = stemM.keySet().toArray()[0].toString();
			for(DBObject stemD : stemsDocument){
				keyD = stemD.keySet().toArray()[0].toString();
				if(keyM.equals(keyD)){	
					feature.add(fcomputer.getFrequencyFromTfidf((Double) stemD.get(keyD), keyD));
					flag = true;
				}
				if(flag){;break;}
			}
			if(!flag){
				feature.add(0.0);
			}else{
				flag = false;
			}
		}
		return feature;
	}
	
	@Override
	public double getFeature(DBObject requete, DBObject document) {
		ArrayList<Double> feature = getFeatureList(requete, document);
		fonction = new WeightSum(feature.size(), 0);
		fonction.setWeightsQuicklyForPolarity("finefoods");
		try {
			return fonction.computeScore(feature);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public String toString(){
		return "ContainsMostUsed";
	}

}
