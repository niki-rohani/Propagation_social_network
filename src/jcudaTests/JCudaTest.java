package jcudaTests;

import jcuda.*;
import jcuda.runtime.*;
import jcuda.driver.JCudaDriver;
import jcuda.driver.*;
import mlp.*;
public class JCudaTest
{
    public static void main(String args[])
    {
    	System.out.println(System.getProperty("java.library.path"));
    	/*try {
        	System.load("/local/lampriers/lampriers/Propagation/Java/Propagation/lib/libJCudaRuntime-linux-x86_64.so");
        	//System.load("/home/lampriers/propagation/Propagation/lib/
        } catch (UnsatisfiedLinkError e) {
          System.err.println("Native code library failed to load.\n" + e);
          System.exit(1);
        }*/
    	Pointer pointer = new Pointer();
        JCuda.cudaMalloc(pointer, 4);
        System.out.println("Pointer: "+pointer);
        JCuda.cudaFree(pointer);
        
        // Load the ptx file.
        CUmodule module = new CUmodule();
        JCudaDriver.cuModuleLoad(module, "Kernels_nmlp.ptx");

        // Obtain a function pointer to the kernel function.
        CUfunction function = new CUfunction();
        JCudaDriver.cuModuleGetFunction(function, module, "add");
        
        // Set up the kernel parameters: A pointer to an array
     // of pointers which point to the actual values.
    /* Pointer kernelParameters = Pointer.to(
         Pointer.to(new int[] numElements), 
         Pointer.to(deviceInputA), 
         Pointer.to(deviceInputB), 
         Pointer.to(deviceOutput)
     );

     // Call the kernel function.
     cuLaunchKernel(function, 
         gridSizeX,  1, 1,      // Grid dimension 
         blockSizeX, 1, 1,      // Block dimension
         0, null,               // Shared memory size and stream 
         kernelParameters, null // Kernel- and extra parameters
     ); */
    }
}