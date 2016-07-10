package mlp;

public abstract class Criterion extends Module {
	protected Tensor tensor_output;
	protected Tensor labels=null;
	public abstract void backward(Tensor input,Tensor output);	
	public abstract Tensor getDelta();
	//public abstract void computeValue(Tensor input,Tensor output);
	public abstract Tensor getValue();
	
	public Criterion(){}
	
	public Criterion(Criterion org){
		origin_module=org;
		labels=org.labels;
	}
	
	public void setLabels(Tensor labels){
		this.labels=labels;
	}
	//public abstract float getAverageValue();

	public Criterion copyCriterionToGPU()
	{
				throw new RuntimeException("This criterion is not able copy itself to GPU");
	}

	public Criterion copyCriterionToCPU()
	{
				throw new RuntimeException("This  criterion is not able copy itself to CPU");
	}
	public abstract Criterion copy();
	
	/*public void forward(Tensor input){
		throw new RuntimeException("need desired outputs => use computeValue");
	}*/
	public Tensor getOutput(){
		return this.tensor_output;
	}
	public void backward_updateGradient(Tensor input,Tensor deltas_output){
		throw new RuntimeException("Nothing to do in a criterion for gradient");
	}
	public void init_gradient(){
		throw new RuntimeException("Nothing to do in a criterion for gradient");
	}
	public void backward_computeDeltaInputs(Tensor input,Tensor deltas_output){
		backward(input,deltas_output);
	}
}
