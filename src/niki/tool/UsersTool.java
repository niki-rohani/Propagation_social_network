package niki.tool;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import niki.clustering.node.UserByCascadeText;
import actionsBD.MongoDB;
import cascades.Cascade;
import clustering.Clustering;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.*;

public class UsersTool {

	
	public static HashMap <Integer, Text> users = new HashMap <Integer, Text> ();
	public static HashMap <Integer, Cascade> cascades = new HashMap <Integer, Cascade> ();
	public static HashMap <String, Boolean> isLoad = new HashMap <String, Boolean> ();
	public static HashMap <Integer, String> group = new HashMap <Integer, String> ();
	public static HashMap <String, User> user;
	public static HashMap <Integer, Cascade> casc; 
	public static HashMap <Integer, Text> usersOrig;
	public static int maxStep = 3;
	public static HashMap <String, Integer> groupT = new HashMap <String, Integer> ();
	/**
	 * Charge une serie d'user et de cascade, et cree une liste UsersTool.users
	 * @param db : DbUser
	 * @param dbcascade : Dbcascade
	 * @param collection : User
	 * @param cascade : Cascade
	 * @throws Exception 
	 */
	
	public static void reset() {
		System.out.println("Reseting ....");
		users = (HashMap<Integer, Text>) usersOrig.clone();
		cascades = (HashMap<Integer, Cascade>) casc.clone();
		isLoad = new HashMap <String, Boolean> ();
		group = new HashMap <Integer, String> ();
		User.users = (HashMap<String, User>) user.clone();
		groupT = new HashMap <String, Integer> ();
	}
	
