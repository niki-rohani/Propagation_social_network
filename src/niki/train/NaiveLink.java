package niki.train;

import java.util.HashMap;

import niki.experiment.Experiment;
import niki.tool.UsersTool;
import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

import core.User;
import propagationModels.PropagationStructLoader;

public class NaiveLink {

    public static void train(String model, long maxIter, int inferMode, String db, String collection, String usersCollection, int nb_cascade) {
            propagationModels.NaiveLink naiveLink = new propagationModels.NaiveLink(model);
            naiveLink.learn(new PropagationStructLoader (db, collection, 1, 1.0, -1,1, -1), db, usersCollection, 1, true);
            naiveLink.save();
    }
	
	
	public static void run (int k, HashMap <Integer, Double> similaritie) throws Exception {
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
		casc = cascade + "l" + user;
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
		train("naiveLink/" + user, -1, 2, db, casc, user, 1000);
		System.out.println ("Training on " + casc + " with user " + user);
	}
}
