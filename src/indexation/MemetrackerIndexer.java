package indexation;

import java.io.BufferedReader;
import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


import core.Post;
import core.User;

import wordsTreatment.Stemmer;
import wordsTreatment.WeightComputer;
import java.util.ArrayList;
//import org.json.simple.parser.ParseException;
import java.text.ParseException;
import java.util.Locale;

/**
 * 
 * @author sylvain lamprier
 *
 * Indexes the mail corpus memetracker from http://snap.stanford.edu/data/memetracker9.html.
 *
 */
public class MemetrackerIndexer extends DataIndexer {

	
	@Override
	public String indexData(String db,String filename, WeightComputer weightComputer,TextTransformer trans) throws IOException {
		String collection=MongoDB.mongoDB.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
		File rep=new File(filename);
		if(!rep.isDirectory()){
			throw new RuntimeException(filename+" is not a directory !");
		}
		
		int nbMsgs=0;
		File[] files=rep.listFiles();
		for(File file:files){
					System.out.println("indexPosts : "+file);
					nbMsgs+=indexPostsFromFile(file, db, collection,weightComputer,trans);
					System.out.println(nbMsgs+" traites");
		}
		nbMsgs++;
		System.out.println(nbMsgs+" Posts");
		System.out.println("Creation indexs");
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		
		col.ensureIndex(new BasicDBObject("tags.tag", 1));
		col.ensureIndex(new BasicDBObject("id", 1));
		col.ensureIndex(new BasicDBObject("timestamp", 1));
		col.ensureIndex(new BasicDBObject("text", 1));
		
		return collection;
	}
	
	public int indexPostsFromFile(File f, String db, String col, WeightComputer weightComputer,TextTransformer trans){
		int nbMsgs=0;
		Stemmer stemmer=new Stemmer();
		BufferedReader lecteur=null;
		//PrintWriter writer=new PrintWriter("data"); 
		try{
			lecteur=new BufferedReader(new FileReader(f));
			String ligne;
			
			boolean messageOn=false;
			int nbLines=0;
			String msg="";
			ArrayList<String> tags=new ArrayList<String>();
			ArrayList<DBObject> links=new ArrayList<DBObject>();
			long date=0;
			String url="";
			String name="";
			String owner="";
			while(((ligne=lecteur.readLine())!=null)){ // && (nb<10000)){
					//System.out.println(ligne); 
					nbLines++;
					ligne=ligne.replaceAll("\r", " ");
					ligne=ligne.replaceAll("\\r", " ");
					ligne=ligne.replaceAll("\\\\r", " ");
					ligne=ligne.replaceAll("\n", " ");
					ligne=ligne.replaceAll("\\n", " ");
					ligne=ligne.replaceAll("\\\\n", " ");
					ligne=ligne.replaceAll("\t", " ");
					ligne=ligne.replaceAll("\\t", " ");
					ligne=ligne.replaceAll("\\\\t", " ");
					ligne=ligne.replaceAll("  ", " ");
					ligne=ligne.replace("^ ", "");
					
					if(ligne.startsWith("P ")){
						nbMsgs++;
						
						if((msg!="") && (date!=0)){
							boolean aff=false;
							if(tags.size()>100){
								aff=true;
								System.out.println(msg);
							}
							
							HashMap<Integer,Double> poids=trans.transform(weightComputer.getWeightsForIds(msg));
							//System.out.println(" weights computed");
							if (poids.size()>0){
								indexPost(db,col,name,owner,date,poids,tags,links);
							}
							if(aff){
								System.out.println("ok "+tags.size()+" tags");
							}
						}
						ligne=ligne.replaceFirst("P", "");
						name=ligne;
						msg="";
						date=0;
						String own[]=ligne.split("//");
						int ind=own[1].indexOf("/");
						if(ind<0){
							owner=own[1];
						}
						else{
							owner=own[1].substring(0,ind);
						}
						name=ligne;
						tags=new ArrayList<String>();
						links=new ArrayList<DBObject>();
						if(nbMsgs%1000==0){
							System.out.println(nbMsgs+" posts traites");
						}
						continue;
					}
					if(ligne.startsWith("T ")){
						SimpleDateFormat textFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
						ligne=ligne.replaceFirst("T", "");
						while(ligne.startsWith(" ")){
							ligne=ligne.substring(1);
						}
						Date myDate = null;
						try{
							myDate = textFormat.parse(ligne);
							date=myDate.getTime();
						}
						catch(ParseException e){
							
						}
						continue;
					}
					if(ligne.startsWith("Q ")){
						ligne=ligne.replaceFirst("Q ", "");
						msg+=" "+ligne;
						tags.add(ligne);
						continue;
					}
					if(ligne.startsWith("L ")){
						ligne=ligne.replaceFirst("L ", "");
						DBObject l=new BasicDBObject();
						l.put("link", ligne);
						links.add(l);
						continue;
					}			
						
						
					
			}
			
			if(msg!=""){
				HashMap<Integer,Double> poids=trans.transform(weightComputer.getWeightsForIds(msg));
				//System.out.println(" weights computed");
				if (poids.size()>0){
					indexPost(db,col,name,owner,date,poids,tags,links);
				}
			}
			
				lecteur.close();
		}
		catch(IOException e){
			throw new RuntimeException("Indexation problem");
		}
		return nbMsgs;
	}
		
	
		
