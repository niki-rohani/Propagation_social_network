package thibault.dynamicCollect;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;

public class extractRandomRewardFromTweet {

	public static void main(String[] args) throws IOException {
		Mongo mongo =new Mongo("localhost");
		DB db = mongo.getDB( "usElections5000_hashtag" );
		//DB db = mongo.getDB( "bertin1" );
		//DB db = mongo.getDB( "dbLudo" );
		DBCollection coll = db.getCollection("posts_1");
		int nbRewards = 5;
		long N = coll.count();
		
		for(int i=0;i<nbRewards;i++){
			DBObject c = coll.find().limit(1).skip((int)Math.floor(Math.random()*N)).next();
			//System.out.println(c);
			//System.out.println(c.get("weights"));
			String poids = c.get("weights").toString();
			poids=poids.replace("[", "");
			poids=poids.replace("]", "");
			poids=poids.replace("{", "(");
			poids=poids.replace("}", ")");
			poids=poids.replace('"', ' ');
			poids=poids.replace(",", "");
			poids=poids.replace(":", ",");
			poids=poids.replace(" ", "");
			poids=poids.replace('"', ' ');
			poids=poids.replace("(3,1.0)", "");
			poids=poids.replace("(2,1.0)", "");
			poids=poids.replace("(1,1.0)", "");
			poids=poids.replace(")", ")\t");
			System.out.println(poids);

		}
		
		//System.out.println(N);

		//coll.drop();
		//Creer un json a partir d'une collection
	/*String fichier = "stems.json";
		 DBCursor cursor1 = coll.find();
		 FileWriter fw = new FileWriter(fichier);
		 BufferedWriter out = new BufferedWriter(fw);
		 try {
		    while(cursor1.hasNext()) {
		        out.write(cursor1.next().toString()+"\n");
		    }
		 } finally {
		    cursor1.close();
		 }
	out.close();*/
		
    /*Fait une collection a partir d'un json
	String fichier ="posts_1.json";
		
		//lecture du fichier texte	

			InputStream ips=new FileInputStream("C:/Users/thibault.gisselbrech/Desktop/stems_2.json"); 
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String ligne;
			while ((ligne=br.readLine())!=null){
				System.out.println(ligne);
				DBObject dbObject = (DBObject) JSON.parse(ligne);
				coll.insert(dbObject);
			}
			br.close();*/
		





		


	}
	

}
