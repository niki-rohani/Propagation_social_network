package simon.test;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import propagationModels.IC;
import propagationModels.MLPproj;
import propagationModels.NaiveLink;
import propagationModels.NaiveNode;
import propagationModels.PropagationModel;
import trash.ICold;
import experiments.ErrorProba;
import experiments.EvalMeasure;
import experiments.EvalMeasureList;
import experiments.EvalPropagationModel;
import experiments.EvalPropagationModelConfig;
import experiments.FMeasure;
import experiments.LogLikelihood;
import experiments.MAP;
import experiments.MeanRank;
import experiments.NbContaminated;
import experiments.Precision;
import experiments.PrecisionForRecall;
import experiments.Recall;

public class TestMLPproj {
	
	public static void main(String args[]) {
		
		
				
		/*MLPproj m = new MLPproj("testMLPproj/propagationModels/MLPProj_Dims-100_step-1_ratioInits-1.0_nbMaxInits--1_db-digg_cascadesCol-cascades_1_filtered_trainAll_4_3_dP-false_tS-true_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-5_unbiased-true_regul-0.0_multiSource-true/best") ;
		m.load();
		EvalPropagationModel.run(new EvalMLPproj(m,"digg", "cascades_1_filtered_test_4_3",  "cascades_1_filtered_pop2_4_3"), "tempresProj"+".test") ;
		*/
		
		if(args.length >0) {
			System.out.println("Usage : db testcascades testusers model resultsfile minprob step likelyhoodEval?");
			String db = args[0] ;
			String testcascades = args[1] ;
			String testusers = args[2] ;
			String model = args[3] ;
			String resultFiles = args[4] ;
			Double minprob = Double.parseDouble(args[5]) ;
			int step = Integer.parseInt(args[6]) ;
			boolean likelyhoodeval = Boolean.parseBoolean(args[7]) ;
			if(!likelyhoodeval) {
				EvalPropagationModelConfig5 ev = new EvalPropagationModelConfig5(db, testcascades, testusers,step) ;
				ev.addModel(new MLPproj(model,100,10,2,minprob),1);
				EvalPropagationModel.run(ev,resultFiles) ;
				return ;
			}else {
				EvalPropagationModelConfig5bis ev = new EvalPropagationModelConfig5bis(db, testcascades, testusers, step) ;
				ev.addModel(new MLPproj(model,0,1,3,minprob),1);
				EvalPropagationModel.run(ev,resultFiles) ;
			}
		} else {
		
			for(int i = 10; i<=10 ; i=i+2)  {
				
				//MLPproj m = new MLPproj("testMLPproj/propagationModels/MLPProj_Dims-10_step-1_ratioInits-1.0_nbMaxInits--1_db-digg_cascadesCol-cascades_1_limited_"+i+"_10_dP-false_tS-true_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-5_unbiased-true_regul-0.0_multiSource-true/last") ;
				//MLPproj m = new MLPproj("testMLPproj/propagationModels/last") ;
				//m.load();
				if(i==6)
					continue ;
				//EvalPropagationModelConfig4 ev = new EvalPropagationModelConfig4("digg", "cascades_2", "users_1") ;
				//EvalPropagationModelConfig4 ev = new EvalPropagationModelConfig4("digg", "cascades_1_filtered_test_4_3", "cascades_1_filtered_pop2_4_3") ;
				//ev.addModel(new MLPproj("testMLPproj/propagationModels/last25dims",100,10,2,0.2),1);
				//ev.addModel(new MLPproj("testMLPproj/propagationModels/last25dims",100,10,2,0.3),1);
				//ev.addModel(new MLPproj("testMLPproj/propagationModels/MLPProj_Dims-10_step-1_ratioInits-1.0,1.0_nbMaxInits--1,-1_db-digg,digg_cascadesCol-cascades_1_filtered_trainA_4_3,cascades_1_filtered_trainB_4_3/dP-false_tS-true_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-5_unbiased-true_regul-0.0_multiSource-true/best",100,10,2,0.1),1);
				//ev.addModel(new MLPproj("testMLPproj/propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-digg_cascadesCol-cascades_1_limited_2_10_dP-false_tS-true_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-5_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.1),1) ;
				//ev.addModel(new MLPproj("testMLPproj/lastTrainAll",100,10,2,0.1),1);
				//EvalPropagationModel.run(ev,"tempresLimitedminProb"+i+".test") ;
				//EvalPropagationModel.run(ev,"tempresFiltered"+".test") ;
				
				
			}
			
			EvalPropagationModelConfig4 ev = new EvalPropagationModelConfig4("digg", "cascades_1_filtered_test_4_3_true", "cascades_1_filtered_pop2_4_3_true") ;
			ev.addModel(new IC("testIC/propagationModels/ICmodel_step1_ratioInits-1.0,1.0_nbMaxInits--1,-1_db-digg,digg_cascadesCol-cascades_1_filtered_trainA_4_3_true,cascades_1_filtered_trainB_4_3_true_start1,1_nbC-1,-1/usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",10,2),20);
			EvalPropagationModel.run(ev,"testIC/res.digg.unlearnable.test");
			
			/*ev = new EvalPropagationModelConfig4("digg", "cascades_1_filtered_4_3_true", "users_1") ;
			ev.addModel(new IC("testIC/propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-digg_cascadesCol-cascades_1_filtered_trainB_4_3_true_start1_nbC-1_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",10,2),20);
			EvalPropagationModel.run(ev,"testIC/res.twitter.test");*/
		}
		
	}
	
}



