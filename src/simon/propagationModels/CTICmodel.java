package simon.propagationModels;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import propagationModels.PropagationModel;
import propagationModels.PropagationStruct;
import actionsBD.MongoDB;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import cascades.Cascade;
import cascades.CascadesProducer;
import core.Link;
import core.Post;
import core.Structure;
import core.User;

public class CTICmodel implements PropagationModel {

	
	private String modelFile ;
	Random rand = new Random();
	private boolean loaded=false ;
	
	public CTICmodel(String string) {
		modelFile=string ;
	}

	public HashSet<String> getUsers(){
		if(!loaded){
			try{
				load();
			}
			catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		return new HashSet<String>(User.users.keySet());
	}
	
	public int getContentNbDims(){
		
		return 0;
	}
	
	public String getName(){
		String sm=modelFile.replaceAll("/", "__");
		return sm;
	}
	
	@Override
	public void load() {
		String filename=modelFile;
		if(filename=="")
			return ;
        User.reinitAllLinks();
        BufferedReader r;
        try{
          r = new BufferedReader(new FileReader(filename)) ;
          String line ;
          while((line=r.readLine()) != null) {
        	String[] tokens = line.split("\t") ;
            if(tokens[2].startsWith("NaN"))
                tokens[2]="0.0" ;
           
            
            double p = Double.parseDouble(tokens[2]) ;
            double d = Double.parseDouble(tokens[3]) ;
            HashMap<Integer,Double> w = new HashMap<Integer, Double>(2);
            w.put(0,p) ; w.put(1,d) ;
            if(p==0)
                continue ;
            User source=User.getUser(tokens[0]);
            User target=User.getUser(tokens[1]);
            Link l=new Link(source,target,0) ;
            l.setWeights(w) ;
            source.addLink(l);
            //System.out.println("new link "+l);
          }
          r.close();
        }
        catch(IOException e){
        	System.out.println("Probleme lecture modele "+filename);
        }
        
	}

	@Override
	public void save() throws IOException {
		// TODO Auto-generated method stub.
	}
	
	
	// retourne une table cascade_id,user => time contamination
 	public static HashMap<Integer,HashMap<User,Long>> getTimeSteps(String db, String collection,int step){
         HashMap<Integer,HashMap<User,Long>> userTimeContamination=new HashMap<Integer,HashMap<User,Long>>();
         DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
         DBCursor cursor = col.find();
         Post p=null;
         try {
             while(cursor.hasNext()) {
                 DBObject res=cursor.next();
                 Cascade c=Cascade.getCascadeFrom(res);
                 int ic=c.getID();
                
                 HashMap<User,Long> ihc=c.getUserContaminationsSteps(-1,step);
                 userTimeContamination.put(c.getID(),ihc);
                 
                 User.reinitAllPosts(); // Pour alleger, on supprime les textes des posts dont on ne se sert pas ici
                 Post.reinitPosts();
                 System.out.println("Cascade "+ic+" chargee");
             }
         } finally {
             cursor.close();
         }
         return(userTimeContamination);
        
     }
 	
 	public String toString() {
 		return "Modele CTIC "+modelFile ;
 	}
	
	public void learn(String db, String cascadesCollection,String usersCollection,int maxIter, double userLinkThreshold,double min_init_d, double max_init_d, double min_init_p, double max_init_p) {
		
		// Initialisation.
		HashMap<Integer,HashMap<User,Long>> cascades = getTimeSteps(db,cascadesCollection,1) ;
		HashSet<User> users=new HashSet<User>(User.users.values());
        // Pour tous les utilisateurs on recupere leurs liens
		for(User u:users){
	            u.loadLinksFrom(db, usersCollection, userLinkThreshold);
	            System.out.println("Liens user "+u+" charge");
	    }
       
        HashSet<Link> allLinks=new HashSet<Link>();
        
        // Tous les liens obtiennent une valeur arbitraire tiree entre min_init et max_init
        double dif_p=max_init_p-min_init_p;
        double dif_d=max_init_d-min_init_d;
        for(User u:User.users.values()){
            HashMap<String,Link> succs=u.getSuccesseurs();
            for(Link l:succs.values()){
            	HashMap<Integer,Double> params = new HashMap<Integer, Double>();
                double v=(Math.random()*dif_p)+min_init_p;
                params.put(0, v) ;
                v=(Math.random()*dif_d)+min_init_d ;
                params.put(1, v) ;
                l.setWeights(params) ;
                allLinks.add(l) ;
            }
        }
        
        HashMap<Link,HashSet<Integer>> Mplus = new HashMap<Link,HashSet<Integer>>() ;
        HashMap<Link,HashSet<Integer>> Mminus = new HashMap<Link,HashSet<Integer>>() ;
        
        HashMap<Integer,HashMap<Link,Double>> A = new HashMap<Integer, HashMap<Link,Double>>() ;
        HashMap<Integer,HashMap<Link,Double>> B = new HashMap<Integer, HashMap<Link,Double>>() ;
        
        HashMap<Integer,HashMap<Link,Double>> a = new HashMap<Integer, HashMap<Link,Double>>() ;
        HashMap<Integer,HashMap<Link,Double>> b = new HashMap<Integer, HashMap<Link,Double>>() ;
        
        
        
        
        
        for(Link l : allLinks) {
        	Mplus.put(l, new HashSet<Integer>()) ;
        	Mminus.put(l, new HashSet<Integer>()) ;
        }
        
        
        // Estimation iterative.
        boolean keepOnGoing = true ;
        boolean firstTime = true ;
        for(int iii=0 ; iii<maxIter ; iii++) {
        	
        	for(int cascadeId : cascades.keySet()) {
        		HashMap<User,Double> sumAB = new HashMap<User, Double>() ; // Le denominateur du calcul de a.
        		for(Link l : allLinks) {
        			double k = l.getWeights().get(0) ;
        			double r = l.getWeights().get(1) ;
        			Long tmu = cascades.get(cascadeId).get((User)l.getNode1()) ;
        			Long tmv = cascades.get(cascadeId).get((User)l.getNode2()) ;
        			
        			if(tmu==null && tmv ==null) {
        				continue ;
        			}
        			if(firstTime) {
		    			if(tmu != null && tmv==null) {
		    				if(!Mminus.containsKey(l))
		    					Mminus.put(l, new HashSet<Integer>()) ;
		    				Mminus.get(l).add(cascadeId) ;
		    				continue ;
		    			} else if(tmu != null && tmv!=null) {
		    				if(tmu < tmv) {
		    					if(!Mplus.containsKey(l))
		    						Mplus.put(l, new HashSet<Integer>()) ;
		    					Mplus.get(l).add(cascadeId) ;
		    					//System.out.println("add : "+l.getNode1().getID()+"->"+l.getNode2().getID() +" : "+cascadeId);
		    				}
		    			}
        			}
        			
        			if(!Mplus.get(l).contains(cascadeId))
        				continue ;
        			
        			
        			
        			// Set A
        			set(A,cascadeId,l,k*r*Math.exp(-r*(tmv-tmu))) ;
        			// Set B
        			set(B,cascadeId,l,k*Math.exp(-r*(tmv-tmu))+(1-k)) ;
        			// Set b
        			set(b,cascadeId,l, (k*Math.exp(-r*(tmv-tmu))) / (get(B,cascadeId,l) )) ;
        			
        			if(!sumAB.containsKey((User)l.getNode2()))
        				sumAB.put(((User)l.getNode2()), get(A,cascadeId,l)* (1/get(B,cascadeId,l))) ;
        			else {
        				double d = sumAB.get((User)l.getNode2()) ;
        				sumAB.put((User)l.getNode2(), d + (get(A,cascadeId,l)* (1/get(B,cascadeId,l)))) ;
        			}
        		}
        		//set a
        		for(Link l : allLinks) {
        			if(!sumAB.containsKey((User)l.getNode2()))
        				continue ;
        			else {
        				Double vA = get(A,cascadeId,l) ;
        				Double vB = get(B,cascadeId,l) ;
        				if(vA == null || vB == null)
        					continue ;
        				set(a,cascadeId,l,(vA/vB) / (sumAB.get((User)l.getNode2()))) ;
        			}
        			
        		}
        	}
        	
        	// Maj des poids.
        	for(Link l : allLinks) {
        			
        		double numr =0 ;
        		double demonr=0 ;
        		double sumr=0 ;
        		int sizeMp = Mplus.get(l) == null ? 0 : Mplus.get(l).size() ;
        		int sizeMm = Mminus.get(l) == null ? 0 : Mminus.get(l).size() ;
        		for(int cascadeId : Mplus.get(l)) {
        			double thisa = get(a, cascadeId, l) ;
        			double thisb = get(b, cascadeId, l) ;
        			Long tmu = cascades.get(cascadeId).get((User)l.getNode1()) ;
        			Long tmv = cascades.get(cascadeId).get((User)l.getNode2()) ;
        			
        			numr += thisa ;
        			demonr += thisa+((1-thisa)*thisb*(tmv-tmu)) ;
        			sumr += thisa+((1-thisa)*thisb) ;
        			
        		}
        		//System.out.println(numr+" "+demonr+" "+sumr);
        		if(!(sizeMp==0)) {
	        		//System.out.println(numr+","+demonr+","+sumr);
	        		HashMap<Integer,Double> w = new HashMap<Integer, Double>() ;
	        		w.put(0, (sumr/(sizeMp+sizeMm))) ; 
	        		w.put(1, numr/demonr) ;
	        		l.setWeights(w) ;
	        		System.out.println((User)l.getNode1()+"->"+(User)l.getNode2() + " - " + w);
        		} else if(firstTime) {
        			HashMap<Integer,Double> w = new HashMap<Integer, Double>() ;
        			w.put(0, 0.0) ; 
	        		w.put(1, 1.0) ;
	        		l.setWeights(w) ;
        		}
        	}
        	firstTime = false ;
        	System.out.println("Fin iteration "+iii);
        }
        
     // Save
        String filename=modelFile;
        System.out.println("sauvegarde...");
        try{
          PrintStream p = new PrintStream(filename) ;
          for(Link l : allLinks) {
            p.println(l.getNode1()+"\t"+l.getNode2()+"\t"+l.getWeights().get(0)+"\t"+l.getWeights().get(1)) ;
          }
        }
        catch(IOException e){
        	System.out.println("Probleme sauvegarde modele "+filename);
        	
        }
        
	
	}
	
	private static void set(HashMap<Integer,HashMap<Link,Double>> map,int c,Link l, double v) {
		if(!map.containsKey(c))
			map.put(c, new HashMap<Link,Double>() ) ;
		map.get(c).put(l,v) ;
	}
	
	private static double get(HashMap<Integer,HashMap<Link,Double>> map,int c,Link l) {
		if(!map.containsKey(c))
			return 0.0 ;
		HashMap<Link,Double> map2 = map.get(c) ;
		if(!map2.containsKey(l))
			return 0.0 ;
		return map2.get(l) ;
	}

	
	
	public void learn(HashMap<Integer,HashMap<User,Long>> cascades,int maxIter, double min_init_d, double max_init_d, double min_init_p, double max_init_p) {
		
		// Initialisation.
		HashSet<User> users=new HashSet<User>(User.users.values());
       
        HashSet<Link> allLinks=new HashSet<Link>();
        
        // Tous les liens obtiennent une valeur arbitraire tiree entre min_init et max_init
        double dif_p=max_init_p-min_init_p;
        double dif_d=max_init_d-min_init_d;
        for(User u:User.users.values()){
            HashMap<String,Link> succs=u.getSuccesseurs();
            for(Link l:succs.values()){
            	HashMap<Integer,Double> params = new HashMap<Integer, Double>();
                double v=(Math.random()*dif_p)+min_init_p;
                params.put(0, v) ;
                v=(Math.random()*dif_d)+min_init_d ;
                params.put(1, v) ;
                l.setWeights(params) ;
                allLinks.add(l) ;
            }
        }
        
        HashMap<Link,HashSet<Integer>> Mplus = new HashMap<Link,HashSet<Integer>>() ;
        HashMap<Link,HashSet<Integer>> Mminus = new HashMap<Link,HashSet<Integer>>() ;
        
        HashMap<Integer,HashMap<Link,Double>> A = new HashMap<Integer, HashMap<Link,Double>>() ;
        HashMap<Integer,HashMap<Link,Double>> B = new HashMap<Integer, HashMap<Link,Double>>() ;
        
        HashMap<Integer,HashMap<Link,Double>> a = new HashMap<Integer, HashMap<Link,Double>>() ;
        HashMap<Integer,HashMap<Link,Double>> b = new HashMap<Integer, HashMap<Link,Double>>() ;
        
        //HashMap<Integer,HashMap<Integer,Long>> cascades = getTimeSteps(db,usersCollection,1) ;
        
        
        
        for(Link l : allLinks) {
        	Mplus.put(l, new HashSet<Integer>()) ;
        	Mminus.put(l, new HashSet<Integer>()) ;
        }
        
        
        // Estimation iterative.
        boolean keepOnGoing = true ;
        boolean firstTime = true ;
        for(int iii = 0 ; iii<maxIter ; iii++) {
        	
        	for(int cascadeId : cascades.keySet()) {
        		HashMap<User,Double> sumAB = new HashMap<User, Double>() ; // Le denominateur du calcul de a.
        		for(Link l : allLinks) {
        			double k = l.getWeights().get(0) ;
        			double r = l.getWeights().get(1) ;
        			Long tmu = cascades.get(cascadeId).get((User)l.getNode1()) ;
        			Long tmv = cascades.get(cascadeId).get((User)l.getNode2()) ;
        			
        			if(tmu==null && tmv ==null) {
        				continue ;
        			}
        			if(firstTime) {
		    			if(tmu != null && tmv==null) {
		    				if(!Mminus.containsKey(l))
		    					Mminus.put(l, new HashSet<Integer>()) ;
		    				Mminus.get(l).add(cascadeId) ;
		    				continue ;
		    			} else if(tmu != null && tmv!=null) {
		    				if(tmu < tmv) {
		    					if(!Mplus.containsKey(l))
		    						Mplus.put(l, new HashSet<Integer>()) ;
		    					Mplus.get(l).add(cascadeId) ;
		    					//System.out.println("add : "+l.getNode1().getID()+"->"+l.getNode2().getID() +" : "+cascadeId);
		    				}
		    			}
        			}
        			
        			if(!Mplus.get(l).contains(cascadeId))
        				continue ;
        			
        			
        			
        			// Set A
        			set(A,cascadeId,l,k*r*Math.exp(-r*(tmv-tmu))) ;
        			// Set B
        			set(B,cascadeId,l,k*Math.exp(-r*(tmv-tmu))+(1-k)) ;
        			// Set b
        			set(b,cascadeId,l, (k*Math.exp(-r*(tmv-tmu))) / (get(B,cascadeId,l) )) ;
        			
        			if(!sumAB.containsKey((User)l.getNode2()))
        				sumAB.put(((User)l.getNode2()), get(A,cascadeId,l)* (1/get(B,cascadeId,l))) ;
        			else {
        				double d = sumAB.get((User)l.getNode2()) ;
        				sumAB.put((User)l.getNode2(), d + (get(A,cascadeId,l)* (1/get(B,cascadeId,l)))) ;
        			}
        		}
        		//set a
        		for(Link l : allLinks) {
        			if(!sumAB.containsKey((User)l.getNode2()))
        				continue ;
        			else {
        				Double vA = get(A,cascadeId,l) ;
        				Double vB = get(B,cascadeId,l) ;
        				if(vA == null || vB == null)
        					continue ;
        				set(a,cascadeId,l,(vA/vB) / (sumAB.get((User)l.getNode2()))) ;
        			}
        			
        		}
        		
        	}
        	
        	// Maj des poids.
        	for(Link l : allLinks) {
        			
        		double numr =0 ;
        		double demonr=0 ;
        		double sumr=0 ;
        		int sizeMp = Mplus.get(l) == null ? 0 : Mplus.get(l).size() ;
        		int sizeMm = Mminus.get(l) == null ? 0 : Mminus.get(l).size() ;
        		for(int cascadeId : Mplus.get(l)) {
        			double thisa = get(a, cascadeId, l) ;
        			double thisb = get(b, cascadeId, l) ;
        			Long tmu = cascades.get(cascadeId).get((User)l.getNode1()) ;
        			Long tmv = cascades.get(cascadeId).get((User)l.getNode2()) ;
        			
        			numr += thisa ;
        			demonr += thisa+((1-thisa)*thisb*(tmv-tmu)) ;
        			sumr += thisa+((1-thisa)*thisb) ;
        			
        		}
        		//System.out.println(numr+" "+demonr+" "+sumr);
        		if(!(sizeMp==0)) {
	        		//System.out.println(numr+","+demonr+","+sumr);
	        		HashMap<Integer,Double> w = new HashMap<Integer, Double>() ;
	        		w.put(0, (sumr/(sizeMp+sizeMm))) ; 
	        		w.put(1, numr/demonr) ;
	        		l.setWeights(w) ;
	        		System.out.println(l.getNode1()+"->"+l.getNode2() + " - " + w);
        		} else if(firstTime) {
        			HashMap<Integer,Double> w = new HashMap<Integer, Double>() ;
        			w.put(0, 0.0) ; 
	        		w.put(1, 1.0) ;
	        		l.setWeights(w) ;
        		}
        	}
        	firstTime = false ;
        	System.out.println();
        } 
        
        
	}
	
	
	
	/*public int inferSimulation(Structure struct){
		throw new RuntimeException("Not implemented");
	}*/
	
	

	@Override
	public int infer(Structure struct) {
		/*if(!loaded)
			load();
    	PropagationStruct pstruct = (PropagationStruct)struct ;
        HashMap<String,Long> contaminated=pstruct.getInitContaminated();
        
        HashMap<String,Long> infected = new HashMap<String,Long>() ;
        //HashSet<User> contagious = new HashSet<User>() ;
        //HashMap<Long,Integer> antiInfected = new HashMap<Long, Integer>() ;
        HashMap<Long,HashSet<User>> contagious = new HashMap<Long, HashSet<User>>();
        
        long timestamp = Long.MAX_VALUE ;
        
        for(String u:contaminated.keySet()){
        	User user=User.getUser(u);
        	//infected.put(u,contaminated.get(u));
        	if(timestamp>contaminated.get(u)) {
        		timestamp=contaminated.get(u) ;
        	}
        	addUser(contaminated.get(u),user,contagious) ;
        }
        
        while(!contagious.isEmpty()) {
        	if(!contagious.containsKey(timestamp)) {
        		timestamp++ ;
        		continue ;
        	}
        		dd 
        	HashSet<User> toDo = contagious.get(timestamp) ;
        	for(User user : toDo) {
        		//System.out.println("a");
        		//user = User.getUser(userId) ;
        		// Sauter le gars s'il a deja ete infecte, finalement
        		if(infected.containsKey(user))
        			continue ;
        		infected.put(user.getName(), timestamp) ;
        		HashMap<String,Link> succs=user.getSuccesseurs();
        		for(Link lsuc : succs.values()) {
        			if(infected.containsKey((User)lsuc.getNode2()))
        				continue ;
        			double proba = lsuc.getWeights().get(0) ;
        			double delta = lsuc.getWeights().get(1) ;
        			if(rand.nextDouble()<proba) {
        				addUser((timestamp+genDelay(delta)),(User)lsuc.getNode2(),contagious) ;
        				//System.out.println(lsuc.getNode2().getID()+" , "+(timestamp+genDelay(delta)));
        			}
        		}
        	}
        	contagious.remove(timestamp) ;
        	//System.out.println("size : "+contagious.size());
        	timestamp++ ;
        }
        
        pstruct.setContaminated(infected) ;*/
		return 0;
	}
	
	
	
	// Loi exp, on se place sur des nombres entiers.
	private long genDelay(double delta) {
		Double u = rand.nextDouble() ;
//		long x = 0 ;
//		while(delta*Math.exp(-delta*x)>u)
//			x++ ;
//		//System.out.print(u+" ");
//		return x ;
		long r = (long) (-Math.log(u)/delta) ;
		if (r==0)
				r=1 ;
		return r ;
	}
	
	private void addUser(long t, User u,HashMap<Long,HashSet<User>> m) {
		if(!m.containsKey(t))
			m.put(t, new HashSet<User>()) ;
		m.get(t).add(u) ;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		CTICmodel m = new CTICmodel("") ;
		System.out.println(m.genDelay(0.001));
		System.out.println(m.genDelay(0.001));
		System.out.println(m.genDelay(0.001));
		System.out.println(m.genDelay(0.001));
		System.out.println(m.genDelay(0.001));
		System.out.println("-------");
		
		User u1 = new User("Aristote") ;
		User u2 = new User("Barnabe") ;
		User u3 = new User("Ceme") ;
		User u4 = new User("Dante") ;
		User u5 = new User("Esteban") ;
		
		HashMap<Integer,Double> w = new HashMap<Integer, Double>();
		w.put(0,1.0) ;
		w.put(1,0.1) ;
		
		Link l12 = new Link(u1,u2,1) ;
		l12.setWeights(w) ;
		u1.addLink(l12) ;
		u2.addLink(l12) ;
		Link l14 = new Link(u1,u4,1) ;
		l14.setWeights(w) ;
		u1.addLink(l14) ;
		u4.addLink(l14) ;
		Link l15 = new Link(u1,u5,1) ;
		l15.setWeights(w) ;
		u1.addLink(l15) ;
		u5.addLink(l15) ;
		Link l23 = new Link(u2,u3,1) ;
		l23.setWeights(w) ;
		u2.addLink(l23) ;
		u3.addLink(l23) ;
		Link l43 = new Link(u4,u3,1) ;
		l43.setWeights(w) ;
		u4.addLink(l43) ;
		u3.addLink(l43) ;
		Link l45 = new Link(u4,u5,1) ;
		l45.setWeights(w) ;
		u4.addLink(l45) ;
		u5.addLink(l45) ;
		
		CascadesProducer cp = new CascadesProducer() ;
		HashMap<User,Long> hm = new HashMap<User, Long>();
		hm.put(u1, (long) 1) ;
		Cascade cas = cp.fromSimpleHashMap(hm, 0) ;
		PropagationStruct ps = new PropagationStruct(cas, 1, 2) ;
		
		m.infer(ps) ;
		//System.out.println(ps.getContaminated());
		
		System.out.println("---------------");
		
		HashMap<Integer,HashMap<User,Long>> train = new HashMap<Integer, HashMap<User,Long>>();	
		
		HashMap<User, Long> c = new HashMap<User, Long>() ;
		c.put(u1,(long)0) ;
		c.put(u2,(long)1000) ;
		c.put(u4,(long)1000) ;
		c.put(u3,(long)10000) ;
		train.put(0,c) ;
		
		c =new  HashMap<User, Long>() ;
		c.put(u1,(long)0) ;
		c.put(u2,(long)0) ;
		c.put(u3,(long)2000) ;
		train.put(1,c) ;
		
		c =new  HashMap<User, Long>() ;
		c.put(u1,(long)0) ;
		c.put(u4,(long)0) ;
		c.put(u5,(long)1000) ;
		train.put(2,c) ;
		
		c =new  HashMap<User, Long>() ;
		c.put(u5,(long)0) ;
		c.put(u4,(long)0) ;
		c.put(u3,(long)1000) ;
		train.put(3,c) ;
		
		c =new  HashMap<User, Long>() ;
		c.put(u2,(long)0) ;
		c.put(u4,(long)0) ;
		train.put(4,c) ;
		
		CTICmodel ic = new CTICmodel("") ;
		ic.learn(train, 100, 0.1, 0.1, 0.0001, 0.001) ;
		
		
		ic = new CTICmodel("modelCTIC") ;
		//ic.learn("us_elections5000", "cascades_2", "users_1", 12, 10, 0.1, 2, 0.1, 0.3) ;
		
		
		

	}

	@Override
	public int inferSimulation(Structure struct) {
		// TODO Auto-generated method stub
		return 0;
	}


}
