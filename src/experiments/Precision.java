package experiments;

import java.util.ArrayList;
import java.util.TreeMap;

import core.User;

import java.util.HashMap;

import propagationModels.PropagationStruct;
import cascades.Cascade;
public class Precision extends EvalMeasure {
	private boolean ignoreInit;
	private boolean zeroIfNoDiffusion;
	//private double pmin; //proba min to consider a node as infected
	public Precision(){
		this(false,false);
	}
	/*public Precision(boolean ignoreInit){
		this(ignoreInit,false);
	}*/
	/*public Precision(boolean ignoreInit,boolean zeroIfNoDiffusion){
		this(ignoreInit,zeroIfNoDiffusion,0.5);
	}*/
	public Precision(boolean ignoreInit,boolean zeroIfNoDiffusion){ //,double pmin){
		//this.pmin=pmin;
		this.ignoreInit=ignoreInit;
		this.zeroIfNoDiffusion=zeroIfNoDiffusion;
	}
	
	
	@Override
	public String getName() {
		return "Precision"+((ignoreInit)?"_ignoreInit":"")+((zeroIfNoDiffusion)?"_zeroIfNoDiffusion":"");
	}

	
	public double getScoreForIt(TreeMap<Long,HashMap<String,Double>> conta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> hconta=PropagationStruct.getPBeforeT(conta);
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		HashMap<String,Double> firsts=ref.get(ref.firstKey());
		double nb=0;
		double nbRef=0;
		for(String u:hconta.keySet()){
			if ((!firsts.containsKey(u)) && ((!ignoreInit) || (!init.containsKey(u)))){
				nb+=hconta.get(u);
				//if(hconta.get(u)>=0.5){
				//	nb++;
					//System.out.println(u);
					if (href.containsKey(u)){
						nbRef+=hconta.get(u);
						//System.out.println("ok");
					}
				//}
			}
		}
		double prec=1;
		
		if (nb>0){
			prec=(1.0*nbRef)/nb;
		}
		else{
			if (this.zeroIfNoDiffusion){
				if(href.size()>init.size()){
					prec=0;
				}
			}
		}
		return(prec);
	}
	public double getScoreForIt(HashMap<String,Double> hconta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		HashMap<String,Double> firsts=ref.get(ref.firstKey());
		double nb=0;
		double nbRef=0;
		for(String u:hconta.keySet()){
			if ((!firsts.containsKey(u)) && ((!ignoreInit) || (!init.containsKey(u)))){
				if(hconta.get(u)>=0.01){
					nb+=hconta.get(u);
					//if(hconta.get(u)>=0.5){
					//	nb++;
						//System.out.println(u);
						if (href.containsKey(u)){
							nbRef+=hconta.get(u);
							//System.out.println("ok");
						}
					//}
				}
			}
		}
		double prec=1;
		
		if (nb>0){
			prec=(1.0*nbRef)/nb;
		}
		else{
			if (this.zeroIfNoDiffusion){
				if(href.size()>init.size()){
					prec=0;
				}
			}
		}
		return(prec);
	}
	
	public double getScoreForItInv(HashMap<String,Double> hconta, TreeMap<Long,HashMap<String,Double>> ref,TreeMap<Long,HashMap<String,Double>> tinit){
		HashMap<String,Double> href=PropagationStruct.getPBeforeT(ref);
		HashMap<String,Double> init=PropagationStruct.getPBeforeT(tinit);
		HashMap<String,Double> firsts=ref.get(ref.firstKey());
		double nb=0;
		double nbRef=0;
		for(String u:hconta.keySet()){
			if ((!firsts.containsKey(u)) && ((!ignoreInit) || (!init.containsKey(u)))){
				nb+=(1.0-hconta.get(u));
				//if(hconta.get(u)>=0.5){
				//	nb++;
					//System.out.println(u);
					if (!href.containsKey(u)){
						nbRef+=(1.0-hconta.get(u));
						//System.out.println("ok");
					}
				//}
			}
		}
		double prec=1.0;
		
		if (nb>0){
			prec=(1.0*nbRef)/nb;
		}
		else{
			if (this.zeroIfNoDiffusion){
				if(href.size()>init.size()){
					prec=0;
				}
			}
		}
		return(prec);
	}
	
	@Override
	public Result eval(Hyp hyp) {
		Cascade c=hyp.getStruct().getCascade();
		TreeMap<Long,HashMap<String,Double>> init=hyp.getInit(); //pstruct.getInitContaminated();
		TreeMap<Long,HashMap<String,Double>> ref=hyp.getRef(); //pstruct.getContaminated();
		ArrayList<TreeMap<Long,HashMap<String,Double>>> contaminations=hyp.getContaminations();
		
		/*double sum=0.0;
		int n=0;
		for(TreeMap<Long,HashMap<String,Double>> conta:contaminations){
			sum+=getScoreForIt(conta,ref,init);
			n++;
		}
		if (n>0){
			sum/=n;
		}*/
		
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
		Result res=new Result("precision","Cascade_"+c.getID());
		res.addScore(this.getName(), getScoreForIt(sumContaminated,ref,init));
		res.addScore("precision_inverse", getScoreForItInv(sumContaminated,ref,init));
		return res;
	}

}
