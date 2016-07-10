package core;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Set;
public class PairsValues<T,V> implements Structure {
	private HashMap<T,HashMap<T,HashMap<Integer,V>>> vals;
	private boolean arrayMode=true;
	
	public PairsValues(){
		this(true);
	}
	
	public PairsValues(boolean arrayMode){
		this.arrayMode=arrayMode;
		vals=new HashMap<T,HashMap<T,HashMap<Integer,V>>>();
	}
	
	public HashMap<Integer,V> getVals(T p1,T p2){
		HashMap<T,HashMap<Integer,V>> h=null;
		if (vals.containsKey(p1)){
			h=vals.get(p1);
		}
		else{
			h=new HashMap<T,HashMap<Integer,V>>();
			vals.put(p1,h);
		}
		HashMap<Integer,V> ret=null;
		if (h.containsKey(p2)){
			ret=h.get(p2);
		}
		else{
			ret=new HashMap<Integer,V>();
			h.put(p2, ret);
			HashMap<T,HashMap<Integer,V>> h2=vals.get(p2);
			if (h2==null){
				h2=new HashMap<T,HashMap<Integer,V>>();
				vals.put(p2, h2);
			}
			h2.put(p1,ret);
		}
		return(ret);
	}
	
	public void put(T p1,T p2, int i, V v){
		if (arrayMode){
			throw new RuntimeException("ArrayMode => impossible d utiliser fonction put(T,T,int,V)");
		}
		HashMap<Integer,V> r=getVals(p1,p2);
		r.put(i, v);
	}
	public void set(T p1,T p2, int i, V v){
		if (!arrayMode){
			throw new RuntimeException("ArrayMode off => impossible d utiliser fonction set(T,T,int,V)");
		}
		HashMap<Integer,V> r=getVals(p1,p2);
		if (i>=r.size()){
			throw new RuntimeException("PairsValues index out of bounds : "+i+" (limit="+(r.size()-1)+")");
		}
		r.put(i, v);
	}
	public void add(T p1,T p2, V v){
		if (!arrayMode){
			throw new RuntimeException("ArrayMode off => impossible d utiliser fonction add(T,T,V)");
		}
		HashMap<Integer,V> r=getVals(p1,p2);
		r.put(r.size(), v);
	}
	public void remove(T p1,T p2, int i){
		HashMap<Integer,V> r=getVals(p1,p2);
		r.remove(i);
	}
	public int size(T p1,T p2){
		return(getVals(p1,p2).size());
	}
	public V get(T p1, T p2, int i){
		HashMap<Integer,V> r=getVals(p1,p2);
		if (i>=r.size()){
			return null;
		}
		return r.get(i);
	}
	public Set<T> keySet(){
		return(vals.keySet());
	}
	public Set<T> keySet(T p){
		HashMap<T,HashMap<Integer,V>> h=null;
		if (vals.containsKey(p)){
			h=vals.get(p);
		}
		else{
			return new HashSet<T>();
		}
		return(h.keySet());
	}
}
