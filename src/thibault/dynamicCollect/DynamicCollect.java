package thibault.dynamicCollect;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import core.Post;
import core.User;
import experiments.Result;

import java.util.ArrayList;

import thibault.simulationBandit.ThompsonBeta;
public class DynamicCollect {
	protected Policy policy;
	protected Reward rewardFunction;
	protected Streamer streamer;
	protected HashSet<Arm> arms;
	protected HashSet<String> armNames;
	protected long t;
	protected int nbArms=10;
	protected long maxIt=-1;
	/*public DynamicCollect(Streamer streamer, Policy selectPolicy, int nbArms, long t){
		this(streamer,selectPolicy,new HashSet<Arm>(),nbArms,t);
	}*/
	public DynamicCollect(Streamer streamer, Policy selectPolicy, int nbArms, long t){
		this.streamer=streamer;
		this.policy=selectPolicy;
		//this.rewardFunction=rewardFunction;
		//this.arms=initArms;
		this.arms=new HashSet<Arm>();
		this.nbArms=nbArms;
		this.t=t;
		this.armNames=new HashSet<String>();
		/*for(Arm a:initArms){
			selectPolicy.addArm(a);
			armNames.add(a.getName());
		}*/
	}
	
	public void setMaxT(long maxIt){
		this.maxIt=maxIt;
	}
	
	public void reinit(){
		this.arms=new HashSet<Arm>();
		this.armNames=new HashSet<String>();
		policy.reinitPolicy();
		streamer.reinitStreamer();
	}
	
	public void saveState(String fileName) throws IOException{

		 FileWriter fw=null;
		try {
			fw = new FileWriter(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		 BufferedWriter out = new BufferedWriter(fw);
		 		
		 	for(Arm a:arms){
				 out.write(a.getName()+"\t"+a.numberPlayed+"\t"+a.sumRewards+"\n");	 
		 }

	out.close();
	}
	
	public String toString()
	{
		return "DynamicCollect_"+streamer+"_"+policy+"_nbArms="+nbArms+"_t="+t;
	}
	
	public void run(Reward rewardFunction){
		run(new CollectRecorder(this.toString(),rewardFunction,0,100)); //new ArrayList<CollectEvalMeasure>());
	}
	
	public void run(Reward rewardFunction,int freqRecords){
		run(new CollectRecorder(this.toString(),rewardFunction,freqRecords,1000)); 
	}
	public void run(Reward rewardFunction,int freqRecords,long maxNumberRecords){
		run(new CollectRecorder(this.toString(),rewardFunction,freqRecords,maxNumberRecords)); 
	}
	
	public void run(CollectRecorder record){ 
		reinit();
		
		long freqRecords=record.getFreqRecords();
		this.rewardFunction=record.getRewardFunction();
		System.out.println("go !");
		boolean ok=true;
		double sum=0;
		long nbIt=0;
		

		
		while(ok){
			
			policy.updateRewards();
			
			if((freqRecords>0) && (nbIt%freqRecords==0)){
				HashMap<String,Double> rarms=new HashMap<String,Double>();
				HashMap<String,Integer> narms=new HashMap<String,Integer>();
				
				for(Arm a:arms){
					rarms.put(a.getName(),a.getSumRewards());
					//narms.put(a.getName(),a.getNumberPlayed());
				}
				record.record(nbIt,rarms,narms);
			}
			
			if((maxIt>0) && (nbIt>maxIt)){
				break;
			}
			
			HashSet<Arm> selectedArms=policy.select(nbArms);
			HashSet<String> users=new HashSet<String>();
			for(Arm a:selectedArms){
				users.add(a.getName());
			}
			HashSet<Post> posts=streamer.streamWhileT(users, t);
			if(posts==null){
				ok=false;
				break;
			}
			
			
			// Eventually adds new users from the stream
			for(Post p:posts){
				//System.out.println(p);
				if(!armNames.contains(p.getOwner().getName())){
					String u=p.getOwner().getName();
					armNames.add(u);
					Arm a=new UserArm(u);
					arms.add(a);
					policy.addArm(a);
				}
			}
			
			
			for(Arm a:selectedArms){
				a.computeReward(this.rewardFunction, posts);
				if (policy instanceof Thompson){
					a.updateFactorsBetaGeneral();
					}
			}

			
			Post.reinitPosts();
			//User.reinitAllLinks();
			//User.reinitAllPosts();
			User.reinitUsers();
			
					
			nbIt++;
		}
		
		HashMap<String,Double> rarms=new HashMap<String,Double>();
		HashMap<String,Integer> narms=new HashMap<String,Integer>();
		
		for(Arm a:arms){
			rarms.put(a.getName(),a.getSumRewards());
			narms.put(a.getName(),a.getNumberPlayed());
		}
		record.record(nbIt,rarms,narms);
		
		/*for(Arm a:arms){
			System.out.println(a.getName()+" "+a.getNumberPlayed());
		}*/
		
		try {
			saveState("./savedStateHybridNormalAlg.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	/*public static void main(String[] args){
		Streamer streamer=new DataBaseStreamer("usElections5000_hashtag","posts_1");
		Policy pol=new UCB();
		HashSet<Op> or=new HashSet<Op>();
		or.add(new Word(3));
		or.add(new Word(4));
		
		Reward reward=new NbOkForBooleanModel(new BooleanModel(new Or(or)));
		HashSet<Arm> initArms=new HashSet<Arm>();
		//UserArm a1=new UserArm("Gangsta_Gossip");
		//initArms.add(a1);
		DynamicCollect dc=new DynamicCollect(streamer,pol,100,100);
		dc.run(reward);
	}*/
}
