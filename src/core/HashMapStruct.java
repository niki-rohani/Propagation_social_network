package core;
import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;

public class HashMapStruct<K,V> extends HashMap<K,V> implements Structure {
	public static final long serialVersionUID=1;
	public HashMapStruct(){
		super();
	}
	public HashMapStruct(HashMap<K,V> map){
		super(map);
	}
	public void serialize(String filename) throws IOException{
		
		File f=new File(filename);
	    File p=f.getParentFile();
	    if (!p.exists()){
	    	p.mkdirs();
	    }
		//FileOutputStream fos = new FileOutputStream(rep+"/"+toString()+".result");
		FileOutputStream fos = new FileOutputStream(filename);
		// creation d'un "flux objet" avec le flux fichier
		ObjectOutputStream oos= new ObjectOutputStream(fos);
		try {
			// serialisation : ecriture de l'objet dans le flux de sortie
			oos.writeObject(this); 
			
			// on vide le tampon
			oos.flush();
			System.out.println("serialization ok");
		} finally {
			//fermeture des flux
			try {
				oos.close();
			} finally {
				fos.close();
			}
		}
	
	}
	public static HashMapStruct deserialize(String fileName) throws IOException{
		//Class c=Class.forName(typeT);
		HashMapStruct ret=null;
		try{
		
			FileInputStream fis = new FileInputStream(fileName);
			ObjectInputStream ois= new ObjectInputStream(fis);
			try {	
				// deserialisation : lecture de l'objet depuis le flux d'entree
				ret = (HashMapStruct) ois.readObject();
				
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
