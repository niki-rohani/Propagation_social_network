package thibault.SNCollect;

import java.util.HashSet;

import core.Post;

public abstract class ModelLive {
	public ModelLive(){
	}
	abstract double eval(Post p);
}

class ModelLiveWords extends ModelLive{

	public HashSet<String> words;
	public ModelLiveWords(HashSet<String> words){
		super();
		this.words=words;
	}
	
	@Override
	double eval(Post p) {
		double scoreWord=0.0;
		//double scoreRT=0.0;
		
		String text=p.getTexte();
		
		for(String word:words){
			if(text.indexOf(word)!=-1){
				scoreWord+=1;
			}	
		}
		
		return scoreWord;
	}
	
	public String toString(){
		String r=new String();
		for(String word:words){
			r+=word+" ";
		}
		return "RewardLiveWords: "+r;
	}
	
}