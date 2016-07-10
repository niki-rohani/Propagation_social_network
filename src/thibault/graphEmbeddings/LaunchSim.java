package thibault.graphEmbeddings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class LaunchSim {

	public static void main(String[] args) throws FileNotFoundException {
		int k=10;
		int sizeSpace=3;
		Graph G = new Graph(k,sizeSpace);
		String folder= "simHeatEmbed";
		
		
		System.out.println("Sim Graph");
		
    	File f=new File(folder+"/resGraph.txt");
    	File dir=f.getParentFile();
		if(!dir.exists()){
			dir.mkdirs();
		}
		PrintStream p = new PrintStream(f) ;
    	
		SimDiffusion sim1= new SimDiffusionGraph(k,sizeSpace,G,10);
		sim1.launchSim();
		
		
		for (Node u:G.nodes){
			String s="";
			for(int i=0;i<u.values.size();i++){
				s+=u.values.get(i)+"\t";
			}
			p.println(s);
		}
		
		p.close();
		
		
		for (Node u:G.nodes){
			u.reinitResults();
		}
		
		System.out.println("Sim Embeddings");
		
    	f=new File(folder+"/resEmbed.txt");
    	dir=f.getParentFile();
		p = new PrintStream(f) ;
		
		SimDiffusion sim2= new SimDiffusionEmbed(k,sizeSpace,G);
		sim2.launchSim();
		
		for (Node u:G.nodes){
			String s="";
			for(int i=0;i<u.values.size();i++){
				s+=u.values.get(i)+"\t";
			}
			p.println(s);
		}
		p.close();
		
		
		for (Node u:G.nodes){
			u.reinitResults();
		}
		
		System.out.println("Sim Embeddings2");
		
    	f=new File(folder+"/resEmbed2.txt");
    	dir=f.getParentFile();
		p = new PrintStream(f) ;
		
		SimDiffusion sim3= new SimDiffusionEmbed2(k,sizeSpace,G);
		sim3.launchSim();
		
		for (Node u:G.nodes){
			String s="";
			for(int i=0;i<u.values.size();i++){
				s+=u.values.get(i)+"\t";
			}
			p.println(s);
		}
		p.close();

	}

}
