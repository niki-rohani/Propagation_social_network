package simon.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

import cascades.CascadesLoader;
import experiments.EvalMeasure;
import experiments.EvalMeasureList;
import experiments.EvalPropagationModelConfig;
import experiments.MAP;
import mlp.DescentDirection;
import mlp.LineSearch;
import propagationModels.MLPdiffusion;
import propagationModels.PropagationModel;
import propagationModels.MLPproj; 
import trash.ICold;
import mlp.Optimizer;
import experiments.EvalPropagationModelConfig; 
import experiments.EvalPropagationModel; 
import mlp.Env;
public class TestMLPDiff {

	public static void main(String[] args) {
		
		
		
		/*MLPdiffusion mlpdif= new MLPdiffusion(100, 0, false, false, false, false, true, false, true) ;
		mlpdif.learn("digg", "cascades_1", 1, 1, Optimizer.getDescent(dir,lsearch), 0.1, false, false);
		
		EvalPropagationModel.run(new EvalMLPDiff(mlpdif), "/home/bourigaults/Bureau/testComm/verifDiff") ;*/
		
 		for(int dim : new int[]{50}) {
 			for(boolean withDiag : new boolean[]{true}) {
 				for(boolean withDiagSender : new boolean[]{false}) {
 					for(boolean transSend : new boolean[]{true}) {
			 			//MLPdiffusion mlpf = new MLPdiffusion(dim, 0,false,false,withDiag,false,transSend,false,withDiagSender);
 						MLPdiffusion mlpf = new MLPdiffusion("/home/bourigaults/workspace/Propagation/propagationModels/MLPdiffusion_Dims-"+dim+"_step-1_nbInit-1_db-digg_cascadesCol-cascades_1_lambda-0.0_iInInit-false_transSend-"+transSend+"_transSendContent-false_diag-true_withDiagContent-false_withDiagSenders-"+withDiagSender+"_unbiasedfalse/last");
 						Env.setVerbose(0) ;
			 			LineSearch lsearch=LineSearch.getFactorLine(0.0003,0.9999999); 
			 	 		DescentDirection dir=DescentDirection.getGradientDirection();
			 	 		dir = DescentDirection.getGradientDirection() ;
			 			//mlpf.learn("digg", "cascades_1", 1, 1, Optimizer.getDescent(dir,lsearch), 0.0, false, false);
			 			EvalPropagationModel.run(new EvalMLPDiff(mlpf), "/home/bourigaults/Bureau/testComm/verifDiff_"+dim+withDiag+withDiagSender+transSend) ;
			 			File f=new File("stop.txt") ;
			 			f.delete() ;
			 			
 					}
 				}
 			}
 		}
 		/*String db="digg";
    	String cascadesCol="cascades_1";
    	String users="users_1";
        CascadesLoader loader=new CascadesLoader(db, cascadesCol,1,false);
        IC myModel = new IC(-1,"/home/bourigaults/Bureau/testComm/verifDiff_IC/modelfile"); // "propagationModels/ICmodel2_3600_1_cascades2_users1s.txt",50) ;
        myModel.learn(loader,db, users, 10, 1, 0.1, 0.3,0.0,0.0,0,0.0,0.0,0.0); //0.00001); //Double.valueOf(args[5]));
        myModel.save();
        EvalPropagationModel.run(new EvalIC(new IC("/home/bourigaults/Bureau/testComm/verifDiff_IC/modelfile",0)), "/home/bourigaults/Bureau/testComm/verifDiff_IC") ;
 		//IC.main(("digg cascades_1 users_1 -1 1 50 0.0 0.0 0 0.0 0.0 0.0").split(" "));*/
		
	}

}


class EvalMLPDiff  extends EvalPropagationModelConfig{

	private MLPdiffusion mlpc ;
	
	public EvalMLPDiff( MLPdiffusion mlpd) {
		this.mlpc=mlpd ;
		
		pars.put("db","digg"); //usElections5000_hashtag");
		pars.put("cascadesCol", "cascades_2");
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

/*class EvalIC  extends EvalPropagationModelConfig{

	private ICold ic ;
	
	public EvalIC( ICold mlpd) {
		this.ic=mlpd ;
		
		pars.put("db","digg"); //usElections5000_hashtag");
		pars.put("cascadesCol", "cascades_2");
		pars.put("step", "1");
		pars.put("nbInitSteps", "1");
		pars.put("nbCascades", "1000");
		pars.put("ignoreDiffInitFinallyLessThan", "1");
		pars.put("allUsers", "users_1");
		
	}
	
	@Override
	public LinkedHashMap<PropagationModel, Integer> getModels() { 
		LinkedHashMap<PropagationModel,Integer> h=new  LinkedHashMap<PropagationModel, Integer>();
		h.put(ic, 1000) ;
		return h;
	}

	@Override
	public EvalMeasureList getMeasures() {
		HashSet<String> u = new HashSet<String>() ;
		this.loadAllUsers() ;
		//for(String s : this.allUsers) { // ON EST SUR DE ça ?
		//	u.add(s) ;
		//}
		MAP map = new MAP(allUsers,true) ;
		ArrayList<EvalMeasure> arrayev=new ArrayList<EvalMeasure>(1) ;
		arrayev.add(map) ;
		EvalMeasureList ev = new EvalMeasureList(arrayev) ;
		return ev;
	}
	
	
}*/
