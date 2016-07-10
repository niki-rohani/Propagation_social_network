package experiments;

import java.util.ArrayList;
import java.util.HashMap;

import utils.Keyboard;

import actionsBD.MongoDB;

import com.mongodb.Bytes;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Structure;
import cascades.*;
import cascades.NbUsers;

public class CascadeFeatures implements Experiment {
	ArrayList<CascadeFeatureProducer> featurers;
	
	public CascadeFeatures(ArrayList<CascadeFeatureProducer> featurers){
		this.featurers=featurers;
	}
	public Result go(Structure struct){
		Cascade cascade=(Cascade)struct;
		System.out.println(cascade);
		Result res=new Result(this.getDescription(),cascade.toString());
		ArrayList<Double> features;
		for(CascadeFeatureProducer featurer:featurers){
			System.out.println(featurer);
			features=featurer.getFeatures(cascade);
			int i=1;
			
			for(Double d:features){
				String name=featurer.toString();
				if(features.size()>1){
					name+="_"+i;
				}
				res.addScore(name,d);
				i++;
			}
		}
		return(res);
	}
	
	public String getDescription(){
		return("CascadeFeatures");
	}
	
	public static void main(String[] args){
	    long minStep=1;
		if (args.length>=4){
			minStep=Long.valueOf(args[3]);
		}
		ArrayList<CascadeFeatureProducer> featurers=new ArrayList<CascadeFeatureProducer>();
		featurers.add(new NbPosts());
		featurers.add(new NbUsers());
		HashMap<Long,Step> steps=Step.loadSteps(args[0],args[2],minStep);
		long duree=Step.getStepLength(steps);
		System.out.println("Nb Steps = "+steps.size()+" Duree step = "+duree);
		featurers.add(new NbSteps(steps));
		featurers.add(new CumulativeNbPosts(steps,true));
		featurers.add(new Chi2Feature(steps));
		CascadeFeatures cf=new CascadeFeatures(featurers);
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(args[0],args[1]);
		DBCursor cursor = col.find().addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		//ArrayList<Result> lr=new ArrayList<Result>();
		ResultFile rf=new ResultFile("res.txt");
		try {
			int nb=0;
			while(cursor.hasNext()) {
				DBObject res=cursor.next();
				//System.out.println(res);
				Cascade c=Cascade.getCascadeFrom(res);
				c.removeEarlyPosts(minStep, duree);
				int nbp=c.getPosts().size();
				System.out.println(nbp);
				if (nbp>=5){
					Result r=cf.go(c);
					//lr.add(r);
					System.out.println(c+":"+r);
					rf.append(r);
					//Result.sequentialWrite(r, "res.txt");
					//Clavier.saisirLigne("Continuer");
				}
				nb++;
			}
		}catch(Exception e){	
			System.out.println(e);	
		} finally {
			cursor.close();
		}
	}
}
