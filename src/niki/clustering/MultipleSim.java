package niki.clustering;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import niki.clustering.node.DataUser;
import niki.clustering.node.UserByCascadeText;
import niki.experiment.Experiment;
import core.Data;
import core.Text;
import similarities.*;

/**
 * Classe representant la similarite entre deux UserByCascadeText
 * @author dantidot
 *
 */
public class MultipleSim extends StrSim {

	HashMap <Integer, Double> similaritie;
	HashMap <Integer, StrSim> sim;
	public MultipleSim (DataUser data,HashMap <Integer, Double> similaritie) throws Exception {
		super (data);
		this.similaritie = similaritie;
		sim = new HashMap <Integer, StrSim> ();
		for (int sims: similaritie.keySet())
			if (sims == Experiment.coCascade)
				sim.put(sims, new JacquartSim(data));
			else if (sims == Experiment.timeStep)
				sim.put(sims, new CosineSim(data));
			else if (sims == Experiment.coSuccessor)
				sim.put(sims, new JacquartSimBySuccessor(data));
			else if (sims == Experiment.coPredecessor)
				sim.put(sims, new CopyOfJacquartSimByPredecessor(data));
			else
				throw new Exception ("Erreur Sim " + sims + " Inconnue");
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
		double r = 0;
		for (int sims: similaritie.keySet()) {
			r += sim.get(sims).computeSimilarity(t1, t2) * similaritie.get(sims);
		}
		
		
		
		return r;
		
	}
	

	

	@Override
	public String toString() {
		return "Multiple similaritie " + similaritie ;
	}

}
