package thibault.dynamicCollect;

import java.util.List;

import com.twitter.Extractor;

import core.Post;

public class sentimentModel {
	
public sentimentModel(){
		
	}

public double eval(Post p){
	double score=2;  //neutre
	
	this.cleanTweet(p.getTexte());
	score = NLP.findSentiment(cleanTweet(p.getTexte()));
	
	/*if(p.getTexte()!=null){
		score = NLP.findSentiment(p.getTexte());
	}*/
	//System.out.println(score);
	
	return score;
}


public String cleanTweet(String Tweet){
	
	List<String> words;
	Extractor extractor = new Extractor();
	
	words = extractor.extractMentionedScreennames(Tweet);
	for(String w:words){
		Tweet=Tweet.replace("@"+w, "");
	}
	words = extractor.extractHashtags(Tweet);
	for(String w:words){
		Tweet=Tweet.replace("#"+w, "");
	}
	words = extractor.extractURLs(Tweet);
	for(String w:words){
		Tweet=Tweet.replace(w, "");
	}
	Tweet=Tweet.replace("rt : ", "");
	Tweet=Tweet.replace("...", "");
	
	return Tweet;
	
}

public String toString(){
	return "sentimentModel";
}
}
