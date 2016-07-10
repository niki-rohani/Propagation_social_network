package clustering;

import core.Data;
import similarities.*;

public abstract class StrClustering {
	protected int k;
	public StrClustering(int k){
		this.k=k;
	}
	public abstract Clustering clusterize(StrSim strSim) throws Exception;
	public void setNbClusters(int k){
		this.k=k;
	}
	public abstract String toString();
}
