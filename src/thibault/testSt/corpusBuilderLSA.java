package thibault.testSt;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.decomposition.*;

import thibault.dynamicCollect.Arm;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import core.Post;

public class corpusBuilderLSA {
	
	public Matrix X, Xk, U, Uk, V, Vk, D, Dk;
	public int nbWords,nbDocs;
	public String dbName, colName, fileName;
	public ArrayList<Post> corpus;
	
	
	public corpusBuilderLSA(int nbWords,int nbDocs, String dbName,String  colName, String fileName){
		this.nbDocs=nbDocs;
		this.nbWords=nbWords;
		this.dbName=dbName;
		this.colName=colName;
		this.fileName=fileName;
		this.corpus= new ArrayList<Post>();	}
	

	public void buildCorpus() throws UnknownHostException{
		//on va streamer la base de poste mongo et mettre les post dans une liste. on prend nbDocs tweet au hasard dans la base
		Mongo mongo =new Mongo("localhost");
		DB db = mongo.getDB(dbName);
		//DB db = mongo.getDB( "bertin1" );
		//DB db = mongo.getDB( "dbLudo" );
		DBCollection coll = db.getCollection(colName);
		long N = coll.count();
		
		for(int i=0;i<nbDocs;i++){
			//DBObject c =coll.find().skip(i).next();
			DBObject c = coll.find().limit(1).skip((int)Math.floor(Math.random()*N)).next();
			Post p=Post.getPostFrom(c);
			if (p!=null){
				corpus.add(p);
				System.out.println(p.getWeights());
			}	
		}
	}
	
	public void buidMatrixOccurence(){//construction de la matrice a partir des post
		X= new Basic2DMatrix(new double[nbWords][nbDocs]);
		for (int j = 0; j<nbDocs;j++){
			for (int i=0;i<nbWords;i++){
				X.set(i, j,0);
			}
		}
		for (int j = 0; j<nbDocs;j++){
			for (Integer i : corpus.get(j).getWeights().keySet()){
				X.set(i-1, j, corpus.get(j).getWeights().get(i));
			}
		}
	}
	
	public void decomposeSVD(){//decomposition SVD
		SingularValueDecompositor s = new SingularValueDecompositor(X);
		Matrix[] m=s.decompose();
		U=m[0];
		D=m[1];
		V=m[2];
	}
	
	public void truncSVD(int nbDimencsions){//prend les premier concepts avec les valeur propre les plus eleve
		int[] index = new int[nbDimencsions];
		/*HashMap<Integer, Double>	eingenValues = new HashMap<Integer, Double>(); //pour classer les valeur propre et recuperer les indices 
		for(int i=0;i<D.rows();i++){
			eingenValues.put(i, D.get(i, i));
		}
		Map<Integer, Double> map = sortByValues(eingenValues);
		Set set = map.entrySet();
	      Iterator iterator = set.iterator();
	      int i=0;
	      while(iterator.hasNext() & i<nbDimencsions) {
	           Map.Entry me = (Map.Entry)iterator.next();
	           index[i]=(Integer) me.getKey();
	           i++;
	      }*/
		for(int i=0;i<nbDimencsions;i++){
			index[i]=i;
		}
	      
		
		  Dk=D.slice(0, 0,nbDimencsions, nbDimencsions);
		  Uk=U.slice(0, 0, U.rows() ,nbDimencsions);
		  Vk=V.slice(0, 0, V.rows(),nbDimencsions);
		  /*
	      Dk=D.select(index, index);
	      Uk=U.select(index, index);
	      Vk=V.select(index, index);*/
	      
	}
	
	/*private static HashMap sortByValues(HashMap map) { 
	       List list = new LinkedList(map.entrySet());
	       Collections.sort(list, new Comparator() {
	            public int compare(Object o1, Object o2) {
	               return ((Comparable) ((Map.Entry) (o2)).getValue())
	                  .compareTo(((Map.Entry) (o1)).getValue());
	            }
	       });

	       HashMap sortedHashMap = new LinkedHashMap();
	       for (Iterator it = list.iterator(); it.hasNext();) {
	              Map.Entry entry = (Map.Entry) it.next();
	              sortedHashMap.put(entry.getKey(), entry.getValue());
	       } 
	       return sortedHashMap;
	  }*/
	
