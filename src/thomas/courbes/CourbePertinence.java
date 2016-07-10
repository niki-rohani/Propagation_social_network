package thomas.courbes;

import java.util.ArrayList;
import thomas.eval.BiJugement;
import thomas.eval.Jugement;
import thomas.eval.Modele;
import thomas.eval.ObjectifEvaluateur;
import thomas.eval.Pertinence;
import thomas.eval.Precision;
import thomas.eval.Sentiment;
import com.mongodb.DBObject;
import com.panayotis.gnuplot.JavaPlot;

public class CourbePertinence implements Courbe {

	private ObjectifEvaluateur abscisse;
	private Jugement absObj;
	private ObjectifEvaluateur ordonnee;
	private Jugement ordObj;
	private ArrayList<Modele> modeles;
	private ArrayList<DBObject> requetes;
	String db, features;
	int nb;

	public CourbePertinence(String db, String documents, String queries, String features, ArrayList<DBObject> requetes, int nb) {
		this.db=db;
		this.features = features;
		this.requetes = requetes;
		this.modeles = new ArrayList<Modele>();
		this.abscisse = new Precision(db);
		this.absObj = new BiJugement(db, documents, queries,
				new Pertinence(db, documents, queries),
				new Sentiment(db, documents, queries));
		this.ordonnee = new Precision(db);
		this.ordObj = new Pertinence(db, documents, queries);
		this.nb = nb;
	}

	public CourbePertinence(String db, String documents,String queries, String features,  ArrayList<Modele> modeles,
			ArrayList<DBObject> requetes, ObjectifEvaluateur abscisse,
			Jugement absObj, int nb) {
		this.db=db;
		this.features = features;
		this.requetes = requetes;
		this.modeles = new ArrayList<Modele>();
		this.abscisse = abscisse;
		this.absObj = absObj;
		this.ordonnee = new Precision(db);
		this.ordObj = new Pertinence(db, documents, queries);
		this.nb = nb;
	}

	public CourbePertinence(ArrayList<Modele> modeles,
			ArrayList<DBObject> requetes, ObjectifEvaluateur abscisse,
			Jugement absObj, ObjectifEvaluateur ordonnee, Jugement ordObj, int nb) {
		this.requetes = requetes;
		this.modeles = new ArrayList<Modele>();
		this.abscisse = abscisse;
		this.absObj = absObj;
		this.ordonnee = ordonnee;
		this.ordObj = ordObj;
		this.nb = nb;
	}

	public void addModele(Modele m) {
		this.modeles.add(m);
	}

	@Override
	public void draw() {
		JavaPlot jplot = new JavaPlot();
		double[][] points = new double[modeles.size() + 1][2];
		points[0][0] = 0.0;
		points[0][1] = 0.0;
		int i = 1;
		ArrayList<Integer> liste;

		while (i <= modeles.size()) {
			double a = 0.0, b = 0.0;
			for (DBObject query : requetes) {
				liste = modeles.get(i - 1).ordonnancement(db, features, query, modeles.get(i-1).featurers);
				liste = new ArrayList<Integer>(liste.subList(0, nb-1));
				System.out.println("Modele " + (i) + " : " + liste);
				try {
					a += abscisse.computeScore(absObj, liste, query);
					b += ordonnee.computeScore(ordObj, liste, query);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			a = a / requetes.size();
			b = b / requetes.size();
			points[i][0] = a;
			points[i][1] = b;
			System.out.println("point (" + points[i][0] + "," + points[i][1]
					+ ")");
			i++;
		}
		jplot.addPlot(points);
		jplot.setTitle(" Precision Pertinence sur PertinentSurPositif");
		jplot.plot();
	}
}
