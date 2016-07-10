package mlp;

public class CPUSplit extends Module {
	protected int indexSplit;  
	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int size;
	
	
	public CPUSplit(int size,int indexSplit){
		this.size=size;
		if(indexSplit<=0){
			throw new RuntimeException("Wrong split => index split ="+indexSplit);
		}
		this.indexSplit=indexSplit;
		tensor_output=new Tensor(2);
		tensor_delta=new Tensor(1);
	}
	public CPUSplit(CPUSplit mod){
		this(mod.size,mod.indexSplit);
		this.origin_module=mod;
	}
	
	public Module forwardSharedModule()
	{
		CPUSplit ret=new CPUSplit(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	
	public Module parametersSharedModule()
	{
		return(new CPUSplit(this));
	}

	void allocate(int minibatch_size)
	{
		
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) return;
			tensor_delta.getMatrix(0).transformTo(minibatch_size, size);
			tensor_output.getMatrix(0).transformTo(minibatch_size, indexSplit);
			tensor_output.getMatrix(0).transformTo(minibatch_size, size-indexSplit);
			
		}
		else{
			tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,size));
			tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,indexSplit));
			tensor_output.setMatrix(1,new CPUMatrix(minibatch_size,size-indexSplit));
		}
	}
	
	@Override
	public void forward(Tensor input) {
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		allocate(minibatch_size);

		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			tensor_output.setMatrix(1, this.origin_module.getOutput().getMatrix(1));
			return;
		}
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		
		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		CPUMatrix _output1=(CPUMatrix)(tensor_output.getMatrix(0));
		CPUMatrix _output2=(CPUMatrix)(tensor_output.getMatrix(1));
		
		double[] in=_input.getValues();
		double[] out1=_output1.getValues();
		double[] out2=_output2.getValues();
		
		double v=0.0f;
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int idx_output=0;idx_output<size;idx_output++)
			{
				if(idx_output<indexSplit){
					out1[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];
				}
				else{
					out2[Matrix.IDX2C(nbexample,idx_output-indexSplit,minibatch_size)]=in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];
				}
			
			}
			
		}
		//System.out.println(this+" "+_output);
	}
	
	@Override
	public Tensor getOutput()
	{		
		return(tensor_output);
	}
	
	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltasOutput) {
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		tensor_delta.ensureCPUMatrices();

		int minibatch_size=input.getMatrix(0).getNumberOfRows();

		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		CPUMatrix _tensor_delta=(CPUMatrix)(tensor_delta.getMatrix(0));
		double[] in=_input.getValues();
		double[] d_in=_tensor_delta.getValues();
		double[] d_out1=null;
		if (deltasOutput!=null){
			deltasOutput.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltasOutput.getMatrix(0));
			d_out1=_output.getValues();
		}
		double[] d_out2=null;
		if (deltasOutput!=null){
			deltasOutput.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltasOutput.getMatrix(1));
			d_out2=_output.getValues();
		}
		
		//Computation of deltas_input
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int idx_input=0;idx_input<size;idx_input++)
			{
				int idx=Matrix.IDX2C(nbexample,idx_input,minibatch_size);
				double o=1.0f;
				if(idx_input<indexSplit){
					if(d_out1!=null){o=d_out1[idx];}
				}
				else{
					if(d_out2!=null){o=d_out2[Matrix.IDX2C(nbexample,idx_input-indexSplit,minibatch_size)];}
				}
				
				d_in[idx]=o;
			}
		}		
	}

	@Override
	public void backward_updateGradient(Tensor input, Tensor deltasOutput) {
		//Nothing to do
	}

	

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}

	
	public String toString()
	{
		String r="CPUSplit_"+this.name+": size="+size+" split="+indexSplit;
		return(r);
	}	


	
	// TODO
	public Module copyModuleToGPU()
	{
		
		return null;
	}
	

}
