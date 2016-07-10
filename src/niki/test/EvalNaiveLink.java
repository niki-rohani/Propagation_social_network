package niki.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import propagationModels.IC;
import propagationModels.NaiveLink;
import propagationModels.NaiveNode;
import propagationModels.PropagationModel;
import clustering.KernelKMeans;
import similarities.StrSim;
import trash.ICold;
import core.Text;
import core.User;
import niki.clustering.JacquartSim;
import niki.tool.ClusterTool;
import niki.tool.UsersTool;
import core.Data;
import experiments.ErrorProba;
import experiments.EvalMeasure;
import experiments.EvalMeasureList;
import experiments.EvalPropagationModel;
import experiments.EvalPropagationModelConfig;
import experiments.FMeasure;
import experiments.LogLikelihood;
import experiments.MAP;
import experiments.NbContaminated;
import experiments.Precision;
import experiments.PrecisionForRecall;
import experiments.Recall;
import niki.clustering.*;
import niki.clustering.node.DataUser;
import niki.clustering.node.UserByCascadeText;
import niki.experiment.Experiment;

public class EvalNaiveLink {

    public static void run(int k, HashMap <Integer, Double> similaritie,String maxIter,String nbSim)
            {
    				String cascade = "cascades_2";
             		 String user = "";
             		 String casc = "";
             		 String db = "";
             		 
             		if (k == 0) {
            			user = "users_1";
            			db = "digg";
            			casc = cascade;
            		}
             		else {
             		user = "user_Kmean_"+Experiment.getStringSimW(similaritie)+"_k_"+k;
             		casc = cascade + "l" + user;
             		db = "niki";
             		}
                     //String casc = "cascades_2"; // FAIRE cascades_1
                     // String user = "users_1";
                     
                     String likelihood = "false";
                     
                     EvalPropagationModelConfig5 ev = new EvalPropagationModelConfig5(db, casc, user,1) ;
    				ev.addModel(new NaiveLink("naiveLink/" + user),Integer.parseInt (nbSim));
    				EvalPropagationModel.run(ev,"naiveLink/result" + user) ;
    				
    			
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