class EvalMLPproj extends EvalPropagationModelConfig{

	private LinkedHashMap<PropagationModel,Integer> mlpst ;
	
	public  EvalMLPproj(MLPproj mlp,String db,String cascades, String users) {
		this.mlpst=new LinkedHashMap<PropagationModel, Integer>() ;
		this.mlpst.put(mlp, 50) ;
		
		pars.put("db",db); //usElections5000_hashtag");
		pars.put("cascadesCol",cascades);
		pars.put("step", "1");
		pars.put("ratioInits", "0.3") ;
		pars.put("maxInits", "1000.0") ;
		pars.put("nbCascades", "1000");
		pars.put("allUsers", users);
		//this.allUsers=new HashSet<String>(mlpc.getUsers()) ;
		
		this.loadAllUsers();
		
	}
	
	@Override
	public LinkedHashMap<PropagationModel, Integer> getModels() { 
		return this.mlpst;
	}

	@Override
	public EvalMeasureList getMeasures() {
		/*HashSet<String> u = new HashSet<String>() ;
		for(String s : ) { // ON EST SUR DE ça ?
			u.add(s) ;
		}*/
		MAP map = new MAP(this.allUsers,true) ;
		ArrayList<EvalMeasure> arrayev=new ArrayList<EvalMeasure>(1) ;
		arrayev.add(map) ;
		EvalMeasureList ev = new EvalMeasureList(arrayev) ;
		return ev;
	}
}



class EvalPropagationModelConfig1 extends EvalPropagationModelConfig{
	public LinkedHashMap<PropagationModel,Integer> getModels(){
		LinkedHashMap<PropagationModel,Integer> mods=new LinkedHashMap<PropagationModel,Integer>();
		int nbIt=10;
		
		PropagationModel mod=new ICold("propagationModels/ICmodel_3600_1_cascades2_users1s.txt",50);
		mods.put(mod,nbIt);
		mod=new NaiveNode("propagationModels/naiveNodeModel_cascades1.txt");
		mods.put(mod,nbIt);
		mod=new NaiveLink("propagationModels/naiveLinkModel_cascades1_users3.txt");
		mods.put(mod, nbIt);
		/*mod = new CTICmodel("propagationModels/CTIC_cascades2_users1.txt") ;
		mods.put(mod, nbIt);*/
		return(mods);
	}
	public EvalMeasureList getMeasures(){
		ArrayList<EvalMeasure> ev=new ArrayList<EvalMeasure>();
		ev.add(new NbContaminated());
		ev.add(new Recall(true));
		ev.add(new Precision(true,true));
		if (allUsers==null){
			loadAllUsers();
		}
		ev.add(new PrecisionForRecall(allUsers,10,true));
		ev.add(new MAP(allUsers,false));
		ev.add(new MAP(allUsers,true));
		ev.add(new ErrorProba(allUsers,true));
		ev.add(new LogLikelihood(allUsers));
		EvalMeasureList mes=new EvalMeasureList(ev);
		return(mes);
	}
}

