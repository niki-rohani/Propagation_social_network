package experiments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import propagationModels.PropagationStruct;
import cascades.Cascade;
import java.util.HashSet;
public class ErrorProba extends EvalMeasure {
	private boolean ignoreInit;
	private HashSet<String> allUsers;
	public ErrorProba(HashSet<String> allUsers,boolean ignoreInit){
		this.ignoreInit=ignoreInit;
		this.allUsers=allUsers;
	}
	public ErrorProba(HashSet<String> allUsers){
		this(allUsers,false);
	}
	@Override
	public String getName() {
		return "ErrorProba"+((ignoreInit)?"_ignoreInit":"");
	}
	
	
	public double getScoreForIt(HashMap<String,Double> hconta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		
		double error=0;
		
		for(String u:allUsers){
			if ((!ignoreInit) || (!init.containsKey(u))){
				Double v=hconta.get(u);
				v=(v==null)?0.0:v;
				if (href.containsKey(u)){
					error+=(1-v)*(1-v);
				}
				else{
					error+=v*v;
				}
			}
		}
		if(allUsers.size()>0){
			error/=allUsers.size();
			error*=10000;
		}
		
		return(error);
	}
	
	@Override
	public Result eval(Hyp hyp) {
		Cascade c=hyp.getStruct().getCascade();
		TreeMap<Long,HashMap<String,Double>> init=hyp.getInit(); //pstruct.getInitContaminated();
		TreeMap<Long,HashMap<String,Double>> ref=hyp.getRef(); //pstruct.getContaminated();
		ArrayList<TreeMap<Long,HashMap<String,Double>>> contaminations=hyp.getContaminations();
		
		int nb=contaminations.size();
		HashMap<String,Double> sumContaminated=new HashMap<String,Double>();
		for(TreeMap<Long,HashMap<String,Double>> conta:contaminations){
			HashMap<String,Double> hconta=PropagationStruct.getPBeforeT(conta);
			for(String u:hconta.keySet()){
				Double n=sumContaminated.get(u);
				double v=hconta.get(u)/nb;
				sumContaminated.put(u,(n==null)?v:(n+v));
			}
		}
		
		Result res=new Result("errorProba","Cascade_"+c.getID());
		res.addScore(this.getName(), getScoreForIt(sumContaminated,ref,init));
		return res;
	}

	

}
