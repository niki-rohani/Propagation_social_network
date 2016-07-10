package optimization;

public class InferFonctionFactory {
	private int config;
	public InferFonctionFactory(int config){
		this.config=config;
	}
	public Fonction buildMinus(Fonction f){
		Minus min=new Minus();
		min.setSubFunction(f);
		return(min);
	}
	public Fonction buildFonction(){
		Fonction ret=null;
		switch (config) {
        	case 1:  ret=config1();
                 break;
        	case 2:  ret=config2();
            	 break;
        	case 3:  ret=config3();
       	 		 break;
        	case 4:  ret=config4();
  	 		 	 break;
        	case 5:  ret=config5();
	 		 	 break;
        	default: ret = null;
                 break;
		}
		return(ret);
	}
	public Fonction config1(){
		DotFonction f=new DotFonction();
		return(f);
	}
	public Fonction config2(){
		LogitFonction f=new LogitFonction();
		DotFonction f2=new DotFonction(true);
		f.setSubFunction(f2);
		return(f);
	}
	public Fonction config3(){
		Inverse f=new Inverse();
		PlusConstant pl=new PlusConstant(1.0);
		Exp exp=new Exp();
		Minus min=new Minus();
		DotFonction f2=new DotFonction(true);
		min.setSubFunction(f2);
		exp.setSubFunction(min);
		pl.setSubFunction(exp);
		f.setSubFunction(pl);
		return(f);
	}
	public Fonction config4(){
		Inverse f=new Inverse();
		PlusConstant pl=new PlusConstant(1.0);
		Exp exp=new Exp();
		Minus min=new Minus();
		PlusConstant pl2=new PlusConstant(-101.20747158198678);
		DotFonction f2=new DotFonction(false);
		pl2.setSubFunction(f2);
		min.setSubFunction(pl2);
		exp.setSubFunction(min);
		pl.setSubFunction(exp);
		f.setSubFunction(pl);
		return(f);
	}
	public Fonction config5(){
		/*Power p=new Power(2);
		DotFonction f2=new DotFonction(false);
		p.setSubFunction(f2);
		return(p);*/
		
		Inverse inv=new Inverse();
		DotFonction f2=new DotFonction(false);
		inv.setSubFunction(f2);
		return(inv);
	}
	
}