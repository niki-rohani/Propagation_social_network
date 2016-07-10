package thibault.dynamicCollect;

import core.Post;

public class nbRetweetModel {
	
	public nbRetweetModel(){
		
	}
	
	public double eval(Post p){
		double score=0.0;
		if(p.getOther()!=null){
			//score=Double.parseDouble(p.getOther().get("iAmRT").toString())+Double.parseDouble(p.getOther().get("iAmRTo").toString());
			score=Double.parseDouble(p.getOther().get("iAmRTo").toString());
			//score=Double.parseDouble(p.getOther().get("iAmRT").toString());
		}
		return score;
	}
	
	public String toString(){
		return "nbRetweetModel";
	}

}
