package mlp;

import java.io.IOException;
import java.util.ArrayList;

import utils.Keyboard;

public class CPURecurrent extends Module {

	protected Tensor tensor_output;
	protected Tensor tensor_delta;
	protected int size; // number of cols of matrix to sum
	protected int nb_inputMatrix;
	//protected ArrayList<Module> activations;
	//protected ArrayList<Module> leftMods;
	//protected ArrayList<Module> rightMods;
	boolean hasActivation=false;
	protected ArrayList<SequentialModule> sums;
	
	
	
	public CPURecurrent(int size,int nb_modules){
		
		/*if(nb_modules>1000){
			throw new RuntimeException(""+nb_modules);
		}*/
		this.size=size;
		this.nb_inputMatrix=nb_modules;
		buildSums();
		//setLeft(new CPULinear(size,size));
		//setRight(new CPULinear(size,size));
		//setActivation(new CPUTimesVals(size,1.0));
		setWeights((CPUMatrix)null);
		tensor_output=new Tensor(1);
		tensor_delta=new Tensor(nb_modules);
	}
	
	
	
	public void setWeights(ArrayList<Double> weights){
		setWeights(((weights!=null)?new CPUMatrix(weights):(CPUMatrix)null));
	}
	
	public void setWeights(CPUMatrix weights){
		if(sharedForward){
			throw new RuntimeException("Please not call setWeights on a shared forward module");
		}
		if(weights==null){
			weights=new CPUMatrix(1,nb_inputMatrix-1); 
			for(int i=0;i<(nb_inputMatrix-1);i++){
				weights.setValue(0,i,1.0f);
			}
		}
		else{
			if(weights.number_of_columns!=(nb_inputMatrix-1)){
				throw new RuntimeException(this+" : input weights matrix has not the right number of columns");
			}
		}
		
		int nbc=weights.number_of_columns;
		int nbr=weights.number_of_rows;
		
		
		for(int i=0;i<(nb_inputMatrix-1);i++){
			CPUMatrix mat=new CPUMatrix(nbr,2);
			for(int j=0;j<nbr;j++){
				double w=weights.getValue(j, i);
				mat.setValue(j, 0, 1.0/(1.0+w));
				mat.setValue(j, 1, 1.0/(1.0+w));
			}
			((CPUSum)sums.get(i).getModule(1)).setWeights(mat);
		}
	}
	
	private int getWeightsNbRows(){
		return ((CPUSum)sums.get(0).getModule(1)).weights.number_of_rows;
	}
	
	private void buildSums(){
		if(sharedForward){
			throw new RuntimeException("Please not call buildSums on a shared forward module");
		}
		sums=new ArrayList<SequentialModule>();
		for(int i=0;i<(nb_inputMatrix-1);i++){
			SequentialModule s=new SequentialModule();
			CPUSum sum=new CPUSum(size,2);
			TableModule table=new TableModule();
			table.addModule(null);
			table.addModule(null);
			s.addModule(table);
			s.addModule(sum);
			if(hasActivation){
				s.addModule(null);
			}
			sums.add(s);
		}
	}
	
	public void setActivation(Module activation){
		if(sharedForward){
			throw new RuntimeException("Please not call setActivation on a shared forward module");
		}
		
		//activations=new ArrayList<Module>();
		for(int i=0;i<(nb_inputMatrix-1);i++){
			Module s=activation.parametersSharedModule();
			if(hasActivation){
				sums.get(i).getModule(2).destroy();
				sums.get(i).setModule(2, s);
			}
			else{
				sums.get(i).addModule(s);
			}
			for(Module mod:getListeners()){
				CPURecurrent m=((CPURecurrent) mod);
				Module s2=s.forwardSharedModule();
				if(hasActivation){
					m.sums.get(i).getModule(2).destroy();
					m.sums.get(i).setModule(2, s2);
				}
				else{
					m.sums.get(i).addModule(s2);
				}
			}
		}
		if(!hasActivation){
			hasActivation=true;
			for(Module mod:getListeners()){
				CPURecurrent m=((CPURecurrent) mod);
				m.hasActivation=true;
			}
		}
			
	}
	
