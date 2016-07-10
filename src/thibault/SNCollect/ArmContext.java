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

public class ArmContext extends Arm{

	private static final long serialVersionUID = 1L;

	public int caseContext; //pour savoir ce que l on choisit comme ctxt commun et indiv
	
	//features communs
	public int sizeFeaturesCom;
	public RealVector featuresCom;

	//feature individuel
	public int sizeFeaturesInd;
	public RealVector featuresInd;
	public RealVector thetaArm;
	public RealVector thetaArmStar;
	public RealVector bArm;
	public RealMatrix AArm;
	public RealMatrix AArmInverse;
	public RealMatrix BArm;

	public ArmContext(String name, int sizeFeaturesCom, int sizeFeaturesInd, int caseContext) {
		super(name);
		this.sizeFeaturesCom=sizeFeaturesCom;
		this.sizeFeaturesInd=sizeFeaturesInd;
		this.caseContext=caseContext;
		if(sizeFeaturesCom>0){
			featuresCom = new ArrayRealVector(new double[sizeFeaturesCom]);
		}
		if(sizeFeaturesInd>0){
			featuresInd = new ArrayRealVector(new double[sizeFeaturesInd]);
			thetaArm = new ArrayRealVector(new double[sizeFeaturesInd]);
			thetaArmStar = new ArrayRealVector(new double[sizeFeaturesInd]);
			bArm = new ArrayRealVector(new double[sizeFeaturesInd]);
			AArm = new Array2DRowRealMatrix(new double[sizeFeaturesInd][sizeFeaturesInd]);
			AArmInverse = new Array2DRowRealMatrix(new double[sizeFeaturesInd][sizeFeaturesInd]);
		}
		
		if(sizeFeaturesInd>0 && sizeFeaturesCom>0){
			BArm = new Array2DRowRealMatrix(new double[sizeFeaturesInd][sizeFeaturesCom]);
		}
		
		this.initArmContext();

	}

	public void initArmContext(){
		for (int i = 0; i<sizeFeaturesCom;i++){
			featuresCom.setEntry(i, 0);
		}
		for (int i = 0; i<sizeFeaturesInd;i++){
			featuresInd.setEntry(i, 0);
			thetaArm.setEntry(i, 0);
			bArm.setEntry(i, 0);
			for (int j = 0; j<sizeFeaturesInd;j++){
				if(i==j){
					AArm.setEntry(i, j, 1.0);
					AArmInverse.setEntry(i, j, 1.0);
				}
				else{
					AArm.setEntry(i, j, 0);
					AArmInverse.setEntry(i, j, 0);
				}
			}
			for (int j = 0; j<sizeFeaturesCom;j++){
				BArm.setEntry(i, j, 0);
			}
			
		}

	}

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
			for(int i=0;i<sizeOthers;i++){
				featuresInd.setEntry(i+sizeText, Math.max(tOthers[i]/1000,featuresInd.getEntry(i)));
			}
			featuresInd.setEntry(sizeFeaturesInd-1, 1.0);
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
			/*for(int i=0;i<sizeOthers;i++){
				featuresCom.setEntry(i+sizeText, Math.max(tOthers[i]/1000,featuresCom.getEntry(i)));
			}*/
			featuresInd.setEntry(0, 1.0);
			//System.out.println(featuresCom);
			break;
		default:
		}
	}
}
