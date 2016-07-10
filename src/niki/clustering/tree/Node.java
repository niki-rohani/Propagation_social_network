package niki.clustering.tree;

import java.util.HashSet;

import core.Link;
import core.User;

public class Node extends core.Node {

	
	
	
	
	
	public static int nbArtificialNode = 0;
	private double h;
	private HashSet <User> users;
	
	public Node(String name) {
		super(name);
		// TODO Auto-generated constructor stub
		h = 0.0;
		users = new HashSet <User> ();
	}
	
	public double getH() {
		return h;
	}
	
	public void setH(double hset){
		h = hset;
	}
	
	public void addUser(User u) {
		users.add(u);
	}
	
	public void addUsers (HashSet <User> u) {
		users.addAll(u);
	}
	
	public HashSet<User> getUsers() {
		return users;
	}
	
	
	public boolean isRoot() {
		return this.getPredecesseurs().size() == 0;
	}
	
	
	public static Node fusion(Node c1, Node c2, double h) {
		Node c = new Node ("c" + nbArtificialNode);
		c.setH(h);
		c.addUsers(c1.getUsers());
		c.addUsers(c2.getUsers());
		c.addLink(new Link (c, c1, 1.0));
		c.addLink(new Link (c, c2, 1.0));
		c1.addLink(new Link (c, c1, 1.0));
		c2.addLink(new Link (c, c2, 1.0));
		return c;
	}

}
