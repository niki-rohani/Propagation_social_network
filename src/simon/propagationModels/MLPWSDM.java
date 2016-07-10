package simon.propagationModels;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.TreeMap;

import javax.xml.transform.Source;

import cascades.Cascade;
import mlp.CPUAverageRows;
import mlp.CPUHingeLoss;
import mlp.CPUMatrix;
import mlp.CPUParams;
import mlp.CPUSum;
import mlp.CPUTermByTerm;
import mlp.DescentDirection;
import mlp.LineSearch;
import mlp.Optimizer;
import mlp.Parameter;
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
import propagationModels.MLP;
import propagationModels.PropagationModel;
import propagationModels.PropagationStruct;
import propagationModels.PropagationStructLoader;
import simon.mlp.CPUL2Dist2;

public class MLPWSDM extends MLP {

	
	private int nbDim ;
	private boolean useDiag=false ;
	private int batch ;
	private double parInf;
	private double parSup;
	
	private HashMap<String,CPUParams> z ;
	private CPUParams diag ;
	
	private TableModule inputZsMinusZj ;
	private TableModule inputZsMinusZi ;
	
	
	private CPUHingeLoss hinge ;
	
	private HashMap<String, HashMap<String, Double>> allHeat;
	
	
	public MLPWSDM(String model_file,int nbDims, int batch, double Par) {
		super(model_file);
		this.nbDim=nbDims ;
		this.batch=batch ;
		this.parInf=-Par ;
		this.parSup=Par ;
		
		// TODO Auto-generated constructor stub
	}

	
	public void construct() {
		
		// TODO : La version diag.
		
		z=new HashMap<String, CPUParams>();
		
		for(String user:users){
			if(!z.containsKey(user)){
				CPUParams mod=new CPUParams(1,nbDim);
				mod.setName(user+"in");
				//System.out.println("CONSTRUIT");
				params.allocateNewParamsFor(mod, parInf, parSup); //,(1.0f*nu)/(1.0f*users.size()));
				z.put(user,mod);
			}
		}
		
		CPUL2Dist2 distsj = new CPUL2Dist2(nbDim, false) ;
		CPUL2Dist2 distsi = new CPUL2Dist2(nbDim, false) ;
		this.inputZsMinusZj = new TableModule() ;
		this.inputZsMinusZi = new TableModule() ;
		

		
		
		
		SequentialModule seqDistsj = new SequentialModule() ;
		SequentialModule seqDistsi = new SequentialModule() ;
		if(useDiag) {
			this.diag = new CPUParams(1,nbDim) ; params.allocateNewParamsFor(this.diag,parInf,parSup) ;
			/*distsi.setParameters(this.diag.getParamList());
			distsj.setParameters(this.diag.getParamList());*/
			
			
			CPUTermByTerm cputbt = new CPUTermByTerm(nbDim) ;
			TableModule t = new TableModule() ;
			t.addModule(this.inputZsMinusZj);
			t.addModule(this.diag);
			
			seqDistsj.addModule(t) ; seqDistsj.addModule(cputbt); seqDistsj.addModule(distsj) ;
			
			CPUTermByTerm cputbt2 = new CPUTermByTerm(nbDim) ;
			TableModule t2 = new TableModule() ;
			t2.addModule(this.inputZsMinusZi);
			t2.addModule(this.diag);
			
			seqDistsi.addModule(t2) ; seqDistsi.addModule(cputbt2); seqDistsi.addModule(distsi) ;
			
		} else {
			seqDistsj.addModule(this.inputZsMinusZj) ; seqDistsj.addModule(distsj) ;
			seqDistsi.addModule(this.inputZsMinusZi) ; seqDistsi.addModule(distsi) ;
		}
		TableModule inputDiff = new TableModule() ;
		inputDiff.addModule(seqDistsj);
		inputDiff.addModule(seqDistsi);
		
		
		CPUMatrix m = new CPUMatrix(batch,1,1.0);

		this.hinge = new CPUHingeLoss(1) ;
		this.hinge.setLabels(new Tensor(m));
		this.global = new SequentialModule() ;
		
		this.global.addModule(inputDiff);
		ArrayList<Double> w = new ArrayList<Double>() ; w.add(1.0) ; w.add(-1.0) ; 
		this.global.addModule(new CPUSum(1,2,w));
		this.global.addModule(hinge);
		this.global.addModule(new CPUAverageRows(1,0));
		
	}
	
	
	
