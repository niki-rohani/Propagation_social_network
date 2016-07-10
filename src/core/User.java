package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import experiments.Result;
import java.util.HashSet;

import actionsBD.MongoDB;
import com.mongodb.WriteResult;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
public class User extends Text {
	public static final long serialVersionUID=1;
	//public static HashMap<Integer,User> users=new HashMap<Integer,User>(); 
	public static HashMap<String,User> users=new HashMap<String,User>(); 
	private HashMap<Integer,Post> posts;
	//private HashMap<User,Double> relations;
	//private HashSet<Link<User>> relations=null;
	//private HashMap<Integer,Link<User>> successeurs=null;
	//private HashMap<Integer,Link<User>> predecesseurs=null;
	
	
	//public User(int id,String name, HashMap<Integer,Double> poids){
	public User(String name, HashMap<Integer,Double> poids){
			
		super(name,poids);
		if (name.length()>0){
			if (users.containsKey(name)){
				throw new RuntimeException("User "+name+" existe deja => utiliser getUser");  //nom du nouvel user = "+this.id);
			}
	
		}
		
		/*relations=new HashSet<Link<User>>();
		successeurs=new HashMap<Integer,Link<User>>();
		predecesseurs=new HashMap<Integer,Link<User>>();*/
		posts=new HashMap<Integer,Post>();
		
		users.put(name, this);
	}
		
	// poids refers to the user's profile
	/*public User(String name, HashMap<Integer,Double> poids){
		this(-1,name,poids);
		//relations=new HashMap<User,Double>();
		
		
	}*/
	
	
	
	public User(String name){
		this(name,null);
	}
	
	/*public User(int id,String name){
		this(id,name,null);
	}*/
	/*public User(int id){
		this(id," ");
	}*/
	
	
	
	
	public static User getUser(String name){
		if (users.containsKey(name)){return(users.get(name));}
		User user=new User(name);
		//System.out.println("Oh No "+id);
		return(user);
	}
	
	@Override
	public HashMap<Integer,Double> getWeights(){
		if(weights==null){
			weights=Text.getCentralWeights(new ArrayList<Text>(posts.values()));
		}
		return(weights);
	}
	
	public void reinitPosts(){
		//System.out.println("reinitPosts");
		this.posts=new HashMap<Integer,Post>();
	}
	public static void reinitUsers(){
		//System.out.println("reinitUsers");
		users=new HashMap<String,User>();
	}
	
	/*private void setID(int id) throws Exception{
		if (users.containsKey(id)){
			throw new Exception("User "+id+" existe deja");
		}
		users.remove(this.id);
		this.id=id;
		users.put(id, this);
	}*/
	
	public void addPost(Post p){
		posts.put(p.getID(), p);
	}
	
	public String getName(){
		return(titre);
	}
	
	public String toString(){
		String s="";
		s=name; /*+" : "+getName()+", Posts : \n";
		for(Post p:posts.values()){
			s+="\t"+p+"\n";
		}*/
		return(s);
	}
	
	public void serialize(String filename) throws IOException{
		
		File f=new File(filename);
	    File p=f.getParentFile();
	    if (!p.exists()){
	    	p.mkdirs();
	    }
		//FileOutputStream fos = new FileOutputStream(rep+"/"+toString()+".result");
		FileOutputStream fos = new FileOutputStream(filename);
		// creation d'un "flux objet" avec le flux fichier
		ObjectOutputStream oos= new ObjectOutputStream(fos);
		try {
			// serialisation : ecriture de l'objet dans le flux de sortie
			oos.writeObject(this); 
			
			// on vide le tampon
			oos.flush();
			System.out.println("le result "+toString()+" a ete serialise");
		} finally {
			//fermeture des flux
			try {
				oos.close();
			} finally {
				fos.close();
			}
		}
	
	}

	public static User deserialize(String fileName) throws IOException{
		User ret=null;
		try{
		
			// ouverture d'un flux d'entree depuis le fichier "personne.serial"
			FileInputStream fis = new FileInputStream(fileName);
			// creation d'un "flux objet" avec le flux fichier
			ObjectInputStream ois= new ObjectInputStream(fis);
			try {	
				// deserialisation : lecture de l'objet depuis le flux d'entree
				ret = (User) ois.readObject();
				if (Text.nb_texts<=ret.getID()) Text.nb_texts=ret.getID()+1;
			} finally {
				// on ferme les flux
				try {
					ois.close();
				} finally {
					fis.close();
				}
			}
		}
		catch(ClassNotFoundException e){
			e.printStackTrace();
			throw new IOException("Probleme classe");
		}
		return(ret);
	}
	
