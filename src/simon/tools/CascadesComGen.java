package simon.tools;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import cascades.Cascade;
import propagationModels.PropagationStruct;
import mlp.CPUMatrix ;




/*
 * Truc pour générer des cascades basés sur des communautés.
 */
public class CascadesComGen {

	int nbUsers ;
	int nbCom ;
	
	double probActive ;
	double probPassive ;
	
	double noize ;
	double probaStop ;
	int maxNbSources ;
	
	ArrayList<ArrayList<Integer>> passives ; // Pour chaque com, liste des cibles possibles
	ArrayList<ArrayList<Integer>>  actives ; // Pour chaque com, liste des sources possibles
	
	
	
	public CascadesComGen(int nbu, int nbc, double probAct, double probPass, double noize, int maxNbSources, double probaStop) {
		this.nbUsers=nbu ;
		this.nbCom=nbc ;
		this.probActive = probAct ;
		this.probPassive=probPass ;
		this.maxNbSources = maxNbSources ;
		
		
		this.passives = new ArrayList<ArrayList<Integer>>() ;
		this.actives = new ArrayList<ArrayList<Integer>>() ;
		
		boolean[] itsSomething = new boolean[nbUsers] ;
		
		Random rng = new Random() ;
		for(int c=0 ; c<nbCom  ;c++) {
			actives.add(new ArrayList<Integer>()) ;
			passives.add(new ArrayList<Integer>()) ;
			for(int u=0 ; u<nbUsers ;u++) {

				if(rng.nextDouble()<probActive) {
					actives.get(c).add(u);
					itsSomething[u]=true ;
				}
				if(rng.nextDouble()<probPassive) {
					passives.get(c).add(u) ;
					itsSomething[u]=true ;
				}

			}
			// Avoir au moins gars dans chaque.
			if(actives.get(c).isEmpty())
				actives.get(c).add(rng.nextInt(nbUsers)) ;
			if(passives.get(c).isEmpty())
				passives.get(c).add(rng.nextInt(nbUsers)) ;
		}
		// Avoir au moins un truc pour chaque gars.
		for(int u=0 ; u<nbUsers ;u++) {
			if(!itsSomething[u]) {
				passives.get(rng.nextInt(nbCom)).add(u) ;
			}
		}
		
		/*for(int c=0 ; c<nbCom ;c++) {
			System.out.println("in :"+actives.get(c));
			System.out.println("out:"+passives.get(c));
			System.out.println();
		}*/
		
		this.noize=noize ;
		this.probaStop=probaStop ;
	}
	
	
	public HashMap<Integer,PropagationStruct> generate(int nb) {
		
		HashMap<Integer, PropagationStruct> cascades = new HashMap<Integer,PropagationStruct>() ;
 
		
		for(int cid = 0; cid<nb ; cid++) {
			TreeMap<Long,HashMap<String,Double>> init=new TreeMap<Long,HashMap<String,Double>>();
			TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
			TreeMap<Integer,Double> diffusion=new TreeMap<Integer,Double>();
			
			Random rng = new Random() ;
			
			// Tirer une communauté
			int community = rng.nextInt(this.nbCom) ;
			
			// Diff init
			HashMap<String,Double> h=new HashMap<String,Double>();
			for(int user : getSources(community)) {
				h.put(Integer.toString(user), 1.0) ;
			}
			init.put(1l, h);
			infections.put(1l, h);
			
			// Diff final
			long t = 1 ;
			h=new HashMap<String,Double>();
			do {
				HashMap<String, Double> h2 = new HashMap<String, Double>() ;
				String u = Integer.toString(getOneFinal(community)) ;
				//System.out.println(u);
				h2.put(u, 1.0) ;
				//t=t+1.0;
				infections.put(t++,h2);
				//h=h2 ;
				//System.out.println();
			} while(rng.nextDouble()>probaStop) ;
			
			//infections.put(2l,h);
			diffusion.put(0, 1.0);
			PropagationStruct struct=new PropagationStruct(new Cascade(cid, "cascade_"+cid, null),1,1,init,infections,diffusion);
			cascades.put(cid, struct);
			//System.out.println(struct.getArrayContamined().size());
		}	
		
		return cascades ;
	}
	
	// Revois une liste de sources pour la diff initiale basee sur actives
	private int[] getSources(int com) {
		
		Random rng = new Random() ;
		int n = rng.nextInt(this.maxNbSources)+1 ;
		int[] s =  new int[n] ;
		
		for(int i = 0 ; i<n ; i++) {
			s[i] = this.actives.get(com).get(rng.nextInt(this.actives.get(com).size())) ;
		}
		
		return s ;
	}
	
