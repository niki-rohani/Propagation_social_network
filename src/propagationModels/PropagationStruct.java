package propagationModels;

import cascades.Cascade;
import java.util.LinkedHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import cascades.Step;
import java.util.HashMap;

import core.Post;
import core.User;
import core.Structure;
import java.util.HashSet;
public class PropagationStruct implements Structure {
	private Cascade cascade;
	private long step;
	private long nbInitSteps;
	private TreeMap<Long,HashMap<String,Double>> initialy_contaminated=null;
	//private HashMap<String,Long> finally_contaminated=null;
	private TreeMap<Long,HashMap<String,Double>> infections=null; // pour chaque t, p(user infecte au temps t)
	//private TreeMap<Long,HashMap<String,Double>> cumulInfections=null; // pour chaque t, p(user infecte avant temps t)
	
	private TreeMap<Integer,Double> diffusion=null; // poids de la diffusion (contenu diffuse par exemple)
	private HashMap<String,Long> times=null;
	//private ArrayList<HashSet<String>> steps=null; // sets of users contamined in a same step
	//private ArrayList<Long> isteps=null;
	private HashMap<String,Double> hashContamined=null;
	private HashMap<String,Double> hashInit=null;
	private ArrayList<String> arrayContamined=null;
	private ArrayList<String> arrayInit=null;
	private LinkedHashSet<String> possibleUsers=null;
	private double ratioInits=0;
	private int nbMaxInits=0;
	
	public PropagationStruct(Cascade c, long step, double ratioInit){
		this(c,step,ratioInit,-1);
	}
	
	public PropagationStruct(Cascade c, long step, double ratioInits, int nbMaxInits){
		//System.out.println("new PropagationStruct "+c.getID());
		cascade=c;
		this.step=step;
		this.ratioInits=ratioInits;
		this.nbInitSteps=1;
		this.nbMaxInits=nbMaxInits;
		getInfections();
		int nb=0;
		Integer nbs=0;
		TreeMap<Long,Integer> nn=new TreeMap<Long,Integer>();
		for(Long t:infections.keySet()){
			HashMap<String,Double> s=infections.get(t);
			nb+=s.size();
			nn.put(t, nb);
		}
		initialy_contaminated=new TreeMap<Long,HashMap<String,Double>>();
		if(nb>0){
			double r=0.0;
			for(Long t:nn.keySet()){
				nbs=nn.get(t);
				r=(nbs*1.0)/nb;
				initialy_contaminated.put(t,infections.get(t));
				nbInitSteps=t;
				if(r>=ratioInits){
					break;
				}
				if((nbMaxInits>0) && (nbs>=nbMaxInits)){
					break;
				}
			}
		}
		//System.out.println("new PropagationStruct "+c.getID()+" ok");
	}
	public PropagationStruct(Cascade c, long step, int nbInitSteps){
		cascade=c;
		this.step=step;
		this.nbInitSteps=nbInitSteps;
	}
	/*public PropagationStruct(HashMap<String,Long> init, ArrayList<HashMap<String,Double>> infections){
		this.initialy_contaminated=init;
		this.infections=infections;
	}*/
	
	public PropagationStruct(Cascade c, TreeMap<Long,HashMap<String,Double>> init, TreeMap<Long,HashMap<String,Double>> infections){
		this(c,1,1,init,infections,null); //new HashMap<Integer,Double>());
	}
	
	public PropagationStruct(Cascade c, TreeMap<Long,HashMap<String,Double>> init, TreeMap<Long,HashMap<String,Double>> infections, TreeMap<Integer,Double> diffusion){
		this(c,1,1,init,infections,diffusion);
	}
	
	public PropagationStruct(Cascade c, long step, int nbInitSteps, TreeMap<Long,HashMap<String,Double>> init, TreeMap<Long,HashMap<String,Double>> infections, TreeMap<Integer,Double> diffusion){
		this(c,step,nbInitSteps);
		this.initialy_contaminated=init;
		this.infections=infections;
		this.diffusion=diffusion;
	}
	
	public void setPossibleUsers(LinkedHashSet<String> possibles){
		possibleUsers=possibles;
	}
	
	public LinkedHashSet<String> getPossibleUsers(){
		return possibleUsers;
		/*if(possibleUsers==null){
			
		}*/
	}
	
	public Cascade getCascade(){
		return(cascade);
	}
	
