package core;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
public abstract class Tensor implements Structure {
	
	//public abstract void set(T i, T j, T k, V v);
	
	public abstract  void set(Integer i, Matrix m);
		
	public abstract  int add(Matrix m);
	
	//public abstract Set<Map.Entry<Integer,Matrix<V>>> entrySet();
	
	public abstract Matrix getMatrix(Integer i);
	
	//public abstract V get(T i,T j,T k);
	
	//public abstract HashMap<T,V> getLine(T i,T j);
	
	public abstract String toString();
	
	public abstract Set<Integer> keySet();
	
	public abstract Iterator<Matrix> iterator();
	
	public abstract Collection<Matrix> values();
	
}
