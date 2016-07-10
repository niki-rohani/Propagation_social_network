package thibault.simBandit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.dense.BasicVector;

public class LaunchCollectsPoisson {

	public static void main(String[] args) throws IOException {


		int nbArms=10;
		int nbTimeStep=5000;
		int nbToSelect=1;
		int freqReq=10;
		int sizeFeatures=4;
		int nbSim=20;
		ArrayList<Double> thetas = null;
		ArrayList<Policy> policies = new ArrayList<Policy>(); 
		ArrayList<Policy> policiesHidden = new ArrayList<Policy>();
		//policies.add(new RandomPolicy());
		//policies.add(new CUCBPolicy());
		//policies.add(new CUCBVPolicy());
		//policies.add(new ThompsonSimple());
		//policies.add(new ThompsonkPoissonSimple(true));
		//policies.add(new ThompsonkPoissonSimple(false));
		//policies.add(new ThompsonkPoisson(sizeFeatures,1.0,true));
		//policies.add(new ThompsonkPoisson(sizeFeatures,1.0,false));
		//policies.add(new ThompsonkLin(sizeFeatures,1.0,true));
		
		policies.add(new kPoissonUCB(sizeFeatures,1.0,2.0));
		policies.add(new kPoissonUCB(sizeFeatures,1.0,1.0));
		policies.add(new kPoissonUCB(sizeFeatures,1.0,0.5));
		policies.add(new kLinUCB(sizeFeatures,1.0,2.0));
		policies.add(new kLinUCB(sizeFeatures,1.0,1.0));
		policies.add(new kLinUCB(sizeFeatures,1.0,0.5));
		policies.add(new ThompsonkLin(sizeFeatures,1.0,false));
		policies.add(new ThompsonkPoisson(sizeFeatures,1.0,false));
		policies.add(new ThompsonkLin(sizeFeatures,1.0,true));
		policies.add(new ThompsonkPoisson(sizeFeatures,1.0,true));
		
		//policies.add(new kGLMPoissonUCB(sizeFeatures,1.0,2.0));
		//policies.add(new kGLMPoissonUCB(sizeFeatures,1.0,1.0));
		//policies.add(new kGLMPoissonUCB(sizeFeatures,1.0,0.5));
		
		policies.add(new OptPoisson(sizeFeatures));
		
		//policies.add(new PlayBest());


		String  format = "dd.MM.yyyy_H.mm.ss";
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		String sdate=formater.format(date);

		String outputRep="simResults_"+sdate;

		File rep=new File(outputRep);

		if(!rep.exists()){
			rep.mkdirs();
		}

		ContextuelBanditDataGeneratorPoisson b=new ContextuelBanditDataGeneratorPoisson(nbArms,sizeFeatures);
		
		LinkedHashMap<String,ArrayList<Double>> meanRwd= new LinkedHashMap<String,ArrayList<Double>>();
		LinkedHashMap<String,ArrayList<Double>> meanRwdStar= new LinkedHashMap<String,ArrayList<Double>>();
		LinkedHashMap<String,ArrayList<Double>> finalRwd= new LinkedHashMap<String,ArrayList<Double>>();
		LinkedHashMap<String,ArrayList<Double>> finalRwdStar= new LinkedHashMap<String,ArrayList<Double>>();
		
		for(Policy p: policies){
			finalRwd.put(p.toString(), new ArrayList<Double>());
			finalRwdStar.put(p.toString(), new ArrayList<Double>());
			meanRwd.put(p.toString(),new ArrayList<Double>());
			meanRwdStar.put(p.toString(),new ArrayList<Double>());
			for(int i=0;i<nbTimeStep/freqReq;i++){
				meanRwd.get(p.toString()).add(0.0);
				meanRwdStar.get(p.toString()).add(0.0);
			}
		}

		for(int k=0;k<nbSim;k++){
			System.out.println("SimNumber: "+k);
			b.genere(outputRep, nbTimeStep);
			
			double [] betaStarT = new double[sizeFeatures];
			for(int i=0;i<b.beta.size();i++){betaStarT[i]=b.beta.get(i);}
			for(Policy p: policies){p.betaStar=new ArrayRealVector(betaStarT);}

			String simFileName=outputRep+"/"+"sim.txt";

			for(Policy p: policies){
				CollectAllVisible collect=new CollectAllVisible( nbArms,  nbTimeStep, p,  nbToSelect,  freqReq, simFileName);
				
				if(k==0){
					for(int i=0;i<nbArms;i++){
						Arm a=new Arm(i,sizeFeatures);
						p.arms.add(a);
					}
				}

				ArrayList<ArrayList<Double>> result=collect.run();

				ArrayList<Double> resultRwd =  result.get(0);
				ArrayList<Double> resultRwdStar =  result.get(1);
				//ArrayList<Double> meanRwdTemp = meanRwd.get(p.toString());
				//ArrayList<Double> meanRwdStarTemp = meanRwdStar.get(p.toString());
				
				for(int i=0;i<resultRwd.size();i++){
					double val1=meanRwd.get(p.toString()).get(i);
					meanRwd.get(p.toString()).set(i, val1+resultRwd.get(i)/nbSim);
				}
				finalRwd.get(p.toString()).add(resultRwd.get(resultRwd.size()-1));
				finalRwdStar.get(p.toString()).add(resultRwdStar.get(resultRwdStar.size()-1));
				
			}
		}

		try{
			File f=new File(outputRep+"/"+"meanRwd.txt");
			File f1=new File(outputRep+"/"+"meanRwdStar.txt");
			File f2=new File(outputRep+"/"+"finalRwd.txt");
			File f3=new File(outputRep+"/"+"finalRwdStar.txt");
			PrintStream pS = new PrintStream(f) ;
			PrintStream pS1 = new PrintStream(f1) ;
			PrintStream pS2 = new PrintStream(f2) ;
			PrintStream pS3 = new PrintStream(f3) ;
			
			String lineToWrite="Time"+"\t";
			for(String s:meanRwd.keySet()){
				//System.out.println(s);
				lineToWrite += s+"\t";
			}
			pS.println(lineToWrite);
			
			for(int i=0;i<meanRwd.get("OptPoisson").size();i++){
				lineToWrite=(i+1)*freqReq+"\t";
				for(String s:meanRwd.keySet()){
					//System.out.println(s);
					lineToWrite+= meanRwd.get(s).get(i)+"\t";
				}
				pS.println(lineToWrite);
			}
			pS.close();
			
			String lineToWrite1="Time"+"\t";
			for(String s:meanRwdStar.keySet()){
				lineToWrite1 += s+"\t";
			}
			pS1.println(lineToWrite1);
			
			for(int i=0;i<meanRwdStar.get("OptPoisson").size();i++){
				lineToWrite1=(i+1)*freqReq+"\t";
				for(String s:meanRwdStar.keySet()){
					lineToWrite1+= meanRwdStar.get(s).get(i)+"\t";
				}
				pS1.println(lineToWrite1);
			}
			pS1.close();
			
			
			lineToWrite="";
			for(String s:finalRwd.keySet()){
				lineToWrite += s+"\t";
			}
			pS2.println(lineToWrite);
			
			for(int i=0;i<finalRwd.get("OptPoisson").size();i++){
				lineToWrite="";
				for(String s:finalRwd.keySet()){
					lineToWrite+= finalRwd.get(s).get(i)+"\t";
				}
				pS2.println(lineToWrite);
			}
			pS2.close();
			
			
			lineToWrite1="";
			for(String s:finalRwdStar.keySet()){
				lineToWrite1 += s+"\t";
			}
			pS3.println(lineToWrite);
			
			for(int i=0;i<finalRwdStar.get("OptPoisson").size();i++){
				lineToWrite1="";
				for(String s:finalRwdStar.keySet()){
					lineToWrite1+= finalRwdStar.get(s).get(i)+"\t";
				}
				pS3.println(lineToWrite1);
			}
			pS3.close();
			

		}


		catch(IOException e){
			System.out.println("Probleme ecriture "+"averageResults.txt");
		}

	}



}


