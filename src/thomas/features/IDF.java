package thomas.features;

import java.util.ArrayList;

import com.mongodb.DBObject;

public class IDF extends Feature{
	private FrequencyComputer fcomputer;

	public IDF(FrequencyComputer fcomputer){
		this.fcomputer = fcomputer;
	}


	@SuppressWarnings("unchecked")
	public ArrayList<Double> getFeatureList(DBObject requete, DBObject document) {
		ArrayList<Double> feature = new ArrayList<Double>();
		ArrayList<DBObject> stemsRequete = (ArrayList<DBObject>) requete.get("weights");
		String keyR;
		for(DBObject stemR : stemsRequete){
			keyR = stemR.keySet().toArray()[0].toString();
			feature.add(fcomputer.getIdf(keyR));
		}
		return feature;
	}

	public String toString(){
		return "IDF";
	}
}
