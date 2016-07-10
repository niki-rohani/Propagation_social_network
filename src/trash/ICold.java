package trash;

import java.io.BufferedReader;

import cascades.CascadesLoader;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;



//import trash.ArtificialCascadesLoader;
import utils.Keyboard;
import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import statistics.ErrorFunction;
import core.Link;
import core.Post;
import core.Structure;
import core.User;
import cascades.Cascade ;
import cascades.CascadesProducer;
import cascades.IteratorDBCascade;

import java.util.TreeMap;

import propagationModels.PropagationModel;
import propagationModels.PropagationStruct;
public class ICold implements PropagationModel {
   
	private long maxIter=-2;  // -2 => can be modified by loading
	private String modelFile="";
	private boolean loaded=false;
	private int inferMode=0;
	private int contaMaxDelay=1;
	double smoothing=0.00000001;
	int nbGibbsIt=10;
	int nbBurnOut=1;
    int nbPos=25;
    //private HashMap<String,HashMap<String,Double>> pij;
    
	public ICold(int contaMaxDelay){
		this(contaMaxDelay,1);
	}
	public ICold(int contaMaxDelay,int inferMode){
		this.contaMaxDelay=contaMaxDelay;
		this.inferMode=inferMode;
	}
	
/*	public IC(String modelFile, long maxIter){
		this(modelFile,1,maxIter);
	}*/
	
	public ICold(String modelFile, int inferMode){
		this.modelFile=modelFile;
		this.inferMode=inferMode;
		
	}
	
	
	public ICold(String modelFile, int inferMode, long maxIter){
		this.modelFile=modelFile;
		this.inferMode=inferMode;
		this.maxIter=maxIter;
	}
	
