package similarities;

import java.util.HashMap;
import wordsTreatment.*;
import core.*;

public class Cosine extends StrSim {

	public Cosine(){
		this((Data)null);
	}
	public Cosine(Data data){
		super(data);
	}
	
	public StrSim getInstance(Data _data){
		Cosine cos=new Cosine(_data);
		return(cos);
	}
	
	@Override	
	public String toString(){
		return("Cosine");
	}
	
	public double computeSim(HashMap<Integer, Double> poids1,HashMap<Integer, Double> poids2) {
		
		return(computeSimilarity(new Text(poids1),new Text(poids2)));
	}
	
	
	@Override
	public double computeSimilarity(Text t1, Text t2) {
		//String titre1=t1.getTitre();
		HashMap<Integer,Double> poids1=t1.getWeights();
		//String titre2=t2.getTitre();
		HashMap<Integer,Double> poids2=t2.getWeights();
		double ret=0.0;
		double norm1=t1.getNorm();
		double norm2=t2.getNorm();
		double den=norm1*norm2;
		if (den>0){
			for(Integer s:poids1.keySet()){
				double v1=poids1.get(s);
				double v2=0.0;
				if(poids2.containsKey(s)){
					v2=poids2.get(s);
				}
				ret+=v1*v2;
			}
			ret/=den;
		}
		
		//System.out.println("Normes = "+norm1+", "+norm2);
		
		return(ret);
	}
	
	public double getNorm(HashMap<Integer, Double> h){
		double nbt=0.0;
		for(Integer s:h.keySet()){
			double val=h.get(s);
			nbt+=val*val;
		}
		double norm=0.0;
		if (nbt>0){
			norm=Math.sqrt(nbt);
		}
		return(norm);
	}
	
	public static void main (String[] args){
		String t1="Ceci est le texte numero 1";
		//String t2="Ceci est le texte numero 1";
		String t2="C'est un sacre numero ce texte 2";
		
		Stemmer stemmer=new Stemmer();
		HashMap<String,Integer> stems1=stemmer.porterStemmerHash(t1);
		HashMap<String,Integer> stems2=stemmer.porterStemmerHash(t2);
		HashMap<Integer,Double> poids1=new HashMap<Integer,Double>();
		HashMap<Integer,Double> poids2=new HashMap<Integer,Double>();
		HashMap<String,Integer> stems=new HashMap<String,Integer>();
		int nbs=0;
		for(String s:stems1.keySet()){
			int id=-1;
			if (stems.containsKey(s)){
				id=stems.get(s);
			}
			else{
				nbs++;
				id=nbs;
				stems.put(s, nbs);
			}
			int val=stems1.get(s);
			poids1.put(id, (double)val);
		}
		for(String s:stems2.keySet()){
			int id=-1;
			if (stems.containsKey(s)){
				id=stems.get(s);
			}
			else{
				nbs++;
				id=nbs;
				stems.put(s, nbs);
			}
			int val=stems2.get(s);
			poids2.put(id, (double)val);
		}
		
		Cosine cos=new Cosine();
		System.out.println(cos.computeSim(poids1,poids2));
	}

}
