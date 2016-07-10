package simon.mlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.TreeMap;

import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import cascades.Cascade;
import cascades.NbUsers;
import mlp.CPUAddVals;
import mlp.CPUAverageRows;
import mlp.CPUHingeLoss;
import mlp.CPUL2Norm;
import mlp.CPULinear;
import mlp.CPUL1Norm;
import mlp.CPULogistic;
import mlp.CPUMatrix;
import mlp.CPUParams;
import mlp.CPUSparseLinear;
import mlp.CPUAverageCols;
import mlp.CPUSparseMatrix;
import mlp.CPUSquareLoss;
import mlp.CPUSum;
import mlp.CPUTanh;
import mlp.CPUTimesVals;
import mlp.Criterion;
import mlp.DescentDirection;
import mlp.LineSearch;
import mlp.MLPClassical;
import mlp.Matrix;
import mlp.Module;
import mlp.Optimizer;
import mlp.Parameter;
import mlp.Parameters;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.Tensor;
import mlp.TensorModule;
import core.HashMapStruct;
import core.Post;
import core.Structure;
import core.Text;
import core.User;
import experiments.EvalMeasure;
import experiments.EvalMeasureList;
import experiments.EvalPropagationModel;
import experiments.EvalPropagationModelConfig;
import experiments.Hyp;
import experiments.MAP;
import experiments.Result;
import propagationModels.MLP;
import propagationModels.MLPdiffusion;
import propagationModels.PropagationModel;
import propagationModels.PropagationStruct;
import propagationModels.PropagationStructLoader;
import simon.tools.CascadesComGen;



/** 
 "Naivelinks"
 * @author bourigaults
 *
 */


public class MLPCommunities2 extends MLP {

	
	int nbUsers ;
	private Module outputForInference ;
	private Tensor currentInput=null ;
	private Tensor currentLabels=null ;
	
	private Module encode ;
	private Module decode ;
	private Module inter ;
	
	private double NEGVALUEINPUT = 0; // Valeur pour les mecs non infectés en input.
	private double NEGVALUEOUTPUT = 0 ; // Valeur pour les mecs non infectés en output.
	private int contentSize = 0 ;
	
	
	// Metaparams
	private boolean useAutoencodePrelearn ;
	private boolean lockDecodeParams ; 
	private int nbCom ; // Nombre de communautes
	private int batchsize;
	private int posWeight ;
	private int nbIter ;
	private double percentPrelearn ;
	private boolean useContent ;
	private boolean useDoubleLayer ;
	private boolean useNoLayer;
	
	private Criterion cri ;
	private double lambda;
	
	
	 
	
	//public MLPCommunities(String model_file) {
	//	super(model_file);
	//}
	
	public MLPCommunities2(String model_file,int nbCom,int batchsize) {
		this(model_file,nbCom,batchsize,false,false,1,2000,0.25,false,0,0,false) ;
	}
	
	public MLPCommunities2(String model_file,int nbCom,int batchsize,boolean prelearn, boolean lockDecodeParam,int posweight,int maxIter, double percentPrelearn,boolean useContent, int contentSize, double lambda, boolean useDoubleLayer) {
		super(model_file);
		this.nbCom = nbCom ;
		this.batchsize=batchsize ;
		this.useAutoencodePrelearn=prelearn ;
		this.lockDecodeParams=lockDecodeParam ;
		this.posWeight=posweight;
		this.nbIter=maxIter ;
		this.percentPrelearn=percentPrelearn;
		this.useContent=useContent ;
		this.contentSize=contentSize ;
		this.lambda = lambda ;
		this.useDoubleLayer=useDoubleLayer ;
	}
	
