package propagationModels;

import core.Structure;

public interface PropageModel extends PropagationModel {
	public int infer(Structure struct);
}
