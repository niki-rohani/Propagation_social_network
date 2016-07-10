package thomas.features;

import java.util.ArrayList;

import com.mongodb.DBObject;


public class TF extends Feature {
	private FrequencyComputer fcomputer;

	public TF(FrequencyComputer fcomputer){
		this.fcomputer = fcomputer;
	}

	public double getFeature(DBObject requete, DBObject document) {
		ArrayList<Double> feature = getFeatureList(requete, document);
		return sumFeature(feature);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Double> getFeatureList(DBObject requete, DBObject document) {
		ArrayList<Double> feature = new ArrayList<Double>();
		ArrayList<DBObject> stemsRequete = (ArrayList<DBObject>) requete.get("weights");
		ArrayList<DBObject> stemsDocument = (ArrayList<DBObject>) document.get("weights");
		boolean flag = false;
		String keyR, keyD;
		for(DBObject stemR : stemsRequete){
			keyR = stemR.keySet().toArray()[0].toString();
			for(DBObject stemD : stemsDocument){
				keyD = stemD.keySet().toArray()[0].toString();
				if(keyD.equals(keyR)){	
					feature.add(fcomputer.getTfFromTfidf((Double)stemD.get(keyD), keyD));
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

/*
	@SuppressWarnings("unchecked")
	public Double getFeatures(DBObject requete, DBObject document) {
		ArrayList<DBObject> stemsRequete = (ArrayList<DBObject>) requete.get("weights");
		ArrayList<DBObject> stemsDocument = (ArrayList<DBObject>) document.get("weights");
		Double res = 0.0;
		String keyR, keyD;
		for(DBObject stemR : stemsRequete){
			keyR = stemR.keySet().toArray()[0].toString();
			for(DBObject stemD : stemsDocument){
				keyD = stemD.keySet().toArray()[0].toString();
				if(keyD.equals(keyR)){	
					res+=fcomputer.getTfFromTfidf((Double)stemD.get(keyD), keyD);
				}
			}
		}
		return res ;
	}
*/
	public String toString(){
		return "TF";
	}

}
