package clustering;

import similarities.StrSim;

public class AleaClustering extends StrClustering {
	public AleaClustering(int k){
		super(k);
	}
	@Override
	public Clustering clusterize(StrSim strSim) throws Exception{
		Clustering clustering=new Clustering(k,strSim);
		clustering.initAlea();
		return(clustering);
	}
	@Override
	public String toString(){
		String s="AleaClustering_k="+k;
		return(s);
	}

}