	public HashSet<String> getUsers(){
		if(!loaded){
			try{
				load();
			}
			catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		return new HashSet<String>(users);
	}
	
	public int getContentNbDims(){
		if(!loaded){
			try{
				load();
			}
			catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		return contentSize;
	}
	
	
	public void prepareLearning(String db, String cascadesCollection, long step, int nbInitSteps){

		if(db=="test"){
			test() ;
		}else if(db.startsWith("arti")) {
			arti(db) ;
		}else if(db.startsWith("load")) {
			try {
				this.train_cascades=CascadesComGen.loadCascades(db.substring(4)) ;
				this.cascades_ids=new ArrayList(this.train_cascades.keySet()) ;
				System.err.println("Attention...");
				this.users=new ArrayList<String>() ;
				for(int i =0 ; i<200 ; i++)
					this.users.add(Integer.toString(i)) ;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			//super.prepareLearning(db, cascadesCollection, step, nbInitSteps) ;
			super.prepareLearning(new PropagationStructLoader(db, cascadesCollection, 1, 1, 1)) ;
		}
		
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,cascadesCollection);
		if(!useContent)
			contentSize = 0 ;
		
		//this.nbUsers = nbUsers ;
		
		this.nbUsers = this.users.size() ;
		//nbUsers=this.nbUsers;

		this.params=new Parameters();
		
		System.out.print("Initializing...");
		double[] p = new double[(nbUsers+contentSize)*nbUsers] ;
		double[] count = new double[nbUsers] ;
		double[] countw = new double[contentSize] ;
		// Comptage
		for(int cascadeid : this.cascades_ids)  {
 			PropagationStruct c = this.train_cascades.get(cascadeid) ;
 			for(String source : c.getArrayInit()) {
 				int s=this.users.indexOf(source) ;
 				count[s]+=1.0 ;
 				for(String infect : c.getArrayContamined()){
 					int i = this.users.indexOf(infect) ;
 					p[Matrix.IDX2C(s, i, nbUsers+contentSize)]+=1.0 ;
 				}
 			}
 			if(useContent) {
	 			for(int word : c.getDiffusion().keySet()) {
	 				countw[word]+=1.0 ;
	 				for(String infect : c.getArrayContamined()){
	 					int i = this.users.indexOf(infect) ;
	 					p[Matrix.IDX2C(nbUsers+word, i, nbUsers+contentSize)]+=lambda ;
	 				}
	 			}
 			}
 		}
		// Division finale
		for(int i=0 ; i<nbUsers ; i++) {
			if(count[i]>0) {
				for(int j=0 ; j<nbUsers ; j++) {
					p[Matrix.IDX2C(i, j, nbUsers+contentSize)] /= count[i] ;
					//System.out.println("init "+i+","+j+" : "+p[Matrix.IDX2C(i, j, nbUsers)]);
				}
			}
		}
		if(useContent) {
			for(int i=0 ; i<contentSize ; i++) {
				if(countw[i]>0) {
					for(int j=0 ; j<nbUsers ; j++) {
						p[Matrix.IDX2C(nbUsers+i, j, nbUsers+contentSize)] /= countw[i] ;
						//System.out.println("init "+i+","+j+" : "+p[Matrix.IDX2C(i, j, nbUsers)]);
					}
				}
			}
		}
		
		System.out.print(" Done !");
		
		System.out.println("NB USers ="+ nbUsers);
 		encode=new CPULinear(nbUsers+contentSize,nbUsers);
 		
 		this.params.allocateNewParamsFor(encode,p,0.001,0.999f);
 		//CPULogistic tan1=new CPULogistic(nbUsers);
 		CPUTanh tan1=new CPUTanh(nbUsers,1.0,0.2);
 		CPUTimesVals times1 = new CPUTimesVals(nbUsers, 0.998f) ;
 		CPUAddVals add1 = new CPUAddVals(nbUsers, 0.001f) ;
 		
 		SequentialModule seq = new SequentialModule() ;
 		seq.addModule(encode);
 		/*seq.addModule(tan1) ;
 		seq.addModule(times1) ;
 		seq.addModule(add1) ;*/

 		
 		outputForInference=encode ;
 		
 		cri=new FakeCrossEntroCriterion(nbUsers,posWeight) ;
 		//cri=new CPUHingeLoss(nbUsers) ;
 			
 		CPUAverageRows av=new CPUAverageRows(1) ;
 		seq.addModule(cri);
 		seq.addModule(av) ;
 		
 		
 		
 		/*if(this.lambda > 0) {
 			SequentialModule seq2=new SequentialModule();
 	 		CPUParams par=new CPUParams(1,params.size());
 	 		params.giveAllParamsTo(par);
 	 		CPUL2Norm norm=new CPUL2Norm(par.getNbParams());
 	 		seq2.addModule(par);
 	 		seq2.addModule(norm);
 	 		
 	 		TableModule table=new TableModule();
			table.addModule(seq);
			table.addModule(seq2);
			SequentialModule seqs=new SequentialModule();
			seqs.addModule(table);
			ArrayList<Double> weights=new ArrayList<Double>();
			weights.add(1.0);
			weights.add(this.lambda);
			CPUSum avg = new CPUSum(1,2, weights) ;
			seqs.addModule(avg);
			global=seqs;
 		} else {*/
 			global=seq ;
 		//}
 		
 		
 		
 		
 		/*SequentialModule seq2=new SequentialModule();
 		CPUParams par=new CPUParams();
 		params.giveAllParamsTo(par); // On recupere tous les param pour leur appliquer une regularisation
 		CPUL2Norm norm=new CPUL2Norm(par.getNbParams()); // On rajoute un truc de regularisation
 		seq2.addModule(par);
 		seq2.addModule(norm);*/
 		
 		
 		
	}
	
	private void arti(String db) {
		
		String [] args = db.substring(4).split("_") ;
		int nbusers = Integer.parseInt(args[0]);
		int nbCom =Integer.parseInt(args[1]);
		double probAct = Double.parseDouble(args[2]);
		double probPass= Double.parseDouble(args[3]);
		double noize= Double.parseDouble(args[4]);
		int maxSources= Integer.parseInt(args[5]);
		double probaStop= Double.parseDouble(args[6]);
		int nbTrain= Integer.parseInt(args[7]);
		int nbTest= Integer.parseInt(args[8]);
		
		this.users=new ArrayList<String>();
		for(int i = 0 ; i<nbusers ;i++)
			this.users.add(Integer.toString(i)) ;
		
		CascadesComGen gen = new CascadesComGen(nbusers,nbCom,probAct,probPass,noize,maxSources,probaStop) ;
		this.train_cascades = gen.generate(nbTrain) ;
		cascades_ids=new ArrayList<Integer>(train_cascades.keySet());
		
	}	
	
	public double evaltest(HashMap<Integer,PropagationStruct> cascades_test) throws IOException{
	
		double totalMAP = 0.0 ;
		MAP map = new MAP(new HashSet<String>(users),true) ;
		for(int i : cascades_test.keySet()) {
				System.out.println(cascades_test.get(i).getCascade());
				//ArrayList<String> groudtruth = cascades_test.get(i).getArrayContamined() ;
				//this.infer(cascades_test.get(i)) ;
				//TreeMap<Long, HashMap<String, Double>> predicted = cascades_test.get(i).getInfections() ;
				Hyp hyp=new Hyp(cascades_test.get(i),this,1);
				if(hyp.getStruct()==null) {
					System.out.println("Fuck");
				}
				Result result=map.eval(hyp);
				//result.getScores().get("model_1") ;
				System.out.println("r = "+result);
				totalMAP += result.getScores().get("MAP__ignoreInit") ;
				System.out.println(result.getScores().get("MAP__ignoreInit"));
		}
		System.out.println("MAP : "+totalMAP/cascades_test.size());
		return totalMAP/cascades_test.size() ;
		
	}

	public int inferSimulation(Structure struct){
		throw new RuntimeException("Not implemented");
	}
	
	@Override
	public int infer(Structure struct) {
		if(!useContent)
			this.contentSize=0 ;
		System.out.println("Inference...");
		ArrayList<String> init = ((PropagationStruct)struct).getArrayInit() ;
		Matrix minit = new CPUMatrix(1, this.nbUsers+this.contentSize) ;
		this.useAutoencodePrelearn=false ;
		for(String username : init) {
			//int iduser = User.getUser(username).getID()-1 ;
			int iduser = this.users.indexOf(username) ;
			
			
			// Hum... toujours la meme galere
			if(iduser==-1){
				continue;
			}
			
			
			if(iduser>=this.nbUsers) {
				throw new RuntimeException("CPUMatrix setvalue out of bound : " +iduser +">"+minit.getNumberOfColumns()+"matrix" ) ;
			}
			minit.setValue(0,iduser,1) ;
		}
		for(int ii =0 ; ii< minit.getNumberOfRows() ;ii++) {
			for(int jj = 0 ; jj<minit.getNumberOfColumns(); jj++) {
				if(minit.getValue(ii, jj)!=1) {
					minit.setValue(ii,jj, this.NEGVALUEINPUT) ; // mettre à -1 les non infection (hingeloss)
				}
			}
		}
		
		if(useContent){
			TreeMap<Integer, Double> content = ((PropagationStruct)struct).getDiffusion() ;
			for(int word : content.keySet()) {
				minit.setValue(0, this.nbUsers+word, 1) ;
			}
		}
		Tensor input=new Tensor(minit) ;
		if(cri!=null) {
			// TODO ;
			cri.setLabels(new Tensor(new CPUMatrix(1,this.nbUsers,0))) ;
		}
		global.forward(input) ;
		
		TreeMap<Long, HashMap<String, Double>> inf = new TreeMap<Long, HashMap<String,Double>>() ;
		HashMap<String,Double> hinf = new HashMap<String, Double>() ;
		
		Matrix output = this.outputForInference.getOutput().getMatrix(0) ;
		System.out.println("LOSS : "+global.getOutput().getMatrix(0).getValue(0,0)) ;
		for(int ii=0 ; ii<output.getNumberOfColumns() ; ii++) {
			hinf.put(this.users.get(ii),(double) output.getValue(0, ii)) ;	
		}
		
		inf.put((long) 0, hinf) ;
		((PropagationStruct)struct).setInfections(inf) ;
		return 0;
	}

	@Override
	public void load() throws IOException {
		//throw new RuntimeException("Not implemented") ;
		// TODO Auto-generated method stub
		System.err.println("REECRIRE LA FONCTION LOAD !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!") ;
		File file=new File(model_file);
    	BufferedReader f = new BufferedReader(new FileReader(file)) ;
    	
    	
    	f.readLine() ;
    	this.nbCom=Integer.parseInt(f.readLine().substring(6)) ;
    	this.step=f.readLine().substring(5) ;
    	//his.nbInitSteps=Integer.parseInt(f.readLine().substring(11)) ;
    	this.useContent=Boolean.parseBoolean(f.readLine().substring(11)) ;
    	this.contentSize=Integer.parseInt(f.readLine().substring(12)) ;
    	this.useDoubleLayer=Boolean.parseBoolean(f.readLine().substring(14)) ;
    	
    	f.readLine() ;
    	
    	this.users = new ArrayList<String>() ;
    	for(String s = f.readLine() ; s.compareTo("</USERS>")!=0 ; s=f.readLine()) {
    		this.users.add(s) ;
    	}
    	this.nbUsers=this.users.size() ;
    	
    	//double[] params = new double[this.nbUsers*this.nbCom] ;
    	
    	this.params=new Parameters();
    	encode=new CPULinear(nbUsers+contentSize,nbUsers);
 		this.params.allocateNewParamsFor(encode,0,-1.0f,1.0f);
 		CPULogistic tan1=new CPULogistic(nbUsers);
 		CPUTimesVals times1 = new CPUTimesVals(nbUsers, 0.998f) ;
 		CPUAddVals add1 = new CPUAddVals(nbUsers, 0.001f) ;
 		
 		SequentialModule seq = new SequentialModule() ;
 		global.addModule(encode);
 		global.addModule(tan1) ;
 		global.addModule(times1) ;
 		global.addModule(add1) ;

 		
 		outputForInference=tan1 ;
 		
 		f.readLine() ;
 		int nbpar = 0 ;
 		ArrayList<Parameter> thisparams = this.params.getParams() ;
 		for(String s = f.readLine() ; s.compareTo("</PARAMETERS>")!=0 ; s=f.readLine()) {
    		thisparams.get(nbpar).setVal(Float.parseFloat(s)) ;
    		nbpar++ ;
    	}
 		//if(nbpar != (nbCom*nbUsers+nbCom*nbCom+nbUsers*nbCom)) {
 		int expected = (nbUsers*nbUsers) ;
 		if(nbpar != expected) {
 			throw new IOException("Wrong number of parameters in file"+model_file+". Expected "+expected+", found "+nbpar) ;
 		}
 		

 		
	}

	@Override
	public void save() throws IOException {
		//throw new RuntimeException("Being implemented") ;
		File file=new File(model_file);
    	File dir = file.getParentFile();
    	if(dir!=null){
    		dir.mkdirs();
    	}
    	
    	PrintStream p = new PrintStream(file) ;
    	p.println("<MODEL="+this+">");
    	p.println("nbcom="+this.nbCom);
    	p.println("step="+this.step);
    	//p.println("nbInitStep="+this.nbInitSteps);
    	p.println("useContent="+this.useContent) ;
    	p.println("contentSize="+this.contentSize) ;
    	p.println("useDoubleLayer="+this.useDoubleLayer) ;
    	
    	p.println("<USERS>") ;
    	for(String u : this.users) {
    		p.println(u) ;
    	}
    	p.println("</USERS>") ;
    	
    	p.println("<PARAMETERS>") ;
    	int nbp=0 ;
    	for(Parameter par :this.params.getParams()) {
    		p.println(par.getVal()) ;
    		nbp++ ;
    	}
    	System.out.println("PARAM SAVED : "+nbp );
    	
    	
    	
    	p.println("</PARAMETERS>") ;
    	
		p.println("</MODEL>") ;
	}
	
	//public void learn(String db, String cascadesCollection, long step, int nbInitSteps,Optimizer optim){
	//	learn(db,cascadesCollection,step,nbInitSteps,optim,2000) ;
	////
	//}
	
	//public void learn(String db, String cascadesCollection, long step, int nbInitSteps,Optimizer optim, int iterMax) {
	//	learn(db, cascadesCollection, step, nbInitSteps, optim,  iterMax,iterMax) ;
	//}
	
	public void learn(String db, String cascadesCollection, long step, int nbInitSteps,Optimizer optim){
		prepareLearning(db, cascadesCollection, step, nbInitSteps);
		this.batchsize = batchsize ;
		if(this.useAutoencodePrelearn) {
			if(useDoubleLayer) {
				inter.lockParams();
				//optim.optimize(this,0.00001,(int)(this.nbIter*this.percentPrelearn*0.5),false) ;
				//inter.unlockParams();
				//optim.optimize(this,0.00001,(int)(this.nbIter*this.percentPrelearn*0.5),false) ;
				if(lockDecodeParams){
					//this.decode.lockParams();
				}
			} else {
				//optim.optimize(this,0.00001,(int)(this.nbIter*this.percentPrelearn),false) ;
				if(lockDecodeParams){
					//this.decode.lockParams();
				}
			}
			this.useAutoencodePrelearn=false;
			//optim.optimize(this,0.00001,this.nbIter-(int)(this.nbIter*this.percentPrelearn),false) ; 
			
		} else {
			//optim.optimize(this,0.00001,this.nbIter,false) ;
		}
		
	}

	@Override
	public void forward() {
		if(!useContent)
			contentSize = 0 ;
		Tensor input = new Tensor(1) ;
		Tensor labels = new Tensor(1) ;
		Matrix minit = new CPUMatrix(batchsize!=-1 ? batchsize : this.train_cascades.size(),this.nbUsers+contentSize) ;
		Matrix mout = new CPUMatrix(batchsize!=-1 ? batchsize : this.train_cascades.size(),this.nbUsers) ;
		if(batchsize==-1) {
			if(currentInput==null) {
				int i_input=0 ;
				for(int cascadeid : cascades_ids) {
					PropagationStruct c = this.train_cascades.get(cascadeid) ;
					ArrayList<String> init = c.getArrayInit();
					for(String username : init) {
						//int iduser = User.getUser(username).getID()-1 ;
						int iduser = this.users.indexOf(username) ;
						if(i_input>=minit.getNumberOfRows() | iduser>=minit.getNumberOfColumns()  | i_input<0 | iduser <0 ) {
							throw new RuntimeException("CPUMatrix setvalue out of bound : " +i_input +","+iduser +" on a "+minit.getNumberOfRows()+","+minit.getNumberOfColumns()+" matrix" ) ;
						}
						minit.setValue(i_input, iduser,1) ;
						mout.setValue(i_input, iduser,1) ;
					}
					if(useContent){
						TreeMap<Integer, Double> content = (c).getDiffusion() ;
						for(int word : content.keySet()) {
							minit.setValue(i_input, this.nbUsers+word, 1) ;
						}
					}
					
					ArrayList<String> end = c.getArrayContamined() ;
					for(String username : end) {
						//int iduser = User.getUser(username).getID()-1 ;
						int iduser = this.users.indexOf(username) ;
						
						if(i_input>=mout.getNumberOfRows() | iduser>=mout.getNumberOfColumns() | i_input<0 | iduser <0 ) {
							throw new RuntimeException("CPUMatrix setvalue out of bound : " +i_input +","+iduser +" on a "+mout.getNumberOfRows()+","+mout.getNumberOfColumns()+" matrix" ) ;
						}
						mout.setValue(i_input, iduser,1) ;
					}
					for(int ii =0 ; ii< mout.getNumberOfRows() ;ii++) {
						for(int jj = 0 ; jj<mout.getNumberOfColumns(); jj++) {
							if(mout.getValue(ii, jj)!=1) {
								mout.setValue(ii,jj, this.NEGVALUEOUTPUT) ; // mettre à -1 les non infection (hingeloss)
							}
						}
					}
					i_input++ ;
				}
			}
			input.setMatrix(0, minit) ;
			labels.setMatrix(0, mout) ;
			if(this.useAutoencodePrelearn) {
				input.setMatrix(0, mout.copyToCPUSparse()) ;
			}
			if(currentInput==null){currentInput=input;} ;
			if(currentLabels==null){currentLabels=labels;} ;
			cri.setLabels(currentLabels) ;
			global.forward(currentInput) ;
		} else {
		// Tirer batchsize cascades et les mettre dans une matrice.
			for(int i_input = 0 ; i_input < batchsize ; i_input++) {
				int r = (int)(Math.random() * this.cascades_ids.size()) ;
				int cascadeid = cascades_ids.get(r) ;
				PropagationStruct c = this.train_cascades.get(cascadeid) ;
				
				ArrayList<String> init = c.getArrayInit();
				for(String username : init) {
					//int iduser = User.getUser(username).getID()-1 ;
					int iduser = this.users.indexOf(username) ;
					if(i_input>=minit.getNumberOfRows() | iduser>=minit.getNumberOfColumns()  | i_input<0 | iduser <0 ) {
						throw new RuntimeException("CPUMatrix setvalue out of bound : " +i_input +","+iduser +" on a "+minit.getNumberOfRows()+","+minit.getNumberOfColumns()+" matrix" ) ;
					}
					minit.setValue(i_input, iduser,1) ;
					mout.setValue(i_input, iduser,1) ;
				}
				if(useContent){
					TreeMap<Integer, Double> content = (c).getDiffusion() ;
					for(int word : content.keySet()) {
						minit.setValue(i_input, this.nbUsers+word, 1) ;
					}
				}
				
				ArrayList<String> end = c.getArrayContamined() ;
				for(String username : end) {
					//int iduser = User.getUser(username).getID()-1 ;
					int iduser = this.users.indexOf(username) ;
					if(i_input>=mout.getNumberOfRows() | iduser>=mout.getNumberOfColumns() | i_input<0 | iduser <0 ) {
						throw new RuntimeException("CPUMatrix setvalue out of bound : " +i_input +","+iduser +" on a "+mout.getNumberOfRows()+","+mout.getNumberOfColumns()+" matrix" ) ;
					}
					mout.setValue(i_input, iduser,1) ;
				}
				for(int ii =0 ; ii< mout.getNumberOfRows() ;ii++) {
					for(int jj = 0 ; jj<mout.getNumberOfColumns(); jj++) {
						if(mout.getValue(ii, jj)!=1) {
							mout.setValue(ii,jj, this.NEGVALUEOUTPUT) ; 
						}
						if(minit.getValue(ii, jj)!=1) {
							minit.setValue(ii,jj, this.NEGVALUEINPUT) ; 
						}
					}
				}
				
			}
			
			input.setMatrix(0, minit) ;
			labels.setMatrix(0, mout) ;
			if(this.useAutoencodePrelearn) {
				for(int i = 0 ; i<mout.getNumberOfRows() ; i++) {
					for(int j=0 ; j<mout.getNumberOfColumns() ; j++) {
						input.getMatrix(0).setValue(i, j, mout.getValue(i, j));
					}
				}
				
			}
			//if(currentInput==null){currentInput=input ;}
			//if(currentLabels==null){currentLabels=labels ;}
			currentInput=input ;
			currentLabels=labels ;
			cri.setLabels(currentLabels) ;
			global.forward(currentInput) ;
		}
		
		// TODO Auto-generated method stub

	}

	@Override
	public void backward() {
		global.backward_updateGradient(this.currentInput);

	}
	
	public Parameters getUsedParams(){

		return this.params;
	}
	
	/*public ArrayList<String> getUsers() {
		return this.users ;
	}*/
	
	public void updateParams(double line){
		getUsedParams().update(line);
		global.paramsChanged();
	}
	
	public void test(){
		long step=1l;
		int nbInitSteps=1;
		users=new ArrayList<String>();
		users.add("a"); users.add("b"); users.add("c"); users.add("d"); users.add("e");
		//this.train_users=new HashMap<String,HashMap<Integer,Double>>();
		
		train_cascades=new HashMap<Integer,PropagationStruct>();
		
		TreeMap<Long,HashMap<String,Double>> init=new TreeMap<Long,HashMap<String,Double>>();
		TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
		TreeMap<Integer,Double> diffusion=new TreeMap<Integer,Double>();
		HashMap<String,Double> h=new HashMap<String,Double>();
		h.put("a",1.0);
		init.put(1l, h);
		infections.put(1l, h);
		h=new HashMap<String,Double>();
		h.put("b", 1.0);
		infections.put(2l,h);
		h=new HashMap<String,Double>();
		h.put("c", 1.0);
		infections.put(3l,h);
		diffusion.put(0, 1.0);
		PropagationStruct struct=new PropagationStruct(null,step,nbInitSteps,init,infections,diffusion);
		train_cascades.put(1, struct);
		
		
		init=new TreeMap<Long,HashMap<String,Double>>();
		infections=new TreeMap<Long,HashMap<String,Double>>();
		diffusion=new TreeMap<Integer,Double>();
		h=new HashMap<String,Double>();
		h.put("a",1.0);
		init.put(1l, h);
		infections.put(1l, h);
		h=new HashMap<String,Double>();
		h.put("d", 1.0);
		infections.put(2l,h);
		h=new HashMap<String,Double>();
		h.put("e", 1.0);
		infections.put(3l,h);
		diffusion.put(1, 1.0);
		struct=new PropagationStruct(null,step,nbInitSteps,init,infections,diffusion);
		train_cascades.put(2, struct);
		
		init=new TreeMap<Long,HashMap<String,Double>>();
		infections=new TreeMap<Long,HashMap<String,Double>>();
		diffusion=new TreeMap<Integer,Double>();
		h=new HashMap<String,Double>();
		h.put("b",1.0);
		init.put(1l, h);
		infections.put(1l, h);
		h=new HashMap<String,Double>();
		h.put("d", 1.0);
		infections.put(2l,h);
		h=new HashMap<String,Double>();
		h.put("e", 1.0);
		infections.put(3l,h);
		diffusion.put(1, 1.0);
		struct=new PropagationStruct(null,step,nbInitSteps,init,infections,diffusion);
		train_cascades.put(3, struct);
		
		
		cascades_ids=new ArrayList<Integer>(train_cascades.keySet());
	}

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		
		String db = args[0] ;
		int nbComm= Integer.parseInt(args[1]);
		int batchsize= Integer.parseInt(args[2]);
		boolean usePrelearn=Boolean.parseBoolean(args[3]) ;
		boolean lockParams=Boolean.parseBoolean(args[4]) ;
		int posweight=Integer.parseInt(args[5]) ;
		int nbiter=Integer.parseInt(args[6]) ;
		double percentPre=Double.parseDouble(args[7]) ;
		double initLine =Double.parseDouble(args[8]) ;
		double endLine = Double.parseDouble(args[9]) ;
		int contentSize = Integer.parseInt(args[10]) ;
		double lambda = Double.parseDouble(args[11]) ;
		boolean useDoubleLayer = Boolean.parseBoolean(args[12]) ;
		String modelFile = args[13] ;
		String resultsDir =args[14] ;
		
		// TO FINISH
		
		db = "arti100_8_0.01_0.01_1.0_2_0.05_10000_10000" ;
		nbComm= 2;
		batchsize= 50;
		usePrelearn=true ;
		lockParams=true ;
		posweight=20 ;
		nbiter=0 ;
		percentPre=0.40 ;
		initLine = 0.1 ;
		endLine = 0.1 ;
		contentSize = 0 ;
		lambda = 0.5 ;
		useDoubleLayer = false ;
		modelFile = "/home/bourigaults/Bureau/testComm5/modelfile" ;
		resultsDir = "/home/bourigaults/Bureau/testComm5/results" ;

		
		MLPCommunities2 mlpc = new MLPCommunities2(modelFile,nbComm,batchsize,usePrelearn,lockParams,posweight,nbiter,percentPre,(contentSize > 0),contentSize,lambda,useDoubleLayer) ;
		//mlpc.prepareLearning("test", "test", 1, 1) ;
		//LineSearch lsearch=LineSearch.getConstantLine(0.01);
		LineSearch lsearch=LineSearch.getFactorLine(initLine,Math.pow(endLine/initLine,1.0/(double)nbiter)); 
 		DescentDirection dir=DescentDirection.getGradientDirection();
 		if(!db.startsWith("arti"))
 			User.loadUsersFrom(db, "users_1") ;
 		mlpc.learn(db, "cascades_1", 1, 1, Optimizer.getDescent(dir, lsearch)) ;
 		mlpc.save2seperateMoreDivide("/home/bourigaults/Bureau/testComm5/modelfile.arti.new");
 		
 		//try {
 			//mlpc.save() ;
			//mlpc.load() ;
 			//throw new IOException() ;
		//} catch (IOException e) {
			// TODO Auto-generate catch block
		//	e.printStackTrace();
		//}
 		
 		//MLPdiffusion mlpd = new MLPdiffusion(10, 1000, false, false, false, false, false, false, false) ;
 		//mlpd.learn("digg","cascades_1",1,1,Optimizer.getDescent(dir, lsearch),0.5f,false,false) ;
 		//EvalPropagationModel.run(new EvalMLPDiff(mlpd), "/home/bourigaults/Bureau/testComm/verifDiff") ;
		
		
		//EvalPropagationModel.run(new EvalMLPCommunities2(mlpc,db,"cascades_2"), resultsDir) ;
	}
	
	
	
	public void save2seperate(String file) throws FileNotFoundException{
		
		double[] p1 = new double[nbUsers*nbUsers] ;
		double[] c1 = new double[nbUsers] ;
		double[] p2 = new double[nbUsers*nbUsers] ;
		double[] c2 = new double[nbUsers] ;
		
		boolean tictoc = true ;
		
		for(int cascadeid : this.cascades_ids)  {
 			PropagationStruct c = this.train_cascades.get(cascadeid) ;
 			for(String source : c.getArrayInit()) {
 				tictoc = !tictoc ;
 				int s=this.users.indexOf(source) ;
 				if(tictoc) {
	 				c1[s]+=1.0 ;
	 				for(String infect : c.getArrayContamined()){
	 					int i = this.users.indexOf(infect) ;
	 					p1[Matrix.IDX2C(s, i, nbUsers)]+=1.0 ;
	 				}
 				} else {
 					c2[s]+=1.0 ;
	 				for(String infect : c.getArrayContamined()){
	 					int i = this.users.indexOf(infect) ;
	 					p2[Matrix.IDX2C(s, i, nbUsers)]+=1.0 ;
	 				}
 				}
 			}
 		}
		// Division finale
		for(int i=0 ; i<nbUsers ; i++) {
			if(c1[i]>0) {
				for(int j=0 ; j<nbUsers ; j++) {
					p1[Matrix.IDX2C(i, j, nbUsers)] /= c1[i] ;
					//System.out.println("init "+i+","+j+" : "+p[Matrix.IDX2C(i, j, nbUsers)]);
				}
			}
			if(c2[i]>0) {
				for(int j=0 ; j<nbUsers ; j++) {
					p2[Matrix.IDX2C(i, j, nbUsers)] /= c2[i] ;
					//System.out.println("init "+i+","+j+" : "+p[Matrix.IDX2C(i, j, nbUsers)]);
				}
			}
		}
		
		PrintStream f1 = new PrintStream(new File(file+".1")) ;
		PrintStream f2 = new PrintStream(new File(file+".2")) ;
		for(int i=0 ; i<nbUsers*nbUsers ; i++) {
			f1.println(p1[i]);
			f2.println(p2[i]);
		}
		f1.close() ;
		f2.close() ;
		
		
	}
	
	public void save2seperateFull(String file) throws FileNotFoundException{
		double[] p1 = new double[nbUsers*nbUsers] ;
		double[] c1 = new double[nbUsers*nbUsers] ;
		double[] p2 = new double[nbUsers*nbUsers] ;
		double[] c2 = new double[nbUsers*nbUsers] ;
		
		boolean tictoc = true ;
		
		int x = 0 ;
		
		for(int cascadeid : this.cascades_ids)  {
 			PropagationStruct c = this.train_cascades.get(cascadeid) ;
 			HashMap<String,Long> time =  c.getInfectionTimes() ;
 			//System.out.println(time);
 			//ArrayList<String> previous = new ArrayList<String>() ;
 			tictoc=!tictoc ;
 			for(String user1 : time.keySet()) {
 				int i1 = users.indexOf(user1) ;
 				for(String user2 : users) {
 	 				int i2 = users.indexOf(user2) ;
 	 				if(tictoc) {
	 	 				if( !time.containsKey(user2)) {
	 	 					c1[Matrix.IDX2C(i1, i2, nbUsers)]++ ;
	 	 				} else if(time.get(user1) < time.get(user2)) {
	 	 					c1[Matrix.IDX2C(i1, i2, nbUsers)]++ ;
	 	 					p1[Matrix.IDX2C(i1, i2, nbUsers)]++ ;
	 	 				}
 	 				} else {
 	 					if( !time.containsKey(user2)) {
	 	 					c2[Matrix.IDX2C(i1, i2, nbUsers)]++ ;
	 	 				} else if(time.get(user1) < time.get(user2)) {
	 	 					c2[Matrix.IDX2C(i1, i2, nbUsers)]++ ;
	 	 					p2[Matrix.IDX2C(i1, i2, nbUsers)]++ ;
	 	 				}
 	 				}
  	 			}
 			}
 			System.out.println("Cascade "+(x++)+ "/"+this.cascades_ids.size());
 			//if(x>2000)
 			//	break ;
 		}
		// Division finale
		for(int i=0 ; i<nbUsers*nbUsers ; i++) {
			if(c1[i]>0) {
					p1[i] /= c1[i] ;
					//System.out.println("init "+i+","+j+" : "+p[Matrix.IDX2C(i, j, nbUsers)]);
			}
			if(c2[i]>0) {
					p2[i] /= c2[i] ;
					//System.out.println("init "+i+","+j+" : "+p[Matrix.IDX2C(i, j, nbUsers)]);
			}
		}
		
		PrintStream f1 = new PrintStream(new File(file+".1")) ;
		PrintStream f2 = new PrintStream(new File(file+".2")) ;
		for(int i=0 ; i<nbUsers*nbUsers ; i++) {
			f1.println(p1[i]);
			f2.println(p2[i]);
		}
		f1.close() ;
		f2.close() ;
	}
	
	
	/*
	 * Version ou on coupe l'ensemble des cascades en deux. Puis, pour chaque paire de users, on coupe la population en deux.
	 */
	public void save2seperateMoreDivide(String file) throws FileNotFoundException{
		
		PrintStream f = new PrintStream(new File(file)) ;
		
		
		// Creer un petit ensemble de users.
		double[] count = new double[nbUsers] ;
		for(int cid : this.train_cascades.keySet()) {
			PropagationStruct c = this.train_cascades.get(cid) ;
			HashMap<String, Long> time = c.getInfectionTimes();
			for(String s : time.keySet() ) {
				count[users.indexOf(s)]++;
			}
		}
		ArrayList<String> usersToDraw = new ArrayList<String>();
		for(int i=0 ; i<nbUsers ; i++) {
			if(count[i]>1) 
				usersToDraw.add(users.get(i)) ;
		}
		System.out.println("Users to draw size : "+usersToDraw.size());
		
		// Diviser les cascades en deux.
		/*HashSet<PropagationStruct> cascades1 = new HashSet<PropagationStruct>() ;
		HashSet<PropagationStruct> cascades2 = new HashSet<PropagationStruct>() ;
		
		for(int i : this.train_cascades.keySet()) {
			if(i%2 == 0) {
				cascades1.add(train_cascades.get(i)) ;
			} else {
				cascades2.add(train_cascades.get(i)) ;
			}
		}*/
		
		Random rng = new Random() ;
		int N = this.nbUsers ;
		
		// Tirer des paires
		for(int i= 0 ; i<(100) ; i++) {
			System.out.println("Paire" +i);
			String u1 = usersToDraw.get(rng.nextInt(usersToDraw.size())) ;
			String u2 ;
			do{
				u2 = usersToDraw.get(rng.nextInt(usersToDraw.size())) ;
			} while(u2==u1) ;
			
			// Diviser les utilisateurs en deux
			/*ArrayList<String> users1 = new ArrayList<String>() ;
			ArrayList<String> users2 = new ArrayList<String>() ;		
			for(String u : this.users) {
				if(rng.nextBoolean()) {
					users1.add(u) ;
				} else {
					users2.add(u) ;
				}
			}*/
			
			// Calculer les influences sur les autres. 
			
			double[] p1 = new double[nbUsers] ;
			double[] c1 = new double[nbUsers] ;
			double[] p2 = new double[nbUsers] ;
			double[] c2 = new double[nbUsers] ;
			for(int cid : this.train_cascades.keySet()) {
				PropagationStruct c = this.train_cascades.get(cid) ;
				HashMap<String, Long> time = c.getInfectionTimes();
				if(time.containsKey(u1) != time.containsKey(u2)) { // Si la cascade contient UN SEUL DES DEUX
					//System.out.println(time+" "+u1+" "+u2);
					String u = time.containsKey(u1) ? u1 : u2 ;
					int i1=users.indexOf(u) ;
					long t = time.get(u) ;
					for(String uinfected : users) {
						int i2=users.indexOf(uinfected) ;
						if(u==u1) {
							if( !time.containsKey(uinfected)) {
		 	 					c1[i2]++ ;
		 	 				} else if(time.get(u) < time.get(uinfected)) {
		 	 					c1[i2]++ ;
		 	 					p1[i2]++ ;
		 	 				}
						} else {
							if( !time.containsKey(uinfected)) {
		 	 					c2[i2]++ ;
		 	 				} else if(time.get(u) < time.get(uinfected)) {
		 	 					c2[i2]++ ;
		 	 					p2[i2]++ ;
		 	 				}
						}
					}
				}
				
			}
			
			//Ecrire tout ça
			double sim1 = 0 ;
			double norm1 = 0 ;
			double norm2 = 0 ;
			for(int idu = 0 ; idu<nbUsers ; idu++) {
				if(p1[idu]>0) {
					f.print(p1[idu]/c1[idu]+" ");
				} else {
					f.print(0.0+" ");
				}
			} 
			f.println();
			for(int idu = 0 ; idu<nbUsers ; idu++) {
				if(p2[idu]>0) {
					f.print(p2[idu]/c2[idu]+" ");
				} else {
					f.print(0.0+" ");
				}
			}
			f.println();
			
		}
		
	}
	

}



class EvalMLPCommunities2 extends EvalPropagationModelConfig{

