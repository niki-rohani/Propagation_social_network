package thibault.simRelationalBandit;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.math3.distribution.NormalDistribution;


import statistics.Distributions;

public class DataGeneratorRelationalBandit2 {
	int K; //nbArms
	ArrayList<Node> nodes;
	double probLinkExist=0.2;

	
	public DataGeneratorRelationalBandit2(int K){
		this.K=K;
		nodes=new ArrayList<Node>();
		for(int i=0;i<K;i++){
			Node u = new Node(i);
			nodes.add(u);
		}
		
		//this.setWeightsRand();
		this.setWeightsMan();
		
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
			System.out.println(u.Id+" "+u.bias);
			//System.out.println("NbPred: "+u.Pred.keySet().size()+" NbSucc: "+u.Succ.keySet().size());
			for(Node v:u.Pred.keySet()){
				System.out.println(u.Id+" "+v.Id+" "+u.Pred.get(v));
			}
			System.out.println();
		}
		
	}
	
	public void setWeightsRand(){
		Distributions d = new Distributions();
		for(Node u: nodes){
			for(Node v: nodes){
				boolean isLinked=d.nextBoolean(probLinkExist);
				double poid=0.0;
				if(u.Id!=v.Id && isLinked && !v.Succ.containsKey(u)){
					//System.out.println(u.Id);
						poid=Math.random();
						//poid=1;
						u.Succ.put(v, poid);
						v.Pred.put(u, poid);
						//v.Pred.put(u, poid);
				}
			}
		}
	}
	
	public void setWeightsMan(){
		nodes.get(0).Succ.put(nodes.get(1), 1.0);
		nodes.get(1).Pred.put(nodes.get(0), 1.0);
		nodes.get(2).Succ.put(nodes.get(1), 1.0);
		nodes.get(1).Pred.put(nodes.get(2), 1.0);
		
		nodes.get(3).Succ.put(nodes.get(4), 1.0);
		nodes.get(4).Pred.put(nodes.get(3), 1.0);
		nodes.get(5).Succ.put(nodes.get(4), 1.0);
		nodes.get(4).Pred.put(nodes.get(5), 1.0);

		
		for(Node u: nodes){
			u.bias=0.2;
			u.var=0.1;
		}
		
	}
	
	public void genere(String folder,int t){
		
		try{
        	File f=new File(folder+"/sim.txt");
        	File dir=f.getParentFile();
    		/*if(!dir.exists()){
    			dir.mkdirs();
    		}*/
            PrintStream p = new PrintStream(f) ;

            for(int i=0;i<t;i++){
            	String s="";
            	if(i==0){
                	for(Node u: nodes){
            			double mean = u.bias;
            			double var=u.var;
            			u.dist=new NormalDistribution(mean,var);
            			double r = u.dist.sample();
                		u.r0=u.bias;
                		u.r=u.bias;
                	}
            	}
            	else{
            		for(Node u: nodes){
            			double mean = u.bias;
            			double var=u.var;
            			for(Node v: u.Pred.keySet()){
            				mean+=u.Pred.get(v)*v.r0;
            			}
            			u.dist=new NormalDistribution(mean,var);
            			u.r=u.dist.sample();
            		}
            	}
            	for(Node u: nodes){
            		s+=u.Id+","+u.r+";";
            		u.r0=u.r;
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
		DataGeneratorRelationalBandit2 b=new DataGeneratorRelationalBandit2(6);
		b.genere("./testSimRelational", 100);
	}
}