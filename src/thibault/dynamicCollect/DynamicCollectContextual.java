package thibault.dynamicCollect;

import java.awt.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import core.Post;
import core.User;
import experiments.Result;

import java.util.ArrayList;

import jgibblda.Inferencer;
import jgibblda.LDACmdOption;

import org.la4j.inversion.GaussJordanInverter;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;

import thibault.dynamicCollect.Minimax.scoreComparator;
import thibault.simulationBandit.ThompsonBeta;
public class DynamicCollectContextual extends DynamicCollect {
	protected PolicyContextual policyContext;
	protected Matrix convMat ;
	int nbConcept;
	boolean seeAll;
	double gamma; //discount
	protected Inferencer inferencer;

	/*public DynamicCollect(Streamer streamer, Policy selectPolicy, int nbArms, long t){
		this(streamer,selectPolicy,new HashSet<Arm>(),nbArms,t);
	}*/
	public DynamicCollectContextual (Streamer streamer, PolicyContextual policyContext, int nbArms, long t, double gamma, boolean seeAll) throws NumberFormatException, IOException{
		super(streamer, null, nbArms, t);
		this.gamma=gamma;
		this.seeAll=seeAll;
		this.policyContext=policyContext;
		this.nbConcept=policyContext.sizeFeatures;
		//this.createConceptMatrix();
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
	
	public void reinit(){
		this.arms=new HashSet<Arm>();
		this.armNames=new HashSet<String>();
		streamer.reinitStreamer();
		policyContext.reinitPolicy();
	}

	public String toString()
	{
		return "DynamicCollect_"+streamer+"_"+policyContext+"_nbArms="+nbArms+"_t="+t;
	}
	

	
	/*public void createConceptMatrix() throws NumberFormatException, IOException{
		InputStream ips=null;
		Basic2DMatrix U = new Basic2DMatrix(new double[2000][2000]);
		Basic2DMatrix D = new Basic2DMatrix(new double[2000][1000]);
		//Basic2DMatrix Vk = new Basic2DMatrix(new double[1000][1000]);
		Matrix invDk ;
		//Matrix convMat ;
		try {
			ips = new FileInputStream("lsaU");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String ligne;

		
		int i = 0;
		while ((ligne=br.readLine())!=null){
			ligne=ligne.replace(",",".");
			ligne=ligne.replace("[ ","[");
			ligne=ligne.replace(" ]","]");
			ligne=ligne.replace("[","");
			ligne=ligne.replace("]","");
			ligne=ligne.replace("  "," ");
			String[] values=ligne.split(" ");
			for(int j=0;j<values.length;j++){
				U.set(i, j, new Double(values[j]));	
			}
			i++;
		}
		br.close();
		ipsr.close();
		ips.close();
		
		try {
			ips = new FileInputStream("lsaD");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		 ipsr=new InputStreamReader(ips);
		 br=new BufferedReader(ipsr);
		
		i = 0;
		while ((ligne=br.readLine())!=null){
			ligne=ligne.replace(",",".");
			ligne=ligne.replace("[ ","[");
			ligne=ligne.replace(" ]","]");
			ligne=ligne.replace("[","");
			ligne=ligne.replace("]","");
			ligne=ligne.replace("  "," ");
			String[] values=ligne.split(" ");
			for(int j=0;j<values.length;j++){
				D.set(i, j, new Double(values[j]));	
			}
			i++;
		}
		br.close();
		ipsr.close();
		ips.close();
		

		MatrixInverter inverter= new GaussJordanInverter(D.slice(0, 0,nbConcept, nbConcept));
		invDk=inverter.inverse();
		convMat=invDk.multiply(U.slice(0, 0, U.rows() ,nbConcept).transpose());	
		System.out.println(convMat);
	}*/
	
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


		HashSet<Post> posts=null;
		while(ok){
			
			
			if(seeAll==true){
				for(Arm a:arms){a.observeContext(posts,null,inferencer,gamma);}
				}
			
			else{
			int caseNumber=0;
			switch (caseNumber)
			{
			  case 0: //on met tout a jour
				  for(Arm a:arms){
						if(selectedArms.contains(a)){
							a.observeContext(posts,null,inferencer,gamma);
						}
						else{
							a.features=a.sumFeatures.divide(a.numberObs+1);
						}
					}
				    break; 
			  case 1: // on met a jour que quand on a tout

						  for(Arm a:arms){
								if(selectedArms.contains(a)){
									a.observeContext(posts,null,inferencer,gamma);
								}
								else{
									a.features=a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1);
								}
							} 
					  				    break;	
			  case 2:  //on met a jour tout partiellement juste pour theta
				  for(Arm a:arms){
						if(selectedArms.contains(a)){
							a.observeContext(posts,null,inferencer,gamma);
						}
						else{
							a.features=a.sumFeaturesOnObs.divide(a.numberPlayedOnObs+1);
						}
					}
					break;
			  case 3: //on met a jour tout partiellement juste pour beta
				  for(Arm a:arms){
						if(selectedArms.contains(a)){
							a.observeContext(posts,null,inferencer,gamma);
						}
						else{
							a.features=a.sumFeatures.divide(a.numberObs+1);
						}
					}
					break;
			  default: 
				}
			}
			

			Post.reinitPosts();
			User.reinitUsers();
			
		
			policyContext.updateScore();
			
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
			
			
			selectedArms=policyContext.select(nbArms);
			
			//System.out.println(nbIt+"   "+selectedArms.size());
			
			HashSet<String> users=new HashSet<String>();
			for(Arm a:selectedArms){
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
					String u=p.getOwner().getName();
					armNames.add(u);
					Arm a=new UserArm(u,nbConcept);
					arms.add(a);
					policyContext.addArm(a);
				}
			}
			
