package niki.test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Set;

import clustering.KernelKMeans;
import similarities.StrSim;
import core.Text;
import core.User;
import niki.clustering.JacquartSim;
import niki.tool.ClusterTool;
import niki.tool.UsersTool;
import core.Data;
import niki.clustering.*;
import niki.clustering.node.DataUser;
import niki.clustering.node.UserByCascadeText;
import niki.experiment.Experiment;

public class Clustering {
	
	public static void run(int k, HashMap <Integer, Double> similarities, double lambda) 
		{
			
			int step = 2; boolean stepCount = false;
			String similaritie = Experiment.getStringSimW(similarities);
			
			
			 String clCollection = "Kmean_"+similaritie+"_k_" + k;
			 String uCollection = "user_Kmean_"+similaritie+"_k_" + k;
			
			
			
			try {
			/*
				// Cascades
				String [] collection = {"cascades_1"};
		
				if (!stepCount || step == 1) 
				// Load des users : false sans les liens, true avec liens, les liens sont souvent l'apparition dans les cascades
			 for (String db : collection) 
			 UsersTool.load("digg","digg", "users_1", db, true);
				
				else
					for (String db : collection) 
						 UsersTool.load("digg","digg", "users_1", db, false);
			*/
				
			
			 
			 
			 if (!stepCount || step == 0) {
				 System.out.println("Creation cluster " + k);
				 float time = System.currentTimeMillis();
//			 Cluster ////////////////////////////
				 StrSim sim = null;
				 sim = new MultipleSim (null, similarities);
			 
			 Data data = new DataUser (UsersTool.users);
			 // System.out.println (data.getTexts());
			 sim.setData(data);
			 KernelKMeans kMean = new KernelKMeans (k, 1);
			 clustering.Clustering cluster = kMean.clusterize(sim);
			 ClusterTool.saveCluster(cluster, clCollection);
//			 ///////////////////////////////////////////////////////////////////////////
			 System.out.println ("Creation effectue en " + (System.currentTimeMillis() / 100. - time / 100. ) + " secondes" );
			 if (stepCount)
			 System.exit(0);
			 }
			 
			 clustering.Clustering cluster = ClusterTool.loadCluster("Kmean_"+ similaritie + "_k_" + k);
			 PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("cluster/Kmean_"+similaritie+ "_k_" + k + ".csv", true)));
//			 System.out.println (loadCluster);
			// int iu1 = 2201;
			// int iu2 = 2107;
			// Text u1 = data.getText(iu1);
			// Text u2 = data.getText(iu2);
			// ClusterTool.toStatistic (u1, u2, "Jacquart");
			//System.out.println(sim.sum_sim_el_with_group(iu1, loadCluster.getCluster(0)));
			//System.out.println(sim.sum_sim_el_with_group(iu1, loadCluster.getCluster(1)));

			// clustering.Clustering loadCluster = ClusterTool.loadCluster(clCollection);
			 if (!stepCount || step == 1) {
				 
//			 ///////////// CREATION DES UTILISATEURS DE CLUSTER ///////////////////////////////////////////////
			  UsersTool.createGroup("niki", uCollection, cluster, writer);
//			 ///////////////////////////////////////////////////////////////////////////////////////////////
//				
			 if (stepCount)
			  System.exit(0);
			 }
			 
			 
			 // UsersTool.createGroup("niki", "user_Kmean_k_20", loadCluster);
			 
			 User.loadUsersFrom("niki", uCollection);
			 
			 String [] c = {"cascades_1", "cascades_2"};
//			 System.out.println (UsersTool.group);
			 for (int i = 0; i < 2; i++) {
				 
			 ///////////////// Creation cascade /////////////////////////
				 ClusterTool.toCascade("niki", c[i], c[i]+"l" +uCollection + ((lambda==0)?"":"_"+lambda)  , cluster, lambda);
			 }
			 
			 
			
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}	}

}
