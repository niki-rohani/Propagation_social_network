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
import simon.tools.CascadesComGen;
import propagationModels.PropagationStructLoader;


/** 
 * Modèle MLP qui fait : diff_initiale -> couche communautes -> couche communautes -> diffusion finale
 * Ca fait des couches de tailles : nb_users, nb_comm, nb_comm, nb_users
 * @author bourigaults
 *
 */


public class MLPCommunities extends MLP {

	
	int nbUsers ;
	private Module outputForInference ;
	private Tensor currentInput=null ;
	private Tensor currentLabels=null ;
	
	private Module encode ;
	private Module decode ;
	private Module inter ;
	
	private double NEGVALUEINPUT = 0 ; // Valeur pour les mecs non infectés
	private double NEGVALUEOUTPUT = 0 ; // Valeur pour les mecs non infectés.
	private double TanhAlpha =1.0 ;
	private double TanhLambda=1.0 ;
	
	
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
	private int contentSize = 0 ;
	
	private Criterion cri ;
	private double lambda;
	private String logFile;
	private int totalNbForward;
	private PrintStream log;
	private double meanLoss;
	
	CascadesComGen gen ;
	 
	
	//public MLPCommunities(String model_file) {
	//	super(model_file);
	//}
	
	public MLPCommunities(String model_file,int nbCom,int batchsize) {
		this(model_file,nbCom,batchsize,false,false,1,2000,0.25,false,0,0,false) ;
	}
	
