package core;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class MatrixList extends Tensor {
	private ArrayList<Matrix> matrixList;
	private HashSet<Integer> keys;
	public MatrixList(){
		matrixList=new ArrayList<Matrix>();
		keys=new HashSet<Integer>();
	}
	
	public void set(Integer i,Matrix m){
		if(i>=matrixList.size()){
			matrixList.ensureCapacity(i+1);
			for(int j=matrixList.size();j<=i;j++){
				matrixList.add(null);
			}
		}
		matrixList.set(i,m);
		keys.add(i);
	}
	
	
	
	public Matrix getMatrix(Integer i){
		if(i>=matrixList.size()){
			return(null);
		}
		return(matrixList.get(i));
	}
	public Double get(Integer i,Integer j,Integer k){
		Matrix m=getMatrix(i);
		if(m==null){
			return(null);
		}
		return(m.get(j,k));
	}
	
	public String toString(){
		return(matrixList.toString());
	}
	public Set<Integer> keySet(){
		return(keys);
	}
	public Collection<Matrix> values(){
		return matrixList;
	}
	public int add(Matrix m){
		matrixList.add(m);
		int n=matrixList.size()-1;
		keys.add(n);
		return(n);
	}
	public Iterator<Matrix> iterator(){
		return matrixList.iterator();
	}
}

