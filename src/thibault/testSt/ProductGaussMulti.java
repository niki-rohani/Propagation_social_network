package thibault.testSt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class ProductGaussMulti {

	public static void main(String[] args) {
		/*int dim=8;
		double[] mu1=new double[dim];
		double[] mu2=new double[dim];
		double[] rep1=new double[dim];
		double[] rep2=new double[dim];
		double[][] v1=new double[dim][dim];
		double[][] v2=new double[dim][dim];
		int nbSamples = 1000000;
		
		for(int i=0;i<dim;i++){
			mu1[i]=(Math.random()*Math.random()*2);
			mu2[i]=(Math.random()*Math.random()*3-1);
			rep1[i]=(Math.random()*Math.random()*Math.random()*4);
			rep2[i]=(Math.random()*Math.random()*Math.random()*4);
		}
		for(int i=0;i<dim;i++){
			for(int j=0;j<dim;j++){
				if(i==j){
					v1[i][j]=1+rep1[i]*rep1[j];
					v2[i][j]=1+rep2[i]*rep2[j];
				}
				else{
					v1[i][j]=rep1[i]*rep1[j];
					v2[i][j]=rep2[i]*rep2[j];
				}
			}
		}
		
		double sizeInt = 0.01;
		double [] vectSample = new double[nbSamples];
		double [] vectFreq;
		double [] vectIntervalles;
		MultivariateNormalDistribution x = new MultivariateNormalDistribution(mu1,v1);
		MultivariateNormalDistribution y = new MultivariateNormalDistribution(mu2,v2);
		double [] sX;
		double [] sY;	
		double d;

		for(int i=0;i<nbSamples;i++){
			d=0.0;
			sX=x.sample();
			sY=y.sample();
			for(int j=0;j<sX.length;j++){
				d+=sX[j]*sY[j];
			}
			vectSample[i]=d;
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

		int dim=8;
		double[] mu1=new double[dim];
		double[] mu2=new double[dim];
		double finalMu=0.0;
		double finalV=0.0;
		double[] rep1=new double[dim];
		double[] rep2=new double[dim];
		double[][] v1=new double[dim][dim];
		double[][] v2=new double[dim][dim];
		int nbSamples = 1000000;
		
		for(int i=0;i<dim;i++){
			mu1[i]=(Math.random()*Math.random()*2);
			mu2[i]=(Math.random()*Math.random()*3-1);
			rep1[i]=(Math.random()*Math.random()*Math.random()*4);
			rep2[i]=(Math.random()*Math.random()*Math.random()*4);
		}
		for(int i=0;i<dim;i++){
			for(int j=0;j<dim;j++){
				if(i==j){
					v1[i][j]=1+rep1[i]*rep1[j];
					v2[i][j]=1+rep2[i]*rep2[j];
				}
				else{
					v1[i][j]=rep1[i]*rep1[j];
					v2[i][j]=rep2[i]*rep2[j];
				}
			}
		}
		
		int nbPoint=20000;
		int nbSample=100000;
		double minVal =-30.0;
		double maxVal =30.0;
		double stepSize = (maxVal-minVal)/nbPoint;
		double [] evalPoint = new double[nbPoint];
		double [] ValDensity = new double[nbPoint];
		for(int i=0;i<nbPoint;i++){
			evalPoint[i]=minVal+i*stepSize;
		}

		MultivariateNormalDistribution x = new MultivariateNormalDistribution(mu1,v1);
		

		RealVector mu2Vect = new ArrayRealVector(mu2);
		RealMatrix v2Mat = new Array2DRowRealMatrix(v2);
		//RealMatrix invV2Mat= new LUDecomposition(v2Mat).getSolver().getInverse();
		
		for(int i=0;i<nbSample;i++){
			RealVector xS = new ArrayRealVector(x.sample());
			finalMu+=xS.dotProduct(mu2Vect);
			finalV+=xS.dotProduct(v2Mat.operate(xS));
			}
			
		

		
		System.out.println(finalMu/nbSample);
		System.out.println(Math.sqrt(finalV)/nbSample);

		NormalDistribution z = new NormalDistribution(finalMu/nbSample,Math.sqrt(finalV)/nbSample);
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
