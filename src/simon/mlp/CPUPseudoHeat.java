package simon.mlp;

import mlp.CPUAddVals;
import mlp.CPUExp;
import mlp.CPUParams;
import mlp.CPUTimesVals;
import mlp.CPUPower;

public class CPUPseudoHeat extends CPUHeatDist {

	private double x ;
	private int mode = 0 ; //  0 : (1/d) // 1 : exp(truc-d)
	
	public CPUPseudoHeat(int dims, boolean weighted) {
		this(dims,weighted,0) ;
	}
	
	public CPUPseudoHeat(int dims, boolean weighted,int mode) {
		super(dims, weighted);
		x=dims*dims*dims ;
		
		this.mode = mode ;
		
		this.clearModules();
		this.dist=new CPUL2Dist2(dims, weighted) ;
		this.addModule(dist);
		//this.addModule(new CPUTimesVals(1,-1.0));
		//this.addModule(new CPUAddVals(1, x));
		switch(mode) {
		case 0 :
			this.addModule(new CPUPower(1, -1));
			break;
		case 1 :
			this.addModule(new CPUTimesVals(1, -1));
			this.addModule(new CPUAddVals(1,1));
			this.addModule(new CPUExp());
			break ;
		}
		
		
		
	}
	
	public double heatkernel(int dim, CPUParams x1, CPUParams x2, double t) {
		
		double d = dist.dist(x1, x2) ;
		
		switch(mode) {
		case 0 :
			return 1/d ;
		case 1 :
			return Math.exp(1-d) ;
		}
		return 1/d ;
		
	}
	
	// Calcul la température max atteinte par x2 pour une source x1.
	public double heatmax(int dim, CPUParams x1, CPUParams x2) {
		return heatkernel(dim,x1,x2,0) ;
	}
	
	// Calcul la chaleur max transmise avant un horizon T
	public double heatmaxBeforeT(int dim, CPUParams x1, CPUParams x2,double T) {
		return heatmax(dim,x1,x2) ;
	}
	
	// Approximation linéaire de l'instant ou threshold est dépassé entre x1 et x2.
	// Hypothese : le threshold est bien depasse a un moment.
	public  double timeWhen(int dim, CPUParams x1, CPUParams x2, double threshold) {
		return 1.0 ;

	}

}
