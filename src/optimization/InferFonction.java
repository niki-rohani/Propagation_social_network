package optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashMap;
import java.util.HashSet;

public abstract class InferFonction extends Fonction {

	//protected ParametrizedModel model;
	protected ArrayList<HashMap<Integer,Double>> samples;
	
	public void setSamples(ArrayList<HashMap<Integer,Double>> samples){
		this.samples=samples;
		//gradients=null;
		//secondDerivatives=null;
		fonctionChanged();
		
	}
	
	public void setParams(Parameters params){
		this.params=params;
		params.addListener(this);
		if (derivative!=null){
			derivative.setParams(params);
		}
		fonctionChanged();
	}

}

class DotFonction extends InferFonction{
	protected boolean bias;
	
	//private HashSet<Integer> sampleIndices;
	public DotFonction(){
		this(true);
		//sampleIndices=new 
	}
	
	public DotFonction(boolean bias){
		this(bias,0,-1);
	}
	
	public DotFonction(boolean bias,int firstParam,int nbParams){
		this.bias=bias;
		this.firstParam=firstParam;
		this.nbParams=nbParams;
	}
	
	@Override
	public void setSamples(ArrayList<HashMap<Integer,Double>> samples){
		
		this.samples=new ArrayList<HashMap<Integer,Double>>();
		for(HashMap<Integer,Double> sample:samples){
			HashMap<Integer,Double> samp=new HashMap<Integer,Double>();
			for(Integer i:sample.keySet()){
				double v=sample.get(i);
				samp.put(i, v);
			}
			if (bias){
				samp.put(0, 1.0);
			}
			this.samples.add(samp);
		}
		//gradients=null;
		//secondDerivatives=null;
		dimIndices=new HashSet<Integer>(); 
		dimIndices.add(0);
		if (derivative!=null){
			derivative.setSamples(samples);
		}
		fonctionChanged();
		
	}
	
	@Override
	public void inferValues() {
		HashMap<Integer,Double> pars=this.params.getParams();
		values=new ArrayList<HashMap<Integer,Double>>();
		
		for(HashMap<Integer,Double> sample:samples){
			double val=0.0;
			for(Integer i:sample.keySet()){
				//System.out.println(i);
				Double p=pars.get(i-firstParam);
				Double s=sample.get(i);
				double v=0.0;
				//if (i!=0){
				if (s!=null){v=(double)s;}
				p=(p==null)?0.0:p;
				//}
				//else{
				//	v=1.0;
				//}
				val+=v*p;
			}
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			h.put(0, val);
			values.add(h);
			//System.out.println(val);
		}
	}

	/*@Override
	public void computeGradients() {
		gradients=new ArrayList<HashMap<Integer, Double>>();
		for(HashMap<Integer,Double> sample:samples){
			HashMap<Integer, Double> h=new HashMap<Integer, Double>();
			for(Integer i:sample.keySet()){
				h.put(i, sample.get(i));
			}
			//if (bias){
			//	h.put(0, 1.0);
			//}
			gradients.add(h);
		}
	}*/
	
	/*@Override
	public void computeSecondDerivatives() {
		secondDerivatives=new ArrayList<HashMap<Integer, Double>>();
		for(HashMap<Integer,Double> sample:samples){
			HashMap<Integer, Double> h=new HashMap<Integer, Double>();
			secondDerivatives.add(h);
		}
	}*/
	
	@Override
	public void setThings(Fonction f){
		this.bias=((DotFonction)f).bias;
		this.firstParam=((DotFonction)f).firstParam;
	}
	
	/*@Override
	public Fonction copy(){
		DotFonction f=(DotFonction)super.copy();
		f.bias=this.bias;
		return(f);
	}*/
	@Override
	public Fonction getReverseParamsSamplesFonction(){
		if (params==null){throw new RuntimeException("pas de params !");}
		HashMap<Integer,Double> params=this.params.getParams();
		ArrayList<HashMap<Integer,Double>> samps=new ArrayList<HashMap<Integer,Double>>();
		HashMap<Integer,Double> s=new HashMap<Integer,Double>();
		samps.add(s);
		for(Integer p:params.keySet()){
			if (p!=0){
				s.put(p, params.get(p));
			}
		}
		Parameters pp=new Parameters();
		Fonction fonc=null;
		if (bias){
			PlusConstant pl=new PlusConstant(params.get(0));
			DotFonction dot=new DotFonction(false);
			pl.setSubFunction(dot);
			fonc=pl;
			//System.out.println(fonc);
		}
		else{
			fonc=new DotFonction(false);
		}
		fonc.setSamples(samps);
		fonc.setParams(pp);
		return(fonc);
	}
	
