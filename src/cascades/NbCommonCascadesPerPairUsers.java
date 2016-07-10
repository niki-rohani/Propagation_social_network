package cascades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import core.Post;
import core.PairsValues;
public class NbCommonCascadesPerPairUsers extends CascadeSetFeatureProducer {

	@Override
	public ArrayList<Double> getFeatures(HashSet<Cascade> cascades) {
		ArrayList<Double> ret=new ArrayList<Double>();
		//PairsValues<Integer,Integer> common=new PairsValues<Integer,Integer>();
		HashMap<Integer,HashMap<Integer,Integer>> common=new HashMap<Integer,HashMap<Integer,Integer>>(); 
		
		int nbc=0;
		for(Cascade c:cascades){
			int idc=c.getID();
			HashSet<Post> posts=c.getPosts();
			HashSet<Integer> vus=new HashSet<Integer>();
			for(Post p:posts){
				int idu=p.getOwner().getID();
				if (!vus.contains(idu)){
					vus.add(idu);
				}
				
			}
			ArrayList<Integer> users=new ArrayList<Integer>(vus);
			for(int i=0;i<users.size();i++){
				int idi=users.get(i);
				for(int j=i+1;j<users.size();j++){
					int idj=users.get(j);
					//common.add(idi, idj, idc);
					HashMap<Integer,Integer> h=null;
					int x=idi;
					int y=idj;
					if (idi>idj){
						x=idj;
						y=idi;
					}
					
					if (!common.containsKey(x)){
						h=new HashMap<Integer,Integer>();
						h.put(y, 1);
						common.put(x, h);
					}
					else{
						h=common.get(x);
						int nb=0;
						if (h.containsKey(y)){
							nb=h.get(y);
						}
						
						h.put(y, nb+1);
					}
				}
			}
			nbc++;
			System.out.println(nbc+" Cascades traitees");
		}
		int sum=0;
		int nb=0;
		for(Integer idu:common.keySet()){
			HashMap<Integer,Integer> h=common.get(idu);
			for(Integer idu2:h.keySet()){
				sum+=h.get(idu2);
				nb++;
			}
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
		
		return "NbCommonCascadesPerPairUsers";
	}

}
