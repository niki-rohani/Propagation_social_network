package simon.mlp;

import mlp.CPUAverageCols;
import mlp.CPUL2Norm;
import mlp.CPULinear;
import mlp.CPUMatrixProduct;
import mlp.CPUParams;
import mlp.CPUPower;
import mlp.Matrix;
import mlp.Module;
import mlp.Parameters;
import mlp.SequentialModule;
import mlp.Tensor;
import mlp.CPUMatrix ;
import mlp.TensorModule;

public class CPUL2WeightedNorm extends SequentialModule {

	
	private CPULinear lin ;
	private CPUParams weights ;
	
	public CPUL2WeightedNorm(int dim) {
		
		super() ;
		CPUPower pow = new CPUPower(dim, 2) ;
		lin = new CPULinear(dim,1) ;
		
		this.addModule(pow);
		//this.addModule(lin);
		this.addModule(new CPUAverageCols(dim,2));
		
		
	}
	
	
	public void setParameters(Parameters pList) {
		//System.out.println("Hummm");
		//System.out.println(lin.getNbParams());
		lin.setParameters(pList);
		//weights.setParameters(pList);
	}
	
	public int getNbParams() {
		return lin.getNbParams();
	}
	
	public Tensor getParameters(){
		
		return lin.getParameters() ;
	}
	
	
	
	public static void main(String[] args) {
		CPUL2WeightedNorm n = new CPUL2WeightedNorm(2) ;
		Parameters p = new Parameters(2) ;
		p.allocateNewParamsFor(n,1);
		CPUMatrix m = new CPUMatrix(1,2) ;
		m.setValue(0, 0, 1.0);
		m.setValue(0, 1, 2.0);
		n.forward(new Tensor(m));
		System.out.println(n.getOutput()) ;
		n.backward(new Tensor(m), new Tensor(new CPUMatrix(1,2,1.0)));
		System.out.println(n.lin.getDelta());
		n.lin.updateParameters(0.1);
		System.out.println(n.lin.getParamList());
		
	}
	
}
