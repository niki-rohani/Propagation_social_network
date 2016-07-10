package thibault.simRelationalBandit;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class testSuiteRec {
	
	public int p;
	public RealVector U0;
	public RealVector Un;
	public RealMatrix A;
	public RealMatrix P;
	public RealMatrix invP;
	public RealMatrix D;
	
	public testSuiteRec(int p){
		this.p=p;
		this.U0=new ArrayRealVector(new double[p]);
		this.Un=new ArrayRealVector(new double[p]);
		this.A= new Array2DRowRealMatrix(new double[p][p]);
		for(int i=0;i<p;i++){
			U0.setEntry(i, Math.random());
			for(int j=0;j<=i;j++){
				if (i==j){
					A.setEntry(i, j,0);
				}
				else{
					double d=Math.random();
					A.setEntry(i, j,d);
					A.setEntry(j, i,d);
				}
			}
		}
	}
	
	public void computeUnRec(int n){
		Un=U0;
			for(int j=0;j<n;j++){
				Un=A.operate(Un);
			}
			
		System.out.println(Un);
	}
	
	public void computeUn(int n){
		EigenDecomposition eig = new EigenDecomposition(A);
		D= eig.getD();
		P=eig.getV();
		invP = eig.getVT();
		
		Un=P.multiply(D.power(n)).multiply(invP).operate(U0);
		
		System.out.println(Un);
		//RealMatrix invP = new  LUDecomposition(P).getSolver().getInverse();
		//System.out.println(eig.getVT());
		//System.out.println(invP);
		
		//System.out.println(A);
		//System.out.println(P.multiply(D).multiply(invP));
		//System.out.println(D);
		System.out.println(new ArrayRealVector(eig.getRealEigenvalues()));
		//System.out.println(new ArrayRealVector(eig.getImagEigenvalues()));

		
	}
	
	
	 
	
	public static void main(String[] args) {
		testSuiteRec U = new testSuiteRec(5);
		U.computeUn(10);
		U.computeUnRec(10);


	}

}
