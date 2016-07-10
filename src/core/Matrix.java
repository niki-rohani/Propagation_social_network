package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeMap;
public class Matrix implements Structure{
	private ArrayList<MatrixLine> mat;
	private Matrix reversedMatrix;
	//private SortedSet<Integer> colkeys;
	private int maxColKey=-1;
	
	public Matrix(){
		mat=new ArrayList<MatrixLine>();
		//colkeys=new SortedSet<Integer>();
	}
	public int add(TreeMap<Integer,Double> vals){
		SparseMatrixLine line=new SparseMatrixLine(vals);
		/*for(Integer k:vals.keySet()){
			colkeys.add(k);
		}*/
		Integer m=line.maxKey();
		if((vals.size()>0) && (m>maxColKey)){
			maxColKey=m;
		}
		/*for(Map.Entry<Integer, Double> v:vals.entrySet()){
			line.set(v.getKey(), v.getValue());
		}*/
		mat.add(line);
		return(mat.size()-1);
	}
	public int add(ArrayList<Double> vals){
		DenseMatrixLine line=new DenseMatrixLine(vals);
		mat.add(line);
		if((vals.size()-1)>maxColKey){
			maxColKey=(vals.size()-1);
		}
		return(mat.size()-1);
	}
	public int add(MatrixLine line){
		mat.add(line);
		int m=line.maxKey();
		if((line.size()>0) && (m>maxColKey)){
			maxColKey=m;
		}
		return(mat.size()-1);
	}
	
	
	
	public Double get(int i,int j){
		MatrixLine line=mat.get(i);
		if(line==null){
			return(0.0);
		}
		return(line.get(j));
	}
	public Matrix getReversedMatrix(){
		if (reversedMatrix!=null){
			return(reversedMatrix);
		}
		reversedMatrix=new Matrix();
		for(int j=0;j<=maxColKey;j++){
			ArrayList<Double> line=new ArrayList<Double>();
			for(int i=0;i<mat.size();i++){
				MatrixLine li=mat.get(i);
				line.add(li.get(j));
			}
			reversedMatrix.add(line);
		}
		
		return(reversedMatrix);
	}
	
	public Matrix dotProducts(HashMap<Integer,Double> vec){
		return(dotProducts(vec,0.0));
	}
	public Matrix dotProducts(HashMap<Integer,Double> vec, Double bias){
		Matrix ret=new Matrix();
		for(int i=0;i<mat.size();i++){
			MatrixLine h=mat.get(i);
			MatrixLine nh=h.dotProduct(vec,bias);
			ret.add(nh);
		}
		return(ret);
	}
	public Matrix sigmoid(double lambda){
		Matrix ret=new Matrix();
		for(int i=0;i<mat.size();i++){
			MatrixLine h=mat.get(i);
			MatrixLine nh=h.sigmoid(lambda);
			ret.add(nh);
		}
		return(ret);
	}
	
	public int size(){
		return(mat.size());
	}
	
	public String toString(){
		int s=mat.size();
		//System.out.println("maxcolkey = "+maxColKey);
		String ret="\t ";
		for(int j=0;j<=maxColKey;j++){
			ret+=j+"\t ";
		}
		ret+="\n";
		for(int i=0;i<s;i++){
			ret+=i+"\t";
			for(int j=0;j<=maxColKey;j++){
				ret+=get(i,j)+"\t";
			}
			ret+="\n";
		}
		return(ret);
	}
	
	public void insertValsAsNewColumn(Matrix matrix){
		int s=matrix.size();
		int m=matrix.maxColKey;
		if(m<0){
			return;
		}
		int mc=maxColKey;
		int ms=mat.size();
		if(ms<s){
			mat.ensureCapacity(s);
			for(int i=ms;i<s;i++){
				mat.add(new DenseMatrixLine());
			}
		}
		for(int i=0;i<s;i++){
			MatrixLine line=mat.get(i);
			
			for(int j=0;j<=m;j++){
				Double v=matrix.get(i, j);
				line.set(j+mc+1, v);
			}
		}
		maxColKey=mc+m+1;
	}
	
