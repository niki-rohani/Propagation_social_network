package thibault.simRelationalBandit;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.math3.distribution.NormalDistribution;


import statistics.Distributions;

public class DataGeneratorRelationalBandit {
	int K; //nbArms
	ArrayList<Node> nodes;
	double probLinkExist=0.3;

	
	public DataGeneratorRelationalBandit(int K){
		this.K=K;
		nodes=new ArrayList<Node>();
		for(int i=0;i<K;i++){
			Node u = new Node(i);
			nodes.add(u);
		}
		Distributions d = new Distributions();
		for(Node u: nodes){
			for(Node v: nodes){
				boolean isLinked=d.nextBoolean(probLinkExist);
				double poid=0.0;
				if(u.Id!=v.Id && isLinked){
						poid=Math.random();
						u.Pred.put(v, poid);
						v.Succ.put(u, poid);
				}
			}
		}
		for(Node u: nodes){
			double sumPoids=0.0;
			for(Node v:u.Pred.keySet()){
				sumPoids+=u.Pred.get(v);
			}
			if(sumPoids!=0){
				for(Node v:u.Pred.keySet()){
					u.Pred.put(v,u.Pred.get(v)/sumPoids);
				}
			}
		}
		
		for(Node u: nodes){
			System.out.println(u.Pred.keySet().size());
			/*for(Node v:u.Pred.keySet()){
				System.out.println(u.Id+" "+v.Id+" "+u.Pred.get(v));
			}
			System.out.println();*/
		}
		
	}
	

	
	public void genere(String folder,int t, int n){
		
		try{
        	File f=new File(folder+"/sim.txt");
        	File dir=f.getParentFile();
    		if(!dir.exists()){
    			dir.mkdirs();
    		}
            PrintStream p = new PrintStream(f) ;
            


            
            for(int i=0;i<t;i++){
            	String s="";
            	for(Node u: nodes){
            		u.r=u.bias;
            	}
            	for(int j=0;j<n;j++){
            		Collections.shuffle(nodes);
            		for(Node u: nodes){
            			double mean = u.bias;
            			double var=u.var;
            			//System.out.println(u.Pred.keySet().size());
            			for(Node v: u.Pred.keySet()){
            				mean+=u.Pred.get(v)*v.r;
            			}
            			u.dist=new NormalDistribution(mean,var);
            			u.r=u.dist.sample();
            		}
            	}
            	
            	for(Node u: nodes){
            		s+=u.Id+","+u.r+";";
            	}
            	p.println(s);
    		}
            p.close(); 
		}
        catch(IOException e){
        	e.printStackTrace();
        	//System.out.println("Probleme ecriture "+folder);
        }
	}
	public static void main(String[] args){
		DataGeneratorRelationalBandit b=new DataGeneratorRelationalBandit(10);
		b.genere("./testSimRelational", 100,100);
	}
}