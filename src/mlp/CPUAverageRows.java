package mlp;
import java.util.ArrayList;
public class CPUAverageRows extends Module {

	protected int size;
	protected Tensor tensor_delta;
	protected Tensor tensor_output;
	protected ArrayList<Double> weights;
	protected int div=0; // if div>0, averages by blocks of div blocks (output of div rows)
	protected double sumW;
	protected int modeDiv=1; // 0=> divides the sum by the number of rows; 1 => divides the sum by the sum of weights (it is the same than 0 if no weights have been given); 2 => no dvision (sums the rows)
	//protected ArrayList<CPUAverageRows> listeners;
	
	public CPUAverageRows(){
		this(1);
	}
	public CPUAverageRows(int size,int modeDiv){
		this(size);
		this.modeDiv=modeDiv;
		//listeners=new ArrayList<CPUAverageRows>();
	}
	public CPUAverageRows(int size,ArrayList<Double> weights,int modeDiv){
		this(size);
		this.modeDiv=modeDiv;
		this.weights=weights;
		//listeners=new ArrayList<CPUAverageRows>();
		//if(modeDiv==1){
			sumW=0.0f;
			for(Double i:weights){sumW+=i;}
		//}
	}
	
	public CPUAverageRows(int size,ArrayList<Double> weights){
		this(size,weights,1);
	}
	
	public CPUAverageRows(int size){
		this.size=size;
		sumW=0;
		modeDiv=0;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//listeners=new ArrayList<CPUAverageRows>();
		//tensor_output.setMatrix(0, new CPUMatrix(1,size));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
	}
	