	public static void main(String[] args){
		Matrix m1=new Matrix();
		ArrayList<Double> h2=new ArrayList<Double>();
		h2.add(39.0);
		h2.add(0.5);
		m1.add(h2);
		TreeMap<Integer,Double> h=new TreeMap<Integer,Double>();
		h.put(0,1.0);
		h.put(1,2.0);
		h.put(2,3.0);
		m1.add(h);
		System.out.println(m1);
		Matrix m2=new Matrix();
		h=new TreeMap<Integer,Double>();
		h.put(1,3.0);
		h.put(2,5.0);
		h.put(4,1.0);
		m2.add(h);
		
		h=new TreeMap<Integer,Double>();
		h.put(1,10.0);
		h.put(3,12.0);
		m2.add(h);
		System.out.println(m2);
		Matrix m=new Matrix();
		m.insertValsAsNewColumn(m1);
		System.out.println(m);
		m.insertValsAsNewColumn(m2);
		System.out.println(m);
		HashMap<Integer,Double> pars=new HashMap<Integer,Double>();
		pars.put(0,1.0);
		pars.put(4,-2.0);
		m=m.dotProducts(pars,2.0);
		System.out.println(m);
		m=m.sigmoid(1);
		System.out.println(m);
		
	}
}



abstract class MatrixLine implements Structure{
	/*protected Matrix from;
	public MatrixLine(Matrix from){
		this.from=from;
	}*/
	public abstract Double get(int i);
	public abstract void set(int i,Double v);
	//public abstract Collection<Integer> keySet();
	public abstract Collection<Double> values();
	public abstract MatrixLine dotProduct(HashMap<Integer,Double> vec,double bias);
	public abstract MatrixLine sigmoid(double lambda);
	public abstract String toString();
	public abstract Integer maxKey();
	public abstract int size();
}

class SparseMatrixLine extends MatrixLine{
	//private LinkedHashMap<Integer,Double> line;
	private TreeMap<Integer,Double> line;
	//private Integer lastAccessed=null;
	//private ArrayList<Integer> keys;
	//private HashSet<Integer> keys;
	public SparseMatrixLine(){
		//super(from);
		line=new TreeMap<Integer,Double>();
		//keys=new ArrayList<Integer>();	
	}
	public SparseMatrixLine(TreeMap<Integer,Double> vals){
		//super(from);
		line=vals;
		//keys=new ArrayList<Integer>();	
	}
	public int size(){
		return line.size();
	}
	//public void 
	public Double get(int i){
	  Double d=line.get(i);
	  if(d==null){
		 d=0.0;
	  }
	  return(d);
	}
	public void set(int i,Double v){
		/*if(!line.containsKey(i)){
			keys.add(i);
		}*/
		line.put(i,v);
	}
	public Collection<Integer> keySet(){
		return line.keySet();
	}
	public Collection<Double> values(){
		return line.values();
	}
	
	public Iterator<Double> iterator(){
		return(line.values().iterator());
	}
	
	public MatrixLine dotProduct(HashMap<Integer,Double> vec,double bias){
		double sum=bias;
		for(Map.Entry<Integer,Double> el:line.entrySet()){
			Double v=el.getValue();
			Double p=vec.get(el.getKey());
			p=(p==null)?0.0:p;
			sum+=v*p;
		}
		MatrixLine ret=new SparseMatrixLine();
		ret.set(0,sum);
		return ret;
	}
	
	public MatrixLine sigmoid(double lambda){
		MatrixLine ret=new SparseMatrixLine();
		for(Map.Entry<Integer,Double> el:line.entrySet()){
			Double v=el.getValue();
			v=1.0/(Math.exp(-lambda*v)+1.0);
			ret.set(el.getKey(), v);
		}
		return ret;
	}
	
	public String toString(){
		//HashSet<Integer> vus=new HashSet<Integer>();
		String ret="";
		int i=0;
		int nb=line.size();
		int nbvus=0;
		while(nbvus<nb){
			if(line.containsKey(i)){
				nbvus++;
				ret+="\t"+line.get(i);
			}
			i++;
		}
		ret+="\n";
		return(ret);
	}
	
	public Integer maxKey(){
		return(line.lastKey());
	}
	
}


class DenseMatrixLine extends MatrixLine{
	private ArrayList<Double> line;
	public DenseMatrixLine(){
		line=new ArrayList<Double>();
	}
	public DenseMatrixLine(ArrayList<Double> vals){
		line=vals;
	}
	public Double get(int i){
	  if(i>=line.size()){
	      return(0.0);
	  }
	  Double d=line.get(i);
	  
	  return(d);
	}
	public void set(int i,Double v){
		if(i>=line.size()){
			line.ensureCapacity(i+1);
			int s=line.size();
			for(int k=s;k<=i;k++){
				line.add(0.0);
				//System.out.println(line.size());	
			}
		}
		//System.out.println(" i "+i+" size "+line.size());
		line.set(i,v);
	}
	
	public Collection<Double> values(){
		return line;
	}
	
	public MatrixLine dotProduct(HashMap<Integer,Double> vec,double bias){
		double sum=bias;
		for(int i=0;i<line.size();i++){
			Double v=line.get(i);
			Double p=vec.get(i);
			p=(p==null)?0.0:p;
			sum+=v*p;
		}
		
		MatrixLine ret=new DenseMatrixLine();
		ret.set(0,sum);
		return ret;
	}
	
