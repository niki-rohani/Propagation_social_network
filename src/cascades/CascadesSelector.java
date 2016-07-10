package cascades;

import java.util.HashMap;
import java.util.ArrayList;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Environnement;
import core.Post;
import core.User;
import experiments.CascadeFeatures;
import experiments.Result;
import experiments.ResultFile;


public class CascadesSelector {
	public CascadeFeaturer featurers;
	//public ArrayList<Double> bornesInf;
	public CascadesSelector(CascadeFeaturer featurers){ //, ArrayList<Double> bornesInf){
		this.featurers=featurers;
		//this.bornesInf=bornesInf;
		
	}
	
	public String selectCascades(String db, String colCascades, String colSteps){
		return(selectCascades(db,colCascades,colSteps,10));
	}
	public String selectCascades(String db, String colCascades, String colSteps,int minNbPosts){
		return(selectCascades(db,colCascades,colSteps,minNbPosts,1));
	}
	
	public String selectCascades(String db, String colCascades, String colSteps,int minNbPosts, long minStep){
		HashMap<Long,Step> steps=Step.loadSteps(db,colSteps,minStep);
		long duree=Step.getStepLength(steps);
		String desc="cascades selected from "+colCascades+" sur les cascades d au moins "+minNbPosts+" en ignorant les posts avant step="+minStep+" selon collection de steps="+colSteps+" featurers = "+featurers;
		String colOut=MongoDB.mongoDB.createCollection(db,"cascades",desc);
		
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,colCascades);
		DBCursor cursor = col.find().addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		//ArrayList<Result> lr=new ArrayList<Result>();
		int nbok=0;
		try {
			int nb=0;
			while(cursor.hasNext()) {
				if(nb%1000==0){
					System.out.println(nbok+" conserves sur "+nb);
				}
				nb++;
				DBObject res=cursor.next();
				//System.out.println(res);
				Cascade c=Cascade.getCascadeFrom(res);
				c.removeEarlyPosts(minStep, duree);
				int nbp=c.getPosts().size();
				//System.out.println(nbp);
				if (nbp>=minNbPosts){
					
					if (featurers.respectThresholds(c)){
						c.id=nbok+1;
						c.indexInto(db, colOut);
						nbok++;
						System.out.println(nbok+" conserves sur "+nb);
					}
					
				}
				Post.reinitPosts();
				User.reinitUsers();
				
			}
		}catch(Exception e){	
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cursor.close();
		}
		DBCollection outcol=MongoDB.mongoDB.getCollectionFromDB(db,colOut);
		outcol.ensureIndex(new BasicDBObject("id", 1));
		
		return colOut;
		
	}
	
	public String selectCascades(String db, String colCascades, int minNbPosts, long minStamp){
		String desc="cascades selected from "+colCascades+" sur les cascades d au moins "+minNbPosts+" en ignorant les posts avant timestamp="+minStamp+" selon  featurers = "+featurers;
		String colOut=MongoDB.mongoDB.createCollection(db,"cascades",desc);
		
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,colCascades);
		DBCursor cursor = col.find().addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		//ArrayList<Result> lr=new ArrayList<Result>();
		int nbok=0;
		
		try {
			int nb=0;
			while(cursor.hasNext()) {
				if(nb%1000==0){
					System.out.println(nbok+" conserves sur "+nb);
				}
				nb++;
				DBObject res=cursor.next();
				//System.out.println(res);
				Cascade c=Cascade.getCascadeFrom(res);
				c.removeEarlyPosts(minStamp, 1);
				int nbp=c.getPosts().size();
				//System.out.println(nbp);
				if (nbp>=minNbPosts){
					
					if (featurers.respectThresholds(c)){
						c.id=nbok+1;
						c.indexInto(db, colOut);
						
						nbok++;
						System.out.println(nbok+" conserves sur "+nb);
					}
					
				}
				Post.reinitPosts();
				User.reinitUsers();
				
			}
		}catch(Exception e){	
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cursor.close();
		}
		DBCollection outcol=MongoDB.mongoDB.getCollectionFromDB(db,colOut);
		outcol.ensureIndex(new BasicDBObject("id", 1));
		
		return colOut;
		
	}
	
	public static void main(String[] args){
		Environnement.setVerbose(1);
		
		int minNbPosts=10;
		if (args.length>=4){
			minNbPosts=Integer.parseInt(args[3]);
		}
		
		long minStep=1;
		if (args.length>=5){
			minStep=Long.valueOf(args[4]);
		}
		
		
		
		if(args.length<3){
			System.out.println("Usage : CascadesSelector <db> <cascades_collection> <steps_collection>");
		}
		else{
			// A modifier pour filtrer autrement.
			ArrayList<CascadeFeatureProducer> featurers=new ArrayList<CascadeFeatureProducer>();
			featurers.add(new NbUsers());
			HashMap<Long,Step> steps=Step.loadSteps(args[0],args[2],minStep);
			long duree=Step.getStepLength(steps);
			System.out.println("Nb Steps = "+steps.size()+" Duree step = "+duree);
			//featurers.add(new NbSteps(steps));
			//featurers.add(new CumulativeNbPosts(steps,true));
			//featurers.add(new Chi2Feature(steps));
			ArrayList<Double> thresholds=new ArrayList<Double>();
			thresholds.add(5.0);
			//thresholds.add(2.0);
			//thresholds.add(1500.0);
			//thresholds.add(100.0);
			
			CascadeFeaturer cf=new CascadeFeaturer(featurers,thresholds);
			CascadesSelector cp=new CascadesSelector(cf);
			cp.selectCascades(args[0], args[1], args[2],minNbPosts,minStep);
		}
		
	}
	
		
}
