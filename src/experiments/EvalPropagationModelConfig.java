package experiments;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeMap;

import actionsBD.MongoDB;
import cascades.Cascade;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import propagationModels.*;
import trash.ICold;
//import trash.MLPDiag;
//import trash.MLPDiagContent;
//import trash.MLPTranslateContent;
import core.HashMapStruct;
import core.Post;

import java.util.HashMap;

import core.User;

public abstract class EvalPropagationModelConfig {
	protected HashMapStruct<String,String> pars;
	protected LinkedHashSet<String> allUsers=null;
	public EvalPropagationModelConfig(){
		pars=new HashMapStruct<String,String>();
		pars.put("db","diggPruned"); //usElections5000_hashtag");
		pars.put("cascadesCol", "cascades_2");
		pars.put("step", "1");
		pars.put("nbMaxInits", "1");
		pars.put("ratioInits", "0.3");
		pars.put("start", "1");
		pars.put("nbCascades", "1000");
		pars.put("ignoreDiffInitFinallyLessThan", "1"); // if == 0, ignore nothing, if ==1, considers only cascades whose contamination continue after the init steps, if > 0, considers only cascades whose contamination continues at least on this number of users after init steps
		pars.put("allUsers", "users_1");
	}
	protected void loadAllUsers(){
		allUsers=new LinkedHashSet<String>();
		String db=pars.get("db");
		String allUs=pars.get("allUsers");
		System.out.println("Load all users from "+allUs);
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,allUs);
		DBCursor cursor = col.find();
		try {
			while(cursor.hasNext()) {
				DBObject res=cursor.next();
				allUsers.add(res.get("name").toString());
			}
		} finally {
			cursor.close();
		}
	}
	// returns the propagation models to evaluate associated to number of times these models should be run (for stochastic models)
	public abstract LinkedHashMap<PropagationModel,Integer> getModels();
	public abstract EvalMeasureList getMeasures();
	public HashMapStruct<String,String> getParams(){return(pars);}
	public static EvalPropagationModelConfig getEvalPropagationModelConfig4(String db, String cascades,String users){
		return new EvalPropagationModelConfig4(db,cascades,users);
	}
	public static EvalPropagationModelConfig getEvalPropagationModelConfig5(String db, String cascades,String users){
		return new EvalPropagationModelConfig5(db,cascades,users);
	}
	public LinkedHashSet<String> getUsers(){
		if(allUsers==null){
			loadAllUsers();
		}
		return allUsers;
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

class EvalPropagationModelConfig2 extends EvalPropagationModelConfig1{
	public LinkedHashMap<PropagationModel,Integer> getModels(){
		LinkedHashMap<PropagationModel,Integer> mods=new LinkedHashMap<PropagationModel,Integer>();
		int nbIt=1000;
		
		PropagationModel mod; //
		//mod=new ICmodel("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_3_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_asPos-1",100);
		//mods.put(mod,nbIt);
		//mod=new ICLSN2("propagationModels/ICLSN_nbNodes=1_step=1_cascades=artificial_10_users=users_1_linkThreshold=2.0_maxIter=1_sansStepsVides_infiniteDelay");
		//mods.put(mod,1);
		/*mod=new ICLSN2("propagationModels/ICLSN_nbNodes=2_step=1_cascades=artificial_10_users=users_1_linkThreshold=2.0_maxIter=1_sansStepsVides_infiniteDelay");
		mods.put(mod,1);
		*/
		
		/*mod=new MultipleIC("propagationModels/MultipleIC_nbMods1_step1_cascadesartificial_10_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_asPos-1",1,50);
		//mod=new ICLSN2("propagationModels/ICLSN_nbNodes=2_step=1_cascades=artificial_8_users=users_1_linkThreshold=1.0_maxIter=1_sansStepsVides");
		mods.put(mod,1);
		*/
		/*mod=new MultipleIC("propagationModels/MultipleIC_nbMods1_step1_cascadesartificial_10_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_asPos-1_probaSeul0.0",1,50);
		mods.put(mod,1);
		mod=new MultipleIC("propagationModels/MultipleIC_nbMods1_step1_cascadesartificial_10_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_asPos-1_probaSeul0.1",1,50);
		mods.put(mod,1);
		mod=new MultipleIC("propagationModels/MultipleIC_nbMods2_step1_cascadesartificial_10_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_asPos-1_probaSeul0.0",2,50);
		//mod=new ICLSN2("propagationModels/ICLSN_nbNodes=2_step=1_cascades=artificial_8_users=users_1_linkThreshold=1.0_maxIter=1_sansStepsVides");
		mods.put(mod,1);
		*/
		/*mod=new MultipleIC("propagationModels/MultipleIC_nbMods1_step1_cascadesartificial_10_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_asPos-1_probaSeul0.0",1,50);
		mods.put(mod,1);
		mod=new MultipleIC("propagationModels/MultipleIC_nbMods2_step1_cascadesartificial_10_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_asPos-1_probaSeul0.0",2,50);
		mods.put(mod,1);
		*/
		/*mod=new MultipleIC("propagationModels/MultipleIC_nbMods1_step1_cascadesartificial_1_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_asPos1_probaSeul0.0",1,50);
		mods.put(mod,1);*/
		/*mod=new MultipleIC("propagationModels/MultipleIC_nbMods1_step1_cascadesartificial_1_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_asPos-1_probaSeul0.0",1,50);
		mods.put(mod,1);
		*/
		//mod=new MultipleIC("propagationModels/MultipleIC_nbMods4_step1_cascadesartificial_1_usersusers_1_linkThreshold1.0_maxIter1000_sansStepsVides_asPos-1_probaSeul0.0",4,50);
		//mods.put(mod,1);
		//mod=new MultipleIC("propagationModels/MultipleIC_nbMods3_step1_cascadesartificial_1_usersusers_1_linkThreshold1.0_maxIter1000_sansStepsVides_asPos1_probaSeul0.0",3,50);
		//mods.put(mod,1);
		
		//mod=new LinearModel("/home/bourigault/java/Propagation/Java/Propagation/testLm") ;
		//mods.put(mod,1) ;
		//mod=new ICmodel("propagationModels/ICmodel2_step=3600_cascades=cascades_3_users=users_1_linkThreshold=1.0_maxIter=1000_sansStepsVides",100);
		//mods.put(mod,nbIt);
		/*mod=new NaiveNodeModel("propagationModels/naiveNodeModel_cascades_3");
		mods.put(mod,nbIt);
		mod=new NaiveLinkModel("propagationModels/naiveLinkModel_cascades=cascades_3_users=users_1_linkThreshold=1");
		mods.put(mod, nbIt);*/
		/*mod = new CTICmodel("propagationModels/CTIC_cascades2_users1.txt") ;
		mods.put(mod, nbIt);
		*/
		
		//mod=new MLP1("propagationModels/MLP1_Dims=20_step=1_nbInit=1_db=usElections5000_hashtag_cascades=cascades_3/MLP1_28.07.2013_15.54.58_28.07.2013_19.58.09");
		//mods.put(mod, 1);
		//mod=new MLPDiagContent("propagationModels/MLPDiagContent_Dims=10_step=1_nbInit=1_db=usElections5000_hashtag_cascades=cascades_3/MLPDiiagContent_28.07.2013_22.46.16_29.07.2013_11.18.10");
		//mods.put(mod, 1);
		//mod=new MLPDiagContent("propagationModels/MLPDiagContent_Dims=100_step=1_nbInit=1_db=usElections5000_hashtag_cascades=cascades_3/MLPDiagContent_30.07.2013_8.40.05_31.07.2013_11.18.34");
		//mods.put(mod, 1);
		
		//mod=new MLPDiagContent("propagationModels/MLPDiagContent_Dims=20_step=1_nbInit=1_db=icwsmPruned_cascades=cascades_1/MLPDiagContent_03.09.2013_15.03.54_06.09.2013_15.03.54");
		//mods.put(mod, 1);
		
		//mod=new MLPDiagContent("propagationModels/MLPDiagContent_Dims=100_step=1_nbInit=1_db=icwsmPruned_cascades=cascades_1/MLPDiagContent_05.09.2013_15.30.58_06.09.2013_15.05.28");
		//mods.put(mod, 1);
		
		mod=new ICold("propagationModels/ICmodel_step1db_diggPruned_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_asPos-1",100);
		mods.put(mod, 10);
		
		
		
		/*mod=new MLPDiag("propagationModels/MLPDiag_Dims-100_step-1_nbInit-1_db-diggPruned_cascadesCol-cascades_1_lambda-0.0/last");
		mods.put(mod, 1);
		
		mod=new MLPDiagContent("propagationModels/MLPDiagContent_Dims-100_step-1_nbInit-1_db-diggPruned_cascadesCol-cascades_1_bias-true_boolContent-false_logisticDiag-false_maxIdStem-2000_lambda-0.0/last");
		mods.put(mod, 1);
		
		mod=new MLPTranslateContent("propagationModels/MLPTranslateContent_Dims-100_step-1_nbInit-1_db-diggPruned_cascadesCol-cascades_1_withDiag-true_boolContent-false_logisticTrans-false_maxIdStem-2000_lambda-0.0/last");
		mods.put(mod, 1);*/
		//mod=new MLP1("propagationModels/MLP1_Dims=200_step=1_nbInit=1_db=icwsmPruned_cascades=cascades_1/MLP1_06.09.2013_15.51.32_07.09.2013_12.24.33");
		//mod=new MLP1("propagationModels/MLP1_Dims=50_step=1_nbInit=1_db=icwsmPruned_cascades=cascades_1/MLP1_04.09.2013_16.45.16_05.09.2013_10.46.23");
		//mod=new MLP1("propagationModels/MLP1_Dims=100_step=1_nbInit=1_db=icwsmPruned_cascades=cascades_1/MLP1_05.09.2013_11.38.19_05.09.2013_15.07.37");
		//mods.put(mod, 1);

		
		return(mods);
	}
	
}
class EvalPropagationModelConfig3 extends EvalPropagationModelConfig1{
	private String model; //cascadesTrain;
	private int nbDims;
	public EvalPropagationModelConfig3(String[] args){
		super();
		pars.put("db",args[0]);
		pars.put("cascadesCol", args[1]);
		//if(args.length>3){
			model=args[2];
		/*}
		else{
			cascadesTrain=args[1];
		}
		nbDims=Integer.parseInt(args[2]);*/
	}
	public LinkedHashMap<PropagationModel,Integer> getModels(){
		LinkedHashMap<PropagationModel,Integer> mods=new LinkedHashMap<PropagationModel,Integer>();
		int nbIt=1000;
	
		PropagationModel mod;

		
		//mod=new CTIC("propagationModels/CTIC_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter26",1);
		//mods.put(mod, 1000);
		/*mod=new IC("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter26_asPos-1",2);
		mods.put(mod, 1000);
		*/
		//mod=new IC("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter26_asPos-1_addNeg-false_unbiased-0.001",2);
		//mods.put(mod, 1000);
		//mod=new IC("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_2_usersusers_1_linkThreshold1.0_maxIter814653_asPos-1_addNeg-false_unbiased--1.0",2);
		//mods.put(mod, 1000);
		
		//mod=new IC("propagationModels/ICmodel_step1db_enronAll_cascadescascades_2_usersusers_1_linkThreshold1.0_maxIter9077460001_asPos-1_addNeg-1.0_unbiased--1.0",2);
		//mods.put(mod, 1000);
		//mod=new IC("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_2_usersusers_1_linkThreshold1.0_maxIter814653_asPos-1_addNeg-1.0_unbiased--1.0",2);
		//mods.put(mod, 1000);
		
		//mod=new IC("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_2_usersusers_1_linkThreshold1.0_maxIter814653_asPos-1_addNeg-false_unbiased--1.0",1);
		//mods.put(mod, 1000);
		
		/*mod=new IC("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_3_usersusers_1_linkThreshold1.0_maxIter814653_asPos-1_addNeg-0.0_unbiased--1.0_l1reg-7.0",2);
		mods.put(mod, 100);
		mod=new IC("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_3_usersusers_1_linkThreshold1.0_maxIter814653_asPos-1_addNeg-0.0_unbiased--1.0_l1reg-12.0",2);
		mods.put(mod, 100);
		*/
		//mod=new IC("propagationModels/"+model,2);
		//mods.put(mod, 100);
		
		//mod=new NetRate("propagationModels/"+model,10,2,100);
		//mods.put(mod, 1);
		
		//mod=new CTIC("propagationModels/"+model,2);
		//mods.put(mod, 100);
		
		//mod=new CTIC("propagationModels/CTIC_step1db_digg_cascadescascades_2_usersusers_1_linkThreshold1.0",0);
		//mods.put(mod, 100);
		
		//mod=new IC("propagationModels/ICmodel_step1db_digg_cascadescascades_13_usersusers_1_linkThreshold1.0_maxIter19_asPos1_addNeg-0.0_unbiased-0.0_l1reg-0_lambdaReg-0.0_globalExtern-false_individualExtern-0.0",2);
		//mods.put(mod, 100);

		/*mod=new IC("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-0_lambdaReg-0.0_globalExtern-false_individualExtern-0.0",0);
		mods.put(mod, 100);
		mod=new IC("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-0_lambdaReg-0.0_globalExtern-false_individualExtern-0.0",1);
		mods.put(mod, 100);
		*/
		//mod=new CTIC("propagationModels/CTIC_step1db_usElections5000_hashtag_cascadescascades_5_usersusers_1_linkThreshold1.0",2);
		//mods.put(mod, 100);
		/*mod=new IC("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_3_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-0_lambdaReg-0.0_globalExtern-false_individualExtern-0.0",0);
		mods.put(mod, 100);
		mod=new CTIC("propagationModels/CTIC_step1db_usElections5000_hashtag_cascadescascades_3_usersusers_1_linkThreshold1.0",1);
		mods.put(mod, 100);
		mod=new IC("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_3_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-0_lambdaReg-0.0_globalExtern-false_individualExtern-0.0",1);
		mods.put(mod, 100);
		*/
		/*mod=new ICold("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-1000.0_globalExtern-false_individualExtern-0.0",2);
		mods.put(mod, 100);
		mod=new ICold("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-1000.0_globalExtern-false_individualExtern-0.0",0);
		mods.put(mod, 100);
		mod=new ICold("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-10.0_globalExtern-false_individualExtern-0.0",2);
		mods.put(mod, 100);
		mod=new ICold("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-10.0_globalExtern-false_individualExtern-0.0",0);
		mods.put(mod, 100);
		*/
		/*mod=new IC("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-30.0_globalExtern-false_individualExtern-0.0",2);
		mods.put(mod, 100);
		mod=new IC("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-30.0_globalExtern-false_individualExtern-0.0",0);
		mods.put(mod, 100);*/
		/*mod=new IC("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_3_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-30.0_globalExtern-false_individualExtern-0.0",1);
		mods.put(mod, 100);
		mod=new IC("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_3_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-30.0_globalExtern-false_individualExtern-0.0",2);
		mods.put(mod, 100);*/
		
		//mod=new NetRate("propagationModels/NetRate_step-1_nbInit-1_db-digg_cascadesCol-cascades_13_iInInit-false_law-1/best",10,2,100);
		//mods.put(mod, 1);
		
		//mod=new NaiveLinkModel("propagationModels/"+model);
		//mods.put(mod, 1);
		
		/*mod=new NaiveLinkModel("propagationModels/naiveLinkModel_db-usElections5000_hashtag_cascades-cascades_3_users-users_1_linkThreshold-1_iInInit=true_timeOriented-true");
		mods.put(mod, 1);
		mod=new IC("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_3_usersusers_1_linkThreshold1.0_maxIter814653_asPos-1_addNeg-0.0_unbiased--1.0_l1reg-0.0",2);
		mods.put(mod, 100);
		
		mod=new IC("propagationModels/ICmodel_step1db_usElections5000_hashtag_cascadescascades_3_usersusers_1_linkThreshold1.0_maxIter814653_asPos-1_addNeg-0.0_unbiased--1.0_l1reg-1.0",2);
		mods.put(mod, 100);*/
				
		//mod=new NaiveLinkModel("propagationModels/naiveLinkModel_db-digg_cascades-cascades_1_users-users_1_linkThreshold-1_iInInit=false_timeOriented-true");
		//mods.put(mod, 1);
		/*mod=new IC("propagationModels/ICmodel_step1db_icwsmPruned_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter32_asPos-1_addNeg-false_unbiased--1.0",1);
		mods.put(mod, 1000);
		mod=new IC("propagationModels/ICmodel_step1db_icwsmPruned_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter32_asPos-1_addNeg-false_unbiased--1.0",2);
		mods.put(mod, 1000);
		mod=new IC("propagationModels/ICmodel_step1db_icwsmPruned_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter32_asPos-1_addNeg-false_unbiased-0.01",1);
		mods.put(mod, 1000);
		mod=new IC("propagationModels/ICmodel_step1db_icwsmPruned_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter32_asPos-1_addNeg-false_unbiased-0.01",2);
		mods.put(mod, 1000);*/
		
		//
		/*mod=new IC("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter26_asPos-1_addNeg-true",1);
		mods.put(mod, 100);
		mod=new IC("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter26_asPos-1",1);
		mods.put(mod, 100);
		*/
		/*mod=new NaiveLinkModel("propagationModels/naiveLinkModel_db-digg_cascades-cascades_1_users-users_1_linkThreshold-1_iInInit=true_timeOriented-true");
		mods.put(mod, 1);
		*/
		/*mod=new IC("propagationModels/ICmodel_step1000db_enron_cascadescascades_2_usersusers_1_linkThreshold1.0_maxIter98603662_asPos-1",0);
		mods.put(mod, 1000);*/
		//mod=new IC("propagationModels/ICmodel_step1000db_enron_cascadescascades_2_usersusers_1_linkThreshold1.0_maxIter98603662_asPos-1",0,2);
		//mods.put(mod, 1000);
		
		/*mod=new CTIC("propagationModels/CTIC_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter26",0);
		mods.put(mod, 1000);
		mod=new IC("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter26_asPos-1",0);
		mods.put(mod, 1000);*/
		
		//mod=new MLPnaiveProj("propagationModels/MLPnaiveProj5_Dims-100_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_transSend-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-true_sim-5_unbiased-true_multiSource-false/best",0,1);
		/*try{
			mod.load();
			((MLPnaiveProj)mod).withDiagSenders=false;
		}
		catch(Exception e){
			throw new RuntimeException("file pb");
		}*/
		//mods.put(mod, 1);
		mod=new MLPproj("propagationModels/MLPProj_Dims-25_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_transSend-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true_multiSource-false/best",-1,100);
		//mod=new MLPnaiveProj("propagationModels/MLPnaiveProj5_Dims-25_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-false_transSend-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-true_sim-5_unbiased-true_multiSource-true_bis/last",2,10);
		mods.put(mod, 1);
		mod=new MLPproj("propagationModels/MLPProj_Dims-25_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_transSend-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true_multiSource-false/best",2,100);
		mods.put(mod, 1);
		/*mod=new MLPnaiveProj("propagationModels/MLPnaiveProj5_Dims-25_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-false_transSend-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-true_sim-5_unbiased-true_multiSource-true/last",100,100);
		mods.put(mod, 1);
		mod=new MLPnaiveProj("propagationModels/MLPnaiveProj5_Dims-25_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-false_transSend-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-true_sim-5_unbiased-true_multiSource-true_bis/last",100,100);
		mods.put(mod, 1);*/
		//
		
		//mod=new MLPnaiveLink("propagationModels/MLPnaiveLink_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_iInInit-true/best");
		//mods.put(mod, 1);

		
		//mod=new ModifiedIC("propagationModels/ModifiedIC_step3600000db_enron_cascadescascades_2_usersusers_1_linkThreshold1.0_maxIter3414_infiniteDelay-true");
		//mods.put(mod, 1000);
		//mod=new IC("propagationModels/ICmodel_step1db_digg_cascadescascades_1_usersusers_1_linkThreshold1.0_maxIter26_asPos-1",0);
		//mods.put(mod, 1000);
		
		/*mod=new NaiveLinkModel("propagationModels/naiveLinkModel_db-"+pars.get("db")+"_cascades-"+cascadesTrain+"_users-users_1_linkThreshold-1_iInInit=false_timeOriented-true");
		mods.put(mod, 1);
		*/
		
		/*mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-"+nbDims+"_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-true_transSend-true_transSendContent-false_diag-true_withDiagContent-false_withDiagSenders-true/best");
		mods.put(mod, 1);
		
		mod=new NaiveLinkModel("propagationModels/naiveLinkModel_db-"+pars.get("db")+"_cascades-"+cascadesTrain+"_users-users_1_linkThreshold-1_iInInit=true_timeOriented-true");
		mods.put(mod, 1);
		*/
		/*mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-100_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-4_unbiased-true/best");
		mods.put(mod, 1);
		*/
		/*mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj5_Dims-25_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-false_transSend-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-true_sim-5_unbiased-true_multiSource-true/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj5_Dims-100_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_transSend-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-true_sim-5_unbiased-true_multiSource-false/best");
		mods.put(mod, 1);
		
		mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj5_Dims-50_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_transSend-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-true_sim-5_unbiased-true_multiSource-false/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj5_Dims-25_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_transSend-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-true_sim-5_unbiased-true/best");
		mods.put(mod, 1);
		
		mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj5_Dims-25_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_transSend-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true_multiSource-false/best");
		mods.put(mod, 1);
		*/
		
							   
		/*mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj5_Dims-100_step-1_nbInit-1_db-enron_cascadesCol-cascades_2_iInInit-true_transSend-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-true_sim-5_unbiased-true_multiSource-false/best");
		mods.put(mod, 1);
		
		mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj5_Dims-50_step-1_nbInit-1_db-enron_cascadesCol-cascades_2_iInInit-true_transSend-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-true_sim-5_unbiased-true_multiSource-false/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj5_Dims-25_step-1_nbInit-1_db-enron_cascadesCol-cascades_2_iInInit-true_transSend-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-true_sim-5_unbiased-true_multiSource-false/best");
		mods.put(mod, 1);
		
		mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj5_Dims-100_step-1_nbInit-1_db-enron_cascadesCol-cascades_2_iInInit-true_transSend-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true_multiSource-false/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj5_Dims-50_step-1_nbInit-1_db-enron_cascadesCol-cascades_2_iInInit-true_transSend-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true_multiSource-false/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj5_Dims-25_step-1_nbInit-1_db-enron_cascadesCol-cascades_2_iInInit-true_transSend-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true_multiSource-false/best");
		mods.put(mod, 1);*/
		
		
		/*mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj2_Dims-25_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj2_Dims-50_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj5("propagationModels/MLPnaiveProj2_Dims-100_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true/best");
		mods.put(mod, 1);
		*/
		
		
		/*mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-10_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-25_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-50_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-100_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-false_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true/best");
		mods.put(mod, 1);
		*/
		/*mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-50_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-50_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-6_unbiased-true/best");
		mods.put(mod, 1);
		*/
		/*mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-30_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-4_unbiased-true_pI_-1/best_sauv");
		mods.put(mod, 1);
		mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-30_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-4_unbiased-true_pI_-1/best");
		mods.put(mod, 1);*/
		/*mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-40_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-4_unbiased-true/best");
		mods.put(mod, 1);
		
		mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-20_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-4_unbiased-true/best");
		mods.put(mod, 1);
		*//*mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-10_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-true_transSendContent-false_diag-true_withDiagContent-false_withDiagSenders-false_sim-2_unbiased-true/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-10_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-true_transSendContent-false_diag-true_withDiagContent-false_withDiagSenders-true_sim-2_unbiased-true/best");
		mods.put(mod, 1);
		mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-20_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-true_transSendContent-false_diag-true_withDiagContent-false_withDiagSenders-false_sim-2_unbiased-true/best");
		mods.put(mod, 1);
		*/
		//mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-50_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-false_sender-true_transSendContent-false_diag-true_withDiagContent-false_withDiagSenders-false_sim-2_unbiased-true/best");
		//mods.put(mod, 1);
		
		//mod=new MLPnaiveProj2("propagationModels/MLPnaiveProj2_Dims-50_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-true_sender-true_transSendContent-false_diag-true_withDiagContent-false_withDiagSenders-false_sim-2_unbiased-true/best");
		//mods.put(mod, 1);
		//mod=new MLPnaiveProj3("propagationModels/MLPnaiveProj2_Dims-50_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_iInInit-false_sender-true_transSendContent-false_diag-true_withDiagContent-false_withDiagSenders-false_sim-2_unbiased-true/best");
		//mods.put(mod, 1);
		
		 
		
		/*mod=new MLPnaiveProj("propagationModels/MLPnaiveProj_Dims-"+nbDims+"_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_iInInit-true_transSend-true_transSendContent-false_diag-true_withDiagContent-false_sim-1_unbiased-1/best");
		mods.put(mod, 1);*/
		/*mod=new ModifiedIC("propagationModels/ModifiedIC_step1db_"+pars.get("db")+"_cascades"+cascadesTrain+"_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_infiniteDelay-true");
		mods.put(mod, 100);
		*/
		//mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-"+nbDims+"_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-true_transSend-false_transSendContent-false_diag-true_withDiagContent-false/best");
		//mods.put(mod, 1); 
		/*mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-10_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-true_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1);
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-10_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-false_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-20_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-true_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-20_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-false_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-50_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-true_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-50_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-false_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-100_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-true_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-100_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-false_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-200_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-true_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-200_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-false_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-500_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-true_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-500_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-false_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-1000_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-true_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1); 
		mod=new MLPdiffusion("propagationModels/MLPdiffusion_Dims-1000_step-1_nbInit-1_db-"+pars.get("db")+"_cascadesCol-"+cascadesTrain+"_lambda-0.0_iInInit-false_transSend-true_transSendContent-false_diag-true_withDiagContent-false/best");
		mods.put(mod, 1);*/
		
		return mods;
	}
	/*protected void loadAllUsers(){
		allUsers=new HashSet<String>();
		
		String db=pars.get("db");
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,pars.get("cascadesCol"));
        DBCursor cursor = col.find();
        Post p=null;
        int nb=0;
        try {
            while(cursor.hasNext()) {
           	   User.reinitAllPosts(); // Pour alleger, on supprime les textes des posts dont on ne se sert plus
               Post.reinitPosts();
           	   DBObject res=cursor.next();
           	   Cascade c=Cascade.getCascadeFrom(res);
           	   System.out.println("nb posts = "+Post.posts.size());
               int ic=c.getID();
               PropagationStruct ps=new PropagationStruct(c,Long.valueOf(pars.get("step")),Integer.parseInt(pars.get("nbInitSteps")));
               TreeMap<Long,HashMap<String,Double>> inits=ps.getInitContaminated();
               TreeMap<Long,HashMap<String,Double>> steps=ps.getInfections();
               if(steps.size()<=inits.size()){
            	   continue;
               }
               
               HashSet<String> infected=new HashSet<String>((PropagationStruct.getPBeforeT(steps)).keySet()) ;
               
               for(String us:infected){
            	  allUsers.add(us);
               }
               
               
                
               //System.out.println("Cascade "+ic+" chargee");
            }
        } finally {
            cursor.close();
        }
        
		System.out.println("Warning : Temporary load of users => to remove");
		System.out.println("Nb users : "+allUsers.size());
		//System.out.println("Nb cascades = "+cascades_ids.size());
	}*/
}

class EvalPropagationModelConfig4 extends EvalPropagationModelConfig{
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
		pars.put("ignoreDiffInitFinallyLessThan", "1"); // if == 0, ignore nothing, if ==1, considers only cascades whose contamination continue after the init steps, if > 0, considers only cascades whose contamination continues at least on this number of users after init steps
		
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
		ev.add(new MeanRank(allUsers,true));
		ev.add(new PrecisionForRecall(allUsers,10,false));
		ev.add(new LogLikelihood(allUsers));
		EvalMeasureList mes=new EvalMeasureList(ev);
		return(mes);
	}
}

class EvalPropagationModelConfig5 extends EvalPropagationModelConfig{
	private String model; //cascadesTrain;
	private int nbDims;
	LinkedHashMap<PropagationModel,Integer> mods=new LinkedHashMap<PropagationModel,Integer>();
	public EvalPropagationModelConfig5(String db, String cascades,String users){
		super();
		pars.put("db",db);
		pars.put("cascadesCol", cascades);
		pars.put("allUsers", users);
		pars.put("nbMaxInits", "-1");
		pars.put("ratioInits", "1.0");
		pars.put("ignoreDiffInitFinallyLessThan", "0"); // if == 0, ignore nothing, if ==1, considers only cascades whose contamination continue after the init steps, if > 0, considers only cascades whose contamination continues at least on this number of users after init steps
		//pars.put("nbCascades", "1");
		//pars.put("start", "2");
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
		ev.add(new PrecisionForRecall(allUsers,10,false));
		EvalMeasureList mes=new EvalMeasureList(ev);
		return(mes);
	}
}