	private MLPCommunities2 mlpc ;
	
	public EvalMLPCommunities2( MLPCommunities2 mlpc,String db,String cascades) {
		this.mlpc=mlpc ;
		
		pars.put("db",db); //usElections5000_hashtag");
		pars.put("cascadesCol",cascades);
		pars.put("step", "1");
		pars.put("nbInitSteps", "1");
		pars.put("nbCaades", "1000");
		pars.put("allUsers", "users_1");
		
	}
	
	@Override
	public LinkedHashMap<PropagationModel, Integer> getModels() { 
		LinkedHashMap<PropagationModel,Integer> h=new  LinkedHashMap<PropagationModel, Integer>();
		h.put(mlpc, 1) ;
		return h;
	}

	@Override
	public EvalMeasureList getMeasures() {
		HashSet<String> u = new HashSet<String>() ;
		for(String s : mlpc.getUsers()) { // ON EST SUR DE ça ?
			u.add(s) ;
		}
		MAP map = new MAP(u,true) ;
		ArrayList<EvalMeasure> arrayev=new ArrayList<EvalMeasure>(1) ;
		arrayev.add(map) ;
		EvalMeasureList ev = new EvalMeasureList(arrayev) ;
		return ev;
	}
}

class EvalMLPDiff2  extends EvalPropagationModelConfig{

	private MLPdiffusion mlpc ;
	
	public EvalMLPDiff2( MLPdiffusion mlpd) {
		this.mlpc=mlpd ;
		
		pars.put("db","digg"); //usElections5000_hashtag");
		pars.put("cascadesCol", "cascades_1");
		pars.put("step", "1");
		pars.put("nbInitSteps", "1");
		pars.put("nbCascades", "-1");
		pars.put("allUsers", "users_1");
		
	}
	
	@Override
	public LinkedHashMap<PropagationModel, Integer> getModels() { 
		LinkedHashMap<PropagationModel,Integer> h=new  LinkedHashMap<PropagationModel, Integer>();
		h.put(mlpc, 1) ;
		return h;
	}

	@Override
	public EvalMeasureList getMeasures() {
		HashSet<String> u = new HashSet<String>() ;
		this.loadAllUsers() ;
		for(String s : this.allUsers) { // ON EST SUR DE ça ?
			u.add(s) ;
		}
		MAP map = new MAP(u,true) ;
		ArrayList<EvalMeasure> arrayev=new ArrayList<EvalMeasure>(1) ;
		arrayev.add(map) ;
		EvalMeasureList ev = new EvalMeasureList(arrayev) ;
		return ev;
	}
}
