package mlp;

import java.util.ArrayList;

public class TableModule extends Module {
	protected ArrayList<Module> modules;
	//protected ArrayList<TableModule> listeners;
	
	
	//protected int[] nbInputPerMod; // nb matrices each module waits as input (if 
	public TableModule(){ //int[] nbInputPerMod){ //int nbInputPerMod){
		//this.nbInputPerMod=nbInputPerMod;
		modules=new ArrayList<Module>();
		//listeners=new ArrayList<TableModule>();
	}
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		
		TableModule ret=new TableModule();
		ret.sharedForward=true;
		addListener(ret);
		ret.origin_module=this;
		ret.name=this.name;
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
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
	
	public String toString(){
		return "TableModule "+this.name+" "+this.modules.size()+" modules "+this.getListeners().size()+" listeners "+sharedForward;
	}
	
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
	
	public int getNbInputMatrix(){
		int nb=0;
		int i=0;
		//System.out.println("getNbMatrix "+this);
		for(Module mod:modules){
			//System.out.println("getNbMatrix mod="+mod);
			int n=mod.getNbInputMatrix();
			//System.out.println("getNbMatrix mod="+mod+" "+n);
			
			nb+=n;
			//System.out.println(i+" =>"+n);
			i++;
		}
		//System.out.println("getNbMatrix "+this+" "+nb);
		return nb;
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
	
	/*public String toString(){
		return "TableModule";
	}*/
	
	/*public void destroy(){
		for(Module m:modules){
			m.destroy();
		}
		//modules.clear();
	}*/
	
	public void clearModules(){
		if(sharedForward){
			throw new RuntimeException("Please not call clearModules on a shared forward module");
		}
		modules.clear();
		for(Module mod:getListeners()){
			((TableModule) mod).modules.clear();
		}
	}
	
	/*public void lockParams(){
		locked=true;
		
	}
	public void unlockParams(){locked=false;}
	*/
	
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
	/*public void destroy(){
		if(sharedForward){
			((TableModule)this.origin_module).listeners.remove(this);
		}
		for(Module mmod:modules){
			mmod.destroy();
		}
		modules.clear();
		for(TableModule mod:listeners){
			for(Module mmod:mod.modules){
				mmod.destroy();
			}
			mod.modules.clear();
		}
		listeners.clear();
	}*/
	
	public void addModule(Module m)
	{
		if(sharedForward){
			throw new RuntimeException("Please not call addModule on a shared forward module");
		}
		modules.add(m);
		for(Module mod:getListeners()){
			((TableModule) mod).modules.add(m.forwardSharedModule());
		}
	}
	
	public void setModule(int i,Module m)
	{
		if(sharedForward){
			throw new RuntimeException("Please not call setModule on a shared forward module");
		}
		modules.set(i,m);
		for(Module mod:getListeners()){
			((TableModule) mod).modules.set(i, m.forwardSharedModule());
		}
	}
	
	public Module parametersSharedModule()
	{
		if(sharedForward){
			throw new RuntimeException("Please do not copy a shared forward module");
		}
		TableModule m=new TableModule();
		m.origin_module=this;
		for(int i=0;i<(int)modules.size();i++)
		{
			m.addModule(modules.get(i).parametersSharedModule());
		}
		return(m);
	}

	
	@Override
	public void forward(Tensor input) {
		//System.out.println(this+" forward"); 
		
		//System.out.println(this+" "+input);
		int nbM=this.getNbInputMatrix();
		//System.out.println("nb required input matrix = "+nbM);
		
		if((nbM>0) && (input==null)){
			throw new RuntimeException( "Format problem in TableModule" );
		}
		if(input==null){
			input=new Tensor(0);
		}
	
		//System.out.println(input);
		//System.out.println(nbM);
		int nbMats=input.getNumberOfMatrices();
		if(nbM!=nbMats){
			throw new RuntimeException( "Format problem in TableModule "+name+" : required inputs "+nbM+", given inputs "+nbMats );
		}
		int m=0;
		for(int i=0;i<(int)modules.size();i++)
		{
			Module mod=modules.get(i);
			int nbi=mod.getNbInputMatrix();
			Tensor t=new Tensor(nbi);
			for(int k=0;k<nbi;k++)
			{
				t.setMatrix(k,input.getMatrix(m));
				m++;
			}
			modules.get(i).forward(t);
		}
	
	}

	@Override
	public Tensor getOutput() {
		/*if(sharedForward){
			//System.out.println(this+" retourne "+this.origin_module.getOutput()); 
			return this.origin_module.getOutput();
		}*/
		//System.out.println(this+" : getOutput ");
		int s=0;
		for(int i=0;i<(int)(int)modules.size();i++)
		{
			s+=modules.get(i).getOutput().getNumberOfMatrices();
		}

		Tensor t=new Tensor(s);
		int pos=0;
		for(int i=0;i<(int)(int)modules.size();i++)
		{
			//System.out.println(modules.get(i).getOutput());
			for(int k=0;k<modules.get(i).getOutput().getNumberOfMatrices();k++)
			{
				
				t.setMatrix(pos++,modules.get(i).getOutput().getMatrix(k));
			}
		}
		//System.out.println(this+" retourne "+t); 
		return(t);
	}
	
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
	public void updateParameters(double line){
		if(sharedForward){
			throw new RuntimeException("Please not call updateParameters on a shared forward module");
		}
		for(int i=0;i<(int)modules.size();i++)
		{
			if(!modules.get(i).sharedForward){
				modules.get(i).updateParameters(line);
			}
		}
	}
	
	@Override
	public void backward_updateGradient(Tensor input, Tensor deltas_output) {
		
		if(locked==true){
			//System.out.println("Locked!!");
			return;
		}
		
		int nbM=this.getNbInputMatrix();
		int nbMats=0;
		if(input!=null){
			nbMats=input.getNumberOfMatrices();
		}
		if(nbM!=nbMats){
			throw new RuntimeException( "Format problem in  "+this+" requiredInputMats="+nbM+", givenInputMats"+nbMats );
		}
		int m=0;
		int posout=0;
		for(int i=0;i<(int)modules.size();i++)
		{
			Module mod=modules.get(i);
			int nbi=mod.getNbInputMatrix();
			Tensor t=new Tensor(nbi);
			if(input!=null){
				for(int k=0;k<nbi;k++)
				{
					t.setMatrix(k,input.getMatrix(m));
					m++;
				}
			}
			if(deltas_output==null){
				mod.backward_updateGradient(t,null);
			}
			else{
				Tensor tout=new Tensor(mod.getOutput().getNumberOfMatrices());
				for(int k=0;k<mod.getOutput().getNumberOfMatrices();k++)
				{
					tout.setMatrix(k,deltas_output.getMatrix(posout++));
				}
				mod.backward_updateGradient(t,tout);
			}
		}

	}

	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltas_output) {
		throw new RuntimeException( "Don't use backward_computeDeltaInputs with this module please (TableModule)...." );
	}
	
