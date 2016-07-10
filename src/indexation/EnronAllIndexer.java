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
 * Index all documents in the enron corpus
 * Threads are formed between mails of same subject with at least nbMinParticipants common participants.
 * The problem of this indexer is that fields from and to of the mails are not well formatted (lot of different addresses from a same participant for example) and resulting data are very noisy.
 *
 */
public class EnronAllIndexer extends DataIndexer {

	private int nbMinParticipants;
	
	public EnronAllIndexer(){
		this(2);
	}
	public EnronAllIndexer(int nbMinParticipants){
		this.nbMinParticipants=nbMinParticipants;
	}
	
	
	
	
	@Override
	public String indexData(String db,String filename, WeightComputer weightComputer,TextTransformer trans) throws IOException {
		String collection=MongoDB.mongoDB.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
		
		System.out.println("Indexation "+filename);
		//ArrayListStruct<User> ret=new ArrayListStruct<User>();
		HashMap<String,User> users=new HashMap<String,User>();
		HashSet<Post> posts=new HashSet<Post>();
		Stemmer stemmer=new Stemmer();
		int nbThreads=0;
		HashMap<String,HashSet<String>> threadNames=new HashMap<String,HashSet<String>>();
		//HashMap<String,String> threadsSubjects=new HashMap<String,String>();
		HashMap<String,HashSet<String>> threadUsers=new HashMap<String,HashSet<String>>();
		User user;
		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		try{
			String ligne;
			int nbl=0;
			/*while((ligne=lecteur.readLine())!=null){
				nbl++;
			}
			System.out.println(nbl+" lignes");
			lecteur.close();
			lecteur=new BufferedReader(new FileReader(filename));*/
			int nb=0;
			int nbinvalides=0;
			String msg="";
			int nbMsgs=0;
			long date=0;
			String from="";
			String subject="";
			boolean toMode=false;
			boolean ccMode=false;
			boolean bccMode=false;
			boolean date_Ok=false;
			ArrayList<String> to=new ArrayList<String>();
			ArrayList<String> cc=new ArrayList<String>();
			ArrayList<String> bcc=new ArrayList<String>();
			boolean messageOn=false;
			while(((ligne=lecteur.readLine())!=null)){ // && (nb<10000)){
					//System.out.println(ligne); 
					
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
					//System.out.println(ligne);
					if(ligne.startsWith("<File") || ligne.startsWith("</File")){
						toMode=false;
						ccMode=false;
						bccMode=false;
						messageOn=false;
						continue;
					}
					if(ligne.startsWith("Message-ID:")){
						toMode=false;
						ccMode=false;
						bccMode=false;
						if((msg!="") && (date_Ok)){
							// recuperation des id de stems + eventuellement idf
							HashMap<Integer,Double> poids=trans.transform(weightComputer.getWeightsForIds(msg.toString()+" "+subject));
							//System.out.println("subject : "+subject);
							//System.out.println(msg);
							//System.out.println(weightComputer.getWeightsForIds(msg.toString()+" "+subject));
							//System.out.println("Poids : \n "+poids);
							//System.out.println(" weights computed");
							if ((subject.length()>0) && (poids.size()>0)){
								user=User.getUser(from);
								Post p=new Post(subject,user,date,poids);
								HashSet<String> allUsers=new HashSet<String>();
								allUsers.add(from);
								allUsers.addAll(to);
								allUsers.addAll(cc);
								allUsers.addAll(bcc);
								boolean ok=false;
								if(threadNames.containsKey(subject)){
									HashSet<String> tn=threadNames.get(subject);
									for(String tname:tn){
										HashSet<String> parts=threadUsers.get(tname);
										int nbpar=0;
										for(String par:allUsers){
											if(parts.contains(par)){
												nbpar++;
											}	
										}
										if(nbpar>=this.nbMinParticipants){
											p.addTag(tname, ""+nbpar);
											parts.addAll(allUsers);
											ok=true;
										}
										
									}
									
								}
								if(!ok){
									nbThreads++;
									HashSet<String> tn=threadNames.get(subject);
									if(tn==null){
										tn=new HashSet<String>();
										threadNames.put(subject, tn);
									}
									tn.add("T"+nbThreads);
									HashSet<String> parts=new HashSet<String>();
									parts.addAll(allUsers);
									threadUsers.put("T"+nbThreads,parts);
									p.addTag("T"+nbThreads, ""+parts.size());
									
								}
								
								/*p.addTag(from, "from");
								for(String us:to){
									p.addTag(us, "to");
								}
								for(String us:cc){
									p.addTag(us, "cc");
								}
								for(String us:bcc){
									p.addTag(us, "bcc");
								}
								p.addTag(subject,"subject");*/
								
								//p.addTag(tag, val)
								p.indexInto(db,collection);
								user.reinitPosts();
								Post.reinitPosts();
							}
						}
						to=new ArrayList<String>();
						cc=new ArrayList<String>();
						bcc=new ArrayList<String>();
						subject="";
						from="";
						date=0;
						msg="";
						nbMsgs++;
						System.out.println(nbMsgs+" traites");
						date_Ok=false;
						continue;
					}
					if(ligne.startsWith("Date:")){
						toMode=false;
						ccMode=false;
						bccMode=false;
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
					if(ligne.startsWith("From:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
						ligne=ligne.replace("From:", "");
						while(ligne.startsWith(" ")){
							ligne=ligne.substring(1);
						}
						from=ligne;
						continue;
					}
					if(ligne.startsWith("To:")){
						ccMode=false;
						bccMode=false;
						toMode=true;
						ligne=ligne.replace("To:", "");
						while(ligne.startsWith(" ")){
							ligne=ligne.substring(1);
						}
						
					}
					
					if(ligne.startsWith("Subject:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
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
					if(ligne.startsWith("Cc:")){
						ccMode=true;
						bccMode=false;
						toMode=false;
						ligne=ligne.replace("Cc:", "");
						while(ligne.startsWith(" ")){
							ligne=ligne.substring(1);
						}
					}
					
					if(ligne.startsWith("Mime-Version:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
						continue;
					}
					if(ligne.startsWith("Content-Type:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
						continue;
					}
					if(ligne.startsWith("Content-Transfer-Encoding:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
						continue;
					}
					if(ligne.startsWith("Bcc:")){
						ccMode=false;
						bccMode=true;
						toMode=false;
						ligne=ligne.replace("Bcc:", "");
						while(ligne.startsWith(" ")){
							ligne=ligne.substring(1);
						}
					}
					
					if(ligne.startsWith("X-From:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
						continue;
					}
					if(ligne.startsWith("X-To:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
						continue;
					}
					if(ligne.startsWith("X-cc:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
						continue;
					}
					if(ligne.startsWith("X-bcc:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
						continue;
					}
					if(ligne.startsWith("X-Folder:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
						continue;
					}
					if(ligne.startsWith("X-Origin:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
						continue;
					}
					
					if(ligne.startsWith("X-FileName:")){
						ccMode=false;
						bccMode=false;
						toMode=false;
						messageOn=true;
						
						continue;
					}
					String[] li;
					if(toMode){
						//String[] li=ligne.split(": ");
						li=ligne.split(",");
						for(String s:li){
							s=s.replaceAll(" ", "");
							if(s.contains("@")){
								to.add(s);
							}
						}
						continue;
					}
					if(ccMode){
						li=ligne.split(",");
						for(String s:li){
							s=s.replaceAll(" ", "");
							if(s.contains("@")){
								cc.add(s);
							}
						}
						continue;
					}
					if(bccMode){
						li=ligne.split(",");
						for(String s:li){
							s=s.replaceAll(" ", "");
							if(s.contains("@")){
								bcc.add(s);
							}
						}
						continue;
					}
					if(!messageOn){
						continue;
					}
					msg+=" "+ligne;
					
					

			}
			
			
			if((msg!="") && (date_Ok)){
				// recuperation des id de stems + eventuellement idf
				HashMap<Integer,Double> poids=trans.transform(weightComputer.getWeightsForIds(msg.toString()+subject));
				//System.out.println(" weights computed");
				if (poids.size()>0){
					user=User.getUser(from);
					Post p=new Post(subject,user,date,poids);
					HashSet<String> allUsers=new HashSet<String>();
					allUsers.add(from);
					allUsers.addAll(to);
					allUsers.addAll(cc);
					allUsers.addAll(bcc);
					boolean ok=false;
					if(threadNames.containsKey(subject)){
						HashSet<String> tn=threadNames.get(subject);
						for(String tname:tn){
							HashSet<String> parts=threadUsers.get(tname);
							int nbpar=0;
							for(String par:allUsers){
								if(parts.contains(par)){
									nbpar++;
								}	
							}
							if(nbpar>=this.nbMinParticipants){
								p.addTag(tname, ""+nbpar);
								parts.addAll(allUsers);
								ok=true;
							}
							
						}
						
					}
					if(!ok){
						nbThreads++;
						HashSet<String> tn=threadNames.get(subject);
						if(tn==null){
							tn=new HashSet<String>();
							threadNames.put(subject, tn);
						}
						tn.add("T"+nbThreads);
						HashSet<String> parts=new HashSet<String>();
						parts.addAll(allUsers);
						threadUsers.put("T"+nbThreads,parts);
						
					}
					
					/*p.addTag(from, "from");
					for(String us:to){
						p.addTag(us, "to");
					}
					for(String us:cc){
						p.addTag(us, "cc");
					}
					for(String us:bcc){
						p.addTag(us, "bcc");
					}
					p.addTag(subject,"subject");*/
					
					//p.addTag(tag, val)
					p.indexInto(db,collection);
					user.reinitPosts();
					Post.reinitPosts();
				}
			}
			msg="";
			nbMsgs++;
			System.out.println(nbMsgs+" Mails");
			System.out.println("Creation indexs");
			DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
			col.ensureIndex(new BasicDBObject("tags.tag", 1));
			col.ensureIndex(new BasicDBObject("id", 1));
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
		HashMap<String,Integer> stemsNbs=new HashMap<String,Integer>();
		
		Stemmer stemmer=new Stemmer();
		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		//PrintWriter writer=new PrintWriter("data"); 
		try{
			String ligne;
			int nbl=0;
			String old="";
			while((ligne=lecteur.readLine())!=null){
				
					nbl++;
				//}
			}
			
			System.out.println(nbl+" lignes");
			lecteur.close();
			lecteur=new BufferedReader(new FileReader(filename));
			int nb=0;
			int nbinvalides=0;
			String msg="";
			int nbMsgs=0;
			//ArrayList<String> to=new ArrayList<String>();
			boolean messageOn=false;
			int nbLines=0;
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
					//System.out.println(ligne);
					if(ligne.startsWith("<File") || ligne.startsWith("</File")){
						messageOn=false;
						continue;
					}
					if(ligne.startsWith("Message-ID:")){
						continue;
					}
					if(ligne.startsWith("Date:")){
						continue;
					}
					if(ligne.startsWith("From:")){
						continue;
					}
					if(ligne.startsWith("Subject:")){
						msg+=ligne;
						continue;
					}
					if(ligne.startsWith("To:")){
						continue;
					}
					if(ligne.startsWith("Cc:")){
						continue;
					}
					if(ligne.startsWith("Mime-Version:")){
						continue;
					}
					if(ligne.startsWith("Content-Type:")){
						continue;
					}
					if(ligne.startsWith("Content-Transfer-Encoding:")){
						continue;
					}
					if(ligne.startsWith("Bcc:")){
						continue;
					}
					if(ligne.startsWith("X-From:")){
						continue;
					}
					if(ligne.startsWith("X-To:")){
						continue;
					}
					if(ligne.startsWith("X-cc:")){
						continue;
					}
					if(ligne.startsWith("X-bcc:")){
						continue;
					}
					if(ligne.startsWith("X-Folder:")){
						continue;
					}
					if(ligne.startsWith("X-Origin:")){
						continue;
					}
					if(ligne.startsWith("X-FileName:")){
						messageOn=true;
						if(msg!=""){
							HashMap<String,Integer> w=stemmer.porterStemmerHash(msg);
							w.remove(" * ");
							for(String s:w.keySet()){
								if(!s.matches("[0-9]*")){
									Integer nbp=stems.get(s);
									nbp=(nbp==null)?0:nbp;
									stems.put(s, nbp+1);
								}
							}
						}
						
						msg="";
						nbMsgs++;
						System.out.println("nbMsgs = "+nbMsgs+" nbLines = "+nbLines);
						continue;
					}
					if(!messageOn){
						continue;
					}
					msg+=" "+ligne;
						
					HashMap<String,Integer> w=stemmer.porterStemmerHash(ligne);
					w.remove(" * ");
					for(String s:w.keySet()){
							if(!s.matches("[0-9]*")){
								Integer nbp=stemsNbs.get(s);
								nbp=(nbp==null)?0:nbp;
								stemsNbs.put(s, nbp+w.get(s));
							}
					}
					
			}
			if(msg!=""){
				HashMap<String,Integer> w=stemmer.porterStemmerHash(msg);
				w.remove(" * ");
				for(String s:w.keySet()){
					if(!s.matches("[0-9]*")){
						Integer nbp=stems.get(s);
						nbp=(nbp==null)?0:nbp;
						stems.put(s, nbp+1);
					}
				}
			}
			
			msg="";
			nbMsgs++;
			System.out.println(nbMsgs+" Mails");
			
			
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
		}
		finally{
			lecteur.close();
		}
		
		return collection;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args){
		SimpleDateFormat textFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		//SimpleDateFormat textFormat = new SimpleDateFormat("yyyy-MM-dd");
		//String paramDateAsString = "Tue, 24 Jul 2001 05:59:09 -0700 (PDT)";
		//String paramDateAsString = "Friday, November 9, 2001 5:09:13 GMT";
		//String paramDateAsString = "2001-12-12";
		String paramDateAsString = "2008-11-01 00:00:06";
		Date myDate = null;
		try{
			myDate = textFormat.parse(paramDateAsString);
			System.out.print(myDate);
		}
		catch(ParseException e){
			e.printStackTrace();
		}
		
		
	}
	
	
}
