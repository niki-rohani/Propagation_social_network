package simon.mlp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import mlp.CPUAverageRows;
import mlp.CPUL2Norm;
import mlp.CPULinear;
import mlp.CPUMatrix;
import mlp.CPUParams;
import mlp.CPUSquareLoss;
import mlp.CPUSum;
import mlp.CPUTanh;
import mlp.Criterion;
import mlp.DescentDirection;
import mlp.LineSearch;
import mlp.MLPModel;
import mlp.Matrix;
import mlp.Module;
import mlp.Optimizer;
import mlp.Parameters;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.Tensor;
import mlp.TensorModule;

public class MLPSimpleAutoEncode extends MLPModel {
	
	protected Module encoder;
	protected Module decoder ;
	
	protected Criterion cri;
	protected Tensor input;
	protected Module reg;
	protected double lambda;
	protected int batch_size; // if =-1 => all samples are used in batch mode, else if = x, only x training examples are randomly sampled from the set and used at each step  
	
	protected Tensor currentInput;
	protected Tensor currentLabels;
	
	public MLPSimpleAutoEncode(Parameters params) {
		super(params);
		// TODO Auto-generated constructor stub
	}

	public MLPSimpleAutoEncode(String model_file) {
		super(model_file);
		throw new RuntimeException("Not implemented yet") ;
		// TODO Auto-generated constructor stub
	}

	public MLPSimpleAutoEncode(Parameters params, String model_file) {
		super(params, model_file);
		throw new RuntimeException("Not implemented yet") ;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void load() throws IOException {
		throw new RuntimeException("Not implemented yet") ;
		// TODO Auto-generated method stub

	}

	@Override
	public void save() throws IOException {
		throw new RuntimeException("Not implemented yet") ;
		// TODO Auto-generated method stub

	}
	

	public MLPSimpleAutoEncode(Parameters params, Module encoder, Module decoder, Criterion cri, Tensor input, Module reg, double lambda, int batch_size) {
		super(params);
		this.encoder = encoder;
		this.decoder = decoder;
		this.cri = cri;
		cri.setLabels(input);
		this.input = input;
		this.reg = reg;
		this.lambda = lambda;
		this.batch_size = batch_size;
		
		SequentialModule seq=new SequentialModule();
		seq.addModule(encoder);
		seq.addModule(decoder);
		seq.addModule(cri);
		CPUAverageRows av=new CPUAverageRows(1);
		seq.addModule(av);
		//CPUAverageCols avc = new CPUAverageCols(4);
		//seq.addModule(avc);
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
		}else{
			global=seq;
		}
		
	}
	
	public MLPSimpleAutoEncode parametersSharedLoss()
	{
		
		MLPSimpleAutoEncode f;
		if(reg!=null){
			f=new MLPSimpleAutoEncode(params,encoder.parametersSharedModule(),decoder.parametersSharedModule(),cri.copy(),input,reg.parametersSharedModule(),lambda,batch_size);
		}
		else{
			f=new MLPSimpleAutoEncode(params,encoder.parametersSharedModule(),decoder.parametersSharedModule(),cri.copy(),input,null,lambda,batch_size);
		}
		return(f);
	}

