package thibault.diggCollect;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

import propagationModels.MultiSetsPropagationStructLoader;
import propagationModels.PropagationStruct;
import utils.ValInHashMapComparator;
import jgibblda.Estimator;
import jgibblda.LDACmdOption;

public class ldaBuilder {
	
	MultiSetsPropagationStructLoader loader;
	int NbTot=0;
	int nbMsgPerStep;
	String folderName = "./src/thibault/";
	String fileName="savedPostFromDigg";
	public BufferedWriter out;
	public ldaBuilder(String db,String cascades,int nbMsgPerStep,int minNumberMsg) throws IOException{
		FileWriter fw=null;
		fw = new FileWriter(folderName+fileName);
		out = new BufferedWriter(fw);
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
         	  increaseNbTot(u);
         	}
       	out.write(NbTot+"\n");
       	for(String u:lu){
       	  int nb=liste.get(u).size();
       	  if(nb<minNumberMsg) break;
       	  System.out.println(u+" : "+nb+" cascades in First Step");
       	  appenfFileWithPostFromUser(u);
       	}
       	out.close();
	}
	
	public void appenfFileWithPostFromUser(String u) throws IOException{
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

			for(TreeMap<Integer,Double> t: msgs){
				String ligne="";
				for(Integer w:t.keySet()){
					ligne=ligne+" "+w;
				}
				out.write(ligne+"\n");
			}
		
			j+=nbMsgPerStep;
		}
	}
	
	
	public void increaseNbTot(String u) throws IOException{
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

			for(TreeMap<Integer,Double> t: msgs){
				NbTot++;
			}
		
			j+=nbMsgPerStep;
		}
	}
	
	
	
	public static void main(String[] args) throws IOException {

		ldaBuilder expe=new ldaBuilder("digg","cascades1,cascades_2",2,50);
		
		//Partie pour ecrire les posts dans un fichier. Posts deja au format nombre et poids
		
		
		//Partie apprentissage sur le corpus
		System.out.println("Learning LDA model on :"+expe.fileName);
		LDACmdOption ldaOption = new LDACmdOption();
		ldaOption.est=true;
		ldaOption.estc=false;
		ldaOption.inf = false;
		ldaOption.dir = expe.folderName;
		ldaOption.niters = 2000; 
		ldaOption.savestep=2000;
		ldaOption.K=20;
		ldaOption.dfile=expe.fileName;
		Estimator estimator = new Estimator();
		estimator.init(ldaOption);
		estimator.estimate();
	}

	

}
