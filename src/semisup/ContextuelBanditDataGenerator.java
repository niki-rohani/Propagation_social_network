package semisup;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
public class ContextuelBanditDataGenerator {
	int k; // nb arms
	int d; // nb dims
	ArrayList<Double> vars; // reward arm variance
	ArrayList<Double> bias; // arm biases
	ArrayList<MultivariateNormalDistribution> arms; // arm distribs
	ArrayList<ArrayList<Double>> thetas; // arm parameters to discover
	
	public ContextuelBanditDataGenerator(int k,int d){
		this.k=k;
		this.d=d;
		thetas=new ArrayList<ArrayList<Double>>();
		bias=new ArrayList<Double>();
		vars=new ArrayList<Double>();
		arms=new ArrayList<MultivariateNormalDistribution>();
		for(int i=0;i<k;i++){
			double[] rep=new double[d];
			double[] m=new double[d];
			ArrayList<Double> t=new ArrayList<Double>();
		
			for(int j=0;j<d;j++){
				
				m[j]=(Math.random()*(2.0)-1.0);
				rep[j]=(Math.random()*(2.0)-1.0);
				t.add(Math.random()*(2.0)-1.0);
			}
			double[][] cov=new double[d][d];
			//double[][] xy=new double[d][d];
			for(int j=0;j<d;j++){
				for(int l=0;l<d;l++){
					if(j==l)cov[j][l]=1.0+rep[j]*rep[j];
					else cov[j][l]=rep[j]*rep[l];
					System.out.print(cov[j][l]+" ");
				}
			}
			
			MultivariateNormalDistribution dis=new MultivariateNormalDistribution(m,cov);
			arms.add(dis);
			bias.add(Math.random()*10.0);
			vars.add(Math.random()*1.0);
			thetas.add(t);
		}
	}
	
	public void genere(String file,int t){
		
		
		try{
        	File f=new File(file);
        	File dir=f.getParentFile();
        	if(dir!=null){
        		dir.mkdirs();
        	}
            PrintStream p = new PrintStream(file) ;
            for(int i=0;i<t;i++){
            	String s=i+":";
            	//ArrayList<Double> rewards=new ArrayList<Double>();
            	for(int j=0;j<k;j++){
            		double r=bias.get(j);
            		double[] c=arms.get(j).sample();
            		ArrayList<Double> tt=thetas.get(j);
            		s+=j+"(";
            		for(int l=0;l<d;l++){
            			r+=c[l]*tt.get(l);
            			s+=c[l];
            			if(l<d-1) s+=",";
            		}
            		double v=(new NormalDistribution(r,vars.get(j))).sample();
            		s+=")="+v;
            		//rewards.add(v);
            		if(j<k-1){
            			s+=";";
            		}
            	}
            	p.println(s);
    		}
            p.close();
		}
        catch(IOException e){
        	System.out.println("Probleme ecriture "+file);
        	
        }
		
	}
	public static void main(String[] args){
		ContextuelBanditDataGenerator b=new ContextuelBanditDataGenerator(10,5);
		b.genere("a.txt", 5000);
	}
}