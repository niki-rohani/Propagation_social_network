package indexation;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import actionsBD.MongoDB;
import core.Post;
import core.User;

import wordsTreatment.Stemmer;
import wordsTreatment.TF_Weighter;
import wordsTreatment.WeightComputer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class TwitterStreamIndexer extends DataIndexer {

	/*public TwitterStreamIndexer(WeightComputer weightComputer){
		super(weightComputer);
	}*/
	
	@Override
	public String indexData(String db, String filename, WeightComputer weightComputer,TextTransformer trans) throws IOException {
		String collection=MongoDB.mongoDB.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
		DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss +0000 yyyy",Locale.US);
		
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
			while(((ligne=lecteur.readLine())!=null)){ // && (nb<10000)){
		        //System.out.println(ligne); 
				try {
					JSONObject o = (JSONObject) new JSONParser().parse(ligne) ;
					JSONObject u = (JSONObject) o.get("user") ;
					
					User user;
					String name=""+u.get("screen_name");
					if (users.containsKey(name)){
						user=users.get(name);
					}
					else{
						user=new User(name);
						users.put(name,user);
						//ret.add(user);
						
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
					System.out.println(name+"\t"+timestamp+"\t"+text);
					// recuperation des id de stems + eventuellement idf
					HashMap<Integer,Double> poids=trans.transform(weightComputer.getWeightsForIds(text));
					//System.out.println(" weights computed");
					if (poids.size()>0){
						Post p=new Post(text,user,timestamp,poids);
						JSONObject ent = (JSONObject) o.get("entities") ;
						JSONArray tags=(JSONArray)ent.get("hashtags");
						for(Object obj:tags){
							String tag=((JSONObject)obj).get("text").toString();
							tag=tag.toLowerCase();
							p.addTag(tag);
						}
						//posts.add(p);
						//if (postTagger!=null){
						//	postTagger.tagPost(p);
						//}
						p.indexInto(db,collection);
						user.reinitPosts();
						Post.reinitPosts();
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
				if (nb%100==0){
					System.out.println(nb+"/"+nbl+" lignes traitees");
				}
			}
			System.out.println(nbinvalides+" lignes invalides");
			System.out.println("Creation indexs");
			DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
			col.ensureIndex(new BasicDBObject("tags.tag", 1));
			col.ensureIndex(new BasicDBObject("id", 1));
			col.ensureIndex(new BasicDBObject("timestamp", 1));
			
		}
		finally{
			lecteur.close();
		}
		
		return collection;
		

	}
	
	public String indexStems(String db,String filename) throws IOException {
		String collection=MongoDB.mongoDB.createCollection(db,"stems"," stems from "+filename);
		
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
					
					User user;
					
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
			DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
			
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
		String s=this.getClass()+""; //+"_"+this.weightComputer.toString();
		return s;
	}
	public static void main(String[] args){
		try{
			System.out.println("Indexation "+args[0]+" > "+args[1]);
			
			DataIndexer indexer=new TwitterStreamIndexer(); 
			//indexer.indexData(args[1],args[0],new TF_Weighter()); //);
			
		}
		catch(Exception e){
			//System.out.println(e);
			e.printStackTrace();
		}
	}

}
