package mlp;

import java.util.ArrayList;

//import utils.Keyboard;

public class CPUTermByTerm extends Module {
	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int size; // number of cols of output matrix 
	//protected int nbVecs; // nb of vectors of the second matrix, if <=0 => same number as first one
	
	
	public CPUTermByTerm(int size)
	{
		this.size=size;
		//this.nbVecs=nbVecs;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(2);
		//tensor_output.setMatrix(0, new CPUMatrix(1,size));
		//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
		//tensor_delta.setMatrix(1, new CPUMatrix(1,size));
	}
	
	public CPUTermByTerm(CPUTermByTerm org){
		this(org.size);
		this.origin_module=org;
	}
	
	/*public String toString(){
		return "CPUTermByTerm";
	}*/
	
	public Module forwardSharedModule()
	{
		CPUTermByTerm ret=new CPUTermByTerm(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	public Module parametersSharedModule()
	{
		return(new CPUTermByTerm(size));
	}


	void allocate(int nbRowsInput1, int nbRowsInput2)
	{
		
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			if ((tensor_delta.getMatrix(0).getNumberOfRows()==nbRowsInput1)  && (tensor_delta.getMatrix(1).getNumberOfRows()==nbRowsInput2)) return;
			tensor_delta.getMatrix(0).transformTo(nbRowsInput1, size);
			tensor_delta.getMatrix(1).transformTo(nbRowsInput2, size);
			tensor_output.getMatrix(0).transformTo(nbRowsInput1, size);
		}
		else{
		
			tensor_delta.setMatrix(0,new CPUMatrix(nbRowsInput1,size));
	    
			tensor_delta.setMatrix(1,new CPUMatrix(nbRowsInput2,size));
	   
			tensor_output.setMatrix(0,new CPUMatrix(nbRowsInput1,size));
		}
	}

	@Override
	public int getNbInputMatrix(){
		return 2;
	}
	
	@Override
	/** 
	 * Term by term multiplication of vectors of second matrix with the vecs of the first one.
	 * If nb row of matrix 1 > nb rows of matrix 2 => each vec of matrix 2 is used for a block of vecs of matrix 1 (nb rows mat1 % nb rows mat 2 must be equal to 0)
	 * @throws RuntimeException if nb Rows of matrix 2 > nb Rows of matrix 1
	 *  
	 */
	public void forward(Tensor input) {
		//System.out.println(this+" origin "+ this.origin_module+" sharedFwd "+this.sharedForward+" "+input);
		
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		int k=input.getMatrix(0).getNumberOfColumns();
		if(k!=size){
			throw new RuntimeException(this+" Format pb on input matrix 1 of CPUTermByTerm: given "+k+" columns, required "+size);
		}
		int r=input.getMatrix(1).getNumberOfRows();
		if(r>minibatch_size){
			throw new RuntimeException(this+" Format pb on input matrix 2 of CPUTermByTerm: too much rows in second input matrix");
		}
		
		k=input.getMatrix(1).getNumberOfColumns();
		if(k!=size){
			throw new RuntimeException(this+" Format pb on input matrix 2 of CPUTermByTerm: given "+k+" columns ,required "+size);
		}
		if(minibatch_size%r!=0){
			throw new RuntimeException(this+" Format pb on CPUTermByTerm: given "+minibatch_size+" and "+r+" rows => incompatible numbers of rows");
		}
		
		int n=minibatch_size/r;
		
		allocate(minibatch_size,r);
		
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
				double i2=in2[Matrix.IDX2C(v,i,r)];
				out[Matrix.IDX2C(nbexample,i,minibatch_size)]=i1*i2;
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
		
		tensor_delta.ensureCPUMatrices();

		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		int r=input.getMatrix(1).getNumberOfRows();
		int k=input.getMatrix(0).getNumberOfColumns();
		if(k!=size){
			throw new RuntimeException("Format pb on input matrix 1 of CPUTermByTerm");
		}
		if(r>minibatch_size){
			throw new RuntimeException("Format pb on input matrix 2 of CPUTermByTerm: too much rows in second input matrix");
		}
		k=input.getMatrix(1).getNumberOfColumns();
		if(k!=size){
			throw new RuntimeException("Format pb on input matrix 2 of CPUTermByTerm");
		}
		if(minibatch_size%r!=0){
			throw new RuntimeException("Format pb on input matrix 1 of CPUTermByTerm");
		}
		
		int n=minibatch_size/r;
		
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
		//System.out.println("t2 size="+_tensor_delta2.number_of_rows+"*"+_tensor_delta2.number_of_columns);
		for(int j=0;j<r;j++)
		{
			for(int i=0;i<size;i++){
				d_in2[Matrix.IDX2C(j,i,r)]=0.0f;
			}
		}
		for(int nbexample=0;nbexample<minibatch_size;nbexample++)
		{
			for(int i=0;i<size;i++){
				double o=1.0f;
				if(d_out!=null){o=d_out[Matrix.IDX2C(nbexample,i,minibatch_size)];}
				int v=nbexample/n;
				d_in1[Matrix.IDX2C(nbexample,i,minibatch_size)]=o*in2[Matrix.IDX2C(v,i,r)];
				d_in2[Matrix.IDX2C(v,i,r)]+=o*in1[Matrix.IDX2C(nbexample,i,minibatch_size)];
				//System.out.println("CPUTbyT "+i+" din1+="+o*in2[Matrix.IDX2C(v,i,nbVecs)]+" din2+="+o*in1[Matrix.IDX2C(nbexample,i,minibatch_size)]);
			}
			
		}
		
	}

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}

	
	
