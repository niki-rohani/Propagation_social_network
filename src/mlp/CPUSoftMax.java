package mlp;

public class CPUSoftMax extends Module {
	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int size;
	protected double alpha;
	protected int mode_deltas; // 0 true derivative, 1 => exp(alpha*xi)/Sum_j(exp(alpha*xj)) 
	
	public CPUSoftMax(double alpha){
		this(1,alpha);
	}
	
	public CPUSoftMax(int size,double alpha){
		this.alpha=alpha;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		this.size=size;
	}
	
	public CPUSoftMax(int size,double alpha,int mode_deltas){
		this(size,alpha);
		this.mode_deltas=mode_deltas;	
	}
	
	public CPUSoftMax(CPUSoftMax mod){
		this(mod.size,mod.alpha,mod.mode_deltas);
		this.name=mod.name;
		this.origin_module=mod;
	}
	
	public Module forwardSharedModule()
	{
		CPUSoftMax ret=new CPUSoftMax(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	public Module parametersSharedModule()
	{
		return(new CPUSoftMax(this));
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
		//
		double sum_num=0;
		double sum_den=0;
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			sum_num=0;
			sum_den=0;
			double tab[]=new double[size];
			for(int idx_output=0;idx_output<size;idx_output++)
			{
				double v=in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];
				tab[idx_output]=v;
				double x=Math.exp(alpha*v);
				sum_num+=v*x;
				sum_den+=x;
			}
			
			double v=0.0;
			if(sum_den!=0.0){
				v=sum_num/sum_den;
			}
			else{
				String st="[";
				for(double r:tab){
					st+=r+",";
				}
				st+="]";
				throw new RuntimeException("CPUSoftMax : pb null denominator ! "+st);
			}
			out[Matrix.IDX2C(nbexample,0,minibatch_size)]=v;

		}
		//System.out.println(this+" "+_output);
	}

	@Override
	public Tensor getOutput()
	{		
		return(tensor_output);
	}


	@Override
	public void backward_updateGradient(Tensor input, Tensor deltasOutput) {
		//Nothing to do
	}

	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltas_output) {
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		tensor_delta.ensureCPUMatrices();

		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		int nbCols=input.getMatrix(0).getNumberOfColumns();
		
		if(nbCols!=size){
			throw new RuntimeException(this+" : number of input cols != size : "+nbCols+" != "+size);
		}
		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		CPUMatrix _tensor_delta=(CPUMatrix)(tensor_delta.getMatrix(0));

		double[] in=_input.getValues();
		double[] d_in=_tensor_delta.getValues();
		double[] d_out=null;
		//System.out.println(minibatch_size+","+size+" "+_tensor_delta.number_of_rows+","+_tensor_delta.number_of_columns);
		if (deltas_output!=null){
			deltas_output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
			if(_output.getNumberOfColumns()!=1){
				throw new RuntimeException("Bad number of cols of deltas_output");
			}
			d_out=_output.getValues();
		}
		
		//Computation of deltas_input
		double sum_num=0;
		double sum_den=0;
		
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			sum_num=0;
			sum_den=0;
			for(int idx_input=0;idx_input<size;idx_input++)
			{
				double v=in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)];
				double x=Math.exp(alpha*v);
				sum_num+=v*x;
				sum_den+=x;
			}
			
			double v=0.0;
			if(sum_den!=0.0){
				v=sum_num/sum_den;
			}
			else{
				throw new RuntimeException("CPUSoftMax : pb null denominator !");
			}
			
			for(int idx_input=0;idx_input<size;idx_input++)
			{
				int idx=Matrix.IDX2C(nbexample,idx_input,minibatch_size);
				double o=1.0f;
				if(d_out!=null){o=d_out[nbexample];}
				double val=in[idx];
				double s=(Math.exp(alpha*val)/sum_den);
				if(mode_deltas==0){
					s*=(1+(alpha*(val-v)));
				}
				d_in[idx]=o*s;
			}
			
		}	
	}

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}

	
	public String toString()
	{
		String r="CPUSoftMax_"+this.name+": "+size+" alpha="+alpha+" mode_deltas="+mode_deltas;
		return(r);
	}	


	
	// TODO
	public Module copyModuleToGPU()
	{
		
		return null;
	}
	

	

	

}
