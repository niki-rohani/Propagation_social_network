package thomas.features;

import java.util.ArrayList;

import com.mongodb.DBObject;


public class LMIRjm extends Feature {


	private double lambda;
	private FrequencyComputer fcomputer;

	public LMIRjm(double lambda, FrequencyComputer fcomputer){
		this.lambda=lambda;
		this.fcomputer=fcomputer;
	}
	
	public LMIRjm(FrequencyComputer fcomputer){
		this.lambda=0.4;
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
		int n,nwords = fcomputer.getNWords();

		for(DBObject stemR : stemsRequete){
			keyR = stemR.keySet().toArray()[0].toString();
			for(DBObject stemD : stemsDocument){
				keyD = stemD.keySet().toArray()[0].toString();
				if(keyD.equals(keyR)){	
					tf = fcomputer.getTfFromTfidf((Double)stemD.get(keyR), keyR);
					n = fcomputer.getNOcc(keyR);
					feature.add((lambda*tf)+((1-lambda)*(n/nwords)));
				}	if(flag){;break;}
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
		return "LMIRjm";
	}
}