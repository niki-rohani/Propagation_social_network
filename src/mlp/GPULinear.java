package mlp;

import jcuda.*;
import jcuda.jcublas.*;
import jcuda.runtime.JCuda;
import jcuda.runtime.cudaMemcpyKind;
import jcuda.utils.KernelLauncher;
import static jcuda.jcublas.cublasOperation.CUBLAS_OP_N;
import static jcuda.jcublas.cublasOperation.CUBLAS_OP_T;

public class GPULinear extends TensorModule {
	
	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	
	
	public GPULinear(int _input_size,int _output_size)
	{
		super(_input_size,_output_size);
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		parameters=new GPUMatrix(input_size,output_size);
		//tensor_output.setMatrix(0, new GPUMatrix(1,output_size));
		//tensor_delta.setMatrix(0, new GPUMatrix(1,input_size));
	}

	public GPULinear(TensorModule o)
	{
		super(o);
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
		//tensor_output.setMatrix(0, new GPUMatrix(1,output_size));
		//tensor_delta.setMatrix(0, new GPUMatrix(1,input_size));
	}

	public Module parametersSharedModule()
	{
		return(new GPULinear(this));
	}
	
	public Module forwardSharedModule()
	{
		GPULinear ret=new GPULinear(this);
		ret.sharedForward=true;
		return(ret);
	}
	
