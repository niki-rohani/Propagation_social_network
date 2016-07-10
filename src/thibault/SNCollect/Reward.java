package thibault.SNCollect;

import java.util.Collection;

import core.Post;

public abstract class Reward {
	public abstract double getReward(Collection<Post> posts, Arm arm);
}


class ValModelCount extends Reward{
	ModelCount model; 
	public ValModelCount(ModelCount model){
		this.model=model;
	}
	
	public double getReward(Collection<Post> posts, Arm arm){
		double score=0;
		for(Post post:posts){
			score+=model.eval(post);
			}
		return score;
	}
	
	public String toString(){
		return "ValCount("+model.toString()+")";
	}
}


class ValModelCountNorm extends Reward{
	ModelCount model; 
	double coef;
	public ValModelCountNorm(ModelCount model, double coef){
		this.coef=coef;
		this.model=model;
	}
	
	public double getReward(Collection<Post> posts, Arm arm){
		double score=0;
		for(Post post:posts){
			score+=model.eval(post);
			}
		return Math.tanh(coef*score);
	}
	
	public String toString(){
		return "ValCountNormCoef"+this.coef+"("+model.toString()+")";
	}
}

class ValModelSentiment extends Reward{
	ModelSentiment model; 
	public ValModelSentiment(ModelSentiment model){
		this.model=model;
	}
	
	public double getReward(Collection<Post> posts, Arm arm){
		double score=0;
		for(Post post:posts){
			score+=model.eval(post);
			}
		if(posts.size()!=0){
			score=score/posts.size();
		}
		return score;
	}
	
	public String toString(){
		return model.toString();
	}
}


class ValModelLanguage extends Reward{
	ModelLanguage model; 
	public ValModelLanguage(ModelLanguage model){
		this.model=model;
	}
	
	public double getReward(Collection<Post> posts, Arm arm){
		double score=0;
		for(Post post:posts){
			score+=model.eval(post);
			}
		return score;
	}
	
	public String toString(){
		return "ValModelLanguage("+model.toString()+")";
	}
}


class ValModelLive extends Reward{
	ModelLive model; 
	public ValModelLive(ModelLive model){
		this.model=model;
	}
	
	public double getReward(Collection<Post> posts, Arm arm){
		double score=0;
		for(Post post:posts){
			score+=model.eval(post);
			}
		return Math.tanh(score);
	}
	
	public String toString(){
		return "ValModelLive("+model.toString()+")";
	}
}


class ValModelHybrid extends Reward{
	ModelLanguage model1; 
	ModelCount model2;
	public ValModelHybrid(ModelLanguage model1,ModelCount model2){
		this.model1=model1;
		this.model2=model2;
	}
	
	public double getReward(Collection<Post> posts, Arm arm){
		double score1=0.0;
		double score2=1.0;
		for(Post post:posts){
			score1+=model1.eval(post);
			//score2+=model2.eval(post);
			}
		//if(score1*score2>0){System.out.println(score2);}
		
		return Math.tanh(1.0*score1);
	}
	
	public String toString(){
		return "ValModelHybrid("+model1.toString()+"_"+model2.toString()+")";
	}
}