class EvalPropagationModelConfig4 extends EvalPropagationModelConfig1{
	private String model; //cascadesTrain;
	private int nbDims;
	LinkedHashMap<PropagationModel,Integer> mods=new LinkedHashMap<PropagationModel,Integer>();
	public EvalPropagationModelConfig4(String db, String cascades,String users){
		super();
		pars.put("db",db);
		pars.put("cascadesCol", cascades);
		pars.put("allUsers", users);
		pars.put("nbMaxInits", "1");
		pars.put("ratioInits", "0.3");
		pars.put("nbCascades", "1000");
	}
	
	public void addModel(PropagationModel mod,Integer nb){
		mods.put(mod,nb);
	}
	
	public LinkedHashMap<PropagationModel,Integer> getModels(){
		return mods;
	}
	public EvalMeasureList getMeasures(){
		ArrayList<EvalMeasure> ev=new ArrayList<EvalMeasure>();
		ev.add(new NbContaminated());
		ev.add(new FMeasure(true,1));
		if (allUsers==null){
			loadAllUsers();
		}
		ev.add(new MAP(allUsers,true));
		ev.add(new LogLikelihood(allUsers));
		EvalMeasureList mes=new EvalMeasureList(ev);
		return(mes);
	}
}

class EvalPropagationModelConfig5 extends EvalPropagationModelConfig1{
	private String model; //cascadesTrain;
	private int nbDims;
	LinkedHashMap<PropagationModel,Integer> mods=new LinkedHashMap<PropagationModel,Integer>();
	public EvalPropagationModelConfig5(String db, String cascades,String users,int step){
		super();
		pars.put("db",db);
		pars.put("cascadesCol", cascades);
		pars.put("step", ""+step);
		pars.put("allUsers", users);
		pars.put("nbMaxInits", "1");
		pars.put("ratioInits", "0.3");
		pars.put("nbCascades", "1000");
	}
	
	public void addModel(PropagationModel mod,Integer nb){
		mods.put(mod,nb);
	}
	
	public LinkedHashMap<PropagationModel,Integer> getModels(){
		return mods;
	}
	public EvalMeasureList getMeasures(){
		ArrayList<EvalMeasure> ev=new ArrayList<EvalMeasure>();
		ev.add(new NbContaminated());
		ev.add(new FMeasure(true,1));
		if (allUsers==null){
			loadAllUsers();
		}
		ev.add(new MAP(allUsers,true));
		ev.add(new LogLikelihood(allUsers));
		EvalMeasureList mes=new EvalMeasureList(ev);
		return(mes);
	}
}

class EvalPropagationModelConfig5bis extends EvalPropagationModelConfig{
	private String model; //cascadesTrain;
	private int nbDims;
	LinkedHashMap<PropagationModel,Integer> mods=new LinkedHashMap<PropagationModel,Integer>();
	public EvalPropagationModelConfig5bis(String db, String cascades,String users,int step){
		super();
		pars.put("db",db);
		pars.put("cascadesCol", cascades);
		pars.put("allUsers", users);
		pars.put("nbMaxInits", "-1");
		pars.put("ratioInits", "1.0");
		pars.put("step", ""+step) ;
		pars.put("ignoreDiffInitFinallyLessThan", "0"); // if == 0, ignore nothing, if ==1, considers only cascades whose contamination continue after the init steps, if > 0, considers only cascades whose contamination continues at least on this number of users after init steps
		
	}
	
	public void addModel(PropagationModel mod,Integer nb){
		mods.put(mod,nb);
	}
	
	public LinkedHashMap<PropagationModel,Integer> getModels(){
		return mods;
	}
	public EvalMeasureList getMeasures(){
		ArrayList<EvalMeasure> ev=new ArrayList<EvalMeasure>();
		ev.add(new NbContaminated());
		ev.add(new FMeasure(false,1));
		if (allUsers==null){
			loadAllUsers();
		}
		ev.add(new MAP(allUsers,false));
		ev.add(new MeanRank(allUsers,false));
		ev.add(new LogLikelihood(allUsers));
		EvalMeasureList mes=new EvalMeasureList(ev);
		return(mes);
	}
}
