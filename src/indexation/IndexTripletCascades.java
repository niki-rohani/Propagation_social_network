package indexation;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import core.Post;
import core.User;

import wordsTreatment.Stemmer;
import wordsTreatment.WeightComputer;
import cascades.Cascade;
import cascades.ArtificialCascade;

public class IndexTripletCascades extends DataIndexer {

	String cascadesWeightsFile;
	int add=1;  // value to add to index of stems in order that the first stem of the index begins at 1 
	public IndexTripletCascades(String cascadesWeightsFile){
		this.cascadesWeightsFile=cascadesWeightsFile;
	}
	public IndexTripletCascades(String cascadesWeightsFile,int add){
		this.cascadesWeightsFile=cascadesWeightsFile;
		this.add=add;
	}
	
	@Override
	public String indexData(String db, String filename,
			WeightComputer weightComputer, TextTransformer trans)
			throws IOException {
		
		//String posts_collection=MongoDB.mongoDB.createCollection(db,"posts"," posts from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
		String cascades_collection=MongoDB.mongoDB.createCollection(db,"cascades"," cascades from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
		
		
		
		System.out.println("Indexation "+filename);
		//ArrayListStruct<User> ret=new ArrayListStruct<User>();
		HashMap<String,User> users=new HashMap<String,User>();
		HashSet<Post> posts=new HashSet<Post>();
		HashSet<Cascade> cascades=new HashSet<Cascade>();
		
		Stemmer stemmer=new Stemmer();
		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		BufferedReader lecteurW=new BufferedReader(new FileReader(this.cascadesWeightsFile));
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
			int nbc=0;
			int nbinvalides=0;
			lecteur.readLine();
			lecteurW.readLine();
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
					User user;
					
					if (li.length>=2){
						//System.out.println(li[0]+":"+li[1]);
						HashMap<String,Post> pc=new HashMap<String,Post>();
						HashMap<String,Long> vusUsers=new HashMap<String,Long>();
						for(int i=0;i<li.length;i++){
							String[] triplet=li[i].split(",");
							if (users.containsKey(triplet[1])){
								user=users.get(triplet[1]);
							}
							else{
								user=User.getUser(triplet[1]);
								users.put(triplet[1],user);
								
							}
							long t=Long.valueOf(triplet[2])+1;
							if(triplet[0].compareTo("i")==0){
								t=0;
							}
							if((!vusUsers.containsKey(user.getName())) || (vusUsers.get(user.getName())<t)){
								Post p=new Post("",user,t,null);
								pc.put(user.getName(),p);
								vusUsers.put(user.getName(),t);
								posts.add(p);
							}
							
							
						}
						if(pc.size()>1){
							String wl=lecteurW.readLine();
							String[] ws=wl.split(" ");
							HashMap<Integer,Double> weights=new HashMap<Integer,Double>();
							for(int i=0;i<ws.length;i++){
								String[] st=ws[i].split(",");
								weights.put(Integer.parseInt(st[0])+add, Double.valueOf(st[1]));
							}
							nbc++;
							Cascade c=new ArtificialCascade(nbc,nbc+"",new HashSet<Post>(pc.values()),weights);
							c.indexInto(db, cascades_collection);
						}
						
					}
					else{
						
						System.out.println("ligne invalide :" + ligne);
						nbinvalides++;
						//break;
					}
					nb++;
					if (nb%100==0){
						System.out.println(nb+"/"+nbl+" lignes traitees");
					}

			}
			System.out.println(nbinvalides+" lignes invalides");
			
			/*nb=0;
			nbl=posts.size();
			for(Post post:posts){
				post.indexInto(collection);
				if (nb%100==0){
					System.out.println(nb+"/"+nbl+" posts inseres");
				}
				nb++;
			}*/
			System.out.println("Creation indexs");
			DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,cascades_collection);
			col.ensureIndex(new BasicDBObject("id", 1));
		}
		finally{
			lecteur.close();
			lecteurW.close();
		}
		
		return cascades_collection;
	}

	@Override
	public String toString() {
		
		return "indexTripletCascades";
	}

}
