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

public class LaunchCollectsHybrid {

	public static void main(String[] args) throws IOException {


		int nbArms=50;
		int nbTimeStep=5000;
		int nbToSelect=5;
		int freqReq=10;
		int sizeFeatures=4;
		int nbSim=50;
		//int nbObserved=1;
		ArrayList<Integer> nbObserved =new ArrayList<Integer>() ;
		nbObserved.add(0);
		/*nbObserved.add(2);
		nbObserved.add(3);
		nbObserved.add(5);
		nbObserved.add(7);
		nbObserved.add(10);
		nbObserved.add(15);
		nbObserved.add(20);
		nbObserved.add(25);
		nbObserved.add(30);
		nbObserved.add(35);
		nbObserved.add(40);
		nbObserved.add(45);
		nbObserved.add(50);*/
		ArrayList<Double> thetas = null;
		ArrayList<Policy> policies = new ArrayList<Policy>(); 
		ArrayList<Policy> policiesHidden = new ArrayList<Policy>();
		//policies.add(new RandomPolicy());
		policies.add(new CUCBPolicy());
		policies.add(new CUCBVPolicy());
		//policies.add(new CUCBTunedPolicy());
		//policies.add(new kLinUCB(sizeFeatures));
		//policies.add(new ThompsonkLin(sizeFeatures));
		policies.add(new kHybridLinUCB(sizeFeatures));
		//policies.add(new ThompsonHybridkLin(sizeFeatures,1.0,false));
		policies.add(new OptContextual(sizeFeatures));
		

		
		//policiesHidden.add(new NaivekPartialHybridLinUCBMean(sizeFeatures,0.95,0));
		for(int l=0;l<nbObserved.size();l++){
			policiesHidden.add(new NaivekPartialHybridLinUCBSample(sizeFeatures,0.95,nbObserved.get(l),0));
		}
		
		//policiesHidden.add(new NaivekPartialHybridLinUCBSample(sizeFeatures,0.95,0,0));
		
		//policiesHidden.add(new ThompsonkPartialHybridLin(sizeFeatures,0));
		
		//policiesHidden.add(new kPartialHybridLinUCBsameScores(sizeFeatures,0.95,0.3,0));
		//policiesHidden.add(new kPartialHybridLinUCBsameScores(sizeFeatures,0.95,0.5,0));
		//policiesHidden.add(new kPartialHybridLinUCBsameScores(sizeFeatures,0.95,0.7,0));
		
		//policiesHidden.add(new ThompsonkPartialHybridLin(sizeFeatures,0));
		//policiesHidden.add(new ThompsonkPartialLin(sizeFeatures));
		//policiesHidden.add(new kPartialHybridLinUCBdiffScores(sizeFeatures,0.99,0.5,0));
		//policiesHidden.add(new kPartialHybridLinUCBdiffScores(sizeFeatures,0.99,0.3,0));
		
		/*policiesHidden.add(new kPartialHybridLinUCBdiffScores(sizeFeatures,0.95,0.5,1));
		policiesHidden.add(new kPartialHybridLinUCBdiffScores(sizeFeatures,0.99,0.5,1));
		policiesHidden.add(new kPartialHybridLinUCBdiffScores(sizeFeatures,0.99,0.3,1));
		policiesHidden.add(new kPartialHybridLinUCBdiffScores(sizeFeatures,0.99,0.1,1));*/
		
		
		/*policiesHidden.add(new kPartialHybridLinUCBsameScores(sizeFeatures,0.95,0.5,1));
		policiesHidden.add(new kPartialHybridLinUCBsameScores(sizeFeatures,0.99,0.5,1));
		policiesHidden.add(new kPartialHybridLinUCBsameScores(sizeFeatures,0.99,0.3,1));
		policiesHidden.add(new kPartialHybridLinUCBsameScores(sizeFeatures,0.99,0.1,1));*/
		/*policiesHidden.add(new kPartialHybridLinUCB(sizeFeatures,0.7));
		policiesHidden.add(new kPartialHybridLinUCB(sizeFeatures,0.95));
		policiesHidden.add(new kPartialHybridLinUCB(sizeFeatures,0.99));*/


		
		String  format = "dd.MM.yyyy_H.mm.ss";
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		String sdate=formater.format(date);

		String outputRep="simResults_"+sdate;

		File rep=new File(outputRep);

		if(!rep.exists()){
			rep.mkdirs();
		}
		
		ContextuelBanditDataGeneratorHybrid b=new ContextuelBanditDataGeneratorHybrid(nbArms,sizeFeatures);
		
		
		LinkedHashMap<String,ArrayList<Double>> meanRwd= new LinkedHashMap<String,ArrayList<Double>>();
		//LinkedHashMap<String,ArrayList<Double>> meanRwdStar= new LinkedHashMap<String,ArrayList<Double>>();
		LinkedHashMap<String,ArrayList<Double>> finalRwd= new LinkedHashMap<String,ArrayList<Double>>();
		//LinkedHashMap<String,ArrayList<Double>> finalRwdStar= new LinkedHashMap<String,ArrayList<Double>>();
		
		for(Policy p: policies){
			finalRwd.put(p.toString(), new ArrayList<Double>());
			//finalRwdStar.put(p.toString(), new ArrayList<Double>());
			meanRwd.put(p.toString(),new ArrayList<Double>());
			//meanRwdStar.put(p.toString(),new ArrayList<Double>());
			for(int i=0;i<nbTimeStep/freqReq;i++){
				meanRwd.get(p.toString()).add(0.0);
				//meanRwdStar.get(p.toString()).add(0.0);
			}
		}
		
		for(Policy p: policiesHidden){
			finalRwd.put(p.toString(), new ArrayList<Double>());
			//finalRwdStar.put(p.toString(), new ArrayList<Double>());
			meanRwd.put(p.toString(),new ArrayList<Double>());
			//meanRwdStar.put(p.toString(),new ArrayList<Double>());
			for(int i=0;i<nbTimeStep/freqReq;i++){
				meanRwd.get(p.toString()).add(0.0);
				//meanRwdStar.get(p.toString()).add(0.0);
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
						a.thetaStar=b.thetas.get(i);
						p.arms.add(a);
					}
				}

				ArrayList<ArrayList<Double>> result=collect.run();

				ArrayList<Double> resultRwd =  result.get(0);
				//ArrayList<Double> resultRwdStar =  result.get(1);
				//ArrayList<Double> meanRwdTemp = meanRwd.get(p.toString());
				//ArrayList<Double> meanRwdStarTemp = meanRwdStar.get(p.toString());
				
				for(int i=0;i<resultRwd.size();i++){
					double val1=meanRwd.get(p.toString()).get(i);
					meanRwd.get(p.toString()).set(i, val1+resultRwd.get(i)/nbSim);
				}
				finalRwd.get(p.toString()).add(resultRwd.get(resultRwd.size()-1));
				//finalRwdStar.get(p.toString()).add(resultRwdStar.get(resultRwdStar.size()-1));
				
			}
			
