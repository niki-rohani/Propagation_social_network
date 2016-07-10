package simon.propagationModels;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import mlp.CPUMatrix ;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import mlp.Env ;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import org.bson.NewBSONDecoder;

import cascades.Cascade;
import mlp.CPUAddVals;
import mlp.CPUAverageRows;
import mlp.CPUHingeLoss;
import mlp.CPUMatrixProduct;
import mlp.CPUParams;
import mlp.DescentDirection;
import mlp.LineSearch;
import mlp.Optimizer;
import mlp.Parameter;
import mlp.Parameters;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.Tensor;
import core.Structure;
import experiments.EvalMeasure;
import experiments.EvalMeasureList;
import experiments.EvalPropagationModel;
import experiments.EvalPropagationModelConfig;
import experiments.Hyp;
import experiments.MAP;
import experiments.Result;
import propagationModels.MLP ;
import propagationModels.MLPdiffusion;
import propagationModels.PropagationModel;
import propagationModels.PropagationStruct;
import simon.mlp.CPUHeatDist;
import simon.mlp.CPUL2Dist2;
import simon.mlp.MLPCommunities;

public class MLPDiffChain extends MLP {

	protected int nbDims;
	protected boolean dualPoint;
	private boolean withDiag ;
	protected double threshold ;
	private int minibatchsize ;
	
	private int forwardMode =  0; // 0 : forwardMax, 1 : forwardAll
	
	private double parInf = -100.0 ;
	private double parSup = 100.0 ;
	
	
	
	private HashSet<String> debug = new HashSet<String>() ;
	
	private HashMap<String,HashMap<String,Double>> reachThreashAndWhen ;
	HashMap<String,HashMap<String,Double>> maxHeats = new HashMap<String, HashMap<String,Double>>() ;
	
	protected HashMap<String,CPUParams> user_modules_1=new HashMap<String, CPUParams>();
	protected HashMap<String,CPUParams> user_modules_2=new HashMap<String, CPUParams>();
	
	protected CPUHeatDist heat;
	private CPUParams ztarget;
	private CPUParams zsources;
	private TableModule input_table;
	
	
	
	private ArrayList<String> usedUsersForBackward ;
	private HashMap<String, HashMap<String, Double>> heatThroughThisLink;

	
	public MLPDiffChain(String model_file, int nbDims, double threshold, boolean dualPoint,boolean useDiag, double parinf2, double parsup2, int forwardmode, int batchsize) {
		super(model_file);
		this.parInf=parinf2 ;
		this.parSup=parsup2 ;
		this.nbDims = nbDims ;
		this.dualPoint=dualPoint ;
		this.threshold=threshold;
		this.withDiag=useDiag ;
		this.forwardMode=forwardmode;
		this.minibatchsize = batchsize ;
	
		
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
		
		return 0;
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
		}
		System.out.println("learn : "+model_name);
		
		
		this.minibatchsize=minibatchsize;
		
		this.construct();

