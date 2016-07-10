package strLinkUsers;

import java.util.HashMap;
import java.util.HashSet;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import actionsBD.MongoDB;
import core.Post;
import core.User;
import core.Link;
import cascades.Cascade;

public class PostsInSameCascade extends StrLinkUsers {
	boolean oriented; // if true => links are oriented according to order of users in the cascade (link from u1 to u2 if first post from u1 has timestamp < which of u2 in the cascade)
	
	public PostsInSameCascade(){
		this(false);
	}
	public PostsInSameCascade(boolean oriented){
		this.oriented=oriented;
	}
	
	@Override
	public String linkUsers(String db, String col) {
		String desc="users_from_"+col+"_PostsInSameCascade";
		if (oriented){
			desc+="Oriented";
		}
		System.out.println("LinkUsers "+desc);
		String outCol=MongoDB.mongoDB.createCollection(db,"users",desc);
		//String outCol="users_1";
		//HashMap<Integer,String> users=new HashMap<Integer,String>();
		HashSet<Cascade> cascades=Cascade.getCascadesFromDB(db, col);
		int nbusers=User.users.size();
		int nbt=0;
		for(String user:User.users.keySet()){
			if (nbt%1000==0){
				System.gc();
			}
			HashMap<String,Integer> succs=new HashMap<String,Integer>();
			HashMap<String,Integer> preds=new HashMap<String,Integer>();
			User us=User.getUser(user);
			/*if (us.loadLinksFrom(db, outCol)){
				us.reinitLinks();
				nbt++;
				System.out.println(nbt+" users traites sur "+nbusers+" : "+succs.size()+" + "+preds.size());
				System.out.println(user+" ok");
				
				continue;
			}*/
			for(Cascade c:cascades){
				int idc=c.getID();
				HashSet<Post> posts=c.getPosts();
				HashMap<String,Long> vus=new HashMap<String,Long>();
				for(Post p:posts){
					String idu=p.getOwner().getName();
					long t=p.getTimeStamp();
					if ((!vus.containsKey(idu)) || (vus.get(idu)>t)){
						vus.put(idu,t);
					}
				
				}
				if (vus.containsKey(user)){
					long tu=vus.get(user);
					for(String user2:vus.keySet()){
						if (user2!=user){
							long t=vus.get(user2);
							if ((t>=tu) || (!oriented)){
								int nb=0;
								if (succs.containsKey(user2)){
									nb=succs.get(user2);
								}
								succs.put(user2, nb+1);
							}
							if ((t<=tu) && (oriented)){
								int nb=0;
								if (preds.containsKey(user2)){
									nb=preds.get(user2);
								}
								preds.put(user2, nb+1);
							}
						}
					}
				}
			}
			for(String u:succs.keySet()){
				int val=succs.get(u);
				User us2=User.getUser(u);
				Link l=new Link(us,us2,val*1.0);
				us.addLink(l);
				if(!oriented){
					l=new Link(us2,us,val*1.0);
					us.addLink(l);
				}
			}
			if(oriented){
			  for(String u:preds.keySet()){
				int val=preds.get(u);
				User us2=User.getUser(u);
				Link l=new Link(us2,us,val*1.0);
				us.addLink(l);
			  }
			}
			nbt++;
			System.out.println(nbt+" users traites sur "+nbusers+" : "+succs.size()+" + "+preds.size());
			us.indexInto(db, outCol);
			us.reinitLinks();
			
		}
		DBCollection outcol=MongoDB.mongoDB.getCollectionFromDB(db,outCol);
		outcol.ensureIndex(new BasicDBObject("name", 1));
		return outCol;

	}
	
	public static void main(String[] args){
		if(args.length<2){
			System.out.println("Usage : PostsInSameCascade <db> <cascades_collection>");
		}
		else{
			(new PostsInSameCascade(true)).linkUsers(args[0],args[1]);
			
		}
	}

}
