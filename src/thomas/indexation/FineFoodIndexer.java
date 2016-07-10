package thomas.indexation;

import indexation.TextTransformer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import actionsBD.MongoDB;
import thomas.core.FineFoodReview;
import thomas.core.FineFoodUser;
import wordsTreatment.Stemmer;
import wordsTreatment.WeightComputer;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;


/**
 * DataIndexer pour le corpus finefoods
 * @see DataIndexer
 */
public class FineFoodIndexer extends DataIndexer{

	/**
	 * Cree la base de stems pour les revues de type finefoods, en inserant pour chaque stem son IDF et son nombre d'occurrences dans le corpus.
	 * Insere dans une collection annexe : le nombre de mots moyen par document, le nombre de mots total, le nombre de mots distincts et le nombre de documents.
	 * @param db			le nom de la base de donnee.
	 * @param collection 	le nom de la collection de stems.
	 * @param filename		le nom du fichier a indexer.
	 * @return 				le nom de la collection apres insertion.
	 */
	public String indexStems(String db, String collection, String filename) throws IOException {
		collection = MongoDB.mongoDB.createCollection(db, collection, " stems from " + filename + " selon " + this.toString());
		String infCollection = MongoDB.mongoDB.createCollection(db, collection + "_info", "information about " + collection);
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, collection);
		DBCollection infCol = MongoDB.mongoDB.getCollectionFromDB(db,infCollection);

		System.out.println("Creation de la base de stems reussie");
		System.out.println("Indexation stems " + filename);

		Stemmer stemmer=new Stemmer();
		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		HashMap<String,Integer> stemsNDocs=new HashMap<String,Integer>();   //stem=> nbp = nombre de docs qui le contiennent
		HashMap<String,Integer> stemsNWords=new HashMap<String,Integer>();  //stem=> n = nombre de fois dans la base
		BasicDBObject obj = new BasicDBObject();

		String ligne;
		int nl = 0;
		int j = 0;
		int nwords = 0;
		Integer n, nbp;

		while (((ligne = lecteur.readLine()) != null)) {
			replaceAllSpaces(ligne);
			if (isValid(ligne)) {
				nl++;
				if (((nl - 8) % 9) == 0) {
					ligne = ligne.substring(ligne.indexOf(':') + 2);
					HashMap<String, Integer> w = stemmer
							.porterStemmerHash(ligne);
					w.remove(" * ");

					for (String stem : w.keySet()) {
						nwords += w.get(stem);
						n = stemsNWords.get(stem);
						nbp = stemsNDocs.get(stem);
						n = (n == null) ? 0 : n;
						nbp = (nbp == null) ? 0 : nbp;
						stemsNWords.put(stem, n + w.get(stem));
						stemsNDocs.put(stem, nbp + 1);
					}
				}
			} else {
				System.out.println("ligne invalide : " + ligne + " at " + nl);
			}
		}
		int nd = ((nl - 8) / 9) + 1;
		lecteur.close();
		System.out.println(nd + " documents lus.");
		for (String stem : stemsNDocs.keySet()) {
			nbp = stemsNDocs.get(stem);
			double idf = Math.log((1.0 * nd) / (1.0 * nbp));
			j++;
			obj.put("id", j);
			obj.put("stem", stem);
			obj.put("idf", idf);
			obj.put("nOcc", stemsNWords.get(stem));
			col.insert(obj);
			obj.clear();
		}

		MongoDB.mongoDB.insertInformationAbout(db, collection, "nDocs", nd);
		MongoDB.mongoDB.insertInformationAbout(db, collection, "nDistinctWords", stemsNDocs.size());
		MongoDB.mongoDB.insertInformationAbout(db, collection, "nWords", nwords);
		MongoDB.mongoDB.insertInformationAbout(db, collection, "meanNWords", (nwords+0.0)/nd);

		System.out.println(j + " stems indexes.\n");
		col.ensureIndex(new BasicDBObject("stem", 1));
		col.ensureIndex(new BasicDBObject("id", 1));

		infCol.ensureIndex(new BasicDBObject("key", 1));
		return collection;
	}

	private boolean isValid(String ligne) {
		return (ligne.startsWith("review") || ligne.startsWith("product") || (ligne.length()==0));
	}


	/**
	 * Indexe les documents de type finefood dans la base db.
	 * @param db				Le nom de la base de donnee.
	 * @param collection		Le nom de la collection ou seront stockes les documents.
	 * @param weightComputer	La strategie de calcul des poids des stems.
	 * @param trans				La strategie de transformation du texte.
	 * @return					Le nom de la collection apres creation et insertion.
	 */
	@Override
	public String indexData(String db, String collection, String filename, WeightComputer weightComputer, TextTransformer trans) throws IOException {
		collection = MongoDB.mongoDB.createCollection(db, collection," reviews from "+filename+" selon "+this.toString()+" avec "+weightComputer+" transform par "+trans);
		DBCollection col = MongoDB.mongoDB.getCollectionFromDB(db, collection);

		System.out.println("Creation de la base de reviews reussie");
		System.out.println("Indexation reviews "+filename);

		BufferedReader lecteur=new BufferedReader(new FileReader(filename));
		HashMap<String,FineFoodUser> users=new HashMap<String,FineFoodUser>();
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

		
		/*
		(numeroDocument n)
		(productId donneesReview[0])
		(userId donneesReview[1])
		(profileName, donneesReview[2])
		(helpfulness, donneesReview[3])
		(score, donneesReview[4])
		(time, donneesReview[5])
		(summary, donneesReview[6])*/
		String[] donneesReview = new String[8];
		while(((ligne=lecteur.readLine())!=null) && (i<8)){
			replaceAllSpaces(ligne);
			if (isValid(ligne)){
				donneesReview[i] =ligne.substring(ligne.indexOf(':')+2);
				i++;
				if (i == 8){
					n++;
					donneesReview[4] = ""+donneesReview[4].charAt(0);

					FineFoodUser user;
					if (users.containsKey(donneesReview[2])){
						user = users.get(donneesReview[2]);
					}
					else{
						user = new FineFoodUser(donneesReview[1],donneesReview[2]);
						users.put(donneesReview[2],user);				
					}
					HashMap<Integer,Double> poids=trans.transform(weightComputer.getWeightsForIds(donneesReview[7]));
					FineFoodReview r=new FineFoodReview(n,donneesReview[6],user,Long.valueOf(donneesReview[5]),poids, donneesReview[0], 
							donneesReview[3], Short.parseShort(donneesReview[4]));
					r.indexInto(db, collection);
					if(n%1000==0)System.out.println(n+"/"+nd);
					lecteur.readLine();
					i=0;
				}
			}
		}
		System.out.println("Indexation terminee");
		lecteur.close();
		col.ensureIndex(new BasicDBObject("id",1));
		return collection;
	}		

	@Override
	public String toString() {
		return "FineFoodIndexer";
	}



}