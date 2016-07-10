package mlp;

import java.util.ArrayList;

public class CPUAddVecs extends Module {

	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int size; // number of cols of matrix to sum
	//protected int nbVecs; // nb of vectors of the second matrix
	//protected double weightVecs;
	
	public CPUAddVecs(int size){
		this.size=size;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(2);
		/*tensor_output.setMatrix(0, new CPUMatrix(1,size));
		tensor_delta.setMatrix(0, new CPUMatrix(1,size));
		tensor_delta.setMatrix(1, new CPUMatrix(1,size));*/
	}
	
	/*public CPUAddVecs(int nbVecs, int size){
		this(nbVecs,size,1.0f);
	}*/
	/*public CPUAddVecs(int nbVecs, int size, double wVec)
	{
		this.size=size;
		this.nbVecs=nbVecs;
		this.weightVecs=wVec;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(2);
	}*/
	
	
	public CPUAddVecs(CPUAddVecs org){
		//this(org.nbVecs,org.size,org.weightVecs);
		this(org.size);
		this.origin_module=org;
		this.name=name;
	}
	
	public Module forwardSharedModule()
	{
		CPUAddVecs ret=new CPUAddVecs(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	public Module parametersSharedModule()
	{
		return(new CPUAddVecs(size));
	}

	void allocate(int nbRowsInput1, int nbRowsInput2)
	{
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			if ((tensor_delta.getMatrix(0).getNumberOfRows()!=nbRowsInput1)  || (tensor_delta.getMatrix(1).getNumberOfRows()!=nbRowsInput2)) {
		
				tensor_delta.getMatrix(0).transformTo(nbRowsInput1, size);
				tensor_delta.getMatrix(1).transformTo(nbRowsInput2, size);
				tensor_output.getMatrix(0).transformTo(nbRowsInput1, size);
			}
		}
		else{	
			tensor_delta.setMatrix(0,new CPUMatrix(nbRowsInput1,size));
		    
		    tensor_delta.setMatrix(1,new CPUMatrix(nbRowsInput2,size));
		   
			tensor_output.setMatrix(0,new CPUMatrix(nbRowsInput1,size));
		}
		
		
		/*if (tensor_delta.getMatrix(0)!=null)
		{
			if ((tensor_delta.getMatrix(0).getNumberOfRows()==nbRowsInput1)  && (tensor_delta.getMatrix(1).getNumberOfRows()==nbRowsInput2)) return;
		}

		
	    tensor_delta.setMatrix(0,new CPUMatrix(nbRowsInput1,size));
	    
	    tensor_delta.setMatrix(1,new CPUMatrix(nbRowsInput2,size));
	   
		tensor_output.setMatrix(0,new CPUMatrix(nbRowsInput1,size));*/
	}
	

	
	
	/*void allocate(int minibatch_size)
	{
		if ((tensor_output.getMatrix(0)==null) || (tensor_output.getMatrix(0).getNumberOfRows()!=minibatch_size))
		{
			tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,size));
		}
		if ((tensor_delta.getMatrix(0)==null) || (tensor_delta.getMatrix(0).getNumberOfRows()!=minibatch_size))
		{
			tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,size));
			tensor_delta.setMatrix(1,new CPUMatrix(nbVecs,size));
		}
		
		
	}*/
	@Override
	public int getNbInputMatrix(){
		return 2;
	}
	
	public String toString(){
		return "CPUAddVecs"+" name="+name;
	}
	
	
	@Override
	// addition of vectors of second matrix with the vecs of the first one
	// if nb row of matrix 1 < nb rows of matrix 2 => each vec of matrix 2 is used for a block of vecs of matrix 1 (nb rows mat1 % nb rows mat 2 must be equal to 0)
	public void forward(Tensor input) {
		//System.out.println(this+" forward "+this.sharedForward);
		//System.out.println(this+" origin "+ this.origin_module+" sharedFwd "+this.sharedForward+" "+input);
		if(input.getNumberOfMatrices()!=2){
			throw new RuntimeException(this+" Format problem of input => given "+input.getNumberOfMatrices()+" matrices, required 2");
		}
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		int k=input.getMatrix(0).getNumberOfColumns();
		if(k!=size){
			throw new RuntimeException("Format pb on input matrix 1 of CPUAddVecs");
		}
		k=input.getMatrix(1).getNumberOfColumns();
		if(k!=size){
			throw new RuntimeException("Format pb on input matrix 2 of CPUAddVecs");
		}
		int minibatch_size2=input.getMatrix(1).getNumberOfRows();
		if(minibatch_size%minibatch_size2!=0){
			throw new RuntimeException("Format pb on input matrix 1 of CPUAddVecs");
		}
		
		int n=minibatch_size/minibatch_size2;
		
		allocate(minibatch_size,minibatch_size2);
		
		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			return;
		}
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		
		CPUMatrix _output=(CPUMatrix)tensor_output.getMatrix(0);
		

		double[] out=_output.getValues();
		CPUMatrix _input1=(CPUMatrix)input.getMatrix(0);
		CPUMatrix _input2=(CPUMatrix)input.getMatrix(1);
		double[] in1=_input1.getValues();
		double[] in2=_input2.getValues();
		
		
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int i=0;i<size;i++){
				double i1=in1[Matrix.IDX2C(nbexample,i,minibatch_size)];
				int v=nbexample/n;
				double i2=in2[Matrix.IDX2C(v,i,minibatch_size2)];
				out[Matrix.IDX2C(nbexample,i,minibatch_size)]=i1+i2; //this.weightVecs*i2;
			}
		}
		
		//System.out.println("Infer : \n"+tensor_output);
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
		/*if(locked==true){
			return;
		}*/
		tensor_delta.ensureCPUMatrices();

		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		int k=input.getMatrix(0).getNumberOfColumns();
		if(k!=size){
			throw new RuntimeException("Format pb on input matrix 1 of CPUAddVecs");
		}
		k=input.getMatrix(1).getNumberOfColumns();
		if(k!=size){
			throw new RuntimeException("Format pb on input matrix 2 of CPUAddVecs");
		}
		int minibatch_size2=input.getMatrix(1).getNumberOfRows();
		if(minibatch_size%minibatch_size2!=0){
			throw new RuntimeException("Format pb on input matrix 1 of CPUAddVecs");
		}
		
		int n=minibatch_size/minibatch_size2;
		
		double[] d_out=null;
		if (deltas_output!=null){
			deltas_output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
			d_out=_output.getValues();
		}
		input.ensureCPUMatrices();
		CPUMatrix _input1=(CPUMatrix)input.getMatrix(0);
		CPUMatrix _input2=(CPUMatrix)input.getMatrix(1);
		double[] in1=_input1.getValues();
		double[] in2=_input2.getValues();
		
		//Computation of deltas_input
		CPUMatrix _tensor_delta1=(CPUMatrix)(tensor_delta.getMatrix(0));
		double[] d_in1=_tensor_delta1.getValues();
		CPUMatrix _tensor_delta2=(CPUMatrix)(tensor_delta.getMatrix(1));
		double[] d_in2=_tensor_delta2.getValues();
		for(int j=0;j<minibatch_size2;j++)
		{
			for(int i=0;i<size;i++){
				d_in2[Matrix.IDX2C(j,i,minibatch_size2)]=0.0f;
			}
		}
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int i=0;i<size;i++){
				double o=1.0f;
				if(d_out!=null){o=d_out[Matrix.IDX2C(nbexample,i,minibatch_size)];}
				int v=nbexample/n;
				d_in1[Matrix.IDX2C(nbexample,i,minibatch_size)]=o;
				double val=o;
				
				d_in2[Matrix.IDX2C(v,i,minibatch_size2)]+=val;
			}
			
		}
		
	}
	
	

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}

}