	public MatrixLine sigmoid(double lambda){
		ArrayList<Double> lret=new ArrayList<Double>();
		for(int i=0;i<line.size();i++){
			Double v=line.get(i);
			v=1.0/(Math.exp(-lambda*v)+1.0);
			lret.add(i, v);
		}
		MatrixLine ret=new DenseMatrixLine(lret);
		
		return ret;
	}
	
	public int size(){
		return(line.size());
	}
	
	public String toString(){
		String ret="";
		for(int i=0;i<line.size();i++){
			ret+="\t"+line.get(i);
		}
		ret+="\n";
		return(ret);
	}
	public Integer maxKey(){
		return line.size()-1;
	}
}


/*
 * public abstract class Matrix<V extends java.lang.Number> implements Structure {
	
	//public abstract Set<Map.Entry<Integer,HashMap<Integer,V>>> entrySet();
	
	public abstract int add(MatrixLine<V> vals);
	
	
	public abstract V get(Integer i,Integer j);
	
	public abstract MatrixLine<V> get(Integer i);
	
	//public abstract 
	//public abstract Collection<Integer> keySet();
	public abstract Collection<MatrixLine<V>> values();
	
	//public abstract Collection<Integer> getKeys1();
	
	//public abstract Collection<Integer> getKeys2();
	
	
	public abstract void set(Integer i, Integer j, V val);
	
	public abstract void set(Integer i, MatrixLine<V> vals);
	
	public abstract String toString();
	
	public abstract Matrix<V> getReversedMatrix();
	
	public abstract Matrix<V> dotProducts(HashMap<Integer,Double> vec);
	public abstract Matrix<V> dotProducts(HashMap<Integer,Double>vec, V bias);
	public abstract Matrix<V> sigmoid(double lambda);
	//public abstract void insertValsAsNewColumn(Matrix<V> mat);
	//public abstract Matrix<V> add(Matrix<V> mat);
	
	
}
 */


/*
 * package core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.TreeMap;
public class SparseMatrix extends Matrix<Double> {
	private TreeMap<Integer,SparseMatrixLine> mat;
	//private HashSet<Integer> keys1;
	//private HashSet<Integer> columnsIndices;
	private SparseMatrix reversedMatrix;
	
	
	public SparseMatrix(){
		mat=new TreeMap<Integer,SparseMatrixLine>();
		//keys1=new HashSet<Integer>();
		//columnsIndices=new HashSet<Integer>();
	}
	
	
	
	public int add(MatrixLine<Double> line){
		mat.put(mat.lastKey()+1, (SparseMatrixLine)line);
		return mat.lastKey();
	}
	
	public Set<Map.Entry<Integer,SparseMatrixLine>> entrySet(){
		return(mat.entrySet());
	}
	
	public Collection<Integer> keySet(){
		return mat.keySet();
	}
	
	public Collection<SparseMatrixLine> values(){
		return mat.values();
	}
	
	public Iterator<SparseMatrixLine> iterator(){
		return(mat.values().iterator());
	}
	
	public Double get(Integer i,Integer j){
		if (!mat.containsKey(i)){
			return(0.0);
		}
		SparseMatrixLine li=mat.get(i);
		return(li.get(j));
	}
	public MatrixLine<Double> get(Integer i){
		return(mat.get(i));
	}
	
	
	public void set(Integer i, Integer j, Double val){
		SparseMatrixLine li;
		if (!mat.containsKey(i)){
			li=new SparseMatrixLine();
			mat.put(i, li);
		}
		else{
			li=mat.get(i);
		}
		li.set(j,val);
		
		//columnsIndices.add(j);
	}
	public void set(Integer i, MatrixLine<Double> vals){
		SparseMatrixLine vls=(SparseMatrixLine)vals;
		mat.put(i,vls);
		//for(Integer k:vals.keySet()){
		//	columnsIndices.add(k);
		//}
	}
	
	public String toString(){
		//String ret="\t ";
		//
		//for(Integer k:columnsIndices){
		//	ret+=k+"\t";
		//}
		//ret+="\n";
		String ret="";
		for(Integer k:mat.keySet()){
			ret+=k+"\t";
			MatrixLine<Double> li=get(k);
			ret+=li.toString();
			//for(Integer k2:columnsIndices){
			//	ret+=li.get(k2)+"\t";
			//
			//ret+="\n";
		}
		ret+="\n";
		return(ret);
	}
	
	public Matrix<Double> getReversedMatrix(){
		if (reversedMatrix!=null){
			return(reversedMatrix);
		}
		reversedMatrix=new SparseMatrix();
		for(Integer k1:mat.keySet()){
			SparseMatrixLine li=mat.get(k1);
			if (li!=null){
				for(Integer k2:li.keySet()){
					reversedMatrix.set(k2,k1,li.get(k2));
				}
			}
		}
		
		return(reversedMatrix);
	}
	
	public Matrix<Double> dotProducts(HashMap<Integer,Double> vec){
		return(dotProducts(vec,0.0));
	}
	public Matrix<Double> dotProducts(HashMap<Integer,Double> vec, Double bias){
		SparseMatrix ret=new SparseMatrix();
		//MatrixList ret=new MatrixList();
		for(Map.Entry<Integer,SparseMatrixLine> entry:mat.entrySet()){
			MatrixLine<Double> h=entry.getValue();
			MatrixLine<Double> nh=h.dotProduct(vec,bias);
			ret.set(entry.getKey(), nh);
		}
		return(ret);
	}
	
	public Matrix<Double> sigmoid(double lambda){
		SparseMatrix ret=new SparseMatrix();
		for(Map.Entry<Integer,SparseMatrixLine> entry:mat.entrySet()){
			MatrixLine<Double> h=entry.getValue();
			MatrixLine<Double> nh=h.sigmoid(lambda);
			ret.set(entry.getKey(), nh);
		}
		return(ret);
	}
	
}
 */

