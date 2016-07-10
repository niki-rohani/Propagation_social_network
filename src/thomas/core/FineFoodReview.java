package thomas.core;

import java.util.ArrayList;
import java.util.HashMap;

import actionsBD.MongoDB;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import core.Post;
import core.User;

public class FineFoodReview extends Post{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String productId;
	protected String helpfulness;
	protected short score;
	//summary  = titre	

	public FineFoodReview(int id, String titre, User owner, long timestamp,	HashMap<Integer, Double> poids, String productId, String helpfulness, short score) {
		super(id, titre, owner, timestamp, poids);
		this.productId = productId;
		this.helpfulness = helpfulness;
		this.score = score;
	}
	
	public FineFoodReview(String texte, User owner, Long timestamp,
			HashMap<Integer, Double> poids) {
		super(texte, owner, timestamp, poids);

	}

	public FineFoodReview(int id, String titre, HashMap<Integer, Double> poids,
			String productId, short score) {
		super(id, titre, poids);
		this.score=score;
		this.productId=productId;
	}

	@Override
	public void indexInto(String db,String collection){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		BasicDBObject obj = new BasicDBObject();
		obj.put("id", id);
		obj.put("title", titre);
		obj.put("user", owner.getID());
		obj.put("productId", productId);
		obj.put("helfullness", helpfulness);
		obj.put("score", score);
		obj.put("timestamp", timestamp);

		HashMap<Integer,Double> w=getWeights();
		ArrayList<BasicDBObject> poids=new ArrayList<BasicDBObject>(); 
		for(Integer i:w.keySet()){
			BasicDBObject wi=new BasicDBObject();
			wi.put(i+"", w.get(i));
			poids.add(wi);
		}
		obj.put("weights",poids);
		col.insert(obj);
	}

	
	public void indexSimplyInto(String db,String collection){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		BasicDBObject obj = new BasicDBObject();
		obj.put("id", id);
		obj.put("title", titre);
		obj.put("productId", productId);
		obj.put("score", score);

		HashMap<Integer,Double> w=getWeights();
		ArrayList<BasicDBObject> poids=new ArrayList<BasicDBObject>(); 
		for(Integer i:w.keySet()){
			BasicDBObject wi=new BasicDBObject();
			wi.put(i+"", w.get(i));
			poids.add(wi);
		}
		obj.put("weights",poids);
		col.insert(obj);
	}

}
