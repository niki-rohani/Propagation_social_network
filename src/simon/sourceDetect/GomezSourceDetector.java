package simon.sourceDetect;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jblas.ranges.AllRange;

import cascades.Cascade;
import mlp.CPUParams;
import mlp.DescentDirection;
import mlp.LineSearch;
import mlp.Optimizer;
import propagationModels.NetRate;
import propagationModels.PropagationStruct;
import propagationModels.PropagationStructLoader;
import core.Structure;
import core.User;

public class GomezSourceDetector extends SourceDetector {

	
	private HashMap<String, HashMap<String,Double>> alphas ; // Alpha des transmission. Les clefs sont des concatenations
	private HashMap<String,HashSet<String>> preds ;
	private HashMap<Integer,PropagationStruct> train_cascades; // C
	private HashSet<String> Musers ;
	private int L ; // L
	private double k ; 
	private String modelFile ;
	// NOTE : Pour toutes les variables ci dessous, 
	private HashMap<Integer,HashMap<String,Double>> pathLengths ; 
	private HashSet<String> alreadyComputedShortPathSources ;
	private HashMap<Integer,HashMap<String,Double>> sampledtaus ;
	private double tSourceEstimationSample = 20 ; // Pour l'estimation à l'arrache 
	//private double tSourceEstimationMaxVal = 10 ; // Pour l'estimation à l'arrache
	//private double tHiddenEstimationMaxVal = 100 ; // idem
	//private double tHiddenPropaInfinity = 0.
	private boolean CutNearlyZero = true ; // Pour virer tout les 10^-6 de netrate.
	
	
	public GomezSourceDetector(int nbSample, double k, String modelfile, Set<String> obs, Set<String> hid) {
		this.L=nbSample ;
		this.k = k ;
		this.alphas=new HashMap<String, HashMap<String,Double>>() ;
		this.preds = new HashMap<String,HashSet<String>>() ;
		this.Musers= new HashSet<String>() ;
		this.obsUsers=new HashSet<String>(obs) ;
		this.hidUsers=new HashSet<String>(hid) ;
		this.alreadyComputedShortPathSources=new HashSet<String>() ;
		this.pathLengths = new HashMap<Integer,HashMap<String,Double>>() ;
		this.sampledtaus = new HashMap<Integer,HashMap<String,Double>>() ; 
		
		this.modelFile=modelfile ;
		
	}
	
	public double sampleOneTime(double alpha) {
		double r=Math.random() ;
		//System.out.println("VIRER CA !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		//return 1-alpha ;
		return alpha*Math.pow(-Math.log(1-r), 1.0/k) ;
	}
	
	// Tirer L fois des temps de transmission
	private void drawAllTransmissionTimes() {
		//sampledtaus.clear();
		if(!sampledtaus.isEmpty())
			return ;
		for(int index = 0 ; index<L ; index++) {
			sampledtaus.put(index, new HashMap<String,Double>()) ;
			for(String u : alphas.keySet()) {
				for(String v : alphas.get(u).keySet()) {
					sampledtaus.get(index).put(u+";"+v, sampleOneTime(alphas.get(u).get(v))) ;
				}
			}
		}
	}
	
	// TODO : relire qu'il y ai pas d'erreur.
	private void computeAllShortestPath(String source) {
		//System.out.println("Computing shortest paths from "+source+".");
		//pathLength.clear() ;
		if(alreadyComputedShortPathSources.contains(source))
			return ;
		for(int l = 0 ; l<L ; l++) {
			HashMap<String,Double> dists = new HashMap<String,Double>() ;
			if(!pathLengths.containsKey(l)) {
				pathLengths.put(l, dists) ;
			} else {
				dists = pathLengths.get(l) ; //
			}
			HashMap<String,Double> taus = sampledtaus.get(l) ;
			HashSet<String> usersLeftToExamine = new HashSet<String>() ;
			//HashMap<String,String> previousInShortPath  = new HashMap<>();
			// initialisation
			for(String u : User.users.keySet()) {
				dists.put(source+";"+u, Double.POSITIVE_INFINITY) ;
				usersLeftToExamine.add(u) ;
			}
			dists.put(source+";"+source, 0.0) ;
			//usersLeftToExamine.add(source) ;
			// Tant que tout les users ne sont pas marques
			while(!usersLeftToExamine.isEmpty())  {
				String minUser="" ;
				Double minDist=Double.POSITIVE_INFINITY ;
				// Trouver l'acutel plus proche
				for(String u : usersLeftToExamine) {
					if(dists.get(source+";"+u) <= minDist) {
						minDist=dists.get(source+";"+u) ;
						minUser=u ;
					}
				}
				usersLeftToExamine.remove(minUser) ;
				double alt ;
				//System.out.println(minUser);
				if(alphas.containsKey(minUser)) { // C'EST POSSIBLE QUE JE MASQUE UN PROBLEME, LA
					for(String v : alphas.get(minUser).keySet()) {
						alt = taus.get(minUser+";"+v)+minDist ;
						if(alt < dists.get(source+";"+v)) {
							dists.put(source+";"+v, alt) ;
						}
					}
				}
			}
		}
		alreadyComputedShortPathSources.add(source) ;
	}
	public void learn(PropagationStructLoader ploader)  {
		
		NetRate mod ;
		if((new File(modelFile).exists())) {
			mod = new NetRate(modelFile) ;
			mod.load();
			System.out.println("NetRate loaded");
		} else {
			mod=new NetRate(1);
			Optimizer opt=Optimizer.getDescent(DescentDirection.getGradientDirection(), LineSearch.getFactorLine(Double.valueOf(0.1),0.99999));
			System.out.println("Learning netrate...");
			mod.learn(ploader, opt);
			mod.save();
		}
		HashMap<String, HashMap<String, CPUParams>> al = mod.getAlphas() ;
		for(String u : al.keySet()) {
			for(String v : al.get(u).keySet()) {
				double valalpha = al.get(u).get(v).getParameters().getMatrix(0).getValue(0, 0) ;
				
				if(CutNearlyZero && valalpha==0.000001)
					continue ;
				
				if(!alphas.containsKey(u))
					alphas.put(u, new HashMap<String,Double>()) ;
				alphas.get(u).put(v, valalpha) ;
				System.out.println("alpha ("+u+","+v+") = "+valalpha);
				
				if(!preds.containsKey(v))
					preds.put(v, new HashSet<String>()) ;
				preds.get(v).add(u) ;
				
				if(hidUsers.contains(v) && obsUsers.contains(u) && valalpha>0.0) {
					Musers.add(v) ;
				}
			}
		}
		this.train_cascades=ploader.getCascades();
	}
	
