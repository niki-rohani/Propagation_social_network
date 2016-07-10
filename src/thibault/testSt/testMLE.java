package thibault.testSt;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.dense.BasicVector;

public class testMLE {
	public static void main(String[] args){
		
		int d=8;
		int T=100;
		double[] rep=new double[d];
		double[] m=new double[d];
		for(int j=0;j<d;j++){
			m[j]=(Math.random()*10.0);
			rep[j]=(Math.random()*5.0);
		}
		double[][] cov=new double[d][d];
		for(int j=0;j<d;j++){
			for(int l=0;l<d;l++){
				if(j==l)cov[j][l]=0.1+rep[j]*rep[j];
				else cov[j][l]=rep[j]*rep[l];
			}
		}
		
		MultivariateNormalDistribution dis=new MultivariateNormalDistribution(m,cov);
		Vector empSum=new BasicVector(new double[d]);
		Matrix sumProdVar = new Basic2DMatrix(new double[d][d]);
		
		double[] empAvT = new double[d];
		double[][] enpCovT = new double[d][d];
		
		for(int j=0;j<d;j++){
			empSum.set(j, 0);
			empAvT[j]=0;
			for(int l=0;l<d;l++){
				sumProdVar.set(j, l, 0);
				enpCovT[j][l]=0;
			}
		}
		
		for(int i=0;i<T;i++){
			double[] c=dis.sample();
				Vector curCTXT=new BasicVector(c);
				empSum=empSum.add(curCTXT);
				sumProdVar=sumProdVar.add(curCTXT.outerProduct(curCTXT));
				for(int j=0;j<d;j++){
					empAvT[j]=empSum.get(j)/(i+1);
					for(int l=0;l<d;l++){
						enpCovT[j][l]=sumProdVar.get(j, l)/(i+1)-empSum.outerProduct(empSum).get(j, l)/((i+1)*(i+1));
					}
					
				}
				
			}


			System.out.println(new BasicVector(m));
			System.out.println(new BasicVector(empAvT));
			System.out.println(new Basic2DMatrix(enpCovT));
			System.out.println(new Basic2DMatrix(cov));
		
		
	}

}
