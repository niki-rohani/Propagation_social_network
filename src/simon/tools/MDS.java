package simon.tools;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import propagationModels.IC;
import propagationModels.NaiveLink;
import propagationModels.PropagationStructLoader;
import core.User;
import mlp.CPUParams; 
import mlp.Parameter;
import mlp.Parameters;

import org.jblas.*;

public class MDS {
	
	public static HashMap<String,CPUParams> performMDSFromNaive(String db, String cascadesCol, String userscol, int dim, double parinf, double parsup) {
		
		User.loadUsersFrom(db, userscol) ;
		User.loadAllLinksFrom(db, userscol) ;
		int N = User.users.size() ;
		HashMap<String,Integer> ids = new HashMap<String, Integer>() ;
		int id = 0 ;
		for(String s : User.users.keySet()) {
			ids.put(s, id++) ;
		}
		
		NaiveLink nl = new NaiveLink();
		PropagationStructLoader ploader = new PropagationStructLoader(db, cascadesCol, 1, 1.0, -1) ;
		nl.learn(ploader, db, userscol, 0, false);
		
		// Contruire la matrice de similarités.
		HashMap<String,HashMap<String,Double>> probs = nl.getProbas() ;
		DoubleMatrix probMatrix = new DoubleMatrix(N,N) ;
		
		for(String u1 : probs.keySet()) {
			int i1 =ids.get(u1) ;
			for(String u2 : probs.get(u1).keySet()) {
				int i2 =ids.get(u2) ;
				probMatrix.put(i1, i2,probs.get(u1).get(u2)) ;
			}
		}
		for(int i = 0 ; i<N ; i++) {
			probMatrix.put(i, i,1.0) ;
		}
		
		return doMDS(probMatrix,dim,parinf,parsup,ids) ;
		
	}
	
	public static HashMap<String,CPUParams> doMDS(DoubleMatrix probMatrix,int dim, double parinf, double parsup, HashMap<String,Integer> ids) {
		// Contruire la matrice idempotente de centrage.
		int N = ids.size() ;
		DoubleMatrix centeringMatrix = new DoubleMatrix(N,N) ;
		for(int i = 0 ; i<N ; i++) {
			for(int j = 0 ; j<N ; j++) {
				if(i==j)
					centeringMatrix.put(i,j,1.0-(1.0/((double)N))) ;
				else 
					centeringMatrix.put(i,j,-(1.0/((double)N))) ;
			}
		}
		// Centrer
		probMatrix = centeringMatrix.mmul(probMatrix).mmul(centeringMatrix) ;
		
		// Faire la MDS de probmatric centree.
		DoubleMatrix eig[] = Eigen.symmetricEigenvectors(probMatrix) ;
		DoubleMatrix eigVect = eig[0] ;
		DoubleMatrix eigVal = eig[1] ;
		ArrayList<Double> eigvalueslist = new ArrayList<Double>() ;
		ArrayList<DoubleMatrix> eigvectorslist = new ArrayList<DoubleMatrix>() ;
		int NposEig = 0 ;
		for(int i=0 ; i<N ;i++) {
			Double ev = eigVal.get(i,i) ;
			if(ev>0)
				NposEig++ ;
			eigvalueslist.add(ev) ;
			eigvectorslist.add(eigVect.getColumn(i)) ;
			
		}
		int MDSsize = Math.min(NposEig, dim) ;
		DoubleMatrix reducedEigVect = new DoubleMatrix(N,MDSsize) ;
		DoubleMatrix reducedEigVal = new DoubleMatrix(MDSsize,MDSsize) ;
		Collections.sort(eigvalueslist) ;
		Collections.reverse(eigvalueslist);
		
		
		//Filtrer les eigens
		for(int i=0 ; i<MDSsize ; i++) {
			if(eigvalueslist.get(i)>0) {
				reducedEigVal.put(i, i,Math.sqrt(eigvalueslist.get(i))) ;
				reducedEigVect.putColumn(i, eigvectorslist.get(i));
			}
		}
		
		// Faire la MDS
		DoubleMatrix mds = reducedEigVect.mmul(reducedEigVal) ;
		
		HashMap<String,CPUParams> r = new HashMap<String, CPUParams>() ;
		for(String u : User.users.keySet()) {
			int id1 = ids.get(u) ;
			CPUParams cpu = new CPUParams(1, dim, 0.0,parinf,parsup) ;
			Parameters pList = new Parameters(dim, 0.0) ;
			for(int i=0 ; i<MDSsize ; i++) {
				pList.set(i, new Parameter(mds.get(id1, i)*parsup));
			}
			cpu.setParameters(pList);
			r.put(u, cpu) ;
		}
		
		return r ;
	}
	
	public static HashMap<String,CPUParams> performMDSFromIC(String db, String cascadesCol, String userscol, int dim,String modelfile, double parinf, double parsup) {
		
		User.loadUsersFrom(db, userscol) ;
		User.loadAllLinksFrom(db, userscol) ;
		int N = User.users.size() ;
		HashMap<String,Integer> ids = new HashMap<String, Integer>() ;
		int id = 0 ;
		for(String s : User.users.keySet()) {
			ids.put(s, id++) ;
		}
		
		IC ic = new IC(-1) ;
		if(modelfile == "") {
			PropagationStructLoader ploader = new PropagationStructLoader(db, cascadesCol, 1, 1.0, -1,0,20000) ;
			ic.learn(ploader, db, userscol, 100, 0, 0);
		} else {
			ic=new IC(modelfile,100,-1) ;
			ic.load();
		}
		
		// Contruire la matrice de similarités.
		//HashMap<String,HashMap<String,Double>> probs = nl.getProbas() ;
		DoubleMatrix probMatrix = new DoubleMatrix(N,N) ;
		
		for(String u1 : ids.keySet()) {
			int i1 =ids.get(u1) ;
			for(String u2 : ids.keySet()) {
				int i2 =ids.get(u2) ;
				Double d = ic.getProba(u1, u2) ;
				if(d!=null)
					probMatrix.put(i1, i2,d) ;
			}
		}
		for(int i = 0 ; i<N ; i++) {
			probMatrix.put(i, i,1.0) ;
		}
		
		
		return doMDS(probMatrix,dim,parinf,parsup,ids) ; 
		
	}

	
	public static void main(String args[]) {
		HashMap<String, CPUParams> x = performMDSFromIC("irvine", "cascades_1", "users_1", 2, "",-100,100) ;
		try {
			PrintStream p = new PrintStream("testmds") ;
			for(String s : x.keySet()) {
				p.println(x.get(s).getParameters().getMatrix(0).getValue(0, 0)+" "+x.get(s).getParameters().getMatrix(0).getValue(0, 1)) ;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

}
