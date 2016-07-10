package thibault.simBandit;

import java.util.ArrayList;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.la4j.decomposition.EigenDecompositor;
import org.la4j.inversion.GaussJordanInverter;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.dense.BasicVector;

public class test {
	
	public static double invertNonCenteredChi2 (double lambda, double confInt){
		NormalDistribution n = new NormalDistribution();
		double h = 1.0-2.0/3.0*(1.0+lambda)*(1.0+3.0*lambda)/((1.0+2.0*lambda)*(1.0+2.0*lambda));
		double p = (1.0+2.0*lambda)/((1.0+lambda)*(1.0+lambda));
		double m = (h-1.0)*(1.0-3.0*h);
		double val = n.inverseCumulativeProbability(confInt);
		return Math.pow((h*Math.sqrt(2*p)*(1+0.5*m*p))*val+(1+h*p*(h-1-0.5*(2-h)*m*p)), 1.0/h)*(lambda+1);
	}
	
	public static void main(String[] args) {
		int d = 8;
		int n = 1000;
		ArrayList<Double> confInt= new  ArrayList<Double>();
		NormalDistribution normal = new NormalDistribution();
		confInt.add(0.70);
		confInt.add(0.90);
		confInt.add(0.95);
		confInt.add(0.99);
		
		ArrayList<Double> confInt1= new  ArrayList<Double>();
		confInt1.add(0.70);
		confInt1.add(0.90);
		confInt1.add(0.95);
		confInt1.add(0.99);

		double[] rep=new double[d];
		double[] m=new double[d];
		double[] rep1=new double[d];
		double[] m1=new double[d];

		//Matrix Id = new Basic2DMatrix(new double[d][d]);
		for(int j=0;j<d;j++){
			m[j]=(Math.random()*1.0);
			rep[j]=(Math.random()*20+50);
			m1[j]=(Math.random()*1.0);
			rep1[j]=(Math.random()*10+50);
		}
		double[][] cov=new double[d][d];
		double[][] cov1=new double[d][d];
		for(int j=0;j<d;j++){
			for(int l=0;l<d;l++){
				if(j==l){
					cov[j][l]=0.01+rep[j]*rep[j];
					cov1[j][l]=0.01+rep1[j]*rep1[j];
				}
				else {
					cov[j][l]=rep[j]*rep[l];
					cov1[j][l]=rep1[j]*rep1[l];
				}
			}
		}

		
		
		//System.out.println(Id);


		RealMatrix Mat = new Array2DRowRealMatrix(cov);
		EigenDecomposition eigMat = new EigenDecomposition(Mat);
		RealMatrix dMat = eigMat.getD();
		
		RealMatrix Mat1 = new Array2DRowRealMatrix(cov1);
		EigenDecomposition eigMat1 = new EigenDecomposition(Mat1);
		RealMatrix dMat1 = eigMat1.getD();
		RealMatrix pMat1 = eigMat1.getV();
		RealMatrix zMat1 = pMat1.multiply(new Array2DRowRealMatrix(m1));
		
		/*Matrix M = new Basic2DMatrix(cov);
		EigenDecompositor eig = new EigenDecompositor(M);
		Matrix D = eig.decompose()[1];
		
		Matrix M1 = new Basic2DMatrix(cov1);
		EigenDecompositor eig1 = new EigenDecompositor(M1);
		Matrix[] dec1 =  eig1.decompose();
		Matrix D1 = dec1[1];
		Matrix P1 = dec1[0];
		Vector Z1=P1.multiply(new BasicVector(m1));*/
		
		//GaussJordanInverter gauss1= new GaussJordanInverter(P1); 
		//System.out.println(M1);
		//System.out.println(D1);
		//System.out.println(P1.multiply(D1).multiply(gauss1.inverse()));
		
		
		/*/GaussJordanInverter gauss1= new GaussJordanInverter(M1); 
		System.out.println(D1);
		
		
		//System.out.println(sum);
		System.out.println(gauss.inverse());
		//System.out.println(gauss1.inverse());
		//System.out.println(new BasicVector(m)+"\n");
		//System.out.println(new Basic2DMatrix(cov)+"\n");
		
		/*for(Double c: confInt){
			double val =  normal.inverseCumulativeProbability(c);
			double gamma =  Math.pow(Math.sqrt(2.0/9.0)*val+1.0-2.0/9.0, 3.0);
			System.out.println("Confint pour norm Ctxt a "+c+": "+Math.sqrt(D.sum()*gamma));
			System.out.println("Confint pour norm Beta a "+c+": "+Math.sqrt(D1.sum()*gamma));
			System.out.println("Confint pour produit norme "+c+": "+Math.sqrt(D.sum()*gamma)*Math.sqrt(D1.sum()*gamma)+"\n");
		}*/
		/*for(int i = 0;i<d;i++){
			System.out.println(D1.get(i, i));
		}*/
		
		for(Double c: confInt){
			for(Double c1: confInt1){
			double val =  normal.inverseCumulativeProbability(c);
			double gamma =  Math.pow(Math.sqrt(2.0/9.0)*val+1.0-2.0/9.0, 3.0);
			double lambda;
			double sum=0.0;
			double sum1=0.0;
			
			for(int i = 0;i<d;i++){
				lambda=Math.pow(zMat1.getEntry(i,0),2.0)/dMat1.getEntry(i,i);
				sum1+=dMat1.getEntry(i,i)*invertNonCenteredChi2(lambda, c1);
				sum+=dMat.getEntry(i,i);
			}
			System.out.println("Confint pour norm Ctxt Centre a "+c+": "+Math.sqrt(sum*gamma));
			System.out.println("Confint pour norm Beta a "+c1+": "+Math.sqrt(sum1));
			System.out.println("Confint pour produit norme :"+Math.sqrt(sum*gamma)*Math.sqrt(sum1)+"\n");
			}
		}

		
		MultivariateNormalDistribution dist = new MultivariateNormalDistribution(m,cov);
		MultivariateNormalDistribution dist1 = new MultivariateNormalDistribution(m1,cov1);
		double moyNorm=0.0;
		double stdDevNorm=0.0;
		double moyNorm1=0.0;
		double stdDevNorm1=0.0;
		double moyDotProd=0.0;
		double stdDevDotProd=0.0;
		double moyProdNorm=0.0;
		double stdDevProdNorm=0.0;
		for(int i=0;i<n;i++){
			double norm=0;
			double norm1=0;
			double dotProd=0.0;
			double prodNorm=0.0;
			double[] s=dist.sample();
			double[] s1=dist1.sample();
			for(int j=0;j<d;j++){
				norm+=(s[j]-m[j])*(s[j]-m[j]);
				//norm1+=(s1[j]-m1[j])*(s1[j]-m1[j]);
				norm1+=s1[j]*s1[j];
				//dotProd+=(s[j]-m[j])*(s1[j]-m1[j]);
				dotProd+=(s[j]-m[j])*s1[j];
			}
			norm=Math.sqrt(norm);
			norm1=Math.sqrt(norm1);
			dotProd=Math.abs(dotProd);
			prodNorm=norm*norm1;
			//System.out.println("Norme ctxt :"+norm+" Norme beta :"+norm1+" Produit scalaire :"+dotProd+" Produit norme :"+prodNorm);
			//System.out.println("Produit scalaire :"+dotProd+" Produit norme :"+prodNorm);
			moyNorm+=norm/n;
			stdDevNorm+=norm*norm;
			moyNorm1+=norm1/n;
			stdDevNorm1+=norm1*norm1;
			moyDotProd+=dotProd/n;
			stdDevDotProd+=dotProd*dotProd;
			moyProdNorm+=prodNorm/n;
			stdDevProdNorm+=prodNorm*prodNorm;
		}
		stdDevNorm=Math.sqrt(stdDevNorm/n-moyNorm*moyNorm);
		stdDevNorm1=Math.sqrt(stdDevNorm1/n-moyNorm1*moyNorm1);
		stdDevDotProd=Math.sqrt(stdDevDotProd/n-moyDotProd*moyDotProd);
		stdDevProdNorm=Math.sqrt(stdDevProdNorm/n-moyProdNorm*moyProdNorm);
		System.out.println("\n"+"Moyenne ProdNorme: "+moyProdNorm);
		System.out.println("\n"+"Ecart Type ProdNorme: "+stdDevProdNorm);
		System.out.println("\n"+"Moyenne DotProd: "+moyDotProd);
		System.out.println("\n"+"Ecart Type DotProd: "+stdDevDotProd);
		


	}

}
