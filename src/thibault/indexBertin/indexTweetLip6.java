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

public class indexTweetLip6 extends DataIndexer {

	/*public TwitterStreamIndexer(WeightComputer weightComputer){
		super(weightComputer);
	}*/
	

	public String indexDataBis(String db, String filename,  TF_WeighterThib weightComputer, TextTransformerThib trans) throws IOException {
		
		MongoDB m = new MongoDB("localhost");
		
		String collection ;
		collection= m.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
	
		DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss +0000 yyyy",Locale.US);
		
		System.out.println("Indexation "+filename);
		//ArrayListStruct<User> ret=new ArrayListStruct<User>();
		HashMap<String,User> users=new HashMap<String,User>();
		HashSet<Post> posts=new HashSet<Post>();
		//Stemmer stemmer=new Stemmer();
		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		
		
		//Ici on charge la liste des users qu on a streamer  a la base
		HashSet<String> userStreamed = new HashSet<String>();
		InputStream ips=null;
		try {
			ips = new FileInputStream("./src/thibault/indexBertin/StreamedUsersLip6USA");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String userName;
		try {
			while ((userName=br.readLine())!=null){
				userStreamed.add(userName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//
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
			while(((ligne=lecteur.readLine())!=null)){ // && (nb<10000)){
		        //System.out.println(ligne); 
				try {
					JSONObject o = (JSONObject) new JSONParser().parse(ligne) ;
					JSONObject u = (JSONObject) o.get("user") ;
					
					User user ;
					
					
					String name=""+u.get("screen_name");
					BasicDBObject b =new BasicDBObject("RT",0).append("RTo", 0);

					if(!userStreamed.contains(name)){
						
					if(o.get("in_reply_to_screen_name")!=null){
							b.put("RTo", 1);
							name=""+o.get("in_reply_to_screen_name");
						}
					
					else if(o.get("retweeted_status")!=null){
						JSONObject oBis=(JSONObject) o.get("retweeted_status");
						JSONObject uBis = (JSONObject) oBis.get("user") ;
						b.put("RT", 1);
						name=""+uBis.get("screen_name");
						}
					}

					if(userStreamed.contains(name)){
							if (users.containsKey(name)){
								user=users.get(name);
							}
							else{
								user=new User(name);
								users.put(name,user);
							}
						
						
					String time=o.get("created_at").toString();
					Date date=df.parse(time);
					Long timestamp=date.getTime()/1000;
					String text=o.get("text").toString();
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
						PostThib p=new PostThib(text,user,timestamp,poids,b);
						p.indexIntoBis(m,db,collection);
						user.reinitPosts();
						Post.reinitPosts();
						PostThib.reinitPostThibs();
					}
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
			while(((ligne=lecteur.readLine())!=null)){ // && (nb<10000)){
		        //System.out.println(ligne); 
				try {
					JSONObject o = (JSONObject) new JSONParser().parse(ligne) ;
					
					String text=o.get("text").toString();
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
				if (nb%100==0){
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
			String db="usElectionTestComplete";
			//String filename="/data/lip6/db/streamTestUSA_2";
			String filename="/local/gisselbrecht/streamTestUSA_2";
			String postsCol;
			String stemsCol;
			indexTweetLip6 indexer=new indexTweetLip6(); 
			
			//on recupere les stems
			stemsCol =indexer.indexStems(db, filename);
			//on les range
			TextTransformerThib trans=new IDFPrunerThib(2000,db,stemsCol);
			trans.learn();
			//on index les posts
			postsCol=indexer.indexDataBis(db, filename, new TF_WeighterThib(db,stemsCol), trans);
			//on les tags
			TagByPrefixThib strTag=new TagByPrefixThib("#");
		    postsCol=strTag.tagCollection(db,postsCol);
			
			
			
		}
		catch(Exception e){
			//System.out.println(e);
			e.printStackTrace();
		}
	}
}
