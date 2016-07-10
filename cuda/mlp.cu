#include <iostream>
#include "cublas.h"
//#include "cuPrintf.cu"

//#define __CUDA__NBTHREADS 1024

extern "C"
__global__ void add(int n, float *a, float *b, float *sum)
{
    int i = blockIdx.x * blockDim.x + threadIdx.x;
    if (i<n)
    {
        sum[i] = a[i] + b[i];
    }
}

extern "C"
__global__ void GPU_fill(float *dest,float v,int N)
{
	int i = blockIdx.x * blockDim.x + threadIdx.x;
	if (i<N) dest[i]=v;
}


extern "C"
__global__ void setVal(float *dest,float v,int idx)
{
	dest[idx]=v;
}



extern "C"
__global__ void getVal(float *from,float *dest,int idx)
{
	dest[0]=from[idx];
}


extern "C"
__global__ void ccVals(float *from,float **dest,int N)
{
	//# if __CUDA_ARCH__>=200
    //	printf("%d \n", N);
	//#endif 
	int i = blockIdx.x * blockDim.x + threadIdx.x;
	if (i<N) dest[i][0]=from[i];
}

