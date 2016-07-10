package thomas.queryProduction;

import actionsBD.MongoDB;

import com.mongodb.DBCollection;

public abstract class PostSelector {
	protected DBCollection collection;

	public PostSelector(String db, String collection){
		this.collection = MongoDB.mongoDB.getCollectionFromDB(db, collection);
	}

	/**
	 * Selectionne des requetes dans la collection "collection" de la base "db".
	 * @param nb	Le nombre de requetes a selectionner.
	 * @return		Le nom de la collection de requetes.
	 */
	public abstract String select(int nb);

}