	private void sampleInputs(){
		if(this.batch_size<0){
			this.currentInput=this.input;
			this.currentLabels=this.input;
			return;
		}
		try{
			Matrix mat=input.getMatrix(0);
			Matrix labs=input.getMatrix(0);
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
	
	@Override
	public void forward() {
		sampleInputs();
		cri.setLabels(this.currentLabels);
		global.forward(this.currentInput);
		System.out.println("After forward : ");

	}

	@Override
	public void backward() {
		global.backward_updateGradient(this.currentInput);

	}
	
	public Tensor encode(Tensor input) {
		
		global.forward(input) ;
		return encoder.getOutput() ;
		
	}
	
	private Tensor decode(Tensor tin) {
		decoder.forward(tin) ;
		return decoder.getOutput() ;
	}
	
	public void updateParams(double line){
		params.update(line);
		global.paramsChanged();
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		CPUMatrix input=new CPUMatrix(10,4);
 		input.setValue(0, 0, 1.0f); input.setValue(0, 1, 1.0f); input.setValue(0, 2, 0.0f); input.setValue(0, 3, 0.0f);
 		input.setValue(1, 0, 0.9f); input.setValue(1, 1, 0.8f); input.setValue(1, 2, 0.1f); input.setValue(1, 3, 0.1f);
 		input.setValue(2, 0, 1.0f); input.setValue(2, 1, 0.9f); input.setValue(2, 2, 0.0f); input.setValue(2, 3, 0.2f);
 		input.setValue(3, 0, 1.0f); input.setValue(3, 1, 1.0f); input.setValue(3, 2, 0.2f); input.setValue(3, 3, 1.1f);
 		input.setValue(4, 0, 1.0f); input.setValue(4, 1, 0.8f); input.setValue(4, 2, 0.1f); input.setValue(4, 3, 0.0f);
 		input.setValue(5, 0, 0.0f); input.setValue(5, 1, 0.0f); input.setValue(5, 2, 1.0f); input.setValue(5, 3, 1.0f);
 		input.setValue(6, 0, 0.1f); input.setValue(6, 1, 0.0f); input.setValue(6, 2, 1.0f); input.setValue(6, 3, 0.9f);
 		input.setValue(7, 0, 0.1f); input.setValue(7, 1, 0.1f); input.setValue(7, 2, 0.9f); input.setValue(7, 3, 1.0f);
 		input.setValue(8, 0, 0.2f); input.setValue(8, 1, 0.0f); input.setValue(8, 2, 0.9f); input.setValue(8, 3, 0.9f);
 		input.setValue(9, 0, 0.0f); input.setValue(9, 1, 0.2f); input.setValue(9, 2, 0.8f); input.setValue(9, 3, 1.0f);
 		Tensor tin=new Tensor(1);
 		tin.setMatrix(0, input);
 		
 		Parameters params=new Parameters();
 		TensorModule mod1=new CPULinear(4,2);
 		params.allocateNewParamsFor(mod1);
 		TensorModule mod2=new CPULinear(2,4);
 		params.allocateNewParamsFor(mod2);
 		CPUTanh tan1=new CPUTanh(2);
 		CPUTanh tan2=new CPUTanh(4);
 		SequentialModule encode=new SequentialModule();
 		encode.addModule(mod1);
 		encode.addModule(tan1);
 		SequentialModule decode=new SequentialModule();
 		decode.addModule(mod2);
 		decode.addModule(tan2);
 		
 		Criterion cri=new CPUSquareLoss(4);
 		
 		SequentialModule seq2=new SequentialModule();
 		CPUParams par=new CPUParams(1,params.size());
 		params.giveAllParamsTo(par); // On recupere tous les param pour leur appliquer une regularisation
 		CPUL2Norm norm=new CPUL2Norm(par.getNbParams()); // On rajoute un truc de regularisation
 		seq2.addModule(par);
 		seq2.addModule(norm);
 		MLPSimpleAutoEncode loss=new MLPSimpleAutoEncode(params,encode,decode,cri,tin,seq2,0.01f,-1);
 		//MLPClassical loss=new MLPClassical(seq,cri,params,tin,tlabs,-1);
 		
 		System.out.println(params);
 		LineSearch lsearch=LineSearch.getConstantLine(0.001);
 		DescentDirection dir=DescentDirection.getGradientDirection();
 		Optimizer opt=Optimizer.getDescent(dir,lsearch);
 		opt.optimize(loss);
 		
 		input=new CPUMatrix(1,4);
 		input.setValue(0, 0, 1.0f); input.setValue(0, 1, 1.0f);input.setValue(0, 2, 0.0f); input.setValue(0, 3, 0.0f);
 		tin=new Tensor(1);
 		tin.setMatrix(0, input);
 		
 		Tensor encoded = loss.encode(tin);
 		System.out.println("Encoded : "+encoded.getMatrix(0).getValue(0,0)+","+encoded.getMatrix(0).getValue(0,1));
 		System.out.println("Gne ?");
 		Tensor decoded = loss.decode(encoded);
 		System.out.println("ReGne ?");
 		System.out.println("Decoded : "+decoded.getMatrix(0).getValue(0,0)+","+decoded.getMatrix(0).getValue(0,1)+","+decoded.getMatrix(0).getValue(0,2)+","+decoded.getMatrix(0).getValue(0,3));
 		System.out.println("ReReGne ?");
		

	}

	

}
