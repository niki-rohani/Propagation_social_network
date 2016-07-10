package propagationModels;

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
import java.util.TreeMap;


//import trash.ArtificialCascadesLoader;
import utils.Keyboard;
import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;



import core.Link;
import core.Node;
import core.Post;
import core.Structure;
import core.User;
import cascades.Cascade ;
import cascades.CascadesProducer;
import cascades.IteratorDBCascade;

public class MultipleIC implements PropagationModel {
   
	private Random r;
	private int maxIter;
	private String modelFile;
	private boolean loaded=false;
	private boolean inferProbas=true;
	private int nbMods=1;
	private double probaSeul=0.0;
	private boolean infiniteConta=false;
	private boolean withRoot=false;
	private HashMap<Integer,HashMap<Integer,Double>> probaMods;
	private static boolean testMode=false;
	
	public MultipleIC(int maxIter){
		this("",1,maxIter);
	}
	
	public MultipleIC(){
		this("",1,50);
	}
	public MultipleIC(int nbMods,int maxIter){
		this("",nbMods,maxIter);
	}
	
	public MultipleIC(String modelFile,int nbMods,int maxIter){
		this.modelFile=modelFile;
		this.maxIter=maxIter;
		this.nbMods=nbMods;
		r = new Random() ;
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
		String sm=modelFile.replaceAll("/", "_");
		return("ICmodel_"+maxIter+"_"+sm);
	}
	public String getName(){
		String sm=modelFile.replaceAll("/", "__");
		return sm;
	}
	
	public int infer(Structure struct) {
		if(inferProbas){
			return(inferProbasGibbs(struct));
		}
		else{
			return(inferSimulation(struct));
		}
	}
	
