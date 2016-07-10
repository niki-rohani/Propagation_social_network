package niki.tool;







import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import niki.clustering.JacquartSim;
import similarities.StrSim;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import core.Link;
import core.Post;
import core.Text;
import core.User;
import cascades.ArtificialCascade;
import cascades.Cascade;
import clustering.*;
import actionsBD.*;
import niki.clustering.node.*;
public class ClusterTool {

	public static String clusterPrefix = "cluster_";
	public static String clusterDb = "niki";
	/**
	 * Sauvegarde un cluster dans la collection clusterName
	 * @param cluster
	 * @param clusterName
	 */
	public static void saveCluster (Clustering cluster, String clusterName) {
		MongoDB.mongoDB.dropCollection("niki", clusterPrefix + clusterName);
		MongoDB.mongoDB.createCollection("niki", clusterPrefix + clusterName, "");
		int i = 0;
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB("niki",clusterPrefix + clusterName);
		
		for (ArrayList <Integer> clusters : cluster.getClusters()) {
			i++;
			BasicDBObject obj = new BasicDBObject();
			obj.put ("id", i);
			obj.put("user", clusters);
			obj.put("group", System.currentTimeMillis()+""+i);
			WriteResult wr = col.insert(obj);
			if (wr.getError()!=null){
				throw new RuntimeException(wr.getError());
			}
			
		}
		
		
		
	}
	
	public static void saveClusterFile(String sim, int k) throws IOException {
		String db = "niki";
		String user = "user_Kmean_" + sim + "_k_" + k;
		PrintWriter writer = new PrintWriter("cluster/Kmean_"+sim+ "_k_" + k + ".csv");
		writer.print("");
		writer.close();
		writer = new PrintWriter(new BufferedWriter(new FileWriter("cluster/Kmean_"+sim+ "_k_" + k + ".csv", true)));
		
		clustering.Clustering cluster = ClusterTool.loadCluster("Kmean_"+ sim + "_k_" + k);
		System.out.println("Loading Users for cluster " + user + " Cluster : " + cluster);
		User.loadUsersFrom(db, user);
		User.loadAllLinksFrom(db, user);
		writer.println ("cluster,name,users,link");
		for (int i = 0; i < cluster.getNbClusters(); i += 1) {
			System.out.println(User.getUser(UsersTool.group.get(i+1)).getSuccesseurs().size());
			ArrayList <Integer> c = cluster.getCluster(i);
			writer.print(i + "," + UsersTool.group.get(i+1) + "," + c.size() + ",");
			writer.print("[");
			for (Link l: User.getUser(UsersTool.group.get(i+1)).getSuccesseurs().values())
				writer.print((UsersTool.groupT.get(l.getNode2().getName())-1)+":"+l.getVal()+";");
			writer.println("]");
			writer.flush();
		}
		
	}
	
