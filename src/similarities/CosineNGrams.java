package similarities;

import java.util.HashMap;

import core.Text;
import core.Data;

public class CosineNGrams extends StrSim {
	private int ngram; // taille des ngrams ?
	
	public CosineNGrams(int ngram){
		this((Data)null,ngram);
	}
	public CosineNGrams(){
		this(2);
	}
	public CosineNGrams(Data data,int ngram){
		super(data);
		this.ngram=ngram;
	}
	
	
	@Override	
	public String toString(){
		return("CosineNGrams_ngrams="+ngram);
	}
	
	public StrSim getInstance(Data _data){
		CosineNGrams cos=new CosineNGrams(_data,ngram);
		return(cos);
	}
	
	
	
	public double computeSim(String texte1,String texte2){
		return(computeSimilarity(new Text(texte1),new Text(texte2)));
	}
	public double computeSimilarity(Text t1, Text t2) {
		String titre1=t1.getTitre();
		//HashMap<Integer,Double> poids1=t1.getWeights();
		String titre2=t2.getTitre();
		//HashMap<Integer,Double> poids2=t2.getWeights();
		double ret=0.0;
		
		HashMap<String,Integer> h1=getNGrams(titre1);
		double norm1=getNorm(h1);
		HashMap<String,Integer> h2=getNGrams(titre2);
		double norm2=getNorm(h2);
		
		double den=norm1*norm2;
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
	public HashMap<String,Integer> getNGrams(String text){
		HashMap<String,Integer> ret=new HashMap<String,Integer>();
		
		char[] cars=text.toCharArray();
		for(int i=0;i<=(cars.length-ngram);i++){
			String ng=text.substring(i, i+ngram);
			int n=0;
			if(ret.containsKey(ng)){
				n=ret.get(ng);
			}
			n++;
			ret.put(ng, n);
		}
		return(ret);
	}
	
	public static void main (String[] args){
		String t1="Ceci est le texte numero 1";
		//String t2="Ceci est le texte numero 1";
		String t2="C'est un sacre numero ce texte 2";
		CosineNGrams cos=new CosineNGrams(2);
		System.out.println(cos.computeSim(t2,t1));
	}
}