	public int inferProbas(Structure struct) {
        if (!loaded){
        	load();
        }
    	//TODO
	    return 0;
	}
	
	
	public int inferProbasGibbs(Structure struct) {
        if (!loaded){
        	System.out.println("Load model "+modelFile);
        	load();
        	
        }
        
        int nbBurnOut=100;
        int nbIt=500;
        PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> initInfected=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashSet<String> contaminated = new HashSet<String>((PropagationStruct.getPBeforeT(initInfected)).keySet()) ;
        HashMap<String,Integer> utimes=new HashMap<String,Integer>();
        int tt=1;
	    for(long t:initInfected.keySet()){
	    	HashMap<String,Double> inf=initInfected.get(t);
	    	infections.put((long)tt, (HashMap<String,Double>) inf.clone());
	    	for(String u:inf.keySet()){
	    		utimes.put(u, tt);
	    	}
	    	tt++;
	    }
	    int firstNewT=tt;
	    
        
        HashSet<User> initUsers=new HashSet<User>();
        HashMap<User,HashMap<User,Link>> exposed=new HashMap<User,HashMap<User,Link>>();
        HashMap<User,Integer> times=new HashMap<User,Integer>();
	    ArrayList<User> users=new ArrayList<User>(User.users.values());
	    HashMap<Integer,HashMap<User,Double>> ptimespos=new HashMap<Integer,HashMap<User,Double>>();
	    HashMap<Integer,HashMap<User,Double>> ptimesneg=new HashMap<Integer,HashMap<User,Double>>();
	    HashMap<Integer,Double> likes=new HashMap<Integer,Double>();
	    
	    HashMap<Integer, HashSet<User>> ctimes=new HashMap<Integer, HashSet<User>>();
	    //int lastT=1;
        for(String u:utimes.keySet()){
        	     int time=utimes.get(u); //Integer.parseInt(contaminated.get(u).toString());
        	     
        	     
	        	User user=User.getUser(u);
	        	
	        		initUsers.add(user);
	        	HashMap<User,Link> from=new HashMap<User,Link>();
	        	HashMap<String,Link> succs=user.getSuccesseurs();
	        	for(Link l:succs.values()){
	        		User succ=(User)l.getNode2();
	        		from=exposed.get(succ);
	        		if(from==null){
	        			from=new HashMap<User,Link>();
	        			exposed.put(succ, from);
	        		}
	        		from.put(user,l);
	        	}
	        	HashSet<User> us=ctimes.get(time);
    			if(us==null){
    				us=new HashSet<User>();
    				ctimes.put(time, us);
    			}
    			us.add(user);
    			times.put(user, time);
	        	
	    }
	    
	    
	   /* System.out.println(times);
	    System.out.println(ctimes);
	    System.out.println(ptimespos);
	    System.out.println(ptimesneg);*/
        
        likes=this.probaMods.get(pstruct.getCascade().getID());
        System.out.println("Ref likes");
        System.out.println(likes);
	    
	    for(int mod=1;mod<=nbMods;mod++){
	    	HashMap<User,Double> p=new HashMap<User,Double>();
	    	ptimespos.put(mod, p);
	    	p=new HashMap<User,Double>();
	    	ptimesneg.put(mod, p);
	    	likes.put(mod, 1.0/(nbMods*1.0));
	    }
	    // calcul probas pour observations
	    for(User user:users){
	    	if(!initUsers.contains(user)){
	    		for(int mod=1;mod<=nbMods;mod++){
	    			reevalProba(mod,user,ptimespos.get(mod),ptimesneg.get(mod),times,ctimes);
	    		}
	    	}
	    	
	    }
	    
	    System.out.println(likes);
	    
	    reevalLikes(likes,ptimespos,ptimesneg,times,ctimes);
	    /*System.out.println(times);
	    System.out.println(ctimes);
	    System.out.println(ptimespos);
	    System.out.println(ptimesneg);
	    */
	    HashMap<User,HashMap<Integer,Integer>> nbContamined=new HashMap<User,HashMap<Integer,Integer>>();
	    
	    for(int i=1;i<=nbIt;i++){
	    	System.out.println(likes);
	    	//ArrayList<User> exposedList=new ArrayList<User>(exposed.keySet());
	      Collections.shuffle(users);
	      for(User user:users){
	    	  
	    	//int x=(int)(Math.random()*exposedList.size());
	    	//User user=exposedList.get(x);
	    	if(!exposed.containsKey(user)){
	    		continue;
	    	}
	    	String suser=user.getName();
	    	Integer time=times.get(user);
	    	HashMap<User,Link> from=exposed.get(user);
	    	//HashSet<Integer> possibilities=new HashSet<Integer>();
	    	if(!contaminated.contains(suser)){
	    		
	    		HashMap<Integer,HashMap<Integer,Double>> probas=new HashMap<Integer,HashMap<Integer,Double>>();
	    		HashMap<Integer,HashMap<Integer,Double>> probasnon=new HashMap<Integer,HashMap<Integer,Double>>();
	    		HashMap<Integer,HashMap<Integer,Double>> pro=new HashMap<Integer,HashMap<Integer,Double>>();
	    		HashMap<Integer,HashMap<User,HashMap<Integer,Double>>> probasw=new HashMap<Integer,HashMap<User,HashMap<Integer,Double>>>();
	    		HashMap<Integer,HashMap<User,HashMap<Integer,Double>>> probaswnon=new HashMap<Integer,HashMap<User,HashMap<Integer,Double>>>();
	    		
	    		for(int mod=1;mod<=nbMods;mod++){
	    			probas.put(mod, new HashMap<Integer,Double>());
	    			probasnon.put(mod, new HashMap<Integer,Double>());
	    			pro.put(mod, new HashMap<Integer,Double>());
	    			probasw.put(mod,new HashMap<User,HashMap<Integer,Double>>());
	    			probaswnon.put(mod,new HashMap<User,HashMap<Integer,Double>>());
	    		}
	    		//HashMap<String,Link> preds=user.getPredecesseurs();
	    		HashMap<String,Link> succs=user.getSuccesseurs();
	    		
	    		for(User v:from.keySet()){
	    			//User v=(User)l.getNode1();
	    			//String sv=v.getName();
	    			Link l=from.get(v);
	    			HashMap<Integer,Double> vals=l.getWeights(); //getVal(l);
	    			//System.out.println(vals);
	    			Integer t=times.get(v);
	    			if(t!=null){
	    			  for(int z=t+1;z<=(maxIter+1);z++){
	    				if((z==(t+1)) && (z<(maxIter+1))){
	    					for(int mod=1;mod<=nbMods;mod++){
	    						HashMap<Integer,Double> h=probas.get(mod);
	    						Double proba=h.get(z);
	    						proba=(proba==null)?1.0:proba;
	    						proba*=(1.0-getVal(vals.get(mod)));
	    						h.put(z, proba);
	    					}
	    				}
	    				else{
	    					for(int mod=1;mod<=nbMods;mod++){
	    						HashMap<Integer,Double> h=probasnon.get(mod);
	    						Double proba=h.get(z);
	    						proba=(proba==null)?1.0:proba;
	    						proba*=(1.0-getVal(vals.get(mod)));
	    						h.put(z, proba);
	    					}
	    				}
	    			  }
	    		   }
	    		}
	    		
	    		//double s=0.0;
	    		for(int mod=1;mod<=nbMods;mod++){
	    			HashMap<Integer,Double> pos=probas.get(mod);
	    			HashMap<Integer,Double> neg=probasnon.get(mod);
	    			HashMap<Integer,Double> pr=pro.get(mod);
	    			for(int z=firstNewT;z<=(maxIter+1);z++){
	    				Double proba=pos.get(z);
	    				if(z<(maxIter+1)){
	    					proba=(proba==null)?1.0:proba;
	    				}
	    				else{
	    					proba=(proba==null)?0.0:proba;
	    				}
	    				proba=1.0-proba;
	    				
	    				pos.put(z, proba);
	    				Double probanon=neg.get(z);
	    				probanon=(probanon==null)?1.0:probanon;
	    				//System.out.println("pro new pos "+user+" z "+z+" "+proba+" "+probanon);
	    				proba*=probanon;
	    				if(proba>0){
	    					pr.put(z, proba);
	    					//System.out.println(user+" add to z = "+z);
	    				}
	    				//s+=proba;
	    			}
	    		}
	    		
	    		for(Link l:succs.values()){
	    			User w=(User)l.getNode2();
	    			if(contaminated.contains(w.getName())){
	    				continue;
	    			}
	    			Integer tw=times.get(w);
	    			//String sw=w.getName();
	    			HashMap<Integer,Double> vals=l.getWeights();
	    			HashMap<Integer,HashMap<Integer,Double>> prow=new HashMap<Integer,HashMap<Integer,Double>>();
	    			HashMap<Integer,HashMap<Integer,Double>> prononw=new HashMap<Integer,HashMap<Integer,Double>>();
	    			for(int mod=1;mod<=nbMods;mod++){
	    				HashMap<Integer,Double> mprow=new HashMap<Integer,Double>();
	    				prow.put(mod, mprow);
	    				HashMap<Integer,Double> mprononw=new HashMap<Integer,Double>();
	    				prononw.put(mod, mprononw);
	    				HashMap<User,Double> ptpos=ptimespos.get(mod);
	    				HashMap<User,Double> ptneg=ptimesneg.get(mod);
	    				Double ppos=ptpos.get(w);
	    				Double pneg=ptneg.get(w);
	    				HashMap<Integer,Double> pr=pro.get(mod);
	    				HashMap<User,HashMap<Integer,Double>> mprobasw=probasw.get(mod);
	    				HashMap<User,HashMap<Integer,Double>> mprobaswnon=probaswnon.get(mod);
	    				
		    			for(Integer z:pr.keySet()){
		    				//System.out.println(user+" "+w+" z = "+z);
		    				Double proba=pr.get(z);
		    				Integer ti=(z==(maxIter+1))?null:z;
		    				//if((ti==null) && ((tw==null) || ((tw!=null) && (tw>ti)))){
		    					reevalProba(mod,w,ptpos,ptneg,times,ctimes,1,user,getVal(vals.get(mod)),time,ti);
		    				//}
		    				
		    				Double ptimepos=ptpos.get(w);
		    				ptimepos=(ptimepos==null)?1.0:ptimepos;
		    				mprow.put(z, ptimepos);
		    				Double ptimeneg=ptneg.get(w);
		    				//ptimeneg=(ptimeneg==null)?1.0:ptimeneg;
		    				mprononw.put(z, ptimeneg);
		    				proba*=ptimeneg*ptimepos;
		    				pr.put(z, proba);
		    				ptpos.put(w,ppos);
	    					ptneg.put(w,pneg);
		    			}
		    			mprobasw.put(w,mprow);
		    			mprobaswnon.put(w,mprononw);
	    			}
	    		}
	    		double sum=0.0;
	    		HashMap<Integer,Double> pall=new HashMap<Integer,Double>();
	    		for(int mod=1;mod<=nbMods;mod++){
	    			HashMap<Integer,Double> pr=pro.get(mod);
	    			for(Integer z:pr.keySet()){
	    				Double proba=pr.get(z);
	    				Double pa=pall.get(z);
	    				pa=(pa==null)?0.0:pa;
	    				pa+=likes.get(mod)*proba;
	    				pall.put(z, pa);
	    			}
	    		}
	    		for(Double val:pall.values()){
	    			sum+=val;
	    		}
	    		if(sum==0.0){
	    			throw new RuntimeException("sum = 0 !!");
	    		}
	    		double alea=Math.random()*sum;
	    		int chosen=-1;
	    		for(Integer z:pall.keySet()){
	    			if (z<firstNewT){
	    				throw new RuntimeException(" z = "+z);
	    			}
	    			if(chosen==-1){
	    				chosen=z;
	    			}
	    			Double proba=pall.get(z);
	    			alea-=proba;
	    			if(alea<0){
	    				chosen=z;
	    				
	    				break;
	    			}
	    		}
	    		for(Link l:succs.values()){
	    			User w=(User)l.getNode2();
	    			//String sw=w.getName();
	    			if(contaminated.contains(w.getName())){
	    				continue;
	    			}
	    			for(int mod=1;mod<=nbMods;mod++){
	    				HashMap<User,HashMap<Integer,Double>> mprobasw=probasw.get(mod);
	    				HashMap<User,HashMap<Integer,Double>> mprobaswnon=probaswnon.get(mod);
	    				HashMap<User,Double> ptpos=ptimespos.get(mod);
	    				HashMap<User,Double> ptneg=ptimesneg.get(mod);
	    				
		    			Double p=mprobasw.get(w).get(chosen);
		    			if(p==null){
		    				throw new RuntimeException("p pos "+w+" null !");
		    			}
		    			ptpos.put(w, p);
		    			p=mprobaswnon.get(w).get(chosen);
		    			if(p==null){
		    				throw new RuntimeException("p neg "+w+" null !");
		    			}
		    			ptneg.put(w, p);
	    			}
	    		}
	    		/*if(chosen==(maxIter+1)){
	    			ptimespos.remove(suser);
	    		}
	    		else{*/
	    		for(int mod=1;mod<=nbMods;mod++){
	    			HashMap<Integer,Double> pos=probas.get(mod);
	    			HashMap<Integer,Double> neg=probasnon.get(mod);
	    			HashMap<User,Double> ptpos=ptimespos.get(mod);
    				HashMap<User,Double> ptneg=ptimesneg.get(mod);
	    			Double p=pos.get(chosen);
	    			ptpos.put(user, (p==null)?1.0:p);
	    			p=neg.get(chosen);
	    			ptneg.put(user, (p==null)?1.0:p);
	    		}
	    		changeTime(user,times,ctimes,(chosen==(maxIter+1))?null:chosen);
    			// modif exposed
	    		if(chosen==(maxIter+1)){
    				if(time!=null){
    					succs=user.getSuccesseurs();
    		        	for(Link l:succs.values()){
    		        		User succ=(User)l.getNode2();
    		        		from=exposed.get(succ);
    		        		if(from!=null){
    		        			from.remove(user);
    		        		}
    		        		if(from.size()==0){
    		        			exposed.remove(succ);
    		        		}
    		        	}
    				}
    			}
    			else{
    				if(time==null){
    					succs=user.getSuccesseurs();
    		        	for(Link l:succs.values()){
    		        		User succ=(User)l.getNode2();
    		        		from=exposed.get(succ);
    		        		if(from==null){
    		        			from=new HashMap<User,Link>();
    		        			exposed.put(succ, from);
    		        		}
    		        		from.put(user, l);
    		        	}
    				}
    			}
	    	}
	    	
	      }
	      //if(i>50){
	        reevalLikes(likes,ptimespos,ptimesneg,times,ctimes);
	      //}
	    	//System.out.println(ptimespos);
	    	//System.out.println(ptimesneg);
	    	//System.out.println(exposed);
	      
	      if(i>nbBurnOut){
	    	  for(User user:times.keySet()){
	    		  HashMap<Integer,Integer> h=nbContamined.get(user);
	    		  if(h==null){
	    			  h=new HashMap<Integer,Integer>();
	    			  nbContamined.put(user,h);
	    		  }
	    		  Integer t=times.get(user);
	    		  Integer nb=h.get(t);
	    		  nb=(nb==null)?1:(nb+1);
	    		  h.put(t,nb);
	    	  }
	      }
	      //System.out.println(times);
	    	//Clavier.saisirLigne(" ");
	    }
	    System.out.println(likes);
	    //System.out.println(nbContamined);
	    
	    //infections=new ArrayList<HashMap<String,Double>>();
	    for(int i=1;i<=maxIter;i++){
	    	infections.put((long)i,new HashMap<String,Double>());
	    }
	    int nbdiv=nbIt-nbBurnOut;
	    for(User user:nbContamined.keySet()){
	    	String suser=user.getName();
	    	HashMap<Integer,Integer> h=nbContamined.get(user);
	    	if(h!=null){
	    		for(Integer t:h.keySet()){
	    			int time=t;
	    			int nb=h.get(t);
	    			//for(int i=(t-1);i<maxIter;i++){
	    				HashMap<String,Double> inf=infections.get((long)time);
	    				Double n=inf.get(suser);
	    				n=(n==null)?nb:(n+nb);
	    				inf.put(suser, n);
	    			//}
	    		}
	    	}
	    }
	    for(int i=1;i<=maxIter;i++){
	    	HashMap<String,Double> inf=infections.get((long)i);
	    	for(String user:inf.keySet()){
	    		Double n=inf.get(user);
				if(n==null){
					continue;
				}
				
				inf.put(user, (1.0*n/nbdiv));
	    	}
	    }
	    //System.out.println(likes);
    	
	    System.out.println(infections);
	    pstruct.setInfections(infections);
        return(0);
	}
	 