	public void setDiv(int div){
		if(sharedForward){
			throw new RuntimeException("Please not call setDiv on a shared forward module");
		}
		this.div=div;
		for(Module mod:getListeners()){
			((CPUAverageRows) mod).div=div;
		}
	}
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		CPUAverageRows ret;
		if(weights!=null){
			ret=new CPUAverageRows(size,weights);
		}
		else{
			ret=new CPUAverageRows(size);
		}
		ret.div=div;
		ret.modeDiv=modeDiv;
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.name=name;
		addListener(ret);
		return(ret);
	}
	
	public void setWeights(ArrayList<Double> weights){
		if(sharedForward){
			throw new RuntimeException("Please not call setWeights on a shared forward module");
		}
		setWeights(weights,modeDiv);
		for(Module mod:getListeners()){
			((CPUAverageRows) mod).weights=weights;
		}
	}
	
	public void setWeights(ArrayList<Double> weights,int modeDiv){
		if(weights==null){
			setWeights(modeDiv);
			return;
		}
		if(sharedForward){
			throw new RuntimeException("Please not call setWeights on a shared forward module");
		}
		this.weights=weights;
		this.modeDiv=modeDiv;
		sumW=0.0f;
		for(Double i:weights){sumW+=i;}
		for(Module mod:getListeners()){
			((CPUAverageRows) mod).weights=weights;
			((CPUAverageRows) mod).modeDiv=modeDiv;
		}
	}
	
	public void setWeights(int modeDiv){
		if(sharedForward){
			throw new RuntimeException("Please not call setWeights on a shared forward module");
		}
		this.weights=null;
		this.modeDiv=modeDiv;
		sumW=0.0f;
		for(Module mod:getListeners()){
			((CPUAverageRows) mod).weights=weights;
			((CPUAverageRows) mod).modeDiv=modeDiv;
		}
	}
	
	
	
	public Module copy(){
		if(sharedForward){
			throw new RuntimeException("Please do not copy a shared forward module");
		}
		CPUAverageRows ret;
		if(weights!=null){
			ret=new CPUAverageRows(size,weights);
		}
		else{
			ret=new CPUAverageRows(size);
		}
		ret.div=div;
		ret.modeDiv=modeDiv;
		return ret;
	}
	
	public Module parametersSharedModule()
	{
		if(sharedForward){
			throw new RuntimeException("Please do not copy a shared forward module");
		}
		CPUAverageRows ret;
		if(weights!=null){
			ret=new CPUAverageRows(size,weights);
		}
		else{
			ret=new CPUAverageRows(size);
		}
		ret.div=div;
		ret.modeDiv=modeDiv;
		return ret;
	}
	
	void allocate(int minibatch_size)
	{
		int r=1;
		if(div>0){
			 r=minibatch_size/div;
		}
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			
			if ((tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) && (tensor_output.getMatrix(0).getNumberOfRows()==r)) return;
			tensor_delta.getMatrix(0).transformTo(minibatch_size, size);
			
			tensor_output.getMatrix(0).transformTo(r, size);
		}
		else{
			tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,size));
			tensor_output.setMatrix(0,new CPUMatrix(r,size));
		}
		
	}
	
	
	
	@Override
	public void forward(Tensor input) {
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		if(weights!=null){
			if(weights.size()!=minibatch_size){
				throw new RuntimeException("CPUAverageRows format problem : "+weights.size()+"weights for "+minibatch_size+" input rows");
			}
		}
		allocate(minibatch_size);
		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			return;
		}
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		int r=1;
		if(div>0){
			if((minibatch_size%div)!=0){
				throw new RuntimeException("Problem on CPUAverageRows: number of input rows ("+minibatch_size+") not divisible per "+div);
			}
			 r=minibatch_size/div;
		}
		
		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		CPUMatrix _output=(CPUMatrix)(tensor_output.getMatrix(0));
		
		double[] in=_input.getValues();
		double[] out=_output.getValues();
		//double[] act=activations.getValues();
		for(int idx_output=0;idx_output<size;idx_output++)
		{
			for(int i=0;i<r;i++)
				out[Matrix.IDX2C(i,idx_output,r)]=0.0f;
		}
		
		int x=div;
		if(x==0){
			x=minibatch_size;
		}
		
		double d=x;
		if((weights!=null) && (modeDiv==1)){
			d=sumW;
		}
		if(modeDiv==2){
			d=1.0f;
		}
		
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			
			int b=nbexample%x; // indice of nbexample in its bucket
			int z=(int)(nbexample/x); // bucket indice of nbexample
			double w=1.0f;
			if(weights!=null){
				w=weights.get(b);
			}
			for(int idx_output=0;idx_output<size;idx_output++)
			{
				out[Matrix.IDX2C(z,idx_output,r)]+=in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]*w/d;
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
		if(weights!=null){
			if(weights.size()!=minibatch_size){
				throw new RuntimeException("CPUAverageRows format problem : "+weights.size()+"weights for "+minibatch_size+" input rows");
			}
		}
		
		int r=1;
		if(div>0){
			if((minibatch_size%div)!=0){
				throw new RuntimeException("Problem on CPUAverageRows: number of input rows ("+minibatch_size+") not divisible per "+div);
			}
			 r=minibatch_size/div;
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
			if(_output.getNumberOfRows()!=1){
				throw new RuntimeException("Bad number of rows of deltas_output");
			}
			d_out=_output.getValues();
		}
		
		int x=div;
		if(x==0){
			x=minibatch_size;
		}
		
		double d=x;
		if((weights!=null) && (modeDiv==1)){
			d=sumW;
		}
		if(modeDiv==2){
			d=1.0f;
		}
		
		for(int j=0;j<minibatch_size;j++)
		{
			for(int i=0;i<size;i++){
				d_in[Matrix.IDX2C(j,i,minibatch_size)]=0.0f;
			}
		}
		
		//Computation of deltas_input
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			int b=nbexample%x; // indice of nbexample in its bucket
			int z=(int)(nbexample/x); // bucket indice of nbexample
			double w=1.0f;
			if(weights!=null){
				w=weights.get(b);
			}
			
			for(int idx_input=0;idx_input<size;idx_input++)
			{
				int idx=Matrix.IDX2C(nbexample,idx_input,minibatch_size);
				double o=1.0f;
				if(d_out!=null){o=d_out[Matrix.IDX2C(z,idx_input,r)];}
				
				d_in[idx]=o*w/d;
			}
		}	
		//System.out.println("Av delta : "+this.tensor_delta.getMatrix(0));
	}

	@Override
	public Tensor getDelta() {return(tensor_delta);}
	
	public String toString()
	{
		return "CPUAverageRows modeDiv="+modeDiv+" div="+div+" "+size;
	}
	
	//TODO
	public Module copyModuleToGPU()
	{
			return null;
	}

}
