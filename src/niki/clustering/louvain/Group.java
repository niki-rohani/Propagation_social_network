package niki.clustering.louvain;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import core.Link;
import core.Node;
import core.User;

public class Group extends User {
	private static int step = 0;
	private HashMap <String, User> users;
	private String n;
	private static HashMap <String, User> finalUser = new HashMap <String, User> ();
	private static int size = 0;
	
	public Group (HashMap <String, User> user, String name) {
		super(name);
		users = user;
		size+=1;
	}
	public HashMap <String, User> getUser () {
		return users;
	}
	
	
	public Group (String name) {
		super(name);
		size+=1;
	}
	
	public Group (User u) {
		super (size+"_1");
		
		
		users = new HashMap <String, User> ();
		users.put(u.getName(), u);
		size+=1;
	}
	public int getSize() {
		return users.size();
	}
	
	public String toString() {
		return "Group " + name + " : " + step;
	}
	
	
	
	public void setFinalUser (HashMap <String, User> user) {
		finalUser = user;
	}
	
	public void add(User user) {
		users.put(user.getName(), user);
	}
	
	public Group getCopyAdd(User user) {
		Group copy = new Group((HashMap<String, User>) users.clone(), "c_"+name);
		copy.add(user);
		return copy;
	}
	
	
	public void remove(User user) {
		users.remove(user.getName());
	}
	
	public void remove(String u) {
		users.remove(u);
	}
	
	public double getWeight (User user) {
		double w = 0.0;
		for (User u: users.values()) {
			for (Link ul: u.getSuccesseurs().values())
				if (ul.getNode2().getName()==user.getName())
					w+=ul.getVal();
		}
		return w;
	}
	
	public double getWeight (Group user) {
		double w = 0.0;
		for (User u: users.values()) {
			for (Link ul: u.getSuccesseurs().values())
				if (user.contain(ul.getNode2().getName()))
					w+=ul.getVal();
		}
		return w;
	}
	
	/**
	 * Renvoi la sommes de poids des liens dans le groupe
	 * e.g. Ein
	 * @return
	 */
	public double getWeightIntra () {
		return getWeight(this);
	}

	/**
	 * Renvoi la sommes de poids des liens incidents aux noeuds dans le groupe
	 * e.g. Etot
	 * @return
	 */
	public double getIncidentIntra() {
		double w = 0.0;
		for (User u: users.values()) {
			for (Link ul: u.getSuccesseurs().values())
				w+=ul.getVal();
			for (Link ul: u.getPredecesseurs().values())
				w+=ul.getVal();
		}
		return w;
	}
	
	
	/**
	 * Renvoi la sommes de poids des liens incidents au noeud u
	 * e.g. ki
	 * @param u
	 * @return
	 */
	public double getIncidentIntraI(String u) {
		double w = 0.0;
		if (!users.containsKey(u))
			return -1;
		for (Link ul: users.get(u).getSuccesseurs().values())
			w+=ul.getVal();
		for (Link ul: users.get(u).getPredecesseurs().values())
			w+=ul.getVal();
		return w;
	}
	
	public double getSuccesseursIntraI(String u) {
		double w = 0.0;
		if (!users.containsKey(u))
			return -1;
		for (Link ul: users.get(u).getSuccesseurs().values())
			if (users.containsKey(ul.getNode2().getName()))
				w+=ul.getVal();
		return w;
	}
	
	
	
	
	public boolean contain(String u) {
		return users.containsKey(u);
	}
	
	public HashMap<String, Link> getSuccesseurs() {
		HashMap <String, Link> suc = new HashMap <String, Link> ();
		HashMap <Node, Double> link = new HashMap <Node, Double> ();
		for (User u: users.values())
			for (Link l: u.getSuccesseurs().values()){
				if (!link.containsKey(l.getNode2().getName()))
					link.put(l.getNode2(), 0.);
				link.put(l.getNode2(), link.get(l.getNode2())+l.getVal());
			}
		
		for (Node s: link.keySet()) {
			suc.put(s.getName(), new Link(this, s, link.get(s)));
		}
		
		return suc;
		
	}
	
	
	
	public void transformToUser() {
		step += 1;
		User u = new Group(name+step);
		for (Link link: getSuccesseurs().values())
			u.addLink(link);
		for (User us: users.values()) {
			if (us instanceof Group){
				finalUser.putAll(((Group) us).finalUser);
			}
			else {
				finalUser.put(us.getName(), us);
			}
		}
		users = new HashMap <String, User>();
		users.put(u.getName(), u);
	}
	
}