	private double getVal(Double v){
	    	double val=v;
			val*=0.98;
			val+=0.01;
			return(val);
	}
	
	private double getVal(Link l){
		double val=l.getVal();
		val*=0.8;
		val+=0.1;
		return(val);
	}
	
	private void changeTime(User user,HashMap<User,Integer> times,HashMap<Integer, HashSet<User>> ctimes, Integer newTime){
		Integer time=times.get(user);
		if(time!=null){
			HashSet<User> ct=ctimes.get(time);
			ct.remove(user);
		}
		if(newTime!=null){
			HashSet<User> ct=ctimes.get(newTime);
			if(ct==null){
				ct=new HashSet<User>();
				ctimes.put(newTime, ct);
			}
			ct.add(user);
			times.put(user,newTime);
		}
		else{
			times.remove(user);
		}
	}
	
	private void reevalLikes(HashMap<Integer,Double> likes,HashMap<Integer,HashMap<User,Double>> ptimespos,HashMap<Integer,HashMap<User,Double>> ptimesneg,HashMap<User,Integer> times,HashMap<Integer, HashSet<User>> ctimes){
		boolean infinite=false;
		double sum=0.0;
		double max=1.0;
		for(int mod=1;mod<=nbMods;mod++){
			double like=0.0;
			//double sumn=0.0;
			HashMap<User,Double> ptpos=ptimespos.get(mod);
			HashMap<User,Double> ptneg=ptimesneg.get(mod);
			HashSet<User> old=new HashSet<User>();
			for(int t=1;t<maxIter+1;t++){
			    HashSet<User> us=ctimes.get(t);
			    
			    if(us!=null){
			    	if(t>1){
			    		for(User user:us){
			    			double p=1.0;
			    			HashMap<String,Link> preds=user.getPredecesseurs();
			    			int nb=0;
			    			for(Link l:preds.values()){
			    				User pred=(User)l.getNode1();
			    				if(old.contains(pred)){
			    					nb++;
			    					Double val=l.getWeights().get(mod);
			    					p*=(1.0-val);
			    				}
			    			}
			    			if(nb>0){
			    				p=1.0-p;
			    			}
			    			//p=0.1+0.9*p;
			    			if(p==0.0){
			    				p=Double.MIN_VALUE;
			    			}
			    			like+=Math.log(p);
			    			//if (l>=1){
                        		HashMap<String,Link> succs=user.getSuccesseurs();
                        		for(String v:succs.keySet()){
                                    User uv=User.getUser(v);
                        			Integer hl2=times.get(uv);
                                    if((hl2==null) ||(hl2>(t+1))){
                                    	double kvw=succs.get(v).getWeights().get(mod);
                                    	kvw=1.0-kvw;
                                    	if(kvw<=0){
                                    		like-=1000000.0;
                                    		//System.out.println("kvw="+kvw);
                                    	}
                                    	else{
                                    		like+=Math.log(kvw);
                                    	}
                                    }
                        		}
                                    
                        	//}
			    			//like+=Math.log(ptpos.get(user));
			    			//if(t>2){
			    			//	like+=Math.log(ptneg.get(user));
			    			//}
			    		}
			    	}
			    	if(!infinite){
			    		 old=new HashSet<User>();
			    	}
			    	old.addAll(us);
			    }
			    
			}
			//sum+=Math.exp(like);
			if((max>0) || (like>max)){
    			max=like;
    		}
			likes.put(mod, like);
		}
		for(int mod=1;mod<=nbMods;mod++){
			sum+=Math.exp(likes.get(mod)-max);
		}
		sum=(sum==0)?(1.0*nbMods):sum;
		for(int mod=1;mod<=nbMods;mod++){
			Double like=likes.get(mod);
			//System.out.println(like+":"+Math.exp(like));
			like=Math.exp(like-max)/sum;
			likes.put(mod, like);
		}
		//System.out.println(likes);
	}
	/*private void reevalLikes(HashMap<Integer,Double> likes,HashMap<Integer,HashMap<User,Double>> ptimespos,HashMap<Integer,HashMap<User,Double>> ptimesneg,HashMap<User,Integer> times,HashMap<Integer, HashSet<User>> ctimes){
		
		for(User w:times.keySet()){
			//User user=w;
			Integer l=times.get(w);
			//System.out.println("User w = "+w+" => "+l);
			HashMap<String,Link> succs=w.getSuccesseurs();
			
	        for(String v:succs.keySet()){
	            User uv=User.getUser(v);
	        	Long hl2=hc.get(uv);
	            long l2=(hl2!=null)?hl2:-2;
	            if (((contaMaxDelay>=0) && (l2>l) && (l2<=(l+contaMaxDelay))) || ((contaMaxDelay<0) && (l2>l))){
	            	Link lvw=succs.get(v);
	            	HashSet<Integer> cpos=spos.get(lvw);
	                if (cpos==null){
	                    cpos=new HashSet<Integer>();
	                    spos.put(lvw, cpos);
	                }
	                cpos.add(c);
	            }
	            else{
	            	if ((l2==-2) || ((contaMaxDelay>=0) && (l2>(l+contaMaxDelay)))){
	            		Link lvw=succs.get(v);
	            		HashSet<Integer> cneg=sneg.get(lvw);
	                    if (cneg==null){
	                        cneg=new HashSet<Integer>();
	                        sneg.put(lvw, cneg);
	                    }
	                    cneg.add(c);
	            		
	            	}
	            }
	        }
		}
	}*/
	
