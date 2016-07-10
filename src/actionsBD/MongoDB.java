package actionsBD;

import java.util.HashSet;

import com.mongodb.Mongo;
import com.mongodb.WriteResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;





import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.Date;

import utils.Keyboard;


public class MongoDB {
	public static MongoDB mongoDB=new MongoDB();
	private Mongo mongo=null;
	private String host=null; 
	private boolean auth=false;
	private String connexionFile="../mongo.settings";
	private String login="";
	private String pass="";
	//private String db="propagation";
	public MongoDB(){
		//this("132.227.201.134"); //$NON-NLS-1$
		this(null);
	}
	
	
	public MongoDB(String host){
		this.host=host;
		try{ 
			loadSettings();
			mongo= new Mongo(this.host);
			DB db = mongo.getDB("admin");
			auth = db.authenticate(login, pass.toCharArray()); 
			//System.out.println(db.isAuthenticated()+" "+auth);
		}
		catch(UnknownHostException e){
			System.out.println(e);
			throw new RuntimeException(e);
		}
	}
	
	public MongoDB(String host,int port){
		this.host=host;
		try{ 
			loadSettings();
			mongo= new Mongo(this.host,port);
			DB db = mongo.getDB("admin"); //$NON-NLS-1$
			auth = db.authenticate(login, pass.toCharArray()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch(UnknownHostException e){
			System.out.println(e);
			throw new RuntimeException(e);
		}
	}
	
	public void loadSettings(){
		
		BufferedReader r=null;
		try{
			r = new BufferedReader(new FileReader(connexionFile)) ;
			String line ;
			
			while((line=r.readLine()) != null) {
				String[] sline=line.split("=") ;
				if(sline[0].equals("host")){
					if(host==null){
						this.host=sline[1];
						
					}
					System.out.println("host="+host);
				}
				if(sline[0].equals("login")){
					this.login=sline[1];
					System.out.println("login="+login);
				}
				if(sline[0].equals("pass")){
					this.pass=sline[1];
					//System.out.println("pass="+pass);
				}
				
			}
			
			
			
			
			r.close();
		}
		catch(IOException e){
        	System.out.println("Problem in reading mongo connexion settings "+connexionFile+" : "+e);
        }
		
	}
	
	public DB getDB(String dbname){
		DB db = mongo.getDB(dbname);
		
		if((!auth) && (!db.isAuthenticated())){
			boolean auth2 = db.authenticate(login, pass.toCharArray()); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("Authenticate to "+dbname+"= "+auth2); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return(db);
	}
	/*public DBCollection getCollection(String col){
		return(getCollectionFromDB(col,this.db));
	}*/
	public DBCollection getCollectionFromDB(String db,String col){
		return(getDB(db).getCollection(col));
	}
	
	public String createCollection(String db,String name, String type, String desc){
		if(name.startsWith(type)){
			throw new RuntimeException("The name of the new collection cannot start with "+type); //$NON-NLS-1$
		}
		DBCollection cols=getCollectionFromDB(db,"collections"); //$NON-NLS-1$
		BasicDBObject query=new BasicDBObject();
		query.put("name", name); //$NON-NLS-1$
		DBCursor cursor = cols.find(query);
		
		try {
			if(cursor.hasNext()) {
				String rep=Keyboard.saisirLigne("collection "+name+" already exists in "+db+". Do you want to replace it? (Y/N)" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			    if((rep.compareTo("Y")==0) || (rep.compareTo("y")==0)){ //$NON-NLS-1$ //$NON-NLS-2$
			    	dropCollection(db,name,false);
			    }
			    else{
			    	throw new RuntimeException("Collection "+name+" existe deja dans la base "+db); //$NON-NLS-1$ //$NON-NLS-2$
			    }
			}	
			
			BasicDBObject obj = new BasicDBObject();
			obj.put("name", name); //$NON-NLS-1$
			obj.put("num", 0); //$NON-NLS-1$
			obj.put("type", type); //$NON-NLS-1$
			obj.put("desc", desc); //$NON-NLS-1$
			WriteResult wr=cols.insert(obj);
			
		} finally {
			cursor.close();
		}
		return name;
	}
	
	
	
	
	public String createCollection(String db,String type,String desc){
		DBCollection cols=getCollectionFromDB(db,"collections"); //$NON-NLS-1$
		BasicDBObject query=new BasicDBObject();
		query.put("type", type); //$NON-NLS-1$
		DBCursor cursor = cols.find(query);
		int nb=0;
		String name=""; //$NON-NLS-1$
		int max=0;
		try {
			while(cursor.hasNext()) {
				DBObject res=cursor.next();
				//try{
				System.out.println(res);
				
					int num=0;
					try{
						num=Integer.parseInt(res.get("num").toString()); //$NON-NLS-1$
					}
					catch(Exception e){
						double d=(Double.valueOf(res.get("num").toString())); //$NON-NLS-1$
						num=(int)d;
					}
					if(num>max){
						max=num;
					}
					
				/*}
				catch(NumberFormatException e){
					
				}*/
				nb++;
			}
			max++;
			nb++;
			name=type+"_"+max; //$NON-NLS-1$
			query=new BasicDBObject();
			query.put("name", name); //$NON-NLS-1$
			cursor = cols.find(query);
			if(cursor.hasNext()) {
				throw new RuntimeException("Collection "+name+" existe deja dans la base "+db+" avec un type different, veuillez d'abord la renommer");
			}	
			
			BasicDBObject obj = new BasicDBObject();
			obj.put("name", name); //$NON-NLS-1$
			obj.put("num", max); //$NON-NLS-1$
			obj.put("type", type); //$NON-NLS-1$
			obj.put("desc", desc); //$NON-NLS-1$
			WriteResult wr=cols.insert(obj);
			
		} finally {
			cursor.close();
		}
		return(name);
		
	}
	
	// Insert values from Text file fileName into collection col
	// fields names are given in line 1
	// each line corresponds to a document in format : val1;&;val2;&;...;&;valn;&;
	// columns contains index (begining by 1) of columns to consider when inserting data (if null => all columns are considered)
	
	
	public void insertFromTextFile(String db,String fileName,String col,HashSet<Integer> columns) throws IOException{
		BufferedReader lecteur=null;
		DBCollection coll=getCollectionFromDB(db,col);
		String tokenDelim = "\t" ;  //$NON-NLS-1$
		
		try{
			File f=new File(fileName);
			lecteur=new BufferedReader(new FileReader(f));
			String ligne=""; //$NON-NLS-1$
			int nb=0;
			ArrayList<String> colNames=new ArrayList<String>();
			while((ligne=lecteur.readLine())!=null){
				try {
					//System.out.println(ligne+"==");
					if(ligne.lastIndexOf(tokenDelim)==-1) {
							continue ;
					}
					//ligne=ligne.substring(0, ligne.lastIndexOf(tokenDelim));
					String[] li=ligne.split(tokenDelim);
					if (li.length>0){
						if (nb==0){
							for(int i=0;i<li.length;i++){
									colNames.add(li[i]);
							}
						}
						else{
							BasicDBObject doc = new BasicDBObject();
							for(int i=0;i<(Math.min(li.length,colNames.size()));i++){
								if ((columns==null) || (columns.contains(i+1))){
									doc.put(colNames.get(i),li[i]);
								}
							}
							coll.insert(doc);
						}
						nb++;
						if((nb % 1000)==1)
								System.out.println(nb);
					}
				} catch (Exception e) {
					// Si une ligne est mal formatee, on la saute et on continue.
					System.err.println("Warning : ligne ignoree : "+ligne); //$NON-NLS-1$
				}
			}
		}
		finally{
			if (lecteur!=null){
				lecteur.close();
			}
		}
	}
	
	public void insertInformationAbout(String db, String col, String info, Object val){
		DBCollection infCol = getCollectionFromDB(db, col+"_info"); //$NON-NLS-1$
		BasicDBObject obj = new BasicDBObject();
		obj.put("key", info); //$NON-NLS-1$
		obj.put("val", val); //$NON-NLS-1$
		infCol.insert(obj);

	}
	
	public Object getInformationAbout(String db, String about, String key) {
		DBCollection infCol = getCollectionFromDB(db, about+"_info"); //$NON-NLS-1$
		BasicDBObject query = new BasicDBObject();
		query.put("key", key); //$NON-NLS-1$
		DBCursor cursor = infCol.find(query);
		query = (BasicDBObject) cursor.next();
		return query.get("val"); //$NON-NLS-1$
	}
	
	public void dropCollection(String db,String collection){
		dropCollection(db,collection,false);
	}
	
	public void dropCollection(String db,String collection,boolean prompt){
		    
			String rep="Y"; //$NON-NLS-1$
			if(prompt){
		    	rep=Keyboard.saisirLigne("Are you sure you want to drop collection "+collection+" from "+db+"? (Y/N)" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		    }
		    if((rep.compareTo("Y")==0) || (rep.compareTo("y")==0)){ //$NON-NLS-1$ //$NON-NLS-2$
		    	DBCollection collections=getCollectionFromDB(db,"collections"); //$NON-NLS-1$
		    	BasicDBObject obj = new BasicDBObject();
		    	obj.put("name", collection); //$NON-NLS-1$
		    	collections.remove(obj);
		    	DBCollection col=getCollectionFromDB(db,collection);
		    	col.drop();
		    }
			
	}
	
	public static void main(String[] args){
		
		MongoDB m=MongoDB.mongoDB;
		m.getCollectionFromDB("digg","collections");
		m.getDB("niki");
		
		
		/*MongoDB m = new MongoDB() ;
		try{
			HashSet<Integer> l=new HashSet<Integer>();
			l.add(1);l.add(2);l.add(4);
			m.insertFromTextFile("digg","/home/bourigaults/raw_files/digg.raw","raw", null);
		}
		catch(Exception e){
			System.out.println(e);
		}*/
		
		
		
		/*m = new MongoDB("digg") ;
		try{
			HashSet<Integer> l=new HashSet<Integer>();
			l.add(1);l.add(2);l.add(4);
			m.insertFromTextFile("/home/bourigaults/raw_files/digg.raw","raw", null);
		}
		catch(Exception e){
			System.out.println(e);
		}*/
		/*m = new MongoDB("digg") ;
		try{
			HashSet<Integer> l=new HashSet<Integer>();
			l.add(1);l.add(2);l.add(4);
			m.insertFromTextFile("/home/bourigaults/raw_files/digg.raw","raw", null);
		}
		catch(Exception e){
			System.out.println(e);
		}
		m = new MongoDB("digg2") ;
		try{
			HashSet<Integer> l=new HashSet<Integer>();
			l.add(1);l.add(2);l.add(4);
			m.insertFromTextFile("/home/bourigaults/raw_files/digg2.raw","raw", null);
		}
		catch(Exception e){
			System.out.println(e);
		}
		m = new MongoDB("meme") ;
		try{
			HashSet<Integer> l=new HashSet<Integer>();
			l.add(1);l.add(2);l.add(4);
			m.insertFromTextFile("/home/bourigaults/raw_files/meme.raw","raw", null);
		}
		catch(Exception e){
			System.out.println(e);
		}*/
		
		/*DBCollection coll=m.getCollection("test");
		BasicDBObject doc = new BasicDBObject();

        doc.put("name", "MongoDB");
        doc.put("type", "database");
        doc.put("count", 1);

        BasicDBObject info = new BasicDBObject();

        info.put("x", 203);
        info.put("y", 102);

        doc.put("info", info);

        coll.insert(doc);*/
	}
	
	
}
