
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
public class MeanRank extends EvalMeasure {
	protected  LinkedHashSet<String> allUsers;
	boolean ignoreInit;
	
	public MeanRank(HashSet<String> allUsers){
		this(allUsers, true);
	}
	
		
	public MeanRank(HashSet<String> allUsers, boolean ignoreInit){
		this.ignoreInit=ignoreInit;
		this.allUsers=new  LinkedHashSet<String>(allUsers);
	}
	
	@Override
	public String getName() {
		return("MeanRank_"+((ignoreInit)?"_ignoreInit":""));
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
		double sum=0.0;
		//System.out.print("Rels : ");
		int i=1;
		for(String u:list){
			//System.out.println(u+"="+nbContaminated.get(u));
			if ((!firsts.containsKey(u)) && ((!ignoreInit) || (!init.containsKey(u)))){
				nb++;
				//System.out.println(u);
				if (href.containsKey(u)){
					sum+=i;
					//System.out.println("ok");
				}
				i++;
			}
			
		}
		if(nbR>0){
			sum/=nbR;
		}
		res.addScore(getName(),sum);
		
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
