package mlp;

public class CPUHingeLoss extends Criterion {
	//
	protected Tensor tensor_delta;
	protected int size;
	protected double margin=1.0;
	
	public CPUHingeLoss()
	{
		this(1);
	}
	
	public CPUHingeLoss(int _size)
	{
		size=_size;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//tensor_output.setMatrix(0, new CPUMatrix(1,1));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
	}
	
	public  CPUHingeLoss(int _size,double margin){
		this(_size);
		this.margin=margin;
	}
	
	public CPUHingeLoss(CPUHingeLoss org)
	{
		super(org);
		size=org.size;
		margin=org.margin;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//tensor_output.setMatrix(0, new CPUMatrix(1,1));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
	}
	
	public Module forwardSharedModule()
	{
		CPUHingeLoss ret=new CPUHingeLoss(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	public Criterion copy(){
		CPUHingeLoss ret=new CPUHingeLoss(size);
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
	public void backward(Tensor input, Tensor output) {
		
		
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		allocate(minibatch_size);

		input.ensureCPUMatrices();
		
		tensor_delta.ensureCPUMatrices();
		
		
		CPUMatrix _delta=(CPUMatrix)(tensor_delta.getMatrix(0));
		double[] d=_delta.getValues();

		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		double[] in=_input.getValues();
		
		boolean labok=false;
		double[] labs=null;
		if(labels!=null){
			labok=true;
			labels.ensureCPUMatrices();
			CPUMatrix _labs=(CPUMatrix)(labels.getMatrix(0));
			labs=_labs.getValues();
		}
		
		CPUMatrix _output=null;
		double[] out=null;
		if (output!=null){
			output.ensureCPUMatrices();
			_output=(CPUMatrix)(output.getMatrix(0));
			out=_output.getValues();
		}
		
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{			
			for(int s=0;s<size;s++)
			{
				double l=1.0f;
				if(labok){
					l=labs[Matrix.IDX2C(nbexample,s,minibatch_size)];		
				}
				if ((margin-in[Matrix.IDX2C(nbexample,s,minibatch_size)]*l)>0)
				{
					double o=1.0f;
					if(out!=null){o=out[Matrix.IDX2C(nbexample,0,minibatch_size)];}
					double val=-l*o;
					if((Double.isNaN(val)) || (Double.isInfinite(val))){
						throw new RuntimeException(this+".computeDeltas: NaN val computed => "+o+" * "+l);		
					}
					d[Matrix.IDX2C(nbexample,s,minibatch_size)]=val;	
				}
				else 
					d[Matrix.IDX2C(nbexample,s,minibatch_size)]=0;	
				//System.out.println("Hinge "+d[Matrix.IDX2C(nbexample,s,minibatch_size)]);
			}
		}
		//System.out.println("Hinge delta : "+this.tensor_delta.getMatrix(0));
	}
	
	
	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}

	@Override
	public void forward(Tensor input){
		
		Tensor output=labels;
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		allocate(minibatch_size);

		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			return;
		}
		
		input.ensureCPUMatrices();
		
		tensor_output.ensureCPUMatrices();

		CPUMatrix _iv=(CPUMatrix)(tensor_output.getMatrix(0));

		double[] v=_iv.getValues();
		
		CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
		double[] in=_input.getValues();
		
		double[] out=null;
		if(labels!=null){
			output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(output.getMatrix(0));
			out=_output.getValues();
		}
		
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			v[Matrix.IDX2C(nbexample,0,minibatch_size)]=0.0f;
			for(int s=0;s<size;s++)
			{
				double p1=in[Matrix.IDX2C(nbexample,s,minibatch_size)];
				double p2=1.0f;
				if(labels!=null){
				   p2=out[Matrix.IDX2C(nbexample,s,minibatch_size)];
				}
				if ((margin-p1*p2)>0) v[Matrix.IDX2C(nbexample,0,minibatch_size)]+=margin-p1*p2;
			}
		}
		//System.out.println("hinge : \n"+tensor_output.getMatrix(0));
	}

	@Override
	public Tensor getValue() {
		return(tensor_output);
	}

	
	
	//TODO
	public Criterion copyCriterionToGPU()
	{
			//return(new GPUHingeLoss(size));
			return null;
	}
}
