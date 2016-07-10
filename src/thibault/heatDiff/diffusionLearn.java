package thibault.heatDiff;

import org.la4j.decomposition.EigenDecompositor;
import org.la4j.inversion.GaussJordanInverter;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;


public class diffusionLearn {
	
	public double alpha;
	public int P=30;
	public double time=0.4;
	
	public Matrix F0; //Matrice des valeurs initial (un vect f0 par colonne)
	public Matrix Ft; //Matrice des valeurs a t (un vecteur ft par colonne) fixe correspndant au valeur initial de Valeur de t est fixe ici 
	
	public Matrix H;
	
	
	
	public diffusionLearn(double alpha, Matrix F0, Matrix Ft) {
		//this.time=time;
		this.alpha=alpha;
		this.F0=F0;
		this.Ft=Ft;
	}


	public void findH(){

		Matrix At;
		MatrixInverter inverter= new GaussJordanInverter(F0) ;
		At=Ft.multiply(inverter.inverse());
		EigenDecompositor s = new EigenDecompositor(At);
		Matrix[] m=s.decompose();
		Matrix M=m[0];
		Matrix Dt=m[1];
		inverter= new GaussJordanInverter(M) ;
		Matrix invM=inverter.inverse();
		int N=Dt.columns();
		for (int i=0;i<N;i++){
			Dt.set(i, i, Math.log(Dt.get(i, i)));
			//Dt.set(i, i, Math.pow(Dt.get(i, i), 1/P));
		}
		
		/*Matrix Id=new Basic2DMatrix((new double[N][N] ));
		for (int i = 0; i<N;i++){
			for (int j = 0; j<N;j++){
				if(i==j){Id.set(i, j, 1);}
				else{Id.set(i, j, 0);}
			}
		}
		H=M.multiply(Dt).multiply(invM).subtract(Id).multiply(P/(alpha*time));*/
		
		H=M.multiply(Dt).multiply(invM).multiply(1/(alpha*time));
	}

}
