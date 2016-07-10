package thibault.dynamicCollect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import core.Post;
import core.User;

public class DynamicCollectGlobOpt extends DynamicCollect{

	protected HashSet<String> bestArmNames;

	public DynamicCollectGlobOpt(Streamer streamer, Policy selectPolicy,int nbArms, long t){
		super( streamer,  selectPolicy, nbArms,  t);
		this.policy=new OptimalPolicyGlobal();
		this.bestArmNames=new HashSet<String>();
	}
	
	
	public String toString()
	{
		return "DynamicCollectGlobOpt_"+streamer+"_"+policy+"_nbArms="+nbArms+"_t="+t;
	}
	
	public void run(Reward rewardFunction){
		run(new CollectRecorder(this.toString(),rewardFunction,0,100));
	}
	
	public void run(Reward rewardFunction,int freqRecords){
		run(new CollectRecorder(this.toString(),rewardFunction,freqRecords,100)); 
	}
	public void run(Reward rewardFunction,int freqRecords,long maxNumberRecords){
		run(new CollectRecorder(this.toString(),rewardFunction,freqRecords,maxNumberRecords)); 
	}
	
	public void run(CollectRecorder record){ 
		reinit();
		double maxRwd=0;
		this.bestArmNames=new HashSet<String>();
		long freqRecords=record.getFreqRecords();
		this.rewardFunction=record.getRewardFunction();
		BestGlobalArmFinder bestArmFinder=new BestGlobalArmFinder();
		System.out.println("go !");
		boolean ok=true;
		long nbIt=0;
		
		while(ok){
			
			bestArmFinder.updateRewards();
			if((maxIt>0) && (nbIt>maxIt)){
				break;
			}
			HashSet<Arm> selectedArms=bestArmFinder.select(nbArms);
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
					bestArmFinder.addArm(a);
				}
			}
			
			for(Arm a:selectedArms){
				a.computeReward(this.rewardFunction, posts);
			}
			
			Post.reinitPosts();
			User.reinitUsers();
			nbIt++;
		}
		
		for (int i=0;i<nbArms;i++){
			this.bestArmNames.add(bestArmFinder.arms.get(i).getName());
		}
		System.out.println(this.bestArmNames);
		
		
		reinit();
		this.policy.NameOtpArmToPlay=this.bestArmNames;
		System.out.println("go bis!");
		ok=true;
		nbIt=0;
		
		
		while(ok){
			
			policy.updateRewards();
			
			if((freqRecords>0) && (nbIt%freqRecords==0)){
				HashMap<String,Double> rarms=new HashMap<String,Double>();
				HashMap<String,Integer> narms=new HashMap<String,Integer>();
				
				for(Arm a:arms){
					rarms.put(a.getName(),a.getSumRewards());
					narms.put(a.getName(),a.getNumberPlayed());
				}
				record.record(nbIt,rarms,narms);
			}
			
			if((maxIt>0) && (nbIt>maxIt)){
				break;
			}
			
			HashSet<Arm> selectedArms=policy.select(nbArms);
			//System.out.println(selectedArms.size());
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
				//System.out.println(streamer.getT()+": "+a.getName()+" "+a.getNumberPlayed()+" "+(a.getSumRewards()/a.getNumberPlayed()));
				a.computeReward(this.rewardFunction, posts);
				maxRwd=Math.max(maxRwd, a.lastReward);
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
		
		try {
			saveState("./savedStateHybridNormalOpt.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(Arm a:arms){
			System.out.println(a.getName()+" "+a.getNumberPlayed()+" "+a.getSumRewards());
		}
		System.out.println("maxRwd "+maxRwd);
      }
	}
