package thomas.features;


import java.util.ArrayList;

import com.mongodb.DBObject;

public class LMIRdir extends Feature {


	private double gamma;
	private FrequencyComputer fcomputer;

	public LMIRdir(double gamma, FrequencyComputer fcomputer){
		this.gamma=gamma;
		this.fcomputer=fcomputer;
	}
	
	public LMIRdir(FrequencyComputer fcomputer){
		this.gamma=1000;
		this.fcomputer=fcomputer;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Double> getFeatureList(DBObject requete, DBObject document) {
		ArrayList<Double> feature = new ArrayList<Double>();
		ArrayList<DBObject> stemsDocument = (ArrayList<DBObject>) document.get("weights");
		ArrayList<DBObject> stemsRequete = (ArrayList<DBObject>) requete.get("weights");
		String keyR,keyD;
		boolean flag = false;
		double tf;
		int length = fcomputer.getLength(document);
		int nwords = fcomputer.getNWords();
		int n;

		for(DBObject stemR : stemsRequete){
			keyR = stemR.keySet().toArray()[0].toString();
			for(DBObject stemD : stemsDocument){
				keyD = stemD.keySet().toArray()[0].toString();
				if(keyD.equals(keyR)){	
					n = fcomputer.getNOcc(keyR);
					tf = fcomputer.getTfFromTfidf((Double)stemD.get(keyR), keyR);
					feature.add((tf+(gamma*(n/nwords)))/(length+gamma));
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
	public String toString(){
		return "LMIRdir";
	}
}
