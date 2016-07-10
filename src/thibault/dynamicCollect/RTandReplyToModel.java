package thibault.dynamicCollect;

import core.Post;

public abstract class RTandReplyToModel {
	abstract double eval(Post p);
}

 class RTandReplyToModel1 extends RTandReplyToModel{
	
	public RTandReplyToModel1(){
		
	}

	public double eval(Post p){
		double score=0.0;
		if(p.getOther()!=null){
			score=Double.parseDouble(p.getOther().get("iAmRT").toString())+Double.parseDouble(p.getOther().get("iAmRTo").toString());
		}
		
		//System.out.println("score: "+score);
		return score;
	}
	
	public String toString(){
		return "RTandReplyToModel";
	}
}

 