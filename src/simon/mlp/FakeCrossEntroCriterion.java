package simon.mlp;

import java.util.ArrayList;

import mlp.CPUAddVals;
import mlp.CPUAverageCols;
import mlp.CPULog;
import mlp.CPUMatrix;
import mlp.CPUSum;
import mlp.CPUTimesVals;
import mlp.Criterion;
import mlp.Matrix;
import mlp.SequentialModule;
import mlp.Tensor;

public class FakeCrossEntroCriterion extends Criterion {
	
	//private Tensor tensor_delta ;
	private CPUTimesVals times ;
	private CPUAddVals adds ;
	private CPUTimesVals weights ;
	private SequentialModule seq ;
	private int posWeight ;
	private int s ;
	
	public FakeCrossEntroCriterion(int size,int posWeigth) {
		times = new CPUTimesVals(size) ;
		adds = new CPUAddVals(size) ;
		weights= new CPUTimesVals(size) ;
		
		this.seq = new SequentialModule();
		this.seq.addModule(times) ;
		this.seq.addModule(adds) ;
		this.seq.addModule(new CPULog(size)) ;
		this.seq.addModule(weights) ;
		this.seq.addModule(new CPUAverageCols(size,2)) ;
		s=size ;
		this.posWeight=posWeigth ;
	}
	
	public void setLabels(Tensor labels) {
		Matrix l = labels.getMatrix(0) ;
		if (l.getNumberOfColumns() != s) {
			throw new RuntimeException("Bad data size. Got "+labels.getMatrix(0).getNumberOfColumns()+", expected "+s) ;
		}
		CPUMatrix mtimes = l.copyToCPU() ;
		CPUMatrix madds = l.copyToCPU() ;
		CPUMatrix mweights = l.copyToCPU() ;
		for(int i = 0 ; i<l.getNumberOfRows() ; i++) {
			for(int j = 0 ; j<l.getNumberOfColumns() ; j++) {
				if(l.getValue(i,j) == 0) {
					mtimes.setValue(i, j, -1) ;
					madds.setValue(i, j, 1) ;
					mweights.setValue(i,j,-1) ;
				} else {
					madds.setValue(i, j, 0);
					mweights.setValue(i,j,-posWeight) ;
					
				}
			}
		}
		this.labels = labels ;
		times.setVals(mtimes);
		//System.out.println("mtimes"+mtimes);
		adds.setVals(madds);
		//System.out.println("madd"+madds);
		weights.setVals(mweights);
		
	}

	@Override
	public void backward(Tensor input, Tensor output) {
		seq.backward(input,output) ;
		
	}
	

	@Override
	public Tensor getDelta() {
		return seq.getDelta();
	}

	@Override
	public Tensor getValue() {
		return seq.getOutput();
	}

	@Override
	public Criterion copy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void forward(Tensor input) {
		//System.out.println("input "+input);
		//System.out.println("lables "+labels); 
		seq.forward(input) ;
		
	}
	
	public Tensor getOutput() {
		return seq.getOutput() ;
	}

}
