package thibault.SNCollect;

import java.util.HashMap;
import java.util.TreeMap;

import experiments.Result;

public abstract class CollectEvalMeasure {
	
	public int verbose=1;
	public abstract String getName();
	public abstract Result eval(CollectRecorder recorder, long t);
}

class MoyRewards extends CollectEvalMeasure {
	long from=0;
	public MoyRewards(long from){
		this.from=from;
	}
	public MoyRewards(){
		this(0);
	}
	public String getName(){
		return "MoyRewards_from="+from;
	}
	public Result eval(CollectRecorder recorder, long t){
		TreeMap<Long, HashMap<String,Double>> sums=recorder.getSums();
		TreeMap<Long, HashMap<String,Integer>> nbs=recorder.getNbs();
		
		HashMap<String,Double> ht=sums.get(t);
		HashMap<String,Double> htold=null;
		HashMap<String,Integer> nt=nbs.get(t);
		HashMap<String,Integer> ntold=null;
		double sumold=0.0;
		int nbsold=0;
		
		if(from>0){
			int n=0;
			htold=sums.get(t-from);
			ntold=nbs.get(t-from);
			
			while(htold==null){
				n++;
				if((from+n)>t){ 
					break;
				}
				//System.out.println(t+"=>"+(t-from-n));
				htold=sums.get(t-from-n);
				ntold=nbs.get(t-from-n);
				
			}
			if(htold!=null){
				//System.out.println(t+"=>"+(t-from-n));
				for(String v:htold.keySet()){
					sumold+=htold.get(v);
					nbsold+=ntold.get(v);
				}
			}
		}
		
		double sum=0.0;
		int nb=0;
		if(ht!=null){
			
			for(String v:ht.keySet()){
				sum+=ht.get(v);
				nb+=nt.get(v);
			}
		}
		
		sum-=sumold;
		nb-=nbsold;
		/*if(div>0){
			sum/=div;
		}*/
		if(nb>0){
			sum/=nb;
		}
		Result res=new Result(this.getName(),recorder.getRewardFunction().toString()+"_"+t);
		res.addScore(this.getName(), sum);
		return res;
	}
	
}

class sumRewards extends CollectEvalMeasure {
	long from=0;
	public sumRewards(long from){
		this.from=from;
	}
	public sumRewards(){
		this(0);
	}
	public String getName(){
		return "sumRewards_from="+from;
	}
	public Result eval(CollectRecorder recorder, long t){
		TreeMap<Long, HashMap<String,Double>> sums=recorder.getSums();
		
		HashMap<String,Double> ht=sums.get(t);
		HashMap<String,Double> htold=null;
		double sumold=0.0;
		//long div=t;
		
		if(from>0){
			int n=0;
			htold=sums.get(t-from);
			
			while(htold==null){
				n++;
				if((from+n)>t){ 
					break;
				}
				//System.out.println(t+"=>"+(t-from-n));
				htold=sums.get(t-from-n);
			}
			if(htold!=null){
				//System.out.println(t+"=>"+(t-from-n));
				for(String v:htold.keySet()){
					sumold+=htold.get(v);
				}
			}
			//div=from+n;	
		}
		
		double sum=0.0;
		if(ht!=null){	
			for(String v:ht.keySet()){
				sum+=ht.get(v);
			}
		}
		
		sum-=sumold;

		Result res=new Result(this.getName(),recorder.getRewardFunction().toString()+"_"+t);
		res.addScore(this.getName(), sum);
		return res;
	}
	
}
