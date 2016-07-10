package simon.mlp;
import mlp.Module ;
import mlp.Parameter;
import mlp.Parameters;
import mlp.Tensor ;
import mlp.CPUMatrix; 
import mlp.Matrix; 

public class CPUL2Wtest extends Module {

	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int size;
	protected Parameters paramList ;
	private int input_size;
	private int output_size;
	private Matrix parameters;
	
	public CPUL2Wtest(int s){
		size=s;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		input_size=s ;
		output_size=1 ;
		parameters=new CPUMatrix(s,1) ;
		
		//tensor_output.setMatrix(0, new CPUMatrix(1,1));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
	}
	
	/*public CPUL2Norm(CPUL2Norm org)
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
	}*/

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
					val=o*in[idx]*2.0*parameters.getValue(idx_input,0);
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
	public void backward_updateGradient(Tensor input, Tensor deltas_output) {
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
			
				//for(int idx_output=0;idx_output<output_size;idx_output++)
				//{
					for(int idx_input=0;idx_input<input_size;idx_input++)
					{
						double o=1.0f;
						if(d_out!=null){o=d_out[Matrix.IDX2C(nbexample,0,minibatch_size)];}
						double val=o*in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)];
						if(Double.isNaN(val)){
							throw new RuntimeException(this+".updateGradient: NaN val computed => "+o+" * "+in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]);		
						}
						Parameter p=paramList.get(Matrix.IDX2C(idx_input,0,input_size));
						double g=p.getGradient();
						/*if((this.name.equals("right0")) || (this.name.equals("right1"))){
							System.out.println(this+" "+name+" => add grad : "+val+" "+in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)]);
						}*/
						g+=val;
						if(Double.isNaN(g)){
							throw new RuntimeException(this+".updateGradient: NaN gradient computed => "+p.getGradient()+" + "+val);		
						}
						
						p.setGradient(g);
						
						//g_params[Matrix.IDX2C(idx_input,idx_output,input_size)]+=d_out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]*in[Matrix.IDX2C(nbexample,idx_input,minibatch_size)];
					}
				///}
		}
	}

	@Override
	public void forward(Tensor input) {
		majParams() ;
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
				out[Matrix.IDX2C(nbexample,0,minibatch_size)]+=f*f*parameters.getValue(idx_output, 0);
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
	

	public void majParams(){
		if(sharedForward){
			throw new RuntimeException("Please not call majParams on a shared forward module");
		}
		double[] vals=((CPUMatrix)parameters).getValues();
		int nb=input_size*output_size;
		//ArrayList<Parameter> pars=paramList.getParams();
		for(int i=0;i<nb;i++){
			vals[i]=paramList.get(i).getVal();
			//vals[i]=paramList[i].getVal();
		}
		paramsChanged=false;
	}
	
	public Parameters getParamList(){
		return paramList;
	}
	
	
	public int getInputSize(){
		return input_size;
	}
	public int getOutputSize(){
		return output_size;
	}
	
	/*public void destroy()
	{
		paramList=null;
		parameters=null;
	}*/
	/*
		for(Parameter p:paramList){
			p.parent.removeListener(this);
		}
	}*/
	
	/*public void paramsChanged(){
		paramsChanged=true;
		//majParams();
		//init_gradient();
	}*/
	
	/*public Tensor getParameters()
	{
		
		return(new Tensor(parameters));
	}*/
	
	
	public int getNbParams(){
		if(sharedForward){
			this.origin_module.getNbParams();
		}
		return input_size*output_size;
	}
	
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException("Please not call setParameters on a shared forward module");
		}
		if(pList.size()!=(input_size*output_size)){
			throw new RuntimeException("pList is not of good dimensions");
		}
		paramList=pList;
		/*for(Parameter p:pList){
			p.parent.addListener(this);
		}*/
		paramsChanged();
	}
	
	public Tensor getParameters()
	{
		if(sharedForward){
			this.origin_module.getParameters();
		}
		return(new Tensor(parameters));
		//this.paramList.
	}
	
	@Override
	public void updateParameters(double line){
		if(sharedForward){
			throw new RuntimeException("Please not call updateParameters on a shared forward module");
		}
					paramList.update(line);
					paramsChanged();
	}
	public int getNbInputMatrix(){
		
		return 1;
	}

}
