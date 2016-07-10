package thibault.SNCollect;

import core.Post;

public class ModelSentiment {

	public ModelSentiment(){
	}
	
	public double eval(Post p){
		double score=0.0;
		if(p.getOther()!=null){
			score=Double.parseDouble(p.getOther().get("sentiment").toString());
		}
		return score;
	}
	
	public String toString(){
		return "ModelSentiment";
	}
}
