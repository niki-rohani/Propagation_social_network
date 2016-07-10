package thibault.dynamicCollect;
import java.util.HashSet;
import java.util.Collection;

import core.Post;
public abstract class Reward {
	public abstract double getReward(Collection<Post> posts, Arm arm);
}

class NbOkForBooleanModel extends Reward{
	BooleanModel model; 
	public NbOkForBooleanModel(BooleanModel model){
		this.model=model;
	}
	
	public double getReward(Collection<Post> posts, Arm arm){
		int nb=0;
		for(Post post:posts){
			if(model.eval(post)){
				nb++;
			}
		}
		return nb;
	}
	
	public String toString(){
		return "NbOkForBooleanModel("+model.toString()+")";
	}
	
	
}

class valLangModel extends Reward{
	LanguageModel model; 
	public valLangModel(LanguageModel model){
		this.model=model;
	}
	
	public double getReward(Collection<Post> posts, Arm arm){
		double score=0;
		for(Post post:posts){
			score+=model.eval(post);
			//System.out.println(score);
			}
		
		if(posts.size()!=0){
			return score;
		}
		else return 0;
	}
	
	public String toString(){
		return "valLangModel("+model.toString()+")";
	}
}

	
	
	class valRTandReplyTo extends Reward{
		RTandReplyToModel model; 
		public valRTandReplyTo(RTandReplyToModel model){
			this.model=model;
		}
		
		public double getReward(Collection<Post> posts, Arm arm){
			double score=0;
			for(Post post:posts){
				score+=model.eval(post);
				}
			
			return Math.tanh(0.005*score);   //attention ici
		}
		
		public String toString(){
			return "RtandRTo("+model.toString()+")";
		}
	}
	
	
	class valRewardLive extends Reward{
		rewardLive model; 
		public valRewardLive(rewardLive model){
			this.model=model;
		}
		
		public double getReward(Collection<Post> posts, Arm arm){
			double score=0;
			for(Post post:posts){
				score+=model.eval(post);
				}
			
			return Math.tanh(score);  //attention ici
		}
		
		public String toString(){
			return "ValRewardLive("+model.toString()+")";
		}
	}
	
	class valHybrid extends Reward{
		RTandReplyToModel model1; 
		LanguageModel model2;
		
		public valHybrid(RTandReplyToModel model1,LanguageModel model2){
			this.model1=model1;
			this.model2=model2;
		}
		
		public double getReward(Collection<Post> posts, Arm arm){
			double score1=0;
			double score2=0;
			for(Post post:posts){
				//score1+=model1.eval(post);
				score2+=model2.eval(post);
				}
			
			return Math.tanh(score2);   //attention ici
		}
		
		public String toString(){
			return "RewardHybrid("+model1.toString()+" "+model2.toString()+")";
		}
	}
	
	class valNbRetweet extends Reward{
		nbRetweetModel model; 
		
		public valNbRetweet(nbRetweetModel model){
			this.model=model;
		}
		
		public double getReward(Collection<Post> posts, Arm arm){
			double score=0;
			for(Post post:posts){
				score+=model.eval(post);
				}
			
			return score; //1+ pour le log dans le poisson voir ce qu on fera apres
		}
		
		public String toString(){
			return model.toString();
		}
	}
	
	
	class valSentiment extends Reward{
		sentimentModel model; 
		
		public valSentiment(sentimentModel model){
			this.model=model;
		}
		
		public double getReward(Collection<Post> posts, Arm arm){
			double score=0;
			if (posts.size()!=0){
				for(Post post:posts){
					score+=model.eval(post)/4;
					}
				score =score/posts.size(); 
				//System.out.println("NonEmpty :"+score);
			}
			else {
				score=0.0;
				//System.out.println("Empty :"+score);
			}
			return score;
		}
		
		public String toString(){
			return model.toString();
		}
	}