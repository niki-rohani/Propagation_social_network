package thomas.features;

import java.util.ArrayList;

import com.mongodb.DBObject;

public class CoveredTerms extends Feature {
	
	private FrequencyComputer fComputer;

	public CoveredTerms(FrequencyComputer fcomputer) {
		this.fComputer = fcomputer;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<Double> getFeatureList(DBObject requete, DBObject document) {
		ArrayList<Double> feature = new ArrayList<Double>();
		ArrayList<DBObject> stemsRequete = (ArrayList<DBObject>) requete.get("weights");
		ArrayList<DBObject> stemsDocument = (ArrayList<DBObject>) document.get("weights");
		double intersection = 0.0;
		boolean flag = false;
		String keyR, keyD;
		int length=0;
		
		for(DBObject stemR : stemsRequete){
			keyR = stemR.keySet().toArray()[0].toString();
			length+=fComputer.getFrequencyFromTfidf((Double)stemR.get(keyR), keyR);
			for(DBObject stemD : stemsDocument){
				keyD = stemD.keySet().toArray()[0].toString();
				if(keyD.equals(keyR)){	
					intersection++;
					flag = true;
				}
				if(flag){;break;}
			}
		}
		feature.add((intersection/length));
		return feature;
	}

	public String toString(){
		return "CoveredTerms";
	}


}
