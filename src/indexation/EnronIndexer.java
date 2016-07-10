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
 * Indexes the mail corpus enron.
 * Only sent, _sent_mail and sent_items directories are used and mail owners then correspond to the name of the directory of the participant.
 * Threads are formed between mails of a same subject
 * They can also be formed afterwards, by a PostTagger strategy.
 * This results in a very cleaner version of the corpus than with EnronAllIndexer. 
 *
 */
public class EnronIndexer extends DataIndexer {

	/*long step=1;
	int maxNbSteps=100;
	
	public EnronIndexer(long step, int maxNbSteps){
		this.step=step;
		this.maxNbSteps=maxNbSteps;
	}
	*/
	@Override
	public String indexData(String db,String filename, WeightComputer weightComputer,TextTransformer trans) throws IOException {
		String collection=MongoDB.mongoDB.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
		HashMap<String,String> threads=new HashMap<String,String>();
		File rep=new File(filename);
		if(!rep.isDirectory()){
			throw new RuntimeException(filename+" is not a directory !");
		}
		int nbMsgs=0;
		File[] fusers=rep.listFiles();
		HashMap<String,User> users=new HashMap<String,User>();
		for(File fuser:fusers){
			if(fuser.isDirectory()){
				String user=fuser.getName();
				File[] childs=fuser.listFiles();
				for(int i=0;i<childs.length;i++){
					
					File ch=childs[i];
					if((ch.getName().equals("sent")) || (ch.getName().equals("_sent_mail")) || (ch.getName().equals("sent_items"))){
					    System.out.println(ch.getAbsolutePath()); 
						File[] mails=ch.listFiles();
					     for(File f:mails){
					    	 if(f.isFile()){
					    		 Post post=getPostFromFile(f,user,weightComputer,trans,threads);
					    		 if(post!=null){
					    			 nbMsgs++;
					    			 post.indexInto(db,collection);
									 User.reinitUsers();
									 Post.reinitPosts(); 
					    		 }
					    	 }
					     }
					}
				}
			}
		}
		nbMsgs++;
		System.out.println(nbMsgs+" Mails");
		System.out.println("Creation indexs");
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		col.ensureIndex(new BasicDBObject("tags.tag", 1));
		col.ensureIndex(new BasicDBObject("id", 1));
		return collection;
	}
	
