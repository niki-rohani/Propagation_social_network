package simon.mlp;

import mlp.CPUMatrix;
import mlp.Criterion;
import mlp.Matrix;
import mlp.Module;
import mlp.Tensor;

public class CrossEntropyCriterion extends Criterion {

	protected Tensor tensor_delta;
	protected int size;
	private int weigthPos=1;

	public CrossEntropyCriterion()
	{
		this(1);
	}
	
	public CrossEntropyCriterion(int _size){
		this(_size,1) ;
	}
	
	public CrossEntropyCriterion(int _size,int weightPos)
	{
		size=_size;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		this.weigthPos=weightPos;
	}
	
	public CrossEntropyCriterion(CrossEntropyCriterion org)
	{
		super(org);
		size=org.size;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
	}
	
	public Module forwardSharedModule()
	{
		CrossEntropyCriterion ret=new CrossEntropyCriterion(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	
	public Criterion copy(){
		CrossEntropyCriterion ret=new CrossEntropyCriterion(size);
		ret.setLabels(labels);
		return ret;
	}
	
	
	
	void allocate(int minibatch_size)
	{
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) return;
		}
		tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,size));
		tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,1));
	}

	
	@Override
	public void backward(Tensor input, Tensor deltas_output) {
		if(locked==true){
			return;
		}
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
		
		//System.out.println("CPUSQUALOSS : "+deltas_output.getMatrix(0).getNumberOfRows()+","+deltas_output.getMatrix(0).getNumberOfColumns());

		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{			
			for(int s=0;s<size;s++)
			{
				double o=1.0f;
				if(d_out!=null){o=d_out[Matrix.IDX2C(nbexample,0,minibatch_size)];}
				double p2 = labs[Matrix.IDX2C(nbexample,s,minibatch_size)] ;
				double p1 = in[Matrix.IDX2C(nbexample,s,minibatch_size)] ;
				if(p2==1){
					d[Matrix.IDX2C(nbexample,0,minibatch_size)]-=(1.0f/(p1))*o*this.weigthPos ;
					//d[Matrix.IDX2C(nbexample,0,minibatch_size)]-=o*this.weigthPos ;
				} else if(p2==0) {
					d[Matrix.IDX2C(nbexample,0,minibatch_size)]-=(1.0f/(1.0f-p1))*o;
					//d[Matrix.IDX2C(nbexample,0,minibatch_size)]-=-o;
				}
				if(d[Matrix.IDX2C(nbexample,0,minibatch_size)]==Float.NEGATIVE_INFINITY || d[Matrix.IDX2C(nbexample,0,minibatch_size)]==Float.POSITIVE_INFINITY) {
					throw new RuntimeException("Precision..."+p1+" "+o);	
				}
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
				if(p2==1){
					v[Matrix.IDX2C(nbexample,0,minibatch_size)]-=(Math.log(p1))*this.weigthPos ;
					//v[Matrix.IDX2C(nbexample,0,minibatch_size)]-=(p1)*this.weigthPos ;
				} else if(p2==0) {
					v[Matrix.IDX2C(nbexample,0,minibatch_size)]-=(Math.log(1.0f-p1)) ;
					//v[Matrix.IDX2C(nbexample,0,minibatch_size)]-=/*1.0f*/-p1;
				}
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
