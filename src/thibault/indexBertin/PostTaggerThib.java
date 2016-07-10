package thibault.indexBertin;
import java.util.HashSet;

import actionsBD.MongoDB;

import com.mongodb.Bytes;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;
import core.User;
// Strategie de mise en relation de posts
public abstract class PostTaggerThib {
	public void tagPost(Post post){
		HashSet<Post> posts=new HashSet<Post>();
		posts.add(post);
		tagPosts(posts);
	}
	public abstract void tagPosts(HashSet<Post> posts); 

	public String tagCollection(String db,String inputCol){
		return(tagCollection(db,inputCol,true,true,true));	
	}
	
	
	
	// Add tags to collection inputCol and save resulting posts in outputCol  
	// If supOldTag = true => remove old tags
	// If removeText = true => remove text to get a lighter collection
	// If videMem = true => clear users and posts static lists after each post consideration
	public String tagCollection(String inputDB, String inputCol, boolean supOldTags,boolean removeText,boolean videMem){
		
		MongoDB m = new MongoDB("localhost");
		
		String outputCol=m.createCollection(inputDB,"posts"," posts from "+inputCol+" taggues selon "+this.toString()+" supOldTags="+supOldTags+", removeText="+removeText);
		String outputDB=inputDB;
		
		BasicDBObject query = new BasicDBObject();
		DBCollection col=m.getCollectionFromDB(inputDB,inputCol);
		//HashSet<Post> posts=new HashSet<Post>();
		DBCursor cursor=null; 
		try {
			cursor= col.find(query).addOption(Bytes.QUERYOPTION_NOTIMEOUT);
			int nb=0;
			while(cursor.hasNext()) {
				//System.out.println("next");
				DBObject res=cursor.next();
				//System.out.println("ok next");
				PostThib p=PostThib.getPostThibFrom(res);
				
				if (p!=null){
					//System.out.println(p);
					if (supOldTags){
						p.videTags();
					}
					tagPost(p);
					if (removeText){
						p.setTexte(" ");
					}
					p.indexIntoBis(m,outputDB,outputCol);
					
				}
				if (videMem){
					PostThib.reinitPosts();
					User.reinitUsers();
				}
				nb++;
				if((nb%100)==0){System.out.println(nb+" traites");}
				//break;
			}
			
			DBCollection outcol=m.getCollectionFromDB(outputDB,outputCol);
			outcol.ensureIndex(new BasicDBObject("tags.tag", 1));
			outcol.ensureIndex(new BasicDBObject("id", 1));
				
		} finally {
			if (cursor!=null){
				cursor.close();
			}
		}
		return outputCol;
	}
}
