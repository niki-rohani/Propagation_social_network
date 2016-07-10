package thibault.testSt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

public class ProductNormal {

	public static void main(String[] args) {
		/*double mu1=5.0;
		double mu2=2;
		double v1=1.0;
		double v2=1.0;
		int nbSamples = 1000000;
		
		double sizeInt = 0.01;
		double [] vectSample = new double[nbSamples];
		double [] vectFreq;
		double [] vectIntervalles;
		NormalDistribution x = new NormalDistribution(mu1,v1);
		NormalDistribution y = new NormalDistribution(mu2,v2);

		for(int i=0;i<nbSamples;i++){
			vectSample[i]=x.sample()*y.sample();
		}

		double minVal=vectSample[0];
		double maxVal=vectSample[0];
		for(int i=0;i<nbSamples;i++){
			maxVal=Math.max(maxVal, vectSample[i]);
			minVal=Math.min(minVal, vectSample[i]);
		}
		System.out.println("minVal :"+minVal);
		System.out.println("maxVal :"+maxVal);
		int nbIntervalles = (int) Math.ceil((maxVal-minVal)/sizeInt);
		System.out.println("nbIntervalles :"+nbIntervalles);
		vectIntervalles = new double [nbIntervalles];

		for(int i=0;i<nbIntervalles;i++){
			vectIntervalles[i]=minVal+i*sizeInt;
		}

		vectFreq = new double [nbIntervalles];
		for(int i=0;i<nbIntervalles-1;i++){
			vectFreq[i]=0;
		}	

		for(int i=0;i<nbIntervalles-1;i++){
			for(int j=0;j<nbSamples;j++){
				if(vectIntervalles[i]<=vectSample[j] && vectSample[j]<=vectIntervalles[i+1]){
					vectFreq[i]+=1.0;
				}
			}
		}
		for(int i=0;i<nbIntervalles-1;i++){
			vectFreq[i]=vectFreq[i]/nbSamples;
		}	
		
		File f=new File("testProdNorm.txt");
		try {
			PrintStream pS = new PrintStream(f) ;
			for(int i=0;i<nbIntervalles-1;i++){
				pS.println(vectIntervalles[i]+"\t"+vectFreq[i]);
			}
			pS.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}*/

		double mu1=5.0;
		double mu2=2.0;
		double sdev1=1.0;
		double sdev2=1.0;
		double finalMu=0.0;
		double finalsdev=0.0;
		int nbPoint=2000;
		int nbSample=1000;
		double minVal =-10.0;
		double maxVal =40.0;
		double stepSize = (maxVal-minVal)/nbPoint;
		double [] evalPoint = new double[nbPoint];
		double [] ValDensity = new double[nbPoint];
		for(int i=0;i<nbPoint;i++){
			evalPoint[i]=minVal+i*stepSize;
		}

		NormalDistribution x = new NormalDistribution(mu1,sdev1);
		for(int i=0;i<nbSample;i++){
			double xS = x.sample();
			finalMu+=mu2*xS;
			finalsdev+=sdev2*sdev2*xS*xS;
		}
		finalMu=finalMu/nbSample;
		System.out.println(finalMu);
		System.out.println(finalsdev);
		finalsdev=Math.sqrt(finalsdev)/nbSample;
		//System.out.println(finalMu);
		System.out.println(finalsdev);
		NormalDistribution z = new NormalDistribution(finalMu,finalsdev);
		for(int i=0;i<nbPoint;i++){
			ValDensity[i]=z.density(evalPoint[i]);
		}
		
		File f=new File("testProdNorm1.txt");
		try {
			PrintStream pS = new PrintStream(f) ;
			for(int i=0;i<nbPoint-1;i++){
				pS.println(evalPoint[i]+"\t"+ValDensity[i]);
			}
			pS.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
