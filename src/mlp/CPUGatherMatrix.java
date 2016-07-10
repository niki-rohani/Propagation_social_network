package mlp;

import java.util.ArrayList;

/**
 * 
 * Gathers matrices in a unique output matrix.
 * Attention => pas encore tellement testee
 * 
 * @author lamprier
 *
 */


public class CPUGatherMatrix extends Module {

	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected boolean gatherInCols=true;
	//protected ArrayList<CPUSum> listeners;
	protected int nbMatrices;
	
	public CPUGatherMatrix(int nbMatrices, boolean gatherInCols){
		this.gatherInCols=gatherInCols;
		this.nbMatrices=nbMatrices;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(nbMatrices);
	}
	
	
	
	public CPUGatherMatrix(CPUGatherMatrix org){
		this(org.nbMatrices,org.gatherInCols);
		this.origin_module=org;
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(1);
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		CPUGatherMatrix ret=new CPUGatherMatrix(this);
		ret.sharedForward=true;
		//addListener(ret);
		return(ret);
	}
	
	public Module parametersSharedModule()
	{
		if(sharedForward){
			throw new RuntimeException("Please do not copy a shared forward module");
		}
		return(new CPUGatherMatrix(this.nbMatrices,this.gatherInCols));
	}

	
	

	void allocate(Tensor input)
	{
		
		int nbIn=input.getNumberOfMatrices();
		if(nbIn!=nbMatrices){
			throw new RuntimeException(this+": bad number of input matrices");
		}
		int nbD=tensor_delta.getNumberOfMatrices();
		if(nbIn!=nbD){
			tensor_delta=new Tensor(nbIn);
		}
			
		int sumr=0;
		int sumc=0;
		for(int i=0;i<nbIn;i++){
			Matrix in=input.getMatrix(i);
			int inr=in.getNumberOfRows();
			int inc=in.getNumberOfColumns();
			if(this.gatherInCols){
				sumc+=inc;
				if(i==0){
					sumr=inr;
				}
				else{
					if(sumr!=inr){
						throw new RuntimeException(this+": Problem on input tensor, input matrix have not the same number of rows");
					}
				}
			}
			else{
				sumr+=inr;
				if(i==0){
					sumc=inc;
				}
				else{
					if(sumc!=inc){
						throw new RuntimeException(this+": Problem on input tensor, input matrix have not the same number of cols");
					}
				}
			}
			
			Matrix mat=tensor_delta.getMatrix(i);
			if(mat==null){
				tensor_delta.setMatrix(i,new CPUMatrix(inr,inc));
			}
			else{
				mat.transformTo(inr,inc);
				
			}
		}
		
		Matrix out=tensor_output.getMatrix(0);
		if(out==null){
			tensor_output.setMatrix(0,new CPUMatrix(sumr,sumc));
		}
		else{
			out.transformTo(sumr, sumc);
		}
		
	}
	
	public String toString(){
		return "CPUGatherMatrix_"+this.gatherInCols+" name="+name+((sharedForward)?"_sharedForward":"");
	}
	
	@Override
	public void forward(Tensor input) {
		//System.out.println(this); //+" input "+input);
		
		//System.out.println(input);
		allocate(input);
		
		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			return;
		}
		
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		
		CPUMatrix _output=(CPUMatrix)tensor_output.getMatrix(0);
		int nbr=_output.getNumberOfRows();
		
		double[] out=_output.getValues();
		CPUMatrix _input=(CPUMatrix)input.getMatrix(0);
		int nbcIn=_input.getNumberOfColumns();
		int nbrIn=_input.getNumberOfRows();
		
		double[] in=_input.getValues();
		for(int nbexample=0;nbexample<nbrIn;nbexample++)
		{
			for(int idx_output=0;idx_output<nbcIn;idx_output++)
			{
				out[Matrix.IDX2C(nbexample,idx_output,nbr)]=in[Matrix.IDX2C(nbexample,idx_output,nbrIn)];
			}
		}
		int sum=nbrIn;
		if(this.gatherInCols){
			sum=nbcIn;
		}
		
