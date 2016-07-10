package thibault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

import propagationModels.MultiSetsPropagationStructLoader;
import propagationModels.PropagationStruct;
import utils.ValInHashMapComparator;

public class ExampleExpeBanditChooseMsgFromCascades {
	MultiSetsPropagationStructLoader loader;
	int nbMsgPerStep;
	public ExampleExpeBanditChooseMsgFromCascades(String db,String cascades,int nbMsgPerStep,int minNumberMsg){
		this.nbMsgPerStep=nbMsgPerStep;
		loader=new MultiSetsPropagationStructLoader(db,cascades,1,"1.0","-1","1","-1");
		loader.computeFirsts=true;
		loader.load(); 
		HashMap<String,Integer> nbFirsts=new HashMap<String,Integer>(); 
       	HashMap<String,ArrayList<Integer>> liste= loader.getUserAsFirst();
       	for(String u:liste.keySet()){
       		 nbFirsts.put(u,liste.get(u).size());
       		 
       	}
       	ArrayList<String> lu=new ArrayList<String>(nbFirsts.keySet());
       	Collections.sort(lu,new ValInHashMapComparator<String,Integer>(nbFirsts,true));
       	for(String u:lu){
       	  int nb=liste.get(u).size();
       	  if(nb<minNumberMsg) break;
       	  System.out.println(u+" : "+nb+" cascades in First Step");
       	  doExpeForUser(u);
       	}
        
	}
	public void doExpeForUser(String u){
		ArrayList<Integer> allCascades= loader.getUserAsFirst().get(u);
		
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
			
			
			System.out.println(nbFollows.get(0)+" "+msgs.get(0));
			System.out.println(nbFollows.get(1)+" "+msgs.get(1));
			System.out.println();
			//System.out.println(nbFollows);
			
			// choix du bras a jouer parmi les msgs
			// reward=nbFollows correspondant
			
			
			j+=nbMsgPerStep;
		}
	}
	
	public static void main(String[] args){
		ExampleExpeBanditChooseMsgFromCascades expe=new ExampleExpeBanditChooseMsgFromCascades("digg","cascades1,cascades_2",2,50);
	}
}