	public static void saveCascadeFile(String sim, int k, double lambda) throws IOException {
		String db = "niki";
		String user = "user_Kmean_" + sim + "_k_" + k;
		PrintWriter writer = new PrintWriter("cascade/Kmean_"+sim+ "_k_" + k + ((lambda==0)?"":"_"+lambda) + ".csv");
		writer.print("");
		writer.close();
		writer = new PrintWriter(new BufferedWriter(new FileWriter("cascade/Kmean_"+sim+ "_k_" + k + ((lambda==0)?"":"_"+lambda) + ".csv", true)));
		clustering.Clustering cluster = ClusterTool.loadCluster("Kmean_"+ sim + "_k_" + k);
		System.out.println("Loading Users for cluster " + user + " Cluster : " + cluster);
		User.loadUsersFrom(db, user);
		User.loadAllLinksFrom(db, user);
		writer.println ("cluster,name,nb");
		
		String [] c = {"cascades_1", "cascades_2"};
		HashMap <String, ArrayList <Integer>> clusters = new HashMap <String, ArrayList <Integer>>();
		for (int cascade = 0; cascade < 2; cascade++) {
			String casc = c[cascade]+"l" +"user_Kmean_"+sim+"_k_" + k + ((lambda==0)?"":"_"+lambda);
			HashSet <Cascade> cascades = Cascade.getCascadesFromDB ("niki",casc);
			for (Cascade posts: cascades) {
				if (posts.getPosts().size() > 1) {
				for (Post post: posts.getPosts()) {
					String o = post.getOwner().getName();
					if (!clusters.containsKey(o))
						clusters.put(o, new ArrayList <Integer>());
					clusters.get(o).add(posts.getID());
				}
			}
			}
	    }
		for (int i = 0; i < cluster.getNbClusters(); i++) {
			String index = UsersTool.group.get(i+1);
			
			writer.println(i+","+index+","+((clusters.containsKey(index))?clusters.get(index).size():"0"));
		}
		
		writer.flush();
		writer.close();
		
	
		
	}
	
	
	public static void saveUserLinkFile(String sim, int k, double lambda) throws IOException {
		String db = "niki";
		String user = "user_Kmean_" + sim + "_k_" + k;
		PrintWriter writer = new PrintWriter("user/Kmean_"+sim+ "_k_" + k );
		writer.print("");
		writer.close();
		writer = new PrintWriter(new BufferedWriter(new FileWriter("user/Kmean_"+sim+ "_k_" + k , true)));
		clustering.Clustering cluster = ClusterTool.loadCluster("Kmean_"+ sim + "_k_" + k);
		System.out.println("Loading Users for cluster " + user + " Cluster : " + cluster);
		User.loadUsersFrom(db, user);
		User.loadAllLinksFrom(db, user);
		
		for (User users: User.users.values()) {
			for (String suc: users.getSuccesseurs().keySet())
				writer.println(users.getName() + " " + suc + " " + users.getSuccesseurs().get(suc).getVal());
		}
		
		writer.flush();
		writer.close();
		
	
		
	}
	

	public static void saveClusterCascadeFile(String sim, int k, double lambda) throws IOException {
		String db = "niki";
		String user = "user_Kmean_" + sim + "_k_" + k;
		BufferedReader f = new BufferedReader(new FileReader("userCascade"));
		String l = f.readLine();
		l = f.readLine();
		HashMap <Integer, Integer> count = new HashMap <Integer, Integer> ();
		while (l != null) {
			String[] s = l.split(" ");
			count.put(Integer.parseInt(s[0]), (int) Double.parseDouble(s[1]));
			l = f.readLine();
		}
		PrintWriter writer = new PrintWriter("cascadeinformation/Kmean_"+sim+ "_k_" + k + ".csv");
		writer.print("");
		writer.close();
		writer = new PrintWriter(new BufferedWriter(new FileWriter("cascadeinformation/Kmean_"+sim+ "_k_" + k + ".csv", true)));
		clustering.Clustering cluster = ClusterTool.loadCluster("Kmean_"+ sim + "_k_" + k);
		System.out.println("Loading Users for cluster " + user + " Cluster : " + cluster);
		User.loadUsersFrom(db, user);
		writer.println ("cluster mean std max users");
		for (int clusters = 0; clusters < cluster.getNbClusters(); clusters++) {
			int nb = 0;
			int max = 0;
			for (int u: cluster.getCluster(clusters)) {
				nb = nb + count.get(u);
				if (max < count.get(u))
					max = count.get(u);
			}
			double mean = nb/(cluster.getCluster(clusters).size()+0.0);
			double std = 0;
			for (int u: cluster.getCluster(clusters))
				std = std + (count.get(u)-mean)*(count.get(u)-mean);
			std = std / (cluster.getCluster(clusters).size() + 0.0);
			writer.println(clusters + " " + mean + " " + std + " " + max + " " + cluster.getCluster(clusters).size());
		}
		
		writer.flush();
		writer.close();
		
	
		
	}
	
