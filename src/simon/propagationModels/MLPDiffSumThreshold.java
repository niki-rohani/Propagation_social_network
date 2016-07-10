package simon.propagationModels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.management.RuntimeErrorException;
import javax.naming.NoInitialContextException;

import cascades.ArtificialCascade;
import cascades.Cascade;
import mlp.CPUAddVals;
import mlp.CPUAverageRows;
import mlp.CPUHingeLoss;
import mlp.CPUMatrix;
import mlp.CPUParams;
import mlp.DescentDirection;
import mlp.Env;
import mlp.LineSearch;
import mlp.MLPModel;
import mlp.Optimizer;
import mlp.Parameter;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.Tensor;
import core.HashMapStruct;
import core.Structure;
import experiments.EvalMeasure;
import experiments.EvalMeasureList;
import experiments.EvalPropagationModel;
import experiments.EvalPropagationModelConfig;
import experiments.Hyp;
import experiments.MAP;
import experiments.Result;
import propagationModels.MLP;
import propagationModels.NaiveLink;
import propagationModels.PropagationModel;
import propagationModels.PropagationStruct;
import propagationModels.PropagationStructLoader;
import simon.mlp.CPUHeatDist;
import simon.mlp.CPUMinusParams;
import simon.mlp.CPUPseudoHeat;
import simon.mlp.CPUSmoothMaxRows;
import simon.mlp.CPUSoftmax;


// Version Clean.
public class MLPDiffSumThreshold extends MLP {

	
	protected int nbDims; // Le nombre de dimension de l'espace
	protected boolean dualPoint; // Utiliser deux points par utilisateur (source et cible). Pour l'instant, j'ai pas test.
	private boolean withDiag ; // Utiliser une diagonale pour ponderer la distance euclidienne.
	protected double threshold ; // Le seuil de chaleur a franchir pour qu'un utiliser devienne emmetteur.
	private boolean useMaxThreshold=false ;
	private boolean useMultiThresh = false ;
	private int dynamicThresholdMode = 1 ; // 0 : rien, 1 : th+dynPar*nbInfected, 2 : th*dynPar^nbInfected, 3 : th+dynPar*time 
	private double dynPar=0.1 ; // Un paramètre pour l'évolution du seuil, le cas échéant.
	private boolean noIterModeLearn = false ;
	private boolean noIterModeInfer = false ;
	private double probaPreviousWrong = 0.90 ; // Proba de reprendre l'exemple précédent s'il a été mal classé (loss > 0)
	private boolean naiveLinkInit = false ;
	
	private double parInf ;  // Valeur min des embedding
	private double parSup ; // Valeur max.
	
	
	private String fileLogMap="logmap" ;
	private int logMapEvery = 100000000 ;
	private int iterationForLog = 0 ;
	private PrintStream psLogMap ;
	private  LineSearch lsearch;
	private boolean firstForward=true ; 
	
	
	
	private double maxTimeOffsetLearning=0 ;
	private boolean usePseudoHeat=true ;
	private int pseudoHeatMode = 1 ;
	private HashMap<String,HashMap<String,Double>> allHeat ;
	
	
	
	protected HashMap<String,CPUParams> user_modules_1=new HashMap<String, CPUParams>(); // Les embedding des utilisateurs
	protected HashMap<String,CPUParams> user_modules_2=new HashMap<String, CPUParams>(); 
	protected HashMap<String,CPUParams> users_thresh = new HashMap<String, CPUParams>();
	
	
	// Les modules utilises pour le forward.
	protected CPUHeatDist heat; 
	private CPUParams ztargets;
	private CPUParams zsources;
	private TableModule input_table;
	private CPUHingeLoss loss;
	private CPUMinusParams minusParam ;
	
	private boolean biasedLearn = true; 
	
	// En inférence, valeur des timestamp initiaux, finaux, et le pas.
	private double init_t_inf;
	private double final_t_inf;
	private double step_t_inf;
	private CPUAddVals thresholdModule;
	
	
	
	public MLPDiffSumThreshold(String model_file, int nbDims, double threshold, boolean dualPoint,boolean useDiag, double parinf2, double parsup2, boolean usePseudoHeat, boolean useMultiThresh, int dynamicThreshMode, double dynamicTreshPar, boolean noItermodeLearn, boolean noItermodeInfer) {
		super(model_file);
		this.parInf=parinf2 ;
		this.parSup=parsup2 ;
		this.nbDims = nbDims ;
		this.dualPoint=dualPoint ;
		this.threshold=threshold;
		this.withDiag=useDiag ;
		this.usePseudoHeat=usePseudoHeat ;
		this.useMultiThresh = useMultiThresh ;
		this.dynamicThresholdMode=dynamicThreshMode ;
		this.dynPar=dynamicTreshPar ;
		this.noIterModeLearn=noItermodeLearn ;
		this.noIterModeInfer=noItermodeInfer ;
	}
	
