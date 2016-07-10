package utils;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBList;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
public class JsonToDBObject {
	    public static DBObject encode(JSONArray a) {
	        BasicDBList result = new BasicDBList();
            for (int i = 0; i < a.size(); ++i) {
                Object o = a.get(i);
                if (o instanceof JSONObject) {
                    result.add(encode((JSONObject)o));
                } else if (o instanceof JSONArray) {
                    result.add(encode((JSONArray)o));
                } else {
                    result.add(o);
                }
            }
            return result;
	      
	    }

	    public static DBObject encode(JSONObject o) {
	        BasicDBObject result = new BasicDBObject();
	       
	      
	        for(Object k:o.keySet()){
	                Object v = o.get(k);
	                if (v instanceof JSONArray) {
	                    result.put(k.toString(), encode((JSONArray)v));
	                } else if (v instanceof JSONObject) {
	                    result.put(k.toString(), encode((JSONObject)v));
	                } else {
	                    result.put(k.toString(), v);
	                }
	          }
	          return result;
	        
	    }
}
