package mlp;

public class CPUSquareLoss extends Criterion {
	protected Tensor tensor_delta;
	protected int size;

	public CPUSquareLoss()
	{
		this(1);
	}
	
	public CPUSquareLoss(int _size)
	{
		size=_size;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//tensor_output.setMatrix(0, new CPUMatrix(1,1));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
	}
	
	public CPUSquareLoss(CPUSquareLoss org)
	{
		super(org);
		size=org.size;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//tensor_output.setMatrix(0, new CPUMatrix(1,1));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
	}
	
	public Module forwardSharedModule()
	{
		CPUSquareLoss ret=new CPUSquareLoss(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	
	public Criterion copy(){
		CPUSquareLoss ret=new CPUSquareLoss(size);
		ret.setLabels(labels);
		return ret;
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
	public void backward(Tensor input, Tensor deltas_output) {
		
		//System.out.println(deltas_output);
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		allocate(minibatch_size);

		input.ensureCPUMatrices();
		tensor_delta.ensureCPUMatrices();
		labels.ensureCPUMatrices();
		
		CPUMatrix _delta=(CPUMatrix)(tensor_delta.getMatrix(0));
		CPUMatrix _labs=(CPUMatrix)(labels.getMatrix(0));
		
		double[] d=_delta.getValues();

		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		

		double[] in=_input.getValues();
		double[] labs=_labs.getValues();
		
		double[] d_out=null;
		if (deltas_output!=null){
			deltas_output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
			d_out=_output.getValues();
		}
		
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{			
			for(int s=0;s<size;s++)
			{
				double o=1.0f;
				if(d_out!=null){o=d_out[Matrix.IDX2C(nbexample,0,minibatch_size)];}
				d[Matrix.IDX2C(nbexample,s,minibatch_size)]=2*(in[Matrix.IDX2C(nbexample,s,minibatch_size)]-labs[Matrix.IDX2C(nbexample,s,minibatch_size)])*o;
			}
		}
	}

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}

	@Override
	public void forward(Tensor input) {
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		allocate(minibatch_size);
		
		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			return;
		}
		
		Tensor output=labels;
		input.ensureCPUMatrices();
		output.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();

		CPUMatrix _iv=(CPUMatrix)(tensor_output.getMatrix(0));

		double[] v=_iv.getValues();
		
		CPUMatrix _input=(CPUMatrix)input.getMatrix(0);
		CPUMatrix _output=(CPUMatrix)(output.getMatrix(0));

		double[] in=_input.getValues();
		double[] out=_output.getValues();		
			
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			v[Matrix.IDX2C(nbexample,0,minibatch_size)]=0.0f;
			for(int s=0;s<size;s++)
			{
				double p1=in[Matrix.IDX2C(nbexample,s,minibatch_size)];
				double p2=out[Matrix.IDX2C(nbexample,s,minibatch_size)];
			
				v[Matrix.IDX2C(nbexample,0,minibatch_size)]+=(p1-p2)*(p1-p2);
			}
		}
		//System.out.println(getOutput());
	}

	@Override
	public Tensor getValue() {
		return(tensor_output);
	}

	
	public Criterion copyCriterionToCPU()
	{
			return this.copy();
	}
	
	
	//TODO
	public Criterion copyCriterionToGPU()
	{
			//return(new GPUSquareLoss(size));
			return null;
	}
}