	public void backward(Tensor input,Tensor deltas_output)
	{
		
		if(locked==true){
			//System.out.println(this+" Locked!!");
			return;
		}
		/*else{
			System.out.println(this+" Not Locked!!");
			
		}*/
		
		//System.out.println("Here!!");
		int nbM=this.getNbInputMatrix();
		int nbMats=0;
		if(input!=null){
			nbMats=input.getNumberOfMatrices();
		}
		if(nbM!=nbMats){
			throw new RuntimeException( "Format problem in TableModule" );
		}
		int m=0;
		int posout=0;
		for(int i=0;i<(int)modules.size();i++)
		{
			Module mod=modules.get(i);
			int nbi=mod.getNbInputMatrix();
			Tensor t=new Tensor(nbi);
			if(input!=null){
				for(int k=0;k<nbi;k++)
				{
					t.setMatrix(k,input.getMatrix(m));
					m++;
				}
			}
			if(deltas_output==null){
				mod.backward(t,null);
			}
			else{
				Tensor tout=new Tensor(mod.getOutput().getNumberOfMatrices());
				for(int k=0;k<mod.getOutput().getNumberOfMatrices();k++)
				{
					tout.setMatrix(k,deltas_output.getMatrix(posout++));
				}
				mod.backward(t,tout);
			}
		}
		
	}
	

	@Override
	public Tensor getDelta() {
		int s=0;
		for(int i=0;i<(int)(int)modules.size();i++)
		{
			s+=modules.get(i).getDelta().getNumberOfMatrices();
		}

		Tensor t=new Tensor(s);
		int pos=0;
		for(int i=0;i<(int)(int)modules.size();i++)
		{			
			Tensor delta=modules.get(i).getDelta();
			
			for(int k=0;k<delta.getNumberOfMatrices();k++)
			{
				t.setMatrix(pos++,delta.getMatrix(k));
			}
		}
		return(t);	
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
	
	
	
	
	
}
