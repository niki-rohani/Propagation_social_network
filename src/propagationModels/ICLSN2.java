package propagationModels;

import java.io.BufferedReader;
//import utils.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.io.File;
import java.util.LinkedList;

import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.util.ArrayList;

import org.jsoup.nodes.Document;

import core.Text;
import core.Link;
import core.Post;
import core.Node;
import core.Structure;
import core.User;
import cascades.Cascade ;
import cascades.CascadesProducer;
import cascades.IteratorDBCascade;
import utils.Log;
import utils.WorkQueue;
// Independent Cascade between Latent Sub-Nodes 
// Plusieurs actifs par user possible, pas d'activation de noeud d'un user infecte a un timestep precedent 
public class ICLSN2 implements PropagationModel {
   
	private Random r;
	private int maxIter=100;
	private String modelFile;
	private boolean loaded=false;
	private WorkQueue wq=null; // seulement pour le learning
	public boolean testMode=false;
	private boolean inferProbas=true;
	private HashMap<String,ArrayList<Node>> userNodes;
	private HashMap<Node,String> userParents;
	private HashMap<Integer,HashMap<Node,Double>> pactifTrain;
	private HashMap<String,HashMap<Node,Double>> proportionNodes;
	private String learnedFromDb;
	private String learnedFromCol;
	
	
	public ICLSN2(){
		this("");
	}
	
	public ICLSN2(String modelFile){ //,int maxIter){
		this.modelFile=modelFile;
		//this.maxIter=maxIter;
		r = new Random() ;
		
	}
	
	public ICLSN2(String modelFile,boolean inferProbas){
		this.modelFile=modelFile;
		//this.maxIter=maxIter;
		r = new Random() ;
		//this.nbSubNodes=nbSubNodes;
		this.inferProbas=inferProbas;
	}
	
	public HashSet<String> getUsers(){
		if(!loaded){
			load();
		}
		return new HashSet<String>(userNodes.keySet());
	}
	public int getContentNbDims(){
		return 0;
	}
	public String toString(){
		String sm=modelFile.replaceAll("/", "/");
		return("ICLSN_"+sm);
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
    	/*PropagationStruct pstruct = (PropagationStruct)struct ;
        HashMap<String,Long> contaminated=pstruct.getInitContaminated();
        Cascade cascade=pstruct.getCascade();
        Integer c=cascade.getID();
	    
        HashMap<String,Double> infected = new HashMap<String,Double>() ;
        HashMap<Node,Double> nodeInfected=new HashMap<Node,Double>();
        HashMap<Node,Double> actifsc=this.pactifTrain.get(c);
        for(String u:contaminated.keySet()){
	        	infected.put(u,1.0);
	        	for(Node node:this.userNodes.get(u)){
	        		Double pn=actifsc.get(node);
	        		nodeInfected.put(node,(pn==null)?0.0:pn);
	        	}
	    }
	     
        
	    ArrayList<HashMap<String,Double>> infections=new ArrayList<HashMap<String,Double>>();
	    infections.add(infected);
	    HashMap<String,Double> infectedstep=infected;
	    HashMap<Node,Double> nodeInfectedstep=nodeInfected;
	    
	    HashMap<Node,Double> oldinf=new HashMap<Node,Double>(); 
        for(int iteration =1 ; iteration <= maxIter ; iteration++) {
        	//System.out.println("iteration "+iteration);
        	//System.out.println(infectedstep);
        	HashMap<Node,Double> oldoldinf=oldinf;
        	oldinf=nodeInfectedstep;
        	infectedstep = new HashMap<String,Double>() ;
    	    infections.add(infectedstep);
    	    nodeInfectedstep=new HashMap<Node,Double>();
    	    //System.out.println(oldinf);
    	    HashMap<Node,Double> pconta=new HashMap<Node,Double>();
    	    for(Node u:oldinf.keySet()){
    	    	double probaConta=oldinf.get(u);
    	    	if(oldoldinf.containsKey(u)){
    	    		probaConta-=(oldoldinf.get(u));
    	    	}
    	    	if(probaConta>Double.MIN_VALUE){
    	    		pconta.put(u, probaConta);
    	    		//System.out.println(u+" : pconta "+probaConta);
    	    	}
    	    }
    	    int nbchanges=0;
    	    HashSet<Node> vus=new HashSet<Node>();
    	    HashSet<String> us=new HashSet<String>();
    	    for(Node conta:pconta.keySet()){
    	    	HashMap<String,Link> succs=conta.getSuccesseurs();
    	    	//System.out.println(conta+" "+succs.size()+" succs");
    	    	for(Link l:succs.values()){
    	    		Node w=l.getNode2();
    	    		if(vus.contains(w)){
    	    			continue;
    	    		}
    	    		vus.add(w);
	    	    	Double p=oldinf.get(w);
	    	    	p=(p==null)?0.0:p;
	    	    	double pstep=(1.0-p);
	    	    	if(pstep>Double.MIN_VALUE){
	    	    		nbchanges++;
		    	    	double pn=1.0;
		    	    	HashMap<String,Link> preds=w.getPredecesseurs();
		    	    	//System.out.println(w+" "+preds.size()+" preds");
		    	    	for(Link lv:preds.values()){
		    	    		Node v=lv.getNode1();
		    	    		Double pc=pconta.get(v);
		    	    		pc=(pc==null)?0.0:pc;
		    	    		double val=lv.getVal();
		    	    		pn*=(1.0-pc*val);
		    	    		
		    	    		
		    	    	}
		    	    	pstep*=(1.0-pn);
	    	    	}
	    	    	p+=pstep;
	    	    	nodeInfectedstep.put(w, p);
	    	    	String user=userParents.get(w);
	    	    	us.add(user);
    	    	}
    	    	
    	    }
    	    for(Node u:oldinf.keySet()){
    	    	if(!vus.contains(u)){
    	    		nodeInfectedstep.put(u, oldinf.get(u));
    	    		String user=userParents.get(u);
	    	    	us.add(user);
    	    	}
    	    }
    	    for(String user:us){
    	    	double p=1.0;
    	    	if(!contaminated.containsKey(user)){
	    	    	ArrayList<Node> nodes=userNodes.get(user);
	    	    	for(Node n:nodes){
	    	    		Double pr=nodeInfectedstep.get(n);
	    	    		pr=(pr==null)?0.0:pr;
	    	    		p*=(1.0-pr);
	    	    	}
	    	    	p=1.0-p;
	    	    }
    	    	infectedstep.put(user,p);
    	    }
    	    
    	    
           if(nbchanges==0){
        	   break;
           }
            
           
        }
        System.out.println(infections.get(infections.size()-1));
	    pstruct.setInfections(infections) ;
	    */
	    return 0;
	}
	
	//TODO
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
        Cascade cascade=pstruct.getCascade();
        int c=cascade.getID();
        
	     
	     
        //System.out.println("init : "+contaminated);
        //int tstart=firstNewT;
        //this.alreadyTried = new HashMap<User, HashSet<User>>() ;
        //this.trying = new HashMap<User, HashSet<User>>() ;
       
        HashMap<String,Double> contagious = infections.get(infections.lastKey()); 
         
