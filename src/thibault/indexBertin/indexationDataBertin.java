	package thibault.indexBertin;
	import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
	






import postTagger.TagByPrefix;
import strLinkUsers.PostsInSameCascade;
import cascades.CascadeFeatureProducer;
import cascades.CascadeFeaturer;
import cascades.CascadesProducer;
import cascades.CascadesSelector;
import cascades.CascadesTrainTestSetsBuilder;
import cascades.Chi2Feature;
import cascades.CumulativeNbPosts;
import cascades.NbUsers;
import cascades.Step;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

import actionsBD.MongoDB;
import core.Post;
import core.User;
import wordsTreatment.Stemmer;
import wordsTreatment.TF_Weighter;
import wordsTreatment.WeightComputer;
import indexation.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

	public class indexationDataBertin extends DataIndexer {
	
		
		public String indexDataBis(String db, String filename,  TF_WeighterThib weightComputer, TextTransformerThib trans)
				throws IOException {
			
			MongoDB m = new MongoDB("localhost");
			
			String collection ;
			collection= m.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
			
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);
			System.out.println("Indexation "+filename);
			HashMap<String,User> users=new HashMap<String,User>();
			HashSet<Post> posts=new HashSet<Post>();
			
			
			BufferedReader lecteur=new BufferedReader(new FileReader(filename));
			try{
				String ligne;
				int nbl=0;
				while((ligne=lecteur.readLine())!=null){
					nbl++;
					//System.out.println(ligne); 
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
						if(o.get("lang").equals("eng")){
						//Recuperation du user name et creation du user dans la tables users si inexistant
						User user;
						Long timestamp = null;
						String text;
						
						
						String name=""+o.get("user_account");
						if (users.containsKey(name)){
							user=users.get(name);
						}
						else{
							user=new User(name);
							users.put(name,user);
						}
						
						String time=o.get("publication_date").toString();
						try {
							Date date=df.parse(time);
							timestamp=(long) date.getTime()/1000;
						} catch (java.text.ParseException e) {
							e.printStackTrace();
						}
						
						text=o.get("content").toString();
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
						text=text.replace("[\"", "");
						
						//System.out.println(name+"\t"+timestamp+"\t"+text);
						///////////////////////////////////////////////////////////////////	
						
						// recuperation des id de stems + eventuellement idf
						
						HashMap<Integer,Double> poids=trans.transformBis(m,weightComputer.getWeightsForIds(m,text));
						
						//System.out.println(poids);
						if (poids.size()>0){
							PostThib p=new PostThib(text,user,timestamp, poids);
							p.indexIntoBis(m,db,collection);
							user.reinitPosts();
							Post.reinitPosts();
						}
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
			

			
			DBCollection col=m.getCollectionFromDB(db,collection);
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
			MongoDB m = new MongoDB("localhost");

			String collection ;
			collection=m.createCollection(db,"stems"," stems from "+filename);
			
			System.out.println("Indexation "+filename);
			//ArrayListStruct<User> ret=new ArrayListStruct<User>();
			HashMap<String,Integer> stems=new HashMap<String,Integer>();
			Stemmer stemmer=new Stemmer();
			///////////////////////////////
			BufferedReader lecteur=new BufferedReader(new FileReader(filename));

			try{
				String ligne;
				int nbl=0;
				while((ligne=lecteur.readLine())!=null){
					nbl++;
					//System.out.println(ligne); 
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
						if(o.get("lang").equals("eng")){
							
						
						String text=o.get("content").toString();
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
				String db="bertin";
				//String filename="/local/gisselbrecht/bertinFinal.json";
				//String filename="/data/lip6/db/baseBertin/bertinFinal.json";
				String filename="/local/gisselbrecht/bertinFinal.json";
				String postsCol="posts_1";
				//String cascadesCol="cascades_4";
				//String stepsCol="steps_3";
				String stemsCol;
				int minStep=1;
				indexationDataBertin indexer=new indexationDataBertin(); 
				
				//on recupere les stems
				stemsCol =indexer.indexStems(db, filename);
				//on les range
				TextTransformerThib trans=new IDFPrunerThib(2000,db,stemsCol);
				trans.learn();
				//on index les posts
				postsCol=indexer.indexDataBis(db, filename, new TF_WeighterThib(db,stemsCol), trans);
				//on les tag
				TagByPrefixThib strTag=new TagByPrefixThib("#");
			    postsCol=strTag.tagCollection(db,postsCol);
			    //on seleciotnne les posts des 10000 utiisateurs qui font le plus de post
			    // PostsSelector postSel=new PostsSelector();
				//postsCol=postSel.selectPosts(db, postsCol, 10000);
				//creation des cascade
				//CascadesProducer cp=new CascadesProducer();
				//String cascadesCol=cp.produceCascades(db,postsCol,new BasicDBObject(),0,-1,5,-1);
				//Step.indexeSteps(db,postsCol);
			
				
				//ArrayList<CascadeFeatureProducer> featurers=new ArrayList<CascadeFeatureProducer>();
				//featurers.add(new NbUsers());
				//HashMap<Long,Step> steps=Step.loadSteps(db,"steps_3",minStep);
				//long duree=Step.getStepLength(steps);
				//System.out.println("Nb Steps = "+steps.size()+" Duree step = "+duree);
				//featurers.add(new NbSteps(steps));
				//featurers.add(new CumulativeNbPosts(steps,true));
				//featurers.add(new Chi2Feature(steps));
				//ArrayList<Double> thresholds=new ArrayList<Double>();
				//thresholds.add(5.0);
				//thresholds.add(2.0);
				//thresholds.add(1500.0);
				//thresholds.add(100.0);
				
				//CascadeFeaturer cf=new CascadeFeaturer(featurers,thresholds);
				//CascadesSelector cs=new CascadesSelector(cf);
				//cascadesCol=cs.selectCascades(db, cascadesCol, stepsCol,1,minStep);
				//String usersCol=(new PostsInSameCascade(true)).linkUsers(db,cascadesCol);
				//CascadesTrainTestSetsBuilder.build(db,cascadesCol,0.8);
				
			
			}
			catch(Exception e){
				//System.out.println(e);
				e.printStackTrace();
			}
		}

	}
