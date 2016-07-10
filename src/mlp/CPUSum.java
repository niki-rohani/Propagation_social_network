package mlp;

import java.util.ArrayList;

public class CPUSum extends Module {

	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int size; // number of cols of matrix to sum
	protected int nb_modules;
	protected CPUMatrix weights;
	//protected ArrayList<CPUSum> listeners;
	
	
	public CPUSum(int size,int nb_modules){
		this(size,nb_modules,(CPUMatrix)null);
	}
	public CPUSum(int size,int nb_modules,ArrayList<Double> weights){
		this(size,nb_modules,((weights!=null)?new CPUMatrix(weights):(CPUMatrix)null));
	}
	public CPUSum(int size,int nb_modules,CPUMatrix weights){
		/*if(nb_modules>1000){
			throw new RuntimeException(""+nb_modules);
		}*/
		this.size=size;
		this.nb_modules=nb_modules;
		if(weights==null){
			this.weights=new CPUMatrix(1,nb_modules); 
			for(int i=0;i<nb_modules;i++){
				this.weights.setValue(0,i,1.0f);
			}
		}
		else{
			this.weights=weights;
		}
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(nb_modules);
		//listeners=new ArrayList<CPUSum>();
		//for(int i=0;i<nb_modules;i++)
		//	tensor_delta.setMatrix(i, new CPUMatrix(1,size));
		//tensor_output.setMatrix(0, new CPUMatrix(1,size));
		
	}
	
	public CPUSum(CPUSum org){
		this(org.size,org.nb_modules,org.weights);
		this.origin_module=org;
	}
	
	public void setWeights(CPUMatrix mat){
		if(sharedForward){
			throw new RuntimeException("Please not call setWeights on a shared forward module");
		}
		weights=mat;
		this.nb_modules=mat.number_of_columns;
		for(Module mod:getListeners()){
			((CPUSum) mod).weights=weights;
			((CPUSum) mod).nb_modules=mat.number_of_columns;
		}
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(nb_modules);
	}
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		CPUSum ret=new CPUSum(this);
		ret.sharedForward=true;
		addListener(ret);
		return(ret);
	}
	
	public Module parametersSharedModule()
	{
		if(sharedForward){
			throw new RuntimeException("Please do not copy a shared forward module");
		}
		return(new CPUSum(size,nb_modules,weights));
	}

	
	

	void allocate(int minibatch_size)
	{
		
		
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) return;
			for(int i=0;i<nb_modules;i++)
				tensor_delta.getMatrix(i).transformTo(minibatch_size, size);
			tensor_output.getMatrix(0).transformTo(minibatch_size, size);
		}
		else{
			for(int i=0;i<nb_modules;i++)
				tensor_delta.setMatrix(i,new CPUMatrix(minibatch_size,size));
			tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,size));
		}
	}
	
	public String toString(){
		return "CPUSum_"+weights+"_nbModules="+nb_modules+" name="+name+((sharedForward)?"_sharedForward":"");
	}
	
	@Override
	public void forward(Tensor input) {
		//System.out.println(this); //+" input "+input);
		
		//System.out.println(input);
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		allocate(minibatch_size);
		
		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			return;
		}
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		
		if(input.getNumberOfMatrices()!=nb_modules){
			throw new RuntimeException(this+" Format problem of input => given "+input.getNumberOfMatrices()+" matrices, required "+nb_modules);
		}
		
		if(minibatch_size%weights.number_of_rows!=0){
			throw new RuntimeException(this+" Format pb : weights and input number of rows incompatible");
		}
		
		
		CPUMatrix _output=(CPUMatrix)tensor_output.getMatrix(0);
		

		double[] out=_output.getValues();
		CPUMatrix _input=(CPUMatrix)input.getMatrix(0);
		int nbc=_input.getNumberOfColumns();
		if(nbc!=size){
			throw new RuntimeException(this+" : Matrix "+0+" has "+nbc+" columns , required = "+size);
		}
		if(nb_modules!=weights.number_of_columns){
			throw new RuntimeException(this+" Format pb : number of columns weights  and nb of input matrices  incompatible");
		}
		double[] in=_input.getValues();
		int nbrw=weights.number_of_rows;
		int n=minibatch_size/nbrw;
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			double w=weights.getValue((int)(nbexample/n),0);
			for(int idx_output=0;idx_output<size;idx_output++)
			{
				out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=w*in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];
			}
		}
		
		for(int i=1;i<nb_modules;i++)
		{
			_input=(CPUMatrix)(input.getMatrix(i));
			int nbr=_input.getNumberOfRows();
			if(nbr!=minibatch_size){
				throw new RuntimeException(this+" : Matrix "+i+" has "+nbr+" rows , required = "+minibatch_size);
			}
			nbc=_input.getNumberOfColumns();
			if(nbc!=size){
				throw new RuntimeException(this+" : Matrix "+i+" has "+nbc+" columns , required = "+size);
			}
			in=_input.getValues();
			
			for(int nbexample=0;nbexample<minibatch_size;nbexample++)
			{
				double w=weights.getValue((int)(nbexample/n),i);
				
				for(int idx_output=0;idx_output<size;idx_output++)
				{
					out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]+=w*in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];
				}
			}
		}

		//System.out.println("Infer : \n"+tensor_output);
	}

	@Override
	public int getNbInputMatrix(){
		return nb_modules;
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

		if(input.getNumberOfMatrices()!=nb_modules){
			throw new RuntimeException(this+" Format problem of input => given "+input.getNumberOfMatrices()+" matrices, required "+nb_modules);
		}
		
		if(minibatch_size%weights.number_of_rows!=0){
			throw new RuntimeException(this+" Format pb : weights and input number of rows incompatible");
		}
		
		double[] d_out=null;
		if (deltas_output!=null){
			deltas_output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
			d_out=_output.getValues();
		}
		
		if(nb_modules!=weights.number_of_columns){
			throw new RuntimeException(this+" Format pb : number of columns weights  and nb of input matrices  incompatible");
		}
		int nbrw=weights.number_of_rows;
		int n=minibatch_size/nbrw;
		
		//Computation of deltas_input
		
		for(int i=0;i<nb_modules;i++)
		{
			
			CPUMatrix _input=(CPUMatrix)(input.getMatrix(i));
			int nbr=_input.getNumberOfRows();
			if(nbr!=minibatch_size){
				throw new RuntimeException(this+"_compute_deltas : Matrix "+i+" has "+nbr+" rows , required = "+minibatch_size);
			}
			int nbc=_input.getNumberOfColumns();
			if(nbc!=size){
				throw new RuntimeException(this+"_compute_deltas : Matrix "+i+" has "+nbc+" columns , required = "+size);
			}
			
			CPUMatrix _tensor_delta=(CPUMatrix)(tensor_delta.getMatrix(i));
			double[] d_in=_tensor_delta.getValues();
			
			for(int nbexample=0;nbexample<minibatch_size;nbexample++)
			{
				double w=weights.getValue((int)(nbexample/n),i);
				
				for(int idx_input=0;idx_input<size;idx_input++)
				{
					int idx=Matrix.IDX2C(nbexample,idx_input,minibatch_size);
					double o=1.0f;
					if(d_out!=null){o=d_out[idx];}
					d_in[idx]=w*o;
					//System.out.println("CPUSum "+d_in[idx]);
				}
			}
		}		

	}

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}

}
