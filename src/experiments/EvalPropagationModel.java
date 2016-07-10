package experiments;

import java.util.TreeMap;

import actionsBD.MongoDB;
import cascades.Cascade;
import core.HashMapStruct;
import core.Post;
import core.User;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;

import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Structure;
import propagationModels.*;

import java.util.HashMap;
import java.util.HashSet;
public class EvalPropagationModel implements Experiment{
	
	private PropagationModel model;
	private EvalMeasureList mes;
	private ResultFile rf;
	private int nbIterations;
	private String name;
	private EvalPropagationModelConfig evalConfig;
	public static ArrayList <Result> results = new ArrayList <Result> (); // Pour Niki
	
	public EvalPropagationModel(PropagationModel mod,EvalPropagationModelConfig evalConfig,int nbIterations,EvalMeasureList mes, String name, String output){
		//models=new ArrayList<PropagationModel>();
		//loadModels();
		this.model=mod;
		this.mes=mes;
		this.evalConfig=evalConfig;
		this.nbIterations=nbIterations;
		rf=new ResultFile(output);
		this.name=name;
		//this.allUsers=allUsers;
		//loadMeasures();
	}
	
	
	
	public Result go(Structure struct){
		HashMapStruct<String,String> acas=(HashMapStruct<String,String>)struct;
		String db=acas.get("db");
		String cascadesCol=acas.get("cascadesCol");
		
		long step=Long.valueOf(acas.get("step"));
		//int nbInitSteps=Integer.parseInt(acas.get("nbInitSteps"));
		double ratioInits=1;
		if (acas.containsKey("ratioInits")){
			ratioInits=Double.valueOf(acas.get("ratioInits"));
		}
		int nbMaxInits=1;
		if (acas.containsKey("nbMaxInits")){
			nbMaxInits=Integer.parseInt(acas.get("nbMaxInits"));
		}
		int start=0;
		if (acas.containsKey("start")){
			start=Integer.parseInt(acas.get("start"));
		}
		int nbCascades=-1;
		if (acas.containsKey("nbCascades")){
			 nbCascades=Integer.parseInt(acas.get("nbCascades"));
		}
		int ignoreDiffInitFinallyLessThan=-1;
		if (acas.containsKey("ignoreDiffInitFinallyLessThan")){
			ignoreDiffInitFinallyLessThan=Integer.parseInt(acas.get("ignoreDiffInitFinallyLessThan"));
		}
		LinkedHashSet<String> allUsers=evalConfig.getUsers();
		ArrayList<Result> results=new ArrayList<Result>();
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,cascadesCol);
		DBObject orderBy=new BasicDBObject();
		orderBy.put("id", 1);
		DBCursor cursor = col.find().sort(orderBy).addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		try {
			int nb=0;
			int nbIgnore=0;
			while(cursor.hasNext()) {
				Post.reinitPosts();
				User.reinitAllPosts();
				//System.gc();
				nb++;
				
				if ((nbCascades>=0) && ((nb-nbIgnore)>=(nbCascades+start))){
					break;
				}
				DBObject res=cursor.next();
				if (nb>=start){
					
					//System.out.println(res);
					Cascade c=Cascade.getCascadeFrom(res);
					System.out.println("Cascade "+c.getID());
					PropagationStruct pstruct;
					pstruct=new PropagationStruct(c,step,ratioInits,nbMaxInits);
					//pstruct.setPossibleUsers(possibles);
					boolean ok=true;
					TreeMap<Long,HashMap<String,Double>> contamin=pstruct.getInfections();
					TreeMap<Long,HashMap<String,Double>> copyContamin=new TreeMap<Long,HashMap<String,Double>>();
					int nbu=0;
					for(Long l:contamin.keySet()){
						HashMap<String,Double> h=contamin.get(l);
						for(String s:h.keySet()){
							if(allUsers.contains(s)){
								HashMap<String,Double> h2=copyContamin.get(l);
								if(h2==null){
									h2=new HashMap<String,Double>();
									copyContamin.put(l, h2);
								}
								h2.put(s,h.get(s));
								nbu++;
							}
						}
					}
					contamin=copyContamin;
					pstruct.setInfections(contamin);
					if(nbu==0){
						System.out.println("Cascade retiree : aucun contamine dans l'ensemble d'utilisateurs considere ");
						ok=false;
						nbIgnore++;
					}
					
					if (ok && (contamin.size()==1)){
						System.out.println("Cascade retiree : 1 seul pas de temps ");
						ok=false;
						nbIgnore++;
					}
					if (ok && (ignoreDiffInitFinallyLessThan>0)){
						TreeMap<Long,HashMap<String,Double>> initConta=pstruct.getInitContaminated();
						
						HashMap<String,Double> conta=PropagationStruct.getPBeforeT(contamin);
						HashMap<String,Double> iconta=PropagationStruct.getPBeforeT(initConta);
						//System.out.println(conta.size()+" "+initConta.size());
						int dif=conta.size()-iconta.size();
						
						if (dif<ignoreDiffInitFinallyLessThan){
							System.out.println("Cascade retiree : init = "+iconta+" , conta = "+conta);
							ok=false;
							nbIgnore++;
						}
					
					}
					
					if (ok){
						Hyp hyp=new Hyp(pstruct,model,nbIterations);
						Result result=mes.eval(hyp);
						rf.append(result);
						results.add(result);
						System.out.println((nb-nbIgnore-start)+" traites");
					}
				}
				
			}
		}
		catch(RuntimeException e){
			throw e;
		}
		catch(Exception e){	
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cursor.close();
		}
		Result stats=Result.getStats(results);
		stats.setDonnee(name);
		