	public Post getPostFromFile(File f,String owner, WeightComputer weightComputer,TextTransformer trans,HashMap<String,String> threads){
		Post p=null;
		BufferedReader lecteur=null;
		try{
			lecteur=new BufferedReader(new FileReader(f));
			String ligne;
			String msg="";
			long date=0;
			String subject="";
			
			boolean date_Ok=false;
			ArrayList<String> to=new ArrayList<String>();
			ArrayList<String> cc=new ArrayList<String>();
			ArrayList<String> bcc=new ArrayList<String>();
			boolean messageOn=false;
			while(((ligne=lecteur.readLine())!=null)){ 
					
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
					
					if(ligne.startsWith("Date:")){
						
						SimpleDateFormat textFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
						ligne=ligne.replace("Date:", "");
						while(ligne.startsWith(" ")){
							ligne=ligne.substring(1);
						}
						Date myDate = null;
						try{
							myDate = textFormat.parse(ligne);
							date_Ok=true;
							date=myDate.getTime();
						}
						catch(ParseException e){
							SimpleDateFormat textFormat2 = new SimpleDateFormat("EEEE, MMMM dd, yyyy HH:mm:ss Z", Locale.ENGLISH);
							try{
								myDate = textFormat2.parse(ligne);
								date_Ok=true;
								date=myDate.getTime();
							}
							catch(ParseException e2){
								SimpleDateFormat textFormat3 = new SimpleDateFormat("EEEE, MMMM dd, yyyy HH:mm:ss a", Locale.ENGLISH);
								try{
									myDate = textFormat3.parse(ligne);
									date_Ok=true;
									date=myDate.getTime();
								}
								catch(ParseException e3){
									SimpleDateFormat textFormat4 = new SimpleDateFormat("EEEE, MMMM dd, yyyy HH:mm a", Locale.ENGLISH);
									try{
										myDate = textFormat4.parse(ligne);
										date_Ok=true;
										date=myDate.getTime();
									}
									catch(ParseException e4){
										SimpleDateFormat textFormat5 = new SimpleDateFormat("EEEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
										try{
											myDate = textFormat5.parse(ligne);
											date_Ok=true;
											date=myDate.getTime();
										}
										catch(ParseException e5){
											e5.printStackTrace();
										}
									}
								}
							}
						}
						continue;
					}
					
					if(ligne.startsWith("Subject:")){
						
						ligne=ligne.replace("Subject:", "");
						
						while(ligne.startsWith(" ")){
							ligne=ligne.substring(1);
						}
						subject=ligne;
						subject=subject.replaceAll("Re: ", "");
						subject=subject.replaceAll("Re:", "");
						subject=subject.replaceAll("Tr: ", "");
						subject=subject.replaceAll("Tr:", "");
						
						continue;
					}
					
					if(ligne.startsWith("X-FileName:")){
						
						messageOn=true;
						
						continue;
					}
					String[] li;
					
					if(!messageOn){
						continue;
					}
					msg+=" "+ligne;
			}
			
			if((msg!="") && (subject!="") && (date_Ok)){
				// recuperation des id de stems + eventuellement idf
				HashMap<Integer,Double> poids=trans.transform(weightComputer.getWeightsForIds(msg.toString()+subject));
				//System.out.println(" weights computed");
				if (poids.size()>0){
					User user=User.getUser(owner);
					p=new Post(subject,user,date,poids);
					
					
					if(threads.containsKey(subject)){
						String tn=threads.get(subject);
						p.addTag(tn);
					}
					else{
						String tn="T"+threads.size();
						p.addTag(tn);
						threads.put(subject,"T"+threads.size());
					}
					
					
					
				}
			}
			lecteur.close();
		}
		catch(IOException e){
			throw new RuntimeException("Indexation problem");
		}
		return p;
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
		File[] fusers=rep.listFiles();
		HashMap<String,User> users=new HashMap<String,User>();
		for(File fuser:fusers){
			if(fuser.isDirectory()){
				String user=fuser.getName();
				File[] childs=fuser.listFiles();
				for(int i=0;i<childs.length;i++){
					File ch=childs[i];
					if((ch.getName().equals("sent")) || (ch.getName().equals("_sent_mail")) || (ch.getName().equals("sent_items"))){
					     File[] mails=ch.listFiles();
					     for(File f:mails){
					    	 if(f.isFile()){
					    		 indexStemsFromFile(f,stems,stemsNbs);
					    		 nbMsgs++;
					    	 }
					     }
					}
				}
			}
		}

		int i=0;
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		
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
	public void indexStemsFromFile(File f,HashMap<String,Integer> stems,HashMap<String,Integer> stemsNbs){
		String subject="";
		
		Stemmer stemmer=new Stemmer();
		BufferedReader lecteur=null;
		//PrintWriter writer=new PrintWriter("data"); 
		try{
			lecteur=new BufferedReader(new FileReader(f));
			String ligne;
			
			boolean messageOn=false;
			int nbLines=0;
			String msg="";
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
					
					if(ligne.startsWith("Subject:")){
						
						ligne=ligne.replace("Subject:", "");
						
						while(ligne.startsWith(" ")){
							ligne=ligne.substring(1);
						}
						subject=ligne;
						subject=subject.replaceAll("Re: ", "");
						subject=subject.replaceAll("Re:", "");
						subject=subject.replaceAll("Tr: ", "");
						subject=subject.replaceAll("Tr:", "");
						msg+=subject;
						continue;
					}
					
					if(ligne.startsWith("X-FileName:")){
						messageOn=true;
						
						continue;
					}
					if(!messageOn){
						continue;
					}
					msg+=" "+ligne;
					
			}
			
			if(msg!=""){
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
			
			
			lecteur.close();
		}
		catch(IOException e){
			throw new RuntimeException("Indexation problem");
		}	
		
	}
	
	@Override
	public String toString() {
		return("EnronIndexer");
	}

	public static void main(String[] args){
		SimpleDateFormat textFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
		//SimpleDateFormat textFormat = new SimpleDateFormat("yyyy-MM-dd");
		//String paramDateAsString = "Tue, 24 Jul 2001 05:59:09 -0700 (PDT)";
		//String paramDateAsString = "Friday, November 9, 2001 5:09:13 GMT";
		//String paramDateAsString = "Tuesday, November 27, 2001 2:32:45 PM";
		//String paramDateAsString =  "Saturday, September 15, 2001 3:35 PM";
		String paramDateAsString =  "Fri, 09 Feb 2001 22:55:29";
		//String paramDateAsString = "2001-12-12";
		Date myDate = null;
		try{
			myDate = textFormat.parse(paramDateAsString);
			System.out.print(myDate);
		}
		catch(ParseException e){
			e.printStackTrace();
		}
		
		SimpleDateFormat textFormat2 = new SimpleDateFormat("EEEE, MMMM dd, yyyy HH:mm:ss Z", Locale.ENGLISH);
		try{
			myDate = textFormat2.parse(paramDateAsString);
			System.out.print(myDate);
		}
		catch(ParseException e2){
			e2.printStackTrace();
		}
		SimpleDateFormat textFormat3 = new SimpleDateFormat("EEEE, MMMM dd, yyyy HH:mm:ss a", Locale.ENGLISH);
		try{
			myDate = textFormat3.parse(paramDateAsString);
			System.out.print(myDate);
		}
		catch(ParseException e3){
			e3.printStackTrace();
		}
		SimpleDateFormat textFormat4 = new SimpleDateFormat("EEEE, MMMM dd, yyyy HH:mm a", Locale.ENGLISH);
		try{
			myDate = textFormat4.parse(paramDateAsString);
			System.out.print(myDate);
		}
		catch(ParseException e4){
			e4.printStackTrace();
		}
		SimpleDateFormat textFormat5 = new SimpleDateFormat("EEEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
		try{
			myDate = textFormat5.parse(paramDateAsString);
			System.out.print(myDate);
		}
		catch(ParseException e5){
			e5.printStackTrace();
		}
	}
	
	
}
