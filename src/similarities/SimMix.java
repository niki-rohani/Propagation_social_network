package similarities;

import java.util.HashMap;
import core.*;
import wordsTreatment.Stemmer;

public class SimMix extends StrSim {

	private StrSim str1;
	private StrSim str2;
	private double coef1;
	private double coef2;
	
	public SimMix(StrSim str1,double coef1,StrSim str2,double coef2){
		this((Data)null,str1,coef1,str2,coef2);
	}
	public SimMix(Data data,StrSim str1,double coef1,StrSim str2,double coef2){
		super(data);
		this.str1=str1;
		this.str2=str2;
		this.coef1=coef1;
		this.coef2=coef2;
	}
	
	public StrSim getInstance(Data _data){
		StrSim s1=str1.getInstance(_data);
		StrSim s2=str2.getInstance(_data);
		SimMix sm=new SimMix(_data,s1,coef1,s2,coef2);
		return(sm);
	}
	
	@Override	
	public String toString(){
		return("SimMix_un="+str1+"_coef1="+coef1+"_deux="+str2+"_coef2="+coef2);
	}
	
	@Override
	public void setData(Data _data){
		str1.setData(_data);
		str2.setData(_data);
		super.setData(_data);
	}
	
	@Override
	public double computeSim(int text1,int text2) throws Exception{
		double v1=str1.getSim(text1,text2);
		double v2=str2.getSim(text1,text2);
		double ret=0.0;
		double scoef=coef1+coef2;
		if (scoef!=0){
			ret=(coef1*v1+coef2*v2)/scoef;
		}
		return ret;
	}
	
	@Override
	public double computeSimilarity(Text t1, Text t2) {
		
		double v1=str1.computeSimilarity(t1, t2);
		double v2=str2.computeSimilarity(t1, t2);
		double ret=0.0;
		double scoef=coef1+coef2;
		if (scoef!=0){
			ret=(coef1*v1+coef2*v2)/scoef;
		}
		return ret;
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
		CosineNGrams cosn=new CosineNGrams(2);
		SimMix sim=new SimMix(cos,0.8,cosn,0.2);
		Text text1=new Text(t1,poids1);
		Text text2=new Text(t2,poids2);
		
		System.out.println(sim.computeSim(text1,text2));
	}

}
