package thibault.SNCollect;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import jgibblda.Inferencer;
import jgibblda.LDACmdOption;
import core.Post;
import core.User;

public class CollectContext extends CollectBase{

	protected PolicyContext policyCtxt;
	protected int sizeFeaturesInd;
	protected int sizeFeaturesCom;
	protected int caseContext;
	protected Inferencer inferencer;
	
	public CollectContext(Streamer streamer, PolicyBase selectPolicy, int nbArms, long t, PolicyContext policyCtxt,int sizeFeaturesCom,int sizeFeaturesInd,int caseContext) {
		super(streamer, null, nbArms, t);
		this.policyCtxt=policyCtxt;
		this.caseContext=caseContext;
		this.sizeFeaturesCom=sizeFeaturesCom;
		this.sizeFeaturesInd=sizeFeaturesInd;
		LDACmdOption ldaOption = new LDACmdOption();
		ldaOption.est=false;
		ldaOption.estc=false;
		ldaOption.inf = true;
		//ldaOption.dir = "./src/thibault/modelBertin";
		ldaOption.dir = "./src/thibault/modelUS";
		ldaOption.niters = 20; 
		//ldaOption.modelName = "model-final-bertin";
		ldaOption.modelName = "model-final-US";
		this.inferencer = new Inferencer();
		this.inferencer.init(ldaOption);
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
	
	public void reinit(){
		this.armNames=new HashSet<String>();
		policyCtxt.reinitPolicy();
		streamer.reinitStreamer();
	}
	
	public void run(CollectRecorder record){ 
		reinit();
		long freqRecords=record.getFreqRecords();
		this.rewardFunction=record.getRewardFunction();
		System.out.println("go !");
		boolean ok=true;
		long nbIt=0;
		HashSet<Post> posts=null;
		
		while(ok){

			for(ArmContext a:policyCtxt.arms){
				a.observeContext(posts,inferencer);
				}
			

			Post.reinitPosts();
			User.reinitUsers();
			
			policyCtxt.updateScores();

			if((freqRecords>0) && (nbIt%freqRecords==0)){
				HashMap<String,Double> rarms=new HashMap<String,Double>();
				HashMap<String,Integer> narms=new HashMap<String,Integer>();

				for(ArmContext a:policyCtxt.arms){
					rarms.put(a.getName(),a.sumRewards);
					narms.put(a.getName(),a.numberPlayed);
				}
				record.record(nbIt,rarms,narms);
			}

			if((maxIt>0) && (nbIt>maxIt)){
				break;
			}

			policyCtxt.select(nbArms);
			
			HashSet<String> users=new HashSet<String>();
			for(ArmContext a:policyCtxt.lastSelected){
				users.add(a.getName());
			}
			posts=streamer.streamWhileT(users, t);
			if(posts==null){
				ok=false;
				break;
			}


			// Eventually adds new users from the stream
			for(Post p:posts){
				if(!armNames.contains(p.getOwner().getName())){
				//if(armNames.size()<100 &&!armNames.contains(p.getOwner().getName())){
					String u=p.getOwner().getName();
					armNames.add(u);
					ArmContext a=new ArmContext(u,sizeFeaturesCom,sizeFeaturesInd,caseContext);
					policyCtxt.arms.add(a);
				}
			}

			//Compute te rewards
			for(Arm a:policyCtxt.lastSelected){
				a.computeReward(rewardFunction, posts);
			}

			policyCtxt.updateParameters();


			nbIt++;
		}

		HashMap<String,Double> rarms=new HashMap<String,Double>();
		HashMap<String,Integer> narms=new HashMap<String,Integer>();

		for(ArmContext a:this.policyCtxt.arms){
			rarms.put(a.getName(),a.sumRewards);
			narms.put(a.getName(),a.numberPlayed);
		}
		record.record(nbIt,rarms,narms);

		/*try {
			saveState("./savedStateHybridNormalAlg.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}


}
