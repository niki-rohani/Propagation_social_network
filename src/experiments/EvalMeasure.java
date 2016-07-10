package experiments;

import java.util.HashSet;
import java.util.HashMap;
import java.io.Serializable;
public abstract class EvalMeasure implements Serializable {
	protected HashSet<Integer> allUsers=new HashSet<Integer>();
	
	public abstract String getName();
	public abstract Result eval(Hyp hyp);
}