	public static Clustering loadCluster (String clusterN) {
		
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB("niki",clusterPrefix + clusterN);
		DBCursor cur = col.find();
		HashMap <Integer, ArrayList<Integer>> cluster = new HashMap <Integer, ArrayList <Integer>> ();
		while (cur.hasNext()) {
			
			DBObject obj = cur.next();
			if (!cluster.containsKey(obj.get("id"))) {
				cluster.put(Integer.parseInt (obj.get("id").toString()), new ArrayList <Integer>());
			}
			
			ArrayList <Integer> user = (ArrayList<Integer>)obj.get("user");
			for (Integer ob:user) {
				cluster.get(Integer.parseInt (obj.get("id").toString())).add(ob);
			}
			UsersTool.group.put(Integer.parseInt (obj.get("id")+""), obj.get("group")+"");
			UsersTool.groupT.put(obj.get("group")+"", Integer.parseInt (obj.get("id")+""));
		}
		
		
		Clustering load = new Clustering (cluster.size(), null);
		for (int loadclust : cluster.keySet()) {
			for (int user = 0; user < cluster.get(loadclust).size(); user++)
				load.addToCluster(cluster.get(loadclust).get(user), loadclust-1);
		}
		
		
		return load;
	}
	
	public static void saveNewUsers (String cluster) throws Exception {
		System.out.println ("Saving Users with cluster " + cluster);
		Clustering clusters = loadCluster (cluster);
		for (int clusterI=0;clusterI<clusters.getClusters().size();clusterI++) {
			for (Integer u:clusters.getCluster(clusterI)) {
				if (!User.users.containsKey(u)) {
					throw new Exception ("User " + u + " non charge");
				}
			}
		}
	
	}
	
	
	/**
	 * Affiche les statistiques entre deux Text
	 * @param u1 : Text
	 * @param u2 : Text
	 * @param similarityClass : "Jacquart"
	 */
	public static void toStatistic (Text u1, Text u2, String similarityClass) {
		StrSim sim = null;
		if (similarityClass == "Jacquart")
			sim = new JacquartSim (null);
		double simi = sim.computeSim(u1, u2);
		Set <Integer> common = JacquartSim.getCommonCascade(u1, u2);
		System.out.println ("Similariti between " + u1.getName() + " " + u2.getName() + " : " + simi);
		System.out.println ("Cascade of u1 union u2 " + common  );
		System.out.println ("Nb of common : " + common.size() );
		System.out.println ("Cascade u1 " + ((UserByCascadeText)u1).getCascade() );
		System.out.println ("Cascade u2 " + ((UserByCascadeText)u2).getCascade());
	}
	
	
	/**
	 * Transforme une cascade
	 * collectionFrom => collectionTo
	 * Prend un cluster pour transformer les cascades
	 * lambda = taux d'infection dans le groupe pour infecter le groupe entier.
	 * @throws Exception 
	 * 
	 */
	public static void toCascade (String db, String collectionFrom, String collectionTo, Clustering cluster, double lambda) throws Exception {
		HashSet <Cascade> cascades = Cascade.getCascadesFromDB ("digg",collectionFrom);
		System.out.println ("Transform cascade : " + collectionFrom + " => " + collectionTo);
		MongoDB.mongoDB.dropCollection(db, collectionTo);
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collectionTo);
		if (col == null)
			throw new Exception ("ClusterTool, col null");
//		System.out.println ("Cascades load " + cascades);
		for (Cascade c: cascades) {
//			System.out.println ("Saving " + c);
			indexArtificialCascade (col, cluster, c, lambda);
		}
	}
	
	public static void toCascadeSucre (String db, String collectionFrom, String collectionTo, Clustering cluster, double lambda) throws Exception {
		HashSet <Cascade> cascades = Cascade.getCascadesFromDB ("digg",collectionFrom);
		System.out.println ("Transform cascade : " + collectionFrom + " => " + collectionTo + "sucre");
		MongoDB.mongoDB.dropCollection(db, collectionTo + "sucre");
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collectionTo );
		if (col == null)
			throw new Exception ("ClusterTool, col null");
