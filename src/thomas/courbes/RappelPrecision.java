package thomas.courbes;

import java.util.ArrayList;
import thomas.eval.Jugement;
import thomas.eval.Modele;
import thomas.eval.Pertinence;
import thomas.eval.Precision;
import thomas.eval.Rappel;
import com.mongodb.DBObject;
import com.panayotis.gnuplot.JavaPlot;


public class RappelPrecision implements Courbe {
	String db, features;
	private Rappel abscisse;
	public Jugement absObj, ordObj;
	private Precision ordonnee;
	DBObject query;
	Modele m;
	int nb;
	
	public RappelPrecision(String db, String documents, String queries, String features, Modele m, DBObject query, int nb){
		this.db = db;
		this.features = features;
		this.m = m;
		abscisse = new Rappel(db);
		ordonnee = new Precision(db);
		absObj = new Pertinence(db, documents, queries);
		ordObj = new Pertinence(db, documents, queries);
		this.query=query;
		this.nb = nb;
	}
	
	@Override
	public void draw() {
		ArrayList<Integer> liste  = m.ordonnancement(db, features, query, m.featurers);
		liste = new ArrayList<Integer>(liste.subList(0, nb-1));
		ArrayList<Integer> sliste  = new ArrayList<Integer>();
		JavaPlot jplot = new JavaPlot();
		double[][] points = new double[liste.size()+1][2];
		points[0][0] = 0.0;
		points[0][1] = 0.0;
		
		for(int i =0; i<liste.size(); i++){
			sliste.add(liste.get(i));
			try {
				points[i+1][0] = abscisse.computeScore(absObj, sliste, query);
				points[i+1][1] = ordonnee.computeScore(ordObj, sliste, query);
				//System.out.println(sliste);
				System.out.println("point (" + points[i][0] + "," + points[i][1] + ")");
			} catch (Exception e) {
				e.printStackTrace();	
			}
		}
		jplot.addPlot(points);
		jplot.setTitle("Rappel Precision");
		jplot.plot();
	}

}
