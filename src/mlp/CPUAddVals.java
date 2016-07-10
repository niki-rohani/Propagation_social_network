package mlp;

import java.util.ArrayList;

public class CPUAddVals extends Module {

		protected Tensor tensor_output;
		protected Tensor tensor_delta;
		protected int size; // number of cols of input matrix 
		protected CPUMatrix vals;
		
		
		public CPUAddVals(int size){
			this(size,(CPUMatrix)null);
		}
		
		public CPUAddVals(int size,double val){
			this(size,new CPUMatrix(1,size,val));
		}
		
		public CPUAddVals(int size,CPUMatrix vals){
			this.size=size;
			if(vals==null){
				this.vals=new CPUMatrix(1,size); 
				for(int i=0;i<size;i++){
					this.vals.setValue(0,i,1.0f);
				}
			}
			else{
				this.vals=vals;
			}
			tensor_output=new Tensor(1);
			tensor_delta=new Tensor(1);
			//listeners=new ArrayList<CPUAddVals>();
			//tensor_output.setMatrix(0, new CPUMatrix(1,size));
			//tensor_delta.setMatrix(0, new CPUMatrix(1,size));
		}
		
		public CPUAddVals(CPUAddVals org){
			this(org.size,org.vals);
			this.origin_module=org;
			this.name=name;
		}
		
		public void setVals(CPUMatrix mat){
			if(sharedForward){
				throw new RuntimeException("Please not call setVals on a shared forward module");
			}
			vals=mat;
			for(Module l:getListeners()){
				((CPUAddVals)l).vals=mat;
			}
		}
		public void setVals(double v){
			if(sharedForward){
				throw new RuntimeException("Please not call setVals on a shared forward module");
			}
			vals.setValues(v);
		}
		public Module forwardSharedModule()
		{
			if(sharedForward){
				return origin_module.forwardSharedModule();
			}
			CPUAddVals ret=new CPUAddVals(this);
			ret.sharedForward=true;
			ret.origin_module=this;
			addListener(ret);
			return(ret);
		}
		
		public Module parametersSharedModule()
		{
			if(sharedForward){
				throw new RuntimeException("Please do not copy a shared forward module");
			}
			return(new CPUAddVals(this));
		}


		void allocate(int minibatch_size)
		{
			
			if (tensor_delta.getMatrix(0)!=null)
			{
				if (tensor_delta.getMatrix(0).getNumberOfRows()!=minibatch_size){
					tensor_delta.getMatrix(0).transformTo(minibatch_size, size);
					tensor_output.getMatrix(0).transformTo(minibatch_size, size);
				}
			}
			else{	
				tensor_delta.setMatrix(0,new CPUMatrix(minibatch_size,size));
				tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,size));
			}
		}
		
		
		public String toString(){
			return "CPUAddVals_name="+name+((sharedForward)?"_sharedForward":"");
		}
		
		@Override
		public int getNbInputMatrix(){
			return 1;
		}
		
		@Override
		public void forward(Tensor input) {
			//System.out.println(this); //+" input "+input);
			
			//System.out.println(input);
			int minibatch_size=input.getMatrix(0).getNumberOfRows();
			allocate(minibatch_size);
			
			if(sharedForward){
				tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
				return;
			}
			
			input.ensureCPUMatrices();
			tensor_output.ensureCPUMatrices();
			
			if(input.getNumberOfMatrices()!=1){
				throw new RuntimeException(this+" Format problem of input => given "+input.getNumberOfMatrices()+" matrices, required "+1);
			}
			
			if(minibatch_size%vals.number_of_rows!=0){
				throw new RuntimeException(this+" Format pb : vals and input number of rows incompatible");
			}
			
			
			CPUMatrix _output=(CPUMatrix)tensor_output.getMatrix(0);
			

			double[] out=_output.getValues();
			CPUMatrix _input=(CPUMatrix)input.getMatrix(0);
			int nbc=_input.getNumberOfColumns();
			if(nbc!=size){
				throw new RuntimeException(this+" : Matrix "+0+" has "+nbc+" columns , required = "+size);
			}
			if(size!=vals.number_of_columns){
				throw new RuntimeException(this+" Format pb : number of columns vals  and number of columns in input matrix  incompatible");
			}
			double[] in=_input.getValues();
			int nbrw=vals.number_of_rows;
			int n=minibatch_size/nbrw;
			for(int nbexample=0;nbexample<minibatch_size;nbexample++)
			{
				for(int idx_output=0;idx_output<size;idx_output++)
				{
					double w=vals.getValue((int)(nbexample/n),idx_output);
					
					out[Matrix.IDX2C(nbexample,idx_output,minibatch_size)]=w+in[Matrix.IDX2C(nbexample,idx_output,minibatch_size)];
				}
			}
			
			
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

			if(input.getNumberOfMatrices()!=1){
				throw new RuntimeException(this+" Format problem of input => given "+input.getNumberOfMatrices()+" matrices, required "+1);
			}
			
			if(minibatch_size%vals.number_of_rows!=0){
				throw new RuntimeException(this+" Format pb : vals and input number of rows incompatible");
			}
			
			double[] d_out=null;
			if (deltas_output!=null){
				deltas_output.ensureCPUMatrices();
				CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
				d_out=_output.getValues();
			}
			
			if(size!=vals.number_of_columns){
				throw new RuntimeException(this+" Format pb : number of columns vals  and nb of columns of input matrix  incompatible");
			}
			int nbrw=vals.number_of_rows;
			int n=minibatch_size/nbrw;
			
			//Computation of deltas_input
			
			CPUMatrix _input=(CPUMatrix)(input.getMatrix(0));
			int nbr=_input.getNumberOfRows();
			if(nbr!=minibatch_size){
				throw new RuntimeException(this+"_compute_deltas : Input Matrix  has "+nbr+" rows , required = "+minibatch_size);
			}
			int nbc=_input.getNumberOfColumns();
			if(nbc!=size){
				throw new RuntimeException(this+"_compute_deltas : Input Matrix has "+nbc+" columns , required = "+size);
			}
				
			CPUMatrix _tensor_delta=(CPUMatrix)(tensor_delta.getMatrix(0));
			double[] d_in=_tensor_delta.getValues();
				
			for(int nbexample=0;nbexample<minibatch_size;nbexample++)
			{		
				for(int idx_input=0;idx_input<size;idx_input++)
				{
						int idx=Matrix.IDX2C(nbexample,idx_input,minibatch_size);
						double o=1.0f;
						if(d_out!=null){o=d_out[idx];}
						d_in[idx]=o;
						//System.out.println("CPUSum "+d_in[idx]);
					
				}
			}		

		}

		@Override
		public Tensor getDelta() {
			return(tensor_delta);
		}

}
