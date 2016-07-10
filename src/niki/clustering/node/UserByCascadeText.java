package niki.clustering.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.la4j.vector.sparse.CompressedVector;
import org.la4j.vector.sparse.SparseVector;

import niki.tool.UsersTool;
import cascades.Cascade;
import core.Post;
import core.Text;
import core.User;


/**
 * Represente un User et ses cascades
 *
 */
public class UserByCascadeText extends Text{

	// Une cascade est defini par son id
	private HashMap <Integer, Cascade> cascade;
	private CompressedVector time;
	private HashMap <Integer, Integer> succ;
	private HashMap <Integer, Integer> pred;
	
	// Cree un text avec une information sur les cascade
	public UserByCascadeText (int id, String name, HashMap <Integer, Cascade> cascade, Set <String> successor, Set <String> predecessor) {
		super (id, name);
		this.cascade = cascade;
		succ = new HashMap <Integer, Integer> ();
		if (successor != null) {
			for (String l: successor) {
				addSucc(Integer.parseInt(l));
			}
			
		}
		pred = new HashMap <Integer, Integer> ();
		if (predecessor != null) {
			for (String l: predecessor) {
				addPred(Integer.parseInt(l));
			}
			
		}
	}
	
	
	public UserByCascadeText (int id, String name, HashMap <Integer, Cascade> cascade) {
		this (id, name, cascade, null,null);
	}
	
	
	public UserByCascadeText (String name, HashMap <Integer, Cascade> cascade, Set <String> successor, Set <String> predecessor) {
		this (Integer.parseInt(name), name, cascade, successor, predecessor);
	}
	
	public HashSet <Integer> getSuccList () {
		HashSet <Integer> h = new HashSet  <Integer> ();
		h.addAll(succ.values());
		return h;
	}
	
	public HashSet <Integer> getPredList () {
		HashSet <Integer> h = new HashSet  <Integer> ();
		h.addAll(pred.values());
		return h;
	}
	
	public void addSucc (int u) {
		succ.put(u, u);
	}
	public void addPred (int u) {
		pred.put(u, u);
	}
	public UserByCascadeText (String name) {
		this (name, new HashMap <Integer, Cascade> (), null, null);
	}
	public UserByCascadeText (String name, Set <String> successor, Set <String> predecessor) {
		this (name, new HashMap <Integer, Cascade> (), successor, predecessor);
	}
	
	public void addCascade (Cascade cascade) {
		this.cascade.put (cascade.getID(), cascade);
	}
	public HashMap <Integer, Cascade> getCascade () {
		return cascade;
	}
	
	public void setCascade (HashMap <Integer, Cascade> cascade) {
		this.cascade = cascade;
	}

	/**
	 * Renvoie un vecteur plus ou moins sparse en fonction du nombre de participation a un temps donne.
	 * Renvoie une hashMap Time, Nombre de post
	 * @return
	 */
	public HashMap <Long, Integer> getTimeStep() {
		HashMap <Long, Integer> getTime = new HashMap <Long, Integer> ();
		for (Cascade c: cascade.values()) {
			 long time = c.getUserContaminationSteps().get(User.users.get(id+""));
			 if (getTime.containsKey(time) == false)
				 getTime.put(time, 0);
			 getTime.put(time, getTime.get(time)+1);
		}
		return getTime;
	}
	
	
	/**
	 * Renvoie un vecteur plus ou moins sparse en fonction du nombre de participation a un temps donne. Les entrees sont normalisees.
	 * Renvoie une hashMap Time, Nombre de post
	 * @return
	 */
	public HashMap <Long, Double> getTimeStepNormalise() {
		HashMap <Long, Double> getTime = new HashMap <Long, Double> ();
		double size = cascade.size();
		for (Cascade c: cascade.values()) {
			 long time = c.getUserContaminationSteps().get(User.users.get(id+""));
			 if (getTime.containsKey(time) == false)
				 getTime.put(time, 0.0);
			 getTime.put(time, getTime.get(time)+(1./size));
		}
		return getTime;
	}
	
	
	public void computeTimeStep() {
		CompressedVector getTime = new CompressedVector  (UsersTool.maxStep+1);
		double size = cascade.size();
		for (Cascade c: cascade.values()) {
			long origin = Long.MAX_VALUE ;
			long maxTime = Integer.MIN_VALUE;
			 for (Post p: c.getPosts()) {
				 if (maxTime < p.getTimeStamp())
					 maxTime = p.getTimeStamp();
				 if (p.getTimeStamp()<origin)
						origin = p.getTimeStamp() ;
				 
			 }
		//	 System.out.print("MaxTime = " + maxTime + " ");
			 double discret = ((int)(maxTime-origin)/(double)UsersTool.maxStep) + 1;
		//	 System.out.println(" discretise = " + maxTime);
			 int time = (int)((c.getContaminationsSteps(-1, 1).get(id+"")-1) / discret);
			  
		//	 System.out.println ("Insert = " + (time-1));
			 getTime.set(time, getTime.get(time)+(1./size));
		}
		// System.out.println("DONE " + getTime);
		time = getTime;
	}
	/**
	 * Renvoie un vecteur plus ou moins sparse en fonction du nombre de participation a un temps donne. Les entrees sont normalisees.
	 * Renvoie une hashMap Time, Nombre de post
	 * @return
	 */
	public CompressedVector getSparseTimeStepNormalise() {
		return time;
	}
	
	public String toString () {
		return "[UserByCascade " + id + " " + name + " cascade : " + cascade + "] \n";
	}
	
	public String getName() {
		return this.titre;
	}
}
