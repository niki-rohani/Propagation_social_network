#include <iostream>
#include "cublas.h"

#define __CUDA__NBTHREADS 1024

extern "C"
__global__ void functionShrink(const float *A,float *B, int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) 
	{
		if ((A[i]>=-1) && (A[i]<=1)) B[i]=0; 
		else B[i]=1;
	}
}

__global__ void functionDShrink(const float *A,float *B, int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) 
	{
		if ((A[i]>=-1) && (A[i]<=1)) B[i]=0; 		
	}
}

__global__ void functionPShrink(const float *A,float *B, int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) 
	{
		if (A[i]<0) B[i]=0;
		else B[i]=A[i];
	}
}

__global__ void functionDPShrink(const float *A,float *B, int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) 
	{
		if (A[i]<0) B[i]=0;
	}
}


__global__ void functionAdaptedTanH(const float *A,float *B, int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) B[i]=(float)(1.7159*tanh(0.6666*A[i]));
}

__global__ void functionLogistic(const float *A,float *B, int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) B[i]=(float)(1.0/(1.0+exp(-A[i])));
}

__global__ void functionDAdaptedTanH(const float *A,float *B, int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	
	float t=(float)(tanh(0.6666*A[i]));
	if (i<N) B[i]=(float)(0.66666*1.7159*(1.0-t*t));
}

__global__ void functionDLogistic(const float *A,float *B, int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	
	float f=(float)(1.0/(1.0+exp(-A[i])));
	if (i<N) B[i]=(float)(f*(1.0-f));
}

__global__ void kerrorSquare(float *A,const float *z,const float *x,const float *y,int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) A[i]=z[i]*(x[i]-y[i]);
}


__global__ void functionHingeLoss(const float *A,float *B, int N)
{	
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	float f=A[i]*B[i];
	if (f<1) B[i]=1-f; else B[i]=0;
}

void errorSquare(float *A,const float *z,const float *x,const float *y,int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	kerrorSquare<<<nbBlock,__CUDA__NBTHREADS>>>(A,z,x,y,N);
	
}


void activationFunctionShrink( const float *A,float *B, int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	functionShrink<<<nbBlock,__CUDA__NBTHREADS>>>(A,B,N);
}


void activationFunctionAdaptedTanH(const float *A,float *B, int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	functionAdaptedTanH<<<nbBlock,__CUDA__NBTHREADS>>>(A,B,N);
}

void activationFunctionLogistic(const float *A,float *B, int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	functionLogistic<<<nbBlock,__CUDA__NBTHREADS>>>(A,B,N);
}

void activationFunctionPShrink(const float *A,float *B, int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	functionPShrink<<<nbBlock,__CUDA__NBTHREADS>>>(A,B,N);
}



void activationFunctionDShrink(const float *A,float *B, int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	functionDShrink<<<nbBlock,__CUDA__NBTHREADS>>>(A,B,N);
}

void activationFunctionDPShrink(const float *A,float *B, int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	functionDPShrink<<<nbBlock,__CUDA__NBTHREADS>>>(A,B,N);
}

void activationFunctionDAdaptedTanH(const float *A,float *B, int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	functionDAdaptedTanH<<<nbBlock,__CUDA__NBTHREADS>>>(A,B,N);
}

void activationFunctionDLogistic(const float *A,float *B, int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	functionDLogistic<<<nbBlock,__CUDA__NBTHREADS>>>(A,B,N);
}


void hingeLossFunction(const float *A,float *B,int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	functionHingeLoss<<<nbBlock,__CUDA__NBTHREADS>>>(A,B,N);
}


__global__ void kmultiplicationTermeATerme(float *A,const float *B,int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) A[i]=A[i]*B[i];
}

void multiplicationTermeATerme(float *A,const float *B,int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	kmultiplicationTermeATerme<<<nbBlock,__CUDA__NBTHREADS>>>(A,B,N);
}

