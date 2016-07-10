package thibault.graphEmbeddings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import statistics.Distributions;




public class Graph {
	int k; //number of nodes
	int sizeSpace;
	ArrayList<Node> nodes; //node list
	double probLinkExist=0.7; //probability of link existence between nodes
	public RealMatrix D;
	public RealMatrix L;
	public RealMatrix W;
	public RealMatrix V;
	public double[] eigVal;
	ArrayList<RealVector> VFin;

	int nbGradStep=1000;
	double step=0.01;
	double lambda=0.1;


	public Graph(int k,int sizeSpace){
		this.k=k;
		this.sizeSpace=sizeSpace;
		nodes=new ArrayList<Node>();
		D= new Array2DRowRealMatrix(new double[k][k]);
		L= new Array2DRowRealMatrix(new double[k][k]);
		W= new Array2DRowRealMatrix(new double[k][k]);
		V= new Array2DRowRealMatrix(new double[k][k]);
		VFin = new ArrayList<RealVector>();
		this.eigVal=new double[k];
		for(int i=0;i<k;i++){
			for(int j=0;j<k;j++){
				W.setEntry(i, j,0.0);
				D.setEntry(i, j,0.0);
				L.setEntry(i, j,0.0);
			}
		}
		for(int i=0;i<k;i++){
			Node u = new Node(i,sizeSpace);
			nodes.add(u);
		}
		
		//this.initWeightsRandom();
		this.initWeightsDeterm(1);
		
		for(Node u: nodes){
			for(Node v:u.Vois.keySet()){
				System.out.println(u.Id+" "+v.Id+" "+u.Vois.get(v));
			}
			System.out.println();
		}

		this.generateW();
		//System.out.println(W);
		this.generateD();
		//System.out.println(D);
		this.generateL();
		System.out.println(L);
		this.generateGeneralEigenDec();
		this.calculateCoords();
		//this.calculateCoords1();
		this.calculateCoords2();
		
		for(Node u: nodes){
			System.out.println(u.Id+" "+u.coords+" "+u.coords2);
			
			for (Node v: nodes){
				if(v.Id!=u.Id){
					//System.out.println(u.Vois.get(v));
					//System.out.println(u.Id+" "+v.Id+"  "+u.Vois.get(v)+" "+Math.pow(u.coords.subtract(v.coords).getNorm(),2)+" "+Math.pow(u.coords2.subtract(v.coords2).getNorm(),2)); 
				}
			}

		}

	}

	public void generateL(){
		this.L=this.D.subtract(this.W);
	}

	public void generateD(){
		for(Node u: nodes){
			double dii=0.0;
			for(Node v:u.Vois.keySet()){
				dii+=u.Vois.get(v);
			}
			D.setEntry(u.Id, u.Id, dii);
		}
	}

	public void generateW(){
		for(Node u: nodes){
			for(Node v:u.Vois.keySet()){
				W.setEntry(u.Id, v.Id, u.Vois.get(v));
			}
		}
	}

	public void generateGeneralEigenDec(){
		EigenDecomposition Eig = new EigenDecomposition(new LUDecomposition(D).getSolver().getInverse().multiply(L));
		V=Eig.getV();
		//System.out.println(V);

		eigVal=Eig.getRealEigenvalues();
		HashMap<Integer,Double> indOrder = new HashMap<Integer,Double>();
		for(int i =0; i<k;i++){
			System.out.println(Eig.getRealEigenvalues()[i]);
			System.out.println(Eig.getV().getColumnVector(i));
			indOrder.put(i,Eig.getRealEigenvalues()[i]);
		}
		Map sortedInd =sortByValue(indOrder);

		for(Object k:sortedInd.keySet()){
			VFin.add(V.getColumnVector((int)k));
		}
		System.out.println(VFin);
	}

	public void calculateCoords(){
		for (Node u: nodes){
			System.out.println(u.Id);
			for(int j=1;j<sizeSpace+1;j++){
				u.coords.setEntry(j-1, VFin.get(j).getEntry(u.Id));	
			}
			//System.out.println(u.Id+" "+u.coords);
		}
	}

