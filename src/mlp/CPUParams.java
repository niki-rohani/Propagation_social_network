package mlp;

import java.util.ArrayList;

public class CPUParams extends Module {

	//protected CPUMatrix parameters;
	protected Parameters paramList;
	//private ArrayList<Parameters> lists;
	protected Tensor tensor_output;
	protected int nbVecs;
	protected int size=0;
	protected int cursor=0;
	protected ArrayList<CPUParams> listeners;
	//protected Tensor tensor_delta;
	
	
	/*public CPUParams(){
		this(1,-1);
	}*/
	public CPUParams(int nbVecs, int size){ //Parameter[] pList){
		tensor_output=new Tensor(1);
		tensor_output.setMatrix(0, new CPUMatrix(nbVecs,size));
		//tensor_delta=new Tensor(1);
		this.nbVecs=nbVecs;
		this.size=size;
		this.paramList=new Parameters(nbVecs*size);
		//if(size>=0){
		//this.paramList=new Parameters(); //nbVecs*size);
		//lists=new ArrayList<Parameters>();	
		//}
		/*for(Parameter p:pList){
			p.parent.addListener(this);
		}*/
		//majParams();
		paramsChanged=true;
		listeners=new ArrayList<CPUParams>();
	}
	public CPUParams(int nbVecs, int size, double val){ //Parameter[] pList){
		tensor_output=new Tensor(1);
		tensor_output.setMatrix(0, new CPUMatrix(nbVecs,size));
		//tensor_delta=new Tensor(1);
		this.nbVecs=nbVecs;
		this.size=size;
		//if(size>=0){
		this.paramList=new Parameters(nbVecs*size,val);
		//ists=new ArrayList<Parameters>();
		//}
		/*for(Parameter p:pList){
			p.parent.addListener(this);
		}*/
		paramsChanged=true;
		listeners=new ArrayList<CPUParams>();
	}
	public CPUParams(int nbVecs, int size, double val,double lowerBound,double upperBound){ //Parameter[] pList){
		tensor_output=new Tensor(1);
		tensor_output.setMatrix(0, new CPUMatrix(nbVecs,size));
		//tensor_delta=new Tensor(1);
		this.nbVecs=nbVecs;
		this.size=size;
		//if(size>=0){
		this.paramList=new Parameters(nbVecs*size,val,lowerBound,upperBound);
		//lists=new ArrayList<Parameters>();
		//}
		/*for(Parameter p:pList){
			p.parent.addListener(this);
		}*/
		paramsChanged=true;
		listeners=new ArrayList<CPUParams>();
	}
	
	public CPUParams(CPUParams org){
		this(org.nbVecs,org.size);
		if(org.sharedForward){
			throw new RuntimeException(this+": Please do not copy a shared forward module");
		}
		this.origin_module=org;
		//parameters=org.parameters;
		
		paramList=org.paramList;
		//lists=org.lists;
		paramsChanged=true;
		/*for(int i=0;i<paramList.length;i++){
			paramList[i].parent.addListener(this);
		}*/
	}
	
	public void setNbVecs(int nbVecs){
		if(sharedForward){
			throw new RuntimeException(this+": Please do not change the number of vectors of  a shared forward module");
		}
		if(nbVecs!=this.nbVecs){
			this.nbVecs=nbVecs;
			//if(size>=0){
			this.paramList=new Parameters(nbVecs*size);
			//lists=new ArrayList<Parameters>();
			tensor_output.setMatrix(0, new CPUMatrix(nbVecs,size));
			//}
			//parameters=null;
		}
		cursor=0;
		paramsChanged=true;
		for(CPUParams l:listeners){
				l.nbVecs=nbVecs;
				l.paramList=this.paramList; //=new Parameters(nbVecs*size);
				l.tensor_output.setMatrix(0, this.tensor_output.getMatrix(0));
		}
	}
	
