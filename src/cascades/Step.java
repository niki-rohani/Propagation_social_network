package cascades;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;

import java.util.HashSet;
public class Step {
		private long id;
		private long debut;
		private long duree;
		private int nbPosts;
		private static HashMap<Long,HashMap<Long,Step>> steps=new HashMap<Long,HashMap<Long,Step>>();
		
		private Step(long id,long debut,long duree,int nbPosts){
			this.id=id;
			this.debut=debut;
			this.duree=duree;
			this.nbPosts=nbPosts;
		}
		
		private Step(long id,long debut,long duree){
			this(id,debut,duree,0);
		}
		/*public Step(long debut,long duree){
			this(getIdStep(debut,duree),debut,duree);
		}*/
		public Step(long id,long duree){
			this(id,(id-1)*duree,duree);
		}
		public void incNbPosts(){
			nbPosts++;
		}
		public long getStep(){
			return(duree);
		}
		public long getID(){
			return(id);
		}
		public int getNbPosts(){
			return(nbPosts);
		}
		public static Step getStep(long timestamp,long dureeStep){
			long idstep=getIdStep(timestamp,dureeStep);
			HashMap<Long,Step> h=steps.get(dureeStep);
			if (h==null){
				h=new HashMap<Long,Step>();
				steps.put(dureeStep, h);
			}
			Step s=h.get(idstep);
			if (s==null){
				s=new Step(idstep,dureeStep);
				h.put(idstep, s);
			}
			return(s);
		}
		
		// steps contains the step lenghts to consider, associated with the name of the collection of steps to produce for that length
		public static void indexeSteps(String dbName, String colName){ //,HashMap<Long,String> stepNames){
			HashMap<Long,String> stepNames=initiateStepCols(dbName,colName);
			DBCollection col=MongoDB.mongoDB.getCollectionFromDB(dbName,colName);	
			DBCursor cursor = col.find();
			DBObject res;
			Step.reinitSteps();
			try {
				int i=0;
				while(cursor.hasNext()) {
					res=cursor.next();
					long tstamp=Long.valueOf(res.get("timestamp").toString());
					for(Long step:stepNames.keySet()){
						Step s=Step.getStep(tstamp, step);
						s.incNbPosts();
					}
					i++;
					if (i%10000==0){
						System.gc();
						System.out.println(i);
					}

				}
			} finally {
				cursor.close();
			}
			Step.saveSteps(dbName,stepNames);
			
		}
		
		public static void saveSteps(String db){
			saveSteps(db,new HashMap<Long,String>());
		}
		public static void saveSteps(String db,HashMap<Long,String> colNames){
			for(Long d:steps.keySet()){
				String colName="step_"+d;
				if(colNames.containsKey(d)){
					colName=colNames.get(d);
				}
				else{
				    //colName="step_"+colName;
					DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,colName);
					col.drop();
					
				}
				DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,colName);
				HashMap<Long,Step> h=steps.get(d);
				if (h!=null){
					for(Step s:h.values()){
						BasicDBObject obj = new BasicDBObject();
						obj.put("id", s.id);
						obj.put("start", s.debut);
						obj.put("end", s.debut+s.duree);
						obj.put("nbPosts", s.nbPosts);
						col.insert(obj);
					}
				}
				
			}
		}
		public static Step getStepFrom(DBObject res){
			int id=Integer.parseInt(res.get("id").toString());
			long debut=(Long)res.get("start");
			long fin=(Long)res.get("end");
			long duree=fin-debut;
			int nbPosts=Integer.parseInt(res.get("nbPosts").toString());
			Step step=new Step(id,debut,duree,nbPosts);
			return(step);
		}
		public static HashMap<Long,Step> loadSteps(String db,String colName){
			return(loadSteps(db,colName,1));
		}
		public static HashMap<Long,Step> loadSteps(String db,String colName,long minStep){
			HashMap<Long,Step> stps=new HashMap<Long,Step>();
			DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,colName);
			DBCursor cursor = col.find();
			try {
				while(cursor.hasNext()) {
					DBObject res=cursor.next();
					Step s=getStepFrom(res);
					if (s.getID()>=minStep){
						stps.put(s.getID(),s);
					}
				}
			} finally {
				cursor.close();
			}
			return(stps);
		}
		public static long getStepLength(HashMap<Long,Step> stps){
			long ret=0;
			if(stps.size()>0){
				ret=stps.values().iterator().next().getStep();
			}
			return(ret);
		}
		public static long getIdStep(long timestamp,long dureeStep){
			return(((long) (timestamp/(dureeStep*1.0)))+1);
		}
			
		public static void reinitSteps(){
			steps=new HashMap<Long,HashMap<Long,Step>>();
		}
		
		public static HashMap<Long,String> initiateStepCols(String db,String col){
			HashMap<Long,String> stepNames=new HashMap<Long,String>();
			long dur=1;
			String ncol=MongoDB.mongoDB.createCollection(db,"steps","steps seconde (timestep="+dur+") from "+col);
			stepNames.put(dur, ncol);
			dur*=60;
			ncol=MongoDB.mongoDB.createCollection(db,"steps","steps minute (timestep="+dur+")  from "+col);
			stepNames.put(dur, ncol);
			dur*=60;
			ncol=MongoDB.mongoDB.createCollection(db,"steps","steps heure (timestep="+dur+") from "+col);
			stepNames.put(dur, ncol);
			dur*=3;
			ncol=MongoDB.mongoDB.createCollection(db,"steps","steps 3 heures (timestep="+dur+") from "+col);
			stepNames.put(dur, ncol);
			dur*=2;
			ncol=MongoDB.mongoDB.createCollection(db,"steps","steps 6 heures (timestep="+dur+") from "+col);
			stepNames.put(dur, ncol);
			dur*=2;
			ncol=MongoDB.mongoDB.createCollection(db,"steps","steps 12 heures (timestep="+dur+") from "+col);
			stepNames.put(dur, ncol);
			dur*=2;
			ncol=MongoDB.mongoDB.createCollection(db,"steps","steps jour (timestep="+dur+") from "+col);
			stepNames.put(dur, ncol);
			dur*=3;
			ncol=MongoDB.mongoDB.createCollection(db,"steps","steps 3jours (timestep="+dur+") from "+col);
			stepNames.put(dur, ncol);
			dur/=3;
			dur*=7;
			ncol=MongoDB.mongoDB.createCollection(db,"steps","steps semaine (timestep="+dur+") from "+col);
			stepNames.put(dur, ncol);
			dur*=2;
			ncol=MongoDB.mongoDB.createCollection(db,"steps","steps 2 semaines (timestep="+dur+") from "+col);
			stepNames.put(dur, ncol);
			dur/=2;
			dur*=3;
			ncol=MongoDB.mongoDB.createCollection(db,"steps","steps 3 semaines (timestep="+dur+") from "+col);
			stepNames.put(dur, ncol);
			dur/=3;
			dur/=7;
			dur*=30;
			ncol=MongoDB.mongoDB.createCollection(db,"steps","steps mois (timestep="+dur+") from "+col);
			stepNames.put(dur, ncol);
			return(stepNames);
			
		}
		
		public static void main(String[] args){
			if(args.length<2){
				System.out.println("Usage : Step <db> <posts_collection>");
			}
			else{
				indexeSteps(args[0],args[1]);
			}
		}
}
