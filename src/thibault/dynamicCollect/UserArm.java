package thibault.dynamicCollect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import jgibblda.Inferencer;
import jgibblda.Model;

import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.Vectors;
import org.la4j.vector.dense.BasicVector;

import core.Post;
import core.User;

public class UserArm extends Arm{
	
	int maxFavouritesNb=119370;
	int maxFriendsNb=240092;
	int maxStatsNb= 116021;
	int maxFollowersNb= 1782916;
	
	public UserArm(String name){
		super(name);
	}
	
	public UserArm(String name, int nbFeatures){
		super(name);
		this.nbFeatures=nbFeatures;
		this.features=new BasicVector(new double[nbFeatures]);
		this.Theta=new BasicVector(new double[nbFeatures]);
		this.sumFeatures=new BasicVector(new double[nbFeatures]);
		this.sumFeaturesOnObs=new BasicVector(new double[nbFeatures]);
		this.sumProdFeaturesRwd=new BasicVector(new double[nbFeatures]);
		this.sumOuterProdFeatures= new Basic2DMatrix(new double[nbFeatures][nbFeatures] );
		this.sumOuterProdFeaturesOnObs= new Basic2DMatrix(new double[nbFeatures][nbFeatures] );
		this.initFeatures();
		this.denom=0;
	}
	
	
	public void computeReward(Reward r, HashSet<Post> posts) 
	{
		User me=User.getUser(name);
		Collection<Post> mines=me.getPosts().values();
		lastReward=r.getReward(mines,this);
	}
	
	public void initFeatures(){
		for(int i=0;i<nbFeatures;i++){
			//this.features.set(i, Math.random());
			this.features.set(i,0);
			this.Theta.set(i,0);
			this.sumFeatures.set(i,0);
			this.sumFeaturesOnObs.set(i,0);
			this.sumProdFeaturesRwd.set(i,0);
			for(int j=0;j<nbFeatures;j++){
				this.sumOuterProdFeatures.set(i, j, 0);
				this.sumOuterProdFeaturesOnObs.set(i, j, 0);
			}
		}
	}
	


	public void updateMoyRwd(){
		moyRewardPrev=moyReward;
		moyReward=sumRewards/numberPlayed;
	}
	


	
	public void observeContext(HashSet<Post> posts, Matrix M,Inferencer inferencer,double gamma) {
		this.numberObs++;
		User me=User.getUser(name);
		Collection<Post> mines=me.getPosts().values();

		String concatPost[]=new String[1];
		concatPost[0]="";
		//this.RT=0;
		//this.RTo=0;
		double[] t1 = new double[nbFeatures];
		if(!mines.isEmpty()){
		for (Post p:mines){	
			for (int w:p.getWeights().keySet()){
				//context LDA
				concatPost[0]=w+" "+concatPost[0];
				
				//context RT
				//this.RT=this.RT+Integer.parseInt(p.getOther().get("iAmRT").toString());
				//this.RTo=this.RTo+Integer.parseInt(p.getOther().get("iAmRTo").toString());
				//this.followersNb=Math.max(Integer.parseInt(p.getOther().get("followers_count").toString()),this.followersNb);
				//this.statsNb=Math.max(Integer.parseInt(p.getOther().get("statuses_count").toString()),this.statsNb);
				//this.friendsNb=Math.max(Integer.parseInt(p.getOther().get("friends_count").toString()),this.friendsNb);
				//this.favouritesNb=Math.max(Integer.parseInt(p.getOther().get("favourites_count").toString()),this.favouritesNb);
				


			}
			
		}
		//System.out.println(followersNb+" "+statsNb+" "+friendsNb+" "+favouritesNb);
		
		//Context LDA
		//Model newModel = inferencer.inference(concatPost); 
		//Vector featuresTempLDA=new BasicVector(newModel.theta[0]);
		//features=features.multiply(denom*gamma).add(featuresTempLDA).divide(denom*gamma+1);
		//denom=denom*gamma+1;
		//context RT 
		//features = new BasicVector(new double[] {0, RT,RTo,followersNb,statsNb,friendsNb,favouritesNb});

		//Deux ctxt mais sans discount
		Model newModel = inferencer.inference(concatPost);
		t1=newModel.theta[0];
		}
		else{
			for(int i=0;i<nbFeatures-4;i++){
				t1[i]=0;
			}
			
		}
		//double[] t2=new double[] {followersNb/maxFollowersNb,statsNb/maxStatsNb,friendsNb/maxFriendsNb,favouritesNb/maxFavouritesNb};
		double[] t2=new double[] {followersNb/1000,statsNb/1000,friendsNb/1000,favouritesNb/1000};
		//this.normalizeTable(t2);
		double[] t12 = new double[t1.length + t2.length];
		System.arraycopy(t1, 0, t12, 0, t1.length);
		System.arraycopy(t2, 0, t12, t1.length, t2.length);
		features=new BasicVector(t1);


		
		
		

	}

	public void normalizeTable(double[] l){
		double norm=0;
		for(int i=0;i<l.length;i++){
			norm+=l[i]*l[i];
		}
		if(norm!=0){
			norm=Math.sqrt(norm);
			for(int i=0;i<l.length;i++){
				l[i]=l[i]/norm;
			}	
		}
	
	}
	}

