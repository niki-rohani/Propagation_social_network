package thibault.YahooCollect;

import java.util.Collection;
import java.util.HashSet;


import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import thibault.SNCollect.Arm;


public class ArmContextYahoo extends Arm{

	public ArmContextYahoo(String name) {
		super(name);
	}

	private static final long serialVersionUID = 1L;


	public int sizeFeaturesInd;
	public RealVector thetaArm;
	public RealVector bArm;
	public RealMatrix AArm;
	public RealMatrix AArmInverse;
	public RealMatrix BArm;

	public ArmContextYahoo(String name, int sizeFeaturesInd) {
		super(name);
		this.sizeFeaturesInd=sizeFeaturesInd;

		if(sizeFeaturesInd>0){
			thetaArm = new ArrayRealVector(new double[sizeFeaturesInd]);
			bArm = new ArrayRealVector(new double[sizeFeaturesInd]);
			AArm = new Array2DRowRealMatrix(new double[sizeFeaturesInd][sizeFeaturesInd]);
			AArmInverse = new Array2DRowRealMatrix(new double[sizeFeaturesInd][sizeFeaturesInd]);
		}
		
		
		this.initArmContext();

	}

	public void initArmContext(){
		for (int i = 0; i<sizeFeaturesInd;i++){
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
		}

	}
}
