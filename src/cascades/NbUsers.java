package cascades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import core.Post;

public class NbUsers extends CascadeFeatureProducer {
	public ArrayList<Double> getFeatures(Cascade cascade){
		ArrayList<Double> ret=new ArrayList<Double>();
		HashSet<Post> posts=cascade.getPosts();
		HashMap<Integer,Post> uposts=new HashMap<Integer,Post>();
		for(Post p:posts){
			int us=p.getOwner().getID();
			long t=p.getTimeStamp();
			if ((!uposts.containsKey(us)) || (uposts.get(us).getTimeStamp()>t)){
				uposts.put(us, p);
			}
		}
		ret.add(uposts.size()*1.0);
		return(ret);
	}
	
	public String toString(){
		return("NbUsers");
	}
}

