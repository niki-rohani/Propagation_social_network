package cascades;

import java.util.ArrayList;
import core.Post;
import core.User;
import java.util.HashSet;
import java.util.HashMap;

import com.mongodb.BasicDBObject;
public class NbCascadesPerUser extends CascadeSetFeatureProducer {
	
	@Override
	public ArrayList<Double> getFeatures(HashSet<Cascade> cascades) {
		ArrayList<Double> ret=new ArrayList<Double>();
		HashMap<Integer,Integer> nbperuser=new HashMap<Integer,Integer>();
		for(Cascade c:cascades){
			HashSet<Post> posts=c.getPosts();
			HashSet<Integer> vus=new HashSet<Integer>();
			for(Post p:posts){
				int idu=p.getOwner().getID();
				if (!vus.contains(idu)){
					int nb=0;
					if (nbperuser.containsKey(idu)){
						nb=nbperuser.get(idu);
					}
					nb++;
					nbperuser.put(idu, nb);
					vus.add(idu);
				}
				
			}
		}
		int sum=0;
		int nb=0;
		for(Integer idu:nbperuser.keySet()){
			sum+=nbperuser.get(idu);
			nb++;
		}
		double moy=0;
		if (nb>0){
			moy=(sum*1.0)/nb;
		}
		ret.add(moy);
		return ret;
	}

	@Override
	public String toString() {
		return "NbCascadesPerUser";
	}

}
