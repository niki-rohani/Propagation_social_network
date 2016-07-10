package experiments;

import java.util.ArrayList;
import java.util.TreeMap;

import core.User;

import java.util.HashMap;

import propagationModels.PropagationStruct;
import cascades.Cascade;
public class Recall extends EvalMeasure {
	private boolean ignoreInit;
	//private double pmin; //proba min to consider a node as infected
	
	public Recall(){
		this(false);
	}
	/*public Recall(boolean ignoreInit){
		this(ignoreInit,0.5);
	}*/
	public Recall(boolean ignoreInit){ //,double pmin){
		this.ignoreInit=ignoreInit;
		//this.pmin=pmin;
	}
	
	@Override
	public String getName() {
		return "Recall"+((ignoreInit)?"_ignoreInit":"");
	}
	
	
	public double getScoreForIt(TreeMap<Long,HashMap<String,Double>> conta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> hconta=PropagationStruct.getPBeforeT(conta);
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		HashMap<String,Double> firsts=ref.get(ref.firstKey());
		double nb=0;
		double nbRef=0;
		for(String u:href.keySet()){
			if ((!firsts.containsKey(u)) && ((!ignoreInit) || (!init.containsKey(u)))){
				nb+=1.0;
				//System.out.println(u);
				Double v=hconta.get(u);
				v=(v==null)?0.0:v;
				nbRef+=v;
				
			}
		}
		double rec=0.0;
		
		if (nb>0){
			rec=(1.0*nbRef)/nb;
		}
		
		return(rec);
	}
	public double getScoreForIt(HashMap<String,Double> hconta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		HashMap<String,Double> firsts=ref.get(ref.firstKey());
		double nb=0;
		double nbRef=0;
		for(String u:href.keySet()){
			if ((!firsts.containsKey(u)) && ((!ignoreInit) || (!init.containsKey(u)))){
				nb+=1.0;
				//System.out.println(u);
				Double v=hconta.get(u);
				v=(v==null)?0.0:v;
				nbRef+=v;
				
			}
		}
		double rec=0.0;
		
		if (nb>0){
			rec=(1.0*nbRef)/nb;
		}
		
		return(rec);
	}
	
	@Override
	public Result eval(Hyp hyp) {
		Cascade c=hyp.getStruct().getCascade();
		TreeMap<Long,HashMap<String,Double>> init=hyp.getInit(); //pstruct.getInitContaminated();
		TreeMap<Long,HashMap<String,Double>> ref=hyp.getRef(); //pstruct.getContaminated();
		ArrayList<TreeMap<Long,HashMap<String,Double>>> contaminations=hyp.getContaminations();
		
		/*double sum=0.0;
		int n=0;
		for(TreeMap<Long,HashMap<String,Double>> conta:contaminations){
			sum+=getScoreForIt(conta,ref,init);
			n++;
		}
		if (n>0){
			sum/=n;
		}*/
		int nb=contaminations.size();
		HashMap<String,Double> sumContaminated=new HashMap<String,Double>();
		for(TreeMap<Long,HashMap<String,Double>> conta:contaminations){
			HashMap<String,Double> hconta=PropagationStruct.getPBeforeT(conta);
			for(String u:hconta.keySet()){
				Double n=sumContaminated.get(u);
				double v=hconta.get(u)/nb;
				sumContaminated.put(u,(n==null)?v:(n+v));
			}
		}
		Result res=new Result("recall","Cascade_"+c.getID());
		res.addScore(this.getName(), getScoreForIt(sumContaminated,ref,init));
		return res;
	}

}
