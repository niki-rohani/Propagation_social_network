package optimization;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class AggregationFonction extends FonctionWithSub {

}

class AverageFonction extends AggregationFonction{
	@Override
	public void inferValues() {
		ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		values=new ArrayList<HashMap<Integer,Double>>();
		//System.out.println(fonction);
		//System.out.println(vals);
		HashMap<Integer,Double> sums=new HashMap<Integer,Double>();
		for(Integer in:dimIndices){
			
			double sum=0.0;
			for(int i=0;i<vals.size();i++){
				Double s=vals.get(i).get(in);
				double v=0.0;
				if (s!=null){
					v=s;
				}
				sum+=v;
			}
		
			if (vals.size()>0){
				sum/=vals.size();
			}
			sums.put(in, sum);
		}
		values.add(sums);
	}
	
	@Override
	public void buildDerivativeFonction() {
		/*ArrayList<HashMap<Integer,Double>> vals=fonction.getValues();
		double nb=vals.size();
		double constant=0.0;
		if (nb>=0){
			constant=1.0/(1.0*nb);
		}
		derivative=new TimesConstant(constant);*/
		derivative=new AverageFonction();
		derivative.setSubFunction(fonction.getDerivativeFonction());
		
		
	}
	public String toString(){
		return("Average("+fonction+")");
	}
	
	/*@Override
	public void computeGradients() {
		ArrayList<HashMap<Integer, Double>> grads=fonction.getGradients();
		gradients=new ArrayList<HashMap<Integer, Double>>();
		HashMap<Integer, Double> g=new HashMap<Integer, Double>();
		gradients.add(g);
		for(int i=0;i<grads.size();i++){
			HashMap<Integer,Double> gi=grads.get(i);
			for(Integer j:gi.keySet()){
				double v=0.0;
				if (g.containsKey(j)){
					v=g.get(j);
				}
				v+=gi.get(j);
				g.put(j,v);
			}
		}
		int size=grads.size();
		if (size>0){
			for(Integer j:g.keySet()){
				double v=g.get(j);
				v/=size;
				g.put(j,v);
			}
		}
		

	}*/
	
	/*@Override
	public void computeSecondDerivatives() {
		ArrayList<HashMap<Integer, Double>> grads=fonction.getSecondDerivatives();
		secondDerivatives=new ArrayList<HashMap<Integer, Double>>();
		HashMap<Integer, Double> g=new HashMap<Integer, Double>();
		secondDerivatives.add(g);
		for(int i=0;i<grads.size();i++){
			HashMap<Integer,Double> gi=grads.get(i);
			for(Integer j:gi.keySet()){
				double v=0.0;
				if (g.containsKey(j)){
					v=g.get(j);
				}
				v+=gi.get(j);
				g.put(j,v);
			}
		}
		int size=grads.size();
		if (size>0){
			for(Integer j:g.keySet()){
				double v=g.get(j);
				v/=size;
				g.put(j,v);
			}
		}
	}*/
	
}
