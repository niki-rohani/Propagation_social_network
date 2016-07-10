package clustering;

import core.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import similarities.*;
import wordsTreatment.Stemmer;

public class Clustering   implements Serializable{
	public static final long serialVersionUID=1;
	private StrSim strSim; // Les "data" sont dans la sim, souvenons nous en.
	private HashMap<Integer,Integer> iclusters; // iclusters(indice_text dans Data)=indice cluster (indices clusters commencent a 0)
	private ArrayList<ArrayList<Integer>> clusters; // clusters(0) = indices des textes de Data contenus dans le cluster 0
	private int k; // nombre de clusters
	private boolean cohesion_valid=false;
	private double cohesion=0.0;
	private ArrayList<Double> cohesions;
	//private Arraylist<Boolean> cohesions_valid;
	
	public Clustering(int k,StrSim strSim){
		this.strSim=strSim;
		this.k=k;
		reinit();
	}
	
	// "Detruit" le clustering : reset des associations texte->cluster.
	public void reinit(){
		iclusters=new HashMap<Integer,Integer>();
		clusters=new ArrayList<ArrayList<Integer>>();
		for(int i=0;i<k;i++){
			ArrayList<Integer> cl=new ArrayList<Integer>();
			clusters.add(cl);
		}
	}
	// Affecte les elements au hasard dans k clusters
	public void initAlea() throws Exception{
		reinit();
		Data data=strSim.getData();
		if (data==null){
			throw new Exception("Pas de donnees");
		}
		HashMap<Integer,Text> textes=data.getTexts();
		for(Integer i:textes.keySet()){
			int c=(int)(Math.random()*k);
			addToCluster(i,c);
		}
		
	}
	// retourne la cohesion moyenne des clusters
	public double getCohesion(){
		if (!cohesion_valid){
			cohesion=computeCohesion();
			cohesion_valid=true;
		}
		return(cohesion);
	}
	
	// calcule la cohesion moyenne 
	private double computeCohesion(){
			double ret=0.0;
			cohesions=new ArrayList<Double>();
			for(int i=0;i<k;i++){
				double co=computeCohesion(i);
				cohesions.add(co);
				ret+=co;
			}
			if (k>0){
				ret/=k;
			}
			return(ret);
	}
	
