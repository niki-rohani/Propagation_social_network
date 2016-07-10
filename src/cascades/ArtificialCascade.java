package cascades;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

import propagationModels.*;

import java.util.TreeMap;
import java.util.HashMap;
import java.util.HashSet;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.User;
import core.Post;

import java.util.Date;

public class ArtificialCascade extends Cascade {
	protected HashMap<Integer,Double> weights=null;
	/*public ArtificialCascade(String name){
		super(name,"artificial","artificial",new HashSet<Post>());
	}
	public ArtificialCascade(int id,String name){
		super(id,name,"artificial","artificial",new HashSet<Post>());
	}
	
	public ArtificialCascade(String name,String db,String from,HashSet<Post> posts){ 
		super(-1,name,db,from,posts);
	}
	public ArtificialCascade(int id,String name,HashSet<Post> posts){
		super(id,name,"","",posts);
	}*/
	public ArtificialCascade(int id,String name,HashSet<Post> posts,HashMap<Integer,Double> weights){ 
		super(id,name,"artificial","artificial",posts);
		this.weights=weights;
	}
	
	public ArtificialCascade(int id,String name,HashSet<Post> posts,TreeMap<Integer,Double> weights){ 
		super(id,name,"artificial","artificial",posts);
		this.weights=new HashMap<Integer,Double>();
		for(Integer v:weights.keySet()){
			double d=weights.get(v);
			this.weights.put(v, d);
		}
	}
	
	public ArtificialCascade(Cascade cascade){
		super(cascade.id,cascade.name,"artificial","artificial",cascade.posts);
		this.weights=cascade.getContentWeigths();
		
	}
	
	public static void saveCascadesAsTextFile(HashSet<Cascade> l,String f){
		PrintStream p = null;
        try{
        	File file=new File(f);
        	File dir=file.getParentFile();
        	if(dir!=null){
        		dir.mkdirs();
        	}
        	
        	p = new PrintStream(file);
        	for(Cascade c:l){
        		String st="";
        		for(Post post:c.posts){
        			double val=post.getTimeStamp();
        			User user=post.getOwner();
        			st+=user.getName()+":"+val+";";
        		}
        		p.println(st);	
        	}
        }
        catch(Exception e){
        	throw new RuntimeException(e);
        }
        finally{
        	if(p!=null){
        		p.close();
        	}
        }
	}
	
	public void indexInto(String db,String collection){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
		BasicDBObject obj = new BasicDBObject();
		obj.put("id", id);
		obj.put("fromCol", "artificial");
		obj.put("name", name);
		ArrayList<BasicDBObject> postsListe=new ArrayList<BasicDBObject>();
		for(Post p:posts){
			double val=p.getTimeStamp();
			User user=p.getOwner();
			BasicDBObject ti=new BasicDBObject();
			ti.put(user.getName(),val);
			postsListe.add(ti);
		}
		obj.put("contamination",postsListe);
		ArrayList<BasicDBObject> wListe=new ArrayList<BasicDBObject>();
		for(Integer i:weights.keySet()){
			BasicDBObject wi=new BasicDBObject();
			wi.put(i+"", weights.get(i));
			wListe.add(wi);
		}
		obj.put("weights",wListe);
		
		col.insert(obj);
		//System.out.println("Cascade artificielle inseree : "+obj);
	}
	public static Cascade getCascadeFrom(DBObject res){
		int id=Integer.parseInt(res.get("id").toString());
		String name=res.get("name").toString();
		HashSet<Post> posts=new HashSet<Post>();
		HashMap<Integer,Double> w=new HashMap<Integer,Double>();
		
		ArrayList<DBObject> list=(ArrayList<DBObject>)res.get("weights");
		for(DBObject obj:list){
			int st=Integer.parseInt(obj.keySet().iterator().next());
			double val=Double.valueOf(obj.get(st+"").toString());
			w.put(st,val);
		}
		
		list=(ArrayList<DBObject>)res.get("contamination");
		for(DBObject obj:list){
			String st=obj.keySet().iterator().next().toString();
			//System.out.println(st+" "+obj.get(st).toString());
			double time1=Double.valueOf(obj.get(st).toString());
			//System.out.println(time1);
			long time=(long)time1; //Long.valueOf(obj.get(st).toString());
			Post p=new Post(-1,"",User.getUser(st),time,new HashMap<Integer,Double>());
		    posts.add(p);
		}
		//System.out.println(w);
		Cascade c=new ArtificialCascade(id,name,posts,w);	
		return(c);
	}
	
	
	@Override
	public HashMap<Integer,Double> getContentWeigths(long step, long nbSteps){
		return(weights);
	}
	public void setContentWeigths(HashMap<Integer,Double> w){
		weights=w;
	}
	
	public boolean isArtificial(){
		return true;
	}
	
