package mlp;
import java.util.ArrayList;
public class CPUAverageCols extends Module {

	protected int size;
	protected Tensor tensor_delta;
	protected Tensor tensor_output;
	protected ArrayList<Float> weights;
	protected double div=1.0f;
	protected double sumW;
	protected int modeDiv=1; // 0=> divides the sum by the number of cols; 1 => divides the sum by the sum of weights (it is the same if no weights have been given); 2 => no dvision (sums the cols)
	//protected ArrayList<CPUAverageCols> listeners;
	
	public CPUAverageCols(){
		this(1);
	}
	public CPUAverageCols(int size,int modeDiv){
		this(size);
		this.modeDiv=modeDiv;
		//listeners=new ArrayList<CPUAverageCols>();
	}
	public CPUAverageCols(int size,ArrayList<Float> weights,int modeDiv){
		this(size);
		this.modeDiv=modeDiv;
		this.weights=weights;
		//listeners=new ArrayList<CPUAverageCols>();
		//if(modeDiv==1){
			sumW=0.0f;
			for(Float i:weights){sumW+=i;}
		//}
		
	}
	
	public CPUAverageCols(int size,ArrayList<Float> weights){
		this(size,weights,1);
	}
	
	public CPUAverageCols(int size){
		this.size=size;
		sumW=0;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//listeners=new ArrayList<CPUAverageCols>();
		//tensor_output.setMatrix(0, new CPUMatrix(1,1));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
	}
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		CPUAverageCols ret;
		if(weights!=null){
			ret=new CPUAverageCols(size,weights,modeDiv);
		}
		else{
			ret=new CPUAverageCols(size,modeDiv);
		}
		ret.name=name;
		ret.sharedForward=true;
		ret.origin_module=this;
		addListener(ret);
		return(ret);
	}
	
	public void setWeights(ArrayList<Float> weights){
		if(sharedForward){
			throw new RuntimeException("Please not call setWeights on a shared forward module");
		}
		setWeights(weights,modeDiv);
		for(Module mod:getListeners()){
			((CPUAverageCols) mod).setWeights(weights);
		}
	}
	
	public void setWeights(ArrayList<Float> weights,int modeDiv){
		if(sharedForward){
			throw new RuntimeException("Please not call setWeights on a shared forward module");
		}
		this.weights=weights;
		this.modeDiv=modeDiv;
		sumW=0.0f;
		for(Float i:weights){sumW+=i;}
		for(Module mod:getListeners()){
			((CPUAverageCols) mod).weights=weights;
			((CPUAverageCols) mod).modeDiv=modeDiv;
		}
	}
	
	
	
	public Module copy(){
		if(sharedForward){
			throw new RuntimeException("Please do not copy a shared forward module");
		}
		if(weights!=null){
			return new CPUAverageCols(size,weights);
		}
		return new CPUAverageCols(size);
	}
	
	public Module parametersSharedModule()
	{
		if(sharedForward){
			throw new RuntimeException("Please do not copy a shared forward module");
		}
		if(weights!=null){
			return new CPUAverageCols(size,weights);
		}
		return new CPUAverageCols(size);
	}
	
	void allocate(int minibatch_size)
	{
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()!=minibatch_size){
				tensor_delta.getMatrix(0).transformTo(minibatch_size, size);
				tensor_output.getMatrix(0).transformTo(minibatch_size, 1);
			}
		}
		else{
			tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,size));
			tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,1));
		}
		
	}
	
	
	
	@Override
	public void forward(Tensor input) {
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		int nbCols=input.getMatrix(0).getNumberOfColumns();
		if(weights!=null){
			if(weights.size()!=nbCols){
				throw new RuntimeException("CPUAverageCols format problem : "+weights.size()+"weights for "+nbCols+" input columns");
			}
		}
		allocate(minibatch_size);
		if(sharedForward){
			Tensor ten=this.origin_module.getOutput();
			Matrix mat=ten.getMatrix(0);
			tensor_output.setMatrix(0, mat);
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
			out[Matrix.IDX2C(nbexample,0,minibatch_size)]=0.0f;
		}
		double div=size;
		if((weights!=null) && (modeDiv==1)){
			div=sumW;
		}
		if(modeDiv==2){
			div=1.0f;
		}
		for(int idx_output=0;idx_output<size;idx_output++)
		{
			for(int nbexample=0;nbexample<minibatch_size;nbexample++)
			{
				double w=1.0f;
				if(weights!=null){
					w=weights.get(idx_output);
				}
			
				out[Matrix.IDX2C(nbexample,0,minibatch_size)]+=in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]*w/div;
			}
		}

		//System.out.println(getOutput());
	}

	@Override
	public Tensor getOutput() {
		return(tensor_output);
	}

	@Override
	public void backward_updateGradient(Tensor input, Tensor deltas_output) {
	}

	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltas_output) {
		/*if(locked==true){
			return;
		}*/
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		tensor_delta.ensureCPUMatrices();

		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		int nbCols=input.getMatrix(0).getNumberOfColumns();
		
		if(weights!=null){
			if(weights.size()!=nbCols){
				throw new RuntimeException("CPUAverageCols format problem : "+weights.size()+"weights for "+nbCols+" input columns");
			}
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
		double div=size;
		if((weights!=null) && (modeDiv==1)){
			div=sumW;
		}
		if(modeDiv==2){
			div=1.0f;
		}
		//Computation of deltas_input
		for(int idx_input=0;idx_input<size;idx_input++)
		{
			double w=1.0f;
			if(weights!=null){
				w=weights.get(idx_input);
			}
			for(int nbexample=0;nbexample<minibatch_size;nbexample++)
			{
				int idx=Matrix.IDX2C(nbexample,idx_input,minibatch_size);
				double o=1.0f;
				if(d_out!=null){o=d_out[nbexample];}
				
				d_in[idx]=o*w/div;
			}
		}		
	}

	@Override
	public Tensor getDelta() {return(tensor_delta);}
	
	public String toString()
	{
		return "CPUAverageCols "+size;
	}
	
	//TODO
	public Module copyModuleToGPU()
	{
			return null;
	}

}
