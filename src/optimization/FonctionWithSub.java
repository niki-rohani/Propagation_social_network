package optimization;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class FonctionWithSub extends Fonction {
	protected Fonction fonction;
	
	public void setLabels(ArrayList<Double> labels){
		if (fonction!=null){
			fonction.setLabels(labels);
		}
	}
	public void setParams(Parameters params){
		this.params=params;
		if (fonction!=null){
			fonction.setParams(params);
		}
	}
	public boolean setSubFunction(Fonction fonction){
		if (this.fonction==null){
			this.fonction=fonction;
			this.fonction.addListener(this);
			fonctionChanged();
			return(true);
		}
		else{
			boolean ret=this.fonction.setSubFunction(fonction);
			this.fonction.depth=this.depth+1;
			return(ret);
		}
	}
	
	public void fonctionChanged(){
		values=null;
		//gradients=null;
		//secondDerivatives=null;
		if (fonction!=null){
			dimIndices=fonction.dimIndices;
		}
		for(Fonction f:listeners){
			f.fonctionChanged();
		}
	}
	
	public void setSamples(ArrayList<HashMap<Integer,Double>> samples){
		if (fonction!=null){
			fonction.setSamples(samples);
		}
		
	}
	public Parameters getParams(){
		if (params==null){
			params=fonction.getParams();
		}
		return(params);
	}
	
	@Override
	public Fonction copy(){
		try{
			FonctionWithSub nf=this.getClass().newInstance();
			nf.setThings(this);
			if (fonction!=null){
				nf.setSubFunction(fonction.copy());
				//nf.fonction.addListener(nf);
			}
			return(nf);
		}
		catch(Exception e){
			System.out.println(e);
		}
		return(null);
	}
	public Fonction getReverseParamsSamplesFonction(){
		try{
			//System.out.println("reverse "+this.getClass());
			FonctionWithSub nf=this.getClass().newInstance();
			//System.out.println(nf);
			nf.setThings(this);
			if (fonction!=null){
				nf.setSubFunction(fonction.getReverseParamsSamplesFonction());
				//nf.fonction.addListener(nf);
			}
			return(nf);
		}
		catch(Exception e){
			System.out.println(e);
		}
		return(null);
	}

}
