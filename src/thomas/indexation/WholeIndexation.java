package thomas.indexation;

import indexation.TextTransformer;

import java.io.IOException;

import wordsTreatment.TFIDF_Weighter;

public class WholeIndexation{

	public static void indexFineFoods(){
		
		String db="finefoods";
		String stems = "stems";
		String documents = "documents";
		String filename="data/finefoods_test3.txt";
		
		FineFoodIndexer indexer=new FineFoodIndexer();
		try {
			stems = indexer.indexStems(db, stems, filename);
			MostUsedStemsFinder.findMostUsedStemsForIds(50, db, stems);
			indexer.indexData(db, documents, filename, new TFIDF_Weighter(db,stems), TextTransformer.getNoTransform());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args){
		indexFineFoods();
	}
}