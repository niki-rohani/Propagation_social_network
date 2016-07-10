package mlp;

public class CPUSparseLinear extends TensorModule {
	//protected CPUSparseMatrix grad_parameters;
	protected Tensor tensor_output;
	//protected Tensor tensor_delta;
	
	
	public CPUSparseLinear(int _input_size,int _output_size)
	{		
		
			super(_input_size,_output_size);
			/*input_size=_input_size;
			output_size=_output_size;
			parameters=new CPUMatrix(input_size,output_size);*/
			//grad_parameters=new CPUSparseMatrix(input_size,output_size);
			tensor_output=new Tensor(1);
			//tensor_output.setMatrix(0, new CPUMatrix(1,_output_size));
			
			//tensor_delta=new Tensor(1);
	}

	public CPUSparseLinear(CPUSparseLinear o)
	{
		super(o);
		//grad_parameters=new CPUSparseMatrix(input_size,output_size);
		tensor_output=new Tensor(1);
		//tensor_output.setMatrix(0, new CPUMatrix(1,output_size));
		
		//tensor_delta=new Tensor(1);
	}
	
	/*public Tensor getParameters()
	{
		Tensor ret=new Tensor(2);
		ret.setMatrix(0, parameters);
		ret.setMatrix(1, grad_parameters);
		ret.setMatrix(0, parameters);
		
		return(ret);
	}*/
	
	public Module parametersSharedModule()
	{
		return(new CPUSparseLinear(this));
	}

	public Module forwardSharedModule()
	{
		CPUSparseLinear ret=new CPUSparseLinear(this);
		ret.sharedForward=true;
		return(ret);
	}
	

	public void allocate(int minibatch_size)
	{
		if(sharedForward){
			return;
		}
		
		if (tensor_output.getMatrix(0)!=null)
		{
			if (tensor_output.getMatrix(0).getNumberOfRows()==minibatch_size){
				if(!this.sharedForward){
					tensor_output.getMatrix(0).clear();
				}
				return;
			}
			tensor_output.getMatrix(0).transformTo(minibatch_size, output_size);
			tensor_output.getMatrix(0).clear();
		}
		else{
			//tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,input_size));
			tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,output_size));
		}
	}

	@Override
	/**
	 * Computes output values from inputs and stores the results in the matrix at index 0 of the instannce variable tensor_output 
	 * @param input input values are contained in matrix 0 of this tensor 
	 * 
	 * The output tensor must not be deleted by the user
	 */
	public void forward(Tensor input) {
			assert(input.getNumberOfMatrices()==1);
			//System.out.println(this+" origin "+ this.origin_module+" sharedFwd "+this.sharedForward+" "+input);
			
			int minibatch_size=input.getMatrix(0).getNumberOfRows();
			allocate(minibatch_size);
			if(sharedForward){
				tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
				parameters=((CPUSparseLinear)this.origin_module).parameters;
				return;
			}
			
			
			if(paramsChanged){
				majParams();
			}
			//System.out.println("input : "+input);
			

			input.ensureCPUSparseMatrices();
			tensor_output.ensureCPUMatrices();
			//tensor_delta.ensureCPUMatrices();

			CPUMatrix _parameters=(CPUMatrix)parameters;
			CPUSparseMatrix _input=(CPUSparseMatrix)(input.getMatrix(0));
			CPUMatrix _output=(CPUMatrix)(tensor_output.getMatrix(0));
			//System.out.println("input mat : "+_input);
			double[] params=_parameters.getValues();
			double[] out=_output.getValues();
			

			/*for(int idx_output=0;idx_output<output_size;idx_output++)
			{
				for(int nbexample=0;nbexample<minibatch_size;nbexample++)
				{
					out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=0.0f;
				}
			}*/
			//System.out.println("pars : "+_parameters);

			_input.initIterator();
			int nbexample;
			int idx_input;
			double val;
			while(_input.hasNextCell())
			{
				Cell cell=_input.nextCell();
			    nbexample=cell.i;
			    idx_input=cell.j;
			    val=cell.v;
				for(int idx_output=0;idx_output<output_size;idx_output++)
				{
					//System.out.println(nbexample+","+idx_input+","+val+","+idx_output);
					//System.out.println("param : "+params[Matrix.IDX2C(idx_input,idx_output,input_size)]);
					//System.out.println("out avant :"+out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]);
					out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]+=params[Matrix.IDX2C(idx_input,idx_output,input_size)]*val;
					//System.out.println("out apres :"+out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]);
					
				}					
			}
			
			//System.out.println("out : "+tensor_output);
	}


	@Override
	public Tensor getOutput() {
		if(sharedForward){
			this.origin_module.getOutput();
		}
		return(tensor_output);
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

	@Override
	/**
	 * Compute gradient parameters to store in grad_parammeters matrix from input values and deltas_outputs  
	 * 
	 * @param input input values are contained in matrix 0 of this tensor
	 * @param deltas_output deltas on outputs are contained in matrix 0 of this tensor
	 */
	public void backward_updateGradient(Tensor input, Tensor deltas_output) {
		//System.out.println("la "+this+" "+this.sharedForward);
		if(locked==true){
			return;
		}
		if(sharedForward){
			if(this.origin_module.locked){
				return;
			}
		}
		//System.out.println("here");
		input.ensureCPUSparseMatrices();
		tensor_output.ensureCPUMatrices();
		//tensor_delta.ensureCPUMatrices();

		//CPUMatrix _parameters=(parameters);
		CPUSparseMatrix _input=(CPUSparseMatrix)(input.getMatrix(0));
		int minibatch_size=input.getMatrix(0).getNumberOfRows();

		double[] d_out=null;
		if (deltas_output!=null){
			deltas_output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
			d_out=_output.getValues();
		}
		
		//Updating gradient
		int nbexample;
		int idx_input;
		double val;
		_input.initIterator();
		while(_input.hasNextCell())
		{
			Cell cell=_input.nextCell();
			nbexample=cell.i;
			idx_input=cell.j;
			val=cell.v;
			for(int idx_output=0;idx_output<output_size;idx_output++)
			{		
				double o=1.0f;
				if(d_out!=null){o=d_out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];}
				paramList.get(Matrix.IDX2C(idx_input,idx_output,input_size)).gradient+=(o*val);
				//System.out.println(paramList.get(Matrix.IDX2C(idx_input,idx_output,input_size)).gradient);
				//grad_parameters.addValue(idx_input,idx_output,d_out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]*val);
				//System.out.println("CPUSparse param"+Matrix.IDX2C(idx_input,idx_output,input_size)+" add to gradient "+(o*val));
			}
		}	
	}

	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltas_output) {
		System.out.println("ComputingDeltaInputs for a CPUSparseLinear layer may spend a lot of time... (NOT IMPLEMENTED)");

	}

	@Override
	public Tensor getDelta() {
		return(null);
	}

	

	/*@Override
	public void updateParameters(double gradient_step) {
		CPUMatrix _parameters=(CPUMatrix)parameters;
		assert(_parameters!=null);
		
		grad_parameters.initIterator();
		int row;
		int col;
		double val;

		while(grad_parameters.hasNextCell())
		{
			Cell cell=grad_parameters.nextCell();
			row=cell.i;
			col=cell.j;
			val=cell.v;
			_parameters.setValue(row,col,_parameters.getValue(row,col)-gradient_step*val);
		}


	}*/

}
