package indexation;


/**
 * TODO if needed (for considering the memetracker cluster file)
 */
import java.util.ArrayList;
import java.util.List;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import cascades.Cascade;
import cascades.CascadesProducer;

public class MemetrackerCascadesProducer extends CascadesProducer {
	String file;
	public MemetrackerCascadesProducer(String filename){
		file=filename;
	}
	public String produceCascades(String db, String postsCollection,BasicDBObject query,int debut, int nb, int nbMinUsers){
		System.out.println("Produce cascades from "+db+":"+postsCollection+" nbMinUsers="+nbMinUsers);
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,postsCollection);	
		String outputCol=MongoDB.mongoDB.createCollection(db,"cascades","memetracker cascades from "+db+":"+postsCollection+"  nbMinUsers="+nbMinUsers);
		
		
		
		return(outputCol);
	}
}