			for(Arm a:selectedArms){
				a.computeReward(this.rewardFunction, posts);
			}
			
			policyContext.updateMatrix();
			
			
			
			/*if(seeAll==true){
				for(Arm a:arms){a.observeContext(posts,null,inferencer,gamma);}
				}
			else{
				for(Arm a:arms){
					if(selectedArms.contains(a)){
						a.observeContext(posts,null,inferencer,gamma);
					}
					else{
						if(a.numberPlayed!=0){
							a.features=a.moyFeatures;
						}					
					}
					
					}
			}*/
			//System.out.println(nbIt);
			//for(Arm a:selectedArms){
			//	System.out.println(a.getName()+" "+a.numberPlayed);
			//}
			

			
			/*if(seeAll==true){
				for(Arm a:arms){
					a.observeContext(posts,null,inferencer,gamma);
					}
			}
			else{
				for(Arm a:arms){
					if(selectedArms.contains(a)){
						a.observeContext(posts,null,inferencer,gamma);
					}
					else{
							a.features=a.moyFeatures;
					}
					
					}
			}
			
			policyContext.updateScore();

			
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
			
			
			selectedArms=policyContext.select(nbArms);
			
			
			
			

			for(Arm a:selectedArms){
				a.computeReward(this.rewardFunction, posts);
			}
			
			policyContext.updateMatrix();

			Post.reinitPosts();
			User.reinitUsers();
			
			HashSet<String> users=new HashSet<String>();
			for(Arm a:selectedArms){
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
					String u=p.getOwner().getName();
					armNames.add(u);
					Arm a=new UserArm(u,nbConcept);
					arms.add(a);
					policyContext.addArm(a);
				}
			}*/
			
			

			nbIt++;
		}
	
		
		HashMap<String,Double> rarms=new HashMap<String,Double>();
		HashMap<String,Integer> narms=new HashMap<String,Integer>();
		
		for(Arm a:arms){
			rarms.put(a.getName(),a.getSumRewards());
			narms.put(a.getName(),a.getNumberPlayed());
		}
		record.record(nbIt,rarms,narms);
		
		
		//System.out.println();
		try {
			saveState("./savedContextTemp"+policyContext);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
