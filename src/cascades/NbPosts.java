package cascades;

import java.util.ArrayList;


public class NbPosts extends CascadeFeatureProducer {
	public ArrayList<Double> getFeatures(Cascade cascade){
		ArrayList<Double> ret=new ArrayList<Double>();
		ret.add(cascade.getPosts().size()*1.0);
		return(ret);
	}
	
	public String toString(){
		return("NbPosts");
	}
}