	public void writeToFile() throws IOException{
		 FileWriter fw=null;
		try {
			fw = new FileWriter(fileName+"U");
		} catch (IOException e) {
			e.printStackTrace();
		}
		 BufferedWriter out = new BufferedWriter(fw);
		 
			for (int i = 0;i<U.rows() ;i++){
				//for (int j = 0;j<Uk.columns();i++){
					//System.out.println(Uk.getRow(i).toString().replace("[", "").replace("]", "").replace(", ", " "));
					 //out.write(Uk.getRow(i).toString().replace("[", "").replace("]", "").replace(", ", " ")+"\n");
					 out.write(U.getRow(i).toString().replace(", ", " ")+"\n");
				//}
			}
			out.close();
			
			try {
				fw = new FileWriter(fileName+"D");
			} catch (IOException e) {
				e.printStackTrace();
			}
			 	out = new BufferedWriter(fw);
			 
				for (int i = 0;i<D.rows() ;i++){
					//for (int j = 0;j<Uk.columns();i++){
						 //out.write(Dk.getRow(i).toString().replace("[", "").replace("]", "").replace(", ", " ")+"\n");
						 out.write(D.getRow(i).toString().replace(", ", " ")+"\n");
					//}
				}
				out.close();
				
				try {
					fw = new FileWriter(fileName+"V");
				} catch (IOException e) {
					e.printStackTrace();
				}
				 out = new BufferedWriter(fw);
				 
					for (int i = 0;i<V.transpose().rows() ;i++){
						//for (int j = 0;j<Uk.columns();i++){
							// out.write(Vk.transpose().getRow(i).toString().replace("[", "").replace("]", "").replace(", ", " ")+"\n");
							 out.write(V.getRow(i).toString().replace(", ", " ")+"\n");
						//}
					}
				out.close();
	}
	public static void main(String[] args) throws IOException {	
		
		/*corpusBuilderLSA C = new corpusBuilderLSA(5, 20, "usElections5000_hashtag","posts_1","");
		Matrix M= new Basic2DMatrix(new double[C.nbWords][C.nbDocs]);
		for (int j = 0; j<C.nbDocs;j++){
			for (int i=0;i<C.nbWords;i++){
				M.set(i, j,Math.random());
			}
		}

		C.X=M;
		//System.out.println(C.X);
		C.decomposeSVD();
		System.out.println(C.U);
		System.out.println(C.D);
		System.out.println(C.V);
		C.truncSVD(5);
		//System.out.println(C.Uk);
		//System.out.println(C.Dk);
		//System.out.println(C.Vk.transpose());
		C.writeToFile();*/
		
		corpusBuilderLSA C = new corpusBuilderLSA(2000, 1000, "usElections5000_hashtag","posts_1","lsa");
		C.buildCorpus();
		C.buidMatrixOccurence();
		//System.out.println(C.X);
		
		C.decomposeSVD();
		C.truncSVD(50);
		
		//System.out.println(C.Uk);
		//System.out.println(C.Dk);
		//System.out.println(C.Vk.transpose());
		
		C.writeToFile();
		
		
		/*InputStream ips=null;
		try {
			ips = new FileInputStream("Dk");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String ligne;
		Basic2DMatrix UkBis = new Basic2DMatrix(new double[5][5]);
		int i = 0;
		while ((ligne=br.readLine())!=null){
			ligne=ligne.replace(",",".");
			ligne=ligne.replace("[ ","[");
			ligne=ligne.replace(" ]","]");
			ligne=ligne.replace("[","");
			ligne=ligne.replace("]","");
			ligne=ligne.replace("  "," ");
			String[] values=ligne.split(" ");
			System.out.println(ligne);
			for(int j=0;j<values.length;j++){
				System.out.println(values[j]);
			}
			//System.out.println(values.length);
			for(int j=0;j<values.length;j++){
				UkBis.set(i, j, new Double(values[j]));	
			}
			i++;
		}
		
		System.out.println(UkBis);*/
		
	}

}
