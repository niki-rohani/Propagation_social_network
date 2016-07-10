package thibault.indexBertin;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import actionsBD.MongoDB;
import core.Post;
import core.User;
import thibault.dynamicCollect.Arm;
import thibault.dynamicCollect.UserArm;
import wordsTreatment.Stemmer;
import wordsTreatment.TF_Weighter;
import wordsTreatment.WeightComputer;
import indexation.DataIndexer;
import indexation.TextTransformer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class indexationDataLudo extends DataIndexer {

	/*public TwitterStreamIndexer(WeightComputer weightComputer){
		super(weightComputer);
	}*/
	

	public String indexDataBis(String db, String filename,  TF_WeighterThib weightComputer, TextTransformerThib trans) throws IOException {
		
		MongoDB m = new MongoDB("localhost");
		
		String collection ;
		collection= m.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);

		DateFormat df = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
		
		System.out.println("Indexation "+filename);
		//ArrayListStruct<User> ret=new ArrayListStruct<User>();
		HashMap<String,User> users=new HashMap<String,User>();
		HashSet<Post> posts=new HashSet<Post>();
		//Stemmer stemmer=new Stemmer();
		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		
		
		try{
			String ligne;
			int nbl=0;
			while((ligne=lecteur.readLine())!=null){
				nbl++;
			}
			System.out.println(nbl+" lignes");
			lecteur.close();
			lecteur=new BufferedReader(new FileReader(filename));
			int nb=0;
			int nbinvalides=0;
			while(((ligne=lecteur.readLine())!=null)){

				try {
					
					String str[] = ligne.split("\t");

					User user ;
					
					
					String name=str[0];

							if (users.containsKey(name)){
								user=users.get(name);
							}
							else{
								user=new User(name);
								users.put(name,user);
							}
						
						
					String time=str[1];
					Date date=df.parse(time);
					Long timestamp=date.getTime()/1000;
					
					String text=str[2];
					text=text.toLowerCase();
					text=text.replaceAll("\r", " ");
					text=text.replaceAll("\\r", " ");
					text=text.replaceAll("\\\\r", " ");
					text=text.replaceAll("\n", " ");
					text=text.replaceAll("\\n", " ");
					text=text.replaceAll("\\\\n", " ");
					text=text.replaceAll("\t", " ");
					text=text.replaceAll("\\t", " ");
					text=text.replaceAll("\\\\t", " ");
					text=text.replaceAll("  ", " ");
					text=text.replace("^ ", "");
					//System.out.println(name+"\t"+timestamp+"\t"+text);
					// recuperation des id de stems + eventuellement idf
					
					
					HashMap<Integer,Double> poids=trans.transformBis(m,weightComputer.getWeightsForIds(m,text));
					
					//double nbRT=0;

					 //if(o.get("retweeted").toString()=="true"){
						// double nbRT=Double.parseDouble(o.get("retweet_count").toString());
					 //}
					
					//System.out.println(" weights computed");
					if (poids.size()>0){
						PostThib p=new PostThib(text,user,timestamp,poids,null);
						p.indexIntoBis(m,db,collection);
						user.reinitPosts();
						Post.reinitPosts();
						PostThib.reinitPostThibs();
					}
				
					
				} 
				catch(Exception e){
					System.out.println("ligne invalide :" + ligne);
					nbinvalides++;
				}
				/*catch (java.text.ParseException e) {
					System.out.println("Date parse exception :"+e);
					System.out.println("ligne invalide :" + ligne);
					nbinvalides++;
					break;
				}
				catch(ParseException e){
					System.out.println("JSON parse exception "+e);
					System.out.println("ligne invalide :" + ligne);
					nbinvalides++;
					break;
				}*/
				nb++;
				if (nb%10000==0){
					System.out.println(nb+"/"+nbl+" lignes traitees");
				}
			}
			System.out.println(nbinvalides+" lignes invalides");
			System.out.println("Creation indexs");
			
			DBCollection col=m.getCollectionFromDB(db,collection);
			col.ensureIndex(new BasicDBObject("id", 1));
			col.ensureIndex(new BasicDBObject("timestamp", 1));

			
		}
		finally{
			lecteur.close();
		}
		
		return collection;
		

	}
	
	public String indexStems(String db,String filename) throws IOException {
		MongoDB m = new MongoDB("localhost");

		String collection ;
		collection=m.createCollection(db,"stems"," stems from "+filename);
		
		System.out.println("Indexation "+filename);
		//ArrayListStruct<User> ret=new ArrayListStruct<User>();
		HashMap<String,Integer> stems=new HashMap<String,Integer>();
		
		Stemmer stemmer=new Stemmer();
		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		try{
			String ligne;
			int nbl=0;
			while((ligne=lecteur.readLine())!=null){
				nbl++;
			}
			System.out.println(nbl+" lignes");
			lecteur.close();
			lecteur=new BufferedReader(new FileReader(filename));
			int nb=0;
			int nbinvalides=0;
			while(((ligne=lecteur.readLine())!=null && nb<2000000)){

				try {
					
					String str[] = ligne.split("\t");
					
					
					
					String text=str[2];
					text=text.toLowerCase();
					text=text.replaceAll("\r", " ");
					text=text.replaceAll("\\r", " ");
					text=text.replaceAll("\\\\r", " ");
					text=text.replaceAll("\n", " ");
					text=text.replaceAll("\\n", " ");
					text=text.replaceAll("\\\\n", " ");
					text=text.replaceAll("\t", " ");
					text=text.replaceAll("\\t", " ");
					text=text.replaceAll("\\\\t", " ");
					text=text.replaceAll("  ", " ");
					text=text.replace("^ ", "");
					
					HashMap<String,Integer> w=stemmer.porterStemmerHash(text.toString());
					w.remove(" * ");
					for(String s:w.keySet()){
						Integer nbp=stems.get(s);
						nbp=(nbp==null)?0:nbp;
						stems.put(s, nbp+1);
					}
					
					
				} 
				catch(Exception e){
					System.out.println("ligne invalide :" + ligne);
					nbinvalides++;
				}
				
				nb++;
				if (nb%1000==0){
					System.out.println(nb+"/"+nbl+" lignes traitees");
				}
			}
			System.out.println(nbinvalides+" lignes invalides");
			int nbPosts=nb-nbinvalides;
			int i=0;
			
			DBCollection col=m.getCollectionFromDB(db,collection);
			
			for(String stem:stems.keySet()){
				int nbp=stems.get(stem);
				double idf=Math.log((1.0*nbPosts)/(1.0*nbp));
				i++;
				
				BasicDBObject obj = new BasicDBObject();
				obj.put("id", i);
				obj.put("stem",stem);
				obj.put("idf", idf);
				col.insert(obj);
			}
			System.out.println("nb posts = "+nbPosts);
			System.out.println("Creation indexs");
			//DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
			col.ensureIndex(new BasicDBObject("stem", 1));
			col.ensureIndex(new BasicDBObject("id", 1));
		}
		finally{
			lecteur.close();
		}
		
		return collection;
	}

	@Override
	public String toString() {
		String s=this.getClass()+"";
		return s;
	}
	@Override
	public String indexData(String db, String filename,
			WeightComputer weightComputer, TextTransformer trans)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}


	public static void main(String[] args){
		try{
			String db="dbLudo";
			String filename="/data/lip6/db/baseLIP6mdechoud/twitter-released-dataset/allTweets.sql";
			//String filename="/local/gisselbrecht/streamTestUSA_2";
			String postsCol;
			String stemsCol;
			indexationDataLudo indexer=new indexationDataLudo(); 
			
			//on recupere les stems
			stemsCol =indexer.indexStems(db, filename);
			//on les range
			TextTransformerThib trans=new IDFPrunerThib(2000,db,stemsCol);
			trans.learn();
			//on index les posts
			postsCol=indexer.indexDataBis(db, filename, new TF_WeighterThib(db,stemsCol), trans);
			
			
			
		}
		catch(Exception e){
			//System.out.println(e);
			e.printStackTrace();
		}
	}
}
