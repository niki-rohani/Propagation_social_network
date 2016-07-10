package cascades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import core.Post;

public class NbSteps extends CascadeFeatureProducer {
	private HashMap<Long,Step> steps;
	private long step;
	public NbSteps(String db,String colStep){
		this(Step.loadSteps(db,colStep));
	}
	public NbSteps(HashMap<Long,Step> steps){
		this.steps=steps;
		this.step=0;
		if(steps.size()>0){
			step=steps.values().iterator().next().getStep();
		}
	}
	public ArrayList<Double> getFeatures(Cascade cascade){
		ArrayList<Double> ret=new ArrayList<Double>();
		HashSet<Post> posts=cascade.getPosts();
		HashMap<Long,Integer> sposts=new HashMap<Long,Integer>();
		for(Post p:posts){
			long t=Step.getIdStep(p.getTimeStamp(),step);
			Integer n=sposts.get(t);
			int nb=(n==null)?0:n;
			sposts.put(t, nb+1);
		}
		ret.add(sposts.size()*1.0);
		return(ret);
	}
	
	public String toString(){
		return("NbSteps");
	}
}
