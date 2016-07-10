package thomas.features;

import java.util.ArrayList;

import com.mongodb.DBObject;

public class LMIRabs extends Feature {

	private double delta;
	private FrequencyComputer fcomputer;

	public LMIRabs(double delta, FrequencyComputer fcomputer){
		this.delta=delta;
		this.fcomputer=fcomputer;
	}
	
	public LMIRabs(FrequencyComputer fcomputer){
		this.delta=0.8;
		this.fcomputer=fcomputer;
	}


	@SuppressWarnings("unchecked")
	public ArrayList<Double> getFeatureList(DBObject requete, DBObject document) {
		ArrayList<Double> feature = new ArrayList<Double>();
		ArrayList<DBObject> stemsRequete = (ArrayList<DBObject>) requete.get("weights");
		ArrayList<DBObject> stemsDocument = (ArrayList<DBObject>) document.get("weights");
		String keyR,keyD;
		boolean flag = false;
		double tf,max;
		int length = fcomputer.getLength(document);
		int distinctLength = fcomputer.getDistinctLength(document);
		for(DBObject stemR : stemsRequete){
			keyR = stemR.keySet().toArray()[0].toString();
			for(DBObject stemD : stemsDocument){
				keyD = stemD.keySet().toArray()[0].toString();
				if(keyD.equals(keyR)){	
					tf = fcomputer.getTfFromTfidf((Double)stemD.get(keyR), keyR);
					max=(tf-delta>0)?tf-delta:0;
					double res = (max/length)+(delta*tf*(distinctLength/length));
					if (res == 0.0){
						feature.add(0.0);
					}else{
						feature.add(Math.log(res));
					}

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
		return "LMIRabs";
	}
}


