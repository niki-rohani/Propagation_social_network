package thibault.simBandit;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.dense.BasicVector;

import statistics.Distributions;

public class ContextuelBanditDataGeneratorHybrid {
	int k; // nb arms
	int d; // nb dims
	ArrayList<Double> vars; // reward arm variance
	ArrayList<Double> thetas; // arm biases
	ArrayList<MultivariateNormalDistribution> arms; // arm distribs
	ArrayList<Double> beta; // arm parameters to discover
	
	public ContextuelBanditDataGeneratorHybrid(int k,int d){
		this.k=k;
		this.d=d;
		beta=new ArrayList<Double>();
		thetas=new ArrayList<Double>();
		vars=new ArrayList<Double>();
		arms=new ArrayList<MultivariateNormalDistribution>();
		for(int j=0;j<d;j++){
			beta.add(Math.random()*1.0);
		}
		normalizeList(beta);
		for(int i=0;i<k;i++){
			double[] rep=new double[d];
			double[] m=new double[d];
			ArrayList<Double> t=new ArrayList<Double>();
		
			for(int j=0;j<d;j++){
				m[j]=(Math.random());
				//rep[j]=(Math.random()*5*Math.random()*Math.random()*10);
				rep[j]=(Math.random()*Math.random());
			}
			//this.normalizeTable(m);
			//this.normalizeTable(rep);
			
			double[][] cov=new double[d][d];
			for(int j=0;j<d;j++){
				for(int l=0;l<d;l++){
					if(j==l)cov[j][l]=0.1+rep[j]*rep[j];
					else cov[j][l]=rep[j]*rep[l];
					//System.out.print(cov[j][l]+" ");
				}
			}
			
			MultivariateNormalDistribution dis=new MultivariateNormalDistribution(m,cov);
			arms.add(dis);
			thetas.add(Math.random());
			//thetas.add(0.0);
			vars.add(Math.random()*1.0);
		}
	}
	public void normalizeList(ArrayList<Double> l){
		double norm=0;
		for(int i=0;i<l.size();i++){
			norm+=l.get(i)*l.get(i);
		}
		norm=Math.sqrt(norm);
		for(int i=0;i<l.size();i++){
			l.set(i,l.get(i)/norm);
		}	
	}
	
	public void normalizeTable(double[] l){
		double norm=0;
		for(int i=0;i<l.length;i++){
			norm+=l[i]*l[i];
		}
		norm=Math.sqrt(norm);
		for(int i=0;i<l.length;i++){
			l[i]=l[i]/norm;
		}	
	}
	
	public void genere(String file,int t){
		
		
		try{
        	File f=new File(file+"/sim.txt");
        	File dir=f.getParentFile();
        	/*if(dir!=null){
        		dir.mkdirs();
        	}*/
            PrintStream p = new PrintStream(f) ;
            
        	File f1=new File(file+"/Rwd.txt");
        	File dir1=f.getParentFile();
        	/*if(dir!=null){
        		dir1.mkdirs();
        	}*/
            PrintStream p1 = new PrintStream(f1) ;
            
            for(int i=0;i<t;i++){
            	String s=i+":";
            	String s1=""+i;
            	//ArrayList<Double> rewards=new ArrayList<Double>();
            	for(int j=0;j<k;j++){
            		//double r=0;
            		double r=thetas.get(j);
            		double[] c=arms.get(j).sample();
            		//this.normalizeTable(c);
            		s+=j+"(";
            		for(int l=0;l<d;l++){
            			r+=c[l]*beta.get(l);
            			s+=c[l];
            			if(l<d-1) s+=",";
            		}
            		//double v=(new NormalDistribution(r,vars.get(j))).sample();
            		double v=(new NormalDistribution(r,1)).sample();
            		s+=")="+v;
            		s1+="\t"+v;
            		//rewards.add(v);
            		if(j<k-1){
            			s+=";";
            		}
            	}
            	p.println(s);
            	p1.println(s1);
    		}
            p.close(); 
            p1.close();
		}
        catch(IOException e){
        	System.out.println("Probleme ecriture "+file);
        }
		
		try{
        	File f=new File(file+"/simParam.txt");
        	/*File dir=f.getParentFile();
        	if(dir!=null){
        		dir.mkdirs();
        	}*/
            PrintStream p = new PrintStream(f) ;
            p.println("Beta");
            String s="";
            for(int i=0;i<d;i++){
            	s+=beta.get(i)+"\t";
            }
            p.println(s);
            p.println();
            p.println("Arms parameters");
           
            for(int i=0;i<k;i++){
            	p.println("Id"+"\t"+"theta"+"\t"+"var");
            	p.println(i+"\t"+thetas.get(i)+"\t"+vars.get(i));
            	p.println("Mean Vector");
            	p.println(new BasicVector(arms.get(i).getMeans()));
            	p.println("Covariance Matrix");
            	p.println(new Basic2DMatrix(arms.get(i).getCovariances().getData()));
            	p.println();
            }
            
    		
            p.close();
		}
        catch(IOException e){
        	System.out.println("Probleme ecriture "+"simParam.txt");
        	
        }
		
	}
	/*public static void main(String[] args){
		ContextuelBanditDataGeneratorHybrid b=new ContextuelBanditDataGeneratorHybrid(30,8);
		b.genere("sim.txt", 10000);
	}*/
}