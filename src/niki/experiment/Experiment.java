package niki.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;

















import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import propagationModels.PropagationStructLoader;
import core.User;
import experiments.EvalPropagationModel;
import experiments.Result;
import niki.tool.ClusterTool;
import niki.tool.UsersTool;
import niki.train.*;
import niki.test.*;


public class Experiment {
	public static int timeStep = 1;
	public static int coCascade = 0;
	public static int coSuccessor = 2;
	public static int coPredecessor = 3;
	
	int min, max, step, maxiter, nbsim, nbCascade;
	HashMap <Integer, Double> sim;
	int exp;
	String statFile;
	double lambda;
	double minLam;
	double maxLam;
	double stepLam;
	public static String similarities[] = {"sameCascadeSim", "timeStepSim", "SuccesseurSim", "PredecessorSim"};
	public static String simi[] = {"jacquart", "cosine", "successeur", "pred"};
	
	public Experiment (HashMap <Integer, Double> sim, int min, int max, int step, int horizon, int nbSim, int nbCascade,int exp,  double lambda, double minl, double maxl, double stepl) {
		this.min = min;
		this.minLam = minl;
		this.maxLam = maxl;
		this.stepLam = stepl;
		this.max = max;
		this.step = step;
		this.sim = sim;
		maxiter = horizon;
		nbsim = nbSim;
		this.nbCascade = nbCascade;
		this.exp = exp;
		this.lambda = lambda;
		this.statFile = "";
		this.statFile += "experience" + exp + getStringSim(this.sim);
		
	}
	
