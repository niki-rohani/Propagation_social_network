package simon.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

//import com.sun.org.apache.xerces.internal.impl.dv.dtd.NMTOKENDatatypeValidator;

import experiments.EvalMeasure;
import experiments.EvalMeasureList;
import experiments.EvalPropagationModel;
import experiments.EvalPropagationModelConfig;
import experiments.MAP;
import mlp.DescentDirection;
import mlp.LineSearch;
import mlp.Optimizer;
import propagationModels.MLPnaiveLink ;
import propagationModels.MLPproj;
import propagationModels.MultiSetsPropagationStructLoader;
import propagationModels.NaiveLink;
import propagationModels.PropagationModel;
import propagationModels.PropagationStructLoader;

public class TestNaiveLink {

	public static void main(String args[]) {
		NaiveLink m = new NaiveLink() ;
		NaiveLink m2 = new NaiveLink() ;
		
		if(args.length >0) {
			System.out.println("Usage : db testcascades testusers model resultFiles");
			String db = args[0] ;
			String testcascades = args[1] ;
			String testusers = args[2] ;
			String model = args[3] ;
			String resultFiles = args[4] ;
			

			//PropagationStructLoader psl = new MultiSetsPropagationStructLoader(db, traincascades, 1,"0.3","1000");
	 		//m.learn(psl, db, trainUsers, 1, false);
			m = new NaiveLink(model) ;
			m.load(); 
	 		EvalPropagationModel.run(new EvalMLPnaiveLink(m,db, testcascades,  testusers), resultFiles) ;
			return ;
		}
	
 		PropagationStructLoader psl = new MultiSetsPropagationStructLoader("lastfm_song", "cascades_1", 1,"0.3","1000");
 		m.learn(psl, "lastfm_song", "users_1", 1, false);
 		
 		//psl = new PropagationStructLoader("digg", "cascades_1_filtered_test_4_3",  1,0.3,1000);
 		//m2.learn(psl, "digg", "cascades_1_filtered_pop2_4_3", 1, false);
 		
 		//EvalPropagationModel.run(new EvalMLPnaiveLink(m,"digg", "cascades_1_filtered_trainAll_4_3","cascades_1_filtered_popAll_4_3"), "tempresNaive"+".test") ;
 		//EvalPropagationModel.run(new EvalMLPnaiveLink(m,"digg", "cascades_1_filtered_trainA_4_3",  "cascades_1_filtered_pop1_4_3"), "tempresNaive2"+".test") ;
 		//EvalPropagationModel.run(new EvalMLPnaiveLink(m,"digg", "cascades_1_filtered_trainB_4_3",  "cascades_1_filtered_pop2_4_3"), "tempresNaive3"+".test") ;
 		EvalPropagationModel.run(new EvalMLPnaiveLink(m,"irvine", "cascades_2",  "users_1"), "tempresNaiveIrvine"+".test") ;
		
		
		
		/*for(int i = 1 ; i<=10 ; i++) {
			PropagationStructLoader psl = new PropagationStructLoader("digg", "cascades_1_limited_"+i+"_10",  1,0.3,1000);
	 		m.learn(psl, "digg", "users_1", 1, false);
	 		EvalPropagationModel.run(new EvalMLPnaiveLink(m,"digg", "cascades_2",  "users_1"), "tempresNaiveLimited_"+i+".test");
		}*/
 		
 		//EvalPropagationModel.run(new EvalMLPnaiveLink(m,"digg", "cascades_1_filtered_trainA_4_3","cascades_1_filtered_pop1_4_3"), "tempresNaive"+".train") ;
 	//	EvalPropagationModel.run(new EvalMLPnaiveLink(m,"digg", "cascades_1_filtered_test_4_3",  "cascades_1_filtered_pop2_4_3"), "tempresNaive"+".test") ;
 		//System.out.println(m.getProbas());
 		
 		
		
		
		
	}
	
}


class EvalMLPnaiveLink extends EvalPropagationModelConfig{

	private LinkedHashMap<PropagationModel,Integer> mlpst ;
	
	public  EvalMLPnaiveLink(NaiveLink mlpst,String db,String cascades, String users) {
		this.mlpst=new LinkedHashMap<PropagationModel, Integer>() ;
		this.mlpst.put(mlpst, 1) ;
		
		pars.put("db",db); //usElections5000_hashtag");
		pars.put("cascadesCol",cascades);
		pars.put("step", "1");
		pars.put("ratioInits", "1.0") ;
		pars.put("maxInits", "1.0") ;
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