package cascades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import core.Post;
import core.User;

// Calcule un Chi2 entre une cascade et la cascade "de reference" donne a la construction.
// Ca sert a savoir a quel point une cascade devie de la distribution generale.

public class Chi2Feature extends CascadeFeatureProducer {

	private HashMap<Long,Step> steps;
	private long maxid;
	private long minid;
	private long timestep;
	private long nbSteps;
	private int nbTotalPosts;
	//private int minStep;
	
	public Chi2Feature(String db,String colStep){
		this(Step.loadSteps(db,colStep));
	}
	public Chi2Feature(HashMap<Long,Step> steps){
		this.steps=steps;
		this.timestep=0;
		if(steps.size()>0){
			timestep=steps.values().iterator().next().getStep();
		}
		//this.minStep=minStep;
		minid=-1;
		maxid=-1;
		nbTotalPosts=0;
		for(Long id:steps.keySet()){
			//if (id>=minStep){
				if ((minid==-1) || (id<minid)){
					minid=id;
				}
				if ((maxid==-1) || (id>maxid)){
					maxid=id;
				}
				Step st=steps.get(id);
				nbTotalPosts+=st.getNbPosts();
				
			//}
		}
		this.nbSteps=maxid-minid+1;
		
	}
	
	@Override
	public ArrayList<Double> getFeatures(Cascade cascade) {
		
		HashMap<Long,Double> nbs = new HashMap<Long,Double>();
		//HashMap<Integer,Double> Ref = new HashMap<Integer,Double>();	
		int nposts =0;
		for(Post p:cascade.getPosts()){
			long t=Step.getIdStep(p.getTimeStamp(),timestep);
			//if (t>=minStep){
				int n=1;
				if (nbs.containsKey(t)){
					n+=nbs.get(t);
				}
				nbs.put(t, n*1.0);
				nposts++;
			//}
		}
		double chi2 = 0 ;
		if ((nbTotalPosts>0) && (nposts>0)){
		  for(Long i:steps.keySet()){
			//if (i>=minStep){  
				double n=0;
				if (nbs.containsKey(i)){
					n=nbs.get(i);
				}
				//int nref=steps.get(i).getNbPosts();
				double den=(steps.get(i).getNbPosts()*1.0/nbTotalPosts)*nposts;
				chi2 += (Math.pow((n*1.0)-den,2) / den);
			//}
			
		  }
		}
		//	chi2 += (Math.pow(N*A.get(i)-N*B.get(i),2)) / (N*B.get(i)) ;
		
		
		ArrayList<Double> r = new ArrayList<Double>(1) ;
		r.add(chi2) ;
		
		return r ;
	}

	@Override
	public String toString() {
		return "TestChi2"+timestep ;
	}
	
	// Calcule la loi de proba associe a une cascade.
	/*private ArrayList<Double> cascadeToProba(Cascade c) {
		
		ArrayList<Double> retour = new ArrayList<Double>(this.nbSteps) ;
		System.out.println(nbSteps);
		for(int i=0 ; i<nbSteps ; i++) {
			retour.add(0.0);
		}
		
		Iterator<Post> iter = c.getPosts().iterator() ;
		long ts; 
		double total = c.getPosts().size() ;
		Post p ;
		while(iter.hasNext()) {
			p = iter.next() ;
			ts = p.getTimeStamp() ;
			int i = (int)((ts-this.iniTimestamp)/this.timestep) ;
			retour.set(i,retour.get(i)+1.0) ;
		}
		iter = c.getPosts().iterator() ;
		for(int i=0 ; i<retour.size() ; i++) {
			retour.set(i,(retour.get(i))/total) ;
		}
		System.out.println(retour);
		return retour;
		
	}*/
	
	/*public static void main(String args[]) {
		System.out.println((int) Math.ceil((double)((51325489 - 51325000) / (double)50))) ;
		
		// Petit test ;
		User u = new User("barnabe") ;
		HashSet<Post> hp1 = new HashSet<Post>();
		Post p ;
		for(int i=0; i<100 ; i++) {
			p = new Post("", u, i, null) ;
			hp1.add(p) ;
		}
		Cascade ref = new Cascade("ref",hp1) ;
		
		hp1 = new HashSet<Post>();
		p = new Post("",u,05,null) ;hp1.add(p) ;
		p = new Post("",u,15,null) ;hp1.add(p) ;
		p = new Post("",u,25,null) ;hp1.add(p) ;
		p = new Post("",u,55,null) ;hp1.add(p) ;
		p = new Post("",u,65,null) ;hp1.add(p) ;
		p = new Post("",u,75,null) ;hp1.add(p) ;
		Cascade test = new Cascade("test",hp1) ;
		
		Chi2Feature chi2 = new Chi2Feature(ref,10) ;
		System.out.println(chi2.getFeatures(test).get(0)) ;
		
		
	}*/

}
