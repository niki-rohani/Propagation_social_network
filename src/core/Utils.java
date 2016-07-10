package core;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import experiments.Result;
public class Utils {
	public static double computeDot(HashMap<Integer,Double> v1,HashMap<Integer,Double> v2){
		HashMap<Integer,Double> a=v1;
		HashMap<Integer,Double> b=v2;
		if (v1.size()>v2.size()){
			a=v2;
			b=v1;
		}
		double dot=0.0;
		for(Integer i:a.keySet()){
			if (b.containsKey(i)){
				double pa=a.get(i);
				double pb=b.get(i);
				dot+=pa*pb;
			}
		}
		return(dot);
	}
	public static HashMap<Integer,Double> copy(HashMap<Integer,Double> vec){
		HashMap<Integer,Double> cvec=new HashMap<Integer,Double>();
		for(Integer i:vec.keySet()){
			cvec.put(i, vec.get(i));
		}
		return(cvec);
	}
	public static HashMap<Integer,Double>  multiplies(HashMap<Integer,Double> vec,double coef){
		HashMap<Integer,Double> cvec=new HashMap<Integer,Double>();
		for(Integer i:vec.keySet()){
			cvec.put(i, coef*vec.get(i));
		}
		return(cvec);
	}
	
	/**
	 * return ||v||^2
	 */
	public static double getNormSquared(HashMap<Integer,Double> vec){
		double sum=0;
		for(Integer i:vec.keySet()){
			double v=vec.get(i);
			sum+=v*v;
		}
		return(sum);
	}
	
	public static  HashMap<Integer,Double> add( HashMap<Integer,Double> v1,  HashMap<Integer,Double> v2){
		return(add(v1,v2,1.0));
	}
	
	// return v1 + v2 * coef
	public static HashMap<Integer,Double> add(HashMap<Integer,Double> v1,  HashMap<Integer,Double> v2, double coef){
		HashMap<Integer,Double> cvec=new HashMap<Integer,Double>();
		for(Integer i:v1.keySet()){
			cvec.put(i, v1.get(i));
		}
		for(Integer i:v2.keySet()){
			double v=0.0;
			if (cvec.containsKey(i)){
				v=cvec.get(i);
			}
			cvec.put(i, v+(coef*v2.get(i)));
		}
		return(cvec);
	}
	
	public static void serializeThis(Object obj,String filename) throws IOException{
			FileOutputStream fos = new FileOutputStream(filename);
			// creation d'un "flux objet" avec le flux fichier
			ObjectOutputStream oos= new ObjectOutputStream(fos);
			try {
				// serialisation : ecriture de l'objet dans le flux de sortie
				oos.writeObject(obj); 
				
				// on vide le tampon
				oos.flush();
				System.out.println("Objet "+obj.toString()+" serialise");
			} finally {
				//fermeture des flux
				try {
					oos.close();
				} finally {
					fos.close();
				}
			}
		
	}
	
	public static Object deserialize(String fileName) throws IOException{
		Object ret=null;
		try{
			
			// ouverture d'un flux d'entree depuis le fichier "personne.serial"
			FileInputStream fis = new FileInputStream(fileName);
			// creation d'un "flux objet" avec le flux fichier
			ObjectInputStream ois= new ObjectInputStream(fis);
			try {	
				// deserialisation : lecture de l'objet depuis le flux d'entree
				ret = ois.readObject(); 
			} finally {
				// on ferme les flux
				try {
					ois.close();
				} finally {
					fis.close();
				}
			}
		}
		catch(ClassNotFoundException e){
			e.printStackTrace();
			throw new IOException("Probleme classe");
		}
		return(ret);
	}
	
}