	@Override
	public void buildDerivativeFonction() {
		derivative=new ReturnSamples(bias);
		//System.out.println(params);
		derivative.setParams(params);
		derivative.setSamples(samples);
	}
	public String toString(){
		return("Dot("+bias+")"); //,"+params+","+samples+")");
	}
	
}

class ReturnSamples extends InferFonction{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	boolean bias;
	public ReturnSamples(){
		this(true);
	}
	public ReturnSamples(boolean bias){
		this.bias=bias;
	}
	@Override
	public void setSamples(ArrayList<HashMap<Integer,Double>> samples){
		dimIndices=new HashSet<Integer>();
		this.samples=new ArrayList<HashMap<Integer,Double>>();
		for(HashMap<Integer,Double> sample:samples){
			HashMap<Integer,Double> samp=new HashMap<Integer,Double>();
			for(Integer i:sample.keySet()){
				dimIndices.add(i);
				double v=sample.get(i);
				samp.put(i, v);
			}
			if (bias){
				dimIndices.add(0);
				samp.put(0, 1.0);
			}
			this.samples.add(samp);
		}
		//gradients=null;
		//secondDerivatives=null;
		if (derivative!=null){
			derivative.setSamples(samples);
		}
		fonctionChanged();
		
	}
	@Override
	public void buildDerivativeFonction() {
		derivative=new NullFonction(bias);
		derivative.setParams(params);
		derivative.setSamples(samples);
	}
	@Override
	public void inferValues() {
		values=samples;
		//System.out.println(this);
		//System.out.println(values);
	}
	/*@Override
	public Fonction copy(){
		ReturnSamples f=(ReturnSamples)super.copy();
		f.bias=this.bias;
		return(f);
	}*/
	@Override
	public void setThings(Fonction f){
		this.bias=((ReturnSamples)f).bias;
	}
	public String toString(){
		return("ReturnSamples("+bias+")"); //,"+samples+")");
	}
}

class NullFonction extends InferFonction{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//protected double constant;
	protected boolean bias;
	public NullFonction(){
		this(true);
	}
	public NullFonction(boolean bias){
		//this.constant=constant;
		this.bias=bias;
	}
	@Override
	public void setSamples(ArrayList<HashMap<Integer,Double>> samples){
		dimIndices=new HashSet<Integer>();
		this.samples=new ArrayList<HashMap<Integer,Double>>();
		for(HashMap<Integer,Double> sample:samples){
			HashMap<Integer,Double> samp=new HashMap<Integer,Double>();
			for(Integer i:sample.keySet()){
				dimIndices.add(i);
				double v=sample.get(i);
				samp.put(i, v);
			}
			if (bias){
				dimIndices.add(0);
				samp.put(0, 1.0);
			}
			this.samples.add(samp);
		}
		//gradients=null;
		//secondDerivatives=null;
		if (derivative!=null){
			derivative.setSamples(samples);
		}
		fonctionChanged();
		
	}
	@Override
	public void buildDerivativeFonction() {
		derivative=new NullFonction(bias); //Constant(0.0);
		derivative.setParams(params);
		derivative.setSamples(samples);
	}
	@Override
	public void inferValues() {
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<samples.size();i++){
			values.add(new HashMap<Integer,Double>());
		}
	}
	
	/*@Override
	public Fonction copy(){
		NullFonction f=(NullFonction)super.copy();
		f.bias=this.bias;
		return(f);
	}*/
	
	@Override
	public void setThings(Fonction f){
		this.bias=((NullFonction)f).bias;
	}
	
	public String toString(){
		return("NullFonction("+bias+")"); //,"+samples+")");
	}
}