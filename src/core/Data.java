package core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;



public class Data  implements Serializable,Structure{
	public static final long serialVersionUID=1;
	private HashMap<Integer,Text> textes;
	private HashMap<Integer,Double> weights;  // importance des termes dans le dataset
	public Data(HashMap<Integer,Text> textes){
		this.textes=textes;
		weights=null;
		
	}
	public Data(HashMap<Integer,Text> textes, HashMap<Integer,Double> w){
		this.textes=textes;
		weights=new HashMap<Integer,Double>();
		for(Integer i:textes.keySet()){
			//double v=1.0;
			if (w.containsKey(i)){
				double v=w.get(i);
				weights.put(i, v);
			}
			
		}
	}
	public Data(){
		this.textes=new HashMap<Integer,Text>();
		//weights=new HashMap<Integer,Double>();
	}
	public void addText(Text text){
		textes.put(text.getID(),text);
		//weights.put(text.getID(), 1.0);
	}
	public void addText(Text text,double w){
		if (weights==null){
			weights=new HashMap<Integer,Double>();
			/*for(Integer i:textes.keySet()){
				weights.put(i, 1.0);
			}*/
		}
		textes.put(text.getID(),text);
		weights.put(text.getID(), w);
	}
	public Text getText(int i){
		return(textes.get(i));
	}
	public double getWeight(int i){
		if(weights==null){
			return 1.0;
		}
		Double w=weights.get(i);
		if (w==null){
			return 0.0;
		}
		return(w);
	}
	public HashMap<Integer,Text> getTexts(){
		return(textes);
	}
	// retourne une liste ordonnee de n ids tires aleatoirement
	// si n==-1 => tous les elements de textes apparaissent dans la liste
	public ArrayList<Integer> sample(int n){
		if (n<0){
			n=textes.size();
		}
		ArrayList<Integer> ret=new ArrayList<Integer>();
		ArrayList<Integer> set=new ArrayList<Integer>(textes.keySet());
		int nset=set.size();
		int nb=0;
		while(nb<n){
			//System.out.println(nb);
			int i=(int)(Math.random()*nset);
			int x=set.remove(i);
			ret.add(x);
			//System.out.println(x + " size ="+textes.size());
			nset--;
			nb++;
		}
		return(ret);
	}
}
