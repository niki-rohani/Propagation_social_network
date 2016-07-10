package simon.test;

import mlp.CPUParams; 
import mlp.Parameters ;

public class Test {

	public Test() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Double d = null ;
		double d1 = d ;
		System.out.println(d);
		System.out.println(d1);
		
		
		Parameters params = new Parameters() ; 
		
		
		CPUParams p0 = new CPUParams(1, 3) ;
		params.allocateNewParamsFor(p0, 0, 1);
		
		CPUParams p1 = new CPUParams(1, 3) ;
		params.allocateNewParamsFor(p1, 1, 2);
		
		CPUParams p = new CPUParams(2, 3) ;
		p.addParametersFrom(p0);
		p.addParametersFrom(p1);
		
		p0.forward(null);
		System.out.println(p0.getOutput());
		
		p1.forward(null);
		System.out.println(p1.getOutput());
		
		p.forward(null);
		System.out.println(p.getOutput());
		
		//p0.getParamList().get(0).setVal(20.0);
		
		p.getParamList().get(0).setVal(20.0);
		p.paramsChanged();
		p.forward(null);
		System.out.println(p.getOutput());

	}

}
