package similarities;
import java.io.Serializable;
import java.util.HashMap;
import java.util.ArrayList;
import core.*;
public abstract class StrSim implements Serializable{
	public static final long serialVersionUID=1;
	protected HashMap<Integer,HashMap<Integer,Double>> sims; // similarites deja calculees (on stocke que la demi matrice inferieure => sim(1,2) pas sim(2,1)
	protected Data data=null; // donnees pour lesquelles les similarites sont calculees (si vide on n'enregistre rien dans sims, juste utilise pour des calculs ponctuels) 
	public StrSim(Data data){
		this.data=data;
		sims=new HashMap<Integer,HashMap<Integer,Double>>();
	}
	public StrSim(){
		this.data=null;
		sims=null;
	}
	public abstract StrSim getInstance(Data data);
	
	public void setData(Data data){
		this.data=data;
		sims=new HashMap<Integer,HashMap<Integer,Double>>();
	}
	public void setSim(int texte1,int texte2, double val) throws Exception{
		if (data==null){
			throw new Exception("Pas de donnees");
		}
		if (texte1>texte2){
			int swap=texte1;
			texte1=texte2;
			texte2=swap;
		}
		HashMap<Integer,Double> s=sims.get(texte1);
		
		if (s==null){
			s=new HashMap<Integer,Double>();
			sims.put(texte1, s);	
		}
		s.put(texte2, val);
		
	}
	
	public Data getData(){
		return(data);
	}
	
	//public abstract double computeSim(String titre1,HashMap<Integer,Double> poids1,String titre2,HashMap<Integer,Double> poids2);
	public abstract double computeSimilarity(Text t1,Text t2);
	
	// passe par des getSim (au cas ou on fait appel dans une redefinition a une autrre strategie, on recupere des sims deja calculees
	public double computeSim(int text1,int text2) throws Exception{
		if (text1==text2){
			return(1.0);
		}
		Text t1=data.getText(text1);
		Text t2=data.getText(text2);
		if ((t1==null) || (t2==null)){
			throw new Exception("Donnee manquante");
		}
		//return(computeSim(t1.getTitre(),t1.getPoids(),t2.getTitre(),t2.getPoids()));
		return(computeSimilarity(t1,t2));
	}
	
	public double computeSim(Text texte1,Text texte2){
		int id1=texte1.getID();
		int id2=texte2.getID();
		if ((id1>=0) && (id2>=0) && (id1==id2)){
			return(1.0);
		}
		return(computeSimilarity(texte1,texte2));
	}
	public double getSim(int texte1,int texte2) throws Exception{
		if (data==null){
			throw new Exception("Pas de donnees");
		}
		/*Text t1=data.getText(texte1);
		Text t2=data.getText(texte2);
		if ((t1==null) || t2==null)) {
			
		}*/
		if (texte1>texte2){
			int swap=texte1;
			texte1=texte2;
			texte2=swap;
		}
		double val=0.0;
		HashMap<Integer,Double> s=sims.get(texte1);
		
		if (s==null){
			s=new HashMap<Integer,Double>();
			sims.put(texte1, s);	
		}
		if (s.containsKey(texte2)){
			val=s.get(texte2);
		}
		else{
			val=computeSim(texte1,texte2);
			s.put(texte2, val);
		}
		//System.out.println("Sim "+texte1+" et "+texte2+" = "+val);
		return(val);
	}
	public double sum_sim_el_with_group(int el,ArrayList<Integer> groupe){
		double ret=0.0;
		for(int i=0;i<groupe.size();i++){
			try{
				int gi=groupe.get(i);
				ret+=getData().getWeight(gi)*getSim(el,gi);
			}
			catch(Exception e){
				System.out.println("Exception bizarre a partir de StrSim.sum_sim_el_with_group");
				throw new RuntimeException(e);
			}
		}
		return(ret);
	}
	
	public abstract String toString();
	
}
