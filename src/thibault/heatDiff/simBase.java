package thibault.heatDiff;

import java.util.Vector;

import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.dense.BasicVector;

public class simBase {

	public static void main(String[] args) {
		int N=5;
		double alpha=1;
		int nbInit=2;
		double T= 1.0;
		int nbSteps=10;
		
		double[][] D = new double[][]{
				{-4,1,1,1,1},
				{1,-1,0,0,0},
				{1,0,-1,0,0},
				{1,0,0,-1,0},
				{1,0,0,0,-1}
		};
		
		double[] d = new double[]{0,0,0,0,1};
		
		BasicVector f0=new BasicVector(d);
		Matrix H=new Basic2DMatrix(D);
		
		
		//diffusionBase diff = new diffusionBase(alpha,T,nbSteps,H,f0);
		//diffusionBase diff = new diffusionBase(alpha,T,nbSteps,H,nbInit);
		//diffusionBase diff = new diffusionBase(alpha,T,nbSteps,N,nbInit);
		//diffusionBase diff = new diffusionBase(alpha,T,nbSteps,N,f0);
		//diff.simulateDiff();
		//diff.plotResults();
		//System.out.println(diff.f0);
		//System.out.println(diff.H);
		//System.out.println(diff.ft);
		
		System.out.println(H.add(H).divide(4));

	}

}
