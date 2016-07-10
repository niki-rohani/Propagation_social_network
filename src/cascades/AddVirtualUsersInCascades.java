package cascades;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;
import core.User;
import actionsBD.MongoDB;

import java.util.HashMap;
import java.util.HashSet;
public class AddVirtualUsersInCascades {

	public static String addUsers(String db,String cascadesCol,int nb){
		String desc="cascades from "+cascadesCol+" with "+nb+"virtual users";
		String colOut=MongoDB.mongoDB.createCollection(db,"cascades",desc);
		
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,cascadesCol);
		long nbc=col.count();
		HashMap<Long,Integer> virtuals=new HashMap<Long,Integer>();
		for(int i=0;i<nb;i++){
			long x=(long)Math.floor(Math.random()*nbc);
			Integer nbx=virtuals.get(x);
			nbx=(nbx==null)?0:nbx;
			virtuals.put(x, nbx+1);
		}
        DBCursor cursor = col.find();
        Post p=null;
        long nbv=0;
        int nbnew=1;
        try {
            while(cursor.hasNext()) {
                DBObject res=cursor.next();
                Cascade c=Cascade.getCascadeFrom(res);
                c=new ArtificialCascade(c);
                if(virtuals.containsKey(nbv)){
                	HashSet<Post> posts=c.getPosts();
                	long maxT=1;
                	for(Post post:posts){
                		long t=post.getTimeStamp();
                		if(t>maxT){
                			maxT=t;
                		}
                	}
                	long nbx=virtuals.get(nbv);
                	for(int j=0; j<nbx;j++){
                		long nt=(long)Math.floor(Math.random()*maxT)+1;
                    	c.addPost(new Post("",new User("virtual_"+nbnew),nt,new HashMap<Integer,Double>()));
                    	nbnew++;
                	}
                	
                }
                c.indexInto(db, colOut);
                User.reinitAllPosts(); // Pour alleger, on supprime les textes des posts dont on ne se sert pas ici
                Post.reinitPosts();
                //System.out.println("Cascade "+ic+" creee");
                nbv++;
            }
        } finally {
            cursor.close();
        }
		
		return colOut;
	}
	
	public static void main(String[] args){
		addUsers(args[0],args[1],Integer.parseInt(args[2]));
	}
}
