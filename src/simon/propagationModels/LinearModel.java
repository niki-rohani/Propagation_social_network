package simon.propagationModels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import actionsBD.MongoDB;
import cascades.Cascade;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import optimization.Fonction;
import optimization.InferFonctionFactory;
import optimization.LossFonctionFactory;
import optimization.Optimizer;
import optimization.OptimizerFactory;
import optimization.Parameters;
import optimization.ParametrizedModel;
import propagationModels.PropagationModel;
import core.Model;
import core.Post;
import core.Structure;
import core.User;

public class LinearModel implements PropagationModel {

	private HashMap<Integer,ParametrizedModel> m;
	private HashMap<Integer,String> ids;
	private HashMap<String,Integer> ids_inv;
	private String filename ; 
	private boolean loaded=false;
	
	public LinearModel(String filename) {
		this.filename = filename ;
		m = new HashMap<Integer, ParametrizedModel>() ;
		ids = new HashMap<Integer, String>() ;
		ids_inv = new HashMap<String, Integer>() ;
	}
	
	public HashSet<String> getUsers(){
		if(!loaded){
			try{
				load();
			}
			catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		return new HashSet<String>(ids_inv.keySet());
	}
	
	public int getContentNbDims(){
		
		return 0;
	}
	
	public String getName(){
		String sm=filename.replaceAll("/", "__");
		return sm;
	}
	@Override
	public void load() throws IOException {
		
		BufferedReader f2 = new BufferedReader(new FileReader(filename+File.separator+"ids")) ;
		String line ;
		while((line=f2.readLine())!=null) {
			int i = Integer.parseInt(line.split("\t")[0]) ;
			String n = line.split("\t")[1] ;
			ids.put(i, n) ;
			ids_inv.put(n, i) ;
			//System.out.println(i+" "+n);
		}
		
//		File rep = new File(filename);
//		File[] fichiers = rep.listFiles() ;
//		for(File f : fichiers) {
//			String name = f.getAbsolutePath();
//			name = name.substring(name.lastIndexOf(File.separator)+1) ;
//			if(name.equals("ids"))
//				continue ;
//			InferFonctionFactory infact=new InferFonctionFactory(3);
//    		Fonction fon=infact.buildFonction();
//    		try{
//    			//System.out.println("--");
//    			m.put(ids_inv.get(name),new ParametrizedModel(fon,filename+File.separator+name)) ;
//    			System.out.println("loading "+filename+File.separator+name);
//    			m.get(ids_inv.get(name)).load();
//    			//System.out.println(ids_inv.get(name));
//    		}
//    		catch(NumberFormatException e){
//    			e.printStackTrace();
//    		}
//		}
		loaded=true;
	}

	@Override
	public void save() throws IOException {
//		new File(filename).mkdir() ;
//		for(Integer u : m.keySet()) {
//			new File(filename+File.separator+User.getUser(ids.get(u)).getName()).createNewFile() ; ;
//			m.get(u).save();
//		}
		PrintStream f = new PrintStream( new File(filename+File.separator+"ids")) ;
		for(Entry<Integer, String> e : ids.entrySet()) {
			f.println(e.getKey()+"\t"+e.getValue()) ;
		}
	}
	
	
	public int inferSimulation(Structure struct){
		throw new RuntimeException("Not implemented");
	}

	@Override
	public int infer(Structure struct) {
		
		/*PropagationStruct pstruct = (PropagationStruct)struct ;
        HashMap<String,Long> contaminated=pstruct.getInitContaminated();
        HashMap<String,Double> finalcontaminated=new HashMap<String,Double>() ;
        ArrayList<HashMap<Integer, Double>> sample = new ArrayList<HashMap<Integer,Double>>();
        
        
        HashMap<Integer, Double> initinfect=new HashMap<Integer, Double>() ;
        HashMap<String, Double> initinfect2=new HashMap<String, Double>() ;
        
        for(String u : contaminated.keySet()) {
        	initinfect.put(ids_inv.get(u), 1.0) ;
        	initinfect2.put(u, 1.0) ;
        }

        for(Integer u : m.keySet()) {
        	
        	// Creation de la version speciale pour ce user.
            ArrayList<HashMap<Integer,Double>> thisSample = new ArrayList<HashMap<Integer,Double>>() ;
            Set<String> predName = User.getUser(ids.get(u)).getPredecesseurs().keySet() ;
    		List<String> sortedL = sortSet(predName) ;
			HashMap<Integer,Double> newc = new HashMap<Integer, Double>() ;
			for(Entry<Integer,Double> e : initinfect.entrySet()) {
				if(predName.contains(ids.get(e.getKey()))) {
					newc.put(sortedL.indexOf(e.getKey()), e.getValue()) ;
				}
			}

			thisSample.add(newc) ;

        	//finalcontaminated.put(ids.get(u),(double) (m.get(u).infer(thisSample).get(0)>=0 ? 1:0)) ;
			InferFonctionFactory infact=new InferFonctionFactory(3);
    		Fonction fon=infact.buildFonction();
    		ParametrizedModel pm = new ParametrizedModel(fon,filename+File.separator+ids.get(u));
    		try {
				pm.load();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		finalcontaminated.put(ids.get(u),(double) (pm.infer(thisSample).get(0)>=0 ? 1:0)) ;
    		
        }
        ArrayList<HashMap<String, Double>> result = new ArrayList<HashMap<String,Double>>() ;
        result.add(initinfect2);
        result.add(finalcontaminated) ;
        pstruct.setInfections(result) ;
        
        //pstruct.setInfections(finalcontaminated) ;
        */
		return 0;
	}
	
	public void learn(String db, String cascadesCollection,String usersCollection,int initWindow) throws IOException {
		
		HashMap<Integer, HashMap<User, Long>> cascades = CTICmodel.getTimeSteps(db,cascadesCollection,1) ;
		
		
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,cascadesCollection);
        DBCursor cursor = col.find();
        Post p=null;
        
        // Charger tout les users.
        HashSet<User> users=new HashSet<User>(User.users.values());
        for(User u:users){
            u.loadLinksFrom(db, usersCollection, 0);
            System.out.println("Liens user "+u+" charge");
        }
        
        // Init des examples et des labels.
        ArrayList<HashMap<Integer, Double>> samples = new ArrayList<HashMap<Integer,Double>>();
        HashMap<User,ArrayList<Double>> labels = new HashMap<User, ArrayList<Double>>() ;
        
        
        // Creer une liste de labels pour chaque utilisateurs
        for(User u : users) {
        	labels.put(u,new ArrayList<Double>()) ;
        	ids.put(u.getID(),u.getName()) ;
        	System.out.println(u.getID() +" ; "+u.getName());
        	ids_inv.put(u.getName(),u.getID()) ;
        }
        
        System.out.println("ids : "+ids.size());
        
        
        // Creation de l'ensemble de hashmaps "infections initial"
        try {
        	int iii=0 ;
        	//System.out.println(iii);
            for(Integer cid : cascades.keySet()) { // Pour chaque cascade
                //DBObject res=cursor.next();
                //Cascade c=Cascade.getCascadeFrom(res);
                
                
                for(User u : users) {
                	labels.get(u).add(0.0) ; // Ajouter un label "non infecte" a CHAQUE user
                }
                
                // Convertir la cascade en infection initiale.
                //HashMap<User, Long> h = c.getSimpleHashMap();
                HashMap<User, Long> h = cascades.get(cid) ;
                HashMap<Integer, Double> truc = new HashMap<Integer, Double>();
                //System.out.print("1 ");
                for(User i : h.keySet()) {
                	labels.get(i).set(iii,1.0) ;
                	if(h.get(i)<initWindow) {
                		truc.put(i.getID(),(double)h.get(i)) ;
                	}
                	//ids.put(i.getID(),i.getName()) ;
                	//ids_inv.put(i.getName(),i.getID()) ;
                }
                samples.add(truc);
                //labels.add(more>=0.5*init ? 1.0 : -1.0) ;
                //System.out.print(more>=0.5*init ?"y" : "n");
//               if(iii++>20)
//                	break ;
               iii++ ;
            }
        } catch(Exception e) {
        	e.printStackTrace();
        	return ;
        }
        
        // A ce stage, "sample" contient les vecteur d'infection initiaux sur l'ensemble des users.
        int iii = 0 ;
        // On va maintenant apprendre un classifieur par utilisateur.
        for(User u : users ) {
        	if(u.getName()==null){
        		System.out.println("OMG ! "+u.getID());
        	}
        	InferFonctionFactory infact=new InferFonctionFactory(3);
    		Fonction f=infact.buildFonction();
    		System.out.println(filename+File.separator+u.getName());
    		this.m.put(u.getID(),new ParametrizedModel(f,filename+File.separator+u.getName()));
    		Optimizer opt=(new OptimizerFactory(1)).buildOptimizer();
    		Fonction loss=(new LossFonctionFactory(1)).buildFonction();
    		Parameters par=m.get(u.getID()).getParams();
    		HashMap<Integer,Double> pa=new HashMap<Integer,Double>();
    		pa.put(0, -1.0);
    		pa.put(1, 1.0);
    		par.setParams(pa);
    		
    		System.out.println("\n go learn : "+samples.size() +" "+labels.get(u).size());
    		
    		
    		// Construction du sample pour cet utilisateur :
    		Set<String> predName = u.getPredecesseurs().keySet() ;
    		List<String> sortedL = sortSet(predName) ;
    		//System.out.println(sortedL);
    		ArrayList<HashMap<Integer, Double>> thisSample = new ArrayList<HashMap<Integer,Double>>() ; // La liste d'infection init POUR CET USER
    		for(HashMap<Integer,Double> c: samples) {
    			HashMap<Integer,Double> newc = new HashMap<Integer, Double>() ;
    			for(Entry<Integer,Double> e : c.entrySet()) {
    				//System.out.println("Entry : "+e);
    				//System.out.println(ids.get(e.getKey()));
    				if(sortedL.contains(ids.get(e.getKey()))) {
    					//System.out.println("index : "+sortedL.indexOf(ids.get(e.getKey())));
    					newc.put(sortedL.indexOf(ids.get(e.getKey())), 1.0) ;
    					//System.out.println("Name "+ids.get(e.getKey()));
    				}
    			}
    			//System.out.println("SIZE : " + newc.size());
    			thisSample.add(newc) ;

    		}
    		
    		
    		m.get(u.getID()).learn(thisSample, labels.get(u), loss, opt,0.3);
//    		if(iii++>20)
//    			break ;
    		iii++;
    		
    		new File(filename).mkdir() ;
    		new File(filename+File.separator+ids.get(u)).createNewFile() ; ;
    		m.get(u.getID()).save();	
    		m.remove(u.getID()) ;
    		
    		//System.out.println(m.get(u.getID()).getParams().toString());
    		//System.out.println(m.get(u.getID()).getParams().size());
    		
    		//break ;
        }
        loaded=true;
		
	}
	
