package simon.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import core.Link;
import core.User;


/*
 * Classe pour calculer le RAND/jacquard ? indice moyen des voisin sortant entre tout les mecs.
 */
public class ourCoefficient {
	
	
	public static double twoModeClusteringCoef(String db, String usercollection) throws IOException {
		
		User.loadUsersFrom(db, usercollection) ;
		User.loadAllLinksFrom(db, usercollection) ;
				
		PrintStream p = new PrintStream("coef") ;
		
		Date d = new Date() ;
		
		
		double nb3path=0.0 ;
		double nb4cycl=0.0;
		int i = 0 ;
		
		for(String name1 : User.users.keySet()) {
			User user1 = User.users.get(name1) ;
			for(String name2 : user1.getSuccesseurs().keySet()) {
				User user2 = User.users.get(name2) ;
				for(String name3 : user2.getPredecesseurs().keySet()) {
					if(name3.equals(name1))
						continue ;
					User user3 = User.users.get(name3) ;
					for(String name4 : user3.getSuccesseurs().keySet()) {
						if(name4.equals(name2))
							continue ;
						User user4 = User.users.get(name4) ;
						nb3path+=1.0;
						if(user4.getPredecesseurs().keySet().contains(name1)) // GETSUCCESSEUR OU GETPREDECEsseur ?
							nb4cycl+=1.0 ;
						
						if(nb3path%10000000 ==0) {
							Date d2 = new Date() ;
							long t = (d2.getTime()-d.getTime())/1000 ;
							double h=((double)t)/3600.0 ;
							if(h>3.0) {
								return  1.0+(nb4cycl/nb3path) ;
							}
						}
						
						
					}
				}
			}
		}
		
		return  nb4cycl/nb3path ;
		
	}
	
	public static double compute(String db, String usercollection) throws IOException {
		
		User.loadUsersFrom(db, usercollection) ;
		User.loadAllLinksFrom(db, usercollection) ;
		
		PrintStream p = new PrintStream("coef") ;
		
		
		for(String us : User.users.keySet()) {
			User u1 = User.users.get(us) ;
			HashSet<User> set1 = new HashSet<User>() ;
			for(String ut : u1.getSuccesseurs().keySet()) {
				set1.add(User.users.get(u1.getSuccesseurs().get(ut).getNode2().getName())) ;
			}
			for(String ut : User.users.keySet()) {
				if(ut.equals(us))
					continue ;
				User u2 = User.users.get(ut) ;
				HashSet<User> set2 = new HashSet<User>() ;
				for(String ut2 : u2.getSuccesseurs().keySet()) {
					set2.add(User.users.get(u2.getSuccesseurs().get(ut2).getNode2().getName())) ;
				}
				double d = jacquard(set1, set2) ;
				p.println(Double.isNaN(d) ? 0.0 : d ) ;
			}
		}
		
		return 0;
		
	}

	
	
	
	public double rand(Set<User> a, Set<User> b) {
		HashSet<User> c=new HashSet<User>(a) ;c.retainAll(b) ; // inter
		HashSet<User> d=new HashSet<User>(b) ;d.addAll(a) ; // union
		int n = User.users.size() ;
		return ((double)c.size() + (n-d.size())) / ((double)d.size()+(n-c.size()))  ;
		
		
	}
	
	public static double jacquard(Set<User> a, Set<User> b) {
		HashSet<User> c=new HashSet<User>(a) ;c.retainAll(b) ; // inter
		HashSet<User> d=new HashSet<User>(b) ;d.addAll(a) ; // union
		int n = User.users.size() ;
		return ((double)c.size()) / ((double)d.size())  ;
		
		
	}
	
	public static void main(String args[]) {
		
		
		
		try {
			//compute("enronAll","users_1") ;
			double l = twoModeClusteringCoef(args[0], "users_1");
			//double l = twoModeClusteringCoef("weibo", "users_2");		
			System.out.println(l);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
