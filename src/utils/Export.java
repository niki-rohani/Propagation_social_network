package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import optimization.Fonction;
import optimization.InferFonctionFactory;
import optimization.LossFonctionFactory;
import optimization.Optimizer;
import optimization.OptimizerFactory;
import optimization.Parameters;
import optimization.ParametrizedModel;

import actionsBD.MongoDB;
import cascades.Cascade;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Link;
import core.Post;
import core.User;

import java.util.Random ;

import simon.propagationModels.CTICmodel;





// PA 
public class Export {
	
	public static void export(String db, String cascadesCollection,String usersCollection, long step, String file) throws FileNotFoundException {
		
		HashMap<Integer, HashMap<User, Long>> cascades = CTICmodel.getTimeSteps(db,cascadesCollection,1) ;
		
		
		//DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,cascadesCollection);
        
        PrintStream f_init_tmp = new PrintStream(new File(file+".init.tmp")) ;
        PrintStream f_final_tmp = new PrintStream(new File(file+".final.tmp")) ;
        PrintStream f_init = new PrintStream(new File(file+".init")) ;
        PrintStream f_final = new PrintStream(new File(file+".final")) ;
        PrintStream f_names = new PrintStream(new File(file+".names")) ;
        
        
        // Charger tout les users.
        HashSet<User> users=new HashSet<User>(User.users.values());
        HashMap<Integer,Integer> ids = new HashMap<Integer, Integer>() ;
        HashMap<Integer,String> idsName = new HashMap<Integer, String>() ;
        for(User u : users) {
        	ids.put(u.getID(), ids.size()+1) ;
        	f_names.println(u.getName()+"\t"+ids.size()) ;
        }
        
        int nbcascade = cascades.size();
		int nbusers = ids.size() ;
		int nnz_i = 0  ;
		int nnz_f = 0 ;
		int c_number = 0 ;
		for(Integer cID : cascades.keySet()) {
			HashMap<User, Long> hc = cascades.get(cID)  ;
			c_number++ ;
			for(User u : hc.keySet()) {
				f_final_tmp.println(c_number+" "+ids.get(u.getID())+" 1") ;
				nnz_f++ ;
				if(hc.get(u)<=step) {
					f_init_tmp.println(c_number+" "+ids.get(u.getID())+" 1") ;
					nnz_i++ ;
				}
						
			}
			//f_final_tmp.println(c_number+" "+(nbusers+1)+" 1") ;
			//f_init_tmp.println(c_number+" "+(nbusers+1)+" 1") ;

		}
		
		f_init.println("# name DiffInit") ;
		f_init.println("# type: sparse matrix") ;
		f_init.println("# nnz: "+nnz_i) ;
		f_init.println("# rows: "+nbcascade) ;
		f_init.println("# columns: "+(nbusers)) ;
		
		f_final.println("# name DiffFinal") ;
		f_final.println("# type: sparse matrix") ;
		f_final.println("# nnz: "+nnz_f) ;
		f_final.println("# rows: "+nbcascade) ;
		f_final.println("# columns: "+(nbusers)) ;
		
		f_init_tmp.close() ;
        f_final_tmp.close() ;
        f_init.close() ;
        f_final.close() ;
        
        try {
        	
        	String[] args = {"/bin/sh","-c","sort -g --key=2,2 --key=1,1 "+file+".init.tmp >> "+file+".init"} ;
        	Runtime.getRuntime().exec(args) ;
        	String[] args2 = {"/bin/sh","-c","sort -g --key=2,2 --key=1,1 "+file+".final.tmp >> "+file+".final"} ;
        	Runtime.getRuntime().exec(args2) ;
			System.out.println("Tri effectue");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Erreur a l'exectution du tri.");
			e.printStackTrace();
		}
		
		
		
	}
	
