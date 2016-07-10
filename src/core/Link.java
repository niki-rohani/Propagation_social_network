package core;

import java.util.HashMap;


public class Link extends Text{ 
	public static final long serialVersionUID=1;
	private Node node1;
	private Node node2;
	private double val;
	//private int hashlink;
	//private V val;
	//private String name;
	public Link(String type, HashMap<Integer,Double> h, Node un,Node deux, double val){
		super(type,h);
		this.node1=un;
		this.node2=deux;
		hash=(type+"_"+node1.getName()+","+node2.getName()).hashCode(); //+new HashCodeBuilder(13, 17).append(node1.getName()+","+node2.getName()).toHashCode();
		
		this.val=val;
	}
	public Link(String type, Node un,Node deux,double val){ //, V val){
		this(type,new HashMap<Integer,Double>(),un,deux,val);
	}
	public Link(Node un,Node deux,double val){ //, V val){
		this("",new HashMap<Integer,Double>(),un,deux,val);
	}
	public Link(String type, Node un,Node deux){ //, V val){
		this(type,new HashMap<Integer,Double>(),un,deux,1.0);
	}
	public Node getNode1(){
		return node1;
	}
	public Node getNode2(){
		return node2;
	}
	public double getVal(){
		return(val);
	}
	public String getType(){
		return(titre);
	}
	public void setVal(double v){
		val=v;
	}
	public String toString(){
		return(node1+";"+node2+"="+val);
	}
	
	/*public int hashCode() {
        return new HashCodeBuilder(13, 17).append(name).toHashCode();
    }*/
	/*public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;

        Link rhs = (Link) obj;
        return new EqualsBuilder().append(hash, rhs.hash).isEquals();
    }*/
	
	public int hashCode(){
		return(this.hash);
	}
	
	public boolean equals(Object o){
		if(o.getClass()!=this.getClass()){
			return false;
		}
		Link n=(Link)o;
		if(!name.equals(n.name)){
			return false;
		}
		if(!node1.equals(n.node1)){
			return false;
		}
		if(!node2.equals(n.node2)){
			return false;
		}
		return true;
	}
	
	/*public boolean equals(Link l) {
		return this.getNode1() == l.getNode1() && this.getNode2() == l.getNode2() ;
	}*/
}
