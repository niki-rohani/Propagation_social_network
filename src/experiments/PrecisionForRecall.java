package experiments;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeMap;

import propagationModels.PropagationModel;
import propagationModels.PropagationStruct;
import cascades.Cascade;

import java.util.ArrayList;
import java.util.Comparator;

import core.User;
public class PrecisionForRecall extends EvalMeasure {
	protected  LinkedHashSet<String> allUsers;
	int nblevels; // nb of levels of recall to consider
	boolean ignoreInit;
	
	public PrecisionForRecall(HashSet<String> allUsers){
		this(allUsers,10);
	}
	
	public PrecisionForRecall(HashSet<String> allUsers,int nblevels){
		this(allUsers,nblevels, true);
	}
	
	public PrecisionForRecall(HashSet<String> allUsers,int nblevels, boolean ignoreInit){
		this.nblevels=nblevels;
		this.ignoreInit=ignoreInit;
		this.allUsers=new  LinkedHashSet<String>(allUsers);
	}
	
	@Override
	public String getName() {
		return("PrecisionForRecall_nbLevels="+nblevels+((ignoreInit)?"_ignoreInit":""));
	}

	@Override
	public Result eval(Hyp hyp) {
		Cascade c=hyp.getStruct().getCascade();
		TreeMap<Long,HashMap<String,Double>> tinit=hyp.getInit(); //pstruct.getInitContaminated();
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		TreeMap<Long,HashMap<String,Double>> ref=hyp.getRef(); //pstruct.getContaminated();
		ArrayList<TreeMap<Long,HashMap<String,Double>>> contaminations=hyp.getContaminations();
		Result res=new Result(this.getName(),"Cascade_"+c.getID());
		
		
		HashMap<String,Double> sumContaminated=new HashMap<String,Double>();
		for(TreeMap<Long,HashMap<String,Double>> conta:contaminations){
			HashMap<String,Double> hconta=PropagationStruct.getPBeforeT(conta);
			for(String u:hconta.keySet()){
				Double n=sumContaminated.get(u);
				double v=hconta.get(u);
				sumContaminated.put(u,(n==null)?v:(n+v));
			}
		}
		
		ArrayList<String> list=new ArrayList<String>(sumContaminated.keySet());
		ContaminatedComparator comp=new ContaminatedComparator(sumContaminated);
		Collections.sort(list, comp);
		//Collections.shuffle(allUsers);
		for(String u:allUsers){
			if (!sumContaminated.containsKey(u)){
				list.add(u);
			}
		}
		int nb=0;
		int nbRef=0;
		double stepRec=1.0/nblevels;
		double nextRec=stepRec;
		DecimalFormat format = new DecimalFormat();
		format.setMaximumFractionDigits(3);
		format.setGroupingUsed(false);
		
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		/*for(String r:href.keySet()){
			if(!allUsers.contains(r)){
				href.remove(r);
			}
		}*/
		
		HashMap<String,Double> firsts=ref.get(ref.firstKey());
		
		int nbR=href.size();
		if (ignoreInit){
			nbR-=init.size();
		}
		else{
			nbR-=firsts.size();
		}
		
		double recAtR=0.0;
		
		for(String u:list){
			//System.out.println(u+"="+nbContaminated.get(u));
			if ((!firsts.containsKey(u)) && ((!ignoreInit) || (!init.containsKey(u)))){
				nb++;
				//System.out.println(u);
				if (href.containsKey(u)){
					nbRef++;
					//System.out.println("ok");
				}
			}
			if (nbR>0){
				double rec=(nbRef*1.0)/nbR;
				while (rec>=nextRec){
					double prec=(nbRef*1.0)/nb;
					res.addScore("PrecAtRec_"+format.format(nextRec), prec);
					nextRec+=stepRec;
					//if(nextRec>1){
					//	break;
					//}
				}
				if(nb==nbRef){
					recAtR=rec;
				}
			}
		}
		if (nbR>0){
			res.addScore("RecallAtR", recAtR);
		}
		return res;
	}
	
	
	private class ContaminatedComparator implements Comparator<String>{
		 HashMap<String,Double> nbc;
		 public ContaminatedComparator(HashMap<String,Double> nbc){
			 this.nbc=nbc;
		 }
		 public int compare(String x, String y) {
	         Double nx=nbc.get(x);   
	         Double ny=nbc.get(y);
	         double vx=(nx==null)?0.0:nx;
	         double vy=(ny==null)?0.0:ny;
	         if(vy>vx){
	        	 return(1);
	         }
	         else{
	        	 if(vx>vy){
		        	 return(-1);
		         }
	        	 else{
	        		 return(0);
	        	 }
	         }
	     }
	}

}