	public long getStep(){
		return(step);
	}
	public long getNbInitSteps(){
		return(nbInitSteps);
	}
	
	
	// retourne une table id => pois du contenu de la diffusion
	// Attention : id du stem -1  (pour commencer a 0) et poids normalises
	public TreeMap<Integer,Double> getDiffusion(){
			if (this.diffusion==null){
				if(cascade==null){
					return(null);
				}
				HashMap<Integer,Double> w=cascade.getContentWeigths(step, nbInitSteps);
				//System.out.println(w.size()+" mots");
				diffusion=new TreeMap<Integer,Double>();
				double sum=0;
				for(Integer v:w.keySet()){
					double d=w.get(v);
					sum+=d; //*d;
					
				}
				if(sum>0){
					//sum=Math.sqrt(sum);
				}
				for(Integer v:w.keySet()){
					double d=w.get(v);
					diffusion.put(v-1, (double)(d/sum));
				}
			}
			return(diffusion);
	}
	
	// retourne une table user => time step contamination
	public TreeMap<Long,HashMap<String,Double>> getInitContaminated(){
		// nbInitSteps=this.getInfections().size(); => a retenter pour multipleIC (pas les bons likes au depart)
		if (this.initialy_contaminated==null){
			initialy_contaminated=new TreeMap<Long,HashMap<String,Double>>();
			HashMap<String,Double> ihc=new HashMap<String,Double>();
            HashMap<User,Long> hc=cascade.getUserContaminationsSteps(-1,step);
            for(User u:hc.keySet()){
            	Long t=hc.get(u);
                if (t<=nbInitSteps){
                	ihc=initialy_contaminated.get(t);
                	if(ihc==null){
                		ihc=new HashMap<String,Double>();
                		initialy_contaminated.put(t,ihc);
                	}
                	ihc.put(u.getName(), 1.0);
                }
            }
            
		}
		return(initialy_contaminated);
	}
	
	public void setInfections(TreeMap<Long,HashMap<String,Double>> inf){
		this.infections=inf;
	}
	
	public HashMap<String,Long> getInfectionTimes(){
		if(times==null){
			times=new HashMap<String,Long>(); //cascade.getContaminationsSteps(-1,step);
			TreeMap<Long,HashMap<String,Double>> inf=getInfections();
			for(Long l:inf.keySet()){
				HashMap<String,Double> h=inf.get(l);
				for(String s:h.keySet()){
					times.put(s,l);
				}
			}
		}
		return times;
	}
	
	
	
	public HashMap<String,Double> getHashContamined(){
		if(hashContamined==null){
			hashContamined=getPBeforeT(getInfections());
		}
		return hashContamined;
	}
	
	public HashMap<String,Double> getHashInit(){
		if(hashInit==null){
			hashInit=getPBeforeT(getInitContaminated());
		}
		return hashInit;
	}
	public ArrayList<String> getArrayInit(){
		if(arrayInit==null){
			arrayInit=new ArrayList<String>(getPBeforeT(getInitContaminated()).keySet());
		}
		return arrayInit;
	}
	public ArrayList<String> getArrayContamined(){
		if(arrayContamined==null){
			arrayContamined=new ArrayList<String>(getPBeforeT(getInfections()).keySet());
		}
		return arrayContamined;
	}
	
	// without empty
	/*public ArrayList<HashSet<String>> getInfectionSteps(){
		if(steps==null){
			steps=new ArrayList<HashSet<String>>();
			HashMap<Long,HashSet<String>> times= new HashMap<Long,HashSet<String>>(); 
			HashMap<User,Long> hc=cascade.getUserContaminationsSteps(-1,step);
            long max=0;
			for(User u:hc.keySet()){
                Long t=hc.get(u);
                HashSet<String> h=times.get(t);
                if(h==null){
                	h=new HashSet<String>();
                	times.put(t, h);
                }
                h.add(u.getName());
                if(t>max){max=t;}
            }
			ArrayList<Long> atimes=new ArrayList<Long>(times.keySet());
			Collections.sort(atimes);
			for(Long t:atimes){
				steps.add(times.get(t));
			}
		}
		return steps;
	}*/
	