	public void learn(String db, String cascadesCollection, long step, int nbInitSteps,Optimizer optim, int minibatchsize, int maxiters) {
		
		if(model_file.length()!=0){
			if(!loaded){try {
				load();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}}
		}

		String format = "dd.MM.yyyy_H.mm.ss";
		
		//contentLocked=false;
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		if(model_name==""){
			//model_file="Results/Chains/MLPDiffChain_Dims-"+nbDims+"_step-"+step+"_nbInit-"+nbInitSteps+"_db-"+db+"_cascadesCol-"+cascadesCollection+"/MLPdiffusion_"+formater.format(date);
			model_name="Results/Chains/MLPDiffChain_Dims-"+nbDims+"_step-"+step+"_nbInit-"+nbInitSteps+"_db-"+db+"_cascadesCol-"+cascadesCollection+"/MLPdiffusion_"+formater.format(date);
		}
		System.out.println("learn : "+model_name);
		
		if(db.compareTo("test")==0){
			test();	
		}
		else{
			
			//prepareLearning(db, cascadesCollection, step, nbInitSteps);
			PropagationStructLoader psl = new PropagationStructLoader(db, cascadesCollection,1, noIterModeLearn?0.3:0.3 , noIterModeLearn?-1:-1) ;
			psl.load();
			prepareLearning(psl) ;
		}
		System.out.println("learn : "+model_name);
		
		this.construct();

		//optim.optimize(this);
		
		// Calcul de la durée max des cascades, pour fixer les variables final_t_inf, step_t_inf et maxTimeOffsetLearning 
		PropagationStruct ps;
		this.init_t_inf=0 ;
		this.final_t_inf= 0;
		for(int cascade_id : this.cascades_ids) {
			PropagationStruct ps1 = this.train_cascades.get(cascade_id) ;
			HashMap<String, Long> inf = ps1.getInfectionTimes() ;
			for(String u : inf.keySet()) {
				this.final_t_inf = this.final_t_inf > inf.get(u) ? this.final_t_inf : inf.get(u) ;
			}
		}
		this.final_t_inf = this.final_t_inf / 1000 ;
		this.step_t_inf = (final_t_inf - init_t_inf) / 100 ;
		this.maxTimeOffsetLearning = 10*step_t_inf ;
		System.out.println(final_t_inf);
		System.out.println(step_t_inf);
		
		//System.out.println("===>>>>>>>>>>>>>>>>> "+this.maxTimeOffsetLearning); 
		
		try {
			this.save();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(naiveLinkInit) {
				quickEmbedNaiveLink(db,cascadesCollection) ;
		}
		System.out.println("GO LEARNING !");
		optim.optimize(this, 0.000001, maxiters, false);
		
		
	}
	
	
	private void quickEmbedNaiveLink(String db, String cascadesCollection){
		
		if(dualPoint)
			throw new RuntimeException("NaiveLinkInit not implemented with dualpoint") ;
		System.out.println("Starting naivelinkInit");
		NaiveLink m = new NaiveLink("prelearnNaive") ;
		PropagationStructLoader pslnaive = new PropagationStructLoader(db, cascadesCollection, db.equals("enronAll") ? 86400:1,0.3,0);
 		m.learn(pslnaive, db, "users_1", 1, false);
 		HashMap<String,HashMap<String,Double>> p = m.getProbas() ;
 		// Tirer 100 utilisateurs.
 		/*HashSet<String> refUsers = new HashSet<String>() ;
 		String[] list=p.keySet().toArray(new String[1]) ;
 		
 		Random rng = new Random() ;
 		while(refUsers.size()<nbDims) {
 			refUsers.add(list[rng.nextInt(users.size())]) ;
 		}
 		for(String user : this.user_modules_1.keySet()) {
 			CPUParams pars = this.user_modules_1.get(user) ;
 			int i = 0 ;
 			for(String dim : refUsers) {
 				double v = p.get(user).containsKey(refUsers) ? p.get(user).get(refUsers) : 0.0 ;
 				v = (v)*parSup ;
 				pars.getParamList().getParams().get(i).setVal(v); 
 				i++ ;
 			}
 		}*/
 		for(int x=0 ; x<3 ; x++) {
	 		for(String u1 : p.keySet()) {
	 			ArrayList<Parameter> par1 = user_modules_1.get(u1).getParamList().getParams() ;
	 			for(int i = 0 ; i <this.nbDims ; i++) {
						par1.get(i).setVal(0.0);
				}
	 			for(String u2 : p.get(u1).keySet()) {
	 				ArrayList<Parameter> par2 = user_modules_1.get(u2).getParamList().getParams() ;
	 				for(int i = 0 ; i <this.nbDims ; i++) {
	 					double v = par1.get(i).getVal() ;
	 					v+=par2.get(i).getVal()/p.get(u1).size() ;
	 					par1.get(i).setVal(v);
	 				}
	 			}
	 		}
 		}
		
	}
	
	// Fonction permettant de contruire la structure de modules du modele.
	private void construct() {
	
		if(this.useMultiThresh && this.dynamicThresholdMode>0) {
			throw new RuntimeException("Error : multithresh with dynaThresh not implemented yet.") ;
		}
		
		// Création de tout les CPUparams des embeddings.
		for(String user:users){
			int nu = 0 ;
			nu++;
			
			if(!user_modules_1.containsKey(user)){
				CPUParams mod=new CPUParams(1,nbDims);
				mod.setName(user+"in");
				//System.out.println("CONSTRUIT");
				params.allocateNewParamsFor(mod, parInf, parSup); //,(1.0f*nu)/(1.0f*users.size()));
				user_modules_1.put(user,mod);
			}
			if(dualPoint && !user_modules_2.containsKey(user)) {
				CPUParams mod=new CPUParams(1,nbDims);
				mod.setName(user+"out");
				params.allocateNewParamsFor(mod, parInf, parSup); //,(1.0f*nu)/(1.0f*users.size()));
				user_modules_2.put(user,mod);
			}
			if(useMultiThresh) {
				CPUParams mod = new CPUParams(1,1) ;
				mod.setName(user+"thresh");
				params.allocateNewParamsFor(mod, 0, threshold);
				users_thresh.put(user, mod) ;
			}
		}
		
		// La table avec le module des sources et celui de LA cible
		input_table = new TableModule() ;
		input_table.addModule(null);
		input_table.addModule(null);
		
		if(this.usePseudoHeat) {
			heat = new CPUPseudoHeat(this.nbDims,withDiag,pseudoHeatMode) ;
		} else
			heat = new CPUHeatDist(this.nbDims,withDiag,5.0) ;
		
		if(withDiag) {
			params.allocateNewParamsFor(heat,0.1, 1);
			System.out.println("Nb Params heat : "+heat.getNbParams());
		} 
		
		System.out.println("Params total : "+params.getParams().size());
		
		loss = new CPUHingeLoss(1) ;
	
		global = new SequentialModule() ;
		global.addModule(input_table); // Table module contenant les cpuParams des sources et de la cible
		global.addModule(heat); // Calcul des chaleurs reçues
		global.addModule(new CPUAverageRows(1,2)); // Somme des chaleurs reçu.
		//global.addModule(new CPUSmoothMaxRows(1, 2.0)) ;
		if(!useMultiThresh){
			this.thresholdModule = new CPUAddVals(1, -threshold) ;
			global.addModule(thresholdModule); // Soustraire le treshold
		}else {
			this.minusParam = new CPUMinusParams() ;
			global.addModule(minusParam);
		}
		global.addModule(loss); // hingeloss
		
		
	}
	
	@Override
	public int infer(Structure struct) {
		
		/*if(inferDebug(struct)==0)
			return 0 ;*/
		
		if(usePseudoHeat) {
			return inferPseudoHeat(struct) ;
		} else
			return inferActualHeat(struct) ;
	}
	
	
	/*private int inferDebug(Structure struct) {
		// TODO Auto-generated method stub
		PropagationStruct pstruct = (PropagationStruct)struct ;
		try {
			FileWriter fw = new FileWriter("sizes",true) ;
			PrintWriter pw = new PrintWriter(fw) ;
			pw.println(pstruct.getArrayContamined().size()) ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0 ;
	}*/

	public int inferActualHeat(Structure struct) {
		
		if(useMultiThresh) {
			throw new RuntimeException("Error : useMultiThresh not implemented with ActualHeat (should be easy to do, tho...") ;
		} 
		if(dynamicThresholdMode>0) {
			throw new RuntimeException("Error : dynamicThresholdModel not implemented with ActualHeat (should be easy to do, tho...") ;
		}
		if(noIterModeInfer) {
			throw new RuntimeException("Error : noIterModeInfer not implemented with actualHeat (should be easy to do tho...") ;
		}
		
		PropagationStruct ps = (PropagationStruct)struct ;
		ArrayList<String> initials  = ps.getArrayInit() ;
		
		HashMap<String,Double> infectionsTimes = new HashMap<String, Double>() ; // C'est dans ce hashmap qu'on stock les nom des utilisateurs infecte et leur temps d'infection.
		// Mettre deja les gars initiaux dans infectionsTimes.
		for(String u : initials) { 
			infectionsTimes.put(u,0.0) ;
		}
		
		// Ensuite, on itere sur le temps entre init_t et final_t pour calculer les infection au fur et à mesure.
		for(double t = this.init_t_inf+this.step_t_inf ; t<this.final_t_inf  ; t=t+this.step_t_inf) {
			System.out.println(t);

			for(String target : this.users) { /// Parcourir les gars non infectés
				if(infectionsTimes.containsKey(target)) {
					continue ;
				}
				// Pour chaque gars non infecté, on calcule la chaleur atteinte a l'instant t.
				// Pour ça on fait la somme des chaleur reçues.
				double currentTargetHeat = 0.0 ;
				for(String source : infectionsTimes.keySet()) {
					if(!this.users.contains(source)) {
						//System.out.println("Source user "+source+" unknown.");
						continue;
					}
					if(infectionsTimes.get(source)!=t) {
						CPUParams zsource = this.dualPoint ? user_modules_2.get(source) : user_modules_1.get(source) ;
						CPUParams ztarget = user_modules_1.get(target) ;
						double h ;
						if(!usePseudoHeat) {
							h = this.heat.heatkernel(this.nbDims, zsource, ztarget, t-infectionsTimes.get(source)) ;
						} else {
							h=this.allHeat.get(source).get(target) ;
							//System.out.println("H="+h);
						}
						//h = this.heat.heatkernel(this.nbDims, zsource, ztarget, t-infectionsTimes.get(source)) ;

						//currentTargetHeat += this.heat.heatkernel(this.nbDims, zsource, ztarget, t-infectionsTimes.get(source)) ;
						currentTargetHeat += h ;
						//currentTargetHeat = currentTargetHeat < h ? h : currentTargetHeat ;
					}
					if(currentTargetHeat > this.threshold) {
						break ;
					}
				}
				// Si le gars depasse le seuil, on l'ajoute.
				if(currentTargetHeat > this.threshold) {
					infectionsTimes.put(target, t) ;
				}
				
			}
		}
		
		// Mettre le résultat de l'inférence dans la structure.
		TreeMap<Long, HashMap<String, Double>> inf = new TreeMap<Long, HashMap<String,Double>>();
		
		inf.put(0l, new HashMap<String,Double>()) ;
		inf.put(1l, new HashMap<String,Double>()) ;
		for(String u : infectionsTimes.keySet()) {
				double score=1/infectionsTimes.get(u) ;
				inf.get(1l).put(u,score) ;
				//System.out.println("Score :"+score);
		}
		
		ps.setInfections(inf);
		
		return 0;
	}
	
	public int inferPseudoHeatIter(Structure struct) {
		PropagationStruct ps = (PropagationStruct)struct ;
		ArrayList<String> initials  = ps.getArrayInit() ;
		
		HashMap<String,Double> currentHeatReachedsOnUninfected = new HashMap<String, Double>() ;
		for(String u : this.users) {
			currentHeatReachedsOnUninfected.put(u,0.0) ;
		}
		
		HashMap<String,Double> infectionsTimes = new HashMap<String, Double>() ; // C'est dans ce hashmap qu'on stock les nom des utilisateurs infecte et leur temps d'infection.
		// Mettre deja les gars initiaux dans infectionsTimes.
		ArrayList<String> newlyInfected = new ArrayList<String>() ;
		for(String u : initials) { 
			infectionsTimes.put(u,0.0) ;
			newlyInfected.add(u) ;
			currentHeatReachedsOnUninfected.remove(u) ;
		}
		return 0 ;
	}

	public int inferPseudoHeat(Structure struct) {
		
		
		
		PropagationStruct ps = (PropagationStruct)struct ;
		ArrayList<String> initials  = ps.getArrayInit() ;
		
		HashMap<String,Double> currentHeatReachedsOnUninfected = new HashMap<String, Double>() ;
		for(String u : this.users) {
			currentHeatReachedsOnUninfected.put(u,0.0) ;
		}
		
		HashMap<String,Double> infectionsTimes = new HashMap<String, Double>() ; // C'est dans ce hashmap qu'on stock les nom des utilisateurs infecte et leur temps d'infection.
		// Mettre deja les gars initiaux dans infectionsTimes.
		ArrayList<String> newlyInfected = new ArrayList<String>() ;
		for(String u : initials) { 
			infectionsTimes.put(u,0.0) ;
			newlyInfected.add(u) ;
			currentHeatReachedsOnUninfected.remove(u) ;
		}
		
		// Ensuite, on itere sur le temps entre init_t et final_t pour calculer les infection au fur et à mesure.
		boolean keepOnGoing=true  ;
		for(double t = this.init_t_inf+this.step_t_inf ; t<this.final_t_inf && keepOnGoing  ; t=t+this.step_t_inf) {
			keepOnGoing = false ;
			//System.out.println(t);
			ArrayList<String> newlyNewlyInfected = new ArrayList<String>() ;
			for(String target : currentHeatReachedsOnUninfected.keySet()) { /// Parcourir les gars non infectés
				double currentThresh = useMultiThresh ? users_thresh.get(target).getParameters().getMatrix(0).getValue(0, 0) : this.threshold ;
				
				switch(dynamicThresholdMode)  {
				case 1 :
					currentThresh=currentThresh+dynPar*infectionsTimes.size() ;
					break ;
				case 2 :
					currentThresh=currentThresh*Math.pow(dynPar,infectionsTimes.size()) ;
					break ;
				case 3 :
					currentThresh=currentThresh+dynPar*t ;
				}
					
				if(infectionsTimes.containsKey(target)) {
					continue ;
				}
				// Pour chaque gars non infecté, on calcule la chaleur atteinte a l'instant t.
				// Pour ça on fait la somme des chaleur reçues.
				double currentTargetHeat = currentHeatReachedsOnUninfected.get(target) ;
				for(String source : newlyInfected) {
					if(!this.users.contains(source)) {
						//System.out.println("Source user "+source+" unknown.");
						continue;
					}
					if(infectionsTimes.get(source)!=t) {
						//CPUParams zsource = this.dualPoint ? user_modules_2.get(source) : user_modules_1.get(source) ;
						//CPUParams ztarget = user_modules_1.get(target) ;
						double h ;
						
						h=this.allHeat.get(source).get(target) ;
							//System.out.println("H="+h);
						
						//h = this.heat.heatkernel(this.nbDims, zsource, ztarget, t-infectionsTimes.get(source)) ;

						//currentTargetHeat += this.heat.heatkernel(this.nbDims, zsource, ztarget, t-infectionsTimes.get(source)) ;
						currentTargetHeat += h ;
						//currentTargetHeat = currentTargetHeat < h ? h : currentTargetHeat ;
					}
					if(!noIterModeInfer && currentTargetHeat > currentThresh && (!this.useMaxThreshold | currentTargetHeat< this.threshold*2)) {
						break ;
					}
				}
				// Si le gars depasse le seuil, on l'ajoute.
				if(currentTargetHeat > currentThresh && (!this.useMaxThreshold | currentTargetHeat< this.threshold*2)) {
					keepOnGoing = true ;
					if(noIterModeInfer)
						infectionsTimes.put(target, currentTargetHeat) ;
					else
						infectionsTimes.put(target, t) ;
					newlyNewlyInfected.add(target) ;
					
				} else {
					currentHeatReachedsOnUninfected.put(target,currentTargetHeat) ;
				}
				
			}
			for(String newu : newlyNewlyInfected) {
				currentHeatReachedsOnUninfected.remove(newu) ;
			}
			
			newlyInfected=newlyNewlyInfected ;
			if(noIterModeInfer)
				keepOnGoing = false ;
		}
		
		// Mettre le résultat de l'inférence dans la structure.
		TreeMap<Long, HashMap<String, Double>> inf = new TreeMap<Long, HashMap<String,Double>>();
		
		inf.put(0l, new HashMap<String,Double>()) ;
		inf.put(1l, new HashMap<String,Double>()) ;
		for(String u : infectionsTimes.keySet()) {
			if(noIterModeInfer) {
				double score=infectionsTimes.get(u) ;
				inf.get(1l).put(u,score) ;
			}else {
				double score=1/infectionsTimes.get(u) ;
				inf.get(1l).put(u,score) ;
			}
				//System.out.println("Score :"+score);
		}
		
		ps.setInfections(inf);
		
		return 0;
	}
	
	private void computeAllHeat() {
		
		this.allHeat = new HashMap<String, HashMap<String,Double>>() ;
		for(String userSource : this.users) {
			HashMap<String,Double> h=new HashMap<String, Double>() ;
			for(String userTarget : this.users) {
				if(userSource==userTarget)
					continue ;
				CPUParams zsource = this.dualPoint ? user_modules_2.get(userSource) : user_modules_1.get(userSource) ;
				CPUParams ztarget = user_modules_1.get(userTarget) ;
				double heat = this.heat.heatkernel(this.nbDims, zsource, ztarget, 1.0) ;
				h.put(userTarget,heat) ;
				//System.out.println(heat);
			}
			this.allHeat.put(userSource, h) ;
		}
	}

	@Override
	public void load() throws IOException {
		if(loaded)
			return ;
		File file=new File(model_file);
    	BufferedReader f = new BufferedReader(new FileReader(file)) ;
    	
    	
    	f.readLine() ; // <MODEL>
    	this.step=f.readLine().substring(5) ;
    	//this.nbInitSteps=Integer.parseInt(f.readLine().substring(12)) ;
    	this.nbDims=Integer.parseInt(f.readLine().substring(7)) ;
    	this.dualPoint=Boolean.parseBoolean(f.readLine().substring(10)) ;
    	this.threshold=Double.parseDouble(f.readLine().substring(10)) ;
    	this.withDiag=Boolean.parseBoolean(f.readLine().substring(9)) ;
    	this.init_t_inf=Double.parseDouble(f.readLine().substring(11)) ;
		this.final_t_inf=Double.parseDouble(f.readLine().substring(12));
		this.step_t_inf=Double.parseDouble(f.readLine().substring(11)) ;
		this.maxTimeOffsetLearning= Double.parseDouble(f.readLine().substring(22));
		this.useMultiThresh=Boolean.parseBoolean(f.readLine().substring(15)) ;
		this.dynamicThresholdMode=Integer.parseInt(f.readLine().substring(21)) ;
		this.dynPar=Double.parseDouble(f.readLine().substring(7)) ;
		this.noIterModeLearn=Boolean.parseBoolean(f.readLine().substring(16)) ;
		this.noIterModeInfer=Boolean.parseBoolean(f.readLine().substring(16)) ;
		
		//System.out.println(this.step);
		//System.out.println(this.nbInitSteps);
		System.out.println("Nbdims = "+this.nbDims);
		System.out.println("DualPOint ="+this.dualPoint);
		//System.out.println(this.threshold);
		System.out.println("WithDiag ="+this.withDiag);
		//System.out.println(this.init_t_inf);
		//System.out.println(this.final_t_inf);
		//System.out.println(this.step_t_inf);
		//System.out.println(this.maxTimeOffsetLearning);
    	
    	f.readLine() ; // </MODEL>
		 
		f.readLine() ; // <USERS>
		this.users = new ArrayList<String>() ;
    	for(String s = f.readLine() ; s.compareTo("</USERS>")!=0 ; s=f.readLine()) {
    		this.users.add(s) ;
    	}
    	//System.out.println(this.users.size());
    	
    	construct();
    	
    	f.readLine() ; // <PARAMETERS>
    	int nbpar = 0 ;
 		ArrayList<Parameter> thisparams = this.params.getParams() ;
 		for(String s = f.readLine() ; s.compareTo("</PARAMETERS>")!=0 ; s=f.readLine()) {
    		thisparams.get(nbpar).setVal(Float.parseFloat(s)) ;
    		//System.out.println(Float.parseFloat(s));
    		nbpar++ ;
    	}

 		int expected =  nbDims*this.users.size()*(dualPoint ? 2 : 1)+(withDiag ? nbDims : 0);
 		if(nbpar != expected) {
 			throw new IOException("Wrong number of parameters in file"+model_file+". Expected "+expected+", found "+nbpar) ;
 		}
    	
 		/*prepareLearning("digg", "cascades_1", step, nbInitSteps);
 		PropagationStruct ps;
		this.init_t_inf=0 ;
		this.final_t_inf= 0;
		for(int cascade_id : this.cascades_ids) {
			PropagationStruct ps1 = this.train_cascades.get(cascade_id) ;
			HashMap<String, Long> inf = ps1.getInfectionTimes() ;
			for(String u : inf.keySet()) {
				this.final_t_inf = this.final_t_inf > inf.get(u) ? this.final_t_inf : inf.get(u) ;
			}
		}
		this.final_t_inf = this.final_t_inf / 1000 ;
		this.step_t_inf = (final_t_inf - init_t_inf) / 100 ;
		this.maxTimeOffsetLearning = 10*step_t_inf ;*/
 		
 		loaded=true ;
 		System.out.println("Opened : "+nbpar);

	}

	@Override
	public void save() throws FileNotFoundException {
		
		File file=new File(model_file);
    	File dir = file.getParentFile();
    	if(dir!=null){
    		dir.mkdirs();
    	}
    	
    	PrintStream p = new PrintStream(file) ;
    	p.println("<MODEL="+this+">");
    	p.println("step="+this.step);
    	//p.println("nbInitSteps="+this.nbInitSteps);
    	p.println("nbDims="+this.nbDims) ;
		p.println("dualPoint="+this.dualPoint) ;
		p.println("threshold="+this.threshold) ;
		p.println("withDiag="+this.withDiag) ;
		p.println("init_t_inf="+this.init_t_inf) ;
		p.println("final_t_inf="+this.final_t_inf) ;
		p.println("step_t_inf="+this.step_t_inf) ;
		p.println("maxTimeOffsetLearning="+this.maxTimeOffsetLearning) ;
		p.println("useMultiThresh="+useMultiThresh); ;
		p.println("dynamicThresholdMode="+this.dynamicThresholdMode) ;
		p.println("dynPar="+this.dynPar) ;
		p.println("noIterModelLearn="+this.noIterModeLearn) ;
		p.println("noIterModelInfer="+this.noIterModeInfer) ;
		p.println("</MODEL>") ;
		
		p.println("<USERS>") ;
    	for(String u : this.users) {
    		p.println(u) ;
    	}
    	p.println("</USERS>") ;
    	
    	p.println("<PARAMETERS>") ;
    	//int nbp=0 ;
    	System.out.println("Saving " +this.params.getParams().size());
    	for(Parameter par :this.params.getParams()) {
    		p.println(par.getVal()) ;
    	//	nbp++ ;
    	}
    	p.println("</PARAMETERS>") ;
	}
	
	
	
	/*public void forward() {
		if (this.noIterMode)
			forwardNoIter() ;
		else
			forwardNormal() ;
	}*/
	
	
	public void forwardIterNew() {
		
		System.out.println("Forward...");
		
		Random rng = new Random() ;
		
		if(!firstForward && this.probaPreviousWrong>0 && this.getOutput().getMatrix(0).getValue(0, 0) > 0 && rng.nextDouble()<this.probaPreviousWrong) {
			global.forward(null) ;
		}
		
		// Eventuellement, loguer.
		if(logMapEvery>0) {
			if(iterationForLog==0) {
				File file=new File(fileLogMap);
		    	File dir = file.getParentFile();
		    	if(dir!=null){
		    		dir.mkdirs();
		    	}
		    	
		    	try {
					psLogMap = new PrintStream(file) ;
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					throw new RuntimeException("Can't open "+fileLogMap) ;
				}
			}
			iterationForLog++ ;
			if(iterationForLog%logMapEvery == 0){
				computeAllHeat();
				psLogMap.println(this.evaltest(train_cascades)) ;
				File f = new File("semaphoreLine") ;
				if(f.exists()) {
			    	try {
						BufferedReader fb = new BufferedReader(new FileReader(f)) ;
						this.lsearch.setLine(Double.parseDouble(fb.readLine()));
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException("Can't open semaphoreline") ;
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException("Can't read semaphoreline") ;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException("Can't open semaphoreline") ;
					}
					
				}
			}
		}
		
		// Tirage d'une cascade
		int id_cascade ;
		do {
			id_cascade = this.cascades_ids.get(rng.nextInt(this.cascades_ids.size())) ;
		}while(this.train_cascades.get(id_cascade).getArrayContamined().size()<2 || this.train_cascades.get(id_cascade).getArrayContamined().size()<=this.train_cascades.get(id_cascade).getArrayInit().size() );
		PropagationStruct ps = this.train_cascades.get(id_cascade) ;
		HashMap<String, Long> infections_times = ps.getInfectionTimes() ;
		
		String[] infectedarray = infections_times.keySet().toArray(new String[1]) ;
		String targetUser ;
		
		System.out.println(infections_times.keySet().size());
		System.out.println(ps.getArrayInit().size());
		System.out.println(infections_times.keySet());
		System.out.println(ps.getArrayInit());
		
		if(biasedLearn) { // Tirer soit un gars de la cascade, soit un gars PAS dans la cascade (une chance sur 2).
			if(rng.nextBoolean() ||(infectedarray.length==this.users.size())) {
				System.out.println("IN");
				do {
					targetUser = infectedarray[rng.nextInt(infectedarray.length)] ;
				//} while(infections_times.get(targetUser)<2) ;
				} while(ps.getArrayInit().contains(targetUser)) ;
				
			} else {
				System.out.println("OUT "+users.size());
				do {
					targetUser = users.get(rng.nextInt(users.size())) ;
				} while(infections_times.containsKey(targetUser)) ;
				
	
			}
		} else {
			do {
				targetUser = users.get(rng.nextInt(users.size())) ;
			//} while(infections_times.containsKey(targetUser) && infections_times.get(targetUser)<2 ) ;
			} while(infections_times.containsKey(targetUser) && ps.getArrayInit().contains(targetUser)) ;
		}
		
		// Construction des inputs.
		// Il faut mettre les n sources dans la première moitié d'un tableModule
		// Et mettre n fois l'utilisateur cible dans l'autre moitié d'un tableModule.
		CPUParams postarget = user_modules_1.get(targetUser) ;
		boolean targetIsInCascade = infections_times.containsKey(targetUser) ;
		

		if(targetIsInCascade) { // Si la cible est infectée...
			CPUMatrix mlabels = new CPUMatrix(1,1,1.0) ;
			loss.setLabels(new Tensor(mlabels));
			if(!noIterModeLearn) ;
				//infection_times_target=infections_times.get(targetUser) ;
				//TODO
		}
				
	}
	
	
	@Override
	public void forward() {
		forwardOld() ;
	}
	
	
	public void forwardOld() {
		
		System.out.println("Forward...");
		
		Random rng = new Random() ;
		
		if(!firstForward && this.probaPreviousWrong>0 && this.getOutput().getMatrix(0).getValue(0, 0) > 0 && rng.nextDouble()<this.probaPreviousWrong) {
			global.forward(null) ;
		}
		
		// Eventuellement, loguer.
		if(logMapEvery>0) {
			if(iterationForLog==0) {
				File file=new File(fileLogMap);
		    	File dir = file.getParentFile();
		    	if(dir!=null){
		    		dir.mkdirs();
		    	}
		    	
		    	try {
					psLogMap = new PrintStream(file) ;
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					throw new RuntimeException("Can't open "+fileLogMap) ;
				}
			}
			iterationForLog++ ;
			if(iterationForLog%logMapEvery == 0){
				computeAllHeat();
				psLogMap.println(this.evaltest(train_cascades)) ;
				File f = new File("semaphoreLine") ;
				if(f.exists()) {
			    	try {
						BufferedReader fb = new BufferedReader(new FileReader(f)) ;
						this.lsearch.setLine(Double.parseDouble(fb.readLine()));
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException("Can't open semaphoreline") ;
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException("Can't read semaphoreline") ;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						throw new RuntimeException("Can't open semaphoreline") ;
					}
					
				}
			}
		}
		
		// Tirage d'une cascade
		int id_cascade ;
		do {
			id_cascade = this.cascades_ids.get(rng.nextInt(this.cascades_ids.size())) ;
		}while(this.train_cascades.get(id_cascade).getArrayContamined().size()<2 || this.train_cascades.get(id_cascade).getArrayContamined().size()<=this.train_cascades.get(id_cascade).getArrayInit().size() );
		PropagationStruct ps = this.train_cascades.get(id_cascade) ;
		HashMap<String, Long> infections_times = ps.getInfectionTimes() ;
		
		String[] infectedarray = infections_times.keySet().toArray(new String[1]) ;
		String targetUser ;
		
		System.out.println(infections_times.keySet().size());
		System.out.println(ps.getArrayInit().size());
		System.out.println(infections_times.keySet());
		System.out.println(ps.getArrayInit());
		
		if(biasedLearn) { // Tirer soit un gars de la cascade, soit un gars PAS dans la cascade (une chance sur 2).
			if(rng.nextBoolean() ||(infectedarray.length==this.users.size())) {
				System.out.println("IN");
				do {
					targetUser = infectedarray[rng.nextInt(infectedarray.length)] ;
				//} while(infections_times.get(targetUser)<2) ;
				} while(ps.getArrayInit().contains(targetUser)) ;
				
			} else {
				System.out.println("OUT "+users.size());
				do {
					targetUser = users.get(rng.nextInt(users.size())) ;
				} while(infections_times.containsKey(targetUser)) ;
				
	
			}
		} else {
			do {
				targetUser = users.get(rng.nextInt(users.size())) ;
			//} while(infections_times.containsKey(targetUser) && infections_times.get(targetUser)<2 ) ;
			} while(infections_times.containsKey(targetUser) && ps.getArrayInit().contains(targetUser)) ;
		}
		
		// Si on a pris un gars de la cascade, on veut qu'il recoive assez des chaleur des gars infectés avant lui, à un instant quelconque.
		// Si on a pris un gars PAS dans la cascade, on veut qu'il ne recoive pas assez des chaleur des gars infectés avant lui, à un instant quelconque.
		
		ArrayList<Double> time=new ArrayList<Double>() ;
		// Construction des inputs.
		// Il faut mettre les n sources dans la première moitié d'un tableModule
		// Et mettre n fois l'utilisateur cible dans l'autre moitié d'un tableModule.
		CPUParams postarget = user_modules_1.get(targetUser) ;
		
		
		boolean targetIsInCascade = infections_times.containsKey(targetUser) ;
		double infection_times_target=-1 ;
		
		
		if(noIterModeLearn) { // Si on est en mode non iter, on va virer du infectionTimes toutes les non-sources
			HashMap<String,Long> new_infections_times = new HashMap<String, Long>() ;
			if(targetIsInCascade)
				infection_times_target=infections_times.get(targetUser) ;;
			for(String init : ps.getArrayInit()) {
				new_infections_times.put(init, infections_times.get(init)) ;
			}
			infections_times=new_infections_times ;
		}
		
		if(targetIsInCascade) { // Si la cible est infectée...
			CPUMatrix mlabels = new CPUMatrix(1,1,1.0) ;
			loss.setLabels(new Tensor(mlabels));
			if(!noIterModeLearn)
				infection_times_target=infections_times.get(targetUser) ;
			
			// Compter le nombre de source et trouver laquelle est infectée le plus tard.
			int nbPrevious = 0 ;
			double latestTime = 0.0 ;
			for(String infected : infections_times.keySet()) {
				if(infection_times_target>infections_times.get(infected)) {
					nbPrevious++;
					if(latestTime<infections_times.get(infected))
						latestTime=infections_times.get(infected);
				}
			}
			// Tirer un temps situé APRES l'infection des utilisateur précédents.
			//double t = latestTime+rng.nextDouble()*this.maxTimeOffsetLearning+1 ;
			double t = latestTime+this.final_t_inf/2 ;
			
			// Modifier peut etre le threshold
			switch(dynamicThresholdMode) {
				case 1 : 
					this.setDynaThresholdVal(this.threshold+dynPar*nbPrevious);
					break ;
				case 2 :
					this.setDynaThresholdVal(this.threshold*Math.pow(dynPar,nbPrevious));
					break ;
				case 3 :
					this.setDynaThresholdVal(this.threshold+dynPar*infection_times_target);
					break ;
			}
			
			// Mettre les CPU et le t dans les modules.
			ztargets = new CPUParams(nbPrevious,nbDims) ;
			zsources = new CPUParams(nbPrevious,nbDims) ;
			if(useMultiThresh){
				this.minusParam.resetParams(nbPrevious);
				this.minusParam.addParam(this.users_thresh.get(targetUser));
			}
			
			for(String infected : infections_times.keySet()) {
				if(infection_times_target>infections_times.get(infected)) {
					CPUParams possource = dualPoint ? user_modules_2.get(infected) : user_modules_1.get(infected) ;
					ztargets.addParametersFrom(postarget);
					zsources.addParametersFrom(possource);
					time.add(t-infections_times.get(infected)) ;
				}
			}
		} else { // Si la cible n'est pas infectée...
			
			// On fait presque la meme chose qu'au dessus.
			CPUMatrix mlabels = new CPUMatrix(1,1,-1.0) ;
			loss.setLabels(new Tensor(mlabels));
			ztargets = new CPUParams(infections_times.keySet().size(),nbDims) ;
			zsources = new CPUParams(infections_times.keySet().size(),nbDims) ;

			double latestTime = 0.0 ;
			for(String infected : infections_times.keySet()) {
						if(latestTime<infections_times.get(infected))
							latestTime = infections_times.get(infected) ;
			}
			if(useMultiThresh){
				this.minusParam.resetParams(infections_times.keySet().size());
				this.minusParam.addParam(users_thresh.get(targetUser));
			}
			double t = latestTime+rng.nextDouble()*this.maxTimeOffsetLearning+1 ;
			for(String infected : infections_times.keySet()) {
				CPUParams possource = dualPoint ? user_modules_2.get(infected) : user_modules_1.get(infected) ;
				ztargets.addParametersFrom(postarget);
				zsources.addParametersFrom(possource);
				time.add(t-infections_times.get(infected)) ;
			}
		}
		
		// Les input et le tableau des "t" sont constuits, on peut les mettre et lancer le forward.
		this.input_table.setModule(0,ztargets);
		this.input_table.setModule(1,zsources);
		this.heat.setT(time);

	
		global.forward(null); //
		this.firstForward=false ;
	}

	public void setDynaThresholdVal(double t) {
		if(useMultiThresh)
			return ;
		this.thresholdModule.setVals(new CPUMatrix(1,1,-t));
		
	}
	
	@Override
	public void backward() {
		global.backward(null);

	}
	
	// Fonction pour constuire des données jouets.
	public void test(){
		long step=1l;
		int nbInitSteps=1;
		users=new ArrayList<String>();
		users.add("a"); users.add("b"); users.add("c"); users.add("d"); users.add("e");
		users.add("f"); users.add("g"); users.add("h"); users.add("i"); users.add("j");
		/*users.add("k"); users.add("l"); users.add("m"); users.add("n"); users.add("o");
		users.add("p"); users.add("q"); users.add("r"); users.add("s"); users.add("t");
		users.add("u"); users.add("v"); users.add("w"); users.add("x"); users.add("y");*/
		//this.train_users=new HashMap<String,HashMap<Integer,Double>>();
		
		train_cascades=new HashMap<Integer,PropagationStruct>();
		
		//String[] cascades = {"a-b","b-a", "d-e", "e-d", "bd-ace" } ;
		String[] cascades = {"ab-c","a-b","abc-d","efgh-d","ef-h","efh-g","eg-h","i-j","j-i"} ; 		
		int cid = 0;
		for(String c : cascades) {
			TreeMap<Long,HashMap<String,Double>> init=new TreeMap<Long,HashMap<String,Double>>();
			TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
			TreeMap<Integer,Double> diffusion=new TreeMap<Integer,Double>();
			HashMap<String,Double> h=new HashMap<String,Double>();
			for(char user : c.substring(0).toCharArray()) {
				if(user == '-')
					break ;
				h.put(String.valueOf(user), 1.0);
			}
			init.put(1l, h);
			infections.put(1l, h);
			h=new HashMap<String,Double>();
			for(char user : c.substring(c.indexOf('-')+1).toCharArray()) {
				h.put(String.valueOf(user), 1.0);
			}
			infections.put(2l,h);
			diffusion.put(0, 1.0);
			PropagationStruct struct=new PropagationStruct(new Cascade(cid,"Cascade_test_"+cid, null),step,nbInitSteps,init,infections,diffusion);
			train_cascades.put(cid++, struct);
		}
		
		cascades_ids=new ArrayList<Integer>(train_cascades.keySet());
	}
	
	// Fonction pour calculer la map en train sans passer par mongo db (on l'utilise quand on se sert aussi de test().
	public double evaltest(HashMap<Integer,PropagationStruct> cascades_test) {
		
		double totalMAP = 0.0 ;
		MAP map = new MAP(new HashSet<String>(users),true) ;
		for(int i : cascades_test.keySet()) {
				
				Hyp hyp=new Hyp(cascades_test.get(i),this,1);
				if(hyp.getStruct()==null) {
					System.out.println("Fuck");
				}
				Result result=map.eval(hyp) ;
				totalMAP += result.getScores().get("MAP__ignoreInit") ;
		}
		System.out.println("MAP : "+totalMAP/cascades_test.size());
		return totalMAP/cascades_test.size() ;
		
	}
	
	
	public static void main(String args[]) {
		
		int nbDims=10;
		boolean diag = true ;
		double thresh=1.0 ;
		String modelfile = "" ;
		String resultsFile = "tempres.filtered" ;
		String db = "digg" ;
		String cascadetrain = "cascades_1_filtered_trainAll_4_3_true" ;
		String cascadetest = "cascades_1_filtered_test_4_3_true" ;
		double parinf = -5000.0 ;
		double parsup = +5000.0 ;
		//double maxTimeOffsetLearning=  ;
		int maxiters=10000000 ;
		double line = 0.05 ;
		double fact = 1.0 ;
		int batchsize = 30 ;
		boolean dualpoint = false ;
		boolean usePseudoHeat = true ;
		int filter = 0 ;
		boolean useMultiTresh = false;
		int dynamicThreshMode = 0 ;
		double dynamicTreshPar= 0.1 ;
		boolean NoItermodeLearn = false;
		boolean NoItermodeInfer = false;
		
		
		
		/*PrintWriter f =new PrintWriter(new FileWriter(new File("tempcascades"),false)) ;
		f.print("");
		f.close();*/
		//../expDynaThresh.jar 5 false 2.0 model res digg,cascades_1,cascades_2 -5000.0 5000.0 0 50000000 0.2 1.0 30 false true 0 false 1 

		
		if(args.length>0) {
			nbDims = Integer.parseInt(args[0]) ;
			diag = Boolean.parseBoolean(args[1]) ;
			thresh = Double.parseDouble(args[2]) ;
			modelfile = args[3] ;
			resultsFile = args[4] ;
			db = args[5].split(",")[0] ;
			cascadetrain = args[5].split(",")[1] ;
			cascadetest = args[5].split(",")[2] ;
			parinf = Double.parseDouble(args[6]) ;
			parsup = Double.parseDouble(args[7]) ;
			//maxTimeOffsetLearning= Double.parseDouble(args[8]) ;
			maxiters=Integer.parseInt(args[9]) ;
			line = Double.parseDouble(args[10]) ;
			fact = Double.parseDouble(args[11]) ;
			batchsize = Integer.parseInt(args[12]) ;
			dualpoint = Boolean.parseBoolean(args[13]) ; ;
			usePseudoHeat = Boolean.parseBoolean(args[14]) ; ;
			filter = Integer.parseInt(args[15]) ;
			useMultiTresh = Boolean.parseBoolean(args[16]) ;
			dynamicThreshMode=Integer.parseInt(args[17].split(",")[0]) ;
			dynamicTreshPar=Double.parseDouble(args[17].split(",")[1]) ;
			NoItermodeLearn = Boolean.parseBoolean(args[18]) ;
			NoItermodeInfer = Boolean.parseBoolean(args[19]) ;
		}
				
		///*
		
		//*/
		
		MLPDiffSumThreshold m = new MLPDiffSumThreshold(modelfile,nbDims,thresh,dualpoint,diag,parinf,parsup,usePseudoHeat,useMultiTresh,dynamicThreshMode,dynamicTreshPar,NoItermodeLearn,NoItermodeInfer) ;
		
		

		Env.setVerbose(1) ;
		
		/*try {
			m.load();
			m.computeAllHeat();
			ArtificialCascade.genereArtificialCascades(m, "digg", "FakeDigg_max3init", 1000, 3, true);
			return ;
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
				
		m.lsearch=LineSearch.getFactorLine(line,fact); 
 		//DescentDirection dir=DescentDirection.getAverageGradientDirection();
		DescentDirection dir=DescentDirection.getGradientDirection();
		m.learn(db, cascadetrain, 1, 1, Optimizer.getDescent(dir, m.lsearch),batchsize,maxiters);
				
		/*try {
			PrintStream ps = new PrintStream(new File("sizes")) ; 
			for(Integer i : m.cascades_ids) {
				if(i<=1000){
					System.out.println(i);
					
					PropagationStruct p = m.train_cascades.get(i) ;
					ps.println(p.getArrayContamined().size());
				}
				
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		
		
		
		//m.heat.lockParams();
		Date d = new Date() ;
		//m.computeAllTranmissions(); 
		Date d2 = new Date() ;
		System.out.println("TEMPS = "+(d2.getTime()-d.getTime())/1000);
		//m.model_file="tempmod2" ;
		try {
			m.save();
			//m.load();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		if(db=="test") {
			if(m.usePseudoHeat)
				m.computeAllHeat() ;
			
			m.evaltest(m.train_cascades) ;
				
		}else {
			if(m.usePseudoHeat)
				m.computeAllHeat() ;
	
			//EvalPropagationModel.run(new EvalMLPDiffSumThreshold(m,db, cascadetrain,m.users), resultsFile+".train") ;
			EvalPropagationModel.run(new EvalMLPDiffSumThreshold(m,db, cascadetest,m.users), resultsFile+".test") ;
			if(filter>0){
				EvalPropagationModel.run(new EvalMLPDiffSumThreshold(m,db, cascadetest, m.filterUsers(filter)), resultsFile+".test.filter"+filter) ;
				EvalPropagationModel.run(new EvalMLPDiffSumThreshold(m,db, cascadetest, m.filterUsers(2*filter)), resultsFile+".test.filter"+(2*filter)) ;
				EvalPropagationModel.run(new EvalMLPDiffSumThreshold(m,db, cascadetest, m.filterUsers(3*filter)), resultsFile+".test.filter"+(3*filter)) ;
			}
		}
		
	}

	private ArrayList<String> filterUsers(int N) {
		
		HashMap<String,Integer> map = new HashMap<String, Integer>() ;
		for(String u : this.users) {
			map.put(u,0) ;
		}
		for(Integer i : this.train_cascades.keySet() ) {
			PropagationStruct p = this.train_cascades.get(i) ;
			ArrayList<String> inf = new ArrayList<String>(p.getInfectionTimes().keySet()) ;
			for(String user : inf) {
				map.put(user,map.get(user)+1) ;
			}
			
		}
		
		ArrayList<String> newUsers = new ArrayList<String>();
		for(String u : this.users) {
			if(map.get(u) >=N)
				newUsers.add(u) ;
		}
		return newUsers ;
	}

	public HashSet<String> getUsers() {
		return new HashSet<String>(this.users);
	}

	@Override
	public int inferSimulation(Structure struct) {
		infer(struct) ;
		
		PropagationStruct ps = (PropagationStruct)struct ;
		TreeMap<Long, HashMap<String, Double>> inf = ps.getInfections() ;
		TreeMap<Long, HashMap<String, Double>> inf2 = new TreeMap<Long, HashMap<String,Double>>() ;
		HashMap<String, Double> inverseInftimes = inf.get(1l) ;
		System.out.println("InferSimu : size "+inverseInftimes.size());
		for(String user : inverseInftimes.keySet()) {
			double t = 1.0/inverseInftimes.get(user) ;
			long tstep = (long) ((t-this.init_t_inf)/this.step_t_inf);
			if(!inf2.containsKey(tstep))
				inf2.put(tstep,new HashMap<String, Double>()) ;
			if(Math.random()<0.1) {
				inf2.get(tstep).put(user,1.0) ;
			}
			//System.out.println(tstep);
		}
		ps.setInfections(inf2);
		
		
		return 0;
	}



	@Override
	public int getContentNbDims() {
		// TODO Auto-generated method stub
		
		return 0;
	}

	public boolean getNoIterModeInfer() {
		// TODO Auto-generated method stub
		return this.noIterModeInfer;
	}
	
		
}





class EvalMLPDiffSumThreshold extends EvalPropagationModelConfig{

	private MLPDiffSumThreshold mlpst ;
	
	/*public EvalMLPDiffSumThreshold(MLPDiffSumThreshold mlpst,String db,String cascades) {
			this.mlpst=mlpst ;
			
			pars.put("db",db); //usElections5000_hashtag");
			pars.put("cascadesCol",cascades);
			pars.put("step", "1");
			pars.put("nbInitSteps", "1");
			pars.put("nbCascades", "1000");
			pars.put("allUsers", "users_1");
	}*/
	
	public EvalMLPDiffSumThreshold(MLPDiffSumThreshold mlpst,String db,String cascades,ArrayList<String> users) {
		this.mlpst=mlpst ;
		//System.err.println("ATTENTION : INFER DOIT PAS MARCHER SI ON EST PAS EN RATIOINIT");
		pars.put("db",db); //usElections5000_hashtag");
		pars.put("cascadesCol",cascades);
		pars.put("step", "1");
		//pars.put("nbInitSteps", "1");
		pars.put("nbCascades", "1000");
		pars.put("ratioInits",mlpst.getNoIterModeInfer() ? "0.3" : "-1.0") ;
		//pars.put("allUsers", "users_1");
		//this.allUsers=new HashSet<String>(mlpc.getUsers()) ;
		this.allUsers = new LinkedHashSet<String>(users) ;
	}
	
	@Override
	public LinkedHashMap<PropagationModel, Integer> getModels() { 
		LinkedHashMap<PropagationModel,Integer> h=new  LinkedHashMap<PropagationModel, Integer>();
		h.put(mlpst, 1) ;
		return h;
	}

	@Override
	public EvalMeasureList getMeasures() {
		/*HashSet<String> u = new HashSet<String>() ;
		for(String s : mlpst.getUsers()) { // ON EST SUR DE ça ?
			u.add(s) ;
		}*/
		MAP map = new MAP(this.allUsers,true) ;
		ArrayList<EvalMeasure> arrayev=new ArrayList<EvalMeasure>(1) ;
		arrayev.add(map) ;
		EvalMeasureList ev = new EvalMeasureList(arrayev) ;
		return ev;
	}
}