	public String toString() {
		return "Modeles Lineaire\n"+m.toString() ;
	}
	
	
	public List<String> sortSet(Set<String> s) {
		
		ArrayList<String> l = new ArrayList<String>();
		while(0 < s.size()) {
			String min = (String)s.toArray()[0] ;
			for(String i : s) {
				if (i.compareTo(min)<=0)
					min=i ;
			}
			l.add(min) ;
			s.remove(min) ;
		}
		return l ;
	}
	
	
	
	public static void main(String args[]) {
		
		LinearModel lm = new LinearModel("/local/bourigaults/testlm") ;
		try {
			lm.learn("us_elections5000", "cascades_4", "users_1", 86400) ;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
//		ArrayList<HashMap<Integer, Double>> samples = new ArrayList<HashMap<Integer,Double>>();
//		HashMap<Integer, HashMap<User, Long>> cascades = CTICmodel.getTimeSteps("us_elections5000", "cascades_4",1) ;
//		for(Integer cid : cascades.keySet()) {
//            HashMap<User, Long> h = cascades.get(cid) ;
//            HashMap<Integer, Double> truc = new HashMap<Integer, Double>();
//            for(User i : h.keySet()) {
//            	if(h.get(i)<86400) {
//            		truc.put(i.getID(),(double)h.get(i)) ;
//            	} 
//            }
//            samples.add(truc);
//        }
		
		
		try {
			lm.save();
			//System.out.println(lm);
			//lm = new LinearModel("/local/bourigaults/testlm") ;
			//lm.load() ;
			//System.out.println(lm);

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
