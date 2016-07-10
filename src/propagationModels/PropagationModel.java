package propagationModels;
import core.*;
import java.util.HashSet;
public interface PropagationModel extends Model {
	public int infer(Structure struct);
	
	// result : binary information about infection
	public int inferSimulation(Structure struct);
	public HashSet<String> getUsers();
	//public HashSet<String> getContentFeatures();
	public int getContentNbDims();
	//public String getName();
}
