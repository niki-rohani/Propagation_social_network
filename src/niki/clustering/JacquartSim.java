package niki.clustering;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import niki.clustering.node.DataUser;
import niki.clustering.node.UserByCascadeText;
import core.Data;
import core.Text;
import similarities.*;

/**
 * Classe representant la similarite entre deux UserByCascadeText
 * @author dantidot
 *
 */
public class JacquartSim extends StrSim {

	
	public JacquartSim (DataUser data) {
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
		UserByCascadeText u1 = (UserByCascadeText) t1;
		UserByCascadeText u2 = (UserByCascadeText) t2;
		HashSet <Integer> inter = new HashSet <Integer> (u1.getCascade().keySet());
		HashSet <Integer> union = new HashSet <Integer> (u1.getCascade().keySet());
		inter.retainAll(u2.getCascade().keySet());
		union.addAll(u2.getCascade().keySet());
		if (union.size() == 0)
			return 0.;
		return (((double) inter.size() / (double) union.size()));
		
		
	}
	
	public static Set <Integer> getCommonCascade (Text t1, Text t2) {
		UserByCascadeText u1 = (UserByCascadeText) t1;
		UserByCascadeText u2 = (UserByCascadeText) t2;
		Set <Integer> u1u2 = new HashSet<Integer> (u1.getCascade().keySet());
		u1u2.retainAll(u2.getCascade().keySet());
		return u1u2;
	}
	
	

	@Override
	public String toString() {
		return "Jacquart similaritie";
	}

}
