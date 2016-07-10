package mlp;


public class CPUTanh extends Module
{
	//protected CPUMatrix activations;
	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int size;
	protected double alpha;
	protected double lambda;
	
	public CPUTanh(int _size, double alpha, double lambda){
		size=_size;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//activations=new CPUMatrix();
		this.alpha=alpha;
		this.lambda=lambda;
		//tensor_output.setMatrix(0, new CPUMatrix(1,size));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
	}
	
	
	public CPUTanh(int _size)
	{	
		this(_size,1.7159f,0.6666f);
	}

	public CPUTanh(CPUTanh org){
		this(org.size,org.alpha,org.lambda);
		this.origin_module=org;
	}

	public Module forwardSharedModule()
	{
		CPUTanh ret=new CPUTanh(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	
	public Module parametersSharedModule()
	{
		return(new CPUTanh(size,alpha,lambda));
	}

	public void allocate(int minibatch_size)
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
		//activations=new CPUMatrix(minibatch_size,size);
	}

	public double getTanhValue(double x)
	{
		double v=(double)(alpha*Math.tanh(lambda*x));
		 return(v);
	}

	public double getDTanhValue(double x)
	{
		double t=(double)(Math.tanh(lambda*x));
	    double v=(double)(lambda*alpha*(1.0-t*t));
		return(v);
	}

	/**
	 * The output tensor must not be deleted by the user
	 */
	public void forward(Tensor input)
	{	
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
				//act[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=in[IDX2C(nbexample,idx_output,minibatch_size)];
				out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=getTanhValue(in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]);
			}
		}

	}

	public Tensor getOutput()
	{		
		return(tensor_output);
	}

	/**
	 * 
	 **/
	public void init_gradient()
	{		
	}

	

	/**
	 * 
	 **/
	public void backward_updateGradient(Tensor input,Tensor deltas_output)
	{		
	}

	public void backward_computeDeltaInputs(Tensor input,Tensor deltas_output)
	{
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		tensor_delta.ensureCPUMatrices();

		int minibatch_size=input.getMatrix(0).getNumberOfRows();

		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		CPUMatrix _tensor_delta=(CPUMatrix)(tensor_delta.getMatrix(0));

		double[] in=_input.getValues();
		double[] d_in=_tensor_delta.getValues();
		double[] d_out=null;
		if (deltas_output!=null){
			deltas_output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
			d_out=_output.getValues();
			if(_output.number_of_columns!=size){
				System.out.println(_output);
				throw new RuntimeException(this+" : Bad number of columns in the deltas_output matrix");
			}
		}
		
		//Computation of deltas_input
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int idx_input=0;idx_input<size;idx_input++)
			{
				int idx=Matrix.IDX2C(nbexample,idx_input,minibatch_size);
				double o=1.0f;
				if(d_out!=null){
					o=d_out[idx];
				}
				d_in[idx]=getDTanhValue(in[idx])*o;
			}
		}		

	}

	public Tensor getDelta() {return(tensor_delta);}

	/* void push()
	{
	}

	virtual void pop()
	{
	}*/

	public String toString()
	{
		String r="CPUTanh("+alpha+","+lambda+"): "+size;
		return(r);
	}	

	
	// TODO
	public Module copyModuleToGPU()
	{
		//return(new GPUTanh(size,alpha,lambda));
		return null;
	}

}



