package mlp;

import java.util.HashSet; 

import jcuda.Pointer;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmodule;
import jcuda.driver.JCudaDriver;
import jcuda.jcublas.*;
import jcuda.runtime.JCuda;
import jcuda.runtime.cudaError;
import java.util.HashMap;
import jcuda.utils.KernelLauncher;
import jcuda.runtime.dim3;

public class Env
{
	
	protected cublasHandle handle;  
	//protected  CUdevice  device;                                                        
	//protected  CUcontext context; 
	//protected CUmodule module;
	protected boolean handleCreated;
	protected static Env env;
	protected boolean displayCPUGPUCopies;
	protected long cpu_memory_max=1; // Not considered yet
	protected long gpu_memory_max=1; // Not considered yet
	protected long available_cpu_memory; // Not considered yet	     
	protected long available_gpu_memory; // Not considered yet
	//protected HashMap<String,CUfunction> functions;
	protected HashMap<String,KernelLauncher> functions;
	public static int nbThreadsPerBlock=256;
	public static dim3 nbThreads; 
	
	protected HashSet<Tensor> cpu_tensors;
	protected HashSet<Tensor> gpu_tensors;
	private static int verbose=1;
	
	public Env()
	{
		
		 available_cpu_memory=cpu_memory_max;
		 available_gpu_memory=gpu_memory_max;
		 //functions=new HashMap<String,CUfunction>();
		 functions=new HashMap<String,KernelLauncher>();
		 nbThreads=new dim3(Env.nbThreadsPerBlock,1,1);
		 
	}
	
	@Override
	protected void finalize () throws Throwable {
		JCublas2.cublasDestroy(handle);
		super.finalize();
	}
	
	public  cublasHandle getCublasHandle(){
		if(handle==null){
			GPUinit();
		}
		return handle;
	}
	
	public KernelLauncher getFunction(String f){
		//
		/*if(handle==null){
			GPUinit();
		}*/
		if(functions.containsKey(f)){
			return functions.get(f);
		}
		//CUfunction function = new CUfunction();
		//JCudaDriver.cuModuleGetFunction(function, module, f);
		KernelLauncher function = KernelLauncher.create("cuda/mlp.cu",f);
		functions.put(f,function);
		return function;
	}
	
	public static KernelLauncher getGPUFunction(String f){
		return(getEnv().getFunction(f));
	}
	public void GPUinit(){
		JCublas2.setExceptionsEnabled(true);
        JCuda.setExceptionsEnabled(true);
		  if( cudaError.cudaSuccess != JCudaDriver.cuInit( 0 ) ) {                                        
		    throw new RuntimeException("CUDA: Not initialized\n" );                 
		  }
		  /*device = new CUdevice();
		  if( cudaError.cudaSuccess != JCudaDriver.cuDeviceGet( device, 0 ) ) {                        
			  throw new RuntimeException("CUDA: Cannot get the device\n");                 
		  }                                
		  context = new CUcontext();
		  if( cudaError.cudaSuccess != JCudaDriver.cuCtxCreate( context, 0, device ) ) {                
			  throw new RuntimeException("CUDA: Cannot create the context\n");      
		  } */                             
		  handle = new cublasHandle();
		  int status=JCublas2.cublasCreate(handle);
		  if (status==cublasStatus.CUBLAS_STATUS_SUCCESS)
		  {
				handleCreated=true;
				JCublas2.cublasSetPointerMode(handle,cublasPointerMode.CUBLAS_POINTER_MODE_HOST);
		  }
		  else
		  {
				throw new RuntimeException("Unable to create CUBLAS Context...");
		  }     
		  handleCreated=true;
		  // Load the PTX that contains the kernel.
		  /*CUmodule module = new CUmodule();
		  JCudaDriver.cuModuleLoad(module, "mlp.ptx");*/
	}
	
	
	
	public static void setVerbose(int v){
		verbose=v;
	}
	public static int getVerbose(){
		return(verbose);
	}
	
	static boolean getDisplayCPUGPUCopies()
	{
		return(getEnv().displayCPUGPUCopies);		
	}

	static void setDisplayCPUGPUCopies(boolean f)
	{
		getEnv().displayCPUGPUCopies=f;
	}

	

	static Env getEnv()
	{
		if (env==null)
		{
			env=new Env();
			
		}
		
		return(env);
	}

	/*void declareGPUTensor(Tensor t)
	{
		t.setEnv(this);
	}*/
	/*
		Tensor tbis=cpu_tensors.get(t);
		if (tbis)
		{
			available_cpu_memory+=t.getMemorySize();
			cpu_tensors.erase();
		}
		if (gpu_tensors.find(t)==gpu_tensors.end())
		{
			int s=t->getMemorySize();
			available_gpu_memory-=s;
			gpu_tensors.insert(t);
		}
	}
	*/
	
	/*void declareCPUTensor(Tensor t)
	{
		t->setEnv(this);
	}*/
	/*
		if (gpu_tensors.find(t)!=gpu_tensors.end())
		{
			available_gpu_memory+=t->getMemorySize();
			gpu_tensors.erase(gpu_tensors.find(t));
		}
		if (cpu_tensors.find(t)==cpu_tensors.end())
		{
			int s=t->getMemorySize();
			available_cpu_memory-=s;
			cpu_tensors.insert(t);
		}
	}*/

	
	

}
