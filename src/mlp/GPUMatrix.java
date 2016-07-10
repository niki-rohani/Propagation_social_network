package mlp;

import jcuda.*;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.JCudaDriver;
import jcuda.runtime.*;
import jcuda.driver.*;
import jcuda.jcublas.JCublas;
import jcuda.jcublas.JCublas2;
import jcuda.utils.KernelLauncher;

public class GPUMatrix extends Matrix {
	//protected double[] values;
	protected Pointer values;
	//protected Pointer d_values = new Pointer();
	protected boolean memory_allocated_outside;

	public GPUMatrix(int _number_of_rows,int _number_of_columns) 
	{
		super(_number_of_rows,_number_of_columns);
		//values=new double[number_of_rows*number_of_columns];
		values=new CUdeviceptr();
		int status=JCuda.cudaMalloc(values,(long)(number_of_rows*number_of_columns*Sizeof.DOUBLE));
		if (status==cudaError.cudaErrorMemoryAllocation)
		{
			throw new RuntimeException("Probleme d'allocation memoire GPU");
		}
		memory_allocated_outside=false;
		//GPU_init();
	}
	
	public GPUMatrix(int _number_of_rows,int _number_of_columns,CUdeviceptr already_allocated_cuda_memory)
	{
		super(_number_of_rows,_number_of_columns);
		values=already_allocated_cuda_memory;
		//d_values.
		//values=d_values.
		memory_allocated_outside=true;
		//GPU_init();
	}


	public GPUMatrix(int _number_of_rows,int _number_of_columns,double initial_value)
	{
		super(_number_of_rows,_number_of_columns);
		values=new CUdeviceptr();
		
		int status=JCuda.cudaMalloc(values,(long)(number_of_rows*number_of_columns*Sizeof.DOUBLE));
		if (status==cudaError.cudaErrorMemoryAllocation)
		{
			throw new RuntimeException("Probleme d'allocation memoire GPU");
		}
		CPUMatrix c=new CPUMatrix(number_of_rows,number_of_columns,initial_value);
		JCublas.cublasSetVector(number_of_rows*number_of_columns,Sizeof.DOUBLE,Pointer.to(c.getValues()),1,values,1);
		memory_allocated_outside=false;
		//GPU_init();
	}
	
	/**
	 * Transform the matrix to the given number of rows and columns.
	 * Warning : after this call, all values are null if numbers of cols or rows have changed. 
	 * @param nbRows
	 * @param nbCols
	 */
	public void transformTo(int nbRows,int nbCols){
		if((nbRows!=number_of_rows) || (nbCols!=number_of_columns)){
			if (memory_allocated_outside){
				throw new RuntimeException("Cannot transform a GPUMatrix that has been allocated from outside");
			}
			JCuda.cudaFree(values);
			this.number_of_columns=nbCols;
			this.number_of_rows=nbRows;
			values=new CUdeviceptr();
			int status=JCuda.cudaMalloc(values,(long)(number_of_rows*number_of_columns*Sizeof.DOUBLE));
			if (status==cudaError.cudaErrorMemoryAllocation)
			{
				throw new RuntimeException("Probleme d'allocation memoire GPU");
			}
		}
	}


	@Override
	protected void finalize () throws Throwable {
		if (!memory_allocated_outside) JCuda.cudaFree(values);
		super.finalize();
	}
	
	public void clear(){
		fill(0.0f);
	}
	
	
	
