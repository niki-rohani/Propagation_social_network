package thibault.dynamicCollect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;

import com.mongodb.Mongo;
import com.mongodb.WriteResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;

public class TestMongoPourConnaitre {
	public static void main(String[] args) throws IOException {
		Mongo mongo =new Mongo("127.0.0.1");
		//Mongo mongo = new Mongo( "local" );
		DB db = mongo.getDB( "usElections5000_hashtag" );
		//Mongo mongo = new Mongo( "132.227.201.134" );
		//DB db = mongo.getDB( "usElections5000_hashtag" );
		DBCollection coll = db.getCollection("stems_2");

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
		
        // Fait une collection a partir d'un json
	/*String fichier ="posts_1.json";
		
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
		


System.out.println(coll.findOne());

		
//BasicDBObject query = new BasicDBObject("owner" , "HenryQs");
/*DBCursor cursor = coll.find();
try {
   while(cursor.hasNext()) {
       System.out.println(cursor.next());
   }
} finally {
   cursor.close();
}*/
		/*DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String time="2014-03-10T12:50:16.717Z";
		
		Date date = null;
		try {
			date=df.parse(time);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Long timestamp=date.getTime()/1000;
		System.out.println(timestamp);*/
		


	}
	


}