	private void computeAllHeat() {
		
		this.allHeat = new HashMap<String, HashMap<String,Double>>() ;
		CPUL2Dist2 d = new CPUL2Dist2(nbDim) ;
		for(String userSource : this.users) {
			HashMap<String,Double> h=new HashMap<String, Double>() ;
			for(String userTarget : this.users) {
				if(userSource==userTarget)
					continue ;
				//System.out.println(userSource+ ","+userTarget);
				CPUParams zsource = z.get(userSource) ;
				CPUParams ztarget =z.get(userTarget) ;
				double dist = d.dist(zsource, ztarget) ;
				h.put(userTarget,dist) ;
				//System.out.println(heat);
			}
			this.allHeat.put(userSource, h) ;
		}
	}
	
	public void learn(String db, String cascadesCollection, Optimizer optim, int maxiter) {
		if(db=="test") {
			this.test();
		} else {
			PropagationStructLoader psl = new PropagationStructLoader(db, cascadesCollection,1, 1 ,-1) ; // TODO
			psl.load();
			prepareLearning(psl) ;
		}
		
		this.construct();
		//System.out.println(train_cascades);
		optim.optimize(this, 0.000001, maxiter, false);
		
		
	}
	
	@Override
	public int infer(Structure struct) {
		
		PropagationStruct ps = (PropagationStruct)struct ;
		String initial = ps.getArrayInit().get(0) ;
		
		TreeMap<Long, HashMap<String, Double>> inf = new TreeMap<Long, HashMap<String,Double>>();
		inf.put(0l, new HashMap<String,Double>()) ;
		inf.put(1l, new HashMap<String,Double>()) ;
		
		if(this.allHeat.containsKey(initial)) {
			for(String target : this.allHeat.get(initial).keySet()) {
				double score = 1/this.allHeat.get(initial).get(target) ;
				inf.get(1l).put(target,score) ;
			}
		}
		
		ps.setInfections(inf);
		
		
		
		
		return 0;
	}

	@Override
	public int inferSimulation(Structure struct) {
		return infer(struct) ;
	}

	


