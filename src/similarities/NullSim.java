package similarities;

import java.util.HashMap;

import core.Data;
import core.Text;

// Classe de sim retournant 0 pour toutes  les  comparaisons d'elements
public class NullSim extends StrSim {

	public NullSim(){
		this((Data)null);
	}
	public NullSim(Data data){
		super(data);
	}
	
	@Override	
	public String toString(){
		return("NullSim");
	}
	
	@Override
	public StrSim getInstance(Data data) {
		return(new NullSim(data));
	}

	@Override
	public double computeSimilarity(Text t1,Text t2){
		return 0.0;
	}

}
