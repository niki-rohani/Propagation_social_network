package cascades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;
import core.User;

public class CascadesTrainTestSetsBuilder {

	
	
	
	
	
	// loads cascades and returns a table <id_cascade,cascade>
 	public static HashMap<Integer,Cascade> getCascades(String db, String collection){
         HashMap<Integer,Cascade> cascades=new HashMap<Integer,Cascade>();
         DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
         DBCursor cursor = col.find();
         Post p=null;
         try {
             while(cursor.hasNext()) {
                 DBObject res=cursor.next();
                 Cascade c=Cascade.getCascadeFrom(res);
                 int ic=c.getID();
                 cascades.put(ic, c);
                 User.reinitAllPosts(); // Pour alleger, on supprime les textes des posts dont on ne se sert pas ici
                 Post.reinitPosts();
                 System.out.println("Cascade "+ic+" chargee");
             }
         } finally {
             cursor.close();
         }
         return(cascades);
     }
 		
 	 public static String build(String db,String cascadesCol,double trainRatio){
 		return(build(db,cascadesCol,trainRatio,1));
 	 }
 	 public static String build(String db,String cascadesCol,double trainRatio,boolean light){
  		return(build(db,cascadesCol,trainRatio,1,light));
  	 }
 	 // We suppose that cascades have id from 1 to nb cascades
 	 public static String build(String db,String cascadesCol,double trainRatio,double testRatio){
 		return(build(db,cascadesCol,trainRatio,testRatio,false));	 
 	 }
 	 // We suppose that cascades have id from 1 to nb cascades
 	 public static String build(String db,String cascadesCol,double trainRatio,double testRatio,boolean light){
 		//HashMap<Integer,Cascade> cascades=getCascades(db,cascadesCol);
 		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,cascadesCol);
 		long nbc = col.count();
 		System.out.println(nbc+" cascades");
 		//int nbc=cascades.size();
 		int nbtrain=(int)(nbc*trainRatio);
 		int nbtest=(int)((nbc-nbtrain)*testRatio);
 		
		ArrayList<Integer> cascades=new ArrayList<Integer>((int)nbc);
		for(int i=0;i<nbc;i++){
			cascades.add(i+1);
			if((i%100000)==0){
				System.out.println("gener "+i+" ids");
			}
		}
		Collections.shuffle(cascades);
		System.out.println("gener table cascades ok");
		System.out.println(nbtrain+" train cascades");
		System.out.println(nbtest+" test cascades");
		ArrayList<Integer> test=new ArrayList<Integer>();
		ArrayList<Integer> train=new ArrayList<Integer>();
		//int nset=nbc;
		int nb=0;
		for(int i=0;i<nbtrain;i++){
		//while(nb<nbtrain){
			//System.out.println(nb);
			int x=cascades.get(i);
			train.add(x);
			//System.out.println(x + " size ="+textes.size());
			nb++;
			if((nb%100)==0){
				System.out.println(nb+" train ok");
			}
		}
		
		nb=0;
		
		for(int i=nbtrain;i<(nbtrain+nbtest);i++){
				int x=cascades.get(i);
				test.add(x);
				//System.out.println(x + " size ="+textes.size());
				nb++;
				if((nb%100)==0){
					System.out.println(nb+" test ok");
				}
		}
		
		String desc="cascades train from "+cascadesCol+" trainRatio="+trainRatio+((light)?" LightCascades":"");
		String TraincolOut=MongoDB.mongoDB.createCollection(db,"cascades",desc);
		
		for(Integer i:train){
			Cascade c=Cascade.getCascadeFromDB(db, cascadesCol,i);
			if(i==null){
				throw new RuntimeException("Cascade id="+i+" null");
			}
			if(light){
				c=new ArtificialCascade(c);
			}
			c.indexInto(db, TraincolOut);
			Post.reinitPosts();
			User.reinitUsers();
		}
		DBCollection outcol=MongoDB.mongoDB.getCollectionFromDB(db,TraincolOut);
		outcol.ensureIndex(new BasicDBObject("id", 1));
		
		desc="cascades test from "+cascadesCol+" testRatio="+testRatio+((light)?" LightCascades":"");
		String colOut=MongoDB.mongoDB.createCollection(db,"cascades",desc);
		
		for(Integer i:test){
			Cascade c=Cascade.getCascadeFromDB(db, cascadesCol,i);
			if(light){
				c=new ArtificialCascade(c);
			}
			c.indexInto(db, colOut);
			Post.reinitPosts();
			User.reinitUsers();
		}
		outcol=MongoDB.mongoDB.getCollectionFromDB(db,colOut);
		outcol.ensureIndex(new BasicDBObject("id", 1));
		
		return TraincolOut;
 	 }
 	 
 	
 	 
 	 public static void main(String[] args){
 		 if(args.length<4){
 			 System.out.println("Usage : CascadesTrainTestSetsBuilder <db> <cascades_collection> <train ratio> <test ratio>");
 		 }
 		 else{
 			 build(args[0],args[1],Double.valueOf(args[2]),Double.valueOf(args[3]),true);
 		 }
 	 }
}
