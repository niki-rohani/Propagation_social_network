package propagationModels;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

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
//Independent Cascade between Latent Sub-Nodes 
//Meme que 2 mais optimisation user apres user
public class ICLSN3 implements PropagationModel {
   
	private Random r;
	private int maxIter;
	private String modelFile;
	private boolean loaded=false;
	private int nbSubNodes;
	private boolean inferProbas=true;
	
	public ICLSN3(int nbSubNodes,int maxIter){
		this("",nbSubNodes,maxIter);
	}
	
	public ICLSN3(String modelFile,int nbSubNodes,int maxIter){
		this.modelFile=modelFile;
		this.maxIter=maxIter;
		r = new Random() ;
		this.nbSubNodes=nbSubNodes;
	}
	
	public ICLSN3(String modelFile,int nbSubNodes,int maxIter,boolean inferProbas){
		this.modelFile=modelFile;
		this.maxIter=maxIter;
		r = new Random() ;
		this.nbSubNodes=nbSubNodes;
		this.inferProbas=inferProbas;
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
		return("ICLSN_nbSubNodes="+nbSubNodes+"_maxIter="+maxIter+"_"+sm);
	}
	public String getName(){
		String sm=modelFile.replaceAll("/", "__");
		return sm;
	}
	public int infer(Structure struct) {
		if(inferProbas){
			return(inferProbas(struct));
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
        int tstart=firstNewT;
        //this.alreadyTried = new HashMap<User, HashSet<User>>() ;
        //this.trying = new HashMap<User, HashSet<User>>() ;
       
        HashMap<String,Double> contagious = infections.get(infections.lastKey()); 
         
        User currentUser ;
        HashMap<String,Double> infectedstep=new HashMap<String,Double>();
        for(int iteration = tstart ; iteration <= maxIter ; iteration++) {
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
   
   
    public void load(){
		String filename=modelFile;
        User.reinitAllLinks();
        BufferedReader r;
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          while((line=r.readLine()) != null) {
        	String[] tokens = line.split("\t") ;
            if(tokens[2].startsWith("NaN"))
                tokens[2]="0.0" ;
           
            double d = Double.parseDouble(tokens[2]) ;
            if(d==0)
                continue ;
            User source=User.getUser(tokens[0]);
            User target=User.getUser(tokens[1]);
            Link l=new Link(source,target,d);
            source.addLink(l);
            //System.out.println("new link "+l);
          }
          r.close();
        }
        catch(IOException e){
        	System.out.println("Probleme lecture modele "+filename);
        }
        
    }

    public void save() {
		String filename=modelFile;
        try{
          PrintStream p = new PrintStream(filename) ;
          for(User uS : User.users.values()) {
            HashMap<String,Link> succs=uS.getSuccesseurs();
            for(Link lsuc: succs.values()) {
                p.println(uS.getName()+"\t"+lsuc.getNode2().getName()+"\t"+lsuc.getVal());
            }
          }
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
   
 	
 
   
 // "Prediction of Information Diffusion Probabilities for Independent Cascade Model"
    public void learn(String db, String cascadesCollection,String usersCollection,int maxIter, int step, double userLinkThreshold, double min_init, double max_init, boolean zapVides) {
       
    	boolean displayLikelihood=true;
       
        // Pour chaque cascade, liste id users avec leur temps de contamination
        HashMap<Integer,HashMap<User,Long>> userTimeContamination=getTimeSteps(db,cascadesCollection,step,zapVides);
        
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
        
        
        HashSet<User> users=new HashSet<User>(User.users.values());
        // Pour tous les utilisateurs on recupere leurs liens 
        for(User u:users){
        	u.loadLinksFrom(db, usersCollection, userLinkThreshold);
        	/*for(User u2:users){
        		if(u.getID()!=u2.getID()){
        			Link l=new Link(u,u2,1.0);
        			u.addLink(l);
        			u2.addLink(l);
        		}
        	}*/
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
                	if ((t2==null) || (t2>(t1+1))){
                		HashSet<Integer> cneg=sneg.get(l);
                        if (cneg==null){
                            cneg=new HashSet<Integer>();
                            sneg.put(l, cneg);
                        }
                        cneg.add(c);
                	}
                	else{
                		if (t2==(t1+1)){
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
            for(Link l:succs.values()){
        		if (spos.containsKey(l)){
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
            }
            //u1.reinitLinks(true);
        }
       
         
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
       
        //System.out.println(actifs);
        //relabelActifs(actifs, subNodes, parents, times, sansPreds,userTimeContamination);
        //System.out.println("compute probas");
        double likelihood=computeProbaNodes(Pi,actifs,subNodes,parents,times,sansPreds);
        System.out.println("likelihood = "+likelihood);
        for(User v:cascades.keySet()){
        	optimize(v,cascades.get(v),actifs,subNodes,parents,times,sansPreds,userTimeContamination,Pi,spos,sneg,1,1);
        }
        likelihood=computeProbaNodes(Pi,actifs,subNodes,parents,times,sansPreds);
        System.out.println("likelihood = "+likelihood);
        /*System.out.println(times);
        System.out.println(actifs);
        System.out.println(Pi);*/
        ArrayList<Integer> cids=new ArrayList<Integer>(userTimeContamination.keySet());
        ArrayList<User> listUsers=new ArrayList<User>(cascades.keySet());
        boolean go=true;
        boolean last=false;;
        int pass=0;
        double oldl=likelihood;
        int nbmonte=1;
        while(go){
        	pass++;
        	if(pass>10000000){
        		last=true;
        	}
        	if(last){
        		go=false;
        	}
        	
        	// on choisit une cascade et un pas de temps
        	/*int x=(int)(Math.random()*cids.size());
        	int c=cids.get(x);
        	HashMap<Long,HashSet<User>> tc=times.get(c);
        	ArrayList<Long> ctimes=new ArrayList<Long>(tc.keySet());
        	x=(int)(Math.random()*ctimes.size());
        	long t=ctimes.get(x);
        	*/
        	
        	// on choisit un user
        	int x=(int)(Math.random()*listUsers.size());
        	User user=listUsers.get(x);
        	
        	
        	//reinitVals(subNodes,1.0/(pass*0.001+100.0));
        	//likelihood=computeProbaNodes(Pi,actifs,subNodes,parents,times,sansPreds);
        	// Pi=new HashMap<Integer,HashMap<Node,Double>>();
        	// relabel des nodes associes a ce user pour toutes les cascades 
        	relabel(user,cascades.get(user),actifs,subNodes,parents,times,sansPreds,userTimeContamination,Pi);
        	//System.out.println(times);        	
        	//System.out.println(actifs);
        	//System.out.println(Pi);
        	//likelihood=computeProbaNodes(Pi,actifs,subNodes,parents,times,sansPreds);
            //System.out.println("avant "+likelihood); 
        	
        	
        	// optimisation des arcs successeurs de ces nodes
        	int nbit=pass;
        	/*if(pass%100==0){
        		nbit=Integer.MAX_VALUE;
        	}*/
        	optimize(user,cascades.get(user),actifs,subNodes,parents,times,sansPreds,userTimeContamination,Pi,spos,sneg,nbit,nbmonte);
        	
        	if(pass%1000==0){
        		likelihood=computeProbaNodes(Pi,actifs,subNodes,parents,times,sansPreds);
        		System.out.println("nb passes : "+pass+", likelihood ============> "+likelihood); 
        		if(likelihood>oldl){
        			nbmonte++;
        		}
        		oldl=likelihood;
        	}
        	//reinitVals(subNodes,1.0/(pass*0.1+100.0));
        	
        	/*likelihood=computeProbaNodes(Pi,actifs,subNodes,parents,times,sansPreds);
            System.out.println(likelihood); */
        	
        	
        }
        likelihood=computeProbaNodes(Pi,actifs,subNodes,parents,times,sansPreds);
        System.out.println("Likelihood = "+likelihood);
        /*System.out.println(times);
        System.out.println(actifs);
        System.out.println(Pi);
        */
        
        loaded=true;
        if (modelFile.length()==0){
    		modelFile="propagationModels/ICLSN_nbNodes="+this.nbSubNodes+"_step="+step+"_cascades="+cascadesCollection+"_users="+usersCollection+"_linkThreshold="+userLinkThreshold+"_maxIter="+maxIter+((zapVides)?"_sansStepsVides":"");
    	}
    }
   
    public void optimize(User v, HashSet<Integer> cascades, HashMap<Integer,HashSet<Node>> actifsC, HashMap<User,ArrayList<Node>> subNodes, HashMap<Node,User> parents, HashMap<Integer,HashMap<Long,HashSet<User>>> times, HashMap<Integer,HashSet<User>> sansPredsC, HashMap<Integer,HashMap<User,Long>> contaC, HashMap<Integer,HashMap<Node,Double>> pi,HashMap<Link,HashSet<Integer>> spos,HashMap<Link,HashSet<Integer>> sneg, int pass, int monte){
    	HashMap<String,Link> succs=v.getSuccesseurs();
    	ArrayList<Node> vnodes=subNodes.get(v);
    	Collections.shuffle(vnodes);
    	for(Link l:succs.values()){
    		User w=(User)l.getNode2();
    		ArrayList<Node> wnodes=subNodes.get(w);
    		HashSet<Integer> cpos=spos.get(l);
    		HashSet<Integer> cneg=sneg.get(l);
    		if ((cpos==null) || (cpos.size()==0)){
    			// Aucune cascade poositive pour ce couple d'utilisateurs, on le passe
    			continue;
    		}
    		
    		for(Node nv:vnodes){
    			int nbNeg=0;
    			if(cneg!=null){
    				for(Integer c:cneg){
    					HashSet<Node> actifsc=actifsC.get(c);
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
    				for(Integer c:cpos){
    					HashSet<Node> actifsc=actifsC.get(c);
    					if(!actifsc.contains(nv)){ 
    						continue;
    					}
    					else{ok=true;}
    					
    					//System.out.println("ok "+nv);
    					HashMap<Node,Double> pic=pi.get(c);
    					if (pic==null){
    						throw new RuntimeException("Pas de probas pour noeuds de cascade "+c+"!!");
    					}
    					
    					boolean actif=false;
    					if(actifsc.contains(nw)){
    						//System.out.println("ok "+nw);
    						actif=true;
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
    							throw new RuntimeException(e+"\n Pas de score pcwj pour "+nw+" alors que "+nv+" pointe sur lui ("+numer+") ");
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
    					throw new RuntimeException("Nan :"+numer+" "+sumNumer+" "+sumDenom+" pour nv "+nv+" et nw"+nw+"\n");
        				
    				}
    				if(Double.isInfinite(kviwj)){
    					//numer=1.0-Double.MIN_VALUE;
    					throw new RuntimeException("Infinite :"+numer+" "+sumNumer+" "+sumDenom+" pour nv "+nv+" et nw"+nw+"\n");
        				
    				}
    				if(kviwj>1){
    					if(kviwj>1.01){
    						throw new RuntimeException(kviwj+";"+numer+";"+sumDenomNeg+";"+sumDenomPos);
    					}
    					kviwj=1.0;
					}
    				kviwj=modifVal(kviwj,pass,monte);
    				ln.setVal(kviwj); 
    			    if(casv.size()>0){
    			    	majPi(oldk,kviwj,pi,nw,casv,actifsC,subNodes,parents,times,contaC);
    			    }
    				if (numer!=oldk){
    					//nbChanges++;
    				}
    				
    			}
    		}
    	}
    }
    
    public void majPi(double oldV,double newV,HashMap<Integer,HashMap<Node,Double>> pi,Node n,HashSet<Integer> cascades, HashMap<Integer,HashSet<Node>> actifs, HashMap<User,ArrayList<Node>> subNodes, HashMap<Node,User> parents, HashMap<Integer,HashMap<Long,HashSet<User>>> times, HashMap<Integer,HashMap<User,Long>> contaC){
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
    			Long tv=t-1;
    			HashMap<Long,HashSet<User>> tc=times.get(c);
    			HashSet<User> vusers=tc.get(tv);
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
    
    public void relabel(User v, HashSet<Integer> cascades, HashMap<Integer,HashSet<Node>> actifsC, HashMap<User,ArrayList<Node>> subNodes, HashMap<Node,User> parents, HashMap<Integer,HashMap<Long,HashSet<User>>> times, HashMap<Integer,HashSet<User>> sansPredsC, HashMap<Integer,HashMap<User,Long>> contaC, HashMap<Integer,HashMap<Node,Double>> pi){
		//System.out.println("relabel "+v);
    	for(Integer c:cascades){
    		HashMap<User,Long> conta=contaC.get(c);
    		Long t=conta.get(v);
    		HashSet<User> oldies=times.get(c).get(t-1);
    		HashSet<User> nc=times.get(c).get(t);
    		HashSet<User> future=times.get(c).get(t+1);
    		//HashMap<Node,Double> pti=new HashMap<Node,Double>(); //pi.get(c);
    		//pi.put(c, pti);
    		HashMap<Node,Double> pic=pi.get(c);
    		HashMap<Node,Double> pitemp=new HashMap<Node,Double>();
    		
    		HashSet<Node> actifs=actifsC.get(c);
    		HashSet<User> sansPreds=sansPredsC.get(c);
    		
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
            	if(nb1==0){actifs.add(nv); nb1++; throw new RuntimeException("nb 0 !!");} // ne devrait pas arriver (sauf ptet au premier labelling)
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
        						pti=pic;
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
        							//System.out.println("Arrondi, recalcul de pw0 pour "+nw);
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
        					
        					
    						
    						
        					if (actif && (future.contains(w))){
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
            					//System.out.println("sup pour cascade "+c+", noeud "+nv+" pv0="+pv0+" pv1="+pv1+" "+x);
            					pic.remove(nv);
            				}
            			}
            			else{
            				if(!actifs.contains(nv)){
            					actifs.add(nv);
            					maj1=true;
            					nb1++;
            					//System.out.println("add pour cascade "+c+", noeud "+nv+" pv0="+pv0+" pv1="+pv1+" "+x);
            					pic.put(nv,pnv1Preds);
            				}
            			}
            			
            			// Mise a jour des probas pi puisqu il y a eu un changement
            			if((maj0) || (maj1)){
            				HashMap<Node,Double> newpW=pWv0; 
            				if(maj1){
            					newpW=pWv1;
            				}
            				for(Node n:newpW.keySet()){
            					pic.put(n, newpW.get(n));
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
    
    public double modifVal(double val, int pass, int monte){
    	double noise=1.0/(pass*0.001+100.0);
    	//int z=(int)(Math.random()*10.0)+1;
    	/*if(pass%2!=0){
    		noise=0.0;
    	}*/
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
   
    public double computeProbaNodes(HashMap<Integer,HashMap<Node,Double>> Pi, HashMap<Integer,HashSet<Node>> actifs,HashMap<User,ArrayList<Node>> subNodes,HashMap<Node,User> parents,HashMap<Integer,HashMap<Long,HashSet<User>>> times,HashMap<Integer,HashSet<User>> sansPreds){ //, boolean recompute){
    	double suml=0.0;
    	System.out.println("Compute Probas");
    	ArrayList<ProbaComputer> computers=new ArrayList<ProbaComputer>(); 
    	for(Integer c:times.keySet()){
    		HashMap<Node,Double> pic=Pi.get(c);
    		//if(pic==null){
    			pic=new HashMap<Node,Double>();
    			Pi.put(c, pic);
    		//}
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
    		if(computers.size()==100){
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
    			if(computers.size()==100){
    				ProbaComputer computer=computers.get(0);
    				try{
    					computer.join();
    					computer.stop();
    					computer.interrupt();
    					suml+=computer.like;
    					computers.remove(0);
    				}
    				catch(Exception e){
    					e.printStackTrace();
    					throw new RuntimeException("Pb thread : "+e);
    				}
    			}
    			
    		}
    		ProbaComputer computer=new ProbaComputer(c,pic,actifsc,subNodes,parents,tc,sansPredsc);
			computer.start();
			computers.add(computer);
    	}
    	for(ProbaComputer computer:computers){
    		try{
				computer.join();
				computer.stop();
				computer.interrupt();
				suml+=computer.like;
			}
			catch(Exception e){
				e.printStackTrace();
				throw new RuntimeException("Pb thread : "+e);
			}
		}
    	
    	return(suml);
    }
    
    private class ProbaComputer extends Thread{
    	HashMap<Node,Double> pi;
    	HashSet<Node> actifs;
    	HashMap<User,ArrayList<Node>> subNodes;
    	HashMap<Node,User> parents; 
    	HashMap<Long,HashSet<User>> times;
    	HashSet<User> sansPreds;
    	double like;
    	int cascade;
    	boolean fini=false;
    	public ProbaComputer(int cascade,HashMap<Node,Double> pi, HashSet<Node> actifs, HashMap<User,ArrayList<Node>> subNodes, HashMap<Node,User> parents, HashMap<Long,HashSet<User>> times, HashSet<User> sansPreds){
    		this.pi=pi;
    		this.actifs=actifs;
    		this.subNodes=subNodes;
    		this.parents=parents;
    		this.times=times;
    		this.sansPreds=sansPreds;
    		like=0.0;
    		this.cascade=cascade;
    	}
    	
    	public void run(){
    		like=computeProbaNodesCascade();
    		fini=true;
    	}
    	
    	public double computeProbaNodesCascade() { //,boolean meme0){
	    	long t=1;
	    	int nbTimes=times.size();
	    	boolean likelihood=true;
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
			                		StringBuilder s=new StringBuilder();
			                		HashMap<String,Link> preds=nw.getPredecesseurs();
			                		for(Link l:preds.values()){
			                			Node nv=l.getNode1();
			                			User v=parents.get(nv);
			                			s.append("\t Pred "+nv+" oldies="+oldies.contains(v));
			                			if ((oldies.contains(v)) && (actifs.contains(nv))){
			                				double val=l.getVal();
			                				pwj*=(1.0-val);
			                				s.append(" kviwj "+val+" pwj "+pwj);
			                				//Log.println("nw selon "+nv+" val = "+val+" => "+(1.0-val)+" ; pwj =  "+pwj);
			                			}
			                			s.append("\n");
			                					                			
			                		}
			                		pwj=1.0-pwj;
			                		if (pwj<Double.MIN_VALUE){
		        						pwj=Double.MIN_VALUE;
		        						System.out.println(nw+" est a 0 pour cascade "+cascade+"\n "+s);
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
		                if ((likelihood)  && (!sansPreds.contains(w))){
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
    
    public void relabelActifs(HashMap<Integer,HashSet<Node>> actifs,HashMap<User,ArrayList<Node>> subNodes,HashMap<Node,User> parents,HashMap<Integer,HashMap<Long,HashSet<User>>> times,HashMap<Integer,HashSet<User>> sansPreds,HashMap<Integer,HashMap<User,Long>> userTimeContamination){
    	double suml=0.0;
    	ArrayList<Integer> cas=new ArrayList<Integer>(times.keySet());
    	HashSet<Integer> relab=new HashSet<Integer>();
    	int nb=0;
    	int nbrelab=5;
    	if(nbrelab>0){
    		while((nb<nbrelab) && (nb<cas.size())){
    			int x=(int)(Math.random()*cas.size());
    			int c=cas.get(x);
    			if(!relab.contains(c)){
    				relab.add(c);
    			}
    			nb++;
    		}
    	}
    	else{
    		relab=new HashSet<Integer>(times.keySet());
    	}
    	ArrayList<ActifsRelabeller> relabellers=new ArrayList<ActifsRelabeller>();
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
    		
    		
    		if(relabellers.size()==100){
    			ArrayList<ActifsRelabeller> asup=new ArrayList<ActifsRelabeller>();
    			for(ActifsRelabeller relabeller:relabellers){
    				if(relabeller.fini){
    					asup.add(relabeller);
    				}
    			}
    			for(ActifsRelabeller relabeller:asup){
    				relabellers.remove(relabeller);
    			}
    			if(relabellers.size()==100){
    				ActifsRelabeller relabeller=relabellers.get(0);
    				try{
    					relabeller.join();
    					relabeller.stop();
    					relabeller.interrupt();
    					relabellers.remove(0);
    				}
    				catch(Exception e){
    					e.printStackTrace();
    					throw new RuntimeException("Pb thread : "+e);
    				}
    			}
    			
    		}
    		ActifsRelabeller relabeller=new ActifsRelabeller(actifsc,subNodes,parents,tc,sansPredsc,conta);
    		relabeller.start();
    		relabellers.add(relabeller);
    		//suml+=like;
    	}
    	for(ActifsRelabeller relabeller:relabellers){
    		try{
				relabeller.join();
				relabeller.stop();
				relabeller.interrupt();
			}
			catch(Exception e){
				e.printStackTrace();
				throw new RuntimeException("Pb thread : "+e);
			}
    	}
    	//return(suml);
    }
    
		
	
    
    private class ActifsRelabeller extends Thread{
    	HashSet<Node> actifs;
    	HashMap<User,ArrayList<Node>> subNodes;
    	HashMap<Node,User> parents;
    	HashMap<Long,HashSet<User>> times;
    	HashSet<User> sansPreds;
    	HashMap<User,Long> conta;
    	boolean fini=false;
    	public ActifsRelabeller(HashSet<Node> actifs, HashMap<User,ArrayList<Node>> subNodes, HashMap<Node,User> parents, HashMap<Long,HashSet<User>> times, HashSet<User> sansPreds,HashMap<User,Long> conta){
    		this.actifs=actifs;
    		this.conta=conta;
    		this.subNodes=subNodes;
    		this.parents=parents;
    		this.times=times;
    		this.sansPreds=sansPreds;
    	}
    	
    	public void run(){
    		relabelActifsCascade();
    		fini=true;
    	}
    	public void relabel(int c, long t, HashSet<Node> actifs, HashMap<User,ArrayList<Node>> subNodes, HashMap<Node,User> parents, HashMap<Long,HashSet<User>> times, HashSet<User> sansPreds,HashMap<User,Long> conta, HashMap<Node,Double> pi){
    		HashSet<User> oldies=times.get(t-1);
    		HashSet<User> nc=times.get(t);
    		HashSet<User> future=times.get(t+1);
    		HashMap<Node,Double> pti=pi;
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
 	                	if(nb1==0){actifs.add(nv); nb1++;} // ne devrait pas arriver (sauf ptet au premier labelling)
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
	                					double pw0v1=1.0;
	                					double pw0v0=1.0;
	                					boolean pwsok=false;
	                					Double pw0=pti.get(nw);
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
	                					
	                					pWv0.put(nw, (1.0-pw0v0));
                						pWv1.put(nw, (1.0-pw0v1));
                						
                						
	                					if ((actifs.contains(nw)) && (future.contains(w))){
	                						pSuccnv0*=(1.0-pw0v0);
	                						pSuccnv1*=(1.0-pw0v1);
	                					}
	                					else{
	                						pSuccnv0*=pw0v0;
	                						pSuccnv1*=pw0v1;	
	                					}
	             
	                			}
	                			
	                			double pv0=pnv0Preds*pSuccnv0;
	                			double pv1=pnv1Preds*pSuccnv1;
	                			double x=Math.random()*(pv0+pv1);
 	                			boolean maj0=false;
 	                			boolean maj1=false; 
 	                			if(x<pv0){
 	                				if(actifs.contains(nv)){
 	                					actifs.remove(nv);
 	                					maj0=true;
 	                					nb1--;
 	                				}
 	                			}
 	                			else{
 	                				if(!actifs.contains(nv)){
 	                					actifs.add(nv);
 	                					maj1=true;
 	                					nb1++;
 	                				}
 	                			}
 	                			
 	                			// Mise a jour des probas pi puisqu il y a eu un changement
 	                			if((maj0) || (maj1)){
 	                				HashMap<Node,Double> newpW=pWv0; 
 	                				if(maj1){
 	                					newpW=pWv1;
 	                				}
 	                				for(Node n:newpW.keySet()){
 	                					pti.put(n, newpW.get(n));
 	                				}
 	                			}
 	                		}
 	                	}
 	                }
 	                
 	                
    			}
    		}
	    	
    	}
	    public void relabelActifsCascade(){
	    	ArrayList<Long> ltimes=new ArrayList<Long>(times.keySet());
	    	Collections.shuffle(ltimes);
	    	boolean likelihood=true;
	    	HashMap<Node,Double> pti=new HashMap<Node,Double>();
	    	double sump=0.0;
	    	double sumn=0.0;
	    	
	    	for(long t:ltimes){
	    		pti=new HashMap<Node,Double>();
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
	 	                	if(nb1==0){actifs.add(nv); nb1++;} // ne devrait pas arriver (sauf ptet au premier labelling)
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
		                					double pw0v1=1.0;
		                					double pw0v0=1.0;
		                					boolean pwsok=false;
		                					Double pw0=pti.get(nw);
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
		                					
		                					pWv0.put(nw, (1.0-pw0v0));
	                						pWv1.put(nw, (1.0-pw0v1));
	                						
	                						
		                					if ((actifs.contains(nw)) && (future.contains(w))){
		                						pSuccnv0*=(1.0-pw0v0);
		                						pSuccnv1*=(1.0-pw0v1);
		                					}
		                					else{
		                						pSuccnv0*=pw0v0;
		                						pSuccnv1*=pw0v1;	
		                					}
		             
		                			}
		                			
		                			double pv0=pnv0Preds*pSuccnv0;
		                			double pv1=pnv1Preds*pSuccnv1;
		                			double x=Math.random()*(pv0+pv1);
	 	                			boolean maj0=false;
	 	                			boolean maj1=false; 
	 	                			if(x<pv0){
	 	                				if(actifs.contains(nv)){
	 	                					actifs.remove(nv);
	 	                					maj0=true;
	 	                					nb1--;
	 	                				}
	 	                			}
	 	                			else{
	 	                				if(!actifs.contains(nv)){
	 	                					actifs.add(nv);
	 	                					maj1=true;
	 	                					nb1++;
	 	                				}
	 	                			}
	 	                			
	 	                			// Mise a jour des probas pi puisqu il y a eu un changement
	 	                			if((maj0) || (maj1)){
	 	                				HashMap<Node,Double> newpW=pWv0; 
	 	                				if(maj1){
	 	                					newpW=pWv1;
	 	                				}
	 	                				for(Node n:newpW.keySet()){
	 	                					pti.put(n, newpW.get(n));
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
   
   
   
    
   
   
    /**
     * @param args
     */
    public static void main(String[] args) {
       
        //ICmodel2 myModel = new ICmodel2("propagationModels/ICmodel2_3600_1_cascades2_users1s.txt",50) ;
        ICLSN3 mod=new ICLSN3(3,50);
        mod.learn("us_elections5000", "cascades_4", "users_1", 1, 3600, 10, 0.1, 0.3,true) ;
        mod.save();
    }


}