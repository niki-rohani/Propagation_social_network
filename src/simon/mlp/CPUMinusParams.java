package simon.mlp;

import mlp.CPUMatrix;
import mlp.CPUParams;
import mlp.CPUSum;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.CPUPower;
import mlp.Tensor;

public class CPUMinusParams extends SequentialModule {
	
	//private int nb ;
	private CPUPower cpuIn1 ;
	private CPUParams cpuIn2 ;
	private TableModule inputM = new TableModule() ;
	
	public CPUMinusParams() {
		//this.nb=nb ;
		
		inputM = new TableModule() ;
		cpuIn1 = new CPUPower(1, 1.0) ;
		cpuIn2 = null ;
		inputM.addModule(cpuIn1);
		inputM.addModule(cpuIn2);
		CPUSum sum = new CPUSum(1,2) ;
		CPUMatrix w = new CPUMatrix(1,2) ;
		w.setValue(0, 0, 1.0);
		w.setValue(0, 1, -1.0);
		sum.setWeights(w);
		this.addModule(inputM);
		this.addModule(sum);
		
	}
	
	public void forward(Tensor input) {
		//inputM.clearModules();
		if(input.getNumberOfMatrices()>1) {
			throw new RuntimeException("Error : can't use CPUMinusParams on more than 1 matrix.") ;
		}
		Tensor newinput = new Tensor(input.getMatrix(0)) ;
		//System.out.println(newinput);
		//input.setMatrix(1, null);
		super.forward(newinput);
		
	}
	
	public void resetParams(int nb) {
		this.cpuIn2 = new CPUParams(nb, 1) ;
		inputM.setModule(1, cpuIn2);
	}
	
	public void addParam(CPUParams p) {
		if(p.getNbParams()!=1) {
			throw new RuntimeException("Error : too much parameters.") ;
		}
		this.cpuIn2=p;
		inputM.setModule(1, cpuIn2);
		
	}

}