        User currentUser ;
        HashMap<String,Double> infectedstep=new HashMap<String,Double>();
        for(int iteration = firstNewT ; iteration <= maxIter ; iteration++) {
        	 //System.out.println("Nb Contaminated : "+infected.size());
           
            for(String contagiousU : contagious.keySet()) {
            	User contagiousUser=User.getUser(contagiousU);
                 HashMap<String,Link> succs=contagiousUser.getSuccesseurs();
                
                for(Link lsuc : succs.values()){ //get(contagiousUser.getID()).keySet()) {
                    User neighbour=(User)lsuc.getNode2();
                    if(contaminated.contains(neighbour.getName()))
                        continue ;
                    if(function(lsuc) ) {
                        infectedstep.put(neighbour.getName(),1.0) ;
                        contaminated.add(neighbour.getName()) ;
                        //alreadyTried.remove(neighbour) ;
                    }
                }
            }
           
           
            contagious=(HashMap<String,Double>)infectedstep.clone() ;
            if(contagious.isEmpty())
                break ; 
            
            infections.put((long)iteration,infectedstep);
            infectedstep = new HashMap<String,Double>() ;
           
        }
        pstruct.setInfections(infections) ;
        return 0;
    }
   
    
    // Ne fonctionne pour le moment que pour des contaminated de 1 step
    public int inferProbasGibbs(Structure struct) {
        if (!loaded){
        	System.out.println("Load model "+modelFile);
        	load();
        	
        }
        //HashMap<String,Integer> userNbContamined=new HashMap<String,Integer>();
        int nbBurnOut=50;
        int nbIt=300;
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
        HashMap<Node,HashMap<Node,Link>> exposed=new HashMap<Node,HashMap<Node,Link>>();
        HashMap<Node,Integer> times=new HashMap<Node,Integer>();
	    HashMap<Node,Double> ptimespos=new HashMap<Node,Double>();
	    HashMap<Node,Double> ptimesneg=new HashMap<Node,Double>();
	    ArrayList<String> users=new ArrayList<String>(userNodes.keySet());
		HashMap<String,Integer> nbActifsInit=new HashMap<String,Integer>();   
	    HashMap<Integer, HashSet<Node>> ctimes=new HashMap<Integer, HashSet<Node>>();
	   
	    
	    //ctimes.put(1,us);
	    for(String u:utimes.keySet()){
	        //infected.put(u,1.0);
	        //User user=User.getUser(u);
	    	int time=utimes.get(u);
	        ArrayList<Node> unodes=userNodes.get(u);
	        //System.out.println(u+"=>"+unodes);
	        for(Node node:unodes){
	        	HashMap<Node,Link> from=new HashMap<Node,Link>();
	        	HashMap<String,Link> succs=node.getSuccesseurs();
	        	for(Link l:succs.values()){
	        		if(l.getVal()>0){
	        			Node succ=l.getNode2();
	        			from=exposed.get(succ);
	        			if(from==null){
	        				from=new HashMap<Node,Link>();
	        				exposed.put(succ, from);
	        			}
	        			from.put(node,l);
	        			//System.out.println(l);
	        		}
	        	}
	        	
		    	if(!times.containsKey(node)){
		    			//System.out.println("ici "+node);
		    			times.put(node, time);
		    			HashSet<Node> us=ctimes.get(time);
		    			if(us==null){
		    				us=new HashSet<Node>();
		    				ctimes.put(time, us);
		    			}
		    			us.add(node);
		    	}
		    	
	        }
	        nbActifsInit.put(u, unodes.size());
	    }
	    
	    
	    /*System.out.println(times);
	    System.out.println("ctimes "+ctimes);
	    System.out.println(ptimespos);
	    System.out.println(ptimesneg);
	    System.out.println(exposed);
	    */
	    
	     // calcul probas pour observations
	    for(String user:users){
	    	for(Node node:userNodes.get(user)){
	    		reevalProba(node,contaminated,ptimespos,ptimesneg,times,ctimes);
	    	}
	    }
	    /*System.out.println(times);
	    System.out.println("ctimes" +ctimes);
	    System.out.println(ptimespos);
	    System.out.println(ptimesneg);
	    */
	    HashMap<String,HashMap<Integer,Integer>> nbContamined=new HashMap<String,HashMap<Integer,Integer>>();
	    
	    for(int i=1;i<=nbIt;i++){
	      Collections.shuffle(users);
	      for(String user:users){
	    	  HashMap<Node,Double> propNode=this.proportionNodes.get(user);
	    	  boolean propNull=false;
	    	  if(propNode==null){
	    		  propNull=true;
	    		  propNode=new HashMap<Node,Double>();
	    	  }
	    	for(Node node:userNodes.get(user)){
		    	//String suser=user.getName();
		    	Integer time=times.get(node);
		    	HashMap<Node,Link> from=exposed.get(node);
		    	//HashSet<Integer> possibilities=new HashSet<Integer>();
		    	boolean init=false;
		    	if(contaminated.contains(user)){
		    		init=true;
		    		if(nbActifsInit.get(user)==1){
		    			if((time!=null) && (time<firstNewT)){
		    				continue;
		    			}
		    		}
		    	}
		    	else{
		    		if(!exposed.containsKey(node)){
			    		continue;
			    	}
		    	}
		    	HashMap<Integer,Double> probas=new HashMap<Integer,Double>();
		    	HashMap<Integer,Double> probasnon=new HashMap<Integer,Double>();
		    	if (from!=null){
			    	for(Node v:from.keySet()){
			    		Link l=from.get(v);
			    		double val=getVal(l);
			    		Integer t=times.get(v);
			    		if(t!=null){
			    		  for(int z=t+1;z<=(maxIter+1);z++){
			    			if((z==(t+1)) && (z<(maxIter+1))){
			    				Double proba=probas.get(z);
				    			proba=(proba==null)?1.0:proba;
			    				proba*=(1.0-val);
			    				probas.put(z, proba);
			    			}
			    			else{
			    				Double proba=probasnon.get(z);
				    			proba=(proba==null)?1.0:proba;
			    				proba*=(1.0-val);
			    				probasnon.put(z, proba);
			    			}
			    		  }
			    		}
			    	}
		    	}
		    	if(init){
		    		//int x=node.getName().lastIndexOf("_");
		    		//int num=Integer.parseInt(node.getName().substring(x+1));
		    		double pnon;
		    		//if(num==1){
		    			probas.put(1, 0.5);
		    			pnon=0.5;
		    			//probasnon.put(1, 0.5);
		    		/*}
		    		else{
		    			probas.put(1, 0.9);
		    			pnon=0.9;
		    			//probasnon.put(1, 0.9);
		    		}*/
		    		for(int z=2;z<=(maxIter+1);z++){
		    			Double proba=probasnon.get(z);
		    			proba=(proba==null)?1.0:proba;
	    				proba*=pnon;
	    				probasnon.put(z, proba);
		    		}
		    		
		    	}
		    	HashMap<Integer,Double> pro=new HashMap<Integer,Double>(); // probas que a ce moment et pas avant
		    	for(int z=1;z<=(maxIter+1);z++){
		    			Double proba=probas.get(z);
		    			if(z<(maxIter+1)){
		    				proba=(proba==null)?1.0:proba;
		    			}
		    			else{
		    				proba=(proba==null)?0.0:proba;
		    			}
						proba=1.0-proba;
						
						probas.put(z, proba);
						Double probanon=probasnon.get(z);
	    				probanon=(probanon==null)?1.0:probanon;
	    				//System.out.println("pro new pos "+user+" z "+z+" "+proba+" "+probanon);
						proba*=probanon;
						if(proba>0){
							pro.put(z, proba);
							//System.out.println(user+" add to z = "+z);
							
						}
						//s+=proba;
		    	}
		    		
		    	HashMap<Node,HashMap<Integer,Double>> probasw=new HashMap<Node,HashMap<Integer,Double>>();
		    	HashMap<Node,HashMap<Integer,Double>> probaswnon=new HashMap<Node,HashMap<Integer,Double>>();
		    	HashMap<String,Link> succs=node.getSuccesseurs();
		    	//System.out.println(node+" pro => "+pro);
		    	for(Link l:succs.values()){
		    		Node w=l.getNode2();
		    		Integer tw=times.get(w);
		    		double val=getVal(l);
		    		HashMap<Integer,Double> prow=new HashMap<Integer,Double>();
		    		HashMap<Integer,Double> prononw=new HashMap<Integer,Double>();
		    			
		    		Double ppos=ptimespos.get(w);
		    		Double pneg=ptimesneg.get(w);
		    		for(Integer z:pro.keySet()){
		    			//System.out.println(user+" "+w+" z = "+z);
		    			Double proba=pro.get(z);
		    			Integer ti=(z==(maxIter+1))?null:z;
		    			//if((ti==null) && ((tw==null) || ((tw!=null) && (tw>ti)))){
		    				reevalProba(w,contaminated,ptimespos,ptimesneg,times,ctimes,1,node,val,time,ti);
		    			//}
		    				
		    			Double ptimepos=ptimespos.get(w);
		    			ptimepos=(ptimepos==null)?1.0:ptimepos;
		    			prow.put(z, ptimepos);
		    			Double ptimeneg=ptimesneg.get(w);
		    			//ptimeneg=(ptimeneg==null)?1.0:ptimeneg;
		    			prononw.put(z, ptimeneg);
		    			proba*=ptimeneg*ptimepos;
		    			//System.out.println(node+" at "+z+" for "+w+" => "+proba+" "+ptimepos+" "+ptimeneg);
		    			pro.put(z, proba);
		    			ptimespos.put(w,ppos);
	    				ptimesneg.put(w,pneg);
		    		}
		    		probasw.put(w,prow);
		    		probaswnon.put(w,prononw);
		    	}
		    	double sum=0.0;
		    	//int x=node.getName().lastIndexOf("_");
	    		//int num=Integer.parseInt(node.getName().substring(x+1));
		    	/*Double propv=propNode.get(node);
				propv=(propv==null)?0.0:propv;
				propv=propv*0.8+0.1;
				if(propNull){
					propv=1.0;
				}*/
		    	for(Integer z:pro.keySet()){
		    		Double proba=pro.get(z);
		    		
		    		/*if(num>1){
		    			if(z==(maxIter+1)){
		    				proba*=0.9;
		    			}
		    			else{
		    				proba*=0.1;
		    			}
		    			pro.put(z,proba);
		    		}*/
		    		/*if(z<(maxIter+1)){
		    			proba*=propv;
		    			pro.put(z, proba);
		    		}*/
		    		
		    		//System.out.println(user+" proba "+z+" = "+proba);
		    		sum+=proba;
		    	}
		    	if(sum==0.0){
		    		throw new RuntimeException("sum = 0 !!");
		    	}
		    	double alea=Math.random()*sum;
		    	int chosen=-1;
		    	for(Integer z:pro.keySet()){
		    		if(chosen==-1){
		    			chosen=z;
		    		}
		    		Double proba=pro.get(z);
		    		alea-=proba;
		    		if(alea<0){
		    			chosen=z;
		    			break;
		    		}
		    	}
		    	//System.out.println(node+" => "+time+","+chosen);
		    	if(((time!=null) && (time<firstNewT)) && (chosen>1)){
		    	   Integer nba=nbActifsInit.get(user);	
		    	   nbActifsInit.put(user, nba-1);	
		    	}
		    	if(((time==null) || (time>=firstNewT)) && (chosen<firstNewT)){
			    	   Integer nba=nbActifsInit.get(user);	
			    	   nbActifsInit.put(user, nba+1);	
			    }
		    	for(Link l:succs.values()){
		    		Node w=l.getNode2();
		    		//String sw=w.getName();
		    		
		    		Double p=probasw.get(w).get(chosen);
		    		if(p==null){
		    			throw new RuntimeException("p pos "+w+" null !");
		    		}
		    		ptimespos.put(w, p);
		    		p=probaswnon.get(w).get(chosen);
		    		if(p==null){
		    			throw new RuntimeException("p neg "+w+" null !");
		    		}
		    		ptimesneg.put(w, p);
		    	}
		    		
		    	Double p=probas.get(chosen);
		    	ptimespos.put(node, (p==null)?1.0:p);
		    	p=probasnon.get(chosen);
		    	ptimesneg.put(node, (p==null)?1.0:p);
		    	changeTime(node,times,ctimes,(chosen==(maxIter+1))?null:chosen);
		    	//System.out.println(node+" => oldt "+time+" newt "+chosen);
	    		// modif exposed
		    	if(chosen==(maxIter+1)){
	    			if(time!=null){
	    				succs=node.getSuccesseurs();
	    		       	for(Link l:succs.values()){
	    		       		Node succ=l.getNode2();
	    		       		from=exposed.get(succ);
	    		       		if(from!=null){
	    		       			from.remove(node);
	    		       		}
	    		        	if(from.size()==0){
	    		        		exposed.remove(succ);
	    		        	}
	    		        }
	    			}
	    		}
	    		else{
	    			//if(time==null){
	    				succs=node.getSuccesseurs();
	    		       	for(Link l:succs.values()){
	    		        	Node succ=l.getNode2();
	    		        	from=exposed.get(succ);
	    		        	if(from==null){
	    		        		from=new HashMap<Node,Link>();
	    		        		exposed.put(succ, from);
	    		        	}
	    		        	from.put(node, l);
	    		        }
	    			//}
	    		}
		    }
	      }
	      if(i%10==0){
	    	  System.out.println(i+" ranked users : "+nbContamined.keySet().size());
	      }
	      
	      if(i>nbBurnOut){
	    	  HashMap<String,Integer> first=new HashMap<String,Integer>();
	    	  for(Node node:times.keySet()){
	    		  String user=this.userParents.get(node);
	    		  Integer f=first.get(user);
	    		  
	    		  /*HashMap<Integer,Integer> h=nbContamined.get(node);
	    		  if(h==null){
	    			  h=new HashMap<Integer,Integer>();
	    			  nbContamined.put(node,h);
	    		  }*/
	    		  Integer t=times.get(node);
	    		  if((f==null) || (t<f)){
	    			  first.put(user, t);
	    		  }
	    		  /*Integer nb=h.get(t);
	    		  nb=(nb==null)?1:(nb+1);
	    		  h.put(t,nb);*/
	    	  }
	    	  for(String user:first.keySet()){
	    		  Integer t=first.get(user);
	    		  HashMap<Integer,Integer> h=nbContamined.get(user);
	    		  if(h==null){
	    			  h=new HashMap<Integer,Integer>();
	    			  nbContamined.put(user, h);
	    		  }
	    		  Integer nb=h.get(t);
	    		  nb=(nb==null)?1:(nb+1);
	    		  h.put(t, nb);
	    	  }
	      }
	      //System.out.println(nbContamined);
	    	//Clavier.saisirLigne(" ");
	      
	    }
	    
	    //System.out.println(nbContamined);
	    //infections=new ArrayList<HashMap<String,Double>>();
	    
	    //ArrayList<HashMap<Node,Double>> infectNodes=new ArrayList<HashMap<Node,Double>>();
	    for(int i=0;i<maxIter;i++){
	    	infections.put((long)i+1,new HashMap<String,Double>());
	    	//infectNodes.add(new HashMap<Node,Double>());
	    }
	    int nbdiv=nbIt-nbBurnOut;
	    /*for(Node node:nbContamined.keySet()){	  
	    	HashMap<Integer,Integer> h=nbContamined.get(node);
	    	if(h!=null){
	    		for(Integer t:h.keySet()){
	    			int nb=h.get(t);
	    			for(int i=(t-1);i<maxIter;i++){
	    				HashMap<Node,Double> inf=infectNodes.get(i);
	    				Double n=inf.get(node);
	    				n=(n==null)?nb:(n+nb);
	    				inf.put(node, n);
	    			}
	    		}
	    	}
	      
	    }
	    for(int i=0;i<maxIter;i++){
	    	HashMap<Node,Double> inf=infectNodes.get(i);
	    	for(Node node:inf.keySet()){
	    		Double n=inf.get(node);
				if(n==null){
					continue;
				}
				
				inf.put(node, (1.0*n/nbdiv));
	    	}
	    }
	    for(String user:users){
	    	ArrayList<Node> nodes=userNodes.get(user);
	    	for(int i=0;i<maxIter;i++){
	    		HashMap<Node,Double> inf=infectNodes.get(i);
	    		double p=1.0;
	    		for(Node node:nodes){
	    		  Double pn=inf.get(node);
	    		  pn=(pn==null)?0.0:pn;
	    		  p*=(1.0-pn);
	    		}
	    		p=1.0-p;
	    		HashMap<String,Double> infect=infections.get(i);
	    		infect.put(user, p);
	    	}
	    }*/
	    for(String suser:nbContamined.keySet()){
	    	HashMap<Integer,Integer> h=nbContamined.get(suser);
	    	if(h!=null){
	    		for(Integer t:h.keySet()){
	    			int nb=h.get(t);
	    			//for(int i=(t-1);i<maxIter;i++){
	    				HashMap<String,Double> inf=infections.get(t);
	    				Double n=inf.get(suser);
	    				n=(n==null)?nb:(n+nb);
	    				inf.put(suser, n);
	    			//}
	    		}
	    	}
	    }
	    for(int i=0;i<maxIter;i++){
	    	HashMap<String,Double> inf=infections.get(i+1);
	    	for(String user:inf.keySet()){
	    		Double n=inf.get(user);
				if(n==null){
					continue;
				}
				
				inf.put(user, (1.0*n/nbdiv));
	    	}
	    }
	    //System.out.println(infections.get(infections.size()-1));
	    pstruct.setInfections(infections);
        return(0);
	}
   
	 
	private double getVal(Link l){
		double val=l.getVal();
		val*=0.98;
		val+=0.01;
		return(val);
	}
	
	private void changeTime(Node node,HashMap<Node,Integer> times,HashMap<Integer, HashSet<Node>> ctimes, Integer newTime){
		Integer time=times.get(node);
		if(time!=null){
			HashSet<Node> ct=ctimes.get(time);
			ct.remove(node);
		}
		if(newTime!=null){
			HashSet<Node> ct=ctimes.get(newTime);
			if(ct==null){
				ct=new HashSet<Node>();
				ctimes.put(newTime, ct);
			}
			ct.add(node);
			times.put(node,newTime);
		}
		else{
			times.remove(node);
		}
	}
	    
	private void reevalProba(Node node,HashSet<String> contaminated,HashMap<Node,Double> ptimespos,HashMap<Node,Double> ptimesneg,HashMap<Node,Integer> times,HashMap<Integer, HashSet<Node>> ctimes){
		reevalProba(node,contaminated,ptimespos,ptimesneg,times,ctimes,0,null,0.0,null, null);
	}
	    
	// mode = 0 => compute from scratch, mode==1 => reeval after setting user changed at time newTime
	private void reevalProba(Node node, HashSet<String> contaminated, HashMap<Node,Double> ptimespos,HashMap<Node,Double> ptimesneg,HashMap<Node,Integer> times,HashMap<Integer, HashSet<Node>> ctimes,int mode,Node changed,double valLink,Integer oldTime, Integer newTime){
		boolean init=false;
		if(contaminated.contains(this.userParents.get(node))){
			init=true;
		}
		//String suser=user.getName();
    	Double ptimepos=1.0;
    	Double ptimeneg=1.0;
    	
    	//Integer oldTime=null; 
    	Integer time=times.get(node);
    	//System.out.println("eval "+node+" "+time);
    	//oldTime=
    	if(mode!=0){
    		oldTime=times.get(changed);
    		if(oldTime==newTime){
    			return;
    		}
    		ptimepos=ptimespos.get(node);
    		ptimeneg=ptimesneg.get(node);
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
    		HashMap<String,Link> preds=node.getPredecesseurs();
			for(Link l:preds.values()){
				Node v=l.getNode1();
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
					double val=getVal(l);
					if(time==(tv+1)){
						ptimepos*=(1.0-val);
						//System.out.println(node+" ptimepos *= "+(1.0-val)+" pour "+v);
					}
					else{
						ptimeneg*=(1.0-val);
						//System.out.println(node+" ptimeneg *= "+(1.0-val)+" pour "+v);
					}
					
				}
			}
			
			
			if(init){
	    		/*int x=node.getName().lastIndexOf("_");
	    		int num=Integer.parseInt(node.getName().substring(x+1));*/
	    		//if(num==1){
	    			if(time==1){
	    				ptimepos*=0.5;
	    			}
	    			ptimeneg*=0.5;
	    		/*}
	    		else{
	    			if(time==1){
	    				ptimepos*=0.9;
	    			}
	    			ptimeneg*=0.9;
	    		}*/
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
    		ptimespos.put(node, 1.0-ptimepos);
    	//}
    	ptimesneg.put(node, ptimeneg);
    	//System.out.println("changed "+changed+" oldt "+oldTime+" newt "+newTime+" t "+time+" node "+node+" "+(1.0-ptimepos)+" "+ptimeneg);
	}
   
    public void load(){
		String filename=modelFile;
        User.reinitAllLinks();
        BufferedReader r;
        boolean linkMode=true;
        userNodes=new HashMap<String,ArrayList<Node>>();
        pactifTrain=new HashMap<Integer,HashMap<Node,Double>>();
        proportionNodes=new HashMap<String,HashMap<Node,Double>>();
        HashMap<String,Node> nodes=new HashMap<String,Node>();
        HashMap<Node,String> parents=new HashMap<Node,String>();
        boolean proportionMode=false;
        boolean actifsMode=false;
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          while((line=r.readLine()) != null) {
        	if(line.startsWith("LearnedFrom")){
        		String[] tokens = line.split("\t") ;
        		if(tokens.length!=3){
	        		System.out.println("problem for loading model, line wrongly formatted : "+line);
	        	}
	        	else{
	        		this.learnedFromDb=tokens[1];
	        		this.learnedFromCol=tokens[2];
	        		
	        	}
        		continue;
        	}
          	if(line.contains("<Links>")){
                  continue;
          	}
          	if(line.contains("</Links>")){
                  linkMode=false;
          		  continue;
          	}
          	if(line.contains("<Proportion>")){
          		proportionMode=true;
          		continue;
          	}
          	if(line.contains("</Proportion>")){
          		proportionMode=false;
          		continue;
          	}
          	if(line.contains("<Actifs Train>")){
          		actifsMode=true;
          		continue;
          	}
          	if(line.contains("</Actifs Train>")){
          		break;
          	}
        	String[] tokens = line.split("\t") ;
        	if(linkMode){
	        	if(tokens.length!=4){
	        		System.out.println("problem for loading model, line wrongly formatted : "+line);
	        	}
	        	else{
	        		Node n=nodes.get(tokens[1]);
	        		if(n==null){
	        			n=new Node(tokens[1]);
	        			nodes.put(tokens[1],n);
	        			parents.put(n,tokens[0]);
	        		}
	        		
	        		Node n2=nodes.get(tokens[2]);
	        		
	        		if(n2==null){
	        			n2=new Node(tokens[2]);
	        			nodes.put(tokens[2],n2);
	        			//if(parents.get(n2)==null){
		        			String name=n2.getName();
		        			int j=name.indexOf("_");
		        			String us=name.substring(j+1);
		        			//System.out.println(n2+" => "+us);
		        			j=us.lastIndexOf("_");
		        			us=us.substring(0,j);
		        			//System.out.println(n2+" => "+us);
		        			
		        			parents.put(n2,us);
		        		//}
	        		}
	        		//HashMap<String,Link> succs=n.getSuccesseurs();
	        		Double val=Double.valueOf(tokens[3]);
	        		if(val>0.999){
	        			val=0.999;
	        			//System.out.println("replace "+val);
	        		}
	        		if(val>0.0){
	        			Link l=new Link(n,n2,val);
	        			n.addLink(l, true);
	        		}
	        	}
        	}
        	else{
        	 if(proportionMode){
        		 if(tokens.length!=2){
 	        		System.out.println("problem for loading model, line wrongly formatted : "+line);
 	        	}
 	        	else{
 	        		//System.out.println(line);
 	        		Node n=nodes.get(tokens[0]);
	        		 if(n==null){
	        			 n=new Node(tokens[0]);
	        			 nodes.put(tokens[0], n);
	        			 String name=n.getName();
		        		 int j=name.indexOf("_");
		        		 String us=name.substring(j+1);
		        		 //System.out.println(n+" => "+us);
		        		 j=us.lastIndexOf("_");
		        		 us=us.substring(0,j);
		        		 //System.out.println(n2+" => "+us);
		        		 parents.put(n,us);
	        		 }
	        		 String parent=parents.get(n);
	        		 Double val=Double.valueOf(tokens[1]);
	        		 HashMap<Node,Double> prop=this.proportionNodes.get(parent);
	        		 if (prop==null){
	        			 prop=new HashMap<Node,Double>();
	        			 proportionNodes.put(parent, prop);
	        		 }
	        		 prop.put(n, val);
	        		 
 	        	}
        	 }
        	 else{
        		if(tokens.length!=3){
	        		System.out.println("problem for loading model, line wrongly formatted : "+line);
	        	}
	        	else{
	        		 int c=Integer.parseInt(tokens[0]);
	        		 Node n=nodes.get(tokens[1]);
	        		 if(n==null){
	        			 n=new Node(tokens[1]);
	        			 nodes.put(tokens[1], n);
	        			 String name=n.getName();
		        		 int j=name.indexOf("_");
		        		 String us=name.substring(j+1);
		        		 //System.out.println(n2+" => "+us);
		        		 j=us.lastIndexOf("_");
		        		 us=us.substring(0,j);
		        		 //System.out.println(n2+" => "+us);
		        		 parents.put(n,us);
	        		 }
	        		 Double val=Double.valueOf(tokens[2]);
	        		 HashMap<Node,Double> h=pactifTrain.get(c);
	        		 if(h==null){
	        			 h=new HashMap<Node,Double>();
	        			 pactifTrain.put(c,h);	
	        		 }
	        		 h.put(n, val);
	        	}
        	   }
        	}
          }
          
          r.close();
        }
        catch(IOException e){
        	System.out.println("Probleme lecture modele "+filename);
        }
        
        for(Node node:nodes.values()){
        	String user=parents.get(node);
        	ArrayList<Node> list=userNodes.get(user);
        	if(list==null){
        		list=new ArrayList<Node>();
        		userNodes.put(user,list);
        	}
        	list.add(node);
        }
        userParents=parents;
        loaded=true;
    }

    public void save() {
		String filename=modelFile;
        try{
          PrintStream p = new PrintStream(filename) ;
          p.println("LearnedFrom\t"+this.learnedFromDb+"\t"+this.learnedFromCol);
          p.println("<Links>");
          for(String user : userNodes.keySet()) {
            ArrayList<Node> nodes=userNodes.get(user);
            for(Node node:nodes){
            	HashMap<String,Link> succs=node.getSuccesseurs();
            	for(String n2:succs.keySet()){
            		Link l=succs.get(n2);
            		double val=l.getVal();
            		p.println(user+"\t"+node.getName()+"\t"+n2+"\t"+val);
            	}
            }
          }
          p.println("</Links>");
          p.println("<Proportion>");
          for(String user : proportionNodes.keySet()) {
        	  HashMap<Node,Double> nodes=proportionNodes.get(user);
        	  for(Node node:nodes.keySet()){
        		  double x=nodes.get(node);
        		  p.println(node.toString()+"\t"+x);
        	  }
          }
          p.println("</Proportion>");
          p.println("<Actifs Train>");
          for(Integer c:pactifTrain.keySet()){
        	HashMap<Node,Double> h=pactifTrain.get(c);
        	for(Node node:h.keySet()){
        		p.println(c+"\t"+node.toString()+"\t"+h.get(node));
        	}
          }
          p.println("</Actifs Train>");
        }
        catch(IOException e){
        	System.out.println("Probleme sauvegarde modele "+filename);
        	
        }
    }
   
   
    
    
   // retourne une table cascade_id,user => time contamination
   // zapVides indique si on compte compte les timesteps sans posts dans une cascade donnee pour numeroter les temps de contamination (true=> on ne les compte pas) 
 	public static HashMap<Integer,HashMap<User,Long>> getTimeSteps(String db, String collection,int step,boolean zapVides){
         HashMap<Integer,HashMap<User,Long>> userTimeContamination=new HashMap<Integer,HashMap<User,Long>>();
         DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
         DBCursor cursor = col.find();
         Post p=null;
         try {
             while(cursor.hasNext()) {
                 DBObject res=cursor.next();
                 Cascade c=Cascade.getCascadeFrom(res);
                 int ic=c.getID();
                
                 HashMap<User,Long> ihc=new HashMap<User,Long>();
                 userTimeContamination.put(c.getID(),ihc);
                 HashMap<User,Long> hc=c.getUserContaminationsSteps(-1,step);
                 if (!zapVides){
                	 for(User u:hc.keySet()){
                         Long t=hc.get(u);
                         ihc.put(u, t);
                     }
                 }
                 else{
                	 long maxt=0;
                	 HashMap<Long,HashSet<User>> ht=new HashMap<Long,HashSet<User>>();
                	 for(User u:hc.keySet()){
                		 Long t=hc.get(u);
                		 if (t>maxt){
                			 maxt=t;
                		 }
                		 User iu=u;
                		 HashSet<User> tu=ht.get(t);
                		 if (tu==null){
                			 tu=new HashSet<User>();
                			 ht.put(t, tu);
                		 }
                		 tu.add(iu);
                	 }
                	 //System.out.println("cascade "+ic+" max = "+maxt);
                	 long ste=1;
                	 for(long t=0;t<=maxt;t++){
                		 HashSet<User> tu=ht.get(t);
                		 if (tu!=null){
                			 //System.out.println("time "+t+" nb "+tu.size());
                			 for(User iu:tu){
                				 ihc.put(iu, ste);
                			 }
                			 ste++;
                		 }
                	 }
                 }
                 User.reinitAllPosts(); // Pour alleger, on supprime les textes des posts dont on ne se sert pas ici
                 Post.reinitPosts();
                 System.out.println("Cascade "+ic+" chargee");
                 //System.out.println(ihc);
             }
         } finally {
             cursor.close();
         }
         return(userTimeContamination);
        
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
   
 	
 
   
  	// links must have at least minSposSize positive examples to be considered
    // contaMaxDelay = false => propagation can only be done between two users contamined in contiguous steps (as defined in the saito paper). contaMaxDelay=true => a user can have contamined every user contamined after him  
    // only_one=true => only one active node per user
  	// considerPastNodes=true => nodes should not be activated after their user has been contaminated 
  	public void learn(String db, String cascadesCollection,String usersCollection,int nbSubNodes, int maxIter, int step, double userLinkThreshold, double min_init, double max_init, boolean zapVides, int nbrelab, int nbThreads, double minSposSize, boolean infiniteDelay, boolean only_one, boolean considerPastNodes) {
        learnedFromDb=db;
        learnedFromCol=cascadesCollection;
    	wq=new WorkQueue(nbThreads);
    	boolean displayLikelihood=true;
       
        // Pour chaque cascade, liste id users avec leur temps de contamination
        HashMap<Integer,HashMap<User,Long>> userTimeContamination; 
        if(testMode){
        	userTimeContamination=getTimeStepsTest(db,cascadesCollection,step,zapVides);
        }
        else{
        	userTimeContamination=getTimeSteps(db,cascadesCollection,step,zapVides);
        }
        
        // Pour chaque cascade, temps de contamination associes aux ids de users contamines a ce moment la
        HashMap<Integer,HashMap<Long,HashSet<User>>> times=new HashMap<Integer,HashMap<Long,HashSet<User>>>(); 
        
     // Table de sommes de Probabilites de contamination pour chaque utilisateur selon chacun de ses noeuds, indexee par cascade id 
        HashMap<Integer,HashSet<Node>> actifs=new HashMap<Integer,HashSet<Node>>();
        
         
        System.out.println("Reperage cascades pour chaque user");
        HashMap<User,HashSet<Integer>> cascades=new HashMap<User,HashSet<Integer>>();
        for(Integer c:userTimeContamination.keySet()) {
        	HashSet<Node> actifsc=new HashSet<Node>();
        	actifs.put(c, actifsc);
        	
        	HashMap<User, Long> hc = userTimeContamination.get(c);
        	HashMap<Long, HashSet<User>> ct = times.get(c);
        	if (ct==null){
        		ct=new HashMap<Long,HashSet<User>>();
        		times.put(c, ct);
        	}
    		for(User w:hc.keySet()){
    			HashSet<Integer> casc=cascades.get(w);
    			if (casc==null){
    				casc=new HashSet<Integer>();
    				cascades.put(w, casc);
    			}
    			casc.add(c);
    			
    			Long t=hc.get(w);
    			HashSet<User> ctw=ct.get(t);
    			if (ctw==null){
    				ctw=new HashSet<User>();
    				ct.put(t, ctw);
    			}
    			ctw.add(w);
    		}
        }
        
        
        HashSet<User> users=new HashSet<User>(cascades.keySet());
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
        }
        
        //users=new HashSet<User>(User.users.values());
       
      
        HashMap<User,ArrayList<Node>> subNodes=new HashMap<User,ArrayList<Node>>();
        HashMap<Node,User> parents=new HashMap<Node,User>();
     
        
        
     
        
        // Pour tous les utilisateurs on cree nbSubNodes sub nodes 
        for(User u:users){
        	int nNodes=0;
        	//User iu=u;
        	ArrayList<Node> nodes=new ArrayList<Node>();
        	HashSet<Integer> cas=cascades.get(u);
        	int nnodes=nbSubNodes;
        	if(cas.size()<=1){
        		nnodes=1;
        	}
        	for(int i=0;i<nnodes;i++){
        		nNodes++;
        		Node node=new Node("node_"+u.getName()+"_"+nNodes);
        		nodes.add(node);
        		parents.put(node, u);
        		/*if(cas!=null){
        		for(Integer c:cas){
        			HashSet<Node> actifsc=actifs.get(c);
        			actifsc.add(node);
        		}
        		}*/
        		
        	}
        	if(cas!=null){
        		for(Integer c:cas){
        			int x=(int)(Math.random()*nodes.size());
        			HashSet<Node> actifsc=actifs.get(c);
        			actifsc.add(nodes.get(x));
        		}
        	}
        	subNodes.put(u, nodes);
        }
        
         // Creation liens entre nodes
        System.out.println("Creation liens");
        // Table des cascades pour chaque lien <v,w> ou time de v == time de w - 1
        HashMap<Link,HashSet<Integer>> spos=new HashMap<Link,HashSet<Integer>>();
        // Table des cascades pour chaque lien <v,w> ou time de w n appartient pas a C(tv+1)
        //HashMap<Link<User>,HashSet<Integer>> sneg=new HashMap<Link<User>,HashSet<Integer>>();
        HashMap<Link,HashSet<Integer>> sneg=new HashMap<Link,HashSet<Integer>>();
         
        int nbLinks=0;
        
        double dif=max_init-min_init;
        for(User u1:cascades.keySet()){
        	System.out.println("User "+u1.getName());
            //User u=User.getUser(iu1);
        	HashSet<Integer> c1=cascades.get(u1);
            HashMap<String,Link> succs=u1.getSuccesseurs();
            //HashSet<Integer> posSuccs=new HashSet<Integer>();
            for(Integer c:c1){
            	HashMap<User, Long> hc = userTimeContamination.get(c);
            	Long t1=hc.get(u1);
            	for(Link l:succs.values()){
            		User u2=(User)l.getNode2();
            		Long t2=hc.get(u2);
                	//if ((t2==null) || (t2>(t1+1))){
                	//if ((t2==null) || ((infiniteDelay>=0) && (t2>(t1+infiniteDelay)))){
                	if ((t2==null) || ((!infiniteDelay) && (t2>(t1+1)))){
                	                
                		HashSet<Integer> cneg=sneg.get(l);
                        if (cneg==null){
                            cneg=new HashSet<Integer>();
                            sneg.put(l, cneg);
                        }
                        cneg.add(c);
                	}
                	else{

                    	//if (((infiniteDelay>=0) && (t2>t1) && (t2<=(t1+infiniteDelay))) || ((infiniteDelay<0) && (t2>t1))){
                		if (((!infiniteDelay) && (t2==(t1+1))) || ((infiniteDelay) && (t2>t1))){
                            
                		//if ((t2==(t1+1))){ // || (t2==(t1))){
                			HashSet<Integer> cpos=spos.get(l);
                			if (cpos==null){
                				cpos=new HashSet<Integer>();
                				spos.put(l, cpos);
                			}
                			cpos.add(c);
                		}
                		
                	}
                }
            }
            ArrayList<Node> ln1=subNodes.get(u1);
            HashSet<Link> asup=new HashSet<Link>();
            for(Link l:succs.values()){
        		HashSet<Integer> cpos=spos.get(l);
            	if ((cpos!=null) && (cpos.size()>=minSposSize)){ //spos.containsKey(l)){
            		nbLinks++;
        			//allLinks.add(l);
        			User u2=(User)l.getNode2();
        			ArrayList<Node> ln2=subNodes.get(u2);
                    for(int i=0;i<ln1.size();i++){
                    	Node n1=ln1.get(i);
                    	for(int j=0;j<ln2.size();j++){
                    		Node n2=ln2.get(j);
                    		/*HashMap<Integer,Double> poids=new HashMap<Integer,Double>();
                    		for(Integer z:spos.get(l)){
                    			double v=(Math.random()*dif)+min_init;
                    			poids.put(z, v);
                    		}*/
                    		double v=(Math.random()*dif)+min_init;
                    		Link ln=new Link(n1,n2,v);
                    		//ln.setWeights(poids);
                    		n1.addLink(ln);
                    		n2.addLink(ln);
                    		//System.out.println("link "+ln);
                    	}
                    }
        		}
        		else{
        			sneg.remove(l);
        			spos.remove(l);
        			asup.add(l);
        		}
            }
            for(Link l:asup){
            	User u2=(User)l.getNode2();
            	u1.removeSuccesseur(u2, true);
            }
            //u1.reinitLinks(true);
        }
        System.out.println("nbLinks "+nbLinks);
         
        // Table de Probabilite de contamination pour chaque node, indexee par cascade id 
        HashMap<Integer,HashMap<Node,Double>> Pi=new HashMap<Integer,HashMap<Node,Double>>();
            
        
        // Table des utilisateurs n ayant pas de predecesseur direct pour chacune des cascades
        HashMap<Integer,HashSet<User>> sansPreds=new HashMap<Integer,HashSet<User>>();
        System.out.println("Calcul sans preds");
        for(Integer c:userTimeContamination.keySet()){
        	HashMap<User, Long> hc = userTimeContamination.get(c);
        	HashSet<User> sp=new HashSet<User>();
        	sansPreds.put(c, sp);
        	for(User w:hc.keySet()){
        		HashMap<String,Link> preds=w.getPredecesseurs();
                boolean sans=true;
                for(Link l:preds.values()){
                	HashSet<Integer> cpos=spos.get(l);
                	if ((cpos!=null) && (cpos.contains(c))){
                		//System.out.println(l);
                		sans=false;
                		break;
                	}
                }
                if (sans){
                	System.out.println("Cascade "+c+" : "+w+" sans preds");
                	sp.add(w);
                	/*HashMap<Node,Double> pic=Pi.get(c);
                	if (pic==null){
                		pic=new HashMap<Node,Double>();
                		Pi.put(c,pic);
                	}
                	ArrayList<Node> snodes=subNodes.get(w);
                	int io=0;*/
                	
                }
        	}
        }
        
        
        // pour repartir d'un modele pre appris
        if(loaded){
        	System.out.println(userNodes);
        	subNodes=new HashMap<User,ArrayList<Node>>();
        	parents=new HashMap<Node,User>();
        	int maxNb=0;
        	for(String user:userNodes.keySet()){
        		System.out.println(user);
        		ArrayList<Node> nodes=userNodes.get(user);
        		User us=User.getUser(user);
        		subNodes.put(us, nodes);
        		for(Node n:nodes){
        			parents.put(n, us);
        			HashMap<String,Link> succs=n.getSuccesseurs();
        			for(Link l:succs.values()){
        				Double v=l.getVal();
        				if(v>0.9){
        					l.setVal(0.9);
        				}
        			}
        		}
        		if(nodes.size()>maxNb){
        			maxNb=nodes.size();
        		}
        	}
        	if(nbSubNodes>maxNb){
        		for(String user:userNodes.keySet()){
        			ArrayList<Node> nodes=userNodes.get(user);
        			Node un=nodes.get(0);
        			for(int i=maxNb;i<nbSubNodes;i++){
        				Node nouv=new Node("node_"+user+"_"+(i+1));
        				nodes.add(nouv);
        				parents.put(nouv, User.getUser(user));
        				HashMap<String,Link> links=un.getSuccesseurs();
        				for(Link l:links.values()){
        					Node n2=l.getNode2();
        					User u2=parents.get(n2);
        					ArrayList<Node> unodes=subNodes.get(u2);
        					for(Node nu:unodes){
        						Link nl=new Link(nouv,nu,0.1);
        						nouv.addLink(nl, true);
        					}
        				}
        				links=un.getPredecesseurs();
        				for(Link l:links.values()){
        					Node n2=l.getNode1();
        					User u2=parents.get(n2);
        					ArrayList<Node> unodes=subNodes.get(u2);
        					for(Node nu:unodes){
        						Link nl=new Link(nu,nouv,0.1);
        						nouv.addLink(nl, true);
        					}
        				}
        			}
        		}
        	}
        	actifs=new HashMap<Integer,HashSet<Node>>();
        	for(Integer c:this.pactifTrain.keySet()){
        		HashSet<Node> actifsc=new HashSet<Node>();
        		actifs.put(c, actifsc);
        		HashMap<Node,Double> pact=pactifTrain.get(c);
        		HashMap<User,Node> maxn=new HashMap<User,Node>();
        		HashMap<User,Double> max=new HashMap<User,Double>();
        		//HashMap<User,Integer> nbNodes=new HashMap<User,Integer>();
        		for(Node n:pact.keySet()){
        			double d=pact.get(n);
        			User p=parents.get(n);
        			Double m=max.get(p);
        			m=(m==null)?0.0:m;
        			if(d>m){
        				max.put(p, d);
        				maxn.put(p, n);
        			}
        			/*Integer nb=nbNodes.get(p);
        			nb=(nb==null)?1:(nb+1);
        			nbNodes.put(p, nb);*/
        		}
        		for(User user:maxn.keySet()){
        			Node n=maxn.get(user);
        			actifsc.add(n);
        		}
        	}
        }
        HashMap<User,HashMap<Node,Double>> proportion=new HashMap<User,HashMap<Node,Double>>();
        calculProportions(cascades,actifs,subNodes,proportion);
        //System.out.println(actifs);
        //relabelActifs(actifs, subNodes, parents, times, sansPreds,userTimeContamination,Pi);
        //System.out.println("compute probas");
        double likelihood=computeProbaNodes(Pi,actifs,subNodes,parents,times,sansPreds,true,infiniteDelay,considerPastNodes);
        System.out.println("likelihood = "+likelihood);
        /*System.out.println(times);
        System.out.println(actifs);
        System.out.println(Pi);*/
        ArrayList<User> usersList=new ArrayList<User>(users);
        boolean go=true;
        boolean last=false;;
        int pass=0;
        while(go){
        	pass++;
        	
        	System.out.println("pass "+pass);
        	//if(pass>1){
        	for(int i=0;i<1;i++){
        		relabelActifs(actifs, proportion, subNodes, parents, times, sansPreds,userTimeContamination,cascades,Pi,nbrelab,infiniteDelay,only_one,considerPastNodes);
        	}
        	//}
        	/*if(pass>1000000){
        		last=true;
        	}*/
        	File f=new File("./stop");
        	if(f.exists()){
        		last=true;
        	}
        	if(last){
        		go=false;
        	}
        	//last=false;
	        double oldl=likelihood;
	        //System.out.println(last);
	        int nbit=maxIter;
	        if(last){
	        	nbit+=100;
	        }
	        for(int iteration = 0 ; iteration<nbit ; iteration++) {
	        	//System.out.println("Compute probas");
	        	
	        	
	            Collections.shuffle(usersList);
	            for(User v:usersList){
	            	//User uv=User.getUser(v);
	            	HashMap<String,Link> succs=v.getSuccesseurs();
	            	//HashSet<Integer> casc=cascades.get(v);
	            	ArrayList<Node> vnodes=subNodes.get(v);
	            	Collections.shuffle(vnodes);
	            	for(Link l:succs.values()){
	            		User w=(User)l.getNode2();
	            		//int w=uw.getID();
	            		ArrayList<Node> wnodes=subNodes.get(w);
	            		HashSet<Integer> cpos=spos.get(l);
	            		HashSet<Integer> cneg=sneg.get(l);
	            		//cneg=(cneg==null)?0:cneg;
	            		if ((cpos==null) || (cpos.size()==0)){
	            			// Aucune cascade poositive pour ce couple d'utilisateurs, on le passe
	            			continue;
	            		}
	            		
	            		for(Node nv:vnodes){
	            			int nbNeg=0;
	            			if(cneg!=null){
	            				for(Integer c:cneg){
	            					HashSet<Node> actifsc=actifs.get(c);
	            					if(actifsc.contains(nv)){ 
	            						nbNeg++;
	            					}
	            				}
	            			}
	            			
	            			boolean ok=false;
	            			HashMap<String,Link> nsuccs=nv.getSuccesseurs();
	            			for(Node nw:wnodes){
	            				Link ln=nsuccs.get(nw.getName());
	            				if (ln==null){
	            					throw new RuntimeException("Pas de lien entre "+nv+" et "+nw+"!!");
	            				}
	            				double numer=ln.getVal();
	            				//System.out.println(numer);
	            				double sumNumer=0.0;
	            				double sumDenomPos=0.0;
	            				double sumDenomNeg=nbNeg;
	            				HashSet<Integer> casv=new HashSet<Integer>();
	            				HashSet<Integer> cset=cpos;
	            				if(considerPastNodes) cset=cascades.get(v);
	            				for(Integer c:cset){
	            					if((cneg!=null) && (cneg.contains(c))){
	            						continue;
	            					}
	            					
	            					
	            					HashSet<Node> actifsc=actifs.get(c);
	            					if(!actifsc.contains(nv)){ 
	            						continue;
	            					}
	            					else{ok=true;}
	            					boolean actif=false;
	            					if(actifsc.contains(nw)){
	            						//System.out.println("ok "+nw);
	            						actif=true;
	            					}
	            					if(!cpos.contains(c)){
	            						if(!actif){
	            							sumDenomNeg++;
	            						}
	            						continue;
	            					}
	            					
	            					
	            					//System.out.println("ok "+nv);
	            					HashMap<Node,Double> pic=Pi.get(c);
	            					if (pic==null){
	            						throw new RuntimeException("Pas de probas pour noeuds de cascade "+c+"!!");
	            					}
	            					
	            					
	            					
	            					if (actif){
	            						casv.add(c);
	            						//System.out.println("ok "+nw);
	            						//sumNumer+=pcwj*snumer;
	            						Double pcwj=pic.get(nw);
	            						try{ 
	            						  if(pcwj<Double.MIN_VALUE){
	            						    	pcwj=Double.MIN_VALUE; //throw new RuntimeException("Pcwj = "+pcwj);
	            						  }
	            						}
	            						catch(Exception e){
	            							throw new RuntimeException(e+"\n Pas de score pcwj pour "+nw+" alors que "+nv+" pointe sur lui ("+numer+") => "+sansPreds.get(c).contains(nw));
	            						}
	            						if(Double.isNaN(pcwj)){
	            							throw new RuntimeException("Pcwj = "+pcwj);
	            							//pcwj=Double.MIN_VALUE;
	            						}
	            						double x=(numer/pcwj);
	            						if(Double.isInfinite(x)){
		            						x=1.0;
		            						throw new RuntimeException("ifinity Pcwj = "+pcwj+" numer "+numer);
		            					}
	            						if(x>1.0001){
		            						x=1.0;
		            						//throw new RuntimeException(x+" Pcwj = "+pcwj+" numer "+numer+" "+nv+" "+nw+" "+c+"\n"+times+"\n"+actifs+"\n"+Pi+"\n"+P+"\n LOG ================================ > \n"+Log.log);
		            						
	            						}
	            						
	            						sumNumer+=x;
	            						sumDenomPos++;
	            						//sumDenomNeg+=(1.0-pcwj);
	            					}
	            					else{
	            						sumDenomNeg++;
	            					}
	            					
	            				}
	            				
	                			
	            				double oldk=numer;
	            				
	            				double sumDenom=(sumDenomNeg+sumDenomPos);
	            				if (sumDenom<Double.MIN_VALUE){
	        						sumDenom=Double.MIN_VALUE;
	        					}
	            				double kviwj=sumNumer/sumDenom;
	            				if(Double.isNaN(kviwj)){
	            					//numer=1.0-Double.MIN_VALUE;
	            					throw new RuntimeException("Nan :"+numer+" "+sumNumer+" "+sumDenom+" pour nv "+nv+" et nw"+nw+"\n"+actifs);
		            				
	            				}
	            				if(Double.isInfinite(kviwj)){
	            					//numer=1.0-Double.MIN_VALUE;
	            					throw new RuntimeException("Infinite :"+numer+" "+sumNumer+" "+sumDenom+" pour nv "+nv+" et nw"+nw+"\n"+actifs);
		            				
	            				}
	            				if(kviwj>1){
	            					if(kviwj>1.01){
	            						throw new RuntimeException(kviwj+";"+numer+";"+sumDenomNeg+";"+sumDenomPos);
	            					}
	            					kviwj=1.0;
	        					}
	            				if(!last){
	            					kviwj=modifVal(kviwj,pass);
	            				}
	            				ln.setVal(kviwj); 
	            				if(casv.size()>0){
	            			    	majPi(oldk,kviwj,Pi,nw,casv,actifs,subNodes,parents,times,userTimeContamination,infiniteDelay);
	            			    }
	            				
	            	        	
	            			}
	            		}
	            	}
	            }	
	            
	            
	            boolean computeLike=true;
	            if((pass+iteration)%10==0){
	            	likelihood=computeProbaNodes(Pi,actifs,subNodes,parents,times,sansPreds,computeLike,infiniteDelay,considerPastNodes);
	            	if (computeLike){
		             	System.out.println("Pass "+pass+" Iteration "+iteration+" Likelihood = "+likelihood);
		            }
	            	if(testMode){
		            	System.out.println(actifs);
		            }
	            }
	            
            	
	           
	        }
	        //last=!removeActif(P,Pi,Pip,sumPi,actifs,tropactifs,subNodes,parents,times,sansPreds);
	        //changeOne(P,Pi,Pip,sumPi,actifs,subNodes,parents,times,sansPreds);
	        //System.out.println("likelihood apres optimisation "+likelihood);
	      
	        
        }
        
        proportionNodes=new HashMap<String,HashMap<Node,Double>>();
        for(User user:proportion.keySet()){
        	HashMap<Node,Double> h=new HashMap<Node,Double>();
        	HashMap<Node,Double> prop=proportion.get(user);
        	for(Node node:prop.keySet()){
        		double x=prop.get(node);
        		h.put(node, x);
        	}
        	proportionNodes.put(user.getName(), h);
        }
        HashMap<Integer,HashMap<Node,Double>> pact=new HashMap<Integer,HashMap<Node,Double>>();
        HashMap<Integer,Integer> nbRelabelled=new HashMap<Integer,Integer>(); 
        int i=1;
        for(Integer c:actifs.keySet()){
        	nbRelabelled.put(c,1);
        	HashMap<Node,Double> h=new HashMap<Node,Double>();
        	pact.put(c, h);
        	for(Node node:actifs.get(c)){
        		h.put(node, 1.0);
        	}
        }
        boolean relabel=true;
        while(relabel){
        	File f=new File("./stop");
        	if(!f.exists()){
        		break;
        	}
        	System.out.println("nb relabs = "+i);
        	HashSet<Integer> relab=relabelActifs(actifs, proportion, subNodes, parents, times, sansPreds,userTimeContamination, cascades, Pi,nbrelab,infiniteDelay,only_one,considerPastNodes);
        	for(Integer c:relab){
        		Integer nbr=nbRelabelled.get(c);
        		nbRelabelled.put(c,nbr+1);
        		HashMap<Node,Double> h=pact.get(c);
        		for(Node node:actifs.get(c)){
            		Double nb=h.get(node);
            		nb=(nb==null)?1.0:(nb+1.0);
        			h.put(node, nb);
            	}
        	}
        	
        	for(User user:proportion.keySet()){
            	HashMap<Node,Double> h=proportionNodes.get(user.getName());
            	HashMap<Node,Double> prop=proportion.get(user);
            	for(Node node:prop.keySet()){
            		double x=prop.get(node);
            		Double old=h.get(node);
            		old=(old==null)?0.0:old;
            		h.put(node, old+x);
            	}
            }
        	
        	i++;
        }
        for(Integer c:actifs.keySet()){
        	Integer nbr=nbRelabelled.get(c);
        	HashMap<Node,Double> h=pact.get(c);
        	for(Node node:h.keySet()){
        		Double nb=h.get(node);
        		nb/=(1.0*nbr);
        		h.put(node, nb);
        	}
        }
        for(String user:proportionNodes.keySet()){
        	HashMap<Node,Double> h=proportionNodes.get(user);
        	for(Node node:h.keySet()){
        		Double old=h.get(node);
        		old=(old==null)?0.0:old;
        		h.put(node, old/(i*1.0));
        	}
        }
        pactifTrain=pact;
        likelihood=computeProbaNodes(Pi,actifs,subNodes,parents,times,sansPreds,true,infiniteDelay,considerPastNodes);
        System.out.println("Likelihood = "+likelihood);
        /*System.out.println(times);
        System.out.println(actifs);
        System.out.println(Pi);
        */
        wq.stop();
        
        
        
        
        
        userNodes=new HashMap<String,ArrayList<Node>>();
        for(User user:subNodes.keySet()){
        	userNodes.put(user.getName(), subNodes.get(user));
        }
        if ((modelFile.length()==0) || (loaded)){
    		modelFile="propagationModels/ICLSN_nbNodes="+nbSubNodes+"_step="+step+"_cascades="+cascadesCollection+"_users="+usersCollection+"_linkThreshold="+userLinkThreshold+"_maxIter="+maxIter+((zapVides)?"_sansStepsVides":"")+((infiniteDelay)?"_infiniteDelay":"")+((only_one)?"_onlyOne":"")+((considerPastNodes)?"_considerPastNodes":"");
    	}
        loaded=true;
    }
   
    public void majPi(double oldV,double newV,HashMap<Integer,HashMap<Node,Double>> pi,Node n,HashSet<Integer> cascades, HashMap<Integer,HashSet<Node>> actifs, HashMap<User,ArrayList<Node>> subNodes, HashMap<Node,User> parents, HashMap<Integer,HashMap<Long,HashSet<User>>> times, HashMap<Integer,HashMap<User,Long>> contaC, boolean infiniteDelay){
    	//System.out.println("majPi "+n+" oldV="+oldV+" newV="+newV);
    	for(Integer c:cascades){
    		HashMap<Node,Double> pic=pi.get(c);
    		HashSet<Node> actifsC=actifs.get(c);
    		Double pn=pic.get(n);
    		//System.out.println("Cascade "+c+" old pn ="+pn);
    		if (pn==null) pn=0.0;
    		double p0n=1.0-pn;
    		if(p0n<=Double.MIN_VALUE){
    			//System.out.println("arrondi "+n);
    			// il y a eu un arrondi, il faut recalculer pn
    			User w=parents.get(n);
    			Long t=contaC.get(c).get(w);
    			HashMap<Long,HashSet<User>> tc=times.get(c);
    			HashSet<User> vusers=new HashSet<User>();
    			Long tv=(long)1;
    			while(true){
    				Long tt=t-tv;
    				if(tt<1){
    					break;
    				}
    				HashSet<User> us=tc.get(tt);
    				if(us!=null){
    					vusers.addAll(tc.get(tt));
    				}
    				tv++;
    				//if((infiniteDelay>=0) && (tv>infiniteDelay)){
    				if((!infiniteDelay) && (tv>1)){
        				
    				  break;
    				}
    			}
    			
    			HashMap<String,Link> preds=n.getPredecesseurs();
    			pn=1.0;
    			for(Link l:preds.values()){
    				Node nv=l.getNode1();
    				if(!actifsC.contains(nv)){
    					continue;
    				}
    				User v=parents.get(nv);
    				if(!vusers.contains(v)){
    					continue;
    				}
    				double val=l.getVal();
    				pn*=(1.0-val);
    			}
    			pn=1.0-pn;
    		}
    		else{
    			p0n/=(1.0-oldV);
    			p0n*=(1.0-newV);
        		pn=1.0-p0n;
    		}
    		//System.out.println("Cascade "+c+" new pn ="+pn);
    		pic.put(n, pn);
    	}
    }
   
   
    public void reinitVals(HashMap<User,ArrayList<Node>> subNodes, double min, double max){
    	//double dif=max_init-min_init;
    	
    	for(User w:subNodes.keySet()){
    		ArrayList<Node> snodes=subNodes.get(w);
    		for(Node n:snodes){
    			HashMap<String,Link> succs=n.getSuccesseurs();
    			for(Link l:succs.values()){
    				double val=l.getVal();
    				if(val<min){ //Double.MIN_VALUE){
    					val=min; //Double.MIN_VALUE;
    				}
    				if(val>max){ //(1.0-Double.MIN_VALUE)){
    					val=max; //1.0-Double.MIN_VALUE;
    				}
    				double v=val; //(Math.random()*dif)+min_init;
    				l.setVal(v);
    			}
    		}
    	}
    }
    
    public void reinitVals(HashMap<User,ArrayList<Node>> subNodes, double dif){
    	
    	for(User w:subNodes.keySet()){
    		ArrayList<Node> snodes=subNodes.get(w);
    		for(Node n:snodes){
    			HashMap<String,Link> succs=n.getSuccesseurs();
    			for(Link l:succs.values()){
    				double val=l.getVal();
    				double x=Math.random()*dif;
    				x-=dif/2.0;
    				double v=val+x; 
    				if(v>(1.0-(dif/2.0))){v=(1.0-(dif/2.0));}
    				if(v<(dif/2.0)){v=(dif/2.0);}
    				l.setVal(v);
    			}
    		}
    	}
    }
    
   
    public double computeProbaNodes(HashMap<Integer,HashMap<Node,Double>> Pi, HashMap<Integer,HashSet<Node>> actifs,HashMap<User,ArrayList<Node>> subNodes,HashMap<Node,User> parents,HashMap<Integer,HashMap<Long,HashSet<User>>> times,HashMap<Integer,HashSet<User>> sansPreds, boolean likelihood, boolean infiniteDelay,boolean considerPastNodes){ //, boolean recompute){
    	double suml=0.0;
    	System.out.println("Compute Probas");
    	ArrayList<ProbaComputer> computers=new ArrayList<ProbaComputer>(); 
    	for(Integer c:times.keySet()){
    		HashMap<Node,Double> pic=Pi.get(c);
    		if(pic==null){
    			pic=new HashMap<Node,Double>();
    			Pi.put(c, pic);
    		}
    		HashSet<Node> actifsc=actifs.get(c);
    		if(actifsc==null){
    			actifsc=new HashSet<Node>();
    			actifs.put(c,actifsc);
    		}
    		HashMap<Long,HashSet<User>> tc=times.get(c);
    		HashSet<User> sansPredsc=sansPreds.get(c);
    		if (sansPredsc==null){
    			throw new RuntimeException("Pas de points de depart !!");
    		}
    		//System.out.println("compute probas Cascade "+c);    		
    		/*while(computers.size()==nbThreads){
    			
    			ArrayList<ProbaComputer> asup=new ArrayList<ProbaComputer>();
    			for(ProbaComputer computer:computers){
    				if(computer.fini){
    					asup.add(computer);
    					suml+=computer.like;
    				}
    			}
    			for(ProbaComputer computer:asup){
    				computers.remove(computer);
    			}
    			try{
    				Thread.sleep(1);
    			}
    			catch(Exception e){
    				e.printStackTrace();
    				throw new RuntimeException("Pb thread : "+e);
    			}
    			
    		}*/
    		ProbaComputer computer=new ProbaComputer(c,pic,actifsc,subNodes,parents,tc,sansPredsc,likelihood,infiniteDelay,considerPastNodes);
			//computer.start();
			computers.add(computer);
			wq.execute(computer);
    	}
    	for(ProbaComputer computer:computers){
    		synchronized(computer){
    			if(!computer.fini){
    			    try{
    			    	//System.out.println("attend computer "+computer.cascade);
    			    	computer.wait();
    			    }
    			    catch(Exception e){
    			    	throw new RuntimeException("pb wait ! ");
    			    }
    			    if(!computer.fini){
    			    	throw new RuntimeException("pas fini ! ");
    			    }
    			    /*else{
        				System.out.println("fini2");
        			}*/
    			}
    			/*else{
    				System.out.println("fini");
    			}*/
    			suml+=computer.like;
    		}
    	}
    	
    	return(suml);
    }
    
    private class ProbaComputer implements Runnable{
    	HashMap<Node,Double> pi;
    	HashSet<Node> actifs;
    	HashMap<User,ArrayList<Node>> subNodes;
    	HashMap<Node,User> parents; 
    	HashMap<Long,HashSet<User>> times;
    	HashSet<User> sansPreds;
    	boolean infiniteDelay;
    	double like;
    	int cascade;
    	boolean fini=false;
    	boolean likelihood;
    	boolean considerPastNodes;
    	public ProbaComputer(int cascade,HashMap<Node,Double> pi, HashSet<Node> actifs, HashMap<User,ArrayList<Node>> subNodes, HashMap<Node,User> parents, HashMap<Long,HashSet<User>> times, HashSet<User> sansPreds,boolean likelihood,boolean infiniteDelay,boolean considerPastNodes){
    		this.pi=pi;
    		this.actifs=actifs;
    		this.subNodes=subNodes;
    		this.parents=parents;
    		this.times=times;
    		this.sansPreds=sansPreds;
    		this.cascade=cascade;
    		like=0.0;
    		this.likelihood=likelihood;
    		this.infiniteDelay=infiniteDelay;
    		this.considerPastNodes=considerPastNodes;
    	}
    	
    	public void run(){
    		like=computeProbaNodesCascade();
    		fini=true;
    	}
    	
    	public double computeProbaNodesCascade() {
    		long t=1;
	    	int nbTimes=times.size();
	    	//boolean likelihood=true;
	    	HashSet<User> oldies=new HashSet<User>();
	    	HashSet<User> seen=new HashSet<User>();
	    	
	    	double sump=0.0;
	    	double sumn=0.0;
	    	int borneT=nbTimes+1;
	    	/*if(!infiniteDelay>0){
	    		borneT=nbTimes+infiniteDelay;
	    	}*/
	    	while(t<=borneT){ 
	    		HashSet<User> noldies=new HashSet<User>();
	    		HashSet<User> nc=times.get(t);
	    		HashSet<User> leave=null;
	    		if(!infiniteDelay){ //>=0){
	    			leave=times.get(t-1); //(infiniteDelay));
	    		}
	    		else{
	    			if(t==borneT){
	    				leave=seen;
	    			}
	    		}
	    		if(leave==null){
    				leave=new HashSet<User>();
    			}
	    		
	    		if (nc!=null){
	    			for(User w:nc){
	    				seen.add(w);
	    				noldies.add(w);
	    				ArrayList<Node> snodes=subNodes.get(w);
	    				double pw=1.0;
		                for(Node nw:snodes){
		                	if(actifs.contains(nw)){
			                	double pwj=1.0;
			                	if (sansPreds.contains(w)){
			                		pwj=0.5;
			                	}
			                	else{
			                		//StringBuilder s=new StringBuilder();
			                		HashMap<String,Link> preds=nw.getPredecesseurs();
			                		for(Link l:preds.values()){
			                			Node nv=l.getNode1();
			                			User v=parents.get(nv);
			                			//s.append("\t Pred "+nv+" oldies="+oldies.contains(v));
			                			if ((oldies.contains(v)) && (actifs.contains(nv))){
			                				double val=l.getVal();
			                				pwj*=(1.0-val);
			                				//s.append(" kviwj "+val+" pwj "+pwj);
			                				//Log.println("nw selon "+nv+" val = "+val+" => "+(1.0-val)+" ; pwj =  "+pwj);
			                			}
			                			//s.append("\n");
			                					                			
			                		}
			                		pwj=1.0-pwj;
			                		if (pwj<Double.MIN_VALUE){
		        						pwj=Double.MIN_VALUE;
		        						System.out.println(nw+" est a 0 pour cascade "+cascade+"\n ");
			                		}		
			                		
			                	}
			                	
			                	/*if ((likelihood) && (actifs.contains(nw)) && (!sansPreds.contains(w))){
				                	if (pwj<Double.MIN_VALUE){
			    						pwj=Double.MIN_VALUE;			                		
				                	}
				                	//else{
				                		sump+=Math.log(pwj);
				                	//}
				                }*/
			                	pi.put(nw, pwj);
			                	pw*=(1.0-pwj);
		                	}
		                }
		                pw=1.0-pw;
		                if (!sansPreds.contains(w)){
		                	//if(pw)
		                		sump+=Math.log(pw);
		                	//}
		                }
	    			}
	    		}
	    		else{
	    			if(t<borneT){ /// !!!!!
	    				borneT++;
	    			}
	    		}
	    		for(User old:leave){
	    			oldies.remove(old);
	    		}
	    		oldies.addAll(noldies);
	    		if (likelihood){
	    			
	    			for(User v:leave){
	    				ArrayList<Node> snodes=subNodes.get(v);
	    					for(Node nv:snodes){
	    						if(actifs.contains(nv)){
	    							HashMap<String,Link> succs=nv.getSuccesseurs();
	    						for(Link l:succs.values()){
	    							Node nw=l.getNode2();
	    							User w=parents.get(nw);
	    							
	    							if ((!seen.contains(w)) || ((oldies.contains(w)) && (!actifs.contains(nw))) || ((considerPastNodes) && (seen.contains(w)) && (!actifs.contains(nw)))){
	    								
	    								double val=l.getVal();
	    								val=1.0-val;
	    								if(val<Double.MIN_VALUE){
	    									//val=Double.MIN_VALUE;
	    									throw new RuntimeException("voila c est ici pour nv = "+nv+" et nw = "+nw+" val");
	    								}
	    								sumn+=Math.log(val);
	    								
	    							}
	    						}
	    					}}
	    			}
	    		}
	    		
	    		
	    		
	    		t++;
	    	}
	    	
	    	
	    	//System.out.println("sump = "+sump+"; sumn = "+sumn);
	    	return(sump+sumn);
	    }
    	
    	
	    public double computeProbaNodesCascade_old() { //,boolean meme0){
	    	long t=1;
	    	int nbTimes=times.size();
	    	//boolean likelihood=true;
	    	HashSet<User> oldies=new HashSet<User>();
	    	HashSet<User> seen=new HashSet<User>();
	    	
	    	double sump=0.0;
	    	double sumn=0.0;
	    	while(t<=nbTimes+1){
	    		//System.out.println(t+" => "+oldies.size());
	    		HashSet<User> noldies=new HashSet<User>();
	    		HashSet<User> nc=times.get(t);
	    		if (nc!=null){
	    			for(User w:nc){
	    				seen.add(w);
	    				noldies.add(w);
	    				ArrayList<Node> snodes=subNodes.get(w);
	    				double pw=1.0;
		                for(Node nw:snodes){
		                	if(actifs.contains(nw)){
			                	double pwj=1.0;
			                	if (sansPreds.contains(w)){
			                		pwj=0.5;
			                	}
			                	else{
			                		//StringBuilder s=new StringBuilder();
			                		HashMap<String,Link> preds=nw.getPredecesseurs();
			                		for(Link l:preds.values()){
			                			Node nv=l.getNode1();
			                			User v=parents.get(nv);
			                			//s.append("\t Pred "+nv+" oldies="+oldies.contains(v));
			                			if ((oldies.contains(v)) && (actifs.contains(nv))){
			                				double val=l.getVal();
			                				pwj*=(1.0-val);
			                				//s.append(" kviwj "+val+" pwj "+pwj);
			                				//Log.println("nw selon "+nv+" val = "+val+" => "+(1.0-val)+" ; pwj =  "+pwj);
			                			}
			                			//s.append("\n");
			                					                			
			                		}
			                		pwj=1.0-pwj;
			                		if (pwj<Double.MIN_VALUE){
		        						pwj=Double.MIN_VALUE;
		        						System.out.println(nw+" est a 0 pour cascade "+cascade+"\n ");
			                		}		
			                		
			                	}
			                	
			                	/*if ((likelihood) && (actifs.contains(nw)) && (!sansPreds.contains(w))){
				                	if (pwj<Double.MIN_VALUE){
			    						pwj=Double.MIN_VALUE;			                		
				                	}
				                	//else{
				                		sump+=Math.log(pwj);
				                	//}
				                }*/
			                	pi.put(nw, pwj);
			                	pw*=(1.0-pwj);
		                	}
		                }
		                pw=1.0-pw;
		                if (!sansPreds.contains(w)){
		                	//if(pw)
		                		sump+=Math.log(pw);
		                	//}
		                }
	    			}
	    		}
	    		else{
	    			if(t<(nbTimes+1)){
	    				nbTimes++;
	    			}
	    		}
	    		if (likelihood){
	    			
	    			for(User v:oldies){
	    				ArrayList<Node> snodes=subNodes.get(v);
	    					for(Node nv:snodes){
	    						if(actifs.contains(nv)){
	    							HashMap<String,Link> succs=nv.getSuccesseurs();
	    						for(Link l:succs.values()){
	    							Node nw=l.getNode2();
	    							User w=parents.get(nw);
	    							
	    							if ((!seen.contains(w)) || ((noldies.contains(w)) && (!actifs.contains(nw)))){
	    								
	    								double val=l.getVal();
	    								val=1.0-val;
	    								if(val<Double.MIN_VALUE){
	    									//val=Double.MIN_VALUE;
	    									throw new RuntimeException("voila c est ici pour nv = "+nv+" et nw = "+nw+" val");
	    								}
	    								sumn+=Math.log(val);
	    								
	    							}
	    						}
	    					}}
	    			}
	    		}
	    		
	    		
	    		oldies=noldies;
	    		t++;
	    	}
	    	
	    	
	    	//System.out.println("sump = "+sump+"; sumn = "+sumn);
	    	return(sump+sumn);
	    }
    }
    
    public void calculProportions(HashMap<User,HashSet<Integer>> cascades, HashMap<Integer,HashSet<Node>> actifs, HashMap<User,ArrayList<Node>> subNodes, HashMap<User,HashMap<Node,Double>> proportion){
    	proportion.clear();
    	for(User user:cascades.keySet()){
    		HashSet<Integer> cas=cascades.get(user);
    		int nb=cas.size();
    		HashMap<Node,Integer> nbn=new HashMap<Node,Integer>();
    		ArrayList<Node> nodes=subNodes.get(user);
    		
    		for(Integer c:cas){
    			HashSet<Node> act=actifs.get(c);
    			for(Node node:nodes){
    				if(act.contains(node)){
    					Integer nbnode=nbn.get(node);
    					nbnode=(nbnode==null)?1:(nbnode+1);
    					nbn.put(node,nbnode);
    				}
    			}
    		}
    		HashMap<Node,Double> prop=new HashMap<Node,Double>();
    		proportion.put(user,prop);
    		for(Node node:nbn.keySet()){
    			int nbnode=nbn.get(node);
    			prop.put(node, (1.0*nbnode)/(1.0*nb));
    		}
    	}
    }
   
    // returns cascades that have been considered
    public HashSet<Integer> relabelActifs(HashMap<Integer,HashSet<Node>> actifs,HashMap<User,HashMap<Node,Double>> proportion, HashMap<User,ArrayList<Node>> subNodes,HashMap<Node,User> parents,HashMap<Integer,HashMap<Long,HashSet<User>>> times,HashMap<Integer,HashSet<User>> sansPreds,HashMap<Integer,HashMap<User,Long>> userTimeContamination, HashMap<User,HashSet<Integer>> cascades, HashMap<Integer,HashMap<Node,Double>> pi, int nbrelab,boolean infiniteDelay, boolean only_one, boolean considerPastNodes){
    	double suml=0.0;
    	ArrayList<Integer> cas=new ArrayList<Integer>(times.keySet());
    	HashSet<Integer> relab=new HashSet<Integer>();
    	
    	//WorkQueue wq=new WorkQueue(nbThreads);
    	int nb=0;
    	
    	if(nbrelab>0){
    		while((nb<nbrelab) && (nb<cas.size())){
    			int x=(int)(Math.random()*cas.size());
    			int c=cas.get(x);
    			if(!relab.contains(c)){
    				relab.add(c);
    				nb++;
    			}
    			
    		}
    	}
    	else{
    		relab=new HashSet<Integer>(times.keySet());
    	}
    	//HashSet<ActifsRelabeller> relabellersQueue=new HashSet<ActifsRelabeller>();
    	ArrayList<ActifsRelabeller> relabellers=new ArrayList<ActifsRelabeller>();
    	int nbworks=0;
    	HashMap<Integer,HashMap<User,HashMap<Node,Integer>>> diffsc=new HashMap<Integer,HashMap<User,HashMap<Node,Integer>>>();
    	for(Integer c:relab){
    		HashSet<Node> actifsc=actifs.get(c);
    		if(actifsc==null){
    			actifsc=new HashSet<Node>();
    			actifs.put(c,actifsc);
    		}
    		
    		HashMap<User,Long> conta=userTimeContamination.get(c);
    		
    		
    		HashMap<Long,HashSet<User>> tc=times.get(c);
    		HashSet<User> sansPredsc=sansPreds.get(c);
    		if (sansPredsc==null){
    			throw new RuntimeException("Pas de points de depart !! \n");
    		}
    		//System.out.println("relabel actifs "+c);
    		
    		
    		/*while(relabellers.size()==nbThreads){
    			ArrayList<ActifsRelabeller> asup=new ArrayList<ActifsRelabeller>();
    			for(ActifsRelabeller relabeller:relabellers){
    				if(relabeller.fini){
    					asup.add(relabeller);
    				}
    			}
    			for(ActifsRelabeller relabeller:asup){
    				relabellers.remove(relabeller);
    			}
    			try{
    				Thread.sleep(1);
    				
    			}
    			catch(Exception e){
    				e.printStackTrace();
    				throw new RuntimeException("Pb thread : "+e);
    			}
    			
    		}*/
    		//relabeller.start();
    		//synchronized(relabellers){
    		/*while(relabellersQueue.size()>=nbThreads){
    			try{
    				System.out.println(c+" bloque");
    				this.wait();
    				System.out.println(c+" debloque");
    			}
    			catch(Exception e){
    				throw new RuntimeException(e);
    			}
    			//relabellers.add(relabeller);
    			
    			//nbworks++;
    		}*/
    		HashMap<User,HashMap<Node,Integer>> diff=new HashMap<User,HashMap<Node,Integer>>();
    		diffsc.put(c, diff);
    		ActifsRelabeller relabeller=new ActifsRelabeller(c,actifsc,proportion,diff,subNodes,parents,tc,sansPredsc,conta,pi.get(c),infiniteDelay,only_one,considerPastNodes);
    		relabellers.add(relabeller);
			//relabeller.start();
    		//System.out.println(relabellersQueue.size());
    		wq.execute(relabeller);
    		
    		//suml+=like;
    	}
    	for(ActifsRelabeller relabeller:relabellers){
    		synchronized(relabeller){
    			if(!relabeller.fini){
    			    try{
    			    	//System.out.println("attend relabeller "+relabeller.cascade);
    			    	//synchronized(relabeller){
    			    		relabeller.wait();
    			    	//}
    			    }
    			    catch(Exception e){
    			    	throw new RuntimeException("pb wait ! ");
    			    }
    			    if(!relabeller.fini){
    			    	throw new RuntimeException("pas fini ! ");
    			    }
    			    /*else{
        				System.out.println("fini2");
        			}*/
    			}
    			/*else{
    				System.out.println("fini");
    			}*/
    		}
    		//System.out.print(relabeller.cascade+" ");
    	}
    	HashMap<User,HashMap<Node,Integer>> sdiff=new HashMap<User,HashMap<Node,Integer>>();
    	for(Integer c:diffsc.keySet()){
    		HashMap<User,HashMap<Node,Integer>> diff=diffsc.get(c);
    		for(User user:diff.keySet()){
    			HashMap<Node,Integer> h=sdiff.get(user);
    			if(h==null){
    				h=new HashMap<Node,Integer>();
    				sdiff.put(user, h);
    			}
    			HashMap<Node,Integer> dif=diff.get(user);
    			for(Node node:dif.keySet()){
    				Integer x=dif.get(node);
    				Integer s=h.get(node);
    				s=(s==null)?0:s;
    				h.put(node, s+x);
    			}
    		}
    	}
    	for(User user:sdiff.keySet()){
    		HashMap<Node,Integer> h=sdiff.get(user);
    		HashMap<Node,Double> prop=proportion.get(user);
    		if(prop==null){
    			prop=new HashMap<Node,Double>();
    			proportion.put(user, prop);
    		}
    		int nbc=cascades.get(user).size();
    		for(Node node:h.keySet()){
    			Double p=prop.get(node);
    			p=(p==null)?0.0:p;
    			p*=nbc;
    			p+=1.0*h.get(node);
    			p/=(1.0*nbc);
    			prop.put(node, p);
    		}
    	}
    	///System.out.println("relabel ok");
    	//return(suml);
    	//calculProportions(cascades,actifs,subNodes,proportion);
    	return(relab);
    }
    
		
	
    
    private class ActifsRelabeller implements Runnable{
    	HashSet<Node> actifs;
    	HashMap<User,ArrayList<Node>> subNodes;
    	HashMap<Node,User> parents;
    	HashMap<Long,HashSet<User>> times;
    	HashSet<User> sansPreds;
    	HashMap<User,Long> conta;
    	HashMap<Node,Double> pi;
    	boolean infiniteDelay;
    	//ICLSN2 iclsn; //relabellers;
    	//static HashSet<Thread> pool=new HashSet<Thread>();
    	int cascade;
    	boolean fini=false;
    	boolean only_one=true;
    	boolean considerPastNodes;
    	HashMap<User,HashMap<Node,Double>> proportion;
    	HashMap<User,HashMap<Node,Integer>> diff;
    	
    	public ActifsRelabeller(int cascade,HashSet<Node> actifs, HashMap<User,HashMap<Node,Double>> proportion, HashMap<User,HashMap<Node,Integer>> diff, HashMap<User,ArrayList<Node>> subNodes, HashMap<Node,User> parents, HashMap<Long,HashSet<User>> times, HashSet<User> sansPreds,HashMap<User,Long> conta,HashMap<Node,Double> pi,boolean infiniteDelay,boolean only_one,boolean considerPastNodes){
    		//nb++;
    		this.actifs=actifs;
    		this.conta=conta;
    		this.subNodes=subNodes;
    		this.parents=parents;
    		this.times=times;
    		this.sansPreds=sansPreds;
    		this.cascade=cascade;
    		this.pi=pi;
    		this.infiniteDelay=infiniteDelay;
    		this.only_one=only_one;
    		this.considerPastNodes=considerPastNodes;
    		this.proportion=proportion;
    		this.diff=diff;
    		//System.out.println(considerPastNodes);
    		//this.iclsn=iclsn;
    		/*this.relabellers=relabellers;
    		relabellers.add(this);*/
    		//pool.add(this);
    	}
    	
    	
    	public void run(){
    		relabelActifsCascade();
    		fini=true;
    		//this.notify();
    		//nb--;
    	}
    	public void relabelActifsCascade(){
    	    try{  
    	      if(!only_one){
    	    	  relabelActifsCascade_Mult(); 
    	      }
    	      else{
    	    	  relabelActifsCascade_One(); 
    	      }
    	    }
    	    catch(Exception e){
    	    	System.out.println(e);
    	    	e.printStackTrace();
    	    	throw new RuntimeException(e);
    	    }
    	}
    	
    	public void relabelActifsCascade_One(){	
    		ArrayList<Long> ltimes=new ArrayList<Long>(times.keySet());
	    	Collections.shuffle(ltimes);
	    	boolean likelihood=true;
	    	//HashMap<Node,Double> pic=pi;
    		HashMap<Node,Double> pitemp=new HashMap<Node,Double>();
    		
	    	double sump=0.0;
	    	double sumn=0.0;
	    	
	    	for(long t:ltimes){
	    		//System.out.println("t ="+t);
	    		pitemp=new HashMap<Node,Double>();
	    		//HashSet<User> oldies=times.get(t-1);
	    		HashSet<User> nc=times.get(t);
	    		//HashSet<User> future=times.get(t+1);
	    		if (nc!=null){
	    			for(User v:nc){
	    				ArrayList<Node> snodes=new ArrayList<Node>(subNodes.get(v));
	    				//Collections.shuffle(snodes);
	 	                double sum=0.0;
	 	                HashMap<Node,Double> pnvPreds=new HashMap<Node,Double>();
	 	                if (sansPreds.contains(v)){
	 	            	   double pp=1.0/(1.0*snodes.size());
	 	            	   for(Node nv:snodes){
	 	            		  pnvPreds.put(nv,pp);
	 	            	   }
	 	                }
	 	                else{
	 	                   double sumpp=0.0;
	 	                   for(Node nv:snodes){
	 	                	  double pp=1.0; 
	 	                	  HashMap<String,Link> preds=nv.getPredecesseurs();
               				  for(Link l:preds.values()){
               					Node nu=l.getNode1();
               					User u=parents.get(nu);
               					Long tu=conta.get(u);
               					boolean ok=false;
               					if(infiniteDelay){
               						if((tu!=null) && (tu<t)){ok=true;}
               					}
               					else{
               						//if((tu!=null) && (tu>=(t-infiniteDelay)) && (tu<t)){ok=true;}
               						if((tu!=null) && (tu==(t-1))){ok=true;}
               					}
               					if (ok && (actifs.contains(nu))){
               						double val=l.getVal();
               						pp*=(1.0-val);
               					}
               				 }
               				 pnvPreds.put(nv,1.0-pp);
               				 //sumpp+=1.0-pp;
	 	                   }
	 	                   /*if(sumpp>0){
	 	                	   for(Node nv:snodes){
	 	                		   double z=pnvPreds.get(nv);
	 	                		   pnvPreds.put(nv, z/sumpp);
	 	                	   }
	 	                   }*/
	 	                   
               				//System.out.println(pnv1Preds);
               			}
	 	                Node nact=null;
						for(Node nv:snodes){
							if(actifs.contains(nv)){
								nact=nv;
								break;
							}
						}
						/*for(Node nv:snodes){
							System.out.println(cascade+"=>"+nv+":"+pnvPreds.get(nv));
						}*/
	 	                HashMap<Node,HashMap<Node,Double>> pW0v=new HashMap<Node,HashMap<Node,Double>>();
	 	                HashSet<Node> nwok=new HashSet<Node>();
	 	                HashMap<Node,Double> psuccs=new HashMap<Node,Double>();
	 	                //HashSet<User> succUsers=new HashSet<User>();
	 	                HashMap<String,Link> succs=v.getSuccesseurs();
	 	                for(Link l:succs.values()){
	 	                	User w=(User)l.getNode2();
	 	                	//System.out.println(cascade+"=> succ de "+v+" : "+w);
	 	                	Long tw=conta.get(w);
	 	                	boolean pastUser=false;
        					if((tw!=null) && (tw<=t)){ //(conta.contains(w)) || (nc.contains(w))){
        						// w pas dependant de v
        						//Log.println(nv+" succ : "+nw+" zap");
        						if(!considerPastNodes){
        							continue;
        						}
        						pastUser=true;
        						
        					}
        					
        					/*if((tw!=null) && (tw>t) && ((infiniteDelay) || (tw==(t+1)))){
        						succUsers.add(w);
        					}*/
        							
        					ArrayList<Node> wnodes=new ArrayList<Node>(subNodes.get(w));
	 	                	for(Node nw:wnodes){
	 	                		//double pw0;
	 	                		boolean actif=actifs.contains(nw);
	 	                		
	 	                		if(pastUser && actif){
        							continue;
        						}
	 	                		HashMap<Node,Double> h=pW0v.get(nw);
	 	                		if(h==null){
	 	                		   h=new HashMap<Node,Double>();
	 	                		   pW0v.put(nw, h);	
	 	                		}
	 	                		
	 	                		HashMap<String,Link> predsw=nw.getPredecesseurs();
	 	                		/*for(Link lw:predsw.values()){
    								//System.out.println(pw0);
    								double kvw=lw.getVal();
    								System.out.println("lien "+lw.getNode1()+","+nw+"="+kvw);
	 	                		}*/
	 	                		boolean pwsok=false;
            					Double pw0; 
            					HashMap<Node,Double> pti=pitemp;
            					if((tw!=null) && (tw>t) && ((infiniteDelay) || (tw==(t+1))) && (actif)){
            					  pti=pi;
            			    	}
            					pw0=pti.get(nw);
            					if((pw0!=null) && ((1.0-pw0)>Double.MIN_VALUE)){ // on peut deduire les probas sans tout recaculer
            						pw0=1.0-pw0;
            						
            						for(Node nv:snodes){
            							if(actifs.contains(nv)){
            								h.put(nv, pw0);
            							}
            							else{
            								Link lvw=predsw.get(nact.getName());
            								double val=lvw.getVal();
            								//System.out.println("val="+val);
            								double pp=pw0/(1.0-val);
		                					if(Double.isInfinite(pp)){
		                							throw new RuntimeException(nv+" pw0v0 infini pour"+nv+";"+nw+"! : pw0 = "+pw0+" val = "+val+"\n");
		                					}
		                					lvw=predsw.get(nv.getName());
		                					val=lvw.getVal();
		                					pp*=(1.0-val);
		                					h.put(nv, pp);
            							}
            						}
            						
            					}
            					else{
            						pw0=1.0;
            						
        							for(Link lw:predsw.values()){
        								//System.out.println(pw0);
        								double kvw=lw.getVal();
	                					Node npw=lw.getNode1();
	                					User pnpw=parents.get(npw);
	                					Long tp=conta.get(pnpw);
	                					//System.out.println(tp+" "+tw);
	                					boolean ok=false;
 	                					if(infiniteDelay){
 	                						if(tp!=null){
 	                							if ((tw==null) || (tp<tw) || pastUser){ok=true;}
 	                						}
 	                					}
 	                					else{
 	                						if((tp!=null) && (tp==t)){
 	                							ok=true;
 	                							//if(tw==null)
 	                							//if((tw==null))&& (tp>(t-contaMaxDelay)) && (tp<=t)){ok=true;}
 	                						}
 	                					}
	                					
	                					if (ok){
	                						//System.out.println("ok");
	                					  User uw=parents.get(npw);  
	                					  if (actifs.contains(npw)){
	                						
	                						if (uw!=v){
	                							for(Node no:h.keySet()){
	                							   Double pp=h.get(no);	
	                							   pp=(pp==null)?1.0:pp;
	                							   h.put(no, pp*(1.0-kvw));
	                							}
	                							//Log.println(nv+" pw0v0 de "+nw+"*=(1.0-lien avec "+npw+"="+kvw+")="+pw0v0);
	                						}
	                						else{
	                							Double pp=h.get(npw);		
	                							pp=(pp==null)?1.0:pp;
	                							h.put(npw, pp*(1.0-kvw));
	                						}
	                						
	                						pw0*=(1.0-kvw);
	                					  }
	                					  else{
	                						  if (uw==v){
	                							  Double pp=h.get(npw);		
	                							  pp=(pp==null)?1.0:pp;
	                							  h.put(npw, pp*(1.0-kvw));
	                						  }
	                					  }
	                					  //System.out.println("fin ok");
	                					}
        							}
        							pwsok=true;
        							pti.put(nw, 1.0-pw0);
            					}
            					for(Node nv:snodes){
            						Double pp=psuccs.get(nv);
            						pp=(pp==null)?1.0:pp;
            						Double pw=h.get(nv);
            						pw=(pw==null)?1.0:pw;
            						if((tw!=null) && (tw>t) && ((infiniteDelay) || (tw==(t+1))) && (actif)){
		                						pp*=(1.0-pw);
		                						h.put(nv, (1.0-pw)); 
		                						nwok.add(nw);
		                						//pWv1.put(nw, (1.0-pw0v1));
		                			}
		                			else{
		                						pp*=pw;
		                						h.put(nv, (1.0-pw)); 
		                						
		                						//pWv1.put(nw, (1.0-pw0v1));
		                			}
            						psuccs.put(nv, pp);
            					}
            					//System.out.println(cascade+"=>"+nw+":"+h);
    	 	                	
	 	                	}
	 	                }
	 	                
	 	               /*for(Node nv:snodes){
	 	            	  double ps=1.0;
	 	            	  HashMap<User,Double> sums=new HashMap<User,Double>();
	 	            	  HashMap<User,Double> pus=new HashMap<User,Double>();
	 	            	  HashMap<User,Node> actn=new HashMap<User,Node>();
	 	            	  for(Node nw:pW0v.keySet()){
               				HashMap<Node,Double> h=pW0v.get(nw);
               				User w=parents.get(nw);
               				Double pnw=h.get(nv);
               				Double ppus=pus.get(w);
               				ppus=(ppus==null)?1.0:ppus;
               				ppus*=(1.0-pnw);
               				pus.put(w, ppus);
               				if(succUsers.contains(w)){
               					Double sus=sums.get(w);
                   				sus=(sus==null)?0.0:sus;
                   				sus+=pnw;
                   				sums.put(w, sus);
                   				if(actifs.contains(nw)){
                   					actn.put(w, nw);
                   				}
               				}
               			 }
	 	            	 double psucc=1.0;
	 	            	 for(User user:pus.keySet()){
	 	            		 
	 	            		Double ppus=pus.get(user);
	 	            		
	 	            		// Si c est un user possiblement contamine par v, alors on considere la proba qu'il soit contamine * proba que ce soit le noeud actif qui le soit (par rapport somme probas autres noeuds)
	 	            		if(succUsers.contains(user)){
	 	            			ppus=1.0-ppus;
	 	            			psucc*=ppus;
	 	            			HashMap<Node,Double> h=pW0v.get(actn.get(user));
	 	            			psucc*=(h.get(nv)/sums.get(user));
	 	            		}
	 	            		else{
	 	            			// Sinon on considere juste le fait qu il ne soit pas contamine
	 	            			psucc*=ppus;
	 	            		}
	 	            	 }
	 	            	 psuccs.put(nv, psucc); 
	 	               }*/
        					
        				double sumpr=0.0;
        				HashMap<Node,Double> probs=new HashMap<Node,Double>();
        				for(Node nv:snodes){
        					Double pp=pnvPreds.get(nv);
        					Double ps=psuccs.get(nv);
        					ps=(ps==null)?1.0:ps;
        					//System.out.println(cascade+"=>"+nv+": pp "+pp+", ps"+ps);
        					double pb=pp*ps;
        					sumpr+=pb;
        					probs.put(nv, pb);
        				}
        				double x=Math.random()*(sumpr);
        				Node select=null;
        				for(Node nv:snodes){
        					Double pp=probs.get(nv);
        					x-=pp;
        					if(x<=0){
        						select=nv;
        						break;
        					}
        				}
        				actifs.remove(nact);
        				actifs.add(select);
        				pi.remove(nact);
        				pi.put(select,pnvPreds.get(select));
        				if(nact!=select){
        						//System.out.println(cascade+" change "+nact+"=>"+select);
        						//System.out.println(probs);
        						//System.out.println(actifs);
 	                			// Mise a jour des probas pi puisqu il y a eu un changement
 	                			for(Node nw:pW0v.keySet()){
 	                				HashMap<Node,Double> h=pW0v.get(nw);
 	                				if(nwok.contains(nw)){
 	                					pi.put(nw, h.get(select));
 	                				}
 	                				else{
 	                					pitemp.put(nw,h.get(select));
 	                				}
 	                			}
        				}
        				
 	                	
 	                }
	 	                
	 	                
	    			
	    		}
	    		//oldies=noldies;
	    		//t++;
	    	}
	    	//System.out.println(actifs);
	    	 
	    	 
	    }
    		
    	public void relabelActifsCascade_Mult(){
	    	//System.out.println("relabel cascade "+cascade);
	    	//System.out.println(actifs);
	    	ArrayList<Long> ltimes=new ArrayList<Long>(times.keySet());
	    	Collections.shuffle(ltimes);
	    	boolean likelihood=true;
	    	//HashMap<Node,Double> pic=pi;
    		HashMap<Node,Double> pitemp=new HashMap<Node,Double>();
    		
	    	double sump=0.0;
	    	double sumn=0.0;
	    	
	    	for(long t:ltimes){
	    		//System.out.println("t ="+t);
	    		pitemp=new HashMap<Node,Double>();
	    		//HashSet<User> oldies=times.get(t-1);
	    		HashSet<User> nc=times.get(t);
	    		//HashSet<User> future=times.get(t+1);
	    		if (nc!=null){
	    			for(User v:nc){
	    				ArrayList<Node> snodes=new ArrayList<Node>(subNodes.get(v));
	    				Collections.shuffle(snodes);
	 	                double sum=0.0;
	 	                int nb1=0;
	 	                for(Node nv:snodes){
	 	                	if(actifs.contains(nv)){
	 	                		nb1++;
	 	                	}
	 	                }
	 	                HashMap<Node,Integer> dif=diff.get(v);
	 	                if(dif==null){
	 	                	dif=new HashMap<Node,Integer>();
	 	                	diff.put(v, dif);
	 	                }
	 	                HashMap<Node,Double> prop=proportion.get(v);
	 	                for(Node nv:snodes){
	 	                	if(nb1==0){actifs.add(nv); nb1++; throw new RuntimeException("pas d'actifs pour "+v+"!!");} // ne devrait pas arriver (sauf ptet au premier labelling)
	 	                	else{
	 	                		if((nb1==1) && (actifs.contains(nv))){
	 	                			// nothing to do => p(nv inactif = 0)
	 	                		}
	 	                		else{
	 	                			double pnv0Preds=1.0;
	 	                			double pnv1Preds=1.0;
	 	                			if (sansPreds.contains(v)){
	 	                				pnv0Preds=0.5;
	 	                				pnv1Preds=0.5;
	 	                				/*int x=nv.getName().lastIndexOf("_");
	 	                				int num=Integer.parseInt(nv.getName().substring(x+1));
	 	                				if(num>1){
	 	                					pnv0Preds=0.9;
	 	                					pnv1Preds=0.1;
	 	                				}*/
	 	                			}
	 	                			else{
	 	                				HashMap<String,Link> preds=nv.getPredecesseurs();
	 	                				for(Link l:preds.values()){
	 	                					Node nu=l.getNode1();
	 	                					User u=parents.get(nu);
	 	                					Long tu=conta.get(u);
	 	                					boolean ok=false;
	 	                					if(infiniteDelay){
	 	                						if((tu!=null) && (tu<t)){ok=true;}
	 	                					}
	 	                					else{
	 	                						//if((tu!=null) && (tu>=(t-infiniteDelay)) && (tu<t)){ok=true;}
	 	                						if((tu!=null) && (tu==(t-1))){ok=true;}
	 	                					}
	 	                					if (ok && (actifs.contains(nu))){
	 	                						double val=l.getVal();
	 	                						pnv0Preds*=(1.0-val);
	 	                					}
	 	                				}
	 	                				pnv1Preds=1.0-pnv0Preds; 
	 	                				//System.out.println(pnv1Preds);
	 	                			}
	 	                			double pSuccnv0=1.0;
	 	                			double pSuccnv1=1.0;
	 	                			HashMap<Node,Double> pWv1=new HashMap<Node,Double>();
	 	                			HashMap<Node,Double> pWv0=new HashMap<Node,Double>();
	 	                			HashMap<Node,Double> pWv1temp=new HashMap<Node,Double>();
	 	                			HashMap<Node,Double> pWv0temp=new HashMap<Node,Double>();
	 	                			HashMap<String,Link> succs=nv.getSuccesseurs();
	 	                			
		                			for(Link l:succs.values()){
		                					double val=l.getVal();
		                					Node nw=l.getNode2();
		                					User w=parents.get(nw);
		                					//Log.println(nv+" succ : "+nw);
		                					//if((actifs.contains(nw)) && ((seen.contains(w)) || (nc.contains(w)))){
		                					Long tw=conta.get(w);
		                					boolean actif=actifs.contains(nw);
		                					boolean pastNode=false;
		                					if((tw!=null) && (tw<=t)){ //(conta.contains(w)) || (nc.contains(w))){
		                						// w pas dependant de v
		                						//Log.println(nv+" succ : "+nw+" zap");
		                						if(!considerPastNodes){
		                							continue;
		                						}
		                						pastNode=true;
		                						if(actif){
		                							continue;
		                						}
		                					}
		                					
		                					double pw0v1=1.0;
		                					double pw0v0=1.0;
		                					boolean pwsok=false;
		                					Double pw0; 
		                					HashMap<Node,Double> pti=pitemp;
		                					
		                					//if((tw!=null) && (tw>t) && ((infiniteDelay<0) || (tw<=(t+infiniteDelay))) && (actif)){
		                					if((tw!=null) && (tw>t) && ((infiniteDelay) || (tw==(t+1))) && (actif)){
			                					
		                					  pti=pi;
		                			    	}
		                					pw0=pti.get(nw);
		                					//System.out.println(pw0);
		                					if(pw0==null){
		                						pw0=1.0;
		                						HashMap<String,Link> predsw=nw.getPredecesseurs();
	                							for(Link lw:predsw.values()){
	                								//System.out.println(pw0);
	                								double kvw=lw.getVal();
	        	                					Node npw=lw.getNode1();
	        	                					User pnpw=parents.get(npw);
	        	                					Long tp=conta.get(pnpw);
	        	                					//System.out.println(tp+" "+tw);
	        	                					boolean ok=false;
	    	 	                					if(infiniteDelay){
	    	 	                						if(tp!=null){
	    	 	                							if ((tw==null) || (tp<tw) || pastNode){ok=true;}
	    	 	                						}
	    	 	                					}
	    	 	                					else{
	    	 	                						if((tp!=null) && (tp==t)){
	    	 	                							ok=true;
	    	 	                							//if(tw==null)
	    	 	                							//if((tw==null))&& (tp>(t-contaMaxDelay)) && (tp<=t)){ok=true;}
	    	 	                						}
	    	 	                					}
	        	                					
	        	                					if (ok){
	        	                						//System.out.println("ok");
	        	                					  if (actifs.contains(npw)){
	        	                						if (npw!=nv){
	        	                							pw0v0*=(1.0-kvw);
	        	                							//Log.println(nv+" pw0v0 de "+nw+"*=(1.0-lien avec "+npw+"="+kvw+")="+pw0v0);
	        	                						}
	        	                						pw0v1*=(1.0-kvw);
	        	                						//Log.println(nv+" pw0v1 de "+nw+"*=(1.0-lien avec "+npw+"="+kvw+")="+pw0v1);
	        	                						pw0*=(1.0-kvw);
	        	                					  }
	        	                					  else{
	        	                						if (npw==nv){
	          	                							pw0v1*=(1.0-kvw);
	          	                							//Log.println(nv+" pw0v1 de "+nw+"*=(1.0-lien avec "+npw+"="+kvw+")="+pw0v1);
	          	                						}  
	        	                					  }
	        	                					  //System.out.println("fin ok");
	        	                					}
	                							}
	                							pwsok=true;
	                							pti.put(nw, 1.0-pw0);
	                							//System.out.println(pw0);
		                					}
		                					else{
		                						pw0=1.0-pw0;
		                					}
		                					if (!pwsok){
		                						
		                					  if(actifs.contains(nv)){
		                						pw0v1=pw0;
		                						if(pw0>Double.MIN_VALUE){
		                							pw0v0=pw0/(1.0-val);
		                							if(Double.isInfinite(pw0v0)){
		                								throw new RuntimeException(nv+" pw0v0 infini pour"+nv+";"+nw+"! : pw0 = "+pw0+" val = "+val+"\n");
		                							}
		                						}
		                						else{
		                							// il y a eu un arrondi, on doit recalculer pw0
		                							System.out.println("Arrondi, recalcul de pw0 pour "+nw);
		                							pw0v0=1.0;
		                							HashMap<String,Link> predsw=nw.getPredecesseurs();
		                							for(Link lw:predsw.values()){
		                								double kvw=lw.getVal();
		        	                					Node npw=lw.getNode1();
		        	                					User pnpw=parents.get(npw);
		        	                					Long tp=conta.get(pnpw);
		        	                					boolean ok=false;
		        	                						
		    	 	                					if(infiniteDelay){
		    	 	                						if((tp!=null) && ((tw==null) || (tp<tw) || pastNode)){ok=true;}
		    	 	                					}
		    	 	                					else{
		    	 	                						//if((tp!=null) && (tp>=(tw-infiniteDelay)) && (tp<tw)){ok=true;}
		    	 	                						if((tp!=null) && (tp==t)){ok=true;}
		    	 	                					
		    	 	                					}
		    	 	                					
		        	                					if ((npw!=nv) && (ok) && (actifs.contains(npw))){
		        	                						pw0v0*=(1.0-kvw);
		        	                						
		        	                					}
		        	                					
		                							}
		             	                			
		                						}
		                					  }
		                					  else{
		                							pw0v0=pw0;
		                							pw0v1=pw0*(1.0-val);
		                							
		                					  }
		                					} 
		                					
		                					
		                					if((tw!=null) && (tw>t) && ((infiniteDelay) || (tw==(t+1))) && (actif)){
		                					//if((tw!=null) && (tw>t) && ((infiniteDelay<0) || (tw<=(t+infiniteDelay))) && (actif)){
		                					//if ((actifs.contains(nw)) && (future.contains(w))){
		                						pSuccnv0*=(1.0-pw0v0);
		                						pSuccnv1*=(1.0-pw0v1);
		                						pWv0.put(nw, (1.0-pw0v0));
		                						pWv1.put(nw, (1.0-pw0v1));
		                					}
		                					else{
		                						pSuccnv0*=pw0v0;
		                						pSuccnv1*=pw0v1;
		                						pWv0temp.put(nw, (1.0-pw0v0));
		                						pWv1temp.put(nw, (1.0-pw0v1));
		                					}
		             
		                			}
		                			
		                			double pv0=pnv0Preds*pSuccnv0;
		                			double pv1=pnv1Preds*pSuccnv1;
		                			/*int li=nv.getName().lastIndexOf("_");
 	                				int num=Integer.parseInt(nv.getName().substring(li+1));
 	                				if(num>1){
 	                					pv1*=0.1;
 	                					pv0*=0.9;
 	                				}*/
		                			//if(!actifs.contains(nv)){
		                				Double propv=prop.get(nv);
		                				propv=(propv==null)?0.0:propv;
		                				propv=propv*0.8+0.1;
		                				pv1*=propv;
		                				//pv0*=(1.0-propv);
		                				//pv0+=1.0-propv;
		                			//}
		                			//pv0=0.01+pv0; //Double.MIN_VALUE*10.0+pv0; //*0.98;
		                			//pv1=0.01+pv1; //Double.MIN_VALUE*10.0+pv1; //*0.98;
		                			
		                			double x=Math.random()*(pv0+pv1);
	 	                			boolean maj0=false;
	 	                			boolean maj1=false; 
	 	                			if((x==0) || (x<pv0)){
	 	                				if(actifs.contains(nv)){
	 	                					actifs.remove(nv);
	 	                					maj0=true;
	 	                					nb1--;
	 	                					//System.out.println("sup pour cascade "+cascade+", noeud "+nv+" pv0="+pv0+" pv1="+pv1+" "+x);
	 	                					pi.remove(nv);
	 	                					dif.put(nv, -1);
	 	                				}
	 	                			}
	 	                			else{
	 	                				if(!actifs.contains(nv)){
	 	                					actifs.add(nv);
	 	                					maj1=true;
	 	                					nb1++;
	 	                					//System.out.println("add cascade "+cascade+", noeud"+nv+" pv0="+pv0+" pv1="+pv1+" "+x);
	 	                					pi.put(nv,pnv1Preds);
	 	                					dif.put(nv, 1);
	 	                				}
	 	                			}
	 	                			
	 	                			// Mise a jour des probas pi puisqu il y a eu un changement
	 	                			if((maj0) || (maj1)){
	 	                				HashMap<Node,Double> newpW=pWv0; 
	 	                				if(maj1){
	 	                					newpW=pWv1;
	 	                				}
	 	                				for(Node n:newpW.keySet()){
	 	                					pi.put(n, newpW.get(n));
	 	                				}	
	 	                				newpW=pWv0temp; 
	 	                				if(maj1){
	 	                					newpW=pWv1temp;
	 	                				}
	 	                				for(Node n:newpW.keySet()){
	 	                					pitemp.put(n, newpW.get(n));
	 	                				}
	 	                			
	 	                			}
	 	                		}
	 	                	}
	 	                }
	 	                
	 	                
	    			}
	    		}
	    		//oldies=noldies;
	    		//t++;
	    	}
	    	//System.out.println(actifs);
	    }
    	
	    public void relabelActifsCascade_old(){
	    	//System.out.println("relabel cascade "+cascade);
	    	//System.out.println(actifs);
	    	ArrayList<Long> ltimes=new ArrayList<Long>(times.keySet());
	    	Collections.shuffle(ltimes);
	    	boolean likelihood=true;
	    	//HashMap<Node,Double> pic=pi;
    		HashMap<Node,Double> pitemp=new HashMap<Node,Double>();
    		
	    	double sump=0.0;
	    	double sumn=0.0;
	    	
	    	for(long t:ltimes){
	    		pitemp=new HashMap<Node,Double>();
	    		HashSet<User> oldies=times.get(t-1);
	    		HashSet<User> nc=times.get(t);
	    		HashSet<User> future=times.get(t+1);
	    		if (nc!=null){
	    			for(User v:nc){
	    				ArrayList<Node> snodes=new ArrayList<Node>(subNodes.get(v));
	    				Collections.shuffle(snodes);
	 	                double sum=0.0;
	 	                int nb1=0;
	 	                for(Node nv:snodes){
	 	                	if(actifs.contains(nv)){
	 	                		nb1++;
	 	                	}
	 	                }
	 	                for(Node nv:snodes){
	 	                	if(nb1==0){actifs.add(nv); nb1++; throw new RuntimeException("pas d'actifs pour "+v+"!!");} // ne devrait pas arriver (sauf ptet au premier labelling)
	 	                	else{
	 	                		if((nb1==1) && (actifs.contains(nv))){
	 	                			// nothing to do => p(nv inactif = 0)
	 	                		}
	 	                		else{
	 	                			double pnv0Preds=1.0;
	 	                			double pnv1Preds=1.0;
	 	                			if (sansPreds.contains(v)){
	 	                				pnv0Preds=0.5;
	 	                				pnv1Preds=0.5;
	 	                			}
	 	                			else{
	 	                				HashMap<String,Link> preds=nv.getPredecesseurs();
	 	                				for(Link l:preds.values()){
	 	                					Node nu=l.getNode1();
	 	                					User u=parents.get(nu);
	 	                					if ((oldies.contains(u)) && (actifs.contains(nu))){
	 	                						double val=l.getVal();
	 	                						pnv0Preds*=(1.0-val);
	 	                					}
	 	                				}
	 	                				pnv1Preds=1.0-pnv0Preds; 	                				
	 	                			}
	 	                			double pSuccnv0=1.0;
	 	                			double pSuccnv1=1.0;
	 	                			HashMap<Node,Double> pWv1=new HashMap<Node,Double>();
	 	                			HashMap<Node,Double> pWv0=new HashMap<Node,Double>();
	 	                			HashMap<Node,Double> pWv1temp=new HashMap<Node,Double>();
	 	                			HashMap<Node,Double> pWv0temp=new HashMap<Node,Double>();
	 	                			HashMap<String,Link> succs=nv.getSuccesseurs();
	 	                			
		                			for(Link l:succs.values()){
		                					double val=l.getVal();
		                					Node nw=l.getNode2();
		                					User w=parents.get(nw);
		                					//Log.println(nv+" succ : "+nw);
		                					//if((actifs.contains(nw)) && ((seen.contains(w)) || (nc.contains(w)))){
		                					Long tw=conta.get(w);
		                					if((tw!=null) && (tw<=t)){ //(conta.contains(w)) || (nc.contains(w))){
		                						// w pas dependant de v
		                						//Log.println(nv+" succ : "+nw+" zap");
		                						continue;
		                					}
		                					boolean actif=actifs.contains(nw);
		                					double pw0v1=1.0;
		                					double pw0v0=1.0;
		                					boolean pwsok=false;
		                					Double pw0; 
		                					HashMap<Node,Double> pti=pitemp;
		                					if((tw!=null) && (tw==(t+1)) && (actif)){
		                						pti=pi;
		                			    	}
		                					pw0=pti.get(nw);
		                					
		                					if(pw0==null){
		                						pw0=1.0;
		                						HashMap<String,Link> predsw=nw.getPredecesseurs();
	                							for(Link lw:predsw.values()){
	                								double kvw=lw.getVal();
	        	                					Node npw=lw.getNode1();
	        	                					User pnpw=parents.get(npw);
	        	                					if (nc.contains(pnpw)){
	        	                					  if (actifs.contains(npw)){
	        	                						if (npw!=nv){
	        	                							pw0v0*=(1.0-kvw);
	        	                							//Log.println(nv+" pw0v0 de "+nw+"*=(1.0-lien avec "+npw+"="+kvw+")="+pw0v0);
	        	                						}
	        	                						pw0v1*=(1.0-kvw);
	        	                						//Log.println(nv+" pw0v1 de "+nw+"*=(1.0-lien avec "+npw+"="+kvw+")="+pw0v1);
	        	                						pw0*=(1.0-kvw);
	        	                					  }
	        	                					  else{
	        	                						if (npw==nv){
	          	                							pw0v1*=(1.0-kvw);
	          	                							//Log.println(nv+" pw0v1 de "+nw+"*=(1.0-lien avec "+npw+"="+kvw+")="+pw0v1);
	          	                						}  
	        	                					  }
	        	                					}
	                							}
	                							pwsok=true;
	                							pti.put(nw, 1.0-pw0);
		                					}
		                					else{
		                						pw0=1.0-pw0;
		                					}
		                					if (!pwsok){
		                					  if(actifs.contains(nv)){
		                						pw0v1=pw0;
		                						if(pw0>Double.MIN_VALUE){
		                							pw0v0=pw0/(1.0-val);
		                							if(Double.isInfinite(pw0v0)){
		                								throw new RuntimeException(nv+" pw0v0 infini pour"+nv+";"+nw+"! : pw0 = "+pw0+" val = "+val+"\n");
		                							}
		                						}
		                						else{
		                							// il y a eu un arrondi, on doit recalculer pw0
		                							System.out.println("Arrondi, recalcul de pw0 pour "+nw);
		                							pw0v0=1.0;
		                							HashMap<String,Link> predsw=nw.getPredecesseurs();
		                							for(Link lw:predsw.values()){
		                								double kvw=lw.getVal();
		        	                					Node npw=lw.getNode1();
		        	                					User pnpw=parents.get(npw);
		        	                					if ((npw!=nv) && (nc.contains(pnpw)) && (actifs.contains(npw))){
		        	                						pw0v0*=(1.0-kvw);
		        	                						
		        	                					}
		        	                					
		                							}
		             	                			
		                						}
		                					  }
		                					  else{
		                							pw0v0=pw0;
		                							pw0v1=pw0*(1.0-val);
		                							
		                					  }
		                					} 
		                					
		                					
	                						
	                						
		                					if ((actifs.contains(nw)) && (future.contains(w))){
		                						pSuccnv0*=(1.0-pw0v0);
		                						pSuccnv1*=(1.0-pw0v1);
		                						pWv0.put(nw, (1.0-pw0v0));
		                						pWv1.put(nw, (1.0-pw0v1));
		                					}
		                					else{
		                						pSuccnv0*=pw0v0;
		                						pSuccnv1*=pw0v1;
		                						pWv0temp.put(nw, (1.0-pw0v0));
		                						pWv1temp.put(nw, (1.0-pw0v1));
		                					}
		             
		                			}
		                			
		                			double pv0=pnv0Preds*pSuccnv0;
		                			double pv1=pnv1Preds*pSuccnv1;
		                			double x=Math.random()*(pv0+pv1);
	 	                			boolean maj0=false;
	 	                			boolean maj1=false; 
	 	                			if((x==0) || (x<pv0)){
	 	                				if(actifs.contains(nv)){
	 	                					actifs.remove(nv);
	 	                					maj0=true;
	 	                					nb1--;
	 	                					//System.out.println("sup pour cascade "+cascade+", noeud "+nv+" pv0="+pv0+" pv1="+pv1+" "+x);
	 	                					pi.remove(nv);
	 	                				}
	 	                			}
	 	                			else{
	 	                				if(!actifs.contains(nv)){
	 	                					actifs.add(nv);
	 	                					maj1=true;
	 	                					nb1++;
	 	                					//System.out.println("add cascade "+cascade+", noeud"+nv+" pv0="+pv0+" pv1="+pv1+" "+x);
	 	                					pi.put(nv,pnv1Preds);
	 	                				}
	 	                			}
	 	                			
	 	                			// Mise a jour des probas pi puisqu il y a eu un changement
	 	                			if((maj0) || (maj1)){
	 	                				HashMap<Node,Double> newpW=pWv0; 
	 	                				if(maj1){
	 	                					newpW=pWv1;
	 	                				}
	 	                				for(Node n:newpW.keySet()){
	 	                					pi.put(n, newpW.get(n));
	 	                				}	
	 	                				newpW=pWv0temp; 
	 	                				if(maj1){
	 	                					newpW=pWv1temp;
	 	                				}
	 	                				for(Node n:newpW.keySet()){
	 	                					pitemp.put(n, newpW.get(n));
	 	                				}
	 	                			
	 	                			}
	 	                		}
	 	                	}
	 	                }
	 	                
	 	                
	    			}
	    		}
	    		//oldies=noldies;
	    		//t++;
	    	}
	    	//System.out.println(actifs);
	    }
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
   
    
    public double modifVal(double val, int pass){
    	//double noise=1.0/(pass*0.2+100.0);
    	double noise=1.0/(pass+1000.0);
    	//double noise=1.0/(pass+10000000.0);
    	//int z=(int)(Math.random()*10.0)+1;
    	//if(pass%2!=0){
    	//	noise=0.0;
    	//}//
    	//double noise=1.0/(Math.log(pass)*10.0+100.0);
    	//double noise=1.0/(pass*0.01/(0.1*monte)+100.0);
    	//double noise=1.0/(((pass*0.01)*(0.1*monte))+100.0);
    	//double noise=0.0;
    	double x=Math.random()*noise;
    	x-=noise/2.0;
    	val+=x;
    	if(val>(1.0-(noise/2.0))){val=(1.0-(noise/2.0));}
		if(val<(noise/2.0)){val=(noise/2.0);}
		return(val);
    }
   
    private double compareDifProbasOnTrainWithArtificialModel(ArtificialModel mod){
    	if (!loaded){
        	load();
        }
    	double sum=0.0;
    	double std=0.0;
    	int nb=0;
    	for(Integer c:pactifTrain.keySet()){
    		HashMap<Node,Double> actifs=pactifTrain.get(c);
    		User.reinitUsers();
    		Post.reinitPosts();
    		Cascade cascade=Cascade.getCascadeFromDB(this.learnedFromDb,this.learnedFromCol,c);
    		HashMap<Integer,Double> w=cascade.getContentWeigths();
    		TreeMap<Integer,Double> content=new TreeMap<Integer,Double>();
			for(Integer v:w.keySet()){
				double d=w.get(v);
				content.put(v, d);
			}
    		HashMap<String,MultiInterestModel> userModels=mod.userModels;
    		HashMap<String,HashMap<String,Double>> artlinks=mod.links;
    		HashMap<User,Long> times=cascade.getUserContaminationSteps();
    		for(String pred:artlinks.keySet()){
    			HashMap<String,Double> succs=artlinks.get(pred);
    			User user=User.getUser(pred);
    			if(!times.containsKey(user)){
    				continue;
    			}
    			for(String succ:succs.keySet()){
    				User user2=User.getUser(succ);
    				/*if(!times.containsKey(user2)){
        				continue;
        			}*/
    				double pmod=mod.getProba(pred, succ, content);
    				double p=getProbaTrain(c,pred,succ,content);
    				//double dif=Math.pow(p-pmod,2)/pmod;
    				double dif=Math.abs(p-pmod);
    				System.out.println("Cascade "+c+" "+pred+"=>"+succ+" : hyp "+p+", ref "+pmod+", dif "+dif);
    				nb++;
    				sum+=dif;
    				std+=dif*dif;
    			}
    		}
    	}
    	if(nb==0){
    		return 0.0;
    	}
    	std=std/nb;
    	sum/=nb;
    	std=std-sum*sum;
    	if(nb>1){
    		std*=nb/(nb-1);
    	}
    	std=Math.sqrt(std);
    	System.out.println("moy = "+sum+", std = "+std);
    	return(sum);
    }
    private double getProbaTrain(int c,String pred,String succ,TreeMap<Integer,Double> content){
    	System.out.println("Proba "+c+" "+pred+" "+succ);
    	HashMap<Node,Double> actifs=pactifTrain.get(c);
    	//System.out.println("nb actifs "+actifs.size());
    	
    	ArrayList<Node> nodes=userNodes.get(succ);
    	ArrayList<Node> pnodes=userNodes.get(pred);
    	double p=1.0;
    	double mp=0.0;
    	for(Node node:nodes){
    		double pn=1.0;
    		HashMap<String,Link> preds=node.getPredecesseurs();
    		for(Link l:preds.values()){
    			Node n=l.getNode1();
    			if(pnodes.contains(n)){
    				//System.out.println(l);
    				Double pa=actifs.get(n);
    				pa=(pa==null)?0.0:pa;
    				p*=1.0-(l.getVal()*pa);
    				/*pn=(l.getVal()*pa);///(pnodes.size()*nodes.size());
    				if(pn>p){
    					p=pn;
    					//p=l.getVal();
    				}*/
    			}
    		}
    		//p*=(1.0-pn);
    		//Double pa=actifs.get(node);
    		//p+=((pa==null)?0.0:pa)*(1.0-pn);
    		/*if(pn>p){
    			p=pn;
    		}*/
    	}
    	return 1.0-p; //p/nodes.size();
    }
    
    
   
    
    /**
     * @param args
     */
    public static void main(String[] args) {
    	
         
    	ICLSN2 mod=new ICLSN2(); //"propagationModels/ICLSN_nbNodes=4_step=1_cascades=artificial_10_users=users_1_linkThreshold=2.0_maxIter=1_sansStepsVides"); //"propagationModels/ICLSN_nbNodes=1_step=1_cascades=artificial_10_users=users_1_linkThreshold=2.0_maxIter=1_sansStepsVides_infiniteDelay");
    	//mod.load();
    	mod.testMode=false;
        mod.learn("us_elections5000", "artificial_10", "users_1", 2, 1, 1, 2, 0.1, 0.3,true,1000,3,1,true,false,false) ;
        mod.save();
        
    
    	//ICmodel2 myModel = new ICmodel2("propagationModels/ICmodel2_3600_1_cascades2_users1s.txt",50) ;
        //ICLSN2 mod=new ICLSN2("propagationModels/ICLSN_nbNodes=2_step=1_cascades=artificial_2_users=users_1_linkThreshold=10.0_maxIter=1_sansStepsVides_infiniteDelay_onlyOne_considerPastNodes");
    	/*ICLSN2 mod=new ICLSN2("propagationModels/ICLSN_nbNodes=4_step=1_cascades=artificial_10_users=users_1_linkThreshold=2.0_maxIter=1_sansStepsVides_infiniteDelay");
    	
    	ArtificialModel art=new ArtificialModel();
        art.setModelFile("propagationModels/ArtificialModel_dim1_nMods1_usersusers_1_nbUsers100_linkThreshold2.0");
        art.load();
        mod.compareDifProbasOnTrainWithArtificialModel(art);
        */
    	/*
    	System.out.println(mod.userNodes);
   	 HashMap<String,Long> init=new HashMap<String,Long>();
        init.put("user_5",new Long(1));
        init.put("user_1",new Long(1));
        PropagationStruct ps=new PropagationStruct(new Cascade(1,"x",new HashSet<Post>()),init, new ArrayList<HashMap<String,Double>>());
       
   	mod.inferProbasGibbs(ps);*/
    }

    /*public static void main(String[] args) {
	Runtime.getRuntime().addShutdownHook(new Thread(){
		public void run(){
			while(true){
		    	 System.out.println("yy");
		    	}
		}
	});
	while(true){
	 System.out.println("xx");
	}
}*/
}