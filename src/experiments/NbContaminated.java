package experiments;

import java.util.HashMap;
import java.util.TreeMap;

import propagationModels.PropagationStruct;
import cascades.Cascade;
import java.util.ArrayList;
import core.User;
public class NbContaminated extends EvalMeasure {

	//private double pmin=0.5; // proba min to consider a node as infected
	
	@Override
	public String getName() {
		return "NbContaminated";
	}

	@Override
	public Result eval(Hyp hyp) {
		//PropagationStruct pstruct=hyp.getStruct();
		Cascade c=hyp.getStruct().getCascade();
		HashMap<String,Double> initialy_contaminated=(PropagationStruct.getPBeforeT(hyp.getInit())); //pstruct.getInitContaminated();
		TreeMap<Long,HashMap<String,Double>> refContaminated=hyp.getRef(); //pstruct.getContaminated();
		ArrayList<TreeMap<Long,HashMap<String,Double>>> contaminations=hyp.getContaminations();
		
		Result res=new Result("NbContaminated","Cascade_"+c.getID());
		res.addScore("NbInitContaminated", initialy_contaminated.size());
		res.addScore("NbRefContaminated", (PropagationStruct.getPBeforeT(refContaminated)).size());
		
		double s=0;
		int n=0;
		for(TreeMap<Long,HashMap<String,Double>> conta:contaminations){
			HashMap<String,Double> fconta=PropagationStruct.getPBeforeT(conta);
			//System.out.println(fconta);
			
			double cv=0;
			for(Double v:fconta.values()){
				//if(v>=pmin){
					cv+=v;
				//}
			}
			s+=cv;
			n++;
		}
		double nb=0.0;
		if (n>0){
			nb=(s*1.0)/n;
		}
	
		res.addScore("NbHypContaminated", nb);
		
		return res;
	}

}