	public void setLeft(Module left){
		if(sharedForward){
			throw new RuntimeException("Please not call setLeft on a shared forward module");
		}
		
		for(int i=0;i<(nb_inputMatrix-1);i++){
			Module nl=left.parametersSharedModule();
			nl.setName("left"+i);
			((TableModule)sums.get(i).getModule(0)).setModule(0,nl);
			for(Module mod:getListeners()){
				CPURecurrent m=((CPURecurrent) mod);
				Module nl2=nl.forwardSharedModule();
				((TableModule)m.sums.get(i).getModule(0)).setModule(0,nl);
			}
		}
	}
	
	public void setRight(Module right){
		if(sharedForward){
			throw new RuntimeException("Please not call setRight on a shared forward module");
		}
		for(int i=0;i<(nb_inputMatrix-1);i++){
			Module nl=right.parametersSharedModule();
			nl.setName("right"+i);
			
			((TableModule)sums.get(i).getModule(0)).setModule(1,nl);
			for(Module mod:getListeners()){
				CPURecurrent m=((CPURecurrent) mod);
				Module nl2=nl.forwardSharedModule();
				((TableModule)m.sums.get(i).getModule(0)).setModule(1,nl);
			}
		}
	}
	
	
	
	private CPURecurrent(CPURecurrent org){
		this.origin_module=org;
		this.size=org.size;
		this.nb_inputMatrix=org.nb_inputMatrix;
	}
	
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		CPURecurrent ret=new CPURecurrent(this);
		ret.sharedForward=true;
		addListener(ret);
		