	public void indexInto(String db,String collection){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		BasicDBObject obj = new BasicDBObject();
		obj.put("name", name);
		//obj.put("name",getName());
		ArrayList<BasicDBObject> liste=new ArrayList<BasicDBObject>();
		if(successeurs!=null){
		 for(Link link:successeurs.values()){
			User u=(User)link.getNode2();
			//int idu=u.getID();
			String idu=u.getName();
			double val=link.getVal();
			BasicDBObject ti=new BasicDBObject();
			ti.put("user",idu);
			
			ti.put("val", val);
			liste.add(ti);
		 } 
		}
		obj.put("succs",liste);
		liste=new ArrayList<BasicDBObject>();
		if(predecesseurs!=null){
			
		  for(Link link:predecesseurs.values()){
			User u=(User)link.getNode1();
			String idu=u.getName();
			double val=link.getVal();
			BasicDBObject ti=new BasicDBObject();
			ti.put("user",idu);
			
			ti.put("val", val);
			liste.add(ti);
		  }
		}
		obj.put("preds",liste);
		//System.out.println(db+":"+collection);
		//System.out.println(obj);
		//System.out.println(
		WriteResult wr=col.insert(obj);//.getError()
		if (wr.getError()!=null){
			throw new RuntimeException(wr.getError());
		}
		
		//);
	}
	
	public boolean loadLinksFrom(String db,String collection){
		return(loadLinksFrom(db,collection,-1));
	}
	
	public boolean loadLinksFrom(String db,String collection,double thresholdVal){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		BasicDBObject query=new BasicDBObject();
		query.put("name", name);
		DBCursor cursor = col.find(query);
		Post p=null;
		boolean ret=false;
		try {
			if(cursor.hasNext()) {
				DBObject res=cursor.next();
				ArrayList<DBObject> succs=(ArrayList<DBObject>)res.get("succs");
				for(DBObject obj:succs){
					String idu=obj.get("user").toString();
					double val=Double.valueOf(obj.get("val").toString());
					if ((thresholdVal<0) || (val>=thresholdVal)){
						User u=User.getUser(idu);
						Link l=new Link(this,u,val);
						addLink(l,true);
					}
				}
				ArrayList<DBObject> preds=(ArrayList<DBObject>)res.get("preds");
				for(DBObject obj:preds){
					String idu=obj.get("user").toString();
					double val=Double.valueOf(obj.get("val").toString());
					if ((thresholdVal<0) || (val>=thresholdVal)){
						User u=User.getUser(idu);
						Link l=new Link(u,this,val);
						addLink(l,true);
					}
				}
				ret=true;
			}
		} finally {
			cursor.close();
		}
		return(ret);
	}
	
	public static boolean loadAllLinksFrom(String db,String collection) {
		System.out.println("Chargement des liens");
		User u ;
		int nb=0;
		for(String i : users.keySet()) {
			if(nb%100==0){
				System.out.print(nb+" ");
			}
			u = users.get(i) ;
			if(!u.loadLinksFrom(db,collection))
				return false ;
		    nb++;
		    
		}
		return true ;
	}
	public static boolean loadUsersFrom(String db,String collection){
		return(loadUsersFrom(db,collection,-1));
	}
	
	public static boolean loadUsersFrom(String db,String collection, int nbMax) {
		System.out.println ("Chargement des utilisateurs");
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		List<String> names= (List<String>)col.distinct("name");
	    Collections.shuffle(names);
	    int nb=0;
		for(String name:names){
//			if(nb%100==0){
//				System.out.println(nb+" users charges");
//			}
			if((nbMax>=0) && (nb>=nbMax)){
				break;
			}
			User.getUser(name);
			nb++;
		}
		return(true);
	}
	
	public static boolean reinitAllLinks() {
		//System.out.println("User.reinitAllLinks");
		User u ;
		for(String i : users.keySet()) {
			u = users.get(i) ;
			u.reinitLinks();
		}
		return true ;
	}
	public static boolean reinitAllPosts() {
		//System.out.println("User.reinitAllPosts");
		User u ;
		for(String i : users.keySet()) {
			u = users.get(i) ;
			u.posts=new HashMap<Integer,Post>();
		}
		return true ;
	}
	
	public HashMap<Integer,Post> getPosts(){
		return posts;
	}
}
