package simon.mlp;

import java.util.ArrayList;

import mlp.CPUParams;
import mlp.CPUTimesVals;
import mlp.Parameter;
import mlp.Parameters;
import mlp.SequentialModule;
import mlp.CPUExp;
import mlp.CPUMatrix ;
import mlp.TableModule;
import mlp.Tensor;
import mlp.TensorModule;


// Module calculant une équation de la chaleur entre deux point, pour t donné en parametre.
public class CPUHeatDist extends SequentialModule {
	
	private ArrayList<Double> t ;
	private int dims ;
	private CPUTimesVals timesminus4t;
	private CPUTimesVals timesFact; 
	public CPUL2Dist2 dist ;
	private boolean weighted ;
	private double k ;
	
	public CPUHeatDist(int dims) {
		this(dims,false,1.0) ;
	}
	
	public CPUHeatDist(int dims,boolean w) {
		this(dims,w,1.0) ;
	}
	
	public CPUHeatDist(int dims, boolean weighted, double k) {
		///this.t=t;
		this.dims=dims ;
		this.k=k ;
		
		dist = new CPUL2Dist2(dims,weighted) ;
		timesminus4t = new CPUTimesVals(1, 1) ;
		CPUExp exp = new CPUExp(1) ;
		timesFact = new CPUTimesVals(1, 1) ;
		this.addModule(dist);
		this.addModule(timesminus4t);
		this.addModule(exp);
		this.addModule(timesFact);
		this.weighted=weighted ;
		
	}
	
	public void forward(Tensor input) {
		super.forward(input);
		
	}
	
	// ts est est array de double correspondant au terme "delta_t" de l'équation de la chaleur.
	// Il doit contenir autant de double qu'il y a de lignes dans les données qu'on lui passe.
	public void setT(ArrayList<Double> ts) {
		int batchsize=ts.size() ;
		CPUMatrix matexpo = new CPUMatrix(batchsize, 1) ;
		CPUMatrix matfact = new CPUMatrix(batchsize, 1) ;
		for(int i=0 ; i<batchsize ; i++) {
			for(int j=0 ; j<1 ; j++) {
				matexpo.setValue(i, j, -1/(4*ts.get(i)*k));
				matfact.setValue(i, j, Math.pow(4*Math.PI*ts.get(i)*k,-((double)dims)/2.0));
			}
		}
		this.timesminus4t.setVals(matexpo);
		this.timesFact.setVals(matfact);
	}
	
	

	// Calcule rapide d'un heatkernel, sans forward ni rien.
	public double heatkernel(int dim, CPUParams x1, CPUParams x2, double t) {
		
		double d = dist.dist(x1, x2) ;
		d=Math.exp(-d/(4*k*t)) ;
		d=d/Math.pow(4.0*Math.PI*k*t, ((double)dim)/2.0) ;
		return d ;
		
	}
	
	// Calcul la température max atteinte par x2 pour une source x1.
	public double heatmax(int dim, CPUParams x1, CPUParams x2) {
		double d = dist.dist(x1, x2) ;
		return heatkernel(dim,x1,x2,d/(2*dim)) ;
	}
	
	// Calcul la chaleur max transmise avant un horizon T
	public double heatmaxBeforeT(int dim, CPUParams x1, CPUParams x2,double T) {
		double d = dist.dist(x1, x2) ;
		if(d/(2*dim)<=T) 
			return heatkernel(dim,x1,x2,d/(2*dim)) ;
		else 
			return heatkernel(dim,x1,x2,T) ;
	}
	
	// Approximation linéaire de l'instant ou threshold est dépassé entre x1 et x2.
	// Hypothese : le threshold est bien depasse a un moment.
	public  double timeWhen(int dim, CPUParams x1, CPUParams x2, double threshold) {
		
		// Pour l'instant, on fait une approximation.
		double max = heatmax(dim, x1, x2) ;
		double d2 = dist.dist(x1,x2) ;
		return ((d2*threshold) / (2*dim*max)) ;
		
		
	}
	
	public static void main(String[] args) {
		CPUParams z0 = new CPUParams(3,3,0,-1,10) ;
		CPUParams z1 = new CPUParams(3,3,1,-1,10) ;
		ArrayList<Parameter> p0 = z0.getParamList().getParams() ;
		ArrayList<Parameter> p1 = z1.getParamList().getParams() ;
		
		p0.get(0).setVal(1); ;p0.get(1).setVal(1) ; p0.get(2).setVal(1) ; 
		p0.get(3).setVal(1); ;p0.get(4).setVal(2) ; p0.get(5).setVal(1) ; 
		p0.get(6).setVal(3); ;p0.get(7).setVal(2) ; p0.get(8).setVal(1) ; 
		
		p1.get(0).setVal(2); ;p1.get(1).setVal(2) ; p1.get(2).setVal(2) ; 
		p1.get(3).setVal(0); ;p1.get(4).setVal(0) ; p1.get(5).setVal(0) ; 
		p1.get(6).setVal(1); ;p1.get(7).setVal(2) ; p1.get(8).setVal(3) ; 
		
		z0.forward(null ) ;
		z1.forward(null );
		System.out.println(z0.getOutput());
		System.out.println(z1.getOutput());
		
		TableModule t = new TableModule() ;
		t.addModule(z0);t.addModule(z1);
		
		CPUHeatDist h = new CPUHeatDist(3) ;
		
		SequentialModule s = new SequentialModule() ;
		s.addModule(t);
		s.addModule(h);
		
		ArrayList<Double> times = new ArrayList<Double>() ;
		times.add(1.0);times.add(3.0);times.add(4.0);
		
		h.setT(times);
		
		s.forward(null);
		System.out.println(s.getOutput());
		
		System.out.println(h.heatkernel(3, z0, z1, 1.0));
		
		
		
	}
	
	
	public int getNbParams() {
		return this.weighted ? dims : 0 ;
	}
	
	public void setParameters(Parameters pList) {
		
		if(weighted) {
			//System.out.println("Setting CPUHEAT param");
			this.dist.setParameters(pList);
		}
	}
	

		

	
}