	// Export un Artificial model pour recuperer son graphe en octave
	public static void exportArtificialModelGraph(String personsFile,String modelfile,String octaveFile) throws FileNotFoundException {
		
		Scanner p = new Scanner(new FileInputStream(personsFile)) ;
		Scanner m = new Scanner(new FileInputStream(modelfile)) ;
		int nnz = 0 ;
		int n = 0 ;
		HashMap<String,Integer> names = new HashMap<String, Integer>() ;
		while(p.hasNextLine()) {
			String l = p.nextLine() ;
			String[] t = l.split("\t") ;
			names.put(t[0],Integer.parseInt(t[1])) ;
		}
		n=names.size() ;
		PrintStream out = new PrintStream(new File(octaveFile)) ;
		PrintStream outtemp = new PrintStream(new File(octaveFile+".tmp")) ;
		
		
		
		while(m.hasNext()) {
			String l = m.nextLine();
			if(l.startsWith("<Links>")) {
				break ;
			}
		}
		while(m.hasNext()) {
			String l = m.nextLine();
			if(l.startsWith("</Links>"))
				break ;
			String[] t = l.split("\t") ;
			outtemp.println(names.get(t[0])+" "+names.get(t[1])+" "+t[2]) ;
			nnz++ ;
		}
		
		out.println("# name Graph") ;
		out.println("# type: sparse matrix") ;
		out.println("# nnz: "+nnz) ;
		out.println("# rows: "+n) ;
		out.println("# columns: "+n) ;
		try {
			//ProcessBuilder pb = new ProcessBuilder("/bin/sh","-c","sort","-g -k 2,2 -k 1,1 "+octaveFile+".tmp >> "+octaveFile) ;
			String[] args = {"/bin/sh","-c","sort -g --key=2,2 --key=1,1 "+octaveFile+".tmp >> " + octaveFile} ;
			Runtime.getRuntime().exec(args) ;
			//pb.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	
	
	// Exporter au format "Cedric"
	public static void exportCedricexport(String db, String cascadesCollection,String usersCollection, String outDir, int step, double trainPercent, boolean doGraph, double graphThreshold) throws FileNotFoundException {
		
		HashMap<Integer, HashMap<User, Long>> cascades = CTICmodel.getTimeSteps(db,cascadesCollection,step) ;
		(new File(outDir)).mkdirs();
		PrintStream f_train = new PrintStream(new File(outDir+File.separator+"cascades_train.txt")) ;
        PrintStream f_test = new PrintStream(new File(outDir+File.separator+"cascades_test.txt")) ;
        PrintStream f_info = new PrintStream(new File(outDir+File.separator+"infos.txt")) ;
        
        HashSet<User> users=new HashSet<User>(User.users.values());
        HashMap<Integer,Integer> ids = new HashMap<Integer, Integer>() ;
        HashMap<String,Integer> names = new HashMap<String, Integer>() ;
        //HashMap<Integer,String> idsName = new HashMap<Integer, String>() ;
        for(User u : users) {
        	ids.put(u.getID(), ids.size()+1) ;
        	names.put(u.getName(),ids.size()) ;
        }
        
        int nbcascade = cascades.size();
		int nbusers = ids.size() ;
		int nnz_i = 0  ;
		int nnz_f = 0 ;
		int left_train = (int) ((int) nbcascade*trainPercent) ;
		int left_test = nbcascade-left_train ;
		
		f_train.println("#"+left_train+","+nbusers) ;
		f_test.println("#"+left_test+","+nbusers) ;
		f_info.println("nb_users:"+nbusers) ;
		f_info.println("nb_cascades:"+nbcascade) ;
		f_info.println("nb_cascades_training:"+left_train) ;
		f_info.println("nb_cascades_test:"+left_test) ;
		
		long tmax = 0 ;
		for(Integer cID : cascades.keySet()) {
			
			boolean toTrain = new Random().nextDouble()<trainPercent ;
			if(left_train==0)
				toTrain = false ;
			if(left_test==0)
				toTrain=true ;
			
			PrintStream file_to_append ;
			if(toTrain) {
				left_train-- ;
				file_to_append = f_train ;
			} else {
				left_test-- ;
				file_to_append = f_test ;
			}
			
			
			
			
			HashMap<User, Long> hc = cascades.get(cID)  ;
			for(User u : hc.keySet()) {
				long t = hc.get(u)-1 ;
				tmax=tmax<t ? t : tmax ; 
				if(t<=0) {
					file_to_append.print("i"+","+ids.get(u.getID())+","+(t)+" ") ;
				}
						
			}
			for(User u : hc.keySet()) {
				long t = hc.get(u)-1 ;
				if(t>0) {
					file_to_append.print("d"+","+ids.get(u.getID())+","+(t)+" ") ;
				}	
			}
			file_to_append.println();
		}
		
		f_info.println("t_max:"+tmax) ;
        
		
		if(!doGraph)
			return ;
		
		
		
		int nbTotalLiens = 0 ;
		for(User user:users){
			user.loadLinksFrom(db, usersCollection, graphThreshold); 
			nbTotalLiens+=user.getPredecesseurs().size() ;
		}
		PrintStream graph = new PrintStream(new File(outDir+File.separator+"graph.txt")) ;
		graph.println("#"+nbusers+","+nbTotalLiens) ;
		
		for(User u1 : users) {
			HashMap<String, Link> preds = u1.getPredecesseurs() ;
			for(String s : preds.keySet()) {
				int idu2 = names.get(preds.get(s).getName()) ;
				graph.println(idu2+","+names.get(u1)) ;
			}
		}
	}
	
	
	public static void main(String args[]) {
		
		try {
			//export("us_elections5000", "OctaveOLSTest", "users_1", 1, "testArtificial") ;
			//exportArtificialModelGraph("testArtificial.names","artificial","testArtificial.graph") ;
			exportCedricexport("us_elections5000", "cascades_1", "users_1","/local/bourigaults/Data/usElections_step10800", 10800, 0.8, false, 2) ;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static List<User> sort(Set<User> s) {
		//LinkedList r =  new ArrayList<E>(s.size()) 
		return null ;
	}

	
}
