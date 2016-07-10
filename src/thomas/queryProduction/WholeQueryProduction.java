package thomas.queryProduction;

public class WholeQueryProduction {

	private static void produceFineFoodsQueries(int nq) {
		String db="finefoods";
		String collection = "documents_1";
		try{
			MediumScore selector = new MediumScore(db, collection);
			selector.select(nq);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void main(String[] args){
		produceFineFoodsQueries(10);
	}
}
