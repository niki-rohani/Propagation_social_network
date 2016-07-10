package thomas.features;

import java.util.ArrayList;

import com.mongodb.DBObject;

public class SimilariteCosinus extends Feature {

	private FrequencyComputer fcomputer;

	public SimilariteCosinus(FrequencyComputer fcomputer){
		this.fcomputer=fcomputer;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Double> getFeatureList(DBObject requete, DBObject document) {
		ArrayList<Double> feature = new ArrayList<Double>();
		ArrayList<Double> tfq = new ArrayList<Double>();
		ArrayList<Double> tfd = new ArrayList<Double>();

		ArrayList<DBObject> stemsRequete = (ArrayList<DBObject>) requete.get("weights");
		ArrayList<DBObject> stemsDocument = (ArrayList<DBObject>) document.get("weights");
		boolean flag = false;
		String keyR, keyD;
		double idf, tfidf;
		double simi=0.0;
		for(DBObject stemR : stemsRequete){
			keyR = stemR.keySet().toArray()[0].toString();
			tfq.add((Double)stemR.get(keyR));
			for(DBObject stemD : stemsDocument){
				keyD = stemD.keySet().toArray()[0].toString();
				if(keyD.equals(keyR)){
					idf = fcomputer.getIdf(keyR);
					tfidf=(Double)stemD.get(keyD);
					tfd.add(idf*fcomputer.getFrequencyFromTfidf(tfidf, keyR));
					flag = true;
				}
				if(flag){;break;}
			}
			if(!flag){
				tfd.add(0.0);
			}else{
				flag = false;
			}
		}
		
		for(int i=0;i<tfq.size();i++){
			simi+=(tfq.get(i)*tfd.get(i));
		}
		simi = simi/(tfq.size()*tfd.size());
		feature.add(simi);
		return feature;
	}


	public String toString(){
		return "SimilariteCosinus";
	}
}
