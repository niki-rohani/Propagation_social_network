package thibault.dynamicCollect;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import core.Post;
import core.User;

public class DynamicCollectRelational extends DynamicCollect  {
	protected PolicyRelational policyRelational;
	int nbCoordinates;
	
	public DynamicCollectRelational(Streamer streamer, PolicyRelational policyRelational, int nbArms, long t) {
		super(streamer, null, nbArms, t);
		this.policyRelational=policyRelational;
		this.nbCoordinates=policyRelational.sizeFeatures;
	}
	
	public void reinit(){
		this.arms=new HashSet<Arm>();
		this.armNames=new HashSet<String>();
		streamer.reinitStreamer();
		policyRelational.reinitPolicy();
	}
	
	public void saveState(String fileName) throws IOException{

		 FileWriter fw=null;
		 fw = new FileWriter(fileName);
		 BufferedWriter out = new BufferedWriter(fw);
		 for(Arm a:arms){
				out.write(a.getName()+"\t"+a.numberPlayed+"\t"+a.sumRewards+"\t"+a.score+"\t"+a.features+"\n");	 
		 }
		 out.close();
	}
	
	public String toString()
	{
		return "DynamicCollect_"+streamer+"_"+policyRelational+"_nbArms="+nbArms+"_t="+t;
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
		this.reinit();

		long freqRecords=record.getFreqRecords();
		this.rewardFunction=record.getRewardFunction();
		System.out.println("go !");
		boolean ok=true;
		long nbIt=0;
		HashSet<Arm> selectedArms=new HashSet<Arm>();


		
		while(ok){
			
			policyRelational.updateScores();
			
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
			
			
			selectedArms=policyRelational.select(nbArms);
			
			System.out.println(nbIt+"   "+selectedArms.size());
			
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
				if(!armNames.contains(p.getOwner().getName())){
					String u=p.getOwner().getName();
					armNames.add(u);
					Arm a=new UserArm(u,nbCoordinates);
					arms.add(a);
					policyRelational.addArm(a);
				}
			}
			
			for(Arm a:selectedArms){
				a.computeReward(this.rewardFunction, posts);
			}

			policyRelational.updateCoordinates();
			
			/*for(Arm a:arms){
				a.initContext();
				}*/
			
			
			if(nbIt==5){
				try {
					this.saveState("./src/thibault/savedCoordinates");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
			Post.reinitPosts();
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
			this.saveState("./src/thibault/savedCoordinates");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
}
