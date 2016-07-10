package thibault.SNCollect;

import java.util.HashMap;

import core.Post;

public abstract class ModelLanguage {

	HashMap<Integer, Double> poidsModel;
	double norme=0;
	int nbWords=2000;
	public ModelLanguage(){
		this.poidsModel=new HashMap<Integer, Double>();
	}

	abstract void setWeights();
	public abstract String toString();

	double eval(Post p){
		double normePost=0;
		double score = 0.0;
		if(Double.parseDouble(p.getOther().get("iAmRT").toString())==0.0 && Double.parseDouble(p.getOther().get("iAmRTo").toString())==0.0){
			//System.out.println("bas");
			for (int key:p.getWeights().keySet()){
				if(key<=nbWords){
					score+=p.getWeights().get(key)*this.poidsModel.get(key);
					normePost=normePost+p.getWeights().get(key)*p.getWeights().get(key);
				}
			}
			normePost=Math.sqrt(normePost);
			
			if(score!=0){
				score=score/(normePost*this.norme);
			}
		}
		
		return score;

	}
}


//Dans cette classe on peut mettre les poids a partir d une ligne de texte au format (ind1,poids1) (ind2,poids2) ...
//Ensuite on s'en sert quand on va configurer leval en lisant un fichier ligne par ligne
class ModelLanguageFromFile extends ModelLanguage{


	String lineToRead;

	public ModelLanguageFromFile(String lineToRead){
		super();
		this.lineToRead=lineToRead;
		this.setWeights();
	}

	@Override
	void setWeights() {

		HashMap<Integer, Double> poidsNonNull=new HashMap<Integer,Double>();
		String ligne1=lineToRead.replace("(", "");
		String ligne2=ligne1.replace(")", "");
		String str[] =ligne2.split("\t");
		for(int i=0;i<str.length;i++){
			String str1[] =str[i].split(",");
			poidsNonNull.put(Integer.parseInt(str1[0]),Double.parseDouble(str1[1]));
		}
		double sum=0;
		for(int i=1;i<nbWords+1;i++){
			if(poidsNonNull.containsKey(i)){
				double val =poidsNonNull.get(i);
				this.poidsModel.put(i,val);  
				sum = sum+val;	
			}
			else{
				this.poidsModel.put(i,0.0); 
			}

		}

		for(int key:this.poidsModel.keySet()){
			this.poidsModel.put(key,this.poidsModel.get(key)/sum);
			this.norme=this.norme+this.poidsModel.get(key)*this.poidsModel.get(key);
		}

		this.norme=Math.sqrt(this.norme);
	}

	public String toString(){
		String r="";
		for(int key:this.poidsModel.keySet()){
			if (this.poidsModel.get(key)!=0.0){
				r=r+"("+key+","+this.poidsModel.get(key)+") "; 
			}
		}
		return "ModelLanguageFromFile"+r;
	}

}