	// calcule la cohesion d un cluster 
	private double computeCohesion(int i){
		double ret=0.0;
		ArrayList<Integer> cl=clusters.get(i);
		for(int j=0;j<cl.size();j++){
			int el=cl.get(j);
			/*for(int l=0;l<cl.size();l++){
				int el2=cl.get(l);
				try{
					ret+=strSim.getSim(el1, el2);
				}
				catch(Exception e){
					System.out.println("Exception bizarre");
					throw new RuntimeException(e);
				}
			}*/
			ret+=strSim.sum_sim_el_with_group(el, cl);
		}
		if (cl.size()>0){
			ret/=cl.size();
		}
		return(ret);
	}
	// ajoute l'element el dans le cluster numero i
	public void addToCluster(int el,int i){
		iclusters.put(el, i);
		ArrayList<Integer> cl=clusters.get(i);
		if (cl==null){
			cl=new ArrayList<Integer>();
			clusters.set(i, cl);
		}
		
		if (cohesion_valid){
			double co=cohesions.get(i);
			double old_co=co;
			co*=cl.size();
			double sim=strSim.sum_sim_el_with_group(el, cl);
			co+=2.0*sim;
			try{
				co+=strSim.getSim(el, el);
			}
			catch(Exception e){
				System.out.println("Exception bizarre from Clustering.addToCluster");
				throw new RuntimeException(e);
			}
			
			co/=(cl.size()+1);
			
			cohesions.set(i, co);
			cohesion*=k;
			cohesion+=co-old_co;
			if (k>0){
				cohesion/=k;
			}
		}
		
		cl.add(el);
		
		
		
	}
	// retire l'element el du cluster numero i
	public void removeFromCluster(int el,int i){
			iclusters.remove(el);
			ArrayList<Integer> cl=clusters.get(i);
			
			if (cl!=null){
				cl.remove(new Integer(el));
			}	
			
			if (cohesion_valid){
				
				double co=cohesions.get(i);
				double old_co=co;
				co*=(cl.size()+1.0);
				double sim=strSim.sum_sim_el_with_group(el, cl);
				co-=2.0*sim;
				try{
					co-=strSim.getSim(el, el);
				}
				catch(Exception e){
					System.out.println("Exception bizarre from Clustering.addToCluster");
					throw new RuntimeException(e);
				}
				if (cl.size()>0){
					co/=cl.size();
				}
				else{co=0.0;}
				cohesions.set(i, co);
				cohesion*=k;
				cohesion+=co-old_co;
				if (k>0){
					cohesion/=k;
				}
				
			}
	}
	//retourne le cluster d'indice i (null si n'existe pas)
	public ArrayList<Integer> getCluster(int i){
		ArrayList<Integer> cl=clusters.get(i);
		return(cl);
	}
	// retourne l'ensemble de clusters
	public ArrayList<ArrayList<Integer>> getClusters(){
		return(clusters);
	}
	// retourne le numero de cluster de l'element numero el (-1 si non affecte)
	public int getIClusterOf(int el){
		int ret=-1;
		Integer num=iclusters.get(el);
		if (num!=null){
			ret=num;
		}
		return(ret);
	}
	// retourne le cluster de l'element numero el (null si non affecte)
	public ArrayList<Integer> getClusterOf(int el){
			int num=getIClusterOf(el);
			ArrayList<Integer> cl=null;
			if(num>=0){
				cl=clusters.get(num);
			}
			return(cl);
	}

	
	public String toString(){
		String s="Clustering : \n";
		for(int i=0;i<k;i++){
			ArrayList<Integer> cl=getCluster(i);
			s+="Cluster "+i+" : ";
			for(int j=0;j<cl.size();j++){
				s+=cl.get(j)+";";
			}
			if (cohesion_valid){
				s+=" Cohesion = "+cohesions.get(i);
			}
			s+="\n";
		}
		if (cohesion_valid){
			s+="Cohesion moyenne = "+cohesion+"\n";
		}
		return(s);
	}
	
	
	public void removeCluster(int i){
		if(cohesion_valid){
			double co=cohesion;
			double coi=cohesions.get(i);
			co=co*k;
			co-=coi;
			cohesion=0.0;
			if (k>1){
				cohesion=co/(k-1);
			}
			cohesions.remove(i);
		}
		k--;
		for(Integer el:clusters.get(i)){
			iclusters.remove(el);
		}
		clusters.remove(i);
	}
	public int getNbClusters(){
		return(k);
	}
	public double getClusterCohesion(int i){
		if (!cohesion_valid){
			getCohesion();
		}
		return(cohesions.get(i));
	}
	
	public double getBiasedCohesion(){
		double biased=0.0;
		for(int i=0;i<k;i++){
			double co=getClusterCohesion(i);
			int nb=getCluster(i).size();
			if (nb>0){
				co=co/nb;
			}
			biased+=co;
		}
		if (k>0){
			biased/=k;
		}
		return(biased);
	}
	
	
	// Retourne l'element le plus au centre du groupe (le + proche de tous les autres selon strSim)
	public Integer getMostCentralElOfCluster(int i){
		ArrayList<Integer> cluster=getCluster(i);
		double sim_max=0.0;
		int el_max=-1;
		for(Integer el:cluster){
			double sim=strSim.sum_sim_el_with_group(el, cluster);
			if ((el_max==-1) || (sim>sim_max)){
				sim_max=sim;
				el_max=el;
			}
		}
		return(el_max);
	}
	
	public HashMap<Integer,Double> getAverageInternalSimsOfGroup(int i){
		ArrayList<Integer> cluster=getCluster(i);
		HashMap<Integer,Double> sims=new HashMap<Integer,Double>();
		int s=cluster.size();
		for(Integer el:cluster){
			double sim=strSim.sum_sim_el_with_group(el, cluster);
			if (s>0){
				sim/=s;
			}
			sims.put(el,sim);
		}
		return(sims);
	}
	
	
	
	
	
}
