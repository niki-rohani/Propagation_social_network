package thibault.SNCollect;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import core.Post;
import core.User;
import experiments.Result;
import experiments.ResultFile;

public class CollectEval extends Thread{
	private CollectBase model;
	private CollectEvalMeasureList mesRealTime;
	//private CollectEvalMeasureList mesFinal;

	private TreeMap<Long,ResultFile> rf;
	private String name;
	private int freqRecords;
	private String outputRep;

	public CollectEval(CollectBase mod,CollectEvalMeasureList mesRealTime, int freqRecords, String name, String output){
		//models=new ArrayList<PropagationModel>();
		//loadModels();
		this.model=mod;
		this.mesRealTime=mesRealTime;
		//this.mesFinal=mesFinal;
		this.outputRep=output;

		rf=new TreeMap<Long,ResultFile>();
		this.name=name;
		this.freqRecords=freqRecords;
		//this.allUsers=allUsers;
		//loadMeasures();
	}

	public CollectEval(){
	}


	public TreeMap<Long,Result> go(ArrayList<Reward> rewards, HashMap<String,String> pars){
		if(pars.containsKey("maxT")){
			this.model.setMaxT(Long.valueOf(pars.get("maxT")));
		}
		long nbResultPoints=10;
		if(pars.containsKey("NbResultPoints")){
			nbResultPoints=Long.valueOf(pars.get("nbResultPoints"));
		}

		long freqAffiche=1;

		if(pars.containsKey("freqAffiche")){
			freqAffiche=Long.valueOf(pars.get("freqAffiche"));
		}

		if(pars.containsKey("freqRecords")){
			freqRecords=Integer.valueOf(pars.get("freqRecords"));
		}

		//TreeMap<Long,ArrayList<Result>> ret=new TreeMap<Long,ArrayList<Result>>();
		try{
			int j=0;

			for(Reward r:rewards){
				j++;
				String entete="Modele : \t"+this.model.toString()+"\n";
				//entete+="Reward : \t "+r.toString()+"\n";
				entete+=this.mesRealTime.getName()+"\n";
				ResultFile rfr=new ResultFile(outputRep+"/"+name+"_Reward"+j+".txt");
				rfr.setEntete(entete);
				CollectRecorder recorder=new CollectRecorder(name,r,this.freqRecords,1000,this.mesRealTime,rfr);
				recorder.freqAffiche=freqAffiche;
				//recorder.setResultFile(rfr);
				System.out.println("Reward"+j);
				this.model.run(recorder);
				TreeMap<Long, Result> results=recorder.getResults();
				long nbTot=results.size();
				//long point=0; 
				//long longPoint=(nbTot/nbResultPoints);
				//long i=1;
				/*for(Long t:results.keySet()){
					if(i>=point){
						ResultFile rfi=rf.get(t);
						Result result=results.get(t);
						if(rfi==null){
							rfi=new ResultFile(outputRep+"/"+name+"_t"+t+".txt");
							rf.put(t,rfi);
							String entete2="Modele : \t"+this.model.toString()+"\n";
							entete2+="Time "+t+"\n";
							entete2+=this.mesRealTime.getName()+"\n";
							rfi.setEntete(entete2);
							//result.setExperiment("Modele: "+model.toString()+" t="+t);
						}
						result.setDonnee("Reward_"+j);
						rfi.append(result);
						ArrayList<Result> retl=ret.get(t);
						if(retl==null){
							retl=new ArrayList<Result>();
							ret.put(t, retl);
						}
						retl.add(result);
						point+=longPoint;
					}
					i++;
				}*/


			}

		}
		catch(IOException e){
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());

		}
		TreeMap<Long,Result> lstats=new TreeMap<Long,Result>();
		/*for(Long t:ret.keySet()){
			Result r=Result.getStats(ret.get(t)); //name+"_t"+t,name+"_t"+t);
			r.setDonnee(name+"_t"+t);
			lstats.put(t, r);
		}*/
		return(lstats);
	}


	public static void run(CollectEvalConfigBase config,String outputRep) throws FileNotFoundException, Exception{

		if (outputRep.length()==0){
			String format = "dd.MM.yyyy_H.mm.ss";
			java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
			Date date = new Date(); 
			String sdate=formater.format(date);
			outputRep="./CollectResults/CollectResults_"+sdate;
		}

		File rep=new File(outputRep);

		if(rep.exists()){
			try{
				Reader reader = new InputStreamReader(System.in);
				BufferedReader input = new BufferedReader(reader);
				System.out.print("Warning : "+rep+" already exists, overwrite ? (Y/N)");
				String ok = input.readLine();
				if ((ok.compareTo("Y")!=0) && (ok.compareTo("y")!=0)){return;}
				for(File f:rep.listFiles()){
					f.delete();
				}
				rep.delete();
			}
			catch(IOException e){
				e.printStackTrace();
				return;
			}
		}
		rep.mkdirs();



		ArrayList<CollectBase> models=config.getModels();
		CollectEvalMeasureList evMes=config.getMeasures();
		HashMap<String,String> pars=config.getParams();
		ArrayList<Reward> rewards=config.getRewards();

		File f=new File(outputRep+"/Rewards.txt");
		PrintWriter out=null;
		try{
			out=new PrintWriter(new BufferedWriter(new FileWriter(f)));
			out.println("Rewards ");
			int i=0;
			for(Reward r:rewards){
				i++;
				out.println(i+"\t"+r);
			}
			out.close();
		}
		catch(IOException e){
			throw new RuntimeException(e);
		}

		TreeMap<Long,ResultFile> rf=new TreeMap<Long,ResultFile>();
		int i=1;
		String titre="CollectEval \n\n Modeles : \n";
		for(CollectBase m:models){
			titre+="\t model_"+i+": \t "+m.toString()+"\n";
			i++;
		}
		titre+="\n Rewards : \n"; 
		i=1;
		for(Reward r:rewards){
			//titre+="\t reward_"+i+": \t "+r.toString()+"\n";
			titre+="\t reward_"+i+"\n";
			i++;
		}

		titre+="\n "+evMes.getName()+"\n";
		titre+="\n Params : \n";
		for(String par:pars.keySet()){
			titre+="\t "+par+" = "+pars.get(par)+"\n";
		}

		i=1;
		for (CollectBase pm:models){
			String modch="model_"+i;

			CollectEval expe=new CollectEval(pm,evMes,Integer.parseInt(pars.get("freqRecords")),modch,outputRep);
			User.reinitUsers();
			Post.reinitPosts();
			//System.gc();
			TreeMap<Long,Result> res=expe.go(rewards,pars);
			if(i==1){
				for(Result resi:res.values()){resi.setExperiment(titre);}
			}
			try{
				for(Long t:res.keySet()){
					ResultFile filei=rf.get(t);
					if(filei==null){
						filei=new ResultFile(outputRep+"/stats_"+t+".txt");
						rf.put(t,filei);
					}
					filei.append(res.get(t));
				}



			}
			catch(IOException e){
				e.printStackTrace();
			}
			i++;
		}

	}

	public String getDescription(){
		return("CascadeSetFeatures");
	}

	//public static int iteration=0;
	public static void catchExceptLive(String fileNameInit,String fileNameRwd,int idPolicy, int nbArms, int timeWindow, int compte,String outputRep) throws FileNotFoundException, Exception{
		try {
			run(new LiveBase( fileNameInit,fileNameRwd,idPolicy,nbArms,timeWindow,compte), outputRep);
		} catch (ConcurrentModificationException e) {
			long startTime = System.currentTimeMillis();
			System.out.println("Exception concurrente, programme relance");
			while (System.currentTimeMillis() - startTime <= 10000){}		
		}
	}

	public static void main(String[] args) throws FileNotFoundException, Exception{
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		String format = "dd.MM.yyyy_H.mm.ss";
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		String sdate=formater.format(date);



		int i = Integer.parseInt(args[0]);

		String folderPath;
		CollectEvalConfigBase col;
		ResultsConcatener concatener;
		switch (i)
		{
		case 0: //modele lang a partir d un fichier de reward
			col=new LanguageBase(args[4],args[5],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]));
			folderPath="./CollecteNormal"+args[4]+"/langModel/idPol"+args[1]+"k"+args[2]+"T"+args[3];
			run(col,folderPath);
			concatener = new ResultsConcatener(1,col.getRewards().size(),folderPath+"/");
			concatener.concatenate(); 
			break;  
		case 1: //model RT
			col=new CounterNormBase(args[4],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[5]),Double.parseDouble(args[6]));
			folderPath="./CollecteNormal"+args[4]+"/RTModel/idPol"+args[1]+"k"+args[2]+"T"+args[3];
			run(col,folderPath);
			concatener = new ResultsConcatener(1,col.getRewards().size(),folderPath+"/");
			concatener.concatenate(); 
			break;   
		case 2: //model hybrid (parti lang a partir d un fichier)
			col=new HybridBase(args[4],args[5],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]));
			folderPath="./CollecteNormal"+args[4]+"/hybridModel/idPol"+args[1]+"k"+args[2]+"T"+args[3];
			run(col,folderPath);
			concatener = new ResultsConcatener(1,col.getRewards().size(),folderPath+"/");
			concatener.concatenate(); 
			break;  
		case 3: //model nbRT 
			col=new CounterBase(args[4],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[5]));
			folderPath="./CollecteNormal"+args[4]+"/counterModel"+Integer.parseInt(args[5])+"/idPol"+args[1]+"k"+args[2]+"T"+args[3];
			run(col,folderPath);
			concatener = new ResultsConcatener(1,col.getRewards().size(),folderPath+"/");
			concatener.concatenate(); 
			break; 

		case 4: //model sentiment
			col=new SentimentBase(args[4],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]));
			folderPath="./CollecteNormal"+args[4]+"/sentimentModel/idPol"+args[1]+"k"+args[2]+"T"+args[3];
			run(col,folderPath);
			concatener = new ResultsConcatener(1,col.getRewards().size(),folderPath+"/");
			concatener.concatenate(); 
			break; 

		case 5: //collect live
			int iteration=0;
			while(true){
				try {
					iteration++;
					String  repOut="./CollecteLive"+sdate+"/idPol"+args[1]+"k"+args[2]+"T"+args[3]+iteration;
					run(new LiveBase(args[4],args[5],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[6])),repOut);  
				}
				catch(ConcurrentModificationException e){
					iteration++;  
					long startTime = System.currentTimeMillis();
					System.out.println("Exception concurrente, programme relance");
					while (System.currentTimeMillis() - startTime <= 10000){}
					String  repOut="./CollecteLiveBandit"+sdate+"/idPol"+args[1]+"k"+args[2]+"T"+args[3]+iteration;
					catchExceptLive(args[4],args[5],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[6]),repOut);
				}
			}
			
		case 6://Contextuel language
			  col=new LanguageContext(args[4],args[5],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[6]),Integer.parseInt(args[7]),Integer.parseInt(args[8]));
			  folderPath="./CollecteContextual"+args[4]+"/langModel/idPol"+args[1]+"k"+args[2]+"T"+args[3]+"Case"+Integer.parseInt(args[8]);
			  run(col,folderPath);
			  concatener = new ResultsConcatener(1,col.getRewards().size(),folderPath+"/");
			  concatener.concatenate();  
			  break; 
			  
		case 7: //model RT
			col=new CounterNormContext(args[4],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[5]),Double.parseDouble(args[6]),Integer.parseInt(args[7]),Integer.parseInt(args[8]),Integer.parseInt(args[9]));
			folderPath="./CollecteContextual"+args[4]+"/countNormModel/idPol"+args[1]+"k"+args[2]+"T"+args[3]+"Case"+Integer.parseInt(args[9]);
			run(col,folderPath);
			concatener = new ResultsConcatener(1,col.getRewards().size(),folderPath+"/");
			concatener.concatenate(); 
			break;  
			
		case 8: //model hybrid (parti lang a partir d un fichier)
			col=new HybridContext(args[4],args[5],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[6]),Integer.parseInt(args[7]),Integer.parseInt(args[8]));
			folderPath="./CollecteContextual"+args[4]+"/hybridModel/idPol"+args[1]+"k"+args[2]+"T"+args[3]+"Case"+Integer.parseInt(args[8]);
			run(col,folderPath);
			concatener = new ResultsConcatener(1,col.getRewards().size(),folderPath+"/");
			concatener.concatenate(); 
			break;  
		case 9: //model nbRT
			col=new CounterContext(args[4],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[5]),Integer.parseInt(args[6]),Integer.parseInt(args[7]),Integer.parseInt(args[8]));
			folderPath="./CollecteContextual"+args[4]+"/countModel"+Integer.parseInt(args[5])+"/idPol"+args[1]+"k"+args[2]+"T"+args[3]+"Case"+Integer.parseInt(args[8])+"test";
			run(col,folderPath);
			concatener = new ResultsConcatener(1,col.getRewards().size(),folderPath+"/");
			concatener.concatenate(); 
			break;
			
		case 10: //model sentiment
			col=new SentimentContext(args[4],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[5]),Integer.parseInt(args[6]),Integer.parseInt(args[7]));
			folderPath="./CollecteContextual"+args[4]+"/sentimentModel/idPol"+args[1]+"k"+args[2]+"T"+args[3]+"Case"+Integer.parseInt(args[7]);;
			run(col,folderPath);
			concatener = new ResultsConcatener(1,col.getRewards().size(),folderPath+"/");
			concatener.concatenate(); 
			break; 
		case 11: //model hybrid (parti lang a partir d un fichier)
			col=new HybridContextHidden(args[4],args[5],Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[6]),Integer.parseInt(args[7]),Integer.parseInt(args[8]));
			folderPath="./CollecteContextualHidden"+args[4]+"/hybridModel/idPol"+args[1]+"k"+args[2]+"T"+args[3]+"Case"+Integer.parseInt(args[8]);
			run(col,folderPath);
			concatener = new ResultsConcatener(1,col.getRewards().size(),folderPath+"/");
			concatener.concatenate(); 
			break;  
		default:            
		}

	}
}
