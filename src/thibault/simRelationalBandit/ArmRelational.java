package thibault.simRelationalBandit;

import java.util.ArrayList;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class ArmRelational {

	public int Id;
	public int sizeFeatures;
	public int sizeSpace;
	public int numberPlayed=0;
	public double sumRewards=0.0;
	public double lastReward=0.0;
	public double score=0.0;
	public double theta=0.0;
	public RealVector CurrentContext;
	public RealVector thetaVec;
	public RealVector coords;
	public RealVector gradCoords;
	public double gradTheta=0.0;
	public RealVector gradThetaVec;
	
	public RealVector b;
	public RealMatrix A;
	public RealMatrix AInv;
	
	public ArrayList<Double> rewardsList;//
	public ArrayList<RealVector> contextList;//
	
	public ArmRelational(int Id,int sizeFeatures, int sizeSpace){
		//this.theta=Math.random();
		this.Id=Id;
		this.sizeFeatures=sizeFeatures;
		this.sizeSpace=sizeSpace;
		CurrentContext=new ArrayRealVector(new double[sizeFeatures]);
		thetaVec=new ArrayRealVector(new double[sizeFeatures]);
		//thetaVecStar=new ArrayRealVector(new double[sizeFeatures]);
		coords=new ArrayRealVector(new double[sizeSpace]);
		gradCoords=new ArrayRealVector(new double[sizeSpace]);
		gradThetaVec=new ArrayRealVector(new double[sizeFeatures]);
		b=new ArrayRealVector(new double[sizeFeatures]);
		A = new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures]);
		AInv = new Array2DRowRealMatrix(new double[sizeFeatures][sizeFeatures]);
		this.rewardsList=new ArrayList<Double>();
		this.contextList=new ArrayList<RealVector>();
		init();
	}
	
	public void init(){
		for (int i = 0; i<sizeFeatures;i++){
			CurrentContext.setEntry(i, 0);
			thetaVec.setEntry(i, Math.random());
			gradThetaVec.setEntry(i, 0);
			//thetaVecStar.setEntry(i, 0);
			b.setEntry(i, 0);
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){
					A.setEntry(i, j, 1.0);	
					AInv.setEntry(i, j, 1.0);
				}
				else{
					A.setEntry(i, j, 0.0);	
					AInv.setEntry(i, j, 0.0);
				}
			}
		}
		for (int i = 0; i<sizeSpace;i++){
			coords.setEntry(i, Math.random());
			gradCoords.setEntry(i, 0.0);
		}
	}
	
	public void reinit(){
		numberPlayed=0;
		sumRewards=0.0;
		lastReward=0.0;
		score=0.0;
		theta=0.0;
		gradTheta=0.0;
		init();
	}
	
	public void getContext(String[] sTable){
		for(int i=0;i<sTable.length;i++){
			int ind=Integer.parseInt(sTable[i].split(",")[0]);
			double val=Double.parseDouble(sTable[i].split(",")[1]);
			CurrentContext.setEntry(ind, val);
		}
		CurrentContext.setEntry(Id, 1);
		//System.out.println(this.Id+" "+this.CurrentContext);
	}
	
	public void getReward(String[] sTable){
		
		for(int i=0;i<sTable.length;i++){
			int ind=Integer.parseInt(sTable[i].split(",")[0]);
			if(ind==Id){
				double val=Double.parseDouble(sTable[i].split(",")[1]);
				this.lastReward=val;
			}
			
		}
		//System.out.println(this.Id+" "+this.lastReward);
	}
}
