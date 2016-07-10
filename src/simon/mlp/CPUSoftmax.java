package simon.mlp;

//import com.sun.xml.internal.ws.api.pipe.NextAction;

import mlp.Matrix;
import mlp.Module ;
import mlp.Tensor;
import mlp.CPUMatrix ;

public class CPUSoftmax extends Module {

	private double alpha ;
	
	private double[] lastOutput ;
	private double[] lastSums ;
	private Tensor lastInput ; 
	private int lastNbBatch ;
	
	private Tensor delta ; 
	
	public CPUSoftmax(double alpha) {
		this.alpha=alpha ;
	}

	@Override
	public void forward(Tensor input) {
		
		if(input.getNumberOfMatrices()!=1) {
			throw new RuntimeException("Bad number of matrices, expected 1, got "+input.getNumberOfMatrices()) ;
		}
		
		
		Matrix mi = input.getMatrix(0) ;
		int nbBatch = mi.getNumberOfRows() ;
		this.lastOutput=new double[nbBatch] ;
		
		double[] sumAbove = new double[nbBatch] ;
		double[] sumBelow = new double[nbBatch] ;
		for(int nEx = 0 ; nEx<nbBatch ; nEx++) {
			for(int i=0 ; i<mi.getNumberOfColumns() ; i++) {
				double eax = Math.exp(alpha*mi.getValue(nEx, i)) ;
				sumAbove[nEx]+=mi.getValue(nEx, i)*eax ;
				sumBelow[nEx]+=eax ;
			}
			this.lastOutput[nEx]=sumAbove[nEx]/sumBelow[nEx] ;
		}
		
		this.lastSums = sumBelow ;
		this.lastInput=input ;
		this.lastNbBatch=nbBatch ;
	}

	@Override
	public Tensor getOutput() {
		Matrix m = new CPUMatrix(lastNbBatch,1) ;
		for(int i= 0 ; i<lastNbBatch ; i++) {
			m.setValue(i, 0, this.lastOutput[i]);
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
		
		for(int nEx = 0 ; nEx<lastNbBatch ; nEx++) {
			for(int i=0 ; i<mDelta.getNumberOfColumns() ; i++) {
				
				double v = Math.exp(alpha*mi.getValue(nEx, i)) ;
				v=v/this.lastSums[nEx] ;
				v=v*(1+(alpha*(mi.getValue(nEx, i)-lastOutput[nEx]))) ;
				
				double d=deltas_output == null ? 1.0 : deltas_output.getMatrix(0).getValue(nEx, 1) ;
				
				mDelta.setValue(nEx, i, v*d);
				
			}
		}
		this.delta=new Tensor(mDelta) ;
	}

	@Override
	public Tensor getDelta() {
		return delta;
	}
	
	public static void main(String[] args) {
		CPUSoftmax sm = new CPUSoftmax(5) ;
		Matrix m = new CPUMatrix(3, 2);
		m.setValue(0, 0, 1);
		m.setValue(0, 1, 1);
		m.setValue(1, 0, 2);
		m.setValue(1, 1, 1);
		m.setValue(2, 0, 3);
		m.setValue(2, 1, 5);
		Tensor t = new Tensor(m) ;
		sm.forward(t);
		System.out.println(sm.getOutput().getMatrix(0));
		sm.backward_computeDeltaInputs(t);
		System.out.println(sm.getDelta().getMatrix(0));
	}
	
}
