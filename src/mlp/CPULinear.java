package mlp;

public class CPULinear extends TensorModule {

	//protected CPUMatrix grad_parameters;
	//protected CPUMatrix direction;
	//protected CPUMatrix last_grad_parameters;
	///protected CPUMatrix last_direction;
	
	protected Tensor tensor_output;
	protected Tensor tensor_delta;

	public CPULinear(int _input_size,int _output_size)
	{
		super(_input_size,_output_size);
		/*input_size=_input_size;
		output_size=_output_size;
		parameters=new CPUMatrix(input_size,output_size);*/
		//grad_parameters=new CPUMatrix(input_size,output_size);
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//tensor_output.setMatrix(0, new CPUMatrix(1,output_size));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,input_size));
	}

	public CPULinear(TensorModule o)
	{
		super(o);
		//grad_parameters=new CPUMatrix(input_size,output_size);
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//tensor_output.setMatrix(0, new CPUMatrix(1,output_size));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,input_size));
	}

	
	public Module forwardSharedModule()
	{
		CPULinear ret=new CPULinear(this);
		ret.sharedForward=true;
		//addListener(ret);
		return(ret);
	}
	
	public Module parametersSharedModule()
	{
		return(new CPULinear(this));
	}
	
	public void allocate(int minibatch_size)
	{
		
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) {
				if(!this.sharedForward){
					tensor_output.getMatrix(0).clear();
				}
				return;
			}
			tensor_delta.getMatrix(0).transformTo(minibatch_size, input_size);
			tensor_output.getMatrix(0).transformTo(minibatch_size, output_size);
			if(!this.sharedForward){
				tensor_output.getMatrix(0).clear();
			}
		}
		else{
			tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,input_size));
			tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,output_size));
		}
	}


	@Override
	/**
	 * Computes output values from inputs and stores the results in the matrix at index 0 of the instance variable tensor_output 
	 * @param input input values are contained in matrix 0 of this tensor 
	 * 
	 * The output tensor must not be deleted by the user
	 */
	public void forward(Tensor input) {
		
		assert(input.getNumberOfMatrices()==1);
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		allocate(minibatch_size);

		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			parameters=((TensorModule)this.origin_module).parameters;
			return;
		}
		
		if(paramsChanged){
			majParams();
		}
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		//tensor_delta.ensureCPUMatrices();

		CPUMatrix _parameters=(CPUMatrix)(parameters);
		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		CPUMatrix _output=(CPUMatrix)(tensor_output.getMatrix(0));

		double[] params=_parameters.getValues();
		double[] in=_input.getValues();
		double[] out=_output.getValues();
		
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			/*for(int idx_output=0;idx_output<output_size;idx_output++)
			{
				out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=0.0f;
			}*/
			

				for(int idx_input=0;idx_input<input_size;idx_input++)
				{
					for(int idx_output=0;idx_output<output_size;idx_output++)
					{
						//std::cout << "computing : " << params[IDX2C(idx_input,idx_output,input_size)] << "*" << in[IDX2C(nbexample,idx_input,minibatch_size)] << std::endl;
						out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]+=params[Matrix.IDX2C(idx_input,idx_output,input_size)]*in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)];
					}
				}
		}
	}

	@Override
	public Tensor getOutput() {
		if(sharedForward){
			this.origin_module.getOutput();
		}
		return(tensor_output);
	}

	@Override
	public void backward_updateGradient(Tensor input, Tensor deltas_output) {
		if(locked==true){
			return;
		}
		if(sharedForward){
			if(this.origin_module.locked){
				return;
			}
		}
		
		//System.out.println("deltas in cpulinear = "+deltas_output);
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		//tensor_delta.ensureCPUMatrices();

		//CPUMatrix _parameters=(CPUMatrix)(parameters);
		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		int minibatch_size=input.getMatrix(0).getNumberOfRows();

		double[] in=_input.getValues();
		double[] d_out=null;
		if (deltas_output!=null){
			deltas_output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
			d_out=_output.getValues();
		}
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			
				for(int idx_output=0;idx_output<output_size;idx_output++)
				{
					for(int idx_input=0;idx_input<input_size;idx_input++)
					{
						double o=1.0f;
						if(d_out!=null){o=d_out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];}
						double val=o*in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)];
						if(Double.isNaN(val)){
							throw new RuntimeException(this+".updateGradient: NaN val computed => "+o+" * "+in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]);		
						}
						Parameter p=paramList.get(Matrix.IDX2C(idx_input,idx_output,input_size));
						double g=p.gradient;
						/*if((this.name.equals("right0")) || (this.name.equals("right1"))){
							System.out.println(this+" "+name+" => add grad : "+val+" "+in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]);
						}*/
						g+=val;
						if(Double.isNaN(g)){
							throw new RuntimeException(this+".updateGradient: NaN gradient computed => "+p.gradient+" + "+val);		
						}
						
						p.gradient=g;
						
						//g_params[Matrix.IDX2C(idx_input,idx_output,input_size)]+=d_out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]*in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)];
					}
				}
		}
	}

	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltas_output) {
		
		if(sharedForward){
			if(this.origin_module.locked){
				return;
			}
		}
		//tensor_output.ensureCPUMatrices();
		tensor_delta.ensureCPUMatrices();

		int minibatch_size=input.getMatrix(0).getNumberOfRows();

		CPUMatrix _parameters=(CPUMatrix)(parameters);
		CPUMatrix _tensor_delta=(CPUMatrix)(tensor_delta.getMatrix(0));

		double[] params=_parameters.getValues();

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
			for(int idx_input=0;idx_input<input_size;idx_input++)
			{
				d_in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]=0.0f;

				for(int idx_output=0;idx_output<output_size;idx_output++)
				{
					double o=1.0f;
					if(d_out!=null){o=d_out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];}
					double val=params[Matrix.IDX2C(idx_input,idx_output,input_size)]*o;
					if(Double.isNaN(val)){
						throw new RuntimeException(this+".computeDeltas: NaN val computed => "+o+" * "+params[Matrix.IDX2C(idx_input,idx_output,input_size)]);		
					}
					double din=d_in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]+val;
					if(Double.isNaN(din)){
						throw new RuntimeException(this+".computeDeltas: NaN delta computed => "+d_in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]+" + "+val);		
					}
					d_in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]=din;
				
				}
			}
		}		

	}

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}

	/*@Override
	public void init_gradient()
	{
		//grad_parameters.fill(0.0f);
		int nb=input_size*output_size;
		for(int i=0;i<nb;i++){
			paramList[i].gradient=0.0f;
		}
	}*/
	
	/*public Tensor getParameters()
	{
		Tensor ret=new Tensor(2);
		ret.setMatrix(0, parameters);
		ret.setMatrix(1, grad_parameters);
		ret.setMatrix(0, parameters);
		
		return(ret);
	}*/

	/*@Override
	public void updateParameters(double gradient_step) {
		CPUMatrix _parameters=(CPUMatrix)(parameters);
		assert(_parameters!=null);
		
		double[] params=_parameters.getValues();
		double[] g_params=grad_parameters.getValues();

		for(int i=0;i<input_size*output_size;i++)
		{
			params[i]-=gradient_step*(g_params[i]);
		}
	}*/
	
	public String toString()
	{
		return("CPULinear: "+input_size+" ; "+output_size);
	}

	public Module copyModuleToGPU()
	{
		if (origin_module!=null)
		{
			Module module=new GPULinear((TensorModule)origin_module);
			return(module);
			//System.out.println("WARNING : Copying a shared version of a linear module to GPU : Shared connection lost...");
		}
		else{
			//GPUMatrix gpu_parameters=(GPUMatrix)(parameters.copyToGPU());
			Module module=new GPULinear(this);
			return(module);
		}
		
	}

}
