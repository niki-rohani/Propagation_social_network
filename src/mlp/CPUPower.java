package mlp;

public class CPUPower extends Module {
	double power=2;
	int size;
	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	
	public CPUPower(int size,double power){
		this.power=power;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		this.size=size;
		//tensor_output.setMatrix(0, new CPUMatrix(1,size));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
	}
	
	public CPUPower(CPUPower mod){
		this(mod.size,mod.power);
		this.origin_module=mod;
	}
	
	public Module forwardSharedModule()
	{
		CPUPower ret=new CPUPower(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	
	public Module parametersSharedModule()
	{
		return(new CPUPower(this));
	}

	void allocate(int minibatch_size)
	{
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) return;
			tensor_delta.getMatrix(0).transformTo(minibatch_size, size);
			tensor_output.getMatrix(0).transformTo(minibatch_size, size);
		}
		else{
			tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,size));
			tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,size));
		}
	}
	
	@Override
	public void forward(Tensor input) {
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
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
				out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=(double)(Math.pow(in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)],power));
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
		double[] d_out=null;
		if (deltasOutput!=null){
			deltasOutput.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltasOutput.getMatrix(0));
			d_out=_output.getValues();
		}
		
		//Computation of deltas_input
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int idx_input=0;idx_input<size;idx_input++)
			{
				int idx=Matrix.IDX2C(nbexample,idx_input,minibatch_size);
				double o=1.0f;
				if(d_out!=null){o=d_out[idx];}
				double v=(double)(power*Math.pow(in[idx],power-1));
				if(Double.isInfinite(v)){
					v=Double.MAX_VALUE;
				}
				double val=(double)(o*v);
				if(Double.isNaN(val)){
					throw new RuntimeException(this+".computeDeltas: NaN val computed => "+o+" * "+v);		
				}
				d_in[idx]=val;
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
		String r="CPUPower_"+this.name+"("+power+"): "+size;
		return(r);
	}	


	
	// TODO
	public Module copyModuleToGPU()
	{
		
		return null;
	}
	
	
	

}
