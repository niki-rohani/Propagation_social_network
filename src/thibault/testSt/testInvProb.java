package thibault.testSt;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

public class testInvProb {

	
	public static double invertNormalG (double confInt ){
		NormalDistribution n = new NormalDistribution();	
		return n.inverseCumulativeProbability((confInt+1)/(2.0));
	}
	
	public static double CenteredChi2 (double confInt ){
		NormalDistribution n = new NormalDistribution();
		double val =  n.inverseCumulativeProbability(confInt);
		return Math.pow(Math.sqrt(2.0/9.0)*val+1.0-2.0/9.0, 3.0);
	}
	
	public static double invertNonCenteredChi2 (int lambda, double confInt){
		NormalDistribution n = new NormalDistribution();
		double h = 1.0-2.0/3.0*(1.0+lambda)*(1.0+3.0*lambda)/((1.0+2.0*lambda)*(1.0+2.0*lambda));
		double p = (1.0+2.0*lambda)/((1.0+lambda)*(1.0+lambda));
		double m = (h-1.0)*(1.0-3.0*h);
		double val = n.inverseCumulativeProbability(confInt);
		return Math.pow((h*Math.sqrt(2*p)*(1+0.5*m*p))*val+(1+h*p*(h-1-0.5*(2-h)*m*p)), 1.0/h)*(lambda+1);
	}
	
	public static void main(String[] args) {
		System.out.println(testInvProb.invertNormalG(0.95));
		
		//System.out.println(testInvProb.CenteredChi2(0.995));
		//System.out.println(testInvProb.invertNonCenteredChi2(1,0.995));
		
		
		/*ChiSquaredDistribution c = new ChiSquaredDistribution(1);
		System.out.println(c.cumulativeProbability(3.82));
		
		NormalDistribution n = new NormalDistribution();
		System.out.println(n.cumulativeProbability((Math.pow(3.82, 1.0/3)-(1.0-2.0/9.0))/(Math.sqrt(2.0/9.0))));*/
	}

}