	@Override
	public void setValue(int r, int c, double v) {
		if(r>=this.number_of_rows){
			throw new RuntimeException("GPUMatrix.setValue => The matrix does not contain a row "+r+" (only "+this.number_of_rows+" rows)");
		}
		if(c>=this.number_of_columns){
			throw new RuntimeException("GPUMatrix.setValue => The matrix does not contain a column "+c+" (only "+this.number_of_columns+" columns)");
		}
		KernelLauncher func=Env.getGPUFunction("setVal");
		func.setBlockSize(1, 1, 1);
		func.setGridSize(1, 1, 1);
		func.call(values,v,Matrix.IDX2C(r,c,number_of_rows));
		//double[] x={v};
		//JCublas.cublasSetVector(number_of_rows*number_of_columns,Sizeof.FLOAT,Pointer.to(x),1,values,Matrix.IDX2C(r,c,number_of_rows));
		//cudaMemcpy(values[Matrix.IDX2C(r,c,number_of_rows)],v,Sizeof.FLOAT,cudaMemcpyKind.cudaMemcpyHostToDevice);

	}

	@Override
	public double getValue(int r, int c) {
		if(r>=this.number_of_rows){
			throw new RuntimeException("GPUMatrix.getValue => The matrix does not contain a row "+r+" (only "+this.number_of_rows+" rows)");
		}
		if(c>=this.number_of_columns){
			throw new RuntimeException("GPUMatrix.getValue => The matrix does not contain a column "+c+" (only "+this.number_of_columns+" columns)");
		}
		double[] v=new double[1];
		v[0]=0.0f;
		Pointer pv=Pointer.to(v);
		JCuda.cudaMalloc(pv,(long)(Sizeof.DOUBLE));
		KernelLauncher func=Env.getGPUFunction("getVal");
		func.setBlockSize(1, 1, 1);
		func.setGridSize(1, 1, 1);
		func.call(values,pv,Matrix.IDX2C(r,c,number_of_rows));
		JCublas2.cublasGetVector(1,Sizeof.DOUBLE,pv,1,Pointer.to(v),1);
		return v[0];
	}
	
	
	public Pointer getValues() {
		return values;
	}

	
	@Override
	public String toString() {
		CPUMatrix cc=copyToCPU();
		return cc.toString();
	}

	@Override
	public Matrix copy() {
		GPUMatrix m=new GPUMatrix(number_of_rows,number_of_columns);
		JCuda.cudaMemcpy(m.values,values,number_of_rows*number_of_columns*Sizeof.DOUBLE,cudaMemcpyKind.cudaMemcpyDeviceToDevice);
		return(m);
	}

	 
	
	@Override
	public CPUMatrix copyToCPU() {
		CPUMatrix c=new CPUMatrix(number_of_rows,number_of_columns);
		JCublas2.cublasGetVector(number_of_rows*number_of_columns,Sizeof.DOUBLE,values,1,Pointer.to(c.getValues()),1);
		return(c);
	}

	@Override
	public CPUSparseMatrix copyToCPUSparse() {
		return copyToCPU().copyToCPUSparse();
	}

	@Override
	public GPUMatrix copyToGPU() {
		 return((GPUMatrix)copy());
	}

	@Override
	public void fill(double value) {
		KernelLauncher func=Env.getGPUFunction("GPU_fill");
		int size=number_of_rows*number_of_columns;
		int nbt=Env.nbThreadsPerBlock;
		func.setBlockSize(nbt, 1, 1);
		func.setGridSize((size/nbt)+1, 1, 1);
		func.call(values,value,size);

	}

	/*@Override
	public Matrix buildMatrixFromVector(double[] v, int size) {
		// TODO Auto-generated method stub
		return null;
	}*/

	public static void main(String[] args){
		GPUMatrix m=new GPUMatrix(5,5);
		m.fill(2.0f);
		//GPUMatrix m2=new GPUMatrix(5,5);
		//m2.fill(4.0f);
		System.out.println(m);
		m.setValue(1, 2, 7.0f);
		m.setValue(3, 2, 6.0f);
		m.setValue(3, 4, 1.0f);
		System.out.println(m);
		System.out.println(m.getValue(3, 2));
		CPUMatrix m2=m.copyToCPU();
		m2.setValue(1, 1, 3.0F);
		m=m2.copyToGPU();
		System.out.println(m.getValue(1, 1));
		System.out.println(m);
		
		
	}
}
