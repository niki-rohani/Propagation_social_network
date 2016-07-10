package thibault.dynamicCollect;

import java.util.HashMap;

import core.Post;
import thibault.indexBertin.*;
import wordsTreatment.Stemmer;

public class postTreater {

	
	
	/*TF_WeighterThib weightComputer;
	TextTransformerThib trans;
	String dbName;
	String stemsCollectionName;
	
	public postTreater(String dbName, String stemsCollectionName){
		this.dbName=dbName;
		this.stemsCollectionName=stemsCollectionName;
		this.weightComputer=new TF_WeighterThib(dbName,stemsCollectionName);
		this.trans=new IDFPrunerThib(dbName,stemsCollectionName);
	}
	
	public postTreater(){
		this("propagation","stems");
	}
	
	
	
	public void treatPost(Post p){
		
		String text=p.getTexte();
		text=text.toLowerCase();
		text=text.replaceAll("\r", " ");
		text=text.replaceAll("\\r", " ");
		text=text.replaceAll("\\\\r", " ");
		text=text.replaceAll("\n", " ");
		text=text.replaceAll("\\n", " ");
		text=text.replaceAll("\\\\n", " ");
		text=text.replaceAll("\t", " ");
		text=text.replaceAll("\\t", " ");
		text=text.replaceAll("\\\\t", " ");
		text=text.replaceAll("  ", " ");
		text=text.replace("^ ", "");

		HashMap<Integer,Double> poids=trans.transform(weightComputer.getWeightsForIds(text));
		System.out.println(poids);
		p.setWeights(poids);*/
		
	public postTreater(){

	}
	public void treatPost(Post p){
		String text=p.getTexte();
		text=text.toLowerCase();
		text=text.replaceAll("\r", " ");
		text=text.replaceAll("\\r", " ");
		text=text.replaceAll("\\\\r", " ");
		text=text.replaceAll("\n", " ");
		text=text.replaceAll("\\n", " ");
		text=text.replaceAll("\\\\n", " ");
		text=text.replaceAll("\t", " ");
		text=text.replaceAll("\\t", " ");
		text=text.replaceAll("\\\\t", " ");
		text=text.replaceAll("  ", " ");
		text=text.replace("^ ", "");
		Stemmer stemmer=new Stemmer();
		HashMap<String,Integer> w=stemmer.porterStemmerHash(text);
		
		String textStemmed=new String();
		//System.out.println(w.size());
		for(String s: w.keySet()){
			//System.out.println(w);
			textStemmed=s+" "+textStemmed;
		}
		
		p.setTexte(textStemmed);
	}
	
	
}
