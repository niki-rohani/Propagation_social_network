package thibault.dynamicCollect;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.TreeMap;








import core.Post;

import java.util.HashMap;

import core.User;

public abstract class CollectEvalConfig {
	protected HashMap<String,String> pars;
	public CollectEvalConfig(){
		pars=new HashMap<String,String>();
		pars.put("freqRecords","1");
		pars.put("freqAffiche","1");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
		
		
	}
	
	// returns the propagation models to evaluate associated to number of times these models should be run (for stochastic models)
	public abstract ArrayList<DynamicCollect> getModels();
	public abstract CollectEvalMeasureList getMeasures();
	public abstract ArrayList<Reward> getRewards() throws FileNotFoundException, Exception;
	public HashMap<String,String> getParams(){return(pars);}
}




class CollectEvalConfigLive extends CollectEvalConfig{
	
	public String fileNameInit;
	public String fileNameKeyWords;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int compte;
	
	CollectEvalConfigLive(String fileNameInit,String fileNameRwd,int idPolicy, int nbArms, int timeWindow, int compte){
		this.fileNameInit=fileNameInit;
		this.fileNameKeyWords=fileNameRwd;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		this.compte=compte;
		pars.put("freqRecords","1");
		pars.put("freqAffiche","1");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	
	public ArrayList<DynamicCollect> getModels(){
		
		
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();
		Streamer streamer=new LiveStatusStream(compte);
		
		Policy pol;
		DynamicCollect mod=null;
		
		switch (idPolicy)
		{
		  case 0:
			    pol = new RandomPolicy();
			    mod=new DynamicCollectLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			    break;  
		  case 1:
			  	pol = new UCBMod();
			  	mod=new DynamicCollectLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			  	break;   
		  case 2:
			    pol = new UCBMod(0.5);
			    mod=new DynamicCollectLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			    break;
		  case 3:
			    pol = new UCBMod(0.2);
			    mod=new DynamicCollectLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			    break;  
		  case 4:
			    pol = new UCBMod(0.1);
			    mod=new DynamicCollectLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			    break; 
		  case 5:
			    pol = new UCBVMod();
			    mod=new DynamicCollectLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			    break;  
		  case 6:
			    pol = new Minimax();
			    mod=new DynamicCollectLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			    break;  
		  case 7:
			    pol = new Thompson();
			    mod=new DynamicCollectLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			    break;   
		  default:            
		}
	
		mods.add(mod);

		return(mods);
	}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	public ArrayList<Reward> getRewards() throws IOException{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
			HashSet<String> words = new HashSet<String>();
			InputStream ips=new FileInputStream(fileNameKeyWords); 
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String ligne;
			while ((ligne=br.readLine())!=null){
				words.add(ligne);
			}
			br.close();
			
			rewardLive l= new rewardLiveWords(words);
			reward=new valRewardLive(l);
		//RTandReplyToModel l= new RTandReplyToModel1();
		//reward=new valRTandReplyTo(l);
			rewards.add(reward);
		
		return rewards;
	}
	
}

class CollectEvalConfigFromFileRandomTweet extends CollectEvalConfig{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	
	CollectEvalConfigFromFileRandomTweet(String db,String fileName, int idPolicy, int nbArms, int timeWindow){
		this.db=db;
		this.fileNameRwd=fileName;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<DynamicCollect> getModels(){
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();
		
		//Streamer streamer=new DataBaseStreamer("usElections5000_hashtag","posts_1");
		
		Streamer streamer=new DataBaseStreamer(db,"posts_1");
		Policy pol;
		DynamicCollect mod=null;
		
		switch (idPolicy)
		{
		  case 0:
			    pol = new RandomPolicy();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 1:
			  	pol = new UCBMod();
			  	mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			  	break;   
		  case 2:
			    pol = new UCBMod(0.5);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;
		  case 3:
			    pol = new UCBMod(0.2);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 4:
			    pol = new UCBMod(0.1);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break; 
		  case 5:
			    pol = new UCBMod(0.005);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break; 
		  case 6:
			  
			    pol = new UCBMod(0.8);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break; 
		  case 7:
			    pol = new UCBVMod();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 8:
			    pol = new Minimax();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 9:
			    pol = new Thompson();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 10:
			    pol = new OptimalPolicyGlobal();
			    mod=new DynamicCollectGlobOpt(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 11:
			    pol = new playEveryArms();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  default:            
		}
	
		mods.add(mod);

		return(mods);
}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	public ArrayList<Reward> getRewards() throws Exception{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
		InputStream ips=new FileInputStream(fileNameRwd); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String ligne;
		int i=0;
		while ((ligne=br.readLine())!=null){
			if(i==17){
		LanguageModel l= new WeightsFromFileLanguageModel(ligne);
		reward=new valLangModel(l);
		rewards.add(reward);
		}
		i++;
		}
		br.close();
		return rewards;
	}
}

class CollectEvalConfigRT extends CollectEvalConfig{
	
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	CollectEvalConfigRT(String db, int idPolicy, int nbArms, int timeWindow){
		this.db=db;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<DynamicCollect> getModels(){
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();
		
		Streamer streamer=new DataBaseStreamer(db,"posts_1");
		Policy pol;
		DynamicCollect mod=null;
		
		switch (idPolicy)
		{
		  case 0:
			    pol = new RandomPolicy();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 1:
			  pol = new UCBMod();
			  mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			  break;   
		  case 2:
			    pol = new UCBMod(0.5);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;
		  case 3:
			    pol = new UCBMod(0.2);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 4:
			    pol = new UCBMod(0.1);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break; 
		  case 5:
			    pol = new UCBMod(0.0005);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break; 
		  case 6:
			    pol = new UCBMod(0.8);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break; 
		  case 7:
			    pol = new UCBVMod();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 8:
			    pol = new Minimax();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 9:
			    pol = new Thompson();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 10:
			    pol = new OptimalPolicyGlobal();
			    mod=new DynamicCollectGlobOpt(streamer, pol,nbArms,timeWindow);
			    break;  
		  default:            
		}
	
		mods.add(mod);

		return(mods);
}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	public ArrayList<Reward> getRewards() throws Exception{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
		RTandReplyToModel l = new RTandReplyToModel1();
		reward=new valRTandReplyTo(l);
		rewards.add(reward);

		return rewards;
	}
}

class CollectEvalConfignbRT extends CollectEvalConfig{
	
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	CollectEvalConfignbRT(String db, int idPolicy, int nbArms, int timeWindow){
		this.db=db;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<DynamicCollect> getModels(){
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();
		
		Streamer streamer=new DataBaseStreamer(db,"posts_1");
		Policy pol;
		DynamicCollect mod=null;
		
		switch (idPolicy)
		{
		  case 0:
			    pol = new RandomPolicy();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 1:
			  pol = new UCBMod();
			  mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			  break;   
		  case 2:
			    pol = new UCBVMod();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;   
		  case 3:
			    pol = new OptimalPolicyGlobal();
			    mod=new DynamicCollectGlobOpt(streamer, pol,nbArms,timeWindow);
			    break;  
			    
		  case 4:
			    pol = new OptimalPolicyLocal();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 5:
			    pol = new ThompsonPoisson();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break; 
			    
		  default:            
		}
	
		mods.add(mod);

		return(mods);
}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	public ArrayList<Reward> getRewards() throws Exception{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
		nbRetweetModel l = new nbRetweetModel();
		reward=new valNbRetweet(l);
		rewards.add(reward);

		return rewards;
	}
}


class CollectEvalConfigSentiment extends CollectEvalConfig{
	
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	CollectEvalConfigSentiment(String db, int idPolicy, int nbArms, int timeWindow){
		this.db=db;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<DynamicCollect> getModels(){
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();
		
		Streamer streamer=new DataBaseStreamer(db,"posts_1");
		Policy pol;
		DynamicCollect mod=null;
		
		switch (idPolicy)
		{
		  case 0:
			    pol = new RandomPolicy();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 1:
			  pol = new UCBMod(0.8);//Rto:106 RT:340 Lesdeux:
			  mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			  break;   
		  case 2:
			    pol = new UCBVMod();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;   
		  case 3:
			    pol = new OptimalPolicyGlobal();
			    mod=new DynamicCollectGlobOpt(streamer, pol,nbArms,timeWindow);
			    break;  
			    
		  case 4:
			    pol = new OptimalPolicyLocal();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 5:
			    pol = new Thompson();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break; 
			    
		  default:            
		}
	
		mods.add(mod);

		return(mods);
}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	public ArrayList<Reward> getRewards() throws Exception{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
		sentimentModel l = new sentimentModel();
		reward=new valSentiment(l);
		rewards.add(reward);
		return rewards;
	}
}

class CollectEvalConfigHybrid extends CollectEvalConfig{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	CollectEvalConfigHybrid(String db,String fileName, int idPolicy, int nbArms, int timeWindow){
		this.db=db;
		this.fileNameRwd=fileName;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<DynamicCollect> getModels(){
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();
		
		Streamer streamer=new DataBaseStreamer(db,"posts_1");
		Policy pol;
		DynamicCollect mod=null;
		
		switch (idPolicy)
		{
		  case 0:
			    pol = new RandomPolicy();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 1:
			  	pol = new UCBMod();
			  	mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			  	break;   
		  case 2:
			    pol = new UCBMod(0.5);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;
		  case 3:
			    pol = new UCBMod(0.2);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 4:
			    pol = new UCBMod(0.1);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break; 
			    
		  case 5:
			    pol = new UCBMod(0.0005);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break; 
			    
		  case 6:
			    pol = new UCBMod(0.8);
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break; 
		  case 7:
			    pol = new UCBVMod();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 8:
			    pol = new Minimax();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 9:
			    pol = new Thompson();
			    mod=new DynamicCollect(streamer, pol,nbArms,timeWindow);
			    break;  
		  case 10:
			    pol = new OptimalPolicyGlobal();
			    mod=new DynamicCollectGlobOpt(streamer, pol,nbArms,timeWindow);
			    break;  
		  default:            
		}
	
		mods.add(mod);

		return(mods);
		
}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	public ArrayList<Reward> getRewards() throws Exception{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
		InputStream ips=new FileInputStream(fileNameRwd); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String ligne;
		RTandReplyToModel l1 = new RTandReplyToModel1();
		int i=0;
		while ((ligne=br.readLine())!=null){
			if(i>=200 && i<300){
			LanguageModel l2= new WeightsFromFileLanguageModel(ligne);
			reward=new valHybrid(l1,l2);
			rewards.add(reward);	
			}
			i++;
		}
		
		br.close();
		return rewards;
	}
}



class CollectEvalConfigLanguageContextual extends CollectEvalConfig{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int nbFeatures;
	public double alpha;
	public double gamma;
	public boolean seeAll;
	CollectEvalConfigLanguageContextual(String db,String fileName, int idPolicy, int nbArms, int timeWindow, int nbFeatures, double alpha,double gamma,boolean seeAll){
		this.db=db;
		this.fileNameRwd=fileName;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		this.nbFeatures=nbFeatures;
		this.alpha=alpha;
		this.gamma=gamma;
		this.seeAll=seeAll;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","100");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<DynamicCollect> getModels(){
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();

		Streamer streamer=new DataBaseStreamer(db,"posts_1");

		DynamicCollectContextual mod=null;
		PolicyContextual pol;
		
		switch (idPolicy)
		{
		  case 0:
			  	pol = new LinUCB(nbFeatures,alpha);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break;  
		  case 1:
			  	pol = new HybridLinUCB(nbFeatures,alpha,3);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break; 
		  case 2:
			  	pol = new ThompsonSamplingLin(nbFeatures,alpha);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break; 
		  case 3:
			  	pol = new ThompsonSamplingHybridLin(nbFeatures,alpha);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break; 
			    
			    
		  default:            
		}
			  	
		


		mods.add(mod);

		return(mods);
}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	public ArrayList<Reward> getRewards() throws Exception{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
		InputStream ips=new FileInputStream(fileNameRwd); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String ligne;
		int i=0;
		while ((ligne=br.readLine())!=null &&i<300){
		//LanguageModel l= new WeightsFromFileLanguageModel("usElections5000_hashtag","stems_2",ligne);	
		if(i>=200 && i<300){
				LanguageModel l2= new WeightsFromFileLanguageModel(ligne);
				reward=new valLangModel(l2);
				rewards.add(reward);
			}
	
		i++;
		}
		rewards.remove(18);
		rewards.remove(19);
		rewards.remove(20);
		rewards.remove(63);
		br.close();
		return rewards;
	}
}

class CollectEvalConfigRTContextual extends CollectEvalConfig{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int nbFeatures;
	public double alpha;
	public double gamma;
	public boolean seeAll;
	CollectEvalConfigRTContextual(String db,String fileName, int idPolicy, int nbArms, int timeWindow, int nbFeatures, double alpha,double gamma, boolean seeAll){
		this.db=db;
		this.fileNameRwd=fileName;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		this.nbFeatures=nbFeatures;
		this.alpha=alpha;
		this.gamma=gamma;
		this.seeAll= seeAll;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<DynamicCollect> getModels(){
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();

		Streamer streamer=new DataBaseStreamer(db,"posts_1");

		DynamicCollectContextual mod=null;
		PolicyContextual pol;
		
		switch (idPolicy)
		{
		  case 0:
			  	pol = new LinUCB(nbFeatures,alpha);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break;  
		  case 1:
			  	pol = new HybridLinUCB(nbFeatures,alpha,1);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break;   
		  default:            
		}
		mods.add(mod);

		return(mods);
}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	
	public ArrayList<Reward> getRewards() throws Exception{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
		RTandReplyToModel l = new RTandReplyToModel1();
		reward=new valRTandReplyTo(l);
		rewards.add(reward);
		return rewards;
	}
}

class CollectEvalConfignbRTContextual extends CollectEvalConfig{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int nbFeatures;
	public double varLik;
	public double gamma;
	public boolean seeAll;
	CollectEvalConfignbRTContextual(String db,String fileName, int idPolicy, int nbArms, int timeWindow, int nbFeatures, double varLik,double gamma, boolean seeAll){
		this.db=db;
		this.fileNameRwd=fileName;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		this.nbFeatures=nbFeatures;
		this.varLik=varLik;
		this.gamma=gamma;
		this.seeAll= seeAll;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<DynamicCollect> getModels(){
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();

		Streamer streamer=new DataBaseStreamer(db,"posts_1");

		DynamicCollectContextual mod=null;
		PolicyContextual pol;
		
		switch (idPolicy)
		{
		  case 0:
			  	pol = new LinUCB(nbFeatures,varLik); //ici varLik est alpha
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break;  
		  case 1:
			  	pol = new HybridLinUCB(nbFeatures,varLik,0); //ici varLik est alpha
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break; 
		  case 2:
			  	pol = new ThompsonSamplingLin(nbFeatures,varLik);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break;    
		  case 3:
			  	pol = new ThompsonSamplingPoissonApproxGauss(nbFeatures,varLik);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break; 
		  default:            
		}
		mods.add(mod);

		return(mods);
}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	
	public ArrayList<Reward> getRewards() throws Exception{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
		nbRetweetModel l = new nbRetweetModel();
		reward=new valNbRetweet(l);
		rewards.add(reward);
		return rewards;
	}
}


class CollectEvalConfigSentimentContextual extends CollectEvalConfig{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int nbFeatures;
	public double varLik;
	public double gamma;
	public boolean seeAll;
	CollectEvalConfigSentimentContextual(String db,String fileName, int idPolicy, int nbArms, int timeWindow, int nbFeatures, double varLik,double gamma, boolean seeAll){
		this.db=db;
		this.fileNameRwd=fileName;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		this.nbFeatures=nbFeatures;
		this.varLik=varLik;
		this.gamma=gamma;
		this.seeAll= seeAll;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<DynamicCollect> getModels(){
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();

		Streamer streamer=new DataBaseStreamer(db,"posts_1");

		DynamicCollectContextual mod=null;
		PolicyContextual pol;
		
		switch (idPolicy)
		{
		  case 0:
			  	pol = new LinUCB(nbFeatures,varLik); //ici varLik est alpha
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break;  
		  case 1:
			  	pol = new HybridLinUCB(nbFeatures,varLik,0); //ici varLik est alpha
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break; 
		  case 2:
			  	pol = new ThompsonSamplingLin(nbFeatures,varLik);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break; 
		  case 3:
			  	pol = new ThompsonSamplingHybridLin(nbFeatures,varLik);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break; 
		  default:            
		}
		mods.add(mod);

		return(mods);
}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	
	public ArrayList<Reward> getRewards() throws Exception{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
		sentimentModel l = new sentimentModel();
		reward=new valSentiment(l);
		rewards.add(reward);
		return rewards;
	}
}

class CollectEvalConfigHybridContextual extends CollectEvalConfig{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int nbFeatures;
	public double alpha;
	public double gamma;
	public boolean seeAll;
	CollectEvalConfigHybridContextual(String db,String fileName, int idPolicy, int nbArms, int timeWindow, int nbFeatures, double alpha,double gamma,boolean seeAll){
		this.db=db;
		this.fileNameRwd=fileName;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		this.nbFeatures=nbFeatures;
		this.alpha=alpha;
		this.gamma=gamma;
		this.seeAll=seeAll;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","100");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<DynamicCollect> getModels(){
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();

		Streamer streamer=new DataBaseStreamer(db,"posts_1");

		DynamicCollectContextual mod=null;
		PolicyContextual pol;
		
		switch (idPolicy)
		{
		  case 0:
			  	pol = new LinUCB(nbFeatures,alpha);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break;  
		  case 1:
			  	pol = new HybridLinUCB(nbFeatures,alpha,1);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break; 
		  case 2:
			  	pol = new ThompsonSamplingLin(nbFeatures,alpha);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break; 
		  case 3:
			  	pol = new ThompsonSamplingHybridLin(nbFeatures,alpha);
			try {
				mod=new DynamicCollectContextual(streamer,pol,nbArms,timeWindow,gamma,seeAll);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			    break; 
			    
			    
		  default:            
		}
			  	
		


		mods.add(mod);

		return(mods);
}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	public ArrayList<Reward> getRewards() throws Exception{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
		InputStream ips=new FileInputStream(fileNameRwd); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String ligne;
		int i=0;
		RTandReplyToModel l1 = new RTandReplyToModel1();
		while ((ligne=br.readLine())!=null &&i<300){	
		//LanguageModel l= new WeightsFromFileLanguageModel("usElections5000_hashtag","stems_2",ligne);	
		if(i>=200 && i<300){
				LanguageModel l2= new WeightsFromFileLanguageModel(ligne);
				reward=new valHybrid(l1,l2);
				rewards.add(reward);	
			}

		i++;
		}
		rewards.remove(18);
		rewards.remove(19);
		rewards.remove(20);
		rewards.remove(63);
		br.close();
		return rewards;
	}
}


class CollectEvalConfigLanguageRelational extends CollectEvalConfig{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int percentageSeeds;
	public double rate;
	public int nbDim;
	public boolean parralel;
	CollectEvalConfigLanguageRelational(String db,String fileName, int idPolicy, int nbArms, int timeWindow, double rate, int nbDim, boolean parralel){
		this.db=db;
		this.fileNameRwd=fileName;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		this.rate=rate;
		this.nbDim=nbDim;
		this.parralel= parralel;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<DynamicCollect> getModels(){
		ArrayList<DynamicCollect> mods=new ArrayList<DynamicCollect>();
		Streamer streamer=new DataBaseStreamer(db,"posts_1");
		DynamicCollectRelational mod=null;
		PolicyRelational pol = new PolicyRelationalSimple(nbDim,parralel,rate);
		
		mod=new DynamicCollectRelational(streamer,pol,nbArms,timeWindow);
		mods.add(mod);
		return(mods);
}
	public CollectEvalMeasureList getMeasures(){
		ArrayList<CollectEvalMeasure> ev=new ArrayList<CollectEvalMeasure>();
		ev.add(new sumRewards());
		CollectEvalMeasureList mes=new CollectEvalMeasureList(ev);
		return(mes);
	}
	public ArrayList<Reward> getRewards() throws Exception{
		ArrayList<Reward> rewards=new ArrayList<Reward>();
		Reward reward;
		InputStream ips=new FileInputStream(fileNameRwd); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String ligne;
		int i=0;
		while ((ligne=br.readLine())!=null){
		LanguageModel l2= new WeightsFromFileLanguageModel(ligne);
		reward=new valLangModel(l2);
		rewards.add(reward);
		i++;
		}
		
		br.close();
		return rewards;
	}
}