	public TreeMap<Long,HashMap<String,Double>> getInfections(){
		if (this.infections==null){
			infections=new TreeMap<Long,HashMap<String,Double>>();
			HashMap<Long,HashSet<String>> times= new HashMap<Long,HashSet<String>>(); 
			HashMap<User,Long> hc=cascade.getUserContaminationsSteps(-1,step);
            long max=0;
			for(User u:hc.keySet()){
                Long t=hc.get(u);
                HashMap<String,Double> h=infections.get(t);
                if(h==null){
                	h=new HashMap<String,Double>();
                	infections.put(t, h);
                }
                h.put(u.getName(),1.0);
                //if(t>max){max=t;}
            }
			/*HashSet<String> infected=new HashSet<String>();
			for(Long x:times.keySet()){
				HashSet<String> inf=times.get(x);
				inf=(inf==null)?new HashSet<String>():inf;
				HashMap<String,Double> ihc=new HashMap<String,Double>();
				infections.put(x,ihc);
				for(String u:infected){
					ihc.put(u,1.0);
				}
				for(String u:inf){
					infected.add(u);
					ihc.put(u,1.0);
				}
			}*/
              
		}
		//System.out.println(infections);
		return(infections);
	}
	
	/*public TreeMap<Long,HashMap<String,Double>> getCumulInfections(){
		if (this.cumulInfections==null){
			this.cumulInfections=new TreeMap<Long,HashMap<String,Double>>();
			getInfections();
			for(Long t:infections.keySet()){
				cumulInfections.put(t, getPBeforeT(infections,t));
			}
		}
		return cumulInfections;
	}*/
	public static LinkedHashMap<String,Double> getPBeforeT(TreeMap<Long,HashMap<String,Double>> conta){
		return(getPBeforeT(conta,(long)-1));
	}
	
	/**
	 * Computes a map of users with probability of being infected before or at a given time according to a table of infections probabilities indexed by time steps.  
	 * @param conta
	 * @param atTime
	 * @return
	 */
	public static LinkedHashMap<String,Double> getPBeforeT(TreeMap<Long,HashMap<String,Double>> conta,Long atTime){
		LinkedHashMap<String,Double> ret=new LinkedHashMap<String,Double>();
		for(Long t:conta.keySet()){
			if((atTime>=0) && (t>atTime)){
				break;
			}
			HashMap<String,Double> h=conta.get(t);
			for(String u:h.keySet()){
				Double d=ret.get(u);
				d=(d==null)?0.0:d;
				ret.put(u, d+h.get(u));
			}
		}
		
		return(ret);
	}
	
	public static TreeMap<Long,LinkedHashMap<String,Double>> getPCumulBeforeT(TreeMap<Long,HashMap<String,Double>> conta){
		return(getPCumulBeforeT(conta,(long)-1));
	}
	
	/**
	* @param conta
	 * @param atTime
	 * @return
	 */
	public static TreeMap<Long,LinkedHashMap<String,Double>> getPCumulBeforeT(TreeMap<Long,HashMap<String,Double>> conta,Long atTime){
		TreeMap<Long,LinkedHashMap<String,Double>> ret=new TreeMap<Long,LinkedHashMap<String,Double>>();
		HashMap<String,Double> last=new HashMap<String,Double>();
		for(Long t:conta.keySet()){
			if((atTime>=0) && (t>atTime)){
				break;
			}
			LinkedHashMap<String,Double> step=new LinkedHashMap<String,Double>();
			ret.put(t, step);
			
			HashMap<String,Double> h=conta.get(t);
			for(String u:h.keySet()){
				Double d=last.get(u);
				d=(d==null)?0.0:d;
				step.put(u, d+h.get(u));
			}
			for(String u:last.keySet()){
				if(!step.containsKey(u)){
					step.put(u, last.get(u));
				}
			}
			last=step;
		}
		
		return(ret);
	}
	public static TreeMap<Long,ArrayList<String>> getListBeforeT(TreeMap<Long,HashMap<String,Double>> conta){
		return(getListBeforeT(conta,(long)-1));
	}
	
	/**
	* @param conta
	 * @param atTime
	 * @return
	 */
	public static TreeMap<Long,ArrayList<String>> getListBeforeT(TreeMap<Long,HashMap<String,Double>> conta,Long atTime){
		TreeMap<Long,ArrayList<String>> ret=new TreeMap<Long,ArrayList<String>>();
		HashSet<String> vus=new HashSet<String>();
		for(Long t:conta.keySet()){
			if((atTime>=0) && (t>atTime)){
				break;
			}
			vus.addAll(conta.get(t).keySet());
			ArrayList<String> step=new ArrayList<String>(vus);
			ret.put(t, step);
		}
		
		return(ret);
	}
	
	
}
