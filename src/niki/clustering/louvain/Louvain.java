package niki.clustering.louvain;

import java.util.ArrayList;
import java.util.HashMap;

import core.Link;
import core.User;

public class Louvain {

	private ArrayList <Group> group;
	private double m;
	private int step;
	public Louvain () {
		group = new ArrayList <Group> ();
		step = 0;
		m = 0.0;
	}
	
	
	public void pass() {
		int change = 1;
		int loop = 0;
		double q;
		
		// Premiere phase
		while (change > 0) {
			change = 0;
			for (int it = 0; it < group.size(); it++) {
				for (User u: group.get(it).getUser().values()){
					q = 0.0;
					int maxg = 0;
					for (int to = 0; to < group.size(); to++) {
						if (to == it) {
						
						}
						else {
							changeTo (u, it, to);
							double qt = Q(u.getName(), to);
							if (qt > q) {
								q = qt;
								maxg = to;
							}
							changeTo (u, to, it);
						}
						
					}
					if (q > 0) {
						changeTo (u, it, maxg);
						change++;
					}
				}
			}
			System.out.println("Iteration " + loop + " change " + change);
			loop++;
		}
		
		// On change les groupes en utilisateurs
		for (int i = 0; i < group.size(); i++) {
			group.get(i).transformToUser();
		}
	}
	
	public void initialize() {
		System.out.println("Creating group");
		for (User u: User.users.values()) {
			group.add(new Group(u));
		}
		for (User u: User.users.values()) 
			for (Link l: u.getSuccesseurs().values())
				m+=l.getVal();
		
		
			
	}
	
	public double Q(String i, int C) {
		Group c = group.get(C);
		User k = User.users.get(i);
		double Ein = c.getWeightIntra();
		double kiin = c.getSuccesseursIntraI(i);
		double ki = c.getIncidentIntraI(i);
		
		double Etot = c.getIncidentIntra();
		return (((Ein+kiin)/2*m) - (Math.pow((Etot + ki)/2*m, 2) )) - ((Ein/(2*m)) - Math.pow(Etot/(2*m), 2) - Math.pow (ki/(2*m), 2) );
	}
	
	public void changeTo (User i,int from, int to) {
		group.get(from).remove(i);
		group.get(to).add(i);
	}
	
	
	
}
