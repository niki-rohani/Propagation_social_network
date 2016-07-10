package indexation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import strLinkUsers.PostsInSameCascade;
import cascades.ArtificialCascade;
import cascades.Cascade;
import core.Link;
import core.Post;
import core.User;
import actionsBD.MongoDB;
import wordsTreatment.WeightComputer;

public class WeiboIndexer extends DataIndexer {

	private String userFile ;
	private int userThreshold ;

	HashMap<Integer,HashSet<Post>> cascades= new HashMap<Integer,HashSet<Post>>() ; // Id cascades -> set de post
	HashMap<Integer,Integer> postCascades = new HashMap<Integer, Integer>() ; // Id post -> id cascades
	HashMap<String,Integer> midToid = new HashMap<String, Integer>() ;
	HashMap<Integer,String> retMidof = new HashMap<Integer, String>() ;
	
	public WeiboIndexer(String users,int userThresh) throws IOException {
		this.userFile= users ;
		this.userThreshold = userThresh ;
		BufferedReader lecteur=new BufferedReader(new FileReader(userFile));
		
		for(String l = lecteur.readLine() ; l!=null ; l=lecteur.readLine()) {
			String[] s = l.split("\\s") ;
			if(Integer.parseInt(s[0])>userThreshold && s[1].startsWith("u")) {
				User u = new User(s[1]) ;
			}
		}
	}
	
	@Override
	public String indexData(String db, String filename, WeightComputer weightComputer, TextTransformer trans) throws IOException {
		
		String collection=MongoDB.mongoDB.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
		System.out.println("Indexation "+filename);
		
		MongoDB mongo=MongoDB.mongoDB;
		
		String cascades_all=mongo.createCollection(db, "cascades","toutes les cascades");
		String cascades_train=mongo.createCollection(db, "cascades", "cascades train");
		String cascades_test=mongo.createCollection(db, "cascades","cascades test");
		//String allUsers=mongo.createCollection(db, "users","tous les users");
		String linkedUsers=mongo.createCollection(db, "users","tous les users avec les vrais liens");
		
		
		//BufferedReader lecteur=new BufferedReader(new FileReader(userFile));
		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		int postID=0 ;
		
		// lecture du fichier
		for(String l = lecteur.readLine() ; l!=null ; l=lecteur.readLine()) {
			String[] li=l.split(",");
			String mId=li[0] ;
			String retmid=li[1] ;
			String uId=li[2] ;
			//String date = li[li.length-3] ;
			//if(!uId.startsWith("u"))
			//	continue ;
			if(!User.users.containsKey(uId))
				continue ;
			
			// Extraire le TimeStamp.
			Pattern p = Pattern.compile(",(\\d{4})-(\\d{2})-(\\d{2})\\s(\\d{2}):(\\d{2}):(\\d{2}),") ;
			Matcher m = p.matcher(l) ;
			if(!m.find()) {
				continue ;
				//System.out.println(l);
			}
			Calendar c = Calendar.getInstance() ;
			c.set(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2))-1,Integer.parseInt(m.group(3)),Integer.parseInt(m.group(4)),Integer.parseInt(m.group(5)),Integer.parseInt(m.group(6)));
			long timestamp=c.getTime().getTime()/1000 ;
			
			Post post = new Post(postID, User.users.get(uId), timestamp) ;
			midToid.put(mId, postID) ;
			if(!retmid.equals(""))
				retMidof.put(postID, retmid) ;
			postCascades.put(postID, postID) ; 
			postID++ ;
		}
		lecteur.close();
		int NbPost = postID ;
		
		// Extraction des cascades
		for(int i = 0 ; i <NbPost ; i++) {
			Post p1 = Post.getPost(i) ;
			
			if(retMidof.containsKey(i)) {
				if(!midToid.containsKey(retMidof.get(i)))
					continue ;
				int j = midToid.get(retMidof.get(i)) ;
				Post p2 = Post.getPost(j) ;
				Link  l = new Link(p1.getOwner(),p2.getOwner(),1) ;
				p1.getOwner().addLink(l);
				p2.getOwner().addLink(l);
				
				int c1 = postCascades.get(i) ;
				int c2 = postCascades.get(j) ;
				
				if(!cascades.containsKey(c1)) {
					cascades.put(c1, new HashSet<Post>()) ;
					cascades.get(c1).add(p1) ;
				}
				if(!cascades.containsKey(c2)) {
					cascades.put(c2, new HashSet<Post>()) ;
					cascades.get(c2).add(p2) ;
				}
				if(c2<c1) { // Met toujours dans la cascade de plus petit ID.
					postCascades.put(i,c2) ;
					HashSet<Post>set1 = cascades.get(c1) ;
					HashSet<Post>set2 = cascades.get(c2) ;
					set2.addAll(set1) ;
					cascades.remove(c1) ;
				} else {
					postCascades.put(j,c1) ;
					HashSet<Post>set1 = cascades.get(c1) ;
					HashSet<Post>set2 = cascades.get(c2) ;
					set1.addAll(set2) ;
					cascades.remove(c2) ;
				}
			} else {
				int c1 = postCascades.get(i) ;
				if(!cascades.containsKey(c1)) {
					cascades.put(c1, new HashSet<Post>()) ;
					cascades.get(c1).add(p1) ;
				}
			}
			
		}
		
		for(String u : User.users.keySet()) {
			User.users.get(u).indexInto(db, linkedUsers);
		}
		int meansize = 0 ;
		int nbc = 0 ;
		for(Integer i : cascades.keySet()) {
			HashSet<Post> c = cascades.get(i) ;
			if(c.size()<2)
				continue ;
			meansize+=c.size();
			nbc++;
			long minTime = Long.MAX_VALUE ;
			for(Post p : c ) {
				minTime = Math.min(p.getTimeStamp(), minTime) ;
			}
			for(Post p : c ) {
				p.setTimeStamp(p.getTimeStamp()-minTime+1);
			}
			
			ArtificialCascade cas = new ArtificialCascade(i, "cascade_"+i, c, new HashMap<Integer, Double>()) ;
			cas.indexInto(db, cascades_all);
			if(i%10==0) {
				cas.indexInto(db, cascades_test);
			} else {
				cas.indexInto(db, cascades_train);
			}
		}
		
		System.out.println(nbc+" cascades inseree. Taille moyenne : "+(double)meansize/(double)nbc);
		User.reinitAllLinks() ;
		(new PostsInSameCascade(true)).linkUsers(db,cascades_train);
		/*for(String u : User.users.keySet()) {
			User.users.get(u).indexInto(db, allUsers);
		}*/
		
		
		return null;
	}

	@Override
	public String toString() {
		return "WeiboIndexer";
	}

}
