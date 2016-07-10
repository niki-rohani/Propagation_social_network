package indexation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import strLinkUsers.PostsInSameCascade;
import cascades.ArtificialCascade;
import cascades.Cascade;
import core.Link;
import core.Post;
import core.User;
import actionsBD.MongoDB;
import wordsTreatment.WeightComputer;

public class MemeNewIndexer extends DataIndexer {

	
	
	private String fileprefix;
	private String[] fileEnds;
	private int nbUsers;
	private String fileUsers;
	private String cascadesFile ;

	public MemeNewIndexer(String fileprefix, String[] fileEnds, String fileUsers, int nbUsers, String cascadesFile) {
		
		this.fileprefix = fileprefix ;
		this.fileEnds = fileEnds ;
		this.fileUsers= fileUsers ;
		this.nbUsers = nbUsers ;
		this.cascadesFile = cascadesFile ;
	}

	@Override
	public String indexData(String db, String filename, WeightComputer weightComputer, TextTransformer trans) throws IOException {
		MongoDB mongo=MongoDB.mongoDB;
		
		String cascades_all=mongo.createCollection(db, "cascades","toutes les cascades");
		String cascades_train=mongo.createCollection(db, "cascades", "cascades train");
		String cascades_test=mongo.createCollection(db, "cascades","cascades test");
		//String allUsers=mongo.createCollection(db, "users","tous les users");
		String linkedUsers=mongo.createCollection(db,"users","tous les users avec les vrais liens");
		
		
		// Charger les users 
		System.out.println("Chargement des users");
		int n = 0 ;
		
		// Ouverture ;
		BufferedReader lecteur = new BufferedReader(new FileReader(fileUsers)) ;
		
		for(String l = lecteur.readLine() ; l!=null && n<nbUsers  ; l=lecteur.readLine()) {
			String s = l.substring(l.indexOf("http")) ;
			User u = new User(s) ;
			n++ ;
		}
		lecteur.close();
		
		System.out.println((n)+" users chargÃ©s");
		
		// Lire les fichers de log
		String currentUser="" ;
		String currentPost ;
		
		long currentTime ;
		int nbL = 0 ;
		
		
		
		
		// Lire les fichiers de stream pour crÃ©er les liens.
		for(String file : fileEnds) {
			System.out.println("Lecture de "+file);
			lecteur= openGZ(fileprefix+file);
			for(String l = lecteur.readLine() ; l!=null ; l=lecteur.readLine()) {
				if(l.startsWith("P")) { 
					// Il faut trouver l'user qui corresponde
					currentUser=cutUrl(l.substring(l.indexOf("http"))) ;
					if(!User.users.containsKey(currentUser))
						goToNextItem(lecteur) ;
				} else if(l.startsWith("L")) {
					String u = cutUrl(l.substring(l.indexOf("http"))) ;
					if(User.users.containsKey(u) && !u.equals(currentUser) && !User.users.get(u).getSuccesseurs().containsKey(currentUser)) {
						Link link = new Link(User.users.get(u),User.users.get(currentUser),1.0) ;
						User.users.get(u).addLink(link);
						User.users.get(currentUser).addLink(link);
						//System.out.println("lien "+(nbL++) +" crÃ©Ã©." );
					}
						
				}
			}
			lecteur.close();
		}
		
		
		long currentInitTime = Long.MIN_VALUE ;
		
		// Lire les cascades.
		lecteur= openGZ(this.cascadesFile);
		for(int i = 0 ; i<5 ; i++) // Sauter les premieres lignes qui indiquent le format
			lecteur.readLine() ;
		
		HashSet<Post> currentPosts=new HashSet<Post>() ; 
		long currentInit = Long.MAX_VALUE ;
		
		int idP = 0 ;
		int nbc= 0 ;
		for(String l = lecteur.readLine() ; l!=null ; l=lecteur.readLine()) {
			
			if(l.startsWith("\t\t")) {
				//System.out.println(l);
				String[] t = l.split("\t") ;
				//System.out.println(l);
				//System.out.println(t[2]+" "+t[3]+" "+t[4]+" "+t[5]);
				
				String u = cutUrl(t[5]) ;
				if(!User.users.containsKey(u))
					continue ;
				long date = parseDate(t[2]) ;
				Post p = new Post(idP++, User.users.get(u), date) ;
				currentPosts.add(p) ;
				currentInit = Math.min(date, currentInit) ;
			} else if(!l.startsWith("\t")) { // Nouvelle cascade ! Il faut index l'ancienne
				if(currentPosts.size()>1) {
					for(Post p : currentPosts) {
						p.setTimeStamp(p.getTimeStamp()-currentInit+1);
					}
					Cascade cas=new ArtificialCascade(nbc,nbc+"",currentPosts,new HashMap<Integer, Double>()) ;
					nbc++ ;
					cas.indexInto(db, cascades_all);
					if(nbc%10==0) {
						cas.indexInto(db, cascades_test);
					} else {
						cas.indexInto(db, cascades_train);
					}
					System.out.println("cascades "+(nbc-1)+" indexee.");
				}
				currentPosts=new HashSet<Post>() ;
			}
		}
		
		
		for(String u : User.users.keySet()) {
			User.users.get(u).indexInto(db, linkedUsers);
		}
		
		(new PostsInSameCascade(true)).linkUsers(db,cascades_train);
		
			
		//	int postID=0 ;
		
		// lecture du fichier
		//for(String l = lecteur.readLine() ; l!=null ; l=lecteur.readLine()) {
		
		return "" ;
	}
	
	
	
	private void goToNextItem(BufferedReader lecteur) {
		String s="" ;
		do {
			try {
				s = lecteur.readLine() ;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} while(s!=null && !s.equals("")) ;
		
	}
	
	private BufferedReader openGZ(String file) throws IOException {
		FileInputStream fileStream = new FileInputStream(file);
		InputStream gzipStream = new GZIPInputStream(fileStream);
		Reader decoder = new InputStreamReader(gzipStream);
		return new BufferedReader(decoder);
	}

	private String cutUrl(String url) {
		if(url.startsWith("http://")) {
			String s = url.substring(7) ;
			int i = s.indexOf('/') ;
			if(i==-1)
				return url ;
			s=s.substring(0, i+1);
			return "http://"+s ;
		} else {
			return url ;
		}
			
	}
	
	public long parseDate(String datefull) {
		Pattern p = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})") ;

		Matcher m = p.matcher(datefull) ;
		m.matches();
		
		Calendar c = Calendar.getInstance() ;
		c.set(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2))-1,Integer.parseInt(m.group(3)),Integer.parseInt(m.group(4)),Integer.parseInt(m.group(5)),Integer.parseInt(m.group(6)));
		long timestamp=c.getTime().getTime()/1000 ;
		return timestamp ;
			
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
