package mlp;

import java.util.ArrayList;
public abstract class TensorModule extends Module {

	protected Matrix parameters;
	//protected CPUMatrix saved_parameters;
	
	int input_size;
	int output_size;
	//protected Parameter[] paramList;
	protected Parameters paramList;
	
	public TensorModule(int input_size, int output_size) //Parameter[] pList, int input_size, int output_size)
	{
		origin_module=null;
		parameters=new CPUMatrix(input_size,output_size);
		//setParameters(pList,input_size,output_size);
		paramList=new Parameters();
				//new Parameter[input_size*output_size];
		this.input_size=input_size;
		this.output_size=output_size;
		//majParams();*/
		
	}
	
	public TensorModule(TensorModule org)
	{
		if(org.sharedForward){
			throw new RuntimeException("Please do not copy a shared forward module");
		}
		TensorModule origin=(TensorModule)org;
		parameters=origin.parameters;
		locked=org.locked;
		paramList=origin.paramList;
		/*for(int i=0;i<paramList.length;i++){
			paramList[i].parent.addListener(this);
		}*/
		input_size=parameters.getNumberOfRows();
		output_size=parameters.getNumberOfColumns();
		origin_module=origin;
		//majParams();
		/*int nb=input_size*output_size;
		for(int i=0;i<nb;i++){
			paramList[i].getVal();
		}*/
	}


	public void majParams(){
		if(sharedForward){
			throw new RuntimeException("Please not call majParams on a shared forward module");
		}
		double[] vals=((CPUMatrix)parameters).getValues();
		int nb=input_size*output_size;
		//ArrayList<Parameter> pars=paramList.getParams();
		for(int i=0;i<nb;i++){
			vals[i]=paramList.get(i).getVal();
			//vals[i]=paramList[i].getVal();
		}
		paramsChanged=false;
	}
	
	public Parameters getParamList(){
		return paramList;
	}
	
	public int getInputSize(){
		return input_size;
	}
	public int getOutputSize(){
		return output_size;
	}
	
	/*public void destroy()
	{
		paramList=null;
		parameters=null;
	}*/
	/*
		for(Parameter p:paramList){
			p.parent.removeListener(this);
		}
	}*/
	
	/*public void paramsChanged(){
		paramsChanged=true;
		//majParams();
		//init_gradient();
	}*/
	
	/*public Tensor getParameters()
	{
		
		return(new Tensor(parameters));
	}*/
	
	
	public int getNbParams(){
		if(sharedForward){
			this.origin_module.getNbParams();
		}
		return input_size*output_size;
	}
	
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException("Please not call setParameters on a shared forward module");
		}
		if(pList.size()!=(input_size*output_size)){
			throw new RuntimeException("pList is not of good dimensions");
		}
		paramList=pList;
		/*for(Parameter p:pList){
			p.parent.addListener(this);
		}*/
		paramsChanged();
	}
	public Tensor getParameters()
	{
		if(sharedForward){
			this.origin_module.getParameters();
		}
		return(new Tensor(parameters));
	}
	

	/*public void randomize(double variance)
	{
		for(int i=0;i<input_size;i++)
		{
			for(int j=0;j<output_size;j++)
			{
				double v=(double)((Math.random()*2.0-1.0)*variance);
				parameters.setValue(i,j,v);
			}
		}
	}*/
	
	/*public void push()
	{
		saved_parameters=parameters.copy();
	}

	public void pop()
	{
		parameters=saved_parameters;
	}*/
	
	
	@Override
	public void updateParameters(double line){
		if(sharedForward){
			throw new RuntimeException("Please not call updateParameters on a shared forward module");
		}
					paramList.update(line);
					paramsChanged();
	}
	public int getNbInputMatrix(){
		
		return 1;
	}
}