	public void run(int pass) throws Exception {
		System.out.println ("Debut des experiences");
		if (pass == 1 || pass == 8) {
			UsersTool.load("digg","digg", "users_1", "cascades_1", true, true, true);
			UsersTool.user = (HashMap<String, User>) User.users.clone();
		}
		PrintWriter writer  =  null ;
		PrintWriter writerTrain  =  null ;
		if (pass == 3) {
			writer = new PrintWriter(new BufferedWriter(new FileWriter("ic/result/csv/"+statFile+".csv", true)));
			writerTrain = new PrintWriter(new BufferedWriter(new FileWriter("ic/result/csv/"+statFile+"_train.csv", true)));
		}
		
		if (pass == 5)
			writer = new PrintWriter(new BufferedWriter(new FileWriter("naiveLink/result/csv/"+statFile+".csv", true)));
		int oldstep = step;
		if (min <= 100)
			step = 10;
		
		
		
		for (int i = min; i < max; i = i + step) {
			if (i > 100)
				step = oldstep;
			String similaritie = Experiment.getStringSimW(sim);
			String clCollection = "Kmean_"+similaritie+"_k_" + i;
			String uCollection = "user_Kmean_"+similaritie+"_k_" + i;
			
			System.out.println ("k " + sim + " : " + i);
			
			if (pass == 1) {	
				Clustering.run(i, sim, lambda);
			}
			
			if (pass == 6) {
				for (lambda = minLam; lambda < maxLam; lambda = lambda + stepLam) {
				UsersTool.realReset();
				clustering.Clustering cluster = ClusterTool.loadCluster(clCollection);
				User.loadUsersFrom("niki", uCollection);
				User.loadAllLinksFrom("niki", clCollection);
				String [] c = {"cascades_1", "cascades_2"};
				for (int cascade = 0; cascade < 2; cascade++) {
					 
				 ///////////////// Creation cascade /////////////////////////
					 ClusterTool.toCascade("niki", c[cascade], c[cascade]+"l" +"user_Kmean_"+similaritie+"_k_" + i + ((lambda==0)?"":"_"+lambda) , cluster, lambda);
				 }
				}
			}
			if (pass == 2) {
/***/			propagationModels.IC.first = false; // A SUPPRIMER AVANT COMMIT /***/ 
				IC.run(i, nbCascade, sim, lambda);
			} //*
			if (pass == 3) {
			 EvalPropagationModel.results.clear();
			 EvalIC.run (i, sim, String.valueOf (maxiter), String.valueOf(nbsim), lambda, "test");
   		 	 
			 Result res = EvalPropagationModel.results.get(EvalPropagationModel.results.size()-1);
			 if (i==min) {
                     for (String r: res.getScores().keySet())
					 writer.print(r+",");
				 writer.print("k,lambda,sim");
	 writer.println();
			 }
			 for (double r: res.getScores().values()) {
				 writer.print(r+",");
			 }
 writer.print(i+",");
			 writer.print(lambda+",");
			 writer.println(statFile);
			 
			 writer.flush();
			 EvalPropagationModel.results.clear();
			 EvalIC.run (i, sim, String.valueOf (maxiter), String.valueOf(nbsim), lambda, "train");
		 res = EvalPropagationModel.results.get(EvalPropagationModel.results.size()-1);
			 if (i==min) {
 for (String r: res.getScores().keySet())
					 writerTrain.print(r+",");
				 writerTrain.print("k,lambda,sim");
	 writerTrain.println();
			 }
			 for (double r: res.getScores().values()) {
				 writerTrain.print(r+",");
			 }
 writerTrain.print(i+",");
			 writerTrain.print(lambda+",");
			 writerTrain.println(statFile);
			 writerTrain.flush();
			} //*/
			
			if (pass == 1)
				UsersTool.reset();
			if (pass == 2)
				UsersTool.realReset();
			
			if (pass == 4) {
				NaiveLink.run(i, sim);
			}
			//*			
			if (pass == 5) {
		
				EvalNaiveLink.run (i, sim, String.valueOf (maxiter), String.valueOf(nbsim));
		
				Result res = EvalPropagationModel.results.get(EvalPropagationModel.results.size()-1);
				 if (i==min) {
	 for (String r: res.getScores().keySet())
						 writer.print(r+",");
					 writer.print("k,sim");
					 writer.println();
   		 }
				 for (double r: res.getScores().values()) {
					 writer.print(r+",");
				 }
 	 writer.print(i+",");
				 writer.println(statFile);
				 writer.flush();
			}
			
			
			/**/			
			if (pass == 7) {
				if (i == 0)
					continue;
				UsersTool.realReset();
				ClusterTool.saveClusterFile(getStringSimW(sim), i);
				
				
			}
			
			if (pass == 10) {
				UsersTool.realReset();
				ClusterTool.saveCascadeFile(getStringSimW(sim), i, lambda);
				
				
			}
			
			if (pass == 8) {
				 clustering.Clustering cluster = ClusterTool.loadCluster("Kmean_"+ getStringSimW(sim) + "_k_" + i);
				 UsersTool.createGroup("niki", "user_Kmean_"+getStringSimW(sim)+"_k_" + i, cluster, writer);
			}
			
			if (pass == 9) {
				UsersTool.realReset();
				clustering.Clustering cluster = ClusterTool.loadCluster(clCollection);
				User.loadUsersFrom("niki", uCollection);
				User.loadAllLinksFrom("niki", clCollection);
				String [] c = {"cascades_2"};
				for (int cascade = 0; cascade < 1; cascade++) {
					 
				 ///////////////// Creation cascade /////////////////////////
					 ClusterTool.toCascadeSucre("niki", c[cascade], c[cascade]+"l" +"user_Kmean_"+similaritie+"_k_" + i + ((lambda==0)?"":"_"+lambda) , cluster, lambda);
				 }
			
			}

			if (pass == 99) {
				
				UsersTool.realReset();
				clustering.Clustering cluster = ClusterTool.loadCluster(clCollection);
				User.loadUsersFrom("niki", uCollection);
				User.loadAllLinksFrom("niki", clCollection);
				String [] c = {"cascades_1"};
				for (int cascade = 0; cascade < 1; cascade++) {
					 
				 ///////////////// Creation cascade /////////////////////////
					 ClusterTool.toCascade("niki", c[cascade], c[cascade]+"l" +"user_Kmean_"+similaritie+"_k_" + i + ((lambda==0)?"":"_"+lambda) , cluster, lambda);
				 }
				
			}
			if (pass == 11) {
				UsersTool.realReset();
				ClusterTool.saveClusterCascadeFile(getStringSimW(sim), i, lambda);
			}
			
			if (pass == 20) {
				UsersTool.realReset();
				ClusterTool.saveUserLinkFile(getStringSimW(sim), i, lambda);
			}
			
		}


		
		 writer.close();
		
		
	}
	
	
	public static void main (String [] args) throws IOException {
		HashMap <Integer, Double> simies = new HashMap <Integer, Double> ();
		
		/**
		 Step = 1 : Creation de groupe + transformation des cascades selon le groupe les cascade et lambda
		 Step = 2 : Creation model IC
		 Step = 3 : Evaluation model 
		 Step = 4 : Creation model NaiveLink
		 Step = 5 : Evaluation model
		 Step = 6 : transformation des cascades selon lambda (si jamais on veux juste modifier lambda)
		 Step = 7 : Dump des clusters dans un fichier
		 Step = 8 : Creation des users ( si on veut refaire l'etape de transformation des users )
		**/
		//////////////////
		/*************************** STEP *****************************/
								int step = 6; 
		/************************************************************/
		double tresholdInfect = 0.0;
		double minThres = 0.0;
		double maxThres = 0.01;
		double stepThres = 0.01;
		
		int kmin = 610;
		int kmax = 1113;
		// int similaritieChoice = timeStep;
	    // int similaritieChoice = coCascade;
		// int similaritieChoice = coSuccessor;
		int experienceNumber = 6;
		int step_k = 100;
		
		/** EXPERIENCE 8 = threshold 0.3 SimSuccessor
		*
		
		*/
		/********************** Similaritie ***********************/
	
		//	simies.put(coCascade, 0.5);    ///// EXPERIENCE 4
		//	simies.put(coSuccessor, 0.5);
		
		//	simies.put(coCascade, 1.0); ///////// EXPERIENCE 1
		
		//	simies.put(timeStep, 1.0); ///////// EXPERIENCE 1
		
		//  simies.put(coSuccessor, 1.0); ///////// EXPERIENCE 1
		
			simies.put(coPredecessor, 0.5); ///// EXPERIENCE 6
			simies.put(coSuccessor, 0.5);
		
		//	simies.put(coPredecessor, 1.0); ///// EXPERIENCE 7
		/*********************************************************/
		//////////////////////////////////////////
		
		
		
		// Sim = 0 : Coexistance de cascade, jacquart
		// Sim = 1 : Coexistance de temps dans la cascade, cosine
		// Sim = 2 : Nombre de successeur communs, jacquart
		
		try {
			new Experiment (simies, kmin, kmax, step_k, -1, 10, 500, experienceNumber, tresholdInfect, minThres, maxThres, stepThres).run(step);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String getStringSim (HashMap <Integer, Double> simies) {
		String r = "_sim";
		for (int sims: simies.keySet()) {
			r+= "_" + similarities[sims];
		}
		return r;
	}
	
	public static String getStringSimW (HashMap <Integer, Double> simies) {
		String r = "";
		for (int sims: simies.keySet()) {
			r+= "_" + similarities[sims] + "." + simies.get(sims);
		}
		return r;
	}
	
}