//		System.out.println ("Cascades load " + cascades);
		for (Cascade c: cascades) {
//			System.out.println ("Saving " + c);
			indexArtificialCascadeSucre (col, cluster, c, lambda);
		}
	}
	
	
	/**
	 * Enregistre la cascade
	 * @param col         
	 * @param cluster 
	 * @param cascade
	 * @param lambda       taux d'infection d'un groupe.
	 */
	public static void indexArtificialCascade (DBCollection col, Clustering cluster, Cascade cascade, double lambda) {
			
			BasicDBObject obj = new BasicDBObject();
			HashMap <String, Double> isAlreadyOnCascade = new HashMap <String, Double> ();
			HashMap <String, Integer> nbCascade = new HashMap <String, Integer> ();
			obj.put("id", cascade.getID());
			obj.put("fromCol", "artificial");
			obj.put("name", cascade.getID());
			
			
			ArrayList<BasicDBObject> postsListe=new ArrayList<BasicDBObject>();
			
			for(Post p:cascade.getPosts()){
				double time=p.getTimeStamp();
				User user=p.getOwner();
				String i = UsersTool.group.get (cluster.getIClusterOf (Integer.parseInt (user.getName()))+1);
				if (!isAlreadyOnCascade.containsKey(i) || isAlreadyOnCascade.get(i) > time) {
//				System.out.println(i);
				isAlreadyOnCascade.put(i, time);
				}
				if (!nbCascade.containsKey(i))
					nbCascade.put(i,0);
				
				
				nbCascade.put(i, nbCascade.get(i)+1);
				
				
			}
			
			for (String u : nbCascade.keySet()) {
				BasicDBObject ti=new BasicDBObject();
				if (lambda == 0 || nbCascade.get(u) > 
							cluster.getCluster
									(UsersTool.groupT.get(u) - 1).size()*lambda) {
					ti.put(u,isAlreadyOnCascade.get(u));
					postsListe.add(ti);
				}
			}
//			System.out.println (postsListe);
			obj.put("contamination",postsListe);
			ArrayList<BasicDBObject> wListe=new ArrayList<BasicDBObject>();
//			String wListe = "";
			/*
			for(Integer i:cascade.getWeigths().keySet()){
				BasicDBObject wi=new BasicDBObject();
				wi.put(i+"", weights.get(i));
				wListe.add(wi);
			}
			*/
			obj.put("weights",wListe);
//			System.out.println (obj);
			if (obj == null)
				return;
			col.insert(obj);
			//System.out.println("Cascade artificielle inseree : "+obj);
		
	}
	
	public static void indexArtificialCascadeSucre (DBCollection col, Clustering cluster, Cascade cascade, double lambda) {
		
		BasicDBObject obj = new BasicDBObject();
		HashMap <String, Double> isAlreadyOnCascade = new HashMap <String, Double> ();
		HashMap <String, Integer> nbCascade = new HashMap <String, Integer> ();
		obj.put("id", cascade.getID());
		obj.put("fromCol", "artificial");
		obj.put("name", cascade.getID());
		
		
		ArrayList<BasicDBObject> postsListe=new ArrayList<BasicDBObject>();
		
		for(Post p:cascade.getPosts()){
			double time=p.getTimeStamp();
			User user=p.getOwner();
			int c = cluster.getIClusterOf (Integer.parseInt (user.getName()));
			if (c==-1) {}
			else {
			if (cluster.getCluster(c).size() > 1000) {}
			else {
			String i = UsersTool.group.get (c+1);
			
			if (!isAlreadyOnCascade.containsKey(i) || isAlreadyOnCascade.get(i) > time) {
//			System.out.println(i);
			isAlreadyOnCascade.put(i, time);
			}
			if (!nbCascade.containsKey(i))
				nbCascade.put(i,0);
			
			
			nbCascade.put(i, nbCascade.get(i)+1);
			
				}
			}
		}
		
		for (String u : nbCascade.keySet()) {
			BasicDBObject ti=new BasicDBObject();
			if (lambda == 0 || nbCascade.get(u) > 
						cluster.getCluster
								(UsersTool.groupT.get(u) - 1).size()*lambda) {
				ti.put(u,isAlreadyOnCascade.get(u));
				postsListe.add(ti);
			}
		}
//		System.out.println (postsListe);
		obj.put("contamination",postsListe);
		ArrayList<BasicDBObject> wListe=new ArrayList<BasicDBObject>();
//		String wListe = "";
		/*
		for(Integer i:cascade.getWeigths().keySet()){
			BasicDBObject wi=new BasicDBObject();
			wi.put(i+"", weights.get(i));
			wListe.add(wi);
		}
		*/
		obj.put("weights",wListe);
//		System.out.println (obj);
		
		col.insert(obj);
		//System.out.println("Cascade artificielle inseree : "+obj);
	
}
}
