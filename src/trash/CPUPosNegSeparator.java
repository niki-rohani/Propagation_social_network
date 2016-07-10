package trash;

import mlp.*;

public class CPUPosNegSeparator extends Module{
	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int size; // number of input dims  
	
	
	public CPUPosNegSeparator(int size)
	{
		this.size=size;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//tensor_output.setMatrix(0, new CPUMatrix(1,size*2));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
	}
	public CPUPosNegSeparator(CPUPosNegSeparator org)
	{
		this(org.size);
		
	}
	
	public Module parametersSharedModule()
	{
		return(new CPUPosNegSeparator(this));
	}
	public Module forwardSharedModule()
	{
		CPUPosNegSeparator ret=new CPUPosNegSeparator(this);
		ret.sharedForward=true;
		return(ret);
	}

	void allocate(int minibatch_size)
	{
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) return;
			tensor_delta.getMatrix(0).transformTo(minibatch_size, size);
			tensor_output.getMatrix(0).transformTo(minibatch_size, size*2);
		}
		else{		
			tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,size));
			tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,size*2));
		}
	}
	
	@Override
	public void forward(Tensor input) {
		
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		int s=input.getMatrix(0).getNumberOfColumns();
		if(s!=size){
			throw new RuntimeException(this+" : Input Format Problem => "+s+" columns "+size+" required");
		}
		allocate(minibatch_size);

		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			return;
		}
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		
		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		CPUMatrix _output=(CPUMatrix)(tensor_output.getMatrix(0));
		
		double[] in=_input.getValues();
		double[] out=_output.getValues();
		//double[] act=activations.getValues();
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int idx_output=0;idx_output<size;idx_output++)
			{
				double val=in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];
				out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=((val>=0)?val:0.0f);
				out[Matrix.IDX2C(nbexample,idx_output+size,minibatch_size)]=((val<0)?-val:0.0f);
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
		
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		tensor_delta.ensureCPUMatrices();

		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		int s=input.getMatrix(0).getNumberOfColumns();
		if(s!=size){
			throw new RuntimeException(this+" : Input Format Problem => "+s+" columns "+size+" required");
		}
		
		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		CPUMatrix _tensor_delta=(CPUMatrix)(tensor_delta.getMatrix(0));

		double[] in=_input.getValues();
		double[] d_in=_tensor_delta.getValues();
		double[] d_out=null;
		if (deltas_output!=null){
			deltas_output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
			d_out=_output.getValues();
		}
		
		//Computation of deltas_input
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int idx_input=0;idx_input<size;idx_input++)
			{
				int idx=Matrix.IDX2C(nbexample,idx_input,minibatch_size);
				double o1=1.0f;
				if(d_out!=null){o1=d_out[idx];}
				int idx2=Matrix.IDX2C(nbexample,idx_input+size,minibatch_size);
				double o2=1.0f;
				if(d_out!=null){o2=d_out[idx2];}
				double inV=in[idx];
				double val=0.0f;
				if(inV>=0.0f){
					val+=o1;
					//val-=o2/1000.0f;
				}
				if(inV<0.0f){
					val-=o2;
					//val+=o1/1000.0f;
				}
				//val=o1-o2;
				if(Double.isNaN(val)){
					throw new RuntimeException(this+".computeDeltas: NaN val computed => "+o1+" "+o2+" "+inV); 
				}
				d_in[idx]=val;
			}
		}		

	}

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}
	public String toString()
	{
		String r="CPUPosNegSeparator_"+name+": "+size;
		return(r);
	}	
}