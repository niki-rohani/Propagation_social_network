package experiments;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;



// Classe d'enregistrement de resultats
//

public class Result implements Serializable{
	String experiment;
	ArrayList<String> donnees; // a priori une seule donnee si c'est un result normal, plusieurs seulement si c'est un result resultant un getStats
	ArrayList<String> score_names;
	HashMap<String,Double> scores;
	public Result(String donnee){
		this(null,initArrayList(donnee));
	}
	public Result(String expe,String donnee){
		this(expe,initArrayList(donnee));
	}
	
	private Result(String expe,ArrayList<String> donnees){
		this.experiment=expe;
		this.donnees=donnees;
		scores=new HashMap<String,Double>();
		score_names=new ArrayList<String>();
		//score_names=new ArrayList<String>();
	}
	
	public void setExperiment(String e){
		this.experiment=e;
	}
	
	public void addScore(String name,double val){
		if(!scores.containsKey(name)){
			scores.put(name,val);
			score_names.add(name);
		}
		else{
			System.out.println("Le score "+name+" existe deja !!!");
		}
	}
	
	public HashMap<String,Double> getScores(){
		return(scores);
	}
	public Double getScore(String sname){
		return(scores.get(sname));
	}
	public String getDonnee(){
		return(donnees.get(0));
	}
	public void setDonnee(String s){
		if(donnees.isEmpty())
			donnees.add(s) ;
		else {
			donnees.set(0, s);
		}
	}
	
	// Ajoute un Result => fusion des listes de scores
	// On suppose que les noms donnes aux scores sont differents dans les differents Result fusionnes
	public void add(Result r){
		int i=0;
		for(String name:r.score_names){
			addScore(name,r.getScores().get(name));
		}
	}
	
	public String toString(){
		String s="";
		for(String name:score_names){
			s+=name+"="+scores.get(name)+";";
		}
		return(s);
	}
	
	// Retourne un Result contenant les moyennes et ecarts-types des scores contenus dans la liste de Result passee en paramtres
	// on suppose que tous les result contenus dans la liste concernent la meme expe (tous les results concernent une donnee diff)
	public static Result getStats(ArrayList<Result> results){
		ArrayList<String> donnees=new ArrayList<String>();
		HashMap<String,Double> sums=new HashMap<String,Double>();
		HashMap<String,Integer> nbs=new HashMap<String,Integer>();
		HashMap<String,Double> sums2=new HashMap<String,Double>();
		ArrayList<String> names=new ArrayList<String>();
		int i=0;
		String expe="";
		for(Result r:results){
			if (i==0){
				expe=r.experiment;
			}
			donnees.add(r.donnees.get(0));
			//rep_traces.add(r.getTracesDir(0));
			//sd_files.add(r.getSDFile(0));
			HashMap<String,Double> scores=r.scores;
			//ArrayList<String> score_names=r.score_names;
			for(String name:r.score_names){
				Double score=scores.get(name);
				Integer nb;
				Double sum;
				Double sum2;
				if(nbs.containsKey(name)){
					nb=nbs.get(name);
					sum=sums.get(name);
					sum2=sums2.get(name);
					
				}
				else{
					nb=new Integer(0);
					sum=new Double(0.0);
					sum2=new Double(0.0);
					names.add(name);
				}
				nbs.put(name, new Integer(nb.intValue()+1));
				sums.put(name,new Double(sum.doubleValue()+score));
				sums2.put(name,new Double(sum2.doubleValue()+(score*score)));
				
			}
			i++;
		}
		Result res=new Result(expe,donnees);
		//HashMap<String,Double> scores=res.scores;
		
		for(String s:names){ //nbs.keySet()){
			int nb=nbs.get(s).intValue();
			double sum=sums.get(s).doubleValue();
			double sum2=sums2.get(s).doubleValue();
			if (nb>0){
				sum/=nb;
				sum2/=nb;
			}
			//System.out.println(sum2);
			//System.out.println(sum);
			//System.out.println(sum*sum);
			double ec=Math.sqrt(sum2-sum*sum);
			//System.out.println(s+" "+ec);
			if (nb>1){
				ec=ec*Math.sqrt(((double)nb)/(nb-1));
			}
			res.addScore("moyenne_"+s, sum);
			res.addScore("ecart_"+s, ec);
			//scores.put("moyenne_"+s, sum);
			//scores.put("ecart_"+s, ec);
			//System.out.println(s+" "+ec);
		}
		
		return(res);
	}
	
	
	public void serialize(String filename) throws IOException{
		
			// ouverture d'un flux de sortie vers le fichier "personne.serial"
		    /*File ff=new File(rep_traces.get(0));
		    File p=ff.getParentFile();
		    File rep=new File(p.getAbsolutePath()+"/Results");
		    if (!rep.exists()){
		    	rep.mkdirs();
		    }*/
			//FileOutputStream fos = new FileOutputStream(rep+"/"+toString()+".result");
			FileOutputStream fos = new FileOutputStream(filename);
			// creation d'un "flux objet" avec le flux fichier
			ObjectOutputStream oos= new ObjectOutputStream(fos);
			try {
				// serialisation : ecriture de l'objet dans le flux de sortie
				oos.writeObject(this); 
				
				// on vide le tampon
				oos.flush();
				System.out.println("le result "+toString()+" a ete serialise");
			} finally {
				//fermeture des flux
				try {
					oos.close();
				} finally {
					fos.close();
				}
			}
		
	}
	
