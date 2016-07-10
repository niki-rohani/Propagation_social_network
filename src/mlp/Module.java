package mlp;

import java.util.ArrayList;

/**
 * 
 * @author denoyer, lamprier
 *
 */

public abstract class Module {
	
	protected boolean paramsChanged=true;
	protected boolean locked=false;
	protected Module origin_module=null;
	protected boolean sharedForward=false; 
	public static int nbModules=0;
	public int idModule;
	public String name;
	private ArrayList<Module> listeners;
	public Module(){
		nbModules++;
		idModule=nbModules;
		name=""+idModule;
		listeners=null;
	}
	public int hashCode(){
		return idModule;
	}
	public boolean isLocked(){
		return locked;
	}
	public void setName(String name){
		this.name=name;
	}
	
	/**
	 * The output tensor must not be deleted by the user
	 * dans input: 1 exemple par ligne, nb_lignes=minibatch_size
	 */
	public abstract void forward(Tensor input);
	public abstract Tensor getOutput();
	public abstract void backward_updateGradient(Tensor input,Tensor deltas_output);	
	public abstract void backward_computeDeltaInputs(Tensor input,Tensor deltas_output);
	public void addListener(Module mod){
		if(sharedForward){
			throw new RuntimeException(this+".addListener => Cannot add a listener to a sharedForward module");
		}
		if(!mod.getClass().equals(this.getClass())){
			throw new RuntimeException(this+".addListener => "+mod+" is not of the same class than this object :"+mod.getClass().toString());
		}
		if(listeners==null){
			listeners=new ArrayList<Module>();
		}
		listeners.add(mod);
	}
	public void removeListener(Module mod){
		
		if(listeners!=null){
			
			listeners.remove(mod);
		}
	}
	public void clearListeners(){
		if(this.sharedForward){
			if(origin_module==null){throw new RuntimeException(this+": sharedForward without origin_module");}
			origin_module.removeListener(this);
		}
		sharedForward=false;
		origin_module=null;
		for(Module mod:getListeners()){
			mod.origin_module=null;
			mod.sharedForward=false;
		}
		listeners=null;
	}
	public void destroy(){
		clearListeners();
	}
	public ArrayList<Module> getListeners(){
		if(listeners==null){
			listeners=new ArrayList<Module>();
		}
		return listeners;
	}
	public void backward(Tensor input,Tensor deltas_output)
	{
		if(locked==false){
			backward_updateGradient(input,deltas_output);
			backward_computeDeltaInputs(input,deltas_output);
		}
	}
	public void showStructure(){
		
		System.out.println(getStructure(0));
	}
		
	protected String getStructure(int prof){
		String s="";
		for(int i=0;i<prof;i++){
			s+="\t";
		}
		return s+this.toString();
	}
	
	public void backward(Tensor input){
		/*Tensor tens=new Tensor(1);
		CPUMatrix mat=new CPUMatrix(0,0);
		tens.setMatrix(0, mat);*/
		
		backward(input,null);
	}
	public void backward_updateGradient(Tensor input){
		/*Tensor tens=new Tensor(1);
		CPUMatrix mat=new CPUMatrix(0,0);
		tens.setMatrix(0, mat);*/
		backward_updateGradient(input,null);
	}
	/*public void transferGradient(){
		// nothing to do
	}*/
	public void backward_computeDeltaInputs(Tensor input){
		/*Tensor tens=new Tensor(1);
		CPUMatrix mat=new CPUMatrix(0,0);
		tens.setMatrix(0, mat);*/
		backward_computeDeltaInputs(input,null);
	}
	
	public abstract Tensor getDelta();
	
	//public abstract void init_gradient();

	//public abstract void updateParameters(double gradient_step);

	public void paramsChanged(){
		if(sharedForward){
			throw new RuntimeException("Please not call paramsChanged on a shared forward module");
		}
		//System.out.println(this+" "+super.toString());
		paramsChanged=true;
	}
	
	public int getNbInputMatrix(){
		return 1;
	}
	
	
	
	/*public int getNbOutputMatrix(){
		return 1;
	}*/
	
	/*public void destroy(){
		
	}*/
	
	public void lockParams(){lockParams(false);}
	public void unlockParams(){unlockParams(false);}
	public void lockParams(boolean rec){
		locked=true;
		if(rec){
			if(listeners==null){
				return;
			}
			for(Module m:listeners){
				m.lockParams(rec);
			}
		}
	}
	public void unlockParams(boolean rec){
		locked=false;
		if(rec){
			if(listeners==null){
				return;
			}
			for(Module m:listeners){
				m.unlockParams(rec);
			}
		}
	}
	
	/**
	 * Returns the parameters of the module as a tensor
	 * null if no parameters
	 */
	public Tensor getParameters()
	{
		return(null);
	}
	
	public Parameters getParamList(){
		return(new Parameters());
	}
	
	public void setParameters(Parameters pList)
	{
		throw new RuntimeException("Pas de parametres pour ce module "+this);
	}

	public int getNbParams(){
		return(0);
	}
	
	public String getHierarchy(int level){
		String s="";
		for(int i=0;i<level;i++){
			s+="-";
		}
		s+=this;
		return s;
	}
	
	/**
	 * Randomize the parameters of a module
	 */
	/*public void randomize(double variance)
	{		
	}*/

	/**
	* Push the current  parameters 
	*/
	public void push()
	{
		System.out.println("This module is not able to push its parameters...");
	}

	/**
	 * Pop the parameters
	 */
	public void pop()
	{
		System.out.println("This module is not able to push its parameters...");
	}

	public Module copyModuleToGPU()
	{
			throw new RuntimeException("This module is not able copy itself to GPU");
			//return(null);
	}

	public Module copyModuleToCPU()
	{
		throw new RuntimeException("This module is not able copy itself to CPU");
		
		//return(null);
	}

	public Module parametersSharedModule()
	{
		throw new RuntimeException(this+".parametersSharedModule :This module is not able create a shared version of itself");
		//return(null);
	}
	
	public Module forwardSharedModule()
	{
		throw new RuntimeException(this+".forwardSharedModule :This module is not able create a shared version of itself");
		//return(null);
	}

	public String toString()
	{
		return("Module::"+this.getClass()+" name="+name);
	}
	
	public void updateParameters(double line){
		if(sharedForward){
			throw new RuntimeException("Please not call updateParameters on a shared forward module");
		}
	}
}
