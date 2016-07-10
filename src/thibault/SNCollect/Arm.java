package thibault.SNCollect;

import java.util.Collection;
import java.util.HashSet;

import core.Post;
import core.Text;
import core.User;

public class Arm extends Text{
	
	private static final long serialVersionUID = 1L;
	
	public int numberPlayed=0;
	public double sumRewards=0.0;
	public double sumProdRewards=0.0;
	public double lastReward=0.0;
	public double score=0.0;
	
	public Arm(String name){
		super(name);
	}
	
	public void computeReward(Reward r, HashSet<Post> posts) {
		User me=User.getUser(name);
		Collection<Post> mines=me.getPosts().values();
		lastReward=r.getReward(mines,this);
	}
	
}
