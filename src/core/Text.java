package core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import wordsTreatment.Stemmer;
import java.util.Date;
import java.util.Set;
public class Text extends Node implements Serializable{
	public static final long serialVersionUID=1;
	protected int id; 
	protected String titre;
	protected HashMap<String,Integer> stemmedTitle;
	protected HashMap<Integer,Double> weights; // poids des stems dans le texte
	protected double norm=-1; // norm of the vector weights
	protected static int nb_texts=0;
	public Text(int id,String titre){
		this(id,titre,new HashMap<Integer,Double>());
	}
	public Text(String titre,HashMap<Integer,Double> weights){
		this(nb_texts+1,titre,weights);
	}
	public Text(HashMap<Integer,Double> weights){
		this("",weights);
	}
	public Text(String titre){
		this(titre,new HashMap<Integer,Double>());
	}
	public Text(){
		this("",new HashMap<Integer,Double>());
	}
	public Text(int id){
		this(id,"",new HashMap<Integer,Double>());
	}
	public Text(int id,String titre,HashMap<Integer,Double> weights){
		//super(titre+"_"+id);
		super(titre);
		this.id=id;
		this.titre=titre;
		this.weights=weights;
		stemmedTitle=null;
		nb_texts++;
	}
	
	public HashMap<Integer,Double> getWeights(){
		return(weights);
	}
	
	public HashMap<String,Integer> getStemmedTitle(){
		if (this.stemmedTitle!=null){
			return(this.stemmedTitle);
		}
		Stemmer stemmer=new Stemmer();
		stemmedTitle=stemmer.porterStemmerHash(titre);
		stemmedTitle.remove(" * ");
		return(stemmedTitle);
	}
	
	public String getTitre(){
		return(titre);
	}
	public int hashCode(){
		return(titre.hashCode());
	}
	
	public void setWeights(HashMap<Integer,Double> weights){
		this.weights=weights;
		norm=-1;
	}
	public int getID(){
		return(id);
	}
	public void videweights(){
		weights=null;
	}
	public String toString(){
		String s="Texte "+id+", titre : "+titre+"\n";
		/*s+="weights = ";
		for(Integer i:weights.keySet()){
			s+=i+"="+weights.get(i)+";";
		}
		s+="\n\n";*/
		return(s);
	}
	public static HashMap<Integer,Double> getCentralWeights(ArrayList<Text> texts){
		HashMap<Integer,Double> weights=new HashMap<Integer,Double>();
		for(Text t:texts){
			HashMap<Integer,Double> p=t.getWeights();
			for(Integer i:p.keySet()){
				double w=0.0;
				if (weights.containsKey(i)){
					w=weights.get(i);
				}
				w+=p.get(i);
				weights.put(i, w);
			}
		}
		int n=texts.size();
		if (n>0){
			for(Integer i:weights.keySet()){
				double w=weights.get(i);
				weights.put(i, w/n);
			}
		}
		return(weights);
	}
	public double getNorm(){
		if (norm>=0){
			return(norm);
		}
		double nbt=0.0;
		for(Integer s:weights.keySet()){
			double val=weights.get(s);
			nbt+=val*val;
		}
		norm=0.0;
		if (nbt>0){
			norm=Math.sqrt(nbt);
		}
		
		return(norm);
	}
	
	public void normalizeWeights(){
		
		normalize(weights);
	}
	
	public static void normalize(HashMap<Integer,Double> w){
		double nbt=0.0;
		for(Integer s:w.keySet()){
			double val=w.get(s);
			nbt+=val*val;
		}
		double norm=0.0;
		if (nbt>0){
			norm=Math.sqrt(nbt);
		
			Set<Integer> stems=w.keySet();
			for(Integer s:stems){
				double val=w.get(s)/norm;
				w.put(s,val);
			}
		}
	}
}