/*
 * package core;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
public class DenseMatrix extends Matrix<Double> {
	private ArrayList<DenseMatrixLine> mat;
	//private ArrayList<Integer> keys1;
	//private HashSet<Integer> keys2;
	
	private DenseMatrix reversedMatrix;
	
	
	public DenseMatrix(){
		mat=new ArrayList<DenseMatrixLine>();
		//keys1=new ArrayList<Integer>();
		//keys2=new HashSet<Integer>();
	}
	
	public Double get(Integer i,Integer j){
		//if (!keys1.contains(i)){
		//	return(null);
		//}
		if(i>=mat.size()){
			return(null);
		}
		DenseMatrixLine li=mat.get(i);
		
		return(li.get(j));
	}
	
	public MatrixLine<Double> get(Integer i){
		return(mat.get(i));
	}
	
	
	public int add(MatrixLine<Double> line){
		mat.add((DenseMatrixLine)line);
		return mat.size()-1;
	}
	
	public void set(Integer i, Integer j, Double val){
		DenseMatrixLine li;
		if(i>=mat.size()){
			mat.ensureCapacity(i+1);
			for(int k=mat.size();k<=i;k++){
				mat.add(null);
					
			}
		}
		li=mat.get(i);
		if(li==null){
			li=new DenseMatrixLine();
			mat.set(i, li);
		}
		li.set(j, val);
		
	}
	public void set(Integer i, MatrixLine<Double> vals){
		DenseMatrixLine vls=(DenseMatrixLine)vals;
		set(i,0,1.0);
		mat.set(i, vls);
	}
	
	public String toString(){
		//String ret="\t ";
		String ret="";
		for(int k=0;k<mat.size();k++){
			ret+=k+"\t";
			DenseMatrixLine li=mat.get(k);
			if(li!=null){
					ret+=li.toString();
			}
			else{
				ret+="null";
			}
			
			ret+="\n";
		}
		ret+="\n";
		return(ret);
	}
	
	public Matrix<Double> getReversedMatrix(){
		if (reversedMatrix!=null){
			return(reversedMatrix);
		}
		reversedMatrix=new DenseMatrix();
		for(int i=0;i<mat.size();i++){
			DenseMatrixLine li=mat.get(i);
			if (li!=null){
				for(int j=0;j<li.size();j++){
					reversedMatrix.set(j,i,li.get(j));
				}
			}
		}
		
		return(reversedMatrix);
	}
	public Matrix<Double> dotProducts(HashMap<Integer,Double> vec){
		return(dotProducts(vec,0.0));
	}
	public Matrix<Double> dotProducts(HashMap<Integer,Double> vec, Double bias){
		DenseMatrix ret=new DenseMatrix();
		for(int i=0;i<mat.size();i++){
			DenseMatrixLine h=mat.get(i);
			MatrixLine<Double> nh=h.dotProduct(vec,bias);
			ret.set(i, nh);
		}
		return(ret);
	}
	public Matrix<Double> sigmoid(double lambda){
		
		DenseMatrix ret=new DenseMatrix();
		for(int i=0;i<mat.size();i++){
			DenseMatrixLine h=mat.get(i);
			MatrixLine<Double> nh=h.sigmoid(lambda);
			
			ret.set(i, nh);
		}
		return(ret);
	}
		
	public void insertValsAsNewColumn(Matrix<Double> mat){
		for(MatrixLine<Double> line:mat.values()){
			
		}
	}
}


*/
 

