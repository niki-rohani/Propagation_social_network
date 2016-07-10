package clustering;

import similarities.StrSim;
import core.*;
import java.util.ArrayList;

public class KernelKMeans extends StrClustering {
	private int nb_it; // on relance nb_it fois 
	private int max_passes=500; // maximum de passages pour une iteration
	public KernelKMeans(int k){
		this(k,1);
	}
	public KernelKMeans(){
		this(5,1);
	}
	public KernelKMeans(int k,int nb_it){
		super(k);
		this.nb_it=nb_it;
	}
	
	@Override
	public String toString(){
		String s="KernelKmeans_nbit="+nb_it;
		return(s);
	}
	
	@Override
	public Clustering clusterize(StrSim strSim) throws Exception{
		double max_cohesion=0.0;
		Clustering max_clust=null;
		for(int i=0;i<nb_it;i++){
			Clustering clust=clusterizeIt(strSim);
			double co=clust.getCohesion();
			if ((max_clust==null) || (co>max_cohesion)){
				max_clust=clust;
				max_cohesion=co;
			}
			System.out.println("iteration "+(i+1)+", cohesion = "+co);
		}
		return(max_clust);
	}
	public Clustering clusterizeIt(StrSim strSim) throws Exception{
		Clustering clustering=new Clustering(k,strSim);
		clustering.initAlea();
		Data data=strSim.getData();
		//double cohesion=clustering.getCohesion();
		int change=1;
		int passe=0;
		while((change>0) && (passe<max_passes)){
			change=0;
			passe++;
			
			//System.out.println("Sampling...");
			ArrayList<Integer> els=data.sample(-1);
			
			for(Integer el:els){
				//System.out.println(el);
				double max_cohesion=clustering.getCohesion();
				int c=clustering.getIClusterOf(el);
				int cmax=c;
				clustering.removeFromCluster(el, c);
				for(int i=0;i<k;i++){
					if (i!=c){
						clustering.addToCluster(el, i);
						double co=clustering.getCohesion();
						if (co>max_cohesion){
							max_cohesion=co;
							cmax=i;
						}
						clustering.removeFromCluster(el, i);
					}
				}
				clustering.addToCluster(el, cmax);
				if (cmax!=c){
					change++;
				}	
			}
			System.out.println("passe "+passe+", change "+change);
		}
		
		return(clustering);
	}
	
}