__global__ void k_squareLoss(float *dest,const float *A,const float *B,int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) dest[i]=(A[i]-B[i])*(A[i]-B[i]);
}

__global__ void k_hingeloss(float *dest,const float *A,const float *B,int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) 
	{
		float d=A[i]*B[i];
		if (A[i]*B[i]<1) dest[i]=1-d;
		else dest[i]=0;
	}
}

void GPU_SquareLoss(float *dest,const float *A,const float *B,int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	k_squareLoss<<<nbBlock,__CUDA__NBTHREADS>>>(dest,A,B,N);
}
void GPU_HingeLoss(float *dest,const float *A,const float *B,int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	k_hingeloss<<<nbBlock,__CUDA__NBTHREADS>>>(dest,A,B,N);
}


__global__ void k_fill(float *dest,float v,int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) dest[i]=v;
}

__global__ void k_dhingeloss(float *dest,const float *A,const float *B,int N)
{	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) 
	{
		if (A[i]*B[i]<1) dest[i]=-B[i]; else dest[i]=0;
	}
}



__global__ void k_difference(float *dest,const float *A,const float *B,int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) dest[i]=(A[i]-B[i]);
}


void GPU_Difference(float *dest,const float *A,const float *B,int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	k_difference<<<nbBlock,__CUDA__NBTHREADS>>>(dest,A,B,N);
}

void GPU_DHingeLoss(float *dest,const float *A,const float *B,int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	k_dhingeloss<<<nbBlock,__CUDA__NBTHREADS>>>(dest,A,B,N);
}

__global__ void k_addition(float *dest,const float *A,const float *B,int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) dest[i]=(A[i]+B[i]);
}

void GPU_Addition(float *dest,const float *A,const float *B,int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	k_addition<<<nbBlock,__CUDA__NBTHREADS>>>(dest,A,B,N);
}

__global__ void k_negative(float *dest,const float *A,int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) dest[i]=-A[i];
}


void GPU_Negative(float *dest,const float *A,int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	k_negative<<<nbBlock,__CUDA__NBTHREADS>>>(dest,A,N);
}


void GPU_fill(float *dest,float v,int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	k_fill<<<nbBlock,__CUDA__NBTHREADS>>>(dest,v,N);
}

__global__ void k_square(float *dest,const float *A,int N)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<N) dest[i]=A[i]*A[i];
}

void GPU_Square(float *dest,const float *A,int N)
{
	int nbBlock=N/__CUDA__NBTHREADS+1;
	k_square<<<nbBlock,__CUDA__NBTHREADS>>>(dest,A,N);
}

__global__ void k_l2norm_delta(float *dest,const float *A,const float *B,int nbexamples,int sizeexamples)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<nbexamples*sizeexamples)
	{	
		dest[i]=A[i%nbexamples]*B[i];
	}
}

void GPU_GPUL2Norm_Delta(float *dest,const float *A,const float *B,int nbexamples,int sizeexamples)
{
	int nbBlock=(nbexamples*sizeexamples)/__CUDA__NBTHREADS+1;
	k_l2norm_delta<<<nbBlock,__CUDA__NBTHREADS>>>(dest,A,B,nbexamples,sizeexamples);
}

__global__ void k_sum_parexemple(float *dest,const float *A,int nbexamples,int sizeexamples)
{
	int i = blockIdx.x * __CUDA__NBTHREADS + threadIdx.x;
	if (i<nbexamples*sizeexamples)
	{	
		dest[i%nbexamples]+=A[i];
	}
}

void GPU_Sum_ParExemple(float *out, float *A,int minibatch_size,int size)
{
	int nbBlock=(minibatch_size*size)/__CUDA__NBTHREADS+1;
	k_fill<<<nbBlock,__CUDA__NBTHREADS>>>(out,0.0f,minibatch_size*size);
	k_sum_parexemple<<<nbBlock,__CUDA__NBTHREADS>>>(out,A,minibatch_size,size);

}
