package thibault.diggCollect;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

import jgibblda.Inferencer;
import jgibblda.LDACmdOption;
import propagationModels.MultiSetsPropagationStructLoader;
import propagationModels.PropagationStruct;
import utils.ValInHashMapComparator;

public class collectDiggTest {
	MultiSetsPropagationStructLoader loader;
	int nbMsgPerStep;
	Inferencer inferencer;
	String fileName;
	public BufferedWriter out;
	boolean firstUser=true;
	TreeMap<String,TreeMap<Integer,Double>> resultsRwd;
	TreeMap<String,TreeMap<Integer,Double>> resultsNbMess;
	int nbSim=1;
	boolean useLDA =true;
	
	
	public collectDiggTest(String db,String cascades,int nbMsgPerStep,int minNumberMsg) throws IOException{
		this.nbMsgPerStep=nbMsgPerStep;
		fileName="./src/thibault/resultsDigg"+nbMsgPerStep+"100featsThompson.txt";
		resultsRwd = new  TreeMap<String,TreeMap<Integer,Double>>();
		resultsNbMess = new  TreeMap<String,TreeMap<Integer,Double>>();
		loader=new MultiSetsPropagationStructLoader(db,cascades,1,"1.0","-1","1","-1");
		loader.computeFirsts=true;
		loader.load(); 
		HashMap<String,Integer> nbFirsts=new HashMap<String,Integer>(); 
       	HashMap<String,ArrayList<Integer>> liste= loader.getUserAsFirst();
       	LDACmdOption ldaOption = new LDACmdOption();
		ldaOption.est=false;
		ldaOption.estc=false;
		ldaOption.inf = true;
		ldaOption.dir = "./src/thibault/modelDigg/model100old";
		ldaOption.niters = 50; 
		ldaOption.modelName = "model-final-Digg";
		inferencer = new Inferencer();
		inferencer.init(ldaOption);
		FileWriter fw=null;
		fw = new FileWriter(fileName);
		out = new BufferedWriter(fw);
       	for(String u:liste.keySet()){
       		 nbFirsts.put(u,liste.get(u).size());
       		 
       	}
       	ArrayList<String> lu=new ArrayList<String>(nbFirsts.keySet());
       	Collections.sort(lu,new ValInHashMapComparator<String,Integer>(nbFirsts,true));

       	for(String u:lu){
       	  int nb=liste.get(u).size();
       	  if(nb<minNumberMsg) break;
       	  System.out.println(u+" : "+nb+" cascades in First Step");
       	  doExpeForUser(u,nb);
       	}
       	
       	String s="NbIt";
       	for(String pol:resultsRwd.keySet()){
       		s=s+"\t"+pol;
       	}
       	out.write(s+"\n");
       	
       	for(Integer i:resultsRwd.get("Random").keySet()){
       		s=i.toString();
       		for(String pol:resultsRwd.keySet()){
       			s=s+"\t"+resultsRwd.get(pol).get(i)/nbSim;
       		}
       		out.write(s+"\n");
       	}
       	out.write("\n");
       	
       	
       	s="NbIt";
       	for(String pol:resultsRwd.keySet()){
       		s=s+"\t"+pol;
       	}
       	out.write(s+"\n");
       	
       	for(Integer i:resultsRwd.get("Random").keySet()){
       		s=i.toString();
       		for(String pol:resultsRwd.keySet()){
       			s=s+"\t"+resultsNbMess.get(pol).get(i)*1.0/nbSim;
       		}
       		out.write(s+"\n");
       	}
       	out.write("\n"+"\n"+"\n"+"\n");
       	
    	/*s="NbIt";
       	for(String pol:resultsRwd.keySet()){
       		s=s+"\t"+pol;
       	}
       	out.write(s+"\n");
       	
       	for(Integer i:resultsRwd.get("Random").keySet()){
       		s=i.toString();
       		for(String pol:resultsRwd.keySet()){
       			s=s+"\t"+resultsNbMess.get(pol).get(i)*1.0/nbSim;
       		}
       		out.write(s+"\n");
       	}*/
       	
       	out.close();
        
	}
	public void doExpeForUser(String u,int nb) throws IOException{
		
		ArrayList<Integer> allCascades= loader.getUserAsFirst().get(u);
		
		int sizeFeatures = 100;
		
		
		ArrayList<PolicyDigg> pols = new ArrayList<PolicyDigg>();
		pols.add(new Random());
		//pols.add(new Optimal());
		for(int i = 0;i<nbSim;i++){
			pols.add(new Random());
			pols.add(new ThompsonLinCtxt(sizeFeatures, 0.5,2.0,false));
		    pols.add(new ThompsonPoissonCtxt(sizeFeatures, 0.5,2.0,false));
		    pols.add(new ThompsonLinCtxt(sizeFeatures, 0.5,2.0,true));
			pols.add(new ThompsonPoissonCtxt(sizeFeatures, 0.5,2.0,true));
		}

		/*pols.add(new LinUCB(sizeFeatures, 0.5,0.2,false));
		pols.add(new LinUCB(sizeFeatures, 0.5,0.5,false));
		pols.add(new LinUCB(sizeFeatures, 0.5,1.0,false));
		pols.add(new LinUCB(sizeFeatures, 0.5,2.0,false));
		pols.add(new LinUCB(sizeFeatures, 0.5,5.0,false));
		pols.add(new PoissonUCB(sizeFeatures, 0.5,0.2,false));
		pols.add(new PoissonUCB(sizeFeatures, 0.5,0.5,false));
		pols.add(new PoissonUCB(sizeFeatures, 0.5,1.0,false));
		pols.add(new PoissonUCB(sizeFeatures, 0.5,2.0,false));
		pols.add(new PoissonUCB(sizeFeatures, 0.5,5.0,false));*/
		
		
		if(firstUser){
			for(PolicyDigg pol:pols){
				resultsRwd.put(pol.toString(), new TreeMap<Integer,Double>() );
				resultsNbMess.put(pol.toString(), new TreeMap<Integer,Double>() );
			}
			firstUser=false;
		}
		
		/*out.write("UserId"+"\t"+u+"\t"+"NbCascades"+"\t"+nb+"\n");
		String s="";
		for(PolicyDigg pol:pols){
			s=s+"\t"+pol.toString();
		}
		out.write(s+"\n");
		s="";*/
		
		for(PolicyDigg pol:pols){
			int j=0;
			while(true){
				if((j+nbMsgPerStep)>allCascades.size()) break;
				ArrayList<TreeMap<Integer,Double>> msgs=new ArrayList<TreeMap<Integer,Double>>(); 
				ArrayList<Integer> nbFollows=new ArrayList<Integer>();
				for(int i=0;i<nbMsgPerStep;i++){
					int id=allCascades.get(j+i);
					PropagationStruct prop=loader.getCascades().get(id);
					TreeMap<Integer,Double> content=prop.getDiffusion();
					msgs.add(content);
					nbFollows.add(prop.getArrayContamined().size());
				}
				
				if(resultsRwd.get(pol.toString()).get(pol.nbIt-1)!=null){
					resultsRwd.get(pol.toString()).put(pol.nbIt-1,resultsRwd.get(pol.toString()).get(pol.nbIt-1)+ pol.currentReward);
					resultsNbMess.get(pol.toString()).put(pol.nbIt-1,resultsNbMess.get(pol.toString()).get(pol.nbIt-1)+ 1.0);
				}
				
				else{
					resultsRwd.get(pol.toString()).put(pol.nbIt-1,pol.currentReward);
					resultsNbMess.get(pol.toString()).put(pol.nbIt-1, 1.0);
				}
				
				pol.arms= new ArrayList<ArmContextDigg>();
				for(int i =0; i<msgs.size();i++){
					ArmContextDigg a =  new ArmContextDigg(i,sizeFeatures);
					pol.arms.add(a);
					a.getContext(msgs.get(i),inferencer, true);
					a.getPotentialReward(nbFollows.get(i));
				}
				pol.updateScore();
				pol.select();
				pol.lastSelected.getReward();
				pol.updateParameters();
				
				//System.out.println(msgs);
				//System.out.println(nbFollows);
				
				j+=nbMsgPerStep;
			}
			
			if(resultsRwd.get(pol.toString()).get(pol.nbIt-1)!=null){
				resultsRwd.get(pol.toString()).put(pol.nbIt-1,resultsRwd.get(pol.toString()).get(pol.nbIt-1)+ pol.currentReward);
				resultsNbMess.get(pol.toString()).put(pol.nbIt-1,resultsNbMess.get(pol.toString()).get(pol.nbIt-1)+ 1.0);
			}
			
			else{
				resultsRwd.get(pol.toString()).put(pol.nbIt-1,pol.currentReward);
				resultsNbMess.get(pol.toString()).put(pol.nbIt-1, 1.0);
			}
			
			System.out.println("Total Reward for policy "+pol.toString()+" and user "+u+" :"+pol.totalReward+" nbItTot: "+pol.nbIt);
		}


		
		
	}
	
	public static void main(String[] args) throws IOException{
		collectDiggTest expe1=new collectDiggTest("digg","cascades1,cascades_2",2,50);
		collectDiggTest expe2=new collectDiggTest("digg","cascades1,cascades_2",3,50);
		collectDiggTest expe3=new collectDiggTest("digg","cascades1,cascades_2",4,50);
	}
}
