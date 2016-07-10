package thibault.heatDiff;

import java.util.Random;

import javax.swing.JFrame;

import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.dense.BasicVector;
//import org.math.plot.Plot2DPanel;


public class diffusionBase {

	public Matrix H;
	public Vector f0;
	public Matrix ft; //matrice de ft une colonne par ft
	public int N;
	public double alpha;
	public int P=30;
	public double T;
	public int nbSteps;
	
	public diffusionBase(double alpha,double T, int nbSteps, int N, int nbInit){
		this.alpha=alpha;
		this.T=T;
		this.nbSteps=nbSteps;
		this.N=N;
		this.H=new Basic2DMatrix((new double[N][N] ));
		this.f0=new BasicVector(new double[N]);	
		this.initF0(nbInit);
		this.initH();
	}
	
	public diffusionBase(double alpha,double T, int nbSteps, int N, Vector f0){
		this.alpha=alpha;
		this.T=T;
		this.nbSteps=nbSteps;
		this.N=N;
		this.f0=f0;
		this.H=new Basic2DMatrix((new double[N][N] ));
		this.initH();
	}
	
	public diffusionBase(double alpha,double T, int nbSteps, Matrix H, Vector f0){
		this.alpha=alpha;
		this.T=T;
		this.nbSteps=nbSteps;
		this.H=H;
		this.f0=f0;
		this.N=f0.length();
	}
	
	public diffusionBase(double alpha,double T, int nbSteps, Matrix H, int nbInit){
		this.N=H.columns();
		this.alpha=alpha;
		this.T=T;
		this.nbSteps=nbSteps;
		this.H=H;
		this.f0=new BasicVector(new double[N]);	
		this.initF0(nbInit);
	}
	
	public void initF0(int nbInit){
		for (int i = 0; i<N;i++){
			f0.set(i, 0);	
		}
		Random rand = new Random();
		for (int i = 0; i<nbInit;i++){
			int j = rand.nextInt(N);
			double k = rand.nextDouble()*3;
			while(f0.get(j)==1){
				j = rand.nextInt(N);
			}
			f0.set(j, k);	
		}
	}
	
	public void initH(){
		Random rand = new Random();
		for (int i = 0; i<N;i++){
			for (int j = 0; j<i;j++){
				int k = rand.nextInt(2);
				//System.out.println(k);
				H.set(i, j,k);	
				H.set(j, i,k);
			}
		}
		for (int i = 0; i<N;i++){
			double sum=0;
			for (int j = 0; j<N;j++){
				sum=sum+H.get(i, j);
			}
			H.set(i, i,-sum);
		}
		
		
	}
	
	
	public void simulateDiff(){ //T = longueur de la simulation et sizeStep = taille d'une pas de temps
		this.ft=new Basic2DMatrix((new double[N][nbSteps] ));
		double stepSize=T/nbSteps;
		Matrix Id=new Basic2DMatrix((new double[N][N] ));
		for (int i = 0; i<N;i++){
			for (int j = 0; j<N;j++){
				if(i==j){Id.set(i, j, 1);}
				else{Id.set(i, j, 0);}
			}
		}	
		for (int i=0;i<nbSteps;i++){
			Vector ftTemp=new BasicVector(new double[N]);	
			ftTemp=H.multiply(alpha*stepSize*i/P).add(Id).power(P).multiply(f0);
			ft.setColumn(i, ftTemp);
		}
	}
	
	public void plotResults(){
		/*Plot2DPanel plot = new Plot2DPanel();
		double[] x = new double[nbSteps];
        double[] y = new double[nbSteps];
        
		for (int i = 0; i<N;i++){
			
			for (int j = 0; j<nbSteps;j++){
				x[j]=j*T/nbSteps;
				y[j]=ft.get(i, j);
			}
			//System.out.println(x);
	        //plot.addLegend("i");
	        plot.addLinePlot("my plot"+i, x, y);
		}	
        
        
        JFrame frame = new JFrame("Heat Diffusion");
        frame.setSize(600, 600);
        frame.setContentPane(plot);
        frame.setVisible(true);*/


	}
	
}
