package simon.mlp;

import mlp.CPUMatrix;
import mlp.Matrix;
import mlp.Module;
import mlp.Tensor;

public class CPUSmoothMaxRows extends Module {

	private double alpha ;
	private int size ;
	
	private double[] lastOutput ;
	private Tensor delta ;
	private double[] lastSums;
	private double[] lastAboveForGradient ;
	private int lastNbBatch;
	private Tensor lastInput; 
	
	
	public CPUSmoothMaxRows(int size , double alpha) {
		this.alpha=alpha ;
		this.size = size ;
	}
	
	@Override
	public void forward(Tensor input) {
		
		if(input.getNumberOfMatrices()!=1) {
			throw new RuntimeException("Bad number of matrices, expected 1, got "+input.getNumberOfMatrices()) ;
		}
		
		
		
		Matrix mi = input.getMatrix(0) ;
		if(mi.getNumberOfColumns() != size)
			throw new RuntimeException("Bad matrix size, expected"+this.size+", got "+mi.getNumberOfColumns()) ;
		int nbBatch = mi.getNumberOfRows() ;
		this.lastOutput=new double[size] ;
		
		double[] sumAbove = new double[size] ;
		double[] sumBelow = new double[size] ;
		//this.lastAboveForGradient=new double[size] ;
		for(int i=0 ; i<size ; i++) {
			for(int nEx = 0 ; nEx<nbBatch ; nEx++) {
				double eax = Math.exp(alpha*mi.getValue(nEx, i)) ;
				sumAbove[i]+=mi.getValue(nEx, i)*eax ;
				sumBelow[i]+=eax ;
				//this.lastAboveForGradient[i] = eax ;
			}
			this.lastOutput[i]=sumAbove[i]/sumBelow[i] ;
		}
		
		this.lastSums = sumBelow ;
		this.lastInput=input ;
		this.lastNbBatch=nbBatch ;

	}

	@Override
	public Tensor getOutput() {
		Matrix m = new CPUMatrix(1,size) ;
		for(int i= 0 ; i<size ; i++) {
			m.setValue(0, i, this.lastOutput[i]);
		}
		return new Tensor(m);
	}
	
	@Override
	public void backward_updateGradient(Tensor input, Tensor deltas_output) {
		return ;
	}

	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltas_output) {
		Matrix mDelta = new CPUMatrix(input.getMatrix(0).getNumberOfRows(),input.getMatrix(0).getNumberOfColumns()) ;
		if(mDelta.getNumberOfRows()!=lastNbBatch) {
			throw new RuntimeException("Bad input size. Expected "+lastNbBatch+", got "+mDelta.getNumberOfRows()) ;
		}
		Matrix mi = input.getMatrix(0) ;
		
		for(int i=0 ; i<size ; i++) {
			for(int nEx = 0 ; nEx<lastNbBatch ; nEx++) {
				double v = Math.exp(alpha*mi.getValue(nEx, i)) ;
				v=v/this.lastSums[i] ;
				v=v*(1+(alpha*(mi.getValue(nEx, i)-lastOutput[i]))) ;
				
				double d=(deltas_output == null ? 1.0 : deltas_output.getMatrix(0).getValue(0, i)) ;
				
				mDelta.setValue(nEx, i, v*d);
				
			}
		}
		this.delta=new Tensor(mDelta) ;
	}

	@Override
	public Tensor getDelta() {
		return delta;
	}

}