	public void genereArtificialCascades(PropagationModel mod, String db,String collection,int nbCascades,int nbMaxInit){
		genereArtificialCascades(mod,db,collection,nbCascades,nbMaxInit,false);
	}
	
	/**
	 * Generates nb artificial cascades that are stored in db.collection (a new collection if collection == "").
	 * 
	 * @param mod the propagation model to use for simulating cascades
	 * @param db database to store the cascades.
	 * @param collection  the collection to store the cascades (a new one is created if this params is empty).
	 * @param nb the number of cascades to generate
	 * @param nbMaxInit the maximal number of init users
	 * @param fixe if true, content does never change
	 */
	public static void genereArtificialCascades(PropagationModel mod, String db,String collection,int nbCascades,int nbMaxInit,boolean fixe){
		
		//System.out.println(dim);
		//String collection="";
		if(collection.length()==0){
			collection=MongoDB.mongoDB.createCollection(db,"artificial","artificial cascades from Artificial Model "+mod.toString()+" avec db="+db+" nbcascades="+nbCascades+" nbMaxInit="+nbMaxInit+" contenuFixe="+fixe);
		}
		else{
			collection=MongoDB.mongoDB.createCollection(db,collection,"artificial","artificial cascades from Artificial Model "+mod.toString()+" avec db="+db+" nbcascades="+nbCascades+" nbMaxInit="+nbMaxInit+" contenuFixe="+fixe);
		}
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db, collection);
		long nbEls=col.count();
		ArrayList<String> users=new ArrayList<String>(mod.getUsers());
		if (users.size()==0){
			throw new RuntimeException("Pas de users charges");
		}
		TreeMap<Integer,Double> vals=new TreeMap<Integer,Double>();
		
		int i=0;
		int dim=mod.getContentNbDims();
		while(i<nbCascades){
		//for(int i=0;i<nbCascades;i++){
			int id=i+1+(int)(nbEls);
			String name="art_"+id;
			if(!fixe){
				vals=new TreeMap<Integer,Double>();
				for(int j=1;j<=dim;j++){
					double x=Math.random();
					if(x<0.5){
						continue;
					}
					x=Math.random();
					vals.put(j, x);
				}
			}
			HashMap<String,Double> probas=new HashMap<String,Double>();
			int nbUsers=users.size();
			/*for(String user:users){
				MultiInterestModel mod=userModels.get(user);
				double p=1.0;
				if(!fixe){
					p=mod.getProba(vals);
				}
				
				sump+=p;
				probas.put(user, p);
			}
			if(fixe){
				sump=users.size();
			}*/
			HashMap<String,Double> init=new HashMap<String,Double>();
			int nbInit=((int)(Math.random()*nbMaxInit))+1;
			for(int j=0;j<nbInit;j++){
				double x=Math.random()*nbUsers;
				String select=users.get(0);
				for(String user:users){
					double p=1.0;
					if(!fixe){
						p=probas.get(user);
					}
					x-=p;
					if(x<0){
						select=user;
						break;
					}
				}
				init.put(select, 1.0);
			}
			System.out.println(init);
			
			ArtificialCascade c=new ArtificialCascade(id,"art_"+id,new HashSet<Post>(),vals);
			
			TreeMap<Long,HashMap<String,Double>> tinit=new TreeMap<Long,HashMap<String,Double>> ();
			tinit.put((long)1.0, init);
			
			PropagationStruct pstruct=new PropagationStruct(c,tinit,new TreeMap<Long,HashMap<String,Double>>());
			mod.inferSimulation(pstruct);
			
			
			TreeMap<Long,HashMap<String,Double>> infections=pstruct.getInfections();
			//System.out.println(infections);
			HashSet<String> vus=new HashSet<String>();
			for(Long t:infections.keySet()){
				HashMap<String,Double> h=infections.get(t);
				for(String user:h.keySet()){
					if(!vus.contains(user)){
						Post p=new Post(-1,"",User.getUser(user),t,new HashMap<Integer,Double>());
						c.addPost(p);
						vus.add(user);
					}
				}
				
			}
			if(vus.size()>nbInit){
				c.indexInto(db, collection);
				i++;
			}
		
		}
	}
	
	public static void main(String[] args){
		//MLPproj mod=new MLPproj("propagationModels/MLPProj_Dims-25_step-1_nbInit-1_db-digg_cascadesCol-cascades_3_iInInit-true_transSend-true_transSendContent-false_diag-false_withDiagContent-false_withDiagSenders-false_sim-5_unbiased-true_regul-0.0_multiSource-false/best",100,1);
		//mod.rescaleDiag(1.5);
		//genereArtificialCascades(mod,"digg","mlpProjx1o5",10000,5,true);
		HashSet<Cascade> cas=getCascadesFromDB("c","cascades_4");
		saveCascadesAsTextFile(cas,"cascades_test.txt");
	}
}