	public void calculateCoords1(){
		for (int i=0;i<nbGradStep;i++){
			for (Node u: nodes){
				RealVector grad= new ArrayRealVector(new double[sizeSpace]);
				for(int j=0;j<sizeSpace;j++){
					grad.setEntry(j, 0.0);
				}
				for (Node v: nodes){
					if(v.Id!=u.Id){
						if(u.Vois.keySet().contains(v)){
							grad=grad.add(u.coords1.subtract(v.coords1).mapMultiply(u.Vois.get(v)));
						}
					}
				}
				u.coords1=u.coords1.subtract(grad.mapMultiply(step));
			}
			//evaluateF();
			//System.out.println(i+" "+evaluateF());
		}

	}
	
	public void calculateCoords2(){
		for (int i=0;i<nbGradStep;i++){
			for (Node u: nodes){
				RealVector grad= new ArrayRealVector(new double[sizeSpace]);
				for(int j=0;j<sizeSpace;j++){
					grad.setEntry(j, 0.0);
				}
				for (Node v: nodes){
					if(v.Id!=u.Id){
						if(u.Vois.keySet().contains(v)){
							grad=grad.add(u.coords2.subtract(v.coords2).mapMultiply(u.Vois.get(v)));
						}
						else{
							grad=grad.subtract(u.coords2.subtract(v.coords2).mapMultiply(lambda/(Math.pow(u.coords2.subtract(v.coords2).getNorm(), 4))));
						}
					}
				}
				u.coords2=u.coords2.subtract(grad.mapMultiply(step));
			}
			//evaluateF();
			//System.out.println(i+" "+evaluateF());
		}

	}

	public double evaluateF2(){
		double val=0.0;
		for (Node u: nodes){
			for (Node v: nodes){
				if(v.Id!=u.Id){
					if(u.Vois.keySet().contains(v)){
						val+=u.Vois.get(v)*Math.pow(u.coords2.subtract(v.coords2).getNorm(),2);
						//System.out.println(val);
					}
					else{
						val+=lambda*1/Math.pow(u.coords2.subtract(v.coords2).getNorm(),2);
					}	
				}

			}
		}
		return val;
	}

	
	public void initWeightsRandom(){
		Distributions d = new Distributions();
		for(Node u: nodes){
			for(Node v: nodes){
				boolean isLinked=d.nextBoolean(probLinkExist);
				double poid=0.0;
				if(u.Id!=v.Id && isLinked){
					poid=Math.random();
					//poid=1;
					u.Vois.put(v, poid);
					v.Vois.put(u, poid);
				}
			}
		}
	}
	
	public void initWeightsDeterm(int cas){
		if(cas==0){
		for (int i=0;i<k-1;i++){
			double poid=0.0;
			//poid=Math.random();
			poid=1;
			nodes.get(i).Vois.put(nodes.get(i+1), poid);
			nodes.get(i+1).Vois.put(nodes.get(i), poid);
		}
		}
		if(cas==1){
			for (int i=0;i<k-1;i++){
				double poid=0.0;
				//poid=Math.random();
				poid=1;
				nodes.get(i).Vois.put(nodes.get(i+1), poid);
				nodes.get(i+1).Vois.put(nodes.get(i), poid);
			}
			double poid=0.0;
			//poid=Math.random();
			poid=1;
			nodes.get(0).Vois.put(nodes.get(k-1), poid);
			nodes.get(k-1).Vois.put(nodes.get(0), poid);
		}
		
		if(cas==2){
			for (int i=0;i<k-1;i++){
				double poid=0.0;
				//poid=Math.random();
				poid=1;
				nodes.get(i).Vois.put(nodes.get(i+1), poid);
				nodes.get(i+1).Vois.put(nodes.get(i), poid);
			}
			double poid=0.0;
			//poid=Math.random();
			poid=1;
			nodes.get(0).Vois.put(nodes.get(k-1), poid);
			nodes.get(k-1).Vois.put(nodes.get(0), poid);
			nodes.get(1).Vois.put(nodes.get(k-1), poid);
			nodes.get(k-1).Vois.put(nodes.get(1), poid);
		}
		
		
	}
	
	
	
	
	public static Map sortByValue(Map unsortMap) {	 
		List list = new LinkedList(unsortMap.entrySet());

		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		Map sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	class ValueComparator implements Comparator {

		Map map;

		public ValueComparator(Map map) {
			this.map = map;
		}

		public int compare(Object keyA, Object keyB) {
			Comparable valueA = (Comparable) map.get(keyA);
			Comparable valueB = (Comparable) map.get(keyB);
			return valueB.compareTo(valueA);
		}
	}

	public static void main(String[] args){
		Graph g = new Graph(10,2);
	}
}
