package similarities;

import java.util.HashMap;
import wordsTreatment.*;
import core.*;

public class CosineTitre extends StrSim {

	public CosineTitre(){
		this((Data)null);
	}
	public CosineTitre(Data data){
		super(data);
	}
	
	@Override	
	public String toString(){
		return("CosineTitre");
	}
	
	public StrSim getInstance(Data _data){
		CosineTitre cos=new CosineTitre(_data);
		return(cos);
	}
	
	
	
	public double computeSim(String texte1,String texte2){
		return(computeSimilarity(new Text(texte1),new Text(texte2)));
	}
	public double computeSimilarity(Text t1,Text t2){
		//Stemmer stemmer=new Stemmer();
		/*HashMap<String,Integer> h1=stemmer.porterStemmerHash(titre1);
		HashMap<String,Integer> h2=stemmer.porterStemmerHash(titre2);
		h1.remove(" * ");
		h2.remove(" * ");*/
		HashMap<String,Integer> h1=t1.getStemmedTitle();
		HashMap<String,Integer> h2=t2.getStemmedTitle();
		
		double norm1=getNorm(h1);
		double norm2=getNorm(h2);
		double den=norm1*norm2;
		double ret=0.0;
		if (den>0){
			for(String s:h1.keySet()){
				int v1=h1.get(s);
				int v2=0;
				if(h2.containsKey(s)){
					v2=h2.get(s);
				}
				ret+=v1*v2;
			}
			ret/=den;
		}
		
		return(ret);
	}
	public double getNorm(HashMap<String,Integer> h){
		int nbt=0;
		for(String s:h.keySet()){
			int val=h.get(s);
			nbt+=val*val;
		}
		double norm=0.0;
		if (nbt>0){
			norm=Math.sqrt(nbt);
		}
		return(norm);
	}
	
	

}