	public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(User.users.keySet());
	}
	public int getContentNbDims(){
		return 0;
	}
	
	
	public String toString(){
		String sm=modelFile.replaceAll("/", "/");
		return("ICmodel_"+contaMaxDelay+"_inferMode-"+inferMode+"_maxIter-"+maxIter+"_"+sm);
	}
	public String getName(){
		String sm=modelFile.replaceAll("/", "__");
		return sm;
	}
	public int infer(Structure struct) {
		//if(inferMode==0) return(inferSimulation(struct));
		//if(inferMode<=1) 
		if(inferMode<=2){
			return(inferSimulationProbas(struct));
		}
		//else{return(inferOneStepFromRef(struct));}
		//if(inferMode==2) return(inferProbasGibbs(struct));
		return -1;
	}
	
	
	public int inferSimulation(Structure struct) {
		inferMode=0;
		return inferSimulationProbas(struct);
	}
	
	/*public int inferEachStepFromRef(Structure struct){
		if (!loaded){
	    	load();
	    }
		PropagationStruct pstruct = (PropagationStruct)struct ;
		TreeMap<Long,HashMap<String,Double>> ref=pstruct.getInfections();
	    TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
	    TreeMap<Long,ArrayList<String>> cumul=PropagationStruct.getListBeforeT(infections);
	    
	    
	    for(Long time:ref.keySet()){
	    //boolean noMax=true;
	    while(ok){
	    	if(it>=maxIter){
	    		break;
	    	}
	    	//currentT++;
	    	for(String contagiousU : contagious.keySet()) {
	    		User contagiousUser=User.getUser(contagiousU);
	            Long time=contagious.get(contagiousU);
	            HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
	            
	            long tpos=horizon;
	            if((time+horizon)>maxIter){
	            	tpos=maxIter-time;
	            }
	            long minT=time+1;
	            if(time<(firstNewT-1)){
	            	tpos-=(firstNewT-time+1);
	            	minT=firstNewT;
	            }
	            
	            if(tpos<0){
	            	continue;
	            }
	            for(Link lsuc : succs.values()){ //get(contagiousUser.getID()).keySet()) {
	                User neighbour=(User)lsuc.getNode2();
	                if(infectedBefore.contains(neighbour.getName()))
	                    continue ;
	                //System.out.println(contagiousU+" => "+neighbour.getName()+" = "+lsuc.getVal());
	                double val=lsuc.getVal();
		 
	                if(Math.random()<val) {
	                	long ti=((long)(Math.random()*tpos))+minT;
	                	//System.out.println(neighbour.getName()+"=>"+ti);
	                    
	                    Long oldT=times.get(neighbour.getName());
	                    if((oldT!=null) && (ti<oldT)){
	                    	//if(this.inferMode!=2){
	                    		HashMap<String,Double> infectedStep=infections.get(oldT);
	                    		infectedStep.remove(neighbour.getName());
	                    		if(infectedStep.size()==0){
	                    			infections.remove(oldT);
	                    			cTimes.remove(oldT);
	                    		}
	                    	//}
	                    		
	                    		
	                    }
	                    
	                    if((oldT==null) || (ti<oldT)){
	                    	//if(this.inferMode!=2){
	                    		HashMap<String,Double> infectedStep=infections.get(ti);
	                    		if(infectedStep==null){
	                    			infectedStep=new HashMap<String,Double>();
	                    			infections.put(ti, infectedStep);
	                    		}
	                    		infectedStep.put(neighbour.getName(), 0.999);
	                    		
	                    	//}
	                    	cTimes.add(ti);
	                		times.put(neighbour.getName(),ti);
	                		
	                    }
	                }	
	            }
	        }
	    	if(cTimes.size()==0){
	    		//System.out.println("maxT="+it);
	    		break;
	    	}
	    	Long time=cTimes.first();
	    	if(time>=maxIter){
	    		it=time;
	    		break;
	    	}
	    	//System.out.println("time of new contagious = "+time);
	    	//System.out.println(times);
	    	HashMap<String,Double> infectedStep=infections.get(time);
	    	cTimes.remove(time);
	    	contagious = new HashMap<String,Long>();
	    	for(String user:infectedStep.keySet()){
	    		contagious.put(user, time);
	    		infectedBefore.add(user);
	    	}
	    	
			
	    	
	        if(contagious.isEmpty())
	            break ; 
	        
	       
	        it=time;
	    	
	    }
	    //System.out.println(times);
	    it=maxIter+1;
	    if(this.inferMode>=1){
	    	if(this.inferMode==2){
	    		infectedBefore=new HashSet<String>();
	    		cTimes=new TreeSet<Long>(infections.keySet());
	    		for(Long t:cTimes){
	    			if(t>=firstNewT){
	    				infections.remove(t);
	    			}
	    			else{
	    				infectedBefore.addAll(infections.get(t).keySet());
	    			}
	    		}
	    	}
	        HashMap<String,Double> notYet=new HashMap<String,Double>();
	        
	        for(String user:User.users.keySet()){
	        	if(((this.inferMode==2) && (!infectedBefore.contains(user)))|| (!times.containsKey(user))){
	        		notYet.put(user,1.0);
	        	}
	        }
	         
	        for(String user : times.keySet()) {
	            Long time=times.get(user);
	            if((time+horizon)<it){
	            	continue;
	            }
	            User contagiousUser=User.getUser(user); 
	    		HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
	            //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
	           
	            for(Link lsuc : succs.values()){ //get(contagiousUser.getID()).keySet()) {
	                User neighbour=(User)lsuc.getNode2();
	                String v=neighbour.getName();
	                if(!notYet.containsKey(v))
	                    continue ;
	                if(v.equals(user)){
	                	continue;
	                }
	                Double p=notYet.get(v);
	                p*=(1.0-lsuc.getVal());
	                notYet.put(v,p);
	            }
	        }
	        for(String user:notYet.keySet()){
	        	double p=1.0-notYet.get(user);
	        	infectedstep.put(user,p*0.998+0.001);
	        	//System.out.println(user + " : "+(1.0-notYet.get(user)));
	        }
	        
	       
	        infections.put(it,infectedstep);
	    }
	    //infections.add(infectedstep);
	    //System.out.println(PropagationStruct.getPBeforeT(infections));
	    pstruct.setInfections(infections) ;
	    return 0;
	}*/
	
	
	
	public int inferProbasGibbs(Structure struct) {
        if (!loaded){
        	System.out.println("Load model "+modelFile);
        	load();
        	
        }
        long horizon=this.contaMaxDelay;
        if(horizon<0){
        	horizon=this.maxIter;
        }
        
        
        PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashSet<String> infected = new HashSet<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        HashMap<User,Long> times=new HashMap<User,Long>();
        HashMap<User,Double> pYes=new HashMap<User,Double>();
	    HashMap<User,Double> pNo=new HashMap<User,Double>();
	    HashMap<User,HashSet<User>> exposed=new HashMap<User,HashSet<User>>();
        
	    //HashMap<Long, HashSet<User>> ctimes=new HashMap<Long, HashSet<User>>();
	    ArrayList<User> users=new ArrayList<User>(User.users.values());
	    long maxT=contaminated.lastKey(); 
	    HashSet<User> contagious=new HashSet<User>();
	    for(long t:contaminated.keySet()){
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	infections.put(t, (HashMap<String,Double>) inf.clone());
	    	for(String u:inf.keySet()){
        		
	        	User user=User.getUser(u);
	        	if(t+horizon>maxT){
	        		contagious.add(user);
	        		HashMap<String,Link> succs=user.getSuccesseurs();
	        		for(Link l:succs.values()){
	        			User succ=(User)l.getNode2();
	        			if(!times.containsKey(succ.getName())){
	        				HashSet<User> from=exposed.get(succ);
	        				if(from==null){
	        					from=new HashSet<User>();
	        					exposed.put(succ, from);
	        				}
	        				from.add(user);
	        			}
	        		}
	        	}
	        	
	        	/*HashSet<User> us=ctimes.get(t);
	        	if(us==null){
	        		us=new HashSet<User>();
	        		ctimes.put(t, us);
	        	}	
	        	us.add(user);*/
	        	
	        	times.put(user, t);
	    	}
	    }
	    long firstNewT=maxT+1;
	    
	   
	    
	    //System.out.println(ctimes);
	    
	    // calcul probas pour observations
	    HashMap<User,HashMap<Long,Integer>> nbContamined=new HashMap<User,HashMap<Long,Integer>>();
	    for(User user:users){
	    	if(!infected.contains(user.getName())){
	    		evalProbas(user,times.get(user),pYes,pNo,times,horizon);
	    	}
	    	HashMap<Long,Integer> nbC=new HashMap<Long,Integer>();
	    	nbContamined.put(user, nbC);
	    	
	    }
	    
	    
	    
	    long nbT=maxIter-maxT;
	    HashMap<User,Double> tmpY=new HashMap<User,Double>();
		HashMap<User,Double> tmpN=new HashMap<User,Double>();
	    for(int it=1;it<=nbGibbsIt;it++){
	    	if(it%1==0){
	    		System.out.println(times);
	    	}
	    	//ArrayList<User> exposedList=new ArrayList<User>(exposed.keySet());
	      Collections.shuffle(users);
	      for(User user:users){
	    	    
	    	  	if((!exposed.containsKey(user)) && (!times.containsKey(user))){
		    		continue;
		    	}
		    	String suser=user.getName();
		    	Long time=times.get(user);
		    	time=(time==null)?maxIter*2:time;
		    	//HashMap<User,Link> from=exposed.get(user);
		    	if(!infected.contains(suser)){
		    		HashMap<Long,Double> pro=new HashMap<Long,Double>();
		    		HashSet<Long> pos=new HashSet<Long>();
		    		for(int i=0; i<nbPos;i++){
		    			long t=(long)(Math.random()*(nbT+1)+maxT+1);
		    			if(t>maxIter){
		    				t=maxIter*2;
		    			}
		    			pos.add(t);
		    		}
		    		//System.out.println(times);
			    	//System.out.println(pYes);
				    //System.out.println(pNo);
		    		//System.out.println(user.getName()+" pos : "+pos);
		    		HashMap<Long,Double> pFromPastY=new HashMap<Long,Double>();
		    		HashMap<Long,Double> pFromPastN=new HashMap<Long,Double>();
		    		HashMap<Long,HashMap<User,Double>> pSuccsY=new HashMap<Long,HashMap<User,Double>>();
		    		HashMap<Long,HashMap<User,Double>> pSuccsN=new HashMap<Long,HashMap<User,Double>>();
		    		double py=pYes.get(user);
		    		double pn=pNo.get(user);
		    		pFromPastY.put(time, py);
		    		pFromPastN.put(time, pn);
		    		double prob=0.0;
		    		if(time<=maxIter){
		    			prob+=Math.log(1.0-Math.exp(py))+pn;
		    		}
		    		else{
		    			prob+=pn;
		    		}
		    		HashMap<String,Link> succs=user.getSuccesseurs();
		    		HashMap<String,Link> preds=user.getPredecesseurs();
		    		
		    		HashMap<User,Double> ptY=new HashMap<User,Double>();
		    		pSuccsY.put(time,ptY);
		    		HashMap<User,Double> ptN=new HashMap<User,Double>();
		    		pSuccsN.put(time,ptN);
		    		for(Link lw:succs.values()){
		    			User w=(User)lw.getNode2();
		    			if(infected.contains(w.getName())){
		    				continue;
		    			}
			    			
		    			py=pYes.get(w);
		    			pn=pNo.get(w);
		    			ptY.put(w,py);
		    			ptN.put(w,pn);
		    			if(times.get(w)!=null){
		    				prob+=Math.log(1.0-Math.exp(py))+pn;
			    		}
			    		else{
			    			prob+=pn;
			    		}
		    		}
		    		pro.put(time, prob);
		    		double maxP=prob;
		    		
		    		for(Long t:pos){
		    			if(t==time){
		    				continue;
		    			}
		    			prob=0.0;
		    			tmpY.clear();
		    			tmpN.clear();
		    			evalProbas(user,t,tmpY,tmpN,times,horizon);
		    			//System.out.println(t+" : ");
		    			//System.out.println(tmpY);
		    			//System.out.println(tmpN);
		    			py=tmpY.get(user);
		    			pn=tmpN.get(user);
		    			pFromPastY.put(t, py);
		    			pFromPastN.put(t, pn);
		    			if(t<=maxIter){
			    			prob+=Math.log(1.0-Math.exp(py))+pn;
			    		}
			    		else{
			    			prob+=pn;
			    		}
		    		    //System.out.println("prob = "+prob);
		    			ptY=new HashMap<User,Double>();
		    			ptN=new HashMap<User,Double>();
			    		pSuccsY.put(t,ptY);
			    		pSuccsN.put(t,ptN);
		    			for(Link lw:succs.values()){
			    			User w=(User)lw.getNode2();
			    			if(infected.contains(w.getName())){
			    				continue;
			    			}
			    			double val=smooth(lw.getVal());
			    			
			    			val=Math.log(1.0-val);
			    			double pwY=pYes.get(w);
			    			double pwN=pNo.get(w);
			    			Long tw=times.get(w);
			    			tw=(tw==null)?maxIter*2:tw;
			    			if(t<=maxIter){
			    				if(time<=maxIter){
			    					if(t<time){
			    						if(tw<=maxIter){
			    						   if((tw>t) && (tw<=(t+horizon)) && (tw<=time)){
			    							  pwY+=val;
			    						   }
			    						   else if((tw>time) && (tw>(t+horizon)) && (tw<=(time+horizon))){
			    							  pwY-=val;
			    							  pwN+=val;
			    						   }
			    						}
			    					}
			    					else{
			    						if(tw<=maxIter){
				    						   if((tw>time) && (tw<=(time+horizon)) && (tw<=t)){
				    							  pwY-=val;
				    						   }
				    						   else if((tw>t) && (tw>(time+horizon)) && (tw<=(t+horizon))){
				    							  pwY+=val;
				    							  pwN-=val;
				    						   }
				    					}
			    					}
			    				}
			    				else{
			    					if(tw>(t+horizon)){
			    						pwN+=val;
			    					}
			    					else if((tw>t) && (tw<=(t+horizon))){
			    						pwY+=val;
			    					}
			    				}
			    			}
			    			else{
			    				if(tw>(time+horizon)){
		    						pwN-=val;
		    					}
		    					else if((tw>time) && (tw<=(time+horizon))){
		    						pwY-=val;
		    					}
			    				
			    			}
				    		ptY.put(w, pwY);
				    		ptN.put(w, pwN);
				    		if(tw<=maxIter){
				    			prob+=Math.log(1.0-Math.exp(pwY))+pwN;
				    		}
				    		else{
				    			prob+=pwN;
				    		}
				    		
		    			}
		    			pro.put(t, prob);
		    			if(prob>maxP){
		    				maxP=prob;
		    			}
		    		}
		    		//System.out.println(pro);
		    		//selection du nouveau temps de user
		    		double sumP=0.0;
		    		for(Long t:pro.keySet()){
		    			double p=pro.get(t);
		    			if(Double.isNaN(p)){
		    				throw new RuntimeException("Nan");
		    			}
		    			sumP+=Math.exp(p-maxP);
		    		}
		    		
		    		for(Long t:pro.keySet()){
		    			double p=pro.get(t);
		    			pro.put(t, Math.exp(p-maxP)/sumP );
		    			if(Double.isNaN(p)){
		    				throw new RuntimeException("Nan => sumP="+sumP);
		    			}
		    		}
		    		
		    		/*double r=1.0/pro.size();
		    		for(Long t:pro.keySet()){
		    			double p=pro.get(t);
		    			pro.put(t, (p+r)/2.0);
		    			
		    		}*/
		    		//System.out.println(pro);
		    		ArrayList<Long> pp=new ArrayList<Long>(pro.keySet());
		    		double x=Math.random();
		    		Long select=pp.get(0);
		    		for(int i=0;i<pp.size();i++){
		    			Long t=pp.get(i);
		    			double v=pro.get(t);
		    			
		    			x-=v;
		    			//System.out.println(x+" "+t+" "+v);
		    			if(x<0){
		    				select=pp.get(i);
		    				break;
		    			}
		    		}
		    		if(select==time){
		    			continue;
		    		}
		    		if(select<=maxIter){
		    			times.put(user, select);
		    		}
		    		else{
		    			times.remove(user);
		    		}
		    		// mise a jour structures selon temps selectionne
		    		ptY=pSuccsY.get(select);
		    		ptN=pSuccsN.get(select);
		    		for(User w:ptY.keySet()){
		    			HashSet<User> from=exposed.get(w);
		    			Long tw=times.get(w);
		    			tw=(tw==null)?maxIter*2:tw;
		    			if(select<tw){
		    				if(from==null){
		    					from=new HashSet<User>();
		    					exposed.put(w, from);
		    				}
		    				from.add(user);
		    			}
		    			else{
		    				if(from!=null){
		    					from.remove(user);
		    					if(from.size()==0){
		    						exposed.remove(w);
		    					}
		    				}
		    			}
		    			pYes.put(w, ptY.get(w));
		    			pNo.put(w, ptN.get(w));
		    		}
		    		pYes.put(user, pFromPastY.get(select));
		    		pNo.put(user, pFromPastN.get(select));
		    		HashSet<User> from=exposed.get(user);
		    		if(from==null){
		    			from=new HashSet<User>();
		    			exposed.put(user, from);
		    		}
		    		for(Link lv:preds.values()){
		    			User v=(User)lv.getNode1();
		    			Long tv=times.get(v);
		    			if((tv!=null) && (tv<select) && ((tv+horizon)>=firstNewT)){
		    				from.add(v);
		    			}
		    		}
		    		if(from.size()==0){
		    			exposed.remove(user);
		    		}
		    		
		    	}
	    	 
	      }
	      if(it>nbBurnOut){
	    	  for(User user:times.keySet()){
	    		  Long t=times.get(user);
	    		  HashMap<Long,Integer> nbC=nbContamined.get(user);
	    		  Integer nbc=nbC.get(t);
	    		  nbc=(nbc==null)?0:nbc;
	    		  nbC.put(t, nbc+1);
	    	  }
	      }
	      
	    }
	    int nbdiv=nbGibbsIt-nbBurnOut;
	    for(User user:nbContamined.keySet()){
	    	String suser=user.getName();
	    	HashMap<Long,Integer> h=nbContamined.get(user);
	    	if(h!=null){
	    		for(Long t:h.keySet()){
	    			int nb=h.get(t);
	    			HashMap<String,Double> inf=infections.get(t);
	    			if(inf==null){
	    				inf=new HashMap<String,Double>();
	    				infections.put(t, inf);
	    			}
	    			Double n=inf.get(suser);
	    			n=(n==null)?nb:(n+nb);
	    			inf.put(suser, n);
	    		}
	    	}
	    }
	    for(Long t:infections.keySet()){
	    	HashMap<String,Double> inf=infections.get(t);
	    	for(String user:inf.keySet()){
	    		Double n=inf.get(user);
				if(n==null){
					continue;
				}
				
				inf.put(user, (1.0*n/nbdiv));
	    	}
	    }
	    //System.out.println(infections);
	    pstruct.setInfections(infections);
        return(0);
	}
	
	
	public void evalProbas(User user,Long tw, HashMap<User,Double> pYes,HashMap<User,Double> pNo,HashMap<User,Long> times,long horizon){
		HashMap<String,Link> preds=user.getPredecesseurs();
		
		tw=(tw==null)?maxIter*2:tw;
		double py=Math.log(smooth(1.0)); //-0.0001);
		double pn=0;
		for(Link lv:preds.values()){
			User v=(User)lv.getNode1();
			Long tv=times.get(v);
			if((tv!=null) && (tv<tw)){
				double val=smooth(lv.getVal());
				
				if((tw<=maxIter) && (tv+horizon)>=tw){
					py+=Math.log(1.0-val);
				}
				else{
					pn+=Math.log(1.0-val);
				}
			}
		}
		pNo.put(user, pn);
		pYes.put(user, py);
	}
	
	
	
	/**
	 * On last step, we set probas of infection for non infected nodes at this step rather than a binary information from simulation. 
	 * @param struct
	 * @return
	 */
	 public int inferSimulationProbas(Structure struct) {
        if (!loaded){
        	load();
        }
    	PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> contaminated=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashMap<String,Long> contagious = new HashMap<String,Long>();
        HashMap<String,Long> times=new HashMap<String,Long>();
        HashSet<String> infectedBefore = new HashSet<String>((PropagationStruct.getPBeforeT(contaminated)).keySet()) ;
        
        long maxt=0;
        TreeSet<Long> cTimes=new TreeSet<Long>();
	    for(long t:contaminated.keySet()){
	    	HashMap<String,Double> inf=contaminated.get(t);
	    	infections.put(t, (HashMap<String,Double>) inf.clone());
	    	for(String user:inf.keySet()){
	    		contagious.put(user, t);
	    		times.put(user, t);
	    	}
	    	maxt=t;
	    	
	    	//cTimes.add(t);
	    }
	    long firstNewT=maxt+1;
        
        HashMap<String,Double> lastcontagious=(HashMap<String,Double>)contagious.clone();
        HashMap<String,Double> infectedstep=new HashMap<String,Double>();
        User currentUser ;
        //int it=tt;
        long it=firstNewT;
        long horizon=this.contaMaxDelay;
        if(horizon<0){
        	horizon=this.maxIter;
        }
        boolean ok=true;
        long currentT=firstNewT-1;
        //boolean noMax=true;
        while(ok){
        	if(it>=maxIter){
        		break;
        	}
        	//currentT++;
        	for(String contagiousU : contagious.keySet()) {
        		User contagiousUser=User.getUser(contagiousU);
                Long time=contagious.get(contagiousU);
                HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
                //System.out.println(contagiousU+" : "+succs.size());
                
                //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
                /*long maxT=horizon;
                if((time+maxT)>maxIter){
                	maxT=maxIter-time;
                }
                long minT=time+1;
                if(time<(firstNewT-1)){
                	minT=firstNewT;
                }*/
                long tpos=horizon;
                if((time+horizon)>maxIter){
                	tpos=maxIter-time;
                }
                long minT=time+1;
                if(time<(firstNewT-1)){
                	tpos-=(firstNewT-time+1);
                	minT=firstNewT;
                }
                
                if(tpos<0){
                	continue;
                }
                for(Link lsuc : succs.values()){ //get(contagiousUser.getID()).keySet()) {
                    User neighbour=(User)lsuc.getNode2();
                    if(infectedBefore.contains(neighbour.getName()))
                        continue ;
                    //System.out.println(contagiousU+" => "+neighbour.getName()+" = "+lsuc.getVal());
                    double val=lsuc.getVal();
		 
                    if(Math.random()<val) {
                    	long ti=((long)(Math.random()*tpos))+minT;
                    	//System.out.println(neighbour.getName()+"=>"+ti);
                        
                        Long oldT=times.get(neighbour.getName());
                        if((oldT!=null) && (ti<oldT)){
                        	//if(this.inferMode!=2){
                        		HashMap<String,Double> infectedStep=infections.get(oldT);
                        		infectedStep.remove(neighbour.getName());
                        		if(infectedStep.size()==0){
                        			infections.remove(oldT);
                        			cTimes.remove(oldT);
                        		}
                        	//}
                        		
                        		
                        }
                        
                        if((oldT==null) || (ti<oldT)){
                        	//if(this.inferMode!=2){
                        		HashMap<String,Double> infectedStep=infections.get(ti);
                        		if(infectedStep==null){
                        			infectedStep=new HashMap<String,Double>();
                        			infections.put(ti, infectedStep);
                        		}
                        		infectedStep.put(neighbour.getName(), 1.0);
                        		
                        	//}
                        	cTimes.add(ti);
                    		times.put(neighbour.getName(),ti);
                    		
                        }
                    }	
                }
            }
        	if(cTimes.size()==0){
        		//System.out.println("maxT="+it);
        		break;
        	}
        	Long time=cTimes.first();
        	if(time>=maxIter){
        		it=time;
        		break;
        	}
        	//System.out.println("time of new contagious = "+time);
        	//System.out.println(times);
        	HashMap<String,Double> infectedStep=infections.get(time);
        	cTimes.remove(time);
        	contagious = new HashMap<String,Long>();
        	for(String user:infectedStep.keySet()){
        		contagious.put(user, time);
        		infectedBefore.add(user);
        	}
        	
    		
        	
            if(contagious.isEmpty())
                break ; 
            
           
            it=time;
        	
        }
        //System.out.println(times);
        it=maxIter+1;
        if(this.inferMode>=1){
        	if(this.inferMode==2){
        		infectedBefore=new HashSet<String>();
        		cTimes=new TreeSet<Long>(infections.keySet());
        		for(Long t:cTimes){
        			if(t>=firstNewT){
        				infections.remove(t);
        			}
        			else{
        				infectedBefore.addAll(infections.get(t).keySet());
        			}
        		}
        	}
	        HashMap<String,Double> notYet=new HashMap<String,Double>();
	        
	        for(String user:User.users.keySet()){
	        	if(((this.inferMode==2) && (!infectedBefore.contains(user)))|| (!times.containsKey(user))){
	        		notYet.put(user,1.0);
	        	}
	        }
	         
	        for(String user : times.keySet()) {
	            Long time=times.get(user);
	            if((time+horizon)<it){
	            	continue;
	            }
	            User contagiousUser=User.getUser(user); 
	    		HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
	            //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
	           
	            for(Link lsuc : succs.values()){ //get(contagiousUser.getID()).keySet()) {
	                User neighbour=(User)lsuc.getNode2();
	                String v=neighbour.getName();
	                if(!notYet.containsKey(v))
	                    continue ;
	                if(v.equals(user)){
	                	continue;
	                }
	                Double p=notYet.get(v);
	                if(lsuc.getVal()<0.0001){
	                	continue;
	                }
	                p*=(1.0-lsuc.getVal());
	                notYet.put(v,p);
	            }
	        }
	        for(String user:notYet.keySet()){
	        	double p=1.0-notYet.get(user);
	        	infectedstep.put(user,p); //*0.998+0.001);
	        	//System.out.println(user + " : "+(1.0-notYet.get(user)));
	        }
	        
	       
	        infections.put(it,infectedstep);
        }
        //infections.add(infectedstep);
        //System.out.println(PropagationStruct.getPBeforeT(infections));
        pstruct.setInfections(infections) ;
        return 0;
    }
	    
	
	
	
	
   
    public void load(){
		String filename=modelFile;
        User.reinitAllLinks();
        BufferedReader r;
        //pij=new HashMap<String,HashMap<String,Double>>();
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          String[] sline ;
          while((line=r.readLine()) != null) {
        	if(line.startsWith("maxIter")){
            	if(this.maxIter==-2){
            		sline=line.split("=");
            		maxIter=Long.valueOf(sline[1]);
            	}
                continue;
            }
        	if(line.startsWith("contaMaxDelay")){
            	sline=line.split("=");
            	contaMaxDelay=Integer.parseInt(sline[1]);
                continue;
            }
        	String[] tokens = line.split("\t") ;
            if(tokens[2].startsWith("NaN"))
                tokens[2]="0.0" ;
           
            double d = Double.parseDouble(tokens[2]) ;
            if(d<=Double.MIN_VALUE)
                continue ;
            //String source=
            /*HashMap<String,Double> hi=pij.get(tokens[0]);
            hi*/
            User source=User.getUser(tokens[0]);
            User target=User.getUser(tokens[1]);
            Link l=new Link(source,target,d);
            source.addLink(l);
            target.addLink(l);
            //System.out.println("new link "+l);
          }
          r.close();
          loaded=true;
          System.out.println("Loaded" );
        }
        catch(IOException e){
        	System.out.println("Probleme lecture modele "+filename);
        }
        /*for(User user:User.users.values()){
		System.out.println("nb succs "+user+" => "+user.getSuccesseurs().size());
        }*/
        
    }

    public void save() {
    	
		String filename=modelFile;
        try{
          PrintStream p = new PrintStream(filename) ;
          p.println("maxIter="+maxIter);
          p.println("contaMaxDelay="+contaMaxDelay);
          for(User uS : User.users.values()) {
            HashMap<String,Link> succs=uS.getSuccesseurs();
            for(Link lsuc: succs.values()) {
                p.println(uS.getName()+"\t"+lsuc.getNode2().getName()+"\t"+lsuc.getVal()+"\t"+lsuc.getWeights());
            }
          }
        }
        catch(IOException e){
        	System.out.println("Probleme sauvegarde modele "+filename);
        	
        }
    }
   
    
    private double unbiasVal(double p,double n, double z){
    	// Wilson Method for taking the center of a given confidence interval
    	// unbiased = 2.576 => 99% confidence interval for kvw
    	// unbiased = 1.960 => 95% confidence interval for kvw
    	// unbiased = 1.645 => 90% confidence interval for kvw
    	double den=(2.0*(n+z*z));
    	double num=((2.0*n*p)+z*z);
    	//double num2=(-1.0-Math.sqrt(z2*(z2-2.0-(1.0/n)+4.0*p*(n*(1.0-p)+1.0))));
    	double num2=-z*Math.sqrt(z*z+(4.0*n*p*(1.0-p)));
    	double ret=(num+num2)/den;
    	/*if(((num+num2)<0) || (Double.isNaN(kvw))){
    		throw new RuntimeException("p="+p+" z2="+z2+" n="+n+" num="+num+" num2="+num2+" den="+den+" kvw="+kvw);
    	}*/
    	if(ret<0){ret=0.0;}
    	return ret;
    }
 
   
 	
 	
    // "Prediction of Information Diffusion Probabilities for Independent Cascade Model"
    // contaMaxDelay = 1 => propagation can only be done between two users contamined in contiguous steps (as defined in the saito paper). contaMaxDelay=n => propagation can be between steps separated one from each other from at max n steps. contaMaxDelay=-1 => a user can have contamined every user contamined after him  
    public void learn(CascadesLoader loader, String db, String usersCollection, int nbIter, double userLinkThreshold, double min_init, double max_init, double addNeg, double unbiased,int l1reg,double lambdaReg,double globalExtern,double individualExtern) {
    	System.out.println("lambdaReg="+lambdaReg);
    	System.out.println("l1Reg="+l1reg);
    	//boolean regul=true;
        
    	boolean displayLikelihood=true;
        // Pour chaque cascade, liste id users avec leur temps de contamination
        HashMap<Integer,HashMap<User,Long>> userTimeContamination=loader.getTimeSteps(true);
        
        
        
        HashMap<Integer,TreeMap<Long,HashSet<User>>> ctimes=new HashMap<Integer,TreeMap<Long,HashSet<User>>>();
        
       
        HashSet<User> users=new HashSet<User>(User.users.values());
        // Pour tous les utilisateurs on recupere leurs liens
        for(User u:users){
            u.loadLinksFrom(db, usersCollection, userLinkThreshold);
            System.out.println("Liens user "+u.getName()+" charge");
            //System.out.println(u.getSuccesseurs().values());
        }
       
        System.out.println("Initialisation valeurs liens");
        HashSet<Link> allLinks=new HashSet<Link>();
        // Tous les liens obtiennent une valeur arbitraire tiree entre min_init et max_init
        double dif=max_init-min_init;
        for(User u:User.users.values()){
            HashMap<String,Link> succs=u.getSuccesseurs();
            System.out.println("User "+u);
            
            for(Link l:succs.values()){
            	
                double v=(Math.random()*dif)+min_init;
                l.setVal(v);
                
                //System.out.println("Link "+l);
                allLinks.add(l);
                /*User u2=(User)l.getNode2();
            	if(!User.users.values().contains(u2)){
            		throw new RuntimeException("Blem Users");
            	}
            	HashMap<String,Link> preds=u2.getPredecesseurs();
                if((!preds.containsKey(u.getName())) || (preds.get(u.getName())!=l)){
                	throw new RuntimeException("Blem Links");
                }*/
            }
            /*HashMap<String,Link> preds=u.getPredecesseurs();
            for(Link l:preds.values()){
            	User u2=(User)l.getNode1();
            	HashMap<String,Link> succs2=u2.getSuccesseurs();
            	if(!User.users.values().contains(u2)){
            		throw new RuntimeException("Blem Users");
            	}
            	if((!succs2.containsKey(u.getName())) || (succs2.get(u.getName())!=l)){
                	throw new RuntimeException("Blem Links");
                }
            }*/
        }
       
        
        // Table de Probabilite de contamination pour chaque user, indexee par cascade id
        HashMap<Integer,HashMap<User,Double>> P;
       
        // Table des cascades pour chaque lien <v,w> ou time de v == time de w - 1
        HashMap<Link,HashSet<Integer>> spos=new HashMap<Link,HashSet<Integer>>();
       
        // Table des cascades pour chaque lien <v,w> ou time de v == time de w - 1
        //HashMap<Link,HashSet<Integer>> sneg=new HashMap<Link,HashSet<Integer>>();
        HashMap<Link,Double> sneg=new HashMap<Link,Double>();
          
        System.out.println("Compute sneg et spos");
        // compute sneg et spos
        maxIter=1;
        /*int initNeg=0;
        if(addNeg){
        	initNeg=1;
        }*/
        double sumParticip=0.0;
        HashMap<User,Double> nbParticip=new HashMap<User,Double>();
       	double moyParticip=0.0;
        for(Integer c:userTimeContamination.keySet()) {
       			TreeMap<Long,HashSet<User>> ctimesc=new TreeMap<Long,HashSet<User>>();
       			ctimes.put(c, ctimesc);
        		HashMap<User, Long> hc = userTimeContamination.get(c);
        		System.out.println("Cascade c = "+c);
        		User userc=null;
        		if(individualExtern>0){
        			userc=User.getUser("cascade_"+c);
        			hc.put(userc, 0l);
        		}
        		moyParticip+=hc.size();
        		for(User w:hc.keySet()){
        			HashSet<Integer> cpos=null;
        			if(individualExtern>0){
        				Link lc=new Link("individualExtern",userc,w,0.5);
            			userc.addLink(lc,true);
            			allLinks.add(lc);
        				cpos=new HashSet<Integer>();
        				spos.put(lc, cpos);
                        cpos.add(c);
                        
        			}
                    
        			sumParticip+=1.0;
        			Double nbp=nbParticip.get(w);
        			nbp=(nbp==null)?0.0:nbp;
        			nbParticip.put(w,nbp+1.0);
        			//User user=w;
        			Long l=hc.get(w);
        			if(l>maxIter){
        				maxIter=l;
        			}
        			
        			HashSet<User> tusers=ctimesc.get(l);
        			if(tusers==null){
        				tusers=new HashSet<User>();
        				ctimesc.put(l, tusers);
        			}
        			tusers.add(w);
        			
        			//System.out.println("User w = "+w+" => "+l);
        			HashMap<String,Link> succs=w.getSuccesseurs();
        			/*if(w.getName().equals("iddaaaliyiz")){
                    	System.out.println(w+","+succs.size()+" succs");
                    }*/
                    for(String v:succs.keySet()){
                        User uv=User.getUser(v);
                        
                    	Long hl2=hc.get(uv);
                    	/*if((uv.getName().equals("NJConservative9")) && (w.getName().equals("KeshaRam"))){
                        	System.out.println(c+" "+hl2+" "+l);
                        }*/
                        long l2=(hl2!=null)?hl2:-2;
                        if (((contaMaxDelay>=0) && (l2>l) && (l2<=(l+contaMaxDelay))) || ((contaMaxDelay<0) && (l2>l))){
                        	Link lvw=succs.get(v);
                        	cpos=spos.get(lvw);
                            if (cpos==null){
                                cpos=new HashSet<Integer>();
                                spos.put(lvw, cpos);
                            }
                            cpos.add(c);
                            /*if((uv.getName().equals("NJConservative9")) && (w.getName().equals("KeshaRam"))){
                            	System.out.println("NJConservative9 in "+c+" after "+w+" ("+l2+","+l+") "+cpos);
                            	
                            }
                            if((uv.getName().equals("KeshaRam")) && (w.getName().equals("NJConservative9"))){
                            	System.out.println("KeshaRam in "+c+" after "+w+" ("+l2+","+l+") "+cpos);
                            	
                            }*/
                            	
                        }
                        else{
                        	if ((l2==-2) || ((contaMaxDelay>=0) && (l2>(l+contaMaxDelay)))){
                        		Link lvw=succs.get(v);
                        		/*HashSet<Integer> cneg=sneg.get(lvw);
                        		if (cneg==null){
                                    cneg=new HashSet<Integer>();
                                    sneg.put(lvw, cneg);
                                }
                                cneg.add(c);*/
                        		Double x=sneg.get(lvw);
                        		x=(x==null)?0.0:x;
                        		sneg.put(lvw, x+1.0);
                        	}
                        }
                    }
        		}
        }
        if(userTimeContamination.size()>0){
        	System.out.println("Cascade average size = "+ moyParticip/userTimeContamination.size());
        }
       	System.out.println(allLinks.size()+" liens "+spos.size()+" actifs");
       	System.out.println(User.users.size()+" users");
       	HashSet<Link> asup=new HashSet<Link>();
       	double sum=0.0;
       	
        for(Link l : allLinks) {
        	HashSet<Integer> cpos=spos.get(l);
        	//HashSet<Integer> cneg=sneg.get(l);
        	User u1=(User)l.getNode1();
        	User u2=(User)l.getNode2();
        	/*if((u1.getName().equals("KeshaRam")) && (u2.getName().equals("NJConservative9"))){
                throw new RuntimeException("cpos = "+cpos+ " \n cneg = "+sneg.get(l));
        	}*/
        	
        	
        	if (cpos==null){
                l.setVal(0.0);
                asup.add(l);
                continue;
            }
            else{
            	Double nneg=sneg.get(l);
            	nneg=(nneg==null)?0.0:nneg;
            	/*if(cneg!=null){
            		nneg=cneg.size();
            	}*/
            		
            	nneg+=addNeg;
            	Integer npos=cpos.size();
            	//double v=(1.0*npos)/(nneg+npos);
            	double v=0.5; //(Math.random()*dif)+min_init;	
	        	/*if(unbiased>=0){
            		l.setVal(unbiasVal(v,(nneg+npos),unbiased));
            	}
            	else{*/
            		l.setVal(v);
            	//}
	        	sum+=l.getVal();
	        	HashMap<Integer,Double> weights=new HashMap<Integer,Double>();
	        	weights.put(1,npos*1.0);
	        	weights.put(-1,nneg*1.0);
	        	l.setWeights(weights);
	        	
            }
        }
        for(Link l : asup) {
        	allLinks.remove(l);
        	User v=(User)l.getNode1();
            User w=(User)l.getNode2();
            v.removeSuccesseur(w);
            w.removePredecesseur(v);
            if(spos.get(l)!=null){
            	throw new RuntimeException("cpos non null alors que l a sup");
            }
        }
        
        double z2=unbiased*unbiased;
        
        
        double sumR=-1.0;
       
        for(int iteration = 0 ; iteration<nbIter ; iteration++) {
        	if (modelFile.length()==0){
        		modelFile="propagationModels/ICmodel_step"+loader.getStep()+"db_"+db+"_cascades"+loader.getCollection()+"_users"+usersCollection+"_linkThreshold"+userLinkThreshold+((loader.getEmptyIgnored())?"_sansStepsVides":"")+"_asPos"+contaMaxDelay+"_addNeg-"+addNeg+"_unbiased-"+unbiased+"_l1reg-"+l1reg+"_lambdaReg-"+lambdaReg+"_globalExtern-"+globalExtern+"_individualExtern-"+individualExtern;
        	}
        	save();
            System.out.println("iteration : "+iteration);
           
            double sump=0;
            double sumnp=0;
            
            
            long tmoy=0;
            int tnb=0;
            long told=0;
            // Estimate P
            P=new HashMap<Integer,HashMap<User,Double>>();
            System.out.println("P... pour "+userTimeContamination.size());
            for(Integer c:ctimes.keySet()) {
            		TreeMap<Long,HashSet<User>> ctimesc=ctimes.get(c);
       			
//                    System.out.print((iii++) + ", ");
            		
                    HashMap<User,Double> pc=new HashMap<User,Double>();
                    P.put(c, pc);
                    HashMap<User, Long> hc = userTimeContamination.get(c);
                   /* User te=User.getUser("NJConservative9");
                    if(c==304){
                    	if(!hc.containsKey(te)){
                    		//System.out.println()
                    		throw new RuntimeException(hc+"\n 304 Ne contient pas NJConservative9");
                    	}
                    }*/
                    told=0;
                    for(Long tc:ctimesc.keySet()){
                    	HashSet<User> tusers=ctimesc.get(tc);
                    	//System.out.println(tc);
                    	for(User w:tusers){
                    		double p=1.0-globalExtern;
                    		Long l=tc;
                    		/*if(l<=1){
                    			p=0.0;
                        	}*/
                    		//System.out.println(w+" "+l);
                    		
                    		HashMap<String,Link> preds=w.getPredecesseurs();
                    		int npd=0;
                    		for(String v:preds.keySet()){
                    			User uv=User.getUser(v);
                    			Long hl2=hc.get(uv);
                    			long l2=(hl2!=null)?hl2:-2;
                    			//System.out.println("l1 = "+l+" l2="+l2);
                    			if (((contaMaxDelay>=0) && (l2>=(l-contaMaxDelay)) && (l2<l) && (l2>=0)) || ((contaMaxDelay<0) && (hl2!=null) && (l2<l))){
                    				Link lvw=preds.get(v);
                    				double kvw=lvw.getVal();
                    				//Double pv=pc.get(uv);
                    				//pv=(pv==null)?0.0:pv;
                    				
                    				/*if(pv==null){
                    					throw new RuntimeException("Pas vu "+v+" alors que time actuel = "+l+" et time de v = "+l2);
                    				}*/
                    				/*if(pv<0.000001){
                    					pv=0.000001;
                    				}*/
                    				double x=1.0; //(safety*pv)+(1.0-safety);
                    				p*=(1.0-x*kvw);
                    				npd++;
                    			
                    			}
                    		}
                    		if (npd>0){
                        	   p=1.0-p;
                    		}
                        	/*if(p<0.000001){
                        		//p=Double.MIN_VALUE;
                        		p=0.000001;
                        	}*/
                        
                        	pc.put(w, p);
                        	if (p<0){
                        		System.out.println("Blem user "+w.getName()+" pour cascade "+c+" => p = "+p);
                        	}
                        	if (displayLikelihood){
                        	
                        		if (l>1){
                        			if (p>0){
                        				sump+=Math.log(p);
                        			}
                        			else{
                        				System.out.println("p="+p);
                        				sump-=1000000.0;
                        			}
                        		}
                        	
                        		if (l>=1){
                        			HashMap<String,Link> succs=w.getSuccesseurs();
                        			for(String v:succs.keySet()){
                        				User uv=User.getUser(v);
                        				Long hl2=hc.get(uv);
                        				if((hl2==null) || ((contaMaxDelay>=0) && (hl2>(l+contaMaxDelay)))){
                        					//Double pv=pc.get(v);
                            				//pv=(pv==null)?0.0:pv;
                        					//double pv=p;
                        					/*if(pv<0.000001){
                            					pv=0.000001;
                            				}*/
                        					double x=1.0; //(safety*pv)+(1.0-safety);
                        					double kvw=succs.get(v).getVal();
                        					kvw=1.0-x*kvw;
                        					if(kvw<=0){
                        						sumnp-=1000000.0;
                        						System.out.println("kvw="+kvw+" x="+x+" val="+succs.get(v).getVal());
                        					}
                        					else{
                        						sumnp+=Math.log(kvw);
                        					}
                        				}
                        			}
                                    
                        		}
                            
                        	}
                    	}
                    	if(told!=0){
                    		tmoy+=tc-told;
                    		
                    		tnb++;
                    	}
                    	told=tc;
                    }
            }
            
            System.out.println("Avg delay = "+tmoy/tnb);
            
            if (displayLikelihood){
            	if(sumR<0){
            		System.out.println("Likelihood = "+(sump+sumnp));
            	}
            	else{
            		System.out.println("Likelihood = "+(sump+sumnp)+" \t "+sumR+" \t "+(sump+sumnp-sumR));
            	}
            }
           
            
            int nbChanges=0;
            sum=0.0;
            sumR=0.0;
            int nb1=0;
            // Estimate weights ;
            System.out.println("w... pour "+spos.size()+" liens");
            double sc=0.0;
            for(Link l : allLinks) {
                User v=(User)l.getNode1();
                User w=(User)l.getNode2();
                double kvw=l.getVal();
                double okvw=kvw;
                
                if (okvw==1){
                	nb1++;
                }
                HashSet<Integer> cpos=spos.get(l);
                if (cpos==null){
                    l.setVal(0.0);
                    //asup.add(l);
                    continue;
                }
               
                //HashSet<Integer> cneg=sneg.get(l);
                
                double npos=cpos.size();
                Double nneg=sneg.get(l);
                nneg=(nneg==null)?0.0:nneg;
                nneg+=addNeg;
                
                
                double sp=0.0;
                for(Integer c:cpos){
                    HashMap<User,Double> pc=P.get(c);
                    double x=1.0;
                    /*if(safety>0){
                    	Double pv=pc.get(v);
                    	//pv=(pv==null)?0.0:pv;
                    	x=(safety*pv)+(1.0-safety);
                    }*/
                    //npos+=x;
                    Double pu=pc.get(w);
                    if (pu!=null){
                        sp+=(x/pu);
                    }
                    else{
                    	throw new RuntimeException(c+"=> pu null ");
                    }
                }
                /*if(npos<=0.00001){
                	npos=0.00001;
                }*/
               /* if((safety>0) && (cneg!=null)){
                	for(Integer c:cneg){
                		HashMap<User,Double> pc=P.get(c);
                		double x=1.0;
                		if(safety>0){
                			Double pv=pc.get(v);
                			//pv=(pv==null)?0.0:pv;
                			x=(safety*pv)+(1.0-safety);
                		}
                		nneg+=x;
                	}	
                	
                }
                else{
                	if(cneg!=null){
                		nneg=cneg.size();
                	}
                	
                //}*/
                //nneg+=addNeg;
                
                
                kvw*=sp;
                double n=(1.0*npos+1.0*nneg);
                double lambda=lambdaReg;
                int regType=l1reg;
                if(l.getName().equals("individualExtern")){
                	lambda=individualExtern;
                	regType=1;
                }
                
            	if(iteration==(nbIter-1)){
            		if(regType>1){
            			regType=0;
            			//lambda=1.0;
            		}
            	}
                if(regType<=0){
                	kvw*=(1.0/n);
                	
                }
                else{
                	//double f=Math.exp(0.1*(-1.0*npos-1.0*nneg));
                	double reg=lambda; //
                	if(regType==2){
                		reg=reg/(nbParticip.get(v)); ///(sumParticip/nbParticip.size())); //1.0+Math.log(1.0*npos));
                	}
                	if(regType==3){
                		reg=reg/(npos+1); ///(sumParticip/nbParticip.size())); //1.0+Math.log(1.0*npos));
                	}
                	if(regType==4){
                		reg=reg*(Math.exp(-1.0*npos)); ///(sumParticip/nbParticip.size())); //1.0+Math.log(1.0*npos));
                		//System.out.println(reg);
                	}
                	if(regType==5){
                		reg=reg/(nbParticip.get(v)-nneg+1); ///(sumParticip/nbParticip.size())); //1.0+Math.log(1.0*npos));
                	}
                	if(reg<0.00000001){
                		reg=0.00000001;
                	}
                	sc=((1.0*npos)+(1.0*nneg)+reg);
                	double x=Math.pow(sc,2.0)-(4.0*reg*kvw);
                	if(x<-0.01){
                		throw new RuntimeException(kvw+" : sc="+sc+" sp="+sp+" okvw="+okvw+" x="+x+" reg="+reg);
                	}
                	if(x<0){x=0.0;}
                	kvw=(sc-Math.sqrt(x))/(2.0*reg);
                	if((Double.isNaN(kvw)) || Double.isInfinite(kvw)){
                		throw new RuntimeException(kvw+" => sc="+sc+" sp="+sp+" okvw="+okvw+" x="+x+" reg="+reg);
                	}
                	sumR+=reg*kvw;
                }
                if (kvw>=1){
                	/*System.out.println("v "+v.getName()+" w "+w.getName()+" = "+kvw+" "+okvw+" "+npos+" "+nneg);
                	for(Integer c:cpos){
                        HashMap<User,Double> pc=P.get(c);
                        Double pu=pc.get(w);
                        //System.out.println(c + "=> "+userTimeContamination.get(c));
                        if (pu!=null){
                        	System.out.println(c+"=> pu = "+pu);
                        }
                        else{
                        	throw new RuntimeException(c+"=> pu null ");
                        }
                    }*/
                	kvw=1.0;
                	if(iteration<(nbIter-1)){
                		kvw-=0.00001;
                	}
                }
                if((unbiased>0) && (iteration<(nbIter-1))){
                	
                	double p=kvw;
                	// Wilson Method for taking the center of a given confidence interval
                	// unbiased = 2.576 => 99% confidence interval for kvw
                	// unbiased = 1.960 => 95% confidence interval for kvw
                	// unbiased = 1.645 => 90% confidence interval for kvw
                	double den=(2.0*(n+z2));
                	double num=((2.0*n*p)+z2);
                	//double num2=(-1.0-Math.sqrt(z2*(z2-2.0-(1.0/n)+4.0*p*(n*(1.0-p)+1.0))));
                	double num2=0; //-Math.sqrt(z2*(z2+(4.0*n*p*(1.0-p))));
                	kvw=(num+num2)/den;
                	//double erf=ErrorFunction.erf(unbiased/(Math.sqrt((2.0*kvw*(1.0-kvw))/n)));
                	//kvw=erf*kvw; //+(1.0-erf)*0.5;
                	//if(((num+num2)<0) || (Double.isNaN(kvw))){
                	//	throw new RuntimeException("p="+p+" z2="+z2+" n="+n+" num="+num+" num2="+num2+" den="+den+" kvw="+kvw);
                	//}
                	/*double den=Math.log(1+userTimeContamination.size());
                	if(unbiased>1){

                		kvw=(Math.log(1.0+n)/Math.log(unbiased))*kvw/den;
                	}
                	else{
                		kvw=(Math.log(1.0+n))*kvw/den;
                	}*/
                	
                }
                if (kvw>=1){
                	kvw=1.0;
                }
                if(kvw<=0){
                	kvw=0.0;
                }
                if((kvw>=(1-0.000001)) && (iteration<(nbIter-1))){
                		kvw=1.0-0.000001;
                	
                }
            	if((kvw<=(0.000001)) && (iteration<(nbIter-1))){
            		kvw=0.000001;
            	}
        	
                sum+=kvw;
                l.setVal(kvw);
                if (kvw!=okvw){
                	nbChanges++;
                }
                //System.out.println("lien "+v.getID()+","+w.getID()+" => "+kvw);
            }
            
            
            
            System.out.println("nbChanges ="+nbChanges);
            System.out.println("Sum weights = "+sum);
            System.out.println("nb 1 = "+nb1);
            /*if (nbChanges==0){
            	break;
            }*/
           
            
            
           
        } 
        
        loaded=true;
        
        if (modelFile.length()==0){
    		modelFile="propagationModels/ICmodel_step"+loader.getStep()+"db_"+db+"_cascades"+loader.getCollection()+"_users"+usersCollection+"_linkThreshold"+userLinkThreshold+((loader.getEmptyIgnored())?"_sansStepsVides":"")+"_asPos"+contaMaxDelay+"_addNeg-"+addNeg+"_unbiased-"+unbiased+"_l1reg-"+l1reg+"_lambdaReg-"+lambdaReg+"_globalExtern-"+globalExtern+"_individualExtern-"+individualExtern;
    	}
        
    }
   
   
    
    

   
    public boolean function(Link link) {
       
        //System.out.println(weights.get(source).get(target));
        try {
        	//System.out.print(link);
            return Math.random() <= link.getVal() ;
        } catch (NullPointerException e) {
            return false ;
        }
       
    }
   
  public double smooth(double val){
	   double v=smoothing;
	   return val*(1.0-v*2)+v;
   }
   
   
   
   
    /**
     * @param args
     */
    public static void main(String[] args) {
    	String db=args[0];
    	
    	String cascadesCol=args[1];
    	String users=args[2];
        CascadesLoader loader=new CascadesLoader(db, cascadesCol,Integer.parseInt(args[4]),false);
        ICold myModel = new ICold(Integer.parseInt(args[3])); // "propagationModels/ICmodel2_3600_1_cascades2_users1s.txt",50) ;
        myModel.learn(loader,db, users, Integer.parseInt(args[5]), 1, 0.1, 0.3,Double.valueOf(args[6]),Double.valueOf(args[7]),Integer.valueOf(args[8]),Double.valueOf(args[9]),Double.valueOf(args[10]),Double.valueOf(args[11])); //0.00001); //Double.valueOf(args[5]));
        myModel.save();
    	
    	
    	//System.out.println(Math.sqrt(0.002154));
    	//test();
    }
        
    

    public static void test(){
    	ICold myModel = new ICold(1);
        User u1=User.getUser("1");
        User u2=User.getUser("2");
        User u3=User.getUser("3");
        User u4=User.getUser("4");
        User u5=User.getUser("5");
        User u6=User.getUser("6");
        User u7=User.getUser("7");
        User u8=User.getUser("8");
        User u9=User.getUser("9");
        User u10=User.getUser("10");
        Link l=new Link(u1,u2,0.3);
        u1.addLink(l);
        u2.addLink(l);
        l=new Link(u1,u3,0.1);
        u1.addLink(l);
        u3.addLink(l);
        l=new Link(u2,u5,0.5);
        u2.addLink(l);
        u5.addLink(l);
        l=new Link(u3,u1,0.2);
        u3.addLink(l);
        u1.addLink(l);
        l=new Link(u3,u4,0.5);
        u3.addLink(l);
        u4.addLink(l);
        l=new Link(u4,u5,0.1);
        u4.addLink(l);
        u5.addLink(l);
        l=new Link(u5,u6,1.0);
        u5.addLink(l);
        u6.addLink(l);
        l=new Link(u6,u7,0.4);
        u6.addLink(l);
        u7.addLink(l);
        l=new Link(u7,u8,0.3);
        u7.addLink(l);
        u8.addLink(l);
        l=new Link(u7,u9,0.6);
        u7.addLink(l);
        u9.addLink(l);
        l=new Link(u7,u10,0.8);
        u7.addLink(l);
        u10.addLink(l);
        l=new Link(u10,u3,0.4);
        u10.addLink(l);
        u3.addLink(l);
        myModel.loaded=true;
        HashMap<String,Double> init=new HashMap<String,Double>();
        init.put(u1.getName(),1.0);
        TreeMap<Long,HashMap<String,Double>> tinit=new TreeMap<Long,HashMap<String,Double>> ();
		tinit.put((long)1.0, init);
        PropagationStruct p=new PropagationStruct(new Cascade(1,"x",new HashSet<Post>()),tinit, new TreeMap<Long,HashMap<String,Double>>());
        //myModel.inferSimulationProbas(p);
        myModel.inferProbasGibbs(p);
        System.out.println(p.getInfections());
        /*HashMap<User,HashMap<Integer,Integer>> nbContamined=new HashMap<User,HashMap<Integer,Integer>>();
        for(int i=0;i<99000;i++){
        	myModel.inferSimulation(p);
	        
        	ArrayList<HashMap<String,Double>> inf=p.getInfections();
	        HashMap<String,Double> infect=inf.get(inf.size()-1);
	        for(String u:infect.keySet()){
	  		  User user=User.getUser(u);
	          HashMap<Integer,Integer> h=nbContamined.get(user);
	  		  if(h==null){
	  			  h=new HashMap<Integer,Integer>();
	  			  nbContamined.put(user,h);
	  		  }
	  		  Integer t=2; 
	  		  Integer nb=h.get(t);
	  		  nb=(nb==null)?1:(nb+1);
	  		  h.put(t,nb);
	  	  }
	    }
	    System.out.println(nbContamined);*/
	        
    }


}
