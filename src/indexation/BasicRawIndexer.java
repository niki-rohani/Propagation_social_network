package indexation;

import java.io.BufferedReader;

import java.io.PrintWriter;
import java.util.Comparator;

import wordsTreatment.Stemmer;
import wordsTreatment.TF_Weighter;
import wordsTreatment.TFIDF_Weighter;
import wordsTreatment.WeightComputer;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import postTagger.*;
import actionsBD.MongoDB;
import core.*;

import java.io.*;
import java.util.HashSet;


import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

import strLinkUsers.*;
import indexation.*;
public class BasicRawIndexer extends DataIndexer {

	
	//private PostTagger postTagger=null; // Strategie de mise en relation de posts
	//private StrLinkUsers strLinkUsers=null; // Strategie de mise en relation de users
	
	
	/*public BasicRawIndexer(){
		this(new TF_Weighter());
	}*/
	/*public Trec11Indexer(String collection,WeightComputer wc){
		this(collection,wc,null); //,null);
	}*/
	
	/*public Trec11Indexer(WeightComputer wc,StrLinkPosts str){
		this(wc,str,null);
	}*/

	/*public BasicRawIndexer(WeightComputer wc){ //,PostTagger postTagger){ //, StrLinkUsers struser){
		super(wc);
		//this.postTagger=postTagger;
		//this.strLinkUsers=struser;
	}*/
	
	@Override
	public String indexData(String db,String filename, WeightComputer weightComputer,TextTransformer trans) throws IOException {
		String collection=MongoDB.mongoDB.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
		
		System.out.println("Indexation "+filename);
		//ArrayListStruct<User> ret=new ArrayListStruct<User>();
		HashMap<String,User> users=new HashMap<String,User>();
		HashSet<Post> posts=new HashSet<Post>();
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
					String[] li=ligne.split(" ");
					if (li.length>=2){
						//System.out.println(li[0]+":"+li[1]);
						try{
							Long.valueOf(li[1]);
						}
						catch(NumberFormatException e){
							System.out.println("ligne invalide :" + ligne);
							System.out.println(e);
							nbinvalides++;
							continue;
							
						}
						User user;
						if (users.containsKey(li[0])){
							user=users.get(li[0]);
						}
						else{
							user=new User(li[0]);
							users.put(li[0],user);
							//ret.add(user);
							
						}
						
						StringBuilder sb=new StringBuilder();
						for(int i=2;i<li.length;i++){
							sb.append(" "+li[i]);
						}
						//System.out.println("compute weights");
						
						// recuperation des id de stems + eventuellement idf
						HashMap<Integer,Double> poids=trans.transform(weightComputer.getWeightsForIds(sb.toString()));
						//System.out.println(" weights computed");
						if (poids.size()>0){
							Post p=new Post(sb.toString(),user,Long.valueOf(li[1]),poids);
							//posts.add(p);
							//if (postTagger!=null){
							//	postTagger.tagPost(p);
							//}
							p.indexInto(db,collection);
							user.reinitPosts();
							Post.reinitPosts();
						}
						
						
					}
					else{
						System.out.println("ligne invalide :" + ligne);
						nbinvalides++;
						break;
					}
					nb++;
					if (nb%100==0){
						System.out.println(nb+"/"+nbl+" lignes traitees");
					}

			}
			System.out.println(nbinvalides+" lignes invalides");
			
			/*nb=0;
			nbl=posts.size();
			for(Post post:posts){
				post.indexInto(collection);
				if (nb%100==0){
					System.out.println(nb+"/"+nbl+" posts inseres");
				}
				nb++;
			}*/
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
	
	
	
	//  sed 's/\r//g' tweet09.raw > ~/propagation/Propagation/data/tweet09
	
	public String indexStems(String db,String filename) throws IOException {
		String collection=MongoDB.mongoDB.createCollection(db,"stems"," stems from "+filename);
		
		System.out.println("Indexation "+filename);
		//ArrayListStruct<User> ret=new ArrayListStruct<User>();
		HashMap<String,Integer> stems=new HashMap<String,Integer>();
		
		Stemmer stemmer=new Stemmer();
		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		//PrintWriter writer=new PrintWriter("data"); 
		try{
			String ligne;
			int nbl=0;
			String old="";
			while((ligne=lecteur.readLine())!=null){
				/*if(ligne.contains("\\\n")){
					System.out.println(ligne);
				}
				if(ligne.startsWith("\n")){
					old+=" "+ligne;
				}
				else{
					writer.println(old);
					old=ligne;*/
					nbl++;
				//}
			}
			/*if(old.length()!=0){
				writer.println(old);
				nbl++;
			}
			writer.close();
			*/
			System.out.println(nbl+" lignes");
			lecteur.close();
			lecteur=new BufferedReader(new FileReader(filename));
			int nb=0;
			int nbinvalides=0;
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
					String[] li=ligne.split(" ");
					if (li.length>=2){
						//System.out.println(li[0]+":"+li[1]);
						try{
							Long.valueOf(li[1]);
						}
						catch(NumberFormatException e){
							System.out.println("ligne invalide :" + ligne);
							System.out.println(e);
							nbinvalides++;
							continue;
							
						}
						StringBuilder sb=new StringBuilder();
						for(int i=2;i<li.length;i++){
							sb.append(" "+li[i]);
						}
						//System.out.println("compute weights");
						
						HashMap<String,Integer> w=stemmer.porterStemmerHash(sb.toString());
						w.remove(" * ");
						for(String s:w.keySet()){
							Integer nbp=stems.get(s);
							nbp=(nbp==null)?0:nbp;
							stems.put(s, nbp+1);
						}
						//System.out.println("ligne "+ligne);
					}
					else{
						System.out.println("ligne invalide :" + ligne);
						nbinvalides++;
						break;
					}
					nb++;
					if (nb%10000==0){
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
	
	
	
	public String toString(){
		String s=this.getClass()+""; //+"_"+this.weightComputer.toString();
		
		return(s);
	}
	
	public static void main(String[] args){
		
		try{
			
			//System.out.println("Indexation "+args[0]+" > "+args[1]);
			
			String s=" What is an Object? Easy Lesson\\nhttp://tinyurl.com/64rgwu";
			System.out.println(s);
			s=s.replaceAll("\\\\n", " ");
			System.out.println(s);
			return;
			
			//BasicRawIndexer indexer=new BasicRawIndexer(); //,new TagByHashTag());
			//ArrayListStruct<User> data=(ArrayListStruct<User>)(
			//indexer.indexStems(args[1],args[0]);
			//indexer.indexData("tweet09","./data/tweet09",new TF_Weighter("tweet09","stems_1"),new IDFPruner("tweet09","stems2")); //);
			//for(User user:data){
			//	System.out.println(user);
				//user.serialize("./Users/"+loader.toString()+"/user_"+user.getID()+".serial");
			//}
			//data.serialize("./Users/xx.serial");
			
			/*ArrayListStruct<User> data=(ArrayListStruct<User>)ArrayListStruct.deserialize("./Users/xx.serial");
			for(User user:data){
				System.out.println(user);
				
			}*/
		}
		catch(Exception e){
			System.out.println(e);
		}
		
		/*BasicRawIndexer indexerc=new BasicRawIndexer(new TFIDF_Weighter("twitter09","stems"));
		HashMap<String,Integer> f=new HashMap<String,Integer>();
		f.put("a", 2);
		f.put("b", 4);
		FreqComp c=indexerc.new FreqComp(f);
		ArrayList<String> s=new ArrayList<String>(f.keySet());
		Collections.sort(s,c);
		System.out.println(s);*/
	}

}
