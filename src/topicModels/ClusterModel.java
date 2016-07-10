package topicModels;

import java.io.BufferedReader;

import actionsBD.MongoDB;
import clustering.Clustering;
import clustering.StrClustering;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileWriter;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import similarities.Cosine;
import clustering.KernelKMeans;
import core.Data;
import core.Model;
import core.Post;
import core.Text;
import similarities.StrSim;
import java.util.HashSet;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
public class ClusterModel implements Model {
	private boolean allSims=false; // false => only max sim is returned for a text, true => every sim is returned
	private ArrayList<Text> centers;
	private StrSim strSim;
	private StrClustering strClust;
	private String fromFile="";
	private String modelFile;
	public ClusterModel(StrSim strSim, String modelFile){
		this(strSim,null,modelFile);
	}
	public ClusterModel(StrSim strSim,StrClustering strClust, String modelFile){
		this(strSim,strClust,false,modelFile);
	}
	public ClusterModel(StrSim strSim,StrClustering strClust,boolean allSims, String modelFile){
		this.strClust=strClust;
		this.strSim=strSim;
		this.allSims=allSims;
		this.modelFile=modelFile;
	}
	
	public String toString(){
		return("StrSim="+strSim+"_model="+modelFile);
	}
	
	public void learn(String db, String collection, double ratio){
		if (ratio>1.0){ratio=1.0;}
		BasicDBObject query = new BasicDBObject();
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		long nb=col.count(query);
		ArrayList<Long> ind=new ArrayList<Long>();
		for(long i=0;i<nb;i++){
			ind.add(i);
		}
		Collections.shuffle(ind);
		ArrayList<Long> tind=new ArrayList<Long>();
		for(int i=0;i<(1.0*nb*ratio);i++){
			tind.add(ind.get(i));
		}
		Collections.sort(tind);
		HashSet<Text> trainSet=new HashSet<Text>();
		DBCursor cursor = col.find(query);
		try {
			nb=0;
			int x=0;
			long el=tind.get(x);
			while((x<tind.size()) && (cursor.hasNext())) {
				DBObject res=cursor.next();
				if (nb==el){
					Post p=Post.getPostFrom(res);
					trainSet.add(p);
					x++;
					if (x<tind.size()){
						el=tind.get(x);
					}
				}
				nb++;
			}
		}
		finally{
			cursor.close();
		}
		System.out.println("Train Set built");
		learn(trainSet);
		
	}
	
	public void learn(HashSet<Text> trainSet){
		centers=new ArrayList<Text>();
		System.out.println("trainSet :"+trainSet.size()+" posts");
		if (strClust!=null){
			centers=new ArrayList<Text>();
			Data data=new Data();
			for(Text text:trainSet){
				data.addText(text);
			}
			strSim.setData(data);
			try{
				Clustering clust=strClust.clusterize(strSim);
				ArrayList<ArrayList<Integer>> clusts=clust.getClusters();
				for(ArrayList<Integer> cl:clusts){
					ArrayList<Text> clt=new ArrayList<Text>();
					for(Integer i:cl){
						clt.add(data.getText(i));
					}
					HashMap<Integer,Double> center=Text.getCentralWeights(clt);
					addCenter(center);
				}
				
			}
			catch(Exception e){
				System.out.println(e);
			}
		}
		else{
			System.out.println("Learn Model Problem : pas de strategie de clustering");
		}
	}
	
	public void addCenter(HashMap<Integer,Double> center){
		this.centers.add(new Text("center_"+(centers.size()+1),center));
	}
	
	public HashMap<Text,HashMap<Integer,Double>> inferTopics(HashSet<Text> texts){
		if (centers.size()==0){
			throw new RuntimeException("Model without parameters => run learn");
		}
		HashMap<Text,HashMap<Integer,Double>> topics=new HashMap<Text,HashMap<Integer,Double>>();
		for(Text t:texts){
			HashMap<Integer,Double> sims=inferTopics(t);
			topics.put(t,sims);
		}
		return(topics);
	}
	public HashMap<Integer,Double> inferTopics(Text text){
		if (centers==null){
			try{
				load();
			}
			catch(IOException e){
				System.out.println(e);
			}
		}
		if (centers.size()==0){
			throw new RuntimeException("Model without parameters => run learn");
		}
		HashMap<Integer,Double> sims=new HashMap<Integer,Double>();
		double max=0;
		int imax=0;
		for(int i=0;i<centers.size();i++){
			double sim=strSim.computeSimilarity(text, centers.get(i));
			//System.out.println("Topic "+(i+1)+" : "+sim);
			if ((i==0) || (max<sim)){
				max=sim;
				imax=i+1;
			}
			if (allSims){
				sims.put(i+1,sim);
			}
		}
		if(!allSims){
			sims.put(imax,max);
		}
		return(sims);
	}
	@Override
	public void load() throws IOException {
		String filename=modelFile;
		BufferedReader lecteur=null;
		try{
			File f=new File(filename);
			lecteur=new BufferedReader(new FileReader(f));
			String ligne;
			centers=new ArrayList<Text>();
			int nb=0;
			while((ligne=lecteur.readLine())!=null){
				String li[]=ligne.split("=");
				if (li.length<2){
					throw new IOException("Probleme fichier modele "+filename);
				}
				HashMap<Integer,Double> w=new HashMap<Integer,Double>();
				String vec[]=li[1].split(";");
				for(int i=0;i<vec.length;i++){
					String couple[]=vec[i].split(":");
					if (couple.length==2){
						int st=Integer.parseInt(couple[0]);
						String va=couple[1].replaceAll(",", ".");
						double val=Double.valueOf(va);
						w.put(st,val);
					}
				}
				nb++;
				Text center=new Text("Center_"+nb,w);
				centers.add(center);
			}
		}
		finally{
			if (lecteur!=null){
				lecteur.close();
			}
		}
	}

	@Override
	public void save() throws IOException {
		String filename=modelFile;
		PrintWriter ecrivain=null;
		try{
			File f=new File(filename);
			ecrivain=new PrintWriter(new BufferedWriter(new FileWriter(f)));
			DecimalFormat format = new DecimalFormat();
			format.setMaximumFractionDigits(4);
			for(int i=0;i<centers.size();i++){
				Text t=centers.get(i);
				String s="Topic"+(i+1)+"=";
				HashMap<Integer,Double> w=t.getWeights();
				ArrayList<Integer> wk=new ArrayList<Integer>(w.keySet());
				Collections.sort(wk);
				for(Integer k:wk){
					double vk=w.get(k);
					s+=k+":"+format.format(vk)+";";
				}
				
				ecrivain.println(s);
			}
		}
		finally{
			if (ecrivain!=null){
				ecrivain.close();
			}
		}
	}
	
	public static void main(String[] args){
		ClusterModel cmod=new ClusterModel(new Cosine(),new KernelKMeans(100),"clusterModel.txt");
		cmod.learn(args[0],args[1],Double.valueOf(args[2]));//0.001);
		
		try{
			cmod.save();
		}
		catch(IOException e){
			System.out.println(e);
		}
	}

}
