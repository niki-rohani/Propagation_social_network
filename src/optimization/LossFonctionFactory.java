package optimization;

public class LossFonctionFactory {
	private int config;
	public LossFonctionFactory(int config){
		this.config=config;
	}
	public Fonction buildFonction(){
		Fonction ret=null;
		switch (config) {
        	case 1:  ret=config1();
                 break;
        	default: ret = null;
                 break;
		}
		return(ret);
	}
	public Fonction config1(){
		AverageFonction f=new AverageFonction();
		f.setSubFunction(new SquaredLoss());
		return(f);
	}
	
}
