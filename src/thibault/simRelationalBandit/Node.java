package thibault.simRelationalBandit;
import java.util.HashMap;

import org.apache.commons.math3.distribution.NormalDistribution;

public class Node {

	public int Id;
	public double bias;
	public double var;
	public double r,r0;
	public HashMap<Node, Double> Pred;
	public HashMap<Node, Double> Succ;
	public NormalDistribution dist;
	
	public Node(int Id){
		this.Id=Id;
		this.r=0.0;
		this.Pred=new HashMap<Node, Double>();
		this.Succ=new HashMap<Node, Double>();
		this.bias=Math.random();
		this.var=Math.random();
		this.dist=new NormalDistribution(bias,var);
	}
}
