package simon.mlp;

import java.util.ArrayList;

import mlp.CPUAddVals;
import mlp.CPUL2Norm;
import mlp.CPUParams;
import mlp.CPUPower;
import mlp.CPUSum;
import mlp.Parameter;
import mlp.Parameters;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.Module ;
import mlp.Matrix ;
import mlp.Tensor ;
import mlp.CPUMatrix ;


// Module calculant les distance entre les deux CPU params passés en input.
public class CPUL2Dist2 extends SequentialModule {
	
	private static double minDist = 0.1 ;
	private boolean weighted ;
	private Module norm ;

	
	public CPUL2Dist2(int size) {
		this(size,false) ;
	}
	
	public CPUL2Dist2(int size, boolean wei) {
		this.weighted = wei ;
		ArrayList<Double> w = new  ArrayList<Double>(2);
		w.add(1.0) ;
		w.add(-1.0) ;
		CPUSum cpus = new CPUSum(size,2,w) ;
		
		if(weighted) {
			norm = new CPUL2Wtest(size) ;
		} else {
			norm = new CPUL2Norm(size) ;
		}
			
	
		this.addModule(cpus);
		this.addModule(norm);
		if(minDist!=0)
			this.addModule(new CPUAddVals(1,minDist));
	}
	
	
	public double dist(CPUParams x1 , CPUParams x2) {

		ArrayList<Parameter> p1 = x1.getParamList().getParams() ;
		ArrayList<Parameter> p2 = x2.getParamList().getParams() ;
		if(p1.size()!=p2.size()){
			throw new RuntimeException("P1 and P2 not same size ("+p1.size()+", "+p2.size()+").") ;
		}
		double d = minDist ;
		if(!weighted) {
			for(int i=0 ; i<p1.size() ; i++) {
				d+=Math.pow(p1.get(i).getVal()-p2.get(i).getVal(),2) ;
				//System.out.println(m1.getValue(0, i)+" ; "+ m2.getValue(0, i));
			}
		} else {
			Tensor weights = ((CPUL2Wtest)norm).getParameters() ;
			//System.out.println( ((CPUL2WeightedNorm)norm).getParameters());
			for(int i=0 ; i<p1.size() ; i++) {
				double w = weights.getMatrix(0).getValue(i, 0) ;
				d+=Math.pow(p1.get(i).getVal()-p2.get(i).getVal(),2) ;
				//System.out.println(m1.getValue(0, i)+" ; "+ m2.getValue(0, i));
			}
		}
		return d ;
		
	}
	
	
	public static void main(String[] args) {
		Matrix m = new CPUMatrix(3,2) ;
		m.setValue(0, 0, 1);
		m.setValue(0, 1, 1);
		m.setValue(1, 0, 1);
		m.setValue(1, 1, 1);
		m.setValue(2, 0, 1);
		m.setValue(2, 1, 2);
		Matrix m2 = new CPUMatrix(3,2) ;
		m2.setValue(0, 0, 4);
		m2.setValue(0, 1, 5);
		m2.setValue(1, 0, 3);
		m2.setValue(1, 1, 3);
		m2.setValue(2, 0, -1);
		m2.setValue(2, 1, -1);
		Tensor t=new Tensor(m, m2) ;
		CPUL2Dist2 d = new CPUL2Dist2(2) ;
		d.forward(t);
		
		System.out.println(d.getOutput());
		
		System.out.println();
	}
	
	public void setParameters(Parameters pList) {
		if(weighted) {
			((CPUL2Wtest)norm).setParameters(pList);
		}
	}
	
	

}