	public void allocate(int minibatch_size)
	{
		
		
		/*if(grad_params==null){
			grad_params=new GPUMatrix(minibatch_size,output_size);
		}*/
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) return;
			tensor_delta.getMatrix(0).transformTo(minibatch_size, input_size);
			tensor_output.getMatrix(0).transformTo(minibatch_size, output_size);
			if(!this.sharedForward){
				tensor_output.getMatrix(0).clear();
			}
		}
		else{
			tensor_delta.setMatrix(0,new GPUMatrix(minibatch_size,input_size));
			tensor_output.setMatrix(0,new GPUMatrix(minibatch_size,output_size));
		}
		
		
	}

	@Override
	public void majParams(){
		if(sharedForward){
			throw new RuntimeException("Please not call majParams on a shared forward module");
		}
		Pointer vals=((GPUMatrix)parameters).getValues();
		int nb=input_size*output_size;
		double[] v=new double[nb];
		for(int i=0;i<nb;i++){
			v[i]=paramList.get(i).getVal();
		}
		JCublas.cublasSetVector(nb,Sizeof.DOUBLE,Pointer.to(v),1,vals,1);
	    paramsChanged=false;
	}
	
	
	/*@Override
	public void transferGradient(){
		// nothing to do
	}*/
	
	@Override
	/**
	 * Computes output values from inputs and stores the results in the matrix at index 0 of the instannce variable tensor_output 
	 * @param input input values are contained in matrix 0 of this tensor 
	 * 
	 * The output tensor must not be deleted by the user
	 */
	public void forward(Tensor input) {
		
		assert(input.getNumberOfMatrices()==1);
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		allocate(minibatch_size);
		
		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			parameters=((TensorModule)this.origin_module).parameters;
			return;
		}
		if(paramsChanged){
			majParams();
		}

		input.ensureGPUMatrices();
		tensor_output.ensureGPUMatrices();
		tensor_delta.ensureGPUMatrices();

		GPUMatrix _parameters=(GPUMatrix)(parameters);
		GPUMatrix _input=(GPUMatrix)(input.getMatrix(0));
		GPUMatrix _output=(GPUMatrix)(tensor_output.getMatrix(0));

		Pointer params=_parameters.getValues();
		Pointer in=_input.getValues();
		Pointer out=_output.getValues();
		cublasHandle handle=Env.getEnv().getCublasHandle();
		double alpha=1.0f;
		double beta=0.0f;
		JCublas2.cublasSetPointerMode(handle,cublasPointerMode.CUBLAS_POINTER_MODE_HOST);
		Pointer pAlpha = Pointer.to(new double[]{alpha});
        Pointer pBeta = Pointer.to(new double[]{beta});
        int status=JCublas2.cublasSgemm(
				handle,
				CUBLAS_OP_N,CUBLAS_OP_N,
				minibatch_size,output_size,input_size,
				pAlpha,in,minibatch_size,params,input_size,
				pBeta,out,minibatch_size);
		if (status!=0)
		{
			throw new RuntimeException("Error with CUBLAS in GPULinear : "  + status );
		}
		
	}

	@Override
	public Tensor getOutput() {
		return(tensor_output);
	}

	@Override
	public void backward_updateGradient(Tensor input, Tensor deltas_output) {
		if(locked==true){
			return;
		}
		if(sharedForward){
			if(this.origin_module.locked){
				return;
			}
		}
		input.ensureGPUMatrices();
		tensor_output.ensureGPUMatrices();
		tensor_delta.ensureGPUMatrices();
		
		GPUMatrix _input=(GPUMatrix)(input.getMatrix(0));
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		GPUMatrix grad_params=new GPUMatrix(input_size,output_size);
		
		cublasHandle handle=Env.getEnv().getCublasHandle();
		JCublas2.cublasSetPointerMode(handle,cublasPointerMode.CUBLAS_POINTER_MODE_HOST);
		
		double alpha=1.0f;
		double beta=0.0f;
		Pointer pAlpha = Pointer.to(new double[]{alpha});
        Pointer pBeta = Pointer.to(new double[]{beta});
		Pointer in=_input.getValues();
		Pointer gpars=grad_params.getValues();
		Pointer d_out=null;
		if (deltas_output!=null){
			deltas_output.ensureGPUMatrices();
			GPUMatrix _output=(GPUMatrix)(deltas_output.getMatrix(0));
			d_out=_output.getValues();
		}
		else{
			GPUMatrix m1=new GPUMatrix(minibatch_size,output_size);
			m1.fill(1.0f);
			d_out=m1.getValues();
			//System.out.println(m1);
		}
		//System.out.println(input);
		
		JCublas2.cublasSgemm(
					handle,
					CUBLAS_OP_T,CUBLAS_OP_N,
					input_size,output_size,minibatch_size,
					pAlpha,in,minibatch_size,d_out,minibatch_size,
					pBeta,gpars,input_size);
			
		
		//JCuda.cudaMemcpy(Pointer.to(gp), gpars, input_size*output_size, cudaMemcpyKind.cudaMemcpyDeviceToHost);
		//System.out.println(grad_params);
		double[] gp=new double[input_size*output_size];
		JCublas2.cublasGetVector(input_size*output_size,Sizeof.DOUBLE,gpars,1,Pointer.to(gp),1);
		for(int i=0;i<input_size;i++){
			for(int j=0;j<output_size;j++){
				paramList.get(Matrix.IDX2C(i,j,input_size)).gradient+=gp[Matrix.IDX2C(i, j, input_size)];
				//System.out.print(gp[Matrix.IDX2C(i, j, input_size)]+";");
			}
			//System.out.println("");
		}
		
	}

	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltas_output) {
		
		
		input.ensureGPUMatrices();
		tensor_output.ensureGPUMatrices();
		tensor_delta.ensureGPUMatrices();

		int minibatch_size=input.getMatrix(0).getNumberOfRows();

		GPUMatrix _parameters=(GPUMatrix)(parameters);
		GPUMatrix _tensor_delta=(GPUMatrix)(tensor_delta.getMatrix(0));

		Pointer params=_parameters.getValues();

		Pointer d_in=_tensor_delta.getValues();
		Pointer d_out=null;
		if (deltas_output!=null){
			deltas_output.ensureGPUMatrices();
			GPUMatrix _output=(GPUMatrix)(deltas_output.getMatrix(0));
			d_out=_output.getValues();
		}
		else{
			GPUMatrix m1=new GPUMatrix(minibatch_size,output_size);
			m1.fill(1.0f);
			d_out=m1.getValues();
		}
		double alpha=1.0f;
		double beta=0.0f;
		Pointer pAlpha = Pointer.to(new double[]{alpha});
        Pointer pBeta = Pointer.to(new double[]{beta});
        cublasHandle handle=Env.getEnv().getCublasHandle();
		JCublas2.cublasSetPointerMode(handle,cublasPointerMode.CUBLAS_POINTER_MODE_HOST);
		
        JCublas2.cublasSgemm(
				handle,
				CUBLAS_OP_N,CUBLAS_OP_T,
				minibatch_size,input_size,output_size,
				pAlpha,d_out,minibatch_size,params,input_size,
				pBeta,d_in,minibatch_size);
        
        //System.out.println(tensor_delta);
	}

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}


	
	public String toString()
	{
		return("GPULinear: "+input_size+" ; "+output_size);
	}

	
	public Module copyModuleToCPU()
	{
		if (origin_module!=null)
		{
			Module module=new CPULinear((TensorModule)origin_module);
			return(module);
			//System.out.println("WARNING : Copying a shared version of a linear module to GPU : Shared connection lost...");
		}
		else{
			//GPUMatrix gpu_parameters=(GPUMatrix)(parameters.copyToGPU());
			Module module=new CPULinear(this);
			return(module);
		}
		
	}

	
	public static void main(String[] args){
		double[] x={2.0f};
		Pointer px=Pointer.to(x);
		double[] y={1.0f};
		Pointer py=Pointer.to(y);
		double[] v=new double[2];
		v[0]=4.0f;
		v[1]=5.0f;
		Pointer pv=Pointer.to(v);
		Pointer pl=Pointer.to(px,py);
		//JCuda.cudaMalloc(pv,(long)(2*Sizeof.DOUBLE));
		//int status=JCuda.cudaMalloc(pl,(long)(2*Sizeof.DOUBLE*Sizeof.DOUBLE));
		//System.out.println(status);
		/*KernelLauncher func=Env.getGPUFunction("ccVals");
		func.setBlockSize(2, 1, 1);
		func.setGridSize(1, 1, 1);
		func.call(pv,pl,2);
		//JCuda.cudaMemcpy(pl,values,number_of_rows*number_of_columns*Sizeof.DOUBLE,cudaMemcpyKind.cudaMemcpyDeviceToHost);
		System.out.println(x);
		System.out.println(y);*/
		//KernelLauncher func=Env.getGPUFunction("testKernel");
		//func.call(2);
		GPULinear gl=new GPULinear(2,1);
		Parameter p1=new Parameter(1.0f);
		Parameter p2=new Parameter(2.0f);
		Parameters ps=new Parameters(2);
		ps.set(0,p1);
		ps.set(1,p2);
		Parameters lpa=new Parameters(2);
		lpa.set(0,p1);
		lpa.set(1,p2);
		gl.setParameters(lpa);
		GPUMatrix m=new GPUMatrix(5,2);
		m.fill(1.0f);
		m.setValue(1, 0, 7.0f);
		m.setValue(3, 0, 6.0f);
		m.setValue(3, 1, 1.0f);
		Tensor t=new Tensor(1);
		GPUMatrix o=new GPUMatrix(5,1);
		o.fill(2.0f);
		Tensor t2=new Tensor(1);
		
		t.setMatrix(0, m);
		t2.setMatrix(0, o);
		
		gl.forward(t);
		System.out.println("input : "+t);
		System.out.println("out : " +gl.getOutput());
		gl.backward_updateGradient(t);
		gl.backward_computeDeltaInputs(t,t2);
		
	}
}
