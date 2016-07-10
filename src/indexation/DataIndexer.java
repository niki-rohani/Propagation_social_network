package indexation;
import java.util.ArrayList;
import indexation.*;
import core.*;
import java.io.*;

import wordsTreatment.WeightComputer;
public abstract class DataIndexer {
	//protected String db;
	//protected String collection; // Collection in which data are recorded 
	//protected WeightComputer weightComputer=null; // Strategie de ponderation des termes
	/*public DataIndexer(WeightComputer weightComputer){
		//this.db=db;
		this.weightComputer=weightComputer;
		//this.collection=collection;
	}*/
	
	public static ArrayList<File> getRecursiveChilds(File f){
		ArrayList<File> ret=new ArrayList<File>();
		
		if(f.isDirectory()){
			File[] childs=f.listFiles();
			for(File c:childs){
				ret.addAll(getRecursiveChilds(c));
			}
		}
		else{
			ret.add(f);
		}
		return ret;
	}
	
	public static String gatherAllFiles(String repName){
		File rep=new File(repName);
		ArrayList<File> files=getRecursiveChilds(rep);
		String retName=repName+"/all.txt";
		File ret=new File(retName);
		PrintWriter out=null;
		BufferedReader lecteur=null;
		int i=1;
		try{
			out=new PrintWriter(new BufferedWriter(new FileWriter(ret)));
			for(File f:files){
				out.println("<File "+f.getAbsolutePath()+">");
				lecteur=new BufferedReader(new FileReader(f));
				String line="";
				while(((line=lecteur.readLine())!=null)){ 
					out.println(line);
				}
				lecteur.close();
				out.println("</File "+f.getAbsolutePath()+">");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			if(out!=null){
				out.close();
			}
		}
		return(retName);
	}
	public abstract String indexData(String db,String filename,WeightComputer weightComputer,TextTransformer trans) throws IOException;
	public abstract String toString();
	
	public static void main(String[] args){
		gatherAllFiles("/home/lampriers/propagation/Propagation/data/enron");
	}
}
