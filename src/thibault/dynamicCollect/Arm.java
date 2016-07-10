package thibault.dynamicCollect;

import core.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import core.Post;
import core.User;

import java.util.HashSet;

import jgibblda.Inferencer;

import org.la4j.matrix.Matrix;
import org.la4j.vector.Vector;

import thibault.simulationBandit.Distributions;
public abstract class Arm extends Text{
	
	
	//4 1 100 100 usElections5000_hashtag src/thibault/RewardsFull.txt 30 2.0 false
	//-Xincgc  -Xms512m -Xmx12G
	public int numberPlayed=0;
	public int numberPlayedOnObs=0; //nombre de fois ou on l a joue sachant qu on la observe au pas d avant
	public int numberObs=0; //nombre de fois ou l a observe
	
	public int nbItPolicy=0; // nombre de pas de temps dans la politique
	
	public double lastReward=0.0;
	public double sumRewards=0.0;
	public double sumRewardsOnObs=0.0;
	
	
	public double moyReward=0.0;
	public double moyRewardPrev=0.0;
	public double moyRewardOnObs=0.0;
	public double moyRewardOnObsPrev=0.0;
	
	
	public double sumSqrtRewards=0.0;
	
	public double prob=0.0;
	public double estimatedCumulativeGain=0.0;
	double thompsonPrior;
	double S=0;
	double F=0;
	
	public Vector features;
	public Vector sumFeatures;
	public Vector sumFeaturesOnObs;
	public Vector sumProdFeaturesRwd;
	public Matrix sumOuterProdFeatures;
	public Matrix sumOuterProdFeaturesOnObs;

	
	public int nbFeatures;
	public int nbFeaturesInd;
	double denom; 
	
	int RT=0;
	int RTo=0;
	int followersNb=1;
	int statsNb=1;
	int friendsNb=1;
	int favouritesNb=1;
	
	
	
	public double score=0.0;
	
	public double theta=0.0;
	public Vector Theta;
	
	public Arm(String name){
		super(name);
	}
	public int getNumberPlayed(){
		return numberPlayed;
	}
	public double getSumRewards(){
		return sumRewards;
	}
	double getSumSqrtRewards(){
		return sumSqrtRewards;
	}
	double getLastReward(){
		return lastReward;
	}
	double getProb(){
		return prob;
	}
	double getEstimatedCumulativeGain(){
		return estimatedCumulativeGain;
	}
	
	public void updateFactorsBetaGeneral(){
		Distributions simValue= new Distributions();
		boolean val=simValue.nextBoolean(lastReward);
		double r= val ? 1.0:0.0;	
		if(r==1.0){S=S+1;}
		else{F=F+1;}
}

	public abstract void computeReward(Reward r, HashSet<Post> posts); 
	public abstract void observeContext( HashSet<Post> posts,Matrix M,Inferencer inferencer,double gamma); 
	public abstract void initFeatures(); 
	
}

