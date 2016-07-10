package thibault.graphEmbeddings;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class Node {

	public int Id;
	public int sizeSpace;
	public double bias;
	public double var;
	public double r,r0;
	public HashMap<Node, Double> Vois;
	public NormalDistribution dist;
	public RealVector coords;
	public RealVector coords1;
	public RealVector coords2;
	public ArrayList<Double> values;
	
	public Node(int Id, int sizeSpace){
		this.Id=Id;
		this.sizeSpace=sizeSpace;
		this.r=0.0;
		this.Vois=new HashMap<Node, Double>();
		//this.Succ=new HashMap<Node, Double>();
		this.bias=Math.random();
		this.var=Math.random();
		this.dist=new NormalDistribution(bias,var);
		this.coords=new ArrayRealVector(new double[sizeSpace]);
		this.coords1=new ArrayRealVector(new double[sizeSpace]);
		this.coords2=new ArrayRealVector(new double[sizeSpace]);
		this.values=new ArrayList<Double>();
		this.initCoords2();
	}
	
	public void reinitResults(){
		this.values=new ArrayList<Double>();
	}
	
	public void initCoords2(){
		for(int j=0;j<sizeSpace;j++){
			coords1.setEntry(j, Math.random());
			coords2.setEntry(j, Math.random());
		}
		//System.out.println(coords2);
	}
}