	public MLPCommunities(String model_file,int nbCom,int batchsize,boolean prelearn, boolean lockDecodeParam,int posweight,int maxIter, double percentPrelearn,boolean useContent, int contentSize, double lambda, boolean useDoubleLayer) {
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
	
	
	public void prepareLearning(PropagationStructLoader ploader){
		String db=ploader.getDb();
		if(db=="test"){
			test() ;
		} else if(db.startsWith("arti")) {
			arti(db) ;
		} else if(db.startsWith("load")) {
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
		}else {
			super.prepareLearning(ploader) ;
		}
		//String 
		//DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,cascadesCollection);
		if(!useContent)
			contentSize = 0 ;
		
		//this.nbUsers = nbUsers ;
		
		this.nbUsers = this.users.size() ;
		//nbUsers=this.nbUsers;

		this.params=new Parameters();
		System.out.println("NB USers ="+ nbUsers);
 		encode=new CPUSparseLinear(nbUsers+contentSize,nbCom);
 		double[] tempPar = new double[(nbUsers+contentSize)*nbCom] ;
 		double[] tempPar2 = new double[(nbUsers+contentSize)*nbCom] ;
 		for(int i=0 ; i<(nbUsers+contentSize) ; i++) {
 			for(int j=0 ; j<nbCom ; j++) {
	 			double x = Math.random() ;
	 			if(x<0.25) {
	 				tempPar[i*nbCom+j]=0.2 ;
	 				tempPar2[j*(nbUsers+contentSize)+i]=0.8 ;
	 			} else {
	 				tempPar[i*nbCom+j]=0.8;
	 				tempPar2[j*(nbUsers+contentSize)+i]=0.2 ;
	 			} 
 			}
 		}
 		this.params.allocateNewParamsFor(encode,0f,1.0f);
 		CPUTanh tan1=new CPUTanh(nbCom,TanhAlpha,TanhLambda);
 		//CPULogistic tan1=new CPULogistic(nbCom);
 		CPUTimesVals times1 = new CPUTimesVals(nbCom, 0.998f) ; times1.setName("times1") ;
 		CPUAddVals add1 = new CPUAddVals(nbCom, 0.001f) ;
 		
 		decode=new CPULinear(nbCom,nbUsers);
 		this.params.allocateNewParamsFor(decode,0f,1.0f);
 		CPUTanh tan3=new CPUTanh(nbUsers,TanhAlpha,TanhLambda);
 		//CPULogistic tan3=new CPULogistic(nbUsers);
 		CPUTimesVals times2 = new CPUTimesVals(nbUsers, 0.998f) ; times2.setName("times2") ;
 		CPUAddVals add2 = new CPUAddVals(nbUsers, 0.001f) ;
 		//CPUSoftmax softmax=new CPUSoftmax(nbUsers);
 		
 		SequentialModule seq = new SequentialModule() ;
 		seq.addModule(encode);
 		seq.addModule(tan1) ;
 		seq.addModule(times1) ;
 		seq.addModule(add1) ;
 		
 		if(useDoubleLayer) {
 			inter=new CPULinear(nbCom,nbCom);
 			double[] p = new double[nbCom*nbCom] ;
 			for(int i = 0 ; i <nbCom ; i++) {
 				p[Matrix.IDX2C(i, i, nbCom)]=1 ;
 			}
 			this.params.allocateNewParamsFor(inter,p,-1.0f,1.0f);
 	 		CPULogistic tanI=new CPULogistic(nbCom);
 	 		CPUTimesVals timesI = new CPUTimesVals(nbCom, 0.998f) ;
 	 		CPUAddVals addI = new CPUAddVals(nbCom, 0.001f) ;
 	 		seq.addModule(inter);
 	 		//seq.addModule(tanI) ;
 	 		//seq.addModule(timesI) ;
 	 		//seq.addModule(addI) ;
 		} 
 		seq.addModule(decode);
 		seq.addModule(tan3) ;
 		seq.addModule(times2) ;
 		seq.addModule(add2) ;
 		
 		outputForInference=tan3 ;
		
		/*TensorModule mod1=new CPUSparseLinear(nbUsers,nbUsers);
 		this.params.allocateNewParamsFor(mod1,-2.0f,2.0f);
 		CPUTanh tan1=new CPUTanh(nbUsers);
 		global.addModule(mod1) ;
 		global.addModule(tan1) ;*/
 		
 		if(this.useAutoencodePrelearn) {
 			cri=new FakeCrossEntroCriterion(nbUsers,posWeight) ;
 		} else {
 			//cri=new CPUHingeLoss(nbUsers);
 			cri=new FakeCrossEntroCriterion(nbUsers,posWeight) ;
 		}
 		CPUAverageRows av=new CPUAverageRows(1) ;
 		seq.addModule(cri);
 		seq.addModule(av) ;
 		
 		if(this.lambda > 0) {
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
 		} else {
 			global=seq ;
 		}
 		
 		/*SequentialModule seq2=new SequentialModule();
 		CPUParams par=new CPUParams();
 		params.giveAllParamsTo(par); // On recupere tous les param pour leur appliquer une regularisation
 		CPUL2Norm norm=new CPUL2Norm(par.getNbParams()); // On rajoute un truc de regularisation
 		seq2.addModule(par);
 		seq2.addModule(norm);*/
 		
 		
 		
	}

	@Override
	public int infer(Structure struct) {
		if(!useContent)
			this.contentSize=0 ;
		System.out.println("Inference...");
		ArrayList<String> init = ((PropagationStruct)struct).getArrayInit() ;
		Matrix minit = new CPUSparseMatrix(1, this.nbUsers+this.contentSize) ;
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
	
	public int inferSimulation(Structure struct){
		throw new RuntimeException("Not implemented");
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
    	//this.nbInitSteps=Integer.parseInt(f.readLine().substring(11)) ;
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
    	encode = new CPUSparseLinear(nbUsers+contentSize,nbCom);
    	this.params.allocateNewParamsFor(encode,0,0f,1.0f);
    	CPUTanh tan1=new CPUTanh(nbCom,TanhAlpha,TanhLambda);
 		//CPULogistic tan1=new CPULogistic(nbCom);
 		CPUTimesVals times1 = new CPUTimesVals(nbCom, 0.998f) ; times1.setName("times1") ;
 		CPUAddVals add1 = new CPUAddVals(nbCom, 0.001f) ;
 		
 		decode=new CPULinear(nbCom,nbUsers);
 		this.params.allocateNewParamsFor(decode,0f,1.0f);
 		CPUTanh tan3=new CPUTanh(nbUsers,TanhAlpha,TanhLambda);
 		//CPULogistic tan3=new CPULogistic(nbUsers);
 		CPUTimesVals times2 = new CPUTimesVals(nbUsers, 0.998f) ; times2.setName("times2") ;
 		CPUAddVals add2 = new CPUAddVals(nbUsers, 0.001f) ;
 		//CPUSoftmax softmax=new CPUSoftmax(nbUsers);
 		
 		global = new SequentialModule() ;
 		global.addModule(encode);
 		global.addModule(tan1) ;
 		global.addModule(times1) ;
 		global.addModule(add1) ;
 		
 		if(useDoubleLayer) {
 			inter=new CPULinear(nbCom,nbCom);
 			this.params.allocateNewParamsFor(inter,-1.0f,1.0f);
 	 		CPULogistic tanI=new CPULogistic(nbCom);
 	 		CPUTimesVals timesI = new CPUTimesVals(nbCom, 0.998f) ;
 	 		CPUAddVals addI = new CPUAddVals(nbCom, 0.001f) ;
 	 		global.addModule(inter);
 	 		global.addModule(tanI) ;
 	 		global.addModule(timesI) ;
 	 		global.addModule(addI) ;
 		} 
 		
 		global.addModule(decode);
 		global.addModule(tan3) ;
 		global.addModule(times2) ;
 		global.addModule(add2) ;
 		
 		outputForInference=tan3 ;
 		
 		f.readLine() ;
 		int nbpar = 0 ;
 		ArrayList<Parameter> thisparams = this.params.getParams() ;
 		for(String s = f.readLine() ; s.compareTo("</PARAMETERS>")!=0 ; s=f.readLine()) {
    		thisparams.get(nbpar).setVal(Float.parseFloat(s)) ;
    		nbpar++ ;
    	}
 		//if(nbpar != (nbCom*nbUsers+nbCom*nbCom+nbUsers*nbCom)) {
 		int expected = (useDoubleLayer ? (2*nbUsers*nbCom+nbCom*contentSize+nbCom*nbCom) : (2*nbUsers*nbCom+nbCom*contentSize)) ;
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
	
	public void learn(PropagationStructLoader ploader,Optimizer optim, String logfile){
		prepareLearning(ploader);
		this.logFile=logfile ;
		this.totalNbForward=0 ;
		this.meanLoss = 0.0 ;
		try {
			this.log = new PrintStream(new File(this.logFile)) ;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.batchsize = batchsize ;
		if(this.useAutoencodePrelearn && percentPrelearn!=0.0) {
			if(useDoubleLayer) {
				inter.lockParams();
				optim.optimize(this,0.00001,(int)(this.nbIter*this.percentPrelearn*0.5),false) ;
				//inter.unlockParams();
				//optim.optimize(this,0.00001,(int)(this.nbIter*this.percentPrelearn*0.5),false) ;
				if(lockDecodeParams){
					this.decode.lockParams();
				}
			} else {
				optim.optimize(this,0.00001,(int)(this.nbIter*this.percentPrelearn),false) ;
				if(lockDecodeParams){
					this.decode.lockParams();
				}
			}
			this.useAutoencodePrelearn=false;
			if(percentPrelearn!=1.0)
				optim.optimize(this,0.00001,(int)(this.nbIter*(1.0-this.percentPrelearn)),false) ; 
			
		} else {
			optim.optimize(this,0.00001,this.nbIter,false) ;
		}
		
	}

	@Override
	public void forward() {
		if(!useContent)
			contentSize = 0 ;
		Tensor input = new Tensor(1) ;
		Tensor labels = new Tensor(1) ;
		Matrix minit = new CPUSparseMatrix(batchsize!=-1 ? batchsize : this.train_cascades.size(),this.nbUsers+contentSize) ;
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
						//mout.setValue(i_input, iduser,1) ;
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
						minit.setValue(i, j, mout.getValue(i, j));
					}
				}
				
			}
			//if(currentInput==null){currentInput=input ;}
			//if(currentLabels==null){currentLabels=labels ;}
			currentInput=input ;
			currentLabels=labels ;
			//System.out.println(currentInput);
			//System.out.println(currentLabels);
			cri.setLabels(currentLabels) ;
			global.forward(currentInput) ;
		}
		
		this.totalNbForward++ ;
		this.meanLoss+=global.getOutput().getMatrix(0).getValue(0, 0) ;
		if(logFile != "" && ((this.totalNbForward % 100) ==0)) {
			this.log.println("Frwrd "+this.totalNbForward+", loss = "+(this.meanLoss/100)) ;
			this.meanLoss=0 ;
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
		/*users.add("f"); users.add("g"); users.add("h"); users.add("i"); users.add("j");
		users.add("k"); users.add("l"); users.add("m"); users.add("n"); users.add("o");
		users.add("p"); users.add("q"); users.add("r"); users.add("s"); users.add("t");
		users.add("u"); users.add("v"); users.add("w"); users.add("x"); users.add("y");*/
		//this.train_users=new HashMap<String,HashMap<Integer,Double>>();
		
		train_cascades=new HashMap<Integer,PropagationStruct>();
		
		String[] cascades = {"ade","bde","cde", "dabc", "eabc"} ;
		
		int cid = 0;
		for(String c : cascades) {
			TreeMap<Long,HashMap<String,Double>> init=new TreeMap<Long,HashMap<String,Double>>();
			TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
			TreeMap<Integer,Double> diffusion=new TreeMap<Integer,Double>();
			HashMap<String,Double> h=new HashMap<String,Double>();
			h.put(c.substring(0, 1), 1.0) ;
			init.put(1l, h);
			infections.put(1l, h);
			h=new HashMap<String,Double>();
			for(char user : c.substring(1).toCharArray()) {
				h.put(String.valueOf(user), 1.0);
			}
			infections.put(2l,h);
			diffusion.put(0, 1.0);
			PropagationStruct struct=new PropagationStruct(null,step,nbInitSteps,init,infections,diffusion);
			train_cascades.put(cid++, struct);
		}
		
		cascades_ids=new ArrayList<Integer>(train_cascades.keySet());
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
		
		gen = new CascadesComGen(nbusers,nbCom,probAct,probPass,noize,maxSources,probaStop) ;
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		try {
			runTestArti() ;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(Math.random()<1.0) {
			return ;
		}
		
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
		String logfile =args[15] ;
		boolean learn =Boolean.parseBoolean(args[16]) ;
		String cascadetrain = args[17] ;
		String cascadetest = args[18] ;
		System.out.println(learn);
		
		
		System.out.println("String db = args[0] ;int nbComm= Integer.parseInt(args[1]);int batchsize= Integer.parseInt(args[2]);boolean usePrelearn=Boolean.parseBoolean(args[3]) ;boolean lockParams=Boolean.parseBoolean(args[4]) ;int posweight=Integer.parseInt(args[5]) ;int nbiter=Integer.parseInt(args[6]) ;double percentPre=Double.parseDouble(args[7]) ;double initLine =Double.parseDouble(args[8]) ;double endLine = Double.parseDouble(args[9]) ;int contentSize = Integer.parseInt(args[10]) ;double lambda = Double.parseDouble(args[11]) ;boolean useDoubleLayer = Boolean.parseBoolean(args[12]) ;String modelFile = args[13] ;String resultsDir =args[14] ;String logfile =args[15] ;Boolean learn =Boolean.parseBoolean(args[16]) ; String cascadetrain = args[17] ;String cascadetest = args[18] ;");
		
		// TO FINISH
		// "arti100_8_0.01_0.01_0.05_2_0.1_10000_10000"
		db = "arti100_8_0.01_0.01_0.05_2_0.1_10000_10000" ;
		nbComm= 8;
		batchsize= 25;
		usePrelearn=false ;
		lockParams=false ;
		posweight=1;
		nbiter=200 ;
		percentPre=0.0;
		initLine = 0.3 ;
		endLine = 0.01 ;
		contentSize = 0 ;
		lambda = 0.2 ;
		useDoubleLayer = false ;
		modelFile = "/home/bourigaults/Bureau/testComm5/modelfile" ;
		resultsDir = "/home/bourigaults/Bureau/testComm5/results" ;
		logfile = "/home/bourigaults/Bureau/testComm5/log.txt" ;
		learn = true ;
		cascadetrain = "cascades_1" ;
		cascadetest = "cascades_2" ;
		double ratioInits=0.1;

		
		MLPCommunities mlpc = new MLPCommunities(modelFile,nbComm,batchsize,usePrelearn,lockParams,posweight,nbiter,percentPre,(contentSize > 0),contentSize,lambda,useDoubleLayer) ;
		//mlpc.prepareLearning("test", "test", 1, 1) ;
		//LineSearch lsearch=LineSearch.getFactorLine(initLine,endLine);
		LineSearch lsearch=LineSearch.getFactorLine(initLine,Math.pow(endLine/initLine,1.0/(double)nbiter)); 
 		DescentDirection dir=DescentDirection.getGradientDirection();
 		PropagationStructLoader ploader=new PropagationStructLoader(args[0],args[1],(long)1,ratioInits,-1);
		
 		if(!db.startsWith("arti"))
 			User.loadUsersFrom(db, "users_1") ;
 		if(learn)
 			mlpc.learn(ploader, Optimizer.getDescent(dir, lsearch),logfile) ;

 		CPUSparseLinear s = ((CPUSparseLinear) mlpc.encode ) ;
 		for(int i=0 ; i<mlpc.nbUsers ; i++) {
 			for(int j=0 ; j<2 ; j++ ) {
 				System.out.print(s.getParameters().getMatrix(0).getValue(i, j)+" ") ;
 			}
 			System.out.println();
 		}
 		CPULinear t = ((CPULinear)  mlpc.decode ) ;
 		for(int i=0 ; i<2 ; i++) {
 			for(int j=0 ; j<mlpc.nbUsers ; j++ ) {
 				System.out.print(t.getParameters().getMatrix(0).getValue(i, j)+" ") ;
 			}
 			System.out.println();
 		}
 			
 		
 		try {
 			if(learn)
 				mlpc.save() ;
 			else
 				mlpc.load() ;
 			//throw new IOException() ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 		
 		//MLPdiffusion mlpd = new MLPdiffusion(10, 1000, false, false, false, false, false, false, false) ;
 		//mlpd.learn("digg","cascades_1",1,1,Optimizer.getDescent(dir, lsearch),0.5f,false,false) ;
 		//EvalPropagationModel.run(new EvalMLPDiff(mlpd), "/home/bourigaults/Bureau/testComm/verifDiff") ;
		
		
		//EvalPropagationModel.run(new EvalMLPCommunities(mlpc,db,cascadetrain), resultsDir+"train") ;
		//EvalPropagationModel.run(new EvalMLPCommunities(mlpc,db,cascadetest), resultsDir+"test") ;
 		
 		try {
			double a = mlpc.evaltest(mlpc.gen.generate(2000));
			double b = mlpc.evaltest(mlpc.gen.generate(2000));
			double c = mlpc.evaltest(mlpc.gen.generate(2000));
			double d = mlpc.evaltest(mlpc.gen.generate(2000));
			double e = mlpc.evaltest(mlpc.gen.generate(2000));
			System.out.println(a+"\n"+b+"\n"+c+"\n"+d+"\n"+e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 		
	}
	
	
	
	
	
	public static void runTestArti() throws IOException {
		
		PrintStream f;
		f = new PrintStream("/home/bourigaults/workspace/Propagation/Results/expsArti/resultfew/finalLog");
		
		int[] nbcs = {24} ;
		int[] nbcModel={24} ;
		boolean[] prelearn = {true} ;
		double[] probaActs = {0.1} ;
		double[] probaPasss = {0.3} ;
		for(int nbc : nbcs) {
			for(double probAct : probaActs ) {
				for(double probPass : probaPasss) {
					f.println(nbc+"_"+probAct+"_"+probPass);
					for(int i = 0 ; i<1 ; i++) {
						for(int nbcM : nbcModel) {
							for(boolean usePre : prelearn) {
								File d = new File("/home/bourigaults/workspace/Propagation/Results/expsArti/resultfew/NUMBER-"+i+"_"+nbc+"_"+probAct+"_"+probPass) ;
								if (!d.exists())
									d.mkdir() ;
								String s = "/home/bourigaults/workspace/Propagation/Results/expsArti/resultfew/NUMBER-"+i+"_"+nbc+"_"+probAct+"_"+probPass ;
								MLPCommunities mlpc = new MLPCommunities(s+"/modelFile",nbcM,50,usePre,false,1,400,0.25,false,0,0.0,false) ;
								LineSearch lsearch=LineSearch.getFactorLine(0.3,Math.pow(0.01/0.3,1.0/(double)400)); 
						 		DescentDirection dir=DescentDirection.getGradientDirection();
								//mlpc.learn("load/home/bourigaults/workspace/Propagation/Results/expsArti/datafew/train_NUMBER-"+i+"_"+nbc+"_"+probAct+"_"+probPass, "", 1, 1, Optimizer.getDescent(dir, lsearch), s+"/log.txt");
								//gen.saveCascades(10000,"/home/bourigaults/workspace/Propagation/Results/expsArti/data/train_NUMBER-"+i+"_"+nbc+"_"+probAct+"_"+probPass) ;
								//gen.saveCascades(10000,"/home/bourigaults/workspace/Propagation/Results/expsArti/data/test_NUMBER-"+i+"_"+nbc+"_"+probAct+"_"+probPass) ;
								
								MLPCommunities2 mlpc2 = new MLPCommunities2(s+"/modelFile2",nbcM,50,false,false,1,0,0.25,false,0,0.1,false) ;
								lsearch=LineSearch.getFactorLine(0.3,Math.pow(0.01/0.3,1.0/(double)2000)); 
						 		dir=DescentDirection.getGradientDirection();
								mlpc2.learn("load/home/bourigaults/workspace/Propagation/Results/expsArti/datafew/train_NUMBER-"+i+"_"+nbc+"_"+probAct+"_"+probPass, "", 1, 1, Optimizer.getDescent(dir, lsearch));
								
								f.println(nbcM+":"+usePre+" "+mlpc.evaltest(CascadesComGen.loadCascades("/home/bourigaults/workspace/Propagation/Results/expsArti/datafew/test_NUMBER-"+i+"_"+nbc+"_"+probAct+"_"+probPass))) ;
								f.println(nbcM+":"+usePre+" "+mlpc2.evaltest(CascadesComGen.loadCascades("/home/bourigaults/workspace/Propagation/Results/expsArti/datafew/test_NUMBER-"+i+"_"+nbc+"_"+probAct+"_"+probPass))) ;
							}
						}
					}
					f.println();
				}
			}
		}
		f.close() ;
			
	}

}



class EvalMLPCommunities extends EvalPropagationModelConfig{

	private MLPCommunities mlpc ;
	
	public EvalMLPCommunities( MLPCommunities mlpc,String db,String cascades) {
		this.mlpc=mlpc ;
		
		pars.put("db",db); //usElections5000_hashtag");
		pars.put("cascadesCol",cascades);
		pars.put("step", "1");
		pars.put("nbInitSteps", "1");
		pars.put("nbCascades", "1000");
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

class EvalMLPDiff  extends EvalPropagationModelConfig{

	private MLPdiffusion mlpc ;
	
	public EvalMLPDiff( MLPdiffusion mlpd) {
		this.mlpc=mlpd ;
		
		pars.put("db","digg"); //usElections5000_hashtag");
		pars.put("cascadesCol", "cascades_1");
		pars.put("step", "1");
		pars.put("nbInitSteps", "1");
		pars.put("nbCascades", "1000");
		pars.put("ignoreDiffInitFinallyLessThan", "1");
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
