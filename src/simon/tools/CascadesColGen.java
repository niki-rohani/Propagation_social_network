package simon.tools;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import actionsBD.MongoDB;
import core.Link;
import core.Post;
import core.User;
import propagationModels.PropagationStruct;
import cascades.Cascade; 
import org.jblas.Eigen; 

// Classe pour réaliser certains traitement sur la base de donnée.
public class CascadesColGen {
	
	public void filter(String db, String cascades1, String cascades2, int minNbCascades, int minCascadesSize, boolean unlearnablePop2, int maxPop2) {
		
		HashMap<String,Integer> users = new HashMap<String,Integer>() ;
		HashSet<String> pop1 = new HashSet<String>() ;
		HashSet<String> pop2 = new HashSet<String>() ;
		HashSet<User> usersObj = new HashSet<User>() ;
		
		boolean tictoc =false;
		
		
		System.out.println("Chargement cascades...");
		// Chargement des cascades et des users, et diviser ces users en deux groupes.
		HashSet<Cascade> allcascades = Cascade.getCascadesFromDB(db, cascades1) ;
		if(cascades2!="") {
			allcascades.addAll(Cascade.getCascadesFromDB(db, cascades2)) ;
		}
		HashSet<PropagationStruct> allStructs = new HashSet<PropagationStruct>() ;
		for(Cascade c : allcascades) {
			for(Post p : c.getPosts()) {
				usersObj.add(p.getOwner()) ;
			}
			PropagationStruct ps=new PropagationStruct(c,1,1,-1);
			allStructs.add(ps) ;
			for(String u : ps.getInfectionTimes().keySet()) {
				if(users.containsKey(u)) {
					users.put(u,users.get(u)+1) ;
				} else {
					users.put(u,1) ;
				}
				if(users.get(u)==minNbCascades) {
					if(tictoc || pop2.size()>=maxPop2)
						pop1.add(u) ;
					else
						pop2.add(u) ;
					tictoc=!tictoc ;
				}
				
			}
		}
		System.out.println(" pop1 :   "+pop1.size());
		System.out.println(" pop2 :   "+pop2.size());
		System.out.println(" users:   "+usersObj.size());
		
		
		System.out.println(" allcasc :"+allcascades.size());
		System.out.println(" allstruc:"+allStructs.size());
		System.out.println("Filtrage des cascades sans utilisateurs....");
		// Filtrer les cascades qui ne contienne aucun users (ou un seul) des users filtres.
		allcascades.clear();
		System.out.println(" allstruc:"+allStructs.size());
		for(PropagationStruct ps : allStructs) {
			Set<String> inf = ps.getInfectionTimes().keySet() ;
			inf.retainAll(users.keySet()) ;
			if(inf.size()>minCascadesSize) {
				allcascades.add(ps.getCascade()) ;
			}
			
			
			
		}
		allStructs.clear(); 
		
		System.out.println(" allcasc :"+allcascades.size());
		System.out.println(" allstruc:"+allStructs.size());
		System.out.println("Filtrage des utilisateurs...");
		// Virer des cascades tout les gars qui ne sont pas dans allusers
		HashSet<Cascade> newallcascades = new HashSet<Cascade>() ;
		for(Cascade c: allcascades) {
			retainUsers(c,users.keySet());
			if(c.getPosts().size()>0)
				newallcascades.add(c) ;
		}
		allcascades = newallcascades ;
		
		// Diviser et filtrer les cascades.
		// Algo :
		//	Extraire pour chaque utilisateur de pop2 UNE cascades dans laquelle il y'a au moins un utilisateur de pop1. Ce sera l'ensemble "train B".
		//  Effectuer sur le reste des cascades :
		//		Si c contient uniquement pop1, ajouter c à train A.
		//		Si c contient uniquement pop2, ajouter c à test.
		//		Sinon, tirer au sort et virer les users en trop.
		//
		// XP : Apprendre un modèle sur train A + train B (en meme temps ou l'un après l'autre). Tester sur Test.
		
		// Version "unlearnablePop2"
		// Algo
		//	Pour chaque cascade, compter utilisateurs de pop2 :
		//		0 : ajouter a trainA
		//		1 : ajouter à trainB
		//		2 : ajouter à test.
		
		HashSet<Cascade> trainA = new HashSet<Cascade>() ;
		HashSet<Cascade> trainB = new HashSet<Cascade>() ;
		HashSet<Cascade> test = new HashSet<Cascade>() ;
		Random rng = new Random() ;
		HashSet<String> toAddpop2 = new HashSet<String>(pop2) ;
		
		System.out.println(" allcasc :"+allcascades.size());
		System.out.println(" allstruc:"+allStructs.size());
		System.out.println("Répartition des cascades....");
		
		
		for(Cascade c: allcascades) {
			PropagationStruct ps=new PropagationStruct(c,1,1,-1);
			allStructs.add(ps) ;
			Set<String> userinthiscascade = new HashSet<String>(ps.getInfectionTimes().keySet()) ;
		
			if(!unlearnablePop2) {
				// Test pour ajout à trainB
				userinthiscascade.retainAll(toAddpop2) ;
				if(userinthiscascade.size()>0) {
					trainB.add(c) ;
					toAddpop2.removeAll(userinthiscascade) ;
					continue ;
				}
					
				// Ajout à train A et test
				userinthiscascade = new HashSet<String>(ps.getInfectionTimes().keySet()) ;
				int containsPop1 ;
				int containsPop2 ;
				containsPop1 = intersectionSize(userinthiscascade, pop1) ;
				containsPop2 = intersectionSize(userinthiscascade, pop2) ;
				//System.out.println(containsPop1+" : "+containsPop2+" : "+userinthiscascade.size());
				if(containsPop1>1 && containsPop2==0) {
					trainA.add(c) ;
				} else if(containsPop1==0 && containsPop2>1) {
					test.add(c) ;
				} else if(containsPop1>1 || containsPop2>1) {
					if(rng.nextBoolean() || containsPop2<2) {
						retainUsers(c, pop1);
						trainA.add(c) ;
					} else {
						retainUsers(c,pop2) ;
						test.add(c) ;
					}
				}
			} else {
				int inter1 = intersectionSize(userinthiscascade, pop1) ;
				int inter2 = intersectionSize(userinthiscascade, pop2) ;
				if(inter2==0) {
					trainA.add(c) ;
				} else if(inter2==1 && intersectionSize(toAddpop2, userinthiscascade)>0) {
					trainB.add(c) ;
					toAddpop2.removeAll(userinthiscascade) ;
				} else {
					retainUsers(c,pop2) ;
					test.add(c) ;
				}
			}
			
		}
		
		System.out.println("Indexation...");
		System.out.println("Sizes : ");
		System.out.println(" trainA : "+trainA.size());
		System.out.println(" trainB : "+trainB.size());
		System.out.println(" test :   "+test.size());
		System.out.println(" pop1 :   "+pop1.size());
		System.out.println(" pop2 :   "+pop2.size());
		System.out.println(" popAll : "+users.size());
		System.out.println(" userObj :"+usersObj.size());
		System.out.println(intersectionSize(pop1, pop2));
		System.out.println(" allcasc :"+allcascades.size());
		System.out.println(" allstruc:"+allStructs.size());
		MongoDB mongo = new MongoDB() ;
		mongo.createCollection(db, cascades1+"_filtered_trainAll_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2, "String", "Cascades filtré pour l'XP sur la capactié de généralisation.") ;
		mongo.createCollection(db, cascades1+"_filtered_trainA_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2, "String", "Cascades filtré pour l'XP sur la capactié de généralisation.") ;
		for(Cascade c : trainA) {
			c.indexInto(db, cascades1+"_filtered_trainA_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2);
			c.indexInto(db, cascades1+"_filtered_trainAll_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2);
		}
		mongo.createCollection(db, cascades1+"_filtered_trainB_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2, "String", "Cascades filtré pour l'XP sur la capactié de généralisation.") ;
		for(Cascade c : trainB) {
			c.indexInto(db, cascades1+"_filtered_trainB_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2);
			c.indexInto(db, cascades1+"_filtered_trainAll_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2);
		}
		mongo.createCollection(db, cascades1+"_filtered_test_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2, "String", "Cascades filtré pour l'XP sur la capactié de généralisation.") ;
		for(Cascade c : test) {
			c.indexInto(db, cascades1+"_filtered_test_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2);
		}
		mongo.createCollection(db, cascades1+"_filtered_pop1_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2,"String", "") ;
		mongo.createCollection(db, cascades1+"_filtered_pop2_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2,"String", "") ;
		mongo.createCollection(db, cascades1+"_filtered_popAll_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2,"String", "") ;
		linkAll(trainA) ;
		linkAll(trainB) ;
		for(User u : usersObj) {
			String name = u.getName() ;
			if(pop1.contains(name)) {
				u.indexInto(db, cascades1+"_filtered_pop1_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2);
				u.indexInto(db, cascades1+"_filtered_popAll_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2);
			}
			if(pop2.contains(name)) {
				u.indexInto(db, cascades1+"_filtered_pop2_"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2);
				u.indexInto(db, cascades1+"_filtered_popAll"+minNbCascades+"_"+minCascadesSize+"_"+unlearnablePop2);
			}
			
		}
		
	}
	
	
	
	
	public void decouper(String db, String cascades, int nbpart, boolean onlyOnePart) {
		
		HashSet<Cascade> allcascades = Cascade.getCascadesFromDB(db, cascades) ;
		MongoDB mongo = new MongoDB() ;
		if(!onlyOnePart) {
			for(int i = 1 ; i<=nbpart ; i++) {
				mongo.dropCollection(db, cascades+"_limited_"+i+"_"+nbpart);
				mongo.createCollection(db,cascades+"_limited_"+i+"_"+nbpart, "String", "ensemble limité pour tester l'impact de la taille") ;
			}
		} else {
			mongo.dropCollection(db, cascades+"_limited_"+1+"_"+nbpart);
			mongo.createCollection(db,cascades+"_limited_"+1+"_"+nbpart, "String", "ensemble limité pour tester l'impact de la taille") ;
		}
		int ipart = 1 ;
		for(Cascade c : allcascades) {
			
			if(!onlyOnePart) {
				for(int i=nbpart ; i>=ipart ; i--) {
					c.indexInto(db, cascades+"_limited_"+i+"_"+nbpart);
				}
			}else if(ipart==1) {
				c.indexInto(db, cascades+"_limited_"+1+"_"+nbpart);
			}
			
			ipart++ ;
			if(ipart == nbpart+1)
				ipart=1 ;
		}
		
	}
	
	public void uniciter(String db, String cascades1, String cascades2) {
		HashSet<Cascade> allcascades = Cascade.getCascadesFromDB(db, cascades1) ;
		if(cascades2!="") {
			allcascades.addAll(Cascade.getCascadesFromDB(db, cascades2)) ;
		}
		
		HashSet<Cascade> train = new HashSet<Cascade>() ;	
		HashSet<PropagationStruct> test = new HashSet<PropagationStruct>() ;	
		
		HashSet<String> pop = new HashSet<String>() ;
		HashSet<PropagationStruct> pss = new HashSet<PropagationStruct>() ;
		for(Cascade c : allcascades) {
			PropagationStruct ps = new PropagationStruct(c, 1, 1.0) ;
			pop.addAll(ps.getInfectionTimes().keySet()) ;
			pss.add(ps) ;
		}
		
		HashSet<String> toAdd = new HashSet<String>(pop) ;
		HashSet<String> added = new HashSet<String>() ;
		
		int maxDoublon = 0 ;
		do {
			test.clear();
			for(PropagationStruct ps : pss) {
				Set<String> userinthiscascade = new HashSet<String>(ps.getInfectionTimes().keySet()) ;
				if(intersectionSize(added, userinthiscascade)<=maxDoublon && intersectionSize(toAdd, userinthiscascade)>0) {
					train.add(ps.getCascade()) ;
					toAdd.removeAll(userinthiscascade) ;
					added.addAll(userinthiscascade) ;
				} else {
					test.add(ps);
				}
			}
			pss.clear();
			pss.addAll(test) ;
			
			maxDoublon++ ;
			System.out.println("maxDoublon="+maxDoublon+", toAdd="+toAdd.size());
			
		}while(toAdd.size()>100) ;
		
		MongoDB mongo = new MongoDB() ;
		mongo.createCollection(db, cascades1+"_unique_train","String","") ;
		mongo.createCollection(db, cascades1+"_unique_test","String","") ;
		for(Cascade c : train) {
			c.indexInto(db, cascades1+"_unique_train");
		}
		for(PropagationStruct ps : test) {
			ps.getCascade().indexInto(db, cascades1+"_unique_test");
		}

	}
	
	public void retainUsers(Cascade c, Set<String> users) {
		HashSet<Post> posts = c.getPosts() ;
		HashSet<Post> newposts = new HashSet<Post>() ;
		for(Post p : posts) {
			if(users.contains(p.getOwner().getName())) {
				newposts.add(p) ;
			}
		}
		c.setPosts(newposts);
	}
	
	public void removeUsers(Cascade c, Set<String> users) {
		HashSet<Post> posts = c.getPosts() ;
		HashSet<Post> newposts = new HashSet<Post>() ;
		for(Post p : posts) {
			if(!users.contains(p.getOwner().getName())) {
				newposts.add(p) ;
			}
		}
		c.setPosts(newposts);
	}
	
	public int intersectionSize(Set<String> a, Set<String> b) {
		int x = 0;
		for(String s : a) {
			if(b.contains(s))
				x++ ;
		}
		return x ;
	}
	
	public static void linkAll(Set<Cascade> cascades) {
		for(Cascade c: cascades) {
			for(Post p1 : c.getPosts()) {
				for(Post p2 : c.getPosts()) {
					User u1 = p1.getOwner() ;
					User u2 = p2.getOwner() ;
					if (u1.getName()==u2.getName())
						continue ;
					Link l = new Link(u1, u2, 1) ;
					u1.addLink(l, true);
				}
			}
		}
	}
	
	public static void crosser(String db, String cascades, int n) {
		HashSet<Cascade> allcascades = Cascade.getCascadesFromDB(db, cascades) ;
		MongoDB mongo = new MongoDB() ;
		for(int i=1 ; i<=n ; i++) {
			mongo.createCollection(db, cascades+"_train"+i, "String", "cross valid "+i+"/"+n) ;
			mongo.createCollection(db, cascades+"_test"+i, "String", "cross valid "+i+"/"+n) ;
		}
		int i = 1 ;
		for(Cascade c : allcascades) {
			c.indexInto(db, cascades+"_test"+i) ;
			for(int j=1 ; j<=n ; j++) {
				if(j!=i)
					c.indexInto(db, cascades+"_train"+j);
			}
			i++ ;
			if(i>n)
				i=1;
		}
		
	}
	
	public static void main(String args[]) {
		
		CascadesColGen ccg = new CascadesColGen() ;
		//ccg.filter("digg", "cascades_1", "cascades_2", 4, 3,true,100);
		//ccg.decouper("lastfm_songs", "cascades_1", 50,true);
		ccg.crosser("irvine", "cascades_all", 10);
		//ccg.uniciter("digg", "cascades_1", "cascades_2");
		
	}
	
	
	
	
}
