package propagationModels;

import java.util.HashSet;

public interface ProbabilisticTransmissionModel {
	public Double getProba(String from,String to);
	public HashSet<String> getUsers();
	public String getModelFile();
}