	private int getOneFinal(int com) {
		Random rng = new Random() ;
		int u = this.passives.get(com).get(rng.nextInt(this.passives.get(com).size())) ;
		if(rng.nextDouble()<noize) { // Cas où on sort un utilisateur au hasard
			while(actives.get(com).contains(u) | passives.get(com).contains(u)) {
				u=rng.nextInt(nbUsers) ;
			}
		}
		return u;
	}
	
	
	public static void main(String args[]) {
		
		/*int nb = 10 ;
		CascadesComGen gen = new CascadesComGen(20, 4, 0.0, 0.0,0.1,2, 0.05) ;
		HashMap<Integer,PropagationStruct> cs = gen.generate(nb) ;
		for(int i = 0 ; i<nb ; i++) {
			System.out.println(cs.get(i).getArrayInit()); 
			System.out.println(cs.get(i).getArrayContamined()); 
			System.out.println();
		}*/
		
		CascadesComGen gen ;
		//int[] nbus = {100,500,2000} ;
		int[] nbcs = {24} ;
		double[] probaActs = {0.1} ;
		double[] probaPasss = {0.3} ;
		double[] noizes = { 0.1} ;
		double[] probasStop = {0.05} ;
		//for(int nbu : nbus) {
			for(int nbc : nbcs) {
				for(double probAct : probaActs ) {
					for(double probPass : probaPasss) {
						//for(double noize : noizes) {
							//for(double probaStop : probasStop) {
								//try {
									for(int i = 0 ; i<1 ; i++) {
										gen = new CascadesComGen(100, 8, 0.1, 0.1, 1, 2, 0.1) ;
										HashMap<Integer,PropagationStruct> c= gen.generate(1) ;
										System.out.println(c.get(0).getInfectionTimes());
										//System.out.println("/home/bourigaults/workspace/Propagation/Results/expsArti/datafew/train_NUMBER-"+i+"_"+nbc+"_"+probAct+"_"+probPass);
										//gen.saveCascades(0,"/home/bourigaults/workspace/Propagation/Results/expsArti/datafew/train_NUMBER-"+i+"_"+nbc+"_"+probAct+"_"+probPass) ;
										//gen.saveCascades(50000,"/home/bourigaults/workspace/Propagation/Results/expsArti/datafew/test_NUMBER-"+i+"_"+nbc+"_"+probAct+"_"+probPass) ;
									}
								//} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
								//	e.printStackTrace();
								//}
							//}
						//}
					}
				}
			}
		//}
		
		
	}
	
	public void saveCascades(int nbCascades, String file) throws FileNotFoundException {
		HashMap<Integer,PropagationStruct> cs = generate(nbCascades) ;
		PrintStream f = new PrintStream(file) ;
		//f.println(this.nbUsers) ;
		for(int i : cs.keySet()) {
			PropagationStruct c = cs.get(i) ;
			for(String u : c.getArrayInit())
				f.print(u+" ");
			f.println();
			for(String u : c.getArrayContamined())
				f.print(u+" ");
			f.println();
		}
	}
	
	public static HashMap<Integer,PropagationStruct> loadCascades(String file) throws IOException {
		HashMap<Integer,PropagationStruct> cs=new HashMap<Integer, PropagationStruct>() ;
		BufferedReader f = new BufferedReader(new FileReader(file)) ;
		int id = 0 ;
		for(String init = f.readLine() ; init != null ; init=f.readLine()) {
			
			TreeMap<Long,HashMap<String,Double>> initial=new TreeMap<Long,HashMap<String,Double>>();
			TreeMap<Long,HashMap<String,Double>> infections=new TreeMap<Long,HashMap<String,Double>>();
			TreeMap<Integer,Double> diffusion=new TreeMap<Integer,Double>();diffusion.put(0,1.0) ;
			
			initial.put(1l, new HashMap<String,Double>()) ;
			for(String u : init.split(" ")) {
				initial.get(1l).put(u,1.0) ;
			}
			
			String conta = f.readLine();
			infections.put(1l, new HashMap<String,Double>()) ;
			for(String u : conta.split(" ")) {
				infections.get(1l).put(u,1.0) ;
			}
			
			PropagationStruct struct=new PropagationStruct(new Cascade(id, "cascade_"+id, null),1,1,initial,infections,diffusion);
			cs.put(id++, struct);
		}
		return cs ;
	}
	
	public static void indexInto(HashMap<Integer,PropagationStruct> cs, String db, String collection) {
		
	}
}
