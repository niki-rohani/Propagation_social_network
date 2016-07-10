package mlp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

//import utils.Keyboard;

public class MLPClassical extends MLPModel{
	protected Module fonction;
	protected Criterion cri;
	protected Tensor labels;
	protected Tensor input;
	protected Module reg;
	//protected Parameters params;
	protected double lambda;
	//protected Tensor tensor_output;
	protected int batch_size; // if =-1 => all samples are used in batch mode, else if = x, only x training examples are randomly sampled from the set and used at each step  
	
	protected Tensor currentInput;
	protected Tensor currentLabels;
	//protected Tensor tensor_delta;
	
	public MLPClassical(Module fonc, Criterion cri, Parameters params, Tensor input,Tensor labels, int batch_size){
		this(fonc,cri,0.0f,null,params,input,labels,batch_size);
	}
	public MLPClassical(Module fonc, Criterion cri, double lambda, Module reg, Parameters params, Tensor input,Tensor labels, int batch_size){
		super(params);
		this.fonction=fonc;
		this.cri=cri;
		this.labels=labels;
		cri.setLabels(labels);
		this.input=input;
		this.params=params;
		this.reg=reg;
		this.lambda=lambda;
		this.batch_size=batch_size;
		
		SequentialModule seq=new SequentialModule();
		seq.addModule(fonction);
		seq.addModule(cri);
		CPUAverageRows av=new CPUAverageRows(1);
		seq.addModule(av);
		if(reg!=null){
			TableModule table=new TableModule();
			table.addModule(seq);
			table.addModule(reg);
			SequentialModule seqs=new SequentialModule();
			seqs.addModule(table);
			ArrayList<Double> weights=new ArrayList<Double>();
			weights.add(1.0);
			weights.add(lambda);
			CPUSum sum=new CPUSum(1,2,weights);
			seqs.addModule(sum);
			global=seqs;
		}
		else{
			global=seq;
		}
	}
	public MLPClassical parametersSharedLoss()
	{
		
		MLPClassical f;
		if(reg!=null){
			f=new MLPClassical(fonction.parametersSharedModule(),cri.copy(),params,input,labels,batch_size);
		}
		else{
			f=new MLPClassical(fonction.parametersSharedModule(),cri.copy(),lambda, reg.parametersSharedModule(),params,input,labels,batch_size);
		}
		return(f);
	}
	
	public Module getFonction() {return(fonction);}

	void setFonction(Module m)
	{
		fonction=m;
		
	}
	
	/*public void updateParams(double line){
		params.update(line);
		this.init_gradient();
	}*/
	
	public void setLabels(Tensor labels){
		this.labels=labels;
		
	}
	public void setInput(Tensor input){
		this.input=input;
		
	}
	
	
	
