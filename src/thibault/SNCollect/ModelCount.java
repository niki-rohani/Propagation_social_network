package thibault.SNCollect;

import core.Post;

public class ModelCount {
	
	public int caseCount;
	public ModelCount(int caseCount){
		this.caseCount=caseCount;
	}
	
	public double eval(Post p){
		double score=0.0;
		if(p.getOther()!=null){
			switch (caseCount) {
			case 1://cas 1: rewteet et reply
				score=Double.parseDouble(p.getOther().get("iAmRT").toString())+Double.parseDouble(p.getOther().get("iAmRTo").toString());
				break;
			case 2://cas 1: rewteet 
				score=Double.parseDouble(p.getOther().get("iAmRT").toString());
				break;
			case 3://cas 1: reply
				score=Double.parseDouble(p.getOther().get("iAmRTo").toString());
				break;
				default:
			}
		}
		return score;
	}
	
	public String toString(){
		return "ModelCount"+caseCount;
	}

}