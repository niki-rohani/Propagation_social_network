package thomas.indexation;

//import indexation.NoTransform;
import indexation.TextTransformer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import actionsBD.MongoDB;
import thomas.core.FineFoodReview;
import wordsTreatment.Stemmer;
import wordsTreatment.TFIDF_Weighter;
import wordsTreatment.WeightComputer;

public class FineFoodAllValidIndexer extends DataIndexer{

	public String indexStems(String db, String collection, String filename) throws IOException {
		collection = MongoDB.mongoDB.createCollection(db, collection," stems from "+filename+" selon "+this.toString());
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, collection);
		System.out.println("Creation de la base de stems reussie");
		System.out.println("Indexation stems "+filename);

		Stemmer stemmer=new Stemmer();
		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		HashMap<String,Integer> stems=new HashMap<String,Integer>();
		BasicDBObject obj = new BasicDBObject();

		String ligne;
		int nl=0;
		int j=0;

		while(((ligne=lecteur.readLine())!=null)){
			replaceAllSpaces(ligne);
			nl++;
			if (((nl-8)%9)==0){
				ligne = ligne.substring(ligne.indexOf(':')+2);
				HashMap<String,Integer> w=stemmer.porterStemmerHash(ligne);
				w.remove(" * ");

				for(String stem:w.keySet()){
					Integer nbp=stems.get(stem);
					nbp=(nbp==null)?0:nbp;
					stems.put(stem, nbp+1);		//nbp = nb de fois ou le stem apparait dans la base =/= nb de documents ou le stem apparait
				}
			}
		}

		int nd = ((nl-8)/9)+1;
		lecteur.close();
		System.out.println(nd + " documents lus.");
		for(String stem:stems.keySet()){
			int nbp=stems.get(stem);
			double idf=Math.log((1.0*nd)/(1.0*nbp));
			j++;
			obj.put("id", j);
			obj.put("stem",stem);
			obj.put("idf",idf);
			col.insert(obj);
			obj.clear();
		}

		System.out.println(j + " stems indexes.\n");
		return collection;
	}


	/*
	(numeroDocument n)
	(productId donneesReview[0])
	(userId donneesReview[1])
	(profileName, donneesReview[2])
	(helpfulness, donneesReview[3])
	(score, donneesReview[4])
	(time, donneesReview[5])
	(summary, donneesReview[6])*/
	@Override
	public String indexData(String db, String collection ,String filename, WeightComputer weightComputer, TextTransformer trans) throws IOException {
		collection = MongoDB.mongoDB.createCollection(db, collection," reviews from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);

		System.out.println("Creation de la base de reviews reussie");
		System.out.println("Indexation reviews "+filename);

		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		//HashMap<String,FineFoodUser> users=new HashMap<String,FineFoodUser>();
		String ligne;


		int nbl=0;
		while((ligne=lecteur.readLine())!=null){
			nbl++;
		}
		int nd = ((nbl-8)/9)+1;
		System.out.println(nd +" reviews");
		lecteur.close();


		lecteur=new BufferedReader(new FileReader(filename));
		int i = 0;
		int n = 0;

		String[] donneesReview = new String[8];
		while(((ligne=lecteur.readLine())!=null) && (i<8)){
			replaceAllSpaces(ligne);
			donneesReview[i] =ligne.substring(ligne.indexOf(':')+2);
			i++;
			if (i == 8){
				n++;
				donneesReview[4] = ""+donneesReview[4].charAt(0);
				/*
				FineFoodUser user;
				if (users.containsKey(donneesReview[2])){
					user = users.get(donneesReview[2]);
				}
				else{
					user = new FineFoodUser(donneesReview[1],donneesReview[2]);
					users.put(donneesReview[2],user);				
				}
				 */
				HashMap<Integer,Double> poids=trans.transform(weightComputer.getWeightsForIds(donneesReview[7]));
				FineFoodReview r=new FineFoodReview(n,donneesReview[6],poids, donneesReview[0], Short.parseShort(donneesReview[4]));
				//FineFoodReview r=new FineFoodReview(n,donneesReview[6],user,Long.valueOf(donneesReview[5]),poids, donneesReview[0], 
				//		donneesReview[3], Short.parseShort(donneesReview[4]));
				r.indexSimplyInto(db, collection);
				System.out.println(n+"/"+nd);
				lecteur.readLine();
				i=0;
			}
		}
		System.out.println("Indexation terminee");
		lecteur.close();
		return collection;
	}		


	@Override
	public String toString() {
		return "FineFoodBasicIndexerAllValidLigns";
	}

	public static void main(String[] args){
		String db="finefoods";
		String filename="data/finefoodsNoInvalidLigns.txt";
		FineFoodBasicIndexer indexer=new FineFoodBasicIndexer();
		try {
			//String stems = indexer.indexStems(db, filename);
			String stems = "stems_1";
			indexer.indexData(db, "documents", filename, new TFIDF_Weighter(db,stems), TextTransformer.getNoTransform());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
