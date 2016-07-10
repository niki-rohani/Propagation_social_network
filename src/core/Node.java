package core;

import java.util.HashMap;
import java.util.HashSet;


public class Node {
	protected HashSet<Link> relations=null;
	protected HashMap<String,Link> successeurs=null;
	protected HashMap<String,Link> predecesseurs=null;
	protected String name;
	protected int hash;
	public Node(String name){
		this.name=name;
		hash=name.hashCode(); //new HashCodeBuilder(17, 31).append(name).toHashCode();
	}
	
	public String getName(){
		
		return(name);
	}
	
	public boolean equals(Object o){
		if(o.getClass()!=this.getClass()){
			return false;
		}
		Node n=(Node)o;
		if(!name.equals(n.name)){
			return false;
		}
		return true;
	}
	public void addLink(Link l){
		addLink(l,false);
	}
	public void addLink(Link l, boolean both){
		Node autre=null;
		boolean autreOk=false;
		if(relations==null){
			relations=new HashSet<Link>();
			successeurs=new HashMap<String,Link>();
			predecesseurs=new HashMap<String,Link>();
		}
		if (l.getNode1()==this){
			autre=l.getNode2();
			
			Link l2=l.getNode2().getPredecesseurs().get(name);
			if (l2!=null){
				l=l2;
				autreOk=true;
			}
			
			successeurs.put(l.getNode2().getName(),l);
		}
		else{
			Link l2=l.getNode1().getSuccesseurs().get(name);
			if (l2!=null){
				l=l2;
				autreOk=true;
			}
			autre=l.getNode1();
			predecesseurs.put(l.getNode1().getName(),l);
		}
		relations.add(l);
		if ((both) && (!autreOk)){
			autre.addLink(l);
		}
	}
	
	public void reinitLinks(){
		reinitLinks(false);
	}
	
	public void reinitLinks(boolean fromBoth){
		if(relations==null){
			return;
		}
		relations=null; //new HashSet<Link>();
		if(fromBoth){
			for(Link l:successeurs.values()){
				Node n=l.getNode2();
				n.removePredecesseur(this);
			}
			for(Link l:predecesseurs.values()){
				Node n=l.getNode1();
				n.removeSuccesseur(this);
			}
		}
		successeurs=null; //new HashMap<String,Link>();
		predecesseurs=null; //new HashMap<String,Link>();
	}
	
	
	public void removePredecesseur(Node n){
		removePredecesseur(n,false);
		
	}
	public void removeSuccesseur(Node n){
		removeSuccesseur(n,false);
	}
	
	public void removePredecesseur(Node n,boolean fromBoth){
		if(relations==null){
			return;
		}
		Link l=predecesseurs.get(n.getName());
		relations.remove(l);
		predecesseurs.remove(n.getName());
		if(fromBoth){
			l.getNode1().removeSuccesseur(this,false);
		}
	}
	public void removeSuccesseur(Node n, boolean fromBoth){
		if(relations==null){
			return;
		}
		Link l=successeurs.get(n.getName());
		relations.remove(l);
		successeurs.remove(n.getName());
		if(fromBoth){
			l.getNode2().removePredecesseur(n, false);
		}
	}
	
	public HashMap<String,Link> getSuccesseurs() {
		if(relations==null){
			relations=new HashSet<Link>();
			successeurs=new HashMap<String,Link>();
			predecesseurs=new HashMap<String,Link>();
		}
		return successeurs ;
	}
	public HashMap<String,Link> getPredecesseurs() {
		if(relations==null){
			relations=new HashSet<Link>();
			successeurs=new HashMap<String,Link>();
			predecesseurs=new HashMap<String,Link>();
		}
		return predecesseurs ;
	}
	public HashSet<Link> getRelations() {
		if(relations==null){
			relations=new HashSet<Link>();
			successeurs=new HashMap<String,Link>();
			predecesseurs=new HashMap<String,Link>();
		}
		return relations ;
	}
	/*public Set<Node> getNeighbours() {
		HashSet<Node> r = new HashSet<Node>() ;
		for(Link<User> l : successeurs.values()) {
			r.add(l.getNode2()) ;
		}
		return r ;
	}*/
	public String toString(){
		return(name);
	}
	public int hashCode(){
		return(hash); //name.hashCode());
	}
	/*public int hashCode() {
        return new HashCodeBuilder(17, 31).append(name).toHashCode();
    }*/
	/*public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;

        Node rhs = (Node) obj;
        return new EqualsBuilder().append(name, rhs.name).isEquals();
    }*/
	
}