	public static void realReset() {
		users = new HashMap <Integer, Text> ();
		cascades = new HashMap <Integer, Cascade> ();
		isLoad = new HashMap <String, Boolean> ();
		group = new HashMap <Integer, String> ();
		User.users = new HashMap<String,User>(); 
		groupT = new HashMap <String, Integer> ();
	}
	public static HashMap <Integer, Text> load(String db,String dbcascade, String collection, String cascade, boolean link, boolean successor, boolean first) throws Exception {
		HashMap <Integer, Text> r = new HashMap <Integer, Text> ();
		if (!isLoad.containsKey(dbcascade+collection)) {
			User.loadUsersFrom(db, collection);
			if (link)
			if (!User.loadAllLinksFrom(db, collection))
				throw new Exception ("Impossible de charger les user");
		}
		isLoad.put(dbcascade+collection, true);
		HashMap<Integer, Set <User>> c = loadCascade (dbcascade, cascade);
		System.out.println("Cascade chargees");
		for (String u : User.users.keySet()) {
			if (!users.containsKey(Integer.parseInt(u)))
				if (successor)
			users.put(Integer.parseInt (u), new UserByCascadeText (u, User.users.get(u).getSuccesseurs().keySet(), User.users.get(u).getPredecesseurs().keySet()));
				else
			users.put(Integer.parseInt (u), new UserByCascadeText (u));
		 r.put (Integer.parseInt(u), users.get(Integer.parseInt(u)));
		} for (Integer cursor : c.keySet()) {
			for (User u : c.get(cursor)) {
				((UserByCascadeText)users.get(Integer.parseInt(u.getName()))).addCascade(cascades.get(cursor));
			} 
			
			
		} for (Text u : users.values()) {
			((UserByCascadeText)u).computeTimeStep();
		}
		if (first)
		usersOrig = (HashMap<Integer, Text>) users.clone();
		if (first)
		casc = (HashMap<Integer, Cascade>) cascades.clone();
		return r;
	}
	public static ArrayList <Integer> getCascadeID (int u) {
		ArrayList <Integer> list = new ArrayList <Integer> ();
		return list;
	}
	
	
	/**
	 * Charge une serie de cascade avec leur user
	 * @param db
	 * @param collection
	 * @return HashMap <Integer, Set <User>> Cascade avec leur user
	 */
	public static HashMap <Integer, Set <User>> loadCascade (String db, String collection) {
		 System.out.println("Chargement Cascade .... ");
		 DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		 DBCursor cursor = col.find();
		 HashMap <Integer, Set <User>> c = new HashMap <Integer, Set <User>> ();
		 int i = 0;
		 while (cursor.hasNext()) {
			 i++;
			 DBObject res=cursor.next();
			 Cascade cRes=Cascade.getCascadeFrom(res);
			 c.put(cRes.getID(), cRes.getUserContaminationSteps().keySet());
			 cascades.put(cRes.getID(), cRes);
		 }
     	 return c;
     	 
	}
	
	
	public static void saveUserParticipation () throws IOException {
		 String[] cascade = {"cascades_1", "cascades_2"};
		 HashMap <Integer, Double> count = new HashMap <Integer, Double> ();
		 User.loadUsersFrom("digg", "users_1");
		 for (String users: User.users.keySet()) 
			 count.put(Integer.parseInt(users), 0.0);
		 System.out.println(count.size());
		 int i = 0;
		 for (String c: cascade) {
		 DBCollection col=MongoDB.mongoDB.getCollectionFromDB("digg",c);
		 DBCursor cursor = col.find();
		 while (cursor.hasNext()) {
			 if (i%100 == 0)
				 System.out.println (i + " cascade");
			 i++;
			 DBObject res=cursor.next();
			 Cascade cRes=Cascade.getCascadeFrom(res);
			 Set <User> u = cRes.getUserContaminationSteps().keySet();
			 for (User user: u) {
				
				 if (!count.containsKey(user.getID()))
					 count.put(user.getID(), 0.0);
				 count.put (user.getID(), count.get(user.getID())+1);
				 
			 }
			 
		 }
    	 }
		 System.out.println(i + " Cascades");
		 PrintWriter writerTrain = new PrintWriter(new BufferedWriter(new FileWriter("userCascade", true)));
		 for (Integer user: count.keySet()) {
			 writerTrain.println(user.intValue()+ " " + count.get(user.intValue()));
		 }
		 writerTrain.flush();
		 writerTrain.close();
		 
	}
	
	

	
	
	
	/**
	 * Cree un User par groupe, regroupe chaque user d'un groupe en un User, convertie les successeurs et predecesseur, enregistre le resultat.
	 * @param db
	 * @param collection Collection d'enregistrement
	 * @param cluster le cluster contenant la definition des groupe
	 * @throws Exception 
	 */
	
	
	public static void createGroup (String db, String collection, Clustering cluster, PrintWriter writer) throws Exception {
		ArrayList <User> group = new ArrayList <User> ();
		for (int i = 0; i < cluster.getClusters().size(); i++) {
			group.add(new User(UsersTool.group.get(i+1)+""));
		}
		// writer.println("cluster,name,users,link");
		
		// Recuperation des liens
		for (int i = 0; i < cluster.getClusters().size(); i++) {
			// writer.print(i+","+group.get(i).getName()+","+cluster.getCluster(i).size()+",");
			int link = 0;
			HashMap <Integer, Double> predOfCluster = new HashMap <Integer, Double> ();
			HashMap <Integer, Double> suOfluster = new HashMap <Integer, Double> ();
			
			// Chaque user dans un groupe
			for (Integer u: cluster.getCluster(i)) {
				
				
				if (!User.users.containsKey(u+"")) {
					throw new Exception ("User " + u + " non charge");
				}
				// System.out.println("Getting informations about " + u + " in cluster " + i);
				User user = User.users.get(u+"");
				
				HashMap <String, Link> pred =  user.getPredecesseurs();
				HashMap <String, Link> succ = user.getSuccesseurs();
				
				
				// Est-ce que pred et succ sont toujours coherent ?
				for (String l:pred.keySet()) {
				
//					if (link%50==0 && link != 0)
//						System.out.println (link + "Link created for " + i);
					Integer predI = cluster.getIClusterOf(Integer.parseInt(l));
				
					// Si un lien existe
					if (predI > 0 && predI != i) {
					link++;
					
					if (!predOfCluster.containsKey(predI))
						predOfCluster.put(predI, 0.0);
					// Ajout de ce liens et incrment de la valeur
					predOfCluster.put(predI, predOfCluster.get(predI)+pred.get(l).getVal());
					}
					
				}
				// Si non coherent
				for (String l:succ.keySet()) {
					
//					if (link%50==0 && link != 0)
//						System.out.println (link + "Link created for " + i);
					Integer sI = cluster.getIClusterOf(Integer.parseInt(l));
					
					if (sI > 0 && sI != i) {
					link++;
					if (!suOfluster.containsKey(sI))
						suOfluster.put(sI, 0.0);
					suOfluster.put(sI, suOfluster.get(sI)+succ.get(l).getVal());
					}
					
				}
				
			}
			for (Integer l:predOfCluster.keySet()) {
				group.get(i).addLink(new Link ((Node)group.get(l),(Node)group.get(i), predOfCluster.get(l)));
			}
			for (Integer l:suOfluster.keySet()) {
				group.get(i).addLink(new Link ((Node)group.get(i),(Node)group.get(l), suOfluster.get(l)));
			}
		}
		MongoDB.mongoDB.dropCollection(db, collection);
		
		for (User u: group) {
			u.indexInto(db, collection);
		}
		
		
	}
	
	
}