	private void reevalProba(int mod,User user,HashMap<User,Double> ptimespos,HashMap<User,Double> ptimesneg,HashMap<User,Integer> times,HashMap<Integer, HashSet<User>> ctimes){
		reevalProba(mod,user,ptimespos,ptimesneg,times,ctimes,0,null,0.0,null, null);
	}
	    
	// mode = 0 => compute from scratch, mode==1 => reeval after setting user changed at time newTime
	private void reevalProba(int mod,User user,HashMap<User,Double> ptimespos,HashMap<User,Double> ptimesneg,HashMap<User,Integer> times,HashMap<Integer, HashSet<User>> ctimes,int mode,User changed,double valLink,Integer oldTime, Integer newTime){
		//String suser=user.getName();
    	Double ptimepos=1.0;
    	Double ptimeneg=1.0;
    	
    	//Integer oldTime=null; 
    	Integer time=times.get(user);
    	
    	//oldTime=
    	if(mode!=0){
    		oldTime=times.get(changed);
    		if(oldTime==newTime){
    			return;
    		}
    		ptimepos=ptimespos.get(user);
    		ptimeneg=ptimesneg.get(user);
    		//System.out.println(user+" ptimepos "+ptimepos+" ptimeneg "+ptimeneg);
    		if(((ptimepos==null) && (time!=null)) || (((oldTime!=null) && (time!=null) && (ptimepos>=(1.0-Double.MIN_VALUE))))){
    			if(time!=null){
    				mode=0;
    				//changeTime(changed,times,ctimes,newTime);
    			}
    		}
    		if((ptimeneg==null) || ((oldTime!=null) && (ptimeneg<=Double.MIN_VALUE))){
    			mode=0;
    			//changeTime(changed,times,ctimes,newTime);
    		}
    	}	
    	
    	//if(!contaminated.containsKey(suser)){
    	if(mode==0){
    		ptimepos=1.0;
        	ptimeneg=1.0;
    		if(time==null){
    			time=maxIter+2;
    			ptimepos=0.0;
    		}
    		HashMap<String,Link> preds=user.getPredecesseurs();
			for(Link l:preds.values()){
				User v=(User)l.getNode1();
				//String sv=v.getName();
				Integer tv=times.get(v);
				if((changed!=null) && (changed.getName().equals(v.getName()))){
					tv=newTime;
				}
				//System.out.println(user+" tw "+time+" "+v+" time "+tv);
				if(tv!=null){
					if(time<=tv){
						continue;
					}
					double val=getVal(l.getWeights().get(mod));
					if(time==(tv+1)){
						ptimepos*=(1.0-val);
						//System.out.println(user+" ptimepos *= "+(1.0-val)+" pour "+v);
					}
					else{
						ptimeneg*=(1.0-val);
						//System.out.println(user+" ptimeneg *= "+(1.0-val)+" pour "+v);
					}
					
				}
			}
    	}
    	else{
    		//changeTime(changed,times,ctimes,newTime);
    		ptimepos=1.0-ptimepos;
    		if((oldTime!=null) && (time!=null) && (oldTime==(time-1))){
    			ptimepos/=(1.0-valLink);
    		}
    		if((newTime!=null) && (time!=null) && (newTime==(time-1))){
    			ptimepos*=(1.0-valLink);
    		}
    		if((oldTime!=null) &&  ((time==null) || (oldTime<(time-1)))){
    			ptimeneg/=(1.0-valLink);
    		}
    		if((newTime!=null) && ((time==null) || (newTime<(time-1)))){
    			ptimeneg*=(1.0-valLink);
    		}
    	}
    	//if(time!=(maxIter+2)){
    		ptimespos.put(user, 1.0-ptimepos);
    	//}
    	ptimesneg.put(user, ptimeneg);
    	//System.out.println("changed "+changed+" oldt "+oldTime+" newt "+newTime+" t "+time+" user "+user+" "+(1.0-ptimepos)+" "+ptimeneg);
	}
	
	    
	
	
    public int inferSimulation(Structure struct) {
        if (!loaded){
        	load();
        }
        PropagationStruct pstruct = (PropagationStruct)struct ;
    	TreeMap<Long,HashMap<String,Double>> initInfected=pstruct.getInitContaminated();
        TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
        HashSet<String> contaminated = new HashSet<String>((PropagationStruct.getPBeforeT(initInfected)).keySet()) ;
        
        int tt=1;
	    for(long t:initInfected.keySet()){
	    	HashMap<String,Double> inf=initInfected.get(t);
	    	infections.put((long)tt, (HashMap<String,Double>) inf.clone());
	    	tt++;
	    }
	    int firstNewT=tt;
	     
	    
	    
	     
        //System.out.println("init : "+contaminated);
        //int tstart=tt;
        //this.alreadyTried = new HashMap<User, HashSet<User>>() ;
        //this.trying = new HashMap<User, HashSet<User>>() ;
       
        HashMap<String,Double> contagious = infections.get(infections.lastKey()); 
        
        User currentUser ;
        HashMap<String,Double> infectedstep=new HashMap<String,Double>();
        for(int iteration =firstNewT ; iteration <= maxIter ; iteration++) {
        	
            //System.out.println("Nb Contaminated : "+infected.size());
           
            for(String contagiousU : contagious.keySet()) {
                User contagiousUser=User.getUser(contagiousU);
                HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
                //System.out.println(contagiousUser.getID()+" => nb succs = "+succs.size());
               
                for(Link lsuc : succs.values()){ //get(contagiousUser.getID()).keySet()) {
                    User neighbour=(User)lsuc.getNode2();
                    if(infectedstep.containsKey(neighbour.getName()))
                        continue ;
                    if(function(lsuc) ) {
                    	infectedstep.put(neighbour.getName(),1.0) ;
                        contaminated.add(neighbour.getName()) ;
                        
                    }
                }
            }
           
           
            contagious=(HashMap<String,Double>)infectedstep.clone() ;
            if(contagious.isEmpty())
                break ; 
            
            infections.put((long)iteration,infectedstep);
            infectedstep = new HashMap<String,Double>() ;
           
           
           
        }
        //infections.add(infectedstep);
        pstruct.setInfections(infections) ;
        return 0;
    }
   
   
    public void load(){
		String filename=modelFile;
        User.reinitAllLinks();
        BufferedReader r;
        probaMods=new HashMap<Integer,HashMap<Integer,Double>>();
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          boolean linkMode=false;
          boolean likeMode=false;
          line=r.readLine();
          String[] tokens=line.split(" ");
          int inf=Integer.parseInt(tokens[1]);
          if(inf==1){
        	  infiniteConta=true;
          }
          else{
        	  infiniteConta=false;
          }
          line=r.readLine();
          tokens=line.split(" ");
          inf=Integer.parseInt(tokens[1]);
          if(inf==1){
        	  withRoot=true;
          }
          else{
        	  withRoot=false;
          }
          line=r.readLine();
          tokens=line.split(" ");
          probaSeul=Double.valueOf(tokens[1]);
          while((line=r.readLine()) != null) {
        	if(line.contains("<Links>")){
        		 linkMode=true;
                  continue;
          	}
          	if(line.contains("</Links>")){
                  linkMode=false;
          		  continue;
          	}
          	if(line.contains("<Likes>")){
          		likeMode=true;
          		continue;
          	}
          	if(line.contains("</Likes>")){
          		likeMode=false;
          		continue;
          	}
        	if(linkMode){  
        		tokens = line.split("\t") ;
	            
	            String vals=tokens[2].substring(1);
	            vals=vals.substring(0,vals.length()-1);
	            vals=vals.replaceAll(" ","");
	            String[] w=vals.split(",");
	            HashMap<Integer,Double> weights=new HashMap<Integer,Double>();
	            for(int i=0;i<w.length;i++){
	            	String[] els=w[i].split("=");
	            	int mod=Integer.parseInt(els[0]);
	            	double val=Double.valueOf(els[1]);
	            	weights.put(mod, val);
	            }
	            
	            User source=User.getUser(tokens[0]);
	            User target=User.getUser(tokens[1]);
	            Link l=new Link(source,target,1.0);
	            l.setWeights(weights);
	            source.addLink(l);
	            target.addLink(l);
	            //System.out.println("new link "+l);
        	}
        	if(likeMode){
        		tokens = line.split("\t") ;
        		int c=Integer.parseInt(tokens[0]);
        		HashMap<Integer,Double> h=probaMods.get(c);
        		if(h==null){
        			h=new HashMap<Integer,Double>();
        			probaMods.put(c,h);
        		}
        		int mod=Integer.parseInt(tokens[1]);
        		double p=Double.valueOf(tokens[2]);
        		h.put(mod, p);
        	}
          }
          r.close();
          loaded=true;
        }
        catch(IOException e){
        	System.out.println("Probleme lecture modele "+filename);
        }
        
    }

    public void save() {
    	
		String filename=modelFile;
        try{
          PrintStream p = new PrintStream(filename) ;
          p.println("infiniteConta "+((infiniteConta)?1:0));
          p.println("withRoot "+((withRoot)?1:0));
          p.println("probaSeul "+probaSeul);
          p.println("<Links>");
          for(User uS : User.users.values()) {
            HashMap<String,Link> succs=uS.getSuccesseurs();
            for(Link lsuc: succs.values()) {
                p.println(uS.getName()+"\t"+lsuc.getNode2().getName()+"\t"+lsuc.getWeights());
            }
          }
          p.println("</Links>");
          p.println("<Likes>");
          for(Integer c:probaMods.keySet()){
        	  HashMap<Integer,Double> h=probaMods.get(c);
        	  for(Integer mod:h.keySet()){
        		  p.println(c+"\t"+mod+"\t"+h.get(mod));
        	  }
          }
          p.println("</Likes>");
        }
        catch(IOException e){
        	System.out.println("Probleme sauvegarde modele "+filename);
        	
        }
    }
   
 
 // retourne une table cascade_id,user => time contamination
    // zapVides indique si on compte compte les timesteps sans posts dans une cascade donnee pour numeroter les temps de contamination (true=> on ne les compte pas) 
  	public static HashMap<Integer,HashMap<User,Long>> getTimeStepsTest(String db, String collection,int step,boolean zapVides){
          HashMap<Integer,HashMap<User,Long>> userTimeContamination=new HashMap<Integer,HashMap<User,Long>>();
          //DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
          //DBCursor cursor = col.find();
          Post p=null;
          HashMap<Integer,User> us=new HashMap<Integer,User>();
          for(int i=1;i<=10;i++){
        	  User u=new User("user_"+i);
        	  us.put(i, u);
          }
          HashMap<User,Long> ihc=new HashMap<User,Long>();
          userTimeContamination.put(1,ihc);
          ihc.put(us.get(1), (long) 1);
          //ihc.put(2, (long) 1);
          ihc.put(us.get(2), (long) 2);
          ihc.put(us.get(4), (long) 3);
          /*ihc.put(5, (long) 4);
          ihc.put(6, (long) 4);
          ihc.put(7, (long) 4);
          ihc.put(8, (long) 5);
          ihc.put(9, (long) 6);
          ihc.put(10, (long) 6);*/
          
          ihc=new HashMap<User,Long>();
          userTimeContamination.put(2,ihc);
          ihc.put(us.get(1), (long) 1);
          ihc.put(us.get(2), (long) 2);
          ihc.put(us.get(3), (long) 3);
          /*ihc.put(8, (long) 4);
          ihc.put(9, (long) 5);
          ihc.put(10, (long) 5);*/
          
          
          ihc=new HashMap<User,Long>();
          userTimeContamination.put(3,ihc);
          ihc.put(us.get(1), (long) 1);
          ihc.put(us.get(2), (long) 2);
          ihc.put(us.get(3), (long) 2);
          ihc.put(us.get(4), (long) 3);
          ihc.put(us.get(5), (long) 4);
          ihc.put(us.get(6), (long) 4);
          ihc.put(us.get(7), (long) 4);
          ihc.put(us.get(8), (long) 5);
          ihc.put(us.get(9), (long) 6);
          ihc.put(us.get(10), (long) 6);
          
          
          ihc=new HashMap<User,Long>();
          userTimeContamination.put(4,ihc);
          ihc.put(us.get(6), (long) 1);
          ihc.put(us.get(5), (long) 2);
          ihc.put(us.get(4), (long) 2);
          ihc.put(us.get(3), (long) 3);
          ihc.put(us.get(1), (long) 4);
          ihc.put(us.get(2), (long) 5);
          ihc.put(us.get(10), (long) 6);
          
          return(userTimeContamination);
         
      }
 	
 	
    // "Prediction of Information Diffusion Probabilities for Independent Cascade Model"
 	// contaMaxDelay = 1 => propagation can only be done between two users contamined in contiguous steps (as defined in the saito paper). contaMaxDelay=n => propagation can be between steps separated one from each other from at max n steps. contaMaxDelay=-1 => a user can have contamined every user contamined after him  
    public void learn(CascadesLoader loader, String db, String usersCollection, int maxIter, double userLinkThreshold, double min_init, double max_init, boolean infiniteConta, double probaSeul, boolean withRoot) {
    	
    	boolean displayLikelihood=true;
       
    	int contaMaxDelay=1;
    	if(infiniteConta){
    		contaMaxDelay=-1;
    	}
    	this.infiniteConta=infiniteConta;
    	this.probaSeul=probaSeul;
    	this.withRoot=withRoot;
    	// Pour chaque cascade, liste id users avec leur temps de contamination
        HashMap<Integer,HashMap<User,Long>> userTimeContamination;
    	if(testMode){
    		userTimeContamination=getTimeStepsTest("","",1,true);
    	}
    	else{
    		userTimeContamination=loader.getTimeSteps(true);
    	}
       
        HashSet<User> users=new HashSet<User>(User.users.values());
        // Pour tous les utilisateurs on recupere leurs liens
        for(User u:users){
        	if(!testMode){
        	    u.loadLinksFrom(db, usersCollection, userLinkThreshold);
        	}
        	else{
        	   for(User u2:users){
        		   if(u.getID()!=u2.getID()){
        			   Link l=new Link(u,u2,1.0);
        			   u.addLink(l);
        			   u2.addLink(l);
        		   }
        	   }
        	}
            System.out.println("Liens user "+u.getName()+" charge");
            //System.out.println(u.getSuccesseurs().values());
        }
       
        System.out.println("Initialisation valeurs liens");
        HashSet<Link> allLinks=new HashSet<Link>();
        // Tous les liens obtiennent une valeur arbitraire tiree entre min_init et max_init
        double dif=max_init-min_init;
        User root=null;
        if(withRoot){
        	root=User.getUser("rootUser");
        }
        for(User u:User.users.values()){
            HashMap<String,Link> succs=u.getSuccesseurs();
            System.out.println("User "+u);
            
            for(Link l:succs.values()){
            	
                HashMap<Integer,Double> weights=new HashMap<Integer,Double>();
                for(int m=1;m<=nbMods;m++){
                	double v=(Math.random()*dif)+min_init;
                    weights.put(m, v);
                }
                l.setWeights(weights);
                /*if(u.getName().equals("iddaaaliyiz")){
                	System.out.println(u+","+l.getNode2()+"=>"+v);
                }*/
                //System.out.println("Link "+l);
                allLinks.add(l);
            }
            if(withRoot){
            	Link l=new Link(root,u,1.0);
            	HashMap<Integer,Double> weights=new HashMap<Integer,Double>();
            	for(int m=1;m<=nbMods;m++){
            		double v=(Math.random()*dif)+min_init;
            		weights.put(m, v);
            	}
            	l.setWeights(weights);
            	u.addLink(l,true);
            }
        }
        
        System.out.println(allLinks.size());
        
        
        // Table des cascades pour chaque lien <v,w> ou time de v == time de w - 1
        HashMap<Link,HashSet<Integer>> spos=new HashMap<Link,HashSet<Integer>>();
        HashMap<Link,HashSet<Integer>> sneg=new HashMap<Link,HashSet<Integer>>();
        
          
        System.out.println("Compute sneg et spos");
        // compute sneg et spos
       	for(Integer c:userTimeContamination.keySet()) {
        		HashMap<User, Long> hc = userTimeContamination.get(c);
        		if(withRoot){
        			hc.put(root,(long)0.0);
        		}
        		System.out.println("Cascade c = "+c);
        		for(User w:hc.keySet()){
        			//User user=w;
        			Long l=hc.get(w);
        			//System.out.println("User w = "+w+" => "+l);
        			HashMap<String,Link> succs=w.getSuccesseurs();
        			/*if(w.getName().equals("iddaaaliyiz")){
                    	System.out.println(w+","+succs.size()+" succs");
                    }*/
                    for(String v:succs.keySet()){
                        User uv=User.getUser(v);
                    	Long hl2=hc.get(uv);
                        long l2=(hl2!=null)?hl2:-2;
                        if (((contaMaxDelay>=0) && (l2>l) && (l2<=(l+contaMaxDelay))) || ((contaMaxDelay<0) && (l2>l))){
                        	Link lvw=succs.get(v);
                        	HashSet<Integer> cpos=spos.get(lvw);
                            if (cpos==null){
                                cpos=new HashSet<Integer>();
                                spos.put(lvw, cpos);
                            }
                            cpos.add(c);
                        }
                        else{
                        	if ((l2==-2) || ((contaMaxDelay>=0) && (l2>(l+contaMaxDelay)))){
                        		Link lvw=succs.get(v);
                        		HashSet<Integer> cneg=sneg.get(lvw);
                                if (cneg==null){
                                    cneg=new HashSet<Integer>();
                                    sneg.put(lvw, cneg);
                                }
                                cneg.add(c);
                        		
                        	}
                        }
                    }
        		}
        }
        HashSet<Link> asup=new HashSet<Link>();
        for(Link l : allLinks) {
        	HashSet<Integer> cpos=spos.get(l);
            if (cpos==null){
                l.setVal(0.0);
                asup.add(l);
                continue;
            }
        }
        for(Link l : asup) {
        	allLinks.remove(l);
        	User v=(User)l.getNode1();
            User w=(User)l.getNode2();
            v.removeSuccesseur(w);
            w.removePredecesseur(v);
        }
        
        HashMap<Integer,HashMap<Integer,Double>> pmod=new HashMap<Integer,HashMap<Integer,Double>>();
        HashMap<Integer,HashMap<User,HashMap<Integer,Double>>> Pmods=new HashMap<Integer,HashMap<User,HashMap<Integer,Double>>>();
        System.out.println(spos.size()+" liens positifs");
        HashMap<Integer,HashMap<Integer,Double>> likes=new HashMap<Integer,HashMap<Integer,Double>>();
        for(int iteration = 0 ; iteration<=maxIter ; iteration++) {
           
            System.out.println("iteration : "+iteration);
            likes=new HashMap<Integer,HashMap<Integer,Double>>();
            
            for(Integer c:userTimeContamination.keySet()) {
            	HashMap<User,HashMap<Integer,Double>> P=new HashMap<User,HashMap<Integer,Double>>(); 
            	Pmods.put(c,P);
            	HashMap<User, Long> hc = userTimeContamination.get(c);
            	// Estimate P
            	//System.out.println("P... pour cascade "+c);
            	//int iii =0;
                    
                for(User w:hc.keySet()){
                    	
                        HashMap<Integer,Double> p=new HashMap<Integer,Double>();
                        Long l=hc.get(w);
                        User user=w;
                        HashMap<String,Link> preds=user.getPredecesseurs();
                        int npd=0;
                        for(String v:preds.keySet()){
                            User uv=User.getUser(v);
                        	Long hl2=hc.get(uv);
                            long l2=(hl2!=null)?hl2:-2;
                            //System.out.println("l1 = "+l+" l2="+l2);
                            if (((contaMaxDelay>=0) && (l2>=(l-contaMaxDelay)) && (l2<l) && (l2>=0)) || ((contaMaxDelay<0) && (hl2!=null) && (l2<l))){
                                Link lvw=preds.get(v);
                                HashMap<Integer,Double> weights=lvw.getWeights();
                                for(int mod=1;mod<=nbMods;mod++){
                            		//double sump=0;
                                	//double sumnp=0; 
                                	Double kvw=weights.get(mod);
                                	kvw=(kvw==null)?0.0:kvw;
                                	Double pm=p.get(mod);
                                	pm=(pm==null)?1.0:pm;
                                	pm*=(1.0-kvw);
                                	p.put(mod,pm);
                                }
                                
                                npd++;
                            }
                        }
                        if (npd>0){
                        	for(int mod=1;mod<=nbMods;mod++){
                        		Double pm=p.get(mod);
                        		pm=1.0-pm;
                        		pm=probaSeul+(1.0-probaSeul)*pm;
                        		if(pm==0.0){
                        			pm=Double.MIN_VALUE;
                        		}
                        		
                        		p.put(mod,pm);
                        		if (pm<0){
                                	System.out.println("Blem user "+w.getName()+" pour cascade "+c+" => pm = "+pm);
                                }
                        	}
                        }
                        else{
                        	for(int mod=1;mod<=nbMods;mod++){
                        		p.put(mod,1.0);
                        	}
                        }
                        P.put(w, p);
                        //System.out.println(c+" "+w+" => "+p); 
                       
                		HashMap<Integer,Double> h=likes.get(c);
                        if(h==null){
                         	h=new HashMap<Integer,Double>();
                         	likes.put(c,h);
                        }
                        for(int mod=1;mod<=nbMods;mod++){
                        	 Double pm=p.get(mod);
                             Double li=h.get(mod);
                             li=(li==null)?0.0:li;
                    		 double sump=0.0;
                    		 double sumnp=0.0;
                             if (displayLikelihood){
	                        	if (l>1){
	                        		//pm=0.1+0.9*pm;
	                        		if (pm>0){
	                                	sump+=Math.log(pm);
	                                }
	                            	else{
	                            		//System.out.println("p="+p);
	                            		sump-=1000000.0;
	                            	}
	                        	}
	                        	
	                        	if (l>=1){
	                        		HashMap<String,Link> succs=user.getSuccesseurs();
	                        		for(String v:succs.keySet()){
	                                    User uv=User.getUser(v);
	                        			Long hl2=hc.get(uv);
	                                    if((hl2==null) || ((contaMaxDelay>=0) && (hl2>(l+contaMaxDelay)))){
	                                    	double kvw=succs.get(v).getWeights().get(mod);
	                                    	kvw=1.0-kvw;
	                                    	if(kvw<=0){
	                                    		sumnp-=1000000.0;
	                                    		//System.out.println("kvw="+kvw);
	                                    	}
	                                    	else{
	                                    		sumnp+=Math.log(kvw);
	                                    	}
	                                    }
	                        		}
	                                    
	                        	}
	                            
                    		}
                            li+=sump+sumnp;
                            /*if(li==0.0){
                            	li=-Double.MIN_VALUE;
                            }*/
                            h.put(mod,li);
                        }
                   
            	}
            
            	
            }
            double like=0.0;
            for(Integer c:likes.keySet()){
            	HashMap<Integer,Double> h=likes.get(c);
            	double max=1.0;
            	for(Integer mod:h.keySet()){
            		double v=h.get(mod);
            		if((max>0) || (v>max)){
            			max=v;
            		}
            	}
            	double sum=0.0;
            	//System.out.println("max "+max);
            	for(Integer mod:h.keySet()){
            		//sum+=1.0/h.get(mod);
            		sum+=Math.exp(h.get(mod)-max);
            		//System.out.println(mod+" => "+h.get(mod)+" "+(h.get(mod)-max)+" "+Math.exp(h.get(mod)-max));
            	}
            	//sum=(sum==0)?(1.0*nbMods):sum;
            	HashMap<Integer,Double> pm=new HashMap<Integer,Double>();
            	pmod.put(c, pm);
            	for(Integer mod:h.keySet()){
            		double v=h.get(mod);
            		double p=0.0;
            		/*if(sum==0.0){
            			p=1.0/nbMods;
            		}
            		else{*/
            			//p=(1.0/v)/sum;
            			p=Math.exp(v-max)/sum;
            		//}
            		pm.put(mod, p);
            		like+=p*v;
            		//System.out.println(c+","+mod+"="+sum+" "+v+" "+p);
            		if(Double.isNaN(p)){
            			throw new RuntimeException("NAN !");
            		}
            		
            	}
            	
            }
            
            if (displayLikelihood){
        		System.out.println("Likelihood = "+(like));
        	}
            System.out.println(pmod);
            if(iteration<maxIter){
            int nbChanges=0;
            //double sum=0;
            //int nb1=0;
            // Estimate weights ;
            System.out.println("w...");
            for(Link l : allLinks) {
            	User v=(User)l.getNode1();
            	User w=(User)l.getNode2();
            	
            	HashMap<Integer,Double> kvw=l.getWeights();
            	//System.out.println(v+"=>"+w+":"+kvw);
            	//HashMap<Integer,Double> okvw=new HashMap<Integer,Double>(kvw);
            	HashSet<Integer> cpos=spos.get(l);
            	HashSet<Integer> cneg=sneg.get(l);
            	if(cneg==null){
            		cneg=new HashSet<Integer>();
            	}
            	int npos=cpos.size();
            	int nneg=cneg.size();
            	HashMap<Integer,Double> num=new HashMap<Integer,Double>();
            	HashMap<Integer,Double> denp=new HashMap<Integer,Double>();
            	HashMap<Integer,Double> denn=new HashMap<Integer,Double>();
            	
            	for(Integer c:cpos){
                    HashMap<User,HashMap<Integer,Double>> pc=Pmods.get(c);
                    
                    HashMap<Integer,Double> pmodel=pmod.get(c);
                    HashMap<Integer,Double> pp=pc.get(w);
                    for(int mod=1;mod<=nbMods;mod++){
                    	Double pw=pp.get(mod);
                    	if(pw==0.0){
                			throw new RuntimeException("pw null => "+mod+" "+w+" "+c);
                		}
                    	//pw=(pw==null)?0.0:pw;
                    	double pmc=pmodel.get(mod);
                    	Double nu=num.get(mod);
                    	nu=(nu==null)?0.0:nu;
                    	num.put(mod, nu+(pmc/pw));
                    	Double den=denp.get(mod);
                    	den=(den==null)?0.0:den;
                    	denp.put(mod, den+pmc);
                    }
                }
            	for(Integer c:cneg){
                    HashMap<Integer,Double> pmodel=pmod.get(c);
                    for(int mod=1;mod<=nbMods;mod++){
                    	double pmc=pmodel.get(mod);
                    	Double den=denn.get(mod);
                    	den=(den==null)?0.0:den;
                    	denn.put(mod, den+pmc);
                    }
                }
            	for(int mod=1;mod<=nbMods;mod++){
            		double k=kvw.get(mod);
            		double old=k;
            		double nu=num.get(mod);
            		Double dn=denn.get(mod);
            		dn=(dn==null)?0.0:dn;
            		double den=dn+denp.get(mod);
            		if(den==0.0){
            			//throw new RuntimeException("den null => "+nu+" "+dn+" "+den);
            			k*=Double.MIN_VALUE;
            		}
            		else{
            			k*=(nu/den);
            		}
            		if (k>1){
                    	k=1.0;
                    }
            		//k=0.01+k*0.98;
            		kvw.put(mod, k);
            		if (k!=old){
                    	nbChanges++;
                    }
            		
            	}
                //System.out.println("lien "+v.getID()+","+w.getID()+" => "+kvw);
            }
            
            
            
            System.out.println("nbChanges ="+nbChanges);
            
            if (nbChanges==0){
            	iteration=maxIter;
            }
            }
           
        } 
        
        loaded=true;
        
        if (modelFile.length()==0){
    		modelFile="propagationModels/MultipleIC_nbMods"+nbMods+"_step"+loader.getStep()+"_cascades"+loader.getCollection()+"_users"+usersCollection+"_linkThreshold"+userLinkThreshold+"_maxIter"+maxIter+((loader.getEmptyIgnored())?"_sansStepsVides":"")+"_asPos"+contaMaxDelay+"_probaSeul"+probaSeul+((withRoot)?"_withRoot":"");
    	}
        System.out.println(pmod);
        this.probaMods=pmod;
    }
   
   
    
    

   
    public boolean function(Link link) {
       
        //System.out.println(weights.get(source).get(target));
        try {
        	//System.out.print(link);
            return r.nextDouble() <= link.getVal() ;
        } catch (NullPointerException e) {
            return false ;
        }
       
    }
   
   
   
   
   
   
    /**
     * @param args
     */
    public static void main(String[] args) {
        /*CascadesLoader loader=new CascadesLoader("us_elections5000", "artificial_1",1,true);
        MultipleIC myModel = new MultipleIC(3,50); // "propagationModels/ICmodel2_3600_1_cascades2_users1s.txt",50) ;
        testMode=true;
        myModel.learn(loader,"us_elections5000", "users_1", 100, 1, 0.1, 0.3,false,0.0,false) ;
        myModel.save();*/
    	HashMap<String,Double> init1=new HashMap<String,Double>();
        init1.put("user_1",1.0);
        HashMap<String,Double> init2=new HashMap<String,Double>();
        init2.put("user_2",1.0);
        HashMap<String,Double> init3=new HashMap<String,Double>();
        init3.put("user_4",1.0);
        TreeMap<Long,HashMap<String,Double>> tinit=new TreeMap<Long,HashMap<String,Double>> ();
		tinit.put((long)1.0, init1);
		//tinit.put((long)2.0, init2);
		//tinit.put((long)3.0, init3);
		PropagationStruct ps=new PropagationStruct(new Cascade(1,"x",new HashSet<Post>()),tinit, new TreeMap<Long,HashMap<String,Double>>());
    	MultipleIC myModel = new MultipleIC("propagationModels/MultipleIC_nbMods3_step1_cascadesartificial_1_usersusers_1_linkThreshold1.0_maxIter100_sansStepsVides_asPos1_probaSeul0.0",3,50);
    	myModel.inferProbasGibbs(ps);
    	//test();
    }
        
    


}