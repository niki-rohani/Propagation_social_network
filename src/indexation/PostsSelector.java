package indexation;

import java.util.HashMap;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.BasicDBObject;

import core.Post;
import core.User;
import actionsBD.MongoDB;

import java.util.Collections;
import java.util.HashSet;
import java.util.Comparator;
import java.util.ArrayList;

public class PostsSelector {
	public String selectPosts(String db,String col,int nbUsers){
		System.out.println("Selection posts from col filtres selon nbMinUsers="+nbUsers+" et qui a des tags");
		String newCol=MongoDB.mongoDB.createCollection(db,"posts"," posts from "+col+" filtres : posts possedant des tags et appartenant a un des "+nbUsers+" users les plus actifs");
		DBCollection collec=MongoDB.mongoDB.getCollectionFromDB(db,col);	
		DBCursor cursor = collec.find(new BasicDBObject());
		HashMap<String,HashSet<Integer>> users=new HashMap<String,HashSet<Integer>>();
		try {
		    int n=0;
			int nbTraites=0;
			while(cursor.hasNext()) {
				nbTraites++;
			 	 if(nbTraites%1000==0){
			 		 System.out.println(nbTraites+" posts traites, "+n+" avec tags");
			 		 //break;
			 	 }
			  Post.reinitPosts();
			  User.reinitUsers();
			  DBObject res=cursor.next();
		 	  Post p=Post.getPostFrom(res,true);
		 	  int id=p.getID();
		 	  String owner=p.getOwner().getName();
		 	  HashMap<String,String> ptags=p.getTags();
		 	 
		 	  if(ptags.size()==0){
		 		  continue;
		 	  }
		 	  HashSet<Integer> pu=users.get(owner);
		 	  if(pu==null){
		 		  pu=new HashSet<Integer>();
		 		  users.put(owner,pu);
		 		  
		 	  }
		 	  pu.add(id);
		 	  n++;
			}
		}
		finally {
				cursor.close();
		}
		NbUsersComp comp=new NbUsersComp(users);
		ArrayList<String> us=new ArrayList<String>(users.keySet());
		Collections.sort(us,comp);
		BasicDBObject query;
		int nbUs=0;
		for(String user:us){
			
			if(nbUs<nbUsers){
				HashSet<Integer> pu=users.get(user);
				//if(pu.size()>=nbMinPostsPerUser){
				System.out.println("user "+user+" => "+pu.size()+" posts");
					for(Integer id:pu){
						Post.reinitPosts();
						User.reinitUsers();
						//Post p=Post.getPost(id);
						query=new BasicDBObject("id",id);
						Post p=Post.getPostFromDB(db, col, query);
						//System.out.println("post "+p);
						p.indexInto(db, newCol);
					}
				//}
					nbUs++;
			}
			else{
				break;
			}
		}
		
		DBCollection co=MongoDB.mongoDB.getCollectionFromDB(db,newCol);
		co.ensureIndex(new BasicDBObject("tags.tag", 1));
		co.ensureIndex(new BasicDBObject("id", 1));
		
		return newCol;
	}
	
	private class NbUsersComp implements Comparator<String>
	{
		HashMap<String,HashSet<Integer>> users;
		public NbUsersComp(HashMap<String,HashSet<Integer>> users){
			this.users=users;
		}
		public int compare(String un,String deux){
			int x=users.get(un).size();
			int y=users.get(deux).size();
			if(x>y) return -1;
			if(x<y) return 1;
			return 0;
		}
	}
	
	public static void main(String[] args){
		String postsCol="posts_2";
		PostsSelector postSel=new PostsSelector();
		postsCol=postSel.selectPosts("tweet09", postsCol, 5000);
	}
}
