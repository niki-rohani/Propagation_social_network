package thibault.dynamicCollect;

import java.util.HashSet;

import core.Post;

public abstract class rewardLive {
	public rewardLive(){
	}
	abstract double eval(Post p);
}

class rewardLiveWords extends rewardLive{

	public HashSet<String> words;
	public rewardLiveWords(HashSet<String> words){
		super();
		this.words=words;
	}
	
	@Override
	double eval(Post p) {
		double scoreWord=0.0;
		double scoreRT=0.0;
		
		String text=p.getTexte();

		/*String str[] =text.split(" ");
		
		HashSet<String> textMod = new HashSet<String>();
		for (int i = 0; i < str.length; i++){
			textMod.add(str[i]);
		}
		
		
		for(String word:words){
			if(textMod.contains(word)){
				scoreWord+=1;
			}	
		}
		scoreWord=scoreWord/Math.sqrt(words.size()*text.length);
		*/
		
		for(String word:words){
			if(text.indexOf(word)!=-1){
				scoreWord+=1;
			}	
		}
		
		//scoreWord=scoreWord/Math.sqrt(words.size()*text.split(" ").length);
		
		/*if(p.getOther()!=null){
			scoreRT=Double.parseDouble(p.getOther().get("nbRT").toString());
		}*/
		
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