package indexation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import actionsBD.MongoDB;

import wordsTreatment.TF_Weighter;
import wordsTreatment.WeightComputer;
import core.Post;
import core.User;


// Mme que Basic mais la troisieme colonne du fichier constitue les tags des posts
public class RawWithStoryIdIndexer extends DataIndexer {

	/*public RawWithStoryIdIndexer(){
		this(new TF_Weighter());
	}

	public RawWithStoryIdIndexer(WeightComputer wc){ //,PostTagger postTagger){ //, StrLinkUsers struser){
		super(wc);
	}*/
	
	@Override
	public String indexData(String db,String filename, WeightComputer weightComputer,TextTransformer trans) throws IOException {
		String collection=MongoDB.mongoDB.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
		
		System.out.println("Indexation "+filename);
		//ArrayListStruct<User> ret=new ArrayListStruct<User>();
		HashMap<String,User> users=new HashMap<String,User>();
		HashSet<Post> posts=new HashSet<Post>();
		//Stemmer stemmer=new Stemmer();
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
			int nb=0;
			while(((ligne=lecteur.readLine())!=null)){ // && (nb<10000)){
					//System.out.println(ligne); 
				ligne=ligne.replaceAll("\r", " ");
				ligne=ligne.replaceAll("\\r", " ");
				ligne=ligne.replaceAll("\\\\r", " ");
				ligne=ligne.replaceAll("\n", " ");
				ligne=ligne.replaceAll("\\n", " ");
				ligne=ligne.replaceAll("\\\\n", " ");
				ligne=ligne.replaceAll("\t", " ");
				ligne=ligne.replaceAll("\\t", " ");
				ligne=ligne.replaceAll("\\\\t", " ");
				ligne=ligne.replaceAll("  ", " ");
				ligne=ligne.replace("^ ", "");
					//System.out.println(ligne);
					String[] li=ligne.split(" ");
					if (li.length>=4){
						//System.out.println(li[0]+":"+li[1]);
						User user;
						if (users.containsKey(li[0])){
							user=users.get(li[0]);
						}
						else{
							user=new User(li[0]);
							users.put(li[0],user);
							//ret.add(user);
							
						}
						
						StringBuilder sb=new StringBuilder();
						for(int i=3;i<li.length;i++){
							sb.append(" "+li[i]);
						}
						
						
						// recuperation des id de stems + eventuellement idf
						HashMap<Integer,Double> poids=trans.transform(weightComputer.getWeightsForIds(sb.toString()));
						
						if (poids.size()>0){
							Post p=new Post(sb.toString(),user,Long.valueOf(li[1]),poids);
							p.addTag(li[2]);
							p.indexInto(db,collection);
							user.reinitPosts();
							Post.reinitPosts();
						}
						
						
					}
					else{
						System.out.println("ligne invalide :" + ligne);
						break;
					}
					nb++;
					if (nb%100==0){
						System.out.println(nb+"/"+nbl+" lignes traitees");
					}

			}
			System.out.println("Creation indexs");
			DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,collection);
			col.ensureIndex(new BasicDBObject("tags.tag", 1));
			col.ensureIndex(new BasicDBObject("id", 1));
			
		}
		finally{
			lecteur.close();
		}
		return collection;
	}
	
	public String toString(){
		String s=this.getClass()+""; //+"_"+this.weightComputer.toString();
		return(s);
	}
	
	public static void main(String[] args){
		try{
			System.out.println("Indexation "+args[0]+" > "+args[1]);
			
			RawWithStoryIdIndexer indexer=new RawWithStoryIdIndexer();
			//ArrayListStruct<User> data=(ArrayListStruct<User>)(
			//indexer.indexData(args[1],args[0],new TF_Weighter(),new IDFPruner("tweet09","stems2")); //);
			//for(User user:data){
			//	System.out.println(user);
				//user.serialize("./Users/"+loader.toString()+"/user_"+user.getID()+".serial");
			//}
			//data.serialize("./Users/xx.serial");
			
			/*ArrayListStruct<User> data=(ArrayListStruct<User>)ArrayListStruct.deserialize("./Users/xx.serial");
			for(User user:data){
				System.out.println(user);
				
			}*/
		}
		catch(Exception e){
			System.out.println(e);
		}
	}

}
