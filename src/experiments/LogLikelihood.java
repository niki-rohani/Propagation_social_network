package experiments;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import propagationModels.PropagationStruct;
import cascades.Cascade;
import java.util.HashSet;
public class LogLikelihood extends EvalMeasure {
	//private boolean ignoreInit;
	private HashSet<String> allUsers;
	public LogLikelihood(HashSet<String> allUsers){ //,boolean ignoreInit){
		//this.ignoreInit=ignoreInit;
		this.allUsers=allUsers;
	}
	
	@Override
	public String getName() {
		return "LogLikelihood";
	}
	
	
	public double getScoreForItPerplexity(HashMap<String,Double> hconta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		
		double like=0;
		double min=0.001;
		for(String u:allUsers){
			//if ((!ignoreInit) || (!init.containsKey(u))){
				Double v=hconta.get(u);
				v=(v==null)?0.0:v;
				/*if(v<Double.MIN_VALUE){
					v=Double.MIN_VALUE;
				}
				if(v>(1.0-Double.MIN_VALUE)){
					v=1.0-Double.MIN_VALUE;
				}*/
				
				//v*=0.99998;
				//v+=0.00001;
				
				if (href.containsKey(u)){
					if(v<min){
						v=min;
					}
					like+=Math.log(v);
				}
				else{
					if(v>(1.0-min)){
						v=1.0-min;
					}
					like+=Math.log(1.0-v);
				}
			//}
		}
		if(allUsers.size()>0){
			like/=allUsers.size();
			//like*=10000;
		}
		if(Double.isInfinite(like)){
			like=Math.log(0.00000000000000000001)/Math.log(2);
		}
		return(like);
	}
	
	public double getScoreForItLike(HashMap<String,Double> hconta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		
		double like=0;
		double min=0.001; //Double.MIN_VALUE;
		for(String u:allUsers){
			//if ((!ignoreInit) || (!init.containsKey(u))){
				Double v=hconta.get(u);
				v=(v==null)?0.0:v;
				/*if(v<Double.MIN_VALUE){
					v=Double.MIN_VALUE;
				}
				if(v>(1.0-Double.MIN_VALUE)){
					v=1.0-Double.MIN_VALUE;
				}*/
				
				//v*=0.99998;
				//v+=0.00001;
				
				if (href.containsKey(u)){
					if(v<min){
						v=min;
					}
					like+=Math.log(v);
				}
				else{
					if(v>(1.0-min)){
						v=1.0-min;
					}
					like+=Math.log(1.0-v);
				}
			//}
		}
		if(allUsers.size()>0){
			like/=allUsers.size();
			//like*=10000;
		}
		if(Double.isInfinite(like)){
			like=Math.log(0.00000000000000000001);
		}
		return(like);
	}
	public double getScoreForItLikeBis(HashMap<String,Double> hconta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		
		double like=0;
		double min=0.01;
		for(String u:allUsers){
			//if ((!ignoreInit) || (!init.containsKey(u))){
				Double v=hconta.get(u);
				v=(v==null)?0.0:v;
				/*if(v<Double.MIN_VALUE){
					v=Double.MIN_VALUE;
				}
				if(v>(1.0-Double.MIN_VALUE)){
					v=1.0-Double.MIN_VALUE;
				}*/
				
				//v*=0.99998;
				//v+=0.00001;
				
				if (href.containsKey(u)){
					if(v<min){
						v=min;
					}
					like+=Math.log(v);
				}
				else{
					if(v>(1.0-min)){
						v=1.0-min;
					}
					like+=Math.log(1.0-v);
				}
			//}
		}
		if(allUsers.size()>0){
			like/=allUsers.size();
			//like*=10000;
		}
		if(Double.isInfinite(like)){
			like=Math.log(0.00000000000000000001);
		}
		return(like);
	}
	
	
	public double getScoreForItLikeTer(HashMap<String,Double> hconta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		
		double like=0;
		double min=0.0000001;
		for(String u:allUsers){
			//if ((!ignoreInit) || (!init.containsKey(u))){
				Double v=hconta.get(u);
				v=(v==null)?0.0:v;
				/*if(v<Double.MIN_VALUE){
					v=Double.MIN_VALUE;
				}
				if(v>(1.0-Double.MIN_VALUE)){
					v=1.0-Double.MIN_VALUE;
				}*/
				
				//v*=0.99998;
				//v+=0.00001;
				
				if (href.containsKey(u)){
					if(v<min){
						v=min;
					}
					like+=Math.log(v);
				}
				else{
					if(v>(1.0-min)){
						v=1.0-min;
					}
					like+=Math.log(1.0-v);
				}
			//}
		}
		if(allUsers.size()>0){
			like/=allUsers.size();
			//like*=10000;
		}
		if(Double.isInfinite(like)){
			like=Math.log(0.00000000000000000001);
		}
		return(like);
	}
	public double getScoreForItSum(HashMap<String,Double> hconta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		
		double like=0;
		
		for(String u:allUsers){
			//if ((!ignoreInit) || (!init.containsKey(u))){
				Double v=hconta.get(u);
				v=(v==null)?0.0:v;
				/*if(v<Double.MIN_VALUE){
					v=Double.MIN_VALUE;
				}
				if(v>(1.0-Double.MIN_VALUE)){
					v=1.0-Double.MIN_VALUE;
				}*/
				
				
				
				if (href.containsKey(u)){
					
					like+=v; //*v;
				}
				else{
					
					like+=(1.0-v); //*(1.0-v);
				}
			//}
		}
		if(allUsers.size()>0){
			like/=allUsers.size();
			//like*=10000;
		}
		
		return(like);
	}
	
	public double getScoreForItSquaredError(HashMap<String,Double> hconta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		
		double like=0;
		
		for(String u:allUsers){
			//if ((!ignoreInit) || (!init.containsKey(u))){
				Double v=hconta.get(u);
				v=(v==null)?0.0:v;
				/*if(v<Double.MIN_VALUE){
					v=Double.MIN_VALUE;
				}
				if(v>(1.0-Double.MIN_VALUE)){
					v=1.0-Double.MIN_VALUE;
				}*/
				
				
				
				if (href.containsKey(u)){
					
					like+=(1.0-v)*(1.0-v);
				}
				else{
					
					like+=v*v;
				}
			//}
		}
		if(allUsers.size()>0){
			like/=allUsers.size();
			//like*=10000;
		}
		
		return(like);
	}
	
	@Override
	public Result eval(Hyp hyp) {
		Cascade c=hyp.getStruct().getCascade();
		TreeMap<Long,HashMap<String,Double>> init=hyp.getInit(); //pstruct.getInitContaminated();
		TreeMap<Long,HashMap<String,Double>> ref=hyp.getRef(); //pstruct.getContaminated();
		ArrayList<TreeMap<Long,HashMap<String,Double>>> contaminations=hyp.getContaminations();
		
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
		
		Result res=new Result("logLikelihood","Cascade_"+c.getID());
		res.addScore(this.getName(), getScoreForItLike(sumContaminated,ref,init));
		res.addScore(this.getName()+"_bis", getScoreForItLikeBis(sumContaminated,ref,init));
		res.addScore(this.getName()+"_ter", getScoreForItLikeTer(sumContaminated,ref,init));
		res.addScore("avProbas", getScoreForItSum(sumContaminated,ref,init));
		res.addScore("squaredError", getScoreForItSquaredError(sumContaminated,ref,init));
		res.addScore("perplexity", Math.pow(2, -getScoreForItPerplexity(sumContaminated,ref,init)));
		return res;
	}

	

}
