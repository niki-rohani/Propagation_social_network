package nasim;

import propagationModels.MLPproj;
import propagationModels.PropagationStructLoader;
import mlp.DescentDirection;
import mlp.EmbeddingsModel;
import mlp.Env;
import mlp.LineSearch;
import mlp.Optimizer;
public class Test {

	
	public static void main(String[] args){
		
		// Exemple d'initialisation d'un embedding model 
		// (utilise toujours ca pour le moment, on pourra discuter des differents parametres par la suite)
		int nbDims=10;
		PropagationStructLoader ploader=new PropagationStructLoader("digg", "cascades_1",(long)1,1.0,-1,1,100);
		Env.setVerbose(0);                          
		
		MLPproj mlp=new MLPproj(nbDims,2000,false,false,false,false,false,false,false,false,false,false,5,false,0.0);
		
		mlp.prepareLearning(ploader);
		EmbeddingsModel mod=mlp;
		
		// Initialisation de l'optimizer
		LineSearch line= LineSearch.getFactorLine(1, 0.9999);
		Optimizer optimizer=Optimizer.getDescent(DescentDirection.getGradientDirection(),line);
		
		// Pour modifier les parametres line et decFactor (permettent de gerer le pas d'apprentissage), utilise setLine et setDecFactor definis pour l'objet FactorLine
		// Exemples :
		line.setLine(0.1);
		line.setDecFactor(0.9999);
		
		// Exemple d'optimisation du modele pendant 100 x 100 iterations avec affichage du loss a chaque tour de boucle
		for(int i=0;i<100;i++){
			mod.optimizeNext(optimizer, 100);
			System.out.println(mod.getGlobalLoss()+" "+mod.getLoss());
			System.out.println("line = "+mod.getLastLine());
			mod.reinitLoss();
		}
		
		
		
		// Recuperation des embeddings (utile pour afficher les points) :
		mod.getEmbeddings();
		
		// Pour le reste regarde ce qui est defini dans EmbeddingsModel
		
		
	}
}