	@Override
	public void load() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void save() throws IOException {
		File file=new File(model_file);
    	File dir = file.getParentFile();
    	if(dir!=null){
    		dir.mkdirs();
    	}
    	
    	PrintStream p = new PrintStream(file) ;
    	p.println("<MODEL="+this+">");
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


	@Override
	public void forward() {
		
		
		// TODO : forward depuis des mecs en début de cascade.
		
		CPUParams s1 = new CPUParams(batch, nbDim) ;
		CPUParams s2 = new CPUParams(batch, nbDim) ;
		CPUParams zi = new CPUParams(batch, nbDim) ;
		CPUParams zj = new CPUParams(batch, nbDim) ;
		
		// Constuire le batch.
		Random rng = new Random() ;
		for(int i=0 ; i<batch ; i++) {
			
			PropagationStruct ps ;
			
			do {
				ps = this.train_cascades.get(rng.nextInt(this.train_cascades.size())) ;
			} while(ps==null || ps.getInfectionTimes().size()<2) ;
			
			HashMap<String,Long>infectionTimes = ps.getInfectionTimes() ;
			String[] inf = infectionTimes.keySet().toArray(new String[0]) ;
			
			/*String initUser = ps.getArrayInit().get(0) ;*/
			
			
			double lastTime = 0 ;
			for(String u : inf) {
				if(infectionTimes.get(u)>lastTime)
					lastTime = infectionTimes.get(u) ;
			}
			String initUser ;
			do {
				initUser = inf[rng.nextInt(inf.length)] ;
			} while(infectionTimes.get(initUser)>=lastTime) ;
			//initUser = ps.getArrayInit().get(0) ;
			
			s1.addParametersFrom(this.z.get(initUser));
			s2.addParametersFrom(this.z.get(initUser));
			
			// Tirer un user infecte apres
			String useri ;
			do {
				useri = inf[rng.nextInt(inf.length)] ;
			} while(infectionTimes.get(useri)>infectionTimes.get(initUser)) ;
			
			// Tirer un autre gars infecte encore apres ou pas infecte.
			String userj ;
			do {
				userj = this.users.get(rng.nextInt(this.users.size())) ;
			} while(!infectionTimes.containsKey(userj) || infectionTimes.get(userj)>infectionTimes.get(useri)) ;
			
			zi.addParametersFrom(this.z.get(useri));
			zj.addParametersFrom(this.z.get(userj));
			
		}
		
		this.inputZsMinusZi.clearModules();
		this.inputZsMinusZi.addModule(s2);
		this.inputZsMinusZi.addModule(zi);
		
		this.inputZsMinusZj.clearModules();
		this.inputZsMinusZj.addModule(s1);
		this.inputZsMinusZj.addModule(zj);
		
		this.global.forward(null);
		
	}

	@Override
	public void backward() {
		this.global.backward(null);

	}


	@Override
	public HashSet<String> getUsers() {
		return new HashSet<String>(this.users)  ;
	}


	@Override
	public int getContentNbDims() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	public void test(){
		long step=1l;
		int nbInitSteps=1;
		users=new ArrayList<String>();
		users.add("a"); users.add("b"); users.add("c"); users.add("d"); users.add("e");
		//users.add("f"); users.add("g"); users.add("h"); users.add("i"); users.add("j");
		/*users.add("k"); users.add("l"); users.add("m"); users.add("n"); users.add("o");
		users.add("p"); users.add("q"); users.add("r"); users.add("s"); users.add("t");
		users.add("u"); users.add("v"); users.add("w"); users.add("x"); users.add("y");*/
		//this.train_users=new HashMap<String,HashMap<Integer,Double>>();
		
		train_cascades=new HashMap<Integer,PropagationStruct>();
		
		String[] cascades = {"a-b","b-a", "d-e", "e-d", "c-de" } ;
		//String[] cascades = {"ab-c","a-b","abc-d","efgh-d","ef-h","efh-g","eg-h","i-j","j-i"} ; 		
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
	
	
	public static void main(String[] args) {
		
		String db = "digg" ;
		
		MLPWSDM m = new MLPWSDM("",10, 50, 10.0) ;
		LineSearch lsearch = LineSearch.getFactorLine(0.05,1.0); 
		DescentDirection dir=DescentDirection.getGradientDirection();
		
		m.learn(db, "cascades_34", Optimizer.getDescent(dir, lsearch), 5000000);

		m.computeAllHeat() ;
		//System.out.println(m.allHeat.size());
		if(db!="test") {
			EvalPropagationModel.run(new EvalMLPWSDM(m,db, "cascades_35",new ArrayList<String>(m.getUsers())), "test") ;
		} else {
			System.out.println(m.evaltest(m.train_cascades)); 
		}
		
	}

	
}



class EvalMLPWSDM extends EvalPropagationModelConfig{

	private MLPWSDM mlpst ;
	
	/*public EvalMLPDiffSumThreshold(MLPDiffSumThreshold mlpst,String db,String cascades) {
			this.mlpst=mlpst ;
			
			pars.put("db",db); //usElections5000_hashtag");
			pars.put("cascadesCol",cascades);
			pars.put("step", "1");
			pars.put("nbInitSteps", "1");
			pars.put("nbCascades", "1000");
			pars.put("allUsers", "users_1");
	}*/
	
	public EvalMLPWSDM(MLPWSDM mlpst,String db,String cascades,ArrayList<String> users) {
		this.mlpst=mlpst ;
		//System.err.println("ATTENTION : INFER DOIT PAS MARCHER SI ON EST PAS EN RATIOINIT");
		pars.put("db",db); //usElections5000_hashtag");
		pars.put("cascadesCol",cascades);
		pars.put("step", "1");
		//pars.put("nbInitSteps", "1");
		pars.put("nbCascades", "1000");
		pars.put("ratioInits", "1.0") ;
		pars.put("maxInits", "1.0") ;
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
		for(String s : mlpst.getUsers()) { 
			u.add(s) ;
		}*/
		MAP map = new MAP(this.allUsers,true) ;
		ArrayList<EvalMeasure> arrayev=new ArrayList<EvalMeasure>(1) ;
		arrayev.add(map) ;
		EvalMeasureList ev = new EvalMeasureList(arrayev) ;
		return ev;
	}
}
