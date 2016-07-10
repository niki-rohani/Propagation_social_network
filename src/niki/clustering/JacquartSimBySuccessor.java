package niki.clustering;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import niki.clustering.node.DataUser;
import niki.clustering.node.UserByCascadeText;
import core.Data;
import core.Link;
import core.Text;
import core.User;
import elie.dataGenerator.Representations.user;
import similarities.*;

/**
 * Classe representant la similarite entre deux UserByCascadeText
 * @author dantidot
 *
 */
public class JacquartSimBySuccessor extends StrSim {

	// private static HashMap <String, Double> sim = new HashMap <String, Double> ();
	public JacquartSimBySuccessor (DataUser data) {
		super (data);
	}
	
	public DataUser getDataUser() {
		return (DataUser)this.getData();
	}
	@Override
	public StrSim getInstance(Data data) {
		return this;
	}

	@Override
	public double computeSimilarity(Text t1, Text t2) {
		String name;
	//	name = t1.getName()+","+t2.getName();

	//	if (sim.containsKey(name)) 
	//		return sim.get(name);
		UserByCascadeText u1 = (UserByCascadeText) t1;
		UserByCascadeText u2 = (UserByCascadeText) t2;
		Collection inter = u1.getSuccList();
		Collection <Integer> union = u1.getSuccList(); 
		inter.retainAll(u2.getSuccList());
		union.addAll(u2.getSuccList());
		if (union.size() == 0) {
	//		sim.put(name, 0.0);
			return 0.;
			
		}
		double computeSim = (((double) inter.size() / (double) union.size()));
	//	sim.put(name, computeSim);
		return computeSim;
		
		
	}
	
	
	

	@Override
	public String toString() {
		return "Jacquart similaritie";
	}

}