	public static Result deserialize(String fileName) throws IOException{
		Result ret=null;
		try{
			
			// ouverture d'un flux d'entree depuis le fichier "personne.serial"
			FileInputStream fis = new FileInputStream(fileName);
			// creation d'un "flux objet" avec le flux fichier
			ObjectInputStream ois= new ObjectInputStream(fis);
			try {	
				// deserialisation : lecture de l'objet depuis le flux d'entree
				ret = (Result) ois.readObject(); 
			} finally {
				// on ferme les flux
				try {
					ois.close();
				} finally {
					fis.close();
				}
			}
		}
		catch(ClassNotFoundException e){
			e.printStackTrace();
			throw new IOException("Probleme classe");
		}
		return(ret);
	}
	
	public static ArrayList<Result> getResultsFromDir(String repertoire){
		ArrayList<Result> results=new ArrayList<Result>();
		try{
			File rep=new File(repertoire);
			File[] files=rep.listFiles();
			for(int i=0;i<files.length;i++){
			   if (files[i].getName().indexOf(".result")>0){
				   Result result=deserialize(files[i].getAbsolutePath());
				   results.add(result);
			   }
			}
		}
		catch(IOException e){
			   System.out.println(e.getMessage());
		}
		return(results);
	}
	
	// Deprecated : utiliser classer ResultFile
	// On suppose que tous les results concernent une meme expe sur des donnees differentes 
	// Utile pour avoir le detail des executions d'une expe sur un ensemble de donnees
	public static void saveResults1DonneeParLigne(ArrayList<Result> results, String fileName) throws FileNotFoundException{
		File dotFile=new File(fileName);

		 FileOutputStream fout = new FileOutputStream(dotFile);

		 PrintStream out = new PrintStream(fout);
		 
		 String s="Pour Expe : \n";
		 s+=results.get(0).experiment;
		 s+="\n";
		 out.println(s);
		 
		 s="";
		 s+="Donnee "; 
		 int taillemaxdonnee=7;
		 ArrayList<String> noms_donnees=new ArrayList<String>();
		 for(Result res:results){
			 for(String ndo:res.donnees){
				 int x=ndo.length();
				 if (x>taillemaxdonnee){
					 taillemaxdonnee=x;
				 }
			 }
		  }
		  for(int j=7;j<taillemaxdonnee;j++){
				 s+=" ";
		  }
		  s+="\t\t";
		 
		  for(Result res:results){
			  for(String ndo:res.donnees){
					 int x=ndo.length();
					 for(int j=x;j<taillemaxdonnee;j++){
						 ndo+=" ";
					 }
					 noms_donnees.add(ndo);
			  }
		  }
		 
		  //ArrayList<String> score_names=new ArrayList<String>(results.get(0).scores.keySet());
		  //ArrayList<String> score_names=results.get(0).score_names;
		  //HashSet<String> dits=new HashSet<String>();
		  ArrayList<String> score_names=results.get(0).score_names;
		  HashSet<String> dits=new HashSet<String>();
		  dits.addAll(score_names);
		  for(Result res:results){
			  Set<String> scn=res.getScores().keySet();
			  for(String nn:scn){
				  if (!dits.contains(nn)){
					  score_names.add(nn);
					  dits.add(nn);
				  }
			  }
		  }
		  
		  ArrayList<Integer> tailles=new ArrayList<Integer>();
		  for (String st : score_names) {
				   s+=st+"\t";
				   int tt=st.length();
				   tailles.add(tt);
		  }
		  out.println(s);
		  out.println();
		  s="";
		  DecimalFormat format = new DecimalFormat();
		  format.setMaximumFractionDigits(3);
		  format.setGroupingUsed(false);
		  int ia=0;
		  for(Result res:results){
			   for(String ndo:res.donnees){
				   int i=0;
				   s+=noms_donnees.get(ia)+"\t\t";
				   ia++;
				   for (String name : score_names) {
					   String val="";
					   if (!res.scores.containsKey(name)){
						   val="NA";
					   }
					   else{
						   Double st=res.scores.get(name);
						   val=format.format(st);
					   }
					   int tt=val.length();
					   int ta=tailles.get(i);
					   s+=val;
					   for(int j=tt;j<ta;j++){
						   s+=" ";
					   }
					   s+="\t";
					   i++;
				   }
				   s+="\n";
			    }
			}
			out.println(s);
			out.close();
	}
	
	
	// On suppose que tous les results concernent une expe differente
	// Utile pour comparer des resultats condenses entre differentes expe
	public static void saveResults1ExpeParLigne(ArrayList<Result> results, String fileName) throws FileNotFoundException{
		 File dotFile=new File(fileName);

		 FileOutputStream fout = new FileOutputStream(dotFile);

		 PrintStream out = new PrintStream(fout);
		 
		 String s="Pour Expes : \n";
		 int nume=1;
		 for(Result res:results){
			 String al=res.experiment;
		     s+="Exp_"+nume+" : "+al+" Sur Donnnees : ";
		     for (String st : res.donnees) {
		    	 s+=st+", ";
		     }
		     s+="\n";
		     nume++;
		 }
		 out.println(s);
		 
		 
		 out.println();
		 s="";
		 s+="Experiment "; 
		 int taillemaxexpe=11;
		 nume=1;
		 ArrayList<String> noms_expes=new ArrayList<String>();
		 for(Result res:results){
			 String nnume="Exp_"+nume;
			 int x=nnume.length();
			 if (x>taillemaxexpe){
				 taillemaxexpe=x;
			 }
			 nume++;
		 }
		 for(int j=11;j<taillemaxexpe;j++){
			   s+=" ";
		 }
		 s+="\t\t";
		 nume=1;
		 for(Result res:results){
			 String nnume="Exp_"+nume;
			 int x=nnume.length();
			 for(int j=x;j<taillemaxexpe;j++){
				   nnume+=" ";
			 }
			 noms_expes.add(nnume);
			 nume++;
		 }
		 
		 
		 ArrayList<String> score_names=results.get(0).score_names;
		 HashSet<String> dits=new HashSet<String>();
		 dits.addAll(score_names);
		 for(Result res:results){
			  Set<String> scn=res.getScores().keySet();
			  for(String nn:scn){
				  if (!dits.contains(nn)){
					  score_names.add(nn);
					  dits.add(nn);
				  }
			  }
		  }
		 
		 ArrayList<Integer> tailles=new ArrayList<Integer>();
		 for (String st : score_names) {
			   s+=st+"\t";
			   int tt=st.length();
			   tailles.add(tt);
		 }
		 
		 
		 out.println(s);
		 out.println();
		 s="";
		 DecimalFormat format = new DecimalFormat();
	     format.setMaximumFractionDigits(3);
	     format.setGroupingUsed(false);
	     
	     int ia=0;
		 for(Result res:results){
		   int i=0;
		   s+=noms_expes.get(ia)+"\t\t";
		   ia++;
		   for (String name : score_names) {
			   String val="";
			   if (!res.scores.containsKey(name)){
				   val="NA";
			   }
			   else{
				   Double st=res.scores.get(name);
				   val=format.format(st);
			   }
			   int tt=val.length();
			   int ta=tailles.get(i);
			   s+=val;
			   for(int j=tt;j<ta;j++){
				   s+=" ";
			   }
			   s+="\t";
			   i++;
		   }
		   s+="\n";
		 }
		 out.println(s);
		 out.close();
	}
	
	
	private static ArrayList<String> initArrayList(String s){
		ArrayList<String> a=new ArrayList<String>();
		a.add(s);
		return(a);
	}

}
