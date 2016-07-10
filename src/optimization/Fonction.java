package optimization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
public abstract class Fonction implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected ArrayList<HashMap<Integer,Double>> values=null;
	//protected ArrayList<HashMap<Integer,Double>> gradients=null;
	//protected ArrayList<HashMap<Integer,Double>> secondDerivatives=null;
	protected HashSet<Fonction> listeners;
	protected int depth;
	protected Parameters params=null;
	protected Fonction derivative=null;
	protected int firstParam=0;
	protected int nbParams=-1;
	protected HashSet<Integer> dimIndices=new HashSet<Integer>(); // dimensions des valeurs calculees 
	
	
	public Fonction(){
		depth=0;
		listeners=new HashSet<Fonction>();
	}
	
	public void fonctionChanged(){
		values=null;
		//gradients=null;
		//secondDerivatives=null;
		//System.out.println("Fonction changed : "+this);
		for(Fonction f:listeners){
			f.fonctionChanged();
		}
	}
	
	public void addListener(Fonction f){
		listeners.add(f);
	}
	public void clearListeners(){
		this.listeners=new HashSet<Fonction>();
	}
	public Parameters getParams(){
		return(params);
	}
	
	public Fonction getDerivativeFonction(){
		if (derivative==null){
			buildDerivativeFonction();
		}
		return(derivative);
	}
	
	public abstract void buildDerivativeFonction(); 
	
	
	
	
	/*{
		System.out.println("buildDerivativeFonction pas implemente pour ce type de fonction...");
	}*/
	
	/*public void reinit(){
		values=null;
		gradients=null;
	}*/
	
	public void setSamples(ArrayList<HashMap<Integer,Double>> samples){
		//System.out.println("Pas de features pour cette fonction");
	}
	public void setLabels(ArrayList<Double> labels){
		//System.out.println("Pas de labels");
	}
	public boolean setSubFunction(Fonction fonction){
		return(false);
		//System.out.println("Pas de sous fonction");
	}
	
	public void setParams(Parameters params){
		this.params=params;
		//System.out.println("Pas de modele pour cette fonction");
	}
	// returns the values matrix
	public ArrayList<HashMap<Integer,Double>> getValues(){
		if (values==null){
			inferValues();
		}
		return(values);
	}
	// returns the values according to a given dimension
	public ArrayList<Double> getValues(int i){
		if (values==null){
			inferValues();
		}
		ArrayList<Double> vals=new ArrayList<Double>();
		for(int j=0;j<values.size();j++){
			double v=0.0;
			HashMap<Integer,Double> h=values.get(j);
			if(h.containsKey(i)){
				v=h.get(i);
			}
			vals.add(v);
		}
		return(vals);
	}
	
	// returns the value of the given sample (throws exception if the row of values for the sample in concern contains more than one dimension)
	public double getValue(int i){
		HashMap<Integer,Double> vals=getValues().get(i);
		if (vals.size()>1){
			throw new RuntimeException("Fonction "+this+" ne contient pas une valeur unique");
		}
		double val=0.0;
		if (vals.size()==1){
			val=vals.values().iterator().next();
		}
		return(val);
	}
	public double getValue(){
		return(getValue(0));
	}
	public abstract void inferValues();
		
	
	public ArrayList<HashMap<Integer,Double>> getGradients(){
		return(getDerivativeFonction().getValues());
	}
	public HashMap<Integer,Double> getGradient(int i){
		return(getDerivativeFonction().getValues().get(i));
	}
	public HashMap<Integer,Double> getGradient(){
		return(getGradient(0));
	}
	
	
	/*public ArrayList<HashMap<Integer,Double>> getGradients(){
		if (gradients==null){
			computeGradients();
		}
		return(gradients);
	}
	public HashMap<Integer,Double> getGradient(int i){
		return(getGradients().get(i));
	}
	public HashMap<Integer,Double> getGradient(){
		return(getGradient(0));
	}
	
	public void computeGradients(){
		System.out.println("ComputeGradients pas implemente pour ce type de fonction...");
	}
	
	public ArrayList<HashMap<Integer,Double>> getSecondDerivatives(){
		if (secondDerivatives==null){
			computeSecondDerivatives();
		}
		return(secondDerivatives);
	}
	public HashMap<Integer,Double> getSecondDerivative(int i){
		return(getSecondDerivatives().get(i));
	}
	public HashMap<Integer,Double> getSecondDerivative(){
		return(getSecondDerivative(0));
	}
	
	public void computeSecondDerivatives(){
		System.out.println("ComputeSecondDerivatives pas implemente pour ce type de fonction...");
	}*/
	
	public Fonction copy(){
		try{
			Fonction nf=this.getClass().newInstance();
			nf.setThings(this);
			return(nf);
		}
		catch(Exception e){
			System.out.println(e);
		}
		return(null);
	}
	public Fonction getReverseParamsSamplesFonction(){
		try{
			Fonction nf=this.getClass().newInstance();
			nf.setThings(this);
			return(nf);
		}
		catch(Exception e){
			System.out.println(e);
		}
		return(null);
	}
	
	public void setThings(Fonction f){}

}



