package propagationModels;

import mlp.*;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import cascades.Cascade;
import core.Link;
import core.Post;
import core.Structure;
import core.Text;
import core.User;

import java.util.ArrayList;

public abstract class MLP extends MLPModel implements PropagationModel, Serializable {	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected HashMap<Integer,PropagationStruct> train_cascades;
	protected HashMap<String,HashMap<Integer,Double>> train_users;
	protected ArrayList<String> users;
	protected ArrayList<Integer> cascades_ids;
	protected HashMap<String,HashMap<Integer,Long>> users_cascades;
	//protected HashMap<String,String> couplesInTrain;
	protected String last_save;
	protected String model_name;
	protected String step;
	protected double nbCascadesPerUser;
	
	//protected Parameters params;
	
	
	public MLP(String model_file){
		super(new Parameters(),model_file);
		last_save="";
		model_name="";
	}
	
	
	public String getModelFile(){
		return model_file;
	}
	public String getName(){
		String sm=model_file.replaceAll("/", "__");
		return sm;
	}
	public void prepareLearning(PropagationStructLoader prop){
		/*this.prepareLearning(db, cascadesCollection, step, ratioInits, nbMaxInits, -1);
		}
		public void prepareLearning(String db, String cascadesCollection, long step, double ratioInits,int nbMaxInits, int nbMax){*/
		train_cascades=prop.getCascades();
		train_users=prop.getUsers_profiles();
		users_cascades=prop.getUsers_cascades();
        nbCascadesPerUser=0.0;
		for(String us:users_cascades.keySet()){
			nbCascadesPerUser+=users_cascades.get(us).size();
		}
		
		
		this.users=new ArrayList<String>(train_users.keySet());
		nbCascadesPerUser=(1.0*nbCascadesPerUser)/users.size();
		cascades_ids=new ArrayList<Integer>(train_cascades.keySet());	
		this.step=prop.getStep();
		System.out.println("Step : "+step);
		System.out.println("Nb users : "+users.size());
		System.out.println("Nb cascades = "+cascades_ids.size());
	}
	
}


