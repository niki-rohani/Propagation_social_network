package simon.mlp;

import mlp.CPUMatrix;
import mlp.Matrix;
import mlp.Module;
import mlp.Tensor;


public class CPUSoftmaxOLD extends Module
{
	//protected CPUMatrix activations;
	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int size;
	
	public CPUSoftmaxOLD(int _size){
		size=_size;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//activations=new CPUMatrix();
	}
	
	public CPUSoftmaxOLD(CPUSoftmaxOLD org){
		this(org.size);
		this.origin_module=org;
	}

	public Module forwardSharedModule()
	{
		CPUSoftmaxOLD ret=new CPUSoftmaxOLD(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	
	public Module parametersSharedModule()
	{
		return(new CPUSoftmaxOLD(size));
	}

	public void allocate(int minibatch_size)
	{
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) return;
		}

		tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,size));
		tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,size));
		//activations=new CPUMatrix(minibatch_size,size);
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
			
			double sumexp = 0 ;
			for(int idx_output=0;idx_output<size;idx_output++)
			{
				//act[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=in[IDX2C(nbexample,idx_output,minibatch_size)];
				double v = (double) Math.exp(in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]);
				out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=v ;
				sumexp+=v ;
			}
			for(int idx_output=0;idx_output<size;idx_output++)
			{
				//act[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=in[IDX2C(nbexample,idx_output,minibatch_size)];
				double v = out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)] ;
				out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=v/sumexp ;
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
		
		if(locked==true){
			return;
		}
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
			//System.out.println(d_out.length);
		}

		//Computation of deltas_input
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			
			double vect[] = new double[size] ;
			double sum = 0f ;
			for(int idx_input=0;idx_input<size;idx_input++) {
				vect[idx_input] = in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)] ;
				sum += Math.exp(in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]) ;
			}
			for(int idx_input=0;idx_input<size;idx_input++)
			{
				System.out.println("example no "+idx_input);
				d_in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]=0.0f;

				for(int idx_output=0;idx_output<size;idx_output++)
				{
					double o=1.0f;
					if(d_out!=null){o=d_out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];}
					double val=getDSoftmax(vect,sum,idx_output,idx_input)*o;
					if(Double.isNaN(val)){
						throw new RuntimeException(this+".computeDeltas: NaN val computed");		
					}
					double din=d_in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]+val;
					if(Double.isNaN(din)){
						throw new RuntimeException(this+".computeDeltas: NaN delta computed");		
					}
					d_in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]=din;
				
				}
			}
		}		

	}
	
	// Dérivé en top en fonction de bot
	private double getDSoftmax(double[] vect,double sum, int top, int bot) {
		//double sum = 0.01f ;
		for(double i : vect) {
			sum+=Math.exp(i) ;
		}
		if(top==bot) {
			return (double) ((Math.exp(vect[top])/sum)*(1-Math.exp(vect[bot])/sum)) ;
		} else {
			return (double) ((Math.exp(vect[top])/sum)*(1-Math.exp(vect[bot])/sum)) ;
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
		String r="CPUSoftmax: "+size;
		return(r);
	}	

	
	// TODO
	public Module copyModuleToGPU()
	{
		//return(new GPUTanh(size,alpha,lambda));
		return null;
	}

}



