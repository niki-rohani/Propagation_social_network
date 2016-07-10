package mlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.Serializable;

public class Parameter implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static int nbPara=0;
	private int id;
	private double val=0;
	double gradient=0;
	double last_gradient=0;
	double direction=0;
	double last_direction=0;
	double lowerBound=-Double.MAX_VALUE;
	double upperBound=Double.MAX_VALUE;
	double last_val=0.0f;
	double maxMove=-1; //Maximal Move
	//int version=0;
	//Parameters parent=null;
	public Parameter(){
		val=0.0f;
		gradient=0.0f;
		nbPara++;
		id=nbPara;
		//this.params=new HashMap<Integer,Double>();
	}
	public Parameter(double val){
		gradient=0.0f;
		setVal(val);
		nbPara++;
		id=nbPara;
	}
	public Parameter(double val, double lowerBound, double upperBound){
		this.lowerBound=lowerBound;
		this.upperBound=upperBound;
		setVal(val);
		gradient=0.0f;
		nbPara++;
		id=nbPara;
	}
	
	// pour sauvegarde de parametres uniquement
	public Parameter(Parameter p){
		this.lowerBound=p.lowerBound;
		this.upperBound=p.upperBound;
		this.gradient=p.gradient;
		this.last_gradient=p.last_gradient;
		this.direction=p.direction;
		this.last_direction=p.last_direction;
		this.last_val=p.last_val;
		//this.version=p.version;
		setVal(p.val);
		nbPara++;
		id=nbPara;
	}
	
	public int hashCode(){
		return id;
	}
	
	public void setMaxMove(double maxMove){
		this.maxMove=maxMove;
	}
	
	public void setVal(double v){
		if(maxMove>=0){
			if(Math.abs(v-val)>maxMove){
				if(v<val){
					v=val-maxMove;
				}
				else{
					v=val+maxMove;
				}
			}
		}
		if(v!=this.last_val){
			this.last_val=val;
		}
		if(val!=v){
			this.val=v;
			if(val>this.upperBound){
				val=upperBound;
			}
			if(val<this.lowerBound){
				val=lowerBound;
			}
			last_direction=direction;
			direction=0.0f;
			gradient=0.0f;
		}
		/*if(val==1.0f){
			System.out.println("setVal "+val);
		}*/
		//paramsChanged();
	}
	
	
	public double getVal(){
		return val;
	}
	
	public void add(double v){
		if(maxMove>=0){
			if(Math.abs(v)>maxMove){
				if(v<0){
					v=-maxMove;
				}
				else{
					v=maxMove;
				}
			}
		}
		this.last_val=val;
		val+=v;
		if(val>this.upperBound){
			val=upperBound;
		}
		if(val<this.lowerBound){
			val=lowerBound;
		}
	}
	
	public double getGradient(){
		return gradient;
	}
	public void setGradient(double g){
		gradient=g;
	}
	
	public double getDirection(){
		return direction;
	}
	public void setLowerBound(double lb){
		this.lowerBound=lb;
		if(val<this.lowerBound){
			val=lowerBound;
		}
	}
	public void setUpperBound(double ub){
		this.upperBound=ub;
		if(val>this.upperBound){
			val=upperBound;
		}
	}
	
	
	
	
}
