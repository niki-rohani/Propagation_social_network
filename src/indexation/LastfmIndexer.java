package indexation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import strLinkUsers.PostsInSameCascade;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;

import cascades.ArtificialCascade;
import cascades.Cascade;
import core.Post;
import core.User;
import actionsBD.MongoDB;
import wordsTreatment.WeightComputer;

public class LastfmIndexer extends DataIndexer {

	private boolean indexArtist;

	public LastfmIndexer(boolean indexArtist) {
		this.indexArtist = indexArtist ;
	}
	
	@Override
	public String indexData(String db, String filename,
			WeightComputer weightComputer, TextTransformer trans)
			throws IOException {
		
		String collection=MongoDB.mongoDB.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
		System.out.println("Indexation "+filename);
		//ArrayListStruct<User> ret=new ArrayListStruct<User>();
		
		HashMap<String,User> users=new HashMap<String,User>();
		HashSet<Post> posts=new HashSet<Post>();
		HashSet<String> currentUsers = new HashSet<String>() ;
		HashSet<Cascade> cascades = new HashSet<Cascade>() ;
		Cascade cas ;
		
		int postid = 0 ;
		int nbc = 0 ;
		String currentSong = "" ;
		
		MongoDB mongo = new MongoDB() ;
		String cascades_all=mongo.createCollection(db, "cascades","toutes les cascades");
		String cascades_train=mongo.createCollection(db, "cascades", "cascades train");
		String cascades_test=mongo.createCollection(db, "cascades","cascades test");
		//String allUsers=mongo.createCollection(db, "users","tous les users");
		
		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		try{
			String ligne;
			int nbl=0;
			while((ligne=lecteur.readLine())!=null){
				nbl++;
			}
			System.out.println(nbl+" lignes");
			lecteur.close();
			lecteur=new BufferedReader(new FileReader(filename));
			int l = 0 ;
			
			while(((ligne=lecteur.readLine())!=null)){ // && (nb<10000)){
				String[] li=ligne.split("\t");
				String userid = li[0];
				String artistid=li[2];
				String songid=li[4];
				String datefull=li[1] ;
				
				if(indexArtist)
					songid=artistid ;
				
				
				if((l++)%1000==0)
					System.out.println("Processing line "+l+"/"+nbl );
				
				
				if(currentSong=="") {
					currentSong=songid;
				} else if (!currentSong.equals(songid)) { // Nouvelle chanson.
					// creer une cascade.
					//System.out.println(songid);
					//System.out.println("Ajout cascade de taille "+posts.size());
					cas=new ArtificialCascade(nbc,nbc+"",posts,new HashMap<Integer, Double>()) ;
					nbc++ ;
					cas.indexInto(db, cascades_all);
					if(posts.size()>1) {
						if(nbc%10==0) {
							cas.indexInto(db, cascades_test);
						} else {
							cas.indexInto(db, cascades_train);
						}
					}
					// pret pour nouvelle chanson
					posts.clear();
					currentSong=songid;
					currentUsers.clear();
				}
				
				// convertir la date en timestamp.
				Pattern p = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})Z") ;
				try {
					Matcher m = p.matcher(datefull) ;
					m.matches();
					
					Calendar c = Calendar.getInstance() ;
					c.set(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2))-1,Integer.parseInt(m.group(3)),Integer.parseInt(m.group(4)),Integer.parseInt(m.group(5)),Integer.parseInt(m.group(6)));
					long timestamp=c.getTime().getTime()/1000 ;
					
					// Ajouter user et post
					if(!users.containsKey(userid))
						users.put(userid, new User(userid)) ;
					
					if(currentUsers.add(userid)) {
						Post post = new Post(postid++, users.get(userid), timestamp) ;
						posts.add(post) ;
					}
				}catch(Exception e) {
					e.printStackTrace();
					System.out.println("ERREUR LIGNE "+l);
					System.out.println(ligne);
					System.out.println(datefull);
					return null ;
				}
			}
			(new PostsInSameCascade(true)).linkUsers(db,cascades_train);
			/*for(String u : users.keySet()) {
				users.get(u).indexInto(db, allUsers);
			}*/
			System.out.println("Creation indexs");
			DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,cascades_all);
			col.ensureIndex(new BasicDBObject("id", 1));
		} finally{
			lecteur.close();
		}
			
			
		
		return  null ;
	}

	@Override
	public String toString() {
		return "lastfmIndexer";
	}

}
