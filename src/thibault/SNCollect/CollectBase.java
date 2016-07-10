package thibault.SNCollect;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import core.Post;
import core.User;


public class CollectBase {
	protected PolicyBase policy;
	protected Reward rewardFunction;
	protected Streamer streamer;
	protected HashSet<String> armNames;
	protected long t;
	protected int nbArms=10;
	protected long maxIt=-1;

	public CollectBase(Streamer streamer, PolicyBase selectPolicy, int nbArms, long t){
		this.streamer=streamer;
		this.policy=selectPolicy;
		this.nbArms=nbArms;
		this.t=t;
		this.armNames=new HashSet<String>();
	}

	public void setMaxT(long maxIt){
		this.maxIt=maxIt;
	}

	public void reinit(){
		this.armNames=new HashSet<String>();
		policy.reinitPolicy();
		streamer.reinitStreamer();
	}

	public String toString(){
		return "DynamicCollect_"+streamer+"_"+policy+"_nbArms="+nbArms+"_t="+t;
	}

	public void saveState(String fileName) throws IOException{

		FileWriter fw=null;
		try {
			fw = new FileWriter(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		BufferedWriter out = new BufferedWriter(fw);

		for(Arm a:policy.arms){
			out.write(a.getName()+"\t"+a.numberPlayed+"\t"+a.sumRewards+"\n");	 
		}

		out.close();
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
		HashSet<Post> posts=null;
		
		//String[] armNamesTest = new String[] 
				//{"EntMagazine","	hughhewitt","	RiIeyJokess","	JeffersonObama","	Damienfaitlcon","	maryclimer","	AristeguiOnline","	OFA_FL","WashingtonDCTea","	JimPethokoukis","	GOPBlackChick","	RBPundit","	ThePlumLineGS","	grimshawarma","	mckaycoppins","	Mittisms","	DawnRiseth","	NolteNC","	ObamaDoctrine12","	Maxime_Medard","	JoeBiden","	OBAMA_GAMES","	thesavvy","	thehill","	PatDollard","	GaltsGirl","	Slate","	DebWilliams57","	NancyWonderful","	foxnation","	dmataconis","	AliVelshi","	ElizabethYate11","	KatyinIndy","	DylanChatelain","	adrian_gray","	dennygirltwo","	WestWingReport","	Norsu2","	FloridaJayhawk","	imsure","	patricionavia","	NoticiasCaracol","	HuffPostPol","	slone","	RevRichardColes","	TPO_Hisself","	cleo54123","	NTN24","ColorMeRed"};

		while(ok){

			policy.updateScores();
			
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
			posts=streamer.streamWhileT(users, t);
			if(posts==null){
				ok=false;
				break;
			}


			// Eventually adds new users from the stream
			for(Post p:posts){
				if(!armNames.contains(p.getOwner().getName())){
				//if(armNames.size()<100 && !armNames.contains(p.getOwner().getName())){
				//for(int i = 0;i<armNamesTest.length;i++){
					//if(armNamesTest[i].equals(p.getOwner().getName())){
							String u=p.getOwner().getName();
							armNames.add(u);
							Arm a=new Arm(u);
							policy.arms.add(a);
						//}
					//}
				}
			}

			//Compute te rewards
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