	public void destroy(){
		if(sharedForward){
			((CPUParams)this.origin_module).listeners.remove(this);
		}
		
		/*for(CPUParams mod:listeners){
			mod.destroy();
		}*/
		listeners.clear();
	}
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		CPUParams ret=new CPUParams(this);
		ret.sharedForward=true;
		listeners.add(ret);
		return(ret);
		
	}
	
	
	public Module parametersSharedModule()
	{
		if(sharedForward){
			throw new RuntimeException("Please not call parametersSharedModule on a shared forward module");
		}
		return(new CPUParams(this));
	}

	/*private void createParamList(){
		//System.out.println(nbVecs+"*"+size);
		//System.out.println(lists.size());
		paramList=new Parameters();
		//ArrayList<Parameters> vals=ArrayList<Parameters>(); //new double[nbVecs*size];
		for(int i=0;i<(nbVecs*size);i++){
			int row=i%nbVecs;
			int col=(int)(i/nbVecs);
			paramList.addParam(lists.get(row).get(col));
		}
		paramsChanged=true;
	}*/
	
	private void majParams(){
		if(sharedForward){
			throw new RuntimeException("Please not call majParams on a shared forward module");
		}
		
		/*if(size<0){
			if(paramList.length%nbVecs==0){
				size=paramList.length/nbVecs;
			}
			else{
				throw new RuntimeException(this.getClass()+" => Pb : Nombre de params incorrect ");
			}
		}*/
		
		/*if((this.paramList==null) && (size>=0)){
			this.paramList=new Parameters(nbVecs*size);
		}*/
		//System.out.println(nbVecs+"*"+size+"="+nb);
		ArrayList<Parameter> pars=paramList.getParams();
		int nb=pars.size();
		
		CPUMatrix parameters=(CPUMatrix)tensor_output.getMatrix(0);//new CPUMatrix(nbVecs,size);
		double[] vals=parameters.getValues();
		for(int i=0;i<nb;i++){
			//System.out.println(i);
			//System.out.println("vals_i "+vals[i]);
			//System.out.println(paramList.get(i));
			vals[i]=pars.get(i).getVal();
		}
		paramsChanged=false;
	}
	
	public void reinitCursor(){
		cursor=0;
	}
	
	
	/*public void paramsChanged(){
		paramsChanged=true;
	}*/
	
	
	/*public Tensor getParameters()
	{
		
		return(new Tensor(parameters));
	}*/
	
	
	public int getNbParams(){
		/*if((this.paramList==null) && (size>=0)){
			this.paramList=new Parameters(nbVecs*size);
		}*/
		return nbVecs*size;
	}
	
	public CPUParams extractVec(int i){
		CPUParams ret=new CPUParams(1,size);
		Parameters l=new Parameters(size);
		//ArrayList<Parameter> list=new ArrayList<Parameter>();
		
		for(int j=0;j<size;j++){
			l.set(j,this.paramList.get(Matrix.IDX2C(i, j, nbVecs)));
		}
		//l.s
		ret.setParameters(l);
		return(ret);
	}
	
	@Override
	public void updateParameters(double line){
		if(sharedForward){
			throw new RuntimeException("Please not call updateParameters on a shared forward module");
		}
					paramList.update(line);
					paramsChanged();
	}
	
	public void setParametersFrom(int i,CPUParams mod)
	{
		if(sharedForward){
			throw new RuntimeException("Please not call setParametersFrom on a shared forward module");
		}
		if(mod.nbVecs!=1){
			throw new RuntimeException("Can only add parameters from a vector of params");
		}
		/*if(mod.locked){
			locked=true;
		}*/	
		Parameters l=mod.paramList;
		if(l.size()!=size){
			throw new RuntimeException("This set of parameters has not the good number of params");
		}
		
		//if((this.paramList==null) && (size>=0)){
		//	this.paramList=new Parameters(nbVecs*size);
		//}
		
		for(int j=0;j<size;j++){
			int idx=Matrix.IDX2C(i, j, nbVecs);
			paramList.set(idx,l.get(j));
			//l[j].parent.addListener(this);
		}
		//lists.set(i, l);
		paramsChanged=true;
	}
	
	public void addParametersFrom(CPUParams mod)
	{
		if(sharedForward){
			throw new RuntimeException("Please not call addParametersFrom on a shared forward module");
		}
		//if(mod.locked){
		//	locked=true;
		//}
		
		//paramList.addSubParamList(l);
		//paramsChanged=true;
		//lists.add(null);
		setParametersFrom(cursor,mod);
		cursor++;
		/*if(cursor==nbVecs){
			this.createParamList();
		}*/
	}
	
	
	
	
	/*public void destroy()
	{
		paramList=null;
		parameters=null;
		this.tensor_output=null;
	}*/
	
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException("Please not call setParameters on a shared forward module");
		}
		/*if(nbVecs!=1){
			throw new RuntimeException("This operation is not allowed if the set of parameters is not a vector");
		}*/
		paramList=pList;
		
		//size=pList.size();
		
		/*for(Parameter p:pList){
			p.parent.addListener(this);
		}*/
		//majParams();
		paramsChanged=true;
		
	}
	
	public int getNbInputMatrix(){
		
		return 0;
	}
	
	/*public ArrayList<CPUParams> split(ArrayList<Integer> indexSplits){
		ArrayList<CPUParams> list=new ArrayList<CPUParams>();
		if((indexSplits.size()==0)  || (indexSplits.size()>size)){
			throw new RuntimeException("CPUParams Split problem : ")
		}
	}*/
	
	
	
	@Override
	public void forward(Tensor input) {
		if(paramList==null){
			throw new RuntimeException(this+": Param List empty !!");
		}
		//if(name.compareTo("simPParam")==0){
		//	System.out.println(this+" forward "+size);
		//}
		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			//parameters=((CPUParams)this.origin_module).parameters;
			return;
		}
		if(paramsChanged){
			majParams();
		}
		//tensor_output.setMatrix(0, parameters);
	}

	@Override
	public Tensor getOutput() {
		
		if(sharedForward){
			return this.origin_module.getOutput();
		}
		return tensor_output;
	}

	public Parameters getParamList(){
		if(sharedForward){
			return this.origin_module.getParamList();
		}
		/*if((this.paramList==null) && (size>=0)){
			this.paramList=new Parameters(nbVecs*size);
		}*/
		return paramList;
	}
	
	@Override
	public void backward_updateGradient(Tensor input, Tensor deltas_output) {
		if(locked==true){
			//System.out.println(this+" locked!");
			return;
		}
		if(sharedForward){
			if(this.origin_module.locked){
				return;
			}
		}
		/*if(size<0){
			majParams();
		}*/
		double[] d_out=null;
		if (deltas_output!=null){
			deltas_output.ensureCPUMatrices();
			CPUMatrix _output=(CPUMatrix)(deltas_output.getMatrix(0));
			d_out=_output.getValues();
		}
		ArrayList<Parameter> pars=paramList.getParams();
		int nb=pars.size();
		//Updating gradient
		for(int i=0;i<nb;i++)
		{
			double o=1.0f;
			if(d_out!=null){o=d_out[i];}
			//System.out.println("pars "+i+" "+pars.get(i)+" "+o);
			double g=pars.get(i).gradient+o;
			if(Double.isNaN(g)){
				throw new RuntimeException(this+".updateGradient: NaN gradient computed => "+pars.get(i).gradient+" + "+o);		
			}
			pars.get(i).gradient=g;
			//System.out.println("CPUParam param"+i+" add to gradient "+o);
		}

	}

	public Tensor getParameters()
	{
		if(sharedForward){
			return this.origin_module.getParameters();
		}
		if(paramsChanged){
			majParams();
		}
		return(getOutput());
	}
	
	@Override
	public void backward_computeDeltaInputs(Tensor input, Tensor deltas_output) {
		//throw new RuntimeException("No deltas here (CPUParams)");
		return;
	}

	@Override
	public Tensor getDelta() {
		//throw new RuntimeException(this+" getDelats => No deltas here (CPUParams)");
		return new Tensor(0);
	}

}
