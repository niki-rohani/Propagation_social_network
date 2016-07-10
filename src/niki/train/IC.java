package niki.train;

import java.util.HashMap;

import niki.experiment.Experiment;
import niki.tool.UsersTool;
import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

import core.User;
import propagationModels.PropagationStructLoader;

public class IC {

    public static void train(String model, long maxIter, int inferMode, String db, String collection, String usersCollection, int nbIter, int nb_cascade) {
            propagationModels.IC ic = new propagationModels.IC(model, maxIter, inferMode);
            ic.learn(new PropagationStructLoader (db, collection, 1, 1.0, -1,1, -1), db, usersCollection, nbIter, 1, 0.3);
            ic.save();
    }
	
	
	public static void run (int k, int nb_cascade, HashMap <Integer, Double> similaritie, double lambda) throws Exception {
		String cascade = "cascades_1";
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
		casc = cascade + "l" + user + ((lambda==0)?"":"_"+lambda);
		db = "niki";
		}
		UsersTool.realReset();
		System.out.println("Loading user");
		System.out.println("Before " + User.users);
		User.loadUsersFrom(db, user);
		User.loadAllLinksFrom(db, user);
		
	
		
		
		System.out.println("After : " + User.users);
		//String casc = "cascades_1_unique_train";
		//String user = "users_1";
		train("ic/" + user + ((lambda==0)?"":"_"+lambda), 100, 2, db, casc, user, 100, nb_cascade);
		System.out.println ("Training on " + casc + " with user " + user);
	}
}