			for(Policy p: policiesHidden){
				
				
				
				if(k==0){
					for(int i=0;i<nbArms;i++){
						Arm a=new Arm(i,sizeFeatures);
						p.arms.add(a);
					}
				}
				
					CollectHiddenFixeObs collect=new CollectHiddenFixeObs( nbArms,  nbTimeStep, p,  nbToSelect,  freqReq, simFileName,p.nbObserved);
					
					//CollectHiddenRandObs collect=new CollectHiddenRandObs( nbArms,  nbTimeStep, p,  nbToSelect,  freqReq, simFileName);
				
					ArrayList<ArrayList<Double>> result=collect.run();

					ArrayList<Double> resultRwd =  result.get(0);
					//ArrayList<Double> resultRwdStar =  result.get(1);
					//ArrayList<Double> meanRwdTemp = meanRwd.get(p.toString());
					//ArrayList<Double> meanRwdStarTemp = meanRwdStar.get(p.toString());
					
					for(int i=0;i<resultRwd.size();i++){
						double val1=meanRwd.get(p.toString()).get(i);
						meanRwd.get(p.toString()).set(i, val1+resultRwd.get(i)/nbSim);
					}
					finalRwd.get(p.toString()).add(resultRwd.get(resultRwd.size()-1));
					//finalRwdStar.get(p.toString()).add(resultRwdStar.get(resultRwdStar.size()-1));
				}

			}
		
		try{
			File f=new File(outputRep+"/"+"meanRwd.txt");
			//File f1=new File(outputRep+"/"+"meanRwdStar.txt");
			File f2=new File(outputRep+"/"+"finalRwd.txt");
			//File f3=new File(outputRep+"/"+"finalRwdStar.txt");
			PrintStream pS = new PrintStream(f) ;
			//PrintStream pS1 = new PrintStream(f1) ;
			PrintStream pS2 = new PrintStream(f2) ;
			//PrintStream pS3 = new PrintStream(f3) ;
			
			String lineToWrite="Time"+"\t";
			for(String s:meanRwd.keySet()){
				//System.out.println(s);
				lineToWrite += s+"\t";
			}
			pS.println(lineToWrite);
			
			for(int i=0;i<meanRwd.get("OptContextual").size();i++){
				lineToWrite=(i+1)*freqReq+"\t";
				for(String s:meanRwd.keySet()){
					//System.out.println(s);
					lineToWrite+= meanRwd.get(s).get(i)+"\t";
				}
				pS.println(lineToWrite);
			}
			pS.close();
			
			/*String lineToWrite1="Time"+"\t";
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
			*/
			
			lineToWrite="";
			for(String s:finalRwd.keySet()){
				lineToWrite += s+"\t";
			}
			pS2.println(lineToWrite);
			
			for(int i=0;i<finalRwd.get("OptContextual").size();i++){
				lineToWrite="";
				for(String s:finalRwd.keySet()){
					lineToWrite+= finalRwd.get(s).get(i)+"\t";
				}
				pS2.println(lineToWrite);
			}
			pS2.close();
			
			
			/*lineToWrite1="";
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
			pS3.close();*/
			

		}


		catch(IOException e){
			System.out.println("Probleme ecriture "+"averageResults.txt");
		}
		
		
		
		
		
		
		
		
		
		/*for(Policy p: policies){
			ArrayList<Double> tempMeanRwd = new ArrayList<Double>();
			ArrayList<Double> tempMeanRwdStar = new ArrayList<Double>();
			finalRwd.put(p.toString(), new ArrayList<Double>());
			//finalRwdStar.put(p.toString(), new ArrayList<Double>());
			for(int i=0;i<nbTimeStep/freqReq;i++){
				tempMeanRwd.add(0.0);
				tempMeanRwdStar.add(0.0);
			}
			meanRwd.put(p.toString(), tempMeanRwd);
			//meanRwdStar.put(p.toString(), tempMeanRwdStar);
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

				for(int i=0;i<nbArms;i++){
					Arm a=new Arm(i,sizeFeatures);
					p.arms.add(a);
				}
				ArrayList<ArrayList<Double>> result=collect.run();
				ArrayList<Double> resultRwd =  result.get(0);
				ArrayList<Double> resultRwdStar =  result.get(1);
				

				ArrayList<Double> meanRwdTemp = meanRwd.get(p.toString());
				ArrayList<Double> meanRwdStarTemp = meanRwdStar.get(p.toString());
				
				for(int i=0;i<result.size();i++){
					meanRwdTemp.set(i, meanRwdTemp.get(i)+result.get(0).get(i)/nbSim);
					meanRwdStarTemp.set(i, meanRwdStarTemp.get(i)+result.get(1).get(i)/nbSim);
				}
				meanRwd.put(p.toString(), meanRwdTemp);
				meanRwdStar.put(p.toString(), meanRwdStarTemp);
				
				finalRwd.get(p.toString()).add(result.get(0).get(result.size()-1));
				finalRwdStar.get(p.toString()).add(result.get(1).get(result.size()-1));
				
			}
		}
			b.genere(outputRep, nbTimeStep);
			

		
			for(int k=0;k<nbSim;k++){
				System.out.println("SimNumber: "+k);
				b.genere(outputRep, nbTimeStep);
				
				double [] betaStarT = new double[sizeFeatures];
				thetas=new ArrayList<Double>();
				for(int i=0;i<b.beta.size();i++){betaStarT[i]=b.beta.get(i);}
				for(int i=0;i<nbArms;i++){thetas.add(b.thetas.get(i));}
				for(Policy p: policies){p.betaStar=new ArrayRealVector(betaStarT);}

				String simFileName=outputRep+"/"+"sim.txt";

				for(Policy p: policies){
					CollectAllVisible collect=new CollectAllVisible( nbArms,  nbTimeStep, p,  nbToSelect,  freqReq, simFileName);

					for(int i=0;i<nbArms;i++){
						Arm a=new Arm(i,sizeFeatures);
						a.thetaStar=thetas.get(i);
						p.arms.add(a);
					}
					
					ArrayList<ArrayList<Double>> result=collect.run();
					ArrayList<Double> resultRwd =  result.get(0);
					ArrayList<Double> resultRwdStar =  result.get(1);
					

					ArrayList<Double> meanRwdTemp = meanRwd.get(p.toString());
					ArrayList<Double> meanRwdStarTemp = meanRwdStar.get(p.toString());
					
					for(int i=0;i<result.size();i++){
						meanRwdTemp.set(i, meanRwdTemp.get(i)+result.get(0).get(i)/nbSim);
						meanRwdStarTemp.set(i, meanRwdStarTemp.get(i)+result.get(1).get(i)/nbSim);
					}
					meanRwd.put(p.toString(), meanRwdTemp);
					meanRwdStar.put(p.toString(), meanRwdStarTemp);
					
					finalRwd.get(p.toString()).add(result.get(0).get(result.size()-1));
					finalRwdStar.get(p.toString()).add(result.get(1).get(result.size()-1));
					
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
					lineToWrite += s+"\t";
				}
				pS.println(lineToWrite);
				
				for(int i=0;i<meanRwd.get("OptPoisson").size();i++){
					lineToWrite=(i+1)*freqReq+"\t";
					for(String s:meanRwd.keySet()){
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
			}*/
			
			
		/*String simFileName=outputRep+"/"+"sim.txt";
		
		for(Policy p: policies){
			CollectAllVisible collect=new CollectAllVisible( nbArms,  nbTimeStep, p,  nbToSelect,  freqReq, simFileName);
			
			for(int i=0;i<nbArms;i++){
				Arm a=new Arm(i,sizeFeatures);
				if(thetas!=null){
					a.thetaStar=thetas.get(i);
				}
				p.arms.add(a);
			}
			ArrayList<Double> result = collect.run();

			String resultSim=p.toString()+".txt";
			try{
				
				File f=new File(outputRep+"/"+resultSim);
				PrintStream pS = new PrintStream(f) ;
				pS.println("Reward:");
				String lineToWrite = "";
				for(int i=0;i<result.size();i++){
					lineToWrite+=result.get(i)+"\t";
				}
				pS.println(lineToWrite);
				pS.println();
				for(Arm a:p.arms){
					pS.println("Id"+"\t"+"nbPlayed"+"\t"+"empAverage"+"\t"+"empStdDev");
					pS.println(a.Id+"\t"+a.numberPlayed+"\t"+a.sumRewards/a.numberPlayed+"\t"+Math.sqrt((a.sumSqrtRewards/(1.0*a.numberPlayed)-Math.pow(a.sumRewards/(1.0*a.numberPlayed), 2))));
					pS.println("Empirical Mean Vector");
					pS.println(new BasicVector(a.MeanContext));
					pS.println("Empirical Covariance Matrix");
					pS.println(new Basic2DMatrix(a.CovMatrix));
					pS.println();
				}
				pS.close();	
			}
			catch(IOException e){
				System.out.println("Probleme ecriture "+resultSim);
			}

		}
		for(Policy p: policiesHidden){
			CollectHidden collect=new CollectHidden( nbArms,  nbTimeStep, p,  nbToSelect,  freqReq, simFileName);
			for(int i=0;i<nbArms;i++){
				Arm a=new Arm(i,sizeFeatures);
				p.arms.add(a);
			}
			ArrayList<Double> result = collect.run();

			String resultSim=p.toString()+".txt";
			try{
				File f=new File(outputRep+"/"+resultSim);
				PrintStream pS = new PrintStream(f) ;
				pS.println("Reward:");
				String lineToWrite = "";
				for(int i=0;i<result.size();i++){
					lineToWrite+=result.get(i)+"\t";
				}
				pS.println(lineToWrite);
				pS.println();
				for(Arm a:p.arms){
					pS.println("Id"+"\t"+"nbPlayed"+"\t"+"empAverage"+"\t"+"empStdDev");
					pS.println(a.Id+"\t"+a.numberPlayed+"\t"+a.sumRewards/a.numberPlayed+"\t"+Math.sqrt((a.sumSqrtRewards/(1.0*a.numberPlayed)-Math.pow(a.sumRewards/(1.0*a.numberPlayed), 2))));
					pS.println("Empirical Mean Vector");
					pS.println(new BasicVector(a.MeanContext));
					pS.println("Empirical Covariance Matrix");
					pS.println(new Basic2DMatrix(a.CovMatrix));
					pS.println();
				}
				pS.close();	
			}


			catch(IOException e){
				System.out.println("Probleme ecriture "+resultSim);
			}
		}*/

	}





}


