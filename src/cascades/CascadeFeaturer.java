package cascades;

import java.util.ArrayList;



public class CascadeFeaturer {
	private ArrayList<CascadeFeatureProducer> featurers;
	private ArrayList<Double> thresholds;
	public CascadeFeaturer(ArrayList<CascadeFeatureProducer> featurers){
		this(featurers,new ArrayList<Double>());
	}
	public CascadeFeaturer(ArrayList<CascadeFeatureProducer> featurers, ArrayList<Double> thresholds){
		this.featurers=featurers;
		this.thresholds=thresholds;
		
	}
	/*public CascadeFeaturer(CascadeFeatureProducersFactory tfpf){
		featurers=tfpf.getFeatureProducers();
	}*/
	public ArrayList<Double> getFeatures(Cascade cascade){
		ArrayList<Double> features=new ArrayList<Double>();
		for(CascadeFeatureProducer featurer:featurers){
			features.addAll(featurer.getFeatures(cascade));
		}
		cascade.setFeatures(features);
		return(features);
	}
	public String toString(){
		String s="";
		int nt=0;
		for(CascadeFeatureProducer cp:featurers){
			for(int i=0;i<cp.getNbFeatures();i++){
				s+="_"+cp;
				if (thresholds.size()!=0){
					s+="_"+thresholds.get(nt);
					nt++;
				}
			}
		}
		return(s);
	}
	public boolean respectThresholds(Cascade cascade){
		ArrayList<Double> features=getFeatures(cascade);
		System.out.println(features);
		int nt=0;
		for(CascadeFeatureProducer cp:featurers){
			for(int i=0;i<cp.getNbFeatures();i++){
				if (thresholds.size()!=0){
					if (features.get(nt)<thresholds.get(nt)){
						return(false);
					}
					nt++;
				}
			}
		}
		return(true);
		
	}
}
