package strLinkUsers;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Link;
import core.Post;
import core.User;

import actionsBD.MongoDB;
import cascades.Cascade;
import cascades.CascadesProducer;


// implementation de NetInf.
public class NetInf extends StrLinkUsers {
	
	private int NB_EDGES ; // Nombre d'arcs a atteindre.
	private double ALPHA ; // Delta de la fonction d'infection.
	private int MODE ; // 0 : exp, 1 : Power
	private int TIMESTEP ;
	private double EPS; 
	private String usersCollection;
	private int  userLinkThreshold;
	
	
	public NetInf( String usersCollection, int userLinkThreshold, int n, double a, int m, int t, double e) {
		this.NB_EDGES=n ;
		this.ALPHA = a ;
		this.MODE=m ;
		this.TIMESTEP = t ;
		this.usersCollection=usersCollection;
		this.userLinkThreshold=userLinkThreshold;
		this.EPS=e;
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
	
	@Override
	public String linkUsers(String db, String col) {
		System.out.println("NetInf");
		PrintStream p;
		try {
			p = new PrintStream("templinks");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		}

		 // Pour chaque cascade, liste des users avec leur temps de contamination
		HashMap<Integer,HashMap<User,Long>> contamination=getTimeSteps(db,col,TIMESTEP);
		
		 HashSet<User> users=new HashSet<User>(User.users.values());
	     // Pour tous les utilisateurs on recupere leurs liens
	     for(User u:users){
	            u.loadLinksFrom(db, usersCollection, userLinkThreshold);
	            System.out.println("Liens user "+u.getName()+" charge");
	     }
	       
	     HashSet<Link> allLinks=new HashSet<Link>();
	     HashMap<User,HashSet<User>> G=new HashMap<User,HashSet<User>>(); 
	     
	     HashMap<Link,HashSet<Integer>> cascades=new HashMap<Link,HashSet<Integer>>();
		 HashMap<Integer, HashMap<User,User>> trees=new HashMap<Integer, HashMap<User,User>>();
	     
	     System.out.println("Calcul trees");
         // Calcul des trees
         for(User u:users){
        	 System.out.println("Trees selon user "+u.getName()); 
        	//String j=u.getName();
            HashMap<String,Link> preds=u.getPredecesseurs();
            HashMap<Integer,Double> max=new HashMap<Integer,Double>();
            HashMap<Integer,User> imax=new HashMap<Integer,User>();
            if (preds==null){continue;}
            for(Integer c:contamination.keySet()){
            	HashMap<User,Long> hc=contamination.get(c);
				Long tj=hc.get(u);
				if (tj==null){
					continue;
				}
				Double m=max.get(c);
            	for(Link l:preds.values()){
            		User i = (User)l.getNode1();
					Long ti=hc.get(i);
					if (ti==null){
						continue;
					}
					if (tj>ti){
					    double wc=getWc(ti,tj);
						if ((m==null) || (wc>m)){
							max.put(c, wc);
							imax.put(c,i);
						}
						HashSet<Integer> cu=cascades.get(l);
						if (cu==null){
							cu=new HashSet<Integer>();
							cascades.put(l, cu);
						}
						cu.add(c);
						
						l.setVal(0.0);
						allLinks.add(l);
					}
				}
            }
            for(Integer c:imax.keySet()){
            	HashMap<User,User> tc=trees.get(c);
            	if (tc==null){
            		tc=new HashMap<User,User>();
            		trees.put(c, tc);
            	}
            	tc.put(u, imax.get(c));
            }
         }
	     
         User.reinitAllLinks();
         
		int currentNbEdges = 0 ;
		HashMap<Link,HashSet<Integer>> spos=new HashMap<Link,HashSet<Integer>>();
		User lastAddedj=null;
		while(currentNbEdges < NB_EDGES) {
			System.out.println("currentNbEdges = "+currentNbEdges);
			// Parcourir toutes les aretes (paires de users possibles)
			Link maxl=null;//(allLinks.size()>0)?allLinks.get(0):null;
			double max = 0; //(maxl!=null)?maxl.getVal():0;
			
			// on a retire de Links ceux qui sont dans G
			for(Link link : allLinks) {
				User j = (User)link.getNode2();
				double likelihood = 0 ;
				if ((currentNbEdges>0) && (j!=lastAddedj)){
					likelihood=link.getVal(); 
					if (likelihood>=max){
						max=likelihood;
						maxl=link;
					}	
					continue;
				}
				User i = (User)link.getNode1(); 
				 
				
				HashSet<Integer> cpos=new HashSet<Integer>();
				spos.put(link,cpos);
				HashSet<Integer> cu=cascades.get(link);
				if (cu==null){
					continue;
				}
				//System.out.println(i+","+j);
				for(Integer c : cu) {
					HashMap<User,Long> hc=contamination.get(c);	
					Long tj=hc.get(j);
					if (tj==null){
						continue;
					}
					Long ti=hc.get(i);
					if (ti==null){
						continue;
					}
					if(tj>ti){
						double wc=getWc(ti,tj);
						double wcp=0;
						HashMap<User,User> tree=trees.get(c);
						if (tree!=null){
							User par=tree.get(j);
							if (par!=null){
								if ((G.containsKey(par)) && (G.get(par).contains(j))){
									//System.out.println(par+","+j+" in G");
									Long tpar=hc.get(par);
									if (tpar!=null){
										if (tpar<tj){
											wcp=getWc(tpar,tj);
										}
									}
								}
							}
						}
						if (wc>=wcp){
							likelihood+=wc-wcp;
							cpos.add(c);
						}
					}
				}
				/*if (currentNbEdges==0){
					link.setVal(likelihood);
					allLinks.addLink(link);
				}
				else{
					allLinks.changeValue(link, likelihood);
				}*/
				link.setVal(likelihood);
				if (likelihood>=max){
					max=likelihood;
					maxl=link;
				}	
			}
			if (maxl==null){
				break;
			}
			User i=(User)maxl.getNode1();
			User j=(User)maxl.getNode2();
			HashSet<User> hG=G.get(i);
			if (hG==null){
				hG=new HashSet<User>();
				G.put(i, hG);
			}
			hG.add(j);
			lastAddedj=j;
			allLinks.remove(maxl);
			//links.remove(maxl);
			cascades.remove(maxl);
			HashSet<Integer> m=spos.get(maxl);
			
			if (m!=null){
				for(Integer c:m){
					HashMap<User,User> tc=trees.get(c);
	            	if (tc==null){
	            		tc=new HashMap<User,User>();
	            		trees.put(c, tc);
	            	}
	            	tc.put(j, i);
				}
			}
			
			System.out.println("Edge : "+i+" -> "+j+" = "+max);
			p.println("Edge : "+i+" -> "+j+" = "+max);
			currentNbEdges++;
			
		}
		allLinks.clear();
		cascades.clear();
		contamination.clear();
		trees.clear();
		System.gc();
		// Insertion des links en base de donnees.
		String desc="Users from cascades "+col+" en utilisant NetInf considerant les liens de "+usersCollection+" avec un trheshold de liens = "+this.userLinkThreshold+". Params de Netinf : nb_edges="+this.NB_EDGES+", alpha="+this.ALPHA+", eps="+this.EPS+", mode="+MODE+" TIMESTEP="+this.TIMESTEP;
		String outCol=MongoDB.mongoDB.createCollection(db,"users",desc);
		System.out.println("Indexation users dans "+outCol);
		for(User i:G.keySet()){
			System.out.println("Creation liens pour "+i);
			//User ui=User.getUser(i);
			HashSet<User> hj=G.get(i);
			for(User j:hj){
				//User uj=User.getUser(j);
				Link l=new Link(i,j,1.0);
				i.addLink(l);
				j.addLink(l);
			}
		}
		for(User u:User.users.values()){
			System.out.println("Indexation user "+u.getName());
			u.indexInto(db, outCol);
		}
		DBCollection outcol=MongoDB.mongoDB.getCollectionFromDB(db,outCol);
		outcol.ensureIndex(new BasicDBObject("name", 1));
		return outCol;
	}
	

	private double getPc(double d){
		if(d<=0)
			return 0 ;
		if(MODE == 0)
			return (Math.exp(-d/ALPHA)) ;
		if(MODE == 1)
			return (1.0 / Math.pow(d,ALPHA)) ;
		return 0 ;
	}
	
	// Get the wc(i,j).
	private double getWc(double ti, double tj) {
		double d = tj - ti ;
		if(d<=0)
			return 0 ;
		double res=Math.log(getPc(d)) - Math.log(EPS);
		if (res<0){
			System.out.println("Wc negatif pour "+ti+","+tj+"="+res);
			res=0;
		}
		return (res) ;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		NetInf netinf = new NetInf("users_1",1,100000,1,1,1,0.0000001) ;
		netinf.linkUsers("us_elections5000", "cascades_1") ;
		
	}
	

}



