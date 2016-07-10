package optimization;

import java.util.ArrayList;

// TODO

public class Neurone extends Fonction {
	ArrayList<Fonction> subs=new ArrayList<Fonction>();
	boolean bias;
	ActivationFonction act=null;
	public Neurone(int nbParams,boolean bias, ActivationFonction act){
	   this.bias=bias;
	   this.nbParams=nbParams;
	   this.act=act;
	}
	public boolean setSubFonction(Fonction fonction){
		int nbp=(bias)?(nbParams-1):nbParams;
		if(nbp==subs.size()) return false;
		subs.add(fonction);
		return true;
	}
	
	@Override
	public void buildDerivativeFonction() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inferValues() {
		// TODO Auto-generated method stub

	}
	public Fonction getReverseParamsSamplesFonction(){
		throw new RuntimeException("Not defined for this kind of fonction");
	}
}
abstract class  ActivationFonction extends UnaryOperator{
	private static final long serialVersionUID = 1L;
}
class InputNeurone extends Neurone{
	public InputNeurone(int nbParams,boolean bias, ActivationFonction act){
		super(nbParams,bias,act);
		DotFonction dot=new DotFonction(bias);
		subs.add(dot);

	}
	public boolean setSubFonction(Fonction fonction){
		return false;
	}
 
}

class NeuralNetwork extends Fonction{
	public NeuralNetwork(ArrayList<Integer> layerSizes, int nbDimsSamples, ArrayList<Boolean> bias, ArrayList<ActivationFonction> acts){
		ArrayList<Neurone> lastLayer=null;
		for(int i=0;i<layerSizes.size();i++){
			ArrayList<Neurone> currentLayer=new ArrayList<Neurone>();;
			int nb=layerSizes.get(i);
		    
		    boolean b=bias.get(i);
		    ActivationFonction act=acts.get(i);
			for(int j=0;j<nb;j++){
				Neurone n;
				if(i==0){
					int nbd=(b)?(nbDimsSamples+1):nbDimsSamples;
					n=new InputNeurone(nbd,b,act);
				}
				else{
					int nbd=(b)?(lastLayer.size()+1):lastLayer.size();
					n=new Neurone(nbd,b,act);
					for(Neurone pn:lastLayer){
						n.setSubFonction(pn);
					}
				}
				currentLayer.add(n);
			}
			lastLayer=currentLayer;
		}
	}
	@Override
	public void buildDerivativeFonction() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inferValues() {
		// TODO Auto-generated method stub

	}
}