	@Override
	public void save() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void load() {
		
		
	}

	@Override
	public void detect(Structure s) {
		
		SourceDetectStructure sd = (SourceDetectStructure)s ;
		
		String solution = "" ;
		
		// Calculer pour chaque source possible le temps le plus vraisemblable
		HashMap<String,Double> sourcesLikelyHood = new HashMap<>() ;
		double maxLike = -1.0 ;
		for(String source : this.hidUsers) {
			double score = estimateSourceLikelyhood(source,sd.getObsTimes());
			System.out.println("score : "+score+ " pour "+source+","+sd.getObsTimes());
			if(score>=maxLike) {
				solution = source ;
				maxLike=score ;
			}
		}
		HashMap<String,Double> sol = new HashMap<>() ;
		sol.put(solution, 1.0) ;
		sd.setSourcesScores(sol);
		
		
	}
	
	
	private double estimateSourceLikelyhood(String source,HashMap<String,Double> obstimes) {
		return estimateSourceLikelyhoodSimpleSlow(source,obstimes) ;
	}

	// Version la plus simple : echantillonner des temps pour la source ET pour les mecs cachés.
	private double estimateSourceLikelyhoodSimpleSlow(String source,HashMap<String,Double> obstimes) {
		
		drawAllTransmissionTimes() ;
		computeAllShortestPath(source);
		//System.out.println(pathLengths);
		
		// Trouver l'obs infecté le plus tot.
		double minObsTime = Double.POSITIVE_INFINITY ;
		for(String u : obstimes.keySet()) 
			minObsTime = Math.min(minObsTime, obstimes.get(u)) ;
		
		HashMap<String,Double> alltimes = new HashMap<>() ;
		double finallike = 0.0 ;
		// Tirer des temps Ts au pif.
		for(int it = 0 ; it<tSourceEstimationSample ; it++) {
			double ts = Math.random()*minObsTime;
			double likeliHoodForThisTs=0.0 ;
			// Pour chaque temps Ts, faire L simulation de diffusion.
			for(int l=0 ; l<L ; l++) {
				
				// Constuire un vecteur de temps avec les obs ET les hidden
				alltimes.clear();
				alltimes.putAll(obstimes) ;
				for(String h : hidUsers) {
					alltimes.put(h, pathLengths.get(l).get(source+";"+h)+ts) ;
				}
				alltimes.put(source,ts) ;
				
				// Calculer la proba de ce vecteur de temps
				double probTime = 1.0 ;
				for(String u : alltimes.keySet()) {
					if(u.compareTo(source)==0)
						continue ;
					double ptiforp = pTiForTparents(u,alltimes.get(u),alltimes) ;
					probTime = probTime * ptiforp ;
					if(ptiforp >1)
						System.out.println(ptiforp+" ?? "+source + " , "+u + " , "+alltimes);
						
				
				}
				
				likeliHoodForThisTs+=probTime ;
			}
			likeliHoodForThisTs = likeliHoodForThisTs / L ; // Estimation de likelyhood de la source pour ce TS
			finallike = Math.max(finallike, likeliHoodForThisTs) ; // On garde le Ts qui maximise la likelihood de la source.
		}
		return finallike;
		
	}
	
	

	
	// Calcule une proba pour un delay
	private double f(double delay,double alpha) {
		if(delay<=0)
			return 0 ;
		return (k*Math.pow(delay, k-1))/Math.pow(alpha, k)*Math.exp(-Math.pow(delay/alpha, k)) ;
	}
	
