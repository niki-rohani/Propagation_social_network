package optimization;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class MathFonction extends FonctionWithSub {
	
	

}


class LogitFonction extends MathFonction{
	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> vv=vals.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			for(Integer in:dimIndices){
				Double s=vv.get(in);
				double v=0.0;
				if (s!=null){
					v=s;
				}
				v=1.0/(Math.exp(-1.0*v)+1.0);
				if (v!=0.0){
					h.put(in, v);
				}
			}
				
			values.add(h);
		}

	}
	
	@Override
	public void buildDerivativeFonction() {
		Exp exp=new Exp();
		Minus min=new Minus();
		min.setSubFunction(fonction);
		exp.setSubFunction(min);
		Times times=new Times();
		times.setSubFunction(exp);
		times.setSubFunction(fonction.getDerivativeFonction());
		PlusConstant plus=new PlusConstant(1);
		plus.setSubFunction(exp);
		Power pow=new Power(2);
		pow.setSubFunction(plus);
		Inverse inv=new Inverse();
		inv.setSubFunction(pow);
		derivative=new Times();
		derivative.setSubFunction(times);
		derivative.setSubFunction(inv);
	}

	public String toString(){
		return("Logit("+fonction+")");
	}
	
	/*@Override
	public void computeGradients() {
		ArrayList<Double> vals=fonction.getValues();
		ArrayList<HashMap<Integer, Double>> grads=fonction.getGradients();
		gradients=new ArrayList<HashMap<Integer, Double>>();
		for(int i=0;i<vals.size();i++){
			double v=vals.get(i);
			HashMap<Integer, Double> gi=grads.get(i);
			HashMap<Integer, Double> ngi=new HashMap<Integer, Double>();
			gradients.add(ngi);
			double nomin=Math.exp(-1.0*v);
			double den=(Math.exp(-1.0*v)+1.0);
			den=den*den;
			double mult=nomin/den;
			for(Integer j:gi.keySet()){
				double x=gi.get(j);
				x*=mult;
				ngi.put(j, x);
			}
		}
	}*/
	
	/*@Override
	public void computeSecondDerivatives() {
		ArrayList<Double> vals=fonction.getValues();
		ArrayList<HashMap<Integer, Double>> grads=fonction.getGradients();
		ArrayList<HashMap<Integer, Double>> sec=fonction.getGradients();
		secondDerivatives=new ArrayList<HashMap<Integer, Double>>();
		for(int i=0;i<vals.size();i++){
			double v=vals.get(i);
			HashMap<Integer, Double> gi=grads.get(i);
			HashMap<Integer, Double> si=sec.get(i);
			HashMap<Integer, Double> nsi=new HashMap<Integer, Double>();
			secondDerivatives.add(nsi);
			double exp=Math.exp(-1.0*v);
			double expplus1=Math.exp(-1.0*v)+1;
			double den=expplus1*expplus1;
			den=den*den;
			for(Integer j:gi.keySet()){
				double gj=gi.get(j);
				double sj=si.get(j);
				double left=exp*(sj-(gj*gj))*expplus1*expplus1;
				double right=-2.0*expplus1*gj*gj*exp*exp;
				nsi.put(j, (left+right)/den);
			}
		}
	}*/
}
