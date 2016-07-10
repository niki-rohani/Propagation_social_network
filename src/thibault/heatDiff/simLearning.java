package thibault.heatDiff;

import org.la4j.decomposition.EigenDecompositor;
import org.la4j.inversion.GaussJordanInverter;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;


public class simLearning {

	public static void main(String[] args) {
		
		double alpha=1;
		double time=0.4;
		
		double[][] A = new double[][]{
				{1,0,0,0,0},
				{0,1,0,0,0},
				{0,0,1,0,0},
				{0,0,0,1,0},
				{0,0,0,0,1}
		};
		
		double[][] B = new double[][]{
				{0.301,0.175,0.175,0.175,0.175},
				{0.175,0.708,0.039,0.039,0.039},
				{0.175,0.039,0.708,0.039,0.039},
				{0.175,0.039,0.039,0.708,0.039},
				{0.175,0.039,0.039,0.039,0.708}
		};
		
		Matrix F0=new Basic2DMatrix(A);
		Matrix Ft=new Basic2DMatrix(B);
		

		diffusionLearn diff = new diffusionLearn(alpha,F0, Ft);
		diff.findH();
		System.out.println(diff.H);


	}

}
