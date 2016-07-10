package experiments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import propagationModels.PropagationStruct;
import cascades.Cascade;

public class FMeasure extends EvalMeasure {
	private double lambda;
	private boolean ignoreInit;
	public FMeasure(boolean ignoreInit, double lambda){
		this.ignoreInit=ignoreInit;
		this.lambda=lambda;
	}
	public FMeasure(boolean ignoreInit){
		this(ignoreInit, 1); 
	}
	public FMeasure(){
		this(true, 1); 
	}
	
	@Override
	public String getName() {
		return "F"+lambda+"-Measure"+((ignoreInit)?"_ignoreInit":"");
	}

	@Override
	public Result eval(Hyp hyp) {
			Cascade c=hyp.getStruct().getCascade();
			TreeMap<Long,HashMap<String,Double>> init=hyp.getInit(); //pstruct.getInitContaminated();
			TreeMap<Long,HashMap<String,Double>> ref=hyp.getRef(); //pstruct.getContaminated();
			ArrayList<TreeMap<Long,HashMap<String,Double>>> contaminations=hyp.getContaminations();
			
			Precision prec=new Precision(ignoreInit,true);
			Recall rec=new Recall(ignoreInit);
			
			int nb=contaminations.size();
			HashMap<String,Double> sumContaminated=new HashMap<String,Double>();
			double sprec=0.0;
			double srec=0.0;
			double sf=0.0;
			int i=0;
			for(TreeMap<Long,HashMap<String,Double>> conta:contaminations){
				HashMap<String,Double> hconta=PropagationStruct.getPBeforeT(conta);
				double p=prec.getScoreForIt(hconta,ref,init);
				double r=rec.getScoreForIt(hconta,ref,init);
				double f=0.0;
				if((r*p)>0){f=((1.0+lambda*lambda)*p*r)/((lambda*lambda*p)+r);}
				sprec+=p;
				srec+=r;
				sf+=f;
				i++;
			}
			Result res=new Result("fmeasure","Cascade_"+c.getID());
			
			res.addScore(prec.getName(), sprec/(i*1.0));
			res.addScore(rec.getName(), srec/(i*1.0));
			res.addScore(this.getName(), sf/(i*1.0));
			return res;
	}
	

}
