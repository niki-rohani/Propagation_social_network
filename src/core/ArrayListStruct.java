package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;

// C'est un ArrayList "Serializable", avec des fonction de serialize/deserialize.
public class ArrayListStruct<T> extends ArrayList<T> implements Structure {
	public static final long serialVersionUID=1;
	public ArrayListStruct(){
		super();
	}
	public ArrayListStruct(Collection<T> col){
		super(col);
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
	public static ArrayListStruct deserialize(String fileName) throws IOException{
		//Class c=Class.forName(typeT);
		ArrayListStruct ret=null;
		try{
		
			// ouverture d'un flux d'entree depuis le fichier "personne.serial"
			FileInputStream fis = new FileInputStream(fileName);
			// creation d'un "flux objet" avec le flux fichier
			ObjectInputStream ois= new ObjectInputStream(fis);
			try {	
				// deserialisation : lecture de l'objet depuis le flux d'entree
				ret = (ArrayListStruct) ois.readObject();
				
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
