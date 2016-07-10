package thibault.graphEmbeddings;
import java.util.HashMap;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public abstract class SimDiffusion {
	int k;
	int sizeSpace;
	Graph G;
	double maxT=1.5;
	double sizeIt=0.01;
	double alpha=2;
	public RealVector initCond;
	
	public SimDiffusion(int k, int sizeSpace, Graph G){
		this.k=k;
		this.sizeSpace=sizeSpace;
		this.G=G;
		this.initCond=new ArrayRealVector(new double[k]);
		this.initCond.setEntry(0, 1);
	}
	
	public abstract void launchSim();

}


class SimDiffusionGraph extends SimDiffusion{

	int P;
	public SimDiffusionGraph(int k, int sizeSpace, Graph G, int P) {
		super(k, sizeSpace, G);
		this.P=P;	
	}
	
	@Override
	public void launchSim() {
		RealMatrix I=new Array2DRowRealMatrix(new double[k][k]);
		RealMatrix H=this.G.L.scalarMultiply(-1);
		for(int i=0;i<k;i++){
			for(int j=0;j<k;j++){
				if(i==j){
					I.setEntry(i, j, 1.0);
				}
				else{
					I.setEntry(i, j, 0.0);
				}
			}
		}
		for(int i=0;i<maxT/sizeIt;i++){
			RealMatrix D=I.add(H.scalarMultiply(alpha*i*sizeIt/P)).power(P);
			RealVector res = D.operate(initCond);
			for (Node u:G.nodes){
				u.values.add(res.getEntry(u.Id));
			}
		}

	}
	
}

class SimDiffusionEmbed extends SimDiffusion{

	public SimDiffusionEmbed(int k, int sizeSpace, Graph G) {
		super(k, sizeSpace, G);
	}

	@Override
	public void launchSim() {
		for(int i=0;i<maxT/sizeIt;i++){
			RealVector x0 =new ArrayRealVector(new double[sizeSpace]); ;
			for (Node u:G.nodes){
					if(u.Id==0){
						x0=u.coords;
				}
			}
			for (Node u:G.nodes){
				double val;
				if(i==0){
					if(u.Id==0){
						val= 1;
					}
					else{
						val=0;
					}
					
				}
				else{
					if(u.Id==0){
						val = 1/Math.pow(4*Math.PI*i*sizeIt*alpha, this.sizeSpace*1.0/2);
					}
					else{
						val = 1/Math.pow(4*Math.PI*i*sizeIt*alpha, this.sizeSpace*1.0/2)*Math.exp(-u.coords.subtract(x0).dotProduct(u.coords.subtract(x0))/(4*i*sizeIt*alpha));
					}
					 
				}
				
				//System.out.println(val);
				u.values.add(val);
			}
		}
		
	}
}
	

class SimDiffusionEmbed2 extends SimDiffusion{

	public SimDiffusionEmbed2(int k, int sizeSpace, Graph G) {
		super(k, sizeSpace, G);
	}

	@Override
	public void launchSim() {
		for(int i=0;i<maxT/sizeIt;i++){
			RealVector x0 =new ArrayRealVector(new double[sizeSpace]); ;
			for (Node u:G.nodes){
					if(u.Id==0){
						x0=u.coords2;
				}
			}
			for (Node u:G.nodes){
				double val;
				if(i==0){
					if(u.Id==0){
						val= 1;
					}
					else{
						val=0;
					}
					
				}
				else{
					if(u.Id==0){
						val = 1/Math.pow(4*Math.PI*i*sizeIt*alpha, this.sizeSpace*1.0/2);
					}
					else{
						val = 1/Math.pow(4*Math.PI*i*sizeIt*alpha, this.sizeSpace*1.0/2)*Math.exp(-u.coords2.subtract(x0).dotProduct(u.coords2.subtract(x0))/(4*i*sizeIt*alpha));
					}
					 
				}
				
				//System.out.println(val);
				u.values.add(val);
			}
		}
		
	}
}

	
