package optimization;

import java.util.ArrayList;
import java.util.HashMap;
public abstract class UnaryOperator extends FonctionWithSub {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}

class Minus extends UnaryOperator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void buildDerivativeFonction() {
		Minus min=new Minus();
		derivative=min;
		min.setSubFunction(fonction.getDerivativeFonction());
	}

	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> fv=vals.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			for(Integer in:dimIndices){
				Double s=fv.get(in);
				double v=0.0;
				if (s!=null){
					v=s;
				}
				v=(-1.0*v);
				
				h.put(in, v);
			}
			values.add(h);
		}

	}
	public String toString(){
		return("Minus("+fonction+")");
	}

}

class PlusConstant extends UnaryOperator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected double constant;
	public PlusConstant(){
		this(0.0);
	}
	public PlusConstant(double constant){
		this.constant=constant;
	}
	
	@Override
	public void buildDerivativeFonction() {
		derivative=fonction.getDerivativeFonction();
	}

	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> fv=vals.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			for(Integer in:dimIndices){
				Double s=fv.get(in);
				double v=0.0;
				if (s!=null){
					v=s;
				}
				v=(v+constant);
				if (v>Double.MAX_VALUE){v=Double.MAX_VALUE;}
				if (v<-Double.MAX_VALUE){v=-Double.MAX_VALUE;}
				h.put(in, v);
			}
			values.add(h);
		}

	}
	@Override
	public void setThings(Fonction f){
		this.constant=((PlusConstant)f).constant;
	}
	public String toString(){
		return("PlusConstant("+constant+","+fonction+")");
	}
}

class TimesConstant extends UnaryOperator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected double constant;
	
	public TimesConstant(){
		this(0.0);
	}
	
	public TimesConstant(double constant){
		this.constant=constant;
	}
	
	@Override
	public void buildDerivativeFonction() {
		TimesConstant times=new TimesConstant(constant);
		times.setSubFunction(fonction.getDerivativeFonction());
		derivative=times;
	}

	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> fv=vals.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			for(Integer in:dimIndices){
				Double s=fv.get(in);
				double v=0.0;
				if (s!=null){
					v=s;
				}
				v=(v*constant);
				if (v>Double.MAX_VALUE){v=Double.MAX_VALUE;}
				if (v<-Double.MAX_VALUE){v=-Double.MAX_VALUE;}
				h.put(in, v);
			}
			values.add(h);
		}
		//System.out.println(this);
		//System.out.println(values);

	}
	@Override
	public void setThings(Fonction f){
		this.constant=((TimesConstant)f).constant;
	}

	public String toString(){
		return("TimesConstant("+constant+","+fonction+")");
	}
}

class Exp extends UnaryOperator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void buildDerivativeFonction() {
		Exp exp=new Exp();
		exp.setSubFunction(fonction);
		Times times=new Times();
		times.setSubFunction(fonction.getDerivativeFonction());
		times.setSubFunction(exp);
		derivative=times;
		
	}

	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> fv=vals.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			for(Integer in:dimIndices){
				Double s=fv.get(in);
				double v=0.0;
				if (s!=null){
					v=s;
				}
				//System.out.print("exp de "+v);
				v=Math.exp(v);
				if (v>Double.MAX_VALUE){v=Double.MAX_VALUE;}
				if (v<-Double.MAX_VALUE){v=-Double.MAX_VALUE;}
				//System.out.println("= "+v);
				h.put(in, v);
			}
			values.add(h);
		}

	}
	public String toString(){
		return("Exp("+fonction+")");
	}

}

class Power   extends UnaryOperator {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected int power;
	
	public Power(){
		this(1);
	}
	
	public Power(int power){
		this.power=power;
		if (power<0){
			System.out.println("Power attend une puissance positive (utiliser inverse pour produire des puissances negatives)");
			this.power=0;
		}
	}
	
	@Override
	public void buildDerivativeFonction() {
		if (power==1){
			derivative=fonction.getDerivativeFonction();
		}
		else{
			TimesConstant times=new TimesConstant(power);
			if (power==0){
				times.setSubFunction(fonction);
				derivative=times;
			}
			else{
				Power pow=new Power(power-1);
				pow.setSubFunction(fonction);
				times.setSubFunction(pow);
				derivative=new Times();
				derivative.setSubFunction(times);
				derivative.setSubFunction(fonction.getDerivativeFonction());
			}
		}
	}

	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> fv=vals.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			for(Integer in:dimIndices){
				Double s=fv.get(in);
				double v=0.0;
				if (s!=null){
					v=s;
				}
				//System.out.print("pow de "+v);
				v=Math.pow(v,power);
				if (v>Double.MAX_VALUE){v=Double.MAX_VALUE;}
				if (v<-Double.MAX_VALUE){v=-Double.MAX_VALUE;}
				//System.out.println("= "+v);
				h.put(in, v);
			}
			values.add(h);
		}

	}
	@Override
	public void setThings(Fonction f){
		this.power=((Power)f).power;
	}
	public String toString(){
		return("Power("+power+","+fonction+")");
	}
}


class Inverse extends UnaryOperator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void buildDerivativeFonction() {
		Power pow=new Power(2);
		pow.setSubFunction(fonction);
		Inverse inv=new Inverse();
		inv.setSubFunction(pow);
		Times times=new Times();
		times.setSubFunction(fonction.getDerivativeFonction());
		times.setSubFunction(inv);
		derivative=new Minus();
		derivative.setSubFunction(times);
	}

	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> fv=vals.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			for(Integer in:dimIndices){
				Double s=fv.get(in);
				double v=0.0;
				if (s!=null){
					v=s;
				}
				//System.out.print("inv de "+v);
				v=1.0/(v*1.0);
				if (v>Double.MAX_VALUE){v=Double.MAX_VALUE;}
				if (v<-Double.MAX_VALUE){v=-Double.MAX_VALUE;}
				//System.out.println("= "+v);
				h.put(in, v);
			}
			values.add(h);
		}

	}
	public String toString(){
		return("Inverse("+fonction+")");
	}
}
