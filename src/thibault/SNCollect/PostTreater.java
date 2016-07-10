package thibault.SNCollect;

import java.util.HashMap;

import core.Post;
import wordsTreatment.Stemmer;

public class PostTreater {

		
	public PostTreater(){
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
		for(String s: w.keySet()){
			textStemmed=s+" "+textStemmed;
		}
		
		p.setTexte(textStemmed);
	}
	
	
}
