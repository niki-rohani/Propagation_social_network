package thibault.dynamicCollect;
import thibault.dynamicCollect.*;

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
import java.util.Iterator;

import core.Post;
import core.User;
import experiments.Result;

import java.util.ArrayList;

public class DynamicCollectLive extends DynamicCollect {

	long nbIt=0;
	String initFile;
	
	public DynamicCollectLive(Streamer streamer, Policy selectPolicy, int nbArms, long t){
		super(streamer, selectPolicy, nbArms, t);
		this.initFile="./src/thibault/initLiveStreamer.txt";
	}
	
	public DynamicCollectLive(Streamer streamer, Policy selectPolicy, int nbArms, long t,String initFile){
		super(streamer, selectPolicy, nbArms, t);
		this.initFile=initFile;
	}
	
	public void setMaxT(long maxIt){
		this.maxIt=maxIt;
	}
	
	public void reinit(){
		this.arms=new HashSet<Arm>();
		this.armNames=new HashSet<String>();
		policy.reinitPolicy();
		nbIt=0;
	}
	
	public void initUserSet(String fileName){
		
		InputStream ips=null;
		try {
			ips = new FileInputStream(fileName);
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
				policy.nbPlayed=(int)nbIt;
				currentLine++;
			}
			else{
				String[] str = ligne.split("\t") ;
				armNames.add(str[0]);
				Arm a=new UserArm(str[0]);
				a.numberPlayed=Integer.parseInt(str[1]);
				a.sumRewards=Double.parseDouble(str[2]);
				//System.out.println(str[0]);
				a.sumSqrtRewards=Double.parseDouble(str[3]);
				arms.add(a);
				policy.addArm(a);
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
		 	for(Arm a:arms){
				 out.write(a.getName()+"\t"+a.numberPlayed+"\t"+a.sumRewards+"\t"+a.sumSqrtRewards+"\n");	 
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
		initUserSet(initFile);
		
		long freqRecords=record.getFreqRecords();
		this.rewardFunction=record.getRewardFunction();
		System.out.println("go !");
		boolean ok=true;
		
		

		
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
			HashSet<String> users=new HashSet<String>(); ///juste ca a modifier, peut etre mettre des id et caster avec des long mais on s en branle pour l instant
			for(Arm a:selectedArms){
				users.add(a.getName());
			}

			
			HashSet<Post> posts=streamer.streamWhileT(users, t);
			
			
			if(posts==null){
				ok=false;
				break;
			}

			if(armNames.size()<500000){
				HashSet<String> armNamesStreamer=this.streamer.getArmNames();
				
				for(String s:armNamesStreamer){
					if(!armNames.contains(s)){
						armNames.add(s);
						Arm a =new UserArm(s);
						arms.add(a);
						policy.addArm(a);
					}
				}
			}
			
		
			for(Arm a:selectedArms){
				a.computeReward(this.rewardFunction, posts);
			}


			System.out.println("Nombre de bras au total: "+arms.size());
			System.out.println("Nombre de bras selectionnes: "+selectedArms.size());
			
			try {
				saveState("./src/thibault/savedStreamed"+this.toString()+".txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			/*for(Arm a:arms){
				System.out.println("Id: "+a.getName()+"   nbPlayed: "+a.numberPlayed+"   sumRwd: "+a.sumRewards+"   sumSqrtRwd: "+a.sumSqrtRewards);
			}*/
			

			
			
			
			
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
	}
	
	
	
	/*public static void main(String[] args){
		Streamer streamer=new LiveStatusStream();
		Policy pol=new UCB();

		Reward reward=new testReward();
		
		DynamicCollectLive dc=new DynamicCollectLive(streamer,pol,2,80);
		
		
	    long users[] = {
	    		8350912, //le figaro
	    		1367531, //Fox
	    		759251,  //CNN
	    		742143, //BBC
	    		24744541  //le monde
	    };

		
		for (int i=0;i<users.length;i++){
			String u=Long.toString(users[i]);
			dc.armNames.add(u);
			Arm a=new UserArm(u);
			dc.arms.add(a);
			dc.policy.addArm(a);
		}
		dc.run(reward);
	}*/
}

