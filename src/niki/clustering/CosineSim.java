package niki.clustering;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.la4j.vector.sparse.SparseVector;

import niki.clustering.node.DataUser;
import niki.clustering.node.UserByCascadeText;
import niki.tool.Tools;
import core.Data;
import core.Text;
import similarities.*;

/**
 * Classe representant la similarite entre deux UserByCascadeText
 * @author dantidot
 *
 */
public class CosineSim extends StrSim {

	public static HashMap <String, Double> sim = new HashMap <String, Double> ();
	
	
	public CosineSim (DataUser data) {
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
		/* String hash;
		if (t1.getID() > t2.getID())
			hash = t1.getID()+""+t2.getID();
		else
			hash = t2.getID()+""+t1.getID();
		
		if (sim.containsKey(hash))
				return sim.get(hash);
		*/
		UserByCascadeText u1 = (UserByCascadeText) t1;
		UserByCascadeText u2 = (UserByCascadeText) t2;
		//HashMap <Long, Double> c1 = u1.getTimeStepNormalise();
		//HashMap <Long, Double> c2 = u2.getTimeStepNormalise();
		//double norm = (Tools.norm(c1)*Tools.norm(c2));
		//if (norm == 0)
		//	return 0;
		//return (Tools.dotProduct(c1, c2))/norm;
		
		SparseVector c1 = u1.getSparseTimeStepNormalise();
		SparseVector c2 = u2.getSparseTimeStepNormalise();
		// if (c1.innerProduct(c2) > 0)
			// System.out.println (c1.innerProduct(c2));
		double norm = c1.norm() * c2.norm();
		if (norm == 0)
			return 0;
		double inner = c1.innerProduct(c2);
		// sim.put(hash, inner/norm);
		return inner/norm;
		}
	
	
	
	
	
	

	@Override
	public String toString() {
		return "Cos similaritie";
	}

}
