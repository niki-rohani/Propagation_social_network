package mlp;

public class CPUL2Norm extends Module {

	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int size;
	
	public CPUL2Norm(int s){
		size=s;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//tensor_output.setMatrix(0, new CPUMatrix(1,1));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
	}
	
	public CPUL2Norm(CPUL2Norm org)
	{
		this(org.size);
		this.origin_module=org;
		
	}
	
	public Module forwardSharedModule()
	{
		CPUL2Norm ret=new CPUL2Norm(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	public Module parametersSharedModule()
	{
		return(new CPUL2Norm(size));
	}

	void allocate(int minibatch_size)
	{
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) return;
			tensor_delta.getMatrix(0).transformTo(minibatch_size, size);
			tensor_output.getMatrix(0).transformTo(minibatch_size, 1);
		}
		else{
			tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,size));
			tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,1));
		}
	}
	
	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltasOutput) {
		
		tensor_delta.ensureCPUMatrices();

		int minibatch_size=input.getMatrix(0).getNumberOfRows();

		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		CPUMatrix _tensor_delta1=(CPUMatrix)(tensor_delta.getMatrix(0));

		double[] d_in1=_tensor_delta1.getValues();
		double[] in=_input.getValues();

		
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
				if(d_out!=null){o=d_out[nbexample];}
				
				double val=0.0f;
				if(o!=0.0f){
					val=o*in[idx]*2.0;
				}
				if(Double.isNaN(val)){
					throw new RuntimeException(this+".computeDeltas: NaN val computed => "+o+" * "+in[idx]);		
				}
				d_in1[idx]=val;
				//System.out.println("CPUL2Norm "+val);
			}
		}		
	}

	@Override
	public void backward_updateGradient(Tensor input, Tensor deltasOutput) {
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
		
		CPUMatrix _input1=(CPUMatrix)(input.getMatrix(0));
		CPUMatrix _output=(CPUMatrix)(tensor_output.getMatrix(0));
		
		double[] in1=_input1.getValues();
		double[] out=_output.getValues();
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			out[Matrix.IDX2C(nbexample,0,minibatch_size)]=0;

			for(int idx_output=0;idx_output<size;idx_output++)
			{
				double f=in1[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];
				out[Matrix.IDX2C(nbexample,0,minibatch_size)]+=f*f;
			}
		}		
		//System.out.println(_output);
	}

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}

	@Override
	public Tensor getOutput() {
		return(tensor_output);
	}

}
