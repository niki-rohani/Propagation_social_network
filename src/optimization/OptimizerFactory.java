package optimization;

public class OptimizerFactory {
	private int config;
	public OptimizerFactory(int config){
		this.config=config;
	}
	public Optimizer buildOptimizer(){
		Optimizer ret=null;
		switch (config) {
        	case 1:  ret=config1();
                 break;
        	case 2:  ret=config2();
            break;
        	case 3:  ret=config3();
            break;
        	default: ret = null;
                 break;
		}
		return(ret);
	}
	public Optimizer config1(){
		LineSearch ls=new LineSearch6();
		DescentDirection dd=new GradientDirection();
		Descent d=new Descent(dd,ls,false);
		return(d);
	}
	public Optimizer config2(){
		LineSearch ls=new LineSearch6();
		DescentDirection dd=new FR_ConjugateGradientDirection();
		Descent d=new Descent(dd,ls,false);
		return(d);
	}
	public Optimizer config3(){
		LineSearch ls=new LineSearch4(10,0.01);
		DescentDirection dd=new GradientDirection();
		Descent d=new Descent(dd,ls,true);
		return(d);
	}
	
	
}