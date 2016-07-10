package thibault.testSt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;









import jgibblda.Estimator;
import jgibblda.Inferencer;
import jgibblda.LDACmdOption;
import jgibblda.Model;

import org.la4j.vector.dense.BasicVector;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import core.Post;
import core.User;

public class ldaModelBuilder {

	public static void main(String[] args) throws IOException {
		String dbName="bertin";
		String colName= "posts_1";
		String folderName = "./src/thibault/";
		String fileName="tweetFile"+dbName+colName;
	
		
		//Partie pour ecrire les tweet dans un fichier txt. Tweet deja au format nombre et poids
		Mongo mongo =new Mongo("localhost");
		DB db = mongo.getDB(dbName);
		DBCollection coll = db.getCollection(colName);
		FileWriter fw=null;
		fw = new FileWriter(folderName+fileName);
		BufferedWriter out = new BufferedWriter(fw);
		long N = coll.count();
		System.out.println("Nombre de doc "+N);
		out.write(N+"\n");
		DBCursor cursor = coll.find();
		while(cursor.hasNext()) {
			DBObject res=cursor.next();
			Post p=Post.getPostFrom(res);
			if (p!=null){
				String ligne="";
				for(Integer w: p.getWeights().keySet()){
					ligne=w+" "+ligne;
				}
				out.write(ligne+"\n");
			}
				Post.reinitPosts();
				User.reinitUsers();
		}
		out.close();
		
		
		System.out.println("Learning LDA model on :"+"tweetFile"+dbName+colName);
		LDACmdOption ldaOption = new LDACmdOption();
		ldaOption.est=true;
		ldaOption.estc=false;
		ldaOption.inf = false;
		ldaOption.dir = folderName;
		ldaOption.niters = 1000; 
		ldaOption.savestep=1000;
		ldaOption.K=30;
		ldaOption.dfile=fileName;
		Estimator estimator = new Estimator();
		estimator.init(ldaOption);
		estimator.estimate();
		 
		
		
		/*LDACmdOption ldaOption = new LDACmdOption();
		ldaOption.est=false;
		ldaOption.estc=false;
		ldaOption.inf = true;
		ldaOption.dir = "./src/thibault/";
		ldaOption.niters = 20; 
		ldaOption.modelName = "nomdemonmodel";
		//ldaOption.dfile="huss"; //rien je veux pas saver mais juste choper les umero des phi et tout
		Inferencer inferencer = new Inferencer();
		inferencer.init(ldaOption);
		
		String[] test = "";
		for (Post p:mines){	
			for (int w:p.getWeights().keySet()){
				test=w+" "+test;
			}
		}
		Model newModel = inferencer.inference(test); 
		features=new BasicVector(newModel.theta[0][]);*/
		//mon vecteur de features est donc p(topic sanchant doc) donc theta (0)(j) j allant de 0 a nbTopic-1
		 

	}

}
