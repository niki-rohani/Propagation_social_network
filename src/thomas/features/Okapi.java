package thomas.features;

import java.util.ArrayList;

import com.mongodb.DBObject;

public class Okapi extends Feature{

	double alpha, beta;
	private FrequencyComputer fcomputer;

	public Okapi(double alpha, double beta, FrequencyComputer fcomputer){
		this.alpha = alpha;
		this.beta = beta;
		this.fcomputer=fcomputer;
	}
	public Okapi(FrequencyComputer fcomputer){
		this.alpha = 2.2;
		this.beta = 0.75;
		this.fcomputer=fcomputer;
	}

	
	@SuppressWarnings("unchecked")
	public ArrayList<Double> getFeatureList(DBObject requete, DBObject document) {
		ArrayList<Double> feature = new ArrayList<Double>();
		ArrayList<DBObject> stemsRequete = (ArrayList<DBObject>) requete.get("weights");
		ArrayList<DBObject> stemsDocument = (ArrayList<DBObject>) document.get("weights");

		String keyR,keyD;
		boolean flag = false;
		double tf;
		int length = fcomputer.getLength(document);
		double meanNWords = fcomputer.getMeanNWords();

		for(DBObject stemR : stemsRequete){
			keyR = stemR.keySet().toArray()[0].toString();
			for(DBObject stemD : stemsDocument){
				keyD = stemD.keySet().toArray()[0].toString();
				if(keyD.equals(keyR)){	
					tf = fcomputer.getTfFromTfidf((Double)stemD.get(keyR), keyR);
					feature.add(fcomputer.getIdf(keyR)*((alpha*tf)/(tf+(alpha*(1-beta+(beta*length/meanNWords))))));
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
		return "Okapi";
	}

}
