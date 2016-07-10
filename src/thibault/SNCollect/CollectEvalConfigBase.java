package thibault.SNCollect;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class CollectEvalConfigBase {
	protected HashMap<String,String> pars;
	public CollectEvalConfigBase(){
		pars=new HashMap<String,String>();
		pars.put("freqRecords","1");
		pars.put("freqAffiche","1");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}

	public abstract ArrayList<CollectBase> getModels();
	public abstract CollectEvalMeasureList getMeasures();
	public abstract ArrayList<Reward> getRewards() throws FileNotFoundException, Exception;
	public HashMap<String,String> getParams(){return(pars);}
}


class LiveBase extends CollectEvalConfigBase{

	public String fileNameInit;
	public String fileNameKeyWords;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int compte;

	public LiveBase(String fileNameInit,String fileNameRwd,int idPolicy, int nbArms, int timeWindow, int compte){
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

	public ArrayList<CollectBase> getModels(){


		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();
		Streamer streamer=new StreamerLive(compte);

		PolicyBase pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new Random();
			mod=new CollectBaseLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			break;  
		case 1:
			pol = new CUCB();
			mod=new CollectBaseLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			break;   
		case 2:
			pol = new CUCBV();
			mod=new CollectBaseLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			break;
		case 3:
			pol = new ThompsonBernouilli();
			mod=new CollectBaseLive(streamer, pol,nbArms,timeWindow,fileNameInit);
			break;
		case 4:
			pol = new ThompsonPoisson();
			mod=new CollectBaseLive(streamer, pol,nbArms,timeWindow,fileNameInit);
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

		ModelLive l= new ModelLiveWords(words);
		reward=new ValModelLive(l);
		rewards.add(reward);

		return rewards;
	}
}

class LanguageBase extends CollectEvalConfigBase{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;

	public LanguageBase(String db,String fileName, int idPolicy, int nbArms, int timeWindow){
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
	public ArrayList<CollectBase> getModels(){
		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();

		Streamer streamer=new StreamerDataBase(db,"posts_1");
		PolicyBase pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new Random();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 1:
			pol = new CUCB();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;   
		case 2:
			pol = new CUCBV();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;
		case 3:
			pol = new ThompsonBernouilli();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 4:
			pol = new ThompsonPoisson();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 5:
			pol = new OptimalStationnaire();
			mod=new CollectBaseOptStat(streamer, pol,nbArms,timeWindow);
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
			if(i>=200 && i<300){
			ModelLanguage l= new ModelLanguageFromFile(ligne);
			reward=new ValModelLanguage(l);
			rewards.add(reward);
			}
			i++;
		}
		br.close();
		return rewards;
	}
}

class CounterNormBase extends CollectEvalConfigBase{

	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int countCase;
	public double coefTanH;
	CounterNormBase(String db, int idPolicy, int nbArms, int timeWindow, int countCase, double coefTanH){
		this.db=db;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.countCase=countCase;
		this.coefTanH=coefTanH;
		this.timeWindow=timeWindow;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<CollectBase> getModels(){
		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();

		Streamer streamer=new StreamerDataBase(db,"posts_1");
		PolicyBase pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new Random();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 1:
			pol = new CUCB();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;   
		case 2:
			pol = new CUCBV();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;
		case 3:
			pol = new ThompsonBernouilli();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 4:
			pol = new ThompsonPoisson();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 5:
			pol = new OptimalStationnaire();
			mod=new CollectBaseOptStat(streamer, pol,nbArms,timeWindow);
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
		ModelCount l = new ModelCount(countCase);
		reward=new ValModelCountNorm(l,coefTanH);
		rewards.add(reward);
		return rewards;
	}
}

class CounterBase extends CollectEvalConfigBase{

	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int countCase;
	CounterBase(String db, int idPolicy, int nbArms, int timeWindow, int countCase){
		this.db=db;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.countCase=countCase;
		this.timeWindow=timeWindow;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<CollectBase> getModels(){
		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();

		Streamer streamer=new StreamerDataBase(db,"posts_1");
		PolicyBase pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new Random();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 1:
			pol = new CUCB(106*106);
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;   
		case 2:
			pol = new CUCBV();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;
		case 3:
			pol = new ThompsonBernouilli();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 4:
			pol = new ThompsonPoisson();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 5:
			pol = new OptimalStationnaire();
			mod=new CollectBaseOptStat(streamer, pol,nbArms,timeWindow);
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
		ModelCount l = new ModelCount(countCase);
		reward=new ValModelCount(l);
		rewards.add(reward);
		return rewards;
	}
}

class SentimentBase extends CollectEvalConfigBase{

	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	SentimentBase(String db, int idPolicy, int nbArms, int timeWindow){
		this.db=db;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<CollectBase> getModels(){
		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();

		Streamer streamer=new StreamerDataBase(db,"posts_1");
		PolicyBase pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new Random();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 1:
			pol = new CUCB();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;   
		case 2:
			pol = new CUCBV();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;
		case 3:
			pol = new ThompsonBernouilli();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 4:
			pol = new ThompsonPoisson();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 5:
			pol = new OptimalStationnaire();
			mod=new CollectBaseOptStat(streamer, pol,nbArms,timeWindow);
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
		ModelSentiment l = new ModelSentiment();
		reward=new ValModelSentiment(l);
		rewards.add(reward);
		return rewards;
	}
}

class HybridBase extends CollectEvalConfigBase{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	HybridBase(String db,String fileName, int idPolicy, int nbArms, int timeWindow){
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
	public ArrayList<CollectBase> getModels(){
		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();

		Streamer streamer=new StreamerDataBase(db,"posts_1");
		PolicyBase pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new Random();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 1:
			pol = new CUCB();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;   
		case 2:
			pol = new CUCBV();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;
		case 3:
			pol = new ThompsonBernouilli();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 4:
			pol = new ThompsonPoisson();
			mod=new CollectBase(streamer, pol,nbArms,timeWindow);
			break;  
		case 5:
			pol = new OptimalStationnaire();
			mod=new CollectBaseOptStat(streamer, pol,nbArms,timeWindow);
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
		ModelCount l1 = new ModelCount(1);
		int i=0;
		while ((ligne=br.readLine())!=null){
			if(i>=200 && i<300){
			ModelLanguage l2= new ModelLanguageFromFile(ligne);
			reward=new ValModelHybrid(l2,l1);
			rewards.add(reward);	
			}
			i++;
		}

		br.close();
		return rewards;
	}
}

class LiveContext extends CollectEvalConfigBase{

