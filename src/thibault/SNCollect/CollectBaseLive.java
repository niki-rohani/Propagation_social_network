package thibault.SNCollect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;

import core.Post;
import core.User;


public class CollectBaseLive extends CollectBase{

	protected String initFile;
	protected long nbIt=0;

	public CollectBaseLive(Streamer streamer, PolicyBase selectPolicy,int nbArms, long t) {
		super(streamer, selectPolicy, nbArms, t);
		this.initFile="./src/thibault/initLiveStreamer.txt";
	}

	public CollectBaseLive(Streamer streamer, PolicyBase selectPolicy,int nbArms, long t, String initFile) {
		super(streamer, selectPolicy, nbArms, t);
		this.initFile=initFile;
	}

	public void initUserSet(){

		InputStream ips=null;
		try {
			ips = new FileInputStream(initFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String ligne;
		int currentLine = 0 ;
		try {
			while ((ligne=br.readLine())!=null){
				if(currentLine==0){
					nbIt=Long.parseLong(ligne);
					policy.nbIt=(int)nbIt;
					currentLine++;
				}
				else{
					String[] str = ligne.split("\t") ;
					armNames.add(str[0]);
					Arm a=new Arm(str[0]);
					a.numberPlayed=Integer.parseInt(str[1]);
					a.sumRewards=Double.parseDouble(str[2]);
					//System.out.println(str[0]);
					a.sumProdRewards=Double.parseDouble(str[3]);
					policy.arms.add(a);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveState(String fileName) throws IOException{

		 FileWriter fw=null;
		try {
			fw = new FileWriter(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		 BufferedWriter out = new BufferedWriter(fw);
		 		
		 	out.write((int) nbIt+"\n");
		 	for(Arm a:policy.arms){
				 out.write(a.getName()+"\t"+a.numberPlayed+"\t"+a.sumRewards+"\t"+a.sumProdRewards+"\n");	 
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
		this.initUserSet();
		long freqRecords=record.getFreqRecords();
		this.rewardFunction=record.getRewardFunction();
		System.out.println("go !");
		boolean ok=true;
		long nbIt=0;

		while(ok){

			policy.updateParameters();

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
			if(armNames.size()<500000){
				HashSet<String> armNamesStreamer=this.streamer.getArmNames();
				
				for(String s:armNamesStreamer){
					if(!armNames.contains(s)){
						armNames.add(s);
						Arm a =new Arm(s);
						policy.arms.add(a);
					}
				}
			}

			//Compute te rewards
			for(Arm a:policy.lastSelected){
				a.computeReward(rewardFunction, posts);
			}

			System.out.println("Nombre de bras au total: "+policy.arms.size());
			System.out.println("Nombre de bras selectionnes: "+policy.lastSelected.size());
			
			try {
				saveState("./src/thibault/savedStreamed"+this.toString()+".txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Post.reinitPosts();
			User.reinitUsers();
			
			nbIt++;
		}

	}

}


