package thibault.indexBertin;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.twitter.Extractor;

import actionsBD.MongoDB;
import core.Post;
import core.User;
import thibault.dynamicCollect.Arm;
import thibault.dynamicCollect.NLP;
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
import java.util.List;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class indexTweetLip6WithFeatures extends DataIndexer {

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
					int iRT=0; //1 si l utilisateur retweet (pour I retweet)
					int iAmRT=0; //1 si l utilisateur est retweeted (pour i am retweeted)
					int iRTo=0; //1 si l utilisateur reply (pour I reply)
					int iAmRTo=0; //1 si on repond a l utilisateur (pour a i am replied to)
					int followersNb=0;
					int statsNb=0;
					int friendsNb=0;
					int favouritesNb=0;
					double sentiment=0.5;
					
					BasicDBObject b=null;
					
					String name=""+u.get("screen_name");
					
					
					if(userStreamed.contains(name)){
						if(o.get("in_reply_to_screen_name")!=null){
							iRTo=1;
							}
						else if(o.get("retweeted_status")!=null){
							iRT=1;
						}
						followersNb=Integer.parseInt(u.get("followers_count").toString());
						statsNb=Integer.parseInt(u.get("statuses_count").toString());
						friendsNb=Integer.parseInt(u.get("friends_count").toString());
						favouritesNb=Integer.parseInt(u.get("favourites_count").toString());
					}
					
					else{
						if(o.get("in_reply_to_screen_name")!=null){
							iAmRTo=1;
							name=""+o.get("in_reply_to_screen_name");
						}
					
					else if(o.get("retweeted_status")!=null){
						JSONObject oBis=(JSONObject) o.get("retweeted_status");
						JSONObject uBis = (JSONObject) oBis.get("user") ;
						String textRef=oBis.get("text").toString();
						name=""+uBis.get("screen_name");
						iAmRT=1;
						followersNb=Integer.parseInt(uBis.get("followers_count").toString());
						statsNb=Integer.parseInt(uBis.get("statuses_count").toString());
						friendsNb=Integer.parseInt(uBis.get("friends_count").toString());
						favouritesNb=Integer.parseInt(uBis.get("favourites_count").toString());
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
					
					
					String cleanedText = text;
					List<String> words;
					Extractor extractor = new Extractor();
					words = extractor.extractMentionedScreennames(cleanedText);
					for(String w:words){
						cleanedText=cleanedText.replace("@"+w, "");
					}
					words = extractor.extractHashtags(cleanedText);
					for(String w:words){
						cleanedText=cleanedText.replace("#"+w, "");
					}
					words = extractor.extractURLs(cleanedText);
					for(String w:words){
						cleanedText=cleanedText.replace(w, "");
					}
					cleanedText=cleanedText.replace("rt : ", "");
					cleanedText=cleanedText.replace("...", "");
					sentiment = NLP.findSentiment(cleanedText)*1.0/4;
					
					b =new BasicDBObject("iRT",iRT);
					b.append("iRTo", iRTo);
					b.append("iAmRTo", iAmRTo);
					b.append("iAmRT", iAmRT);
					b.append("followers_count", followersNb);
					b.append("statuses_count", statsNb);
					b.append("friends_count", friendsNb);
					b.append("favourites_count", favouritesNb);
					b.append("sentiment", sentiment);
					b.append("timePeriod", this.computeTimePeriodObj(date.getHours()));

					
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
	
	public BasicDBObject computeTimePeriodObj(int hour){
		BasicDBObject c = new BasicDBObject();
		for(int i=0;i<12;i++){
			if(2*i<hour && hour<2*i+2){
				c.append("period_"+i, i);
			}
			else{
				c.append("period_"+i, 0);
			}
			
		}
		return c;
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
			NLP.init();
			String db="dbUS";
			String filename="/data/lip6/db/baseLIP6usElections/streamTestUSA_2";
			//String filename="/local/gisselbrecht/streamTestUSA_2";
			String postsCol;
			String stemsCol;
			indexTweetLip6WithFeatures indexer=new indexTweetLip6WithFeatures(); 
			
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