		for(int i=1;i<this.nbMatrices;i++)
		{
			_input=(CPUMatrix)(input.getMatrix(i));
			nbcIn=_input.getNumberOfColumns();
			nbrIn=_input.getNumberOfRows();
			in=_input.getValues();
			
			for(int nbexample=0;nbexample<nbrIn;nbexample++)
			{
				for(int idx_output=0;idx_output<nbcIn;idx_output++)
				{
					if(this.gatherInCols){
						out[Matrix.IDX2C(nbexample,idx_output+sum,nbr)]=in[Matrix.IDX2C(nbexample,idx_output,nbrIn)];
					}
					else{
						out[Matrix.IDX2C(nbexample+sum,idx_output,nbr)]=in[Matrix.IDX2C(nbexample,idx_output,nbrIn)];
					}
				}
			}
			if(this.gatherInCols){
				sum+=nbcIn;
			}
			else{
				sum+=nbrIn;
			}
		}

		//System.out.println("Infer : \n"+tensor_output);
	}

	@Override
	public int getNbInputMatrix(){
		return this.nbMatrices;
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

		CPUMatrix output=(CPUMatrix)tensor_output.getMatrix(0);
		int nbr=output.getNumberOfRows();
		int nbc=output.getNumberOfColumns();
		
		if(input.getNumberOfMatrices()!=nbMatrices){
			throw new RuntimeException(this+" Format problem of input => given "+input.getNumberOfMatrices()+" matrices, required "+nbMatrices);
		}
		
		
		
		
		double[] d_out=null;
		if (deltas_output!=null){
			if(deltas_output.getNumberOfMatrices()!=0){
				throw new RuntimeException(this+" Format problem of deltas_output => given "+deltas_output.getNumberOfMatrices()+" matrices, required 1");
			}
			deltas_output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
			if(_output.getNumberOfColumns()!=nbc){
				throw new RuntimeException(this+" Format problem of deltas_output => given "+_output.getNumberOfColumns()+" columns, required "+nbc);
			
			}
			if(_output.getNumberOfRows()!=nbr){
				throw new RuntimeException(this+" Format problem of deltas_output => given "+_output.getNumberOfRows()+" rows, required "+nbr);
			
			}
			d_out=_output.getValues();
		}
		
	
		//Computation of deltas_input
		int sum=0;
		for(int i=0;i<nbMatrices;i++)
		{
			
			CPUMatrix _input=(CPUMatrix)(input.getMatrix(i));
			int nbrIn=_input.getNumberOfRows();
			int nbcIn=_input.getNumberOfColumns();
			
			if(this.gatherInCols){
				if(nbrIn!=nbr){
					throw new RuntimeException(this+" Format problem on input => given "+nbrIn+" rows, required "+nbr);
					
				}
				
			}
			else{
				if(nbcIn!=nbc){
					throw new RuntimeException(this+" Format problem on input => given "+nbcIn+" columns, required "+nbc);
					
				}
			}
			
			
			CPUMatrix _tensor_delta=(CPUMatrix)(tensor_delta.getMatrix(i));
			int nbrD=_input.getNumberOfRows();
			
			if(nbrIn!=nbrD){
				throw new RuntimeException(this+"_compute_deltas : Matrix "+i+" has "+nbrIn+" rows , required = "+nbrD);
			}
			this.tensor_delta.getMatrix(i);
			int nbcD=_input.getNumberOfColumns();
			if(nbcIn!=nbcD){
				throw new RuntimeException(this+"_compute_deltas : Matrix "+i+" has "+nbcIn+" columns , required = "+nbcD);
			}
			double[] d_in=_tensor_delta.getValues();
			
			
			
			for(int nbexample=0;nbexample<nbrIn;nbexample++)
			{
				
				for(int idx_input=0;idx_input<nbcIn;idx_input++)
				{
					double o=1.0f;
					if(d_out!=null){
						if(this.gatherInCols){
							o=d_out[Matrix.IDX2C(nbexample,idx_input+sum,nbr)];
						}
						else{
							o=d_out[Matrix.IDX2C(nbexample+sum,idx_input+sum,nbr)];
						}
					}
						
					d_in[Matrix.IDX2C(nbexample,idx_input,nbrIn)]=o;
					//System.out.println("CPUSum "+d_in[idx]);
				}
			}
			
			if(this.gatherInCols){
				sum+=nbcIn;
			}
			else{
				sum+=nbrIn;
			}
			
		}		

	}

	@Override
	public Tensor getDelta() {
		return(tensor_delta);
	}

}
