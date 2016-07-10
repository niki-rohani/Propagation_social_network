package thibault.SNCollect;

import java.util.Collection;
import java.util.HashSet;

import jgibblda.Inferencer;
import jgibblda.Model;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import core.Post;
import core.User;


public class ArmContextFull extends ArmContext{

	private static final long serialVersionUID = 1L;
	
	//features communs
	public RealVector sumFeaturesCom;
	public RealMatrix sumProdFeaturesCom;
	public RealVector sumFeaturesOnObsCom;

	
	//feature individuel
	public RealVector sumFeaturesInd;
	public RealMatrix sumProdFeaturesInd;
	public RealVector sumFeaturesOnObsInd;
	
	//Attributs qui bougent a chaque pas de temps et diférent pour chaque simulation
	public int numberPlayedOnObs=0;
	public int numberObserved=0;
	public double sumRewardsOnObs=0.0;

	public ArmContextFull(String name, int sizeFeaturesCom, int sizeFeaturesInd,int caseContext){
		super(name,sizeFeaturesCom,sizeFeaturesInd, caseContext);
		if(sizeFeaturesCom>0){
			sumFeaturesCom = new ArrayRealVector(new double[sizeFeaturesCom]);
			sumProdFeaturesCom = new Array2DRowRealMatrix(new double[sizeFeaturesCom][sizeFeaturesCom]);
			sumFeaturesOnObsCom = new ArrayRealVector(new double[sizeFeaturesCom]);

		}
		
		if(sizeFeaturesInd>0){
			sumFeaturesInd = new ArrayRealVector(new double[sizeFeaturesInd]);
			sumProdFeaturesInd = new Array2DRowRealMatrix(new double[sizeFeaturesInd][sizeFeaturesInd]);
			sumFeaturesOnObsInd = new ArrayRealVector(new double[sizeFeaturesInd]);
		}

		
		this.initArmEstimateDistrib();
	}
	
	public void initArmEstimateDistrib(){
		for (int i = 0; i<sizeFeaturesCom;i++){
			sumFeaturesCom.setEntry(i, 0);
			sumFeaturesOnObsCom.setEntry(i, 0);
			for (int j = 0; j<sizeFeaturesCom;j++){
				if(i==j){
					sumProdFeaturesCom.setEntry(i, j, 0.1);
				}
				else{
					sumProdFeaturesCom.setEntry(i, j, 0);
				}
			}
		}
		for (int i = 0; i<sizeFeaturesInd;i++){
			sumFeaturesInd.setEntry(i, 0);
			sumFeaturesOnObsInd.setEntry(i, 0);
			for (int j = 0; j<sizeFeaturesInd;j++){
				if(i==j){
					sumProdFeaturesInd.setEntry(i, j, 0.1);
				}
				else{
					sumProdFeaturesInd.setEntry(i, j, 0);
				}
				
			}
		}
	}
	
	@Override
	public void observeContext(HashSet<Post> posts, Inferencer inferencer) {
		
		User me=User.getUser(name);
		Collection<Post> mines=me.getPosts().values();
		int sizeOthers = 4;
		int sizeText = 30;
		String concatPost[]=new String[1];
		concatPost[0]="";
		double[] tOthers = new double[sizeOthers];
		double[] tText = new double[sizeText];
		if(!mines.isEmpty()){
			for (Post p:mines){	
				for (int w:p.getWeights().keySet()){
					concatPost[0]=w+" "+concatPost[0];
					tOthers[0]=Integer.parseInt(p.getOther().get("followers_count").toString());
					tOthers[1]=Integer.parseInt(p.getOther().get("statuses_count").toString());
					tOthers[2]=Integer.parseInt(p.getOther().get("friends_count").toString());
					tOthers[3]=Integer.parseInt(p.getOther().get("favourites_count").toString());
				}
			}
			Model newModel = inferencer.inference(concatPost);
			tText = newModel.theta[0];
		}
		else{
			for(int i=0;i<sizeText;i++){
				tText[i]=0;
			}
			for(int i=0;i<sizeOthers;i++){
				tOthers[i]=0;
			}
		}


		switch (caseContext) {
		case 1://cas 1: tout indiv pas de commun
			for(int i=0;i<sizeText;i++){
				featuresInd.setEntry(i, tText[i]);
			}
			for(int i=sizeText;i<sizeOthers;i++){
				featuresInd.setEntry(i, Math.max(tOthers[i],featuresInd.getEntry(i)));
			}
			break;
		case 2:  //cas 2: tout commun pas d'indiv
			for(int i=0;i<sizeText;i++){
				featuresCom.setEntry(i, tText[i]);
			}
			for(int i=sizeText;i<sizeOthers;i++){
				featuresCom.setEntry(i, Math.max(tOthers[i],featuresCom.getEntry(i)));
			}
			break;
		case 3:  //cas 3: text indiv et other commun
			for(int i=0;i<sizeText;i++){
				featuresInd.setEntry(i, tText[i]);
			}
			for(int i=sizeText;i<sizeOthers;i++){
				featuresCom.setEntry(i, Math.max(tOthers[i],featuresCom.getEntry(i)));
			}
			break;
		case 4: //cas 4: text commun et other indiv
			for(int i=0;i<sizeText;i++){
				featuresCom.setEntry(i, tText[i]);
			}
			for(int i=sizeText;i<sizeOthers;i++){
				featuresInd.setEntry(i, Math.max(tOthers[i],featuresCom.getEntry(i)));
			}
			break;
		case 5: 
			//cas 5: tout commun et indiv toujours a 1 (pour notre model hybrid quand on n'a pas tout le monde)
			for(int i=0;i<sizeText;i++){
				featuresCom.setEntry(i, tText[i]);
			}
			featuresInd.setEntry(0, 1);
			break;
		default:
		}
		
		//Mise a jour parametre des bras
		numberObserved++;
		sumFeaturesCom=sumFeaturesCom.add(featuresCom);
		sumProdFeaturesCom=sumProdFeaturesCom.add(featuresCom.outerProduct(featuresCom));
		sumFeaturesInd=sumFeaturesInd.add(featuresInd);
		sumProdFeaturesInd=sumProdFeaturesInd.add(featuresInd.outerProduct(featuresInd));
		
	}
	
	
}
