package simon.sourceDetect;

import java.util.HashSet;
import java.util.Set;

import core.Structure;
import propagationModels.PropagationStruct;

public abstract class SourceDetector {
	
	protected HashSet<String> obsUsers ;
	protected HashSet<String> hidUsers ;
	
	public abstract void save() ;
	public abstract void load() ;
	public abstract void detect(Structure s) ;

	public abstract Set<String> getObsUsers () ;
	
}
