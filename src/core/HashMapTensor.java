package core;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
public class HashMapTensor extends Tensor{
	private HashMap<Integer,Matrix> matrix;
	public HashMapTensor(){
		matrix=new HashMap<Integer,Matrix>();
	}
	/*public void set(Integer i, Integer j, Integer k, V v){
		Matrix<V> m=matrix.get(i);
		if(m==null){
			m=new Matrix<V>();
			matrix.put(i, m);
		}
		m.set(j,k, v);
	}*/
	public void set(Integer i,Matrix m){
		matrix.put(i,m);
	}
	/*public Set<Map.Entry<Integer,Matrix<V>>> entrySet(){
		return(matrix.entrySet());
	}*/
	
	public Matrix getMatrix(Integer i){
		return(matrix.get(i));
	}
	public Double get(Integer i,Integer j,Integer k){
		Matrix m=matrix.get(i);
		if(m==null){
			return(null);
		}
		return(m.get(j,k));
	}
	/*public HashMap<Integer,V> getLine(Integer i,Integer j){
		Matrix<V> m=matrix.get(i);
		if(m==null){
			return(null);
		}
		return(m.get(j));
	}*/
	public String toString(){
		return(matrix.toString());
	}
	public Set<Integer> keySet(){
		return(matrix.keySet());
	}
	public Collection<Matrix> values(){
		return matrix.values();
	}
	
	public int add(Matrix m){
		Set<Integer> keys=matrix.keySet();
		int nk=0;
		for(int i=0;i<matrix.size()+1;i++){
			if(!keys.contains(i)){
				nk=i;
				break;
			}
		}
		matrix.put(nk, m);
		return(nk);
	}
	
	public Iterator<Matrix> iterator(){
		return matrix.values().iterator();
	}
}

