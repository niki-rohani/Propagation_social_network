package mlp;

import java.io.IOException;
import java.util.HashMap;

/**
 * An abstract mlp model defining embeddings of points in a given space.
 * 
 * @author sylvain lamprier
 *
 */
public interface EmbeddingsModel {

	
	//protected HashMap<String,CPUParams> embeddings;
	
	/**
	 * Launch the optimization of the model for nb steps.
	 * @param nb number of optimization steps to perform.
	 */
	public void optimizeNext(Optimizer optimizer, int nb);
	
	
	/**
	 * Returns the loss estimated by averaging all losses computed from the begining of the optimization.
	 * 
	 */
	public double getGlobalLoss();
	
	/**
	 * Returns the loss estimated by averaging all losses computed from the last call to the reinitLoss function (or the begining of the optimization if no reinit).
	 * 
	 */
	public double getLoss();
	
	
	/**
	 * Allows to reinit the estimated loss. 
	 */
	public void reinitLoss();
	
	/**
	 * Returns a map containing the embeddings of the model (indexed by the name of the point).
	 *
	 */
	public HashMap<String,double[]> getEmbeddings(); 
	
	
	/**
	 * Returns the similarities of points with the referer point whose name in given as parameter.
	 * @param referer point
	 * @return sims
	 */
	public HashMap<String,Double> getSims(String referer);
	
	/**
	 * Returns the tendency of attractivity / repulsion effect points have on the referer point whose name in given as parameter.
	 * @param referer point
	 * @return map containing for each point its attractivity level on the referer : values between -1 (high level of repulsion) and 1 (high level of attractivity).
	 */
	public HashMap<String,Double> getAttractivities(String referer);
	
	
	/**
	 * Allows to set a line parameter used by the optimizing process. 
	 * @param line
	 */
	//public abstract void setLine(double line);
	// Finalement pour ca => modifier directement dans l'optimizer

	
	/**
	 * 
	 */
	public double getLastLine();
	
	
	/**
	 * Allows to set a line decreasing factor (at each learning step, the line parameter is multiplied by this factor).
	 * @param dec a decreasing factor in ]0;1]
	 */
	//public abstract void setLineDecreasingFactor(double dec);
	// Finalement pour ca => modifier directement dans l'optimizer

	
	/**
	 * Allows to ask to the optimizer to ignore the concerning point during a given number of learning steps.
	 * @param name
	 * @param nb
	 */
	public void setDiscard(String name, int nb);
	
	
	/**
	 * Returns the table of discards points
	 */
	public HashMap<String,Integer> getDiscards();
}
