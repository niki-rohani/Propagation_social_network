package thibault.SNCollect;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import experiments.Result;
import experiments.ResultFile;

public class CollectRecorder {

	public long freqRecords=1;
	public long freqAffiche=1;
	protected TreeMap<Long, HashMap<String,Double>> sums; 
	protected TreeMap<Long, HashMap<String,Integer>> nbs;
	protected Reward reward;
	protected CollectEvalMeasureList measures;
	protected String modelName="";
	protected ResultFile resFile;
	protected TreeMap<Long, Result> results;
	protected long maxNumberRecords=1000;
	protected int nbRecords=0;
	
	public CollectRecorder(String modelName, Reward reward, int freqRecords,long maxNumberRecords){
		this.reward=reward;
		this.modelName=modelName;
		this.freqRecords=freqRecords;
		sums=new TreeMap<Long, HashMap<String,Double>>();
		nbs=new  TreeMap<Long, HashMap<String,Integer>>();
		results=new TreeMap<Long, Result>();
		this.maxNumberRecords=maxNumberRecords;
	}
	public CollectRecorder(String modelName, Reward reward, int freqRecords,long maxNumberRecords, CollectEvalMeasureList mes, ResultFile rf){
		this(modelName,reward,freqRecords,maxNumberRecords);
		this.measures=mes;
		this.resFile=rf;
	}
	
	public void setEvalMeasures(CollectEvalMeasureList mes){
		this.measures=mes;
	}
	
	public void setResultFile(ResultFile rf){
		this.resFile=rf;
	}
	
	public TreeMap<Long, HashMap<String,Double>> getSums(){
		return sums;
	}
	
	public TreeMap<Long, HashMap<String,Integer>> getNbs(){
		return nbs;
	}
	
	public long getFreqRecords(){
		return freqRecords;
	}
	public Reward getRewardFunction(){
		return reward;
	}
	
	public void record(long t,HashMap<String,Double> sumRewards, HashMap<String,Integer> nbPlayed){
		sums=new TreeMap<Long, HashMap<String,Double>>();
		nbs=new  TreeMap<Long, HashMap<String,Integer>>();
		
		sums.put(t, sumRewards);
		//nbs.put(t, nbPlayed);
		
		
		nbRecords++;
		if(measures!=null){
			if(nbRecords%freqAffiche==0){
				System.out.println("Eval de "+this.modelName+" temps t="+t+" reward = "+reward.toString());
				measures.verbose=1;
			}
			else{
				measures.verbose=0;
			}
			Result res=measures.eval(this,t);
			res.setDonnee(""+t);
			results.put(t, res);
			try{
				resFile.append(res);
			}
			catch(IOException e){
				throw new RuntimeException(e);
			}
		}
		
		
		// Removes one over two elements if the max number of records is reached
		if(sums.size()>maxNumberRecords){
			int i=0;
			HashSet<Long> asup=new HashSet<Long>();
			for(Long time:sums.keySet()){
				i++;
				if((i%2)==0){
					asup.add(time);
				}
			}
			
			for(Long time:asup){
				sums.remove(time);
				nbs.remove(time);
				//System.out.println(" suppression "+time);
			}
		}
		//System.out.println("record!!");
	}
	
	public TreeMap<Long, Result> getResults(){
		return results;
	}
	
	public String getModelName(){
		return modelName;
	}
	
}
