package optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
public abstract class BinaryOperator extends FonctionWithSub {
	protected Fonction fonction2;
	@Override
	public boolean setSubFunction(Fonction fonction){
		boolean ret=super.setSubFunction(fonction);
		if (ret){
			return(true);
		}
		if (this.fonction2==null){
			this.fonction2=fonction;
			this.fonction2.addListener(this);
			fonctionChanged();
			return(true);
		}
		else{
			ret=this.fonction2.setSubFunction(fonction);
			this.fonction2.depth=this.depth+1;
			return(ret);
		}
	}
	@Override
	public void fonctionChanged(){
		values=null;
		//gradients=null;
		//secondDerivatives=null;
		
		
		if(fonction!=null){
			HashSet<Integer> d1=fonction.dimIndices;
			dimIndices=d1;
			if (fonction2!=null){
				HashSet<Integer> d2=fonction2.dimIndices;
				if ((d2.size()==1) && (d2.iterator().next()==0)){
					Fonction tmp=fonction;
					fonction=fonction2;
					fonction2=tmp;
				}
				dimIndices=fonction2.dimIndices;
			}
		}
		
		
		/*if (fonction!=null){
			dimIndices=fonction.dimIndices;
			
		}
		if (fonction2!=null){
			if ((fonction2.dimIndices.size()>dimIndices.size()) || ((fonction2.dimIndices.size()==1) && (fonction2.dimIndices.iterator().next()>0))) {
				dimIndices=fonction2.dimIndices;
				
			}
		}*/
		for(Fonction f:listeners){
			f.fonctionChanged();
		}
	}
	@Override
	public void setSamples(ArrayList<HashMap<Integer,Double>> samples){
		super.setSamples(samples);
		if (fonction2!=null){
			fonction2.setSamples(samples);
		}
		
	}
	@Override
	public void setLabels(ArrayList<Double> labels){
		super.setLabels(labels);
		if (fonction2!=null){
			fonction2.setLabels(labels);
		}
	}
	@Override
	public void setParams(Parameters params){
		super.setParams(params);
		if (fonction2!=null){
			fonction2.setParams(params);
		}
	}
	@Override
	public Parameters getParams(){
		if (params==null){
			params=fonction.getParams();
		}
		if (params==null){
			params=fonction2.getParams();
		}
		return(params);
	} 
	
	@Override
	public Fonction copy(){
		try{
			BinaryOperator nf=this.getClass().newInstance();
			nf.setThings(this);
			if (fonction!=null){
				nf.setSubFunction(fonction.copy());
			}
			if (fonction2!=null){
				nf.setSubFunction(fonction2.copy());
			}
			return(nf);
		}
		catch(Exception e){
			System.out.println(e);
		}
		return(null);
	}
	@Override
	public Fonction getReverseParamsSamplesFonction(){
		try{
			System.out.println("reverse "+this.getClass());
			BinaryOperator nf=this.getClass().newInstance();
			nf.setThings(this);
			if (fonction!=null){
				nf.setSubFunction(fonction.getReverseParamsSamplesFonction());
			}
			if (fonction2!=null){
				nf.setSubFunction(fonction2.getReverseParamsSamplesFonction());
			}
			return(nf);
		}
		catch(Exception e){
			System.out.println(e);
		}
		return(null);
	}
}

// Term by term addition
class Plus extends BinaryOperator{
	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		ArrayList<HashMap<Integer,Double>> vals2=fonction2.getValues();
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> fv1=vals.get(i);
			HashMap<Integer,Double> fv2=vals2.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			for(Integer in:dimIndices){
				Double s=fv1.get(in);
				double v1=0.0;
				if (s!=null){
					v1=s;
				}
				s=fv2.get(in);
				double v2=0.0;
				if (s!=null){
					v2=s;
				}
				double v=v1+v2;
				//System.out.println("v1 ="+v);
				if (v>Double.MAX_VALUE){v=Double.MAX_VALUE;}
				if (v<-Double.MAX_VALUE){v=-Double.MAX_VALUE;}
				//System.out.println("v2 ="+v);
				h.put(in, v);
			}
			values.add(h);
		}

	}
	
	@Override
	public void buildDerivativeFonction(){
		Plus plus=new Plus();
		derivative=plus;
		plus.setSubFunction(fonction.getDerivativeFonction());
		plus.setSubFunction(fonction2.getDerivativeFonction());
	}
	public String toString(){
		return("Plus("+fonction+","+fonction2+")");
	}
}


// multiplication of values
// if one operand only contains one dimension : it is multiplied with every dimension of the other operand
// else term by term multiplication
class Times extends BinaryOperator{

	
	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		ArrayList<HashMap<Integer,Double>> vals2=fonction2.getValues();
		boolean termByTerm=false;
		if (fonction.dimIndices.size()>1){
			if (fonction2.dimIndices.size()>1){
				termByTerm=true;
			}
			else{
				System.out.println("Pas normal");
				/*ArrayList<HashMap<Integer,Double>> tmp=vals2;
				vals2=vals;
				vals=tmp;*/
			}
		}
		/*if (termByTerm){System.out.println(this);
			System.out.println(vals);
			System.out.println(vals2);
			throw new RuntimeException("Times : One operand should contain only one dimension");
		}*/
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> fv1=vals.get(i);
			HashMap<Integer,Double> fv2=vals2.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			double v1=0.0;
			double v2=0.0;
			Double s;
			if (!termByTerm){
				s=fv1.values().iterator().next();
				if (s!=null){
					v1=s;
				}
			}
			for(Integer in:dimIndices){
				if (termByTerm){
					v1=0.0;
					s=fv1.get(in);
					if (s!=null){
						v1=s;
					}
				}
				s=fv2.get(in);
				v2=0.0;
				if (s!=null){
					v2=s;
				}
				//System.out.println(v1+","+v2);
				double v=v1*v2;
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
	public void buildDerivativeFonction(){
		Plus plus=new Plus();
		Times t1=new Times();
		t1.setSubFunction(fonction.getDerivativeFonction());
		t1.setSubFunction(fonction2);
		Times t2=new Times();
		t2.setSubFunction(fonction);
		t2.setSubFunction(fonction2.getDerivativeFonction());
		plus.setSubFunction(t1);
		plus.setSubFunction(t2);
		derivative=plus;
	}
	public String toString(){
		return("Times("+fonction+","+fonction2+")");
	}
	
}