	private void indexPost(String db,String col, String name, String owner, long date, HashMap<Integer,Double> poids, ArrayList<String> tags, ArrayList<DBObject> links){
		User user=User.getUser(owner);
		BasicDBObject obj=new BasicDBObject();
		obj.put("links", links);
		Post p=new Post(name,user,date,poids,obj);
		for(String tag:tags) p.addTag(tag);
		p.indexInto(db, col);
		User.reinitUsers();
		Post.reinitPosts();
		
	}
	
	
	public String indexStems(String db,String filename) throws IOException {
		String collection=MongoDB.mongoDB.createCollection(db,"stems"," stems from "+filename);
		System.out.println("Indexation "+filename);
		HashMap<String,Integer> stems=new HashMap<String,Integer>();
		HashMap<String,Integer> stemsNbs=new HashMap<String,Integer>();
		
		File rep=new File(filename);
		if(!rep.isDirectory()){
			throw new RuntimeException(filename+" is not a directory !");
		}
		int nbMsgs=0;
		File[] files=rep.listFiles();
		for(File file:files){
			System.out.println("indexStems : "+file);
			nbMsgs+=indexStemsFromFile(file,stems,stemsNbs);
			System.out.println(nbMsgs+" traites");
		}

		int i=0;
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		System.out.println("Insert stems in collection ");
		
		for(String stem:stems.keySet()){
			int nbp=stems.get(stem);
			double idf=Math.log((1.0*nbMsgs)/(1.0*nbp));
			i++;
			
			BasicDBObject obj = new BasicDBObject();
			obj.put("id", i);
			obj.put("stem",stem);
			obj.put("idf", idf);
			obj.put("nb", stemsNbs.get(stem));
			col.insert(obj);
		}
		System.out.println("Creation indexs");
		//DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		col.ensureIndex(new BasicDBObject("stem", 1));
		col.ensureIndex(new BasicDBObject("id", 1));
		
		return collection;
	}
	
	// returns the number of considered documents
	public int indexStemsFromFile(File f,HashMap<String,Integer> stems,HashMap<String,Integer> stemsNbs){
		int nbMsgs=0;
		Stemmer stemmer=new Stemmer();
		BufferedReader lecteur=null;
		//PrintWriter writer=new PrintWriter("data"); 
		try{
			lecteur=new BufferedReader(new FileReader(f));
			String ligne;
			
			boolean messageOn=false;
			int nbLines=0;
			String msg="";
			String url="";
			
			while(((ligne=lecteur.readLine())!=null)){ // && (nb<10000)){
					//System.out.println(ligne); 
					nbLines++;
					ligne=ligne.replaceAll("\r", " ");
					ligne=ligne.replaceAll("\\r", " ");
					ligne=ligne.replaceAll("\\\\r", " ");
					ligne=ligne.replaceAll("\n", " ");
					ligne=ligne.replaceAll("\\n", " ");
					ligne=ligne.replaceAll("\\\\n", " ");
					ligne=ligne.replaceAll("\t", " ");
					ligne=ligne.replaceAll("\\t", " ");
					ligne=ligne.replaceAll("\\\\t", " ");
					ligne=ligne.replaceAll("  ", " ");
					ligne=ligne.replace("^ ", "");
					
					if(ligne.startsWith("P ")){
						nbMsgs++;
						if(msg!=""){
							indexStemsMsg(stemmer, msg, stems,stemsNbs);
						}
						msg="";
						if(nbMsgs%1000==0){
							System.out.println(nbMsgs+" messages traites");
						}
						continue;
					}
					if(ligne.startsWith("T ")){
						continue;
					}
					if(ligne.startsWith("Q ")){
						ligne=ligne.replace("Q ", "");
						msg+=" "+ligne;
						continue;
					}
					if(ligne.startsWith("L ")){
						continue;
					}
					
					
					
			}
			
			if(msg!=""){
				indexStemsMsg(stemmer, msg, stems,stemsNbs);
			}
			
			
			lecteur.close();
		}
		catch(IOException e){
			throw new RuntimeException("Indexation problem");
		}	
		
		
		return nbMsgs;
	}
	
	private void indexStemsMsg(Stemmer stemmer, String msg, HashMap<String,Integer> stems, HashMap<String,Integer> stemsNbs){
		HashMap<String,Integer> w=stemmer.porterStemmerHash(msg);
		w.remove(" * ");
		for(String s:w.keySet()){
			if(!s.matches("[0-9]*")){
				Integer nbp=stems.get(s);
				nbp=(nbp==null)?0:nbp;
				stems.put(s, nbp+1);
				Integer nbps=stemsNbs.get(s);
				nbps=(nbps==null)?0:nbps;
				stemsNbs.put(s, nbps+w.get(s));
			}
		}
	}
	
	@Override
	public String toString() {
		return("MemetrackerIndexer");
	}

	
	
	
}