	public String fileNameInit;
	public String fileNameKeyWords;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int compte;
	
	public LiveContext(String fileNameInit,String fileNameRwd,int idPolicy, int nbArms, int timeWindow, int compte){
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

public ArrayList<CollectBase> getModels(){


	ArrayList<CollectBase> mods=new ArrayList<CollectBase>();
	Streamer streamer=new StreamerLive(compte);

	PolicyBase pol;
	CollectBase mod=null;

	switch (idPolicy)
	{
	case 0:
		pol = new Random();
		mod=new CollectBaseLive(streamer, pol,nbArms,timeWindow,fileNameInit);
		break;  
	case 1:
		pol = new CUCB();
		mod=new CollectBaseLive(streamer, pol,nbArms,timeWindow,fileNameInit);
		break;   
	case 2:
		pol = new CUCBV();
		mod=new CollectBaseLive(streamer, pol,nbArms,timeWindow,fileNameInit);
		break;
	case 3:
		pol = new ThompsonBernouilli();
		mod=new CollectBaseLive(streamer, pol,nbArms,timeWindow,fileNameInit);
		break;
	case 4:
		pol = new ThompsonPoisson();
		mod=new CollectBaseLive(streamer, pol,nbArms,timeWindow,fileNameInit);
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

	ModelLive l= new ModelLiveWords(words);
	reward=new ValModelLive(l);
	rewards.add(reward);

	return rewards;
}
}

class LanguageContext extends CollectEvalConfigBase{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int sizeFeaturesInd;
	public int sizeFeaturesCom;
	public int caseContext;

	public LanguageContext(String db,String fileName, int idPolicy, int nbArms, int timeWindow, int sizeFeaturesInd, int sizeFeaturesCom,int caseContext){
		this.db=db;
		this.fileNameRwd=fileName;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		this.sizeFeaturesCom=sizeFeaturesCom;
		this.sizeFeaturesInd=sizeFeaturesInd;
		this.caseContext=caseContext;
		
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<CollectBase> getModels(){
		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();

		Streamer streamer=new StreamerDataBase(db,"posts_1");
		PolicyContext pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new CLinUCBCom(sizeFeaturesCom,0,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 1:
			pol = new CLinUCBInd(0,sizeFeaturesInd,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
			break;  
		case 2:
			pol = new CLinUCBHybrid(sizeFeaturesCom,sizeFeaturesInd,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,sizeFeaturesInd,caseContext);//cas 3 4 ou 5
			break;
		case 3:
			pol = new CLinThompsonCom(sizeFeaturesCom,0,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 4:
			pol = new CLinThompsonInd(0,sizeFeaturesInd,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
			break; 
		case 5:
			pol = new CPoissonThompsonCom(sizeFeaturesCom,0,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 6:
			pol = new CPoissonThompsonInd(0,sizeFeaturesInd,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
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
			if(i>=200 && i<300){
			ModelLanguage l= new ModelLanguageFromFile(ligne);
			reward=new ValModelLanguage(l);
			rewards.add(reward);
			}
			i++;
		}
		br.close();
		return rewards;
	}
}

class CounterNormContext extends CollectEvalConfigBase{

	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int countCase;
	public double coefTanH;
	public int sizeFeaturesInd;
	public int sizeFeaturesCom;
	public int caseContext;
	
	CounterNormContext(String db, int idPolicy, int nbArms, int timeWindow, int countCase, double coefTanH, int sizeFeaturesInd, int sizeFeaturesCom,int caseContext){
		this.db=db;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.countCase=countCase;
		this.coefTanH=coefTanH;
		this.timeWindow=timeWindow;
		this.sizeFeaturesCom=sizeFeaturesCom;
		this.sizeFeaturesInd=sizeFeaturesInd;
		this.caseContext=caseContext;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<CollectBase> getModels(){
		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();

		Streamer streamer=new StreamerDataBase(db,"posts_1");
		PolicyContext pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new CLinUCBCom(sizeFeaturesCom,0,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 1:
			pol = new CLinUCBInd(0,sizeFeaturesInd,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
			break;  
		case 2:
			pol = new CLinUCBHybrid(sizeFeaturesCom,sizeFeaturesInd,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,sizeFeaturesInd,caseContext);//cas 3 4 ou 5
			break;
		case 3:
			pol = new CLinThompsonCom(sizeFeaturesCom,0,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 4:
			pol = new CLinThompsonInd(0,sizeFeaturesInd,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
			break; 
		case 5:
			pol = new CPoissonThompsonCom(sizeFeaturesCom,0,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 6:
			pol = new CPoissonThompsonInd(0,sizeFeaturesInd,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
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
		ModelCount l = new ModelCount(countCase);
		reward=new ValModelCountNorm(l,coefTanH);
		rewards.add(reward);
		return rewards;
	}
}

class CounterContext extends CollectEvalConfigBase{

	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int countCase;
	public int sizeFeaturesInd;
	public int sizeFeaturesCom;
	public int caseContext;
	
	CounterContext(String db, int idPolicy, int nbArms, int timeWindow, int countCase, int sizeFeaturesInd, int sizeFeaturesCom,int caseContext){
		this.db=db;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.countCase=countCase;
		this.timeWindow=timeWindow;
		this.sizeFeaturesCom=sizeFeaturesCom;
		this.sizeFeaturesInd=sizeFeaturesInd;
		this.caseContext=caseContext;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<CollectBase> getModels(){
		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();

		Streamer streamer=new StreamerDataBase(db,"posts_1");
		PolicyContext pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new CLinUCBCom(sizeFeaturesCom,0,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 1:
			pol = new CLinUCBInd(0,sizeFeaturesInd,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
			break;  
		case 2:
			pol = new CLinUCBHybrid(sizeFeaturesCom,sizeFeaturesInd,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,sizeFeaturesInd,caseContext);//cas 3 4 ou 5
			break;
		case 3:
			pol = new CLinThompsonCom(sizeFeaturesCom,0,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 4:
			pol = new CLinThompsonInd(0,sizeFeaturesInd,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
			break; 
		case 5:
			pol = new CPoissonThompsonCom(sizeFeaturesCom,0,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 6:
			pol = new CPoissonThompsonInd(0,sizeFeaturesInd,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
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
		ModelCount l = new ModelCount(countCase);
		reward=new ValModelCount(l);
		rewards.add(reward);
		return rewards;
	}
}

class SentimentContext extends CollectEvalConfigBase{

	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int sizeFeaturesInd;
	public int sizeFeaturesCom;
	public int caseContext;

	SentimentContext(String db, int idPolicy, int nbArms, int timeWindow, int sizeFeaturesInd, int sizeFeaturesCom,int caseContext){
		this.db=db;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		this.sizeFeaturesCom=sizeFeaturesCom;
		this.sizeFeaturesInd=sizeFeaturesInd;
		this.caseContext=caseContext;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<CollectBase> getModels(){
		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();

		Streamer streamer=new StreamerDataBase(db,"posts_1");
		PolicyContext pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new CLinUCBCom(sizeFeaturesCom,0,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 1:
			pol = new CLinUCBInd(0,sizeFeaturesInd,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
			break;  
		case 2:
			pol = new CLinUCBHybrid(sizeFeaturesCom,sizeFeaturesInd,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,sizeFeaturesInd,caseContext);//cas 3 4 ou 5
			break;
		case 3:
			pol = new CLinThompsonCom(sizeFeaturesCom,0,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 4:
			pol = new CLinThompsonInd(0,sizeFeaturesInd,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
			break; 
		case 5:
			pol = new CPoissonThompsonCom(sizeFeaturesCom,0,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 6:
			pol = new CPoissonThompsonInd(0,sizeFeaturesInd,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
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
		ModelSentiment l = new ModelSentiment();
		reward=new ValModelSentiment(l);
		rewards.add(reward);
		return rewards;
	}
}

class HybridContext extends CollectEvalConfigBase{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int sizeFeaturesInd;
	public int sizeFeaturesCom;
	public int caseContext;
	
	HybridContext(String db,String fileName, int idPolicy, int nbArms, int timeWindow, int sizeFeaturesInd, int sizeFeaturesCom,int caseContext){
		this.db=db;
		this.fileNameRwd=fileName;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		this.sizeFeaturesCom=sizeFeaturesCom;
		this.sizeFeaturesInd=sizeFeaturesInd;
		this.caseContext=caseContext;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<CollectBase> getModels(){
		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();

		Streamer streamer=new StreamerDataBase(db,"posts_1");
		PolicyContext pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new CLinUCBCom(sizeFeaturesCom,0,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 1:
			pol = new CLinUCBInd(0,sizeFeaturesInd,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
			break;  
		case 2:
			pol = new CLinUCBHybrid(sizeFeaturesCom,sizeFeaturesInd,1.0,2.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,sizeFeaturesInd,caseContext);//cas 3 4 ou 5
			break;
		case 3:
			pol = new CLinThompsonCom(sizeFeaturesCom,0,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 4:
			pol = new CLinThompsonInd(0,sizeFeaturesInd,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
			break; 
		case 5:
			pol = new CPoissonThompsonCom(sizeFeaturesCom,0,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,0,2);
			break;  
		case 6:
			pol = new CPoissonThompsonInd(0,sizeFeaturesInd,1.0);
			mod=new CollectContext(streamer, null,nbArms,timeWindow,pol,0,sizeFeaturesInd,1);
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
		ModelCount l1 = new ModelCount(2);
		//int i=0;
		while ((ligne=br.readLine())!=null){
			//if(i>=200 && i<300){
			ModelLanguage l2= new ModelLanguageFromFile(ligne);
			reward=new ValModelHybrid(l2,l1);
			rewards.add(reward);	
			//}
			//i++;
		}

		br.close();
		return rewards;
	}
}


class HybridContextHidden extends CollectEvalConfigBase{
	public String fileNameRwd;
	public String db;
	public int idPolicy;
	public int nbArms;
	public int timeWindow;
	public int sizeFeaturesInd;
	public int sizeFeaturesCom;
	public int caseContext;
	
	HybridContextHidden(String db,String fileName, int idPolicy, int nbArms, int timeWindow, int sizeFeaturesInd, int sizeFeaturesCom,int caseContext){
		this.db=db;
		this.fileNameRwd=fileName;
		this.idPolicy=idPolicy;
		this.nbArms=nbArms;
		this.timeWindow=timeWindow;
		this.sizeFeaturesCom=sizeFeaturesCom;
		this.sizeFeaturesInd=sizeFeaturesInd;
		this.caseContext=caseContext;
		pars.put("freqRecords","100");
		pars.put("freqAffiche","1000");
		pars.put("maxT", "-1");
		pars.put("nbResultPoints", "10");
	}
	public ArrayList<CollectBase> getModels(){
		ArrayList<CollectBase> mods=new ArrayList<CollectBase>();

		Streamer streamer=new StreamerDataBase(db,"posts_1");
		PolicyContextHidden pol;
		CollectBase mod=null;

		switch (idPolicy)
		{
		case 0:
			pol = new HiddenCLinUCBHybrid(sizeFeaturesCom,1,1.0,2.0);
			mod=new CollectContextHidden(streamer, null,nbArms,timeWindow,pol,sizeFeaturesCom,1,5);
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
		ModelCount l1 = new ModelCount(1);
		int i=0;
		while ((ligne=br.readLine())!=null){
			if(i>=200 && i<300){
			ModelLanguage l2= new ModelLanguageFromFile(ligne);
			reward=new ValModelHybrid(l2,l1);
			rewards.add(reward);	
			}
			i++;
		}

		br.close();
		return rewards;
	}
}