		ret.sums=new ArrayList<SequentialModule>();
		for(Module sum:sums){
			ret.sums.add(((SequentialModule)sum.forwardSharedModule()));
		}
		ret.hasActivation=hasActivation;
		return(ret);
	}
	
	public Module parametersSharedModule()
	{
		if(sharedForward){
			throw new RuntimeException("Please do not copy a shared forward module");
		}
		CPURecurrent ret=new CPURecurrent(this);
		
		ret.sums=new ArrayList<SequentialModule>();
		for(Module sum:sums){
			ret.sums.add(((SequentialModule)sum.parametersSharedModule()));
		}
		ret.hasActivation=hasActivation;
		return(ret);
	}

	
	
	public String toString(){
		return "CPURecurrent_nbModules="+nb_inputMatrix+" name="+name+((sharedForward)?"_sharedForward":"");
	}
	
	void allocate(int minibatch_size)
	{
		
		
		
		if (tensor_delta.getMatrix(0)!=null)
		{
			if (tensor_delta.getMatrix(0).getNumberOfRows()==minibatch_size) return;
			for(int i=0;i<nb_inputMatrix;i++)
				tensor_delta.getMatrix(i).transformTo(minibatch_size, size);
			tensor_output.getMatrix(0).transformTo(minibatch_size, size);
		}
		else{
			for(int i=0;i<nb_inputMatrix;i++)
				tensor_delta.setMatrix(i,new CPUMatrix(minibatch_size,size));
			tensor_output.setMatrix(0,new CPUMatrix(minibatch_size,size));
		}
	}
	
	@Override
	public void forward(Tensor input) {
		int minibatch_size=input.getMatrix(0).getNumberOfRows();
		allocate(minibatch_size);
		
		if(sharedForward){
			tensor_output.setMatrix(0, this.origin_module.getOutput().getMatrix(0));
			return;
		}
		input.ensureCPUMatrices();
		tensor_output.ensureCPUMatrices();
		
		if(input.getNumberOfMatrices()!=nb_inputMatrix){
			throw new RuntimeException(this+" Format problem of input => given "+input.getNumberOfMatrices()+" matrices, required "+nb_inputMatrix);
		}
		
		if(minibatch_size%getWeightsNbRows()!=0){
			throw new RuntimeException(this+" Format pb : weights and input number of rows incompatible");
		}
		
		
		CPUMatrix _output=(CPUMatrix)tensor_output.getMatrix(0);
		
		double[] out=_output.getValues();
		CPUMatrix _input1=(CPUMatrix)input.getMatrix(0);
		int nbc=_input1.getNumberOfColumns();
		if(nbc!=size){
			throw new RuntimeException(this+" : Matrix "+0+" has "+nbc+" columns , required = "+size);
		}
		CPUMatrix _input2=null;
		Tensor tin=new Tensor(2);
		for(int i=1;i<nb_inputMatrix;i++){
			_input2=(CPUMatrix)input.getMatrix(i);
			int nbc2=_input2.getNumberOfColumns();
			if(nbc2!=size){
				throw new RuntimeException(this+" : Matrix "+i+" has "+nbc+" columns , required = "+size);
			}
			
			tin.setMatrix(0, _input1);
			tin.setMatrix(1, _input2);
			sums.get(i-1).forward(tin);
			_input1=(CPUMatrix)sums.get(i-1).getOutput().getMatrix(0);
			//System.out.println(_input1);
		}
		tensor_output.setMatrix(0,sums.get(nb_inputMatrix-2).getOutput().getMatrix(0)); 
		
	}

	@Override
	public int getNbInputMatrix(){
		return nb_inputMatrix;
	}
	
	@Override
	public Tensor getOutput() {
		return tensor_output;
		//return(activations.get(nb_modules-1).getOutput());
	}

	
	@Override
	public void updateParameters(double line){
		if(sharedForward){
			throw new RuntimeException("Please not call updateParameters on a shared forward module");
		}
		Module mod=sums.get(0);
		if(!mod.sharedForward){
				mod.updateParameters(line);
		}
		/*for(Module sum:sums){
			sum.updateParameters(line);
		}*/
		
	}

	@Override
	public Parameters getParamList(){
		if(sharedForward){
			return this.origin_module.getParamList();
		}
		return sums.get(0).getParamList();
	}
	
	@Override
	public void backward_updateGradient(Tensor input, Tensor deltas_output) {
		//System.out.println("input "+input);
		
		
		
		assert(sums.size()>0);
		if (sums.size()==1)
		{
			sums.get(0).backward(input,deltas_output);
		}
		else
		{
			Tensor in=new Tensor(2);
			for(int i=0;i<(int)sums.size();i++)
			{
				int index=sums.size()-1-i;
				
				Matrix mat=(index == 0)? input.getMatrix(0) : sums.get(index-1).getOutput().getMatrix(0);
				in.setMatrix(0, mat);
				in.setMatrix(1,input.getMatrix(index+1));
				//System.out.println(in);
				Tensor d=(index==sums.size()-1) ? deltas_output : ((TableModule)sums.get(index+1).getModule(0)).getModule(0).getDelta();
				sums.get(index).backward(in,d);
				
			}
		}
		
	}
	
	@Override
	public void paramsChanged(){
		if(sharedForward){
			throw new RuntimeException("Please not call paramsChanged on a shared forward module");
		}
		for(Module m:sums){
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
		throw new RuntimeException("Don't use backward_computeDeltaInputs with this module please (RecurrentModule)....");
	}

	@Override
	public void backward(Tensor input,Tensor deltas_output)
	{
		
		backward_updateGradient(input,deltas_output);
		this.tensor_delta.setMatrix(0, ((TableModule)sums.get(0).getModule(0)).getModule(0).getDelta().getMatrix(0));
		//System.out.println("0 left:"+((TableModule)sums.get(0).getModule(0)).getModule(0).getDelta().getMatrix(0));
		
		for(int i=1;i<(this.nb_inputMatrix);i++){
			
			Matrix mat=((TableModule)sums.get(i-1).getModule(0)).getModule(1).getDelta().getMatrix(0);
			this.tensor_delta.setMatrix(i,mat); 
			
		}
		//System.out.println("0 right:"+((TableModule)sums.get(0).getModule(0)).getModule(1).getDelta().getMatrix(0));
		//System.out.println("1 left :"+((TableModule)sums.get(1).getModule(0)).getModule(0).getDelta().getMatrix(0));
		//System.out.println("1 right:"+((TableModule)sums.get(1).getModule(0)).getModule(1).getDelta().getMatrix(0));
		
		//this.tensor_delta.setMatrix(this.nb_inputMatrix-1, ((TableModule)sums.get(sums.size()-1).getModule(0)).getModule(1).getDelta().getMatrix(0));
	}
	
	@Override
	public Tensor getDelta() {
		
		return(this.tensor_delta);	
	}

	
	
	//TODO
	public Module copyModuleToGPU()
	{
		SequentialModule module=new SequentialModule();
		
		
		/*for(int i=0;i<(int)modules.size();i++)
			module.addModule(modules.get(i).copyModuleToGPU());*/
		return(module);
	}

	//TODO
	public Module copyModuleToCPU()
	{
		SequentialModule module=new SequentialModule();
		/*for(int i=0;i<(int)modules.size();i++)
			module.addModule(modules.get(i).copyModuleToCPU());*/
		return(module);
	}
	
	public void destroy(){
		super.destroy();
		
		for(Module mmod:sums){
			mmod.destroy();
		}
		sums.clear();
		
	}
	
	public Module getLeft(){
		return ((TableModule)sums.get(0).getModule(0)).getModule(0);
	}
	public Module getRight(){
		return ((TableModule)sums.get(0).getModule(0)).getModule(1);
	}
	public static void main(String args[]){
		CPURecurrentTest test=new CPURecurrentTest();
		test.learn();
	}
}

