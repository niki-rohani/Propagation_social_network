package mlp;

import java.util.ArrayList;

public class CPUMatrixProduct extends Module {
	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int nbDims; // number of cols of Matrix1
	protected int size; // number of cols of output matrix 
	
	
	public CPUMatrixProduct(int nbDims, int size)
	{
		this.size=size;
		this.nbDims=nbDims;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(2);
		//tensor_output.setMatrix(0, new CPUMatrix(1,size));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,nbDims));
		//tensor_delta.setMatrix(1, new CPUMatrix(nbDims,size));
	}
	public CPUMatrixProduct(CPUMatrixProduct org){
		this(org.nbDims,org.size);
		this.origin_module=org;
	}
	
	public Module forwardSharedModule()
	{
		CPUMatrixProduct ret=new CPUMatrixProduct(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	public Module parametersSharedModule()
	{
		return(new CPUMatrixProduct(nbDims,size));
	}


	@Override
	public int getNbInputMatrix(){
		return 2;
	}
	
	void allocate(int minibatch_size)
	{
		
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) return;
			tensor_delta.getMatrix(0).transformTo(minibatch_size, nbDims);
			tensor_delta.getMatrix(1).transformTo(nbDims,size);
			tensor_output.getMatrix(0).transformTo(minibatch_size, size);
		}
		else{
		
			tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,nbDims));
			tensor_delta.setMatrix(1,new CPUMatrix(nbDims,size));
	    
			tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,size));
		}
	}


	@Override
	public void forward(Tensor input) {
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		int k=input.getMatrix(0).getNumberOfColumns();
		if(k!=nbDims){
			throw new RuntimeException("Format pb on input matrix 1 of CPUproduct");
		}
		k=input.getMatrix(1).getNumberOfRows();
		if(k!=nbDims){
			throw new RuntimeException("Format pb on input matrix 2 of CPUproduct");
		}
		allocate(minibatch_size);
		
		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			return;
		}
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		
		CPUMatrix _output=(CPUMatrix)tensor_output.getMatrix(0);
		

		double[] out=_output.getValues();
		CPUMatrix _input1=(CPUMatrix)input.getMatrix(0);
		CPUMatrix _input2=(CPUMatrix)input.getMatrix(1);
		double[] in1=_input1.getValues();
		double[] in2=_input2.getValues();
		
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int j=0;j<size;j++){
				out[Matrix.IDX2C(nbexample,j,minibatch_size)]=0.0f;
			}
		}
		
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int i=0;i<nbDims;i++){
				double i1=in1[Matrix.IDX2C(nbexample,i,minibatch_size)];
				for(int j=0;j<size;j++){
					double i2=in2[Matrix.IDX2C(i,j,nbDims)];
					out[Matrix.IDX2C(nbexample,j,minibatch_size)]+=i1*i2;
				}
				
			}
		}

	}

	@Override
	public Tensor getOutput() {
		return(tensor_output);
	}

	@Override
	public void backward_updateGradient(Tensor input, Tensor deltas_output) {
		return;
	}

	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltas_output) {
		
		tensor_delta.ensureCPUMatrices();

		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		int k=input.getMatrix(0).getNumberOfColumns();
		if(k!=nbDims){
			throw new RuntimeException("Format pb on input matrix 1 of CPUproduct");
		}
		k=input.getMatrix(1).getNumberOfRows();
		if(k!=nbDims){
			throw new RuntimeException("Format pb on input matrix 2 of CPUproduct");
		}
		
		double[] d_out=null;
		if (deltas_output!=null){
			deltas_output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
			d_out=_output.getValues();
		}
		input.ensureCPUMatrices();
		CPUMatrix _input1=(CPUMatrix)input.getMatrix(0);
		CPUMatrix _input2=(CPUMatrix)input.getMatrix(1);
		double[] in1=_input1.getValues();
		double[] in2=_input2.getValues();
		
		//Computation of deltas_input
		CPUMatrix _tensor_delta=(CPUMatrix)(tensor_delta.getMatrix(0));
		double[] d_in=_tensor_delta.getValues();
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int i=0;i<nbDims;i++){
				d_in[Matrix.IDX2C(nbexample,i,minibatch_size)]=0.0f;
				for(int j=0;j<size;j++){
					double o=1.0f;
					if(d_out!=null){o=d_out[Matrix.IDX2C(nbexample,j,minibatch_size)];}
					double val=in2[Matrix.IDX2C(i,j,nbDims)]*o;
					if(Double.isNaN(val)){
						throw new RuntimeException(this+".computeDeltas: NaN val computed => "+o+" * "+in2[Matrix.IDX2C(i,j,nbDims)]);		
					}
					d_in[Matrix.IDX2C(nbexample,i,minibatch_size)]+=val;
				}
			}
			
		}
		
		_tensor_delta=(CPUMatrix)(tensor_delta.getMatrix(1));
		d_in=_tensor_delta.getValues();
		for(int i=0;i<nbDims;i++){
			for(int j=0;j<size;j++){
				d_in[Matrix.IDX2C(i,j,nbDims)]=0.0f;
				for(int nbexample=0;nbexample<minibatch_size;nbexample++)
				{
					double o=1.0f;
					if(d_out!=null){o=d_out[Matrix.IDX2C(nbexample,j,minibatch_size)];}
					double val=o*in1[Matrix.IDX2C(nbexample,i,minibatch_size)];
					if(Double.isNaN(val)){
						throw new RuntimeException(this+".computeDeltas: NaN val computed => "+o+" * "+in1[Matrix.IDX2C(nbexample,i,minibatch_size)]);		
					}
					d_in[Matrix.IDX2C(i,j,nbDims)]+=val;
				}
			}
			
		}

	}

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}

}
