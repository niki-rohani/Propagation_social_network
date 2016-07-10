package optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.Serializable;

public class Parameters implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HashMap<Integer,Double> params; // parameters (with 0 stands as the bias)
	private HashSet<Fonction> listeners;
	public Parameters(){
		this.listeners=new HashSet<Fonction>();
		this.params=new HashMap<Integer,Double>();
	}
	public HashMap<Integer,Double> getParams(){
		return(params);
	}
	public void setParams(HashMap<Integer,Double> params){
		this.params=params;
		paramsChanged();
	}
	public void addListener(Fonction f){
		listeners.add(f);
	}
	public void clearListeners(){
		this.listeners=new HashSet<Fonction>();
	}
	public void add(HashMap<Integer,Double> vec){
		add(vec,1.0);
	}
	public void add(HashMap<Integer,Double> vec,double coef){
		for(Integer i:vec.keySet()){
			double v=vec.get(i)*coef;
			double p=0.0;
			if (params.containsKey(i)){
				p=params.get(i);
			}
			params.put(i, p+v);
		}
		paramsChanged();
	}
	public void clearParameters(){
		this.params=new HashMap<Integer,Double>();
		paramsChanged();
	}
	public void paramsChanged(){
		for(Fonction f:listeners){
			f.fonctionChanged();
		}
	}
	public String toString(){
		String s="";
		for(Integer i:params.keySet()){
			double v=params.get(i);
			s+=i+":"+v+";";
		}
		return(s);
	}
}
