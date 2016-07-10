package thibault.dynamicCollect;

import java.awt.List;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Map;
import java.util.Set;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import core.Post;

public abstract class LanguageModel {

	HashMap<Integer, Double> poidsModel;
	double norme=0;
	int nbWords=2000;
	public LanguageModel(){
		this.poidsModel=new HashMap<Integer, Double>();
	}
	
	abstract void setWeights();
	public abstract String toString();
	
	double eval(Post p){
		double normePost=0;
		double score = 0.0;
			for (int key:p.getWeights().keySet()){
				if(key<=nbWords){
				score+=p.getWeights().get(key)*this.poidsModel.get(key);
				normePost=normePost+p.getWeights().get(key)*p.getWeights().get(key);
			}}
			normePost=Math.sqrt(normePost);
			if(score!=0){
				score=score/(normePost*this.norme);
			}
			
		return score;
	}
	
	void highestWeightsModel(int size){
		ArrayList<Double> listValue=new ArrayList<Double>(this.poidsModel.values());
		Collections.sort(listValue);
		for(int i=1;i<nbWords+1;i++){
			if(this.poidsModel.get(i)<listValue.get(nbWords-size)){
				this.poidsModel.put(i, 0.0);
			}
		}
	}
}

class RandomWeightsLanguageModel extends LanguageModel{

	int nbWordsNonZero;
	
	public RandomWeightsLanguageModel(int nbNonZero){
		super();
		this.nbWordsNonZero=nbNonZero;
		this.setWeights();
}



public void setWeights() {
	HashSet<Integer> indNonNull=new HashSet<Integer>();
	for(int i=0;i<nbWordsNonZero;i++){
		int indice = 1 + (int)(Math.random() * ((nbWords - 1) + 1));
		while(indNonNull.contains(indice)==true){
			indice = 1 + (int)(Math.random() * ((nbWords - 1) + 1));
		}
		indNonNull.add(indice);
	}

	double sum=0;
	
	for(int i=1;i<nbWords+1;i++){
		if(indNonNull.contains(i)){
			if(i==1 || i==2){
				this.poidsModel.put(i,0.0); 
			}
			Random r = new Random();
			double randomValue = r.nextDouble();
			this.poidsModel.put(i,randomValue);  
			sum = sum+randomValue;	
		}
		else{
			this.poidsModel.put(i,0.0); 
		}

	}

	
	for(int key:this.poidsModel.keySet()){
		this.poidsModel.put(key, this.poidsModel.get(key)/sum);
		this.norme=this.norme+this.poidsModel.get(key)*this.poidsModel.get(key);
	}

	this.norme=Math.sqrt(this.norme);
}


 public String toString(){
	 String r="";
	 for(int key:this.poidsModel.keySet()){
		 if (this.poidsModel.get(key)!=0.0){
			r=r+"("+key+","+this.poidsModel.get(key)+") "; 
		 }
	 }
	return ""+"RandomWeightsLanguageModel"+r;
	
}
}

 class ManualWeightsLanguageModel extends LanguageModel{

	HashMap<Integer, Double> poidsNonNull;

	public ManualWeightsLanguageModel(HashMap<Integer, Double> poidsNonNull){
	super();
	this.poidsNonNull=poidsNonNull;
	this.setWeights();
	}

	@Override
	void setWeights() {
		double sum=0;
		for(int i=1;i<nbWords+1;i++){
			if(poidsNonNull.containsKey(i)){
				double val =poidsNonNull.get(i);
				this.poidsModel.put(i,val);  
				sum = sum+val;	
			}
			else{
				this.poidsModel.put(i,0.0); 
			}
		}

		
		for(int key:this.poidsModel.keySet()){
			this.poidsModel.put(key,this.poidsModel.get(key)/sum);
			this.norme=this.norme+this.poidsModel.get(key)*this.poidsModel.get(key);
		}

		this.norme=Math.sqrt(this.norme);
		}
		
	
 
 public String toString(){
	 String r="";
	 for(int key:this.poidsModel.keySet()){
		 if (this.poidsModel.get(key)!=0.0){
			r=r+"("+key+","+this.poidsModel.get(key)+") "; 
		 }
	 }
	return ""+"ManualWeightsLanguageModel"+r;
}


 }

  
 //Dans cette classe on peut mettre les poids a partir d une ligne de texte au format (ind1,poids1) (ind2,poids2) ...
//Ensuite on s'en sert quand on va configurer leval en lisant un fichier ligne par ligne
 class WeightsFromFileLanguageModel extends LanguageModel{


	 String lineToRead;
	 
	public WeightsFromFileLanguageModel(String lineToRead){
	super();
	this.lineToRead=lineToRead;
	this.setWeights();
	}
	
	@Override
	void setWeights() {
		
		HashMap<Integer, Double> poidsNonNull=new HashMap<Integer,Double>();
		String ligne1=lineToRead.replace("(", "");
		String ligne2=ligne1.replace(")", "");
		String str[] =ligne2.split("\t");
		for(int i=0;i<str.length;i++){
			String str1[] =str[i].split(",");
			poidsNonNull.put(Integer.parseInt(str1[0]),Double.parseDouble(str1[1]));
		}
		double sum=0;
		for(int i=1;i<nbWords+1;i++){
			if(poidsNonNull.containsKey(i)){
				double val =poidsNonNull.get(i);
				this.poidsModel.put(i,val);  
				sum = sum+val;	
			}
			else{
				this.poidsModel.put(i,0.0); 
			}

		}
		
		for(int key:this.poidsModel.keySet()){
			this.poidsModel.put(key,this.poidsModel.get(key)/sum);
			this.norme=this.norme+this.poidsModel.get(key)*this.poidsModel.get(key);
		}

		this.norme=Math.sqrt(this.norme);
		//System.out.println(poidsModel);
	}

	 public String toString(){
		 String r="";
		 for(int key:this.poidsModel.keySet()){
			 if (this.poidsModel.get(key)!=0.0){
				r=r+"("+key+","+this.poidsModel.get(key)+") "; 
			 }
		 }
		return ""+"WeightsFromFileLanguageModel"+r;
	}

 }
 