		return(stats);
	}
	public static void run(EvalPropagationModelConfig config){
		run(config,"");
	}
	public static void run(EvalPropagationModelConfig config, String outputRep){
		
		if (outputRep.length()==0){
			String format = "dd.MM.yyyy_H.mm.ss";
			java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
			Date date = new Date(); 
			String sdate=formater.format(date);
			outputRep="./Results/PropagationResults_"+sdate;
		}
		
		File rep=new File(outputRep);
		rep.mkdirs();
		
		HashMap<PropagationModel,Integer> models=config.getModels();
		EvalMeasureList evMes=config.getMeasures();
		HashMapStruct<String,String> pars=config.getParams();
		
		
		
		ResultFile rf=new ResultFile(outputRep+"/stats.txt");
		int i=1;
		String titre="EvalPropagationModel \n\n Modeles : \n";
		for(PropagationModel m:models.keySet()){
			titre+="\t model_"+i+": \t "+m.toString()+"\n";
			i++;
		}
		titre+="\n "+evMes.getName()+"\n";
		titre+="\n Params : \n";
		for(String par:pars.keySet()){
			titre+="\t "+par+" = "+pars.get(par)+"\n";
		}
		i=1;
		for (PropagationModel pm:models.keySet()){
			String modch="model_"+i;
			/*int x=modch.lastIndexOf("/");
			if(x>=0){
				modch=modch.substring(x+1);
			}*/
			Experiment expe=new EvalPropagationModel(pm,config,models.get(pm),evMes,modch,outputRep+"/"+modch+".txt");
			User.reinitUsers();
			Post.reinitPosts();
			//System.gc();
			Result res=expe.go(pars);
			if(i==1){
				res.setExperiment(titre);
			}
			try{
				rf.append(res);
				results.add(res); 
			}
			catch(IOException e){
				e.printStackTrace();
			}
			i++;
		}
		
	}
	
	public String getDescription(){
		return("EvalPropagationModels");
	}
	
	
	
	public static void main(String[] args){
		
		String db=args[0]; //"digg"; //"memetracker";//"usElections5000_hashtag";//"digg"; //"enronAll"; ///"usElections5000_hashtag";
		String cascades_train=args[1]; //"cascades_9";
		String cascades_test=args[2]; //"cascades_10";
		String users=args[3]; //"users_3";
		
		//IC.main((db+" "+cascades_train+" "+users+" 1 309832961 200 0.0 0.0 0 0.0 0.0 0.0").split(" "));
		
		/*IC.main((db+" "+cascades_train+" "+users+" -1 1 200 0.0 0.0 0 0.0 0.0 0.0").split(" "));
		
		IC.main((db+" "+cascades_train+" "+users+" -1 1 200 0.0 0.0 6 1.0 0.0 0.0").split(" "));
		
		IC.main((db+" "+cascades_train+" "+users+" -1 1 200 0.0 0.0 6 2.0 0.0 0.0").split(" "));
		
		IC.main((db+" "+cascades_train+" "+users+" -1 1 200 0.0 0.0 6 5.0 0.0 0.0").split(" "));
		
		IC.main((db+" "+cascades_train+" "+users+" -1 1 200 0.0 0.0 6 10.0 0.0 0.0").split(" "));
		
		IC.main((db+" "+cascades_train+" "+users+" -1 1 200 0.0 0.0 6 30.0 0.0 0.0").split(" "));
		
		IC.main((db+" "+cascades_train+" "+users+" -1 1 200 0.0 0.0 6 100.0 0.0 0.0").split(" "));*/
		
		//IC.main((db+" "+cascades_train+" "+users+" -1 1 50 0.0 0.0 0 0.0 0.0 0.0").split(" "));
		
		//NaiveLink.main((db+" "+cascades_train+" "+users+" true true").split(" "));
		//NaiveLink.main((db+" "+cascades_train+" "+users+" false true").split(" "));
		
		//NaiveNode.main((db+" "+cascades_train).split(" "));
		
		//CTIC.main((db+" "+cascades_train+" "+users+" 1 200").split(" "));
		
		EvalPropagationModelConfig4 conf=new EvalPropagationModelConfig4(db,cascades_test,users);
		
		
		////// Weibo//////////////
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,false),1);
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-100_step-1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,false),1);
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-100_step-1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,true),1);
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-100_step-1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-1.0_multiSource-true/last",100,10,2,0.0,false),1);
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-100_step-1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-1.0_multiSource-true/last",100,10,2,0.01,false),1);
		/*conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-100_step-1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,false),1);
		conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-100_step-1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,true),1);
		conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-100_step-1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true-rescale_mode4-zStat1.2/last",100,10,2,0.0,false),1);
		conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-100_step-1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true-rescale_mode4-zStat1.2/last",100,10,2,0.0,true),1);
		conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-100_step-1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true-rescale_mode2-zStat0.1/last",100,10,2,0.0,false),1);
		conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-100_step-1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true-rescale_mode2-zStat0.1/last",100,10,2,0.0,true),1);
		
		conf.addModel(new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/usersusers_3_linkThreshold1.0_contaMaxDelay-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",100,2),10);
		*/
		
		////// lastFM//////////////
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-lastfm_artists_cascadesCol-cascades_1_start1_nbC10000/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",0,1,3,0.0,false), 1);
		/*conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-lastfm_artists_cascadesCol-cascades_1_start1_nbC10000/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",0,1,3,0.0,true), 1);
		
		conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-lastfm_artists_cascadesCol-cascades_1_start1_nbC10000/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",0,1,3,0.01,false), 1);
		conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-lastfm_artists_cascadesCol-cascades_1_start1_nbC10000/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",0,1,3,0.01,true), 1);
		conf.addModel(new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-lastfm_artists_cascadesCol-cascades_1_start1_nbC10000/usersusers_1_linkThreshold1.0_contaMaxDelay-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",0,3),1);
			*/
			
		///// MemeTracker ////////
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-10_step-1_ratioInits-1.0_nbMaxInits--1_db-memetrackerNew_cascadesCol-cascades_2_start1_nbC10000/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-1_unbiased-true_regul-0.0_multiSource-true/last",100,100,0,0.0,false), 1);
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-15_step-1_ratioInits-1.0_nbMaxInits--1_db-memetrackerNew_cascadesCol-cascades_2_start1_nbC10000/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-1_unbiased-true_regul-0.0_multiSource-true/last",100,100,0,0.0,false), 1);
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-20_step-1_ratioInits-1.0_nbMaxInits--1_db-memetrackerNew_cascadesCol-cascades_2_start1_nbC10000/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-1_unbiased-true_regul-0.0_multiSource-true/last",100,100,0,0.0,false), 1);
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-memetrackerNew_cascadesCol-cascades_2_start1_nbC10000/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-1_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,false), 1);
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-memetrackerNew_cascadesCol-cascades_2_start1_nbC10000/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,false), 1);
		//conf.addModel(new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-memetrackerNew_cascadesCol-cascades_2_start1_nbC10000/usersusers_2_linkThreshold1.0_contaMaxDelay-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",100,2),10);
		//conf.addModel(new CTIC("propagationModels/CTIC_step-1_ratioInits-1.0_nbMaxInits--1_db-memetrackerNew_cascadesCol-cascades_2_usersusers_2_linkThreshold1.0",2),10);
		conf.addModel(new NetRate("propagationModels/NetRate_step-null_ratioInits-1.0_nbMaxInits--1_db-memetrackerNew_cascadesCol-cascades_2_law-1/last",100,0,10),1);
				
		
		//// Enron //////////////
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-enronAll_cascadesCol-cascades_2_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.01,false), 1);
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-enronAll_cascadesCol-cascades_2_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.01,true), 1);
		//conf.addModel(new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-enronAll_cascadesCol-cascades_2_start1_nbC-1/usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",100,2),10);
		
		
		//// Digg ///////////
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-digg_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,false), 1);
		/*
		conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-digg_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,true), 1);
		conf.addModel(new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-digg_cascadesCol-cascades_1_start1_nbC-1/usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",100,2),10);
		*/
		
		/////ICWSM//////////
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-icwsmPruned_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,false), 1);
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-icwsmPruned_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,true), 1);
		/*conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-icwsmPruned_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.01,false), 1);
		conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-icwsmPruned_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.01,true), 1);
		
		conf.addModel(new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-icwsmPruned_cascadesCol-cascades_1_start1_nbC-1/usersusers_1_linkThreshold1.0_contaMaxDelay-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",100,2),10);
		*/
		
		/////Irvine//////////
		/*conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-irvine_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,false), 1);	
		conf.addModel(new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-irvine_cascadesCol-cascades_1_start1_nbC-1/usersusers_2_linkThreshold1.0_contaMaxDelay-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",100,2),10);
		*/
		
		
		/////Tweeter ///////
		//conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-irvine_cascadesCol-cascades_1_start1_nbC-1/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,false), 1);	
		conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-usElections5000_hashtag_cascadesCol-cascades_3_start1_nbC1000/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,false), 1);	
		conf.addModel(new MLPproj("propagationModels/MLPProj_Dims-25_step-1_ratioInits-1.0_nbMaxInits--1_db-usElections5000_hashtag_cascadesCol-cascades_3_start1_nbC1000/dP-true_tS-false_tR-false_tSC-false_diag-false_wDC-false_wDS-false_wDR-false_sim-2_unbiased-true_regul-0.0_multiSource-true/last",100,10,2,0.0,true), 1);	
		
		conf.addModel(new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-usElections5000_hashtag_cascadesCol-cascades_3_start1_nbC1000/usersusers_1_linkThreshold1.0_contaMaxDelay-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",100,2),10);
		//884830 liens
		// 2841 users
		run(conf,args[args.length-1]);
	}
}
