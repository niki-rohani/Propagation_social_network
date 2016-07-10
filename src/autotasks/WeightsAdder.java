package autotasks;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import wordsTreatment.WeightComputer;
import actionsBD.MongoDB; 

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

// Lit un collec, extrait une propriete, applique un WeightComputer et cree une nouvelle collection ou la propriete est remplace par ce weight.
public class WeightsAdder {
	
	protected String dataBase ; 
	protected String intable ; 
	protected String outtable ;
	protected WeightComputer wc ;
	protected String inField ;
	protected String outField ;
	
	public WeightsAdder(String dataBase, String intable, String outtable, WeightComputer wc, String inField, String outField) {
		super();
		this.dataBase = dataBase;
		this.intable = intable;
		this.outtable = outtable;
		this.wc = wc;
		this.inField = inField;
		this.outField = outField;
	}
	
	public void startTask() throws UnknownHostException {
		
		DB mongoDB = (new Mongo("132.227.201.134")).getDB(dataBase) ;
		DBCollection inColl =  mongoDB.getCollection(intable) ;
		DBCollection outColl =  mongoDB.getCollection(outtable) ;
		
		DBCursor cursorIn = inColl.find() ;
		Iterator<DBObject> iterIn = cursorIn.iterator() ;
		
		while(iterIn.hasNext()) {
			DBObject obj = iterIn.next() ;
			String content = (String) obj.get(inField) ;			
			HashMap<Integer,Double> v = wc.getWeightsForIds(content) ;
			
			BasicDBObject outVersion = new BasicDBObject(v) ;
			BasicDBObject newObj = new BasicDBObject();
			newObj.putAll(obj);
		}
		
	}
	
	public static void main (String[] args) {
		WeightsAdder a = new WeightsAdder("digg","raw","tfidf",null,null,null) ;
		try {
			a.startTask() ;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