class CPURecurrentTest extends MLPModel{
	
	public void learn(){
		CPUHingeLoss sq=new CPUHingeLoss();
		Tensor tlabels=new Tensor(1);
		CPUMatrix labels=new CPUMatrix(2,1);
 		labels.setValue(0, 0, 1);
 		labels.setValue(1, 0, -1);
 		tlabels.setMatrix(0, labels);
		sq.setLabels(tlabels);
		CPURecurrent rec=new CPURecurrent(2,3);
		CPUParams input1=new CPUParams(2,2);
		CPUParams input2=new CPUParams(2,2);
		CPUParams input3=new CPUParams(2,2);
		TableModule table=new TableModule();
		table.addModule(input1);
		table.addModule(input2);
		table.addModule(input3);
		
		
		CPUParams item1=new CPUParams(1,2);
		CPUParams item2=new CPUParams(1,2);
		CPUParams item3=new CPUParams(1,2);
		params.allocateNewParamsFor(item1, 1, -1, 1);
		params.allocateNewParamsFor(item2, 1, -1, 1);
		params.allocateNewParamsFor(item3, -1, -1, 1);
		// les deux types voient les memes items mais pas dans le meme ordre => on veut classer le premier en tant que 1 et le second en tant que 2
		input1.addParametersFrom(item1);
		input2.addParametersFrom(item2);
		input3.addParametersFrom(item3);
		input1.addParametersFrom(item1);
		input2.addParametersFrom(item3);
		input3.addParametersFrom(item2);
		/*input1.lockParams();
		input2.lockParams();
		input3.lockParams();
		*/
		CPULinear linearLeft=new CPULinear(2,2);
		
		CPULinear linearRight=new CPULinear(2,2);
		//linearLeft.lockParams();
		//linearRight.lockParams();
		
		params.allocateNewParamsFor(linearLeft);
		params.allocateNewParamsFor(linearRight);
		CPUTanh act=new CPUTanh(2);
		rec.setLeft(linearLeft);
		rec.setRight(linearRight);
		rec.setActivation(act);
		
		global.addModule(table);
		global.addModule(rec);
		global.addModule(new CPUAverageCols(2,2));
		global.addModule(sq);
		global.addModule(new CPUAverageRows(1,0));		
		
		LineSearch lsearch=new ConstantLine(0.01);
 		DescentDirection dir=new GradientDirection();
		Optimizer opt=new Descent(dir,lsearch);
 		opt.optimize(this);
	}
	
	public void forward(){
		global.forward(new Tensor(0));
	}
	public void backward(){
		global.backward_updateGradient(new Tensor(0));
		//System.out.println(params);
		//System.out.println(params);
		Keyboard.saisirLigne("tapez touche");
	}
	
	public void load() throws IOException{
		
	}
	public void save() throws IOException{
		
	}
	
	
}


