package optimization;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class LossFonction extends FonctionWithSub {
	protected ArrayList<Double> labels=null;
	//protected Fonction fonction;
	
	public void setLabels(ArrayList<Double> labels){
		this.labels=labels;
		if (derivative!=null){
				derivative.setLabels(labels);
		}
		fonctionChanged();
	}
	
}

class MinusLabels extends LossFonction{
	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		//System.out.println(fonction);
		//System.out.println(vals);
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> fv=vals.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			double ref=labels.get(i);
			for(Integer in:dimIndices){
				Double s=fv.get(in);
				double v=0.0;
				if (s!=null){
					v=s;
				}
				double dif=v-ref;
				if (dif!=0.0){
					h.put(in,dif);
				}
			}
			values.add(h);
		}
		//System.out.println(this);
		//System.out.println(values);
	}
	@Override
	public void buildDerivativeFonction() {
		derivative=fonction.getDerivativeFonction();
		derivative.setLabels(labels);
	}
	public String toString(){
		return("MinusLabels("+fonction+")"); //,"+labels+")");
	}
}

class SquaredLoss extends LossFonction{
	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		//System.out.println(fonction);
		//System.out.println(vals);
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> fv=vals.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			double ref=labels.get(i);
			for(Integer in:dimIndices){
				Double s=fv.get(in);
				double v=0.0;
				if (s!=null){
					v=s;
				}
				double dif=v-ref;
				dif=dif*dif;
				if (dif!=0.0){
					h.put(in,dif);
				}
			}
			values.add(h);
		}
	}
	
	@Override
	public void buildDerivativeFonction() {
		MinusLabels ml=new MinusLabels();
		ml.setSubFunction(fonction);
		ml.setLabels(labels);
		TimesConstant tc=new TimesConstant(2);
		tc.setSubFunction(ml);
		Times times=new Times();
		times.setSubFunction(tc);
		times.setSubFunction(fonction.getDerivativeFonction());
		derivative=times;
	}
	
	public String toString(){
		return("SquaredLoss("+fonction+")"); //,"+labels+")");
	}

	/*@Override
	public void computeGradients() {
		ArrayList<HashMap<Integer, Double>> grads=fonction.getGradients();
		ArrayList<Double> vals=fonction.getValues();
		//ArrayList<Double> values=new ArrayList<Double>();
		gradients=new ArrayList<HashMap<Integer, Double>>();
		for(int i=0;i<vals.size();i++){
			double v=vals.get(i);
			double ref=labels.get(i);
			double dif=v-ref;
			v=2*dif;
			HashMap<Integer, Double> ogi=grads.get(i);
			HashMap<Integer, Double> gi=new HashMap<Integer, Double>();
			for(Integer j:ogi.keySet()){
				double g=ogi.get(j);
				gi.put(j, g*v);
			}
			gradients.add(gi);
		}
	}*/
	/*@Override
	public void computeSecondDerivatives() {
		ArrayList<HashMap<Integer, Double>> sec=fonction.getSecondDerivatives();
		secondDerivatives=new ArrayList<HashMap<Integer, Double>>();
		ArrayList<HashMap<Integer, Double>> grads=fonction.getGradients();
		ArrayList<Double> vals=fonction.getValues();
		for(int i=0;i<vals.size();i++){
			double v=vals.get(i);
			double ref=labels.get(i);
			double dif=v-ref;
			HashMap<Integer, Double> ogi=grads.get(i);
			HashMap<Integer, Double> osi=sec.get(i);
			HashMap<Integer, Double> si=new HashMap<Integer, Double>();
			for(Integer j:ogi.keySet()){
				double g=ogi.get(j);
				double s=osi.get(j);
				g=2*g*g;
				s=s*2*dif;
				si.put(j, g+s);
			}
			secondDerivatives.add(si);
		}
	}*/

}
/*class HingeLossRegression extends LossFonction{
	private static final long serialVersionUID = 1L;
	protected double margin;
	public HingeLossRegression(){
		this(1.0);
	}
	public HingeLossRegression(double margin){
		this.margin=margin;
	}
	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		//System.out.println(fonction);
		//System.out.println(vals);
		values=new ArrayList<HashMap<Integer,Double>>();
		for(int i=0;i<vals.size();i++){
			HashMap<Integer,Double> fv=vals.get(i);
			HashMap<Integer,Double> h=new HashMap<Integer,Double>();
			double ref=labels.get(i);
			for(Integer in:dimIndices){
				Double s=fv.get(in);
				double v=0.0;
				if (s!=null){
					v=s;
				}
				double dif=v-ref;
				dif=dif*dif;
				if (dif!=0.0){
					h.put(in,dif);
				}
			}
			values.add(h);
		}
	}
	@Override
	public void buildDerivativeFonction() {
	
	}
	public String toString(){
		return("HingeLoss("+fonction+")"); //,"+labels+")");
	}
	@Override
	public void setThings(Fonction f){
		this.margin=((HingeLoss)f).margin;
	}
}*/