	public static void main(String[] args){
		CPUMatrix input=new CPUMatrix(10,2);
 		input.setValue(0, 0, 1.0f); input.setValue(0, 1, 1.0f);
 		input.setValue(1, 0, 0.5f); input.setValue(1, 1, 1.0f);
 		input.setValue(8, 0, -1.0f); input.setValue(8, 1, 0.2f);
 		input.setValue(3, 0, 3.0f); input.setValue(3, 1, 1.0f);
 		input.setValue(5, 0, 3.0f); input.setValue(5, 1, -21.0f);
 		input.setValue(4, 0, 0.0f); input.setValue(4, 1, 1.0f);
 		input.setValue(6, 0, -1.0f); input.setValue(6, 1, -1.0f);
 		input.setValue(7, 0, -1.0f); input.setValue(7, 1, 2.0f);
 		input.setValue(2, 0, 1.0f); input.setValue(2, 1, 0.0f);
 		input.setValue(9, 0, 0.9f); input.setValue(9, 1, -3.0f);
 		Tensor tin=new Tensor(1);
 		tin.setMatrix(0, input);
 		
 		CPUMatrix labels=new CPUMatrix(10,1);
 		labels.setValue(0, 0, 1);
 		labels.setValue(1, 0, 1);
 		labels.setValue(8, 0, -1);
 		labels.setValue(3, 0, 1);
 		labels.setValue(5, 0, -1);
 		labels.setValue(4, 0, 1);
 		labels.setValue(6, 0, -1);
 		labels.setValue(7, 0, -1);
 		labels.setValue(2, 0, 1);
 		labels.setValue(9, 0, -1);
 		Tensor tlabs=new Tensor(1);
 		tlabs.setMatrix(0, labels);
 		
 		Parameters params=new Parameters();
 		TensorModule mod=new CPULinear(2,1);
 		params.allocateNewParamsFor(mod,1.0f);
 		CPUTermByTerm tbt=new CPUTermByTerm(1);
 		CPUParams par=new CPUParams(1,1);
 		params.allocateNewParamsFor(par,1.0f);
 		//par.lockParams();
 		TableModule table=new TableModule();
 		table.addModule(mod);
 		table.addModule(par);
 		SequentialModule seq=new SequentialModule();
 		seq.addModule(table);
 		seq.addModule(tbt);
 		CPUHingeLoss hinge=new CPUHingeLoss();
 		hinge.setLabels(tlabs);
 		 
 		MLPClassical ml=new MLPClassical(seq,hinge,params,tin,tlabs,-1);
 		System.out.println(params);
 		LineSearch lsearch=new ConstantLine(0.01);
 		DescentDirection dir=new GradientDirection();
 		Optimizer opt=new Descent(dir,lsearch);
 		Env.setVerbose(3);
 		opt.optimize(ml);
	}
}

/*Passe 1
Value : 0.21933600306510925
pas applique : 0.01
gain : -6.639957427978516E-4
Params : Param Values : 
0=1.002,1=0.9978,2=1.0,
Gradients : 
0=-0.2,1=0.120000005,2=0.0,
Direction : 
0=0.2,1=-0.120000005,2=-0.0,*/
