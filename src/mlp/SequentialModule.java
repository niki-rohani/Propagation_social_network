package mlp;

import java.util.ArrayList;

//import utils.Keyboard;

public class SequentialModule extends Module {
	protected ArrayList<Module> modules;
	//protected ArrayList<SequentialModule> listeners;
	
	public SequentialModule(){
		modules=new ArrayList<Module>();
		//listeners=new ArrayList<SequentialModule>();
	}
	
	/*public void removeListener(SequentialModule l){
		listeners.remove(l);
	}*/
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		SequentialModule ret=new SequentialModule();
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
	public int getNbInputMatrix(){
		int nb=0;
		if(modules.size()>0){
			nb=modules.get(0).getNbInputMatrix();
			//System.out.println(modules.get(0));
		}
		
		return nb;
	}
	
	/*public String toString(){
		return "SequentialModule";
	}*/
	
	public String getHierarchy(int level){
		String t="";
		for(int i=0;i<level;i++){
			t+="-";
		}
		
		String s=t+this+"\n";
		for(Module mod:modules){
			if(mod==null){
				s+=t+"-"+"null\n";
			}
			else{
				s+=mod.getHierarchy(level+1)+"\n";
			}
		}
		return s;
	}
	
	public Module parametersSharedModule()
	{
		if(sharedForward){
			throw new RuntimeException("Please do not copy a shared forward module");
		}
		
		SequentialModule m=new SequentialModule();
		m.origin_module=this;
		for(int i=0;i<(int)modules.size();i++)
		{
			m.addModule(modules.get(i).parametersSharedModule());
		}
		return(m);
	}
	
	public Module getModule(int i) {
		if(sharedForward){
			throw new RuntimeException("Please not call getModule on a shared forward module");
		}
		return(modules.get(i));
	}

	public ArrayList<Module> getModules(){
		if(sharedForward){
			throw new RuntimeException("Please not call getModules on a shared forward module");
		}
		return modules;
	}
	
	
	public void destroy(){
		super.destroy();
		
		for(Module mmod:modules){
			mmod.destroy();
		}
		modules.clear();
		
	}
	
	public void clearListeners(){
		super.clearListeners();
		
		for(Module mmod:modules){
			mmod.clearListeners();
		}
		
	}
	
	public void clearModules(){
		if(sharedForward){
			throw new RuntimeException("Please not call clearModules on a shared forward module");
		}
		modules.clear();
		for(Module mod:getListeners()){
			((SequentialModule)mod).modules.clear();
		}
	}
	
	public void addModule(Module m)
	{
		if(sharedForward){
			throw new RuntimeException("Please not call addModule on a shared forward module");
		}
		modules.add(m);
		for(Module mod:getListeners()){
			((SequentialModule) mod).modules.add(m.forwardSharedModule());
		}
	}
	
	public void setModule(int i,Module m)
	{
		if(sharedForward){
			throw new RuntimeException("Please not call setModule on a shared forward module");
		}
		modules.set(i,m);
		for(Module mod:getListeners()){
			((SequentialModule)mod).modules.set(i,m.forwardSharedModule());
		}
	}
	
	/*public void destroy(){
		for(Module m:modules){
			m.destroy();
		}
		//modules.clear();
		
	}*/
	
	@Override
	protected String getStructure(int prof){
		String s="";
		for(int i=0;i<prof;i++){
			s+="\t";
		}
		s+=this+":\n";
		for(Module m:modules){
			s+=m.getStructure(prof+1)+"\n";
		}
		return s;
	}
	
	@Override
	public void forward(Tensor input) {
		
		for(int i=0;i<(int)modules.size();i++)
		{
			modules.get(i).forward(input);
			input=modules.get(i).getOutput();
		}
	}
	
	@Override
	public void updateParameters(double line){
		if(sharedForward){
			throw new RuntimeException("Please not call updateParameters on a shared forward module");
		}
		for(int i=0;i<(int)modules.size();i++)
		{
			Module m=modules.get(i);
			if(m==null){
				continue;
			}
			if(!m.sharedForward){
				m.updateParameters(line);
			}
		}
	}

	@Override
	public Parameters getParamList(){
		if(sharedForward){
			return this.origin_module.getParamList();
		}
		Parameters p=new Parameters();
		for(int i=0;i<(int)modules.size();i++)
		{
			p.addSubParamList(modules.get(i).getParamList());
		}
		return p;
	}
	
	
	
	@Override
	public Tensor getOutput() {
		/*if(sharedForward){
			return this.origin_module.getOutput();
		}*/
		return(modules.get(modules.size()-1).getOutput());
	}

	@Override
	public void backward_updateGradient(Tensor input, Tensor deltas_output) {
		if(locked==true){
			return;
		}
		assert(modules.size()>0);
		if (modules.size()==1)
		{
			modules.get(0).backward_updateGradient(input,deltas_output);
		}
		else
		{
			for(int i=0;i<(int)modules.size();i++)
			{
				int index=modules.size()-1-i;
				Tensor in=index == 0 ? input : modules.get(index-1).getOutput();
				Tensor d=index==modules.size()-1 ? deltas_output : modules.get(index+1).getDelta();
				if (index!=0){ modules.get(index).backward(in,d); }
				else modules.get(index).backward_updateGradient(in,d);
			}
		}
		//Clavier.saisirLigne("");
	}
	
	/*@Override
	public void lockParams(){
		locked=true;
		
	}
	@Override
	public void unlockParams(){locked=false;}
	*/
	
	@Override
	public void paramsChanged(){
		if(sharedForward){
			throw new RuntimeException("Please not call paramsChanged on a shared forward module");
		}
		for(Module m:modules){
			if(m==null){
				continue;
			}
			if(!m.sharedForward){
				m.paramsChanged();
			}
		}
	}

	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltas_output) {
		throw new RuntimeException("Don't use backward_computeDeltaInputs with this module please (SequentialModule)....");
	}

	@Override
	public void backward(Tensor input,Tensor deltas_output)
	{
		
		assert(modules.size()>0);
		if (modules.size()==1)
		{
			modules.get(0).backward(input,deltas_output);
		}
		else
		{
			for(int i=0;i<(int)modules.size();i++)
			{
				int index=modules.size()-1-i;
				Tensor in=(index == 0) ? input : modules.get(index-1).getOutput();
				Tensor d=(index==modules.size()-1) ? deltas_output : modules.get(index+1).getDelta();
				modules.get(index).backward(in,d);
				
			}
		}
	}
	
	@Override
	public Tensor getDelta() {
		return(modules.get(0).getDelta());
	}

	/*@Override
	public void init_gradient() {
		for(int i=0;i<(int)modules.size();i++) modules.get(i).init_gradient();
	}*/
	
	public Module copyModuleToGPU()
	{
		SequentialModule module=new SequentialModule();
		for(int i=0;i<(int)modules.size();i++)
			module.addModule(modules.get(i).copyModuleToGPU());
		return(module);
	}

	public Module copyModuleToCPU()
	{
		SequentialModule module=new SequentialModule();
		for(int i=0;i<(int)modules.size();i++)
			module.addModule(modules.get(i).copyModuleToCPU());
		return(module);
	}

}
