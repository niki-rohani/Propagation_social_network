package thibault.indexBertin;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;
import actionsBD.MongoDB;
public abstract class TextTransformerThib {
	public abstract void learn();
	public abstract HashMap<Integer,Double> transform(HashMap<Integer,Double> text);
	public abstract HashMap<Integer,Double> transformBis(MongoDB m,HashMap<Integer,Double> text);
	/*public static void main(String[] args){
		IDFPruner pr=new IDFPruner(2000,"tweet09","stems_1",1);
		pr.learn();
	}*/
	public String toString(){
		return this.getClass().toString();
	}
}

class NoTransform extends TextTransformerThib{
	public void learn(){}
	public HashMap<Integer,Double> transform(HashMap<Integer,Double> text){
		return text;
	}
	@Override
	public HashMap<Integer, Double> transformBis(MongoDB m,
			HashMap<Integer, Double> text) {
		// TODO Auto-generated method stub
		return null;
	}
}
 