	// Calcule la fonction de survie
	private double S(double delay,double alpha) {
		if(delay<=0)
			return 1 ;
		return Math.exp(-Math.pow(delay/alpha, k)) ;
	}
	
	// Calcule la fonction de Hazard
		private double H(double delay,double alpha) {
			
			double r= f(delay,alpha)/S(delay,alpha) ;
			//if(Double.isNaN(r))
			//	System.out.println(delay+" : "+alpha);
			return r ;
		}
	
	
	// Calculer p(ti) sachant les temps de ses parent (peut y avoir d'autres temps mais on les ignore).
	// Equation 1 du papier de Farajtaber et Gomez Rodrigez.
	// Comment gerer les gars qui n'ont pas de parent ? Un gars qui n'a pas de parent ne peut etre que source
	private double pTiForTparents(String user, double ti, Map<String,Double> times) {
		if(!preds.containsKey(user)) {
			//System.err.println("User sans parents : "+user) ;
			return 1.0 ;
		}
		double prodTerm=1 ;
		double sumTerm=0 ;
		double tp ;
		double alpha ;
		boolean atLeastOneInfectedParent=false;
		int debug=0 ;
		for(String parent : preds.get(user)) {
			if(times.containsKey(parent)) {
				tp=times.get(parent) ;
				if(tp == Double.POSITIVE_INFINITY)
					continue ;
				atLeastOneInfectedParent = true ;
				alpha = alphas.get(parent).get(user) ;
				prodTerm *= S(ti-tp,alpha) ;
				sumTerm += H(ti-tp,alpha) ;
				debug++ ;
			}
		}
		if(!atLeastOneInfectedParent && ti==Double.POSITIVE_INFINITY)
			return 1.0 ;
		return prodTerm*sumTerm ;
	}
	
	@Override
	public Set<String> getObsUsers() {
		// TODO Auto-generated method stub
		return new HashSet<String>(this.obsUsers);
	}
	
	
	private double getAlpha(String i,String j) {
		if(!alphas.containsKey(i)) {
			if(alphas.get(i).containsKey(j)) {
				return alphas.get(i).get(j) ;
			}
		}
		return 0.0 ;
	}
	
			
	public static void main(String args[]) {
		
		String DB = "testSourceDet" ;
		String U = "users_1" ;
		String C = "cascades_1" ;
		/*String DB = "memetrackerNew" ;
		String U = "users_1" ;
		String C = "cascades_1" ;*/
		
		PropagationStructLoader ploader=new PropagationStructLoader(DB,C,(long)1,1.0,-1,1,20000);
		HashSet<String> obs = new HashSet<>() ; HashSet<String> hid = new HashSet<>() ;
		User.reinitUsers();
		User.loadUsersFrom(DB, U) ;
		
		/*for(String u : User.users.keySet()) {
			//System.out.println(u);
			if(Math.random()<0.1) {
				obs.add(u) ;
			} else {
				hid.add(u) ;
			}
		}*/
		 obs.add("3") ; obs.add("4") ;
		hid.add("0") ; hid.add("1") ; hid.add("2") ;
		
		GomezSourceDetector gsd = new GomezSourceDetector(5,1,"propagationModels/NetRate_step-1_ratioInits-1.0_nbMaxInits--1_db-testSourceDet_cascadesCol-cascades_1_law-1/last",obs,hid) ;
		//GomezSourceDetector gsd = new GomezSourceDetector(10,1,"propagationModels/NetRate_step-1_ratioInits-1.0_nbMaxInits--1_db-memetrackerNew_cascadesCol-cascades_1_law-1/last",obs,hid) ;
		gsd.learn(ploader) ;
		
		HashMap<Integer, PropagationStruct> cas = ploader.getCascades() ;
		for(int i = 0 ; i<100 ;) {
			
			PropagationStruct ps = cas.get(i) ;
			String s = ps.getArrayInit().get(0) ;
			if(obs.contains(s))
					continue ;
			System.out.println("Testing cascade "+i);
			i++ ;
			double ts = Math.random()*10.0 ;
			double minTimeObs = Double.POSITIVE_INFINITY ;
			HashMap<String, Long> t = ps.getInfectionTimes() ;
			HashMap<String,Double> obsTimes = new HashMap<String,Double>();
			for(String u : t.keySet()) {
				if(obs.contains(u)) {
					obsTimes.put(u, (double)t.get(u)+ts );
				}
			}
			SourceDetectStructure sds = new SourceDetectStructure(obsTimes, s) ;
			gsd.detect(sds);
			System.out.println(s);
			System.out.println(sds.getSourcesScores());
			int reussi =0 ; int rate =0;
			if(sds.getSourcesScores().containsKey(s)) {
				System.out.println("Réussi");
				reussi++;
			} else {
				System.out.println("Raté");
				rate++;
			}
			System.out.println(reussi + ","+rate);
		}
		
		// TODO : Faires des TOUTES PETITES données jouet et les mettres en base.
		
	}
	

}