	// on suppose que inputs et labels n'ont qu'une seule matrice et que seule la colonne 0 de la matrice de labels est renseignee
	private void sampleInputs(){
		if(this.batch_size<0){
			this.currentInput=this.input;
			this.currentLabels=this.labels;
			return;
		}
		try{
			Matrix mat=input.getMatrix(0);
			Matrix labs=labels.getMatrix(0);
			int nbr=mat.getNumberOfRows();
			int nbc=mat.getNumberOfColumns();
			Matrix ret=(Matrix)((mat.getClass()).getConstructor(int.class,int.class)).newInstance(batch_size,nbc);
			Matrix rlabs=(Matrix)((mat.getClass()).getConstructor(int.class,int.class)).newInstance(batch_size,1);
			
			int nb=0;
			HashSet<Integer> vus=new HashSet<Integer>();
			Random random = new Random();
			while(nb<batch_size){
				int i = random.nextInt(nbr);
				if(!vus.contains(i)){
					for(int j=0;j<nbc;j++){
						ret.setValue(nb, j, mat.getValue(i, j));
					}
					rlabs.setValue(nb, 0, labs.getValue(i, 0));
				}
				nb++;
			}
			this.currentInput=new Tensor(1);
			this.currentInput.setMatrix(0, ret);
			this.currentLabels=new Tensor(1);
			this.currentLabels.setMatrix(0, rlabs);
			
		}
		catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("Problem in creating a new Instance of Matrix");
		}
		
		
	}
	
	
	public void forward(){
		sampleInputs();
		//Tensor output=labels;
		cri.setLabels(this.currentLabels);
		global.forward(this.currentInput);
		
		//System.out.println(global.getOutput());
	}

	public void updateParams(double line){
		params.update(line);
		global.paramsChanged();
		
	}

	/*public void backward_updateGradient(Tensor input, Tensor output) {
		cri.backward(module.getOutput(), output);
		module.backward_updateGradient(input, cri.getDelta());
	}*/

	/*public void backward_computeDeltaInputs(Tensor input, Tensor deltas_output) {
		throw new RuntimeException("Don't use backward_computeDeltaInputs with this module please (LossFonction)....");
	}*/

	
	public void backward()
	{
		/*Tensor output=labels;
		cri.backward(fonction.getOutput(), output);*/
		global.backward_updateGradient(this.currentInput);
		//Clavier.saisirLigne("");
	}
	
	
	/*public Tensor getDelta() {
		return(fonction.getDelta());
	}*/

	
	/*public void init_gradient() {
		module.init_gradient();
	}*/
	
	public MLPClassical copyModuleToGPU()
	{
		MLPClassical loss=new MLPClassical(fonction.copyModuleToGPU(),cri.copyCriterionToGPU(),params,input,labels,batch_size);
			
		return(loss);
	}

	public MLPClassical copyModuleToCPU()
	{
		MLPClassical loss=new MLPClassical(fonction.copyModuleToCPU(),cri.copyCriterionToCPU(),params,input,labels,batch_size);
		
		return(loss);
	}
	
	
	
	public void load() throws IOException
	{
		//TODO
	}
	public void save() throws IOException
	{
		//TODO
	}

	
	public static void main(String[] args){
 		CPUSparseMatrix input=new CPUSparseMatrix(10,2);
 		input.setValue(0, 0, 1.0f); input.setValue(0, 1, 1.0f);
 		input.setValue(1, 0, 0.5f); input.setValue(1, 1, 1.0f);
 		input.setValue(2, 0, -1.0f); input.setValue(2, 1, 0.2f);
 		input.setValue(3, 0, 3.0f); input.setValue(3, 1, 1.0f);
 		input.setValue(4, 0, 3.0f); input.setValue(4, 1, -21.0f);
 		input.setValue(5, 0, 0.0f); input.setValue(5, 1, 1.0f);
 		input.setValue(6, 0, -1.0f); input.setValue(6, 1, -1.0f);
 		input.setValue(7, 0, -1.0f); input.setValue(7, 1, 2.0f);
 		input.setValue(8, 0, 1.0f); input.setValue(8, 1, 0.0f);
 		input.setValue(9, 0, 0.9f); input.setValue(9, 1, -3.0f);
 		Tensor tin=new Tensor(1);
 		tin.setMatrix(0, input);
 		
 		CPUMatrix labels=new CPUMatrix(10,1);
 		labels.setValue(0, 0, 1);
 		labels.setValue(1, 0, 1);
 		labels.setValue(2, 0, -1);
 		labels.setValue(3, 0, 1);
 		labels.setValue(4, 0, -1);
 		labels.setValue(5, 0, 1);
 		labels.setValue(6, 0, -1);
 		labels.setValue(7, 0, -1);
 		labels.setValue(8, 0, 1);
 		labels.setValue(9, 0, -1);
 		Tensor tlabs=new Tensor(1);
 		tlabs.setMatrix(0, labels);
 		
 		Parameters params=new Parameters();
 		TensorModule mod=new CPUSparseLinear(2,2);
 		params.allocateNewParamsFor(mod);
 		TensorModule mod2=new CPULinear(2,1);
 		params.allocateNewParamsFor(mod2);
 		CPULogistic log=new CPULogistic(2);
 		CPUTanh tan=new CPUTanh(1);
 		CPUTanh tan2=new CPUTanh(2);
 		SequentialModule seq=new SequentialModule();
 		seq.addModule(mod);
 		seq.addModule(tan2);
 		seq.addModule(mod2);
 		seq.addModule(tan);
 		Criterion cri=new CPUSquareLoss();
 		SequentialModule seq2=new SequentialModule();
 		CPUParams par=new CPUParams(1,params.size());
 		params.giveAllParamsTo(par);
 		CPUL2Norm norm=new CPUL2Norm(par.getNbParams());
 		seq2.addModule(par);
 		seq2.addModule(norm);
 		MLPClassical loss=new MLPClassical(seq,cri,0.0001f,seq2,params,tin,tlabs,-1);
 		//MLPClassical loss=new MLPClassical(seq,cri,params,tin,tlabs,-1);
 		
 		System.out.println(params);
 		LineSearch lsearch=new ConstantLine(0.001);
 		DescentDirection dir=new GradientDirection();
 		Optimizer opt=new Descent(dir,lsearch);
 		opt.optimize(loss);
 	}
}
