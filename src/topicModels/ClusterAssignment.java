package topicModels;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.IOException;

import core.Text;
import similarities.*;
import clustering.*;
import core.Data;
public class ClusterAssignment extends TopicIdentificator {
	private StrSim strSim;
	private StrClustering strClust;
	private ClusterModel cmod=null;
	
	public ClusterAssignment(ClusterModel cmod){
		this.cmod=cmod;
	}
	
	
	public ClusterAssignment(String modelFile,StrSim strSim) throws IOException{
		this.cmod=new ClusterModel(strSim,modelFile);
		cmod.load();
	}
	
	@Override
	public HashMap<Text,HashMap<Integer,Double>> inferTopics(HashSet<Text> texts){
		HashMap<Text,HashMap<Integer,Double>> topics=cmod.inferTopics(texts);	
		return(topics);
	}
	
	public String toString(){
		return("ClusterAssignment "+cmod);
	}
	
	/*public ClusterAssignment(){
		this("");
	}
	public ClusterAssignment(String modelFile){
		this(0.0,modelFile);
	}
	public ClusterAssignment(double trainRatio){
		this(trainRatio, "");
	}
	public ClusterAssignment(double ratio,String modelFile){
		this(new Cosine(),new KernelKMeans(100),ratio,modelFile);
	}
	public ClusterAssignment(StrSim strSim,StrClustering strClust){
		this(strSim,strClust,1.0,"");
	}
	public ClusterAssignment(StrSim strSim,StrClustering strClust,double ratio){
		this(strSim,strClust,ratio,"");
	}
	public ClusterAssignment(StrSim strSim,String modelFile){
		this(strSim,new KernelKMeans(100),0.0,modelFile);
	}
	public ClusterAssignment(StrSim strSim,StrClustering strClust,double ratio,String modelFile){
		super(ratio,modelFile);
		this.strSim=strSim;
		this.strClust=strClust;
	}*/
	
	
	
		/*if(cmod==null){
			cmod=new ClusterModel(strSim);
			if (trainRatio>0.0){
				int nb=texts.size();
				HashSet<Text> trainSet=new HashSet<Text>();
				if (trainRatio>=1.0){
					trainSet=texts;
				}
				else{
					double ratio=0.0;
					ArrayList<Text> trains=new ArrayList<Text>(texts);
					Collections.shuffle(trains);
					int i=0;
					while((i<nb) && (ratio<trainRatio)){
						trainSet.add(trains.get(i));
						i++;
						ratio=i/(nb*1.0);
					}
				}
				cmod.learn(trainSet, strClust);
			}
			else{
				try{
					cmod.load(modelFile);
				}
				catch(IOException e){
					System.out.println(e);
					return(null);
				}
			}
		}*/
		
		
	
}
