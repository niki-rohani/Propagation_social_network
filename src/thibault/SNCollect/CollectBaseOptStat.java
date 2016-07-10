package thibault.SNCollect;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import core.Post;
import core.User;


public class CollectBaseOptStat extends CollectBase {

	protected HashSet<String> bestArmNames;

	public CollectBaseOptStat(Streamer streamer, PolicyBase selectPolicy,int nbArms, long t) {
		super(streamer, selectPolicy, nbArms, t);
		this.policy=new OptimalStationnaire();
		this.bestArmNames=new HashSet<String>();
	}

	public class scoreComparatorGlob implements Comparator<Arm>
	{	
		public int compare(Arm arm1,Arm arm2){
			double r1=arm1.sumRewards;
			double r2=arm2.sumRewards;
			if(r1>r2){
				return -1;
			}
			if(r1<r2){
				return 1;
			}		
			return 0;
		}
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
		long nbIt=0;
		this.bestArmNames=new HashSet<String>();
		BestArmFinder bestArmFinder=new BestArmFinder();

		while(ok){
			
			

			Post.reinitPosts();
			User.reinitUsers();

			if((maxIt>0) && (nbIt>maxIt)){
				break;
			}

			bestArmFinder.select(nbArms);
			HashSet<String> users=new HashSet<String>();
			for(Arm a:bestArmFinder.lastSelected){
				users.add(a.getName());
			}
			HashSet<Post> posts=streamer.streamWhileT(users, t);
			if(posts==null){
				ok=false;
				break;
			}


			// Eventually adds new users from the stream
			for(Post p:posts){
				if(!armNames.contains(p.getOwner().getName())){
				//if(armNames.size()<100 && !armNames.contains(p.getOwner().getName())){
					String u=p.getOwner().getName();
					armNames.add(u);
					Arm a=new Arm(u);
					bestArmFinder.arms.add(a);
				}
			}
			//System.out.println(bestArmFinder.lastSelected.size());
			//Compute te rewards
			for(Arm a:bestArmFinder.lastSelected){
				a.computeReward(rewardFunction, posts);
			}

			bestArmFinder.updateParameters();

			nbIt++;
		}
		Collections.sort(bestArmFinder.arms,new scoreComparatorGlob());

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

			Post.reinitPosts();
			User.reinitUsers();

			if((freqRecords>0) && (nbIt%freqRecords==0)){
				HashMap<String,Double> rarms=new HashMap<String,Double>();
				HashMap<String,Integer> narms=new HashMap<String,Integer>();

				for(Arm a:policy.arms){
					rarms.put(a.getName(),a.sumRewards);
					narms.put(a.getName(),a.numberPlayed);
				}
				record.record(nbIt,rarms,narms);
			}

			if((maxIt>0) && (nbIt>maxIt)){
				break;
			}

			policy.select(nbArms);
			HashSet<String> users=new HashSet<String>();
			for(Arm a:policy.lastSelected){
				users.add(a.getName());
			}
			HashSet<Post> posts=streamer.streamWhileT(users, t);
			if(posts==null){
				ok=false;
				break;
			}


			// Eventually adds new users from the stream
			for(Post p:posts){
				if(armNames.size()<100 && !armNames.contains(p.getOwner().getName())){
				//if(!armNames.contains(p.getOwner().getName())){
					String u=p.getOwner().getName();
					armNames.add(u);
					Arm a=new Arm(u);
					policy.arms.add(a);
				}
			}

			//Compute te rewards
			//System.out.println(policy.lastSelected.size());
			for(Arm a:policy.lastSelected){
				a.computeReward(rewardFunction, posts);
			}

			policy.updateParameters();


			nbIt++;
		}

		HashMap<String,Double> rarms=new HashMap<String,Double>();
		HashMap<String,Integer> narms=new HashMap<String,Integer>();

		for(Arm a:this.policy.arms){
			rarms.put(a.getName(),a.sumRewards);
			narms.put(a.getName(),a.numberPlayed);
		}
		record.record(nbIt,rarms,narms);

		try {
			saveState("./savedStateHybridNormalAlg.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



}
