package simon.sourceDetect;

import java.util.HashMap;
import java.util.HashSet;

import core.Structure;

public class SourceDetectStructure implements Structure {
	
	
	private HashMap<String,Double> obsTimes ;
	private String actualSource ;
	private HashMap<String,Double> sourcesScores ;
	
	
	public SourceDetectStructure(HashMap<String, Double> obsTimes, String actualSource) {
		this.obsTimes = obsTimes;
		this.actualSource = actualSource;
	}
	
	public HashMap<String,Double> getObsTimes()  {
		return obsTimes ;
	}
	
	public String getActualSource() {
		return actualSource ;
	}
	
	public void setSourcesScores(HashMap<String,Double> s) {
		this.sourcesScores = s; 
	}
	
	public HashMap<String,Double> getSourcesScores() {
		return sourcesScores;
	}

}