		//optim.optimize(this);
		optim.optimize(this, 0.000001, maxiters, false);
	}
	
	private void construct() {
		
		for(String user:users){
			int nu = 0 ;
			nu++;
			
			if(!user_modules_1.containsKey(user)){
				CPUParams mod=new CPUParams(1,nbDims);
				mod.setName(user);
				//System.out.println("CONSTRUIT");
				params.allocateNewParamsFor(mod, parInf, parSup); //,(1.0f*nu)/(1.0f*users.size()));
				user_modules_1.put(user,mod);
			}
			if(dualPoint && !user_modules_2.containsKey(user)) {
				CPUParams mod=new CPUParams(1,nbDims);
				mod.setName(user);
				params.allocateNewParamsFor(mod, parInf, parSup); //,(1.0f*nu)/(1.0f*users.size()));
				user_modules_2.put(user,mod);
			}
		}
		
		
		input_table = new TableModule() ;
		input_table.addModule(null);
		input_table.addModule(null);
		
		heat = new CPUHeatDist(this.nbDims,withDiag) ;
		if(withDiag) {
			params.allocateNewParamsFor(heat,0.1, 1);
		}
		
		// Créer le loss et mettre les labels 1 et -1 en alternance.
		CPUHingeLoss loss = new CPUHingeLoss(1) ;
		CPUMatrix mlabels = new CPUMatrix(minibatchsize*2,1) ;
		for(int i=0 ; i<minibatchsize*2 ; i=i+2) {
			mlabels.setValue(i, 0, 1.0);
			mlabels.setValue(i+1, 0, -1.0);
		}
		loss.setLabels(new Tensor(mlabels));
		
		CPUAverageRows avg = new CPUAverageRows(1,2) ;
		
		global = new SequentialModule() ;
		global.addModule(input_table);
		global.addModule(heat);
		//global.addModule(new CPUL2Dist2(nbDims));
		global.addModule(new CPUAddVals(1, -threshold));
		global.addModule(loss);
		global.addModule(avg);
	}
	
	protected void computeAllTranmissions() {
		this.reachThreashAndWhen = new HashMap<String, HashMap<String,Double>>() ;
		this.heatThroughThisLink = new HashMap<String, HashMap<String,Double>>() ;
		for(String source : this.users) {
			this.reachThreashAndWhen.put(source,new HashMap<String, Double>()) ;
			this.heatThroughThisLink.put(source,new HashMap<String, Double>()) ;
			for(String target : this.users) {
				CPUParams ztarget = user_modules_1.get(target) ;
				CPUParams zsource = dualPoint ? user_modules_2.get(source) : user_modules_1.get(source) ;
				double h = heat.heatmax(nbDims, ztarget, zsource) ;
				this.heatThroughThisLink.get(source).put(target, h) ;
				if(h>=this.threshold) {
					System.out.println("===> "+h);
					double t = heat.timeWhen(nbDims, ztarget, zsource, this.threshold) ;
					this.reachThreashAndWhen.get(source).put(target,t) ;
					if(h==1.1709966304863832) {
						System.out.println(ztarget + ";" +zsource);
					}
				}
			}
		}
	}
	

	public int infer1(Structure struct) {
		
		
		
		PropagationStruct ps = (PropagationStruct)struct ;
		ArrayList<String> initials  = ps.getArrayInit() ;
		
		HashMap<String,Double> infectionsTimes = new HashMap<String, Double>() ;
		for(String u : initials) {
			infectionsTimes.put(u,0.0) ;
		}
		
		
		boolean new_user_added = false ;
		double maxTinfection = 0.0 ;
		
		Set<String> newly_infected_users = infectionsTimes.keySet() ;
		int debugiter =  0 ;
		do {
			System.out.println(debugiter++);
			
			new_user_added = false ;
			HashMap<String,Double> nextinfectionTimes = new HashMap<String,Double>() ;
			
			for(String targetuser : this.users) { // Parcourirs les utilisateurs non infectes
				if(infectionsTimes.containsKey(targetuser) || newly_infected_users.contains(targetuser))
					continue ;
				double timestampsInfection=Double.MAX_VALUE ;
				for(String sourcesuser : newly_infected_users) { // Parcourir les utilisateurs deja infectes
					CPUParams ztarget = user_modules_1.get(targetuser) ;
					CPUParams zsource = dualPoint ? user_modules_2.get(sourcesuser) : user_modules_1.get(sourcesuser) ;
					//System.out.println(targetuser+" : "+sourcesuser);
					if(!users.contains(sourcesuser)) {
						System.out.println("ARG : "+sourcesuser);
						continue ;
					}
					
					Double h=null ;
					if(maxHeats.containsKey(targetuser)){
						h=maxHeats.get(targetuser).get(sourcesuser);
					} else {
						maxHeats.put(targetuser, new HashMap<String, Double>()) ;
					}
					
					if(h==null) {
						h = heat.heatmax(nbDims, ztarget, zsource) ;
						maxHeats.get(targetuser).put(sourcesuser, h) ;
					}
						
					//System.out.println("DISTANCE"+targetuser+sourcesuser+CPUL2Dist2.dist(ztarget, zsource));
					//System.out.println("heatreceive "+targetuser+sourcesuser+"="+h +" "+ ztarget+" "+zsource);
					if(h>this.threshold) { // Trouver quelle source fait passer le seuil à target LE PLUS TOT.
						
						double t = heat.timeWhen(nbDims, ztarget, zsource, this.threshold) ;
						if((infectionsTimes.get(sourcesuser)+t)<timestampsInfection) {
							timestampsInfection=infectionsTimes.get(sourcesuser)+t ;
						}
					}
				}
				if(timestampsInfection<Double.MAX_VALUE) { // Ajouter l'utilisateur au tableau s'il depasse le seuil
					nextinfectionTimes.put(targetuser,timestampsInfection) ;
					maxTinfection=Math.max(maxTinfection,timestampsInfection) ;
					new_user_added=true ; // Ce truc nous fera faire une autre itération.
				}
			}
			newly_infected_users = nextinfectionTimes.keySet() ;
			infectionsTimes.putAll(nextinfectionTimes); 
			
		}while(new_user_added) ;
		
		TreeMap<Long, HashMap<String, Double>> inf = new TreeMap<Long, HashMap<String,Double>>();
		/*for(long t=0  ;t<maxTinfection ; t++) {
			inf.put(t, new HashMap<String,Double>()) ;
		}
		for(String u : infectionsTimes.keySet()) {
			double ti = infectionsTimes.get(u) ;
			for(long t=0 ; t<ti ;t++) {
				inf.get(t).put(u,1.0) ;
			}
		}*/
		inf.put(0l, new HashMap<String,Double>()) ;
		inf.put(1l, new HashMap<String,Double>()) ;
		for(String u : infectionsTimes.keySet()) {
				inf.get(1l).put(u,1.0) ;
		}
		
		
		//System.out.println(inf);
		ps.setInfections(inf);
		
		return 0;
	}

	public int infer2(Structure struct) {
		
		
		
		PropagationStruct ps = (PropagationStruct)struct ;
		ArrayList<String> initials  = ps.getArrayInit() ;
		
		HashMap<String,Double> infectionsTimes = new HashMap<String, Double>() ;
		for(String u : initials) {
			infectionsTimes.put(u,0.0) ;
		}
		
		
		boolean new_user_added = false ;
		double maxTinfection = 0.0 ;
		
		Set<String> newly_infected_users = infectionsTimes.keySet() ;
		int debugiter =  0 ;
		do {
			System.out.println(debugiter++);
			
			new_user_added = false ;
			HashMap<String,Double> nextinfectionTimes = new HashMap<String,Double>() ;
			
			for(String targetuser : this.users) { // Parcourirs les utilisateurs non infectes
				if(infectionsTimes.containsKey(targetuser))
					continue ;
				double timestampsInfection=Double.MAX_VALUE ;
				for(String sourcesuser : newly_infected_users) { // Parcourir les utilisateurs deja infectes
					CPUParams ztarget = user_modules_1.get(targetuser) ;
					CPUParams zsource = dualPoint ? user_modules_2.get(sourcesuser) : user_modules_1.get(sourcesuser) ;
					//System.out.println(targetuser+" : "+sourcesuser);
					if(!users.contains(sourcesuser)) {
						System.out.println("ARG : "+sourcesuser);
						continue ;
					}
					
					Double h=null ;
					if(maxHeats.containsKey(targetuser)){
						h=maxHeats.get(targetuser).get(sourcesuser);
					} else {
						maxHeats.put(targetuser, new HashMap<String, Double>()) ;
					}
					
					if(h==null) {
						h = heat.heatmax(nbDims, ztarget, zsource) ;
						maxHeats.get(targetuser).put(sourcesuser, h) ;
					}
						
					//System.out.println("DISTANCE"+targetuser+sourcesuser+CPUL2Dist2.dist(ztarget, zsource));
					//System.out.println("heatreceive "+targetuser+sourcesuser+"="+h +" "+ ztarget+" "+zsource);
					if(h>this.threshold) { // Trouver quelle source fait passer le seuil à target LE PLUS TOT.
						
						double t = heat.timeWhen(nbDims, ztarget, zsource, this.threshold) ;
						if((infectionsTimes.get(sourcesuser)+t)<timestampsInfection) {
							timestampsInfection=infectionsTimes.get(sourcesuser)+t ;
							//break ;
						}
						
					}
				}
				if(timestampsInfection<Double.MAX_VALUE) { // Ajouter l'utilisateur au tableau s'il depasse le seuil
					nextinfectionTimes.put(targetuser,timestampsInfection) ;
					maxTinfection=Math.max(maxTinfection,timestampsInfection) ;
					new_user_added=true ; // Ce truc nous fera faire une autre itération.
				}
			}
			newly_infected_users = nextinfectionTimes.keySet() ;
			infectionsTimes.putAll(nextinfectionTimes); 
			
		}while(new_user_added) ;
		
		TreeMap<Long, HashMap<String, Double>> inf = new TreeMap<Long, HashMap<String,Double>>();
		/*for(long t=0  ;t<maxTinfection ; t++) {
			inf.put(t, new HashMap<String,Double>()) ;
		}
		for(String u : infectionsTimes.keySet()) {
			double ti = infectionsTimes.get(u) ;
			for(long t=0 ; t<ti ;t++) {
				inf.get(t).put(u,1.0) ;
			}
		}*/
		inf.put(0l, new HashMap<String,Double>()) ;
		inf.put(1l, new HashMap<String,Double>()) ;
		for(String u : infectionsTimes.keySet()) {
				inf.get(1l).put(u,1.0) ;
		}
		
		
		//System.out.println(inf);
		ps.setInfections(inf);
		
		return 0;
	}


	public int infer3(Structure struct) {
	
	
		PropagationStruct ps = (PropagationStruct)struct ;
		ArrayList<String> initials  = ps.getArrayInit() ;
		
		HashMap<String,Double> infectionsTimes = new HashMap<String, Double>() ;
		for(String u : initials) {
			infectionsTimes.put(u,0.0) ;
		}
		
		//////////
		/*PrintWriter f = null ;
		try {
			f = new PrintWriter(new FileWriter(new File("tempcascades"),true)) ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		//////////
		
		
		boolean new_user_added = false ;
		double maxTinfection = 0.0 ;
		HashMap<String,Double> maxTreached = new HashMap<String, Double>() ;
		for(String user : this.users) {
			maxTreached.put(user, 0.0) ;
		}
		
		Set<String> newly_infected_users = infectionsTimes.keySet() ;
		//int debugiter =  0 ;
		int N = 0 ;
		do {
			
			new_user_added = false ;
			HashMap<String,Double> nextinfectionTimes = new HashMap<String,Double>() ;
			
			for(String sourceUser : newly_infected_users) {
				if(!users.contains(sourceUser)) {
					debug.add(ps.toString()) ;
					continue ;
				}
				for(String targetUser : this.heatThroughThisLink.get(sourceUser).keySet()) {
					double t1=maxTreached.get(targetUser) ;
					double t2 = this.heatThroughThisLink.get(sourceUser).get(targetUser) ;
					maxTreached.put(targetUser, t1 > t2 ? t1 : t2) ;
				}
				HashMap<String, Double> temps = reachThreashAndWhen.get(sourceUser) ;
				// TODO mettre la temps max atteinte.
				if(!temps.isEmpty()) {
					for(String targetUser : temps.keySet()) {
						if(infectionsTimes.containsKey(targetUser)|| newly_infected_users.contains(targetUser))
							continue ;
						Double currentTargetInfection = nextinfectionTimes.get(targetUser) ;
						if(currentTargetInfection==null) {
							nextinfectionTimes.put(targetUser, infectionsTimes.get(sourceUser)+temps.get(targetUser)) ;
						} else {
							double inf = Math.min(currentTargetInfection, infectionsTimes.get(sourceUser)+temps.get(targetUser)) ;
							nextinfectionTimes.put(targetUser, inf) ;
						}
						new_user_added=true ;
						
					}
				}
			}
			
			newly_infected_users = nextinfectionTimes.keySet() ;
			infectionsTimes.putAll(nextinfectionTimes); 
			
		}while(new_user_added) ;
		
		TreeMap<Long, HashMap<String, Double>> inf = new TreeMap<Long, HashMap<String,Double>>();
		/*for(long t=0  ;t<maxTinfection ; t++) {
			inf.put(t, new HashMap<String,Double>()) ;
		}
		for(String u : infectionsTimes.keySet()) {
			double ti = infectionsTimes.get(u) ;
			for(long t=0 ; t<ti ;t++) {
				inf.get(t).put(u,1.0) ;
			}
		}*/
		inf.put(0l, new HashMap<String,Double>()) ;
		inf.put(1l, new HashMap<String,Double>()) ;
		//for(String u : infectionsTimes.keySet()) {
		for(String u : users) {
				//inf.get(1l).put(u,1.0) ;
			inf.get(1l).put(u,1.0)  ;
			//if(maxTreached.get(u)>this.threshold) {
				//f.print(maxTreached.get(u)+ " ") ;
			//}
		}
		//f.println();
		//f.close() ;
		
		
		//System.out.println(inf);
		ps.setInfections(inf);
	
		return 0;
	}

	
	
	public int infer(Structure struct) {
		
		PropagationStruct ps = (PropagationStruct)struct ;
		ArrayList<String> initials  = ps.getArrayInit() ;
		
		HashMap<String,Double> infectionsTimes = new HashMap<String, Double>() ;
		for(String u : initials) {
			infectionsTimes.put(u,0.0) ;
		}
		
		
		boolean new_user_added = false ;
		double maxTinfection = 0.0 ;
		HashMap<String,Double> maxTreached ;
		
		Set<String> newly_infected_users = infectionsTimes.keySet() ;
		//int debugiter =  0 ;
		do {
			
			new_user_added = false ;
			HashMap<String,Double> nextinfectionTimes = new HashMap<String,Double>() ;
			
			for(String sourceUser : newly_infected_users) {
				if(!users.contains(sourceUser)) {
					debug.add(ps.toString()) ;
					continue ;
				}
				HashMap<String, Double> temps = reachThreashAndWhen.get(sourceUser) ;
				if(!temps.isEmpty()) {
					for(String targetUser : temps.keySet()) {
						if(infectionsTimes.containsKey(targetUser))
							continue ;
						Double currentTargetInfection = nextinfectionTimes.get(targetUser) ;
						if(currentTargetInfection==null) {
							nextinfectionTimes.put(targetUser, infectionsTimes.get(sourceUser)+temps.get(targetUser)) ;
						} else {
							double inf = Math.min(currentTargetInfection, infectionsTimes.get(sourceUser)+temps.get(targetUser)) ;
							nextinfectionTimes.put(targetUser, inf) ;
						}
						new_user_added=true ;
						
					}
				}
			}
			
			newly_infected_users = nextinfectionTimes.keySet() ;
			infectionsTimes.putAll(nextinfectionTimes); 
			
		}while(new_user_added) ;
		
		TreeMap<Long, HashMap<String, Double>> inf = new TreeMap<Long, HashMap<String,Double>>();
		/*for(long t=0  ;t<maxTinfection ; t++) {
			inf.put(t, new HashMap<String,Double>()) ;
		}
		for(String u : infectionsTimes.keySet()) {
			double ti = infectionsTimes.get(u) ;
			for(long t=0 ; t<ti ;t++) {
				inf.get(t).put(u,1.0) ;
			}
		}*/
		inf.put(0l, new HashMap<String,Double>()) ;
		inf.put(1l, new HashMap<String,Double>()) ;
		for(String u : infectionsTimes.keySet()) {
			double score = 1.0 / infectionsTimes.get(u) ;
			inf.get(1l).put(u,score) ;
		}
		
		
		//System.out.println(inf);
		ps.setInfections(inf);
		
		return 0;
	}
	
	@Override
	public void load() throws IOException {
		
		File file=new File(model_file);
    	BufferedReader f = new BufferedReader(new FileReader(file)) ;
    	
    	
    	f.readLine() ; // <MODEL>
    	this.step=f.readLine().substring(5);
    	//this.nbInitSteps=Integer.parseInt(f.readLine().substring(11)) ;
    	this.nbDims=Integer.parseInt(f.readLine().substring(7)) ;
    	this.dualPoint=Boolean.parseBoolean(f.readLine().substring(9)) ;
    	this.threshold=Double.parseDouble(f.readLine().substring(10)) ;
    	this.minibatchsize=Integer.parseInt(f.readLine().substring(14));
    	this.withDiag=Boolean.parseBoolean(f.readLine().substring(9)) ;
    	
    	f.readLine() ; // </MODEL>
		 
		f.readLine() ; // <USERS>
		this.users = new ArrayList<String>() ;
    	for(String s = f.readLine() ; s.compareTo("</USERS>")!=0 ; s=f.readLine()) {
    		this.users.add(s) ;
    	}
    	
    	construct();
    	
    	f.readLine() ; // <PARAMETERS>
    	int nbpar = 0 ;
 		ArrayList<Parameter> thisparams = this.params.getParams() ;
 		for(String s = f.readLine() ; s.compareTo("</PARAMETERS>")!=0 ; s=f.readLine()) {
    		thisparams.get(nbpar).setVal(Float.parseFloat(s)) ;
    		nbpar++ ;
    	}

 		int expected =  nbDims*this.users.size()*(dualPoint ? 2 : 1)+(withDiag ? nbDims : 0);
 		if(nbpar != expected) {
 			throw new IOException("Wrong number of parameters in file"+model_file+". Expected "+expected+", found "+nbpar) ;
 		}
    	
 		loaded=true ;
    	
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
    	//p.println("nbInitStep="+this.nbInitSteps);
    	p.println("nbDims="+this.nbDims) ;
		p.println("duaPoint="+this.dualPoint) ;
		p.println("threshold="+this.threshold) ;
		p.println("minibatchsize="+this.minibatchsize) ;
		p.println("withDiag="+this.withDiag) ;
		p.println("</MODEL>") ;
		
		p.println("<USERS>") ;
    	for(String u : this.users) {
    		p.println(u) ;
    	}
    	p.println("</USERS>") ;
    	
    	p.println("<PARAMETERS>") ;
    	//int nbp=0 ;
    	for(Parameter par :this.params.getParams()) {
    		p.println(par.getVal()) ;
    	//	nbp++ ;
    	}
    	p.println("</PARAMETERS>") ;
	}

	@Override
	public void forward() {
		if(forwardMode==0) {
			forwardMax();
		} else if(forwardMode==1)
			forwardAll() ;
	}
	
	
	
	// Version ou dans le forward, on cherche à rapprocher TOUTES les sources
	public void forwardAll() {
		
		ztarget = new CPUParams(2*minibatchsize, nbDims) ;
		zsources = new CPUParams(2*minibatchsize, nbDims) ;
		
		//System.out.println(this.user_modules_src.get("a").getParameters());
		
		// Constuction des inputs.
		ArrayList<Double> tdumax = new ArrayList<Double>(minibatchsize*2) ;
		//tdumax.ensureCapacity(minibatchsize*2);
		Random rng = new Random() ;
		
		//System.out.println("Forward.");
		
		for(int i_input = 0 ; i_input<minibatchsize*2 ; i_input+=2) {
			tdumax.add(0.0);
			tdumax.add(0.0);
			int id_cascade ;
			do {
				id_cascade = this.cascades_ids.get(rng.nextInt(this.cascades_ids.size())) ;
			}while(this.train_cascades.get(id_cascade).getArrayContamined().size()<2);
			
			PropagationStruct ps = this.train_cascades.get(id_cascade) ;
			HashMap<String, Long> infections_times = ps.getInfectionTimes() ;
			//System.out.println("Infection times = "+infections_times);
			// Tirer un gars de la cascade.
			String[] infectedarray = infections_times.keySet().toArray(new String[1]) ;
			String targetUser ;
			do {
				targetUser = infectedarray[rng.nextInt(infectedarray.length)] ;
			} while(infections_times.get(targetUser)<2) ;
			
			//System.out.println("Target="+targetUser);
			
			// Prendre un des gars infectés avant lui.
			String hotestPrevious="" ;
			double hotestTemp = 0.0 ;
			/*for(String previous : infections_times.keySet()) {
				if(infections_times.get(targetUser) <= infections_times.get(previous))
					continue ;
				// TODO : la version ou tu cherche à ce qu'il soit infecté AVANT quand meme.
				double maxtemp = heat.heatmax(nbDims, user_modules_src.get(targetUser), user_modules_src.get(previous));
				//System.out.println("maxtemps="+maxtemp);
				if(maxtemp>hotestTemp) {
					hotestPrevious=previous ;
					hotestTemp=maxtemp ;
				}
			}*/
			ArrayList<String> temp = new ArrayList<String>(infections_times.keySet()) ;
			hotestPrevious = temp.get(rng.nextInt(temp.size())) ;
			
			//System.out.println("Hottest="+hotestPrevious);
			
			//Voila on a le max. Et pour par cher en plus.
			// TODO : la version ou tu cherche à ce qu'il soit infecté AVANT quand meme.
			//System.out.println(user_modules_src.get(targetUser));
			//System.out.println(user_modules_src.get(hotestPrevious));
			//System.out.println("TDUMAX SIZE = "+tdumax.size());
			//System.out.println(hotestPrevious+":"+targetUser+":"+infections_times.keySet()+":"+infections_times);
			tdumax.set(i_input,heat.dist.dist(user_modules_1.get(targetUser), dualPoint ? user_modules_2.get(hotestPrevious) : user_modules_1.get(hotestPrevious))/(2*nbDims));
			ztarget.addParametersFrom(this.user_modules_1.get(targetUser));
			zsources.addParametersFrom(dualPoint ? user_modules_2.get(hotestPrevious) : user_modules_1.get(hotestPrevious));
			
			
			// Tirer un gars qui est PAS dans la cascade
			String mecPasDedans = "" ;
			do {
				mecPasDedans=this.users.get(rng.nextInt(this.users.size())) ;
			}while(infections_times.containsKey(mecPasDedans));
			
			//System.out.println("pas dedans="+mecPasDedans);
			
			hotestPrevious="" ;
			hotestTemp = 0.0 ;
			for(String previous : infections_times.keySet()) {
				// TODO : la version ou tu cherche à ce qu'il soit infecté AVANT quand meme.
				double maxtemp = heat.heatmax(nbDims, user_modules_1.get(mecPasDedans), dualPoint ? user_modules_2.get(previous) : user_modules_1.get(previous));
				if(maxtemp>hotestTemp) {
					hotestPrevious=previous ;
					hotestTemp=maxtemp ;
				}
			}
			// TODO : la version ou tu cherche à ce qu'il soit infecté AVANT quand meme.
			tdumax.set(i_input+1,heat.dist.dist(user_modules_1.get(mecPasDedans), dualPoint ? user_modules_2.get(hotestPrevious) : user_modules_1.get(hotestPrevious))/(2*nbDims));
			ztarget.addParametersFrom(this.user_modules_1.get(mecPasDedans));
			zsources.addParametersFrom(dualPoint ? this.user_modules_2.get(hotestPrevious) : this.user_modules_1.get(hotestPrevious));
			
			
		}
		
		this.input_table.setModule(0,ztarget);
		this.input_table.setModule(1,zsources);
		this.heat.setT(tdumax);
		
		/*System.out.println(ztarget.getParameters());
		System.out.println("----");
		System.out.println(zsources.getParameters());
		System.out.println("----");
		System.out.println(tdumax);
		System.out.println("----");
		System.out.println() ;*/
		
		global.forward(null); //
		
		//System.out.println("heat="+heat.getOutput());
		
	}

	// Version ou dans le forward, on cherche le plus proche pour être la source.
		public void forwardMax() {
			
			ztarget = new CPUParams(2*minibatchsize, nbDims) ;
			zsources = new CPUParams(2*minibatchsize, nbDims) ;
			
			//System.out.println(this.user_modules_src.get("a").getParameters());
			
			// Constuction des inputs.
			ArrayList<Double> tdumax = new ArrayList<Double>(minibatchsize*2) ;
			//tdumax.ensureCapacity(minibatchsize*2);
			Random rng = new Random() ;
			
			//System.out.println("Forward.");
			
			for(int i_input = 0 ; i_input<minibatchsize*2 ; i_input+=2) {
				tdumax.add(0.0);
				tdumax.add(0.0);
				int id_cascade ;
				do {
					id_cascade = this.cascades_ids.get(rng.nextInt(this.cascades_ids.size())) ;
				}while(this.train_cascades.get(id_cascade).getArrayContamined().size()<2);
				
				PropagationStruct ps = this.train_cascades.get(id_cascade) ;
				HashMap<String, Long> infections_times = ps.getInfectionTimes() ;
				//System.out.println("Infection times = "+infections_times);
				// Tirer un gars de la cascade.
				String[] infectedarray = infections_times.keySet().toArray(new String[1]) ;
				String targetUser ;
				do {
					targetUser = infectedarray[rng.nextInt(infectedarray.length)] ;
				} while(infections_times.get(targetUser)<2) ;
				
				//System.out.println("Target="+targetUser);
				
				// Trouver le infecté avant lui qui lui a envoyé le plus de chaleur.
				String hotestPrevious="" ;
				double hotestTemp = 0.0 ;
				for(String previous : infections_times.keySet()) {
					if(infections_times.get(targetUser) <= infections_times.get(previous))
						continue ;
					// TODO : la version ou tu cherche à ce qu'il soit infecté AVANT quand meme.
					double maxtemp = heat.heatmax(nbDims, user_modules_1.get(targetUser), dualPoint ? user_modules_2.get(previous) : user_modules_1.get(previous));
					//System.out.println("maxtemps="+maxtemp);
					if(maxtemp>hotestTemp) {
						hotestPrevious=previous ;
						hotestTemp=maxtemp ;
					}
				}
				
				//System.out.println("Hottest="+hotestPrevious);
				
				//Voila on a le max. Et pour par cher en plus.
				// TODO : la version ou tu cherche à ce qu'il soit infecté AVANT quand meme.
				//System.out.println(user_modules_src.get(targetUser));
				//System.out.println(user_modules_src.get(hotestPrevious));
				//System.out.println("TDUMAX SIZE = "+tdumax.size());
				//System.out.println(hotestPrevious+":"+targetUser+":"+infections_times.keySet()+":"+infections_times);
				tdumax.set(i_input,heat.dist.dist(user_modules_1.get(targetUser), dualPoint ? user_modules_2.get(hotestPrevious) : user_modules_1.get(hotestPrevious))/(2*nbDims));
				ztarget.addParametersFrom(this.user_modules_1.get(targetUser));
				zsources.addParametersFrom(dualPoint ? this.user_modules_2.get(hotestPrevious) : this.user_modules_1.get(hotestPrevious));
				
				
				// Tirer un gars qui est PAS dans la cascade
				String mecPasDedans = "" ;
				do {
					mecPasDedans=this.users.get(rng.nextInt(this.users.size())) ;
				}while(infections_times.containsKey(mecPasDedans));
				
				//System.out.println("pas dedans="+mecPasDedans);
				
				hotestPrevious="" ;
				hotestTemp = 0.0 ;
				for(String previous : infections_times.keySet()) {
					// TODO : la version ou tu cherche à ce qu'il soit infecté AVANT quand meme.
					double maxtemp = heat.heatmax(nbDims, user_modules_1.get(mecPasDedans), dualPoint ? user_modules_2.get(previous) : user_modules_1.get(previous));
					if(maxtemp>hotestTemp) {
						hotestPrevious=previous ;
						hotestTemp=maxtemp ;
					}
				}
				// TODO : la version ou tu cherche à ce qu'il soit infecté AVANT quand meme.
				tdumax.set(i_input+1,heat.dist.dist(user_modules_1.get(mecPasDedans), dualPoint ? user_modules_2.get(hotestPrevious) : user_modules_1.get(hotestPrevious))/(2*nbDims));
				ztarget.addParametersFrom(this.user_modules_1.get(mecPasDedans));
				zsources.addParametersFrom(dualPoint ? this.user_modules_2.get(hotestPrevious) : this.user_modules_1.get(hotestPrevious));
				
				
			}
			
			this.input_table.setModule(0,ztarget);
			this.input_table.setModule(1,zsources);
			this.heat.setT(tdumax);
			
			/*System.out.println(ztarget.getParameters());
			System.out.println("----");
			System.out.println(zsources.getParameters());
			System.out.println("----");
			System.out.println(tdumax);
			System.out.println("----");
			System.out.println() ;*/
			
			global.forward(null); //
			
			//System.out.println("heat="+heat.getOutput());
			
		}
	
	@Override
	public void backward() {
		global.backward(null);
		
	}
	
	public void test(){
		long step=1l;
		int nbInitSteps=1;
		users=new ArrayList<String>();
		users.add("a"); users.add("b"); users.add("c"); users.add("d");
		/*users.add("f"); users.add("g"); users.add("h"); users.add("i"); users.add("j");
		users.add("k"); users.add("l"); users.add("m"); users.add("n"); users.add("o");
		users.add("p"); users.add("q"); users.add("r"); users.add("s"); users.add("t");
		users.add("u"); users.add("v"); users.add("w"); users.add("x"); users.add("y");*/
		//this.train_users=new HashMap<String,HashMap<Integer,Double>>();
		
		train_cascades=new HashMap<Integer,PropagationStruct>();
		
		String[] cascades = {"ab","ba", "cd", "dc"} ;
		
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
			PropagationStruct struct=new PropagationStruct(new Cascade(cid,"Cascade_test_"+cid, null),step,nbInitSteps,init,infections,diffusion);
			train_cascades.put(cid++, struct);
		}
		
		cascades_ids=new ArrayList<Integer>(train_cascades.keySet());
	}
	
	public double evaltest(HashMap<Integer,PropagationStruct> cascades_test) {
		
		double totalMAP = 0.0 ;
		MAP map = new MAP(new HashSet<String>(users),true) ;
		for(int i : cascades_test.keySet()) {
				//System.out.println(cascades_test.get(i).getCascade());
				//ArrayList<String> groudtruth = cascades_test.get(i).getArrayContamined() ;
				//this.infer(cascades_test.get(i)) ;
				//TreeMap<Long, HashMap<String, Double>> predicted = cascades_test.get(i).getInfections() ;
				Hyp hyp=new Hyp(cascades_test.get(i),this,1);
				if(hyp.getStruct()==null) {
					System.out.println("Fuck");
				}
				Result result=map.eval(hyp);
				//result.getScores().get("model_1") ;
				//System.out.println("r = "+result);
				totalMAP += result.getScores().get("MAP__ignoreInit") ;
				//System.out.println(result.getScores().get("MAP__ignoreInit"));
		}
		System.out.println("MAP : "+totalMAP/cascades_test.size());
		return totalMAP/cascades_test.size() ;
		
	}
	
	/*public ArrayList<String> getUsers() {
		return this.users ;
	}*/
	
	public void updateParams(double line){
		getUsedParams().update(line);
		global.paramsChanged();
	}
	
	private void checkCascades() {
		for(Integer i : this.train_cascades.keySet()) {
			PropagationStruct p = this.train_cascades.get(i) ;
			ArrayList<String> a = p.getArrayInit() ;
			for(String init : a) {
				if(!this.users.contains(init)){
					System.out.println(init);
				}
			}
		}
	}
	
	public Parameters getUsedParams(){
		return params;
	}
	
	public static void main(String[] args) throws IOException {
		
		int nbDims=2;
		boolean diag = false ;
		double thresh= 1.0 ;
		String modelfile = "" ;
		String resultsFile = "tempres" ;
		String db = "digg" ;
		String cascadetrain = "cascades_1" ;
		String cascadetest = "cascades_2" ;
		double parinf = -Double.MAX_VALUE ;
		double parsup = +Double.MAX_VALUE ;
		int forwardmode= 1 ;
		int maxiters=1000000 ;
		double line = 0.01 ;
		double fact = 1.0 ;
		int batchsize = 30 ;
		boolean dualpoint = false ;
		
		PrintWriter f =new PrintWriter(new FileWriter(new File("tempcascades"),false)) ;
		f.print("");
		f.close();
		
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
			forwardmode= Integer.parseInt(args[8]) ;
			maxiters=Integer.parseInt(args[9]) ;
			line = Double.parseDouble(args[10]) ;
			fact = Double.parseDouble(args[11]) ;
			batchsize = Integer.parseInt(args[12]) ;
			dualpoint = Boolean.parseBoolean(args[13]) ; ;
		}
				
		///*
		
		//*/
		
		//MLPDiffChain m = new MLPDiffChain("", 10, 1.0, false,false) ;
		MLPDiffChain m = new MLPDiffChain(modelfile,nbDims, thresh, dualpoint,diag,parinf,parsup,forwardmode,batchsize) ;
		
		//m.prepareLearning("icwsm","cascades_1", 1, 1);
		// m.checkCascades();
		/*System.out.println("Check done");
		if(5>3){
			return ;
		}*/

		Env.setVerbose(1) ;
		LineSearch lsearch=LineSearch.getFactorLine(line,fact); 
 		DescentDirection dir=DescentDirection.getGradientDirection();
		m.learn(db, cascadetrain, 1, 1, Optimizer.getDescent(dir, lsearch),100,maxiters);
		//m.heat.lockParams();
		Date d = new Date() ;
		m.computeAllTranmissions(); 
		Date d2 = new Date() ;
		System.out.println("TEMPS = "+(d2.getTime()-d.getTime())/1000);
		/*if(5>3){
			return ;
		}*/
		/*try {
			//m.save();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		/*System.out.println("MAP = "+m.evaltest(m.train_cascades)) ;
		for(Parameter p : m.getParams().getParams()){
			System.out.println(p.getVal());
		}*/
		/*for(String u : m.user_modules_src.keySet()) {
			m.user_modules_src.get(u).paramsChanged();//m.user_modules_src.get(u).forward(null);
			System.out.println(u +" : " +m.user_modules_src.get(u).getParameters().getMatrix(0));
		}*/
		EvalPropagationModel.run(new EvalMLPDiffChain(m,db, cascadetrain), resultsFile+".train") ;
		//m.threshold = 1.0 ;
		EvalPropagationModel.run(new EvalMLPDiffChain(m,db, cascadetest), resultsFile+".test") ;
		/*m.threshold = 1.05 ;
		m.computeAllTranmissions(); 
		EvalPropagationModel.run(new EvalMLPDiffChain(m,"digg", "cascades_2"), resultsFile+".test15") ;
		m.threshold = 1.1 ;
		m.computeAllTranmissions(); 
		EvalPropagationModel.run(new EvalMLPDiffChain(m,"digg", "cascades_2"), resultsFile+".test11") ;
		m.threshold = 1.2 ;
		m.computeAllTranmissions(); 
		EvalPropagationModel.run(new EvalMLPDiffChain(m,"digg", "cascades_2"), resultsFile+".test12") ;
		*/
	}
	@Override
	public int inferSimulation(Structure struct) {
		// TODO Auto-generated method stub
		return 0;
	}	

}








class EvalMLPDiffChain extends EvalPropagationModelConfig{

	private MLPDiffChain mlpc ;
	
	public EvalMLPDiffChain( MLPDiffChain mlpc,String db,String cascades) {
		this.mlpc=mlpc ;
		
		pars.put("db",db); //usElections5000_hashtag");
		pars.put("cascadesCol",cascades);
		pars.put("step", "1");
		pars.put("nbInitSteps", "1");
		pars.put("nbCascades", "1000");
		pars.put("allUsers", "users_1");
		//this.allUsers=new HashSet<String>(mlpc.getUsers()) ;
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
